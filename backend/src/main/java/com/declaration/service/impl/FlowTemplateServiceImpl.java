package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.FlowNodeDao;
import com.declaration.dao.FlowTemplateDao;
import com.declaration.dao.FlowTemplateNodeDao;
import com.declaration.dao.FlowTemplateStepDao;
import com.declaration.entity.FlowNode;
import com.declaration.entity.FlowTemplate;
import com.declaration.entity.FlowTemplateNode;
import com.declaration.entity.FlowTemplateStep;
import com.declaration.service.FlowTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 流程模板服务实现类
 *
 * @author Administrator
 * @since 2026-06-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowTemplateServiceImpl extends ServiceImpl<FlowTemplateDao, FlowTemplate>
        implements FlowTemplateService {

    private final FlowTemplateStepDao stepDao;
    private final FlowTemplateNodeDao templateNodeDao;
    private final FlowNodeDao flowNodeDao;

    @Override
    public FlowTemplate getTemplateWithSteps(Long id) {
        FlowTemplate template = this.getById(id);
        if (template == null) {
            return null;
        }
        template.setSteps(getStepsByTemplateId(id));
        template.setTemplateNodes(getTemplateNodes(id));
        return template;
    }

    @Override
    public List<FlowTemplate> listAllWithSteps() {
        LambdaQueryWrapper<FlowTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowTemplate::getStatus, 1)
               .orderByDesc(FlowTemplate::getIsDefault)
               .orderByAsc(FlowTemplate::getId);

        List<FlowTemplate> templates = this.list(wrapper);
        for (FlowTemplate tpl : templates) {
            tpl.setSteps(getStepsByTemplateId(tpl.getId()));
            tpl.setTemplateNodes(getTemplateNodes(tpl.getId()));
        }
        return templates;
    }

    @Override
    public List<FlowTemplate> listByProcessType(String processType) {
        LambdaQueryWrapper<FlowTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowTemplate::getStatus, 1)
               .eq(FlowTemplate::getProcessType, processType)
               .orderByDesc(FlowTemplate::getIsDefault)
               .orderByAsc(FlowTemplate::getId);

        List<FlowTemplate> templates = this.list(wrapper);
        for (FlowTemplate tpl : templates) {
            tpl.setSteps(getStepsByTemplateId(tpl.getId()));
            tpl.setTemplateNodes(getTemplateNodes(tpl.getId()));
        }
        return templates;
    }

    @Override
    public List<FlowTemplateStep> getStepsByTemplateId(Long templateId) {
        LambdaQueryWrapper<FlowTemplateStep> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowTemplateStep::getTemplateId, templateId)
               .orderByAsc(FlowTemplateStep::getSortOrder);
        return stepDao.selectList(wrapper);
    }

    @Override
    public List<FlowTemplateNode> getTemplateNodes(Long templateId) {
        LambdaQueryWrapper<FlowTemplateNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowTemplateNode::getTemplateId, templateId)
               .orderByAsc(FlowTemplateNode::getSortOrder);
        List<FlowTemplateNode> templateNodes = templateNodeDao.selectList(wrapper);

        // 填充关联的节点信息
        for (FlowTemplateNode tn : templateNodes) {
            if (tn.getNodeId() != null) {
                FlowNode node = flowNodeDao.selectById(tn.getNodeId());
                tn.setNode(node);
            }
        }
        return templateNodes;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSteps(Long templateId, List<FlowTemplateStep> steps) {
        // 删除现有步骤
        LambdaQueryWrapper<FlowTemplateStep> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(FlowTemplateStep::getTemplateId, templateId);
        stepDao.delete(deleteWrapper);

        // 插入新步骤
        if (steps != null && !steps.isEmpty()) {
            for (int i = 0; i < steps.size(); i++) {
                FlowTemplateStep step = steps.get(i);
                step.setId(null);  // 强制新建
                step.setTemplateId(templateId);
                if (step.getSortOrder() == null) {
                    step.setSortOrder(i + 1);
                }
                if (step.getEnabled() == null) {
                    step.setEnabled(1);
                }
                stepDao.insert(step);
            }
        }
        log.info("模板 {} 步骤配置已更新，共 {} 个步骤", templateId, steps != null ? steps.size() : 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveNodes(Long templateId, List<FlowTemplateNode> nodes) {
        // 删除现有编排
        LambdaQueryWrapper<FlowTemplateNode> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(FlowTemplateNode::getTemplateId, templateId);
        templateNodeDao.delete(deleteWrapper);

        // 插入新编排
        if (nodes != null && !nodes.isEmpty()) {
            for (int i = 0; i < nodes.size(); i++) {
                FlowTemplateNode tn = nodes.get(i);
                tn.setId(null);
                tn.setTemplateId(templateId);
                if (tn.getSortOrder() == null) {
                    tn.setSortOrder(i + 1);
                }
                if (tn.getEnabled() == null) {
                    tn.setEnabled(1);
                }
                templateNodeDao.insert(tn);
            }

            // 同步更新旧的 flow_template_step 表（向下兼容）
            syncStepsFromNodes(templateId, nodes);
        }

        log.info("模板 {} 节点编排已更新，共 {} 个节点", templateId, nodes != null ? nodes.size() : 0);
    }

    /**
     * 从节点编排同步到 flow_template_step（向下兼容）
     */
    private void syncStepsFromNodes(Long templateId, List<FlowTemplateNode> nodes) {
        // 清除旧步骤
        LambdaQueryWrapper<FlowTemplateStep> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(FlowTemplateStep::getTemplateId, templateId);
        stepDao.delete(deleteWrapper);

        // 从节点编排重建步骤
        for (FlowTemplateNode tn : nodes) {
            FlowNode node = flowNodeDao.selectById(tn.getNodeId());
            if (node == null) continue;

            FlowTemplateStep step = new FlowTemplateStep();
            step.setTemplateId(templateId);
            step.setStepKey(node.getNodeKey());
            step.setStepName(node.getNodeName());
            step.setEnabled(tn.getEnabled());
            step.setTargetStatus(node.getTargetStatus());
            step.setFormSection(node.getFormSection());
            step.setSortOrder(tn.getSortOrder());
            stepDao.insert(step);
        }
    }

    @Override
    public Map<String, Boolean> getSkipFlags(Long templateId) {
        // 优先从 flow_template_node 读取
        List<FlowTemplateNode> templateNodes = getTemplateNodes(templateId);
        if (!templateNodes.isEmpty()) {
            Map<String, Boolean> flags = new LinkedHashMap<>();
            for (FlowTemplateNode tn : templateNodes) {
                if (tn.getNode() != null) {
                    flags.put(tn.getNode().getNodeKey(),
                              tn.getEnabled() == null || tn.getEnabled() == 0);
                }
            }
            return flags;
        }
        // 回退到 flow_template_step
        List<FlowTemplateStep> steps = getStepsByTemplateId(templateId);
        Map<String, Boolean> flags = new LinkedHashMap<>();
        for (FlowTemplateStep step : steps) {
            flags.put(step.getStepKey(), step.getEnabled() == null || step.getEnabled() == 0);
        }
        return flags;
    }

    @Override
    public Integer getTargetStatus(Long templateId, String stepKey) {
        if (templateId == null || stepKey == null) {
            return null;
        }
        // 优先从 flow_template_node + flow_node 读取
        List<FlowTemplateNode> templateNodes = getTemplateNodes(templateId);
        if (!templateNodes.isEmpty()) {
            for (FlowTemplateNode tn : templateNodes) {
                if (tn.getNode() != null && stepKey.equals(tn.getNode().getNodeKey())) {
                    return tn.getNode().getTargetStatus();
                }
            }
            return null;
        }
        // 回退到 flow_template_step
        LambdaQueryWrapper<FlowTemplateStep> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowTemplateStep::getTemplateId, templateId)
               .eq(FlowTemplateStep::getStepKey, stepKey)
               .last("LIMIT 1");
        FlowTemplateStep step = stepDao.selectOne(wrapper);
        return step != null ? step.getTargetStatus() : null;
    }

    @Override
    public FlowTemplate getDefaultTemplate() {
        // 先查 is_default=1 的
        LambdaQueryWrapper<FlowTemplate> defaultWrapper = new LambdaQueryWrapper<>();
        defaultWrapper.eq(FlowTemplate::getStatus, 1)
                      .eq(FlowTemplate::getIsDefault, 1)
                      .last("LIMIT 1");
        FlowTemplate tpl = this.getOne(defaultWrapper);
        if (tpl != null) {
            return tpl;
        }

        // 无默认模板时返回第一个启用的
        LambdaQueryWrapper<FlowTemplate> fallback = new LambdaQueryWrapper<>();
        fallback.eq(FlowTemplate::getStatus, 1)
                .orderByAsc(FlowTemplate::getId)
                .last("LIMIT 1");
        return this.getOne(fallback);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(FlowTemplate template) {
        // 如果设为默认，取消其他默认
        if (template.getIsDefault() != null && template.getIsDefault() == 1) {
            clearAllDefaults();
        }

        // 保存模板
        this.save(template);
        Long templateId = template.getId();

        // 从节点库获取所有可编排节点（userTask + serviceTask），自动编排
        LambdaQueryWrapper<FlowNode> nodeWrapper = new LambdaQueryWrapper<>();
        nodeWrapper.in(FlowNode::getNodeType, "userTask", "serviceTask")
                   .orderByAsc(FlowNode::getNodeType)  // serviceTask 在前
                   .orderByAsc(FlowNode::getTargetStatus);
        List<FlowNode> allNodes = flowNodeDao.selectList(nodeWrapper);

        int order = 1;
        for (FlowNode node : allNodes) {
            // 写入 flow_template_node
            FlowTemplateNode tn = new FlowTemplateNode();
            tn.setTemplateId(templateId);
            tn.setNodeId(node.getId());
            tn.setEnabled(1);
            tn.setSortOrder(order);
            templateNodeDao.insert(tn);

            // 同步写入 flow_template_step（向下兼容，仅 userTask）
            if ("userTask".equals(node.getNodeType())) {
                FlowTemplateStep step = new FlowTemplateStep();
                step.setTemplateId(templateId);
                step.setStepKey(node.getNodeKey());
                step.setStepName(node.getNodeName());
                step.setEnabled(1);
                step.setSortOrder(order);
                step.setTargetStatus(node.getTargetStatus());
                step.setFormSection(node.getFormSection());
                stepDao.insert(step);
            }

            order++;
        }

        log.info("创建流程模板: id={}, name={}, code={}, 节点数={}",
                templateId, template.getName(), template.getCode(), allNodes.size());
        return templateId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        // 逻辑删除模板
        this.removeById(id);

        // 物理删除步骤
        LambdaQueryWrapper<FlowTemplateStep> stepDeleteWrapper = new LambdaQueryWrapper<>();
        stepDeleteWrapper.eq(FlowTemplateStep::getTemplateId, id);
        stepDao.delete(stepDeleteWrapper);

        // 物理删除节点编排
        LambdaQueryWrapper<FlowTemplateNode> nodeDeleteWrapper = new LambdaQueryWrapper<>();
        nodeDeleteWrapper.eq(FlowTemplateNode::getTemplateId, id);
        templateNodeDao.delete(nodeDeleteWrapper);

        log.info("删除流程模板: id={}", id);
    }

    /**
     * 取消所有模板的默认标志
     */
    private void clearAllDefaults() {
        this.lambdaUpdate()
            .set(FlowTemplate::getIsDefault, 0)
            .eq(FlowTemplate::getIsDefault, 1)
            .update();
    }
}
