package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 开票文件20%拆分产品明细实体
 */
@Data
@TableName("invoice_split_item")
public class InvoiceSplitItem {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 申报单ID */
    private Long formId;

    /** HS编码 */
    private String hsCode;

    /** 产品名称 */
    private String productName;

    /** 规格型号 */
    private String spec;

    /** 数量 */
    private BigDecimal quantity;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 金额 */
    private BigDecimal amount;

    /** 排序 */
    private Integer sort;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer delFlag;
}
