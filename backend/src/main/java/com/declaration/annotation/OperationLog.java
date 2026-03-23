package com.declaration.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标记需要记录操作日志的方法
 *
 * @author Administrator
 * @since 2026-03-23
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作类型
     */
    String operationType() default "";

    /**
     * 业务类型
     */
    String businessType() default "";

    /**
     * 是否记录请求参数
     */
    boolean saveRequestData() default true;

    /**
     * 是否记录响应结果
     */
    boolean saveResponseData() default true;
}