package com.eatwhat.security;

import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.utils.UserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 认证与鉴权拦截器：
 * <ol>
 *   <li>解析 Authorization: Bearer &lt;token&gt;，校验签名与过期时间；</li>
 *   <li>将登录用户写入 {@link UserContext}；</li>
 *   <li>校验 {@link RequireRole} 声明的角色。</li>
 * </ol>
 * 所有 /api 请求默认需要登录，{@link PublicApi} 标注的接口跳过。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthInterceptor(JwtTokenProvider tokenProvider, TokenBlacklistService tokenBlacklistService) {
        this.tokenProvider = tokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        boolean isPublic = AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), PublicApi.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), PublicApi.class);

        String token = resolveToken(request);
        if (token == null) {
            if (isPublic) {
                return true;
            }
            throw BizException.of(ErrorCode.UNAUTHORIZED);
        }

        Claims claims;
        try {
            claims = tokenProvider.parse(token);
        } catch (ExpiredJwtException e) {
            throw BizException.of(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw BizException.of(ErrorCode.TOKEN_INVALID);
        }

        if (tokenProvider.isRefreshToken(claims)) {
            throw BizException.of(ErrorCode.TOKEN_INVALID, "请使用访问令牌访问业务接口");
        }

        // 顾客-05：已退出登录的令牌进入失效列表，此后不再被接受
        if (tokenBlacklistService.isRevoked(claims)) {
            throw BizException.of(ErrorCode.TOKEN_INVALID, "登录状态已失效，请重新登录");
        }

        UserContext.LoginUser loginUser = new UserContext.LoginUser(
                tokenProvider.getUserId(claims),
                tokenProvider.getPhone(claims),
                null,
                tokenProvider.getRole(claims));
        UserContext.set(loginUser);

        RequireRole requireRole = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequireRole.class);
        if (requireRole == null) {
            requireRole = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireRole.class);
        }
        if (requireRole != null && !requireRole.allowAnonymous()) {
            String role = loginUser.getRole();
            boolean allowed = Arrays.asList(requireRole.value()).contains(role);
            if (!allowed) {
                throw BizException.of(ErrorCode.ROLE_NOT_ALLOWED);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
