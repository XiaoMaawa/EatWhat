package com.eatwhat.modules.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.constant.OrderStatus;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.content.service.SensitiveWordService;
import com.eatwhat.modules.auth.entity.SysUser;
import com.eatwhat.modules.auth.mapper.SysUserMapper;
import com.eatwhat.modules.order.entity.Order;
import com.eatwhat.modules.order.mapper.OrderMapper;
import com.eatwhat.modules.review.dto.CreateReviewRequest;
import com.eatwhat.modules.review.dto.UpdateReviewRequest;
import com.eatwhat.modules.review.entity.Review;
import com.eatwhat.modules.review.mapper.ReviewMapper;
import com.eatwhat.modules.review.service.ReviewService;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.mapper.ShopMapper;
import com.eatwhat.modules.shop.service.ShopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 评价服务实现。
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

    /** 评价可修改/删除的时间窗口（小时） */
    private static final int REVIEW_EDIT_HOURS = 24;

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final ShopMapper shopMapper;
    private final SysUserMapper userMapper;
    private final ShopService shopService;

    /** 敏感词服务；单元测试环境下为空，调用前须判空 */
    @Autowired(required = false)
    private SensitiveWordService sensitiveWordService;

    public ReviewServiceImpl(ReviewMapper reviewMapper,
                             OrderMapper orderMapper,
                             ShopMapper shopMapper,
                             SysUserMapper userMapper,
                             ShopService shopService) {
        this.reviewMapper = reviewMapper;
        this.orderMapper = orderMapper;
        this.shopMapper = shopMapper;
        this.userMapper = userMapper;
        this.shopService = shopService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Review create(Long userId, CreateReviewRequest request) {
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw BizException.of(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw BizException.of(ErrorCode.DATA_NOT_OWNED);
        }
        if (!Integer.valueOf(CommonConstants.ORDER_COMPLETED).equals(order.getStatus())) {
            throw BizException.of(ErrorCode.REVIEW_NOT_ALLOWED, "订单完成后方可评价");
        }
        LocalDateTime completedAt = order.getCompletedAt() == null ? order.getUpdatedAt() : order.getCompletedAt();
        if (completedAt != null && completedAt.plusDays(CommonConstants.REVIEW_VALID_DAYS).isBefore(LocalDateTime.now())) {
            throw BizException.of(ErrorCode.REVIEW_TIMEOUT);
        }
        Long exists = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, order.getId()));
        if (exists != null && exists > 0) {
            throw BizException.of(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        checkContent(request.getContent());

        Review review = new Review();
        review.setOrderId(order.getId());
        review.setUserId(userId);
        review.setShopId(order.getShopId());
        review.setRiderId(order.getRiderId());
        review.setShopScore(request.getShopScore());
        review.setDeliveryScore(request.getDeliveryScore());
        review.setContent(request.getContent().trim());
        review.setImages(joinImages(request.getImages()));
        review.setIsAnonymous(Boolean.TRUE.equals(request.getIsAnonymous()) ? 1 : 0);
        reviewMapper.insert(review);

        // 刷新店铺平均分
        try {
            shopService.refreshScoreAndSales(order.getShopId(), 0);
        } catch (Exception e) {
            log.warn("刷新店铺评分失败 shopId={}", order.getShopId(), e);
        }
        return review;
    }

    @Override
    public PageResult<Review> pageMyReviews(Long userId, int page, int size) {
        Page<Review> result = reviewMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getUserId, userId)
                        .orderByDesc(Review::getId));
        result.getRecords().forEach(r -> fillShop(r, false));
        return PageResult.of(result);
    }

    @Override
    public PageResult<Review> pageShopReviews(Long shopId, int page, int size, Integer score) {
        Page<Review> result = reviewMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getShopId, shopId)
                        .eq(score != null, Review::getShopScore, score)
                        .orderByDesc(Review::getId));
        result.getRecords().forEach(r -> fillShop(r, true));
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long userId, Long reviewId, String reply) {
        if (reply == null || reply.isBlank()) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "回复内容不能为空");
        }
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "评价不存在");
        }
        Shop shop = shopService.currentShop(userId);
        if (!review.getShopId().equals(shop.getId())) {
            throw BizException.of(ErrorCode.DATA_NOT_OWNED);
        }
        if (review.getReply() != null && !review.getReply().isBlank()) {
            throw BizException.of(ErrorCode.REVIEW_ALREADY_REPLIED);
        }
        review.setReply(reply.trim());
        review.setRepliedAt(LocalDateTime.now());
        reviewMapper.updateById(review);
    }

    @Override
    public Review findByOrderId(Long orderId) {
        return reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, orderId)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Review update(Long userId, Long reviewId, UpdateReviewRequest request) {
        Review review = requireEditable(userId, reviewId);
        checkContent(request.getContent());
        review.setShopScore(request.getShopScore());
        review.setDeliveryScore(request.getDeliveryScore());
        review.setContent(request.getContent().trim());
        review.setImages(joinImages(request.getImages()));
        review.setIsAnonymous(Boolean.TRUE.equals(request.getIsAnonymous()) ? 1 : 0);
        reviewMapper.updateById(review);
        try {
            shopService.refreshScoreAndSales(review.getShopId(), 0);
        } catch (Exception e) {
            log.warn("刷新店铺评分失败 shopId={}", review.getShopId(), e);
        }
        return review;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long reviewId) {
        Review review = requireEditable(userId, reviewId);
        reviewMapper.deleteById(review.getId());
        try {
            shopService.refreshScoreAndSales(review.getShopId(), 0);
        } catch (Exception e) {
            log.warn("刷新店铺评分失败 shopId={}", review.getShopId(), e);
        }
    }

    @Override
    public Review detailForOwner(Long userId, Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "评价不存在");
        }
        if (!userId.equals(review.getUserId())) {
            throw BizException.of(ErrorCode.DATA_NOT_OWNED);
        }
        fillShop(review, false);
        return review;
    }

    /**
     * 敏感词检测：命中平台词库时拒绝提交。
     */
    private void checkContent(String text) {
        if (sensitiveWordService == null || text == null || text.isBlank()) {
            return;
        }
        List<String> hits = sensitiveWordService.hits(text, "REVIEW");
        if (hits != null && !hits.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_INVALID,
                    "评价内容包含敏感词：" + String.join("、", hits) + "，请修改后再提交");
        }
    }

    /**
     * 校验评价归属与 24 小时可编辑窗口。
     */
    private Review requireEditable(Long userId, Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "评价不存在");
        }
        if (!userId.equals(review.getUserId())) {
            throw BizException.of(ErrorCode.DATA_NOT_OWNED);
        }
        LocalDateTime createdAt = review.getCreatedAt();
        if (createdAt != null
                && createdAt.plusHours(REVIEW_EDIT_HOURS).isBefore(LocalDateTime.now())) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL,
                    "评价提交超过 " + REVIEW_EDIT_HOURS + " 小时，无法再修改或删除");
        }
        return review;
    }

    /**
     * 填充昵称/头像/店铺名；对外展示时匿名评价做脱敏处理。
     */
    private void fillShop(Review review, boolean maskAnonymous) {
        if (review.getShopId() != null) {
            Shop shop = shopMapper.selectById(review.getShopId());
            if (shop != null) {
                review.setShopName(shop.getName());
            }
        }
        boolean anonymous = Integer.valueOf(1).equals(review.getIsAnonymous());
        if (anonymous && maskAnonymous) {
            review.setNickname("匿名用户");
            review.setAvatar(null);
        } else if (review.getUserId() != null) {
            SysUser user = userMapper.selectById(review.getUserId());
            if (user != null) {
                review.setNickname(user.getNickname());
                review.setAvatar(user.getAvatar());
            }
        }
        if (review.getImages() != null && !review.getImages().isBlank()) {
            review.setImageList(Arrays.stream(review.getImages().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList());
        }
    }

    private String joinImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return String.join(",", images);
    }
}
