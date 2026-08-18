package com.worklog.common;

/**
 * 请求级用户上下文：由 JwtAuthInterceptor 在鉴权通过后写入当前用户 id，
 * Service 层统一从 {@link #get()} 获取以强制数据隔离，避免越权。
 * 基于 ThreadLocal，请求结束由拦截器 afterCompletion 清理。
 */
public final class UserContext {

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId) {
        HOLDER.set(userId);
    }

    /** 当前登录用户 id；未登录（定时任务等无请求上下文）返回 null */
    public static Long get() {
        return HOLDER.get();
    }

    /** 当前登录用户 id；未登录抛 401，用于必须登录才能访问的业务 */
    public static Long require() {
        Long id = HOLDER.get();
        if (id == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return id;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
