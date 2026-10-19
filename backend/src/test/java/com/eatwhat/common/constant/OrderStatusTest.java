package com.eatwhat.common.constant;

import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 订单状态机单元测试。
 * 逐条校验 docs/05 §6.6 状态迁移权限矩阵，以及非法迁移的拦截行为。
 */
@DisplayName("订单状态机")
class OrderStatusTest {

    @Test
    @DisplayName("全部状态码与描述符合约定")
    void allStatusCodes() {
        assertThat(OrderStatus.of(0)).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(OrderStatus.of(1)).isEqualTo(OrderStatus.PENDING_ACCEPT);
        assertThat(OrderStatus.of(2)).isEqualTo(OrderStatus.PREPARING);
        assertThat(OrderStatus.of(3)).isEqualTo(OrderStatus.PENDING_DELIVERY);
        assertThat(OrderStatus.of(4)).isEqualTo(OrderStatus.DELIVERING);
        assertThat(OrderStatus.of(5)).isEqualTo(OrderStatus.COMPLETED);
        assertThat(OrderStatus.of(6)).isEqualTo(OrderStatus.CANCELLED);
        assertThat(OrderStatus.of(7)).isEqualTo(OrderStatus.REFUNDING);
        assertThat(OrderStatus.descOf(0)).isEqualTo("待支付");
        assertThat(OrderStatus.descOf(5)).isEqualTo("已完成");
        assertThat(OrderStatus.descOf(null)).isNull();
        assertThat(OrderStatus.descOf(99)).isEqualTo("未知状态");
    }

    @Test
    @DisplayName("未知状态码抛出订单状态非法")
    void unknownStatusCode() {
        assertThatThrownBy(() -> OrderStatus.of(99))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未知的订单状态");
    }

    @Test
    @DisplayName("已完成与已取消属于终态")
    void finalStatus() {
        assertThat(OrderStatus.COMPLETED.isFinal()).isTrue();
        assertThat(OrderStatus.CANCELLED.isFinal()).isTrue();
        assertThat(OrderStatus.DELIVERING.isFinal()).isFalse();
        assertThat(OrderStatus.PENDING_PAYMENT.isFinal()).isFalse();
    }

    @ParameterizedTest(name = "合法迁移 {0} -> {1} 由 {2} 发起")
    @CsvSource({
            "0, 1, SYSTEM",
            "0, 1, CONSUMER",
            "1, 2, MERCHANT",
            "2, 3, MERCHANT",
            "3, 4, RIDER",
            "4, 5, RIDER",
            "4, 5, CONSUMER",
            "4, 5, SYSTEM",
            "3, 5, SYSTEM",
            "0, 6, CONSUMER",
            "1, 6, CONSUMER",
            "0, 6, SYSTEM",
            "2, 6, SYSTEM",
            "4, 6, SYSTEM"
    })
    @DisplayName("合法状态迁移放行")
    void legalTransitions(int from, int to, String role) {
        assertThatCode(() -> OrderStatus.assertTransition(from, to, role)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "非法迁移 {0} -> {1} 由 {2} 发起")
    @CsvSource({
            // 支付回调只能由系统或消费者触发
            "0, 1, MERCHANT",
            "0, 1, RIDER",
            // 备餐只能商家发起
            "1, 2, CONSUMER",
            "1, 2, RIDER",
            "1, 2, SYSTEM",
            // 跨状态跳跃
            "1, 3, MERCHANT",
            "2, 4, MERCHANT",
            // 骑手只能在待配送之后接单
            "2, 4, RIDER",
            "1, 4, RIDER",
            // 未送达不能完成
            "3, 5, RIDER",
            "2, 5, RIDER",
            "1, 5, MERCHANT",
            // 消费者只能取消待支付 / 待接单
            "2, 6, CONSUMER",
            "3, 6, CONSUMER",
            "4, 6, CONSUMER",
            // 商家无权取消
            "1, 6, MERCHANT"
    })
    @DisplayName("非法状态迁移被拦截")
    void illegalTransitions(int from, int to, String role) {
        assertThatThrownBy(() -> OrderStatus.assertTransition(from, to, role))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.ORDER_STATUS_ILLEGAL.getCode()));
    }

    @Test
    @DisplayName("状态原地踏步视为非法")
    void sameStatusRejected() {
        assertThatThrownBy(() -> OrderStatus.assertTransition(1, 1, "MERCHANT"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("订单已是目标状态");
    }

    @Test
    @DisplayName("终态不可再变更")
    void finalStatusLocked() {
        assertThatThrownBy(() -> OrderStatus.assertTransition(5, 6, "SYSTEM"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("订单已结束");
        assertThatThrownBy(() -> OrderStatus.assertTransition(6, 1, "SYSTEM"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("订单已结束");
    }

    @Test
    @DisplayName("消费者可取消的状态为待支付与待接单")
    void consumerCancelable() {
        assertThat(OrderStatus.consumerCancelableStatuses()).containsExactly(0, 1);
    }
}
