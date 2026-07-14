package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.FlowNodeDao;
import com.declaration.entity.FlowNode;
import com.declaration.service.FlowNodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 流程节点库服务实现
 *
 * @author Administrator
 * @since 2026-06-25
 */
@Slf4j
@Service
public class FlowNodeServiceImpl extends ServiceImpl<FlowNodeDao, FlowNode>
        implements FlowNodeService {

    @Override
    public List<FlowNode> listAll() {
        LambdaQueryWrapper<FlowNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(FlowNode::getTargetStatus);
        return this.list(wrapper);
    }

    @Override
    public List<FlowNode> listByProcessType(String processType) {
        LambdaQueryWrapper<FlowNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowNode::getProcessType, processType)
               .orderByAsc(FlowNode::getTargetStatus);
        return this.list(wrapper);
    }

    @Override
    public List<FlowNode> listUserTaskNodesByProcessType(String processType) {
        LambdaQueryWrapper<FlowNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowNode::getNodeType, "userTask")
               .eq(FlowNode::getProcessType, processType)
               .orderByAsc(FlowNode::getTargetStatus);
        return this.list(wrapper);
    }

    @Override
    public List<FlowNode> listOrchestratableNodes(String processType) {
        LambdaQueryWrapper<FlowNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FlowNode::getNodeType, "userTask", "serviceTask")
               .eq(FlowNode::getProcessType, processType)
               .orderByAsc(FlowNode::getNodeType)  // serviceTask 在前，userTask 在后
               .orderByAsc(FlowNode::getTargetStatus);
        return this.list(wrapper);
    }

    @Override
    public List<FlowNode> listSystemNodes() {
        LambdaQueryWrapper<FlowNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowNode::getIsSystem, 1)
               .eq(FlowNode::getNodeType, "serviceTask");
        return this.list(wrapper);
    }

    @Override
    public FlowNode getByNodeKey(String nodeKey) {
        LambdaQueryWrapper<FlowNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowNode::getNodeKey, nodeKey)
               .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    @Override
    public Integer getTargetStatusByNodeKey(String nodeKey) {
        FlowNode node = getByNodeKey(nodeKey);
        return node != null ? node.getTargetStatus() : null;
    }

    @Override
    public Long createNode(FlowNode node) {
        // 检查 nodeKey 唯一性
        FlowNode existing = getByNodeKey(node.getNodeKey());
        if (existing != null) {
            throw new RuntimeException("节点Key '" + node.getNodeKey() + "' 已存在");
        }
        if (node.getNodeType() == null) {
            node.setNodeType("userTask");
        }
        if (node.getIsSystem() == null) {
            node.setIsSystem(0);
        }
        this.save(node);
        log.info("创建流程节点: id={}, key={}, name={}", node.getId(), node.getNodeKey(), node.getNodeName());
        return node.getId();
    }

    @Override
    public void updateNode(Long id, FlowNode node) {
        FlowNode existing = this.getById(id);
        if (existing == null) {
            throw new RuntimeException("节点不存在: id=" + id);
        }
        // 系统节点不允许修改 nodeKey 和 nodeType
        if (existing.getIsSystem() != null && existing.getIsSystem() == 1) {
            node.setNodeKey(null);
            node.setNodeType(null);
        }
        // 如果修改了 nodeKey，检查唯一性
        if (node.getNodeKey() != null && !node.getNodeKey().equals(existing.getNodeKey())) {
            FlowNode byKey = getByNodeKey(node.getNodeKey());
            if (byKey != null && !byKey.getId().equals(id)) {
                throw new RuntimeException("节点Key '" + node.getNodeKey() + "' 已被使用");
            }
        }
        node.setId(id);
        this.updateById(node);
        log.info("更新流程节点: id={}, key={}", id, existing.getNodeKey());
    }

    @Override
    public void deleteNode(Long id) {
        FlowNode existing = this.getById(id);
        if (existing == null) {
            throw new RuntimeException("节点不存在: id=" + id);
        }
        if (existing.getIsSystem() != null && existing.getIsSystem() == 1) {
            throw new RuntimeException("系统内置节点不可删除: " + existing.getNodeKey());
        }
        this.removeById(id);
        log.info("删除流程节点: id={}, key={}", id, existing.getNodeKey());
    }
}

