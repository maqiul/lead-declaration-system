package com.declaration.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.common.PageParam;
import com.declaration.entity.Role;

import java.util.List;

/**
 * 角色服务接口
 *
 * @author Administrator
 * @since 2026-03-13
 */
public interface RoleService extends IService<Role> {

    /**
     * 分页查询角色列表
     *
     * @param pageParam 分页参数
     * @param role 查询条件
     * @return 角色分页数据
     */
    IPage<Role> getRolePage(PageParam pageParam, Role role);

    /**
     * 新增角色
     *
     * @param role 角色信息
     * @return 是否成功
     */
    boolean saveRole(Role role);

    /**
     * 修改角色
     *
     * @param role 角色信息
     * @return 是否成功
     */
    boolean updateRole(Role role);

    /**
     * 删除角色
     *
     * @param id 角色ID
     * @return 是否成功
     */
    boolean deleteRole(Long id);

    /**
     * 获取用户角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> getUserRoles(Long userId);

    /**
     * 分配角色给用户
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 是否成功
     */
    boolean assignRoleToUser(Long userId, List<Long> roleIds);

    /**
     * 获取角色菜单权限ID列表
     *
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    List<Long> getRoleMenuIds(Long roleId);

    /**
     * 分配菜单权限给角色
     *
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     * @return 是否成功
     */
    boolean assignMenuToRole(Long roleId, List<Long> menuIds);

    /**
     * 为管理员角色分配所有权限
     *
     * @param roleId 管理员角色ID
     * @return 是否成功
     */
    boolean assignAllPermissionsToAdmin(Long roleId);

    /**
     * 获取角色的所有用户列表
     *
     * @param roleId 角色ID
     * @return 用户列表
     */
    List<com.declaration.entity.User> getRoleUsers(Long roleId);

    /**
     * 批量为用户分配角色
     *
     * @param userIds 用户ID列表
     * @param roleIds 角色ID列表
     * @return 是否成功
     */
    boolean batchAssignUserRoles(List<Long> userIds, List<Long> roleIds);

    /**
     * 移除用户的角色
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 是否成功
     */
    boolean removeUserRole(Long userId, Long roleId);
}