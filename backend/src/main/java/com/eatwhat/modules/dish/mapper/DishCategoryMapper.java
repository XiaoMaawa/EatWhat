package com.eatwhat.modules.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatwhat.modules.dish.entity.DishCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜品分类 Mapper。
 */
@Mapper
public interface DishCategoryMapper extends BaseMapper<DishCategory> {
}
