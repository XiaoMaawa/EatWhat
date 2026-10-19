package com.eatwhat.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色访问控制注解。打在 Controller 类或方法上，由 {@link AuthInterceptor} 校验。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 允许访问的角色，满足其一即可 */
    String[] value();

    /** 是否允许匿名访问（默认 false） */
    boolean allowAnonymous() default false;
}
