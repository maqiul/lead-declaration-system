package com.declaration.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.EntityConfig;
import com.declaration.service.EntityConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 主体配置管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/entity-configs")
@RequiredArgsConstructor
@Tag(name = "主体配置管理", description = "主体配置相关接口")
public class EntityConfigController {

    private final EntityConfigService entityConfigService;

    /**
     * 分页查询主体配置
     */
    @GetMapping
    @Operation(summary = "分页查询主体配置")
    @RequiresPermissions("system:entity-config:view")
    public Result<Page<EntityConfig>> getEntityConfigs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {

        Page<EntityConfig> page = new Page<>(current, size);
        LambdaQueryWrapper<EntityConfig> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(EntityConfig::getEntityName, keyword)
                   .or()
                   .like(EntityConfig::getEntityNameCn, keyword);
        }

        if (status != null) {
            wrapper.eq(EntityConfig::getStatus, status);
        }

        wrapper.orderByAsc(EntityConfig::getSort)
               .orderByDesc(EntityConfig::getCreateTime);

        Page<EntityConfig> result = entityConfigService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 获取所有启用的主体（用于下拉选择）
     */
    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的主体")
    public Result<List<EntityConfig>> getEnabledList() {
        List<EntityConfig> list = entityConfigService.getEnabledList();
        return Result.success(list);
    }

    /**
     * 获取默认主体
     */
    @GetMapping("/default")
    @Operation(summary = "获取默认主体")
    public Result<EntityConfig> getDefault() {
        EntityConfig entity = entityConfigService.getDefault();
        return Result.success(entity);
    }

    /**
     * 根据ID获取主体配置
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取主体配置")
    @RequiresPermissions("system:entity-config:view")
    public Result<EntityConfig> getById(@Parameter(description = "主体ID") @PathVariable Long id) {
        EntityConfig entity = entityConfigService.getById(id);
        if (entity == null) {
            return Result.fail("主体不存在");
        }
        return Result.success(entity);
    }

    /**
     * 新增主体配置
     */
    @PostMapping
    @Operation(summary = "新增主体配置")
    @RequiresPermissions("system:entity-config:add")
    public Result<Void> add(@RequestBody EntityConfig entity) {
        // 检查英文名是否重复
        if (entity.getEntityName() != null && !entity.getEntityName().isEmpty()) {
            boolean exists = entityConfigService.lambdaQuery()
                    .eq(EntityConfig::getEntityName, entity.getEntityName())
                    .exists();
            if (exists) {
                return Result.fail("该英文名称已存在");
            }
        }

        // 如果设为默认，取消其他默认
        if (entity.getIsDefault() != null && entity.getIsDefault() == 1) {
            entityConfigService.lambdaUpdate()
                    .set(EntityConfig::getIsDefault, 0)
                    .update();
        }

        boolean saved = entityConfigService.save(entity);
        return saved ? Result.success() : Result.fail("新增失败");
    }

    /**
     * 修改主体配置
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改主体配置")
    @RequiresPermissions("system:entity-config:update")
    public Result<Void> update(
            @Parameter(description = "主体ID") @PathVariable Long id,
            @RequestBody EntityConfig entity) {

        EntityConfig existing = entityConfigService.getById(id);
        if (existing == null) {
            return Result.fail("主体不存在");
        }

        // 检查英文名是否重复（排除自己）
        if (entity.getEntityName() != null && !entity.getEntityName().isEmpty()) {
            boolean exists = entityConfigService.lambdaQuery()
                    .eq(EntityConfig::getEntityName, entity.getEntityName())
                    .ne(EntityConfig::getId, id)
                    .exists();
            if (exists) {
                return Result.fail("该英文名称已存在");
            }
        }

        // 如果设为默认，取消其他默认
        if (entity.getIsDefault() != null && entity.getIsDefault() == 1) {
            entityConfigService.lambdaUpdate()
                    .set(EntityConfig::getIsDefault, 0)
                    .ne(EntityConfig::getId, id)
                    .update();
        }

        entity.setId(id);
        boolean updated = entityConfigService.updateById(entity);
        return updated ? Result.success() : Result.fail("修改失败");
    }

    /**
     * 删除主体配置
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除主体配置")
    @RequiresPermissions("system:entity-config:delete")
    public Result<Void> delete(@Parameter(description = "主体ID") @PathVariable Long id) {
        EntityConfig entity = entityConfigService.getById(id);
        if (entity == null) {
            return Result.fail("主体不存在");
        }

        boolean removed = entityConfigService.removeById(id);
        return removed ? Result.success() : Result.fail("删除失败");
    }

    /**
     * 启用/禁用主体配置
     */
    @PostMapping("/{id}/toggle-status")
    @Operation(summary = "启用/禁用主体配置")
    @RequiresPermissions("system:entity-config:update")
    public Result<Void> toggleStatus(
            @Parameter(description = "主体ID") @PathVariable Long id,
            @Parameter(description = "状态 0-禁用 1-启用") @RequestParam Integer status) {

        EntityConfig entity = entityConfigService.getById(id);
        if (entity == null) {
            return Result.fail("主体不存在");
        }

        entity.setStatus(status);
        boolean updated = entityConfigService.updateById(entity);
        return updated ? Result.success() : Result.fail("操作失败");
    }

    /**
     * 设置默认主体
     */
    @PostMapping("/{id}/set-default")
    @Operation(summary = "设置默认主体")
    @RequiresPermissions("system:entity-config:update")
    public Result<Void> setDefault(@Parameter(description = "主体ID") @PathVariable Long id) {
        EntityConfig entity = entityConfigService.getById(id);
        if (entity == null) {
            return Result.fail("主体不存在");
        }

        // 取消所有默认
        entityConfigService.lambdaUpdate()
                .set(EntityConfig::getIsDefault, 0)
                .update();

        // 设置当前主体为默认
        entity.setIsDefault(1);
        boolean updated = entityConfigService.updateById(entity);
        return updated ? Result.success() : Result.fail("设置失败");
    }
}
