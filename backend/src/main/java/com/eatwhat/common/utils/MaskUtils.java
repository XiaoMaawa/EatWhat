package com.eatwhat.common.utils;

import java.util.regex.Pattern;

/**
 * 数据脱敏工具。
 */
public final class MaskUtils {

    private static final Pattern PHONE = Pattern.compile("^(\\d{3})\\d{4}(\\d{4})$");
    private static final Pattern ID_CARD = Pattern.compile("^(\\d{6})\\d{8}(\\d{3}[0-9Xx])$");

    private MaskUtils() {
    }

    /** 手机号脱敏：138****0001 */
    public static String phone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 身份证脱敏 */
    public static String idCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }
}
