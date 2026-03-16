package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 流程实例实体类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_process_instance")
@Schema(description = "流程实例对象", name = "流程实例表")
public class ProcessInstance extends BaseEntity {

    @Schema(description = "流程实例ID")
    private String instanceId;

    @Schema(description = "流程定义ID")
    private String definitionId;

    @Schema(description = "流程定义KEY")
    private String processKey;

    @Schema(description = "流程名称")
    private String processName;

    @Schema(description = "发起人ID")
    private Long starterId;

    @Schema(description = "发起人姓名")
    private String starterName;

    @Schema(description = "当前节点ID")
    private String currentActivityId;

    @Schema(description = "当前节点名称")
    private String currentActivityName;

    @Schema(description = "流程状态 0:运行中 1:已完成 2:已终止")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTimeDb;

    @Schema(description = "业务KEY")
    private String businessKey;

    @Schema(description = "启动时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "删除原因")
    private String deleteReason;
}