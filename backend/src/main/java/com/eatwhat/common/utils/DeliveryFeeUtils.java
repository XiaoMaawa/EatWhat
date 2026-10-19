package com.eatwhat.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 配送费计算工具。
 *
 * <p>支持两种计费方式：
 * <ul>
 *   <li>固定配送费：无论距离远近都收取店铺设置的配送费；</li>
 *   <li>按距离阶梯计价：3 公里内收取基础配送费，每超出 1 公里加收 1 元。</li>
 * </ul>
 */
public final class DeliveryFeeUtils {

    /** 固定配送费 */
    public static final int TYPE_FIXED = 0;

    /** 按距离阶梯计价 */
    public static final int TYPE_BY_DISTANCE = 1;

    /** 阶梯计价的免费半径（公里） */
    private static final BigDecimal BASE_KM = BigDecimal.valueOf(3);

    /** 每超出 1 公里加收的金额 */
    private static final BigDecimal EXTRA_FEE_PER_KM = BigDecimal.ONE;

    private DeliveryFeeUtils() {
    }

    /**
     * 计算配送费。
     *
     * @param baseFee  店铺配置的配送费，阶梯计价时作为 3 公里内的起步价
     * @param feeType  计费方式，0 固定 / 1 按距离阶梯
     * @param distance 配送距离（公里），为 null 时按固定配送费处理
     * @return 配送费，保留两位小数，永不为 null
     */
    public static BigDecimal calculate(BigDecimal baseFee, Integer feeType, BigDecimal distance) {
        BigDecimal fee = baseFee == null ? BigDecimal.ZERO : baseFee;
        if (!Integer.valueOf(TYPE_BY_DISTANCE).equals(feeType) || distance == null) {
            return fee.setScale(2, RoundingMode.HALF_UP);
        }
        if (distance.compareTo(BASE_KM) <= 0) {
            return fee.setScale(2, RoundingMode.HALF_UP);
        }
        // 超出的公里数向上取整，例如 3.1 公里按 1 公里加价
        BigDecimal extraKm = distance.subtract(BASE_KM).setScale(0, RoundingMode.CEILING);
        return fee.add(extraKm.multiply(EXTRA_FEE_PER_KM)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 判断计费方式是否合法。
     */
    public static boolean isValidType(Integer feeType) {
        return feeType != null && (feeType == TYPE_FIXED || feeType == TYPE_BY_DISTANCE);
    }
}
