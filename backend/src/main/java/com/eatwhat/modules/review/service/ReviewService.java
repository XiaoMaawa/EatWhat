package com.eatwhat.modules.review.service;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.review.dto.CreateReviewRequest;
import com.eatwhat.modules.review.dto.UpdateReviewRequest;
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

    /**
     * 顾客修改自己的评价：评价提交后 24 小时内可修改一次。
     */
    Review update(Long userId, Long reviewId, UpdateReviewRequest request);

    /**
     * 顾客删除自己的评价：评价提交后 24 小时内可删除一次。
     */
    void delete(Long userId, Long reviewId);

    /**
     * 查询单条评价详情（仅本人可看）。
     */
    Review detailForOwner(Long userId, Long reviewId);
}
