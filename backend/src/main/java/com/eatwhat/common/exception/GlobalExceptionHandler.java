package com.eatwhat.common.exception;

import com.eatwhat.common.result.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 *
 * <p>HTTP 状态码遵循 {@code docs/05-接口设计规范.md} §1.4：认证失败 401、无权限 403、
 * 路径不存在 404、方法不支持 405、请求体/参数无法解析 400、服务端内部错误 500；
 * 业务规则失败（库存不足、状态非法等）仍返回 HTTP 200，由 {@code code} 字段区分。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：认证与权限类按 HTTP 语义返回 401/403，其余业务规则失败仍为 200。 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBiz(BizException e, HttpServletRequest request) {
        log.warn("业务异常 [{}] {} -> {}", e.getCode(), request.getRequestURI(), e.getMessage());
        R<Void> body = R.fail(e.getCode(), e.getMessage());
        return ResponseEntity.status(httpStatusOf(e.getCode())).body(body);
    }

    private HttpStatus httpStatusOf(Integer code) {
        if (code == null) {
            return HttpStatus.OK;
        }
        return switch (code) {
            case 1004, 1005, 2000 -> HttpStatus.UNAUTHORIZED;
            case 2001, 2002, 2003 -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.OK;
        };
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return ResponseEntity.badRequest()
                .body(R.fail(ErrorCode.PARAM_INVALID, msg.isEmpty() ? ErrorCode.PARAM_INVALID.getMessage() : msg));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return ResponseEntity.badRequest()
                .body(R.fail(ErrorCode.PARAM_INVALID, msg.isEmpty() ? ErrorCode.PARAM_INVALID.getMessage() : msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        return ResponseEntity.badRequest()
                .body(R.fail(ErrorCode.PARAM_INVALID, msg.isEmpty() ? ErrorCode.PARAM_INVALID.getMessage() : msg));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest()
                .body(R.fail(ErrorCode.PARAM_MISSING, "缺少必要参数：" + e.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest()
                .body(R.fail(ErrorCode.PARAM_TYPE_ERROR, "参数格式不正确：" + e.getName()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(R.fail(ErrorCode.PARAM_INVALID, "请求体格式不正确，无法解析"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethod(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(R.fail(ErrorCode.PARAM_INVALID, "不支持的请求方法：" + e.getMethod()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<R<Void>> handleNotFound(NoHandlerFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(R.fail(4004, "请求路径不存在：" + e.getRequestURL()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<R<Void>> handleDuplicate(DuplicateKeyException e) {
        log.warn("唯一约束冲突：{}", e.getMessage());
        return ResponseEntity.ok(R.fail(ErrorCode.DATA_CONFLICT, "数据已存在，请勿重复提交"));
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<R<Void>> handleUploadSize(Exception e) {
        return ResponseEntity.ok(R.fail(ErrorCode.FILE_SIZE_EXCEEDED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleOthers(Exception e, HttpServletRequest request) {
        log.error("未捕获异常 {} {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(R.fail(ErrorCode.SYSTEM_ERROR));
    }
}
