package com.eatwhat.modules.delivery.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import com.eatwhat.modules.delivery.service.DeliveryService;
import com.eatwhat.modules.message.service.MessageService;
import com.eatwhat.modules.order.entity.Order;
import com.eatwhat.modules.order.mapper.OrderMapper;
import com.eatwhat.modules.order.service.OrderService;
import com.eatwhat.modules.shop.mapper.ShopMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 配送异常与转单服务单元测试。
 * 覆盖转单的任务归属/状态前置/原子退回、异常上报、分页与管理员处理。
 */
@DisplayName("配送异常与转单服务")
class DeliveryExceptionServiceImplTest {

    private static final Long USER_ID = 20L;
    private static final Long RIDER_ID = 3L;
    private static final Long TASK_ID = 9L;
    private static final Long ORDER_ID = 17L;
    private static final Long SHOP_ID = 1L;
    private static final Long EXCEPTION_ID = 7L;

    private DeliveryExceptionMapper exceptionMapper;
    private DeliveryTaskMapper taskMapper;
    private RiderMapper riderMapper;
    private OrderMapper orderMapper;
    private DeliveryService deliveryService;
    private OrderService orderService;
    private MessageService messageService;
    private DeliveryExceptionServiceImpl service;

    @BeforeAll
    static void initMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.eatwhat.modules.delivery.mapper.DeliveryExceptionMapper");
        TableInfoHelper.initTableInfo(assistant, DeliveryException.class);
    }

    @BeforeEach
    void setUp() {
        exceptionMapper = mock(DeliveryExceptionMapper.class);
        taskMapper = mock(DeliveryTaskMapper.class);
        riderMapper = mock(RiderMapper.class);
        orderMapper = mock(OrderMapper.class);
        ShopMapper shopMapper = mock(ShopMapper.class);
        deliveryService = mock(DeliveryService.class);
        orderService = mock(OrderService.class);
        messageService = mock(MessageService.class);

        service = new DeliveryExceptionServiceImpl(exceptionMapper, taskMapper, riderMapper,
                orderMapper, shopMapper, deliveryService, orderService, messageService);
    }

    private Rider rider() {
        Rider rider = new Rider();
        rider.setId(RIDER_ID);
        rider.setUserId(USER_ID);
        rider.setRealName("王骑手");
        rider.setAuditStatus(CommonConstants.AUDIT_PASSED);
        return rider;
    }

    private DeliveryTask task(int status, Long riderId) {
        DeliveryTask task = new DeliveryTask();
        task.setId(TASK_ID);
        task.setOrderId(ORDER_ID);
        task.setShopId(SHOP_ID);
        task.setRiderId(riderId);
        task.setStatus(status);
        task.setDistance(BigDecimal.valueOf(1.2));
        task.setFee(BigDecimal.valueOf(5));
        return task;
    }

    private Order order() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setOrderNo("EW20261019202511474098");
        order.setUserId(2L);
        order.setShopId(SHOP_ID);
        order.setStatus(CommonConstants.ORDER_DELIVERING);
        return order;
    }

    private DeliveryExceptionRequest request(Integer type, Integer reasonType, String description) {
        DeliveryExceptionRequest request = new DeliveryExceptionRequest();
        request.setType(type);
        request.setReasonType(reasonType);
        request.setDescription(description);
        return request;
    }

    @Test
    @DisplayName("转单：任务退回订单池并把订单回退到待配送")
    void transferReturnsTaskToPool() {
        when(deliveryService.requireApprovedRider(USER_ID)).thenReturn(rider());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task(CommonConstants.TASK_PICKED, RIDER_ID));
        when(taskMapper.release(TASK_ID, RIDER_ID)).thenReturn(1);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order());

        DeliveryException result = service.transfer(USER_ID, TASK_ID,
                request(null, 2, "电动车爆胎，需要转交其他骑手配送"));

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(DeliveryException.TYPE_TRANSFER);
        assertThat(result.getReasonType()).isEqualTo(2);
        assertThat(result.getOrderNo()).isEqualTo("EW20261019202511474098");
        assertThat(result.getStatus()).isEqualTo(DeliveryException.STATUS_PENDING);
        verify(orderService).transition(eq(ORDER_ID), eq(CommonConstants.ORDER_PENDING_DELIVERY), eq("SYSTEM"), anyString());
        verify(messageService).send(eq(2L), eq(1), anyString(), anyString(), eq(ORDER_ID), anyString(), eq(null));
    }

    @Test
    @DisplayName("转单：他人任务抛无权操作")
    void transferRejectsOtherRiderTask() {
        when(deliveryService.requireApprovedRider(USER_ID)).thenReturn(rider());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task(CommonConstants.TASK_GRABBED, 99L));

        assertThatThrownBy(() -> service.transfer(USER_ID, TASK_ID, request(null, 2, "电动车爆胎了")))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.DATA_NOT_OWNED.getCode());
        verify(taskMapper, never()).release(anyLong(), anyLong());
    }

    @Test
    @DisplayName("转单：待抢单任务不允许转单")
    void transferRejectsWaitingTask() {
        when(deliveryService.requireApprovedRider(USER_ID)).thenReturn(rider());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task(CommonConstants.TASK_WAITING, RIDER_ID));

        assertThatThrownBy(() -> service.transfer(USER_ID, TASK_ID, request(null, 2, "电动车爆胎了")))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.ORDER_STATUS_ILLEGAL.getCode());
    }

    @Test
    @DisplayName("转单：任务已被他人抢走时抛冲突提示")
    void transferRejectsWhenTaskAlreadyTaken() {
        when(deliveryService.requireApprovedRider(USER_ID)).thenReturn(rider());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task(CommonConstants.TASK_GRABBED, RIDER_ID));
        when(taskMapper.release(TASK_ID, RIDER_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.transfer(USER_ID, TASK_ID, request(null, 2, "电动车爆胎了")))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.TASK_ALREADY_TAKEN.getCode());
        verify(orderService, never()).transition(anyLong(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("转单：显式传 type=2 时按异常上报落库")
    void transferRespectsExplicitType() {
        when(deliveryService.requireApprovedRider(USER_ID)).thenReturn(rider());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task(CommonConstants.TASK_GRABBED, RIDER_ID));
        when(taskMapper.release(TASK_ID, RIDER_ID)).thenReturn(1);
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order());

        DeliveryException result = service.transfer(USER_ID, TASK_ID,
                request(DeliveryException.TYPE_REPORT, 1, "联系不上顾客，电话无人接听"));

        assertThat(result.getType()).isEqualTo(DeliveryException.TYPE_REPORT);
    }

    @Test
    @DisplayName("异常上报：只记录不改变任务状态")
    void reportDoesNotTouchTaskStatus() {
        when(deliveryService.requireApprovedRider(USER_ID)).thenReturn(rider());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task(CommonConstants.TASK_PICKED, RIDER_ID));
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order());

        DeliveryException result = service.report(USER_ID, TASK_ID,
                request(null, 3, "餐品在配送途中洒出，需要联系商家"));

        assertThat(result.getType()).isEqualTo(DeliveryException.TYPE_REPORT);
        assertThat(result.getStatus()).isEqualTo(DeliveryException.STATUS_PENDING);
        verify(taskMapper, never()).release(anyLong(), anyLong());
        verify(orderService, never()).transition(anyLong(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("异常上报：任务不存在时抛业务异常")
    void reportRejectsMissingTask() {
        when(deliveryService.requireApprovedRider(USER_ID)).thenReturn(rider());
        when(taskMapper.selectById(TASK_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.report(USER_ID, TASK_ID, request(null, 3, "餐品洒了需要处理")))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.DELIVERY_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("异常上报：情况说明两端空白会被去除")
    void reportTrimsDescription() {
        when(deliveryService.requireApprovedRider(USER_ID)).thenReturn(rider());
        when(taskMapper.selectById(TASK_ID)).thenReturn(task(CommonConstants.TASK_PICKED, RIDER_ID));
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order());

        DeliveryException result = service.report(USER_ID, TASK_ID,
                request(null, 3, "  餐品洒了，需要联系商家处理  "));

        assertThat(result.getDescription()).isEqualTo("餐品洒了，需要联系商家处理");
    }

    @Test
    @DisplayName("我的记录：按骑手过滤并透传分页")
    void pageMineFiltersByRider() {
        when(deliveryService.requireApprovedRider(USER_ID)).thenReturn(rider());
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<DeliveryException> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(List.of(new DeliveryException()));
        page.setTotal(1);
        when(exceptionMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<DeliveryException> result = service.pageMine(USER_ID, DeliveryException.TYPE_TRANSFER, 1, 10);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
    }

    @Test
    @DisplayName("管理端列表：回填骑手名与店铺名")
    void pageAllFillsExtras() {
        DeliveryException record = new DeliveryException();
        record.setId(EXCEPTION_ID);
        record.setRiderId(RIDER_ID);
        record.setOrderId(ORDER_ID);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<DeliveryException> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(List.of(record));
        page.setTotal(1);
        when(exceptionMapper.selectPage(any(), any())).thenReturn(page);
        when(riderMapper.selectById(RIDER_ID)).thenReturn(rider());
        when(orderMapper.selectById(ORDER_ID)).thenReturn(order());
        when(orderMapper.selectBatchIds(any())).thenReturn(List.of());

        PageResult<DeliveryException> result = service.pageAll(null, null, null, 1, 10);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getRiderName()).isEqualTo("王骑手");
    }

    @Test
    @DisplayName("管理员处理：写入处理说明与处理时间")
    void handleWritesRemark() {
        DeliveryException record = new DeliveryException();
        record.setId(EXCEPTION_ID);
        record.setStatus(DeliveryException.STATUS_PENDING);
        when(exceptionMapper.selectById(EXCEPTION_ID)).thenReturn(record);

        service.handle(1L, EXCEPTION_ID, "已联系骑手核实，异常情况属实");

        verify(exceptionMapper).updateById(any(DeliveryException.class));
    }

    @Test
    @DisplayName("管理员处理：重复处理被拦截")
    void handleRejectsDuplicate() {
        DeliveryException record = new DeliveryException();
        record.setId(EXCEPTION_ID);
        record.setStatus(DeliveryException.STATUS_HANDLED);
        when(exceptionMapper.selectById(EXCEPTION_ID)).thenReturn(record);

        assertThatThrownBy(() -> service.handle(1L, EXCEPTION_ID, "再次处理"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.DATA_CONFLICT.getCode());
        verify(exceptionMapper, never()).updateById(any(DeliveryException.class));
    }

    @Test
    @DisplayName("管理员处理：记录不存在时抛业务异常")
    void handleRejectsMissingRecord() {
        when(exceptionMapper.selectById(EXCEPTION_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.handle(1L, EXCEPTION_ID, "处理"))
                .isInstanceOf(BizException.class)
                .extracting("code").isEqualTo(ErrorCode.DELIVERY_NOT_FOUND.getCode());
    }
}
