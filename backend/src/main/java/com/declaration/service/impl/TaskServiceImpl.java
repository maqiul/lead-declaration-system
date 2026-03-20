package com.declaration.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.declaration.dao.TaskInstanceDao;
import com.declaration.entity.TaskInstance;
import com.declaration.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务服务实现类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final org.flowable.engine.TaskService flowableTaskService;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final HistoryService historyService;
    private final TaskInstanceDao taskInstanceDao;

    @Override
    public List<TaskInstance> getAssignedTasks(Long assigneeId) {
        TaskQuery query = flowableTaskService.createTaskQuery();
        List<Task> tasks = query.taskAssignee(assigneeId.toString()).list();
        
        return tasks.stream()
                .map(this::convertTaskInstance)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskInstance> getCandidateTasks(Long userId, List<String> groupIds) {
        TaskQuery query = flowableTaskService.createTaskQuery();
        
        // 查询用户组任务
        if (groupIds != null && !groupIds.isEmpty()) {
            query.taskCandidateGroupIn(groupIds);
        }
        
        List<Task> tasks = query.list();
        
        return tasks.stream()
                .map(this::convertTaskInstance)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimTask(String taskId, Long userId) {
        flowableTaskService.claim(taskId, userId.toString());
        
        // 更新本地任务状态
        TaskInstance taskInstance = taskInstanceDao.selectOne(
            new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getTaskId, taskId)
        );
        if (taskInstance != null) {
            taskInstance.setAssigneeId(userId);
            taskInstance.setClaimTime(LocalDateTime.now());
            taskInstance.setUpdateTime(LocalDateTime.now());
            taskInstanceDao.updateById(taskInstance);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unclaimTask(String taskId) {
        flowableTaskService.unclaim(taskId);
        
        // 更新本地任务状态
        TaskInstance taskInstance = taskInstanceDao.selectOne(
            new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getTaskId, taskId)
        );
        if (taskInstance != null) {
            taskInstance.setAssigneeId(null);
            taskInstance.setClaimTime(null);
            taskInstance.setUpdateTime(LocalDateTime.now());
            taskInstanceDao.updateById(taskInstance);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(String taskId, Map<String, Object> variables) {
        flowableTaskService.complete(taskId, variables);
        
        // 更新本地任务状态
        TaskInstance taskInstance = taskInstanceDao.selectOne(
            new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getTaskId, taskId)
        );
        if (taskInstance != null) {
            taskInstance.setStatus(1); // 已办/已完成
            taskInstance.setEndTime(LocalDateTime.now());
            taskInstance.setUpdateTime(LocalDateTime.now());
            taskInstanceDao.updateById(taskInstance);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectTask(String taskId, String targetActivityId, String reason) {
        // Flowable驳回实现
        Map<String, Object> variables = new HashMap<>();
        variables.put("rejected", true);
        variables.put("rejectReason", reason);
        variables.put("targetActivityId", targetActivityId);
        
        flowableTaskService.complete(taskId, variables);
        
        // 记录驳回日志
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            addTaskComment(taskId, userId, "驳回任务: " + reason);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferTask(String taskId, Long assigneeId) {
        flowableTaskService.setAssignee(taskId, assigneeId.toString());
        
        // 更新本地任务状态
        TaskInstance taskInstance = taskInstanceDao.selectOne(
            new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getTaskId, taskId)
        );
        if (taskInstance != null) {
            taskInstance.setAssigneeId(assigneeId);
            taskInstance.setUpdateTime(LocalDateTime.now());
            taskInstanceDao.updateById(taskInstance);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegateTask(String taskId, Long assigneeId) {
        flowableTaskService.delegateTask(taskId, assigneeId.toString());
        
        // 更新本地任务状态
        TaskInstance taskInstance = taskInstanceDao.selectOne(
            new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getTaskId, taskId)
        );
        if (taskInstance != null) {
            taskInstance.setAssigneeId(assigneeId);
            taskInstance.setUpdateTime(LocalDateTime.now());
            taskInstanceDao.updateById(taskInstance);
        }
    }

    @Override
    public TaskInstance getTaskById(String taskId) {
        Task task = flowableTaskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return null;
        }
        return convertTaskInstance(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTaskComment(String taskId, Long userId, String message) {
        flowableTaskService.addComment(taskId, null, message);
    }

    /**
     * Flowable任务转换为本地任务实例
     */
    private TaskInstance convertTaskInstance(Task task) {
        TaskInstance instance = new TaskInstance();
        instance.setTaskId(task.getId());
        instance.setTaskName(task.getName());
        instance.setInstanceId(task.getProcessInstanceId());
        instance.setDefinitionId(task.getProcessDefinitionId());
        instance.setActivityId(task.getTaskDefinitionKey());
        if (task.getAssignee() != null) {
            try {
                Long assigneeId = Long.valueOf(task.getAssignee());
                instance.setAssigneeId(assigneeId);
                instance.setAssigneeName("用户" + assigneeId);
            } catch (Exception e) {}
        }
        instance.setCreateTime(task.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        instance.setPriority(task.getPriority());
        instance.setStatus(task.getAssignee() != null ? 0 : 1);
        
        // 获取流程定义名称
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(task.getProcessDefinitionId())
                .singleResult();
        if (definition != null) {
            instance.setProcessDefinitionName(definition.getName());
        }

        // 获取业务KEY
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();
        if (pi != null) {
            instance.setBusinessKey(pi.getBusinessKey());
        } else {
            // 如果运行中查不到，查历史
            HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            if (hpi != null) {
                instance.setBusinessKey(hpi.getBusinessKey());
            }
        }
        
        return instance;
    }

    @Override
    public List<TaskInstance> getAllActiveTasks() {
        List<Task> tasks = flowableTaskService.createTaskQuery()
                .active()
                .orderByTaskCreateTime().desc()
                .list();
        
        return tasks.stream()
                .map(this::convertTaskInstance)
                .collect(Collectors.toList());
    }

    @Override
    public long getRunningTasksCount() {
        return flowableTaskService.createTaskQuery()
                .active()
                .count();
    }

    @Override
    public List<TaskInstance> getCompletedTasks(Long userId) {
        // 使用 Flowable HistoryService 查询用户已完成的任务
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(userId.toString())
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .list();
        
        return historicTasks.stream()
                .map(this::convertHistoricTaskInstance)
                .collect(Collectors.toList());
    }

    /**
     * 将历史任务转换为本地任务实例
     */
    private TaskInstance convertHistoricTaskInstance(HistoricTaskInstance task) {
        TaskInstance instance = new TaskInstance();
        instance.setTaskId(task.getId());
        instance.setTaskName(task.getName());
        instance.setInstanceId(task.getProcessInstanceId());
        instance.setDefinitionId(task.getProcessDefinitionId());
        instance.setActivityId(task.getTaskDefinitionKey());
        if (task.getAssignee() != null) {
            try {
                Long assigneeId = Long.valueOf(task.getAssignee());
                instance.setAssigneeId(assigneeId);
                instance.setAssigneeName("用户" + assigneeId);
            } catch (Exception e) {}
        }
        if (task.getCreateTime() != null) {
            instance.setCreateTime(task.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        if (task.getEndTime() != null) {
            instance.setEndTime(task.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        if (task.getClaimTime() != null) {
            instance.setClaimTime(task.getClaimTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        instance.setPriority(task.getPriority());
        instance.setStatus(1); // 已完成
        
        // 获取流程定义名称
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(task.getProcessDefinitionId())
                .singleResult();
        if (definition != null) {
            instance.setProcessDefinitionName(definition.getName());
        }

        // 获取业务KEY
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();
        if (hpi != null) {
            instance.setBusinessKey(hpi.getBusinessKey());
            // 获取发起人名称
            if (hpi.getStartUserId() != null) {
                instance.setStarterName("用户" + hpi.getStartUserId());
            }
        }
        
        return instance;
    }
}