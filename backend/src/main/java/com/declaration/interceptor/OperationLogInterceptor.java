package com.declaration.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.declaration.annotation.OperationLog;
import com.declaration.service.OperationLogService;
import com.declaration.util.ServletUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 操作日志拦截器
 *
 * @author Administrator
 * @since 2026-03-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogInterceptor implements HandlerInterceptor {

    private final OperationLogService operationLogService;

    // 需要排除的URL
    private static final List<String> EXCLUDE_URLS = Arrays.asList(
        "/user/login",
        "/user/logout",
        "/captcha",
        "/favicon.ico",
        "/error"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只处理方法级别的处理器
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        // 检查是否有操作日志注解
        OperationLog operationLog = method.getAnnotation(OperationLog.class);
        if (operationLog == null) {
            operationLog = handlerMethod.getBeanType().getAnnotation(OperationLog.class);
        }

        if (operationLog != null) {
            // 设置开始时间到request属性中
            request.setAttribute("operationStartTime", System.currentTimeMillis());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 只处理方法级别的处理器
        if (!(handler instanceof HandlerMethod)) {
            return;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        // 检查是否有操作日志注解
        OperationLog operationLog = method.getAnnotation(OperationLog.class);
        if (operationLog == null) {
            operationLog = handlerMethod.getBeanType().getAnnotation(OperationLog.class);
        }

        if (operationLog != null) {
            try {
                // 构建操作日志对象
                com.declaration.entity.OperationLog logEntity = buildOperationLog(request, response, handlerMethod, operationLog, ex);
                
                // 异步保存操作日志
                operationLogService.saveOperationLog(logEntity);
            } catch (Exception e) {
                log.error("记录操作日志失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 构建操作日志对象
     */
    private com.declaration.entity.OperationLog buildOperationLog(HttpServletRequest request, HttpServletResponse response,
                                                                 HandlerMethod handlerMethod, OperationLog operationLog, Exception ex) {
        com.declaration.entity.OperationLog logEntity = new com.declaration.entity.OperationLog();
        
        // 获取用户信息
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            logEntity.setUserId(userId);
            // 这里可以从用户服务获取用户名，暂时用ID代替
            logEntity.setUsername(String.valueOf(userId));
        }

        // 设置操作类型和业务类型
        logEntity.setOperationType(StringUtils.defaultIfEmpty(operationLog.operationType(), getOperationType(request)));
        logEntity.setBusinessType(StringUtils.defaultIfEmpty(operationLog.businessType(), getBusinessType(handlerMethod)));

        // 设置请求信息
        logEntity.setMethod(request.getMethod());
        logEntity.setRequestUrl(request.getRequestURI());
        
        // 设置IP和浏览器信息
        logEntity.setIpAddress(ServletUtils.getIpAddr(request));
        logEntity.setBrowser(ServletUtils.getBrowser(request));
        logEntity.setOs(ServletUtils.getOS(request));

        // 设置请求参数
        if (operationLog.saveRequestData()) {
            logEntity.setRequestParams(buildRequestParams(request));
        }

        // 设置响应结果和状态
        logEntity.setStatus(ex == null ? 0 : 1);
        if (ex != null) {
            logEntity.setErrorMsg(ex.getMessage());
        }

        // 设置操作耗时
        Long startTime = (Long) request.getAttribute("operationStartTime");
        if (startTime != null) {
            logEntity.setCostTime(System.currentTimeMillis() - startTime);
        }

        // 设置时间
        logEntity.setCreateTime(LocalDateTime.now());
        logEntity.setUpdateTime(LocalDateTime.now());

        return logEntity;
    }

    /**
     * 获取操作类型
     */
    private String getOperationType(HttpServletRequest request) {
        String method = request.getMethod();
        switch (method) {
            case "GET": return "QUERY";
            case "POST": return "CREATE";
            case "PUT": return "UPDATE";
            case "DELETE": return "DELETE";
            default: return "OTHER";
        }
    }

    /**
     * 获取业务类型
     */
    private String getBusinessType(HandlerMethod handlerMethod) {
        String className = handlerMethod.getBeanType().getSimpleName();
        // 移除Controller后缀
        if (className.endsWith("Controller")) {
            return className.substring(0, className.length() - 10);
        }
        return className;
    }

    /**
     * 构建请求参数
     */
    private String buildRequestParams(HttpServletRequest request) {
        try {
            // 获取请求参数
            String queryString = request.getQueryString();
            if (StringUtils.isNotBlank(queryString)) {
                return queryString;
            }
            
            // 获取POST请求体
            String body = ServletUtils.getBodyString(request);
            return StringUtils.defaultString(body, "");
        } catch (Exception e) {
            log.warn("构建请求参数失败: {}", e.getMessage());
            return "";
        }
    }
}