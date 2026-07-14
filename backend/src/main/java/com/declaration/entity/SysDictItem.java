package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统字典项实体
 *
 * @author Administrator
 * @since 2026-07-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_dict_item")
@Schema(description = "系统字典项", name = "字典项表")
public class SysDictItem extends BaseEntity {

    @Schema(description = "所属字典编码")
    @TableField("dict_code")
    private String dictCode;

    @Schema(description = "字典项值")
    @TableField("item_value")
    private String itemValue;

    @Schema(description = "字典项显示文本")
    @TableField("item_label")
    private String itemLabel;

    @Schema(description = "标签颜色")
    @TableField("item_color")
    private String itemColor;

    @Schema(description = "排序")
    @TableField("sort_order")
    private Integer sortOrder;

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
