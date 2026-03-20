package com.declaration.dto;

import com.declaration.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 包含角色信息的用户DTO
 *
 * @author Administrator
 * @since 2026-03-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "包含角色信息的用户DTO")
public class UserWithRolesDTO extends User {
    
    @Schema(description = "角色ID列表")
    private List<Long> roleIds;
}