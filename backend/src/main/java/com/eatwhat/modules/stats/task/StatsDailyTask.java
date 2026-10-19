package com.eatwhat.modules.stats.task;

import com.eatwhat.modules.stats.service.StatsDailyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 每日统计汇总任务（公共-09）：每天 00:05 汇总前一日订单与交易数据。
 * 失败不影响其他任务，次日补跑同日期会覆盖旧记录（幂等）。
 */
@Component
public class StatsDailyTask {

    private static final Logger log = LoggerFactory.getLogger(StatsDailyTask.class);

    private final StatsDailyService statsDailyService;

    public StatsDailyTask(StatsDailyService statsDailyService) {
        this.statsDailyService = statsDailyService;
    }

    @Scheduled(cron = "0 5 0 * * ?", zone = "Asia/Shanghai")
    public void summarizeYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        try {
            statsDailyService.summarize(yesterday);
        } catch (Exception e) {
            log.error("每日统计汇总失败 date={}", yesterday, e);
        }
    }
}