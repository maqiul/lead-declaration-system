package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 主体配置实体
 *
 * @author Administrator
 * @since 2026-04-28
 */
@Data
@TableName("entity_config")
public class EntityConfig {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 公司英文名（如 NINGBO ZIYI TECHNOLOGY CO.,LTD）
     */
    private String entityName;

    /**
     * 英文地址
     */
    private String entityAddress;

    /**
     * 公司中文名
     */
    private String entityNameCn;

    /**
     * 中文地址
     */
    private String entityAddressCn;

    /**
     * 发票模板文件名（空=用系统默认）
     */
    private String invoiceTemplate;

    /**
     * 装箱单模板文件名
     */
    private String packingListTemplate;

    /**
     * 海关附件模板文件名
     */
    private String fullDocumentsTemplate;

    /**
     * 提货单模板文件名
     */
    private String pickupListTemplate;

    /**
     * 水单模板文件名
     */
    private String remittanceTemplate;

    /**
     * 是否默认主体 0-否 1-是
     */
    private Integer isDefault;

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
