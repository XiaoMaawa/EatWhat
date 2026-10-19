package com.eatwhat.common.utils;

/**
 * 当前请求上下文：保存登录用户信息，供 Service 层读取（避免 Service 依赖 Servlet API）。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    public static String role() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getRole();
    }

    public static String phone() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getPhone();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(role());
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 登录用户快照。
     */
    public static class LoginUser {
        private Long userId;
        private String phone;
        private String nickname;
        private String role;

        public LoginUser() {
        }

        public LoginUser(Long userId, String phone, String nickname, String role) {
            this.userId = userId;
            this.phone = phone;
            this.nickname = nickname;
            this.role = role;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
