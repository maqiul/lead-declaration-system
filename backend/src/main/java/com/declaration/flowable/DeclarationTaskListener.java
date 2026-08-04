package com.declaration.flowable;

import com.declaration.entity.DeclarationForm;
import com.declaration.entity.DeclarationAttachment;
import com.declaration.service.DeclarationFormService;
import com.declaration.service.DeclarationAttachmentService;
import com.declaration.service.DeclarationMaterialItemService;
import com.declaration.service.DeclarationMaterialExemptionService;
import com.declaration.service.FlowNodeService;
import com.declaration.service.MaterialSupplementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.RepositoryService;
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
 * 4 - 待补充资料提交（资料审核通过后）
 * 5 - 待补充资料审核
 * 6 - 待申请开票金额（补充资料审核通过后）
 * 7 - 待开票金额审核
 * 8 - 待发票提交（开票金额审核通过后）
 * 9 - 待发票审核
 * 10 - 已完成（可进入财务流程）
 * 11 - 退回待审
 */
@Slf4j
@Component("declarationTaskListener")
@RequiredArgsConstructor
public class DeclarationTaskListener implements TaskListener, ExecutionListener {

    private final DeclarationFormService declarationFormService;
    private final DeclarationAttachmentService attachmentService;
    private final DeclarationMaterialItemService materialItemService;
    private final DeclarationMaterialExemptionService exemptionService;
    private final MaterialSupplementService materialSupplementService;
    private final FlowNodeService flowNodeService;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final DeclarationProcessVersionHelper processVersionHelper;

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
                        newStatus = 10;  // 流程完成 → 已完成
                        break;
                }
            } else {
                log.info("节点 {} 驳回 (approved=false)，暂不更新状态", taskKey);
            }
        }

        // 任务创建事件 - 优先从节点库读取 targetStatus，回退到硬编码
        if (newStatus == null && ("create".equals(eventName) || "end".equals(eventName) || "complete".equals(eventName))) {
            // 特殊节点仍然硬编码（系统 serviceTask）
            if (taskKey != null && taskKey.startsWith("rejectHandler")) {
                // 驳回修改节点：根据 BPMN 路由目标从节点库读取正确的 targetStatus
                newStatus = resolveRejectTargetStatus(delegateTask);
                log.info("驳回节点 {} 路由目标状态: {}", taskKey, newStatus);
            } else {
                switch (taskKey) {
                    case "genPreEntryTask":
                        break;
                    case "genCustomsDoc":
                        break;
                    case "endEvent":
                        if (newStatus == null) newStatus = 10;
                        break;
                    default:
                        // 从节点库读取 targetStatus
                        newStatus = flowNodeService.getTargetStatusByNodeKey(taskKey);
                        break;
                }
            }

            // 兼容旧版流程：invoiceSubmit 在旧版 BPMN 中应映射到 status=4
            if ("invoiceSubmit".equals(taskKey) && newStatus != null && isLegacyProcessDefinition(delegateTask)) {
                newStatus = 4;
            }
        }

        // 更新状态(仅当状态变化时)
        if (newStatus != null && !newStatus.equals(form.getStatus())) {
            if (shouldUpdateStatus(form.getStatus(), newStatus)) {
                // 如果是驳回回到草稿状态,删除预录入单
                if (newStatus == 0 && form.getStatus() != 0) {
                    deletePreEntryDocuments(form.getId());
                    // 回草稿同时清理资料补交与豁免流程（按驳回处理：删除增量资料与记录，终止在途流程实例）
                    try {
                        materialSupplementService.cleanupByFormId(form.getId());
                    } catch (Exception e) {
                        log.warn("回草稿清理资料补交流程失败 formId={}: {}", form.getId(), e.getMessage());
                    }
                    try {
                        exemptionService.cleanupByFormId(form.getId());
                    } catch (Exception e) {
                        log.warn("回草稿清理豁免流程失败 formId={}: {}", form.getId(), e.getMessage());
                    }
                }

                form.setStatus(newStatus);
                declarationFormService.updateById(form);
                log.info("申报单 {} (ID={}) 状态更新为: {} (节点: {}, 事件: {})",
                         form.getFormNo(), form.getId(), newStatus, taskKey, eventName);
            }
        }
    }

    private boolean isLegacyProcessDefinition(DelegateTask delegateTask) {
        try {
            if (delegateTask == null || delegateTask.getProcessDefinitionId() == null) {
                return false;
            }
            return !processVersionHelper.isNewVersionDefinition(delegateTask.getProcessDefinitionId());
        } catch (Exception e) {
            log.warn("判断流程版本失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 BPMN 模型中解析 rejectHandler 的路由目标节点，并返回该节点的 targetStatus
     * rejectHandler → (sequenceFlow) → targetNode
     */
    private Integer resolveRejectTargetStatus(DelegateTask delegateTask) {
        try {
            if (delegateTask == null) return null;
            String processDefinitionId = delegateTask.getProcessDefinitionId();
            String taskDefKey = delegateTask.getTaskDefinitionKey();
            org.flowable.bpmn.model.BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
            if (bpmnModel != null) {
                org.flowable.bpmn.model.FlowElement element = bpmnModel.getFlowElement(taskDefKey);
                if (element instanceof org.flowable.bpmn.model.UserTask userTask) {
                    var outgoing = userTask.getOutgoingFlows();
                    if (outgoing != null && !outgoing.isEmpty()) {
                        String targetRef = outgoing.get(0).getTargetRef();
                        if (targetRef != null && !targetRef.isEmpty()) {
                            // 路由目标是审核节点（自循环，如初审驳回 → deptAudit），应回草稿
                            if (targetRef.endsWith("Audit")) {
                                log.info("rejectHandler 路由目标为审核节点: {}，回到草稿", targetRef);
                                return 0;
                            }
                            Integer status = flowNodeService.getTargetStatusByNodeKey(targetRef);
                            log.info("rejectHandler 路由目标: {} → targetStatus={}", targetRef, status);
                            return status;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析 rejectHandler 路由目标失败: {}", e.getMessage());
        }
        return null;
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
     * 新版流程状态更新规则: 0→1→2→3→4→5→6→7→8→9→10
     * 自用申报(SELF): 5→10 (补充资料审核通过后直接完成)
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

        // 补充资料审核驳回回到 supplementSubmit 需要回调 5 → 4
        if (newStatus == 4 && currentStatus != null && currentStatus == 5) {
            return true;
        }

        // 开票金额审核驳回回到 invoiceAmountSubmit 需要回调 7 → 6
        if (newStatus == 6 && currentStatus != null && currentStatus == 7) {
            return true;
        }

        // 发票审核驳回回到 invoiceSubmit 需要回调 9 → 8
        if (newStatus == 8 && currentStatus != null && currentStatus == 9) {
            return true;
        }

        // 退回上一步：下一阶段 → 上一审核节点（数值减小，但不是驳回）
        // 动态适配：支持跳过阶段的流程模板（如 8→5、8→3 等）
        if (newStatus > 0 && newStatus < currentStatus) {
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
