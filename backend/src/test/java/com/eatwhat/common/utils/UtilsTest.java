package com.eatwhat.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通用工具类单元测试：脱敏、距离计算、编号生成。
 */
@DisplayName("通用工具")
class UtilsTest {

    @Test
    @DisplayName("手机号脱敏保留前三后四")
    void maskPhone() {
        assertThat(MaskUtils.phone("13800000001")).isEqualTo("138****0001");
        assertThat(MaskUtils.phone("13800000000")).isEqualTo("138****0000");
        assertThat(MaskUtils.phone("1234567")).isEqualTo("123****4567");
    }

    @Test
    @DisplayName("手机号为空或过短时原样返回")
    void maskPhoneEdgeCase() {
        assertThat(MaskUtils.phone(null)).isNull();
        assertThat(MaskUtils.phone("123456")).isEqualTo("123456");
    }

    @Test
    @DisplayName("身份证脱敏保留前四后四")
    void maskIdCard() {
        assertThat(MaskUtils.idCard("330106199303031234")).isEqualTo("3301**********1234");
        assertThat(MaskUtils.idCard(null)).isNull();
        assertThat(MaskUtils.idCard("123")).isEqualTo("123");
    }

    @Test
    @DisplayName("同一点距离为零，杭州到上海约 160 公里")
    void distanceKm() {
        BigDecimal zero = GeoUtils.distanceKm(
                new BigDecimal("30.2770"), new BigDecimal("120.1250"),
                new BigDecimal("30.2770"), new BigDecimal("120.1250"));
        assertThat(zero).isEqualByComparingTo("0.00");

        BigDecimal hangzhouToShanghai = GeoUtils.distanceKm(
                new BigDecimal("30.2741"), new BigDecimal("120.1551"),
                new BigDecimal("31.2304"), new BigDecimal("121.4737"));
        assertThat(hangzhouToShanghai.doubleValue()).isBetween(150.0, 180.0);
    }

    @Test
    @DisplayName("坐标缺失时距离返回 null")
    void distanceWithNull() {
        assertThat(GeoUtils.distanceKm(null, new BigDecimal("120"), new BigDecimal("30"), new BigDecimal("120")))
                .isNull();
        assertThat(GeoUtils.distanceKm(new BigDecimal("30"), new BigDecimal("120"), null, null)).isNull();
    }

    @Test
    @DisplayName("米转公里保留两位小数")
    void metersToKm() {
        assertThat(GeoUtils.metersToKm(3000)).isEqualByComparingTo("3.00");
        assertThat(GeoUtils.metersToKm(1500)).isEqualByComparingTo("1.50");
        assertThat(GeoUtils.metersToKm(null)).isNull();
    }

    @Test
    @DisplayName("订单号前缀 EW 且长度固定、不重复")
    void orderNo() {
        String a = BizNoGenerator.orderNo();
        String b = BizNoGenerator.orderNo();
        assertThat(a).startsWith("EW").hasSize(22);
        assertThat(a).isNotEqualTo(b);
        assertThat(a.substring(2)).containsPattern("\\d{20}");
    }

    @Test
    @DisplayName("交易号前缀 MOCK，traceId 为 16 位十六进制")
    void tradeNoAndTraceId() {
        assertThat(BizNoGenerator.tradeNo()).startsWith("MOCK");
        assertThat(BizNoGenerator.traceId()).hasSize(16).matches("[0-9a-f]{16}");
    }
}
