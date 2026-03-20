package com.declaration.flowable;

import com.declaration.entity.DeclarationForm;
import com.declaration.service.DeclarationFormService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

/**
 * 申报单流程任务/执行监听器 - 自动同步业务状态
 */
@Slf4j
@Component("declarationTaskListener")
@RequiredArgsConstructor
public class DeclarationTaskListener implements TaskListener, ExecutionListener {

    private final DeclarationFormService declarationFormService;
    private final RuntimeService runtimeService;

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();

        // 核心修复：增加 null 检查，避免流程启动瞬时的查询失败导致 NPE
        String businessKey = null;
        try {
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(delegateTask.getProcessInstanceId())
                    .singleResult();
            if (pi != null) {
                businessKey = pi.getBusinessKey();
            }
        } catch (Exception e) {
            log.warn("获取流程业务Key失败: {}", e.getMessage());
        }

        handleEvent(businessKey, taskDefinitionKey, eventName, false);
    }

    @Override
    public void notify(DelegateExecution execution) {
        String eventName = execution.getEventName();
        String currentActivityId = execution.getCurrentActivityId();
        String businessKey = execution.getProcessInstanceBusinessKey();

        handleEvent(businessKey, currentActivityId, eventName, true);
    }

    private void handleEvent(String businessKey, String nodeKey, String eventName, boolean isExecution) {
        if (businessKey == null || businessKey.isEmpty()) {
            return;
        }

        log.info("监听器触发: 节点={}, 事件={}, 业务Key={}, 类型={}", nodeKey, eventName, businessKey, isExecution ? "执行" : "任务");

        try {
            Long formId = Long.valueOf(businessKey);
            DeclarationForm form = declarationFormService.getById(formId);

            if (form == null) {
                log.warn("未找到对应的申报单: {}", formId);
                return;
            }

            // 仅在 create (任务到达) 或是 end (流程到达结束节点) 时更新状态
            if ("create".equals(eventName) || "end".equals(eventName)) {
                updateStatusByTask(form, nodeKey);
            }
        } catch (NumberFormatException e) {
            log.error("业务Key解析失败: {}", businessKey);
        }
    }

    private void updateStatusByTask(DeclarationForm form, String taskKey) {
        Integer newStatus = null;

        switch (taskKey) {
            case "deptAudit": // 部门审核 -> 待初审
                newStatus = 1;
                break;
            case "depositPayment": // 上传定金凭证 -> 待付定金
                newStatus = 2;
                break;
            case "depositAudit": // 财务确认定金 -> 定金待审
                newStatus = 3;
                break;
            case "balancePayment": // 上传尾款凭证 -> 待付尾款
                newStatus = 4;
                break;
            case "balanceAudit": // 财务确认尾款 -> 尾款待审
                newStatus = 5;
                break;
            case "pickupListUpload": // 上传提货单 -> 提货单待传
                newStatus = 6;
                break;
            case "pickupListAudit": // 财务确认提货单 -> 提货单待审
                newStatus = 7;
                break;
            case "endEvent1": // 流程结束 -> 已完成
                newStatus = 8;
                break;
            case "rejectHandler": // 驳回修改 -> 退回草稿
                newStatus = 0;
                break;
        }

        if (newStatus != null && !newStatus.equals(form.getStatus())) {
            form.setStatus(newStatus);
            declarationFormService.updateById(form);
            log.info("申报单 {} (ID={}) 状态自动同步为: {}", form.getFormNo(), form.getId(), newStatus);
        }
    }
}
