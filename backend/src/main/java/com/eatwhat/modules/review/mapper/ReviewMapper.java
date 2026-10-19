package com.eatwhat.modules.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eatwhat.modules.review.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 评价 Mapper。
 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    /**
     * 计算店铺平均评分。
     */
    @Select("SELECT IFNULL(AVG(shop_score), 5.0) FROM review WHERE shop_id = #{shopId} AND deleted = 0")
    BigDecimal avgShopScore(@Param("shopId") Long shopId);
    /**
     * 菜品近 30 天好评率：统计含该菜品的订单评价，4 星及以上占比，保留一位小数。
     */
    @Select("SELECT IFNULL(ROUND(SUM(CASE WHEN r.shop_score >= 4 THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1), 100.0) "
            + "FROM review r JOIN order_item oi ON r.order_id = oi.order_id "
            + "WHERE r.deleted = 0 AND oi.dish_id = #{dishId} AND r.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)")
    BigDecimal goodRateByDish(@Param("dishId") Long dishId);
}
