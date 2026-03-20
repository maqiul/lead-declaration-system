package com.declaration.controller;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.PageParam;
import com.declaration.common.Result;
import com.declaration.entity.Menu;
import com.declaration.dto.UserInfoDTO;
import com.declaration.dto.UserWithRolesDTO;
import com.declaration.entity.User;
import com.declaration.service.PermissionService;
import com.declaration.service.RoleService;
import com.declaration.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户控制器
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PermissionService permissionService;
    private final RoleService roleService;

    @PostMapping("/login")
    public Result<String> login(@RequestBody User user) {
        try {
            String token = userService.login(user.getUsername(), user.getPassword());
            return Result.success("登录成功", token);
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody User user) {
        try {
            boolean result = userService.register(user);
            if (result) {
                return Result.success("注册成功", user);
            } else {
                return Result.fail("注册失败");
            }
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return new Result<Void>().setCode(200).setMessage("退出成功");
    }

    @GetMapping("/info")
    public Result<UserInfoDTO> getCurrentUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        
        // 构造包含角色和权限的用户信息
        UserInfoDTO userInfo = new UserInfoDTO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setPhone(user.getPhone());
        userInfo.setEmail(user.getEmail());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setOrgId(user.getOrgId());
        userInfo.setStatus(user.getStatus());
        
        // 获取用户角色和权限
        userInfo.setRoles(new ArrayList<>(permissionService.getUserRoles(userId)));
        userInfo.setPermissions(new ArrayList<>(permissionService.getUserPermissions(userId)));
        
        return Result.success(userInfo);
    }

    @GetMapping("/page")
    @RequiresPermissions("user:list")
    public Result<IPage<User>> getUserPage(
            @Valid PageParam pageParam,
            User user) {
        IPage<User> page = userService.getUserPage(pageParam, user);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @RequiresPermissions("user:query")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        // 获取用户角色ID列表并填充
        List<Long> roleIds = permissionService.getUserRoleIds(id);
        user.setRoleIds(roleIds);
        return Result.success(user);
    }

    @PostMapping
    @RequiresPermissions("user:add")
    public Result<User> createUser(@Valid @RequestBody UserWithRolesDTO userDto) {
        // 转换为User实体
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setNickname(userDto.getNickname());
        user.setPhone(userDto.getPhone());
        user.setEmail(userDto.getEmail());
        user.setOrgId(userDto.getOrgId());
        user.setStatus(userDto.getStatus());
        
        // 如果密码为空，设置默认密码
        if (userDto.getPassword() == null || userDto.getPassword().trim().isEmpty()) {
            String defaultPassword = user.getUsername() + "@123";
            user.setPassword(SaSecureUtil.md5(defaultPassword));
        } else {
            // 如果提供了密码，则进行加密
            user.setPassword(SaSecureUtil.md5(userDto.getPassword()));
        }
        
        boolean result = userService.save(user);
        if (result) {
            // 如果用户有角色信息，分配角色
            if (userDto.getRoleIds() != null && !userDto.getRoleIds().isEmpty()) {
                roleService.assignRoleToUser(user.getId(), userDto.getRoleIds());
            }
            return Result.success("创建成功", user);
        } else {
            return Result.fail("创建失败");
        }
    }

    @PutMapping("/{id}")
    @RequiresPermissions("user:update")
    public Result<User> updateUser(@PathVariable Long id, @Valid @RequestBody UserWithRolesDTO userDto) {
        // 更新用户基本信息
        User user = new User();
        user.setId(id);
        user.setUsername(userDto.getUsername());
        user.setNickname(userDto.getNickname());
        user.setPhone(userDto.getPhone());
        user.setEmail(userDto.getEmail());
        user.setOrgId(userDto.getOrgId());
        user.setStatus(userDto.getStatus());
        
        boolean result = userService.updateById(user);
        if (result) {
            // 如果用户有角色信息，更新角色分配
            if (userDto.getRoleIds() != null) {
                roleService.assignRoleToUser(id, userDto.getRoleIds());
            }
            return Result.success("更新成功", user);
        } else {
            return Result.fail("更新失败");
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("user:delete")
    public Result<Void> deleteUser(@PathVariable Long id) {
        // 检查是否为admin用户
        User user = userService.getById(id);
        if (user != null && "admin".equals(user.getUsername())) {
            return Result.fail("不能删除管理员用户");
        }
        
        boolean result = userService.removeById(id);
        if (result) {
            return new Result<Void>().setCode(200).setMessage("删除成功");
        } else {
            return Result.fail("删除失败");
        }
    }

    @GetMapping("/routes")
    public Result<List<Menu>> getUserRoutes() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<Menu> routes = permissionService.getUserMenuTree(userId);
        return Result.success(routes);
    }

    @PutMapping("/{id}/password")
    @RequiresPermissions("user:resetPwd")
    public Result<Void> resetUserPassword(@PathVariable Long id) {
        // 获取用户信息
        User user = userService.getById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        
        // 生成默认密码：用户名@123
        String defaultPassword = user.getUsername() + "@123";
        String encryptedPassword = SaSecureUtil.md5(defaultPassword);
        
        // 更新用户密码
        User userToUpdate = new User();
        userToUpdate.setId(id);
        userToUpdate.setPassword(encryptedPassword);
        
        boolean result = userService.updateById(userToUpdate);
        if (result) {
            return Result.success("密码已重置为默认密码: " + defaultPassword, null);
        } else {
            return Result.fail("密码重置失败");
        }
    }
    
    @GetMapping("/permissions")
    public Result<Set<String>> getUserPermissions() {
        Long userId = StpUtil.getLoginIdAsLong();
        Set<String> permissions = permissionService.getUserPermissions(userId);
        return Result.success(permissions);
    }
}