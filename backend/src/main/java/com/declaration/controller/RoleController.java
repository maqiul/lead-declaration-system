package com.declaration.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.PageParam;
import com.declaration.common.Result;
import com.declaration.entity.Role;
import com.declaration.service.RoleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 角色管理控制器
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
@Tag(name = "角色管理", description = "角色管理相关接口")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "获取角色列表")
    @RequiresPermissions("role:list")
    public Result<List<Role>> getRoleList() {
        List<Role> roles = roleService.list();
        return Result.success(roles);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询角色列表")
    @RequiresPermissions("role:list")
    public Result<IPage<Role>> getRolePage(
            @Valid PageParam pageParam,
            Role role) {
        IPage<Role> page = roleService.getRolePage(pageParam, role);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取角色详情")
    @RequiresPermissions("role:query")
    public Result<Role> getRole(@PathVariable Long id) {
        Role role = roleService.getById(id);
        if (role == null) {
            return Result.fail("角色不存在");
        }
        return Result.success(role);
    }

    @PostMapping
    @Operation(summary = "创建角色")
    @RequiresPermissions("role:add")
    public Result<Role> createRole(@Valid @RequestBody Role role) {
        boolean result = roleService.saveRole(role);
        if (result) {
            return Result.success("创建成功", role);
        } else {
            return Result.fail("创建失败");
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新角色")
    @RequiresPermissions("role:update")
    public Result<Role> updateRole(@PathVariable Long id, @Valid @RequestBody Role role) {
        role.setId(id);
        boolean result = roleService.updateRole(role);
        if (result) {
            return Result.success("更新成功", role);
        } else {
            return Result.fail("更新失败");
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    @RequiresPermissions("role:delete")
    public Result<Void> deleteRole(@PathVariable Long id) {
        // 检查是否为admin角色
        Role role = roleService.getById(id);
        if (role != null && "admin".equals(role.getRoleCode())) {
            return Result.fail("不能删除管理员角色");
        }
        
        boolean result = roleService.deleteRole(id);
        if (result) {
            return new Result<Void>().setCode(200).setMessage("删除成功");
        } else {
            return Result.fail("删除失败");
        }
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户角色")
    @RequiresPermissions("role:user")
    public Result<List<Role>> getUserRoles(
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId) {
        List<Role> roles = roleService.getUserRoles(userId);
        return Result.success(roles);
    }

    @PostMapping("/assign")
    @Operation(summary = "分配角色给用户")
    @RequiresPermissions("role:assign")
    public Result<Boolean> assignRoleToUser(@RequestBody RoleAssignDTO dto) {
        boolean result = roleService.assignRoleToUser(dto.getUserId(), dto.getRoleIds());
        return Result.success("分配成功", result);
    }

    @GetMapping("/menu/{roleId}")
    @Operation(summary = "获取角色菜单权限")
    @RequiresPermissions("role:menu")
    public Result<List<Long>> getRoleMenus(
            @Parameter(description = "角色ID", required = true) @PathVariable Long roleId) {
        List<Long> menuIds = roleService.getRoleMenuIds(roleId);
        return Result.success(menuIds);
    }

    @PostMapping("/menu")
    @Operation(summary = "分配菜单权限给角色")
    @RequiresPermissions("role:menu")
    public Result<Boolean> assignMenuToRole(@RequestBody RoleMenuDTO dto) {
        boolean result = roleService.assignMenuToRole(dto.getRoleId(), dto.getMenuIds());
        return Result.success("分配成功", result);
    }

    @PostMapping("/admin/all-permissions/{roleId}")
    @Operation(summary = "为管理员角色分配所有权限")
    @RequiresPermissions("role:menu")
    public Result<Boolean> assignAllPermissionsToAdmin(
            @Parameter(description = "角色ID", required = true) @PathVariable Long roleId) {
        try {
            boolean result = roleService.assignAllPermissionsToAdmin(roleId);
            return Result.success("权限分配成功", result);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 角色分配DTO
     */
    @Data
    public static class RoleAssignDTO {
        private Long userId;
        private List<Long> roleIds;
    }

    /**
     * 角色菜单分配DTO
     */
    @Data
    public static class RoleMenuDTO {
        private Long roleId;
        private List<Long> menuIds;
    }
}