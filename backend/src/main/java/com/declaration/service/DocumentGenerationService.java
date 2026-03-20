package com.declaration.service;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;

/**
 * 文档生成服务 - Flowable服务任务
 * 用于退税流程中生成财务文件
 * 参照申报管理中DeclarationServiceTask的设计模式
 */
@Slf4j
@Service
public class DocumentGenerationService implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        String businessKey = execution.getProcessInstanceBusinessKey();
        String processInstanceId = execution.getProcessInstanceId();
        
        log.info("开始生成财务文件: businessKey={}, processInstanceId={}", businessKey, processInstanceId);
        
        try {
            // TODO: 实际的文档生成逻辑
            // 这里可以调用文档生成服务，生成退税相关的财务文件
            
            // 设置生成结果作为流程变量
            execution.setVariable("documentGenerated", true);
            execution.setVariable("documentGenerationTime", System.currentTimeMillis());
            
            log.info("财务文件生成完成: businessKey={}", businessKey);
        } catch (Exception e) {
            log.error("财务文件生成失败: businessKey={}", businessKey, e);
            execution.setVariable("documentGenerated", false);
            execution.setVariable("documentGenerationError", e.getMessage());
            // 不抛出异常，让流程继续执行
        }
    }
}
