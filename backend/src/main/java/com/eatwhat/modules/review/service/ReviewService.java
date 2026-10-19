package com.eatwhat.modules.review.service;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.review.dto.CreateReviewRequest;
import com.eatwhat.modules.review.entity.Review;

/**
 * 评价服务。
 */
public interface ReviewService {

    /**
     * 提交评价。
     */
    Review create(Long userId, CreateReviewRequest request);

    /**
     * 顾客查看自己的评价。
     */
    PageResult<Review> pageMyReviews(Long userId, int page, int size);

    /**
     * 顾客端查看某店铺的评价（匿名评价隐藏昵称头像）。
     */
    PageResult<Review> pageShopReviews(Long shopId, int page, int size, Integer score);

    /**
     * 商家回复评价。
     */
    void reply(Long userId, Long reviewId, String reply);

    /**
     * 查询某订单的评价，无则返回 null。
     */
    Review findByOrderId(Long orderId);
}
