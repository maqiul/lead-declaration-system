package com.declaration.flowable;

import com.declaration.entity.DeclarationForm;
import com.declaration.entity.DeclarationAttachment;
import com.declaration.service.DeclarationFormService;
import com.declaration.service.DeclarationAttachmentService;
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
 * 
 * 简化流程:
 * 提交申报 → 生成预录入单 → 审核 → 通过/驳回
 *   - 通过 → 生成海关报关单 → 完成
 *   - 驳回 → 回到草稿
 * 
 * 状态定义:
 * 0 - 草稿
 * 1 - 待审核
 * 2 - 已完成
 */
@Slf4j
@Component("declarationTaskListener")
@RequiredArgsConstructor
public class DeclarationTaskListener implements TaskListener, ExecutionListener {

    private final DeclarationFormService declarationFormService;
    private final DeclarationAttachmentService attachmentService;
    private final RuntimeService runtimeService;

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();

        String businessKey = null;
        try {
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(delegateTask.getProcessInstanceId())
                    .singleResult();
            if (pi != null) {
                businessKey = pi.getBusinessKey();
            }
        } catch (Exception e) {
            log.warn("获取流程业务 Key 失败:{}", e.getMessage());
        }

        handleEvent(businessKey, taskDefinitionKey, eventName, false, delegateTask);
    }

    @Override
    public void notify(DelegateExecution execution) {
        String eventName = execution.getEventName();
        String currentActivityId = execution.getCurrentActivityId();
        String businessKey = execution.getProcessInstanceBusinessKey();

        handleEvent(businessKey, currentActivityId, eventName, true, null);
    }

    private void handleEvent(String businessKey, String nodeKey, String eventName, boolean isExecution, DelegateTask delegateTask) {
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

            if ("create".equals(eventName) || "end".equals(eventName) || "complete".equals(eventName)) {
                updateStatusByTask(form, nodeKey, eventName, delegateTask);
            }
        } catch (NumberFormatException e) {
            log.error("业务Key解析失败: {}", businessKey);
        }
    }

    /**
     * 根据当前任务/节点和事件类型更新申报单状态
     *
     * 简化流程状态定义:
     * 0 - 草稿 (初始状态或驳回后)
     * 1 - 待审核 (已提交,已生成预录入单)
     * 2 - 已完成 (审核通过,已生成海关报关单)
     */
    private void updateStatusByTask(DeclarationForm form, String taskKey, String eventName, DelegateTask delegateTask) {
        Integer newStatus = null;

        // 根据事件类型处理不同逻辑
        if ("end".equals(eventName) || "complete".equals(eventName)) {
            // 任务完成事件 - 需要判断是通过还是驳回
            Boolean approved = getApprovalVariable(delegateTask);

            if (approved == null || approved) {
                // 通过场景:正常推进状态
                switch (taskKey) {
                    case "deptAudit":            // 审核申报完成
                        newStatus = 2;           // 已完成 (自动生成海关报关单后)
                        break;
                }
            } else {
                // 驳回场景:不更新状态,等待驳回到目标节点后的 create 事件来更新状态
                log.info("节点 {} 驳回 (approved=false),暂不更新状态", taskKey);
            }
        }
        
        // 任务创建事件 - 处理任务到达时的状态更新(包括驳回回来的情况)
        if (newStatus == null) {
            switch (taskKey) {
                case "genPreEntryTask":          // 生成预录入表单 (ServiceTask,不更新状态)
                    break;
                case "deptAudit":                // 审核申报
                    newStatus = 1;               // 待审核
                    break;
                case "rejectHandler":            // 驳回修改
                    newStatus = 0;               // 草稿
                    break;
                case "genCustomsDoc":            // 生成海关报关单 (ServiceTask,不更新状态)
                    break;
                case "endEvent":                 // 流程完成
                    newStatus = 2;               // 已完成
                    break;
            }
        }

        // 更新状态(仅当状态变化时)
        if (newStatus != null && !newStatus.equals(form.getStatus())) {
            if (shouldUpdateStatus(form.getStatus(), newStatus)) {
                // 如果是驳回回到草稿状态,删除预录入单
                if (newStatus == 0 && form.getStatus() != 0) {
                    deletePreEntryDocuments(form.getId());
                }

                form.setStatus(newStatus);
                declarationFormService.updateById(form);
                log.info("申报单 {} (ID={}) 状态更新为: {} (节点: {}, 事件: {})",
                         form.getFormNo(), form.getId(), newStatus, taskKey, eventName);
            }
        }
    }

    /**
     * 从流程变量中获取 approved 标志
     */
    private Boolean getApprovalVariable(DelegateTask delegateTask) {
        try {
            if (delegateTask != null && delegateTask.hasVariable("approved")) {
                Object approvedObj = delegateTask.getVariable("approved");
                if (approvedObj instanceof Boolean) {
                    return (Boolean) approvedObj;
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("获取 approved 变量失败:{}", e.getMessage());
            return null;
        }
    }

    /**
     * 判断是否应该更新状态
     * 简化流程状态更新规则: 0→1→2
     */
    private boolean shouldUpdateStatus(Integer currentStatus, Integer newStatus) {
        if (currentStatus == null || newStatus == null) {
            return true;
        }

        // 回到草稿(驳回场景)
        if (newStatus == 0) {
            return true;
        }

        // 正常推进: 0→1→2
        if (newStatus > currentStatus) {
            return true;
        }

        return false;
    }

    /**
     * 删除预录入单文档
     * 当初审驳回时调用，清理自动生成的预录入单
     */
    private void deletePreEntryDocuments(Long formId) {
        try {
            long count = attachmentService.lambdaQuery()
                    .eq(DeclarationAttachment::getFormId, formId)
                    .eq(DeclarationAttachment::getFileType, "FullDocuments")
                    .count();

            if (count > 0) {
                boolean deleted = attachmentService.lambdaUpdate()
                        .eq(DeclarationAttachment::getFormId, formId)
                        .eq(DeclarationAttachment::getFileType, "FullDocuments")
                        .remove();

                if (deleted) {
                    log.info("删除申报单 {} 的预录入单文档 {} 个", formId, count);
                }
            }
        } catch (Exception e) {
            log.error("删除预录入单文档失败，formId={}", formId, e);
        }
    }
}
