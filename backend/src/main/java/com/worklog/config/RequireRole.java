package com.worklog.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限注解：标注在 Controller 方法或类上，要求当前用户具备指定角色之一。
 * 由 RoleAuthInterceptor 校验，不满足返回 403。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 允许访问的角色集合，如 {"ADMIN"} 或 {"ADMIN", "AUTHOR"} */
    String[] value();
}
