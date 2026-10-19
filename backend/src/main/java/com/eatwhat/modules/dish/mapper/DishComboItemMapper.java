package com.eatwhat.modules.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatwhat.modules.dish.entity.DishComboItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 套餐组成 Mapper。
 */
@Mapper
public interface DishComboItemMapper extends BaseMapper<DishComboItem> {
}
