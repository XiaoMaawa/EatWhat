package com.eatwhat.common.constant;

/**
 * 平台通用常量。
 */
public final class CommonConstants {

    /** 订单状态：0 待支付 */
    public static final int ORDER_PENDING_PAYMENT = 0;
    /** 订单状态：1 待接单 */
    public static final int ORDER_PENDING_ACCEPT = 1;
    /** 订单状态：2 备餐中 */
    public static final int ORDER_PREPARING = 2;
    /** 订单状态：3 待配送 */
    public static final int ORDER_PENDING_DELIVERY = 3;
    /** 订单状态：4 配送中 */
    public static final int ORDER_DELIVERING = 4;
    /** 订单状态：5 已完成 */
    public static final int ORDER_COMPLETED = 5;
    /** 订单状态：6 已取消 */
    public static final int ORDER_CANCELLED = 6;
    /** 订单状态：7 退款中 */
    public static final int ORDER_REFUNDING = 7;

    /** 配送任务状态：0 待抢单 */
    public static final int TASK_WAITING = 0;
    /** 配送任务状态：1 已接单 */
    public static final int TASK_GRABBED = 1;
    /** 配送任务状态：2 已取餐 */
    public static final int TASK_PICKED = 2;
    /** 配送任务状态：3 已送达 */
    public static final int TASK_DELIVERED = 3;

    /** 菜品状态：0 下架 */
    public static final int DISH_OFF = 0;
    /** 菜品状态：1 在售 */
    public static final int DISH_ON = 1;
    /** 菜品状态：2 售罄 */
    public static final int DISH_SOLD_OUT = 2;

    /** 审核状态：0 待审核 */
    public static final int AUDIT_PENDING = 0;
    /** 审核状态：1 通过 */
    public static final int AUDIT_PASSED = 1;
    /** 审核状态：2 驳回 */
    public static final int AUDIT_REJECTED = 2;

    /** 店铺营业状态：1 营业 */
    public static final int SHOP_OPEN = 1;
    /** 店铺营业状态：0 打烊 */
    public static final int SHOP_CLOSED = 0;

    /** 用户状态：1 正常 */
    public static final int USER_ENABLED = 1;
    /** 用户状态：0 禁用 */
    public static final int USER_DISABLED = 0;

    /** 系统操作者 ID（订单状态日志） */
    public static final long SYSTEM_OPERATOR_ID = 0L;

    /** 订单未支付超时时间（分钟） */
    public static final int ORDER_PAY_TIMEOUT_MINUTES = 15;

    /** 评价有效期（天） */
    public static final int REVIEW_VALID_DAYS = 7;

    /** 登录失败锁定阈值 */
    public static final int LOGIN_FAIL_LIMIT = 5;
    /** 登录失败锁定时长（分钟） */
    public static final int LOGIN_LOCK_MINUTES = 15;

    private CommonConstants() {
    }
}
