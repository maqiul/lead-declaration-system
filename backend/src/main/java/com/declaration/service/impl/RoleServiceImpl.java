package com.declaration.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.common.PageParam;
import com.declaration.dao.RoleDao;
import com.declaration.dao.RoleMenuDao;
import com.declaration.dao.UserRoleDao;
import com.declaration.entity.Menu;
import com.declaration.entity.Role;
import com.declaration.entity.RoleMenu;
import com.declaration.entity.UserRole;
import com.declaration.service.MenuService;
import com.declaration.service.PermissionService;
import com.declaration.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleDao, Role> implements RoleService {

    private final UserRoleDao userRoleDao;
    private final RoleMenuDao roleMenuDao;
    private final PermissionService permissionService;
    private final MenuService menuService;
    private final com.declaration.dao.UserDao userDao;

    @Override
    public IPage<Role> getRolePage(PageParam pageParam, Role role) {
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();

        // 添加查询条件
        if (role != null) {
            if (role.getRoleName() != null && !role.getRoleName().isEmpty()) {
                queryWrapper.like(Role::getRoleName, role.getRoleName());
            }
            if (role.getRoleCode() != null && !role.getRoleCode().isEmpty()) {
                queryWrapper.like(Role::getRoleCode, role.getRoleCode());
            }
            if (role.getStatus() != null) {
                queryWrapper.eq(Role::getStatus, role.getStatus());
            }
        }

        try {
            if (!cn.dev33.satoken.stp.StpUtil.hasRole("ADMIN")) {
                queryWrapper.notIn(Role::getRoleCode, "ADMIN", "ADMIN");
            }
        } catch (Exception e) {
            log.warn("检查当前用户角色状态异常", e);
        }

        queryWrapper.orderByDesc(Role::getCreateTime);

        // 分页查询
        Page<Role> page = new Page<>(pageParam.getCurrent(), pageParam.getSize());
        return this.page(page, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveRole(Role role) {
        return this.save(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(Role role) {
        boolean result = this.updateById(role);
        if (result) {
            // 清除相关用户的权限缓存
            List<UserRole> userRoles = userRoleDao.selectList(
                    new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, role.getId()));
            if (CollUtil.isNotEmpty(userRoles)) {
                userRoles.forEach(ur -> permissionService.clearUserPermissionCache(ur.getUserId()));
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRole(Long id) {
        // 检查是否有用户关联
        long userCount = userRoleDao.selectCount(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, id));
        if (userCount > 0) {
            throw new RuntimeException("该角色已被用户使用，无法删除");
        }

        boolean result = this.removeById(id);
        if (result) {
            // 删除角色菜单关联
            roleMenuDao.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id));
        }
        return result;
    }

    @Override
    public List<Role> getUserRoles(Long userId) {
        List<UserRole> userRoles = userRoleDao.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));

        if (CollUtil.isEmpty(userRoles)) {
            return CollUtil.newArrayList();
        }

        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());

        return this.listByIds(roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoleToUser(Long userId, List<Long> roleIds) {
        // 删除原有角色关联
        userRoleDao.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));

        // 添加新角色关联
        if (CollUtil.isNotEmpty(roleIds)) {
            for (Long roleId : roleIds) {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleDao.insert(userRole);
            }
        }

        // 清除用户权限缓存
        permissionService.clearUserPermissionCache(userId);

        return true;
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        List<RoleMenu> roleMenus = roleMenuDao.selectList(
                new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));

        return roleMenus.stream()
                .map(RoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignMenuToRole(Long roleId, List<Long> menuIds) {
        // 参数校验
        if (roleId == null) {
            throw new IllegalArgumentException("角色ID不能为空");
        }

        // 删除原有菜单关联
        roleMenuDao.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));

        // 添加新菜单关联
        if (CollUtil.isNotEmpty(menuIds)) {
            for (Long menuId : menuIds) {
                if (menuId == null) {
                    log.warn("跳过空的菜单ID");
                    continue;
                }

                RoleMenu roleMenu = new RoleMenu();
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                roleMenuDao.insert(roleMenu);
            }
        }

        // 清除所有拥有此角色的用户的权限缓存
        List<UserRole> userRoles = userRoleDao.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId));
        if (CollUtil.isNotEmpty(userRoles)) {
            userRoles.forEach(ur -> permissionService.clearUserPermissionCache(ur.getUserId()));
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignAllPermissionsToAdmin(Long roleId) {
        // 检查是否为管理员角色
        Role role = this.getById(roleId);
        if (role == null || !("admin".equals(role.getRoleCode()) || "ADMIN".equals(role.getRoleCode()))) {
            throw new RuntimeException("只能为管理员角色分配全部权限");
        }

        // 获取所有启用的菜单
        List<Menu> allMenus = menuService.list(
                new LambdaQueryWrapper<Menu>()
                        .eq(Menu::getStatus, 1)
                        .orderByAsc(Menu::getSort));

        if (CollUtil.isEmpty(allMenus)) {
            log.warn("没有找到启用的菜单");
            return true;
        }

        // 提取所有菜单ID
        List<Long> allMenuIds = allMenus.stream()
                .map(Menu::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(allMenuIds)) {
            log.warn("没有有效的菜单ID");
            return true;
        }

        // 删除原有菜单关联
        roleMenuDao.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));

        // 添加所有菜单关联
        for (Long menuId : allMenuIds) {
            RoleMenu roleMenu = new RoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            roleMenuDao.insert(roleMenu);
        }

        // 清除所有拥有此角色的用户的权限缓存
        List<UserRole> userRoles = userRoleDao.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId));
        if (CollUtil.isNotEmpty(userRoles)) {
            userRoles.forEach(ur -> permissionService.clearUserPermissionCache(ur.getUserId()));
        }

        log.info("为管理员角色[{}]分配了{}个菜单权限", role.getRoleName(), allMenuIds.size());
        return true;
    }

    @Override
    public List<com.declaration.entity.User> getRoleUsers(Long roleId) {
        // 查询该角色下的所有用户ID
        List<UserRole> userRoles = userRoleDao.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId));
        
        if (CollUtil.isEmpty(userRoles)) {
            return List.of();
        }

        // 获取用户ID列表
        List<Long> userIds = userRoles.stream()
                .map(UserRole::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 查询用户详情
        return listUsersByIds(userIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchAssignUserRoles(List<Long> userIds, List<Long> roleIds) {
        if (CollUtil.isEmpty(userIds) || CollUtil.isEmpty(roleIds)) {
            log.warn("用户ID列表或角色ID列表为空");
            return false;
        }

        // 批量插入用户角色关联
        for (Long userId : userIds) {
            for (Long roleId : roleIds) {
                // 检查是否已存在
                long count = userRoleDao.selectCount(
                        new LambdaQueryWrapper<UserRole>()
                                .eq(UserRole::getUserId, userId)
                                .eq(UserRole::getRoleId, roleId));
                
                if (count == 0) {
                    UserRole userRole = new UserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    userRoleDao.insert(userRole);
                    log.info("为用户[{}]分配角色[{}]", userId, roleId);
                }
            }
            // 清除用户权限缓存
            permissionService.clearUserPermissionCache(userId);
        }

        log.info("批量为用户分配角色: {}个用户, {}个角色", userIds.size(), roleIds.size());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeUserRole(Long userId, Long roleId) {
        int deleted = userRoleDao.delete(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId)
                        .eq(UserRole::getRoleId, roleId));
        
        if (deleted > 0) {
            // 清除用户权限缓存
            permissionService.clearUserPermissionCache(userId);
            log.info("移除用户[{}]的角色[{}]", userId, roleId);
            return true;
        }
        
        log.warn("用户[{}]的角色[{}]不存在", userId, roleId);
        return false;
    }

    /**
     * 根据用户ID列表查询用户
     */
    private List<com.declaration.entity.User> listUsersByIds(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return List.of();
        }
        
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.declaration.entity.User> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.in(com.declaration.entity.User::getId, userIds);
        wrapper.eq(com.declaration.entity.User::getStatus, 1); // 只查询启用的用户
        
        return userDao.selectList(wrapper);
    }
}