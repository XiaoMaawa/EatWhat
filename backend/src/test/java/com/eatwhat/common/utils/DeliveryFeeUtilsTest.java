package com.eatwhat.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 配送费计算单元测试。
 */
@DisplayName("配送费计算")
class DeliveryFeeUtilsTest {

    @Test
    @DisplayName("固定计费时无论多远都返回基础配送费")
    void fixedTypeIgnoresDistance() {
        BigDecimal fee = DeliveryFeeUtils.calculate(new BigDecimal("3.00"),
                DeliveryFeeUtils.TYPE_FIXED, new BigDecimal("20.00"));
        assertThat(fee).isEqualByComparingTo("3.00");
    }

    @Test
    @DisplayName("计费方式为空时按固定配送费处理")
    void nullTypeFallsBackToFixed() {
        BigDecimal fee = DeliveryFeeUtils.calculate(new BigDecimal("2.50"), null, new BigDecimal("8.00"));
        assertThat(fee).isEqualByComparingTo("2.50");
    }

    @Test
    @DisplayName("距离为空时按固定配送费处理")
    void nullDistanceFallsBackToFixed() {
        BigDecimal fee = DeliveryFeeUtils.calculate(new BigDecimal("2.50"),
                DeliveryFeeUtils.TYPE_BY_DISTANCE, null);
        assertThat(fee).isEqualByComparingTo("2.50");
    }

    @Test
    @DisplayName("阶梯计价：3 公里内不加价")
    void withinBaseDistance() {
        assertThat(DeliveryFeeUtils.calculate(new BigDecimal("3.00"),
                DeliveryFeeUtils.TYPE_BY_DISTANCE, new BigDecimal("3.00")))
                .isEqualByComparingTo("3.00");
        assertThat(DeliveryFeeUtils.calculate(new BigDecimal("3.00"),
                DeliveryFeeUtils.TYPE_BY_DISTANCE, new BigDecimal("1.20")))
                .isEqualByComparingTo("3.00");
    }

    @Test
    @DisplayName("阶梯计价：超出部分按公里向上取整加价")
    void beyondBaseDistance() {
        // 3.1 公里 → 超出 0.1 公里，向上取整为 1 公里，加收 1 元
        assertThat(DeliveryFeeUtils.calculate(new BigDecimal("3.00"),
                DeliveryFeeUtils.TYPE_BY_DISTANCE, new BigDecimal("3.10")))
                .isEqualByComparingTo("4.00");
        // 4.0 公里 → 超出 1 公里，加收 1 元
        assertThat(DeliveryFeeUtils.calculate(new BigDecimal("3.00"),
                DeliveryFeeUtils.TYPE_BY_DISTANCE, new BigDecimal("4.00")))
                .isEqualByComparingTo("4.00");
        // 5.5 公里 → 超出 2.5 公里，向上取整为 3 公里，加收 3 元
        assertThat(DeliveryFeeUtils.calculate(new BigDecimal("3.00"),
                DeliveryFeeUtils.TYPE_BY_DISTANCE, new BigDecimal("5.50")))
                .isEqualByComparingTo("6.00");
    }

    @Test
    @DisplayName("基础配送费为空时按 0 处理")
    void nullBaseFee() {
        assertThat(DeliveryFeeUtils.calculate(null, DeliveryFeeUtils.TYPE_FIXED, null))
                .isEqualByComparingTo("0.00");
        assertThat(DeliveryFeeUtils.calculate(null, DeliveryFeeUtils.TYPE_BY_DISTANCE,
                new BigDecimal("6.00")))
                .isEqualByComparingTo("3.00");
    }

    @Test
    @DisplayName("计费方式合法性判断")
    void validTypeCheck() {
        assertThat(DeliveryFeeUtils.isValidType(0)).isTrue();
        assertThat(DeliveryFeeUtils.isValidType(1)).isTrue();
        assertThat(DeliveryFeeUtils.isValidType(2)).isFalse();
        assertThat(DeliveryFeeUtils.isValidType(null)).isFalse();
    }
}
