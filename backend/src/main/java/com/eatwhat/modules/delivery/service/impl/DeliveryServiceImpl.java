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
import java.util.ArrayList;
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

    /** 骑手未设置时的默认接单半径（公里） */
    private static final BigDecimal DEFAULT_MAX_DISTANCE = BigDecimal.valueOf(5.0);

    /** 骑手未设置时的默认同时接单上限 */
    private static final int DEFAULT_MAX_ORDERS = 5;

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
    public List<DeliveryTask> availableTasks(Long userId, String sortBy, BigDecimal minDistance,
                                              BigDecimal maxDistance, BigDecimal minFee, BigDecimal maxFee) {
        Rider rider = requireApprovedRider(userId);
        if (!Integer.valueOf(1).equals(rider.getAccepting())) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "已暂停接单，请先在「骑手认证」页开启接单开关");
        }

        BigDecimal radius = rider.getMaxDistance() == null ? DEFAULT_MAX_DISTANCE : rider.getMaxDistance();
        int maxOrders = rider.getMaxOrders() == null ? DEFAULT_MAX_ORDERS : rider.getMaxOrders();
        long ongoing = taskMapper.selectCount(new LambdaQueryWrapper<DeliveryTask>()
                .eq(DeliveryTask::getRiderId, rider.getId())
                .in(DeliveryTask::getStatus, CommonConstants.TASK_GRABBED, CommonConstants.TASK_PICKED));

        List<DeliveryTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<DeliveryTask>()
                .eq(DeliveryTask::getStatus, CommonConstants.TASK_WAITING)
                .isNull(DeliveryTask::getRiderId)
                .orderByAsc(DeliveryTask::getId));

        // 按接单半径过滤，超距订单不展示
        tasks = tasks.stream()
                .filter(task -> task.getDistance() == null
                        || task.getDistance().compareTo(radius) <= 0)
                .collect(java.util.stream.Collectors.toList());

        // 骑手-13：按距离 / 收益区间筛选
        tasks = tasks.stream()
                .filter(task -> minDistance == null || task.getDistance() == null
                        || task.getDistance().compareTo(minDistance) >= 0)
                .filter(task -> maxDistance == null || task.getDistance() == null
                        || task.getDistance().compareTo(maxDistance) <= 0)
                .filter(task -> minFee == null || task.getFee() == null
                        || task.getFee().compareTo(minFee) >= 0)
                .filter(task -> maxFee == null || task.getFee() == null
                        || task.getFee().compareTo(maxFee) <= 0)
                .collect(java.util.stream.Collectors.toList());

        // 骑手-09：接单偏好排序（distance 距离优先 / fee 收益优先）
        java.util.Comparator<DeliveryTask> comparator = null;
        if ("distance".equalsIgnoreCase(sortBy)) {
            comparator = java.util.Comparator.comparing(DeliveryTask::getDistance,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
        } else if ("fee".equalsIgnoreCase(sortBy)) {
            comparator = java.util.Comparator.comparing(DeliveryTask::getFee,
                    java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()));
        }
        if (comparator != null) {
            tasks.sort(comparator);
        }

        // 已达同时接单上限时不再展示新单
        if (ongoing >= maxOrders) {
            return new ArrayList<>();
        }
        tasks.forEach(this::fillOrderNo);
        return tasks;
    }

    @Override
    public void updateSettings(Long userId, Integer accepting, BigDecimal maxDistance, Integer maxOrders) {
        Rider rider = requireApprovedRider(userId);
        Rider update = new Rider();
        update.setId(rider.getId());
        if (accepting != null) {
            if (accepting != 0 && accepting != 1) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "接单开关取值只能是 0 或 1");
            }
            update.setAccepting(accepting);
        }
        if (maxDistance != null) {
            if (maxDistance.compareTo(BigDecimal.ONE) < 0 || maxDistance.compareTo(BigDecimal.TEN) > 0) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "接单半径需在 1 至 10 公里之间");
            }
            update.setMaxDistance(maxDistance);
        }
        if (maxOrders != null) {
            if (maxOrders < 1 || maxOrders > 10) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "同时接单数需在 1 至 10 之间");
            }
            update.setMaxOrders(maxOrders);
        }
        riderMapper.updateById(update);
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
        // 抢单即给出预计送达时间（30 分钟备餐 + 每公里 3 分钟）。
        // 注意：grab 的原生 SQL 已把 version 自增，这里不能再用 updateById（乐观锁会匹配不到），改用条件更新。
        taskMapper.updateEta(taskId, estimateArrival(task));
        // 同步订单进入配送中
        orderService.transition(task.getOrderId(), CommonConstants.ORDER_DELIVERING, "RIDER", "骑手已接单");
    }

    /** 预计送达 = 当前时间 + 30 分钟备餐 + 每公里 3 分钟（不足 1 公里按 1 公里） */
    private java.time.LocalDateTime estimateArrival(DeliveryTask task) {
        int minutes = 30;
        BigDecimal distance = task.getDistance() == null ? BigDecimal.ONE : task.getDistance();
        int km = Math.max(1, distance.setScale(0, java.math.RoundingMode.CEILING).intValue());
        minutes += km * 3;
        return java.time.LocalDateTime.now().plusMinutes(minutes);
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
        java.time.LocalDateTime deliveredAt = java.time.LocalDateTime.now();
        task.setDeliveredAt(deliveredAt);
        // 准时送达：实际送达时间不晚于预计送达时间
        boolean punctual = task.getEstimatedArrivalAt() == null
                || !deliveredAt.isAfter(task.getEstimatedArrivalAt());
        task.setOnTime(punctual ? 1 : 0);
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

        // 本周 / 本月收入与单量
        LocalDate todayDate = LocalDate.now();
        LocalDate weekStart = todayDate.with(java.time.DayOfWeek.MONDAY);
        LocalDate monthStart = todayDate.withDayOfMonth(1);

        BigDecimal weekIncome = sumFee(delivered, d -> !d.getDeliveredAt().toLocalDate().isBefore(weekStart));
        int weekOrders = countDelivered(delivered, d -> !d.getDeliveredAt().toLocalDate().isBefore(weekStart));
        BigDecimal monthIncome = sumFee(delivered, d -> !d.getDeliveredAt().toLocalDate().isBefore(monthStart));
        int monthOrders = countDelivered(delivered, d -> !d.getDeliveredAt().toLocalDate().isBefore(monthStart));

        stats.put("weekOrders", weekOrders);
        stats.put("weekIncome", weekIncome.setScale(2, RoundingMode.HALF_UP));
        stats.put("monthOrders", monthOrders);
        stats.put("monthIncome", monthIncome.setScale(2, RoundingMode.HALF_UP));

        // 近 7 日收入趋势
        java.util.List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = todayDate.minusDays(i);
            List<DeliveryTask> dayTasks = delivered.stream()
                    .filter(t -> t.getDeliveredAt() != null
                            && t.getDeliveredAt().toLocalDate().equals(day))
                    .toList();
            BigDecimal dayIncome = dayTasks.stream()
                    .map(t -> t.getFee() == null ? BigDecimal.ZERO : t.getFee())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> point = new HashMap<>();
            point.put("date", day.toString());
            point.put("orders", dayTasks.size());
            point.put("income", dayIncome.setScale(2, RoundingMode.HALF_UP));
            trend.add(point);
        }
        stats.put("incomeTrend", trend);

        // 准时送达率（骑手-26）
        long onTimeCount = delivered.stream()
                .filter(t -> Integer.valueOf(1).equals(t.getOnTime()))
                .count();
        long lateCount = delivered.stream()
                .filter(t -> Integer.valueOf(0).equals(t.getOnTime()))
                .count();
        int judged = (int) (onTimeCount + lateCount);
        stats.put("onTimeOrders", onTimeCount);
        stats.put("lateOrders", lateCount);
        stats.put("onTimeRate", judged == 0 ? 100.0
                : Math.round(onTimeCount * 1000.0 / judged) / 10.0);

        return stats;
    }

    @Override
    public PageResult<DeliveryTask> incomeDetail(Long userId, String startDate, String endDate,
                                                 Integer onTime, int page, int size) {
        Rider rider = requireApprovedRider(userId);
        LambdaQueryWrapper<DeliveryTask> wrapper = new LambdaQueryWrapper<DeliveryTask>()
                .eq(DeliveryTask::getRiderId, rider.getId())
                .eq(DeliveryTask::getStatus, CommonConstants.TASK_DELIVERED);
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(DeliveryTask::getDeliveredAt, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.lt(DeliveryTask::getDeliveredAt, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }
        if (onTime != null) {
            wrapper.eq(DeliveryTask::getOnTime, onTime);
        }
        wrapper.orderByDesc(DeliveryTask::getDeliveredAt);
        Page<DeliveryTask> result = taskMapper.selectPage(new Page<>(page, Math.min(size, 100)), wrapper);
        // 补订单号，供「结算记录」展示
        for (DeliveryTask task : result.getRecords()) {
            Order order = orderMapper.selectById(task.getOrderId());
            if (order != null) {
                task.setOrderNo(order.getOrderNo());
            }
        }
        return PageResult.of(result);
    }

    private BigDecimal sumFee(List<DeliveryTask> tasks, java.util.function.Predicate<DeliveryTask> filter) {
        return tasks.stream()
                .filter(t -> t.getDeliveredAt() != null)
                .filter(filter)
                .map(t -> t.getFee() == null ? BigDecimal.ZERO : t.getFee())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int countDelivered(List<DeliveryTask> tasks, java.util.function.Predicate<DeliveryTask> filter) {
        return (int) tasks.stream()
                .filter(t -> t.getDeliveredAt() != null)
                .filter(filter)
                .count();
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
