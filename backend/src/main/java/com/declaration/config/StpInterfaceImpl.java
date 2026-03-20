package com.declaration.config;

import cn.dev33.satoken.stp.StpInterface;
import com.declaration.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Sa-Token 权限认证接口实现
 * 将 Sa-Token 的权限查询接入到项目的 PermissionService
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private PermissionService permissionService;

    /**
     * 获取用户权限列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (loginId == null) {
            return new ArrayList<>();
        }
        Long userId = Long.parseLong(loginId.toString());
        Set<String> permissions = permissionService.getUserPermissions(userId);
        return new ArrayList<>(permissions);
    }

    /**
     * 获取用户角色列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return new ArrayList<>();
        }
        Long userId = Long.parseLong(loginId.toString());
        Set<String> roles = permissionService.getUserRoles(userId);
        return new ArrayList<>(roles);
    }
}
