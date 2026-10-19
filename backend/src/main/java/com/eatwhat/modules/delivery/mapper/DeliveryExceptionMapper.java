package com.eatwhat.modules.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatwhat.modules.delivery.entity.DeliveryException;
import org.apache.ibatis.annotations.Mapper;

/**
 * 配送异常与转单申请 Mapper。
 */
@Mapper
public interface DeliveryExceptionMapper extends BaseMapper<DeliveryException> {
}
