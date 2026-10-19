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
}
