package com.eatwhat.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 地理距离与时长的通用计算工具。
 */
public final class GeoUtils {

    /** 地球半径（公里） */
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private GeoUtils() {
    }

    /**
     * 计算两点之间的球面距离（公里），任一点坐标缺失时返回 null。
     * 使用 Haversine 公式。
     */
    public static BigDecimal distanceKm(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return null;
        }
        double radLat1 = Math.toRadians(lat1.doubleValue());
        double radLat2 = Math.toRadians(lat2.doubleValue());
        double deltaLat = radLat2 - radLat1;
        double deltaLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(EARTH_RADIUS_KM * c).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 将米转换为公里，保留两位小数。
     */
    public static BigDecimal metersToKm(Integer meters) {
        if (meters == null) {
            return null;
        }
        return BigDecimal.valueOf(meters).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
    }
}
