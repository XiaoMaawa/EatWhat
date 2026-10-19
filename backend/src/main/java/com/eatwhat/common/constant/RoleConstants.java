package com.eatwhat.common.constant;

/**
 * 角色常量。与 sys_user.role 字段取值一致。
 */
public final class RoleConstants {

    public static final String CONSUMER = "CONSUMER";
    public static final String MERCHANT = "MERCHANT";
    public static final String RIDER = "RIDER";
    public static final String ADMIN = "ADMIN";

    private RoleConstants() {
    }

    public static String[] all() {
        return new String[]{CONSUMER, MERCHANT, RIDER, ADMIN};
    }
}
