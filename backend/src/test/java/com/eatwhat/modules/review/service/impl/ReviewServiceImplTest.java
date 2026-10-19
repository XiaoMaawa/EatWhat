package com.eatwhat.modules.review.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.modules.order.entity.Order;
import com.eatwhat.modules.order.mapper.OrderMapper;
import com.eatwhat.modules.review.dto.CreateReviewRequest;
import com.eatwhat.modules.review.dto.UpdateReviewRequest;
import com.eatwhat.modules.review.entity.Review;
import com.eatwhat.modules.review.mapper.ReviewMapper;
import com.eatwhat.modules.shop.mapper.ShopMapper;
import com.eatwhat.modules.shop.service.ShopService;
import com.eatwhat.modules.user.mapper.UserAddressMapper;
import com.eatwhat.modules.delivery.entity.Rider;
import com.eatwhat.modules.delivery.mapper.RiderMapper;
import com.eatwhat.modules.auth.entity.SysUser;
import com.eatwhat.modules.auth.mapper.SysUserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 评价服务单元测试。
 * 覆盖提交校验（订单归属 / 状态 / 时限 / 重复）、修改删除的归属与 24 小时窗口。
 */
@DisplayName("评价服务")
class ReviewServiceImplTest {

    private static final Long USER_ID = 2L;
    private static final Long OTHER_USER_ID = 3L;
    private static final Long ORDER_ID = 100L;
    private static final Long REVIEW_ID = 500L;
    private static final Long SHOP_ID = 1L;

    private ReviewMapper reviewMapper;
    private OrderMapper orderMapper;
    private ShopService shopService;
    private ReviewServiceImpl service;

    @BeforeAll
    static void initMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.eatwhat.modules.review.mapper.ReviewMapper");
        TableInfoHelper.initTableInfo(assistant, Review.class);
    }

    @BeforeEach
    void setUp() throws Exception {
        reviewMapper = mock(ReviewMapper.class);
        orderMapper = mock(OrderMapper.class);
        shopService = mock(ShopService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        RiderMapper riderMapper = mock(RiderMapper.class);
        ShopMapper shopMapper = mock(ShopMapper.class);
        UserAddressMapper addressMapper = mock(UserAddressMapper.class);

        Constructor<?>[] ctors = ReviewServiceImpl.class.getConstructors();
        Constructor<?> target = ctors[0];
        Object[] args = new Object[target.getParameterCount()];
        Class<?>[] types = target.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(ReviewMapper.class)) {
                args[i] = reviewMapper;
            } else if (types[i].equals(OrderMapper.class)) {
                args[i] = orderMapper;
            } else if (types[i].equals(ShopService.class)) {
                args[i] = shopService;
            } else if (types[i].equals(SysUserMapper.class)) {
                args[i] = userMapper;
            } else if (types[i].equals(RiderMapper.class)) {
                args[i] = riderMapper;
            } else if (types[i].equals(ShopMapper.class)) {
                args[i] = shopMapper;
            } else if (types[i].equals(UserAddressMapper.class)) {
                args[i] = addressMapper;
            } else {
                args[i] = mock(types[i]);
            }
        }
        service = (ReviewServiceImpl) target.newInstance(args);
    }

    private Order completedOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUserId(USER_ID);
        order.setShopId(SHOP_ID);
        order.setStatus(CommonConstants.ORDER_COMPLETED);
        order.setCompletedAt(LocalDateTime.now().minusDays(1));
        return order;
    }

    private Review review(LocalDateTime createdAt) {
        Review r = new Review();
        r.setId(REVIEW_ID);
        r.setOrderId(ORDER_ID);
        r.setUserId(USER_ID);
        r.setShopId(SHOP_ID);
        r.setShopScore(5);
        r.setDeliveryScore(4);
        r.setContent("米饭超级好吃，配送也很快，下次还来！");
        r.setCreatedAt(createdAt);
        return r;
    }

    private CreateReviewRequest createRequest() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setOrderId(ORDER_ID);
        request.setShopScore(5);
        request.setDeliveryScore(4);
        request.setContent("米饭超级好吃，配送也很快，下次还来！");
        return request;
    }

    private UpdateReviewRequest updateRequest() {
        UpdateReviewRequest request = new UpdateReviewRequest();
        request.setShopScore(4);
        request.setDeliveryScore(5);
        request.setContent("改一下：米饭还是很好吃，就是配送员绕了远路。");
        return request;
    }

    @Test
    @DisplayName("订单不存在时提交评价失败")
    void createOrderNotFound() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(null);
        assertThatThrownBy(() -> service.create(USER_ID, createRequest()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.ORDER_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("评价他人订单被拒绝")
    void createOtherOrder() {
        Order order = completedOrder();
        order.setUserId(OTHER_USER_ID);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        assertThatThrownBy(() -> service.create(USER_ID, createRequest()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.DATA_NOT_OWNED.getCode());
    }

    @Test
    @DisplayName("订单未完成时不能评价")
    void createNotCompleted() {
        Order order = completedOrder();
        order.setStatus(CommonConstants.ORDER_PENDING_PAYMENT);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        assertThatThrownBy(() -> service.create(USER_ID, createRequest()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED.getCode());
    }

    @Test
    @DisplayName("超过 7 天评价窗口被拒绝")
    void createTimeout() {
        Order order = completedOrder();
        order.setCompletedAt(LocalDateTime.now().minusDays(CommonConstants.REVIEW_VALID_DAYS + 1));
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order);
        assertThatThrownBy(() -> service.create(USER_ID, createRequest()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.REVIEW_TIMEOUT.getCode());
    }

    @Test
    @DisplayName("重复评价同一订单被拒绝")
    void createDuplicate() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(completedOrder());
        when(reviewMapper.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(() -> service.create(USER_ID, createRequest()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS.getCode());
    }

    @Test
    @DisplayName("合法订单可以提交评价")
    void createSuccess() {
        when(orderMapper.selectById(ORDER_ID)).thenReturn(completedOrder());
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        when(reviewMapper.insert(any(Review.class))).thenReturn(1);
        Review created = service.create(USER_ID, createRequest());
        assertThat(created.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(created.getUserId()).isEqualTo(USER_ID);
        assertThat(created.getShopId()).isEqualTo(SHOP_ID);
        assertThat(created.getIsAnonymous()).isEqualTo(0);
    }

    @Test
    @DisplayName("评价不存在时修改失败")
    void updateNotFound() {
        when(reviewMapper.selectById(REVIEW_ID)).thenReturn(null);
        assertThatThrownBy(() -> service.update(USER_ID, REVIEW_ID, updateRequest()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.PARAM_INVALID.getCode());
    }

    @Test
    @DisplayName("修改他人评价被拒绝")
    void updateOtherReview() {
        when(reviewMapper.selectById(REVIEW_ID)).thenReturn(review(LocalDateTime.now().minusHours(1)));
        assertThatThrownBy(() -> service.update(OTHER_USER_ID, REVIEW_ID, updateRequest()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.DATA_NOT_OWNED.getCode());
    }

    @Test
    @DisplayName("超过 24 小时不能再修改评价")
    void updateExpired() {
        when(reviewMapper.selectById(REVIEW_ID))
                .thenReturn(review(LocalDateTime.now().minusHours(25)));
        assertThatThrownBy(() -> service.update(USER_ID, REVIEW_ID, updateRequest()))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.ORDER_STATUS_ILLEGAL.getCode());
    }

    @Test
    @DisplayName("24 小时内可以修改评价内容与评分")
    void updateSuccess() {
        when(reviewMapper.selectById(REVIEW_ID))
                .thenReturn(review(LocalDateTime.now().minusHours(2)));
        when(reviewMapper.updateById(any(Review.class))).thenReturn(1);
        Review updated = service.update(USER_ID, REVIEW_ID, updateRequest());
        assertThat(updated.getShopScore()).isEqualTo(4);
        assertThat(updated.getDeliveryScore()).isEqualTo(5);
        assertThat(updated.getContent()).startsWith("改一下");
    }

    @Test
    @DisplayName("24 小时内可以删除自己的评价")
    void deleteSuccess() {
        when(reviewMapper.selectById(REVIEW_ID))
                .thenReturn(review(LocalDateTime.now().minusHours(2)));
        when(reviewMapper.deleteById(REVIEW_ID)).thenReturn(1);
        service.delete(USER_ID, REVIEW_ID);
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("超过 24 小时不能删除评价")
    void deleteExpired() {
        when(reviewMapper.selectById(REVIEW_ID))
                .thenReturn(review(LocalDateTime.now().minusHours(30)));
        assertThatThrownBy(() -> service.delete(USER_ID, REVIEW_ID))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.ORDER_STATUS_ILLEGAL.getCode());
    }

    @Test
    @DisplayName("删除他人评价被拒绝")
    void deleteOtherReview() {
        when(reviewMapper.selectById(REVIEW_ID))
                .thenReturn(review(LocalDateTime.now().minusHours(2)));
        assertThatThrownBy(() -> service.delete(OTHER_USER_ID, REVIEW_ID))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.DATA_NOT_OWNED.getCode());
    }
}
