package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 水单信息实体类(独立流程,支持多对多关联申报单)
 */
@Data
@TableName("declaration_remittance")
public class DeclarationRemittance {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 申报单ID(保留兼容旧数据,新流程使用关联表)
     */
    private Long formId;

    /**
     * 水单编号(独立编号,不再依赖申报单)
     */
    private String remittanceNo;

    /**
     * 收汇名称
     */
    private String remittanceName;

    /**
     * 收汇日期
     */
    private LocalDate remittanceDate;

    /**
     * 收汇金额(原币)
     */
    private BigDecimal remittanceAmount;

    /**
     * 水单类型(已废弃,保留仅用于数据兼容): 1-定金 2-尾款
     * @deprecated 不再有定金/尾款分类,所有水单统一管理
     */
    @Deprecated
    private Integer remittanceType;

    /**
     * 币种
     */
    private String currency;

    /**
     * 审核时填写的汇率（如 7.2 表示 1 USD = 7.2 CNY）
     */
    private BigDecimal taxRate;

    /**
     * 审核时选择的银行ID
     */
    private Long bankAccountId;

    /**
     * 审核时选择的银行名称
     */
    private String bankAccountName;

    /**
     * 银行手续费率(%)
     */
    private BigDecimal bankFeeRate;

    /**
     * 手续费(原币)
     */
    private BigDecimal bankFee;

    /**
     * 入账金额(原币)
     */
    private BigDecimal creditedAmount;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 水单照片 URL
     */
    private String photoUrl;

    /**
     * 状态: 0-草稿 1-待审核 2-已审核 3-已驳回
     */
    private Integer status;

    /**
     * 审核备注
     */
    private String auditRemark;

    /**
     * 审核人ID
     */
    private Long auditBy;

    /**
     * 审核人姓名
     */
    private String auditByName;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;

    /**
     * Flowable流程实例ID
     */
    private String processInstanceId;

    /**
     * 提交时间
     */
    private LocalDateTime submitTime;

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
     * 已关联金额合计（非数据库字段，查询时动态计算）
     */
    @TableField(exist = false)
    private BigDecimal totalRelatedAmount;
}
