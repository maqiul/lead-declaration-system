package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 税务退费申请实体
 *
 * @author Administrator
 * @since 2026-03-17
 */
@Data
@TableName("tax_refund_application")
public class TaxRefundApplication {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 申请编号
     */
    private String applicationNo;

    /**
     * 关联申报单ID
     */
    private Long declarationFormId;

    /**
     * 发起人ID
     */
    private Long initiatorId;

    /**
     * 发起人姓名
     */
    private String initiatorName;

    /**
     * 所属组织ID
     */
    @TableField("org_id")
    private Long orgId;

    /**
     * 发起部门ID
     */
    private Long departmentId;

    /**
     * 发起部门名称
     */
    private String departmentName;

    /**
     * 申请类型
     */
    private String applicationType;

    /**
     * 申请金额
     */
    private BigDecimal amount;

    /**
     * 申请说明
     */
    private String description;

    /**
     * 当前状态：0-草稿 1-已提交 2-财务初审 4-退回补充 6-财务复审 7-已完成 8-已拒绝
     * 注：状态3(文件生成)、5(发票提交)属于旧流程，简化后已不再使用，但数据库中可能仍存在旧数据
     */
    private Integer status;

    /**
     * 财务初审意见
     */
    private String firstReviewOpinion;

    /**
     * 当前审批人ID
     */
    private Long currentApproverId;

    /**
     * 当前审批人姓名
     */
    private String currentApproverName;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 退回补充原因
     */
    private String returnReason;

    /**
     * 发票号码
     */
    private String invoiceNo;

    /**
     * 发票金额
     */
    private BigDecimal invoiceAmount;

    /**
     * 税率
     */
    private BigDecimal taxRate;

    /**
     * 预计退税金额 (数据库计算字段，只读)
     */
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private BigDecimal expectedRefundAmount;

    /**
     * 财务复审意见
     */
    private String finalReviewOpinion;

    /**
     * 生成的退税文件路径
     */
    private String filePath;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 更新人
     */
    private Long updateBy;

    /**
     * 删除标志 0-正常 1-删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}