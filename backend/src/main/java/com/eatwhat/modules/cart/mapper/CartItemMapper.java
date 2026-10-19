package com.eatwhat.modules.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatwhat.modules.cart.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 购物车 Mapper。
 */
@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}
