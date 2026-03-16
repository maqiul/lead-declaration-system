package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HS商品类型配置实体
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Data
@TableName("product_type_config")
public class ProductTypeConfig {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * HS编码
     */
    private String hsCode;

    /**
     * 英文名称
     */
    private String englishName;

    /**
     * 中文名称
     */
    private String chineseName;

    /**
     * 申报要素配置JSON
     */
    private String elementsConfig;

    /**
     * 计量单位类型
     */
    private String unitType;

    /**
     * 计量单位代码
     */
    private String unitCode;

    /**
     * 计量单位名称
     */
    private String unitName;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态 0-禁用 1-启用
     */
    private Integer status;

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
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 删除标志 0-正常 1-删除
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 申报要素对象列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<DeclarationElement> elements;
}
