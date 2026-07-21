package com.declaration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.declaration.entity.DeclarationMaterialExemption;
import com.declaration.service.DeclarationMaterialExemptionService;
import com.declaration.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资料豁免审批接口
 */
@Slf4j
@RestController
@RequestMapping("/v1/material/exemption")
@RequiredArgsConstructor
@Tag(name = "资料豁免审批")
public class DeclarationMaterialExemptionController {

    private final DeclarationMaterialExemptionService exemptionService;
    private final TaskService flowableTaskService;

    /**
     * 查询指定申报单的豁免记录列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询豁免记录列表")
    public Result<List<DeclarationMaterialExemption>> list(@RequestParam Long formId) {
        return Result.success(exemptionService.listByFormId(formId));
    }

    /**
     * 查询单条豁免记录详情
     */
    @GetMapping("/detail")
    @Operation(summary = "查询豁免记录详情")
    public Result<DeclarationMaterialExemption> detail(@RequestParam Long id) {
        DeclarationMaterialExemption exemption = exemptionService.getById(id);
        if (exemption == null) {
            return Result.fail("豁免记录不存在");
        }
        return Result.success(exemption);
    }

    /**
     * 查询待审核的豁免记录（审核人用）
     */
    @GetMapping("/pending")
    @Operation(summary = "查询待审核豁免记录")
    public Result<List<DeclarationMaterialExemption>> pending() {
        List<DeclarationMaterialExemption> list = exemptionService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DeclarationMaterialExemption>()
                        .eq(DeclarationMaterialExemption::getStatus, 0)
                        .orderByDesc(DeclarationMaterialExemption::getCreateTime));
        return Result.success(list);
    }

    /**
     * 查询豁免流程当前步骤信息
     */
    @GetMapping("/current-task")
    @Operation(summary = "查询豁免流程当前审核步骤")
    public Result<Map<String, Object>> currentTask(@RequestParam Long exemptionId) {
        DeclarationMaterialExemption exemption = exemptionService.getById(exemptionId);
        if (exemption == null) {
            return Result.fail("豁免记录不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("exemptionId", exemptionId);
        result.put("exemptionType", exemption.getExemptionType());
        result.put("status", exemption.getStatus());

        String piId = exemption.getProcessInstanceId();
        if (piId != null && !piId.isEmpty()) {
            List<Task> tasks = flowableTaskService.createTaskQuery()
                    .processInstanceId(piId)
                    .list();
            if (!tasks.isEmpty()) {
                Task task = tasks.get(0);
                result.put("taskKey", task.getTaskDefinitionKey());
                result.put("taskName", task.getName());
                result.put("taskId", task.getId());
                // 判断是第几步
                boolean isSecondStep = "exemptionInvoiceAudit".equals(task.getTaskDefinitionKey());
                result.put("step", isSecondStep ? 2 : 1);
                result.put("totalSteps", "INVOICE".equals(exemption.getExemptionType())
                        || "MIXED".equals(exemption.getExemptionType()) ? 2 : 1);
            } else {
                result.put("step", 0);
                result.put("taskName", "流程已结束");
            }
        } else {
            result.put("step", 1);
            result.put("totalSteps", 1);
            result.put("taskName", "豁免审核(无流程实例)");
        }

        return Result.success(result);
    }

    /**
     * 批量查询待审核豁免记录（按 formId 列表）
     * 返回 formId → exemptionId 的映射
     */
    @GetMapping("/batch-pending")
    @Operation(summary = "批量查询豁免状态")
    public Result<Map<String, Map<String, Object>>> batchPending(@RequestParam String formIds) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        if (formIds == null || formIds.isEmpty()) return Result.success(result);
        try {
            List<Long> ids = java.util.Arrays.stream(formIds.split(","))
                    .map(String::trim).map(Long::valueOf).collect(Collectors.toList());
            // 查询所有豁免记录，按 formId 分组取最新一条
            List<DeclarationMaterialExemption> list = exemptionService.list(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DeclarationMaterialExemption>()
                            .in(DeclarationMaterialExemption::getFormId, ids)
                            .orderByDesc(DeclarationMaterialExemption::getCreateTime));

            // 获取当前用户信息，用于判断是否有权审核豁免
            String currentUserId = StpUtil.isLogin() ? String.valueOf(StpUtil.getLoginIdAsLong()) : null;
            List<String> userRoles = StpUtil.isLogin() ? StpUtil.getRoleList() : List.of();

            // 收集待审核豁免的流程实例ID，用于查询当前用户是否有任务
            Map<String, String> formIdToProcessInstanceId = new HashMap<>();
            java.util.Set<Long> seen = new java.util.HashSet<>();
            for (DeclarationMaterialExemption ex : list) {
                if (seen.add(ex.getFormId())) {
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", ex.getId());
                    info.put("status", ex.getStatus());
                    result.put(String.valueOf(ex.getFormId()), info);
                    if (ex.getStatus() == 0 && ex.getProcessInstanceId() != null) {
                        formIdToProcessInstanceId.put(String.valueOf(ex.getFormId()), ex.getProcessInstanceId());
                    }
                }
            }

            // 查询当前用户在豁免流程实例中是否有任务（通过 candidateGroups 匹配）
            if (!formIdToProcessInstanceId.isEmpty() && currentUserId != null) {
                List<String> piIds = new java.util.ArrayList<>(formIdToProcessInstanceId.values());
                // 查询当前用户可处理的豁免任务
                java.util.Set<String> myExemptionPiIds = new java.util.HashSet<>();
                if (!userRoles.isEmpty()) {
                    flowableTaskService.createTaskQuery()
                            .processInstanceIdIn(piIds)
                            .taskCandidateGroupIn(userRoles)
                            .list()
                            .forEach(t -> myExemptionPiIds.add(t.getProcessInstanceId()));
                }
                // assignee 匹配
                flowableTaskService.createTaskQuery()
                        .processInstanceIdIn(piIds)
                        .taskAssignee(currentUserId)
                        .list()
                        .forEach(t -> myExemptionPiIds.add(t.getProcessInstanceId()));

                // 设置 canAudit 字段
                for (Map.Entry<String, String> entry : formIdToProcessInstanceId.entrySet()) {
                    Map<String, Object> info = result.get(entry.getKey());
                    if (info != null) {
                        info.put("canAudit", myExemptionPiIds.contains(entry.getValue()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("批量查询豁免状态失败: {}", e.getMessage());
        }
        return Result.success(result);
    }

    /**
     * 审核豁免（通过/驳回）
     */
    @PostMapping("/audit")
    @Operation(summary = "审核豁免")
    public Result<String> audit(@RequestBody Map<String, Object> body) {
        Object idObj = body.get("id");
        if (idObj == null) return Result.fail("豁免记录ID不能为空");
        Long id = Long.valueOf(idObj.toString());

        Object resultObj = body.get("result"); // 1=通过 2=驳回
        boolean approved = resultObj != null && "1".equals(resultObj.toString());
        String remark = body.get("remark") == null ? "" : body.get("remark").toString();
        Long auditorId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;

        try {
            exemptionService.auditExemption(id, approved, remark, auditorId);
            return Result.success("豁免审核" + (approved ? "通过" : "驳回") + "成功");
        } catch (Exception e) {
            log.warn("豁免审核失败 id={} : {}", id, e.getMessage());
            return Result.fail(e.getMessage());
        }
    }
}
