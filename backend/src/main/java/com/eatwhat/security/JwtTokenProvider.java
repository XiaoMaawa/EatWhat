package com.eatwhat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 令牌提供者：签发、解析、校验。
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_PHONE = "phone";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;
    private final long expiration;
    private final long refreshExpiration;

    public JwtTokenProvider(@Value("${eatwhat.jwt.secret}") String secret,
                            @Value("${eatwhat.jwt.expiration:7200000}") long expiration,
                            @Value("${eatwhat.jwt.refresh-expiration:604800000}") long refreshExpiration) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
    }

    public long getExpirationMs() {
        return expiration;
    }

    /** 签发访问令牌 */
    public String createAccessToken(Long userId, String phone, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setId(java.util.UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_PHONE, phone)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 签发刷新令牌 */
    public String createRefreshToken(Long userId, String phone, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setId(java.util.UUID.randomUUID().toString())
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_PHONE, phone)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshExpiration))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public Long getUserId(Claims claims) {
        Object val = claims.get(CLAIM_USER_ID);
        if (val == null) {
            return Long.valueOf(claims.getSubject());
        }
        return Long.valueOf(String.valueOf(val));
    }

    public String getPhone(Claims claims) {
        return claims.get(CLAIM_PHONE, String.class);
    }

    public String getRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 校验失败：{}", e.getMessage());
            return false;
        }
    }
}
