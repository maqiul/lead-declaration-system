package com.declaration.service;

import com.declaration.annotation.RequiresPermissions;
import com.declaration.annotation.RequiresRoles;
import com.declaration.entity.Menu;

import java.util.List;
import java.util.Set;

/**
 * 权限服务接口
 *
 * @author Administrator
 * @since 2026-03-13
 */
public interface PermissionService {

    /**
     * 获取用户权限标识集合
     *
     * @param userId 用户ID
     * @return 权限标识集合
     */
    Set<String> getUserPermissions(Long userId);

    /**
     * 获取用户角色标识集合
     *
     * @param userId 用户ID
     * @return 角色标识集合
     */
    Set<String> getUserRoles(Long userId);

    /**
     * 获取用户菜单树
     *
     * @param userId 用户ID
     * @return 菜单树
     */
    List<Menu> getUserMenuTree(Long userId);

    /**
     * 验证用户是否拥有指定权限
     *
     * @param userId 用户ID
     * @param permissions 权限标识数组
     * @param logical 逻辑关系
     * @return 是否拥有权限
     */
    boolean hasPermissions(Long userId, String[] permissions, RequiresPermissions.Logical logical);

    /**
     * 验证用户是否拥有指定角色
     *
     * @param userId 用户ID
     * @param roles 角色标识数组
     * @param logical 逻辑关系
     * @return 是否拥有角色
     */
    boolean hasRoles(Long userId, String[] roles, RequiresRoles.Logical logical);

    /**
     * 清除用户权限缓存
     *
     * @param userId 用户ID
     */
    void clearUserPermissionCache(Long userId);
}