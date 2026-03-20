package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.declaration.dao.ContractTemplateDao;
import com.declaration.entity.ContractGeneration;
import com.declaration.entity.ContractTemplate;
import com.declaration.entity.DeclarationForm;
import com.declaration.service.ContractGenerateService;
import com.declaration.service.ContractGenerationService;
import com.declaration.service.DeclarationFormService;
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
    private final DeclarationFormService declarationFormService;

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

            // 2. 查询申报单详情
            DeclarationForm declarationForm = declarationFormService.getById(declarationFormId);
            if (declarationForm == null) {
                throw new RuntimeException("申报单不存在");
            }

            // 3. 构建模板数据（合并外部传入的数据和申报单数据）
            Map<String, Object> templateData = new HashMap<>();
            if (dataMap != null) {
                templateData.putAll(dataMap);
            }

            // 自动填充申报单核心字段
            templateData.put("formNo", declarationForm.getFormNo() != null ? declarationForm.getFormNo() : "");
            templateData.put("declarationDate", declarationForm.getDeclarationDate() != null ? declarationForm.getDeclarationDate().toString() : "");
            templateData.put("shipperCompany", declarationForm.getShipperCompany() != null ? declarationForm.getShipperCompany() : "");
            templateData.put("shipperAddress", declarationForm.getShipperAddress() != null ? declarationForm.getShipperAddress() : "");
            templateData.put("consigneeCompany", declarationForm.getConsigneeCompany() != null ? declarationForm.getConsigneeCompany() : "");
            templateData.put("consigneeAddress", declarationForm.getConsigneeAddress() != null ? declarationForm.getConsigneeAddress() : "");
            templateData.put("invoiceNo", declarationForm.getInvoiceNo() != null ? declarationForm.getInvoiceNo() : "");
            templateData.put("totalAmount", declarationForm.getTotalAmount() != null ? declarationForm.getTotalAmount().toString() : "0");
            templateData.put("totalQuantity", declarationForm.getTotalQuantity() != null ? declarationForm.getTotalQuantity().toString() : "0");
            templateData.put("totalCartons", declarationForm.getTotalCartons() != null ? declarationForm.getTotalCartons().toString() : "0");
            templateData.put("totalGrossWeight", declarationForm.getTotalGrossWeight() != null ? declarationForm.getTotalGrossWeight().toString() : "0");
            templateData.put("totalNetWeight", declarationForm.getTotalNetWeight() != null ? declarationForm.getTotalNetWeight().toString() : "0");
            templateData.put("totalVolume", declarationForm.getTotalVolume() != null ? declarationForm.getTotalVolume().toString() : "0");
            templateData.put("currency", declarationForm.getCurrency() != null ? declarationForm.getCurrency() : "");
            templateData.put("transportMode", declarationForm.getTransportMode() != null ? declarationForm.getTransportMode() : "");
            templateData.put("departureCity", declarationForm.getDepartureCity() != null ? declarationForm.getDepartureCity() : "");
            templateData.put("destinationCountry", declarationForm.getDestinationCountry() != null ? declarationForm.getDestinationCountry() : "");
            
            // 生成日期
            templateData.put("generatedDate", LocalDate.now().toString());

            // 4. 生成合同编号
            String contractNo = generateContractNumber(template.getTemplateType());

            // 5. 构造模板文件路径
            Path templatePath = Paths.get(System.getProperty("user.dir"), template.getFilePath());
            if (!Files.exists(templatePath)) {
                log.warn("模板文件不存在: {}，跳过合同生成", templatePath);
                return null;
            }

            // 6. 生成输出文件路径
            String fileName = contractNo + "_" + System.currentTimeMillis() + ".docx";
            Path outputPath = Paths.get(System.getProperty("user.dir"), contractUploadPath, fileName);

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
            generation.setGeneratedFilePath(outputPath.toString());
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
    public String uploadTemplateFile(MultipartFile file, Long templateId) {
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

            // 构造保存路径
            Path savePath = Paths.get(System.getProperty("user.dir"), templateUploadPath, fileName);
            Files.createDirectories(savePath.getParent());

            // 保存文件
            file.transferTo(savePath);

            log.info("模板文件上传成功: {}, 大小: {} bytes", fileName, file.getSize());
            return savePath.toString();

        } catch (Exception e) {
            log.error("模板文件上传失败", e);
            throw new RuntimeException("模板文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public File getContractFile(Long contractId) {
        ContractGeneration generation = contractGenerationService.getById(contractId);
        if (generation == null) {
            throw new RuntimeException("合同记录不存在");
        }

        File file = new File(generation.getGeneratedFilePath());
        if (!file.exists()) {
            throw new RuntimeException("合同文件不存在");
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
}