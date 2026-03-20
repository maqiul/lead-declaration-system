package com.declaration.service;

import com.declaration.entity.ProcessDefinition;
import org.flowable.engine.repository.Deployment;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 流程定义服务接口
 *
 * @author Administrator
 * @since 2026-03-13
 */
public interface ProcessDefinitionService {

    /**
     * 部署流程定义
     *
     * @param bpmnXml BPMN XML内容
     * @param processDefinition 流程定义信息
     * @return 部署信息
     */
    Deployment deployProcess(String bpmnXml, ProcessDefinition processDefinition);

    /**
     * 部署流程定义 (从流部署)
     *
     * @param inputStream BPMN 文件流
     * @param processName 流程名称
     * @param processKey 流程KEY
     * @param category 分类
     * @param description 描述
     * @return 部署信息
     */
    Deployment deployProcess(InputStream inputStream, String processName, String processKey, String category, String description);

    /**
     * 通过文件部署流程
     *
     * @param file BPMN文件
     * @param processDefinition 流程定义信息
     * @return 部署信息
     */
    Deployment deployProcessFromFile(MultipartFile file, ProcessDefinition processDefinition);

    /**
     * 通过资源部署流程
     *
     * @param resourceName 资源名称
     * @param inputStream 资源输入流
     * @param processDefinition 流程定义信息
     * @return 部署信息
     */
    Deployment deployProcessFromResource(String resourceName, InputStream inputStream, ProcessDefinition processDefinition);

    /**
     * 获取流程定义列表
     *
     * @return 流程定义列表
     */
    List<ProcessDefinition> getProcessDefinitions();

    /**
     * 根据KEY获取最新流程定义
     *
     * @param processKey 流程KEY
     * @return 流程定义
     */
    ProcessDefinition getLatestProcessDefinition(String processKey);

    /**
     * 根据KEY获取流程定义的XML原文
     *
     * @param processKey 流程KEY
     * @return XML字符串
     */
    String getProcessDefinitionXml(String processKey);

    /**
     * 挂起流程定义
     *
     * @param processDefinitionId 流程定义ID
     */
    void suspendProcessDefinition(String processDefinitionId);

    /**
     * 激活流程定义
     *
     * @param processDefinitionId 流程定义ID
     */
    void activateProcessDefinition(String processDefinitionId);

    /**
     * 删除流程定义
     *
     * @param processDefinitionId 流程定义ID
     * @param cascade 是否级联删除流程实例
     */
    void deleteProcessDefinition(String processDefinitionId, boolean cascade);
}