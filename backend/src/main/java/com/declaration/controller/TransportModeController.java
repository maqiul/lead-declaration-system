package com.declaration.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.TransportMode;
import com.declaration.service.TransportModeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 运输方式配置管理控制器
 *
 * @author Administrator
 * @since 2026-03-23
 */
@Slf4j
@RestController
@RequestMapping("/v1/transport-modes")
@RequiredArgsConstructor
@Tag(name = "运输方式配置管理", description = "运输方式配置相关接口")
public class TransportModeController {

    private final TransportModeService transportModeService;

    /**
     * 分页查询运输方式
     */
    @GetMapping
    @Operation(summary = "分页查询运输方式")
    @RequiresPermissions("system:transport:view")
    public Result<Page<TransportMode>> getTransportModes(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        
        Page<TransportMode> result = transportModeService.getPage(page, size, keyword, status);
        return Result.success(result);
    }

    /**
     * 获取所有启用的运输方式（用于下拉选择）
     */
    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的运输方式")
    public Result<List<TransportMode>> getEnabledTransportModes() {
        List<TransportMode> list = transportModeService.getEnabledList();
        return Result.success(list);
    }

    /**
     * 根据ID获取运输方式
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取运输方式")
    @RequiresPermissions("system:transport:view")
    public Result<TransportMode> getTransportModeById(@Parameter(description = "运输方式ID") @PathVariable Long id) {
        TransportMode transportMode = transportModeService.getById(id);
        if (transportMode == null) {
            return Result.fail("运输方式不存在");
        }
        return Result.success(transportMode);
    }

    /**
     * 新增运输方式
     */
    @PostMapping
    @Operation(summary = "新增运输方式")
    @RequiresPermissions("system:transport:create")
    public Result<Void> addTransportMode(@RequestBody TransportMode transportMode) {
        // 检查名称是否重复
        boolean nameExists = transportModeService.lambdaQuery()
                .eq(TransportMode::getName, transportMode.getName())
                .exists();
        if (nameExists) {
            return Result.fail("运输方式名称(英文)已存在");
        }
        
        // 检查代码是否重复
        if (transportMode.getCode() != null && !transportMode.getCode().isEmpty()) {
            boolean codeExists = transportModeService.lambdaQuery()
                    .eq(TransportMode::getCode, transportMode.getCode())
                    .exists();
            if (codeExists) {
                return Result.fail("运输方式代码已存在");
            }
        }
        
        boolean saved = transportModeService.save(transportMode);
        if (saved) {
            return Result.success();
        } else {
            return Result.fail("新增失败");
        }
    }

    /**
     * 修改运输方式
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改运输方式")
    @RequiresPermissions("system:transport:update")
    public Result<Void> updateTransportMode(
            @Parameter(description = "运输方式ID") @PathVariable Long id,
            @RequestBody TransportMode transportMode) {
        
        TransportMode existing = transportModeService.getById(id);
        if (existing == null) {
            return Result.fail("运输方式不存在");
        }
        
        // 检查名称是否重复（排除自己）
        boolean nameExists = transportModeService.lambdaQuery()
                .eq(TransportMode::getName, transportMode.getName())
                .ne(TransportMode::getId, id)
                .exists();
        if (nameExists) {
            return Result.fail("运输方式名称(英文)已存在");
        }
        
        // 检查代码是否重复（排除自己）
        if (transportMode.getCode() != null && !transportMode.getCode().isEmpty()) {
            boolean codeExists = transportModeService.lambdaQuery()
                    .eq(TransportMode::getCode, transportMode.getCode())
                    .ne(TransportMode::getId, id)
                    .exists();
            if (codeExists) {
                return Result.fail("运输方式代码已存在");
            }
        }
        
        transportMode.setId(id);
        boolean updated = transportModeService.updateById(transportMode);
        if (updated) {
            return Result.success();
        } else {
            return Result.fail("修改失败");
        }
    }

    /**
     * 删除运输方式
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除运输方式")
    @RequiresPermissions("system:transport:delete")
    public Result<Void> deleteTransportMode(@Parameter(description = "运输方式ID") @PathVariable Long id) {
        TransportMode transportMode = transportModeService.getById(id);
        if (transportMode == null) {
            return Result.fail("运输方式不存在");
        }
        
        boolean removed = transportModeService.removeById(id);
        if (removed) {
            return Result.success();
        } else {
            return Result.fail("删除失败");
        }
    }

    /**
     * 启用/禁用运输方式
     */
    @PostMapping("/{id}/toggle-status")
    @Operation(summary = "启用/禁用运输方式")
    @RequiresPermissions("system:transport:update")
    public Result<Void> toggleStatus(
            @Parameter(description = "运输方式ID") @PathVariable Long id,
            @Parameter(description = "状态 0-禁用 1-启用") @RequestParam Integer status) {
        
        TransportMode transportMode = transportModeService.getById(id);
        if (transportMode == null) {
            return Result.fail("运输方式不存在");
        }
        
        transportMode.setStatus(status);
        boolean updated = transportModeService.updateById(transportMode);
        if (updated) {
            return Result.success();
        } else {
            return Result.fail("操作失败");
        }
    }
}
