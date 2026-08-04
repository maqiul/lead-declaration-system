package com.declaration.flowable;

import com.declaration.entity.MaterialSupplement;
import com.declaration.service.MaterialSupplementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * 资料补交流程任务/执行监听器
 *
 * 职责：
 * 1. 任务创建时记录当前审核步骤（日志）
 * 2. 流程结束（endEvent）时：approved=true → 增量转正；false → 清除增量
 *
 * 补交流程独立于申报主流程，不做任何主流程任务回调（不阻塞）。
 */
@Slf4j
@Component("supplementTaskListener")
@RequiredArgsConstructor
public class SupplementTaskListener implements TaskListener, org.flowable.engine.delegate.ExecutionListener {

    private final MaterialSupplementService supplementService;
    private final RuntimeService runtimeService;

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
        String businessKey = getBusinessKey(delegateTask.getProcessInstanceId());

        log.info("补交流程监听器: 节点={}, 事件={}, businessKey={}", taskDefinitionKey, eventName, businessKey);

        if (businessKey == null || businessKey.isEmpty()) return;

        try {
            Long supplementId = Long.valueOf(businessKey);
            if ("create".equals(eventName)) {
                MaterialSupplement supplement = supplementService.getById(supplementId);
                if (supplement != null) {
                    log.info("补交 {} 进入审核步骤: {} (taskId={})", supplementId, taskDefinitionKey, delegateTask.getId());
                }
            }
        } catch (NumberFormatException e) {
            log.warn("补交流程businessKey解析失败: {}", businessKey);
        }
    }

    @Override
    public void notify(DelegateExecution execution) {
        String eventName = execution.getEventName();
        String currentActivityId = execution.getCurrentActivityId();
        String businessKey = execution.getProcessInstanceBusinessKey();

        log.info("补交流程执行监听器: 节点={}, 事件={}, businessKey={}", currentActivityId, eventName, businessKey);

        if (businessKey == null || businessKey.isEmpty()) return;
        if (!"end".equals(eventName)) return;

        try {
            Long supplementId = Long.valueOf(businessKey);
            MaterialSupplement supplement = supplementService.getById(supplementId);
            if (supplement == null) {
                log.warn("补交记录不存在: {}", supplementId);
                return;
            }
            // 已审核过的记录跳过（防止重复落地）
            if (supplement.getStatus() != null && supplement.getStatus() != 0) {
                log.info("补交 {} 已审核(status={})，跳过监听器落地", supplementId, supplement.getStatus());
                return;
            }

            // 获取流程变量判断是否通过
            Object approvedObj = execution.getVariable("approved");
            boolean approved = approvedObj instanceof Boolean && (Boolean) approvedObj;

            // 落地审核结果：增量转正/清除 + 状态更新 + 留痕（不回调主流程）
            // 备注/审核人优先沿用 complete 任务前已写入补交记录的值
            String remark = (supplement.getAuditRemark() != null && !supplement.getAuditRemark().isEmpty())
                    ? supplement.getAuditRemark()
                    : (approved ? "补交流程审核通过" : "补交流程审核驳回");
            supplementService.applyAuditResult(supplement, approved, remark, supplement.getAuditorId());
            log.info("补交流程结束 supplementId={} approved={}", supplementId, approved);
        } catch (NumberFormatException e) {
            log.warn("补交流程businessKey解析失败: {}", businessKey);
        }
    }

    private String getBusinessKey(String processInstanceId) {
        try {
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            return pi != null ? pi.getBusinessKey() : null;
        } catch (Exception e) {
            log.warn("获取补交流程businessKey失败: {}", e.getMessage());
            return null;
        }
    }
}
