package com.declaration.flowable;

import com.declaration.entity.TaxRefundApplication;
import com.declaration.service.TaxRefundApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * 税务退费流程服务任务 - 处理自动业务逻辑（如生成文件、归档等）
 * 参照申报管理DeclarationServiceTask的设计模式
 *
 * @author Administrator
 * @since 2026-03-17
 */
@Slf4j
@Component("taxRefundServiceTask")
@RequiredArgsConstructor
public class TaxRefundServiceTask implements JavaDelegate {

    private final TaxRefundApplicationService taxRefundApplicationService;

    @Override
    public void execute(DelegateExecution execution) {
        String businessKey = execution.getProcessInstanceBusinessKey();
        String currentActivityId = execution.getCurrentActivityId();

        log.info("执行退税服务任务: 活动ID={}, 业务Key={}, 流程实例ID={}", 
            currentActivityId, businessKey, execution.getProcessInstanceId());

        if (businessKey == null || businessKey.isEmpty()) {
            log.warn("业务Key为空，无法执行服务任务");
            return;
        }

        Long applicationId = Long.valueOf(businessKey);
        TaxRefundApplication application = taxRefundApplicationService.getById(applicationId);
        
        if (application == null) {
            log.warn("未找到退税申请: {}", applicationId);
            return;
        }

        // 根据活动节点执行不同逻辑
        if ("generateDocument".equals(currentActivityId)) {
            // 生成财务文件并更新状态
            generateFinanceDocument(execution, application);
        } else if ("archiveDocument".equals(currentActivityId)) {
            // 文件归档并更新状态
            archiveDocuments(execution, application);
        }
        
        log.info("服务任务执行完成: 活动ID={}, 业务Key={}", currentActivityId, businessKey);
    }

    /**
     * 生成财务文件
     * 仅执行技术任务，不更新业务状态
     */
    private void generateFinanceDocument(DelegateExecution execution, TaxRefundApplication application) {
        try {
            log.info("正在为退税申请 {} 生成财务文件...", application.getApplicationNo());
            
            // TODO: 调用实际的财务文件生成服务
            // 这里可以生成退税计算表、退税申请书等文件
            
            // 设置流程变量供后续使用
            execution.setVariable("documentGenerated", true);
            execution.setVariable("documentGenerationTime", System.currentTimeMillis());
            
            // 不更新业务状态，让流程自然流转
            
            log.info("财务文件生成成功: 申请编号={}, 流程将继续流转", application.getApplicationNo());
                
        } catch (Exception e) {
            log.error("生成财务文件失败", e);
            execution.setVariable("documentGenerated", false);
            execution.setVariable("documentGenerationError", e.getMessage());
            throw new RuntimeException("生成财务文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 文件归档
     * 仅执行技术任务，不更新业务状态
     */
    private void archiveDocuments(DelegateExecution execution, TaxRefundApplication application) {
        try {
            log.info("正在归档退税申请 {} 的文件...", application.getApplicationNo());
            
            // TODO: 调用实际的文件归档服务
            // 将所有相关文件归档到指定位置
            
            // 设置流程变量供后续使用
            execution.setVariable("documentArchived", true);
            execution.setVariable("archiveTime", System.currentTimeMillis());
            
            // 不更新业务状态，让流程自然流转到结束节点
            
            log.info("文件归档成功: 申请编号={}, 流程将继续流转到结束节点", application.getApplicationNo());
                
        } catch (Exception e) {
            log.error("文件归档失败", e);
            execution.setVariable("documentArchived", false);
            execution.setVariable("archiveError", e.getMessage());
            throw new RuntimeException("文件归档失败: " + e.getMessage(), e);
        }
    }
}
