package com.eatwhat.common.exception;

import com.eatwhat.common.result.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理：所有异常转换为统一响应结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e, HttpServletRequest request) {
        log.warn("业务异常 [{}] {} -> {}", e.getCode(), request.getRequestURI(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return R.fail(ErrorCode.PARAM_INVALID, msg.isEmpty() ? ErrorCode.PARAM_INVALID.getMessage() : msg);
    }

    @ExceptionHandler(BindException.class)
    public R<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return R.fail(ErrorCode.PARAM_INVALID, msg.isEmpty() ? ErrorCode.PARAM_INVALID.getMessage() : msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        return R.fail(ErrorCode.PARAM_INVALID, msg.isEmpty() ? ErrorCode.PARAM_INVALID.getMessage() : msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return R.fail(ErrorCode.PARAM_MISSING, "缺少必要参数：" + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return R.fail(ErrorCode.PARAM_TYPE_ERROR, "参数格式不正确：" + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return R.fail(ErrorCode.PARAM_INVALID, "请求体格式不正确，无法解析");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleMethod(HttpRequestMethodNotSupportedException e) {
        return R.fail(ErrorCode.PARAM_INVALID, "不支持的请求方法：" + e.getMethod());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public R<Void> handleNotFound(NoHandlerFoundException e) {
        return R.fail(4004, "请求路径不存在：" + e.getRequestURL());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public R<Void> handleDuplicate(DuplicateKeyException e) {
        log.warn("唯一约束冲突：{}", e.getMessage());
        return R.fail(ErrorCode.DATA_CONFLICT, "数据已存在，请勿重复提交");
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public R<Void> handleUploadSize(Exception e) {
        return R.fail(ErrorCode.FILE_SIZE_EXCEEDED);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleOthers(Exception e, HttpServletRequest request) {
        log.error("未捕获异常 {} {}", request.getMethod(), request.getRequestURI(), e);
        return R.fail(ErrorCode.SYSTEM_ERROR);
    }
}
