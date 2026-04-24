package com.declaration.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.repository.Deployment;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import cn.dev33.satoken.stp.StpUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 工作流管理控制器
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

    @PostMapping("/definition/deploy")
    @Operation(summary = "部署流程定义")
    @RequiresPermissions("workflow:definition:deploy")
    public Result<String> deployProcessDefinition(
            @RequestParam("file") MultipartFile file,
            @RequestParam("processName") String processName,
            @RequestParam("processKey") String processKey,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "description", required = false) String description) {
        
        try {
            Deployment deployment = processDefinitionService.deployProcess(
                file.getInputStream(), 
                processName, 
                processKey, 
                category, 
                description
            );
            return Result.success(deployment.getId());
        } catch (Exception e) {
            log.error("部署流程失败", e);
            return Result.fail("部署流程失败: " + e.getMessage());
        }
    }

    @GetMapping("/definition")
    @Operation(summary = "获取流程定义列表")
    @RequiresPermissions("workflow:definition:view")
    public Result<IPage<ProcessDefinition>> getProcessDefinitions(
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String processName,
            @RequestParam(required = false) String processKey,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        
        List<ProcessDefinition> definitions = processDefinitionService.getProcessDefinitions();
        
        Stream<ProcessDefinition> stream = definitions.stream();
        if (processName != null && !processName.isEmpty()) {
            stream = stream.filter(d -> d.getProcessName() != null && d.getProcessName().contains(processName));
        }
        if (processKey != null && !processKey.isEmpty()) {
            stream = stream.filter(d -> d.getProcessKey() != null && d.getProcessKey().contains(processKey));
        }
        if (category != null && !category.isEmpty()) {
            stream = stream.filter(d -> category.equals(d.getCategory()));
        }
        if (status != null) {
            stream = stream.filter(d -> status.equals(d.getStatus()));
        }
        
        List<ProcessDefinition> filtered = stream.collect(Collectors.toList());
        int total = filtered.size();
        
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<ProcessDefinition> pagedData = fromIndex < total ? filtered.subList(fromIndex, toIndex) : new ArrayList<>();

        Page<ProcessDefinition> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(pagedData);
        
        return Result.success(page);
    }

    @PostMapping("/definition/disable/{processDefinitionId}")
    @Operation(summary = "挂起/停用流程定义")
    @RequiresPermissions("workflow:definition:update")
    public Result<Void> suspendProcessDefinition(@PathVariable String processDefinitionId) {
        processDefinitionService.suspendProcessDefinition(processDefinitionId);
        return Result.success(null);
    }

    @PostMapping("/definition/enable/{processDefinitionId}")
    @Operation(summary = "激活/启用流程定义")
    @RequiresPermissions("workflow:definition:update")
    public Result<Void> activateProcessDefinition(@PathVariable String processDefinitionId) {
        processDefinitionService.activateProcessDefinition(processDefinitionId);
        return Result.success(null);
    }

    @DeleteMapping("/definition/{processDefinitionId}")
    @Operation(summary = "删除流程定义")
    @RequiresPermissions("workflow:definition:delete")
    public Result<Void> deleteProcessDefinition(
            @PathVariable String processDefinitionId,
            @RequestParam(defaultValue = "false") boolean cascade) {
        processDefinitionService.deleteProcessDefinition(processDefinitionId, cascade);
        return Result.success(null);
    }

    @GetMapping("/definition/xml/{processKey}")
    @Operation(summary = "获取流程定义XML内容")
    @RequiresPermissions("workflow:definition:view")
    public Result<String> getProcessDefinitionXml(@PathVariable String processKey) {
        String xml = processDefinitionService.getProcessDefinitionXml(processKey);
        return Result.success(xml);
    }

    @PostMapping("/instance/start")
    @Operation(summary = "启动流程实例")
    @RequiresPermissions("workflow:instance:start")
    public Result<ProcessInstance> startProcessInstance(
            @RequestParam String processDefinitionKey,
            @RequestParam String businessKey,
            @RequestBody(required = false) Map<String, Object> variables) {
        ProcessInstance instance = processInstanceService.startProcessInstance(processDefinitionKey, businessKey, variables);
        return Result.success(instance);
    }

    @GetMapping("/instances/my")
    @Operation(summary = "获取我的流程实例")
    @RequiresPermissions("workflow:instance:view")
    public Result<IPage<ProcessInstance>> getMyProcessInstances(
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @Parameter(description = "流程名称") @RequestParam(required = false) String processName) {
        if (!StpUtil.isLogin()) {
            return Result.fail("用户未登录");
        }
        
        Long userId = StpUtil.getLoginIdAsLong();
        List<ProcessInstance> instances = processInstanceService.getProcessInstancesByStarter(userId);
        
        // 过滤条件
        Stream<ProcessInstance> stream = instances.stream();
        if (processName != null && !processName.isEmpty()) {
            stream = stream.filter(instance -> instance.getProcessName() != null && 
                    instance.getProcessName().contains(processName));
        }
        
        List<ProcessInstance> filtered = stream.collect(Collectors.toList());
        int total = filtered.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<ProcessInstance> pagedData = fromIndex < total ? filtered.subList(fromIndex, toIndex) : new ArrayList<>();

        Page<ProcessInstance> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(pagedData);
        
        return Result.success(page);
    }
    
    @GetMapping("/instances/running")
    @Operation(summary = "获取运行中的流程实例")
    @RequiresPermissions("workflow:instance:view")
    public Result<IPage<ProcessInstance>> getRunningProcessInstances(
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @Parameter(description = "流程名称") @RequestParam(required = false) String processName,
            @Parameter(description = "发起人姓名") @RequestParam(required = false) String starterName,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        List<ProcessInstance> instances = processInstanceService.getRunningProcessInstances();
        
        // 过滤条件
        Stream<ProcessInstance> stream = instances.stream();
        if (processName != null && !processName.isEmpty()) {
            stream = stream.filter(instance -> instance.getProcessName() != null && 
                    instance.getProcessName().contains(processName));
        }
        
        if (starterName != null && !starterName.isEmpty()) {
            stream = stream.filter(instance -> instance.getStarterName() != null && 
                    instance.getStarterName().contains(starterName));
        }
        
        if (status != null) {
            stream = stream.filter(instance -> instance.getStatus() != null && 
                    instance.getStatus().equals(status));
        }
        
        List<ProcessInstance> filtered = stream.collect(Collectors.toList());
        int total = filtered.size();
        
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<ProcessInstance> pagedData = fromIndex < total ? filtered.subList(fromIndex, toIndex) : new ArrayList<>();

        Page<ProcessInstance> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(pagedData);
        
        return Result.success(page);
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
    @RequiresPermissions("workflow:task:view")
    public Result<IPage<TaskInstance>> getMyAssignedTasks(
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @Parameter(description = "任务名称") @RequestParam(required = false) String taskName,
            @Parameter(description = "流程名称") @RequestParam(required = false) String processName) {
        if (!StpUtil.isLogin()) {
            return Result.fail("未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        List<TaskInstance> tasks = taskService.getAssignedTasks(userId);
        
        // 过滤条件
        Stream<TaskInstance> stream = tasks.stream();
        if (taskName != null && !taskName.isEmpty()) {
            stream = stream.filter(task -> task.getTaskName() != null && 
                    task.getTaskName().contains(taskName));
        }
        if (processName != null && !processName.isEmpty()) {
            stream = stream.filter(task -> task.getProcessDefinitionName() != null && 
                    task.getProcessDefinitionName().contains(processName));
        }
        
        List<TaskInstance> filtered = stream.collect(Collectors.toList());
        int total = filtered.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<TaskInstance> pagedData = fromIndex < total ? filtered.subList(fromIndex, toIndex) : new ArrayList<>();

        Page<TaskInstance> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(pagedData);
        
        return Result.success(page);
    }

    @GetMapping("/tasks/candidate")
    @Operation(summary = "获取我的候选任务")
    @RequiresPermissions("workflow:task:view")
    public Result<IPage<TaskInstance>> getMyCandidateTasks(
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @Parameter(description = "任务名称") @RequestParam(required = false) String taskName,
            @Parameter(description = "流程名称") @RequestParam(required = false) String processName) {
        if (!StpUtil.isLogin()) {
            return Result.fail("未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        
        List<String> roleKeys = StpUtil.getRoleList();
        
        List<TaskInstance> tasks = taskService.getCandidateTasks(userId, roleKeys);
        
        // 过滤条件
        Stream<TaskInstance> stream = tasks.stream();
        if (taskName != null && !taskName.isEmpty()) {
            stream = stream.filter(task -> task.getTaskName() != null && 
                    task.getTaskName().contains(taskName));
        }
        if (processName != null && !processName.isEmpty()) {
            stream = stream.filter(task -> task.getProcessDefinitionName() != null && 
                    task.getProcessDefinitionName().contains(processName));
        }
        
        List<TaskInstance> filtered = stream.collect(Collectors.toList());
        int total = filtered.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<TaskInstance> pagedData = fromIndex < total ? filtered.subList(fromIndex, toIndex) : new ArrayList<>();

        Page<TaskInstance> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(pagedData);
        
        return Result.success(page);
    }

    @GetMapping("/tasks/completed")
    @Operation(summary = "获取我的已完成任务")
    @RequiresPermissions("workflow:task:view")
    public Result<IPage<TaskInstance>> getMyCompletedTasks(
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @Parameter(description = "任务名称") @RequestParam(required = false) String taskName,
            @Parameter(description = "流程名称") @RequestParam(required = false) String processName) {
        if (!StpUtil.isLogin()) {
            return Result.fail("未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        List<TaskInstance> tasks = taskService.getCompletedTasks(userId);
        
        // 过滤条件
        Stream<TaskInstance> stream = tasks.stream();
        if (taskName != null && !taskName.isEmpty()) {
            stream = stream.filter(task -> task.getTaskName() != null && 
                    task.getTaskName().contains(taskName));
        }
        if (processName != null && !processName.isEmpty()) {
            stream = stream.filter(task -> task.getProcessDefinitionName() != null && 
                    task.getProcessDefinitionName().contains(processName));
        }
        
        List<TaskInstance> filtered = stream.collect(Collectors.toList());
        int total = filtered.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<TaskInstance> pagedData = fromIndex < total ? filtered.subList(fromIndex, toIndex) : new ArrayList<>();

        Page<TaskInstance> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(pagedData);
        
        return Result.success(page);
    }

    @PostMapping("/task/claim/{taskId}")
    @Operation(summary = "签收任务")
    @RequiresPermissions("workflow:task:claim")
    public Result<Boolean> claimTask(@Parameter(description = "任务ID") @PathVariable String taskId) {
        if (!StpUtil.isLogin()) {
            return Result.fail("未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        taskService.claimTask(taskId, userId);
        return Result.success(true);
    }

    @PostMapping("/task/complete/{taskId}")
    @Operation(summary = "完成任务")
    @RequiresPermissions("workflow:task:complete")
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
    @RequiresPermissions("workflow:task:view")
    public Result<List<TaskInstance>> getTasksByProcessInstance(@Parameter(description = "流程实例ID") @PathVariable String instanceId) {
        List<TaskInstance> tasks = processInstanceService.getTasksByProcessInstance(instanceId);
        return Result.success(tasks);
    }

    @GetMapping("/monitor/stats")
    @Operation(summary = "获取流程监控统计数据")
    @RequiresPermissions("workflow:monitor:view")
    public Result<Map<String, Object>> getMonitorStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProcesses", processDefinitionService.getProcessDefinitions().size());
        stats.put("runningInstances", processInstanceService.getRunningProcessInstances().size());
        stats.put("pendingTasks", taskService.getRunningTasksCount());
        stats.put("completedToday", 0);
        return Result.success(stats);
    }

    @GetMapping("/monitor/tasks")
    @Operation(summary = "获取全系统所有活跃任务")
    @RequiresPermissions("workflow:monitor:view")
    public Result<IPage<TaskInstance>> getAllActiveTasks(
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @Parameter(description = "任务名称") @RequestParam(required = false) String taskName,
            @Parameter(description = "流程名称") @RequestParam(required = false) String processName) {
        List<TaskInstance> tasks = taskService.getAllActiveTasks();
        
        // 过滤条件
        Stream<TaskInstance> stream = tasks.stream();
        if (taskName != null && !taskName.isEmpty()) {
            stream = stream.filter(task -> task.getTaskName() != null && 
                    task.getTaskName().contains(taskName));
        }
        if (processName != null && !processName.isEmpty()) {
            stream = stream.filter(task -> task.getProcessDefinitionName() != null && 
                    task.getProcessDefinitionName().contains(processName));
        }
        
        List<TaskInstance> filtered = stream.collect(Collectors.toList());
        int total = filtered.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<TaskInstance> pagedData = fromIndex < total ? filtered.subList(fromIndex, toIndex) : new ArrayList<>();

        Page<TaskInstance> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(pagedData);
        
        return Result.success(page);
    }

    @GetMapping("/monitor/charts")
    @Operation(summary = "获取流程监控图表数据")
    @RequiresPermissions("workflow:monitor:view")
    public Result<Map<String, Object>> getMonitorCharts() {
        Map<String, Object> data = new HashMap<>();
        List<ProcessDefinition> defs = processDefinitionService.getProcessDefinitions();
        List<Map<String, Object>> pieData = new ArrayList<>();
        
        for (ProcessDefinition def : defs) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", def.getProcessName());
            // 真实统计
            long count = ProcessEngines.getDefaultProcessEngine()
                    .getRuntimeService().createProcessInstanceQuery()
                    .processDefinitionKey(def.getProcessKey())
                    .count();
            item.put("value", count);
            pieData.add(item);
        }
        data.put("typeDistribution", pieData);
        return Result.success(data);
    }
}