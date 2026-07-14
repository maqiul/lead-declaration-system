package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板-节点编排实体
 *
 * @author Administrator
 * @since 2026-06-25
 */
@Data
@TableName("flow_template_node")
public class FlowTemplateNode {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属模板ID */
    private Long templateId;

    /** 引用 flow_node.id */
    private Long nodeId;

    /** 1=启用 0=跳过 */
    private Integer enabled;

    /** 排序（越小越靠前） */
    private Integer sortOrder;

    /** 办理人表达式覆盖（为空则使用 flow_node 默认值） */
    private String assignee;

    /** 候选组覆盖（为空则使用 flow_node 默认值） */
    private String candidateGroups;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ---- 非数据库字段：关联的节点信息 ----

    /** 关联的节点定义（非数据库字段） */
    @TableField(exist = false)
    private FlowNode node;
}
