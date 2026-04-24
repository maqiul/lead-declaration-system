package com.declaration.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.service.DeclarationRemittanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 水单流程Controller(Flowable集成)
 */
@Slf4j
@Tag(name = "水单流程管理接口")
@RestController
@RequestMapping("/v1/remittance-process")
@RequiredArgsConstructor
public class RemittanceProcessController {

    private final DeclarationRemittanceService remittanceService;

    @GetMapping("/pending-tasks")
    @Operation(summary = "获取待审核任务列表")
    @RequiresPermissions("business:remittance:audit")
    public Result<List<Map<String, Object>>> getPendingTasks() {
        List<Map<String, Object>> tasks = remittanceService.getPendingAuditTasks();
        return Result.success(tasks);
    }

    @GetMapping("/process-instance-id")
    @Operation(summary = "获取水单流程实例ID")
    @RequiresPermissions("business:remittance:view")
    public Result<String> getProcessInstanceId(@Parameter(description = "水单ID") Long remittanceId) {
        String processInstanceId = remittanceService.getProcessInstanceId(remittanceId);
        return Result.success(processInstanceId);
    }
}
