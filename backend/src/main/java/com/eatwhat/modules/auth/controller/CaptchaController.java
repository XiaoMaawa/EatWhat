package com.eatwhat.modules.auth.controller;

import com.eatwhat.common.result.R;
import com.eatwhat.modules.auth.service.CaptchaService;
import com.eatwhat.security.PublicApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 图形验证码接口。
 */
@Tag(name = "认证模块")
@RestController
@RequestMapping("/api/v1/captcha")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @PublicApi
    @Operation(summary = "获取图形验证码")
    @GetMapping
    public R<Map<String, Object>> captcha() {
        return R.ok(captchaService.generate());
    }
}
