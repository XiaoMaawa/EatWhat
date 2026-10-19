package com.eatwhat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 令牌签发与校验单元测试。
 * 覆盖正常往返、刷新令牌区分、签名篡改与过期令牌等安全场景。
 */
@DisplayName("JWT 令牌")
class JwtTokenProviderTest {

    private static final String SECRET = "eatwhat-unit-test-secret-key-0123456789abcdef";
    private static final long EXPIRATION = 7_200_000L;
    private static final long REFRESH_EXPIRATION = 604_800_000L;

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, EXPIRATION, REFRESH_EXPIRATION);
    }

    @Test
    @DisplayName("访问令牌可正确解析出用户信息")
    void accessTokenRoundTrip() {
        String token = provider.createAccessToken(7L, "13800000001", "CONSUMER");
        Claims claims = provider.parse(token);
        assertThat(provider.getUserId(claims)).isEqualTo(7L);
        assertThat(provider.getPhone(claims)).isEqualTo("13800000001");
        assertThat(provider.getRole(claims)).isEqualTo("CONSUMER");
        assertThat(provider.isRefreshToken(claims)).isFalse();
        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(provider.validate(token)).isTrue();
    }

    @Test
    @DisplayName("刷新令牌可被识别")
    void refreshTokenFlagged() {
        String token = provider.createRefreshToken(9L, "13800000010", "MERCHANT");
        Claims claims = provider.parse(token);
        assertThat(provider.isRefreshToken(claims)).isTrue();
        assertThat(provider.getRole(claims)).isEqualTo("MERCHANT");
    }

    @Test
    @DisplayName("过期时间按配置生效")
    void expirationHonoured() {
        String token = provider.createAccessToken(1L, "13800000000", "ADMIN");
        Claims claims = provider.parse(token);
        long ttl = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(ttl).isEqualTo(EXPIRATION);
        assertThat(provider.getExpirationMs()).isEqualTo(EXPIRATION);
    }

    @Test
    @DisplayName("签名被篡改的令牌无法解析")
    void tamperedTokenRejected() {
        String token = provider.createAccessToken(2L, "13800000002", "CONSUMER");
        String tampered = token.substring(0, token.length() - 2) + "xy";
        assertThat(provider.validate(tampered)).isFalse();
        assertThatThrownBy(() -> provider.parse(tampered)).isNotNull();
    }

    @Test
    @DisplayName("其他密钥签发的令牌不被信任")
    void foreignKeyRejected() {
        JwtTokenProvider other = new JwtTokenProvider(
                "another-secret-key-with-enough-length-000000", EXPIRATION, REFRESH_EXPIRATION);
        String foreign = other.createAccessToken(3L, "13800000011", "MERCHANT");
        assertThat(provider.validate(foreign)).isFalse();
    }

    @Test
    @DisplayName("过期令牌解析抛出 ExpiredJwtException")
    void expiredTokenRejected() {
        Date now = new Date();
        String expired = Jwts.builder()
                .setSubject("4")
                .claim("userId", 4L)
                .claim("role", "RIDER")
                .claim("type", "access")
                .setIssuedAt(new Date(now.getTime() - 10_000))
                .setExpiration(new Date(now.getTime() - 5_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
        assertThatThrownBy(() -> provider.parse(expired)).isInstanceOf(ExpiredJwtException.class);
        assertThat(provider.validate(expired)).isFalse();
    }

    @Test
    @DisplayName("空串与非法字符串不会被误判为有效")
    void garbageRejected() {
        assertThat(provider.validate("")).isFalse();
        assertThat(provider.validate("not-a-jwt")).isFalse();
        assertThat(provider.validate(null)).isFalse();
    }

    @Test
    @DisplayName("过短的密钥会被补零到 32 字节后仍可用")
    void shortSecretPadded() {
        JwtTokenProvider shortKey = new JwtTokenProvider("tiny", EXPIRATION, REFRESH_EXPIRATION);
        String token = shortKey.createAccessToken(5L, "13800000020", "RIDER");
        assertThat(shortKey.getUserId(shortKey.parse(token))).isEqualTo(5L);
    }
}
