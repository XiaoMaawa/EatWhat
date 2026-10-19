package com.eatwhat.modules.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.delivery.dto.DeliveryExceptionRequest;
import com.eatwhat.modules.delivery.entity.DeliveryException;
import com.eatwhat.modules.delivery.entity.DeliveryTask;
import com.eatwhat.modules.delivery.entity.Rider;
import com.eatwhat.modules.delivery.mapper.DeliveryExceptionMapper;
import com.eatwhat.modules.delivery.mapper.DeliveryTaskMapper;
import com.eatwhat.modules.delivery.mapper.RiderMapper;
import com.eatwhat.modules.delivery.service.DeliveryExceptionService;
import com.eatwhat.modules.delivery.service.DeliveryService;
import com.eatwhat.modules.message.service.MessageService;
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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配送异常与转单申请服务实现（docs/09 骑手-19、骑手-20）。
 */
@Service
public class DeliveryExceptionServiceImpl implements DeliveryExceptionService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryExceptionServiceImpl.class);

    private final DeliveryExceptionMapper exceptionMapper;
    private final DeliveryTaskMapper taskMapper;
    private final RiderMapper riderMapper;
    private final OrderMapper orderMapper;
    private final ShopMapper shopMapper;
    private final DeliveryService deliveryService;
    private final OrderService orderService;
    private final MessageService messageService;

    public DeliveryExceptionServiceImpl(DeliveryExceptionMapper exceptionMapper,
                                        DeliveryTaskMapper taskMapper,
                                        RiderMapper riderMapper,
                                        OrderMapper orderMapper,
                                        ShopMapper shopMapper,
                                        @Lazy DeliveryService deliveryService,
                                        @Lazy OrderService orderService,
                                        @Lazy MessageService messageService) {
        this.exceptionMapper = exceptionMapper;
        this.taskMapper = taskMapper;
        this.riderMapper = riderMapper;
        this.orderMapper = orderMapper;
        this.shopMapper = shopMapper;
        this.deliveryService = deliveryService;
        this.orderService = orderService;
        this.messageService = messageService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeliveryException transfer(Long userId, Long taskId, DeliveryExceptionRequest request) {
        Rider rider = deliveryService.requireApprovedRider(userId);
        DeliveryTask task = requireOwnedTask(rider.getId(), taskId);
        if (task.getStatus() == null
                || (task.getStatus() != CommonConstants.TASK_GRABBED
                && task.getStatus() != CommonConstants.TASK_PICKED)) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "只有已接单或已取餐的任务可以转单");
        }
        // 原子退回订单池，其他骑手可重新抢单
        int rows = taskMapper.release(taskId, rider.getId());
        if (rows == 0) {
            throw BizException.of(ErrorCode.TASK_ALREADY_TAKEN, "任务状态已变更，请刷新后重试");
        }
        // 订单回到待配送
        orderService.transition(task.getOrderId(), CommonConstants.ORDER_PENDING_DELIVERY,
                "SYSTEM", "骑手转单，订单退回配送池");

        DeliveryException record = buildRecord(task, rider.getId(),
                request.getType() == null ? DeliveryException.TYPE_TRANSFER : request.getType(),
                request, DeliveryException.TYPE_TRANSFER);

        Order order = orderMapper.selectById(task.getOrderId());
        if (order != null) {
            notifyCustomer(order, "配送员已转单",
                    "订单 " + order.getOrderNo() + " 的配送已转交给其他骑手，预计送达时间可能会略有延后。");
        }
        log.info("骑手转单 riderId={} taskId={} orderId={}", rider.getId(), taskId, task.getOrderId());
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeliveryException report(Long userId, Long taskId, DeliveryExceptionRequest request) {
        Rider rider = deliveryService.requireApprovedRider(userId);
        DeliveryTask task = requireOwnedTask(rider.getId(), taskId);
        DeliveryException record = buildRecord(task, rider.getId(),
                request.getType() == null ? DeliveryException.TYPE_REPORT : request.getType(),
                request, DeliveryException.TYPE_REPORT);

        Order order = orderMapper.selectById(task.getOrderId());
        if (order != null) {
            notifyCustomer(order, "配送异常提醒",
                    "骑手反馈订单 " + order.getOrderNo() + " 出现异常（" + record.getReasonDesc()
                            + "）：" + record.getDescription() + "，平台已介入处理。");
        }
        log.info("骑手异常上报 riderId={} taskId={} reasonType={}", rider.getId(), taskId, record.getReasonType());
        return record;
    }

    @Override
    public PageResult<DeliveryException> pageMine(Long userId, Integer type, int page, int size) {
        Rider rider = deliveryService.requireApprovedRider(userId);
        LambdaQueryWrapper<DeliveryException> wrapper = new LambdaQueryWrapper<DeliveryException>()
                .eq(DeliveryException::getRiderId, rider.getId());
        if (type != null) {
            wrapper.eq(DeliveryException::getType, type);
        }
        wrapper.orderByDesc(DeliveryException::getId);
        Page<DeliveryException> result = exceptionMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result);
    }

    @Override
    public PageResult<DeliveryException> pageAll(Integer type, Integer status, String keyword, int page, int size) {
        LambdaQueryWrapper<DeliveryException> wrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            wrapper.eq(DeliveryException::getType, type);
        }
        if (status != null) {
            wrapper.eq(DeliveryException::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(DeliveryException::getOrderNo, kw)
                    .or().like(DeliveryException::getDescription, kw));
        }
        wrapper.orderByDesc(DeliveryException::getId);
        Page<DeliveryException> result = exceptionMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(this::fillExtras);
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long adminId, Long exceptionId, String remark) {
        DeliveryException record = exceptionMapper.selectById(exceptionId);
        if (record == null) {
            throw BizException.of(ErrorCode.DELIVERY_NOT_FOUND, "异常记录不存在");
        }
        if (Integer.valueOf(DeliveryException.STATUS_HANDLED).equals(record.getStatus())) {
            throw BizException.of(ErrorCode.DATA_CONFLICT, "该记录已处理，无需重复操作");
        }
        DeliveryException update = new DeliveryException();
        update.setId(exceptionId);
        update.setStatus(DeliveryException.STATUS_HANDLED);
        update.setHandleRemark(remark == null || remark.isBlank() ? "平台已受理" : remark.trim());
        update.setHandledAt(LocalDateTime.now());
        exceptionMapper.updateById(update);
        log.info("管理员处理配送异常 adminId={} exceptionId={}", adminId, exceptionId);
    }

    private DeliveryException buildRecord(DeliveryTask task, Long riderId, Integer type,
                                          DeliveryExceptionRequest request, int fallbackType) {
        DeliveryException record = new DeliveryException();
        record.setTaskId(task.getId());
        record.setOrderId(task.getOrderId());
        record.setRiderId(riderId);
        record.setType(type == null ? fallbackType : type);
        record.setReasonType(request.getReasonType() == null ? 6 : request.getReasonType());
        record.setDescription(request.getDescription().trim());
        record.setStatus(DeliveryException.STATUS_PENDING);
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            record.setImages(String.join(",", request.getImages()));
        }
        Order order = orderMapper.selectById(task.getOrderId());
        if (order != null) {
            record.setOrderNo(order.getOrderNo());
        }
        exceptionMapper.insert(record);
        return record;
    }

    private void notifyCustomer(Order order, String title, String content) {
        try {
            messageService.send(order.getUserId(), 1, title, content, order.getId(), order.getOrderNo(), null);
        } catch (Exception e) {
            log.warn("发送配送消息失败 orderId={} msg={}", order.getId(), e.getMessage());
        }
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

    private void fillExtras(DeliveryException record) {
        if (record.getRiderId() != null) {
            Rider rider = riderMapper.selectById(record.getRiderId());
            if (rider != null) {
                record.setRiderName(rider.getRealName());
            }
        }
        if (record.getOrderId() != null) {
            Order order = orderMapper.selectById(record.getOrderId());
            if (order != null && order.getShopId() != null) {
                Shop shop = shopMapper.selectById(order.getShopId());
                if (shop != null) {
                    record.setShopName(shop.getName());
                }
            }
        }
    }
}
