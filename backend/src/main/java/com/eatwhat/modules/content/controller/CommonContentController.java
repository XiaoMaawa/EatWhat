package com.eatwhat.modules.content.controller;

import com.eatwhat.common.result.R;
import com.eatwhat.modules.content.entity.SysConfig;
import com.eatwhat.modules.content.service.SysConfigService;
import com.eatwhat.security.PublicApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 平台公共参数接口（免登录）。
 */
@Tag(name = "公共内容")
@RestController
@RequestMapping("/api/v1/common")
public class CommonContentController {

    private final SysConfigService sysConfigService;

    public CommonContentController(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    @Operation(summary = "平台参数")
    @PublicApi
    @GetMapping("/configs")
    public R<Map<String, String>> configs(@RequestParam(required = false) String group) {
        Map<String, String> map = new LinkedHashMap<>();
        for (SysConfig config : sysConfigService.listByGroup(group)) {
            map.put(config.getConfigKey(), config.getConfigValue());
        }
        return R.ok(map);
    }
}

