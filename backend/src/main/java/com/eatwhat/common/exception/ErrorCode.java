package com.eatwhat.common.exception;

/**
 * 平台错误码。区间：0 成功；1xxx 认证；2xxx 权限；3xxx 参数；4xxx 业务规则；5xxx 系统与外部依赖。
 *
 * <p>1001-1006、2001-2003、3001-3003、4001-4016、5001-5004 的码值与文案严格对齐
 * {@code docs/03-详细设计说明书.md} §2.3；4000 段 4017 之后为文档未覆盖的业务扩展码。</p>
 */
public enum ErrorCode {

    SUCCESS(0, "success"),

    // ---------- 认证与账号 1xxx ----------
    PHONE_ALREADY_EXISTS(1001, "该手机号已注册"),
    PASSWORD_INCORRECT(1002, "账号或密码错误"),
    ACCOUNT_DISABLED(1003, "账号已被禁用"),
    TOKEN_INVALID(1004, "登录凭证无效，请重新登录"),
    TOKEN_EXPIRED(1005, "登录已过期，请重新登录"),
    LOGIN_TOO_FREQUENT(1006, "登录失败次数过多，请 15 分钟后再试"),
    /** 扩展：图形验证码校验失败 */
    CAPTCHA_INVALID(1007, "验证码错误或已失效"),

    // ---------- 权限 2xxx ----------
    /** 扩展：未携带或无法识别身份凭证（HTTP 401） */
    UNAUTHORIZED(2000, "未登录或登录已过期"),
    ACCESS_DENIED(2001, "无访问权限"),
    ROLE_NOT_ALLOWED(2002, "当前角色无权执行该操作"),
    DATA_NOT_OWNED(2003, "无权操作他人数据"),

    // ---------- 参数 3xxx ----------
    PARAM_MISSING(3001, "必填参数缺失"),
    PARAM_INVALID(3002, "参数格式不正确"),
    PARAM_TYPE_ERROR(3003, "参数类型错误"),

    // ---------- 业务 4xxx ----------
    DISH_NOT_FOUND(4001, "菜品不存在"),
    DISH_OFF_SALE(4002, "菜品已下架"),
    STOCK_NOT_ENOUGH(4003, "库存不足"),
    SHOP_CLOSED(4004, "店铺已打烊"),
    BELOW_MIN_AMOUNT(4005, "未达到起送金额"),
    ORDER_NOT_FOUND(4006, "订单不存在"),
    ORDER_STATUS_ILLEGAL(4007, "当前订单状态不允许该操作"),
    ORDER_STATUS_CONFLICT(4008, "订单状态已变更，请刷新后重试"),
    CART_EMPTY(4009, "购物车为空"),
    CART_CROSS_SHOP(4010, "购物车中存在其他店铺的菜品"),
    TASK_ALREADY_TAKEN(4011, "该订单已被其他骑手接单"),
    REVIEW_ALREADY_EXISTS(4012, "该订单已评价"),
    REVIEW_TIMEOUT(4013, "评价时间已过期"),
    MERCHANT_NOT_APPROVED(4014, "商家资质未通过审核"),
    RIDER_NOT_APPROVED(4015, "骑手认证未通过审核"),
    OUT_OF_DELIVERY_RANGE(4016, "收货地址超出配送范围"),

    // ---------- 业务扩展 4017+ ----------
    USER_NOT_FOUND(4017, "用户不存在"),
    SHOP_NOT_FOUND(4018, "店铺不存在"),
    ADDRESS_NOT_FOUND(4019, "收货地址不存在"),
    REVIEW_NOT_ALLOWED(4020, "该订单当前不可评价"),
    REVIEW_ALREADY_REPLIED(4021, "该评价已回复，不支持重复回复"),
    CATEGORY_NOT_FOUND(4022, "菜品分类不存在"),
    CATEGORY_NOT_EMPTY(4023, "分类下存在菜品，不允许删除"),
    MERCHANT_NOT_FOUND(4024, "商家信息不存在"),
    RIDER_NOT_FOUND(4025, "骑手信息不存在"),
    DELIVERY_NOT_FOUND(4026, "配送任务不存在"),
    ORDER_ALREADY_PAID(4027, "订单已支付"),
    AUDIT_ALREADY_DONE(4028, "该申请已审核完成"),
    DATA_CONFLICT(4029, "数据已存在，请勿重复提交"),

    // ---------- 系统 5xxx ----------
    SYSTEM_ERROR(5001, "系统繁忙，请稍后重试"),
    UPLOAD_FAILED(5002, "文件上传失败"),
    FILE_TYPE_NOT_ALLOWED(5003, "不支持的文件类型"),
    FILE_SIZE_EXCEEDED(5004, "文件大小超出限制"),
    /** 扩展：数据库访问异常 */
    DATA_ACCESS_ERROR(5005, "数据访问异常"),
    /** 扩展：第三方服务调用失败 */
    THIRD_PARTY_ERROR(5006, "第三方服务调用失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
