package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出款水单实体类（独立管理，支持与申报单多对多关联）
 */
@Data
@TableName("payment_remittance")
public class PaymentRemittance {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联申报单ID（保留兼容，新流程使用关联表） */
    private Long formId;

    /** 出款水单编号 */
    private String paymentNo;

    /** 收款人 */
    private String payeeName;

    /** 出款日期 */
    private LocalDate paymentDate;

    /** 出款金额 */
    private BigDecimal paymentAmount;

    /** 币种 */
    private String currency;

    /** 银行账户ID */
    private Long bankAccountId;

    /** 银行名称 */
    private String bankAccountName;

    /** 凭证照片URL */
    private String photoUrl;

    /** 备注 */
    private String remarks;

    /** 状态: 0-草稿 1-待审核 2-已审核 3-已驳回 */
    private Integer status;

    /** 审核备注 */
    private String auditRemark;

    /** 审核人ID */
    private Long auditBy;

    /** 审核人姓名 */
    private String auditByName;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** Flowable流程实例ID */
    private String processInstanceId;

    /** 提交时间 */
    private LocalDateTime submitTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /** 已关联金额合计（非数据库字段，查询时动态计算） */
    @TableField(exist = false)
    private BigDecimal totalRelatedAmount;
}
