package com.declaration.flowable;

import com.declaration.entity.DeclarationForm;
import com.declaration.entity.DeclarationAttachment;
import com.declaration.service.DeclarationFormService;
import com.declaration.service.DeclarationAttachmentService;
import com.declaration.service.DeclarationMaterialItemService;
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
 * 新版流程:
 * 提交申报 → 生成预录入单 → 初审(deptAudit)
 *   - 通过 → 生成海关报关单(genCustomsDoc) → 资料提交(materialSubmit) → 资料审核(materialAudit)
 *          - 通过 → 完成
 *          - 驳回 → 回 materialSubmit
 *   - 驳回 → 回草稿
 *
 * 状态定义:
 * 0 - 草稿
 * 1 - 待初审
 * 2 - 待资料提交（海关报关单已生成）
 * 3 - 待资料审核
 * 4 - 待发票提交（资料审核通过后）
 * 5 - 待发票审核
 * 6 - 已完成（可进入财务流程）
 * 9 - 退回待审
 */
@Slf4j
@Component("declarationTaskListener")
@RequiredArgsConstructor
public class DeclarationTaskListener implements TaskListener, ExecutionListener {

    private final DeclarationFormService declarationFormService;
    private final DeclarationAttachmentService attachmentService;
    private final DeclarationMaterialItemService materialItemService;
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
     * 新版状态定义:
     * 0 - 草稿 (初始状态或驳回后)
     * 1 - 待初审 (已提交，已生成预录入单)
     * 2 - 待资料提交 (初审通过，海关报关单已生成)
     * 3 - 待资料审核 (用户已提交资料)
     * 4 - 待发票提交 (资料审核通过)
     * 5 - 待发票审核 (发票已提交)
     * 6 - 已完成 (发票审核通过)
     */
    private void updateStatusByTask(DeclarationForm form, String taskKey, String eventName, DelegateTask delegateTask) {
        Integer newStatus = null;

        // 任务完成事件 - 需要判断是通过还是驳回
        if ("end".equals(eventName) || "complete".equals(eventName)) {
            Boolean approved = getApprovalVariable(delegateTask);

            if (approved == null || approved) {
                switch (taskKey) {
                    case "genCustomsDoc":         // 海关报关单生成完毕 → 进入待资料提交
                        newStatus = 2;
                        break;
                    case "endEvent":
                        newStatus = 6;
                        break;
                }
            } else {
                log.info("节点 {} 驳回 (approved=false)，暂不更新状态", taskKey);
            }
        }

        // 任务创建事件 - 处理任务到达时的状态更新
        if (newStatus == null) {
            switch (taskKey) {
                case "genPreEntryTask":          // 生成预录入表单 (ServiceTask)
                    break;
                case "deptAudit":                // 初审
                    newStatus = 1;
                    break;
                case "rejectHandler":            // 驳回修改
                    newStatus = 0;
                    break;
                case "genCustomsDoc":            // 生成海关报关单 (ServiceTask create)
                    break;
                case "materialSubmit":           // 资料提交
                    newStatus = 2;
                    // create 时自动同步模板到本申报单
                    if ("create".equals(eventName) && form.getId() != null) {
                        try {
                            materialItemService.syncFromTemplate(form.getId());
                        } catch (Exception e) {
                            log.warn("同步资料项模板失败 formId={}", form.getId(), e);
                        }
                    }
                    break;
                case "materialAudit":            // 资料审核
                    newStatus = 3;
                    break;
                case "invoiceSubmit":            // 业务发票提交
                    newStatus = 4;
                    break;
                case "invoiceAudit":             // 业务发票审核
                    newStatus = 5;
                    break;
                case "endEvent":                 // 流程完成
                    newStatus = 6;
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
     * 新版流程状态更新规则: 0→1→2→3→4→5→6
     */
    private boolean shouldUpdateStatus(Integer currentStatus, Integer newStatus) {
        if (currentStatus == null || newStatus == null) {
            return true;
        }

        // 回到草稿(驳回场景)
        if (newStatus == 0) {
            return true;
        }

        // 资料审核驳回回到 materialSubmit 需要回调 3 → 2
        if (newStatus == 2 && currentStatus != null && currentStatus == 3) {
            return true;
        }

        // 发票审核驳回回到 invoiceSubmit 需要回调 5 → 4
        if (newStatus == 4 && currentStatus != null && currentStatus == 5) {
            return true;
        }

        // 正常推进: 0→1→2→3→4→5→6
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
