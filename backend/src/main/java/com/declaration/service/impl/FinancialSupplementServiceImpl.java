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
    private final DeclarationMaterialItemService materialItemService;
    private final MaterialAttachmentService materialAttachmentService;
    private final DeclarationProductService declarationProductService;
    private final ProductTypeConfigService productTypeConfigService;

    // 数字格式化器
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat RATE_FORMAT = new DecimalFormat("0.####");

    // 资料项发票模板 code（数据源：declaration_material_item.code）
    private static final String CODE_FREIGHT = "FREIGHT_INVOICE";
    private static final String CODE_CUSTOMS = "CUSTOMS_AGENT_INVOICE";

    /**
     * 获取所有发票类资料项的扣减金额汇总
     * 判断规则：formSchema 含 amount 字段 / stage=INVOICE / 已知发票编码
     * 返回：每项的{name, code, amount}列表 + 合计
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
            if (!isInvoiceTypeItem(item)) continue;

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

    /** 判断资料项是否为发票类型（仅资料提交环节，排除业务发票） */
    private boolean isInvoiceTypeItem(DeclarationMaterialItem item) {
        // 排除业务发票环节（INVOICE 是出口发票，不是费用扣减项）
        if ("INVOICE".equals(item.getStage())) return false;
        // 排除财务补充环节
        if ("FINANCE_SUPPLEMENT".equals(item.getStage())) return false;
        // 1. 已知发票编码（货代/报关代理）
        if (CODE_FREIGHT.equals(item.getCode()) || CODE_CUSTOMS.equals(item.getCode())) return true;
        // 2. 资料提交环节中 formSchema 含 amount 字段的项（发票类默认 schema）
        if ("MATERIAL_SUBMIT".equals(item.getStage())
                && item.getFormSchema() != null && item.getFormSchema().contains("\"amount\"")) return true;
        return false;
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
            String currency = r.get("currency") != null ? (String) r.get("currency") : "USD";
            
            // 计算人民币金额: 原币金额 × 汇率
            BigDecimal cnyAmount = amt.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
            totalCny = totalCny.add(cnyAmount);

            // 按币种统计原币金额
            currencyOriginalAmounts.merge(currency, amt, BigDecimal::add);

            // 计算银行手续费（原币）：从水单记录取 bankFee，按比例分摊
            // 比例 = 关联金额 / 水单总金额（拆分时按比例分配）
            BigDecimal bankFeeOriginal = r.get("bankFee") != null ? (BigDecimal) r.get("bankFee") : BigDecimal.ZERO;
            BigDecimal proportionalFee = BigDecimal.ZERO;
            BigDecimal bankFeeCny = BigDecimal.ZERO;
            
            // 计算内部操作手续费（CNY）：从水单记录取 internalBankFee，按比例分摊
            BigDecimal internalBankFeeOriginal = r.get("internalBankFee") != null ? (BigDecimal) r.get("internalBankFee") : BigDecimal.ZERO;
            BigDecimal internalBankFee = BigDecimal.ZERO;
            
            // 计算分摊比例
            BigDecimal proportion = BigDecimal.ONE;
            if (fullAmt.compareTo(BigDecimal.ZERO) > 0 && relationAmt != null && relationAmt.compareTo(BigDecimal.ZERO) > 0) {
                proportion = relationAmt.divide(fullAmt, 8, RoundingMode.HALF_UP);
            }
            
            // 银行手续费按比例分摊
            if (bankFeeOriginal.compareTo(BigDecimal.ZERO) > 0) {
                // 按比例分摊后的原币手续费
                proportionalFee = bankFeeOriginal.multiply(proportion).setScale(4, RoundingMode.HALF_UP);
                // 转换为人民币
                bankFeeCny = proportionalFee.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
            }
            totalBankFeeCny = totalBankFeeCny.add(bankFeeCny);
            
            // 内部操作手续费按比例分摊（已经是CNY）
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
            detail.put("bankFee", proportionalFee);           // 按比例分摊后的银行手续费（原币）
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

        // 7.2 获取第一条水单的汇率（用于商品换算CNY）
        BigDecimal firstRemittanceRate = BigDecimal.ZERO;
        if (!auditedRemittances.isEmpty()) {
            Map<String, Object> firstR = auditedRemittances.get(0);
            firstRemittanceRate = firstR.get("taxRate") != null ? (BigDecimal) firstR.get("taxRate") : BigDecimal.ZERO;
        }
        calculationSteps.add(String.format("使用汇率: 第一条水单汇率 = %s", RATE_FORMAT.format(firstRemittanceRate)));

        // 7.3 逐产品计算退税加成：商品总价 × 第一条水单汇率 × (1 + 退税率)
        BigDecimal amountWithTaxRefund = BigDecimal.ZERO;
        List<Map<String, Object>> productTaxDetails = new ArrayList<>();

        if (products.isEmpty()) {
            // 无产品时，不计算退税
            amountWithTaxRefund = totalGoodsAmount;
            calculationSteps.add(String.format("退税加成: %s CNY（无商品信息）", AMOUNT_FORMAT.format(amountWithTaxRefund)));
        } else {
            for (DeclarationProduct product : products) {
                BigDecimal productAmount = product.getAmount() != null ? product.getAmount() : BigDecimal.ZERO;
                if (productAmount.compareTo(BigDecimal.ZERO) <= 0) continue;

                // 商品总价 × 第一条水单汇率 = CNY
                BigDecimal productCny = productAmount.multiply(firstRemittanceRate).setScale(2, RoundingMode.HALF_UP);

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
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("productName", product.getProductName() != null ? product.getProductName() : product.getProductChineseName());
                detail.put("hsCode", product.getHsCode());
                detail.put("amount", productAmount);
                detail.put("exchangeRate", firstRemittanceRate);
                detail.put("cnyAmount", productCny);
                detail.put("taxRefundRate", productTaxRefundRate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
                detail.put("amountWithTaxRefund", productAmountWithTax);
                productTaxDetails.add(detail);

                calculationSteps.add(String.format("商品[%s]: %s × %s = %s CNY, 退税率%s%%: %s × (1 + %s%%) = %s CNY",
                        product.getProductName() != null ? product.getProductName() : product.getHsCode(),
                        AMOUNT_FORMAT.format(productAmount), RATE_FORMAT.format(firstRemittanceRate), AMOUNT_FORMAT.format(productCny),
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
        BigDecimal bankFeeRate = BigDecimal.ZERO;
        if (totalCny.compareTo(BigDecimal.ZERO) > 0) {
            bankFeeRate = totalFeeAmount.divide(totalCny, 6, RoundingMode.HALF_UP);
        }
        BigDecimal bankFeePercent = bankFeeRate.multiply(new BigDecimal("100"));
        calculationSteps.add(String.format("减 银行手续费: -%s CNY, 内部操作费: -%s CNY, 合计: -%s CNY (综合费率≈%s%%)",
                AMOUNT_FORMAT.format(bankFeeAmount), AMOUNT_FORMAT.format(internalBankFeeTotal),
                AMOUNT_FORMAT.format(totalFeeAmount), RATE_FORMAT.format(bankFeePercent)));

        // 10. 计算最终开票金额
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
        result.put("internalBankFee", internalBankFeeTotal);
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
}
