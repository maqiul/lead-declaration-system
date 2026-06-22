package com.declaration.controller;

import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.FinancialSupplement;
import com.declaration.entity.DeclarationAttachment;
import com.declaration.service.FinancialSupplementService;
import com.declaration.service.DeclarationAttachmentService;
import com.declaration.service.ExcelExportService;
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

    @Value("${file.upload-path:uploads/exports/}")
    private String uploadPath;
    
    public FinancialSupplementController(FinancialSupplementService supplementService, 
            DeclarationFormService formService, 
            DeclarationRemittanceService remittanceService, 
            DeclarationAttachmentService attachmentService,
            com.declaration.service.SystemConfigService systemConfigService) {
        this.supplementService = supplementService;
        this.formService = formService;
        this.remittanceService = remittanceService;
        this.attachmentService = attachmentService;
        this.systemConfigService = systemConfigService;
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

    private void createDataRow(XSSFSheet sheet, int rowNum, String label, String value, XSSFCellStyle headerStyle) {
        XSSFRow row = sheet.createRow(rowNum);
        XSSFCell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(headerStyle);
        row.createCell(1).setCellValue(value != null ? value : "");
    }
}
