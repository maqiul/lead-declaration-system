package com.declaration.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.common.PageParam;
import com.declaration.entity.Menu;

import java.util.List;

/**
 * 菜单服务接口
 *
 * @author Administrator
 * @since 2026-03-13
 */
public interface MenuService extends IService<Menu> {

    /**
     * 获取菜单树
     *
     * @return 菜单树
     */
    List<Menu> getMenuTree();

    /**
     * 获取路由菜单
     *
     * @return 路由菜单
     */
    List<Menu> getRouteMenus();

    /**
     * 分页查询菜单列表
     *
     * @param pageParam 分页参数
     * @param menu 查询条件
     * @return 菜单分页数据
     */
    IPage<Menu> getMenuPage(PageParam pageParam, Menu menu);

    /**
     * 新增菜单
     *
     * @param menu 菜单信息
     * @return 是否成功
     */
    boolean saveMenu(Menu menu);

    /**
     * 修改菜单
     *
     * @param menu 菜单信息
     * @return 是否成功
     */
    boolean updateMenu(Menu menu);

    /**
     * 删除菜单
     *
     * @param id 菜单ID
     * @return 是否成功
     */
    boolean deleteMenu(Long id);

    /**
     * 根据父级ID获取按钮权限列表
     *
     * @param parentId 父级菜单ID
     * @return 按钮权限列表
     */
    List<Menu> getButtonsByParentId(Long parentId);
}