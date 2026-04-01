package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 计量单位配置实体
 *
 * @author Administrator
 * @since 2026-03-14
 */
@Data
@TableName("measurement_units")
public class MeasurementUnit {

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 单位代码
     */
    private String unitCode;

    /**
     * 单位名称
     */
    private String unitName;

    /**
     * 英文单位名称
     */
    private String unitNameEn;

    /**
     * 英文单位单数名称
     */
    private String unitNameEnSingular;

    /**
     * 单位类型
     */
    private String unitType;

    /**
     * 单位描述
     */
    private String description;

    /**
     * 状态 0-禁用 1-启用
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sort;

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