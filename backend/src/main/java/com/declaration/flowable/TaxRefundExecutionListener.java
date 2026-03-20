package com.declaration.flowable;

import com.declaration.entity.TaxRefundApplication;
import com.declaration.service.TaxRefundApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 税务退费流程统一监听器
 * 采用精简设计，主要处理任务分配和基础状态同步
 * 参照申报管理的设计理念，避免过度复杂化
 *
 * @author Administrator
 * @since 2026-03-17
 */
@Slf4j
@Component("taxRefundExecutionListener")
public class TaxRefundExecutionListener implements ExecutionListener, TaskListener {

    @Autowired
    private TaxRefundApplicationService taxRefundApplicationService;
    
    @Autowired
    private RuntimeService runtimeService;

    @Override
    public void notify(DelegateExecution execution) {
        // 执行监听器 - 主要处理流程级别的事件
        String eventName = execution.getEventName();
        String currentActivityId = execution.getCurrentActivityId();
        String businessKey = execution.getProcessInstanceBusinessKey();
        
        // 在关键事件时处理状态更新，增加take事件以确保服务任务完成后能正确更新状态
        if ("start".equals(eventName) || "end".equals(eventName) || "take".equals(eventName)) {
            handleExecutionEvent(businessKey, currentActivityId, eventName);
        }
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        // 任务监听器 - 主要处理任务级别的事件
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
        String processInstanceId = delegateTask.getProcessInstanceId();
        
        // 获取业务Key
        String businessKey = getBusinessKey(processInstanceId);
        if (businessKey == null || businessKey.isEmpty()) {
            log.warn("无法获取业务Key，任务节点: {}", taskDefinitionKey);
            return;
        }

        // 根据任务节点执行相应处理
        handleTaskAssignment(delegateTask, taskDefinitionKey);
    }

    private void handleExecutionEvent(String businessKey, String nodeKey, String eventName) {
        if (businessKey == null || businessKey.isEmpty()) {
            return;
        }

        log.info("流程执行事件触发: 节点={}, 事件={}, 业务Key={}", nodeKey, eventName, businessKey);

        Long applicationId = Long.valueOf(businessKey);
        TaxRefundApplication application = taxRefundApplicationService.getById(applicationId);
        
        if (application == null) {
            log.warn("未找到对应的退税申请: {}", applicationId);
            return;
        }

        // 只对用户任务节点更新业务状态
        if (isUserTaskNode(nodeKey)) {
            updateBusinessStatus(application, nodeKey, eventName);
        }
    }

    private boolean isUserTaskNode(String nodeKey) {
        // 简化流程：只有以下用户任务节点对应业务状态
        // 已删除旧流程节点: returnDocument, invoiceSubmit
        return "departmentSubmit".equals(nodeKey) ||
               "financeReview".equals(nodeKey) ||
               "financeFinalReview".equals(nodeKey) ||
               "endEvent".equals(nodeKey);
    }

    private void handleTaskAssignment(DelegateTask delegateTask, String taskDefinitionKey) {
        log.debug("任务分配监听器触发: 节点={}", taskDefinitionKey);
        
        switch (taskDefinitionKey) {
            case "departmentSubmit":
                // 设置个人任务负责人
                handlePersonalTaskAssignment(delegateTask);
                break;
            case "financeReview":
            case "financeFinalReview":
                // 设置候选组
                handleGroupTaskAssignment(delegateTask);
                break;
        }
    }

    private void handlePersonalTaskAssignment(DelegateTask delegateTask) {
        String initiator = (String) delegateTask.getVariable("initiator");
        if (initiator != null && !initiator.isEmpty()) {
            delegateTask.setAssignee(initiator);
            log.info("设置个人任务负责人: {}", initiator);
        }
    }

    private void handleGroupTaskAssignment(DelegateTask delegateTask) {
        delegateTask.addCandidateGroup("finance");
        log.info("设置候选组: finance");
    }

    private void updateBusinessStatus(TaxRefundApplication application, String nodeKey, String eventName) {
        // 简化状态更新逻辑，只在关键节点更新
        if ("start".equals(eventName) || "take".equals(eventName) || "complete".equals(eventName)) {
            Integer newStatus = getStatusForNode(nodeKey);
            if (newStatus != null && !newStatus.equals(application.getStatus())) {
                application.setStatus(newStatus);
                taxRefundApplicationService.updateById(application);
                log.info("退税申请 {} 状态更新为: {}", application.getApplicationNo(), newStatus);
            }
        }
    }

    private Integer getStatusForNode(String nodeKey) {
        // 简化流程状态映射：
        // 0-草稿 1-已提交 2-财务初审 4-退回补充 6-财务复审 7-已完成 8-已拒绝
        // 已删除状态: 3(文件生成)、5(发票提交)
        switch (nodeKey) {
            case "departmentSubmit": return 1; // 已提交
            case "financeReview": return 2;    // 财务初审
            case "financeFinalReview": return 6; // 财务复审
            case "endEvent": return 7;         // 已完成
            default: return null;
        }
    }

    private String getBusinessKey(String processInstanceId) {
        try {
            ProcessInstance processInstance = 
                runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            return processInstance != null ? processInstance.getBusinessKey() : null;
        } catch (Exception e) {
            log.error("获取业务Key失败", e);
            return null;
        }
    }
}
