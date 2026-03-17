package com.declaration.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.annotation.RequiresRoles;
import com.declaration.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * 权限拦截器
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是映射到方法直接通过
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        // 获取当前登录用户ID
        if (!StpUtil.isLogin()) {
            return true; // 未登录的情况由Sa-Token拦截器处理
        }

        Long userId = StpUtil.getLoginIdAsLong();

        // 检查Sa-Token权限注解
        SaCheckPermission saPermission = method.getAnnotation(SaCheckPermission.class);
        if (saPermission == null) {
            saPermission = handlerMethod.getBeanType().getAnnotation(SaCheckPermission.class);
        }
        if (saPermission != null) {
            // Sa-Token会自动处理权限验证，这里只需要让请求通过
            // 但如果需要额外的日志记录，可以在这里添加
            log.debug("检测到Sa-Token权限注解: {}", String.join(",", saPermission.value()));
        }
        
        // 检查Sa-Token角色注解
        SaCheckRole saRole = method.getAnnotation(SaCheckRole.class);
        if (saRole == null) {
            saRole = handlerMethod.getBeanType().getAnnotation(SaCheckRole.class);
        }
        if (saRole != null) {
            // Sa-Token会自动处理角色验证，这里只需要让请求通过
            log.debug("检测到Sa-Token角色注解: {}", String.join(",", saRole.value()));
        }

        // 检查角色注解
        RequiresRoles requiresRoles = method.getAnnotation(RequiresRoles.class);
        if (requiresRoles == null) {
            requiresRoles = handlerMethod.getBeanType().getAnnotation(RequiresRoles.class);
        }
        if (requiresRoles != null) {
            if (!permissionService.hasRoles(userId, requiresRoles.value(), requiresRoles.logical())) {
                log.warn("用户 {} 角色权限不足，访问路径: {}", userId, request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"code\":403,\"message\":\"角色权限不足\"}");
                return false;
            }
        }

        // 检查权限注解
        RequiresPermissions requiresPermissions = method.getAnnotation(RequiresPermissions.class);
        if (requiresPermissions == null) {
            requiresPermissions = handlerMethod.getBeanType().getAnnotation(RequiresPermissions.class);
        }
        if (requiresPermissions != null) {
            if (!permissionService.hasPermissions(userId, requiresPermissions.value(), requiresPermissions.logical())) {
                log.warn("用户 {} 操作权限不足，访问路径: {}", userId, request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"code\":403,\"message\":\"操作权限不足\"}");
                return false;
            }
        }

        return true;
    }
}