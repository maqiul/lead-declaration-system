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

import cn.dev33.satoken.stp.StpUtil;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return Result.success(result);
    }

    /**
     * 下载合同文件
     */
    @GetMapping("/download/{id}")
    @Operation(summary = "下载合同文件")
    @RequiresPermissions("business:contract:download")
    public ResponseEntity<Resource> downloadContract(@Parameter(description = "合同ID") @PathVariable Long id) {
        try {
            File contractFile = contractGenerateService.getContractFile(id);
            
            InputStreamResource resource = new InputStreamResource(new FileInputStream(contractFile));
            
            String fileName = contractFile.getName();
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("合同文件下载失败", e);
            return ResponseEntity.notFound().build();
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
}