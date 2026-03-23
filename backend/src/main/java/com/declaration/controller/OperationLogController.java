package com.declaration.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.common.Result;
import com.declaration.entity.OperationLog;
import com.declaration.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志控制器
 *
 * @author Administrator
 * @since 2026-03-23
 */
@Tag(name = "操作日志管理")
@RestController
@RequestMapping("/operation-log")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping("/page")
    @Operation(summary = "分页查询操作日志")
    public Result<Page<OperationLog>> getOperationLogPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operationType,
            @Parameter(description = "业务类型") @RequestParam(required = false) String businessType,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {

        Page<OperationLog> page = new Page<>(current, size);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        // 查询条件
        if (StringUtils.isNotBlank(username)) {
            wrapper.like(OperationLog::getUsername, username);
        }
        if (StringUtils.isNotBlank(operationType)) {
            wrapper.eq(OperationLog::getOperationType, operationType);
        }
        if (StringUtils.isNotBlank(businessType)) {
            wrapper.eq(OperationLog::getBusinessType, businessType);
        }
        if (StringUtils.isNotBlank(startTime)) {
            wrapper.ge(OperationLog::getCreateTime, LocalDateTime.parse(startTime));
        }
        if (StringUtils.isNotBlank(endTime)) {
            wrapper.le(OperationLog::getCreateTime, LocalDateTime.parse(endTime));
        }

        // 按创建时间倒序排列
        wrapper.orderByDesc(OperationLog::getCreateTime);

        Page<OperationLog> result = operationLogService.page(page, wrapper);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询操作日志详情")
    public Result<OperationLog> getOperationLogById(@Parameter(description = "日志ID") @PathVariable Long id) {
        OperationLog log = operationLogService.getById(id);
        return log != null ? Result.success(log) : Result.fail("日志不存在");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除操作日志")
    public Result<Void> deleteOperationLog(@Parameter(description = "日志ID") @PathVariable Long id) {
        boolean removed = operationLogService.removeById(id);
        return removed ? Result.success() : Result.fail("删除失败");
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除操作日志")
    public Result<Void> deleteOperationLogs(@Parameter(description = "日志ID列表") @RequestBody List<Long> ids) {
        boolean removed = operationLogService.removeBatchByIds(ids);
        return removed ? Result.success() : Result.fail("批量删除失败");
    }

    @DeleteMapping("/clean")
    @Operation(summary = "清理过期日志")
    public Result<Void> cleanExpiredLogs(@Parameter(description = "保留天数") @RequestParam(defaultValue = "30") Integer days) {
        operationLogService.cleanExpiredLogs(days);
        return Result.success();
    }

    @GetMapping("/stats")
    @Operation(summary = "获取操作日志统计信息")
    public Result<Object> getOperationLogStats() {
        // 这里可以实现各种统计逻辑
        // 比如：今日操作次数、本周操作次数、各操作类型统计等
        return Result.success("统计功能待实现");
    }
}