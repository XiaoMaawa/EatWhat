package com.eatwhat.modules.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatwhat.modules.content.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告 Mapper。
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}