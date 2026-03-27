package com.declaration.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.declaration.entity.CountryInfo;
import com.declaration.entity.DeclarationAttachment;
import com.declaration.entity.DeclarationCarton;
import com.declaration.entity.DeclarationCartonProduct;
import com.declaration.entity.DeclarationForm;
import com.declaration.entity.DeclarationProduct;
import com.declaration.entity.ExportDataRequest;
import com.declaration.entity.ProductTypeConfig;
import com.declaration.service.CountryInfoService;
import com.declaration.service.DeclarationAttachmentService;
import com.declaration.service.ExcelExportService;
import com.declaration.service.ProductTypeConfigService;
import com.declaration.service.SystemConfigService;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.apache.fesod.sheet.write.metadata.fill.FillConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.fesod.sheet.write.merge.OnceAbsoluteMergeStrategy;

/**
 * Excel导出服务实现类
 */
@Slf4j
@Service
public class ExcelExportServiceImpl implements ExcelExportService {

    // 模板文件名常量
    private static final String TEMPLATE_INVOICE = "temple.xlsx";
    private static final String TEMPLATE_REMITTANCE = "remittance_template.xlsx";
    private static final String TEMPLATE_ALLTEMPLE = "alltemple_template.xlsx";

    @Value("${file.upload-path:uploads/exports/}")
    private String uploadPath;

    @Autowired
    private DeclarationAttachmentService attachmentService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private CountryInfoService countryInfoService;

    @Autowired
    private ProductTypeConfigService productTypeConfigService;

    @Override
    public DeclarationAttachment generateAndSaveExportDocuments(DeclarationForm form) throws IOException {
        String templatePath = getTemplatePath();
        if (templatePath == null) {
            throw new RuntimeException("模板文件不存在");
        }

        // 创建目录
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 创建以申报号命名的子目录
        String formNoDir = form.getFormNo();
        File formDir = new File(uploadPath, formNoDir);
        if (!formDir.exists()) {
            formDir.mkdirs();
        }
        
        String fileName = "预录入表单_" + form.getFormNo() + "_" + IdUtil.simpleUUID() + ".xlsx";
        String filePath = Paths.get(formDir.getAbsolutePath(), fileName).toString();
        File targetFile = new File(filePath);

        // 准备数据
        Map<String, Object> fillData = prepareFillData(form);
        List<ExportDataRequest.ProductInfo> productList = createProductListData(form);
        Map<String, Object> packingData = preparePackingListData(form);

        // 计算装箱单的合并策略
        // temple.xlsx: 装箱单在Sheet 1（index=1），数据从第9行开始（0-indexed row=8）
        // 箱数列PKGS在C4(index=3)，体积列MEAS在C10(index=9)
        List<OnceAbsoluteMergeStrategy> mergeStrategies = calculateCartonMergeStrategies(productList, 8, 3, 9);
        
        // 使用 Fesod 填充并保存到文件，并注册合并策略
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            org.apache.fesod.sheet.write.builder.ExcelWriterBuilder writerBuilder = 
                FesodSheet.write(fos).withTemplate(templatePath);
            
            // 注册合并策略（只对装箱单 Sheet 1 生效）
            for (OnceAbsoluteMergeStrategy strategy : mergeStrategies) {
                writerBuilder.registerWriteHandler(strategy);
            }
            
            try (ExcelWriter writer = writerBuilder.build()) {
                // 第一个工作表：商业发票
                WriteSheet invoiceSheet = FesodSheet.writerSheet(0).build();
                FillConfig invoiceConfig = FillConfig.builder().forceNewRow(false).build();
                writer.fill(fillData, invoiceSheet);
                writer.fill(productList, invoiceConfig, invoiceSheet);
                
                // 第二个工作表：装箱单
                WriteSheet packingSheet = FesodSheet.writerSheet(1).build();
                FillConfig packingConfig = FillConfig.builder().forceNewRow(false).build();
                writer.fill(packingData, packingSheet);
                writer.fill(productList, packingConfig, packingSheet);
                
                writer.finish();
            }
        }

        // 构建附件对象
        DeclarationAttachment attachment = new DeclarationAttachment();
        attachment.setFormId(form.getId());
        attachment.setFileName("预录入表单_" + form.getFormNo() + ".xlsx");
        attachment.setFileUrl("/api/v1/files/download?path=" + formNoDir + "/" + fileName);
        attachment.setFileType("FullDocuments");
        attachment.setCreateTime(LocalDateTime.now());
        
        // 查询是否存在同类型旧记录，实现替换而非新增
        LambdaQueryWrapper<DeclarationAttachment> query = new LambdaQueryWrapper<>();
        query.eq(DeclarationAttachment::getFormId, form.getId())
             .eq(DeclarationAttachment::getFileType, "FullDocuments");
        DeclarationAttachment oldAttachment = attachmentService.getOne(query);
        
        if (oldAttachment != null) {
            // 替换：更新旧记录
            oldAttachment.setFileName(attachment.getFileName());
            oldAttachment.setFileUrl(attachment.getFileUrl());
            oldAttachment.setCreateTime(LocalDateTime.now());
            attachmentService.updateById(oldAttachment);
            return oldAttachment;
        } else {
            // 新增
            attachmentService.save(attachment);
            return attachment;
        }
    }

    /**
     * 通用模板路径解析（三层回退机制）
     * 1. 优先从 SystemConfigService 获取数据库配置路径
     * 2. 其次尝试 ClassPath 资源加载
     * 3. 最后 fallback 到本地文件系统路径
     *
     * @param templateFileName 模板文件名
     * @return 模板文件绝对路径，未找到返回 null
     */
    private String resolveTemplatePath(String templateFileName) {
        // 第一层：从数据库配置获取模板目录路径
        try {
            String configuredDir = systemConfigService.getConfigValue("file.upload.template-path", "/uploads/templates");
            File configTemplateFile = new File(configuredDir, templateFileName);
            if (configTemplateFile.exists()) {
                log.info("模板文件从配置路径加载: {}", configTemplateFile.getAbsolutePath());
                return configTemplateFile.getAbsolutePath();
            }
        } catch (Exception e) {
            log.warn("从配置获取模板路径失败，尝试ClassPath加载: {}", e.getMessage());
        }

        // 第二层：ClassPath 资源加载
        try {
            ClassPathResource resource = new ClassPathResource("templates/" + templateFileName);
            if (resource.exists()) {
                String path = resource.getFile().getAbsolutePath();
                log.info("模板文件从ClassPath加载: {}", path);
                return path;
            }
        } catch (IOException e) {
            log.warn("从ClassPath获取模板路径失败，尝试本地文件加载: {}", e.getMessage());
        }

        // 第三层：本地文件系统路径
        File templateFile = new File("templates/" + templateFileName);
        if (templateFile.exists()) {
            log.info("模板文件从本地文件加载: {}", templateFile.getAbsolutePath());
            return templateFile.getAbsolutePath();
        }

        log.error("模板文件未找到: {}", templateFileName);
        return null;
    }

    private String getTemplatePath() {
        return resolveTemplatePath(TEMPLATE_INVOICE);
    }

    @Override
    public String getInvoiceTemplatePath() {
        return resolveTemplatePath(TEMPLATE_INVOICE);
    }

    private Map<String, Object> prepareFillData(DeclarationForm form) {
        Map<String, Object> fillData = new HashMap<>();
        fillData.put("invoiceNo", form.getInvoiceNo());
        // 格式化日期为英文格式 "December. 29, 2025"
                if (form.getDeclarationDate() != null) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM. dd, yyyy", Locale.ENGLISH);
                    fillData.put("date", form.getDeclarationDate().format(formatter));
                } else {
                    fillData.put("date", "");
                }
        fillData.put("shipperCompany", form.getShipperCompany());
        fillData.put("shipperAddress", form.getShipperAddress());
        fillData.put("consigneeCompany", form.getConsigneeCompany());
        fillData.put("consigneeAddress", form.getConsigneeAddress());
        fillData.put("currency", form.getCurrency());
        fillData.put("transportMode", form.getTransportMode());
        fillData.put("departureCity", form.getDepartureCity());
        fillData.put("destinationRegion", form.getDestinationCountry());
        fillData.put("totalAmount", form.getTotalAmount());
        // 总金额英文大写
        if (form.getTotalAmount() != null) {
            fillData.put("totalAmountWords", convertAmountToWords(form.getTotalAmount().doubleValue()));
        }
        
        // 从关联数据计算统计字段
        fillData.put("totalCartons", calculateTotalCartons(form));
        fillData.put("totalGrossWeight", calculateTotalGrossWeight(form));
        fillData.put("totalNetWeight", calculateTotalNetWeight(form));
        fillData.put("totalVolume", calculateTotalVolume(form));
        fillData.put("totalQuantity", calculateTotalQuantity(form));
        fillData.put("contonEN", getCaronsType(form).get("contonEN"));
        fillData.put("contonCH", getCaronsType(form).get("contonCH"));
        fillData.put("departureCityEnglish",form.getDepartureCityEnglish());
//        fillData.put("contonEN",)
        return fillData;
    }
    
    /**
     * 计算总箱数 - 优先从箱子列表累加，否则使用表单字段
     */
    private Integer calculateTotalCartons(DeclarationForm form) {
        if (form.getCartons() != null && !form.getCartons().isEmpty()) {
            int total = 0;
            for (DeclarationCarton carton : form.getCartons()) {
                total += carton.getQuantity() != null ? carton.getQuantity() : 1;
            }
            return total;
        }
        return form.getTotalCartons() != null ? form.getTotalCartons() : 0;
    }
    
    /**
     * 计算总毛重 - 优先从产品明细累加，否则使用表单字段
     */
    private java.math.BigDecimal calculateTotalGrossWeight(DeclarationForm form) {
        if (form.getProducts() != null && !form.getProducts().isEmpty()) {
            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            for (DeclarationProduct product : form.getProducts()) {
                if (product.getGrossWeight() != null) {
                    total = total.add(product.getGrossWeight());
                }
            }
            if (total.compareTo(java.math.BigDecimal.ZERO) > 0) {
                return total;
            }
        }
        return form.getTotalGrossWeight();
    }
    
    /**
     * 计算总净重 - 优先从产品明细累加，否则使用表单字段
     */
    private java.math.BigDecimal calculateTotalNetWeight(DeclarationForm form) {
        if (form.getProducts() != null && !form.getProducts().isEmpty()) {
            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            for (DeclarationProduct product : form.getProducts()) {
                if (product.getNetWeight() != null) {
                    total = total.add(product.getNetWeight());
                }
            }
            if (total.compareTo(java.math.BigDecimal.ZERO) > 0) {
                return total;
            }
        }
        return form.getTotalNetWeight();
    }
    
    /**
     * 计算总体积 - 优先从箱子列表累加，否则从产品累加，最后使用表单字段
     */
    private java.math.BigDecimal calculateTotalVolume(DeclarationForm form) {
        // 优先从箱子列表累加
        if (form.getCartons() != null && !form.getCartons().isEmpty()) {
            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            for (DeclarationCarton carton : form.getCartons()) {
                if (carton.getVolume() != null) {
                    total = total.add(carton.getVolume());
                }
            }
                return total;

        }

        return form.getTotalVolume();
    }
    
    /**
     * 计算总数量 - 优先从产品明细累加，否则使用表单字段
     */
    private Integer calculateTotalQuantity(DeclarationForm form) {
        if (form.getProducts() != null && !form.getProducts().isEmpty()) {
            int total = 0;
            for (DeclarationProduct product : form.getProducts()) {
                if (product.getQuantity() != null) {
                    total += product.getQuantity();
                }
            }
            if (total > 0) {
                return total;
            }
        }
        return form.getTotalQuantity() != null ? form.getTotalQuantity() : 0;
    }
   private Map<String,String> getCaronsType(DeclarationForm form){
// 优先从箱子列表累加
       if (form.getCartons() != null && !form.getCartons().isEmpty()) {
           DeclarationCarton carton = form.getCartons().get(0);
           String contonCH = carton.getTypeChinese();
           String contonEN = carton.getTypeEnglish();
           HashMap<String,String> map = new HashMap<>();
           map.put("contonCH", contonCH);
           map.put("contonEN", contonEN);
           return map;
       }
       HashMap<String, String> map2 = new HashMap<String, String>(2);
       map2.put("contonEN", "CARTONS");
       map2.put("contonCH", "纸箱");
//        return "CARTONS";
       return map2;
   }

    private List<ExportDataRequest.ProductInfo> createProductListData(DeclarationForm form) {
        List<ExportDataRequest.ProductInfo> productList = new ArrayList<>();
        
        // 建立产品ID到箱子ID的映射
        Map<Long, Long> productToCartonMap = new HashMap<>();
        if (form.getCartonProducts() != null) {
            for (DeclarationCartonProduct cp : form.getCartonProducts()) {
                productToCartonMap.put(cp.getProductId(), cp.getCartonId());
            }
        }
        
        // 建立箱子ID到箱子信息的映射
        Map<Long, DeclarationCarton> cartonMap = new HashMap<>();
        if (form.getCartons() != null) {
            for (DeclarationCarton c : form.getCartons()) {
                cartonMap.put(c.getId(), c);
            }
        }
        
        if (form.getProducts() != null) {
            form.getProducts().forEach(p -> {
                ExportDataRequest.ProductInfo info = new ExportDataRequest.ProductInfo();
                info.setProductName(p.getProductEnglishName());
                info.setHsCode(p.getHsCode());
                info.setQuantity(p.getQuantity());
                info.setUnit(p.getUnit());
                info.setUnitPrice(p.getUnitPrice());
                info.setAmount(p.getAmount() != null ? p.getAmount().toString() : "0.00");
                info.setGrossWeight(p.getGrossWeight());
                info.setNetWeight(p.getNetWeight());
                System.out.println("获取当前产品体积:"+p.getVolume());
                info.setVolume(p.getVolume());
                info.setCurrency(form.getCurrency());
                
                // 设置产品箱数
                info.setCartons(p.getCartons());
                // 重量单位
                info.setWgt("KGS");
                
                // 获取关联的箱子信息
                Long cartonId = productToCartonMap.get(p.getId());
                if (cartonId != null) {
                    DeclarationCarton carton = cartonMap.get(cartonId);
                    if (carton != null) {
                        info.setCartonNo(carton.getCartonNo());
                        info.setCartonQuantity(carton.getQuantity());
                        info.setCartonVolume(carton.getVolume());
                        info.setCartonType(carton.getTypeEnglish());
                    }
                }
                
                // 设置申报要素
                if (p.getElementValues() != null) {
                    List<Map<String, Object>> elements = new ArrayList<>();
                    p.getElementValues().forEach(ev -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", ev.getElementName());
                        map.put("value", ev.getElementValue());
                        elements.add(map);
                    });
                    info.setDeclarationElements(elements);
                }
                
                productList.add(info);
            });
        }
        return productList;
    }

    private Map<String, Object> preparePackingListData(DeclarationForm form) {
        return prepareFillData(form);
    }
    
    /**
     * 获取水单模板路径
     */
    private String getRemittanceTemplatePath() {
        return resolveTemplatePath(TEMPLATE_REMITTANCE);
    }
    
    /**
     * 准备水单填充数据
     */
    private Map<String, Object> prepareRemittanceData(com.declaration.entity.DeclarationRemittance remittance, DeclarationForm form) {
        Map<String, Object> data = new HashMap<>();
        
        // 水单基本信息
        data.put("remittanceType", remittance.getRemittanceType() == 1 ? "定金" : "尾款");
        data.put("remittanceName", remittance.getRemittanceName());
        // 格式化水单日期为英文格式
        if (remittance.getRemittanceDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM. dd, yyyy", Locale.ENGLISH);
            data.put("remittanceDate", remittance.getRemittanceDate().format(formatter));
        } else {
            data.put("remittanceDate", "");
        }
        data.put("remittanceAmount", remittance.getRemittanceAmount());
        data.put("exchangeRate", remittance.getExchangeRate());
        data.put("bankFee", remittance.getBankFee());
        data.put("creditedAmount", remittance.getCreditedAmount());
        data.put("remarks", remittance.getRemarks() != null ? remittance.getRemarks() : "");
        
        // 申报单信息
        data.put("formNo", form.getFormNo());
        data.put("invoiceNo", form.getInvoiceNo());
        // 格式化申报日期为英文格式
        if (form.getDeclarationDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM. dd, yyyy", Locale.ENGLISH);
            data.put("declarationDate", form.getDeclarationDate().format(formatter));
        } else {
            data.put("declarationDate", "");
        }
        
        // 发货人信息
        data.put("shipperCompany", form.getShipperCompany());
        data.put("shipperAddress", form.getShipperAddress());
        
        // 收货人信息
        data.put("consigneeCompany", form.getConsigneeCompany());
        data.put("consigneeAddress", form.getConsigneeAddress());
        
        // 金额转英文大写
        if (remittance.getRemittanceAmount() != null) {
            String amountInWords = convertAmountToWords(remittance.getRemittanceAmount().doubleValue());
            data.put("amountInWords", amountInWords);
        }
        
        return data;
    }
    
    /**
     * 金额转英文大写
     */
    private String convertAmountToWords(double amount) {
        String[] ones = {"", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE"};
        String[] teens = {"TEN", "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN", "SEVENTEEN", "EIGHTEEN", "NINETEEN"};
        String[] tens = {"", "", "TWENTY", "THIRTY", "FORTY", "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY"};
        String[] thousands = {"", "THOUSAND", "MILLION", "BILLION"};
        
        if (amount == 0) return "ZERO USD ONLY";
        
        long wholePart = (long) amount;
        int decimalPart = (int) Math.round((amount - wholePart) * 100);
        
        StringBuilder result = new StringBuilder("SAY ");
        
        if (wholePart > 0) {
            String wholeWords = convertWholeNumber(wholePart, ones, teens, tens, thousands);
            result.append(wholeWords);
        }
        
        if (decimalPart > 0) {
            if (wholePart > 0) result.append(" AND ");
            String decimalWords = convertWholeNumber(decimalPart, ones, teens, tens, thousands);
            result.append(decimalWords).append(" CENTS");
        } else if (wholePart == 0) {
            result.append("ZERO");
        }
        
        result.append(" USD ONLY");
        return result.toString();
    }
    
    /**
     * 转换整数部分
     */
    private String convertWholeNumber(long number, String[] ones, String[] teens, String[] tens, String[] thousands) {
        if (number == 0) return "ZERO";
        
        StringBuilder result = new StringBuilder();
        int thousandIndex = 0;
        
        while (number > 0) {
            long chunk = number % 1000;
            if (chunk != 0) {
                String chunkWords = convertThreeDigits((int) chunk, ones, teens, tens);
                if (thousandIndex > 0) {
                    chunkWords += " " + thousands[thousandIndex];
                }
                if (result.length() > 0) {
                    result.insert(0, chunkWords + " ");
                } else {
                    result.append(chunkWords);
                }
            }
            number /= 1000;
            thousandIndex++;
        }
        
        return result.toString().trim();
    }
    
    /**
     * 转换三位数
     */
    private String convertThreeDigits(int number, String[] ones, String[] teens, String[] tens) {
        StringBuilder result = new StringBuilder();
        
        int hundreds = number / 100;
        int remainder = number % 100;
        
        if (hundreds > 0) {
            result.append(ones[hundreds]).append(" HUNDRED");
            if (remainder > 0) {
                result.append(" ");
            }
        }
        
        if (remainder > 0) {
            if (remainder < 10) {
                result.append(ones[remainder]);
            } else if (remainder < 20) {
                result.append(teens[remainder - 10]);
            } else {
                int tensDigit = remainder / 10;
                int onesDigit = remainder % 10;
                result.append(tens[tensDigit]);
                if (onesDigit > 0) {
                    result.append(" ").append(ones[onesDigit]);
                }
            }
        }
        
        return result.toString();
    }
    @Override
    public DeclarationAttachment generateAndSaveRemittanceReport(com.declaration.entity.DeclarationRemittance remittance, DeclarationForm form) throws IOException {
        String templatePath = getRemittanceTemplatePath();
        if (templatePath == null) {
            throw new RuntimeException("水单模板文件不存在");
        }

        // 创建目录
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 创建以申报号命名的子目录
        String formNoDir = form.getFormNo();
        File formDir = new File(uploadPath, formNoDir);
        if (!formDir.exists()) {
            formDir.mkdirs();
        }

        String typeName = remittance.getRemittanceType() == 1 ? "定金" : "尾款";
        String fileName = "Remittance_" + typeName + "_" + form.getFormNo() + "_" + IdUtil.simpleUUID() + ".xlsx";
        String filePath = Paths.get(formDir.getAbsolutePath(), fileName).toString();
        File targetFile = new File(filePath);

        // 准备水单数据
        Map<String, Object> remittanceData = prepareRemittanceData(remittance, form);

        // 使用Fesod模板填充生成Excel
        try (FileOutputStream fos = new FileOutputStream(targetFile);
             ExcelWriter writer = FesodSheet.write(fos).withTemplate(templatePath).build()) {
            
            WriteSheet sheet = FesodSheet.writerSheet(0).build();
            writer.fill(remittanceData, sheet);
            writer.finish();
        }
        
        // 构建附件对象
        String fileType = "Remittance_" + (remittance.getRemittanceType() == 1 ? "Deposit" : "Balance");
        DeclarationAttachment attachment = new DeclarationAttachment();
        attachment.setFormId(form.getId());
        attachment.setFileName(typeName + "水单_" + form.getFormNo() + ".xlsx");
        attachment.setFileUrl("/api/v1/files/download?path=" + formNoDir + "/" + fileName);
        attachment.setFileType(fileType);
        attachment.setCreateTime(LocalDateTime.now());
        
        // 查询是否存在同类型旧记录，实现替换而非新增
        LambdaQueryWrapper<DeclarationAttachment> query = new LambdaQueryWrapper<>();
        query.eq(DeclarationAttachment::getFormId, form.getId())
             .eq(DeclarationAttachment::getFileType, fileType);
        DeclarationAttachment oldAttachment = attachmentService.getOne(query);
        
        if (oldAttachment != null) {
            // 替换：更新旧记录
            oldAttachment.setFileName(attachment.getFileName());
            oldAttachment.setFileUrl(attachment.getFileUrl());
            oldAttachment.setCreateTime(LocalDateTime.now());
            attachmentService.updateById(oldAttachment);
            return oldAttachment;
        } else {
            // 新增
            attachmentService.save(attachment);
            return attachment;
        }
    }

    @Override
    public DeclarationAttachment generateAndSaveAllTempleExportDocuments(DeclarationForm form) throws IOException {
        String templatePath = getAllTempleTemplatePath();
        if (templatePath == null) {
            throw new RuntimeException("模板文件不存在: alltemple_template.xlsx");
        }

        File dir = new File(uploadPath);
        if (!dir.exists()) dir.mkdirs();
        
        // 创建以申报号命名的子目录
        String formNoDir = form.getFormNo();
        File formDir = new File(uploadPath, formNoDir);
        if (!formDir.exists()) {
            formDir.mkdirs();
        }

        String fileName = "AllTemple_" + form.getFormNo() + "_" + IdUtil.simpleUUID() + ".xlsx";
        String filePath = Paths.get(formDir.getAbsolutePath(), fileName).toString();
        File targetFile = new File(filePath);

        Map<String, Object> fillData = prepareAllTempleFillData(form);
        System.out.println("数据:"+JSONObject.from(fillData).toJSONString());
        // Fesod uses list wrapper map for items list filling
        List<Map<String, Object>> productListData = createAllTempleProductListData(form);
        System.out.println(productListData.get(0));
//        System.out.println(JSON.parseArray(productListData.toString()));
        // Map<String, Object> listWrap = new HashMap<>();
        // listWrap.put("items", productListData);
        
        // 计算装箱单的合并策略
        // alltemple_template.xlsx: 装箱单在Sheet 3（index=3），数据从第9行开始（0-indexed row=8）
        // 箱数列PKGS在C4(index=3)，体积列MEAS在C10(index=9)
        List<OnceAbsoluteMergeStrategy> mergeStrategies = calculateCartonMergeStrategiesForAllTemple(productListData, 8, 3, 9);

        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            org.apache.fesod.sheet.write.builder.ExcelWriterBuilder writerBuilder = 
                FesodSheet.write(fos).withTemplate(templatePath);
            
            // 注册合并策略
            for (OnceAbsoluteMergeStrategy strategy : mergeStrategies) {
                writerBuilder.registerWriteHandler(strategy);
            }
            
            try (ExcelWriter writer = writerBuilder.build()) {
                // We have 4 sheets: 报关单、申报要素、发票、装箱单
                for (int i = 0; i < 4; i++) {
                    WriteSheet sheet = FesodSheet.writerSheet(i).build();
                    FillConfig fillConfig = FillConfig.builder().forceNewRow(false).build();
                    writer.fill(fillData, sheet);
                    // Fill list data for {items.xx} placeholders
                    writer.fill(productListData, fillConfig, sheet);
                }
                writer.finish();
            }
        }

        DeclarationAttachment attachment = new DeclarationAttachment();
        attachment.setFormId(form.getId());
        attachment.setFileName("报关表单_" + form.getFormNo() + ".xlsx");
        attachment.setFileUrl("/api/v1/files/download?path=" + formNoDir + "/" + fileName);
        attachment.setFileType("AllDocuments");
        attachment.setCreateTime(LocalDateTime.now());
        
        // 查询是否存在同类型旧记录，实现替换而非新增
        LambdaQueryWrapper<DeclarationAttachment> query = new LambdaQueryWrapper<>();
        query.eq(DeclarationAttachment::getFormId, form.getId())
             .eq(DeclarationAttachment::getFileType, "AllDocuments");
        DeclarationAttachment oldAttachment = attachmentService.getOne(query);
        
        if (oldAttachment != null) {
            // 替换：更新旧记录
            oldAttachment.setFileName(attachment.getFileName());
            oldAttachment.setFileUrl(attachment.getFileUrl());
            oldAttachment.setCreateTime(LocalDateTime.now());
            attachmentService.updateById(oldAttachment);
            return oldAttachment;
        } else {
            // 新增
            attachmentService.save(attachment);
            return attachment;
        }
    }

    private String getAllTempleTemplatePath() {
        return resolveTemplatePath(TEMPLATE_ALLTEMPLE);
    }

    private Map<String, Object> prepareAllTempleFillData(DeclarationForm form) {
        Map<String, Object> data = new HashMap<>();
        data.put("preEntryNo", "");
        data.put("shipperCompany", form.getShipperCompany());
        data.put("exportCustoms", form.getDepartureCity());
        // 格式化导出日期和申报日期为英文格式

        data.put("exportDate", "");
        data.put("declarationDate", "");

        data.put("recordNo", "");
        data.put("consigneeCompany", form.getConsigneeCompany());
        data.put("transportNameAndVoyage", "");
        data.put("billOfLadingNo", "");
        data.put("manufacturer", form.getShipperCompany());
        data.put("supervisionMode", "0110");
        data.put("exemptionNature", "一般征税");
        data.put("licenseNo", "");
        data.put("contractNo", form.getInvoiceNo());
        
        // 国家信息处理（安全方式）
        String destinationCountryDisplay = form.getDestinationCountry();
        try {
            CountryInfo tradeCountry = countryInfoService.getCountryInfoByEnglishName(form.getDestinationCountry());
            if (tradeCountry != null) {
                destinationCountryDisplay = tradeCountry.getChineseName();
            }
        } catch (Exception e) {
            log.warn("获取国家信息失败，使用原始值: {}", form.getDestinationCountry());
        }
        String tradeCountryDisplay = form.getTradeCountry();
        try {
            CountryInfo countryInfo = countryInfoService.getCountryInfoByEnglishName(form.getTradeCountry());
            if (countryInfo!=null){
                tradeCountryDisplay = countryInfo.getChineseName();
            }
        }catch (Exception e){
            log.warn("获取贸易国信息失败");
        }
        
        data.put("tradeCountry", tradeCountryDisplay);
        data.put("destinationCountry", destinationCountryDisplay);
        data.put("portOfDestination", destinationCountryDisplay);
        data.put("portOfDeparture", "");
        data.put("packageType", getCaronsType(form).get("contonCH"));
        data.put("contonEN", getCaronsType(form).get("contonEN"));
        data.put("totalCartons", calculateTotalCartons(form));
        data.put("totalGrossWeight", calculateTotalGrossWeight(form));
        data.put("totalNetWeight", calculateTotalNetWeight(form));
        data.put("totalQuantity", calculateTotalQuantity(form));
        data.put("totalVolume", calculateTotalVolume(form));
        data.put("totalAmount", form.getTotalAmount());
        data.put("tradeTerm", "FOB");
        data.put("freight", "");
        data.put("premium", "");
        data.put("miscFee", "");
        data.put("attachment1", "合同");
        data.put("attachment2", "发票，装箱单");
        data.put("marksAndRemarks", "N/M");
        data.put("currency", form.getCurrency());
        data.put("CompanyName", form.getShipperCompany());
        data.put("shipperAddress",form.getShipperAddress());
        data.put("CompanyAddress", form.getShipperAddress());
        data.put("consigneeAddress", form.getConsigneeAddress());
        data.put("invoiceNo", form.getInvoiceNo());
        data.put("packingListNo", form.getInvoiceNo());
//        data.put(destinationCountryDisplay, tradeCountryDisplay);
        // 格式化发票日期和装箱单日期为英文格式
        if (form.getDeclarationDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM. dd, yyyy", Locale.ENGLISH);
            String formattedDate = form.getDeclarationDate().format(formatter);
            data.put("invoiceDate", formattedDate);
            data.put("packingListDate", formattedDate);
        } else {
            data.put("invoiceDate", "");
            data.put("packingListDate", "");
        }
        data.put("transportMode", (form.getTransportMode() != null ? form.getTransportMode() : "TRUCK"));
        data.put("paymentTerms", "T/T");
        data.put("portOfDepartureEng", form.getDepartureCity());
        data.put("destinationRegion",form.getDestinationCountry());
        data.put("departureCityEnglish", form.getDepartureCityEnglish());
//        data.put("destinationCountry", form.getDestinationCountry());
//        data.put("packageType", "CARTONS");
        
        if (form.getTotalAmount() != null) {
            data.put("totalAmountWords", convertAmountToWords(form.getTotalAmount().doubleValue()));
        } else {
            data.put("totalAmountWords", "");
        }
        
        return data;
    }

    private List<Map<String, Object>> createAllTempleProductListData(DeclarationForm form) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (form.getProducts() != null) {
            // 建立产品ID到箱子ID的映射
            Map<Long, Long> productToCartonMap = new HashMap<>();
            if (form.getCartonProducts() != null) {
                for (DeclarationCartonProduct cp : form.getCartonProducts()) {
                    productToCartonMap.put(cp.getProductId(), cp.getCartonId());
                }
            }
            
            // 建立箱子ID到箱子信息的映射
            Map<Long, DeclarationCarton> cartonMap = new HashMap<>();
            if (form.getCartons() != null) {
                for (DeclarationCarton c : form.getCartons()) {
                    cartonMap.put(c.getId(), c);
                }
            }
            
            // 按箱子ID排序，同箱子的产品排列在一起
            List<DeclarationProduct> sortedProducts = new ArrayList<>(form.getProducts());
            sortedProducts.sort((a, b) -> {
                Long cartonA = productToCartonMap.getOrDefault(a.getId(), Long.MAX_VALUE);
                Long cartonB = productToCartonMap.getOrDefault(b.getId(), Long.MAX_VALUE);
                return cartonA.compareTo(cartonB);
            });
            
            int no = 1;
            for (com.declaration.entity.DeclarationProduct p : sortedProducts) {
                Map<String, Object> item = new HashMap<>();
                item.put("no", no++);
                ProductTypeConfig productTypeConfig = productTypeConfigService.getByHsCode(p.getHsCode());
                item.put("hsCode", p.getHsCode());
                item.put("productName", p.getProductEnglishName());

                item.put("nameCh", p.getProductChineseName());
                item.put("quantityStr", p.getQuantity() + " " + p.getUnit());
                item.put("quantity", p.getQuantity());
                item.put("unitPrice", p.getUnitPrice());
                item.put("unit", "PCS");
                item.put("amount", p.getAmount());
                item.put("currency", form.getCurrency());
                // item.put("currencyEng", form.getCurrency());
                item.put("originCountry", "中国(CHN)");
                CountryInfo originCountry = countryInfoService.getCountryInfoByEnglishName(form.getDestinationCountry());
                String destinationCountry = originCountry != null ? originCountry.getChineseName() + " (" + originCountry.getCountryCode() + ")" : form.getDestinationCountry();
                item.put("destinationCountry", destinationCountry);
                item.put("sourceRegion", "宁波其他（33029）");
                item.put("exemptionType", "照章征税（1）");
                item.put("statQuantity", p.getQuantity() + " " + p.getUnit());
                
                item.put("purpose", "");
                item.put("isRecorded", "否");
                item.put("brand", "");
                item.put("model", "");
                item.put("brandType", "无品牌");
                item.put("exportPreference", "不确定");
                
                item.put("unitEng", "PCS");
                item.put("contonEN", getCaronsType(form).get("contonEN"));
                
                // 获取关联的箱子信息
                Long cartonId = productToCartonMap.get(p.getId());
                if (cartonId != null) {
                    DeclarationCarton carton = cartonMap.get(cartonId);
                    if (carton != null) {
                        item.put("cartonNo", carton.getCartonNo());
                        item.put("cartons", carton.getQuantity());
                        item.put("cartonVolume", carton.getVolume());
                    } else {
                        item.put("cartonNo", null);
                        item.put("cartons", p.getCartons() != null ? p.getCartons() : 0);
                        item.put("cartonVolume", p.getVolume());
                    }
                } else {
                    item.put("cartonNo", null);
                    item.put("cartons", p.getCartons() != null ? p.getCartons() : 0);
                    item.put("cartonVolume", p.getVolume());
                }
                
                item.put("grossWeight", p.getGrossWeight());
                item.put("netWeight", p.getNetWeight());
                item.put("volume", p.getVolume());
                item.put("cbm","CBM");
                item.put("wgt","KGS");
                
                if (p.getElementValues() != null) {
                    for (com.declaration.entity.DeclarationElementValue ev : p.getElementValues()) {
                        String eName = ev.getElementName() != null ? ev.getElementName() : "";
                        String eValue = ev.getElementValue() != null ? ev.getElementValue() : "";
                        if (eName.contains("用途")) item.put("purpose", eValue);
                        if (eName.contains("品牌")) item.put("brand", eValue);
                        if (eName.contains("型号")) item.put("model", eValue);
                    }
                }
                
                StringBuilder specAndModel = new StringBuilder();
                specAndModel.append("用途:").append(item.get("purpose")).append(";");
                specAndModel.append("品牌:").append(item.get("brand")).append(";");
                specAndModel.append("型号:").append(item.get("model"));
                item.put("specAndModel", specAndModel.toString());
                
                list.add(item);
            }
        }
        return list;
    }
    
    /**
     * 计算装箱单的合并策略（用于 ProductInfo 类型的产品列表）
     * @param productList 产品列表
     * @param dataStartRow 数据起始行（0-indexed）
     * @param cartonsColIdx 箱数列索引（0-indexed）
     * @param volumeColIdx 体积列索引（0-indexed）
     * @return 合并策略列表
     */
    private List<OnceAbsoluteMergeStrategy> calculateCartonMergeStrategies(
            List<ExportDataRequest.ProductInfo> productList,
            int dataStartRow,
            int cartonsColIdx,
            int volumeColIdx) {
        
        List<OnceAbsoluteMergeStrategy> strategies = new ArrayList<>();
        
        if (productList == null || productList.isEmpty()) {
            return strategies;
        }
        
        int i = 0;
        while (i < productList.size()) {
            String cartonNo = productList.get(i).getCartonNo();
            int groupStart = i;
            
            // 找同一箱子的连续产品
            while (i + 1 < productList.size() &&
                   cartonNo != null &&
                   !cartonNo.isEmpty() &&
                   cartonNo.equals(productList.get(i + 1).getCartonNo())) {
                i++;
            }
            
            int groupSize = i - groupStart + 1;
            if (groupSize > 1 && cartonNo != null && !cartonNo.isEmpty()) {
                int startRow = dataStartRow + groupStart;
                int endRow = dataStartRow + i;
                
                // 合并 cartons(箱数) 列
                strategies.add(new OnceAbsoluteMergeStrategy(startRow, endRow, cartonsColIdx, cartonsColIdx));
                // 合并 volume(体积) 列
                strategies.add(new OnceAbsoluteMergeStrategy(startRow, endRow, volumeColIdx, volumeColIdx));
                
                log.debug("计算合并策略: cartonNo={}, rows {}-{}", cartonNo, startRow, endRow);
            }
            i++;
        }
        
        log.info("生成 {} 个合并策略", strategies.size());
        return strategies;
    }
    
    /**
     * 计算装箱单的合并策略（用于 Map 类型的产品列表）
     * @param productList 产品列表
     * @param dataStartRow 数据起始行（0-indexed）
     * @param cartonsColIdx 箱数列索引（0-indexed）
     * @param volumeColIdx 体积列索引（0-indexed）
     * @return 合并策略列表
     */
    private List<OnceAbsoluteMergeStrategy> calculateCartonMergeStrategiesForAllTemple(
            List<Map<String, Object>> productList,
            int dataStartRow,
            int cartonsColIdx,
            int volumeColIdx) {
        
        List<OnceAbsoluteMergeStrategy> strategies = new ArrayList<>();
        
        if (productList == null || productList.isEmpty()) {
            return strategies;
        }
        
        int i = 0;
        while (i < productList.size()) {
            Object cartonNoObj = productList.get(i).get("cartonNo");
            String cartonNo = cartonNoObj != null ? cartonNoObj.toString() : null;
            int groupStart = i;
            
            // 找同一箱子的连续产品
            while (i + 1 < productList.size() &&
                   cartonNo != null &&
                   !cartonNo.isEmpty()) {
                Object nextCartonNoObj = productList.get(i + 1).get("cartonNo");
                String nextCartonNo = nextCartonNoObj != null ? nextCartonNoObj.toString() : null;
                if (cartonNo.equals(nextCartonNo)) {
                    i++;
                } else {
                    break;
                }
            }
            
            int groupSize = i - groupStart + 1;
            if (groupSize > 1 && cartonNo != null && !cartonNo.isEmpty()) {
                int startRow = dataStartRow + groupStart;
                int endRow = dataStartRow + i;
                
                // 合并 cartons(箱数) 列
                strategies.add(new OnceAbsoluteMergeStrategy(startRow, endRow, cartonsColIdx, cartonsColIdx));
                // 合并 volume(体积) 列
                strategies.add(new OnceAbsoluteMergeStrategy(startRow, endRow, volumeColIdx, volumeColIdx));
                
                log.debug("计算合并策略(AllTemple): cartonNo={}, rows {}-{}", cartonNo, startRow, endRow);
            }
            i++;
        }
        
        log.info("生成 {} 个合并策略(AllTemple)", strategies.size());
        return strategies;
    }


}
