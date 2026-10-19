package com.eatwhat.modules.auth.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 登录失败次数限制：5 次失败锁定 15 分钟（内存实现，单实例足够）。
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 15;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /**
     * 记录一次失败，返回剩余可尝试次数。
     */
    public int loginFailed(String phone) {
        String key = normalize(phone);
        Attempt attempt = attempts.computeIfAbsent(key, k -> new Attempt());
        int count = attempt.count.incrementAndGet();
        attempt.lastFailedAt = LocalDateTime.now();
        return Math.max(MAX_ATTEMPTS - count, 0);
    }

    /**
     * 清除失败记录（登录成功时调用）。
     */
    public void loginSuccess(String phone) {
        attempts.remove(normalize(phone));
    }

    /**
     * 是否已被锁定；锁定过期的记录会被清理。
     */
    public boolean isBlocked(String phone) {
        String key = normalize(phone);
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return false;
        }
        if (attempt.lastFailedAt.plusMinutes(LOCK_MINUTES).isBefore(LocalDateTime.now())) {
            attempts.remove(key);
            return false;
        }
        return attempt.count.get() >= MAX_ATTEMPTS;
    }

    /**
     * 解封时间（未锁定返回 null）。
     */
    public LocalDateTime unlockAt(String phone) {
        Attempt attempt = attempts.get(normalize(phone));
        if (attempt == null) {
            return null;
        }
        return attempt.lastFailedAt.plusMinutes(LOCK_MINUTES);
    }

    private String normalize(String phone) {
        return phone == null ? "" : phone.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Attempt {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile LocalDateTime lastFailedAt = LocalDateTime.now();
    }
}
