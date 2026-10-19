package com.eatwhat.common.constant;

import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;

import java.util.Arrays;
import java.util.List;

/**
 * 订单状态机：状态码、中文描述、合法迁移表与操作者角色约束。
 */
public enum OrderStatus {

    PENDING_PAYMENT(0, "待支付"),
    PENDING_ACCEPT(1, "待接单"),
    PREPARING(2, "备餐中"),
    PENDING_DELIVERY(3, "待配送"),
    DELIVERING(4, "配送中"),
    COMPLETED(5, "已完成"),
    CANCELLED(6, "已取消"),
    REFUNDING(7, "退款中");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static OrderStatus of(int code) {
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElseThrow(() -> BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "未知的订单状态：" + code));
    }

    public static String descOf(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .map(OrderStatus::getDesc)
                .findFirst()
                .orElse("未知状态");
    }

    /** 是否为终态（不可再次变更） */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * 校验状态迁移是否合法。规则见 docs/05 §6.6 状态迁移权限矩阵。
     */
    public static void assertTransition(int from, int to, String role) {
        if (from == to) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "订单已是目标状态");
        }
        OrderStatus f = of(from);
        OrderStatus t = of(to);
        if (f.isFinal()) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "订单已结束，无法变更状态");
        }
        boolean legal;
        switch (t) {
            case PENDING_ACCEPT:
                // 0 -> 1 由支付回调（系统）触发
                legal = f == PENDING_PAYMENT && ("SYSTEM".equals(role) || "CONSUMER".equals(role));
                break;
            case PREPARING:
                legal = f == PENDING_ACCEPT && "MERCHANT".equals(role);
                break;
            case PENDING_DELIVERY:
                legal = f == PREPARING && "MERCHANT".equals(role);
                break;
            case DELIVERING:
                legal = f == PENDING_DELIVERY && "RIDER".equals(role);
                break;
            case COMPLETED:
                // 系统（平台介入 / 定时结算）可从待配送或配送中强制完结
                legal = f == DELIVERING
                        && ("RIDER".equals(role) || "CONSUMER".equals(role) || "SYSTEM".equals(role))
                        || ("SYSTEM".equals(role) && f == PENDING_DELIVERY);
                break;
            case CANCELLED:
                // 系统（平台介入强制退款）可终止任意未完结订单
                legal = "SYSTEM".equals(role)
                        || ((f == PENDING_PAYMENT || f == PENDING_ACCEPT) && "CONSUMER".equals(role));
                break;
            case REFUNDING:
                legal = (f == PENDING_ACCEPT || f == DELIVERING || f == PENDING_DELIVERY)
                        && ("MERCHANT".equals(role) || "CONSUMER".equals(role));
                break;
            default:
                legal = false;
        }
        if (!legal) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL,
                    "订单状态不能从【" + f.desc + "】变更为【" + t.desc + "】");
        }
    }

    /** 可被消费者取消的中间态 */
    public static List<Integer> consumerCancelableStatuses() {
        return Arrays.asList(PENDING_PAYMENT.code, PENDING_ACCEPT.code);
    }
}
