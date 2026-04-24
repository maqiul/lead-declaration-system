package com.declaration.flowable;

import com.declaration.entity.DeclarationRemittance;
import com.declaration.service.DeclarationRemittanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

/**
 * 水单审核任务监听器
 */
@Slf4j
@Component("remittanceTaskListener")
@RequiredArgsConstructor
public class RemittanceTaskListener implements TaskListener {

    private final DeclarationRemittanceService remittanceService;

    @Override
    public void notify(DelegateTask delegateTask) {
        String eventName = delegateTask.getEventName();
        String processInstanceId = delegateTask.getProcessInstanceId();
        
        // 从流程变量中获取水单ID
        String remittanceIdStr = (String) delegateTask.getVariable("remittanceId");
        Long remittanceId = Long.parseLong(remittanceIdStr);
        
        DeclarationRemittance remittance = remittanceService.getById(remittanceId);
        if (remittance == null) {
            log.error("水单不存在, 水单ID: {}", remittanceId);
            return;
        }
        
        switch (eventName) {
            case "create":
                // 任务创建时
                String taskName = delegateTask.getName();
                if ("财务审核水单".equals(taskName)) {
                    // 更新水单状态为待审核
                    remittance.setStatus(1);
                    remittanceService.updateById(remittance);
                    log.info("水单审核任务创建, 水单ID: {}, 流程实例: {}", remittanceId, processInstanceId);
                } else if ("驳回修改".equals(taskName)) {
                    // 更新水单状态为已驳回
                    remittance.setStatus(3);
                    remittanceService.updateById(remittance);
                    log.info("水单审核驳回, 水单ID: {}", remittanceId);
                }
                break;
                
            case "complete":
                // 任务完成时
                String taskName2 = delegateTask.getName();
                if ("财务审核水单".equals(taskName2)) {
                    Boolean approved = (Boolean) delegateTask.getVariable("approved");
                    if (approved != null && approved) {
                        log.info("水单审核通过, 水单ID: {}", remittanceId);
                    } else {
                        log.info("水单审核驳回, 水单ID: {}", remittanceId);
                    }
                }
                break;
        }
    }
}
