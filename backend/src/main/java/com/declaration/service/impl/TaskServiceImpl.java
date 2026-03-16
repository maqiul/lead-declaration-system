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
        TaskInstance taskInstance = taskInstanceDao.selectById(1L); // 临时处理
        if (taskInstance != null) {
            taskInstance.setAssigneeId(userId);
            taskInstance.setClaimTime(LocalDateTime.now());
            taskInstance.setStatus(0); // 已签收
            taskInstanceDao.updateById(taskInstance);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unclaimTask(String taskId) {
        flowableTaskService.unclaim(taskId);
        
        // 更新本地任务状态
        TaskInstance taskInstance = taskInstanceDao.selectById(1L); // 临时处理
        if (taskInstance != null) {
            taskInstance.setAssigneeId(null);
            taskInstance.setClaimTime(null);
            taskInstance.setStatus(1); // 待签收
            taskInstanceDao.updateById(taskInstance);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(String taskId, Map<String, Object> variables) {
        flowableTaskService.complete(taskId, variables);
        
        // 更新本地任务状态
        TaskInstance taskInstance = taskInstanceDao.selectById(1L); // 临时处理
        if (taskInstance != null) {
            taskInstance.setStatus(2); // 已完成
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
        // instance.setDescription(task.getDescription()); // 字段不存在
        instance.setInstanceId(task.getProcessInstanceId());
        instance.setDefinitionId(task.getProcessDefinitionId());
        instance.setAssigneeId(task.getAssignee() != null ? Long.valueOf(task.getAssignee()) : null);
        instance.setCreateTime(task.getCreateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        // instance.setClaimTime(task.getClaimTime()); // 字段不存在
        // instance.setDueDate(task.getDueDate()); // 字段不存在
        instance.setPriority(task.getPriority());
        instance.setStatus(task.getAssignee() != null ? 0 : 1); // 已分配/待签收
        return instance;
    }
}