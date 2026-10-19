package com.eatwhat.common.utils;

import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 时间解析工具。
 */
public final class TimeUtils {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private TimeUtils() {
    }

    /**
     * 解析 "HH:mm" 或 "HH:mm:ss" 格式的营业时间，空值返回 null。
     */
    public static LocalTime parseClock(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        try {
            if (value.length() == 5) {
                return LocalTime.parse(value, HH_MM);
            }
            return LocalTime.parse(value, HH_MM_SS);
        } catch (DateTimeParseException e) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "时间格式应为 HH:mm 或 HH:mm:ss");
        }
    }

    /**
     * 商家-08：判断当前时刻是否落在营业时段内。
     * 起止任一为空视为不限制；跨零点（如 22:00-02:00）按两段处理。
     */
    public static boolean inBusinessHours(LocalTime openTime, LocalTime closeTime) {
        if (openTime == null || closeTime == null) {
            return true;
        }
        LocalTime now = LocalTime.now();
        if (openTime.equals(closeTime)) {
            return true;
        }
        if (openTime.isBefore(closeTime)) {
            return !now.isBefore(openTime) && now.isBefore(closeTime);
        }
        // 跨零点营业
        return !now.isBefore(openTime) || now.isBefore(closeTime);
    }
}
