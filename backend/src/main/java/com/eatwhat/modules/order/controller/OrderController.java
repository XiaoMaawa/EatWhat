package com.eatwhat.modules.order.controller;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.common.result.R;
import com.eatwhat.common.utils.UserContext;
import com.eatwhat.modules.order.dto.CreateOrderRequest;
import com.eatwhat.modules.order.dto.PayCallbackRequest;
import com.eatwhat.modules.order.dto.UpdateStatusRequest;
import com.eatwhat.modules.order.entity.Order;
import com.eatwhat.modules.order.service.OrderService;
import com.eatwhat.modules.order.vo.OrderDetailVO;
import com.eatwhat.security.PublicApi;
import com.eatwhat.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 订单接口。顾客端为主，详情与状态变更对商家/骑手/管理员同样开放。
 */
@Tag(name = "订单")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "创建订单")
    @RequireRole("CONSUMER")
    @PostMapping
    public R<Map<String, Object>> create(@Valid @RequestBody CreateOrderRequest request) {
        return R.ok(orderService.create(UserContext.userId(), request));
    }

    @Operation(summary = "发起支付")
    @RequireRole("CONSUMER")
    @PostMapping("/{id}/pay")
    public R<Map<String, Object>> pay(@PathVariable Long id) {
        return R.ok(orderService.pay(UserContext.userId(), id));
    }

    @Operation(summary = "支付回调")
    @PublicApi
    @PostMapping("/pay/callback")
    public R<Void> payCallback(@Valid @RequestBody PayCallbackRequest request) {
        orderService.payCallback(request.getOrderNo(), request.getTradeNo(), request.getAmount());
        return R.ok();
    }

    @Operation(summary = "我的订单列表")
    @RequireRole("CONSUMER")
    @GetMapping
    public R<PageResult<Order>> list(@RequestParam(required = false) Integer status,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return R.ok(orderService.pageMyOrders(UserContext.userId(), status, page, size));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<OrderDetailVO> detail(@PathVariable Long id) {
        return R.ok(orderService.detail(UserContext.userId(), UserContext.role(), id));
    }

    @Operation(summary = "变更订单状态")
    @PatchMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        orderService.transition(id, request.getTargetStatus(), UserContext.role(), request.getReason());
        return R.ok();
    }

    @Operation(summary = "取消订单")
    @RequireRole("CONSUMER")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id,
                          @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        orderService.cancel(UserContext.userId(), id, reason);
        return R.ok();
    }

    @Operation(summary = "确认收货")
    @RequireRole("CONSUMER")
    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) {
        orderService.confirm(UserContext.userId(), id);
        return R.ok();
    }
}
