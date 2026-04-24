package com.declaration.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.annotation.RequiresRoles;
import com.declaration.dao.MenuDao;
import com.declaration.dao.RoleDao;
import com.declaration.dao.RoleMenuDao;
import com.declaration.dao.UserRoleDao;
import com.declaration.entity.Menu;
import com.declaration.entity.Role;
import com.declaration.entity.RoleMenu;
import com.declaration.entity.UserRole;
import com.declaration.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 权限服务实现类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final UserRoleDao userRoleDao;
    private final RoleDao roleDao;
    private final RoleMenuDao roleMenuDao;
    private final MenuDao menuDao;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PERMISSION_CACHE_PREFIX = "user:permissions:";
    private static final String ROLE_CACHE_PREFIX = "user:roles:";
    private static final long CACHE_EXPIRE_TIME = 30; // 30分钟

    @Override
    public Set<String> getUserPermissions(Long userId) {
        String cacheKey = PERMISSION_CACHE_PREFIX + userId;

        // 先从缓存获取
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            // 类型安全检查
            if (cachedData instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<String> permissions = (Set<String>) cachedData;
                return permissions;
            } else if (cachedData instanceof Collection) {
                // 兼容 ArrayList 等 Collection 类型（防御性编程）
                @SuppressWarnings("unchecked")
                Collection<String> permCollection = (Collection<String>) cachedData;
                return new HashSet<>(permCollection);
            } else {
                // 如果类型不匹配，清除缓存
                redisTemplate.delete(cacheKey);
                log.warn("缓存数据类型不匹配，已清除缓存: {}", cacheKey);
            }
        }

        // 管理员(userId=1)拥有所有权限
        if (userId != null && userId == 1L) {
            Set<String> adminPerms = new HashSet<>();
            adminPerms.add("*:*:*");
            redisTemplate.opsForValue().set(cacheKey, adminPerms, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
            return adminPerms;
        }

        // 查询用户角色
        List<UserRole> userRoles = userRoleDao.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));

        Set<String> permissions;
        if (CollUtil.isEmpty(userRoles)) {
            permissions = new HashSet<>();
        } else {
            // 查询角色对应的菜单权限
            List<Long> roleIds = userRoles.stream()
                    .map(UserRole::getRoleId)
                    .collect(Collectors.toList());

            List<RoleMenu> roleMenus = roleMenuDao.selectList(
                    new LambdaQueryWrapper<RoleMenu>().in(RoleMenu::getRoleId, roleIds));

            if (CollUtil.isNotEmpty(roleMenus)) {
                List<Long> menuIds = roleMenus.stream()
                        .map(RoleMenu::getMenuId)
                        .collect(Collectors.toList());

                List<Menu> menus = menuDao.selectList(
                        new LambdaQueryWrapper<Menu>()
                                .in(Menu::getId, menuIds)
                                .eq(Menu::getStatus, 1)
                                .isNotNull(Menu::getPermission)
                                .ne(Menu::getPermission, ""));

                permissions = menus.stream()
                        .map(Menu::getPermission)
                        .collect(Collectors.toSet());
            } else {
                permissions = new HashSet<>();
            }
        }

        // 缓存权限数据
        redisTemplate.opsForValue().set(cacheKey, permissions, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        return permissions;
    }

    @Override
    public Set<String> getUserRoles(Long userId) {
        String cacheKey = ROLE_CACHE_PREFIX + userId;

        // 先从缓存获取
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            // 类型安全检查
            if (cachedData instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<String> roles = (Set<String>) cachedData;
                return roles;
            } else if (cachedData instanceof Collection) {
                // 兼容 ArrayList 等 Collection 类型（防御性编程）
                @SuppressWarnings("unchecked")
                Collection<String> roleCollection = (Collection<String>) cachedData;
                return new HashSet<>(roleCollection);
            } else {
                // 如果类型不匹配，清除缓存
                redisTemplate.delete(cacheKey);
                log.warn("角色缓存数据类型不匹配，已清除缓存: {}", cacheKey);
            }
        }

        // 查询用户角色
        List<UserRole> userRoles = userRoleDao.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));

        Set<String> roles;
        if (CollUtil.isEmpty(userRoles)) {
            roles = new HashSet<>();
        } else {
            List<Long> roleIds = userRoles.stream()
                    .map(UserRole::getRoleId)
                    .collect(Collectors.toList());

            List<Role> roleList = roleDao.selectList(
                    new LambdaQueryWrapper<Role>()
                            .in(Role::getId, roleIds)
                            .eq(Role::getStatus, 1));

            roles = roleList.stream()
                    .map(Role::getRoleCode)
                    .collect(Collectors.toSet());
        }

        // 缓存角色数据
        redisTemplate.opsForValue().set(cacheKey, roles, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        return roles;
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        // 查询用户角色
        List<UserRole> userRoles = userRoleDao.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));

        if (CollUtil.isEmpty(userRoles)) {
            return new ArrayList<>();
        }

        return userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Menu> getUserMenuTree(Long userId) {
        // 管理员拥有所有菜单权限
        if (userId != null && userId == 1L) {
            List<Menu> allMenus = menuDao.selectList(
                    new LambdaQueryWrapper<Menu>()
                            .eq(Menu::getStatus, 1)
                            .eq(Menu::getIsShow, 1) // 过滤隐藏菜单
                            .orderByAsc(Menu::getSort));
            return buildMenuTree(allMenus);
        }

        // 1. 查询用户的所有角色ID
        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 通过 sys_role_menu 查询用户角色关联的所有菜单ID
        List<RoleMenu> roleMenus = roleMenuDao.selectList(
                new LambdaQueryWrapper<RoleMenu>().in(RoleMenu::getRoleId, roleIds));

        if (roleMenus == null || roleMenus.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> menuIds = roleMenus.stream()
                .map(RoleMenu::getMenuId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 查询这些菜单的详细信息（只查启用+显示的）
        List<Menu> userMenus = menuDao.selectList(
                new LambdaQueryWrapper<Menu>()
                        .in(Menu::getId, menuIds)
                        .eq(Menu::getStatus, 1)
                        .eq(Menu::getIsShow, 1) // 过滤隐藏菜单
                        .orderByAsc(Menu::getSort));

        // 4. 构建菜单树
        return buildMenuTree(userMenus);
    }

    @Override
    public boolean hasPermissions(Long userId, String[] permissions, RequiresPermissions.Logical logical) {
        // 管理员默认拥有所有权限
        if (userId != null && userId == 1L) {
            return true;
        }

        if (permissions == null || permissions.length == 0) {
            return true;
        }

        Set<String> userPermissions = getUserPermissions(userId);
        if (CollUtil.isEmpty(userPermissions)) {
            return false;
        }

        if (logical == RequiresPermissions.Logical.AND) {
            // AND逻辑：必须拥有所有权限
            for (String permission : permissions) {
                if (!userPermissions.contains(permission)) {
                    return false;
                }
            }
            return true;
        } else {
            // OR逻辑：拥有任一权限即可
            for (String permission : permissions) {
                if (userPermissions.contains(permission)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public boolean hasRoles(Long userId, String[] roles, RequiresRoles.Logical logical) {
        // 管理员默认拥有所有角色
        if (userId != null && userId == 1L) {
            return true;
        }

        if (roles == null || roles.length == 0) {
            return true;
        }

        Set<String> userRoles = getUserRoles(userId);
        if (CollUtil.isEmpty(userRoles)) {
            return false;
        }

        if (logical == RequiresRoles.Logical.AND) {
            // AND逻辑：必须拥有所有角色
            for (String role : roles) {
                if (!userRoles.contains(role)) {
                    return false;
                }
            }
            return true;
        } else {
            // OR逻辑：拥有任一角色即可
            for (String role : roles) {
                if (userRoles.contains(role)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void clearUserPermissionCache(Long userId) {
        redisTemplate.delete(PERMISSION_CACHE_PREFIX + userId);
        redisTemplate.delete(ROLE_CACHE_PREFIX + userId);
    }

    /**
     * 根据权限过滤菜单
     */
    private List<Menu> filterMenusByPermissions(List<Menu> menus, Set<String> permissions) {
        return menus.stream()
                .filter(menu -> menu.getMenuType() != 3 || // 非按钮类型直接显示
                        permissions.contains(menu.getPermission())) // 按钮类型需要有对应权限
                .collect(Collectors.toList());
    }

    /**
     * 构建菜单树
     */
    private List<Menu> buildMenuTree(List<Menu> menus) {
        List<Menu> tree = menus.stream()
                .filter(menu -> menu.getParentId() == null || menu.getParentId() == 0)
                .peek(menu -> menu.setChildren(getChildren(menu, menus)))
                .collect(Collectors.toList());

        return tree;
    }

    /**
     * 获取子菜单
     */
    private List<Menu> getChildren(Menu parent, List<Menu> menus) {
        return menus.stream()
                .filter(menu -> menu.getParentId() != null && menu.getParentId().equals(parent.getId()))
                .peek(menu -> menu.setChildren(getChildren(menu, menus)))
                .sorted((m1, m2) -> {
                    int sort1 = m1.getSort() == null ? 0 : m1.getSort();
                    int sort2 = m2.getSort() == null ? 0 : m2.getSort();
                    return Integer.compare(sort1, sort2);
                })
                .collect(Collectors.toList());
    }
}