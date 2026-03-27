package com.declaration.flowable;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.declaration.entity.*;
import com.declaration.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;

/**
 * 申报单流程服务任务 - 处理自动业务逻辑（如生成单证、自动通知等）
 */
@Slf4j
@Component("declarationServiceTask")
@RequiredArgsConstructor
public class DeclarationServiceTask implements JavaDelegate {

    private final DeclarationFormService declarationFormService;
    private final ExcelExportService excelExportService;
    private final ContractGenerateService contractGenerateService;
    private final BankAccountConfigService bankAccountConfigService;

    @Override
    public void execute(DelegateExecution execution) {
        String businessKey = execution.getProcessInstanceBusinessKey();
        String currentActivityId = execution.getCurrentActivityId();

        log.info("执行服务任务: 活动ID={}, 业务Key={}", currentActivityId, businessKey);

        if (businessKey == null || businessKey.isEmpty()) {
            return;
        }

        try {
            Long formId = Long.valueOf(businessKey);
            
            // 必须加载完整表单，包含产品以用于生成合同和单证
            DeclarationForm form = declarationFormService.getFullDeclarationForm(formId);
            if (form == null) return;
            if("genContractTaskSmall".equals(currentActivityId)){
                log.info("正在为申报单 {} 生成预录入单.", form.getFormNo());
                generateAndSaveExport(form);

            }
            // 根据活动节点执行不同逻辑
            if ("genContractTask".equals(currentActivityId)) {
                log.info("正在为申报单 {} 生成海关申报单", form.getFormNo());
                generateAndSaveAllTempleExport(form);
//                generateContractOnApproval(form, formId);
            }
            // 新增：处理生成预录入单任务
            if ("genPreEntryTask".equals(currentActivityId)) {
                log.info("正在为申报单 {} 生成预录入单", form.getFormNo());
                generateAndSaveExport(form);
            }
        } catch (NumberFormatException e) {
            log.error("服务任务业务Key解析失败: {}", businessKey);
        }
    }
    private void generateAndSaveExport(DeclarationForm form) {
        try {
            // Generate using standard temple.xlsx
            // 注意：generateAndSaveExportDocuments 内部已经处理了保存逻辑
            excelExportService.generateAndSaveExportDocuments(form);


            log.info("申报单 {} 预录入生成完成", form.getFormNo());
        } catch (Exception e) {
            log.error("申报单 {} 预录入自动生成导出文件失败", form.getFormNo(), e);
        }
    }
    private void generateAndSaveAllTempleExport(DeclarationForm form) {
        try {
            
            // Generate using alltemple_template.xlsx
            // 注意：generateAndSaveAllTempleExportDocuments 内部已经处理了保存逻辑
            excelExportService.generateAndSaveAllTempleExportDocuments(form);
            
            log.info("申报单 {} 海关单生成完成", form.getFormNo());
        } catch (Exception e) {
            log.error("申报单 {} 海关单自动生成导出文件失败", form.getFormNo(), e);
        }
    }

    private void generateContractOnApproval(DeclarationForm form, Long formId) {
        try {
            Map<String, Object> contractData = prepareContractData(form);
            Long templateId = 1L; 
            Long generatedBy = form.getCreateBy() != null ? form.getCreateBy() : 1L;
            ContractGeneration contract = 
                contractGenerateService.generateContract(templateId, formId, contractData, generatedBy);
            
            if (contract != null) {
                log.info("审核通过时自动生成合同成功: 合同编号={}", contract.getContractNo());
            } else {
                log.warn("审核通过，但合同模板尚未配置，跳过合同生成。请在合同模板管理中上传模板后重试。");
            }
        } catch (Exception e) {
            log.warn("自动生成合同失败（不影响审核流程）: {}", e.getMessage());
        }
    }

    private Map<String, Object> prepareContractData(DeclarationForm form) {
        Map<String, Object> data = new HashMap<>();
        
        LocalDate contractDate = form.getDeclarationDate() != null 
            ? form.getDeclarationDate() : LocalDate.now();
        data.put("contractDate", contractDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        data.put("contractDateCn", contractDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
        data.put("contractDateEn", contractDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
        
        data.put("buyerName", nullSafe(form.getConsigneeCompany()));
        data.put("buyerNameCn", nullSafe(form.getConsigneeCompany()) + "公司");
        data.put("buyerAddress", nullSafe(form.getConsigneeAddress()));
        
        data.put("sellerName", nullSafe(form.getShipperCompany()));
        data.put("sellerNameCn", nullSafe(form.getShipperCompany()) + "公司");
        data.put("sellerAddress", nullSafe(form.getShipperAddress()));
        
        String productNameEn = "";
        String productNameCn = "";
        String quantity = "0";
        String quantityUnit = "个";
        if (form.getProducts() != null && !form.getProducts().isEmpty()) {
            DeclarationProduct mainProduct = form.getProducts().get(0);
            productNameEn = nullSafe(mainProduct.getProductName());
            productNameCn = nullSafe(mainProduct.getProductName());
            quantity = mainProduct.getQuantity() != null ? mainProduct.getQuantity().toString() : "0";
            quantityUnit = nullSafe(mainProduct.getUnit(), "个");
        }
        data.put("productNameEn", productNameEn);
        data.put("productNameCn", productNameCn);
        data.put("quantity", quantity);
        data.put("quantityUnit", quantityUnit);
        
        String totalAmount = form.getTotalAmount() != null ? form.getTotalAmount().toPlainString() : "0";
        String currency = nullSafe(form.getCurrency(), "USD");
        data.put("totalAmount", totalAmount);
        data.put("currency", currency);
        data.put("currencyCn", getCurrencyCnPrefix(currency));
        data.put("currencyCn2", getCurrencyCnSuffix(currency));
        data.put("paymentPercent", "100");
        data.put("paymentDays", "3");
        
        BankAccountConfig bankAccount = getDefaultBankAccount(currency);
        data.put("bankAccountName", bankAccount != null ? nullSafe(bankAccount.getAccountHolder(), nullSafe(form.getShipperCompany())) : nullSafe(form.getShipperCompany()));
        data.put("bankAccount", bankAccount != null ? nullSafe(bankAccount.getAccountNumber()) : "");
        data.put("bankName", bankAccount != null ? nullSafe(bankAccount.getBankName()) : "");
        data.put("swiftCode", bankAccount != null ? nullSafe(bankAccount.getSwiftCode()) : "");
        
        data.put("deliveryDays", "10");
        
        return data;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
    
    private String nullSafe(String value, String defaultValue) {
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
    
    private String getCurrencyCnPrefix(String currency) {
        if ("USD".equalsIgnoreCase(currency)) return "美金";
        if ("EUR".equalsIgnoreCase(currency)) return "欧元";
        if ("CNY".equalsIgnoreCase(currency) || "RMB".equalsIgnoreCase(currency)) return "人民币";
        return currency;
    }
    
    private String getCurrencyCnSuffix(String currency) {
        if ("USD".equalsIgnoreCase(currency)) return "美元";
        if ("EUR".equalsIgnoreCase(currency)) return "欧元";
        if ("CNY".equalsIgnoreCase(currency) || "RMB".equalsIgnoreCase(currency)) return "元";
        return "";
    }
    
    private BankAccountConfig getDefaultBankAccount(String currency) {
        try {
            LambdaQueryWrapper<BankAccountConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BankAccountConfig::getStatus, 1)
                   .eq(BankAccountConfig::getIsDefault, 1);
            
            if (currency != null && !currency.isEmpty()) {
                wrapper.eq(BankAccountConfig::getCurrency, currency);
            }
            
            BankAccountConfig account = bankAccountConfigService.getOne(wrapper);
            if (account == null) {
                LambdaQueryWrapper<BankAccountConfig> fallbackWrapper = new LambdaQueryWrapper<>();
                fallbackWrapper.eq(BankAccountConfig::getStatus, 1);
                if (currency != null && !currency.isEmpty()) {
                    fallbackWrapper.eq(BankAccountConfig::getCurrency, currency);
                }
                fallbackWrapper.orderByAsc(BankAccountConfig::getSort).last("LIMIT 1");
                account = bankAccountConfigService.getOne(fallbackWrapper);
            }
            return account;
        } catch (Exception e) {
            log.warn("查询默认银行账户失败", e);
            return null;
        }
    }
}
