package com.declaration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.PageParam;
import com.declaration.common.Result;
import com.declaration.entity.Menu;
import com.declaration.entity.User;
import com.declaration.service.PermissionService;
import com.declaration.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
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
    public Result<User> getCurrentUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        return Result.success(user);
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
        return Result.success(user);
    }

    @PostMapping
    @RequiresPermissions("user:add")
    public Result<User> createUser(@Valid @RequestBody User user) {
        boolean result = userService.save(user);
        if (result) {
            return Result.success("创建成功", user);
        } else {
            return Result.fail("创建失败");
        }
    }

    @PutMapping("/{id}")
    @RequiresPermissions("user:update")
    public Result<User> updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
        user.setId(id);
        boolean result = userService.updateById(user);
        if (result) {
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

    @GetMapping("/permissions")
    public Result<Set<String>> getUserPermissions() {
        Long userId = StpUtil.getLoginIdAsLong();
        Set<String> permissions = permissionService.getUserPermissions(userId);
        return Result.success(permissions);
    }
}