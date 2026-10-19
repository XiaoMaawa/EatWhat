package com.eatwhat.common.utils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 业务编号生成器：订单号、交易号。
 */
public final class BizNoGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter ORDER_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private BizNoGenerator() {
    }

    /** 订单号：EW + yyyyMMddHHmmss + 6 位随机数，共 22 位 */
    public static String orderNo() {
        return "EW" + LocalDateTime.now().format(ORDER_FMT) + String.format("%06d", RANDOM.nextInt(1000000));
    }

    /** 模拟交易号：MOCK + 时间戳 + 6 位随机数 */
    public static String tradeNo() {
        return "MOCK" + System.currentTimeMillis() + String.format("%06d", RANDOM.nextInt(1000000));
    }

    /** 追踪号：16 位十六进制 */
    public static String traceId() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(16);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
