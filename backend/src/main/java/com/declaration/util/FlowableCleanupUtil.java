package com.declaration.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Flowable流程清理工具类
 * 用于安全地清理流程定义和实例
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowableCleanupUtil {
    
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    
    /**
     * 清理指定流程定义的所有运行实例
     * @param processDefinitionKey 流程定义Key
     * @return 清理的实例数量
     */
    public int cleanupRunningInstances(String processDefinitionKey) {
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .list();
        
        int count = 0;
        for (ProcessInstance instance : instances) {
            try {
                runtimeService.deleteProcessInstance(instance.getId(), "系统清理");
                log.info("已终止流程实例: {}", instance.getId());
                count++;
            } catch (Exception e) {
                log.error("终止流程实例失败: {}", instance.getId(), e);
            }
        }
        
        log.info("共清理 {} 个运行中的 {} 流程实例", count, processDefinitionKey);
        return count;
    }
    
    /**
     * 检查是否有运行中的流程实例
     * @param processDefinitionKey 流程定义Key
     * @return true表示有运行实例
     */
    public boolean hasRunningInstances(String processDefinitionKey) {
        long count = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .count();
        return count > 0;
    }
}