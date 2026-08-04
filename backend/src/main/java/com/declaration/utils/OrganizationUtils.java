package com.declaration.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.declaration.entity.Organization;
import com.declaration.entity.User;
import com.declaration.service.OrganizationService;
import com.declaration.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 组织ID获取工具类
 * 提供安全的组织ID获取方法，处理各种异常情况
 */
@Slf4j
@Component
public class OrganizationUtils {

    private static UserService userService;
    private static OrganizationService organizationService;

    @Autowired
    public void setUserService(UserService userService) {
        OrganizationUtils.userService = userService;
    }
    
    @Autowired
    public void setOrganizationService(OrganizationService organizationService) {
        OrganizationUtils.organizationService = organizationService;
    }

    /**
     * 获取当前登录用户的组织ID
     * @return 组织ID，如果无法获取则返回null
     */
    public static Long getCurrentUserOrgId() {
        try {
            if (!StpUtil.isLogin()) {
                log.debug("用户未登录，无法获取组织ID");
                return null;
            }

            // 从Session中获取组织ID
            Object orgIdObj = StpUtil.getSession().get("orgId");
            if (orgIdObj != null) {
                try {
                    return Long.valueOf(orgIdObj.toString());
                } catch (NumberFormatException e) {
                    log.warn("组织ID格式转换失败: {}", orgIdObj);
                }
            }

            // 如果Session中没有，尝试从用户信息中获取
            Long userId = StpUtil.getLoginIdAsLong();
            if (userService != null) {
                User user = userService.getById(userId);
                if (user != null && user.getOrgId() != null) {
                    log.debug("从用户信息中获取组织ID: {} 为用户 {}", user.getOrgId(), userId);
                    return user.getOrgId();
                }
            }
            
            log.debug("用户 {} 未设置组织ID", userId);
            return null;
        } catch (Exception e) {
            log.error("获取用户组织ID时发生异常", e);
            return null;
        }
    }

    /**
     * 获取当前登录用户的组织名称
     * @return 组织名称，如果无法获取则返回null
     */
    public static String getCurrentUserOrgName() {
        Long orgId = getCurrentUserOrgId();
        if (orgId == null) {
            return null;
        }

        try {
            if (organizationService != null) {
                Organization org = organizationService.getById(orgId);
                return org != null ? org.getOrgName() : null;
            }
        } catch (Exception e) {
            log.error("获取组织名称时发生异常", e);
        }
        
        return null;
    }

    /**
     * 验证用户是否有指定组织的访问权限
     * @param targetOrgId 目标组织ID
     * @return 是否有权限
     */
    public static boolean hasOrgPermission(Long targetOrgId) {
        if (targetOrgId == null) {
            return false;
        }

        Long currentUserOrgId = getCurrentUserOrgId();
        if (currentUserOrgId == null) {
            // 如果用户没有组织，可能需要特殊处理
            return false;
        }

        // 同一组织内可以访问
        if (targetOrgId.equals(currentUserOrgId)) {
            return true;
        }

        // 管理员可能有跨组织权限
        return StpUtil.hasPermission("system:org:manage");
    }

    /**
     * 递归收集指定组织及其所有子组织的 ID（基于 sys_org.parent_id）
     * @param orgId 根组织ID
     * @return 组织ID集合（含自身），异常或为空时返回仅含自身的列表
     */
    public static List<Long> getDescendantOrgIds(Long orgId) {
        List<Long> result = new ArrayList<>();
        if (orgId == null) {
            return result;
        }
        result.add(orgId);
        try {
            if (organizationService == null) {
                return result;
            }
            // 一次性加载全量组织树，在内存中做 BFS，避免逐层查库
            List<Organization> allOrgs = organizationService.list();
            Deque<Long> queue = new ArrayDeque<>();
            queue.add(orgId);
            while (!queue.isEmpty()) {
                Long current = queue.poll();
                for (Organization org : allOrgs) {
                    if (current.equals(org.getParentId()) && org.getId() != null && !result.contains(org.getId())) {
                        result.add(org.getId());
                        queue.add(org.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取子组织树时发生异常, orgId={}", orgId, e);
        }
        return result;
    }

    /**
     * 获取数据访问范围的组织ID条件
     * 用于构建查询条件
     * @return 组织ID查询条件，null表示无限制
     */
    public static Long getDataScopeOrgId() {
        // 管理员可以查看所有数据
        if (StpUtil.hasPermission("system:data:all")) {
            return null;
        }
        
        // 普通用户只能查看自己组织的数据
        return getCurrentUserOrgId();
    }
}