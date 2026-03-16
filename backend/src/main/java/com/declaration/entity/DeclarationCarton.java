package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 申报单箱子信息实体类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Data
@TableName("declaration_carton")
public class DeclarationCarton {

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
     * 箱号
     */
    private String cartonNo;

    /**
     * 箱子数量
     */
    private Integer quantity;

    /**
     * 总体积(CBM)
     */
    private BigDecimal volume;

    /**
     * 排序
     */
    private Integer sortOrder;

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