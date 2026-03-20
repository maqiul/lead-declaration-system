package com.declaration.controller;

import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.dto.ConfigOptionDTO;
import com.declaration.entity.SystemConfig;
import com.declaration.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 *
 * @author Administrator
 * @since 2026-03-14
 */
@Slf4j
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
@Tag(name = "系统配置管理", description = "系统配置相关接口")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping("/basic-info")
    @Operation(summary = "获取系统基本信息")
    public Result<Map<String, String>> getSystemBasicInfo() {
        Map<String, String> basicInfo = systemConfigService.getSystemBasicInfo();
        return Result.success(basicInfo);
    }

    @GetMapping("/ui-config")
    @Operation(summary = "获取UI配置")
    public Result<Map<String, String>> getUiConfig() {
        Map<String, String> uiConfig = systemConfigService.getUiConfig();
        return Result.success(uiConfig);
    }

    @GetMapping("/group/{group}")
    @Operation(summary = "根据分组获取配置")
    public Result<List<SystemConfig>> getConfigsByGroup(@PathVariable String group) {
        List<SystemConfig> configs = systemConfigService.getConfigsByGroup(group);
        return Result.success(configs);
    }

    @GetMapping("/{configKey}")
    @Operation(summary = "根据配置键获取配置值")
    public Result<String> getConfigValue(@PathVariable String configKey) {
        String value = systemConfigService.getConfigValue(configKey);
        return Result.success(value);
    }

    @PutMapping("/{configKey}")
    @Operation(summary = "更新配置值")
    @RequiresPermissions("system:config:update")
    public Result<Boolean> updateConfigValue(@PathVariable String configKey, @RequestBody Map<String, String> params) {
        String configValue = params.get("configValue");
        boolean success = systemConfigService.updateConfigValue(configKey, configValue);
        return Result.success(success);
    }

    @GetMapping("/all")
    @Operation(summary = "获取所有启用的配置")
    public Result<List<SystemConfig>> getAllEnabledConfigs() {
        List<SystemConfig> configs = systemConfigService.getAllEnabledConfigs();
        return Result.success(configs);
    }

    @PostMapping
    @Operation(summary = "新增配置")
    @RequiresPermissions("system:config:add")
    public Result<Boolean> addConfig(@RequestBody SystemConfig config) {
        boolean success = systemConfigService.save(config);
        return Result.success(success);
    }

    @PutMapping
    @Operation(summary = "修改配置")
    @RequiresPermissions("system:config:update")
    public Result<Boolean> updateConfig(@RequestBody SystemConfig config) {
        boolean success = systemConfigService.updateById(config);
        return Result.success(success);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置")
    @RequiresPermissions("system:config:delete")
    public Result<Boolean> deleteConfig(@PathVariable Long id) {
        boolean success = systemConfigService.removeById(id);
        return Result.success(success);
    }

    @GetMapping("/options/{configKey}")
    @Operation(summary = "获取配置选项")
    public Result<List<ConfigOptionDTO>> getConfigOptions(@PathVariable String configKey) {
        List<ConfigOptionDTO> options = systemConfigService.getConfigOptions(configKey);
        return Result.success(options);
    }

    @PostMapping("/refresh-cache/{configKey}")
    @Operation(summary = "刷新配置缓存")
    @RequiresPermissions("system:config:update")
    public Result<Boolean> refreshConfigCache(@PathVariable String configKey) {
        systemConfigService.refreshConfigCache(configKey);
        return Result.success(true);
    }

    @GetMapping("/system-params")
    @Operation(summary = "获取系统内置参数")
    public Result<List<SystemConfig>> getSystemParameters() {
        List<SystemConfig> params = systemConfigService.getSystemParameters();
        return Result.success(params);
    }

    @GetMapping("/select-options/{configKey}")
    @Operation(summary = "获取下拉框选项")
    public Result<List<ConfigOptionDTO>> getSelectOptions(@PathVariable String configKey) {
        List<ConfigOptionDTO> options = systemConfigService.getSelectOptions(configKey);
        return Result.success(options);
    }

    @PostMapping("/redis/{configKey}")
    @Operation(summary = "将配置存储到Redis")
    @RequiresPermissions("system:config:update")
    public Result<Boolean> saveConfigToRedis(@PathVariable String configKey, @RequestBody Map<String, String> params) {
        String configValue = params.get("configValue");
        boolean success = systemConfigService.saveConfigToRedis(configKey, configValue);
        return Result.success(success);
    }

    @GetMapping("/redis/{configKey}")
    @Operation(summary = "从Redis获取配置")
    public Result<String> getConfigFromRedis(@PathVariable String configKey) {
        String value = systemConfigService.getConfigFromRedis(configKey);
        return Result.success(value);
    }

    @PostMapping("/redis/batch-save")
    @Operation(summary = "批量将配置存储到Redis")
    @RequiresPermissions("system:config:update")
    public Result<Boolean> batchSaveConfigsToRedis(@RequestBody Map<String, String> configs) {
        boolean success = systemConfigService.batchSaveConfigsToRedis(configs);
        return Result.success(success);
    }

    @PostMapping("/redis/batch-get")
    @Operation(summary = "从Redis批量获取配置")
    public Result<Map<String, String>> batchGetConfigsFromRedis(@RequestBody List<String> configKeys) {
        Map<String, String> configs = systemConfigService.batchGetConfigsFromRedis(configKeys);
        return Result.success(configs);
    }
}