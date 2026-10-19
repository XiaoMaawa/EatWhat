package com.eatwhat.security;

import io.jsonwebtoken.Claims;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 令牌失效列表（顾客-05 / 商家-60 / 骑手-30 / 管理员-33 退出登录）。
 *
 * <p>JWT 本身无状态，退出登录后仅靠客户端清除令牌无法阻止旧令牌被继续使用。
 * 这里以令牌唯一标识 {@code jti} 为键维护一份失效列表，条目在令牌自身过期时间
 * 到达后自动淘汰，因此内存占用与「仍在有效期内的已登出令牌数」成正比。</p>
 *
 * <p>单实例部署下使用内存实现；若后续多实例部署，可将本类替换为 Redis 实现，
 * 对外接口保持不变。</p>
 */
@Component
public class TokenBlacklistService {

    /** jti -> 令牌过期时间戳（毫秒），过期后即可移除 */
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    /**
     * 将令牌加入失效列表。
     *
     * @param claims 已解析的令牌声明，需包含 jti 与 exp
     */
    public void revoke(Claims claims) {
        if (claims == null) {
            return;
        }
        String jti = claims.getId();
        if (jti == null || jti.isBlank()) {
            return;
        }
        long expireAt = claims.getExpiration() == null
                ? System.currentTimeMillis() + 7_200_000L
                : claims.getExpiration().getTime();
        blacklist.put(jti, expireAt);
    }

    /**
     * 判断令牌是否已被注销。
     *
     * @param claims 已解析的令牌声明
     * @return true 表示该令牌已失效，不应再被接受
     */
    public boolean isRevoked(Claims claims) {
        if (claims == null) {
            return false;
        }
        String jti = claims.getId();
        if (jti == null || jti.isBlank()) {
            return false;
        }
        Long expireAt = blacklist.get(jti);
        if (expireAt == null) {
            return false;
        }
        if (expireAt < System.currentTimeMillis()) {
            blacklist.remove(jti);
            return false;
        }
        return true;
    }

    /** 当前失效列表规模，供健康检查与测试使用 */
    public int size() {
        return blacklist.size();
    }

    /** 每分钟清理已自然过期的条目，避免内存无限增长 */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 60_000L)
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        blacklist.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}
