package com.eatwhat.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.modules.auth.dto.LoginRequest;
import com.eatwhat.modules.auth.dto.RegisterRequest;
import com.eatwhat.modules.auth.entity.SysUser;
import com.eatwhat.modules.auth.mapper.SysUserMapper;
import com.eatwhat.modules.auth.service.CaptchaService;
import com.eatwhat.modules.auth.service.LoginAttemptService;
import com.eatwhat.modules.auth.vo.LoginVO;
import com.eatwhat.security.JwtTokenProvider;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 认证服务单元测试。
 * 覆盖注册校验、登录密码校验、账号禁用、登录失败锁定与刷新令牌。
 */
@DisplayName("认证服务")
class AuthServiceImplTest {

    private static final String SECRET = "eatwhat-unit-test-secret-key-0123456789abcdef";

    private SysUserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider tokenProvider;
    private LoginAttemptService loginAttemptService;
    private CaptchaService captchaService;
    private AuthServiceImpl service;

    /**
     * MyBatis-Plus 的 LambdaQueryWrapper 依赖实体元数据缓存，
     * 纯单元测试（无 Spring 容器）下需要手动初始化一次。
     */
    @BeforeAll
    static void initMybatisPlusMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.eatwhat.modules.auth.mapper.SysUserMapper");
        TableInfoHelper.initTableInfo(assistant, SysUser.class);
    }

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        passwordEncoder = new BCryptPasswordEncoder(4);
        tokenProvider = new JwtTokenProvider(SECRET, 7_200_000L, 604_800_000L);
        loginAttemptService = new LoginAttemptService();
        captchaService = mock(CaptchaService.class);
        service = new AuthServiceImpl(userMapper, passwordEncoder, tokenProvider,
                loginAttemptService, captchaService);
    }

    private SysUser existingUser(String rawPassword) {
        SysUser user = new SysUser();
        user.setId(2L);
        user.setPhone("13800000001");
        user.setNickname("张三");
        user.setRole("CONSUMER");
        user.setStatus(CommonConstants.USER_ENABLED);
        user.setPassword(passwordEncoder.encode(rawPassword));
        return user;
    }

    private RegisterRequest registerRequest(String pwd, String confirm) {
        RegisterRequest r = new RegisterRequest();
        r.setPhone("13900001111");
        r.setPassword(pwd);
        r.setConfirmPassword(confirm);
        r.setRole("CONSUMER");
        return r;
    }

    @Test
    @DisplayName("两次密码不一致时拒绝注册")
    void registerPasswordMismatch() {
        assertThatThrownBy(() -> service.register(registerRequest("Abc123456", "Abc123457")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("两次输入的密码不一致");
    }

    @Test
    @DisplayName("手机号已注册时拒绝注册")
    void registerDuplicatePhone() {
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertThatThrownBy(() -> service.register(registerRequest("Abc123456", "Abc123456")))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.PHONE_ALREADY_EXISTS.getCode()));
    }

    @Test
    @DisplayName("未填昵称时按手机号后四位生成")
    void registerDefaultNickname() {
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.insert(any(SysUser.class))).thenAnswer(inv -> {
            SysUser u = inv.getArgument(0);
            u.setId(99L);
            assertThat(u.getNickname()).isEqualTo("用户1111");
            assertThat(u.getPassword()).isNotEqualTo("Abc123456");
            assertThat(passwordEncoder.matches("Abc123456", u.getPassword())).isTrue();
            assertThat(u.getStatus()).isEqualTo(CommonConstants.USER_ENABLED);
            return 1;
        });
        assertThat(service.register(registerRequest("Abc123456", "Abc123456"))).isEqualTo(99L);
    }

    @Test
    @DisplayName("自定义昵称按原样保存")
    void registerCustomNickname() {
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.insert(any(SysUser.class))).thenAnswer(inv -> {
            SysUser u = inv.getArgument(0);
            u.setId(100L);
            assertThat(u.getNickname()).isEqualTo("测试昵称");
            return 1;
        });
        RegisterRequest r = registerRequest("Abc123456", "Abc123456");
        r.setNickname("测试昵称");
        assertThat(service.register(r)).isEqualTo(100L);
    }

    private LoginRequest loginRequest(String phone, String password) {
        LoginRequest r = new LoginRequest();
        r.setPhone(phone);
        r.setPassword(password);
        return r;
    }

    @Test
    @DisplayName("登录成功返回脱敏手机号与双令牌")
    void loginSuccess() {
        SysUser user = existingUser("User@123");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        LoginVO vo = service.login(loginRequest("13800000001", "User@123"));
        assertThat(vo.getToken()).isNotBlank();
        assertThat(vo.getRefreshToken()).isNotBlank();
        assertThat(vo.getExpiresIn()).isEqualTo(7_200L);
        assertThat(vo.getUserInfo().getPhone()).isEqualTo("138****0001");
        assertThat(vo.getUserInfo().getRole()).isEqualTo("CONSUMER");
        assertThat(vo.getUserInfo().getNickname()).isEqualTo("张三");

        assertThat(tokenProvider.getUserId(tokenProvider.parse(vo.getToken()))).isEqualTo(2L);
        assertThat(tokenProvider.isRefreshToken(tokenProvider.parse(vo.getRefreshToken()))).isTrue();
    }

    @Test
    @DisplayName("账号不存在时返回账号或密码错误")
    void loginUnknownPhone() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThatThrownBy(() -> service.login(loginRequest("13800009999", "User@123")))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.PASSWORD_INCORRECT.getCode()));
    }

    @Test
    @DisplayName("密码错误时返回剩余尝试次数")
    void loginWrongPassword() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingUser("User@123"));
        assertThatThrownBy(() -> service.login(loginRequest("13800000001", "Wrong@123")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("还可尝试 4 次");
    }

    @Test
    @DisplayName("连续 5 次失败后账号被锁定 15 分钟")
    void loginLockedAfterFiveFailures() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingUser("User@123"));
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.login(loginRequest("13800000001", "Wrong@123")))
                    .isInstanceOf(BizException.class);
        }
        assertThatThrownBy(() -> service.login(loginRequest("13800000001", "User@123")))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.LOGIN_TOO_FREQUENT.getCode()));
        assertThat(loginAttemptService.unlockAt("13800000001")).isNotNull();
    }

    @Test
    @DisplayName("登录成功后清除失败计数")
    void loginSuccessResetsCounter() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingUser("User@123"));
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.login(loginRequest("13800000001", "Wrong@123")));
        }
        service.login(loginRequest("13800000001", "User@123"));
        assertThat(loginAttemptService.isBlocked("13800000001")).isFalse();
        assertThat(loginAttemptService.unlockAt("13800000001")).isNull();
    }

    @Test
    @DisplayName("账号被禁用时拒绝登录")
    void loginDisabledAccount() {
        SysUser user = existingUser("User@123");
        user.setStatus(CommonConstants.USER_DISABLED);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        assertThatThrownBy(() -> service.login(loginRequest("13800000001", "User@123")))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.ACCOUNT_DISABLED.getCode()));
    }

    @Test
    @DisplayName("刷新令牌可换发新的访问令牌")
    void refreshToken() {
        SysUser user = existingUser("User@123");
        String refresh = tokenProvider.createRefreshToken(2L, user.getPhone(), user.getRole());
        when(userMapper.selectById(2L)).thenReturn(user);

        LoginVO vo = service.refresh(refresh);
        assertThat(vo.getToken()).isNotBlank();
        assertThat(tokenProvider.isRefreshToken(tokenProvider.parse(vo.getToken()))).isFalse();
    }

    @Test
    @DisplayName("用访问令牌调用刷新接口被拒绝")
    void refreshWithAccessToken() {
        SysUser user = existingUser("User@123");
        String access = tokenProvider.createAccessToken(2L, user.getPhone(), user.getRole());
        assertThatThrownBy(() -> service.refresh(access))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("刷新令牌无效");
    }

    @Test
    @DisplayName("禁用账号的刷新令牌不可用")
    void refreshDisabledAccount() {
        SysUser user = existingUser("User@123");
        user.setStatus(CommonConstants.USER_DISABLED);
        String refresh = tokenProvider.createRefreshToken(2L, user.getPhone(), user.getRole());
        when(userMapper.selectById(2L)).thenReturn(user);
        assertThatThrownBy(() -> service.refresh(refresh))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.ACCOUNT_DISABLED.getCode()));
    }

    @Test
    @DisplayName("使用验证码登录时会校验验证码")
    void loginWithCaptcha() {
        SysUser user = existingUser("User@123");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        LoginRequest r = loginRequest("13800000001", "User@123");
        r.setCaptchaKey("k1");
        r.setCaptchaCode("ABCD");
        service.login(r);
        org.mockito.Mockito.verify(captchaService).validate("k1", "ABCD");
    }
}
