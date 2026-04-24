package com.declaration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.entity.DeclarationRemittance;
import com.declaration.entity.FinancialSupplement;
import com.declaration.dao.FinancialSupplementMapper;
import com.declaration.service.DeclarationRemittanceService;
import com.declaration.service.FinancialSupplementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FinancialSupplementServiceImpl extends ServiceImpl<FinancialSupplementMapper, FinancialSupplement> implements FinancialSupplementService {

    private final DeclarationRemittanceService remittanceService;

    // 数字格式化器
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat RATE_FORMAT = new DecimalFormat("0.####");

    @Override
    public Map<String, Object> getCalculationDetail(Long formId) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> calculationSteps = new ArrayList<>();

        // 1. 查询财务补充记录
        FinancialSupplement supp = lambdaQuery()
                .eq(FinancialSupplement::getFormId, formId)
                .one();

        // 2. 查询该申报单关联的所有已审核水单
        List<Map<String, Object>> remittances = remittanceService.getRemittancesByFormId(formId);
        
        // 3. 过滤已审核的水单(status=2)
        List<Map<String, Object>> auditedRemittances = new ArrayList<>();
        if (remittances != null) {
            for (Map<String, Object> r : remittances) {
                if (r.get("status") != null && ((Integer) r.get("status")) == 2) {
                    auditedRemittances.add(r);
                }
            }
        }

        // 4. 统一计算所有已审核水单（不区分定金尾款）
        BigDecimal totalCny = BigDecimal.ZERO;               // 人民币汇总
        BigDecimal totalBankFeeCny = BigDecimal.ZERO;         // 银行手续费(CNY)汇总
        Set<String> bankNames = new LinkedHashSet<>();
        // 按币种分组统计原币金额
        Map<String, BigDecimal> currencyOriginalAmounts = new LinkedHashMap<>();

        List<Map<String, Object>> remittanceDetails = new ArrayList<>();
        for (Map<String, Object> r : auditedRemittances) {
            // 使用关联金额（分配给该申报单的金额），如果没有则用水单全额
            BigDecimal relationAmt = r.get("relationAmount") != null ? (BigDecimal) r.get("relationAmount") : null;
            BigDecimal fullAmt = r.get("remittanceAmount") != null ? (BigDecimal) r.get("remittanceAmount") : BigDecimal.ZERO;
            BigDecimal amt = (relationAmt != null && relationAmt.compareTo(BigDecimal.ZERO) > 0) ? relationAmt : fullAmt;
            BigDecimal taxRate = r.get("taxRate") != null ? (BigDecimal) r.get("taxRate") : BigDecimal.ZERO;
            String currency = r.get("currency") != null ? (String) r.get("currency") : "USD";
            
            // 计算人民币金额: 原币金额 × 汇率
            BigDecimal cnyAmount = amt.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
            totalCny = totalCny.add(cnyAmount);

            // 按币种统计原币金额
            currencyOriginalAmounts.merge(currency, amt, BigDecimal::add);

            // 计算银行手续费：直接用 关联金额 × 手续费率
            BigDecimal feeRate = r.get("bankFeeRate") != null ? (BigDecimal) r.get("bankFeeRate") : BigDecimal.ZERO;
            BigDecimal proportionalFee = amt.multiply(feeRate).setScale(4, RoundingMode.HALF_UP);
            // 手续费转CNY: 原币手续费 × 汇率
            BigDecimal bankFeeCny = proportionalFee.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
            totalBankFeeCny = totalBankFeeCny.add(bankFeeCny);

            // 收集银行名称
            String bankName = r.get("bankAccountName") != null ? (String) r.get("bankAccountName") : "";
            if (!bankName.isEmpty()) {
                bankNames.add(bankName);
            }

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("amount", amt);
            detail.put("fullAmount", fullAmt);
            detail.put("relationAmount", relationAmt);
            detail.put("taxRate", taxRate);
            detail.put("cnyAmount", cnyAmount);
            detail.put("remittanceName", r.get("remittanceName"));
            detail.put("bankAccountName", bankName);
            detail.put("bankFee", proportionalFee);       // 按比例调整后的手续费（原币）
            detail.put("bankFeeCny", bankFeeCny);           // 手续费CNY
            detail.put("bankFeeRate", feeRate);
            detail.put("currency", currency);
            remittanceDetails.add(detail);

            calculationSteps.add(String.format("收汇: %s %s × %s = %s CNY, 手续费: %s %s = %s CNY",
                    AMOUNT_FORMAT.format(amt), currency, RATE_FORMAT.format(taxRate), AMOUNT_FORMAT.format(cnyAmount),
                    AMOUNT_FORMAT.format(proportionalFee), currency, AMOUNT_FORMAT.format(bankFeeCny)));
        }

        // 计算加权平均汇率（只对同币种有意义，多币种时显示综合汇率）
        BigDecimal totalOriginalAmount = currencyOriginalAmounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal weightedRate = BigDecimal.ZERO;
        if (totalOriginalAmount.compareTo(BigDecimal.ZERO) > 0) {
            weightedRate = totalCny.divide(totalOriginalAmount, 4, RoundingMode.HALF_UP);
        }
        String foreignExchangeBank = String.join("、", bankNames);

        // 5. 计算总货物金额
        BigDecimal totalGoodsAmount = totalCny;
        calculationSteps.add(String.format("总货物金额: %s CNY", AMOUNT_FORMAT.format(totalGoodsAmount)));

        // 6. 获取财务补充参数
        BigDecimal taxRefundRate = supp != null && supp.getTaxRefundRate() != null
                ? supp.getTaxRefundRate().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)  // 百分比转小数
                : BigDecimal.ZERO;
        BigDecimal freightAmount = supp != null && supp.getFreightAmount() != null
                ? supp.getFreightAmount() : BigDecimal.ZERO;
        BigDecimal customsAmount = supp != null && supp.getCustomsAmount() != null
                ? supp.getCustomsAmount() : BigDecimal.ZERO;

        // 6.1 银行手续费已在上方循环中累加完毕（totalBankFeeCny）
        // 计算综合手续费率（信息展示用）
        BigDecimal bankFeeRate = BigDecimal.ZERO;
        if (totalCny.compareTo(BigDecimal.ZERO) > 0) {
            bankFeeRate = totalBankFeeCny.divide(totalCny, 6, RoundingMode.HALF_UP);
        }

        // 7. 计算退税加成金额: 总金额 * (1 + 退税点)
        BigDecimal onePlusTaxRate = BigDecimal.ONE.add(taxRefundRate);
        BigDecimal amountWithTaxRefund = totalGoodsAmount.multiply(onePlusTaxRate).setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxRatePercent = taxRefundRate.multiply(new BigDecimal("100"));
        calculationSteps.add(String.format("退税加成: %s × (1 + %s%%) = %s CNY",
                AMOUNT_FORMAT.format(totalGoodsAmount), RATE_FORMAT.format(taxRatePercent), AMOUNT_FORMAT.format(amountWithTaxRefund)));

        // 8. 扣除货代发票
        calculationSteps.add(String.format("减 货代发票: -%s CNY", AMOUNT_FORMAT.format(freightAmount)));

        // 9. 扣除海关代理发票
        calculationSteps.add(String.format("减 海关代理发票: -%s CNY", AMOUNT_FORMAT.format(customsAmount)));

        // 10. 银行手续费直接用累加值（已按关联比例分摊并转CNY）
        BigDecimal bankFeeAmount = totalBankFeeCny;
        BigDecimal bankFeePercent = bankFeeRate.multiply(new BigDecimal("100"));
        calculationSteps.add(String.format("减 银行手续费合计: -%s CNY (综合费率≈%s%%)",
                AMOUNT_FORMAT.format(bankFeeAmount), RATE_FORMAT.format(bankFeePercent)));

        // 11. 计算最终开票金额
        BigDecimal invoiceAmount = amountWithTaxRefund
                .subtract(freightAmount)
                .subtract(customsAmount)
                .subtract(bankFeeAmount)
                .setScale(2, RoundingMode.HALF_UP);

        calculationSteps.add(String.format("开票金额: %s - %s - %s - %s = %s CNY",
                AMOUNT_FORMAT.format(amountWithTaxRefund),
                AMOUNT_FORMAT.format(freightAmount),
                AMOUNT_FORMAT.format(customsAmount),
                AMOUNT_FORMAT.format(bankFeeAmount),
                AMOUNT_FORMAT.format(invoiceAmount)));

        // 12. 组装返回结果
        // 收汇信息
        result.put("totalOriginalAmount", totalOriginalAmount);
        result.put("weightedExchangeRate", weightedRate);
        result.put("totalCny", totalCny);
        result.put("remittanceDetails", remittanceDetails);
        result.put("remittanceCount", auditedRemittances.size());

        // 汇总金额
        result.put("totalGoodsAmount", totalGoodsAmount);

        // 财务参数
        result.put("taxRefundRate", taxRatePercent);
        result.put("amountWithTaxRefund", amountWithTaxRefund);
        result.put("freightInvoiceAmount", freightAmount);
        result.put("customsInvoiceAmount", customsAmount);
        result.put("bankFeeRate", bankFeePercent);
        result.put("bankFeeAmount", bankFeeAmount);
        result.put("foreignExchangeBank", foreignExchangeBank);

        // 最终结果
        result.put("invoiceAmount", invoiceAmount);

        // 完整计算步骤
        result.put("calculationSteps", calculationSteps);

        // 为前端兼容添加字段别名
        result.put("amountWithTax", result.get("amountWithTaxRefund"));
        result.put("freightAmount", result.get("freightInvoiceAmount"));
        result.put("customsAmount", result.get("customsInvoiceAmount"));
        result.put("bankFee", result.get("bankFeeAmount"));

        return result;
    }
}
