package com.eatwhat.common.result;

import com.eatwhat.common.exception.ErrorCode;
import org.slf4j.MDC;

import java.io.Serializable;

/**
 * 统一响应结构。
 *
 * <pre>
 * { "code": 0, "message": "success", "data": {}, "timestamp": 1728979200000, "traceId": "a1b2c3d4e5f6" }
 * </pre>
 */
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    private String traceId;

    public R() {
        this.timestamp = System.currentTimeMillis();
        this.traceId = MDC.get("traceId");
    }

    public R(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.traceId = MDC.get("traceId");
    }

    public static <T> R<T> ok() {
        return new R<>(ErrorCode.SUCCESS.getCode(), "success", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ErrorCode.SUCCESS.getCode(), "success", data);
    }

    public static <T> R<T> ok(T data, String message) {
        return new R<>(ErrorCode.SUCCESS.getCode(), message, data);
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> R<T> fail(ErrorCode errorCode, String message) {
        return new R<>(errorCode.getCode(), message, null);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    public boolean isSuccess() {
        return code != null && code == 0;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
