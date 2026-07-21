package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程节点库实体（全局共享）
 *
 * @author Administrator
 * @since 2026-06-25
 */
@Data
@TableName("flow_node")
public class FlowNode {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** BPMN taskDefinitionKey（如 deptAudit, materialSubmit） */
    private String nodeKey;

    /** 节点中文名 */
    private String nodeName;

    /** 节点类型: userTask / serviceTask */
    private String nodeType;

    /** 办理人表达式（如 ${starterId}） */
    private String assignee;

    /** 候选组（如 MATERIAL_AUDITOR），多个逗号分隔 */
    private String candidateGroups;

    /** 到达此节点时 declaration_form.status 值 */
    private Integer targetStatus;

    /** 对应前端表单区块: basic/material/supplement/invoiceAmount/invoice */
    private String formSection;

    /** 是否系统内置节点 0-否 1-是（不可删除） */
    private Integer isSystem;

    /** 节点说明 */
    private String description;

    /** 所属流程类型: declaration/remittance/taxRefund */
    private String processType;

    /** serviceTask 的委托表达式（如 ${declarationServiceTask}） */
    private String delegateExpression;

    /** 驳回时是否直接结束流程 0-否(回退) 1-是(结束) */
    private Integer rejectToEnd;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
