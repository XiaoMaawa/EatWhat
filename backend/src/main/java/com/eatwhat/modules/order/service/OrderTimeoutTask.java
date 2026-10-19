package com.eatwhat.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.constant.OrderStatus;
import com.eatwhat.modules.delivery.entity.DeliveryTask;
import com.eatwhat.modules.delivery.mapper.DeliveryTaskMapper;
import com.eatwhat.modules.dish.mapper.DishMapper;
import com.eatwhat.modules.order.entity.Order;
import com.eatwhat.modules.order.entity.OrderItem;
import com.eatwhat.modules.order.entity.OrderStatusLog;
import com.eatwhat.modules.order.mapper.OrderItemMapper;
import com.eatwhat.modules.order.mapper.OrderMapper;
import com.eatwhat.modules.order.mapper.OrderStatusLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单定时任务：超时未支付自动关闭、库存回滚。
 */
@Component
public class OrderTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutTask.class);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final DishMapper dishMapper;
    private final DeliveryTaskMapper deliveryTaskMapper;

    public OrderTimeoutTask(OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper,
                            OrderStatusLogMapper statusLogMapper,
                            DishMapper dishMapper,
                            DeliveryTaskMapper deliveryTaskMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.statusLogMapper = statusLogMapper;
        this.dishMapper = dishMapper;
        this.deliveryTaskMapper = deliveryTaskMapper;
    }

    /**
     * 每分钟扫描一次，关闭超过 15 分钟仍未支付的订单。
     */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 30_000L)
    @Transactional(rollbackFor = Exception.class)
    public void closeTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now()
                .minusMinutes(CommonConstants.ORDER_PAY_TIMEOUT_MINUTES);
        List<Order> timeoutOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, CommonConstants.ORDER_PENDING_PAYMENT)
                .lt(Order::getCreatedAt, deadline)
                .last("LIMIT 200"));
        if (timeoutOrders.isEmpty()) {
            return;
        }
        for (Order order : timeoutOrders) {
            cancelOrder(order, "超时未支付，系统自动关闭");
        }
        log.info("自动关闭超时未支付订单 {} 笔", timeoutOrders.size());
    }

    private void cancelOrder(Order order, String reason) {
        Order update = new Order();
        update.setId(order.getId());
        update.setStatus(CommonConstants.ORDER_CANCELLED);
        update.setCancelReason(reason);
        orderMapper.updateById(update);

        // 回滚库存：复用 restoreStock，库存归位后同时把「售罄」还原为「在售」
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        for (OrderItem item : items) {
            dishMapper.restoreStock(item.getDishId(), item.getQuantity());
        }

        // 取消配送任务（若已生成）
        deliveryTaskMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DeliveryTask>()
                        .eq(DeliveryTask::getOrderId, order.getId())
                        .in(DeliveryTask::getStatus, CommonConstants.TASK_WAITING, CommonConstants.TASK_GRABBED)
                        .set(DeliveryTask::getStatus, CommonConstants.TASK_DELIVERED));

        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderId(order.getId());
        statusLog.setFromStatus(order.getStatus());
        statusLog.setToStatus(CommonConstants.ORDER_CANCELLED);
        statusLog.setOperatorId(CommonConstants.SYSTEM_OPERATOR_ID);
        statusLog.setOperatorRole("SYSTEM");
        statusLog.setRemark(reason);
        statusLogMapper.insert(statusLog);
    }
}
