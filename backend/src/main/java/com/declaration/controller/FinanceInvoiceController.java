package com.declaration.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.PageParam;
import com.declaration.common.Result;
import com.declaration.entity.DeclarationInvoice;
import com.declaration.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Tag(name = "财务发票管理", description = "进项/出项发票台账")
@RestController
@RequestMapping("/v1/finance/invoices")
@RequiredArgsConstructor
public class FinanceInvoiceController {

    private final InvoiceService invoiceService;

    // 文件存储路径（可配置）
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/invoices/";

    @GetMapping
    @Operation(summary = "获取财务发票列表")
    @RequiresPermissions("finance:invoice:view")
    public Result<IPage<DeclarationInvoice>> getList(
            PageParam pageParam,
            @RequestParam(required = false) Integer invoiceType,
            @RequestParam(required = false) String invoiceNo,
            @RequestParam(required = false) Long formId) {
        return Result.success(invoiceService.getFinanceInvoices(pageParam, invoiceType, invoiceNo, formId));
    }

    @PostMapping
    @Operation(summary = "录入财务发票")
    @RequiresPermissions("finance:invoice:create")
    public Result<Void> create(@RequestBody DeclarationInvoice invoice) {
        // 强制关联申报单
        if (invoice.getFormId() == null) {
            return Result.fail("请关联申报单");
        }
        invoice.setCategory(2); // 强制设为财务留底
        invoiceService.save(invoice);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新财务发票")
    @RequiresPermissions("finance:invoice:update")
    public Result<Void> update(@PathVariable Long id, @RequestBody DeclarationInvoice invoice) {
        // 强制关联申报单
        if (invoice.getFormId() == null) {
            return Result.fail("请关联申报单");
        }
        invoice.setId(id);
        invoice.setCategory(2); // 确保不会被改为业务类
        invoiceService.updateById(invoice);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除财务发票")
    @RequiresPermissions("finance:invoice:delete")
    public Result<Void> delete(@PathVariable Long id) {
        invoiceService.removeById(id);
        return Result.success();
    }

    /**
     * 上传发票文件
     */
    @PostMapping("/{id}/file")
    @Operation(summary = "上传发票文件")
    @RequiresPermissions("finance:invoice:edit")
    public Result<Void> uploadFile(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        DeclarationInvoice invoice = invoiceService.getById(id);
        if (invoice == null) {
            return Result.fail("发票不存在");
        }

        // 确保目录存在
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = "invoice_" + id + "_" + System.currentTimeMillis() + extension;
        
        // 保存文件
        Path targetPath = Paths.get(UPLOAD_DIR + newFilename);
        Files.copy(file.getInputStream(), targetPath);

        // 更新发票记录
        invoice.setFileUrl("/uploads/invoices/" + newFilename);
        invoice.setFileName(originalFilename);
        invoiceService.updateById(invoice);

        return Result.success();
    }

    /**
     * 下载发票文件
     */
    @GetMapping("/{id}/file")
    @Operation(summary = "下载发票文件")
    @RequiresPermissions("finance:invoice:view")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        DeclarationInvoice invoice = invoiceService.getById(id);
        if (invoice == null || invoice.getFileUrl() == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Paths.get(System.getProperty("user.dir") + invoice.getFileUrl());
        Resource resource = new FileSystemResource(filePath.toFile());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + invoice.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
