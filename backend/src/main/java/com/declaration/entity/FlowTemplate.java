package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程模板实体
 *
 * @author Administrator
 * @since 2026-06-25
 */
@Data
@TableName("flow_template")
public class FlowTemplate {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模板名称（如：标准流程、简化流程）
     */
    private String name;

    /**
     * 模板编码（唯一）
     */
    private String code;

    /**
     * 模板说明
     */
    private String description;

    /**
     * 流程类型: declaration/remittance/taxRefund
     */
    private String processType;

    /**
     * 申报类型: SELF-内部 EXTERNAL-外部
     */
    private String declarationType;

    /**
     * 是否默认模板 0-否 1-是
     */
    private Integer isDefault;

    /**
     * 状态 0-禁用 1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 删除标志 0-正常 1-删除
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 步骤列表（非数据库字段，关联查询填充，向下兼容）
     */
    @TableField(exist = false)
    private List<FlowTemplateStep> steps;

    /**
     * 模板-节点编排列表（非数据库字段，关联查询填充）
     */
    @TableField(exist = false)
    private List<FlowTemplateNode> templateNodes;
}
