package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.declaration.dao.ContractTemplateDao;
import com.declaration.dao.BankAccountConfigDao;
import com.declaration.entity.*;
import com.declaration.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 合同生成服务实现类
 *
 * @author Administrator
 * @since 2026-03-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractGenerateServiceImpl implements ContractGenerateService {

    private final ContractTemplateDao contractTemplateDao;
    private final ContractGenerationService contractGenerationService;
    private final ContractTemplateService contractTemplateService;
    private final DeclarationFormService declarationFormService;
    private final SystemConfigService systemConfigService;
    private final BankAccountConfigService bankAccountConfigService;
    private final BankAccountConfigDao bankAccountConfigDao;
    private final ProductTypeConfigService productTypeConfigService;

    @Value("${file.upload.contract-path:/uploads/contracts}")
    private String contractUploadPath;

    @Value("${file.upload.template-path:/uploads/templates}")
    private String templateUploadPath;

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ContractGeneration generateContract(Long templateId, Long declarationFormId, Map<String, Object> dataMap,
            Long generatedBy) {
        try {
            // 1. 获取模板信息
            ContractTemplate template = contractTemplateDao.selectById(templateId);
            if (template == null) {
                log.warn("合同模板 ID={} 不存在，跳过合同生成", templateId);
                return null;
            }

            if (template.getStatus() != 1) {
                log.warn("合同模板 ID={} 已禁用，跳过合同生成", templateId);
                return null;
            }

            // 2. 从数据库配置获取模板文件路径(优先)
            String dynamicTemplatePath = systemConfigService.getConfigValue("file.upload.template-path", templateUploadPath);
            // 3. 从数据库配置获取合同生成路径(优先)
            String dynamicContractPath = systemConfigService.getConfigValue("file.upload.contract-path", contractUploadPath);

            // 4. 获取申报单信息
            DeclarationForm declarationForm = declarationFormService.getFullDeclarationForm(declarationFormId);
            if (declarationForm == null) {
                log.warn("申报单 ID={} 不存在，跳过合同生成", declarationFormId);
                return null;
            }

            // 5. 构建模板数据(合并外部传入的数据和申报单数据)
            Map<String, Object> templateData = new HashMap<>();
            if (dataMap != null) {
                templateData.putAll(dataMap);
            }

            // 自动填充申报单核心字段
            templateData.put("formNo", declarationForm.getFormNo() != null ? declarationForm.getFormNo() : "");
            templateData.put("declarationDate", declarationForm.getDeclarationDate() != null ? declarationForm.getDeclarationDate().toString() : "");
            
            // 合同日期相关
            LocalDate contractDateObj = declarationForm.getDeclarationDate() != null ? 
                declarationForm.getDeclarationDate() : LocalDate.now();
            String contractDate = contractDateObj.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String contractDateCn = contractDateObj.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
            String contractDateEn = contractDateObj.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            
            templateData.put("contractDate", contractDate);
            templateData.put("contractDateCn", contractDateCn);
            templateData.put("contractDateEn", contractDateEn);
            
            // 公司信息
            templateData.put("shipperCompany", declarationForm.getShipperCompany() != null ? declarationForm.getShipperCompany() : "");
            templateData.put("shipperCompanyCn", declarationForm.getShipperCompany() != null ? declarationForm.getShipperCompany()+"公司" : "");
            templateData.put("shipperAddress", declarationForm.getShipperAddress() != null ? declarationForm.getShipperAddress() : "");
            templateData.put("consigneeCompany", declarationForm.getConsigneeCompany() != null ? declarationForm.getConsigneeCompany() : "");
            templateData.put("consigneeCompanyCn",declarationForm.getConsigneeCompany()!=null?declarationForm.getConsigneeCompany()+"公司" : "");
            templateData.put("consigneeAddress", declarationForm.getConsigneeAddress() != null ? declarationForm.getConsigneeAddress() : "");
            
            // 地址信息（兼容模板中的字段名）
            templateData.put("shipperCompanyAddress", declarationForm.getShipperAddress() != null ? declarationForm.getShipperAddress() : "");
            templateData.put("consigneeCompanyAddress", declarationForm.getConsigneeAddress() != null ? declarationForm.getConsigneeAddress() : "");


            templateData.put("invoiceNo", declarationForm.getInvoiceNo() != null ? declarationForm.getInvoiceNo() : "");
            
            // 产品信息
            String productNameEn = "";
            String productNameCn = "";
            String quantity = "0";
            String quantityUnit = "个";
            
            // 获取第一个产品作为主要产品信息
            if (declarationForm.getProducts() != null && !declarationForm.getProducts().isEmpty()) {
                DeclarationProduct mainProduct = declarationForm.getProducts().get(0);
                productNameEn = mainProduct.getProductName() != null ? mainProduct.getProductName() : "";
                ProductTypeConfig productTypeConfig = productTypeConfigService.getByHsCode(mainProduct.getHsCode());
                productNameCn = productTypeConfig.getChineseName() != null ? mainProduct.getProductName() : "";
                quantity = mainProduct.getQuantity() != null ? mainProduct.getQuantity().toString() : "0";
                quantityUnit =  "个";
            }
            
            templateData.put("productNameEn", productNameEn);
            templateData.put("productNameCn", productNameCn);
            templateData.put("quantity", quantity);
            templateData.put("quantityUnit", quantityUnit);
            
            // 金额信息
            String totalAmount = declarationForm.getTotalAmount() != null ? 
                declarationForm.getTotalAmount().toString() : "0";
            String currency = declarationForm.getCurrency() != null ? declarationForm.getCurrency() : "USD";
            
            templateData.put("totalAmount", totalAmount);
            templateData.put("currency", currency);
            
            // 货币中文名称
            templateData.put("currencyCn", getCurrencyChineseName(currency));
            templateData.put("currencyCn2", getCurrencyChineseSuffix(currency));
            
            // 商务条款（默认值，可通过dataMap覆盖）
            templateData.put("paymentDays", "30");  // 默认30天付款期
            templateData.put("paymentPercent", "100"); // 默认100%付款
            templateData.put("deliveryDays", "15"); // 默认15天交货期
            
            // 银行账户信息
            BankAccountConfig bankAccount = getDefaultBankAccount(currency);
            templateData.put("sellerName", bankAccount != null ? 
                (bankAccount.getAccountHolder() != null ? bankAccount.getAccountHolder() : 
                 (declarationForm.getShipperCompany() != null ? declarationForm.getShipperCompany() : "")) : 
                (declarationForm.getShipperCompany() != null ? declarationForm.getShipperCompany() : ""));
            templateData.put("bankAccount", bankAccount != null ? 
                (bankAccount.getAccountNumber() != null ? bankAccount.getAccountNumber() : "") : "");
            templateData.put("bankName", bankAccount != null ? 
                (bankAccount.getBankName() != null ? bankAccount.getBankName() : "") : "");
            templateData.put("swiftCode", bankAccount != null ? 
                (bankAccount.getSwiftCode() != null ? bankAccount.getSwiftCode() : "") : "");
            
            // 生成日期
            templateData.put("generatedDate", LocalDate.now().toString());

            // 4. 生成合同编号
            String contractNo = generateContractNumber(template.getTemplateType());

            // 5. 构造模板文件路径(使用完整配置路径,不再拼接用户目录)
            Path templatePath = Paths.get(dynamicTemplatePath, template.getFileName());
            if (!Files.exists(templatePath)) {
                log.warn("模板文件不存在: {}，跳过合同生成", templatePath);
                return null;
            }

            // 6. 生成输出文件路径(使用完整配置路径,不再拼接用户目录)
            // 创建以申报号命名的子目录
            String formNoDir = declarationForm.getFormNo();
            Path formDirPath = Paths.get(dynamicContractPath, formNoDir);
            String fileName = contractNo + "_" + System.currentTimeMillis() + ".docx";
            Path outputPath = Paths.get(formDirPath.toString(), fileName);

            // 确保输出目录存在
            Files.createDirectories(outputPath.getParent());

            // 7. 使用POI-TL生成合同文档（使用填充后的templateData）
            XWPFTemplate doc = XWPFTemplate.compile(templatePath.toFile()).render(templateData);
            doc.writeAndClose(new FileOutputStream(outputPath.toFile()));

            // 8. 获取文件大小
            long fileSize = Files.size(outputPath);

            // 9. 保存生成记录
            ContractGeneration generation = new ContractGeneration();
            generation.setContractNo(contractNo);
            generation.setDeclarationFormId(declarationFormId);
            generation.setTemplateId(templateId);
            generation.setGeneratedFileName(fileName);
            // 保存相对路径: 申报号/文件名
            generation.setGeneratedFilePath(formNoDir + "/" + fileName);
            generation.setFileSize(fileSize);
            generation.setGeneratedBy(generatedBy);
            generation.setGeneratedTime(LocalDateTime.now());
            generation.setStatus(1);

            contractGenerationService.save(generation);

            log.info("合同生成成功: 合同编号={}, 模板={}, 申报单={}", contractNo, template.getTemplateName(), declarationFormId);
            return generation;

        } catch (Exception e) {
            log.warn("合同生成失败（不影响主流程）: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRED, rollbackFor = Exception.class)
    public String uploadTemplateFile(MultipartFile file, Long templateId) {
        String savedFilePath = null;
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("上传文件不能为空");
            }

            // 检查文件类型
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".docx")) {
                throw new RuntimeException("只支持.docx格式的Word文档");
            }

            // 生成唯一文件名
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString().replaceAll("-", "") + extension;

            // 从数据库配置获取模板文件路径(优先)
            String dynamicTemplatePath = systemConfigService.getConfigValue("file.upload.template-path", templateUploadPath);

            // 构造保存路径(使用完整配置路径,不再拼接用户目录)
            Path savePath = Paths.get(dynamicTemplatePath, fileName);
            Files.createDirectories(savePath.getParent());

            // 保存文件
            file.transferTo(savePath);
            savedFilePath = savePath.toString();
            
            // 更新模板的文件信息
            ContractTemplate template = contractTemplateDao.selectById(templateId);
            if (template != null) {
                template.setFileName(fileName);
                template.setFileSize(file.getSize());
                template.setFilePath(savedFilePath);
                contractTemplateService.updateById(template);
            }

            log.info("模板文件上传成功: {}, 大小: {} bytes", fileName, file.getSize());
            return savedFilePath;

        } catch (Exception e) {
            log.error("模板文件上传失败", e);
            // 发生异常时回滚:删除已上传的文件
            if (savedFilePath != null) {
                try {
                    Path filePath = Paths.get(savedFilePath);
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                        log.info("上传失败,已回滚并删除文件: {}", savedFilePath);
                    }
                } catch (Exception deleteException) {
                    log.warn("回滚时删除文件失败: {}", deleteException.getMessage());
                }
            }
            throw new RuntimeException("模板文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public File getContractFile(Long contractId) {
        ContractGeneration generation = contractGenerationService.getById(contractId);
        if (generation == null) {
            throw new RuntimeException("合同记录不存在");
        }

        // 从数据库配置获取合同生成路径
        String dynamicContractPath = systemConfigService.getConfigValue("file.upload.contract-path", contractUploadPath);
        
        // 构造完整文件路径 (合同路径 + 申报号目录 + 文件名)
        Path fullPath = Paths.get(dynamicContractPath, generation.getGeneratedFilePath());
        System.out.println(fullPath);
        File file = fullPath.toFile();
        
        if (!file.exists()) {
            throw new RuntimeException("合同文件不存在: " + file.getAbsolutePath());
        }

        return file;
    }

    @Override
    public String generateContractNumber(String templateType) {
        try {
            // 简化的编号生成逻辑
            String prefix = "EC"; // 默认出口合同前缀
            if ("IMPORT".equals(templateType)) {
                prefix = "IC"; // 进口合同前缀
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String dateStr = sdf.format(new Date());

            // 这里应该是从数据库获取序列号，简化处理
            String serial = String.format("%06d", System.currentTimeMillis() % 1000000);

            return prefix + dateStr + serial;

        } catch (Exception e) {
            log.error("合同编号生成失败", e);
            throw new RuntimeException("合同编号生成失败");
        }
    }
    
    /**
     * 获取货币中文名称
     */
    private String getCurrencyChineseName(String currencyCode) {
        switch (currencyCode.toUpperCase()) {
            case "USD": return "美元";
            case "EUR": return "欧元";
            case "GBP": return "英镑";
            case "JPY": return "日元";
            case "CNY": 
            case "RMB": return "人民币";
            default: return currencyCode;
        }
    }
    
    /**
     * 获取货币中文后缀
     */
    private String getCurrencyChineseSuffix(String currencyCode) {
        switch (currencyCode.toUpperCase()) {
            case "USD": return "圆整";
            case "EUR": return "整";
            case "GBP": return "整";
            case "JPY": return "整";
            case "CNY": 
            case "RMB": return "整";
            default: return "整";
        }
    }
    
    /**
     * 获取默认银行账户信息
     */
    private BankAccountConfig getDefaultBankAccount(String currency) {
        try {
            LambdaQueryWrapper<BankAccountConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BankAccountConfig::getStatus, 1)
                   .eq(BankAccountConfig::getIsDefault, 1);
            
            if (currency != null && !currency.isEmpty()) {
                wrapper.eq(BankAccountConfig::getCurrency, currency.toUpperCase());
            }
            
            return bankAccountConfigService.getOne(wrapper);
        } catch (Exception e) {
            log.warn("获取默认银行账户失败: {}", e.getMessage());
            return null;
        }
    }
}