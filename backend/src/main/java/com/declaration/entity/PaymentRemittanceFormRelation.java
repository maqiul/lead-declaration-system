package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出款水单与申报单关联实体类（多对多关系）
 */
@Data
@TableName("payment_remittance_form_relation")
public class PaymentRemittanceFormRelation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 出款水单ID */
    private Long remittanceId;

    /** 申报单ID */
    private Long formId;

    /** 关联类型: 1-主关联 2-副关联 */
    private Integer relationType;

    /** 关联金额 */
    private BigDecimal relationAmount;

    /** 备注 */
    private String remarks;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
}
