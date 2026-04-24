package com.declaration.flowable;

import com.declaration.entity.DeclarationRemittance;
import com.declaration.service.DeclarationRemittanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 水单审核服务任务
 */
@Slf4j
@Component("remittanceServiceTask")
@RequiredArgsConstructor
public class RemittanceServiceTask implements JavaDelegate {

    private final DeclarationRemittanceService remittanceService;

    @Override
    public void execute(DelegateExecution execution) {
        String remittanceIdStr = execution.getVariable("remittanceId", String.class);
        Long remittanceId = Long.parseLong(remittanceIdStr);
        
        DeclarationRemittance remittance = remittanceService.getById(remittanceId);
        if (remittance != null) {
            // 更新水单状态为已审核
            remittance.setStatus(2);
            remittance.setAuditTime(LocalDateTime.now());
            remittanceService.updateById(remittance);
            
            log.info("水单审核通过处理完成, 水单ID: {}", remittanceId);
        }
    }
}
