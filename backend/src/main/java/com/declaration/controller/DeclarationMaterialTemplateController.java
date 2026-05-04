package com.declaration.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.DeclarationMaterialTemplate;
import com.declaration.service.DeclarationMaterialTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 申报资料项模板管理（全局）
 */
@Slf4j
@RestController
@RequestMapping("v1/material/templates")
@RequiredArgsConstructor
@Tag(name = "资料项模板", description = "申报资料项模板配置接口")
public class DeclarationMaterialTemplateController {

    private final DeclarationMaterialTemplateService templateService;

    @GetMapping
    @Operation(summary = "获取资料项模板列表")
    @RequiresPermissions("system:material:template:view")
    public Result<List<DeclarationMaterialTemplate>> list(
            @RequestParam(required = false) Integer enabled) {
        LambdaQueryWrapper<DeclarationMaterialTemplate> wrapper = new LambdaQueryWrapper<>();
        if (enabled != null) {
            wrapper.eq(DeclarationMaterialTemplate::getEnabled, enabled);
        }
        wrapper.orderByAsc(DeclarationMaterialTemplate::getSort)
               .orderByAsc(DeclarationMaterialTemplate::getId);
        return Result.success(templateService.list(wrapper));
    }

    @PostMapping
    @Operation(summary = "新增资料项模板")
    @RequiresPermissions("system:material:template:add")
    public Result<Boolean> add(@RequestBody DeclarationMaterialTemplate entity) {
        if (!StringUtils.hasText(entity.getCode())) {
            return Result.fail("资料编码不能为空");
        }
        if (!StringUtils.hasText(entity.getName())) {
            return Result.fail("资料名称不能为空");
        }
        long codeCount = templateService.count(new LambdaQueryWrapper<DeclarationMaterialTemplate>()
                .eq(DeclarationMaterialTemplate::getCode, entity.getCode()));
        if (codeCount > 0) {
            return Result.fail("资料编码已存在");
        }
        if (entity.getRequired() == null) entity.setRequired(1);
        if (entity.getSort() == null) entity.setSort(0);
        if (entity.getEnabled() == null) entity.setEnabled(1);
        return Result.success(templateService.save(entity));
    }

    @PutMapping
    @Operation(summary = "修改资料项模板")
    @RequiresPermissions("system:material:template:edit")
    public Result<Boolean> update(@RequestBody DeclarationMaterialTemplate entity) {
        if (entity.getId() == null) {
            return Result.fail("ID不能为空");
        }
        if (StringUtils.hasText(entity.getCode())) {
            long codeCount = templateService.count(new LambdaQueryWrapper<DeclarationMaterialTemplate>()
                    .eq(DeclarationMaterialTemplate::getCode, entity.getCode())
                    .ne(DeclarationMaterialTemplate::getId, entity.getId()));
            if (codeCount > 0) {
                return Result.fail("资料编码已存在");
            }
        }
        return Result.success(templateService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除资料项模板")
    @RequiresPermissions("system:material:template:delete")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除资料项模板 id={}", id);
        return Result.success(templateService.removeById(id));
    }
}
