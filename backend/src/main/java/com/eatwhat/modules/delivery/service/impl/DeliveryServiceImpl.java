package com.eatwhat.modules.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.constant.OrderStatus;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.delivery.dto.RiderApplyRequest;
import com.eatwhat.modules.delivery.entity.DeliveryTask;
import com.eatwhat.modules.delivery.entity.Rider;
import com.eatwhat.modules.delivery.mapper.DeliveryTaskMapper;
import com.eatwhat.modules.delivery.mapper.RiderMapper;
import com.eatwhat.modules.delivery.service.DeliveryService;
import com.eatwhat.modules.order.entity.Order;
import com.eatwhat.modules.order.mapper.OrderMapper;
import com.eatwhat.modules.order.service.OrderService;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.mapper.ShopMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 骑手与配送任务服务实现。
 */
@Service
public class DeliveryServiceImpl implements DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryServiceImpl.class);

    /** 基础配送费，与距离系数一起决定骑手收益 */
    private static final BigDecimal BASE_FEE = BigDecimal.valueOf(4.0);
    private static final BigDecimal PER_KM_FEE = BigDecimal.valueOf(1.0);

    private final RiderMapper riderMapper;
    private final DeliveryTaskMapper taskMapper;
    private final OrderMapper orderMapper;
    private final ShopMapper shopMapper;
    private final OrderService orderService;

    public DeliveryServiceImpl(RiderMapper riderMapper,
                               DeliveryTaskMapper taskMapper,
                               OrderMapper orderMapper,
                               ShopMapper shopMapper,
                               @Lazy OrderService orderService) {
        this.riderMapper = riderMapper;
        this.taskMapper = taskMapper;
        this.orderMapper = orderMapper;
        this.shopMapper = shopMapper;
        this.orderService = orderService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Rider apply(Long userId, RiderApplyRequest request) {
        Rider rider = riderMapper.selectOne(new LambdaQueryWrapper<Rider>()
                .eq(Rider::getUserId, userId)
                .last("LIMIT 1"));
        if (rider == null) {
            rider = new Rider();
            rider.setUserId(userId);
            rider.setAuditStatus(CommonConstants.AUDIT_PENDING);
            rider.setTotalOrders(0);
            rider.setTotalIncome(BigDecimal.ZERO);
        } else if (Integer.valueOf(CommonConstants.AUDIT_PASSED).equals(rider.getAuditStatus())) {
            throw BizException.of(ErrorCode.AUDIT_ALREADY_DONE, "骑手认证已通过，无需重复提交");
        } else {
            // 驳回后重新提交，回到待审核
            rider.setAuditStatus(CommonConstants.AUDIT_PENDING);
            rider.setAuditRemark(null);
            rider.setAuditedAt(null);
        }
        rider.setRealName(request.getRealName().trim());
        rider.setIdCard(request.getIdCard().toUpperCase());
        rider.setHealthImg(request.getHealthImg());
        rider.setContactPhone(request.getContactPhone());
        if (rider.getId() == null) {
            riderMapper.insert(rider);
        } else {
            riderMapper.updateById(rider);
        }
        return rider;
    }

    @Override
    public Rider profile(Long userId) {
        Rider rider = riderMapper.selectOne(new LambdaQueryWrapper<Rider>()
                .eq(Rider::getUserId, userId)
                .last("LIMIT 1"));
        if (rider == null) {
            // 尚未提交认证申请
            return null;
        }
        return rider;
    }

    @Override
    public Rider requireApprovedRider(Long userId) {
        Rider rider = riderMapper.selectOne(new LambdaQueryWrapper<Rider>()
                .eq(Rider::getUserId, userId)
                .last("LIMIT 1"));
        if (rider == null) {
            throw BizException.of(ErrorCode.RIDER_NOT_APPROVED);
        }
        if (!Integer.valueOf(CommonConstants.AUDIT_PASSED).equals(rider.getAuditStatus())) {
            throw BizException.of(ErrorCode.RIDER_NOT_APPROVED,
                    rider.getAuditStatus() != null && rider.getAuditStatus() == CommonConstants.AUDIT_REJECTED
                            ? "骑手认证未通过审核，请修改资料后重新提交"
                            : "骑手认证还在审核中，通过后才能接单");
        }
        return rider;
    }

    @Override
    public List<DeliveryTask> availableTasks(Long userId) {
        requireApprovedRider(userId);
        List<DeliveryTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<DeliveryTask>()
                .eq(DeliveryTask::getStatus, CommonConstants.TASK_WAITING)
                .isNull(DeliveryTask::getRiderId)
                .orderByAsc(DeliveryTask::getId));
        tasks.forEach(this::fillOrderNo);
        return tasks;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grab(Long userId, Long taskId) {
        Rider rider = requireApprovedRider(userId);
        DeliveryTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw BizException.of(ErrorCode.DELIVERY_NOT_FOUND);
        }
        int rows = taskMapper.grab(taskId, rider.getId());
        if (rows == 0) {
            throw BizException.of(ErrorCode.TASK_ALREADY_TAKEN);
        }
        // 同步订单进入配送中
        orderService.transition(task.getOrderId(), CommonConstants.ORDER_DELIVERING, "RIDER", "骑手已接单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pickup(Long userId, Long taskId) {
        Rider rider = requireApprovedRider(userId);
        DeliveryTask task = requireOwnedTask(rider.getId(), taskId);
        if (!Integer.valueOf(CommonConstants.TASK_GRABBED).equals(task.getStatus())) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "当前任务状态不允许取餐");
        }
        task.setStatus(CommonConstants.TASK_PICKED);
        task.setPickedAt(java.time.LocalDateTime.now());
        taskMapper.updateById(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliver(Long userId, Long taskId) {
        Rider rider = requireApprovedRider(userId);
        DeliveryTask task = requireOwnedTask(rider.getId(), taskId);
        if (!Integer.valueOf(CommonConstants.TASK_PICKED).equals(task.getStatus())) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "取餐后才能送达");
        }
        task.setStatus(CommonConstants.TASK_DELIVERED);
        task.setDeliveredAt(java.time.LocalDateTime.now());
        taskMapper.updateById(task);

        // 骑手累计单量与收益
        rider.setTotalOrders((rider.getTotalOrders() == null ? 0 : rider.getTotalOrders()) + 1);
        rider.setTotalIncome((rider.getTotalIncome() == null ? BigDecimal.ZERO : rider.getTotalIncome())
                .add(task.getFee() == null ? BigDecimal.ZERO : task.getFee()));
        riderMapper.updateById(rider);

        // 订单流转为已完成
        orderService.transition(task.getOrderId(), CommonConstants.ORDER_COMPLETED, "RIDER", "骑手已送达");
        Order order = orderMapper.selectById(task.getOrderId());
        if (order != null && order.getShopId() != null) {
            Shop shop = shopMapper.selectById(order.getShopId());
            if (shop != null) {
                shop.setMonthSales((shop.getMonthSales() == null ? 0 : shop.getMonthSales()) + 1);
                shopMapper.updateById(shop);
            }
        }
    }

    @Override
    public PageResult<DeliveryTask> listTasks(Long userId, String scope, int page, int size) {
        Rider rider = requireApprovedRider(userId);
        LambdaQueryWrapper<DeliveryTask> wrapper = new LambdaQueryWrapper<DeliveryTask>()
                .eq(DeliveryTask::getRiderId, rider.getId());
        if ("ongoing".equalsIgnoreCase(scope)) {
            wrapper.in(DeliveryTask::getStatus, CommonConstants.TASK_GRABBED, CommonConstants.TASK_PICKED);
        } else {
            wrapper.eq(DeliveryTask::getStatus, CommonConstants.TASK_DELIVERED);
        }
        wrapper.orderByDesc(DeliveryTask::getId);
        Page<DeliveryTask> result = taskMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(this::fillOrderNo);
        return PageResult.of(result);
    }

    @Override
    public Map<String, Object> stats(Long userId) {
        Rider rider = requireApprovedRider(userId);

        List<DeliveryTask> delivered = taskMapper.selectList(new LambdaQueryWrapper<DeliveryTask>()
                .eq(DeliveryTask::getRiderId, rider.getId())
                .eq(DeliveryTask::getStatus, CommonConstants.TASK_DELIVERED));
        List<DeliveryTask> today = delivered.stream()
                .filter(t -> t.getDeliveredAt() != null
                        && t.getDeliveredAt().toLocalDate().equals(LocalDate.now()))
                .toList();

        BigDecimal totalIncome = delivered.stream()
                .map(t -> t.getFee() == null ? BigDecimal.ZERO : t.getFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todayIncome = today.stream()
                .map(t -> t.getFee() == null ? BigDecimal.ZERO : t.getFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", delivered.size());
        stats.put("totalIncome", totalIncome.setScale(2, RoundingMode.HALF_UP));
        stats.put("todayOrders", today.size());
        stats.put("todayIncome", todayIncome.setScale(2, RoundingMode.HALF_UP));
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeliveryTask createTask(Long orderId, Long shopId, String pickupAddress,
                                   String deliveryAddress, Double distance) {
        DeliveryTask exists = taskMapper.selectOne(new LambdaQueryWrapper<DeliveryTask>()
                .eq(DeliveryTask::getOrderId, orderId)
                .last("LIMIT 1"));
        if (exists != null) {
            return exists;
        }
        DeliveryTask task = new DeliveryTask();
        task.setOrderId(orderId);
        task.setShopId(shopId);
        task.setStatus(CommonConstants.TASK_WAITING);
        task.setPickupAddress(pickupAddress);
        task.setDeliveryAddress(deliveryAddress);
        BigDecimal km = distance == null ? BigDecimal.ZERO : BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
        task.setDistance(km);
        task.setFee(BASE_FEE.add(PER_KM_FEE.multiply(km)).setScale(2, RoundingMode.HALF_UP));
        task.setVersion(0);
        taskMapper.insert(task);
        log.info("生成配送任务 taskId={} orderId={} fee={}", task.getId(), orderId, task.getFee());
        return task;
    }

    private DeliveryTask requireOwnedTask(Long riderId, Long taskId) {
        DeliveryTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw BizException.of(ErrorCode.DELIVERY_NOT_FOUND);
        }
        if (!riderId.equals(task.getRiderId())) {
            throw BizException.of(ErrorCode.DATA_NOT_OWNED);
        }
        return task;
    }

    private void fillOrderNo(DeliveryTask task) {
        if (task.getOrderId() == null) {
            return;
        }
        Order order = orderMapper.selectById(task.getOrderId());
        if (order != null) {
            task.setOrderNo(order.getOrderNo());
        }
        if (task.getShopId() != null) {
            Shop shop = shopMapper.selectById(task.getShopId());
            if (shop != null) {
                task.setShopName(shop.getName());
            }
        }
    }
}
