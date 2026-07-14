package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程模板步骤配置实体
 *
 * @author Administrator
 * @since 2026-06-25
 */
@Data
@TableName("flow_template_step")
public class FlowTemplateStep {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属模板ID
     */
    private Long templateId;

    /**
     * BPMN任务定义Key（如 deptAudit, materialSubmit）
     */
    private String stepKey;

    /**
     * 步骤中文名
     */
    private String stepName;

    /**
     * 1=启用 0=跳过
     */
    private Integer enabled;

    /**
     * 进入该步骤时 declaration_form.status 应设为的值
     */
    private Integer targetStatus;

    /**
     * 对应前端表单区块组件标识（如 basic, material, supplement, invoiceAmount, invoice）
     */
    private String formSection;

    /**
     * 排序（越小越靠前）
     */
    private Integer sortOrder;

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
}
