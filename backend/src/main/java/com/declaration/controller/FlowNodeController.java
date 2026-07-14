package com.declaration.controller;

import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.FlowNode;
import com.declaration.service.FlowNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程节点库管理控制器
 *
 * @author Administrator
 * @since 2026-06-25
 */
@Slf4j
@RestController
@RequestMapping("/v1/flow-nodes")
@RequiredArgsConstructor
@Tag(name = "流程节点库", description = "全局流程节点定义与管理")
public class FlowNodeController {

    private final FlowNodeService flowNodeService;

    /**
     * 获取所有节点列表（可按流程类型过滤）
     */
    @GetMapping
    @Operation(summary = "获取所有节点列表")
    @RequiresPermissions("system:flow-node:view")
    public Result<List<FlowNode>> list(
            @Parameter(description = "流程类型过滤") @RequestParam(required = false) String processType) {
        if (processType != null && !processType.isEmpty()) {
            return Result.success(flowNodeService.listByProcessType(processType));
        }
        return Result.success(flowNodeService.listAll());
    }

    /**
     * 获取可用于模板编排的 userTask 节点（可按流程类型过滤）
     */
    @GetMapping("/user-tasks")
    @Operation(summary = "获取可用于模板编排的业务节点")
    @RequiresPermissions("system:flow-node:view")
    public Result<List<FlowNode>> listUserTasks(
            @Parameter(description = "流程类型过滤") @RequestParam(required = false) String processType) {
        if (processType != null && !processType.isEmpty()) {
            return Result.success(flowNodeService.listUserTaskNodesByProcessType(processType));
        }
        return Result.success(flowNodeService.listUserTaskNodesByProcessType("declaration"));
    }

    /**
     * 获取可编排的节点（userTask + serviceTask，可按流程类型过滤）
     */
    @GetMapping("/orchestratable")
    @Operation(summary = "获取可编排的节点（userTask + serviceTask）")
    @RequiresPermissions("system:flow-node:view")
    public Result<List<FlowNode>> listOrchestratable(
            @Parameter(description = "流程类型过滤") @RequestParam(required = false) String processType) {
        if (processType != null && !processType.isEmpty()) {
            return Result.success(flowNodeService.listOrchestratableNodes(processType));
        }
        return Result.success(flowNodeService.listOrchestratableNodes("declaration"));
    }

    /**
     * 获取节点详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取节点详情")
    @RequiresPermissions("system:flow-node:view")
    public Result<FlowNode> getDetail(
            @Parameter(description = "节点ID") @PathVariable Long id) {
        FlowNode node = flowNodeService.getById(id);
        if (node == null) {
            return Result.fail("节点不存在");
        }
        return Result.success(node);
    }

    /**
     * 创建节点
     */
    @PostMapping
    @Operation(summary = "创建流程节点")
    @RequiresPermissions("system:flow-node:add")
    public Result<Long> create(@RequestBody FlowNode node) {
        if (node.getNodeKey() == null || node.getNodeKey().trim().isEmpty()) {
            return Result.fail("节点Key不能为空");
        }
        if (node.getNodeName() == null || node.getNodeName().trim().isEmpty()) {
            return Result.fail("节点名称不能为空");
        }
        Long id = flowNodeService.createNode(node);
        return Result.success(id);
    }

    /**
     * 更新节点
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新流程节点")
    @RequiresPermissions("system:flow-node:update")
    public Result<Void> update(
            @Parameter(description = "节点ID") @PathVariable Long id,
            @RequestBody FlowNode node) {
        flowNodeService.updateNode(id, node);
        return Result.success();
    }

    /**
     * 删除节点
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除流程节点")
    @RequiresPermissions("system:flow-node:delete")
    public Result<Void> delete(
            @Parameter(description = "节点ID") @PathVariable Long id) {
        flowNodeService.deleteNode(id);
        return Result.success();
    }
}
