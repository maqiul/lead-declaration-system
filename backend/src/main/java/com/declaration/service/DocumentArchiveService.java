package com.declaration.service;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;

/**
 * 文档归档服务 - Flowable服务任务
 * 用于退税流程中归档文件
 * 参照申报管理中DeclarationServiceTask的设计模式
 */
@Slf4j
@Service
public class DocumentArchiveService implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String businessKey = execution.getProcessInstanceBusinessKey();
        String processInstanceId = execution.getProcessInstanceId();
        
        log.info("开始归档文件: businessKey={}, processInstanceId={}", businessKey, processInstanceId);
        
        try {
            // TODO: 实际的归档逻辑
            // 这里可以调用归档服务，将退税相关的文件进行归档
            
            // 设置归档结果作为流程变量
            execution.setVariable("documentArchived", true);
            execution.setVariable("archiveTime", System.currentTimeMillis());
            
            log.info("文件归档完成: businessKey={}", businessKey);
        } catch (Exception e) {
            log.error("文件归档失败: businessKey={}", businessKey, e);
            execution.setVariable("documentArchived", false);
            execution.setVariable("archiveError", e.getMessage());
            // 不抛出异常，让流程继续执行
        }
    }
}
