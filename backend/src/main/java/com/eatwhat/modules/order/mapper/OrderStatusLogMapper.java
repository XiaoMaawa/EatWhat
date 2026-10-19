package com.eatwhat.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatwhat.modules.order.entity.OrderStatusLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单状态日志 Mapper。
 */
@Mapper
public interface OrderStatusLogMapper extends BaseMapper<OrderStatusLog> {
}
