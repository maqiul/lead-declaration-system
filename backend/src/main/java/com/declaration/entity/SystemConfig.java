package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置实体类
 *
 * @author Administrator
 * @since 2026-03-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_config")
@Schema(description = "系统配置对象", name = "系统配置表")
public class SystemConfig extends BaseEntity {

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "配置键")
    @TableField("config_key")
    private String configKey;

    @Schema(description = "配置名称")
    @TableField("config_name")
    private String configName;

    @Schema(description = "配置值")
    @TableField("config_value")
    private String configValue;

    @Schema(description = "配置输入类型 1-文本框 2-下拉框 3-开关 4-数字输入框")
    @TableField("input_type")
    private Integer inputType;

    @Schema(description = "下拉框选项JSON(当input_type=2时使用)")
    @TableField("select_options")
    private String selectOptions;

    @Schema(description = "是否系统内置参数 0-否 1-是")
    @TableField("is_system_param")
    private Integer isSystemParam;

    @Schema(description = "配置类型 1-系统配置 2-UI配置 3-业务配置")
    @TableField("config_type")
    private Integer configType;

    @Schema(description = "配置分组")
    @TableField("config_group")
    private String configGroup;

    @Schema(description = "备注说明")
    @TableField("remark")
    private String remark;

    @Schema(description = "状态 0-禁用 1-启用")
    @TableField("status")
    private Integer status;

    @Schema(description = "排序")
    @TableField("sort")
    private Integer sort;

    @Schema(description = "删除标志 0-正常 1-删除")
    @TableLogic
    @TableField("del_flag")
    private Integer delFlag;
}