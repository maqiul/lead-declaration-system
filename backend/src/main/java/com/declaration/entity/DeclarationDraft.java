package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 申报单草稿实体类
 */
@Data
@TableName("declaration_draft")
public class DeclarationDraft {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 所属组织ID
     */
    private Long orgId;

    /**
     * 申报单号 (草稿占位)
     */
    private String formNo;

    /**
     * 发货人
     */
    private String shipperCompany;

    /**
     * 收货人
     */
    private String consigneeCompany;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 整个表单的JSON数据
     */
    private String formData;

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
