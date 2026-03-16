package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 组织实体类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_org")
@Schema(description = "组织对象", name = "组织表")
public class Organization extends BaseEntity {

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "组织名称")
    @TableField("org_name")
    private String orgName;

    @Schema(description = "组织编码")
    @TableField("org_code")
    private String orgCode;

    @Schema(description = "父组织ID")
    @TableField("parent_id")
    private Long parentId;

    @Schema(description = "组织层级")
    @TableField("level")
    private Integer level;

    @Schema(description = "子组织列表")
    @TableField(exist = false)
    private List<Organization> children;

    @Schema(description = "负责人")
    @TableField("leader")
    private String leader;

    @Schema(description = "联系电话")
    @TableField("phone")
    private String phone;

    @Schema(description = "邮箱")
    @TableField("email")
    private String email;

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
}