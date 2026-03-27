package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 城市信息实体类
 */
@Data
@TableName("city_info")
public class CityInfo {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 城市中文名
     */
    private String cityName;

    /**
     * 城市英文名
     */
    private String cityEnglishName;

    /**
     * 省份中文名
     */
    private String provinceName;

    /**
     * 省份英文名
     */
    private String provinceEnglishName;

    /**
     * 国家中文名
     */
    private String countryName;

    /**
     * 国家英文名
     */
    private String countryEnglishName;

    /**
     * 城市编码
     */
    private String cityCode;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态（1-启用，0-禁用）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}