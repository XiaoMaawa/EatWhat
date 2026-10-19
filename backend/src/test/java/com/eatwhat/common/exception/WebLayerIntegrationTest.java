package com.eatwhat.common.exception;

import com.eatwhat.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Web 层集成测试：验证统一响应结构、HTTP 状态码映射与认证鉴权拦截器。
 *
 * <p>对应 {@code docs/06-测试方案.md} §2 的 JUnit5 + MockMvc 要求，
 * 以及 {@code docs/05-接口设计规范.md} §1.4 的 HTTP 状态码约定。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Web 层集成测试")
class WebLayerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String consumerToken() {
        return tokenProvider.createAccessToken(2L, "13800000001", "CONSUMER");
    }

    private String merchantToken() {
        return tokenProvider.createAccessToken(4L, "13800000010", "MERCHANT");
    }

    private String adminToken() {
        return tokenProvider.createAccessToken(1L, "13800000000", "ADMIN");
    }

    private int statusOf(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private Map<?, ?> bodyOf(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readValue(json, Map.class);
    }

    @Test
    @DisplayName("成功响应返回 HTTP 200 与统一结构")
    void successReturnsUnifiedBody() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/probe/ok")).andReturn();
        assertThat(statusOf(result)).isEqualTo(200);
        Map<?, ?> body = bodyOf(result);
        assertThat(body.get("code")).isEqualTo(0);
        assertThat(body.get("message")).isEqualTo("success");
        assertThat(body.get("data")).isEqualTo("pong");
        assertThat(body.get("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("业务规则失败返回 HTTP 200 并携带业务码")
    void bizFailureKeepsHttp200() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/probe/biz")).andReturn();
        assertThat(statusOf(result)).isEqualTo(200);
        Map<?, ?> body = bodyOf(result);
        assertThat(body.get("code")).isEqualTo(4003);
        assertThat(body.get("message")).isEqualTo(ErrorCode.STOCK_NOT_ENOUGH.getMessage());
    }

    @Test
    @DisplayName("带自定义消息的业务异常原样透出 message")
    void bizCustomMessage() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/probe/biz-custom")).andReturn();
        assertThat(statusOf(result)).isEqualTo(200);
        Map<?, ?> body = bodyOf(result);
        assertThat(body.get("code")).isEqualTo(3002);
        assertThat(body.get("message")).isEqualTo("自定义提示信息");
    }

    @Test
    @DisplayName("未携带令牌访问受保护接口返回 HTTP 401")
    void noTokenReturns401() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/probe/private")).andReturn();
        assertThat(statusOf(result)).isEqualTo(401);
        assertThat(bodyOf(result).get("code")).isEqualTo(2000);
    }

    @Test
    @DisplayName("令牌被篡改返回 HTTP 401")
    void tamperedTokenReturns401() throws Exception {
        String tampered = consumerToken() + "x";
        MvcResult result = mockMvc.perform(get("/api/v1/probe/private")
                .header("Authorization", "Bearer " + tampered)).andReturn();
        assertThat(statusOf(result)).isEqualTo(401);
        assertThat(bodyOf(result).get("code")).isEqualTo(1004);
    }

    @Test
    @DisplayName("刷新令牌不能访问业务接口")
    void refreshTokenRejected() throws Exception {
        String refresh = tokenProvider.createRefreshToken(2L, "13800000001", "CONSUMER");
        MvcResult result = mockMvc.perform(get("/api/v1/probe/private")
                .header("Authorization", "Bearer " + refresh)).andReturn();
        assertThat(statusOf(result)).isEqualTo(401);
        assertThat(bodyOf(result).get("code")).isEqualTo(1004);
    }

    @Test
    @DisplayName("角色不符返回 HTTP 403")
    void wrongRoleReturns403() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/probe/merchant-only")
                .header("Authorization", "Bearer " + consumerToken())).andReturn();
        assertThat(statusOf(result)).isEqualTo(403);
        assertThat(bodyOf(result).get("code")).isEqualTo(2002);
    }

    @Test
    @DisplayName("角色匹配时正常返回")
    void rightRolePasses() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/probe/merchant-only")
                .header("Authorization", "Bearer " + merchantToken())).andReturn();
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(bodyOf(result).get("data")).isEqualTo("merchant");
    }

    @Test
    @DisplayName("公开接口即使标注角色，未登录也放行")
    void publicApiSkipsAuth() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/probe/admin-only-public")).andReturn();
        assertThat(statusOf(result)).isEqualTo(200);
        assertThat(bodyOf(result).get("data")).isEqualTo("admin");
    }

    @Test
    @DisplayName("请求体校验失败返回 HTTP 400")
    void validationFailureReturns400() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/probe/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":0}")).andReturn();
        assertThat(statusOf(result)).isEqualTo(400);
        Map<?, ?> body = bodyOf(result);
        assertThat(body.get("code")).isEqualTo(3002);
        String message = String.valueOf(body.get("message"));
        assertThat(message).contains("名称不能为空");
        assertThat(message).contains("数量至少为 1");
    }

    @Test
    @DisplayName("缺少必填参数返回 HTTP 400")
    void missingParamReturns400() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/probe/require-param")).andReturn();
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(bodyOf(result).get("code")).isEqualTo(3001);
    }

    @Test
    @DisplayName("路径变量类型不匹配返回 HTTP 400")
    void typeMismatchReturns400() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/probe/number/abc")).andReturn();
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(bodyOf(result).get("code")).isEqualTo(3003);
    }

    @Test
    @DisplayName("请求体无法解析返回 HTTP 400")
    void unreadableBodyReturns400() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/probe/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not-json")).andReturn();
        assertThat(statusOf(result)).isEqualTo(400);
        assertThat(bodyOf(result).get("code")).isEqualTo(3002);
    }

    @Test
    @DisplayName("路径不存在返回 HTTP 404")
    void notFoundReturns404() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/probe/not-exists")
                .header("Authorization", "Bearer " + adminToken())).andReturn();
        assertThat(statusOf(result)).isEqualTo(404);
        assertThat(bodyOf(result).get("code")).isEqualTo(4004);
    }

    @Test
    @DisplayName("请求方法不支持返回 HTTP 405")
    void methodNotAllowedReturns405() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/probe/ok")
                .header("Authorization", "Bearer " + adminToken())).andReturn();
        assertThat(statusOf(result)).isEqualTo(405);
        assertThat(bodyOf(result).get("code")).isEqualTo(3002);
    }

    @Test
    @DisplayName("响应头回写 X-Trace-Id")
    void traceIdHeaderPresent() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/probe/ok")).andReturn();
        assertThat(result.getResponse().getHeader("X-Trace-Id")).isNotBlank();
        assertThat(String.valueOf(bodyOf(result).get("traceId"))).isNotBlank();
    }
}
