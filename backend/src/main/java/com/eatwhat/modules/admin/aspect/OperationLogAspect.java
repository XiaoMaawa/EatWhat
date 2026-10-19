package com.eatwhat.modules.admin.aspect;

import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.utils.RequestUtils;
import com.eatwhat.common.utils.UserContext;
import com.eatwhat.modules.admin.entity.OperationLog;
import com.eatwhat.modules.admin.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 管理端写操作审计切面。
 * <p>拦截 {@code com.eatwhat.modules.admin.controller} 下所有 PATCH / POST 接口，
 * 记录操作人、模块、动作、目标、IP、耗时与结果，落库到 operation_log。</p>
 * <p>审计只做旁路记录：无论写入是否成功都不会影响主流程返回值。</p>
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogService operationLogService;

    public OperationLogAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Around("execution(* com.eatwhat.modules.admin.controller..*.*(..))")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        HttpServletRequest request = currentRequest();
        // 只审计写操作，GET 查询不入库，避免日志表被读请求淹没
        if (request == null || "GET".equalsIgnoreCase(request.getMethod())) {
            return point.proceed();
        }

        long start = System.currentTimeMillis();
        OperationLog entry = new OperationLog();
        entry.setOperatorId(UserContext.userId());
        entry.setOperatorRole(UserContext.role());
        entry.setModule("ADMIN");
        entry.setAction(point.getSignature().getName());
        entry.setRequestMethod(request.getMethod());
        entry.setRequestUri(request.getRequestURI());
        entry.setRequestParams(describeArgs(point.getArgs()));
        entry.setIp(RequestUtils.clientIp(request));

        try {
            Object result = point.proceed();
            entry.setResult(1);
            return result;
        } catch (BizException e) {
            entry.setResult(0);
            entry.setErrorMsg("[" + e.getCode() + "] " + e.getMessage());
            throw e;
        } catch (Throwable e) {
            entry.setResult(0);
            entry.setErrorMsg(e.getClass().getSimpleName() + ": " + e.getMessage());
            throw e;
        } finally {
            entry.setCostMs(System.currentTimeMillis() - start);
            operationLogService.record(entry);
            if (log.isDebugEnabled()) {
                log.debug("操作审计 {} {} 结果={} 耗时={}ms",
                        entry.getRequestMethod(), entry.getRequestUri(), entry.getResult(), entry.getCostMs());
            }
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    /**
     * 把方法入参拼成可读字符串；文件入参只记录文件名，避免把二进制写进日志。
     */
    private String describeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        return Arrays.stream(args)
                .map(this::describeArg)
                .collect(Collectors.joining(", "));
    }

    private String describeArg(Object arg) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof MultipartFile file) {
            return "file:" + file.getOriginalFilename() + "(" + file.getSize() + "B)";
        }
        String text = arg.toString();
        return text.length() > 200 ? text.substring(0, 200) + "…" : text;
    }
}
