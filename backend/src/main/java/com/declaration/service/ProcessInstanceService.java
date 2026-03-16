package com.declaration.service;

import com.declaration.entity.ProcessInstance;
import com.declaration.entity.TaskInstance;

import java.util.List;
import java.util.Map;

/**
 * 流程实例服务接口
 *
 * @author Administrator
 * @since 2026-03-13
 */
public interface ProcessInstanceService {

    /**
     * 启动流程实例
     *
     * @param processDefinitionKey 流程定义KEY
     * @param businessKey 业务KEY
     * @param variables 流程变量
     * @return 流程实例
     */
    ProcessInstance startProcessInstance(String processDefinitionKey, String businessKey, Map<String, Object> variables);

    /**
     * 根据ID获取流程实例
     *
     * @param processInstanceId 流程实例ID
     * @return 流程实例
     */
    ProcessInstance getProcessInstance(String processInstanceId);

    /**
     * 获取用户发起的流程实例列表
     *
     * @param starterId 发起人ID
     * @return 流程实例列表
     */
    List<ProcessInstance> getProcessInstancesByStarter(Long starterId);

    /**
     * 获取运行中的流程实例
     *
     * @return 流程实例列表
     */
    List<ProcessInstance> getRunningProcessInstances();

    /**
     * 挂起流程实例
     *
     * @param processInstanceId 流程实例ID
     */
    void suspendProcessInstance(String processInstanceId);

    /**
     * 激活流程实例
     *
     * @param processInstanceId 流程实例ID
     */
    void activateProcessInstance(String processInstanceId);

    /**
     * 终止流程实例
     *
     * @param processInstanceId 流程实例ID
     * @param reason 终止原因
     */
    void terminateProcessInstance(String processInstanceId, String reason);

    /**
     * 删除流程实例
     *
     * @param processInstanceId 流程实例ID
     * @param reason 删除原因
     */
    void deleteProcessInstance(String processInstanceId, String reason);

    /**
     * 获取流程实例的任务列表
     *
     * @param processInstanceId 流程实例ID
     * @return 任务列表
     */
    List<TaskInstance> getTasksByProcessInstance(String processInstanceId);
}