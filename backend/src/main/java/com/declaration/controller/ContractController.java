package com.declaration.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.ContractGeneration;
import com.declaration.entity.ContractTemplate;
import com.declaration.service.ContractGenerateService;
import com.declaration.service.ContractGenerationService;
import com.declaration.service.ContractTemplateService;
import com.declaration.service.DeclarationFormService;
import com.declaration.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import cn.dev33.satoken.stp.StpUtil;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 合同生成管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/contract")
@RequiredArgsConstructor
@Tag(name = "合同管理", description = "合同模板和生成相关接口")
public class ContractController {

    private final ContractTemplateService contractTemplateService;
    private final ContractGenerationService contractGenerationService;
    private final ContractGenerateService contractGenerateService;
    private final DeclarationFormService declarationFormService;
    private final SystemConfigService systemConfigService;
    
    @Value("${file.upload.contract-path:/uploads/contracts}")
    private String contractUploadPath;

    /**
     * 分页查询合同模板
     */
    @GetMapping("/templates")
    @Operation(summary = "分页查询合同模板")
    @RequiresPermissions("business:contract:template:query")
    public Result<Page<ContractTemplate>> getTemplates(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "模板类型") @RequestParam(required = false) String templateType,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        
        Page<ContractTemplate> page = new Page<>(current, size);
        LambdaQueryWrapper<ContractTemplate> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(ContractTemplate::getTemplateName, keyword)
                   .or()
                   .like(ContractTemplate::getTemplateCode, keyword);
        }
        
        if (templateType != null && !templateType.isEmpty()) {
            wrapper.eq(ContractTemplate::getTemplateType, templateType);
        }
        
        wrapper.eq(ContractTemplate::getStatus, 1)
               .orderByAsc(ContractTemplate::getSort)
               .orderByDesc(ContractTemplate::getCreateTime);
        
        Page<ContractTemplate> result = contractTemplateService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 获取所有启用的合同模板（用于下拉选择）
     */
    @GetMapping("/templates/enabled")
    @Operation(summary = "获取所有启用的合同模板")
    @RequiresPermissions("business:contract:template:query")
    public Result<List<ContractTemplate>> getEnabledTemplates(
            @Parameter(description = "模板类型") @RequestParam(required = false) String templateType) {
        LambdaQueryWrapper<ContractTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractTemplate::getStatus, 1);
        
        if (templateType != null && !templateType.isEmpty()) {
            wrapper.eq(ContractTemplate::getTemplateType, templateType);
        }
        
        wrapper.orderByAsc(ContractTemplate::getSort)
               .orderByAsc(ContractTemplate::getTemplateName);
        
        List<ContractTemplate> templates = contractTemplateService.list(wrapper);
        return Result.success(templates);
    }

    /**
     * 新增合同模板
     */
    @PostMapping("/template")
    @Operation(summary = "新增合同模板")
    @RequiresPermissions("business:contract:template:add")
    public Result<ContractTemplate> addTemplate(@RequestBody ContractTemplate template) {
        template.setStatus(1); // 默认启用
        boolean saved = contractTemplateService.save(template);
        if (saved) {
            return Result.success(template);
        } else {
            return Result.fail("新增模板失败");
        }
    }

    /**
     * 更新合同模板
     */
    @PutMapping("/template/{id}")
    @Operation(summary = "更新合同模板")
    @RequiresPermissions("business:contract:template:update")
    public Result<ContractTemplate> updateTemplate(@PathVariable Long id, @RequestBody ContractTemplate template) {
        template.setId(id);
        boolean updated = contractTemplateService.updateById(template);
        if (updated) {
            return Result.success(template);
        } else {
            return Result.fail("更新模板失败");
        }
    }

    /**
     * 删除合同模板
     */
    @DeleteMapping("/template/{id}")
    @Operation(summary = "删除合同模板")
    @RequiresPermissions("business:contract:template:delete")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        boolean deleted = contractTemplateService.removeById(id);
        if (deleted) {
            return Result.success();
        } else {
            return Result.fail("删除模板失败");
        }
    }

    /**
     * 上传合同模板文件
     */
    @PostMapping("/template/upload")
    @Operation(summary = "上传合同模板文件")
    @RequiresPermissions("business:contract:template:upload")
    public Result<Map<String, Object>> uploadTemplate(
            @Parameter(description = "模板文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "模板ID") @RequestParam("templateId") Long templateId) {
        try {
            String filePath = contractGenerateService.uploadTemplateFile(file, templateId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("filePath", filePath);
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 生成合同
     */
    @PostMapping("/generate")
    @Operation(summary = "生成合同")
    @RequiresPermissions("business:contract:generate")
    public Result<ContractGeneration> generateContract(
            @Parameter(description = "模板ID") @RequestParam Long templateId,
            @Parameter(description = "申报单ID") @RequestParam Long declarationFormId,
            @RequestBody Map<String, Object> dataMap) {
        try {
            // 从登录信息获取用户ID
            Long generatedBy = StpUtil.getLoginIdAsLong();
            
            ContractGeneration generation = contractGenerateService.generateContract(
                templateId, declarationFormId, dataMap, generatedBy);
            
            return Result.success(generation);
        } catch (Exception e) {
            log.error("合同生成失败", e);
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 分页查询合同生成记录
     */
    @GetMapping("/generations")
    @Operation(summary = "分页查询合同生成记录")
    @RequiresPermissions("business:contract:generation:query")
    public Result<Page<ContractGeneration>> getGenerations(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "申报单ID") @RequestParam(required = false) Long declarationFormId,
            @Parameter(description = "合同编号") @RequestParam(required = false) String contractNo) {
        
        Page<ContractGeneration> page = new Page<>(current, size);
        LambdaQueryWrapper<ContractGeneration> wrapper = new LambdaQueryWrapper<>();
        
        if (declarationFormId != null) {
            wrapper.eq(ContractGeneration::getDeclarationFormId, declarationFormId);
        }
        
        if (contractNo != null && !contractNo.isEmpty()) {
            wrapper.like(ContractGeneration::getContractNo, contractNo);
        }
        
        wrapper.eq(ContractGeneration::getStatus, 1)
               .orderByDesc(ContractGeneration::getGeneratedTime);
        
        Page<ContractGeneration> result = contractGenerationService.page(page, wrapper);
        
        // 填充模板名称和申报单编号信息
        if (result.getRecords() != null && !result.getRecords().isEmpty()) {
            for (ContractGeneration generation : result.getRecords()) {
                // 填充模板名称
                if (generation.getTemplateId() != null) {
                    ContractTemplate template = contractTemplateService.getById(generation.getTemplateId());
                    if (template != null) {
                        generation.setTemplateName(template.getTemplateName());
                    }
                }
                
                // 填充申报单编号
                if (generation.getDeclarationFormId() != null) {
                    com.declaration.entity.DeclarationForm declarationForm = 
                        declarationFormService.getById(generation.getDeclarationFormId());
                    if (declarationForm != null) {
                        generation.setDeclarationFormCode(declarationForm.getFormNo());
                    }
                }
            }
        }
        
        return Result.success(result);
    }

    /**
     * 根据申报单ID获取相关合同
     */
    @GetMapping("/by-declaration/{declarationFormId}")
    @Operation(summary = "根据申报单ID获取相关合同")
    @RequiresPermissions("business:contract:generation:query")
    public Result<List<ContractGeneration>> getContractsByDeclaration(
            @Parameter(description = "申报单ID") @PathVariable Long declarationFormId) {
        
        try {
            LambdaQueryWrapper<ContractGeneration> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ContractGeneration::getDeclarationFormId, declarationFormId)
                   .eq(ContractGeneration::getStatus, 1)
                   .orderByDesc(ContractGeneration::getGeneratedTime);
            
            List<ContractGeneration> contracts = contractGenerationService.list(wrapper);
            
            // 填充模板名称信息
            if (contracts != null && !contracts.isEmpty()) {
                for (ContractGeneration contract : contracts) {
                    if (contract.getTemplateId() != null) {
                        ContractTemplate template = contractTemplateService.getById(contract.getTemplateId());
                        if (template != null) {
                            contract.setTemplateName(template.getTemplateName());
                        }
                    }
                }
            }
            
            return Result.success(contracts);
        } catch (Exception e) {
            log.error("查询申报单相关合同失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 下载合同文件
     */
    @GetMapping("/download/{id}")
    @Operation(summary = "下载合同文件")
    @RequiresPermissions("business:contract:download")
    public void downloadContract(@Parameter(description = "合同ID") @PathVariable Long id, HttpServletResponse response) {
        try {
            File contractFile = contractGenerateService.getContractFile(id);
            
            // 设置响应头
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            String fileName = contractFile.getName();
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");
            response.setContentLength((int) contractFile.length());
            
            // 写入文件流
            try (FileInputStream fis = new FileInputStream(contractFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    response.getOutputStream().write(buffer, 0, bytesRead);
                }
                response.getOutputStream().flush();
            }
                    
        } catch (Exception e) {
            log.error("合同文件下载失败", e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * 删除合同记录
     */
    @DeleteMapping("/generation/{id}")
    @Operation(summary = "删除合同记录")
    @RequiresPermissions("business:contract:generation:delete")
    public Result<Void> deleteContract(@Parameter(description = "合同ID") @PathVariable Long id) {
        ContractGeneration generation = contractGenerationService.getById(id);
        if (generation == null) {
            return Result.fail("合同记录不存在");
        }
        
        generation.setStatus(0); // 软删除
        boolean updated = contractGenerationService.updateById(generation);
        if (updated) {
            return Result.success();
        } else {
            return Result.fail("删除失败");
        }
    }

    /**
     * 替换合同文件
     */
    @PostMapping("/generation/{id}/replace")
    @Operation(summary = "替换合同文件")
    @RequiresPermissions("business:contract:edit")
    public Result<ContractGeneration> replaceContractFile(
            @Parameter(description = "合同ID") @PathVariable Long id,
            @Parameter(description = "新合同文件") @RequestParam("file") MultipartFile file) {
        
        try {
            // 1. 验证合同记录是否存在
            ContractGeneration generation = contractGenerationService.getById(id);
            if (generation == null) {
                return Result.fail("合同记录不存在");
            }
            
            if (generation.getStatus() != 1) {
                return Result.fail("合同记录状态异常");
            }
            
            // 2. 验证文件格式
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".docx")) {
                return Result.fail("只支持.docx格式的Word文档");
            }
            
            // 3. 验证文件大小
            if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024) { // 10MB限制
                return Result.fail("文件大小不能超过10MB");
            }
            
            // 4. 从数据库配置获取合同生成路径
            String dynamicContractPath = systemConfigService.getConfigValue("file.upload.contract-path", contractUploadPath);
            
            // 5. 构造新的文件路径
            String formNoDir = declarationFormService.getById(generation.getDeclarationFormId()).getFormNo();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFileName = UUID.randomUUID().toString().replaceAll("-", "") + extension;
            Path newFilePath = Paths.get(dynamicContractPath, formNoDir, newFileName);
            
            // 6. 创建目录并保存新文件
            Files.createDirectories(newFilePath.getParent());
            file.transferTo(newFilePath);
            
            // 7. 更新合同记录
            generation.setGeneratedFileName(newFileName);
            generation.setGeneratedFilePath(formNoDir + "/" + newFileName);
            generation.setFileSize(file.getSize());
            generation.setGeneratedTime(LocalDateTime.now());
            
            boolean updated = contractGenerationService.updateById(generation);
            if (updated) {
                log.info("合同文件替换成功: 合同ID={}, 新文件={}", id, newFileName);
                return Result.success(generation);
            } else {
                // 回滚：删除刚上传的文件
                if (Files.exists(newFilePath)) {
                    Files.delete(newFilePath);
                }
                return Result.fail("合同记录更新失败");
            }
            
        } catch (Exception e) {
            log.error("合同文件替换失败", e);
            return Result.fail("替换失败: " + e.getMessage());
        }
    }
}