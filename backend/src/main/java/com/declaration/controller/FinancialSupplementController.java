package com.declaration.controller;

import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.FinancialSupplement;
import com.declaration.entity.DeclarationAttachment;
import com.declaration.service.FinancialSupplementService;
import com.declaration.service.DeclarationAttachmentService;
import com.declaration.service.ExcelExportService;
import com.declaration.entity.EntityConfig;
import com.declaration.service.EntityConfigService;
import com.declaration.entity.PartyBConfig;
import com.declaration.service.PartyBConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.common.PageParam;
import java.time.LocalDateTime;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.common.PageParam;
import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletResponse;
import com.declaration.service.DeclarationFormService;
import com.declaration.service.DeclarationRemittanceService;
import com.declaration.entity.DeclarationRemittance;
import com.declaration.entity.DeclarationForm;
import java.math.BigDecimal;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

@Slf4j
@Tag(name = "财务开票补充接口")
@RestController
@Component
@RequestMapping("/v1/financial-supplements")
public class FinancialSupplementController {

    private final FinancialSupplementService supplementService;
    private final DeclarationFormService formService;
    private final DeclarationRemittanceService remittanceService;
    private final DeclarationAttachmentService attachmentService;
    private final com.declaration.service.SystemConfigService systemConfigService;
    private final EntityConfigService entityConfigService;
    private final PartyBConfigService partyBConfigService;
    private final com.declaration.service.CurrencyInfoService currencyInfoService;

    @Value("${file.upload-path:uploads/exports/}")
    private String uploadPath;
    
    public FinancialSupplementController(FinancialSupplementService supplementService, 
            DeclarationFormService formService, 
            DeclarationRemittanceService remittanceService, 
            DeclarationAttachmentService attachmentService,
            com.declaration.service.SystemConfigService systemConfigService,
            EntityConfigService entityConfigService,
            PartyBConfigService partyBConfigService,
            com.declaration.service.CurrencyInfoService currencyInfoService) {
        this.supplementService = supplementService;
        this.formService = formService;
        this.remittanceService = remittanceService;
        this.attachmentService = attachmentService;
        this.systemConfigService = systemConfigService;
        this.entityConfigService = entityConfigService;
        this.partyBConfigService = partyBConfigService;
        this.currencyInfoService = currencyInfoService;
    }

    @GetMapping("/form/{formId}")
    @Operation(summary = "获取申报单关联的财务补充记录")
    @RequiresPermissions("business:declaration:view")
    public Result<FinancialSupplement> getByFormId(
            @Parameter(description = "申报单ID") @PathVariable Long formId) {
        FinancialSupplement supp = supplementService.lambdaQuery()
                .eq(FinancialSupplement::getFormId, formId)
                .one();
        return Result.success(supp);
    }

    @GetMapping("/form/{formId}/calculation-detail")
    @Operation(summary = "获取开票明细计算过程")
    @RequiresPermissions("business:declaration:view")
    public Result<Map<String, Object>> getCalculationDetail(
            @Parameter(description = "申报单ID") @PathVariable Long formId) {
        // 获取计算结果
        Map<String, Object> detail = supplementService.getCalculationDetail(formId);
        
        // 同时更新数据库中的计算结果JSON
        FinancialSupplement supp = supplementService.lambdaQuery()
                .eq(FinancialSupplement::getFormId, formId)
                .one();
        if (supp != null) {
            try {
                supp.setCalculationDetail(JSON.toJSONString(detail));
                
                // 单独提取并存储退税金额
                BigDecimal totalGoodsAmount = (BigDecimal) detail.get("totalGoodsAmount");
                BigDecimal amountWithTaxRefund = (BigDecimal) detail.get("amountWithTaxRefund");
                if (totalGoodsAmount != null && amountWithTaxRefund != null) {
                    BigDecimal taxRefundAmount = amountWithTaxRefund.subtract(totalGoodsAmount);
                    supp.setTaxRefundAmount(taxRefundAmount); // 退税金额 = 含税总额 - 原始金额
                }
                
                supp.setUpdateTime(LocalDateTime.now());
                supplementService.updateById(supp);
            } catch (Exception e) {
                log.warn("更新计算明细JSON失败", e);
            }
        }
        
        return Result.success(detail);
    }

    @PostMapping
    @Operation(summary = "创建财务开票补充记录")
    @RequiresPermissions("business:declaration:finance:supplement")
    public Result<FinancialSupplement> createSupplement(
            @RequestBody FinancialSupplement supplement) {
        
        if (supplement.getFormId() != null) {
            DeclarationForm form = formService.getById(supplement.getFormId());
            if (form != null) {
                supplement.setFormNo(form.getFormNo());
            }
        } else if (supplement.getFormNo() != null && !supplement.getFormNo().isBlank()) {
            DeclarationForm form = formService.lambdaQuery()
                    .eq(DeclarationForm::getFormNo, supplement.getFormNo().trim())
                    .one();
            if (form == null) {
                return Result.fail(400, "申报单不存在: " + supplement.getFormNo());
            }
            supplement.setFormId(form.getId());
            supplement.setFormNo(form.getFormNo());
        } else {
            return Result.fail(400, "请选择申报单");
        }

        DeclarationForm targetForm = formService.getById(supplement.getFormId());
        if (targetForm == null) {
            return Result.fail(400, "申报单不存在");
        }
        if (targetForm.getStatus() == null || targetForm.getStatus() < 3) {
            return Result.fail(400, "仅支持资料提交环节之后的申报单维护退税点");
        }

        long exists = supplementService.lambdaQuery()
                .eq(FinancialSupplement::getFormId, supplement.getFormId())
                .count();
        if (exists > 0) {
            return Result.fail(400, "该申报单已有退税点记录，请直接编辑");
        }

        supplement.setCreateTime(LocalDateTime.now());
        supplement.setUpdateTime(LocalDateTime.now());
        supplementService.save(supplement);
        return Result.success(supplement);
    }
        
        @PutMapping("/{id}")
    @Operation(summary = "更新财务开票补充记录")
    @RequiresPermissions("business:declaration:finance:supplement")
    public Result<Void> updateSupplement(
            @Parameter(description = "记录ID") @PathVariable Long id,
            @RequestBody FinancialSupplement supplement) {
        supplement.setId(id);
        supplement.setUpdateTime(LocalDateTime.now());
        supplementService.updateById(supplement);
        return Result.success();
    }
    
    @GetMapping
    @Operation(summary = "分页查询财务补充单证")
    @RequiresPermissions("business:declaration:view")
    public Result<IPage<FinancialSupplement>> getPage(
            @Parameter(description = "分页参数") PageParam pageParam,
            @Parameter(description = "申报单号") @RequestParam(required = false) String formNo,
            @Parameter(description = "状态 (0-待上传, 1-已提交)") @RequestParam(required = false) Integer status) {
            
        Page<FinancialSupplement> page = new Page<>(pageParam.getCurrent(), pageParam.getSize());
        LambdaQueryWrapper<FinancialSupplement> wrapper = new LambdaQueryWrapper<>();
        
        if (formNo != null && !formNo.isEmpty()) {
            wrapper.like(FinancialSupplement::getFormNo, formNo);
        }
        if (status != null) {
            wrapper.eq(FinancialSupplement::getStatus, status);
        }
        
        wrapper.orderByDesc(FinancialSupplement::getCreateTime);
        
        IPage<FinancialSupplement> result = supplementService.page(page, wrapper);
        
        // 填充申报单信息
        for (FinancialSupplement item : result.getRecords()) {
            if (item.getFormId() != null) {
                DeclarationForm form = formService.getById(item.getFormId());
                if (form != null) {
                    item.setDeclarationAmount(form.getTotalAmount());
                    item.setDeclarationCurrency(form.getCurrency());
                    item.setDeclarationStatus(form.getStatus());
                    item.setShipperCompany(form.getShipperCompany());
                    item.setConsigneeCompany(form.getConsigneeCompany());
                    item.setTotalCartons(form.getTotalCartons());
                    item.setRequestedInvoiceAmount(form.getRequestedInvoiceAmount());
                }
            }
        }
        
        return Result.success(result);
    }

    /**
     * 可新增退税点的申报单：资料已提交（status&gt;=3）且尚未建立财务单证记录。
     */
    @GetMapping("/eligible-declarations")
    @Operation(summary = "查询可新增财务单证的申报单")
    @RequiresPermissions("business:declaration:finance:supplement")
    public Result<IPage<DeclarationForm>> listEligibleDeclarations(
            PageParam pageParam,
            @Parameter(description = "申报单号（模糊）") @RequestParam(required = false) String formNo) {
        Page<DeclarationForm> page = new Page<>(pageParam.getCurrent(), pageParam.getSize());
        LambdaQueryWrapper<DeclarationForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(DeclarationForm::getStatus, 3);
        wrapper.apply(
                "NOT EXISTS (SELECT 1 FROM financial_supplement fs WHERE fs.form_id = declaration_form.id)");
        if (formNo != null && !formNo.isBlank()) {
            wrapper.like(DeclarationForm::getFormNo, formNo.trim());
        }
        wrapper.orderByDesc(DeclarationForm::getUpdateTime);
        return Result.success(formService.page(page, wrapper));
    }

    @GetMapping("/form/{formId}/export-finance-calculation")
    @Operation(summary = "导出开票计算明细单")
    @RequiresPermissions("business:declaration:finance:supplement")
    public Result<String> exportFinanceCalculation(@PathVariable Long formId) {
        ByteArrayOutputStream outputStream = null;
        XSSFWorkbook workbook = null;
        try {
            DeclarationForm form = formService.getById(formId);
            if (form == null) {
                throw new RuntimeException("申报单不存在");
            }
            String formNo = form.getFormNo();
            FinancialSupplement supp = supplementService.lambdaQuery()
                    .eq(FinancialSupplement::getFormId, formId)
                    .one();
            
            // 获取完整的计算明细
            Map<String, Object> calcDetail = supplementService.getCalculationDetail(formId);
            if (calcDetail == null) {
                throw new RuntimeException("未找到开票明细数据");
            }
            
            // 生成Excel
            workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("开票明细计算单");

            XSSFCellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            int rowNum = 0;
            XSSFRow titleRow = sheet.createRow(rowNum++);
            XSSFCell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("开票明细计算单 - 单号: " + (supp != null && supp.getDetailsInvoiceNo() != null ? supp.getDetailsInvoiceNo() : form.getFormNo()));
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            
            rowNum++;

            // 基本信息（货代/报关发票号从申报资料项实时读取，financial_supplement 对应字段已废弃）
            createDataRow(sheet, rowNum++, "申报单号", form.getFormNo(), headerStyle);
            {
                com.declaration.service.impl.FinancialSupplementServiceImpl impl =
                        (supplementService instanceof com.declaration.service.impl.FinancialSupplementServiceImpl)
                                ? (com.declaration.service.impl.FinancialSupplementServiceImpl) supplementService : null;
                String freightInvoiceNo = impl != null
                        ? impl.getInvoiceNoFromMaterial(formId, com.declaration.service.impl.FinancialSupplementServiceImpl.getFreightCode())
                        : (supp != null ? supp.getFreightInvoiceNo() : null);
                String customsInvoiceNo = impl != null
                        ? impl.getInvoiceNoFromMaterial(formId, com.declaration.service.impl.FinancialSupplementServiceImpl.getCustomsCode())
                        : (supp != null ? supp.getCustomsInvoiceNo() : null);
                createDataRow(sheet, rowNum++, "货代发票号", freightInvoiceNo, headerStyle);
                createDataRow(sheet, rowNum++, "报关代理发票号", customsInvoiceNo, headerStyle);
            }
            createDataRow(sheet, rowNum++, "外汇银行名称", String.valueOf(calcDetail.get("foreignExchangeBank")), headerStyle);
            
            rowNum++;
            
            // 收汇明细
            XSSFRow remTitleRow = sheet.createRow(rowNum++);
            remTitleRow.createCell(0).setCellValue("收汇明细");
            remTitleRow.getCell(0).setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 3));
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> remittanceDetails = (List<Map<String, Object>>) calcDetail.get("remittanceDetails");
            if (remittanceDetails != null && !remittanceDetails.isEmpty()) {
                for (Map<String, Object> d : remittanceDetails) {
                    String name = d.get("remittanceName") != null ? d.get("remittanceName").toString() : "";
                    BigDecimal amt = (BigDecimal) d.get("amount");
                    BigDecimal rate = (BigDecimal) d.get("taxRate");
                    BigDecimal cny = (BigDecimal) d.get("cnyAmount");
                    BigDecimal bankFeeCny = (BigDecimal) d.get("bankFeeCny");
                    BigDecimal internalFeeCny = (BigDecimal) d.get("internalBankFee");
                    String currency = d.get("currency") != null ? d.get("currency").toString() : getDefaultCurrency();
                    createDataRow(sheet, rowNum++, name.isEmpty() ? "水单" : name,
                            String.format("%1$,.2f %2$s × %3$s = %4$,.2f CNY", amt, currency, rate, cny), headerStyle);
                }
            }
            createDataRow(sheet, rowNum++, "收汇合计(CNY)", String.format("%,.2f", calcDetail.get("totalCny")), headerStyle);

            rowNum++;
            
            // 计算过程
            XSSFRow calcTitleRow = sheet.createRow(rowNum++);
            calcTitleRow.createCell(0).setCellValue("计算步骤与数值明细");
            calcTitleRow.getCell(0).setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 3));

            createDataRow(sheet, rowNum++, "总货物金额(CNY)", String.format("%,.2f", calcDetail.get("totalGoodsAmount")), headerStyle);

            // 逐产品退税明细
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> productTaxDetails = (List<Map<String, Object>>) calcDetail.get("productTaxDetails");
            if (productTaxDetails != null && !productTaxDetails.isEmpty()) {
                for (Map<String, Object> pd : productTaxDetails) {
                    String label = String.format("商品[%1$s] 退税率(%2$s%%) 分摊%3$s%%",
                            pd.get("productName") != null ? pd.get("productName") : "",
                            pd.get("taxRefundRate") != null ? pd.get("taxRefundRate") : "0",
                            pd.get("proportion") != null ? pd.get("proportion") : "0");
                    createDataRow(sheet, rowNum++, label, String.format("%,.2f", pd.get("amountWithTaxRefund")), headerStyle);
                }
            } else {
                createDataRow(sheet, rowNum++, "退税点(%)", "0（无商品信息）", headerStyle);
            }

            // 计算并显示退税金额（含税金额 - 原始金额）
            BigDecimal totalGoodsAmount = (BigDecimal) calcDetail.get("totalGoodsAmount");
            BigDecimal amountWithTaxRefund = (BigDecimal) calcDetail.get("amountWithTaxRefund");
            BigDecimal taxRefundAmount = amountWithTaxRefund.subtract(totalGoodsAmount);
            createDataRow(sheet, rowNum++, "货款金额(CNY)", String.format("%,.2f", totalGoodsAmount), headerStyle);
            createDataRow(sheet, rowNum++, "退税金额(CNY)", String.format("%,.2f", taxRefundAmount), headerStyle);
            createDataRow(sheet, rowNum++, "含税总金额(CNY)", String.format("%,.2f", amountWithTaxRefund), headerStyle);

            // 逐项发票扣减
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> invoiceDeductionItems = (List<Map<String, Object>>) calcDetail.get("invoiceDeductionItems");
            if (invoiceDeductionItems != null && !invoiceDeductionItems.isEmpty()) {
                for (Map<String, Object> ded : invoiceDeductionItems) {
                    String dedName = ded.get("name") != null ? ded.get("name").toString() : "发票";
                    createDataRow(sheet, rowNum++, dedName + "扣减(CNY)", String.format("%,.2f", ded.get("amount")), headerStyle);
                }
            }
            createDataRow(sheet, rowNum++, "发票扣减合计(CNY)", String.format("%,.2f", calcDetail.get("totalInvoiceDeduction")), headerStyle);
            createDataRow(sheet, rowNum++, "银行手续费扣款(CNY)", String.format("%,.2f", calcDetail.get("bankFeeAmount")), headerStyle);
            createDataRow(sheet, rowNum++, "内部操作手续费扣款(CNY)", String.format("%,.2f", calcDetail.get("internalBankFee")), headerStyle);
            createDataRow(sheet, rowNum++, "手续费合计(CNY)", String.format("%,.2f", calcDetail.get("totalFeeAmount")), headerStyle);

            rowNum++;
            XSSFRow resultRow = sheet.createRow(rowNum++);
            resultRow.createCell(0).setCellValue("最终开票金额计算结果");
            resultRow.getCell(0).setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 3));

            createDataRow(sheet, rowNum++, "开票金额(CNY)", String.format("%,.2f", calcDetail.get("invoiceAmount")), headerStyle);
            
            rowNum++;

            // 商品明细
            XSSFRow productTitleRow = sheet.createRow(rowNum++);
            productTitleRow.createCell(0).setCellValue("商品明细");
            productTitleRow.getCell(0).setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));

            if (productTaxDetails != null && !productTaxDetails.isEmpty()) {
                // 计算总费用（发票扣减 + 手续费），按比例分摊到每个商品
                BigDecimal totalDeductions = BigDecimal.ZERO;
                if (calcDetail.get("totalInvoiceDeduction") instanceof BigDecimal) {
                    totalDeductions = totalDeductions.add((BigDecimal) calcDetail.get("totalInvoiceDeduction"));
                }
                if (calcDetail.get("totalFeeAmount") instanceof BigDecimal) {
                    totalDeductions = totalDeductions.add((BigDecimal) calcDetail.get("totalFeeAmount"));
                }
                BigDecimal totalProductAmountWithTax = (BigDecimal) calcDetail.get("amountWithTaxRefund");

                for (Map<String, Object> pd : productTaxDetails) {
                    String productName = pd.get("productName") != null ? pd.get("productName").toString() : "";
                    Integer qty = pd.get("quantity") != null ? ((Number) pd.get("quantity")).intValue() : null;
                    String unit = pd.get("unit") != null ? pd.get("unit").toString() : "";
                    BigDecimal productAmtWithTax = (BigDecimal) pd.get("amountWithTaxRefund");

                    // 按比例分摊费用：商品净金额 = 含税金额 - (含税金额 / 总含税金额 × 总费用)
                    BigDecimal netAmount = productAmtWithTax;
                    if (totalProductAmountWithTax != null && totalProductAmountWithTax.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal allocatedFee = totalDeductions
                                .multiply(productAmtWithTax.divide(totalProductAmountWithTax, 8, java.math.RoundingMode.HALF_UP))
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                        netAmount = productAmtWithTax.subtract(allocatedFee);
                    }

                    String qtyStr = qty != null ? qty + " " + unit : "-";
                    createDataRow(sheet, rowNum++, productName,
                            qtyStr + "  |  " + String.format("%,.2f CNY", netAmount), headerStyle);
                }
            }

            for(int i=0; i<4; i++) {
                sheet.autoSizeColumn(i);
            }

            // 将Excel写入ByteArrayOutputStream
            outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
                        
            // 使用UUID生成唯一文件名
            String originalFilename = "开票明细计算单_" + form.getFormNo() + ".xlsx";
            String extension = ".xlsx";
            String uuidFileName = java.util.UUID.randomUUID().toString() + extension;
                        
            // 创建日期子目录
            String dateDir = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
            String uploadDirPath = uploadPath + formNo + "/" + dateDir + "/";
                        
            File dir = new File(uploadDirPath);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    log.warn("无法创建导出目录: {}", uploadDirPath);
                }
            }
                        
            File tempFile = new File(dir, uuidFileName);
                        
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(outputStream.toByteArray());
            } catch (IOException e) {
                log.error("保存文件失败", e);
                // 继续执行，因为即使保存到磁盘失败，我们仍可以发送Excel数据给用户
                // 但记录错误以便后续处理
            }
                        
            // 构建相对路径
            String relativePath = formNo + "/" + dateDir + "/" + uuidFileName;
            String fileUrl = "/api/v1/files/download?path=" + relativePath;
                        
            // 更新财务补充记录
            if (supp != null) {
                try {
                    supp.setDetailsFileName(originalFilename);
                    supp.setDetailsFileUrl(fileUrl);
                    supp.setUpdateTime(LocalDateTime.now());
                    supplementService.updateById(supp);
                } catch (Exception e) {
                    log.error("更新财务补充记录失败", e);
                    // 继续执行，因为即使数据库更新失败，我们仍可以发送Excel数据给用户
                }
            }
                        
            // 返回下载链接，前端可以使用window.location.href来触发下载
            return Result.success(fileUrl);
                    
        } catch (Exception e) {
            log.error("导出开票明细失败", e);
            try {
                if (workbook != null) {
                    workbook.close();
                }
            } catch (Exception ignored) {}
            
            return Result.fail("导出失败: " + e.getMessage());
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ignored) {}
        }
    }

    @GetMapping("/form/{formId}/export-invoice-notification")
    @Operation(summary = "导出开票通知书")
    @RequiresPermissions("business:declaration:finance:supplement")
    public Result<String> exportInvoiceNotification(@PathVariable Long formId) {
        ByteArrayOutputStream outputStream = null;
        XSSFWorkbook workbook = null;
        try {
            DeclarationForm form = formService.getById(formId);
            if (form == null) {
                throw new RuntimeException("申报单不存在");
            }
            String formNo = form.getFormNo();

            Map<String, Object> calcDetail = supplementService.getCalculationDetail(formId);
            if (calcDetail == null) {
                throw new RuntimeException("未找到开票明细数据");
            }

            workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("开票通知书");

            // --- 样式 ---
            XSSFCellStyle boldCenter = workbook.createCellStyle();
            XSSFFont boldFont16 = workbook.createFont();
            boldFont16.setBold(true);
            boldFont16.setFontHeightInPoints((short) 16);
            boldCenter.setFont(boldFont16);
            boldCenter.setAlignment(HorizontalAlignment.CENTER);

            XSSFCellStyle boldCenter14 = workbook.createCellStyle();
            XSSFFont boldFont14 = workbook.createFont();
            boldFont14.setBold(true);
            boldFont14.setFontHeightInPoints((short) 14);
            boldCenter14.setFont(boldFont14);
            boldCenter14.setAlignment(HorizontalAlignment.CENTER);

            XSSFCellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            XSSFCellStyle rightStyle = workbook.createCellStyle();
            rightStyle.setAlignment(HorizontalAlignment.RIGHT);

            XSSFCellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            borderStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            borderStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            borderStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

            XSSFCellStyle boldLeft = workbook.createCellStyle();
            XSSFFont boldFont12 = workbook.createFont();
            boldFont12.setBold(true);
            boldFont12.setFontHeightInPoints((short) 12);
            boldLeft.setFont(boldFont12);

            int rowNum = 0;

            // === 1) 抬头区 ===
            EntityConfig entityConfig = form.getEntityId() != null ? entityConfigService.getById(form.getEntityId()) : null;
            String companyCn = (entityConfig != null && entityConfig.getEntityNameCn() != null) ? entityConfig.getEntityNameCn() : "-";
            XSSFRow r0 = sheet.createRow(rowNum++);
            XSSFCell c0 = r0.createCell(0);
            c0.setCellValue(companyCn);
            c0.setCellStyle(boldCenter);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            XSSFRow r1 = sheet.createRow(rowNum++);
            XSSFCell c1 = r1.createCell(0);
            c1.setCellValue("NINGBO KINGBOND FOREIGN TRADE SERVICE CORP");
            c1.setCellStyle(centerStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

            XSSFRow r2 = sheet.createRow(rowNum++);
            XSSFCell c2 = r2.createCell(0);
            c2.setCellValue("开票通知书");
            c2.setCellStyle(boldCenter14);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 5));

            XSSFRow r3 = sheet.createRow(rowNum++);
            String docNo = form.getInvoiceNo() != null ? form.getInvoiceNo() : "-";
            String dateStr = form.getDeclarationDate() != null
                    ? form.getDeclarationDate().toString() : java.time.LocalDate.now().toString();
            XSSFCell c3 = r3.createCell(0);
            c3.setCellValue("编号: " + docNo + "    DATE: " + dateStr);
            c3.setCellStyle(rightStyle);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 5));

            rowNum++; // 空行

            // === 2) 购货/销货单位 ===
            String buyerName = companyCn;
            String sellerName = form.getConsigneeCompany() != null ? form.getConsigneeCompany() : "-";
            PartyBConfig partyB = getPartyB(form);
            // 已选乙方则优先用乙方配置，未选保持历史口径（名称回退收货人、其余为 -）
            if (partyB != null && !isBlank(partyB.getPartyBName())) {
                sellerName = partyB.getPartyBName().trim();
            }
            String sellerTaxId = partyB != null && !isBlank(partyB.getTaxId()) ? partyB.getTaxId().trim() : "-";
            String sellerAddrPhone = partyB != null
                    ? defaultDash(joinWithSpace(partyB.getPartyBAddress(), partyB.getContactPhone())) : "-";
            String sellerBankInfo = partyB != null
                    ? defaultDash(joinWithSpace(partyB.getBankName(), partyB.getBankAccount())) : "-";
            String buyerTaxId = (entityConfig != null && entityConfig.getTaxId() != null) ? entityConfig.getTaxId() : "-";
            String buyerPhone = (entityConfig != null && entityConfig.getPhone() != null) ? entityConfig.getPhone() : "-";
            String buyerBank = (entityConfig != null && entityConfig.getBankAccount() != null) ? entityConfig.getBankAccount() : "-";
            String buyerAddr = (entityConfig != null && entityConfig.getEntityAddressCn() != null) ? entityConfig.getEntityAddressCn() : "-";

            XSSFRow buyerTitle = sheet.createRow(rowNum++);
            buyerTitle.createCell(0).setCellValue("购货单位");
            buyerTitle.getCell(0).setCellStyle(boldLeft);
            XSSFRow buyerNameRow = sheet.createRow(rowNum++);
            buyerNameRow.createCell(0).setCellValue("名称: " + buyerName);
            XSSFRow buyerTaxRow = sheet.createRow(rowNum++);
            buyerTaxRow.createCell(0).setCellValue("纳税人识别号: " + buyerTaxId);
            XSSFRow buyerAddrRow = sheet.createRow(rowNum++);
            buyerAddrRow.createCell(0).setCellValue("地址、电话: " + buyerAddr + " " + buyerPhone);
            XSSFRow buyerBankRow = sheet.createRow(rowNum++);
            buyerBankRow.createCell(0).setCellValue("开户行及账号: " + buyerBank);

            rowNum++; // 空行

            XSSFRow sellerTitle = sheet.createRow(rowNum++);
            sellerTitle.createCell(0).setCellValue("销货单位");
            sellerTitle.getCell(0).setCellStyle(boldLeft);
            XSSFRow sellerNameRow = sheet.createRow(rowNum++);
            sellerNameRow.createCell(0).setCellValue("名称: " + sellerName);
            XSSFRow sellerTaxRow = sheet.createRow(rowNum++);
            sellerTaxRow.createCell(0).setCellValue("纳税人识别号: " + sellerTaxId);
            XSSFRow sellerAddrRow = sheet.createRow(rowNum++);
            sellerAddrRow.createCell(0).setCellValue("地址、电话: " + sellerAddrPhone);
            XSSFRow sellerBankRow = sheet.createRow(rowNum++);
            sellerBankRow.createCell(0).setCellValue("开户行及账号: " + sellerBankInfo);

            rowNum++; // 空行

            // === 3) 商品明细表 ===
            XSSFRow prodHeaderRow = sheet.createRow(rowNum++);
            String[] headers = {"货物名称", "规格型号", "数量", "单位", "含税单价", "含税金额"};
            for (int i = 0; i < headers.length; i++) {
                XSSFCell hc = prodHeaderRow.createCell(i);
                hc.setCellValue(headers[i]);
                hc.setCellStyle(borderStyle);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> productTaxDetails = (List<Map<String, Object>>) calcDetail.get("productTaxDetails");
            BigDecimal totalAmount = BigDecimal.ZERO;

            if (productTaxDetails != null && !productTaxDetails.isEmpty()) {
                for (Map<String, Object> pd : productTaxDetails) {
                    XSSFRow pRow = sheet.createRow(rowNum++);
                    String pName = pd.get("productName") != null ? pd.get("productName").toString() : "-";
                    Integer qty = pd.get("quantity") != null ? ((Number) pd.get("quantity")).intValue() : null;
                    String unit = pd.get("unit") != null ? pd.get("unit").toString() : "个";
                    // 使用退税加成后金额作为含税金额，与开票金额一致
                    BigDecimal inclTaxAmt = pd.get("amountWithTaxRefund") instanceof BigDecimal ? (BigDecimal) pd.get("amountWithTaxRefund") : BigDecimal.ZERO;
                    BigDecimal unitPrice = (qty != null && qty > 0 && inclTaxAmt.compareTo(BigDecimal.ZERO) > 0)
                            ? inclTaxAmt.divide(new BigDecimal(qty), 10, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

                    pRow.createCell(0).setCellValue(pName);
                    pRow.createCell(1).setCellValue("-");
                    pRow.createCell(2).setCellValue(qty != null ? qty : 0);
                    pRow.createCell(3).setCellValue(unit);
                    pRow.createCell(4).setCellValue(unitPrice.doubleValue());
                    pRow.createCell(5).setCellValue(inclTaxAmt.doubleValue());
                    totalAmount = totalAmount.add(inclTaxAmt);
                }
            }

            // 中文大写金额行
            XSSFRow wordsRow = sheet.createRow(rowNum++);
            wordsRow.createCell(0).setCellValue("合计金额(大写): " + convertAmountToChineseWords(totalAmount));
            wordsRow.getCell(0).setCellStyle(boldLeft);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 5));

            rowNum++; // 空行

            // === 4) 开票金额计算（单证不体现扣款明细） ===
            BigDecimal amountWithTaxRefund = calcDetail.get("amountWithTaxRefund") instanceof BigDecimal
                    ? (BigDecimal) calcDetail.get("amountWithTaxRefund") : BigDecimal.ZERO;
            BigDecimal invoiceBase = amountWithTaxRefund.multiply(new BigDecimal("0.8")).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal totalDeduction = calcDetail.get("totalInvoiceDeduction") instanceof BigDecimal
                    ? (BigDecimal) calcDetail.get("totalInvoiceDeduction") : BigDecimal.ZERO;
            BigDecimal totalFeeAmount = calcDetail.get("totalFeeAmount") instanceof BigDecimal
                    ? (BigDecimal) calcDetail.get("totalFeeAmount") : BigDecimal.ZERO;
            BigDecimal deductTotal = totalDeduction.add(totalFeeAmount);
            BigDecimal fileInvoiceAmt = invoiceBase.subtract(deductTotal).setScale(2, java.math.RoundingMode.HALF_UP);

            XSSFRow calcTitle = sheet.createRow(rowNum++);
            calcTitle.createCell(0).setCellValue("开票金额计算");
            calcTitle.getCell(0).setCellStyle(boldLeft);

            XSSFRow taxRefundRow = sheet.createRow(rowNum++);
            taxRefundRow.createCell(0).setCellValue(String.format("退税加成合计: %.2f CNY", amountWithTaxRefund.doubleValue()));

            XSSFRow invoiceRow = sheet.createRow(rowNum++);
            invoiceRow.createCell(0).setCellValue(String.format("开票金额: %.2f CNY", fileInvoiceAmt.doubleValue()));
            invoiceRow.getCell(0).setCellStyle(boldLeft);

            rowNum++; // 空行

            // === 5) 外销发票号 + 注意事项 ===
            XSSFRow invNoRow = sheet.createRow(rowNum++);
            invNoRow.createCell(0).setCellValue("外销发票号: " + (form.getInvoiceNo() != null ? form.getInvoiceNo() : "-"));
            invNoRow.getCell(0).setCellStyle(boldLeft);

            rowNum++;
            XSSFRow noteTitle = sheet.createRow(rowNum++);
            noteTitle.createCell(0).setCellValue("注意事项:");
            noteTitle.getCell(0).setCellStyle(boldLeft);

            XSSFRow note1 = sheet.createRow(rowNum++);
            note1.createCell(0).setCellValue("1. 请勿更改货物名称、单位、数量及增值税发票各项内容。");
            XSSFRow note2 = sheet.createRow(rowNum++);
            note2.createCell(0).setCellValue("2. 正确增值税专用发票务必于收到本通知书后5个工作日内送达。");
            XSSFRow note3 = sheet.createRow(rowNum++);
            note3.createCell(0).setCellValue("3. 开具发票时请随附购货方联的原始购货合同。");

            // 列宽调整
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 4000) {
                    sheet.setColumnWidth(i, 4000);
                }
            }

            // 写入输出流
            outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            // 保存文件
            String originalFilename = "开票通知书_" + form.getFormNo() + ".xlsx";
            String uuidFileName = java.util.UUID.randomUUID().toString() + ".xlsx";
            String dateDir = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
            String uploadDirPath = uploadPath + formNo + "/" + dateDir + "/";

            File dir = new File(uploadDirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File tempFile = new File(dir, uuidFileName);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(outputStream.toByteArray());
            } catch (IOException e) {
                log.error("保存开票通知书文件失败", e);
            }

            String relativePath = formNo + "/" + dateDir + "/" + uuidFileName;
            String fileUrl = "/api/v1/files/download?path=" + relativePath;

            return Result.success(fileUrl);

        } catch (Exception e) {
            log.error("导出开票通知书失败", e);
            try {
                if (workbook != null) workbook.close();
            } catch (Exception ignored) {}
            return Result.fail("导出失败: " + e.getMessage());
        } finally {
            try {
                if (outputStream != null) outputStream.close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * 金额转中文大写（如：伍仟柒佰捌拾壹圆伍角捌分）
     */
    private String convertAmountToChineseWords(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return "零圆整";
        String[] cnNums = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
        String[] cnIntUnits = {"", "拾", "佰", "仟"};
        String[] cnBigUnits = {"", "万", "亿", "兆"};
        String[] cnDecUnits = {"角", "分"};

        long intPart = amount.longValue();
        int decPart = amount.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        if (decPart < 0) decPart = -decPart;

        StringBuilder result = new StringBuilder();
        if (intPart > 0) {
            String intStr = String.valueOf(intPart);
            int len = intStr.length();
            for (int i = 0; i < len; i++) {
                int digit = intStr.charAt(i) - '0';
                int pos = len - 1 - i;
                int unitIdx = pos % 4;
                int bigIdx = pos / 4;
                if (digit != 0) {
                    result.append(cnNums[digit]).append(cnIntUnits[unitIdx]);
                } else if (result.length() > 0 && !result.toString().endsWith("零")) {
                    result.append("零");
                }
                if (unitIdx == 0 && bigIdx > 0 && bigIdx < cnBigUnits.length) {
                    // 只在非零段末尾加大单位
                    String tail = result.toString();
                    if (!tail.isEmpty() && !tail.endsWith("零")) {
                        result.append(cnBigUnits[bigIdx]);
                    }
                }
            }
            result.append("圆");
        }

        if (decPart > 0) {
            int jiao = decPart / 10;
            int fen = decPart % 10;
            if (jiao > 0) result.append(cnNums[jiao]).append(cnDecUnits[0]);
            if (fen > 0) result.append(cnNums[fen]).append(cnDecUnits[1]);
        } else {
            result.append("整");
        }
        return result.toString();
    }

    @PostMapping("/form/{formId}/export-invoice-package")
    @Operation(summary = "下载开票文件包(开票通知书+合同)")
    public Result<String> exportInvoicePackage(@PathVariable Long formId) {
        try {
            DeclarationForm form = formService.getById(formId);
            if (form == null) throw new RuntimeException("申报单不存在");
            String formNo = form.getFormNo();

            Map<String, Object> calcDetail = supplementService.getCalculationDetail(formId);
            if (calcDetail == null) throw new RuntimeException("未找到开票明细数据");

            // === 计算原始开票金额（统一基数） ===
            BigDecimal origAmountWithTaxRefund = calcDetail.get("amountWithTaxRefund") instanceof BigDecimal
                    ? (BigDecimal) calcDetail.get("amountWithTaxRefund") : BigDecimal.ZERO;
            BigDecimal origDeduction = calcDetail.get("totalInvoiceDeduction") instanceof BigDecimal
                    ? (BigDecimal) calcDetail.get("totalInvoiceDeduction") : BigDecimal.ZERO;
            BigDecimal origFee = calcDetail.get("totalFeeAmount") instanceof BigDecimal
                    ? (BigDecimal) calcDetail.get("totalFeeAmount") : BigDecimal.ZERO;
            BigDecimal originalInvoiceAmount = origAmountWithTaxRefund.subtract(origDeduction).subtract(origFee)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            // === 构建开票文档 calcDetail（金额口径按比例缩放，不拆分产品） ===
            Map<String, Object> invoiceCalcDetail = buildScaledCalcDetail(calcDetail, new BigDecimal("0.8"), originalInvoiceAmount);

            // 1) 开票通知书
            byte[] notification = generateNotificationWord(form, invoiceCalcDetail);
            // 2) 合同
            byte[] contract = generateContractWord(form, invoiceCalcDetail);

            // 3) 打包 ZIP
            String uuidFileName = java.util.UUID.randomUUID().toString() + ".zip";
            String dateDir = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
            String uploadDirPath = uploadPath + formNo + "/" + dateDir + "/";
            File dir = new File(uploadDirPath);
            if (!dir.exists()) dir.mkdirs();

            File zipFile = new File(dir, uuidFileName);
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                zos.putNextEntry(new ZipEntry("开票通知书_" + formNo + ".docx"));
                zos.write(notification); zos.closeEntry();
                zos.putNextEntry(new ZipEntry("合同_" + formNo + ".docx"));
                zos.write(contract); zos.closeEntry();
            }

            String relativePath = formNo + "/" + dateDir + "/" + uuidFileName;
            String fileUrl = "/api/v1/files/download?path=" + relativePath;
            return Result.success(fileUrl);

        } catch (Exception e) {
            log.error("导出开票文件包失败", e);
            return Result.fail("导出失败: " + e.getMessage());
        }
    }

    /** 构建按比例缩放的 calcDetail（用于80%文档）
     *  统一基数：originalInvoiceAmount × scale
     *  产品含税合计 = 开票金额（扣减不吸收进产品，仅用于展示）
     */
    private Map<String, Object> buildScaledCalcDetail(Map<String, Object> original, BigDecimal scale, BigDecimal originalInvoiceAmount) {
        Map<String, Object> result = new java.util.LinkedHashMap<>(original);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) original.get("productTaxDetails");
        if (products != null && !products.isEmpty()) {
            // 目标开票金额 = 原始开票金额 × scale
            BigDecimal targetInvoiceAmount = originalInvoiceAmount.multiply(scale).setScale(2, java.math.RoundingMode.HALF_UP);
            // 80%的扣减 + 全额手续费（仅用于展示）
            BigDecimal origDeduction = original.get("totalInvoiceDeduction") instanceof BigDecimal
                    ? (BigDecimal) original.get("totalInvoiceDeduction") : BigDecimal.ZERO;
            BigDecimal scaledDeduction = origDeduction.multiply(scale).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal fullFeeAmount = original.get("totalFeeAmount") instanceof BigDecimal
                    ? (BigDecimal) original.get("totalFeeAmount") : BigDecimal.ZERO;
            // 产品含税合计 = 开票金额
            BigDecimal productTarget = targetInvoiceAmount;

            BigDecimal originalAmountWithTaxRefund = original.get("amountWithTaxRefund") instanceof BigDecimal
                    ? (BigDecimal) original.get("amountWithTaxRefund") : BigDecimal.ZERO;

            List<Map<String, Object>> scaledProducts = new ArrayList<>();
            BigDecimal totalCnyAmount = BigDecimal.ZERO;
            BigDecimal totalAmountWithTaxRefund = BigDecimal.ZERO;

            for (int i = 0; i < products.size(); i++) {
                Map<String, Object> pd = products.get(i);
                Map<String, Object> sp = new java.util.LinkedHashMap<>(pd);
                BigDecimal amtWithTax = pd.get("amountWithTaxRefund") instanceof BigDecimal ? (BigDecimal) pd.get("amountWithTaxRefund") : BigDecimal.ZERO;
                BigDecimal cnyAmt = pd.get("cnyAmount") instanceof BigDecimal ? (BigDecimal) pd.get("cnyAmount") : amtWithTax;

                // 按比例分配 productTarget，最后一个产品差额补齐
                BigDecimal finalAmtWithTax;
                if (i == products.size() - 1) {
                    finalAmtWithTax = productTarget.subtract(totalAmountWithTaxRefund);
                } else {
                    BigDecimal ratio = originalAmountWithTaxRefund.compareTo(BigDecimal.ZERO) > 0
                            ? amtWithTax.divide(originalAmountWithTaxRefund, 10, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    finalAmtWithTax = productTarget.multiply(ratio).setScale(2, java.math.RoundingMode.HALF_UP);
                }

                // cnyAmount 按同比缩放
                BigDecimal ratioCny = (amtWithTax.compareTo(BigDecimal.ZERO) > 0)
                        ? cnyAmt.divide(amtWithTax, 10, java.math.RoundingMode.HALF_UP) : BigDecimal.ONE;
                BigDecimal scaledCny = finalAmtWithTax.multiply(ratioCny).setScale(2, java.math.RoundingMode.HALF_UP);

                sp.put("cnyAmount", scaledCny);
                sp.put("amountWithTaxRefund", finalAmtWithTax);
                Number qtyNum = pd.get("quantity") instanceof Number ? (Number) pd.get("quantity") : 0;
                BigDecimal qtyBD = new BigDecimal(qtyNum.toString());
                if (qtyBD.compareTo(BigDecimal.ZERO) > 0) {
                    sp.put("unitPrice", finalAmtWithTax.divide(qtyBD, 10, java.math.RoundingMode.HALF_UP));
                }
                scaledProducts.add(sp);
                totalCnyAmount = totalCnyAmount.add(scaledCny);
                totalAmountWithTaxRefund = totalAmountWithTaxRefund.add(finalAmtWithTax);
            }
            result.put("productTaxDetails", scaledProducts);
            result.put("totalGoodsAmount", totalCnyAmount);
            result.put("amountWithTaxRefund", totalAmountWithTaxRefund);
            result.put("totalCny", totalCnyAmount);
            // 扣减和手续费仅用于文档展示
            result.put("totalInvoiceDeduction", scaledDeduction);
            result.put("totalFeeAmount", fullFeeAmount);
            result.put("bankFeeAmount", original.get("bankFeeAmount") instanceof BigDecimal ? (BigDecimal) original.get("bankFeeAmount") : BigDecimal.ZERO);
            result.put("internalBankFee", original.get("internalBankFee") instanceof BigDecimal ? (BigDecimal) original.get("internalBankFee") : BigDecimal.ZERO);
            if (original.get("totalOriginalAmount") instanceof BigDecimal)
                result.put("totalOriginalAmount", ((BigDecimal) original.get("totalOriginalAmount")).multiply(scale).setScale(2, java.math.RoundingMode.HALF_UP));
            result.put("invoiceAmount", targetInvoiceAmount);
        }
        return result;
    }

    /** 生成开票通知书 Word 文档字节数组 */
    private byte[] generateNotificationWord(DeclarationForm form, Map<String, Object> calcDetail) throws Exception {
        XWPFDocument doc = new XWPFDocument();
        try {
            // 从主体配置获取中文名和英文名
            EntityConfig entityConfig = form.getEntityId() != null ? entityConfigService.getById(form.getEntityId()) : null;
            String companyCn = (entityConfig != null && entityConfig.getEntityNameCn() != null) ? entityConfig.getEntityNameCn() : "-";
            String companyEn = form.getShipperCompany() != null ? form.getShipperCompany() : "-";
            // 抬头
            addWordParagraph(doc, companyCn, true, 16, ParagraphAlignment.CENTER);
            addWordParagraph(doc, companyEn, false, 10, ParagraphAlignment.CENTER);
            addWordParagraph(doc, "开票通知书", true, 14, ParagraphAlignment.CENTER);
            String dateStr = java.time.LocalDate.now().toString();
            addWordParagraph(doc, "编号: " + (form.getInvoiceNo() != null ? form.getInvoiceNo() : "-") + "    DATE: " + dateStr, false, 10, ParagraphAlignment.RIGHT);
            addWordParagraph(doc, "", false, 10, ParagraphAlignment.LEFT);

            // 购货/销货单位左右分栏，与合同保持同一阅读口径（销货方信息留空）
            String buyerTaxId = (entityConfig != null && entityConfig.getTaxId() != null) ? entityConfig.getTaxId() : "-";
            String buyerPhone = (entityConfig != null && entityConfig.getPhone() != null) ? entityConfig.getPhone() : "-";
            String buyerBank = (entityConfig != null && entityConfig.getBankAccount() != null) ? entityConfig.getBankAccount() : "-";
            String buyerAddr = (entityConfig != null && entityConfig.getEntityAddressCn() != null) ? entityConfig.getEntityAddressCn() : "-";
            String buyerName = companyCn;
            List<String> buyerLines = new ArrayList<>();
            buyerLines.add("购货单位");
            buyerLines.add("名称: " + buyerName);
            buyerLines.add("纳税人识别号: " + buyerTaxId);
            buyerLines.add("地址、电话: " + buyerAddr + " " + buyerPhone);
            buyerLines.add("开户行及账号: " + buyerBank);

            // 未关联乙方配置时保持原有留空口径，不影响历史单证
            PartyBConfig partyB = getPartyB(form);
            List<String> sellerLines = new ArrayList<>();
            sellerLines.add("销货单位");
            sellerLines.add("名称: " + partyBText(partyB, PartyBConfig::getPartyBName));
            sellerLines.add("纳税人识别号: " + partyBText(partyB, PartyBConfig::getTaxId));
            sellerLines.add("地址、电话: " + (partyB == null ? ""
                    : defaultEmpty(joinWithSpace(partyB.getPartyBAddress(), partyB.getContactPhone()))));
            sellerLines.add("开户行及账号: " + (partyB == null ? ""
                    : defaultEmpty(joinWithSpace(partyB.getBankName(), partyB.getBankAccount()))));
            addPartyColumnTable(doc, buyerLines, sellerLines);
            addWordParagraph(doc, "", false, 10, ParagraphAlignment.LEFT);

            // 商品明细表
            String[] headers = {"货物名称", "规格型号", "数量", "单位", "含税单价", "含税金额"};
            XWPFTable table = doc.createTable(1, headers.length);
            setTableWidth(table, 9000);
            XWPFTableRow hRow = table.getRow(0);
            for (int i = 0; i < headers.length; i++) {
                setCellText(hRow.getCell(i), headers[i], true, 10);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> products = (List<Map<String, Object>>) calcDetail.get("productTaxDetails");
            BigDecimal totalAmount = BigDecimal.ZERO;
            if (products != null) {
                for (Map<String, Object> pd : products) {
                    XWPFTableRow pRow = table.createRow();
                    String pName = pd.get("productName") != null ? pd.get("productName").toString() : "-";
                    String spec = pd.get("spec") != null ? pd.get("spec").toString() : "-";
                    Number qtyNum = pd.get("quantity") instanceof Number ? (Number) pd.get("quantity") : null;
                    BigDecimal qtyBD = qtyNum != null ? new BigDecimal(qtyNum.toString()) : BigDecimal.ZERO;
                    String unit = pd.get("unit") != null ? pd.get("unit").toString() : "个";
                    // 使用退税加成后金额作为含税金额，与开票金额一致
                    BigDecimal inclTaxAmt = pd.get("amountWithTaxRefund") instanceof BigDecimal ? (BigDecimal) pd.get("amountWithTaxRefund") : BigDecimal.ZERO;
                    BigDecimal unitPrice = qtyBD.compareTo(BigDecimal.ZERO) > 0
                            ? inclTaxAmt.divide(qtyBD, 10, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    setCellText(pRow.getCell(0), pName, false, 10);
                    setCellText(pRow.getCell(1), spec, false, 10);
                    setCellText(pRow.getCell(2), qtyBD.stripTrailingZeros().toPlainString(), false, 10);
                    setCellText(pRow.getCell(3), unit, false, 10);
                    setCellText(pRow.getCell(4), unitPrice.toPlainString(), false, 10);
                    setCellText(pRow.getCell(5), inclTaxAmt.toPlainString(), false, 10);
                    totalAmount = totalAmount.add(inclTaxAmt);
                }
            }
            addWordParagraph(doc, "", false, 6, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "合计金额(大写): " + convertAmountToChineseWords(totalAmount), true, 12, ParagraphAlignment.LEFT);

            // 开票金额（单证不体现扣款明细）
            BigDecimal fileInvoiceAmt = calcDetail.get("invoiceAmount") instanceof BigDecimal ? (BigDecimal) calcDetail.get("invoiceAmount") : BigDecimal.ZERO;
            addWordParagraph(doc, "", false, 6, ParagraphAlignment.LEFT);
            addWordParagraph(doc, String.format("开票金额: %.2f CNY", fileInvoiceAmt.doubleValue()), true, 11, ParagraphAlignment.LEFT);

            // 外销发票号 + 注意事项
            addWordParagraph(doc, "外销发票号: " + (form.getInvoiceNo() != null ? form.getInvoiceNo() : "-"), true, 12, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "", false, 6, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "注意事项:", true, 12, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "1. 请勿更改货物名称、单位、数量及增值税发票各项内容。", false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "2. 正确增值税专用发票务必于收到本通知书后5个工作日内送达。", false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "3. 开具发票时请随附购货方联的原始购货合同。", false, 10, ParagraphAlignment.LEFT);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            doc.write(bos);
            return bos.toByteArray();
        } finally {
            try { doc.close(); } catch (Exception ignored) {}
        }
    }

    /** 生成合同 Word 文档字节数组 */
    private byte[] generateContractWord(DeclarationForm form, Map<String, Object> calcDetail) throws Exception {
        XWPFDocument doc = new XWPFDocument();
        try {
            // 从主体配置获取中文名（甲方联系人信息用）
            EntityConfig entityConfig = form.getEntityId() != null ? entityConfigService.getById(form.getEntityId()) : null;
            String companyCn = (entityConfig != null && entityConfig.getEntityNameCn() != null) ? entityConfig.getEntityNameCn() : "-";
            // 抬头：不再打印公司名称与页码，直接以合同标题起头
            addWordParagraph(doc, "购 货 合 同", true, 18, ParagraphAlignment.CENTER);
            addWordParagraph(doc, "", false, 10, ParagraphAlignment.LEFT);

            // 合同信息
            addWordParagraph(doc, "合同编号: " + (form.getInvoiceNo() != null ? form.getInvoiceNo() : "-"), false, 11, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "签订地点: 宁波", false, 11, ParagraphAlignment.LEFT);
            String signDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
            addWordParagraph(doc, "签订时间: " + signDate, false, 11, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "", false, 10, ParagraphAlignment.LEFT);

            // 甲方（本公司）：与开票通知书口径一致，只打印名称、地址、电话，联系人不再出现
            String partyAPhone = (entityConfig != null && !isBlank(entityConfig.getPhone())) ? entityConfig.getPhone().trim() : "-";
            String partyAAddress = (entityConfig != null && !isBlank(entityConfig.getEntityAddressCn()))
                    ? entityConfig.getEntityAddressCn().trim()
                    : (entityConfig != null && !isBlank(entityConfig.getEntityAddress())) ? entityConfig.getEntityAddress().trim() : "-";
            List<String> partyALines = new ArrayList<>();
            partyALines.add("甲方: " + companyCn);
            partyALines.add("地址: " + partyAAddress);
            partyALines.add("电话: " + partyAPhone);

            // 乙方（取自乙方配置，未关联时保持原有留空口径）
            PartyBConfig partyB = getPartyB(form);
            List<String> partyBLines = new ArrayList<>();
            partyBLines.add("乙方: " + partyBText(partyB, PartyBConfig::getPartyBName));
            if (partyB != null && !isBlank(partyB.getPartyBAddress())) {
                partyBLines.add("地址: " + partyB.getPartyBAddress().trim());
            }
            // 乙方是供货方，联系人属于履约信息，单证上需要保留
            partyBLines.add("联系人: " + partyBText(partyB, PartyBConfig::getContactPerson));
            partyBLines.add("电话: " + partyBText(partyB, PartyBConfig::getContactPhone));
            addPartyColumnTable(doc, partyALines, partyBLines);
            addWordParagraph(doc, "", false, 10, ParagraphAlignment.LEFT);

            // 商品表
            String[] headers = {"货物名称", "数量", "单位", "含税单价", "不含税金额", "含税金额"};
            XWPFTable table = doc.createTable(1, headers.length);
            setTableWidth(table, 9000);
            XWPFTableRow hRow = table.getRow(0);
            for (int i = 0; i < headers.length; i++) {
                setCellText(hRow.getCell(i), headers[i], true, 10);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> products = (List<Map<String, Object>>) calcDetail.get("productTaxDetails");
            BigDecimal totalInclTax = BigDecimal.ZERO;
            BigDecimal totalExclTax = BigDecimal.ZERO;
            if (products != null) {
                for (Map<String, Object> pd : products) {
                    XWPFTableRow pRow = table.createRow();
                    String pName = pd.get("productName") != null ? pd.get("productName").toString() : "-";
                    Number qtyNum = pd.get("quantity") instanceof Number ? (Number) pd.get("quantity") : null;
                    BigDecimal qtyBD = qtyNum != null ? new BigDecimal(qtyNum.toString()) : BigDecimal.ZERO;
                    String unit = pd.get("unit") != null ? pd.get("unit").toString() : "个";
                    // 使用退税加成后金额作为含税金额，与开票金额一致
                    BigDecimal inclTaxAmt = pd.get("amountWithTaxRefund") instanceof BigDecimal ? (BigDecimal) pd.get("amountWithTaxRefund") : BigDecimal.ZERO;
                    BigDecimal unitPrice = qtyBD.compareTo(BigDecimal.ZERO) > 0
                            ? inclTaxAmt.divide(qtyBD, 10, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    BigDecimal exclTax = inclTaxAmt.divide(new BigDecimal("1.13"), 2, java.math.RoundingMode.HALF_UP);
                    setCellText(pRow.getCell(0), pName, false, 10);
                    setCellText(pRow.getCell(1), qtyBD.stripTrailingZeros().toPlainString(), false, 10);
                    setCellText(pRow.getCell(2), unit, false, 10);
                    setCellText(pRow.getCell(3), unitPrice.toPlainString(), false, 10);
                    setCellText(pRow.getCell(4), exclTax.toPlainString(), false, 10);
                    setCellText(pRow.getCell(5), inclTaxAmt.toPlainString(), false, 10);
                    totalInclTax = totalInclTax.add(inclTaxAmt);
                    totalExclTax = totalExclTax.add(exclTax);
                }
            }
            // 合计行
            XWPFTableRow totalRow = table.createRow();
            setCellText(totalRow.getCell(0), "总金额(大写): " + convertAmountToChineseWords(totalInclTax), true, 10);
            setCellText(totalRow.getCell(1), "", false, 10);
            setCellText(totalRow.getCell(2), "", false, 10);
            setCellText(totalRow.getCell(3), "", false, 10);
            setCellText(totalRow.getCell(4), totalExclTax.toPlainString(), false, 10);
            setCellText(totalRow.getCell(5), totalInclTax.toPlainString(), false, 10);

            addWordParagraph(doc, "", false, 6, ParagraphAlignment.LEFT);

            // 附加说明
            addWordParagraph(doc, "产品要求: 按客户要求。", false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "封箱要求: 标志出口货物包装要求。", false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "", false, 10, ParagraphAlignment.LEFT);

            // 合同条款
            addWordParagraph(doc, "合同条款:", true, 12, ParagraphAlignment.LEFT);
            String[] clauses = {
                "1. 甲方签署后3日内必须将原件寄回乙方，合同经双方签章后生效。",
                "2. 乙方必须按时交货，延期交货甲方有权拒收或收货后果自负，所有损失由乙方承担。",
                "3. 乙方保证无知识产权或商业秘密侵权，无论货值大小，一切损失由乙方承担。",
                "4. 最终检验以权威第三方或甲方指定实验室报告为准，报告对双方具有约束力。",
                "5. 包装及质量按合同要求，签约后3日内提供样品，甲方确认后方可批量生产。",
                "6. 争议双方友好协商解决，协商不成提交甲方所在地法院诉讼。"
            };
            for (String c : clauses) {
                addWordParagraph(doc, c, false, 10, ParagraphAlignment.LEFT);
            }
            addWordParagraph(doc, "", false, 10, ParagraphAlignment.LEFT);

            // 签章区
            addWordParagraph(doc, "甲方(签章): ____________          乙方(签章): ____________", false, 11, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "法人代表: ____________            法人代表: ____________", false, 11, ParagraphAlignment.LEFT);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            doc.write(bos);
            return bos.toByteArray();
        } finally {
            try { doc.close(); } catch (Exception ignored) {}
        }
    }

    /** 读取申报单关联的乙方配置（未关联返回 null，单证保持原有留空口径） */
    private PartyBConfig getPartyB(DeclarationForm form) {
        return form.getPartyBId() != null ? partyBConfigService.getById(form.getPartyBId()) : null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** 取乙方单个字段，未配置返回空串（用于原本就留空的单证行） */
    private String partyBText(PartyBConfig partyB, java.util.function.Function<PartyBConfig, String> getter) {
        if (partyB == null) return "";
        String v = getter.apply(partyB);
        return isBlank(v) ? "" : v.trim();
    }

    /** 两段文本用空格拼接，全空返回 null */
    private String joinWithSpace(String first, String second) {
        String a = isBlank(first) ? "" : first.trim();
        String b = isBlank(second) ? "" : second.trim();
        if (a.isEmpty()) return b.isEmpty() ? null : b;
        return b.isEmpty() ? a : a + " " + b;
    }

    private String defaultDash(String v) {
        return v == null ? "-" : v;
    }

    private String defaultEmpty(String v) {
        return v == null ? "" : v;
    }

    /** Word 段落工具方法 */
    private void addWordParagraph(XWPFDocument doc, String text, boolean bold, int fontSize, ParagraphAlignment alignment) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(alignment);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(fontSize);
        run.setFontFamily("宋体");
    }

    /** Word 表格单元格设置 */
    private void setCellText(XWPFTableCell cell, String text, boolean bold, int fontSize) {
        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(fontSize);
        run.setFontFamily("宋体");
    }

    /** 设置表格宽度 */
    private void setTableWidth(XWPFTable table, int width) {
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) tblPr = table.getCTTbl().addNewTblPr();
        // POI 建表时已自带 tblW(auto)，只能改写不能新增：同一位置重复元素不合法，Word 会拼开文件
        CTTblWidth tblWidth = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
        tblWidth.setW(java.math.BigInteger.valueOf(width));
        tblWidth.setType(STTblWidth.DXA);
    }

    /**
     * 甲乙双方（购销单位）左右分栏：用一个两列无边框表格承载两段信息。
     * 原本上下堆叠时甲方会把乙方挤到下一页，签字时得翻页对照；分栏后单页就能看全双方。
     */
    private void addPartyColumnTable(XWPFDocument doc, List<String> leftLines, List<String> rightLines) {
        XWPFTable table = doc.createTable(1, 2);
        setTableWidth(table, 9000);
        fixTableLayout(table);
        removeTableBorders(table);
        XWPFTableRow row = table.getRow(0);
        fillPartyColumn(row.getCell(0), leftLines, 4500);
        fillPartyColumn(row.getCell(1), rightLines, 4500);
    }

    /** 细栏单元格写法：首行当作甲方/乙方抬头加粗，其余行正常字号 */
    private void fillPartyColumn(XWPFTableCell cell, List<String> lines, int width) {
        setCellWidth(cell, width);
        // 单元格自带一个空段落，首行直接复用它，否则开头会多出一行空白
        for (int i = 0; i < lines.size(); i++) {
            XWPFParagraph paragraph = i == 0 ? cell.getParagraphs().get(0) : cell.addParagraph();
            paragraph.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun run = paragraph.createRun();
            run.setText(lines.get(i));
            run.setBold(i == 0);
            run.setFontSize(i == 0 ? 12 : 10);
            run.setFontFamily("宋体");
        }
    }

    /** 去掉表格边框：分栏表格只用于排版，带网格线会被当成未做完的表格 */
    private void removeTableBorders(XWPFTable table) {
        CTTblPr tblPr = table.getCTTbl().getTblPr() != null ? table.getCTTbl().getTblPr() : table.getCTTbl().addNewTblPr();
        CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
        // POI 新建的表已自带 single 边框，必须改现有节点而不是再插一个
        setBorderNone(borders.isSetTop() ? borders.getTop() : borders.addNewTop());
        setBorderNone(borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom());
        setBorderNone(borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft());
        setBorderNone(borders.isSetRight() ? borders.getRight() : borders.addNewRight());
        setBorderNone(borders.isSetInsideH() ? borders.getInsideH() : borders.addNewInsideH());
        setBorderNone(borders.isSetInsideV() ? borders.getInsideV() : borders.addNewInsideV());
    }

    private void setBorderNone(CTBorder border) {
        border.setVal(STBorder.NONE);
        // 线宽/颜色残留会让部分 Word 版本继续画线，一并按无处理
        if (border.isSetSz()) border.unsetSz();
        if (border.isSetColor()) border.unsetColor();
        if (border.isSetSpace()) border.unsetSpace();
    }

    /** 锁定为固定排版：自动列宽会被「地址、电话」这类长文本拉歪，两栏就不再对齐 */
    private void fixTableLayout(XWPFTable table) {
        CTTblPr tblPr = table.getCTTbl().getTblPr() != null ? table.getCTTbl().getTblPr() : table.getCTTbl().addNewTblPr();
        if (!tblPr.isSetTblLayout()) tblPr.addNewTblLayout();
        tblPr.getTblLayout().setType(STTblLayoutType.FIXED);
    }

    /** 固定单元格宽，让两栏均匀分摊页面宽度 */
    private void setCellWidth(XWPFTableCell cell, int width) {
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTTblWidth cellWidth = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
        cellWidth.setW(java.math.BigInteger.valueOf(width));
        cellWidth.setType(STTblWidth.DXA);
    }

    private void createDataRow(XSSFSheet sheet, int rowNum, String label, String value, XSSFCellStyle headerStyle) {
        XSSFRow row = sheet.createRow(rowNum);
        XSSFCell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(headerStyle);
        row.createCell(1).setCellValue(value != null ? value : "");
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
