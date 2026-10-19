package com.eatwhat.modules.auth.service;

import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码：4 位字符，Base64 DataURI 返回，5 分钟有效。
 */
@Service
public class CaptchaService {

    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 4;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int EXPIRE_MINUTES = 5;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Captcha> store = new ConcurrentHashMap<>();

    /**
     * 生成验证码，返回 key 与图片 DataURI。
     */
    public Map<String, Object> generate() {
        purgeExpired();
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        String key = UUID.randomUUID().toString().replace("-", "");
        store.put(key, new Captcha(code.toString(), LocalDateTime.now().plusMinutes(EXPIRE_MINUTES)));

        return Map.of(
                "captchaKey", key,
                "captchaImage", toDataUri(code.toString())
        );
    }

    /**
     * 校验验证码，一次性使用：无论成败立即失效。
     */
    public void validate(String key, String input) {
        if (key == null || key.isBlank() || input == null || input.isBlank()) {
            throw BizException.of(ErrorCode.CAPTCHA_INVALID, "验证码不能为空");
        }
        Captcha captcha = store.remove(key.trim());
        if (captcha == null) {
            throw BizException.of(ErrorCode.CAPTCHA_INVALID, "验证码已失效，请刷新");
        }
        if (captcha.expireAt.isBefore(LocalDateTime.now())) {
            throw BizException.of(ErrorCode.CAPTCHA_INVALID, "验证码已过期，请刷新");
        }
        if (!captcha.code.equalsIgnoreCase(input.trim())) {
            throw BizException.of(ErrorCode.CAPTCHA_INVALID);
        }
    }

    private void purgeExpired() {
        LocalDateTime now = LocalDateTime.now();
        store.entrySet().removeIf(entry -> entry.getValue().expireAt.isBefore(now));
    }

    private String toDataUri(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线
            for (int i = 0; i < 6; i++) {
                g.setColor(randomColor(160, 220));
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }
            // 干扰点
            for (int i = 0; i < 40; i++) {
                g.setColor(randomColor(150, 220));
                g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
            }
            // 字符
            g.setFont(new Font("Arial", Font.BOLD, 26));
            for (int i = 0; i < code.length(); i++) {
                g.setColor(randomColor(20, 120));
                double angle = (random.nextDouble() - 0.5) * 0.5;
                int x = 12 + i * 26;
                int y = 30 + random.nextInt(4) - 2;
                g.rotate(angle, x, y);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.rotate(-angle, x, y);
            }
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw BizException.of(ErrorCode.SYSTEM_ERROR, "验证码生成失败");
        }
    }

    private Color randomColor(int min, int max) {
        int range = max - min;
        return new Color(min + random.nextInt(range), min + random.nextInt(range), min + random.nextInt(range));
    }

    private static final class Captcha {
        private final String code;
        private final LocalDateTime expireAt;

        private Captcha(String code, LocalDateTime expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
