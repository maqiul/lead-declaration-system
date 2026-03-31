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

import com.declaration.entity.FinancialSupplement;
import com.declaration.service.FinancialSupplementService;

/**
 * 申报单流程任务/执行监听器 - 自动同步业务状态
 */
@Slf4j
@Component("declarationTaskListener")
@RequiredArgsConstructor
public class DeclarationTaskListener implements TaskListener, ExecutionListener {

    private final DeclarationFormService declarationFormService;
    private final FinancialSupplementService supplementService;
    private final DeclarationAttachmentService attachmentService;
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
            log.warn("获取流程业务 Key 失败：{}", e.getMessage());
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

            // 处理不同类型的事件
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
     * 完整串行流程状态定义：
     * 0 - 草稿（驳回后）
     * 1 - 待初审
     * 2 - 待上传定金水单（初审通过后）
     * 3 - 定金待审核（用户上传定金水单后）
     * 4 - 待上传尾款水单（定金审核通过后）
     * 5 - 尾款待审核（用户上传尾款水单后）
     * 6 - 待上传提货单（尾款审核通过后）
     * 7 - 提货单待审核（用户上传提货单后）
     * 8 - 已完成
     * 
     * 完整流程控制：
     * 1. 初审通过 → 待上传定金水单 (2)
     * 2. 用户上传定金 → 定金待审核 (3)
     * 3. 定金审核通过 → 待上传尾款水单 (4)
     * 4. 用户上传尾款 → 尾款待审核 (5)
     * 5. 尾款审核通过 → 待上传提货单 (6)
     * 6. 用户上传提货单 → 提货单待审核 (7)
     * 7. 提货单审核通过 → 已完成 (8)
     * 
     * 驳回情况：
     * - 任何审核驳回都会回到上一个上传节点
     * - 状态从审核状态回退到对应的待上传状态（3→2, 5→4, 7→6）
     */
    private void updateStatusByTask(DeclarationForm form, String taskKey, String eventName,DelegateTask delegateTask) {
        // 财务补充任务：自动初始化财务补充记录
        if ("financeUploadTask".equals(taskKey)) {
            initFinancialSupplementIfNeeded(form);
            // 财务补充任务也是并行任务之一，不单独更新主状态
            return;
        }
    
        Integer newStatus = null;
    
        // 根据事件类型处理不同逻辑
        if ("end".equals(eventName) || "complete".equals(eventName)) {
            // 任务完成事件 - 需要判断是通过还是驳回
            Boolean approved = getApprovalVariable(delegateTask);
                
            if (approved == null || approved) {
                // 通过场景：正常推进状态
                switch (taskKey) {
                    case "deptAudit":            // 部门初审完成
                        newStatus = 2;           // 待上传定金水单
                        break;
                    case "depositAudit":         // 财务确认定金完成
                        newStatus = 4;           // 待上传尾款水单
                        break;
                    case "balanceAudit":         // 财务确认尾款完成
                        newStatus = 6;           // 待上传提货单
                        break;
                    case "pickupListAudit":      // 财务确认提货单完成
                        newStatus = 8;           // 已完成
                        break;
                }
            } else {
                // 驳回场景：不更新状态，等待驳回到目标节点后的 create 事件来更新状态
                // 例如：depositAudit 驳回后到 depositPayment，会在 depositPayment 的 create 事件中设置状态=2
                log.info("节点 {} 驳回 (approved=false)，暂不更新状态，等待下一节点 create 事件处理", taskKey);
                    // 任务创建事件 - 处理任务到达时的状态更新（包括驳回回来的情况）
                switch (taskKey) {
                    // === 初审阶段 ===
                    case "deptAudit":            // 部门初审
                        newStatus = 1;           // 待初审
                        break;
                    case "rejectHandler":        // 驳回修改
                        newStatus = 0;           // 草稿（退回修改）
                        break;
                                
                    // === 完整串行处理阶段 ===
                    case "depositPayment":       // 上传定金凭证（可能是驳回回来的）
                        newStatus = 2;           // 待上传定金水单
                        break;
                    case "depositAudit":         // 财务确认定金
                        newStatus = 3;           // 定金待审核
                        break;
                    case "balancePayment":       // 上传尾款凭证（可能是驳回回来的）
                        newStatus = 4;           // 待上传尾款水单
                        break;
                    case "balanceAudit":         // 财务确认尾款
                        newStatus = 5;           // 尾款待审核
                        break;
                    case "pickupListUpload":     // 上传提货单（可能是驳回回来的）
                        newStatus = 6;           // 待上传提货单
                        break;
                    case "pickupListAudit":      // 财务确认提货单
                        newStatus = 7;           // 提货单待审核
                        break;
                                
                    // === 流程结束 ===
                    case "endEvent1":            // 流程完成
                        newStatus = 8;           // 已完成
                        break;
                }
            }
        } else {
            // 任务创建事件 - 处理任务到达时的状态更新（包括驳回回来的情况）
            switch (taskKey) {
                // === 初审阶段 ===
                case "deptAudit":            // 部门初审
                    newStatus = 1;           // 待初审
                    break;
                case "rejectHandler":        // 驳回修改
                    newStatus = 0;           // 草稿（退回修改）
                    break;
                            
                // === 完整串行处理阶段 ===
                case "depositPayment":       // 上传定金凭证（可能是驳回回来的）
                    newStatus = 2;           // 待上传定金水单
                    break;
                case "depositAudit":         // 财务确认定金
                    newStatus = 3;           // 定金待审核
                    break;
                case "balancePayment":       // 上传尾款凭证（可能是驳回回来的）
                    newStatus = 4;           // 待上传尾款水单
                    break;
                case "balanceAudit":         // 财务确认尾款
                    newStatus = 5;           // 尾款待审核
                    break;
                case "pickupListUpload":     // 上传提货单（可能是驳回回来的）
                    newStatus = 6;           // 待上传提货单
                    break;
                case "pickupListAudit":      // 财务确认提货单
                    newStatus = 7;           // 提货单待审核
                    break;
                            
                // === 流程结束 ===
                case "endEvent1":            // 流程完成
                    newStatus = 8;           // 已完成
                    break;
            }
        }

        // 更新状态（仅当状态变化时）
        if (newStatus != null && !newStatus.equals(form.getStatus())) {
            // 串行流程状态更新规则：
            // - 状态只能向前推进或回到草稿状态（驳回）
            // - 审核状态(3,4,5)可以覆盖处理中状态(2)
            if (shouldUpdateStatus(form.getStatus(), newStatus)) {
                // 如果是驳回回到草稿状态，删除预录入单
                if (newStatus == 0 && form.getStatus() != 0) {
                    deletePreEntryDocuments(form.getId());
                }
                
                form.setStatus(newStatus);
                declarationFormService.updateById(form);
                log.info("申报单 {} (ID={}) 状态更新为: {} (节点: {}, 事件: {})", 
                         form.getFormNo(), form.getId(), newStatus, taskKey, eventName);
            } else {
                log.debug("申报单 {} 状态 {} 不更新为 {} (节点: {}, 事件: {})", 
                          form.getFormNo(), form.getStatus(), newStatus, taskKey, eventName);
            }
        }
    }
    
    /**
     * 从流程变量中获取 approved 标志
     * @param delegateTask 任务委托对象
     * @return true-通过，false-驳回，null-未知
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
            log.warn("获取 approved 变量失败：{}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 判断是否应该更新状态
     * 完整串行流程状态更新规则：
     * - 回到草稿（驳回场景）
     * - 进入待初审
     * - 按顺序推进状态：1→2→3→4→5→6→7→8
     * - 驳回时允许回退到上一个"待上传"状态（如 3→2, 5→4, 7→6）
     * - 进入完成状态
     */
    private boolean shouldUpdateStatus(Integer currentStatus, Integer newStatus) {
        if (currentStatus == null || newStatus == null) {
            return true;
        }
        
        // 回到草稿（驳回场景）
        if (newStatus == 0) {
            return true;
        }
        
        // 进入待初审
        if (newStatus == 1) {
            return true;
        }
        
        // 驳回场景：允许从审核状态回退到待上传状态
        // 3 (定金待审核) → 2 (待上传定金)
        // 5 (尾款待审核) → 4 (待上传尾款)
        // 7 (提货单待审核) → 6 (待上传提货单)
        if ((currentStatus == 3 && newStatus == 2) ||
            (currentStatus == 5 && newStatus == 4) ||
            (currentStatus == 7 && newStatus == 6)) {
            return true;
        }
        
        // 完整串行状态推进：1→2→3→4→5→6→7→8
        if (newStatus >= 1 && newStatus <= 8 && newStatus > currentStatus) {
            return true;
        }
        
        // 进入完成状态
        if (newStatus == 8) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 初始化财务补充记录（如果不存在）
     */
    private void initFinancialSupplementIfNeeded(DeclarationForm form) {
        long count = supplementService.lambdaQuery()
                .eq(FinancialSupplement::getFormId, form.getId())
                .count();
        if (count == 0) {
            FinancialSupplement supp = new FinancialSupplement();
            supp.setFormId(form.getId());
            supp.setFormNo(form.getFormNo());
            supp.setStatus(0);
            supplementService.save(supp);
            log.info("自动初始化了申报单 {} 的财务补充记录", form.getFormNo());
        }
    }
    
    /**
     * 删除预录入单文档
     * 当初审驳回时调用，清理自动生成的预录入单
     */
    private void deletePreEntryDocuments(Long formId) {
        try {
            // 先查询要删除的记录数量
            long count = attachmentService.lambdaQuery()
                    .eq(DeclarationAttachment::getFormId, formId)
                    .eq(DeclarationAttachment::getFileType, "FullDocuments")
                    .count();
            
            if (count > 0) {
                // 执行删除操作
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
