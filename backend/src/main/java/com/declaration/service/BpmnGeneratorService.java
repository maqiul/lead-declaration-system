package com.declaration.service;

import com.declaration.entity.FlowTemplate;

/**
 * BPMN XML 自动生成服务
 * 根据流程模板的节点编排配置自动生成 Flowable BPMN 2.0 XML
 *
 * @author Administrator
 * @since 2026-06-25
 */
public interface BpmnGeneratorService {

    /**
     * 根据模板配置生成 BPMN XML
     *
     * @param templateId 模板ID
     * @return BPMN 2.0 XML 字符串
     */
    String generateBpmnXml(Long templateId);

    /**
     * 生成并部署 BPMN 到 Flowable
     *
     * @param templateId 模板ID
     * @return 部署ID
     */
    String generateAndDeploy(Long templateId);
}
