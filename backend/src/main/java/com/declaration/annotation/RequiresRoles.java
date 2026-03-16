package com.declaration.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色控制注解
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRoles {

    /**
     * 角色标识数组
     */
    String[] value() default {};

    /**
     * 逻辑关系 AND-都需要满足 OR-满足其一即可
     */
    Logical logical() default Logical.AND;

    enum Logical {
        AND, OR
    }
}