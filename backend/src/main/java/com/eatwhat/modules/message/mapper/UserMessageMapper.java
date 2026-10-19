package com.eatwhat.modules.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatwhat.modules.message.entity.UserMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户消息 Mapper。
 */
@Mapper
public interface UserMessageMapper extends BaseMapper<UserMessage> {
}
