package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色菜单关联实体类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Data
@TableName("sys_role_menu")
@Schema(description = "角色菜单关联对象", name = "角色菜单关联表")
public class RoleMenu {

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "角色ID")
    @TableField("role_id")
    private Long roleId;

    @Schema(description = "菜单ID")
    @TableField("menu_id")
    private Long menuId;
}