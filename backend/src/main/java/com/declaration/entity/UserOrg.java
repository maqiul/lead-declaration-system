package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户组织关联实体类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Data
@TableName("sys_user_org")
@Schema(description = "用户组织关联对象", name = "用户组织关联表")
public class UserOrg {

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "组织ID")
    @TableField("org_id")
    private Long orgId;
}