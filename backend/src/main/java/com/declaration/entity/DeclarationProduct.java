package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 申报单产品明细实体类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Data
@TableName("declaration_product")
public class DeclarationProduct {

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
     * 产品名称
     */
    private String productName;

    /**
     * 产品中文名
     */
    private String productChineseName;

    /**
     * 产品英文名
     */
    private String productEnglishName;

    /**
     * HS编码
     */
    private String hsCode;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 单位（英文名称）
     */
    private String unit;

    /**
     * 单位代码（关联 measurement_units 表）
     */
    private String unitCode;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 金额是否锁定: 1-锁定(用户手动输入), 0-自动计算
     */
    private Integer amountLocked;

    /**
     * 毛重(KGS)
     */
    private BigDecimal grossWeight;

    /**
     * 净重(KGS)
     */
    private BigDecimal netWeight;

    /**
     * 箱数
     */
    private Integer cartons;

    /**
     * 体积(CBM)
     */
    private BigDecimal volume;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 产品图片ID
     */
    private Long imageId;

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
     * 申报要素值列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<DeclarationElementValue> elementValues;

    /**
     * 箱子产品关联列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<DeclarationCartonProduct> cartonProducts;
}