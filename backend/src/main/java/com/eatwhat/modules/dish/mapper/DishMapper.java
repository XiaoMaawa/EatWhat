package com.eatwhat.modules.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatwhat.modules.dish.entity.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 菜品 Mapper。库存扣减使用条件更新保证原子性。
 */
@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    /**
     * 原子扣减库存：仅当库存充足时扣减，返回受影响行数。
     * 扣减后库存为 0 自动置为售罄状态。
     */
    @Update("UPDATE dish SET stock = stock - #{quantity}, month_sales = month_sales + #{quantity}, "
            + "status = CASE WHEN stock - #{quantity} <= 0 THEN 2 ELSE status END, updated_at = NOW() "
            + "WHERE id = #{dishId} AND deleted = 0 AND stock >= #{quantity}")
    int deductStock(@Param("dishId") Long dishId, @Param("quantity") Integer quantity);

    /**
     * 归还库存（取消订单时）。
     */
    @Update("UPDATE dish SET stock = stock + #{quantity}, month_sales = GREATEST(month_sales - #{quantity}, 0), "
            + "status = CASE WHEN status = 2 AND stock + #{quantity} > 0 THEN 1 ELSE status END, updated_at = NOW() "
            + "WHERE id = #{dishId} AND deleted = 0")
    int restoreStock(@Param("dishId") Long dishId, @Param("quantity") Integer quantity);

    /**
     * 刷新店铺菜品月销量（下单成功后调用）。
     */
    @Update("UPDATE dish SET month_sales = month_sales + #{quantity} WHERE id = #{dishId} AND deleted = 0")
    int increaseSales(@Param("dishId") Long dishId, @Param("quantity") Integer quantity);
}
