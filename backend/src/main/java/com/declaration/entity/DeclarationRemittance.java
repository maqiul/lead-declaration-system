package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 水单信息实体类
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
     * 申报单ID
     */
    private Long formId;

    /**
     * 水单类型: 1-定金, 2-尾款
     */
    private Integer remittanceType;

    /**
     * 收汇名称
     */
    private String remittanceName;

    /**
     * 收汇日期
     */
    private LocalDate remittanceDate;

    /**
     * 收汇金额($)
     */
    private BigDecimal remittanceAmount;

    /**
     * 当日汇率
     */
    private BigDecimal exchangeRate;

    /**
     * 手续费($)
     */
    private BigDecimal bankFee;

    /**
     * 入账金额($)
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
     * 状态: 0-待审核 1-已审核 2-已驳回
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
     * 审核时间
     */
    private LocalDateTime auditTime;

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
