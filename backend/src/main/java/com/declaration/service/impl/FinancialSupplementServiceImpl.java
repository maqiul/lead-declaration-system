package com.declaration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.entity.DeclarationMaterialItem;
import com.declaration.entity.DeclarationProduct;
import com.declaration.entity.DeclarationRemittance;
import com.declaration.entity.FinancialSupplement;
import com.declaration.entity.MaterialAttachment;
import com.declaration.entity.ProductTypeConfig;
import com.declaration.dao.FinancialSupplementMapper;
import com.declaration.service.DeclarationMaterialItemService;
import com.declaration.service.DeclarationProductService;
import com.declaration.service.DeclarationRemittanceService;
import com.declaration.service.FinancialSupplementService;
import com.declaration.service.MaterialAttachmentService;
import com.declaration.service.ProductTypeConfigService;
import com.declaration.service.CurrencyInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialSupplementServiceImpl extends ServiceImpl<FinancialSupplementMapper, FinancialSupplement> implements FinancialSupplementService {

    private final DeclarationRemittanceService remittanceService;
    private final DeclarationMaterialItemService materialItemService;
    private final MaterialAttachmentService materialAttachmentService;
    private final DeclarationProductService declarationProductService;
    private final ProductTypeConfigService productTypeConfigService;
    private final CurrencyInfoService currencyInfoService;

    // 数字格式化器
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat RATE_FORMAT = new DecimalFormat("0.####");

    // 资料项发票模板 code（数据源：declaration_material_item.code）
    private static final String CODE_FREIGHT = "FREIGHT_INVOICE";
    private static final String CODE_CUSTOMS = "CUSTOMS_AGENT_INVOICE";

    /**
     * 获取所有扣款类资料项的金额汇总
     * 规则：直接遍历所有资料项，invoiceCategory=DEDUCTION 的纳入扣款，其余跳过
     */
    private Map<String, Object> getAllInvoiceDeductions(Long formId) {
        List<DeclarationMaterialItem> items = materialItemService.listByFormId(formId);
        List<Map<String, Object>> deductions = new ArrayList<>();
        BigDecimal totalDeduction = BigDecimal.ZERO;

        if (items == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("items", deductions);
            result.put("total", BigDecimal.ZERO);
            return result;
        }

        for (DeclarationMaterialItem item : items) {
            // 只取扣款项
            if (!"DEDUCTION".equals(item.getInvoiceCategory())) continue;

            BigDecimal itemAmount = sumItemAmount(item);
            if (itemAmount.compareTo(BigDecimal.ZERO) <= 0) continue;

            Map<String, Object> deduction = new LinkedHashMap<>();
            deduction.put("name", item.getName());
            deduction.put("code", item.getCode());
            deduction.put("amount", itemAmount);
            deductions.add(deduction);
            totalDeduction = totalDeduction.add(itemAmount);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", deductions);
        result.put("total", totalDeduction);
        return result;
    }

    /** 汇总资料项金额（附件金额优先，无附件则取 item 级金额） */
    private BigDecimal sumItemAmount(DeclarationMaterialItem item) {
        if (item.getId() != null) {
            List<MaterialAttachment> attachments = materialAttachmentService.listByItemId(item.getId());
            if (attachments != null && !attachments.isEmpty()) {
                BigDecimal total = BigDecimal.ZERO;
                for (MaterialAttachment att : attachments) {
                    if (att.getAmount() != null) {
                        total = total.add(att.getAmount());
                    }
                }
                return total;
            }
        }
        return item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
    }

    /** 从申报资料项中按 code 读取发票金额，支持多附件金额汇总 */
    private BigDecimal getInvoiceAmountFromMaterial(Long formId, String code) {
        if (formId == null || code == null) return BigDecimal.ZERO;
        List<DeclarationMaterialItem> items = materialItemService.listByFormId(formId);
        if (items == null) return BigDecimal.ZERO;
        for (DeclarationMaterialItem item : items) {
            if (code.equals(item.getCode())) {
                // 优先汇总附件级别的金额（支持多张发票）
                if (item.getId() != null) {
                    List<MaterialAttachment> attachments = materialAttachmentService.listByItemId(item.getId());
                    if (attachments != null && !attachments.isEmpty()) {
                        BigDecimal total = BigDecimal.ZERO;
                        for (MaterialAttachment att : attachments) {
                            if (att.getAmount() != null) {
                                total = total.add(att.getAmount());
                            }
                        }
                        return total;
                    }
                }
                // 无附件时回退到 item 级别金额
                return item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    /** 从申报资料项中按 code 读取发票号，供 Excel 导出等展示场景使用 */
    public String getInvoiceNoFromMaterial(Long formId, String code) {
        if (formId == null || code == null) return null;
        List<DeclarationMaterialItem> items = materialItemService.listByFormId(formId);
        if (items == null) return null;
        for (DeclarationMaterialItem item : items) {
            if (code.equals(item.getCode())) {
                return item.getInvoiceNo();
            }
        }
        return null;
    }

    public static String getFreightCode() { return CODE_FREIGHT; }
    public static String getCustomsCode() { return CODE_CUSTOMS; }

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
        BigDecimal totalInternalBankFee = BigDecimal.ZERO;    // 内部操作手续费(CNY)汇总
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
            String currency = r.get("currency") != null ? (String) r.get("currency") : getDefaultCurrency();
            
            // 计算人民币金额: 原币金额 × 汇率
            BigDecimal cnyAmount = amt.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
            totalCny = totalCny.add(cnyAmount);

            // 按币种统计原币金额
            currencyOriginalAmounts.merge(currency, amt, BigDecimal::add);

            // 银行手续费：主关联扣全额，副关联不扣
            BigDecimal bankFeeOriginal = r.get("bankFee") != null ? (BigDecimal) r.get("bankFee") : BigDecimal.ZERO;
            BigDecimal bankFeeCny = BigDecimal.ZERO;
            
            // 内部操作手续费（CNY）：始终按比例分摊
            BigDecimal internalBankFeeOriginal = r.get("internalBankFee") != null ? (BigDecimal) r.get("internalBankFee") : BigDecimal.ZERO;
            BigDecimal internalBankFee = BigDecimal.ZERO;
            
            // 判断是否为主关联（relationType=1）
            Integer relationType = r.get("relationType") != null ? (Integer) r.get("relationType") : null;
            boolean isMainRelation = (relationType != null && relationType == 1);
            
            // 计算分摊比例（内部操作费始终按比例）
            BigDecimal proportion = BigDecimal.ONE;
            if (fullAmt.compareTo(BigDecimal.ZERO) > 0 && relationAmt != null && relationAmt.compareTo(BigDecimal.ZERO) > 0) {
                proportion = relationAmt.divide(fullAmt, 8, RoundingMode.HALF_UP);
            }
            
            // 银行手续费：主关联扣全额，副关联不扣
            if (bankFeeOriginal.compareTo(BigDecimal.ZERO) > 0 && isMainRelation) {
                bankFeeCny = bankFeeOriginal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
            }
            totalBankFeeCny = totalBankFeeCny.add(bankFeeCny);
            
            // 内部操作手续费：始终按比例分摊
            if (internalBankFeeOriginal.compareTo(BigDecimal.ZERO) > 0) {
                internalBankFee = internalBankFeeOriginal.multiply(proportion).setScale(2, RoundingMode.HALF_UP);
            }
            totalInternalBankFee = totalInternalBankFee.add(internalBankFee);

            // 获取银行手续费率（仅展示用）
            BigDecimal feeRate = r.get("bankFeeRate") != null ? (BigDecimal) r.get("bankFeeRate") : BigDecimal.ZERO;

            // 收集银行名称
            String bankName = r.get("bankAccountName") != null ? (String) r.get("bankAccountName") : "";
            if (!bankName.isEmpty()) {
                bankNames.add(bankName);
            }

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("amount", amt);
            detail.put("fullAmount", fullAmt);
            detail.put("relationAmount", relationAmt);
            detail.put("proportion", proportion.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)); // 占比百分比
            detail.put("taxRate", taxRate);
            detail.put("cnyAmount", cnyAmount);
            detail.put("remittanceName", r.get("remittanceName"));
            detail.put("bankAccountName", bankName);
            detail.put("bankFeeOriginal", bankFeeOriginal);  // 水单原始银行手续费（原币）
            detail.put("bankFee", isMainRelation ? bankFeeOriginal : BigDecimal.ZERO); // 主关联扣全额，副关联不扣
            detail.put("bankFeeCny", bankFeeCny);             // 银行手续费（CNY）
            detail.put("internalBankFeeOriginal", internalBankFeeOriginal); // 水单原始内部操作手续费（CNY）
            detail.put("internalBankFee", internalBankFee);   // 按比例分摊后的内部操作手续费（CNY）
            detail.put("bankFeeRate", feeRate);
            detail.put("currency", currency);
            remittanceDetails.add(detail);

            calculationSteps.add(String.format("收汇: %s %s × %s = %s CNY, 银行手续费: %s CNY, 内部操作费: %s CNY",
                    AMOUNT_FORMAT.format(amt), currency, RATE_FORMAT.format(taxRate), AMOUNT_FORMAT.format(cnyAmount),
                    AMOUNT_FORMAT.format(bankFeeCny), AMOUNT_FORMAT.format(internalBankFee)));
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

        // 6. 获取所有发票类资料项扣减金额
        Map<String, Object> invoiceDeductionsResult = getAllInvoiceDeductions(formId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> invoiceDeductionItems = (List<Map<String, Object>>) invoiceDeductionsResult.get("items");
        BigDecimal totalInvoiceDeduction = (BigDecimal) invoiceDeductionsResult.get("total");

        // 6.1 手续费已在上方循环中累加完毕（totalBankFeeCny + totalInternalBankFee）

        // 7. 按商品级退税率计算退税加成（无退税率则按 0 计算）
        // 7.1 查询该申报单所有产品
        List<DeclarationProduct> products = declarationProductService.lambdaQuery()
                .eq(DeclarationProduct::getFormId, formId)
                .list();

        // 7.2 计算产品总金额（原币），用于比例分摊
        BigDecimal totalProductAmount = BigDecimal.ZERO;
        for (DeclarationProduct product : products) {
            if (product.getAmount() != null && product.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                totalProductAmount = totalProductAmount.add(product.getAmount());
            }
        }

        // 7.3 逐产品按比例分摊 totalGoodsAmount(CNY)，最后一个产品差额补齐
        BigDecimal amountWithTaxRefund = BigDecimal.ZERO;
        BigDecimal allocatedCny = BigDecimal.ZERO;  // 已累计分配的CNY
        List<Map<String, Object>> productTaxDetails = new ArrayList<>();

        if (products.isEmpty() || totalProductAmount.compareTo(BigDecimal.ZERO) <= 0) {
            // 无产品或无金额时，不计算退税
            amountWithTaxRefund = totalGoodsAmount;
            calculationSteps.add(String.format("退税加成: %s CNY（无商品信息）", AMOUNT_FORMAT.format(amountWithTaxRefund)));
        } else {
            // 过滤出有效产品
            List<DeclarationProduct> validProducts = new ArrayList<>();
            for (DeclarationProduct product : products) {
                BigDecimal pa = product.getAmount() != null ? product.getAmount() : BigDecimal.ZERO;
                if (pa.compareTo(BigDecimal.ZERO) > 0) {
                    validProducts.add(product);
                }
            }

            for (int i = 0; i < validProducts.size(); i++) {
                DeclarationProduct product = validProducts.get(i);
                BigDecimal productAmount = product.getAmount();
                boolean isLast = (i == validProducts.size() - 1);

                // 按比例分摊 CNY：最后一个产品用差额补齐，避免精度丢失
                BigDecimal productCny;
                if (isLast) {
                    productCny = totalGoodsAmount.subtract(allocatedCny);
                } else {
                    BigDecimal proportion = productAmount.divide(totalProductAmount, 8, RoundingMode.HALF_UP);
                    productCny = totalGoodsAmount.multiply(proportion).setScale(2, RoundingMode.HALF_UP);
                    allocatedCny = allocatedCny.add(productCny);
                }

                // 查询该 HS 编码的退税率，无则默认 0
                BigDecimal productTaxRefundRate = BigDecimal.ZERO;
                if (product.getHsCode() != null && !product.getHsCode().isEmpty()) {
                    ProductTypeConfig typeConfig = productTypeConfigService.getByHsCode(product.getHsCode());
                    if (typeConfig != null && typeConfig.getTaxRefundRate() != null) {
                        productTaxRefundRate = typeConfig.getTaxRefundRate().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
                    }
                }

                // 该产品的退税加成：CNY × (1 + 退税率)
                BigDecimal productAmountWithTax = productCny.multiply(BigDecimal.ONE.add(productTaxRefundRate))
                        .setScale(2, RoundingMode.HALF_UP);
                amountWithTaxRefund = amountWithTaxRefund.add(productAmountWithTax);

                // 记录详情用于展示
                BigDecimal displayProportion = productAmount.divide(totalProductAmount, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("productName", product.getProductName() != null ? product.getProductName() : product.getProductChineseName());
                detail.put("hsCode", product.getHsCode());
                detail.put("quantity", product.getQuantity());
                detail.put("unit", product.getUnit());
                detail.put("unitPrice", product.getUnitPrice());
                detail.put("amount", productAmount);
                detail.put("proportion", displayProportion);
                detail.put("cnyAmount", productCny);
                detail.put("taxRefundRate", productTaxRefundRate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
                detail.put("amountWithTaxRefund", productAmountWithTax);
                productTaxDetails.add(detail);

                calculationSteps.add(String.format("商品[%s] 分摊%s%%: %s CNY, 退税率%s%%: %s × (1 + %s%%) = %s CNY",
                        product.getProductName() != null ? product.getProductName() : product.getHsCode(),
                        RATE_FORMAT.format(displayProportion),
                        AMOUNT_FORMAT.format(productCny),
                        RATE_FORMAT.format(productTaxRefundRate.multiply(new BigDecimal("100"))),
                        AMOUNT_FORMAT.format(productCny),
                        RATE_FORMAT.format(productTaxRefundRate.multiply(new BigDecimal("100"))),
                        AMOUNT_FORMAT.format(productAmountWithTax)));
            }

            // 汇总展示
            calculationSteps.add(String.format("退税加成合计: %s CNY", AMOUNT_FORMAT.format(amountWithTaxRefund)));
        }

        // 8. 逐项扣减发票类资料项
        for (Map<String, Object> deduction : invoiceDeductionItems) {
            String name = (String) deduction.get("name");
            BigDecimal amt = (BigDecimal) deduction.get("amount");
            calculationSteps.add(String.format("减 %s: -%s CNY", name, AMOUNT_FORMAT.format(amt)));
        }
        if (invoiceDeductionItems.isEmpty()) {
            calculationSteps.add("发票扣减项: 无");
        }

        // 9. 手续费分别计算（已按关联比例分摊）
        BigDecimal bankFeeAmount = totalBankFeeCny;
        BigDecimal internalBankFeeTotal = totalInternalBankFee;
        BigDecimal totalFeeAmount = bankFeeAmount.add(internalBankFeeTotal);
        // 分别计算费率
        BigDecimal bankFeeRate = BigDecimal.ZERO;
        BigDecimal internalBankFeeRate = BigDecimal.ZERO;
        if (totalCny.compareTo(BigDecimal.ZERO) > 0) {
            bankFeeRate = bankFeeAmount.divide(totalCny, 6, RoundingMode.HALF_UP);
            internalBankFeeRate = internalBankFeeTotal.divide(totalCny, 6, RoundingMode.HALF_UP);
        }
        BigDecimal bankFeePercent = bankFeeRate.multiply(new BigDecimal("100"));
        BigDecimal internalBankFeePercent = internalBankFeeRate.multiply(new BigDecimal("100"));
        calculationSteps.add(String.format("减 银行手续费: -%s CNY (费率≈%s%%), 内部操作费: -%s CNY (费率≈%s%%), 合计: -%s CNY",
                AMOUNT_FORMAT.format(bankFeeAmount), RATE_FORMAT.format(bankFeePercent),
                AMOUNT_FORMAT.format(internalBankFeeTotal), RATE_FORMAT.format(internalBankFeePercent),
                AMOUNT_FORMAT.format(totalFeeAmount)));

        // 10. 计算最终开票金额：退税加成合计 - 扣款项 - 手续费
        BigDecimal invoiceAmount = amountWithTaxRefund
                .subtract(totalInvoiceDeduction)
                .subtract(totalFeeAmount)
                .setScale(2, RoundingMode.HALF_UP);

        calculationSteps.add(String.format("开票金额: %s - %s - %s = %s CNY",
                AMOUNT_FORMAT.format(amountWithTaxRefund),
                AMOUNT_FORMAT.format(totalInvoiceDeduction),
                AMOUNT_FORMAT.format(totalFeeAmount),
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
        result.put("productTaxDetails", productTaxDetails);
        result.put("amountWithTaxRefund", amountWithTaxRefund);
        result.put("invoiceDeductionItems", invoiceDeductionItems);
        result.put("totalInvoiceDeduction", totalInvoiceDeduction);
        result.put("bankFeeRate", bankFeePercent);
        result.put("bankFeeAmount", bankFeeAmount);
        result.put("internalBankFeeRate", internalBankFeePercent);
        result.put("internalBankFee", internalBankFeeTotal);
        result.put("totalFeeAmount", totalFeeAmount);
        result.put("foreignExchangeBank", foreignExchangeBank);

        // 最终结果
        result.put("invoiceAmount", invoiceAmount);

        // 完整计算步骤
        result.put("calculationSteps", calculationSteps);

        // 为前端兼容添加字段别名
        result.put("amountWithTax", result.get("amountWithTaxRefund"));
        result.put("bankFee", result.get("bankFeeAmount"));
        // 保留旧的单值字段兼容（取货代和报关代理的值，如果有的话）
        BigDecimal freightAmount = BigDecimal.ZERO;
        BigDecimal customsAmount = BigDecimal.ZERO;
        for (Map<String, Object> d : invoiceDeductionItems) {
            if (CODE_FREIGHT.equals(d.get("code"))) freightAmount = (BigDecimal) d.get("amount");
            if (CODE_CUSTOMS.equals(d.get("code"))) customsAmount = (BigDecimal) d.get("amount");
        }
        result.put("freightAmount", freightAmount);
        result.put("freightInvoiceAmount", freightAmount);
        result.put("customsAmount", customsAmount);
        result.put("customsInvoiceAmount", customsAmount);

        return result;
    }

    /**
     * 从配置获取默认币种，不再写死 USD
     */
    private String getDefaultCurrency() {
        try {
            var list = currencyInfoService.getEnabledList();
            if (list != null && !list.isEmpty()) {
                return list.get(0).getCurrencyCode();
            }
        } catch (Exception e) {
            log.warn("获取默认币种配置失败，回退 USD", e);
        }
        return "USD";
    }
}
