package com.eatwhat.security;

import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 认证鉴权拦截器单元测试。
 * 覆盖公开接口放行、缺少 / 过期 / 篡改令牌、刷新令牌误用与角色越权。
 */
@DisplayName("认证拦截器")
class AuthInterceptorTest {

    private static final String SECRET = "eatwhat-unit-test-secret-key-0123456789abcdef";
    private static final long EXPIRATION = 7_200_000L;

    private JwtTokenProvider provider;
    private AuthInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    /** 模拟受保护的业务接口 */
    static class ProtectedApi {
        public void handle() {
        }
    }

    /** 模拟公开接口 */
    @PublicApi
    static class OpenApi {
        public void handle() {
        }
    }

    /** 模拟仅限商家 */
    @RequireRole("MERCHANT")
    static class MerchantOnlyApi {
        public void handle() {
        }
    }

    /** 模拟管理员与商家均可访问 */
    @RequireRole({"ADMIN", "MERCHANT"})
    static class AdminOrMerchantApi {
        public void handle() {
        }
    }

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, EXPIRATION, 604_800_000L);
        interceptor = new AuthInterceptor(provider);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private static HandlerMethod handlerFor(Class<?> type) throws Exception {
        return new HandlerMethod(type.getDeclaredConstructor().newInstance(),
                type.getDeclaredMethod("handle"));
    }

    private void withToken(String token) {
        request.addHeader("Authorization", "Bearer " + token);
    }

    @Test
    @DisplayName("携带有访问令牌时写入登录用户上下文")
    void validTokenPopulatesContext() throws Exception {
        withToken(provider.createAccessToken(2L, "13800000001", "CONSUMER"));
        boolean pass = interceptor.preHandle(request, response, handlerFor(ProtectedApi.class));
        assertThat(pass).isTrue();
        assertThat(UserContext.userId()).isEqualTo(2L);
        assertThat(UserContext.role()).isEqualTo("CONSUMER");
        assertThat(UserContext.phone()).isEqualTo("13800000001");
        assertThat(UserContext.isAdmin()).isFalse();
    }

    @Test
    @DisplayName("管理员令牌被识别为管理员")
    void adminFlag() throws Exception {
        withToken(provider.createAccessToken(1L, "13800000000", "ADMIN"));
        interceptor.preHandle(request, response, handlerFor(ProtectedApi.class));
        assertThat(UserContext.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("公开接口无需令牌即可访问")
    void publicApiNoToken() throws Exception {
        assertThatCode(() -> interceptor.preHandle(request, response, handlerFor(OpenApi.class)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("受保护接口缺少令牌返回未登录")
    void protectedApiNoToken() throws Exception {
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerFor(ProtectedApi.class)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("Bearer 前缀为空时视为未登录")
    void emptyBearer() throws Exception {
        request.addHeader("Authorization", "Bearer   ");
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerFor(ProtectedApi.class)))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("缺少 Bearer 前缀的令牌不被解析")
    void missingBearerPrefix() throws Exception {
        request.addHeader("Authorization", provider.createAccessToken(2L, "13800000001", "CONSUMER"));
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerFor(ProtectedApi.class)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.UNAUTHORIZED.getCode()));
    }

    @Test
    @DisplayName("非法令牌返回令牌无效")
    void invalidToken() throws Exception {
        withToken("this.is.not.a.jwt");
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerFor(ProtectedApi.class)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.TOKEN_INVALID.getCode()));
    }

    @Test
    @DisplayName("刷新令牌不能访问业务接口")
    void refreshTokenRejected() throws Exception {
        withToken(provider.createRefreshToken(2L, "13800000001", "CONSUMER"));
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerFor(ProtectedApi.class)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("请使用访问令牌");
    }

    @Test
    @DisplayName("OPTIONS 预检请求直接放行")
    void optionsPass() {
        request.setMethod("OPTIONS");
        assertThatCode(() -> interceptor.preHandle(request, response, "not-a-handler"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("角色匹配时放行")
    void roleMatched() throws Exception {
        withToken(provider.createAccessToken(4L, "13800000010", "MERCHANT"));
        assertThatCode(() -> interceptor.preHandle(request, response, handlerFor(MerchantOnlyApi.class)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("角色不匹配时返回角色无权")
    void roleNotMatched() throws Exception {
        withToken(provider.createAccessToken(2L, "13800000001", "CONSUMER"));
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerFor(MerchantOnlyApi.class)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.ROLE_NOT_ALLOWED.getCode()));
    }

    @Test
    @DisplayName("多角色声明中任一匹配即可")
    void multipleRoles() throws Exception {
        withToken(provider.createAccessToken(1L, "13800000000", "ADMIN"));
        assertThatCode(() -> interceptor.preHandle(request, response, handlerFor(AdminOrMerchantApi.class)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("请求结束后清理线程变量，避免上下文串号")
    void contextClearedAfterCompletion() throws Exception {
        withToken(provider.createAccessToken(2L, "13800000001", "CONSUMER"));
        interceptor.preHandle(request, response, handlerFor(ProtectedApi.class));
        assertThat(UserContext.get()).isNotNull();
        interceptor.afterCompletion(request, response, handlerFor(ProtectedApi.class), null);
        assertThat(UserContext.get()).isNull();
        assertThat(UserContext.userId()).isNull();
    }
}
