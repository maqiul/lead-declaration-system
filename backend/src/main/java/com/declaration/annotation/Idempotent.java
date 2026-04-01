package com.declaration.annotation;

import java.lang.annotation.*;

/**
 * 防重复提交保护注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    /**
     * 防重锁的前缀
     */
    String prefix() default "sys:idempotent:";

    /**
     * 防重过期时间（秒），默认3秒内不能同一用户重复提交同一接口
     */
    long expireTime() default 3;
    
    /**
     * 错误提示语
     */
    String message() default "请求正在处理中，请勿频繁操作！";
}
