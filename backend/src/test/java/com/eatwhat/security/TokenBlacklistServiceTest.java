package com.eatwhat.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 令牌失效列表单元测试（顾客-05）。
 */
class TokenBlacklistServiceTest {

    private static final String SECRET = "eatwhat-test-secret-key-must-be-at-least-32-bytes-long";
    private static final long EXPIRATION = 7_200_000L;

    private JwtTokenProvider provider;
    private TokenBlacklistService blacklist;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, EXPIRATION, 604_800_000L);
        blacklist = new TokenBlacklistService();
    }

    @Test
    @DisplayName("访问令牌签发时带 jti，不同令牌的 jti 互不相同")
    void accessTokenCarriesUniqueJti() {
        String first = provider.createAccessToken(2L, "13800000001", "CONSUMER");
        String second = provider.createAccessToken(2L, "13800000001", "CONSUMER");

        String jti1 = provider.parse(first).getId();
        String jti2 = provider.parse(second).getId();

        assertEquals(36, jti1.length(), "jti 应为 UUID 字符串");
        assertFalse(jti1.equals(jti2), "同一用户两次签发的 jti 不应相同");
    }

    @Test
    @DisplayName("未注销的令牌不在失效列表中")
    void freshTokenIsNotRevoked() {
        Claims claims = provider.parse(provider.createAccessToken(2L, "13800000001", "CONSUMER"));

        assertFalse(blacklist.isRevoked(claims));
        assertEquals(0, blacklist.size());
    }

    @Test
    @DisplayName("注销后同一令牌立即失效，其他令牌不受影响")
    void revokedTokenIsRejected() {
        String revoked = provider.createAccessToken(2L, "13800000001", "CONSUMER");
        String other = provider.createAccessToken(2L, "13800000001", "CONSUMER");

        blacklist.revoke(provider.parse(revoked));

        assertTrue(blacklist.isRevoked(provider.parse(revoked)));
        assertFalse(blacklist.isRevoked(provider.parse(other)));
        assertEquals(1, blacklist.size());
    }

    @Test
    @DisplayName("刷新令牌同样可以注销")
    void refreshTokenCanBeRevoked() {
        String refresh = provider.createRefreshToken(2L, "13800000001", "CONSUMER");

        blacklist.revoke(provider.parse(refresh));

        assertTrue(blacklist.isRevoked(provider.parse(refresh)));
    }

    @Test
    @DisplayName("重复注销同一令牌不会重复计数")
    void revokeTwiceKeepsSingleEntry() {
        String token = provider.createAccessToken(2L, "13800000001", "CONSUMER");

        blacklist.revoke(provider.parse(token));
        blacklist.revoke(provider.parse(token));

        assertEquals(1, blacklist.size());
    }

    @Test
    @DisplayName("已自然过期的条目会被清理，且不再判定为已注销")
    void expiredEntryIsPurged() {
        Claims claims = mock(Claims.class);
        when(claims.getId()).thenReturn("expired-jti");
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() - 1_000L));

        blacklist.revoke(claims);
        assertEquals(1, blacklist.size());

        assertFalse(blacklist.isRevoked(claims), "过期条目应直接放行并移除");
        assertEquals(0, blacklist.size());
    }

    @Test
    @DisplayName("purgeExpired 清理到期的失效条目，保留仍有效的条目")
    void purgeExpiredRemovesOnlyExpired() {
        Claims expired = mock(Claims.class);
        when(expired.getId()).thenReturn("expired");
        when(expired.getExpiration()).thenReturn(new Date(System.currentTimeMillis() - 1_000L));
        Claims alive = mock(Claims.class);
        when(alive.getId()).thenReturn("alive");
        when(alive.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 60_000L));

        blacklist.revoke(expired);
        blacklist.revoke(alive);
        assertEquals(2, blacklist.size());

        blacklist.purgeExpired();

        assertEquals(1, blacklist.size());
        assertFalse(blacklist.isRevoked(expired));
        assertTrue(blacklist.isRevoked(alive));
    }

    @Test
    @DisplayName("空令牌与空声明不会被判定为已注销")
    void nullClaimsIsNotRevoked() {
        assertFalse(blacklist.isRevoked(null));
        blacklist.revoke(null);
        assertEquals(0, blacklist.size());
    }
}
