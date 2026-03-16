package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 箱子产品关联实体类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Data
@TableName("declaration_carton_product")
public class DeclarationCartonProduct {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 箱子ID
     */
    private Long cartonId;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 该箱中产品数量
     */
    private Integer quantity;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}