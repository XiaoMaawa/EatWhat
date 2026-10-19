package com.eatwhat.modules.stats.service;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.stats.entity.StatsDaily;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日统计汇总查询。
 */
public interface StatsDailyService {

    /**
     * 分页查询每日汇总。
     */
    PageResult<StatsDaily> page(String startDate, String endDate, int page, int size);

    /**
     * 指定日期区间汇总（含合计行，供大屏/导出）。
     */
    List<StatsDaily> listRange(String startDate, String endDate);

    /**
     * 汇总指定日期（默认昨天）；幂等，重复执行覆盖原记录。
     *
     * @return 汇总结果
     */
    StatsDaily summarize(LocalDate date);
}