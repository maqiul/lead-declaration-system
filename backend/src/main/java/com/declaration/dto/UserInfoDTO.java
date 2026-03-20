package com.declaration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户信息传输对象（包含角色和权限）
 *
 * @author Administrator
 * @since 2026-03-18
 */
@Data
@Schema(description = "用户信息DTO")
public class UserInfoDTO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "所属组织ID")
    private Long orgId;

    @Schema(description = "状态 0:禁用 1:启用")
    private Integer status;

    @Schema(description = "角色列表")
    private List<String> roles;

    @Schema(description = "权限列表")
    private List<String> permissions;
}