package com.eatwhat.modules.stats.controller;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.common.result.R;
import com.eatwhat.modules.stats.entity.StatsDaily;
import com.eatwhat.modules.stats.service.StatsDailyService;
import com.eatwhat.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日统计汇总查询（管理员端）。
 */
@Tag(name = "每日统计汇总")
@RestController
@RequestMapping("/api/v1/admin/daily-stats")
@RequireRole("ADMIN")
public class StatsDailyController {

    private final StatsDailyService statsDailyService;

    public StatsDailyController(StatsDailyService statsDailyService) {
        this.statsDailyService = statsDailyService;
    }

    @Operation(summary = "分页查询每日汇总")
    @GetMapping
    public R<PageResult<StatsDaily>> page(@RequestParam(required = false) String startDate,
                                          @RequestParam(required = false) String endDate,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return R.ok(statsDailyService.page(startDate, endDate, page, size));
    }

    @Operation(summary = "区间明细（供图表）")
    @GetMapping("/range")
    public R<List<StatsDaily>> range(@RequestParam String startDate, @RequestParam String endDate) {
        return R.ok(statsDailyService.listRange(startDate, endDate));
    }

    @Operation(summary = "手动补跑指定日期汇总（默认昨天）")
    @PostMapping("/summarize")
    public R<StatsDaily> summarize(@RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(statsDailyService.summarize(date == null ? LocalDate.now().minusDays(1) : date));
    }
}