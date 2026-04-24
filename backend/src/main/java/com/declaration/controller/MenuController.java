package com.declaration.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.PageParam;
import com.declaration.common.Result;
import com.declaration.entity.Menu;
import com.declaration.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 菜单管理控制器
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
@Tag(name = "菜单管理")
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    @Operation(summary = "获取菜单列表")
    @RequiresPermissions("menu:view")
    public Result<List<Menu>> getMenuList() {
        List<Menu> menus = menuService.list();
        return Result.success(menus);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取菜单树结构")
    @RequiresPermissions("menu:view")
    public Result<List<Menu>> getMenuTree() {
        List<Menu> menuTree = menuService.getMenuTree();
        return Result.success(menuTree);
    }

    @GetMapping("/routes")
    @Operation(summary = "获取用户路由菜单")
    public Result<List<Menu>> getRouteMenus() {
        List<Menu> routes = menuService.getRouteMenus();
        return Result.success(routes);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询菜单列表")
    @RequiresPermissions("menu:view")
    public Result<IPage<Menu>> getMenuPage(
            @Valid PageParam pageParam,
            Menu menu) {
        IPage<Menu> page = menuService.getMenuPage(pageParam, menu);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取菜单详情")
    @RequiresPermissions("menu:view")
    public Result<Menu> getMenu(@Parameter(description = "菜单ID") @PathVariable Long id) {
        Menu menu = menuService.getById(id);
        if (menu == null) {
            return Result.fail("菜单不存在");
        }
        return Result.success(menu);
    }

    @PostMapping
    @Operation(summary = "创建菜单")
    @RequiresPermissions("menu:create")
    public Result<Menu> createMenu(@Valid @RequestBody Menu menu) {
        boolean result = menuService.saveMenu(menu);
        if (result) {
            return Result.success("创建成功", menu);
        } else {
            return Result.fail("创建失败");
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新菜单")
    @RequiresPermissions("menu:update")
    public Result<Menu> updateMenu(@Parameter(description = "菜单ID") @PathVariable Long id,
                                  @Valid @RequestBody Menu menu) {
        menu.setId(id);
        boolean result = menuService.updateMenu(menu);
        if (result) {
            return Result.success("更新成功", menu);
        } else {
            return Result.fail("更新失败");
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单")
    @RequiresPermissions("menu:delete")
    public Result<Void> deleteMenu(@Parameter(description = "菜单ID") @PathVariable Long id) {
        boolean result = menuService.deleteMenu(id);
        if (result) {
            return new Result<Void>().setCode(200).setMessage("删除成功");
        } else {
            return Result.fail("删除失败");
        }
    }

    @GetMapping("/button/{parentId}")
    @Operation(summary = "获取按钮权限列表")
    @RequiresPermissions("menu:view")
    public Result<List<Menu>> getButtonPermissions(@Parameter(description = "父级菜单ID") @PathVariable Long parentId) {
        List<Menu> buttons = menuService.getButtonsByParentId(parentId);
        return Result.success(buttons);
    }
}