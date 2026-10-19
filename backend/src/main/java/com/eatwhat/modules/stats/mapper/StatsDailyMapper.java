package com.eatwhat.modules.stats.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatwhat.modules.stats.entity.StatsDaily;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日统计汇总 Mapper。
 */
@Mapper
public interface StatsDailyMapper extends BaseMapper<StatsDaily> {
}