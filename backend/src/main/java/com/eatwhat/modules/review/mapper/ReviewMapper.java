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
}
