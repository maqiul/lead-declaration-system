package com.declaration.flowable;

import com.declaration.entity.DeclarationMaterialExemption;
import com.declaration.service.DeclarationMaterialExemptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 资料豁免流程任务/执行监听器
 *
 * 职责：
 * 1. 任务创建时记录当前审核步骤
 * 2. 流程结束（endEvent）时：若 approved=true，complete 主流程被阻塞的任务
 * 3. 驳回时更新豁免记录状态
 */
@Slf4j
@Component("exemptionTaskListener")
@RequiredArgsConstructor
public class ExemptionTaskListener implements TaskListener, org.flowable.engine.delegate.ExecutionListener {

    private final DeclarationMaterialExemptionService exemptionService;
    private final TaskService flowableTaskService;
    private final RuntimeService runtimeService;

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
        String businessKey = getBusinessKey(delegateTask.getProcessInstanceId());

        log.info("豁免流程监听器: 节点={}, 事件={}, businessKey={}", taskDefinitionKey, eventName, businessKey);

        if (businessKey == null || businessKey.isEmpty()) return;

        try {
            Long exemptionId = Long.valueOf(businessKey);

            if ("create".equals(eventName)) {
                // 任务创建：记录当前步骤信息到豁免记录
                DeclarationMaterialExemption exemption = exemptionService.getById(exemptionId);
                if (exemption != null) {
                    String stepInfo = "exemptionAudit".equals(taskDefinitionKey) ? "第一步:豁免审核" : "第二步:豁免复核";
                    log.info("豁免 {} 进入审核步骤: {} (taskId={})", exemptionId, stepInfo, delegateTask.getId());
                }
            }
        } catch (NumberFormatException e) {
            log.warn("豁免流程businessKey解析失败: {}", businessKey);
        }
    }

    @Override
    public void notify(DelegateExecution execution) {
        String eventName = execution.getEventName();
        String currentActivityId = execution.getCurrentActivityId();
        String businessKey = execution.getProcessInstanceBusinessKey();

        log.info("豁免流程执行监听器: 节点={}, 事件={}, businessKey={}", currentActivityId, eventName, businessKey);

        if (businessKey == null || businessKey.isEmpty()) return;
        if (!"end".equals(eventName)) return;

        try {
            Long exemptionId = Long.valueOf(businessKey);
            DeclarationMaterialExemption exemption = exemptionService.getById(exemptionId);
            if (exemption == null) {
                log.warn("豁免记录不存在: {}", exemptionId);
                return;
            }

            // 获取流程变量判断是否通过
            Object approvedObj = execution.getVariable("approved");
            boolean approved = approvedObj instanceof Boolean && (Boolean) approvedObj;

            if (approved) {
                // 流程通过结束：更新豁免状态 + complete主流程任务
                exemption.setStatus(1); // 已通过
                exemption.setAuditTime(LocalDateTime.now());
                exemptionService.updateById(exemption);

                String mainTaskId = exemption.getMainTaskId();
                if (mainTaskId != null && !mainTaskId.isEmpty()) {
                    try {
                        Map<String, Object> variables = new HashMap<>();
                        variables.put("approved", true);
                        variables.put("exemptionApproved", true);
                        flowableTaskService.complete(mainTaskId, variables);
                        log.info("豁免流程通过，已complete主流程任务 exemptionId={} mainTaskId={}", exemptionId, mainTaskId);
                    } catch (Exception e) {
                        log.error("豁免通过后complete主流程任务失败 exemptionId={} mainTaskId={}: {}",
                                exemptionId, mainTaskId, e.getMessage());
                    }
                } else {
                    log.warn("豁免通过但mainTaskId为空 exemptionId={}", exemptionId);
                }
            } else {
                // 流程驳回结束：更新豁免状态为已驳回
                exemption.setStatus(2); // 已驳回
                exemption.setAuditTime(LocalDateTime.now());
                exemptionService.updateById(exemption);
                log.info("豁免流程驳回 exemptionId={}", exemptionId);
            }
        } catch (NumberFormatException e) {
            log.warn("豁免流程businessKey解析失败: {}", businessKey);
        }
    }

    private String getBusinessKey(String processInstanceId) {
        try {
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            return pi != null ? pi.getBusinessKey() : null;
        } catch (Exception e) {
            log.warn("获取豁免流程businessKey失败: {}", e.getMessage());
            return null;
        }
    }
}
