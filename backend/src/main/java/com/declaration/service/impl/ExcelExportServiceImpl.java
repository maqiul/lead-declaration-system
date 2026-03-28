package com.declaration.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.declaration.dto.CustomsItemDTO;
import com.declaration.entity.*;
import com.declaration.service.*;

import org.apache.fesod.sheet.EasyExcel;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.apache.fesod.sheet.write.metadata.fill.FillConfig;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;

import org.apache.fesod.sheet.write.builder.ExcelWriterBuilder;
import org.apache.fesod.sheet.write.builder.ExcelWriterSheetBuilder;
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

    @Autowired
    private TransportModeService transportModeService;

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
        System.out.println("输出数据:" + productList.get(0).getContonEN());

        // 计算装箱单的合并策略
        // temple.xlsx: 装箱单在Sheet 1（index=1），数据从第9行开始（0-indexed row=8）
        List<OnceAbsoluteMergeStrategy> mergeStrategies = calculateCartonMergeStrategies(productList, 8, 3, 9);

        // 使用 Fesod 填充并保存到文件，并注册合并策略
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            ExcelWriterBuilder writerBuilder = FesodSheet.write(fos)
                    .withTemplate(templatePath);

            try (ExcelWriter writer = writerBuilder.build()) {
                // 第一个工作表：商业发票
                WriteSheet invoiceSheet = FesodSheet.writerSheet(0).build();
                FillConfig invoiceConfig = FillConfig.builder().forceNewRow(false).build();
                writer.fill(fillData, invoiceSheet);
                writer.fill(productList, invoiceConfig, invoiceSheet);

                // 第二个工作表：装箱单 (注册合并策略)
                ExcelWriterSheetBuilder packingSheetBuilder = FesodSheet
                        .writerSheet(1);
                for (OnceAbsoluteMergeStrategy strategy : mergeStrategies) {
                    packingSheetBuilder.registerWriteHandler(strategy);
                }
                WriteSheet packingSheet = packingSheetBuilder.build();

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
            String configuredDir = systemConfigService.getConfigValue("file.upload.template-path",
                    "/uploads/templates");
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
        fillData.put("departureCityEnglish", form.getDepartureCityEnglish());
        // fillData.put("contonEN",)
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

    private Map<String, String> getCaronsType(DeclarationForm form) {
        // 优先从箱子列表累加
        if (form.getCartons() != null && !form.getCartons().isEmpty()) {
            DeclarationCarton carton = form.getCartons().get(0);
            String contonCH = carton.getTypeChinese();
            String contonEN = carton.getTypeEnglish();
            HashMap<String, String> map = new HashMap<>();
            map.put("contonCH", contonCH);
            map.put("contonEN", contonEN);
            return map;
        }
        HashMap<String, String> map2 = new HashMap<String, String>(2);
        map2.put("contonEN", "CARTONS");
        map2.put("contonCH", "纸箱");
        // return "CARTONS";
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
                System.out.println("获取当前产品体积:" + p.getVolume());
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
                        info.setContonEN(carton.getTypeEnglish());
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
    private Map<String, Object> prepareRemittanceData(com.declaration.entity.DeclarationRemittance remittance,
            DeclarationForm form) {
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
        String[] ones = { "", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE" };
        String[] teens = { "TEN", "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN", "SEVENTEEN",
                "EIGHTEEN", "NINETEEN" };
        String[] tens = { "", "", "TWENTY", "THIRTY", "FORTY", "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY" };
        String[] thousands = { "", "THOUSAND", "MILLION", "BILLION" };

        if (amount == 0)
            return "ZERO USD ONLY";

        long wholePart = (long) amount;
        int decimalPart = (int) Math.round((amount - wholePart) * 100);

        StringBuilder result = new StringBuilder("SAY ");

        if (wholePart > 0) {
            String wholeWords = convertWholeNumber(wholePart, ones, teens, tens, thousands);
            result.append(wholeWords);
        }

        if (decimalPart > 0) {
            if (wholePart > 0)
                result.append(" AND ");
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
        if (number == 0)
            return "ZERO";

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
    public DeclarationAttachment generateAndSaveRemittanceReport(
            com.declaration.entity.DeclarationRemittance remittance, DeclarationForm form) throws IOException {
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

    /**
     * 创建一个全空的占位对象
     */
    private static CustomsItemDTO createEmptyItem() {
        CustomsItemDTO empty = new CustomsItemDTO();
        // 必须设为空字符串，不能为 null，否则 EasyExcel 可能会跳过该行导致格式错乱
        empty.setNo(null); // 或者空字符串
        empty.setHsCode("");
        empty.setNameCh("");
        empty.setSpecAndModel("");
        empty.setQuantityStr("");
        empty.setUnitPrice(null);
        empty.setTotalPrice(null);
        empty.setCurrency("");
        empty.setOriginCountry("");
        empty.setDestinationCountry("");
        empty.setSourceRegion("");
        empty.setExemptionType("");
        empty.setStatQuantity("");
        return empty;
    }

    // 辅助方法：复制主数据字段
    private static void copyMainFields(CustomsItemDTO target, CustomsItemDTO source) {
        target.setNo(source.getNo());
        target.setHsCode(source.getHsCode());
        target.setNameCh(source.getNameCh());
        target.setQuantityStr(source.getQuantityStr());
        target.setUnitPrice(source.getUnitPrice());
        target.setTotalPrice(source.getTotalPrice());
        target.setCurrency(source.getCurrency());
        target.setOriginCountry(source.getOriginCountry());
        target.setDestinationCountry(source.getDestinationCountry());
        target.setSourceRegion(source.getSourceRegion());
        target.setExemptionType(source.getExemptionType());
        target.setStatQuantity(source.getStatQuantity());
        // 注意：这里不设置 setSpecAndModel，保持 null
    }

    @Override
    public DeclarationAttachment generateAndSaveAllTempleExportDocuments(DeclarationForm form) throws IOException {
        String templatePath = getAllTempleTemplatePath();
        if (templatePath == null) {
            throw new RuntimeException("模板文件不存在: alltemple_template.xlsx");
        }

        File dir = new File(uploadPath);
        if (!dir.exists())
            dir.mkdirs();

        // 创建以申报号命名的子目录
        String formNoDir = form.getFormNo();
        File formDir = new File(uploadPath, formNoDir);
        if (!formDir.exists()) {
            formDir.mkdirs();
        }

        String fileName = "海关申报单_" + form.getFormNo() + "_" + IdUtil.simpleUUID() + ".xlsx";
        String filePath = Paths.get(formDir.getAbsolutePath(), fileName).toString();
        File targetFile = new File(filePath);

        Map<String, Object> fillData = prepareAllTempleFillData(form);
        Map<String, Object> otherData = prepareFillData(form);
        List<CustomsItemDTO> productListData = createAllTempleProductListData(form);
        List<ExportDataRequest.ProductInfo> productList = createProductListData(form);

        List<List<Object>> elementListData = createSingleRowProductListData(form);
        List<OnceAbsoluteMergeStrategy> mergeStrategies = calculateCartonMergeStrategies(productList, 8,
                3, 9);

        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            ExcelWriterBuilder writerBuilder = FesodSheet.write(fos)
                    .withTemplate(templatePath)
                    .inMemory(true);

            try (ExcelWriter writer = writerBuilder.build()) {
                // Sheet 0: 报关单
                WriteSheet hgsheet = FesodSheet.writerSheet(0).build();
                writer.fill(fillData, hgsheet);

                // 获取 Workbook/Sheet 引用
                Workbook workbook = writer.writeContext().writeWorkbookHolder().getWorkbook();
                Sheet sheet = workbook.getSheetAt(0);
                // 定义边框样式（可自定义：细线、粗线等）
                BorderStyle borderStyle = BorderStyle.THIN; // 细线
                short borderColor = IndexedColors.BLACK.getIndex(); // 黑色

                // 1. 获取模板样式引用 (Row 19=18, Row 20=19, Row 21=20)
                Row templateRow1 = sheet.getRow(18);
                Row templateRow2 = sheet.getRow(19);
                Row templateRow3 = sheet.getRow(20);

                float h1 = (templateRow1 != null) ? templateRow1.getHeightInPoints() : 15.0f;
                float h2 = (templateRow2 != null) ? templateRow2.getHeightInPoints() : 15.0f;
                float h3 = (templateRow3 != null) ? templateRow3.getHeightInPoints() : 15.0f;

                // 定义精确索引 (3:名称/规格, 6:数量/统计, 7:单价, 8:总价, 9:币制, 10:原产, 11:目的, 13:货源, 16:征免)
                int[] colIndices = { 0, 1, 3, 6, 7, 8, 9, 10, 11, 13, 16 };
                Map<Integer, CellStyle> sMap1 = new HashMap<>();
                Map<Integer, CellStyle> sMap2 = new HashMap<>();
                Map<Integer, CellStyle> sMap3 = new HashMap<>();

                for (int c : colIndices) {
                    sMap1.put(c,
                            (templateRow1 != null && templateRow1.getCell(c) != null)
                                    ? templateRow1.getCell(c).getCellStyle()
                                    : workbook.createCellStyle());
                    sMap2.put(c,
                            (templateRow2 != null && templateRow2.getCell(c) != null)
                                    ? templateRow2.getCell(c).getCellStyle()
                                    : workbook.createCellStyle());
                    sMap3.put(c,
                            (templateRow3 != null && templateRow3.getCell(c) != null)
                                    ? templateRow3.getCell(c).getCellStyle()
                                    : workbook.createCellStyle());
                }

                // --- 动态行平移逻辑：保护 28 行（Index 27）起的页脚数据 ---
                int numProducts = productListData.size();
                int rowsRequired = numProducts * 3;
                int startRowIdx = 18;
                int footerRowIdx = 27; // 第 28 行的索引
                int availableSpace = footerRowIdx - startRowIdx;

                if (rowsRequired > availableSpace) {
                    int shiftAmount = rowsRequired - availableSpace;
                    // 将页脚及其下方的所有行向下挪动，腾出所需的物理行空间
                    int lastRow = sheet.getLastRowNum();
                    if (lastRow >= footerRowIdx) {
                        sheet.shiftRows(footerRowIdx, lastRow, shiftAmount, true, false);
                    }
                }

                int currentStartRow = 18;
                for (CustomsItemDTO item : productListData) {
                    // --- Row 1: 主信息 ---
                    Row r1 = sheet.getRow(currentStartRow);
                    if (r1 == null) r1 = sheet.createRow(currentStartRow);
                    r1.setHeightInPoints(h1);
                    createCell(r1, 0, item.getNo(), sMap1.get(0));
                    createCell(r1, 1, item.getHsCode(), sMap1.get(1));
                    createCell(r1, 3, item.getNameCh(), sMap1.get(3));
                    createCell(r1, 6, item.getQuantityStr(), sMap1.get(6));
                    createCell(r1, 7,
                            item.getUnitPrice() != null ? item.getUnitPrice().setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() : "",
                            sMap1.get(7));
                    createCell(r1, 8,
                            item.getTotalPrice() != null ? item.getTotalPrice().setScale(2, RoundingMode.HALF_UP).toPlainString()
                                    : "",
                            sMap1.get(8));
                    createCell(r1, 9, item.getCurrency() != null ? item.getCurrency() : "", sMap1.get(9));
                    createCell(r1, 10, item.getOriginCountry(), sMap1.get(10));
                    createCell(r1, 11, item.getDestinationCountry(), sMap1.get(11));
                    createCell(r1, 13, item.getSourceRegion(), sMap1.get(13));
                    createCell(r1, 16, item.getExemptionType(), sMap1.get(16));

                    // --- Row 2: 辅助与规格 1 ---
                    Row r2 = sheet.getRow(currentStartRow + 1);
                    if (r2 == null) r2 = sheet.createRow(currentStartRow + 1);
                    r2.setHeightInPoints(h2);
                    createCell(r2, 3, item.getSpecAndModel(), sMap2.get(3));
                    createCell(r2, 6, item.getStatQuantity(), sMap2.get(6));

                    // --- Row 3: 辅助与规格 2 (作为规格的视觉延伸) ---
                    Row r3 = sheet.getRow(currentStartRow + 2);
                    if (r3 == null) r3 = sheet.createRow(currentStartRow + 2);
                    r3.setHeightInPoints(h3);
                    // 为第 3 行所有列同步样式，底边粗线通常在这里
                    for (int c : colIndices) {
                        createCell(r3, c, null, sMap3.get(c));
                    }

                    // --- 合并策略 ---
                    int rowFrom = currentStartRow;
                    int rowTo = currentStartRow + 2;

                    // // A:、价格、国家、征免等列垂直合并 3 行
                    // int[] v3Cols = { 1, 7, 8, 9, 10, 11, 13, 16 };
                    // for (int vc : v3Cols) {
                    // removeOverlapMergedRegions(sheet, rowFrom, rowTo, vc, vc);
                    // sheet.addMergedRegion(new CellRangeAddress(rowFrom, rowTo, vc, vc));
                    // }
                    // A: 商品编码区域 (第 1 行水平合并 1-2)
                    CellRangeAddress cellRangeAddress = new CellRangeAddress(rowFrom, rowFrom, 1, 2);
                    removeOverlapMergedRegions(sheet, rowFrom, rowFrom, 1, 2);
                    sheet.addMergedRegion(cellRangeAddress);
                    RegionUtil.setBorderTop(borderStyle, cellRangeAddress, sheet);

                    // B: 名称区域 (第 1 行水平合并 3-5)
                    CellRangeAddress cellRangeAddress2 = new CellRangeAddress(rowFrom, rowFrom, 3, 5);
                    removeOverlapMergedRegions(sheet, rowFrom, rowFrom, 3, 5);
                    sheet.addMergedRegion(cellRangeAddress2);
                    RegionUtil.setBorderTop(borderStyle, cellRangeAddress2, sheet);
                    // D: 目的国区域 (第 1 行水平合并 9-10)
                    CellRangeAddress cellRangeAddress3 = new CellRangeAddress(rowFrom, rowFrom, 11, 12);
                    removeOverlapMergedRegions(sheet, rowFrom, rowFrom, 11, 12);
                    sheet.addMergedRegion(cellRangeAddress3);
                    RegionUtil.setBorderTop(borderStyle, cellRangeAddress3, sheet);
                    // E: 货源地 (第 1 行水平合并 9-10)
                    CellRangeAddress cellRangeAddress4 = new CellRangeAddress(rowFrom, rowFrom, 13, 15);
                    removeOverlapMergedRegions(sheet, rowFrom, rowFrom, 13, 15);
                    sheet.addMergedRegion(cellRangeAddress4);
                    RegionUtil.setBorderTop(borderStyle, cellRangeAddress4, sheet);
                    // F: 规格区域 (第 2-3 行物理区域，水平 3-5 合并，垂直 2-3 合并)
                    CellRangeAddress cellRangeAddress5 = new CellRangeAddress(rowFrom + 1, rowTo, 3, 5);
                    removeOverlapMergedRegions(sheet, rowFrom + 1, rowTo, 3, 5);
                    sheet.addMergedRegion(cellRangeAddress5);
                    // RegionUtil.setBorderTop(borderStyle, cellRangeAddress5, sheet);

                    currentStartRow += 3;
                }

                // Sheet 1: 申报要素
                WriteSheet ys = FesodSheet.writerSheet(1).needHead(false).relativeHeadRowIndex(3).build();
                writer.write(elementListData, ys);

                // Sheet 2: 发票
                WriteSheet invoiceSheet = FesodSheet.writerSheet(2).build();
                writer.fill(otherData, invoiceSheet);
                writer.fill(productList, FillConfig.builder().forceNewRow(false).build(), invoiceSheet);

                // Sheet 3: 装箱单
                ExcelWriterSheetBuilder packingSheetBuilder = FesodSheet.writerSheet(3);
                for (OnceAbsoluteMergeStrategy strategy : mergeStrategies) {
                    packingSheetBuilder.registerWriteHandler(strategy);
                }
                WriteSheet packingSheet = packingSheetBuilder.build();
                writer.fill(otherData, packingSheet);
                writer.fill(productList, FillConfig.builder().forceNewRow(false).build(), packingSheet);
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

    /**
     * 辅助方法：移除指定区域内的冲突合并单元格
     */
    private static void removeOverlapMergedRegions(Sheet sheet, int rowFrom, int rowTo, int colFrom, int colTo) {
        CellRangeAddress target = new CellRangeAddress(rowFrom, rowTo, colFrom, colTo);
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.intersects(target)) {
                sheet.removeMergedRegion(i);
                i--;
            }
        }
    }

    /**
     * 辅助方法：创建单元格并设置样式和值
     */
    private static void createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellStyle(style);

        if (value == null) {
            // 如果值为空，不写入内容，保持单元格为空
            return;
        }

        // 根据值的类型设置单元格内容
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
        }
    }

    /**
     * 为“申报要素”等单行显示的 Sheet 提供数据 (1个商品对应1个 Map)
     */
    private List<List<Object>> createSingleRowProductListData(DeclarationForm form) {
        List<List<Object>> list = new ArrayList<>();
        if (form.getProducts() == null)
            return list;

        int no = 1;
        for (DeclarationProduct p : form.getProducts()) {
            // 每一个 add 都会生成新的一行
            list.add(Collections.singletonList("项号:" + no++));
            list.add(Collections.singletonList("商品编码:" + p.getHsCode()));
            list.add(Collections.singletonList("商品名称:" + p.getProductChineseName()));

            if (p.getElementValues() != null) {
                for (int i = 0; i < p.getElementValues().size(); i++) {
                    DeclarationElementValue ev = p.getElementValues().get(i);
                    String eName = ev.getElementName() != null ? ev.getElementName() : "";
                    String eValue = ev.getElementValue() != null ? ev.getElementValue() : "";
                    list.add(Collections.singletonList(i + "." + eName + ":" + eValue));
                }
            }
            // 商品之间加一个空行
            list.add(Collections.singletonList(""));
        }
        return list;
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
            if (countryInfo != null) {
                tradeCountryDisplay = countryInfo.getChineseName();
            }
        } catch (Exception e) {
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
        data.put("shipperAddress", form.getShipperAddress());
        data.put("CompanyAddress", form.getShipperAddress());
        data.put("consigneeAddress", form.getConsigneeAddress());
        data.put("invoiceNo", form.getInvoiceNo());
        data.put("packingListNo", form.getInvoiceNo());
        // data.put(destinationCountryDisplay, tradeCountryDisplay);
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
        String transprotModeDisplay = form.getTransportMode();
        try {
            TransportMode transportMode = transportModeService
                    .getOne(new QueryWrapper<TransportMode>().eq("name", form.getTransportMode()));
            transprotModeDisplay = transportMode.getChineseName();
        } catch (Exception exception) {
            log.warn("获取运输方式失败");
        }
        data.put("transportMode", (form.getTransportMode() != null ? form.getTransportMode() : "TRUCK"));
        data.put("transportModeCn", transprotModeDisplay);
        data.put("paymentTerms", "T/T");
        data.put("portOfDepartureEng", form.getDepartureCity());
        data.put("destinationRegion", form.getDestinationCountry());
        data.put("departureCityEnglish", form.getDepartureCityEnglish());
        // data.put("destinationCountry", form.getDestinationCountry());
        // data.put("packageType", "CARTONS");

        if (form.getTotalAmount() != null) {
            data.put("totalAmountWords", convertAmountToWords(form.getTotalAmount().doubleValue()));
        } else {
            data.put("totalAmountWords", "");
        }

        return data;
    }

    private List<CustomsItemDTO> createAllTempleProductListData(DeclarationForm form) {
        List<CustomsItemDTO> list = new ArrayList<>();
        if (form.getProducts() != null) {
            // 建立产品ID到箱子ID的映射
            Map<Long, Long> productToCartonMap = new HashMap<>();
            if (form.getCartonProducts() != null) {
                for (com.declaration.entity.DeclarationCartonProduct cp : form.getCartonProducts()) {
                    productToCartonMap.put(cp.getProductId(), cp.getCartonId());
                }
            }

            // 建立箱子ID到箱子信息的映射
            Map<Long, com.declaration.entity.DeclarationCarton> cartonMap = new HashMap<>();
            if (form.getCartons() != null) {
                for (com.declaration.entity.DeclarationCarton c : form.getCartons()) {
                    cartonMap.put(c.getId(), c);
                }
            }

            // 按箱子ID排序
            List<com.declaration.entity.DeclarationProduct> sortedProducts = new ArrayList<>(form.getProducts());
            sortedProducts.sort((a, b) -> {
                Long cartonA = productToCartonMap.getOrDefault(a.getId(), Long.MAX_VALUE);
                Long cartonB = productToCartonMap.getOrDefault(b.getId(), Long.MAX_VALUE);
                return cartonA.compareTo(cartonB);
            });

            int no = 1;
            for (com.declaration.entity.DeclarationProduct p : sortedProducts) {
                CustomsItemDTO customsItemDTO = new CustomsItemDTO();
                customsItemDTO.setNo(no++);
                customsItemDTO.setHsCode(p.getHsCode());
                // customsItemDTO.set.put("productName", p.getProductEnglishName());
                customsItemDTO.setNameCh(p.getProductChineseName());
                customsItemDTO.setQuantityStr(p.getQuantity() + " " + "个");
                customsItemDTO.setUnitPrice(p.getUnitPrice());
                customsItemDTO.setTotalPrice(p.getAmount());
                customsItemDTO.setCurrency(form.getCurrency());
                customsItemDTO.setOriginCountry("中国(CHN)");

                CountryInfo destCountryInfo = countryInfoService
                        .getCountryInfoByEnglishName(form.getDestinationCountry());
                String destinationCountry = destCountryInfo != null
                        ? destCountryInfo.getChineseName() + " (" + destCountryInfo.getCountryCode() + ")"
                        : form.getDestinationCountry();
                customsItemDTO.setDestinationCountry(destinationCountry);
                customsItemDTO.setSourceRegion("宁波其他（33029）");
                customsItemDTO.setExemptionType("照章征税（1）");

                // 统计数量 (对应模板中的 {.statQuantity})
                customsItemDTO.setStatQuantity(p.getQuantity() + " (个)");
                // list.add(row1);
                // 箱号处理
                Long cartonId = productToCartonMap.get(p.getId());
                if (cartonId != null) {
                    com.declaration.entity.DeclarationCarton carton = cartonMap.get(cartonId);
                    if (carton != null) {
                        // list.add(row1);

                        // --- Row 2: 统计数量 & 规格型号行 (与统计数量在同一行水平对齐) ---
                        // Map<String, Object> row2 = new HashMap<>();
                        // row1.put("statQuantity", p.getQuantity() + " (个)");

                        // 规格型号解析：拼接所有申报要素
                        StringBuilder specAndModel = new StringBuilder();
                        if (p.getElementValues() != null) {
                            for (com.declaration.entity.DeclarationElementValue ev : p.getElementValues()) {
                                String eName = ev.getElementName() != null ? ev.getElementName() : "";
                                String eValue = ev.getElementValue() != null ? ev.getElementValue() : "";
                                specAndModel.append(eName).append(":").append(eValue).append(";");
                            }
                        }
                        customsItemDTO.setSpecAndModel(specAndModel.toString());
                        list.add(customsItemDTO);

                        // // --- Row 3: 空行，维持三行步进步长 ---
                        // Map<String, Object> row3 = new HashMap<>();
                        // list.add(row3);
                    }
                }
            }
        }
        return list;
    }

    /**
     * 计算装箱单的合并策略（用于 ProductInfo 类型的产品列表）
     * 
     * @param productList   产品列表
     * @param dataStartRow  数据起始行（0-indexed）
     * @param cartonsColIdx 箱数列索引（0-indexed）
     * @param volumeColIdx  体积列索引（0-indexed）
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
     * 
     * @param productList   产品列表
     * @param dataStartRow  数据起始行（0-indexed）
     * @param cartonsColIdx 箱数列索引（0-indexed）
     * @param volumeColIdx  体积列索引（0-indexed）
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
