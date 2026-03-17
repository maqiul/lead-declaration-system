package com.declaration.flowable;

import com.declaration.entity.DeclarationForm;
import com.declaration.service.DeclarationFormService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * 申报单流程服务任务 - 处理自动业务逻辑（如生成单证、自动通知等）
 */
@Slf4j
@Component("declarationServiceTask")
@RequiredArgsConstructor
public class DeclarationServiceTask implements JavaDelegate {

    private final DeclarationFormService declarationFormService;

    @Override
    public void execute(DelegateExecution execution) {
        String businessKey = execution.getProcessInstanceBusinessKey();
        String currentActivityId = execution.getCurrentActivityId();

        log.info("执行服务任务: 活动ID={}, 业务Key={}", currentActivityId, businessKey);

        if (businessKey == null || businessKey.isEmpty()) {
            return;
        }

        Long formId = Long.valueOf(businessKey);
        DeclarationForm form = declarationFormService.getById(formId);
        if (form == null) return;

        // 根据活动节点执行不同逻辑
        if ("genContractTask".equals(currentActivityId)) {
            // 模拟生成合同/单证逻辑
            log.info("正在为申报单 {} 生成全套单证...", form.getFormNo());
            // TODO: 调用 ExcelExportService 预生成文件或标记已生成
        }
    }
}
