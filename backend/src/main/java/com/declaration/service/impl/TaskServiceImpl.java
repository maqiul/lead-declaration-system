package com.declaration.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.declaration.dao.TaskInstanceDao;
import com.declaration.entity.TaskInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements com.declaration.service.TaskService {

    private final org.flowable.engine.TaskService flowableTaskService;
    private final org.flowable.engine.RuntimeService runtimeService;
    private final org.flowable.engine.RepositoryService repositoryService;
    private final org.flowable.engine.HistoryService historyService;
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
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TaskInstance>()
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
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TaskInstance>()
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
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TaskInstance>()
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
        TaskInstance taskInstance = taskInstanceDao.selectById(1L); // 临时处理
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
        TaskInstance taskInstance = taskInstanceDao.selectById(1L); // 临时处理
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
        
        // 保存到本地数据库
        TaskInstance taskInstance = new TaskInstance();
        taskInstance.setTaskId(taskId);
        // taskInstance.setComment(message); // 字段不存在
        taskInstance.setCreateBy(userId);
        taskInstance.setCreateTime(LocalDateTime.now());
        // taskInstanceDao.insert(taskInstance); // 如果有评论表的话
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
        instance.setAssigneeId(task.getAssignee() != null ? Long.valueOf(task.getAssignee()) : null);
        instance.setCreateTime(task.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        instance.setPriority(task.getPriority());
        instance.setStatus(task.getAssignee() != null ? 0 : 1);
        
        // 获取流程定义名称
        org.flowable.engine.repository.ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(task.getProcessDefinitionId())
                .singleResult();
        if (definition != null) {
            instance.setProcessDefinitionName(definition.getName());
        }

        // 获取业务KEY
        org.flowable.engine.runtime.ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();
        if (pi != null) {
            instance.setBusinessKey(pi.getBusinessKey());
        } else {
            // 如果运行中查不到，查历史
            org.flowable.engine.history.HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            if (hpi != null) {
                instance.setBusinessKey(hpi.getBusinessKey());
            }
        }
        
        return instance;
    }
}