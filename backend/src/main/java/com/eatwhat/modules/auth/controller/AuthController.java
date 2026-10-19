package com.eatwhat.modules.auth.controller;

import com.eatwhat.common.result.R;
import com.eatwhat.common.utils.UserContext;
import com.eatwhat.modules.auth.dto.LoginRequest;
import com.eatwhat.modules.auth.dto.RefreshTokenRequest;
import com.eatwhat.modules.auth.dto.RegisterRequest;
import com.eatwhat.modules.auth.dto.ResetPasswordRequest;
import com.eatwhat.modules.auth.service.AuthService;
import com.eatwhat.modules.auth.vo.LoginVO;
import com.eatwhat.security.PublicApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口 /auth。
 */
@Tag(name = "认证模块")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PublicApi
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        Long userId = authService.register(request);
        return R.ok(Map.of("userId", userId));
    }

    @PublicApi
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @PublicApi
    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public R<LoginVO> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return R.ok(authService.refresh(request.getRefreshToken()));
    }

    @PublicApi
    @Operation(summary = "通过手机号与验证码重置密码")
    @PostMapping("/reset-password")
    public R<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return R.ok();
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request,
                          @RequestBody(required = false) Map<String, String> body) {
        String header = request.getHeader("Authorization");
        String accessToken = header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : null;
        String refreshToken = body == null ? null : body.get("refreshToken");
        authService.logout(accessToken, refreshToken);
        return R.ok();
    }
}
