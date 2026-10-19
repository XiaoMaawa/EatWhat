package com.eatwhat.modules.content.controller;

import com.eatwhat.common.result.R;
import com.eatwhat.modules.content.entity.SysDict;
import com.eatwhat.modules.content.service.SysDictService;
import com.eatwhat.security.PublicApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 公共字典接口（无需登录即可读取，供各端初始化下拉框）。
 */
@Tag(name = "公共配置")
@PublicApi
@RestController
@RequestMapping("/api/v1/dicts")
public class DictController {

    /** 前端一次性拼包时最多允许的类型数量，避免被当作全量导出接口 */
    private static final int MAX_BATCH_TYPES = 20;

    private final SysDictService dictService;

    public DictController(SysDictService dictService) {
        this.dictService = dictService;
    }

    @Operation(summary = "按类型查询字典项")
    @GetMapping("/{dictType}")
    public R<List<SysDict>> listByType(@PathVariable String dictType) {
        return R.ok(dictService.listByType(dictType));
    }

    @Operation(summary = "批量查询字典项")
    @GetMapping
    public R<Map<String, List<SysDict>>> batch(@RequestParam String types) {
        List<String> typeList = Arrays.stream(types.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(MAX_BATCH_TYPES)
                .toList();
        return R.ok(dictService.mapByTypes(typeList));
    }

    @Operation(summary = "查询已配置的字典类型")
    @GetMapping("/types")
    public R<List<String>> types() {
        return R.ok(dictService.listTypes());
    }
}
