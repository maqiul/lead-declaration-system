package com.declaration.flowable;

import com.declaration.entity.DeclarationForm;
import com.declaration.service.DeclarationFormService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

/**
 * 申报单流程任务监听器 - 自动同步业务状态
 */
@Slf4j
@Component("declarationTaskListener")
@RequiredArgsConstructor
public class DeclarationTaskListener implements TaskListener {

    private final DeclarationFormService declarationFormService;
    private final org.flowable.engine.RuntimeService runtimeService;

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
        
        // 获取流程实例中的业务Key
        String businessKey = runtimeService.createProcessInstanceQuery()
            .processInstanceId(delegateTask.getProcessInstanceId())
            .singleResult()
            .getBusinessKey();
            
        if (businessKey == null || businessKey.isEmpty()) {
            log.warn("任务 {} 所在的流程实例没有关联的业务Id", delegateTask.getId());
            return;
        }

        log.info("任务监听器触发: 任务={}, 事件={}, 业务Key={}", taskDefinitionKey, eventName, businessKey);

        if (businessKey == null || businessKey.isEmpty()) {
            return;
        }

        Long formId = Long.valueOf(businessKey);
        DeclarationForm form = declarationFormService.getById(formId);
        
        if (form == null) {
            log.warn("未找到对应的申报单: {}", formId);
            return;
        }

        // 根据任务节点和事件更新状态
        if ("create".equals(eventName)) {
            updateStatusByTask(form, taskDefinitionKey);
        } else if ("complete".equals(eventName)) {
            // 可以在任务完成时做一些收尾工作
            log.info("节点 {} 已完成", taskDefinitionKey);
        }
    }

    private void updateStatusByTask(DeclarationForm form, String taskKey) {
        Integer newStatus = null;
        
        switch (taskKey) {
            case "deptAudit":      // 部门审核
                newStatus = 1;     // 待初审
                break;
            case "afterDeptAudit": // 初审通过后（通常是网关后的节点或自动任务）
                newStatus = 2;     // 待付定金
                break;
            case "depositAudit":   // 定金审核
                newStatus = 3;     // 待定金审核
                break;
            case "afterDeposit":   // 定金通过后
                newStatus = 4;     // 待付尾款
                break;
            case "balanceAudit":   // 尾款审核
                newStatus = 5;     // 待尾款审核
                break;
            case "finalStep":      // 流程结束
                newStatus = 6;     // 已完成
                break;
            case "rejectHandler":  // 驳回处理
                newStatus = 0;     // 退回草稿
                break;
        }

        if (newStatus != null && !newStatus.equals(form.getStatus())) {
            form.setStatus(newStatus);
            declarationFormService.updateById(form);
            log.info("申报单 {} (ID={}) 状态自动同步为: {}", form.getFormNo(), form.getId(), newStatus);
        }
    }
}
