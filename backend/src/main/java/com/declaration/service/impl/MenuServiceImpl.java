package com.declaration.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.common.PageParam;
import com.declaration.dao.MenuDao;
import com.declaration.dao.RoleMenuDao;
import com.declaration.entity.Menu;
import com.declaration.entity.RoleMenu;
import com.declaration.service.MenuService;
import com.declaration.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单服务实现类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuDao, Menu> implements MenuService {

    private final RoleMenuDao roleMenuDao;
    private final PermissionService permissionService;

    @Override
    public List<Menu> getMenuTree() {
        // 查询所有有效的菜单
        List<Menu> menus = this.list(
            new LambdaQueryWrapper<Menu>()
                .eq(Menu::getStatus, 1)
                .orderByAsc(Menu::getSort)
        );
        
        // 构建菜单树
        return buildMenuTree(menus);
    }

    @Override
    public List<Menu> getRouteMenus() {
        if (!StpUtil.isLogin()) {
            return CollUtil.newArrayList();
        }
        
        Long userId = StpUtil.getLoginIdAsLong();
        return permissionService.getUserMenuTree(userId);
    }

    @Override
    public IPage<Menu> getMenuPage(PageParam pageParam, Menu menu) {
        LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (menu != null) {
            if (menu.getParentId() != null) {
                queryWrapper.eq(Menu::getParentId, menu.getParentId());
            }
            if (menu.getMenuName() != null && !menu.getMenuName().isEmpty()) {
                queryWrapper.like(Menu::getMenuName, menu.getMenuName());
            }
            if (menu.getMenuType() != null) {
                queryWrapper.eq(Menu::getMenuType, menu.getMenuType());
            }
            if (menu.getStatus() != null) {
                queryWrapper.eq(Menu::getStatus, menu.getStatus());
            }
        }
        
        queryWrapper.orderByAsc(Menu::getSort);
        
        // 分页查询
        Page<Menu> page = new Page<>(pageParam.getCurrent(), pageParam.getSize());
        return this.page(page, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveMenu(Menu menu) {
        return this.save(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMenu(Menu menu) {
        boolean result = this.updateById(menu);
        if (result) {
            // 清除所有用户的权限缓存（因为菜单变更可能影响权限）
            permissionService.clearUserPermissionCache(null); // TODO: 实现清除所有用户缓存
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMenu(Long id) {
        // 检查是否有子菜单
        long childCount = this.count(new LambdaQueryWrapper<Menu>().eq(Menu::getParentId, id));
        if (childCount > 0) {
            throw new RuntimeException("该菜单下存在子菜单，无法删除");
        }
        
        // 检查是否有角色关联
        long roleCount = roleMenuDao.selectCount(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getMenuId, id));
        if (roleCount > 0) {
            throw new RuntimeException("该菜单已被角色使用，无法删除");
        }
        
        boolean result = this.removeById(id);
        if (result) {
            // 清除所有用户的权限缓存
            permissionService.clearUserPermissionCache(null); // TODO: 实现清除所有用户缓存
        }
        return result;
    }

    @Override
    public List<Menu> getButtonsByParentId(Long parentId) {
        return this.list(
            new LambdaQueryWrapper<Menu>()
                .eq(Menu::getParentId, parentId)
                .eq(Menu::getMenuType, 3) // 按钮类型
                .eq(Menu::getStatus, 1)
                .orderByAsc(Menu::getSort)
        );
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