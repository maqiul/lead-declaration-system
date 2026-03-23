package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 货币信息实体
 *
 * @author Administrator
 * @since 2026-03-23
 */
@Data
@TableName("currency_info")
public class CurrencyInfo {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 货币代码(ISO 4217)，如 USD, EUR, CNY
     */
    private String currencyCode;

    /**
     * 英文名称，如 US Dollar
     */
    private String currencyName;

    /**
     * 中文名称，如 美元
     */
    private String chineseName;

    /**
     * 中文单位，如 元、镑
     */
    private String unitCn;

    /**
     * 货币符号，如 $, €, ¥
     */
    private String symbol;

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
}
