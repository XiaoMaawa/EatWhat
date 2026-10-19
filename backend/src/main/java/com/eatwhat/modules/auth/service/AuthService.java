package com.eatwhat.modules.auth.service;

import com.eatwhat.modules.auth.dto.LoginRequest;
import com.eatwhat.modules.auth.dto.RegisterRequest;
import com.eatwhat.modules.auth.dto.ResetPasswordRequest;
import com.eatwhat.modules.auth.vo.LoginVO;

/**
 * 认证服务。
 */
public interface AuthService {

    /** 用户注册，返回用户 ID */
    Long register(RegisterRequest request);

    /** 用户登录 */
    LoginVO login(LoginRequest request);

    /** 刷新令牌 */
    LoginVO refresh(String refreshToken);

    /** 退出登录 */
    void logout(Long userId);

    /** 通过手机号 + 图形验证码重置密码 */
    void resetPassword(ResetPasswordRequest request);
}
