package com.declaration.controller;

import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.ProcessDefinition;
import com.declaration.entity.ProcessInstance;
import com.declaration.entity.TaskInstance;
import com.declaration.service.ProcessDefinitionService;
import com.declaration.service.ProcessInstanceService;
import com.declaration.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.repository.Deployment;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 工作流控制器
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
@Tag(name = "工作流管理")
public class WorkflowController {

    private final ProcessDefinitionService processDefinitionService;
    private final ProcessInstanceService processInstanceService;
    private final TaskService taskService;

    @PostMapping("/deploy")
    @Operation(summary = "部署流程定义")
    @RequiresPermissions("workflow:deploy")
    public Result<String> deployProcess(@RequestParam("file") MultipartFile file,
                                       @Valid ProcessDefinition processDefinition) {
        try {
            Deployment deployment = processDefinitionService.deployProcessFromFile(file, processDefinition);
            return Result.success("部署成功", deployment.getId());
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/definitions")
    @Operation(summary = "获取流程定义列表")
    @RequiresPermissions("workflow:definition:list")
    public Result<List<ProcessDefinition>> getProcessDefinitions() {
        List<ProcessDefinition> definitions = processDefinitionService.getProcessDefinitions();
        return Result.success(definitions);
    }

    @PostMapping("/instance/start")
    @Operation(summary = "启动流程实例")
    @RequiresPermissions("workflow:instance:start")
    public Result<ProcessInstance> startProcessInstance(
            @Parameter(description = "流程定义KEY") @RequestParam String processDefinitionKey,
            @Parameter(description = "业务KEY") @RequestParam(required = false) String businessKey,
            @Parameter(description = "流程变量") @RequestBody(required = false) Map<String, Object> variables) {
        ProcessInstance instance = processInstanceService.startProcessInstance(processDefinitionKey, businessKey, variables);
        return Result.success(instance);
    }

    @GetMapping("/instances/my")
    @Operation(summary = "获取我的流程实例")
    public Result<List<ProcessInstance>> getMyProcessInstances() {
        // TODO: 获取当前用户ID
        Long userId = 1L;
        List<ProcessInstance> instances = processInstanceService.getProcessInstancesByStarter(userId);
        return Result.success(instances);
    }

    @GetMapping("/instances/running")
    @Operation(summary = "获取运行中的流程实例")
    @RequiresPermissions("workflow:instance:list")
    public Result<List<ProcessInstance>> getRunningProcessInstances() {
        List<ProcessInstance> instances = processInstanceService.getRunningProcessInstances();
        return Result.success(instances);
    }

    @PostMapping("/instance/suspend/{instanceId}")
    @Operation(summary = "挂起流程实例")
    @RequiresPermissions("workflow:instance:suspend")
    public Result<Void> suspendProcessInstance(@Parameter(description = "流程实例ID") @PathVariable String instanceId) {
        processInstanceService.suspendProcessInstance(instanceId);
        return new Result<Void>().setCode(200).setMessage("挂起成功");
    }

    @PostMapping("/instance/activate/{instanceId}")
    @Operation(summary = "激活流程实例")
    @RequiresPermissions("workflow:instance:activate")
    public Result<Void> activateProcessInstance(@Parameter(description = "流程实例ID") @PathVariable String instanceId) {
        processInstanceService.activateProcessInstance(instanceId);
        return new Result<Void>().setCode(200).setMessage("激活成功");
    }

    @PostMapping("/instance/terminate/{instanceId}")
    @Operation(summary = "终止流程实例")
    @RequiresPermissions("workflow:instance:terminate")
    public Result<Void> terminateProcessInstance(
            @Parameter(description = "流程实例ID") @PathVariable String instanceId,
            @Parameter(description = "终止原因") @RequestParam String reason) {
        processInstanceService.terminateProcessInstance(instanceId, reason);
        return new Result<Void>().setCode(200).setMessage("终止成功");
    }

    @GetMapping("/tasks/assigned")
    @Operation(summary = "获取我的待办任务")
    public Result<List<TaskInstance>> getMyAssignedTasks() {
        // TODO: 获取当前用户ID
        Long userId = 1L;
        List<TaskInstance> tasks = taskService.getAssignedTasks(userId);
        return Result.success(tasks);
    }

    @GetMapping("/tasks/candidate")
    @Operation(summary = "获取我的候选任务")
    public Result<List<TaskInstance>> getMyCandidateTasks() {
        // TODO: 获取当前用户ID和组信息
        Long userId = 1L;
        List<String> groupIds = Arrays.asList("GROUP1", "GROUP2");
        List<TaskInstance> tasks = taskService.getCandidateTasks(userId, groupIds);
        return Result.success(tasks);
    }

    @PostMapping("/task/claim/{taskId}")
    @Operation(summary = "签收任务")
    public Result<Boolean> claimTask(@Parameter(description = "任务ID") @PathVariable String taskId) {
        // TODO: 获取当前用户ID
        Long userId = 1L;
        taskService.claimTask(taskId, userId);
        return Result.success(true);
    }

    @PostMapping("/task/complete/{taskId}")
    @Operation(summary = "完成任务")
    public Result<Boolean> completeTask(
            @Parameter(description = "任务ID") @PathVariable String taskId,
            @Parameter(description = "任务变量") @RequestBody(required = false) Map<String, Object> variables) {
        taskService.completeTask(taskId, variables);
        return Result.success(true);
    }

    @PostMapping("/task/reject/{taskId}")
    @Operation(summary = "驳回任务")
    @RequiresPermissions("workflow:task:reject")
    public Result<Boolean> rejectTask(
            @Parameter(description = "任务ID") @PathVariable String taskId,
            @Parameter(description = "目标节点ID") @RequestParam String targetActivityId,
            @Parameter(description = "驳回原因") @RequestParam String reason) {
        taskService.rejectTask(taskId, targetActivityId, reason);
        return Result.success(true);
    }

    @PostMapping("/task/transfer/{taskId}")
    @Operation(summary = "转办任务")
    @RequiresPermissions("workflow:task:transfer")
    public Result<Boolean> transferTask(
            @Parameter(description = "任务ID") @PathVariable String taskId,
            @Parameter(description = "新办理人ID") @RequestParam Long assigneeId) {
        taskService.transferTask(taskId, assigneeId);
        return Result.success(true);
    }

    @GetMapping("/instance/tasks/{instanceId}")
    @Operation(summary = "获取流程实例任务列表")
    @RequiresPermissions("workflow:task:list")
    public Result<List<TaskInstance>> getTasksByProcessInstance(@Parameter(description = "流程实例ID") @PathVariable String instanceId) {
        List<TaskInstance> tasks = processInstanceService.getTasksByProcessInstance(instanceId);
        return Result.success(tasks);
    }
}