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
        
        // 2. 查询所有水单记录
        List<DeclarationRemittance> remittances = remittanceService.lambdaQuery()
                .eq(DeclarationRemittance::getFormId, formId)
                .list();
        
        // 3. 分离定金和尾款
        List<DeclarationRemittance> deposits = new ArrayList<>();
        List<DeclarationRemittance> balances = new ArrayList<>();
        
        if (remittances != null) {
            for (DeclarationRemittance r : remittances) {
                if (r.getRemittanceType() != null && r.getRemittanceType() == 1) {
                    deposits.add(r);
                } else if (r.getRemittanceType() != null && r.getRemittanceType() == 2) {
                    balances.add(r);
                }
            }
        }
        
        // 4. 计算定金汇总
        BigDecimal depositAmount = BigDecimal.ZERO;         // 定金原币金额汇总
        BigDecimal depositCny = BigDecimal.ZERO;            // 定金人民币汇总
        BigDecimal depositWeightedRate = BigDecimal.ZERO;   // 加权平均汇率
        
        List<Map<String, Object>> depositDetails = new ArrayList<>();
        for (DeclarationRemittance d : deposits) {
            BigDecimal amt = d.getRemittanceAmount() != null ? d.getRemittanceAmount() : BigDecimal.ZERO;
            BigDecimal rate = d.getExchangeRate() != null ? d.getExchangeRate() : BigDecimal.ONE;
            BigDecimal cny = amt.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            
            depositAmount = depositAmount.add(amt);
            depositCny = depositCny.add(cny);
            
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("amount", amt);
            detail.put("exchangeRate", rate);
            detail.put("cny", cny);
            detail.put("remittanceName", d.getRemittanceName());
            depositDetails.add(detail);
            
            calculationSteps.add(String.format("定金: %s USD × %s = %s CNY", 
                    AMOUNT_FORMAT.format(amt), RATE_FORMAT.format(rate), AMOUNT_FORMAT.format(cny)));
        }
        
        // 计算定金加权平均汇率
        if (depositAmount.compareTo(BigDecimal.ZERO) > 0) {
            depositWeightedRate = depositCny.divide(depositAmount, 4, RoundingMode.HALF_UP);
        }
        
        // 5. 计算尾款汇总
        BigDecimal balanceAmount = BigDecimal.ZERO;         // 尾款原币金额汇总
        BigDecimal balanceCny = BigDecimal.ZERO;            // 尾款人民币汇总
        BigDecimal balanceWeightedRate = BigDecimal.ZERO;   // 加权平均汇率
        
        List<Map<String, Object>> balanceDetails = new ArrayList<>();
        for (DeclarationRemittance b : balances) {
            BigDecimal amt = b.getRemittanceAmount() != null ? b.getRemittanceAmount() : BigDecimal.ZERO;
            BigDecimal rate = b.getExchangeRate() != null ? b.getExchangeRate() : BigDecimal.ONE;
            BigDecimal cny = amt.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            
            balanceAmount = balanceAmount.add(amt);
            balanceCny = balanceCny.add(cny);
            
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("amount", amt);
            detail.put("exchangeRate", rate);
            detail.put("cny", cny);
            detail.put("remittanceName", b.getRemittanceName());
            balanceDetails.add(detail);
            
            calculationSteps.add(String.format("尾款: %s USD × %s = %s CNY", 
                    AMOUNT_FORMAT.format(amt), RATE_FORMAT.format(rate), AMOUNT_FORMAT.format(cny)));
        }
        
        // 计算尾款加权平均汇率
        if (balanceAmount.compareTo(BigDecimal.ZERO) > 0) {
            balanceWeightedRate = balanceCny.divide(balanceAmount, 4, RoundingMode.HALF_UP);
        }
        
        // 6. 计算总货物金额
        BigDecimal totalGoodsAmount = depositCny.add(balanceCny);
        calculationSteps.add(String.format("总货物金额: %s + %s = %s CNY", 
                AMOUNT_FORMAT.format(depositCny), AMOUNT_FORMAT.format(balanceCny), AMOUNT_FORMAT.format(totalGoodsAmount)));
        
        // 7. 获取财务补充参数
        BigDecimal taxRefundRate = supp != null && supp.getTaxRefundRate() != null 
                ? supp.getTaxRefundRate().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)  // 百分比转小数
                : BigDecimal.ZERO;
        BigDecimal bankFeeRate = supp != null && supp.getBankFeeRate() != null 
                ? supp.getBankFeeRate().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)    // 百分比转小数
                : BigDecimal.ZERO;
        BigDecimal freightAmount = supp != null && supp.getFreightAmount() != null 
                ? supp.getFreightAmount() : BigDecimal.ZERO;
        BigDecimal customsAmount = supp != null && supp.getCustomsAmount() != null 
                ? supp.getCustomsAmount() : BigDecimal.ZERO;
        String foreignExchangeBank = supp != null ? supp.getForeignExchangeBank() : "";
        
        // 8. 计算退税加成金额: 总金额 * (1 + 退税点)
        BigDecimal onePlusTaxRate = BigDecimal.ONE.add(taxRefundRate);
        BigDecimal amountWithTaxRefund = totalGoodsAmount.multiply(onePlusTaxRate).setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal taxRatePercent = taxRefundRate.multiply(new BigDecimal("100"));
        calculationSteps.add(String.format("退税加成: %s × (1 + %s%%) = %s CNY", 
                AMOUNT_FORMAT.format(totalGoodsAmount), RATE_FORMAT.format(taxRatePercent), AMOUNT_FORMAT.format(amountWithTaxRefund)));
        
        // 9. 扣除货代发票
        calculationSteps.add(String.format("减 货代发票: -%s CNY", AMOUNT_FORMAT.format(freightAmount)));
        
        // 10. 扣除海关代理发票
        calculationSteps.add(String.format("减 海关代理发票: -%s CNY", AMOUNT_FORMAT.format(customsAmount)));
        
        // 11. 计算银行手续费: 总金额 * 手续费率
        BigDecimal bankFeeAmount = totalGoodsAmount.multiply(bankFeeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal bankFeePercent = bankFeeRate.multiply(new BigDecimal("100"));
        calculationSteps.add(String.format("减 银行手续费: %s × %s%% = -%s CNY", 
                AMOUNT_FORMAT.format(totalGoodsAmount), RATE_FORMAT.format(bankFeePercent), AMOUNT_FORMAT.format(bankFeeAmount)));
        
        // 12. 计算最终开票金额
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
        
        // 13. 组装返回结果
        // 定金信息
        result.put("depositAmount", depositAmount);
        result.put("depositExchangeRate", depositWeightedRate);
        result.put("depositCny", depositCny);
        result.put("depositDetails", depositDetails);
        result.put("depositCount", deposits.size());
        
        // 尾款信息
        result.put("balanceAmount", balanceAmount);
        result.put("balanceExchangeRate", balanceWeightedRate);
        result.put("balanceCny", balanceCny);
        result.put("balanceDetails", balanceDetails);
        result.put("balanceCount", balances.size());
        
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
        
        // 合并定金和尾款为统一的 remittanceDetails 列表
        List<Map<String, Object>> remittanceDetails = new ArrayList<>();
        for (Map<String, Object> detail : depositDetails) {
            Map<String, Object> item = new LinkedHashMap<>(detail);
            item.put("type", 1);
            item.put("cnyAmount", detail.get("cny"));
            remittanceDetails.add(item);
        }
        for (Map<String, Object> detail : balanceDetails) {
            Map<String, Object> item = new LinkedHashMap<>(detail);
            item.put("type", 2);
            item.put("cnyAmount", detail.get("cny"));
            remittanceDetails.add(item);
        }
        result.put("remittanceDetails", remittanceDetails);
        
        return result;
    }
}
