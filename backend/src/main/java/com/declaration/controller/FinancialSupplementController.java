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
import com.declaration.entity.InvoiceSplitItem;
import com.declaration.dao.InvoiceSplitItemMapper;
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
    private final InvoiceSplitItemMapper splitItemMapper;

    @Value("${file.upload-path:uploads/exports/}")
    private String uploadPath;
    
    public FinancialSupplementController(FinancialSupplementService supplementService, 
            DeclarationFormService formService, 
            DeclarationRemittanceService remittanceService, 
            DeclarationAttachmentService attachmentService,
            com.declaration.service.SystemConfigService systemConfigService,
            EntityConfigService entityConfigService,
            InvoiceSplitItemMapper splitItemMapper) {
        this.supplementService = supplementService;
        this.formService = formService;
        this.remittanceService = remittanceService;
        this.attachmentService = attachmentService;
        this.systemConfigService = systemConfigService;
        this.entityConfigService = entityConfigService;
        this.splitItemMapper = splitItemMapper;
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
                    String currency = d.get("currency") != null ? d.get("currency").toString() : "USD";
                    createDataRow(sheet, rowNum++, name.isEmpty() ? "水单" : name,
                            String.format("%1$,.2f %2$s × %3$s = %4$,.2f CNY，银行手续费: %5$,.2f CNY，内部操作费: %6$,.2f CNY", amt, currency, rate, cny, bankFeeCny, internalFeeCny), headerStyle);
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
            
            // 完整计算公式展示
            XSSFRow formulaRow = sheet.createRow(rowNum++);
            formulaRow.createCell(0).setCellValue("计算公式详解");
            formulaRow.getCell(0).setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum-1, rowNum-1, 0, 3));
            
            @SuppressWarnings("unchecked")
            List<String> steps = (List<String>) calcDetail.get("calculationSteps");
            if (steps != null) {
                int stepNo = 1;
                for (String step : steps) {
                    createDataRow(sheet, rowNum++, "步骤" + stepNo++, step, headerStyle);
                }
            }

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
            sellerTaxRow.createCell(0).setCellValue("纳税人识别号: -");
            XSSFRow sellerAddrRow = sheet.createRow(rowNum++);
            sellerAddrRow.createCell(0).setCellValue("地址、电话: -");
            XSSFRow sellerBankRow = sheet.createRow(rowNum++);
            sellerBankRow.createCell(0).setCellValue("开户行及账号: -");

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
                    BigDecimal cnyAmt = pd.get("cnyAmount") instanceof BigDecimal ? (BigDecimal) pd.get("cnyAmount") : BigDecimal.ZERO;
                    BigDecimal unitPrice = (qty != null && qty > 0 && cnyAmt.compareTo(BigDecimal.ZERO) > 0)
                            ? cnyAmt.divide(new BigDecimal(qty), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

                    pRow.createCell(0).setCellValue(pName);
                    pRow.createCell(1).setCellValue("-");
                    pRow.createCell(2).setCellValue(qty != null ? qty : 0);
                    pRow.createCell(3).setCellValue(unit);
                    pRow.createCell(4).setCellValue(unitPrice.doubleValue());
                    pRow.createCell(5).setCellValue(cnyAmt.doubleValue());
                    totalAmount = totalAmount.add(cnyAmt);
                }
            }

            // 中文大写金额行
            XSSFRow wordsRow = sheet.createRow(rowNum++);
            wordsRow.createCell(0).setCellValue("合计金额(大写): " + convertAmountToChineseWords(totalAmount));
            wordsRow.getCell(0).setCellStyle(boldLeft);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 5));

            rowNum++; // 空行

            // === 4) 开票金额计算框 ===
            BigDecimal invoiceAmount = calcDetail.get("invoiceAmount") instanceof BigDecimal
                    ? (BigDecimal) calcDetail.get("invoiceAmount") : BigDecimal.ZERO;
            BigDecimal totalOriginalAmount = calcDetail.get("totalOriginalAmount") instanceof BigDecimal
                    ? (BigDecimal) calcDetail.get("totalOriginalAmount") : BigDecimal.ZERO;
            BigDecimal totalCny = calcDetail.get("totalCny") instanceof BigDecimal
                    ? (BigDecimal) calcDetail.get("totalCny") : BigDecimal.ZERO;
            BigDecimal weightedRate = calcDetail.get("weightedExchangeRate") instanceof BigDecimal
                    ? (BigDecimal) calcDetail.get("weightedExchangeRate") : BigDecimal.ZERO;
            BigDecimal totalDeduction = calcDetail.get("totalInvoiceDeduction") instanceof BigDecimal
                    ? (BigDecimal) calcDetail.get("totalInvoiceDeduction") : BigDecimal.ZERO;

            XSSFRow calcTitle = sheet.createRow(rowNum++);
            calcTitle.createCell(0).setCellValue("开票金额计算");
            calcTitle.getCell(0).setCellStyle(boldLeft);

            XSSFRow calcFormula = sheet.createRow(rowNum++);
            String formula = String.format("开票金额: %.2f = (%.2f - %.2f / %s * %s * 1.13)",
                    invoiceAmount.doubleValue(),
                    totalOriginalAmount.doubleValue(),
                    totalDeduction.doubleValue(),
                    weightedRate.toPlainString(),
                    weightedRate.toPlainString());
            calcFormula.createCell(0).setCellValue(formula);

            XSSFRow usdRow = sheet.createRow(rowNum++);
            usdRow.createCell(0).setCellValue(String.format("收汇美元总额: %.2f", totalOriginalAmount.doubleValue()));

            XSSFRow cnyRow = sheet.createRow(rowNum++);
            cnyRow.createCell(0).setCellValue(String.format("收汇折人民币总额: %.2f", totalCny.doubleValue()));

            XSSFRow feeRow = sheet.createRow(rowNum++);
            feeRow.createCell(0).setCellValue(String.format("人民币费用: %.2f", totalDeduction.doubleValue()));

            XSSFRow rateRow = sheet.createRow(rowNum++);
            rateRow.createCell(0).setCellValue("汇率: " + weightedRate.toPlainString());

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

    @GetMapping("/form/{formId}/split-items")
    @Operation(summary = "查询已保存的20%拆分产品列表")
    public Result<List<InvoiceSplitItem>> getSplitItems(@PathVariable Long formId) {
        List<InvoiceSplitItem> items = splitItemMapper.selectList(
                new LambdaQueryWrapper<InvoiceSplitItem>()
                        .eq(InvoiceSplitItem::getFormId, formId)
                        .orderByAsc(InvoiceSplitItem::getSort));
        return Result.success(items);
    }

    @PostMapping("/form/{formId}/split-items")
    @Operation(summary = "保存20%拆分产品列表")
    @RequiresPermissions("business:declaration:finance:supplement")
    public Result<Void> saveSplitItems(@PathVariable Long formId, @RequestBody Map<String, Object> body) {
        // 先删后插
        splitItemMapper.delete(new LambdaQueryWrapper<InvoiceSplitItem>()
                .eq(InvoiceSplitItem::getFormId, formId));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) body.get("items");
        if (itemsData != null) {
            for (int i = 0; i < itemsData.size(); i++) {
                Map<String, Object> row = itemsData.get(i);
                InvoiceSplitItem item = new InvoiceSplitItem();
                item.setFormId(formId);
                item.setHsCode((String) row.get("hsCode"));
                item.setProductName((String) row.get("productName"));
                item.setSpec((String) row.get("spec"));
                if (row.get("quantity") != null) {
                    item.setQuantity(new BigDecimal(row.get("quantity").toString()));
                }
                if (row.get("unitPrice") != null) {
                    item.setUnitPrice(new BigDecimal(row.get("unitPrice").toString()));
                }
                if (row.get("amount") != null) {
                    item.setAmount(new BigDecimal(row.get("amount").toString()));
                }
                item.setSort(i);
                splitItemMapper.insert(item);
            }
        }
        return Result.success(null);
    }

    @PostMapping("/form/{formId}/export-invoice-package")
    @Operation(summary = "下载开票文件包(80%开票通知书+80%合同+20%开票通知书+20%合同)")
    public Result<String> exportInvoicePackage(@PathVariable Long formId, @RequestBody Map<String, Object> body) {
        try {
            DeclarationForm form = formService.getById(formId);
            if (form == null) throw new RuntimeException("申报单不存在");
            String formNo = form.getFormNo();

            Map<String, Object> calcDetail = supplementService.getCalculationDetail(formId);
            if (calcDetail == null) throw new RuntimeException("未找到开票明细数据");

            // === 解析前端传入的20%产品列表 ===
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> splitItemsRaw = (List<Map<String, Object>>) body.get("splitItems");
            List<Map<String, Object>> splitProducts = new ArrayList<>();
            if (splitItemsRaw != null) {
                for (Map<String, Object> si : splitItemsRaw) {
                    Map<String, Object> pd = new java.util.LinkedHashMap<>();
                    pd.put("productName", si.get("productName"));
                    pd.put("spec", si.get("spec"));
                    BigDecimal qty = si.get("quantity") != null ? new BigDecimal(si.get("quantity").toString()) : BigDecimal.ZERO;
                    BigDecimal unitPrice = si.get("unitPrice") != null ? new BigDecimal(si.get("unitPrice").toString()) : BigDecimal.ZERO;
                    BigDecimal amount = si.get("amount") != null ? new BigDecimal(si.get("amount").toString()) : qty.multiply(unitPrice);
                    pd.put("quantity", qty.intValue());
                    pd.put("unit", "个");
                    pd.put("cnyAmount", amount);
                    pd.put("unitPrice", unitPrice);
                    pd.put("amount", amount);
                    splitProducts.add(pd);
                }
            }

            // === 构建80% calcDetail ===
            Map<String, Object> calcDetail80 = buildScaledCalcDetail(calcDetail, new BigDecimal("0.8"));
            // === 构建20% calcDetail ===
            Map<String, Object> calcDetail20 = buildCustomCalcDetail(calcDetail, splitProducts);

            // 1) 80% 开票通知书
            byte[] notification80 = generateNotificationWord(form, calcDetail80);
            // 2) 80% 合同
            byte[] contract80 = generateContractWord(form, calcDetail80);
            // 3) 20% 开票通知书
            byte[] notification20 = generateNotificationWord(form, calcDetail20);
            // 4) 20% 合同
            byte[] contract20 = generateContractWord(form, calcDetail20);

            // 5) 打包 ZIP
            String uuidFileName = java.util.UUID.randomUUID().toString() + ".zip";
            String dateDir = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
            String uploadDirPath = uploadPath + formNo + "/" + dateDir + "/";
            File dir = new File(uploadDirPath);
            if (!dir.exists()) dir.mkdirs();

            File zipFile = new File(dir, uuidFileName);
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                zos.putNextEntry(new ZipEntry("开票通知书_80%_" + formNo + ".docx"));
                zos.write(notification80); zos.closeEntry();
                zos.putNextEntry(new ZipEntry("合同_80%_" + formNo + ".docx"));
                zos.write(contract80); zos.closeEntry();
                zos.putNextEntry(new ZipEntry("开票通知书_20%_" + formNo + ".docx"));
                zos.write(notification20); zos.closeEntry();
                zos.putNextEntry(new ZipEntry("合同_20%_" + formNo + ".docx"));
                zos.write(contract20); zos.closeEntry();
            }

            String relativePath = formNo + "/" + dateDir + "/" + uuidFileName;
            String fileUrl = "/api/v1/files/download?path=" + relativePath;
            return Result.success(fileUrl);

        } catch (Exception e) {
            log.error("导出开票文件包失败", e);
            return Result.fail("导出失败: " + e.getMessage());
        }
    }

    /** 构建按比例缩放的 calcDetail（用于80%文档） */
    private Map<String, Object> buildScaledCalcDetail(Map<String, Object> original, BigDecimal scale) {
        Map<String, Object> result = new java.util.LinkedHashMap<>(original);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) original.get("productTaxDetails");
        if (products != null && !products.isEmpty()) {
            // 计算基准比例: invoiceAmount / totalGoodsAmount
            BigDecimal totalGoods = BigDecimal.ZERO;
            for (Map<String, Object> pd : products) {
                BigDecimal cny = pd.get("cnyAmount") instanceof BigDecimal ? (BigDecimal) pd.get("cnyAmount") : BigDecimal.ZERO;
                totalGoods = totalGoods.add(cny);
            }
            BigDecimal invoiceAmount = original.get("invoiceAmount") instanceof BigDecimal ? (BigDecimal) original.get("invoiceAmount") : totalGoods;
            // 如果 invoiceAmount 和 totalGoods 都大于0，用 invoiceAmount 作为基准分配
            BigDecimal baseAmount = (invoiceAmount.compareTo(BigDecimal.ZERO) > 0 && totalGoods.compareTo(BigDecimal.ZERO) > 0)
                    ? invoiceAmount : totalGoods;

            List<Map<String, Object>> scaledProducts = new ArrayList<>();
            BigDecimal totalCnyAmount = BigDecimal.ZERO;
            BigDecimal totalAmountWithTaxRefund = BigDecimal.ZERO;
            BigDecimal targetTotal = baseAmount.multiply(scale).setScale(2, java.math.RoundingMode.HALF_UP);

            for (int i = 0; i < products.size(); i++) {
                Map<String, Object> pd = products.get(i);
                Map<String, Object> sp = new java.util.LinkedHashMap<>(pd);
                BigDecimal cnyAmt = pd.get("cnyAmount") instanceof BigDecimal ? (BigDecimal) pd.get("cnyAmount") : BigDecimal.ZERO;
                BigDecimal amtWithTax = pd.get("amountWithTaxRefund") instanceof BigDecimal ? (BigDecimal) pd.get("amountWithTaxRefund") : cnyAmt;
                // 按比例分配: 最后一个产品补齐舍入差额
                BigDecimal scaledCny;
                if (i == products.size() - 1) {
                    scaledCny = targetTotal.subtract(totalCnyAmount);
                } else {
                    BigDecimal ratio = totalGoods.compareTo(BigDecimal.ZERO) > 0
                            ? cnyAmt.divide(totalGoods, 10, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    scaledCny = targetTotal.multiply(ratio).setScale(2, java.math.RoundingMode.HALF_UP);
                }
                // amountWithTaxRefund 同样按比例
                BigDecimal ratioWT = (amtWithTax.compareTo(BigDecimal.ZERO) > 0 && cnyAmt.compareTo(BigDecimal.ZERO) > 0)
                        ? amtWithTax.divide(cnyAmt, 10, java.math.RoundingMode.HALF_UP) : BigDecimal.ONE;
                BigDecimal scaledAmtWithTax = scaledCny.multiply(ratioWT).setScale(2, java.math.RoundingMode.HALF_UP);

                sp.put("cnyAmount", scaledCny);
                sp.put("amountWithTaxRefund", scaledAmtWithTax);
                Number qtyNum = pd.get("quantity") instanceof Number ? (Number) pd.get("quantity") : 0;
                BigDecimal qtyBD = new BigDecimal(qtyNum.toString());
                if (qtyBD.compareTo(BigDecimal.ZERO) > 0) {
                    sp.put("unitPrice", scaledCny.divide(qtyBD, 2, java.math.RoundingMode.HALF_UP));
                }
                scaledProducts.add(sp);
                totalCnyAmount = totalCnyAmount.add(scaledCny);
                totalAmountWithTaxRefund = totalAmountWithTaxRefund.add(scaledAmtWithTax);
            }
            result.put("productTaxDetails", scaledProducts);
            result.put("totalGoodsAmount", totalCnyAmount);
            result.put("amountWithTaxRefund", totalAmountWithTaxRefund);
            result.put("totalCny", totalCnyAmount);
            // 缩放扣减项
            if (original.get("totalInvoiceDeduction") instanceof BigDecimal)
                result.put("totalInvoiceDeduction", ((BigDecimal) original.get("totalInvoiceDeduction")).multiply(scale).setScale(2, java.math.RoundingMode.HALF_UP));
            if (original.get("totalFeeAmount") instanceof BigDecimal)
                result.put("totalFeeAmount", ((BigDecimal) original.get("totalFeeAmount")).multiply(scale).setScale(2, java.math.RoundingMode.HALF_UP));
            if (original.get("bankFeeAmount") instanceof BigDecimal)
                result.put("bankFeeAmount", ((BigDecimal) original.get("bankFeeAmount")).multiply(scale).setScale(2, java.math.RoundingMode.HALF_UP));
            if (original.get("internalBankFee") instanceof BigDecimal)
                result.put("internalBankFee", ((BigDecimal) original.get("internalBankFee")).multiply(scale).setScale(2, java.math.RoundingMode.HALF_UP));
            if (original.get("invoiceAmount") instanceof BigDecimal)
                result.put("invoiceAmount", ((BigDecimal) original.get("invoiceAmount")).multiply(scale).setScale(2, java.math.RoundingMode.HALF_UP));
            if (original.get("totalOriginalAmount") instanceof BigDecimal)
                result.put("totalOriginalAmount", ((BigDecimal) original.get("totalOriginalAmount")).multiply(scale).setScale(2, java.math.RoundingMode.HALF_UP));
        }
        return result;
    }

    /** 构建自定义产品的 calcDetail（用于20%文档） */
    private Map<String, Object> buildCustomCalcDetail(Map<String, Object> original, List<Map<String, Object>> customProducts) {
        Map<String, Object> result = new java.util.LinkedHashMap<>(original);
        List<Map<String, Object>> products = new ArrayList<>();
        BigDecimal totalCnyAmount = BigDecimal.ZERO;
        if (customProducts != null) {
            for (Map<String, Object> pd : customProducts) {
                Map<String, Object> sp = new java.util.LinkedHashMap<>(pd);
                BigDecimal cnyAmt = pd.get("cnyAmount") instanceof BigDecimal ? (BigDecimal) pd.get("cnyAmount") : BigDecimal.ZERO;
                sp.put("amountWithTaxRefund", cnyAmt);
                sp.put("taxRefundRate", BigDecimal.ZERO);
                products.add(sp);
                totalCnyAmount = totalCnyAmount.add(cnyAmt);
            }
        }
        result.put("productTaxDetails", products);
        result.put("totalGoodsAmount", totalCnyAmount);
        result.put("amountWithTaxRefund", totalCnyAmount);
        result.put("totalCny", totalCnyAmount);
        result.put("totalInvoiceDeduction", BigDecimal.ZERO);
        result.put("totalFeeAmount", BigDecimal.ZERO);
        result.put("bankFeeAmount", BigDecimal.ZERO);
        result.put("internalBankFee", BigDecimal.ZERO);
        result.put("invoiceAmount", totalCnyAmount);
        result.put("totalOriginalAmount", totalCnyAmount);
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

            // 购货/销货单位（销货方信息留空）
            String buyerTaxId = (entityConfig != null && entityConfig.getTaxId() != null) ? entityConfig.getTaxId() : "-";
            String buyerPhone = (entityConfig != null && entityConfig.getPhone() != null) ? entityConfig.getPhone() : "-";
            String buyerBank = (entityConfig != null && entityConfig.getBankAccount() != null) ? entityConfig.getBankAccount() : "-";
            String buyerAddr = (entityConfig != null && entityConfig.getEntityAddressCn() != null) ? entityConfig.getEntityAddressCn() : "-";
            String buyerName = companyCn;
            addWordParagraph(doc, "购货单位", true, 12, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "名称: " + buyerName, false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "纳税人识别号: " + buyerTaxId, false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "地址、电话: " + buyerAddr + " " + buyerPhone, false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "开户行及账号: " + buyerBank, false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "", false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "销货单位", true, 12, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "名称: ", false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "纳税人识别号: ", false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "地址、电话: ", false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "开户行及账号: ", false, 10, ParagraphAlignment.LEFT);
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
                    BigDecimal cnyAmt = pd.get("cnyAmount") instanceof BigDecimal ? (BigDecimal) pd.get("cnyAmount") : BigDecimal.ZERO;
                    BigDecimal unitPrice = pd.get("unitPrice") instanceof BigDecimal ? (BigDecimal) pd.get("unitPrice") :
                            (qtyBD.compareTo(BigDecimal.ZERO) > 0 ? cnyAmt.divide(qtyBD, 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
                    setCellText(pRow.getCell(0), pName, false, 10);
                    setCellText(pRow.getCell(1), spec, false, 10);
                    setCellText(pRow.getCell(2), qtyBD.stripTrailingZeros().toPlainString(), false, 10);
                    setCellText(pRow.getCell(3), unit, false, 10);
                    setCellText(pRow.getCell(4), unitPrice.toPlainString(), false, 10);
                    setCellText(pRow.getCell(5), cnyAmt.toPlainString(), false, 10);
                    totalAmount = totalAmount.add(cnyAmt);
                }
            }
            addWordParagraph(doc, "", false, 6, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "合计金额(大写): " + convertAmountToChineseWords(totalAmount), true, 12, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "", false, 10, ParagraphAlignment.LEFT);

            // 开票金额计算（真实公式：退税加成 - 发票扣减 - 手续费 = 开票金额）
            BigDecimal invoiceAmount = calcDetail.get("invoiceAmount") instanceof BigDecimal ? (BigDecimal) calcDetail.get("invoiceAmount") : BigDecimal.ZERO;
            BigDecimal amountWithTaxRefund = calcDetail.get("amountWithTaxRefund") instanceof BigDecimal ? (BigDecimal) calcDetail.get("amountWithTaxRefund") : BigDecimal.ZERO;
            BigDecimal totalInvoiceDeduction = calcDetail.get("totalInvoiceDeduction") instanceof BigDecimal ? (BigDecimal) calcDetail.get("totalInvoiceDeduction") : BigDecimal.ZERO;
            BigDecimal totalFeeAmount = calcDetail.get("totalFeeAmount") instanceof BigDecimal ? (BigDecimal) calcDetail.get("totalFeeAmount") : BigDecimal.ZERO;
            BigDecimal bankFeeAmt = calcDetail.get("bankFeeAmount") instanceof BigDecimal ? (BigDecimal) calcDetail.get("bankFeeAmount") : BigDecimal.ZERO;
            BigDecimal internalBankFeeAmt = calcDetail.get("internalBankFee") instanceof BigDecimal ? (BigDecimal) calcDetail.get("internalBankFee") : BigDecimal.ZERO;
            BigDecimal totalOrig = calcDetail.get("totalOriginalAmount") instanceof BigDecimal ? (BigDecimal) calcDetail.get("totalOriginalAmount") : BigDecimal.ZERO;
            BigDecimal rate = calcDetail.get("weightedExchangeRate") instanceof BigDecimal ? (BigDecimal) calcDetail.get("weightedExchangeRate") : BigDecimal.ZERO;

            addWordParagraph(doc, "开票金额计算", true, 12, ParagraphAlignment.LEFT);
            addWordParagraph(doc, String.format("退税加成合计: %.2f CNY", amountWithTaxRefund.doubleValue()), false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, String.format("减 发票扣减: -%.2f CNY", totalInvoiceDeduction.doubleValue()), false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, String.format("减 手续费: -%.2f CNY (银行手续费: %.2f + 内部操作费: %.2f)", totalFeeAmount.doubleValue(), bankFeeAmt.doubleValue(), internalBankFeeAmt.doubleValue()), false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, String.format("开票金额: %.2f - %.2f - %.2f = %.2f CNY", amountWithTaxRefund.doubleValue(), totalInvoiceDeduction.doubleValue(), totalFeeAmount.doubleValue(), invoiceAmount.doubleValue()), true, 11, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "", false, 6, ParagraphAlignment.LEFT);
            addWordParagraph(doc, String.format("收汇美元总额: %.2f", totalOrig.doubleValue()), false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "汇率: " + rate.toPlainString(), false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "", false, 10, ParagraphAlignment.LEFT);

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
            // 从主体配置获取中文名和中文地址
            EntityConfig entityConfig = form.getEntityId() != null ? entityConfigService.getById(form.getEntityId()) : null;
            String companyCn = (entityConfig != null && entityConfig.getEntityNameCn() != null) ? entityConfig.getEntityNameCn() : "-";
            String companyAddressCn = (entityConfig != null && entityConfig.getEntityAddressCn() != null) ? entityConfig.getEntityAddressCn() : "-";
            // 抬头
            addWordParagraph(doc, companyCn, true, 16, ParagraphAlignment.CENTER);
            addWordParagraph(doc, "购 货 合 同", true, 18, ParagraphAlignment.CENTER);
            addWordParagraph(doc, "第1页/共1页", false, 10, ParagraphAlignment.CENTER);
            addWordParagraph(doc, "", false, 10, ParagraphAlignment.LEFT);

            // 合同信息
            addWordParagraph(doc, "合同编号: " + (form.getInvoiceNo() != null ? form.getInvoiceNo() : "-"), false, 11, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "签订地点: 宁波", false, 11, ParagraphAlignment.LEFT);
            String signDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
            addWordParagraph(doc, "签订时间: " + signDate, false, 11, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "", false, 10, ParagraphAlignment.LEFT);

            // 甲方（购买方）
            addWordParagraph(doc, "甲方: " + companyCn, true, 12, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "地址: " + companyAddressCn, false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "业务联系人: -    电话: -", false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "财务/物流: -    电话: -", false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "", false, 10, ParagraphAlignment.LEFT);

            // 乙方（销货方信息留空）
            addWordParagraph(doc, "乙方: ", true, 12, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "联系人: ", false, 10, ParagraphAlignment.LEFT);
            addWordParagraph(doc, "电话: ", false, 10, ParagraphAlignment.LEFT);
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
                    BigDecimal cnyAmt = pd.get("cnyAmount") instanceof BigDecimal ? (BigDecimal) pd.get("cnyAmount") : BigDecimal.ZERO;
                    BigDecimal unitPrice = pd.get("unitPrice") instanceof BigDecimal ? (BigDecimal) pd.get("unitPrice") :
                            (qtyBD.compareTo(BigDecimal.ZERO) > 0 ? cnyAmt.divide(qtyBD, 4, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);
                    BigDecimal exclTax = cnyAmt.divide(new BigDecimal("1.13"), 2, java.math.RoundingMode.HALF_UP);
                    setCellText(pRow.getCell(0), pName, false, 10);
                    setCellText(pRow.getCell(1), qtyBD.stripTrailingZeros().toPlainString(), false, 10);
                    setCellText(pRow.getCell(2), unit, false, 10);
                    setCellText(pRow.getCell(3), unitPrice.toPlainString(), false, 10);
                    setCellText(pRow.getCell(4), exclTax.toPlainString(), false, 10);
                    setCellText(pRow.getCell(5), cnyAmt.toPlainString(), false, 10);
                    totalInclTax = totalInclTax.add(cnyAmt);
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
        CTTblWidth tblWidth = tblPr.addNewTblW();
        tblWidth.setW(java.math.BigInteger.valueOf(width));
        tblWidth.setType(STTblWidth.DXA);
    }

    private void createDataRow(XSSFSheet sheet, int rowNum, String label, String value, XSSFCellStyle headerStyle) {
        XSSFRow row = sheet.createRow(rowNum);
        XSSFCell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(headerStyle);
        row.createCell(1).setCellValue(value != null ? value : "");
    }
}
