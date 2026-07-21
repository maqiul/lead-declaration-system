package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 资料豁免审批记录
 * 当资料提交时必填文件不全，用户选择强制提交后创建此记录。
 * 主流程 materialSubmit 任务阻塞，直到豁免通过后才 complete。
 */
@Data
@TableName("declaration_material_exemption")
public class DeclarationMaterialExemption {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 申报单ID */
    private Long formId;

    /** 提交阶段(materialSubmit等) */
    private String stage;

    /** 缺失的必填资料项JSON数组 [{code,name,invoiceMode}] */
    private String missingItems;

    /** 豁免类型: NORMAL-普通文件 INVOICE-发票类 MIXED-混合 */
    private String exemptionType;

    /** 状态: 0-待审核 1-已通过 2-已驳回 */
    private Integer status;

    /** 主流程被阻塞的Flowable任务ID */
    private String mainTaskId;

    /** 豁免流程实例ID */
    private String processInstanceId;

    /** 审核人ID */
    private Long auditBy;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 审核备注 */
    private String auditRemark;

    /** 创建人ID */
    private Long createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
