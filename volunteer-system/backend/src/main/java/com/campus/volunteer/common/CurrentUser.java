package com.campus.volunteer.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 当前登录用户（ThreadLocal 保存，请求结束由拦截器清理）。
 */
@Getter
@AllArgsConstructor
public class CurrentUser {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private final Long id;
    private final String role;
    private final Long labId;

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 获取当前用户，未登录直接抛出 401 业务异常 */
    public static CurrentUser must() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(401, "登录已失效，请重新登录");
        }
        return user;
    }

    /** 获取当前用户，未登录返回 null（定时任务等场景使用） */
    public static CurrentUser optional() {
        return HOLDER.get();
    }

    public static Long id() {
        return must().getId();
    }

    public static String role() {
        return must().getRole();
    }

    public static Long labId() {
        return must().getLabId();
    }

    /**
     * 校验当前用户角色，越权抛出 403 业务异常。
     */
    public static void requireRole(String... roles) {
        CurrentUser user = must();
        for (String r : roles) {
            if (r.equals(user.getRole())) {
                return;
            }
        }
        throw new BizException(403, "无权访问该接口，需要角色：" + String.join("/", roles));
    }

    /** 是否拥有指定角色 */
    public static boolean hasRole(String role) {
        CurrentUser user = HOLDER.get();
        return user != null && role.equals(user.getRole());
    }
}
