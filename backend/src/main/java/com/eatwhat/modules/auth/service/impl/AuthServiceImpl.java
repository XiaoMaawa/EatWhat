package com.eatwhat.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.constant.RoleConstants;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.utils.MaskUtils;
import com.eatwhat.modules.auth.dto.LoginRequest;
import com.eatwhat.modules.auth.dto.RegisterRequest;
import com.eatwhat.modules.auth.dto.ResetPasswordRequest;
import com.eatwhat.modules.auth.entity.SysUser;
import com.eatwhat.modules.auth.mapper.SysUserMapper;
import com.eatwhat.modules.auth.service.AuthService;
import com.eatwhat.modules.auth.service.CaptchaService;
import com.eatwhat.modules.auth.service.LoginAttemptService;
import com.eatwhat.modules.auth.vo.LoginVO;
import com.eatwhat.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务实现。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final LoginAttemptService loginAttemptService;
    private final CaptchaService captchaService;

    public AuthServiceImpl(SysUserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider,
                           LoginAttemptService loginAttemptService,
                           CaptchaService captchaService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.loginAttemptService = loginAttemptService;
        this.captchaService = captchaService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "两次输入的密码不一致");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, request.getPhone()));
        if (exists != null && exists > 0) {
            throw BizException.of(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        SysUser user = new SysUser();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() == null || request.getNickname().isBlank()
                ? "用户" + request.getPhone().substring(7) : request.getNickname());
        user.setRole(request.getRole());
        user.setStatus(CommonConstants.USER_ENABLED);
        userMapper.insert(user);
        log.info("用户注册成功 id={} phone={} role={}", user.getId(), MaskUtils.phone(user.getPhone()), user.getRole());
        return user.getId();
    }

    @Override
    public LoginVO login(LoginRequest request) {
        if (loginAttemptService.isBlocked(request.getPhone())) {
            throw BizException.of(ErrorCode.LOGIN_TOO_FREQUENT);
        }
        if (request.getCaptchaKey() != null && !request.getCaptchaKey().isBlank()) {
            captchaService.validate(request.getCaptchaKey(), request.getCaptchaCode());
        }

        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, request.getPhone())
                .last("LIMIT 1"));
        if (user == null) {
            loginAttemptService.loginFailed(request.getPhone());
            throw BizException.of(ErrorCode.PASSWORD_INCORRECT);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int left = loginAttemptService.loginFailed(request.getPhone());
            throw BizException.of(ErrorCode.PASSWORD_INCORRECT,
                    left > 0 ? "账号或密码错误，还可尝试 " + left + " 次" : "账号或密码错误，账号已锁定 15 分钟");
        }
        if (user.getStatus() != null && user.getStatus() == CommonConstants.USER_DISABLED) {
            throw BizException.of(ErrorCode.ACCOUNT_DISABLED);
        }
        loginAttemptService.loginSuccess(request.getPhone());

        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getLastLoginAt, LocalDateTime.now()));

        return buildLoginVO(user);
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        Claims claims;
        try {
            claims = tokenProvider.parse(refreshToken);
        } catch (ExpiredJwtException e) {
            throw BizException.of(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw BizException.of(ErrorCode.TOKEN_INVALID);
        }
        if (!tokenProvider.isRefreshToken(claims)) {
            throw BizException.of(ErrorCode.TOKEN_INVALID, "刷新令牌无效");
        }
        SysUser user = userMapper.selectById(tokenProvider.getUserId(claims));
        if (user == null) {
            throw BizException.of(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getStatus() != null && user.getStatus() == CommonConstants.USER_DISABLED) {
            throw BizException.of(ErrorCode.ACCOUNT_DISABLED);
        }
        return buildLoginVO(user);
    }

    @Override
    public void logout(Long userId) {
        // 无状态 JWT：由客户端清除令牌；此处仅记录日志，预留黑名单扩展点
        log.info("用户 {} 退出登录", userId);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        captchaService.validate(request.getCaptchaKey(), request.getCaptchaCode());
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "两次输入的密码不一致");
        }

        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, request.getPhone())
                .last("LIMIT 1"));
        if (user == null) {
            throw BizException.of(ErrorCode.USER_NOT_FOUND, "该手机号尚未注册");
        }
        if (user.getStatus() != null && user.getStatus() == CommonConstants.USER_DISABLED) {
            throw BizException.of(ErrorCode.ACCOUNT_DISABLED);
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "新密码不能与原密码相同");
        }

        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getPassword, passwordEncoder.encode(request.getNewPassword())));
        // 重置成功后清除登录失败计数，避免用户继续被锁定
        loginAttemptService.loginSuccess(request.getPhone());
        log.info("用户 {} 通过验证码重置密码成功", user.getId());
    }

    private LoginVO buildLoginVO(SysUser user) {
        LoginVO vo = new LoginVO();
        String role = user.getRole() == null ? RoleConstants.CONSUMER : user.getRole();
        vo.setToken(tokenProvider.createAccessToken(user.getId(), user.getPhone(), role));
        vo.setRefreshToken(tokenProvider.createRefreshToken(user.getId(), user.getPhone(), role));
        vo.setExpiresIn(tokenProvider.getExpirationMs() / 1000);

        LoginVO.UserInfo info = new LoginVO.UserInfo();
        info.setId(user.getId());
        info.setPhone(MaskUtils.phone(user.getPhone()));
        info.setNickname(user.getNickname());
        info.setAvatar(user.getAvatar());
        info.setRole(role);
        vo.setUserInfo(info);
        return vo;
    }
}
