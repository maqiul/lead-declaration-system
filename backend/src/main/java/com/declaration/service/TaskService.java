package com.declaration.service;

import com.declaration.entity.TaskInstance;

import java.util.List;
import java.util.Map;

/**
 * 任务服务接口
 *
 * @author Administrator
 * @since 2026-03-13
 */
public interface TaskService {

    /**
     * 获取用户的待办任务
     *
     * @param assigneeId 办理人ID
     * @return 待办任务列表
     */
    List<TaskInstance> getAssignedTasks(Long assigneeId);

    /**
     * 获取候选任务
     *
     * @param userId 用户ID
     * @param groupIds 用户组IDs
     * @return 候选任务列表
     */
    List<TaskInstance> getCandidateTasks(Long userId, List<String> groupIds);

    /**
     * 签收任务
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     */
    void claimTask(String taskId, Long userId);

    /**
     * 取消签收任务
     *
     * @param taskId 任务ID
     */
    void unclaimTask(String taskId);

    /**
     * 完成任务
     *
     * @param taskId 任务ID
     * @param variables 任务变量
     */
    void completeTask(String taskId, Map<String, Object> variables);

    /**
     * 驳回任务
     *
     * @param taskId 任务ID
     * @param targetActivityId 目标节点ID
     * @param reason 驳回原因
     */
    void rejectTask(String taskId, String targetActivityId, String reason);

    /**
     * 转办任务
     *
     * @param taskId 任务ID
     * @param assigneeId 新办理人ID
     */
    void transferTask(String taskId, Long assigneeId);

    /**
     * 委派任务
     *
     * @param taskId 任务ID
     * @param assigneeId 受托人ID
     */
    void delegateTask(String taskId, Long assigneeId);

    /**
     * 根据任务ID获取任务详情
     *
     * @param taskId 任务ID
     * @return 任务实例
     */
    TaskInstance getTaskById(String taskId);

    /**
     * 添加任务评论
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param message 评论内容
     */
    void addTaskComment(String taskId, Long userId, String message);
}