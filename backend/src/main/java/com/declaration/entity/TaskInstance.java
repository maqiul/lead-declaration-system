package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 任务实例实体类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_task_instance")
@Schema(description = "任务实例对象", name = "任务实例表")
public class TaskInstance extends BaseEntity {

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "流程实例ID")
    private String instanceId;

    @Schema(description = "流程定义ID")
    private String definitionId;

    @Schema(description = "流程定义名称")
    private String processDefinitionName;

    @Schema(description = "业务KEY (申报单ID)")
    private String businessKey;

    @Schema(description = "发起人名称")
    private String starterName;

    @Schema(description = "节点ID")
    private String activityId;

    @Schema(description = "节点名称")
    private String activityName;

    @Schema(description = "办理人ID")
    private Long assigneeId;

    @Schema(description = "办理人姓名")
    private String assigneeName;

    @Schema(description = "候选人IDs")
    private String candidateIds;

    @Schema(description = "候选组IDs")
    private String candidateGroupIds;

    @Schema(description = "任务状态 0:待办 1:已办 2:已撤回 3:已终止")
    private Integer status;

    @Schema(description = "签收时间")
    private LocalDateTime claimTime;

    @Schema(description = "完成时间")
    private LocalDateTime endTime;

    @Schema(description = "到期时间")
    private LocalDateTime dueTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "任务描述")
    private String description;

    @Schema(description = "执行ID")
    private String executionId;

    @Schema(description = "任务定义KEY")
    private String taskDefinitionKey;

    @Schema(description = "候选人")
    private String candidateUsers;

    @Schema(description = "候选组")
    private String candidateGroups;

    @Schema(description = "到期时间")
    private LocalDateTime dueDate;

    @Schema(description = "优先级")
    private int priority;
}