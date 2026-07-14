package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.FlowNode;

import java.util.List;

/**
 * 流程节点库服务接口
 *
 * @author Administrator
 * @since 2026-06-25
 */
public interface FlowNodeService extends IService<FlowNode> {

    /**
     * 获取所有节点列表
     */
    List<FlowNode> listAll();

    /**
     * 按流程类型获取节点列表
     */
    List<FlowNode> listByProcessType(String processType);

    /**
     * 按流程类型获取业务 userTask 节点（用于模板编排选择）
     */
    List<FlowNode> listUserTaskNodesByProcessType(String processType);

    /**
     * 按流程类型获取可编排的节点（userTask + serviceTask，用于模板编排选择）
     */
    List<FlowNode> listOrchestratableNodes(String processType);

    /**
     * 获取所有系统节点（serviceTask，BPMN 生成时自动插入）
     */
    List<FlowNode> listSystemNodes();

    /**
     * 根据 nodeKey 获取节点
     */
    FlowNode getByNodeKey(String nodeKey);

    /**
     * 根据 nodeKey 获取 targetStatus
     */
    Integer getTargetStatusByNodeKey(String nodeKey);

    /**
     * 创建节点
     */
    Long createNode(FlowNode node);

    /**
     * 更新节点
     */
    void updateNode(Long id, FlowNode node);

    /**
     * 删除节点（系统节点不可删）
     */
    void deleteNode(Long id);
}
