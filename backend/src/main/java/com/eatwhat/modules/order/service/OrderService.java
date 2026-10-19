package com.eatwhat.modules.order.service;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.order.dto.CreateOrderRequest;
import com.eatwhat.modules.order.entity.Order;
import com.eatwhat.modules.order.vo.OrderDetailVO;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 订单服务。
 */
public interface OrderService {

    /**
     * 创建订单（购物车结算）。
     */
    Map<String, Object> create(Long userId, CreateOrderRequest request);

    /**
     * 发起支付（模拟支付，返回支付链接）。
     */
    Map<String, Object> pay(Long userId, Long orderId);

    /**
     * 支付回调（幂等）。
     */
    void payCallback(String orderNo, String tradeNo, BigDecimal amount);

    /**
     * 顾客订单列表。
     */
    PageResult<Order> pageMyOrders(Long userId, Integer status, int page, int size);

    /**
     * 订单详情（含明细与状态流转）。
     */
    OrderDetailVO detail(Long userId, String role, Long orderId);

    /**
     * 通用状态流转，供骑手/商家/系统复用。
     */
    void transition(Long orderId, int targetStatus, String role, String remark);

    /**
     * 顾客取消订单。
     */
    void cancel(Long userId, Long orderId, String reason);

    /**
     * 顾客确认收货。
     */
    void confirm(Long userId, Long orderId);

    /**
     * 按主键查询。
     */
    Order getById(Long orderId);

    /**
     * 商家订单列表。
     */
    PageResult<Order> pageMerchantOrders(Long merchantId, Integer status, String keyword,
                                         java.time.LocalDate startDate, java.time.LocalDate endDate,
                                         int page, int size);

    /**
     * 商家接单：1 -> 2。
     */
    void merchantAccept(Long merchantId, Long orderId);

    /**
     * 商家拒单：1 -> 7。
     */
    void merchantReject(Long merchantId, Long orderId, String reason);

    /**
     * 商家备餐完成：2 -> 3，同时创建配送任务。
     */
    void merchantReady(Long merchantId, Long orderId);

    /**
     * 定时任务：关闭超时未支付订单。
     */
    int closeTimeoutOrders();

    /**
     * 平台订单列表（管理员）。
     */
    PageResult<Order> pageAdminOrders(Integer status, String keyword, int page, int size);

    /**
     * 平台强制取消订单（含退款与库存回滚），供管理员干预使用。
     */
    void forceCancel(Long orderId, String reason);
}
