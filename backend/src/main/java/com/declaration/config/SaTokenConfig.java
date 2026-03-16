package com.declaration.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.declaration.interceptor.PermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 登录校验。
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/swagger-resources/**",
                "/webjars/**",
                "/v3/**",
                "/doc.html",
                "/swagger-ui/**",
                "/favicon.ico",
                "/error",
                "/user/login",
                "/user/register",
                "/login",
                "/register"
            );
        
        // 注册权限拦截器
        registry.addInterceptor(permissionInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/swagger-resources/**",
                "/webjars/**",
                "/v3/**",
                "/doc.html",
                "/swagger-ui/**",
                "/favicon.ico",
                "/error",
                "/user/login",
                "/user/register",
                "/login",
                "/register"
            );
    }
}