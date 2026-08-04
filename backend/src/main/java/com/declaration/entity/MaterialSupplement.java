package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资料补交记录（独立 Flowable 流程，不阻塞申报主流程）
 * 申报人发起（草稿）→ 上传补交资料 → 提交补交审核 → 审核人审核 → 通过则增量资料转正，驳回则清除增量
 * 流程模板未配置/启动失败时回退为纯状态机模式（processInstanceId 为空）
 */
@Data
@TableName("declaration_material_supplement")
public class MaterialSupplement implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 申报单ID */
    private Long formId;

    /** 补交原因 */
    private String reason;

    /** 状态 -1-草稿（发起后未提交审核，审核人不可见） 0-补交中（已提交待审核） 1-通过 2-驳回 */
    private Integer status;

    /** Flowable流程实例ID（为空=纯状态机模式） */
    private String processInstanceId;

    /** 发起人ID */
    private Long initiatorId;

    /** 审核人ID */
    private Long auditorId;

    /** 审核备注 */
    private String auditRemark;

    /** 发起时间 */
    private LocalDateTime createTime;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 发起人显示名（非持久化） */
    @TableField(exist = false)
    private String initiatorName;

    /** 审核人显示名（非持久化） */
    @TableField(exist = false)
    private String auditorName;

    /** 申报单号（非持久化） */
    @TableField(exist = false)
    private String formNo;

    /** 本次补交的文件快照（非持久化，历史查询时回填） */
    @TableField(exist = false)
    private java.util.List<MaterialSupplementFile> files;
}
