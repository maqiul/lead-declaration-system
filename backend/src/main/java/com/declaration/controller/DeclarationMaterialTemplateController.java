package com.declaration.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.DeclarationMaterialTemplate;
import com.declaration.entity.MaterialTemplateBinding;
import com.declaration.service.DeclarationMaterialTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

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
            @RequestParam(required = false) Integer enabled,
            @RequestParam(required = false) String stage) {
        LambdaQueryWrapper<DeclarationMaterialTemplate> wrapper = new LambdaQueryWrapper<>();
        if (enabled != null) {
            wrapper.eq(DeclarationMaterialTemplate::getEnabled, enabled);
        }
        if (StringUtils.hasText(stage)) {
            if (!templateService.validStages().contains(stage)) {
                return Result.fail("非法的环节类型：" + stage);
            }
            wrapper.eq(DeclarationMaterialTemplate::getStage, stage);
        }
        wrapper.orderByAsc(DeclarationMaterialTemplate::getSort)
               .orderByAsc(DeclarationMaterialTemplate::getId);
        List<DeclarationMaterialTemplate> list = templateService.list(wrapper);
        // 附带每个模板的绑定规则
        if (!list.isEmpty()) {
            List<Long> tplIds = list.stream().map(DeclarationMaterialTemplate::getId).collect(Collectors.toList());
            Map<Long, List<MaterialTemplateBinding>> bindingMap = templateService.batchGetBindings(tplIds);
            for (DeclarationMaterialTemplate tpl : list) {
                tpl.setBindings(bindingMap.getOrDefault(tpl.getId(), Collections.emptyList()));
            }
        }
        return Result.success(list);
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
        // stage 默认值与校验
        if (!StringUtils.hasText(entity.getStage())) {
            entity.setStage("MATERIAL_SUBMIT");
        } else if (!templateService.validStages().contains(entity.getStage())) {
            return Result.fail("非法的环节类型：" + entity.getStage());
        }
        long codeCount = templateService.count(new LambdaQueryWrapper<DeclarationMaterialTemplate>()
                .eq(DeclarationMaterialTemplate::getCode, entity.getCode()));
        if (codeCount > 0) {
            return Result.fail("资料编码已存在");
        }
        if (entity.getRequired() == null) entity.setRequired(1);
        if (entity.getSort() == null) entity.setSort(0);
        if (entity.getEnabled() == null) entity.setEnabled(1);
        boolean saved = templateService.save(entity);
        if (saved && entity.getId() != null) {
            templateService.saveBindings(entity.getId(), entity.getBindings());
        }
        return Result.success(saved);
    }

    @PutMapping
    @Operation(summary = "修改资料项模板")
    @RequiresPermissions("system:material:template:edit")
    public Result<Boolean> update(@RequestBody DeclarationMaterialTemplate entity) {
        if (entity.getId() == null) {
            return Result.fail("ID不能为空");
        }
        // stage 校验
        if (entity.getStage() != null && !templateService.validStages().contains(entity.getStage())) {
            return Result.fail("非法的环节类型：" + entity.getStage());
        }
        if (StringUtils.hasText(entity.getCode())) {
            long codeCount = templateService.count(new LambdaQueryWrapper<DeclarationMaterialTemplate>()
                    .eq(DeclarationMaterialTemplate::getCode, entity.getCode())
                    .ne(DeclarationMaterialTemplate::getId, entity.getId()));
            if (codeCount > 0) {
                return Result.fail("资料编码已存在");
            }
        }
        boolean updated = templateService.updateById(entity);
        if (updated) {
            templateService.saveBindings(entity.getId(), entity.getBindings());
        }
        return Result.success(updated);
    }

    @GetMapping("/{id}/bindings")
    @Operation(summary = "获取模板绑定规则")
    @RequiresPermissions("system:material:template:view")
    public Result<List<MaterialTemplateBinding>> getBindings(@PathVariable Long id) {
        return Result.success(templateService.getBindings(id));
    }

    @PutMapping("/{id}/bindings")
    @Operation(summary = "设置模板绑定规则")
    @RequiresPermissions("system:material:template:edit")
    public Result<Boolean> saveBindings(@PathVariable Long id, @RequestBody List<MaterialTemplateBinding> bindings) {
        templateService.saveBindings(id, bindings);
        return Result.success(true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除资料项模板")
    @RequiresPermissions("system:material:template:delete")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除资料项模板 id={}", id);
        // 级联删除绑定规则
        templateService.saveBindings(id, null);
        return Result.success(templateService.removeById(id));
    }
}
