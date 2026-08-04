package com.declaration.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.declaration.entity.DeclarationForm;
import com.declaration.entity.User;
import com.declaration.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 申报单数据权限范围工具
 * 三级数据可见性：
 * 1. admin(userId=1) 或拥有 business:declaration:audit（资料/流程审核）→ 全部可见
 * 2. 拥有 business:declaration:view-scope（查看下级申报，配给部门主管类角色）→ 自己 + 本组织及所有子组织
 * 3. 其他用户 → 仅自己创建的申报单
 */
@Slf4j
@Component
public class DeclarationDataScopeUtil {

    /** 查看下级申报（组织树向下可见）权限点 */
    public static final String PERM_VIEW_SCOPE = "business:declaration:view-scope";
    /** 申报审核权限点（可查看全部数据） */
    public static final String PERM_AUDIT = "business:declaration:audit";

    private static UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        DeclarationDataScopeUtil.userService = userService;
    }

    /**
     * 为申报单列表查询应用数据权限过滤
     */
    public static void apply(LambdaQueryWrapper<DeclarationForm> wrapper) {
        if (!StpUtil.isLogin()) {
            // 未登录，返回空条件（查询不到任何数据）
            wrapper.eq(DeclarationForm::getId, -1);
            return;
        }

        Long userId = StpUtil.getLoginIdAsLong();

        // 管理员或审核权限：全部可见
        if (userId == 1L || StpUtil.hasPermission(PERM_AUDIT)) {
            return;
        }

        // 主管类权限：自己 + 本组织及所有子组织
        if (StpUtil.hasPermission(PERM_VIEW_SCOPE)) {
            Long orgId = OrganizationUtils.getCurrentUserOrgId();
            if (orgId != null) {
                List<Long> orgIds = OrganizationUtils.getDescendantOrgIds(orgId);
                wrapper.and(w -> w.eq(DeclarationForm::getCreateBy, userId)
                        .or().in(DeclarationForm::getOrgId, orgIds));
                return;
            }
        }

        // 普通用户：仅自己创建的
        wrapper.eq(DeclarationForm::getCreateBy, userId);
    }

    /**
     * 判断当前用户是否有权查看指定申报单（详情接口越权校验用）
     */
    public static boolean canView(DeclarationForm form) {
        if (form == null) {
            return true;
        }
        if (!StpUtil.isLogin()) {
            return false;
        }

        Long userId = StpUtil.getLoginIdAsLong();
        if (userId == 1L || StpUtil.hasPermission(PERM_AUDIT)) {
            return true;
        }

        // 自己创建的始终可见
        if (form.getCreateBy() != null && form.getCreateBy().equals(userId)) {
            return true;
        }

        if (StpUtil.hasPermission(PERM_VIEW_SCOPE) && form.getOrgId() != null) {
            Long orgId = OrganizationUtils.getCurrentUserOrgId();
            if (orgId != null) {
                return OrganizationUtils.getDescendantOrgIds(orgId).contains(form.getOrgId());
            }
        }

        return false;
    }

    /**
     * 获取当前用户信息（内部使用，容错返回 null）
     */
    public static User getCurrentUser() {
        try {
            if (!StpUtil.isLogin() || userService == null) {
                return null;
            }
            return userService.getById(StpUtil.getLoginIdAsLong());
        } catch (Exception e) {
            log.error("获取当前用户信息失败", e);
            return null;
        }
    }
}
