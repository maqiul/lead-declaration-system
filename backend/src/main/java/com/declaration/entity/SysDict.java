package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统字典类型实体
 *
 * @author Administrator
 * @since 2026-07-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_dict")
@Schema(description = "系统字典类型", name = "字典类型表")
public class SysDict extends BaseEntity {

    @Schema(description = "字典编码（唯一）")
    @TableField("dict_code")
    private String dictCode;

    @Schema(description = "字典名称")
    @TableField("dict_name")
    private String dictName;

    @Schema(description = "状态 0-禁用 1-启用")
    @TableField("status")
    private Integer status;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    @Schema(description = "删除标志 0-正常 1-删除")
    @TableLogic
    @TableField("del_flag")
    private Integer delFlag;
}
