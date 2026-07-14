package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.FlowTemplate;
import com.declaration.entity.FlowTemplateNode;
import com.declaration.entity.FlowTemplateStep;

import java.util.List;
import java.util.Map;

/**
 * 流程模板服务接口
 *
 * @author Administrator
 * @since 2026-06-25
 */
public interface FlowTemplateService extends IService<FlowTemplate> {

    /**
     * 获取模板详情（含步骤列表）
     *
     * @param id 模板ID
     * @return 模板（含steps字段）
     */
    FlowTemplate getTemplateWithSteps(Long id);

    /**
     * 获取所有启用模板列表（含步骤）
     *
     * @return 模板列表
     */
    List<FlowTemplate> listAllWithSteps();

    /**
     * 按流程类型获取模板列表（含步骤）
     *
     * @param processType 流程类型
     * @return 模板列表
     */
    List<FlowTemplate> listByProcessType(String processType);

    /**
     * 获取模板的步骤配置
     *
     * @param templateId 模板ID
     * @return 步骤列表（按sortOrder排序）
     */
    List<FlowTemplateStep> getStepsByTemplateId(Long templateId);

    /**
     * 批量保存/更新步骤配置（全量替换）
     *
     * @param templateId 模板ID
     * @param steps      步骤列表（前端传来完整的9条记录）
     */
    void saveSteps(Long templateId, List<FlowTemplateStep> steps);

    /**
     * 获取模板的 skip 标志 Map
     * Key: stepKey（如 deptAudit）
     * Value: true=跳过，false=启用
     *
     * @param templateId 模板ID
     * @return skip标志Map
     */
    Map<String, Boolean> getSkipFlags(Long templateId);

    /**
     * 获取指定步骤的 target_status
     * 当步骤未找到或未配置时返回 null（调用方应回退到硬编码默认值）
     *
     * @param templateId 模板ID
     * @param stepKey    BPMN任务Key
     * @return target_status 值，或 null
     */
    Integer getTargetStatus(Long templateId, String stepKey);

    /**
     * 获取默认模板
     *
     * @return 默认模板，如无则返回第一个启用的模板
     */
    FlowTemplate getDefaultTemplate();

    /**
     * 创建模板（含步骤初始化）
     *
     * @param template 模板信息
     * @return 创建后的模板ID
     */
    Long createTemplate(FlowTemplate template);

    /**
     * 删除模板（逻辑删除，同时删除步骤和节点编排）
     *
     * @param id 模板ID
     */
    void deleteTemplate(Long id);

    /**
     * 获取模板的节点编排列表（含关联节点详情）
     */
    List<FlowTemplateNode> getTemplateNodes(Long templateId);

    /**
     * 批量保存节点编排（全量替换）
     */
    void saveNodes(Long templateId, List<FlowTemplateNode> nodes);
}
