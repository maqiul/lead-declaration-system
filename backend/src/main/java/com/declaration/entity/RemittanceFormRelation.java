package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 水单与申报单关联实体类(多对多关系)
 */
@Data
@TableName("remittance_form_relation")
public class RemittanceFormRelation {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 水单ID
     */
    private Long remittanceId;

    /**
     * 申报单ID
     */
    private Long formId;

    /**
     * 关联类型: 1-主关联 2-副关联
     */
    private Integer relationType;

    /**
     * 关联金额(水单可能分摊到多个申报单)
     */
    private java.math.BigDecimal relationAmount;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
}
