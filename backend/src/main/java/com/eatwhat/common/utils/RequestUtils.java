package com.eatwhat.common.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 请求工具类。
 */
public final class RequestUtils {

    private RequestUtils() {
    }

    public static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                int idx = value.indexOf(',');
                return idx > 0 ? value.substring(0, idx).trim() : value.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
