package com.declaration.controller;

import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.FlowTemplate;
import com.declaration.entity.FlowTemplateNode;
import com.declaration.entity.FlowTemplateStep;
import com.declaration.service.BpmnGeneratorService;
import com.declaration.service.FlowTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 流程模板管理控制器
 *
 * @author Administrator
 * @since 2026-06-25
 */
@Slf4j
@RestController
@RequestMapping("/v1/flow-templates")
@RequiredArgsConstructor
@Tag(name = "流程模板管理", description = "流程模板配置相关接口")
public class FlowTemplateController {

    private final FlowTemplateService flowTemplateService;
    private final BpmnGeneratorService bpmnGeneratorService;

    /**
     * 获取所有模板列表（可按流程类型过滤）
     */
    @GetMapping
    @Operation(summary = "获取所有模板列表（可按流程类型过滤）")
    @RequiresPermissions("system:flow-template:view")
    public Result<List<FlowTemplate>> list(
            @Parameter(description = "流程类型过滤") @RequestParam(required = false) String processType) {
        if (processType != null && !processType.isEmpty()) {
            return Result.success(flowTemplateService.listByProcessType(processType));
        }
        return Result.success(flowTemplateService.listAllWithSteps());
    }

    /**
     * 获取模板详情（含步骤配置）
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取模板详情（含步骤配置）")
    @RequiresPermissions("system:flow-template:view")
    public Result<FlowTemplate> getDetail(
            @Parameter(description = "模板ID") @PathVariable Long id) {
        FlowTemplate template = flowTemplateService.getTemplateWithSteps(id);
        if (template == null) {
            return Result.fail("模板不存在");
        }
        return Result.success(template);
    }

    /**
     * 获取默认模板
     */
    @GetMapping("/default")
    @Operation(summary = "获取默认模板")
    public Result<FlowTemplate> getDefault() {
        FlowTemplate template = flowTemplateService.getDefaultTemplate();
        if (template == null) {
            return Result.fail("暂无可用模板");
        }
        return Result.success(template);
    }

    /**
     * 获取模板的 skip 标志 Map（供流程引擎使用）
     */
    @GetMapping("/{id}/skip-flags")
    @Operation(summary = "获取模板 skip 标志 Map")
    public Result<Map<String, Boolean>> getSkipFlags(
            @Parameter(description = "模板ID") @PathVariable Long id) {
        Map<String, Boolean> flags = flowTemplateService.getSkipFlags(id);
        return Result.success(flags);
    }

    /**
     * 获取模板的步骤配置
     */
    @GetMapping("/{id}/steps")
    @Operation(summary = "获取模板的步骤配置")
    @RequiresPermissions("system:flow-template:view")
    public Result<List<FlowTemplateStep>> getSteps(
            @Parameter(description = "模板ID") @PathVariable Long id) {
        List<FlowTemplateStep> steps = flowTemplateService.getStepsByTemplateId(id);
        return Result.success(steps);
    }

    /**
     * 创建模板
     */
    @PostMapping
    @Operation(summary = "创建流程模板")
    @RequiresPermissions("system:flow-template:add")
    public Result<Long> create(@RequestBody FlowTemplate template) {
        // 校验必填字段
        if (template.getName() == null || template.getName().trim().isEmpty()) {
            return Result.fail("模板名称不能为空");
        }
        if (template.getCode() == null || template.getCode().trim().isEmpty()) {
            return Result.fail("模板编码不能为空");
        }

        // 检查 code 唯一性
        boolean codeExists = flowTemplateService.lambdaQuery()
                .eq(FlowTemplate::getCode, template.getCode())
                .exists();
        if (codeExists) {
            return Result.fail("模板编码 '" + template.getCode() + "' 已存在");
        }

        // 默认启用
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
        if (template.getIsDefault() == null) {
            template.setIsDefault(0);
        }

        Long id = flowTemplateService.createTemplate(template);
        return Result.success(id);
    }

    /**
     * 更新模板基本信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新流程模板")
    @RequiresPermissions("system:flow-template:update")
    public Result<Void> update(
            @Parameter(description = "模板ID") @PathVariable Long id,
            @RequestBody FlowTemplate template) {

        FlowTemplate existing = flowTemplateService.getById(id);
        if (existing == null) {
            return Result.fail("模板不存在");
        }

        // 如果修改了 code，检查唯一性（排除自己）
        if (template.getCode() != null && !template.getCode().equals(existing.getCode())) {
            boolean codeExists = flowTemplateService.lambdaQuery()
                    .eq(FlowTemplate::getCode, template.getCode())
                    .ne(FlowTemplate::getId, id)
                    .exists();
            if (codeExists) {
                return Result.fail("模板编码 '" + template.getCode() + "' 已存在");
            }
        }

        // 如果设为默认，取消其他默认
        if (template.getIsDefault() != null && template.getIsDefault() == 1) {
            flowTemplateService.lambdaUpdate()
                    .set(FlowTemplate::getIsDefault, 0)
                    .ne(FlowTemplate::getId, id)
                    .eq(FlowTemplate::getIsDefault, 1)
                    .update();
        }

        template.setId(id);
        flowTemplateService.updateById(template);
        return Result.success();
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除流程模板")
    @RequiresPermissions("system:flow-template:delete")
    public Result<Void> delete(
            @Parameter(description = "模板ID") @PathVariable Long id) {
        FlowTemplate existing = flowTemplateService.getById(id);
        if (existing == null) {
            return Result.fail("模板不存在");
        }

        flowTemplateService.deleteTemplate(id);
        return Result.success();
    }

    /**
     * 获取模板的节点编排配置
     */
    @GetMapping("/{id}/nodes")
    @Operation(summary = "获取模板的节点编排配置")
    @RequiresPermissions(value = {"system:flow-template:view", "business:declaration:view"}, logical = RequiresPermissions.Logical.OR)
    public Result<List<FlowTemplateNode>> getNodes(
            @Parameter(description = "模板ID") @PathVariable Long id) {
        List<FlowTemplateNode> nodes = flowTemplateService.getTemplateNodes(id);
        return Result.success(nodes);
    }

    /**
     * 根据模板编码获取节点编排配置（供申报表单页使用，无需模板管理权限）
     */
    @GetMapping("/by-code/{code}/nodes")
    @Operation(summary = "根据模板编码获取节点编排配置")
    public Result<List<FlowTemplateNode>> getNodesByCode(
            @Parameter(description = "模板编码") @PathVariable String code) {
        FlowTemplate template = flowTemplateService.lambdaQuery()
                .eq(FlowTemplate::getCode, code)
                .eq(FlowTemplate::getDelFlag, 0)
                .one();
        if (template == null) {
            return Result.fail("模板不存在: " + code);
        }
        List<FlowTemplateNode> nodes = flowTemplateService.getTemplateNodes(template.getId());
        return Result.success(nodes);
    }

    /**
     * 根据模板编码获取模板详情
     */
    @GetMapping("/by-code/{code}")
    @Operation(summary = "根据模板编码获取模板详情")
    public Result<FlowTemplate> getByCode(
            @Parameter(description = "模板编码") @PathVariable String code) {
        FlowTemplate template = flowTemplateService.lambdaQuery()
                .eq(FlowTemplate::getCode, code)
                .eq(FlowTemplate::getDelFlag, 0)
                .one();
        if (template == null) {
            return Result.fail("模板不存在: " + code);
        }
        return Result.success(template);
    }

    /**
     * 生成 BPMN XML（预览，不部署）
     */
    @GetMapping("/{id}/bpmn-preview")
    @Operation(summary = "预览生成的 BPMN XML")
    @RequiresPermissions("system:flow-template:view")
    public Result<String> previewBpmn(
            @Parameter(description = "模板ID") @PathVariable Long id) {
        try {
            String xml = bpmnGeneratorService.generateBpmnXml(id);
            return Result.success(xml);
        } catch (Exception e) {
            return Result.fail("生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成并部署 BPMN 到 Flowable
     */
    @PostMapping("/{id}/deploy-bpmn")
    @Operation(summary = "生成并部署 BPMN 到 Flowable")
    @RequiresPermissions("system:flow-template:update")
    public Result<String> deployBpmn(
            @Parameter(description = "模板ID") @PathVariable Long id) {
        try {
            bpmnGeneratorService.generateAndDeploy(id);
            return Result.success("部署成功");
        } catch (Exception e) {
            return Result.fail("部署失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新节点编排配置（全量替换）
     */
    @PutMapping("/{id}/nodes")
    @Operation(summary = "批量更新节点编排配置")
    @RequiresPermissions("system:flow-template:update")
    public Result<Void> saveNodes(
            @Parameter(description = "模板ID") @PathVariable Long id,
            @RequestBody List<FlowTemplateNode> nodes) {

        FlowTemplate existing = flowTemplateService.getById(id);
        if (existing == null) {
            return Result.fail("模板不存在");
        }

        flowTemplateService.saveNodes(id, nodes);
        return Result.success();
    }

    /**
     * 批量更新步骤配置（全量替换，向下兼容）
     */
    @PutMapping("/{id}/steps")
    @Operation(summary = "批量更新步骤开关配置")
    @RequiresPermissions("system:flow-template:update")
    public Result<Void> saveSteps(
            @Parameter(description = "模板ID") @PathVariable Long id,
            @RequestBody List<FlowTemplateStep> steps) {

        FlowTemplate existing = flowTemplateService.getById(id);
        if (existing == null) {
            return Result.fail("模板不存在");
        }

        flowTemplateService.saveSteps(id, steps);
        return Result.success();
    }
}
