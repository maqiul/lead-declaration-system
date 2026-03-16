package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 菜单实体类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_menu")
@Schema(description = "菜单对象", name = "菜单表")
public class Menu extends BaseEntity {

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "菜单名称")
    @TableField("menu_name")
    private String menuName;

    @Schema(description = "父菜单ID")
    @TableField("parent_id")
    private Long parentId;

    @Schema(description = "菜单类型 1-目录 2-菜单 3-按钮")
    @TableField("menu_type")
    private Integer menuType;

    @Schema(description = "路由地址")
    @TableField("path")
    private String path;

    @Schema(description = "组件路径")
    @TableField("component")
    private String component;

    @Schema(description = "权限标识")
    @TableField("permission")
    private String permission;

    @Schema(description = "图标")
    @TableField("icon")
    private String icon;

    @Schema(description = "排序")
    @TableField("sort")
    private Integer sort;

    @Schema(description = "状态 0:禁用 1:启用")
    @TableField("status")
    private Integer status;

    @Schema(description = "删除标志 0:未删除 1:已删除")
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @Schema(description = "子菜单列表")
    @TableField(exist = false)
    private List<Menu> children;
}