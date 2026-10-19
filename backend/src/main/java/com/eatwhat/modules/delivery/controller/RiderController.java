package com.eatwhat.modules.delivery.controller;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.common.result.R;
import com.eatwhat.common.utils.UserContext;
import com.eatwhat.modules.delivery.dto.DeliveryExceptionRequest;
import com.eatwhat.modules.delivery.dto.RiderApplyRequest;
import com.eatwhat.modules.delivery.entity.DeliveryException;
import com.eatwhat.modules.delivery.entity.DeliveryTask;
import com.eatwhat.modules.delivery.entity.Rider;
import com.eatwhat.modules.delivery.service.DeliveryExceptionService;
import com.eatwhat.modules.delivery.service.DeliveryService;
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

import java.util.List;
import java.util.Map;

/**
 * 骑手端接口。
 */
@Tag(name = "骑手端")
@RequireRole({"RIDER", "ADMIN"})
@RestController
@RequestMapping("/api/v1/rider")
public class RiderController {

    private final DeliveryService deliveryService;
    private final DeliveryExceptionService deliveryExceptionService;

    public RiderController(DeliveryService deliveryService,
                           DeliveryExceptionService deliveryExceptionService) {
        this.deliveryService = deliveryService;
        this.deliveryExceptionService = deliveryExceptionService;
    }

    @Operation(summary = "提交骑手认证")
    @PostMapping("/apply")
    public R<Rider> apply(@Valid @RequestBody RiderApplyRequest request) {
        return R.ok(deliveryService.apply(UserContext.userId(), request));
    }

    @Operation(summary = "查看认证状态")
    @GetMapping("/profile")
    public R<Rider> profile() {
        return R.ok(deliveryService.profile(UserContext.userId()));
    }

    @Operation(summary = "可抢任务列表")
    @GetMapping("/tasks/available")
    public R<List<DeliveryTask>> available(@RequestParam(required = false) String sortBy,
                                           @RequestParam(required = false) java.math.BigDecimal minDistance,
                                           @RequestParam(required = false) java.math.BigDecimal maxDistance,
                                           @RequestParam(required = false) java.math.BigDecimal minFee,
                                           @RequestParam(required = false) java.math.BigDecimal maxFee) {
        return R.ok(deliveryService.availableTasks(UserContext.userId(), sortBy, minDistance, maxDistance, minFee, maxFee));
    }

    @Operation(summary = "更新接单设置")
    @PatchMapping("/settings")
    public R<Void> updateSettings(@RequestBody Map<String, Object> body) {
        Integer accepting = body.get("accepting") == null ? null
                : Integer.valueOf(body.get("accepting").toString());
        java.math.BigDecimal maxDistance = body.get("maxDistance") == null ? null
                : new java.math.BigDecimal(body.get("maxDistance").toString());
        Integer maxOrders = body.get("maxOrders") == null ? null
                : Integer.valueOf(body.get("maxOrders").toString());
        deliveryService.updateSettings(UserContext.userId(), accepting, maxDistance, maxOrders);
        return R.ok();
    }

    @Operation(summary = "抢单")
    @PostMapping("/tasks/{taskId}/grab")
    public R<Map<String, Object>> grab(@PathVariable Long taskId) {
        deliveryService.grab(UserContext.userId(), taskId);
        return R.ok(Map.of("taskId", taskId, "status", 1));
    }

    @Operation(summary = "取餐")
    @PatchMapping("/tasks/{taskId}/pickup")
    public R<Void> pickup(@PathVariable Long taskId) {
        deliveryService.pickup(UserContext.userId(), taskId);
        return R.ok();
    }

    @Operation(summary = "送达")
    @PatchMapping("/tasks/{taskId}/deliver")
    public R<Void> deliver(@PathVariable Long taskId) {
        deliveryService.deliver(UserContext.userId(), taskId);
        return R.ok();
    }

    @Operation(summary = "我的任务列表")
    @GetMapping("/tasks")
    public R<PageResult<DeliveryTask>> tasks(@RequestParam(defaultValue = "ongoing") String status,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return R.ok(deliveryService.listTasks(UserContext.userId(), status, page, size));
    }

    @Operation(summary = "骑手统计")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(deliveryService.stats(UserContext.userId()));
    }

    @Operation(summary = "收入明细（按订单逐单，含准时标记与结算状态）")
    @GetMapping("/income-detail")
    public R<PageResult<DeliveryTask>> incomeDetail(@RequestParam(required = false) String startDate,
                                                    @RequestParam(required = false) String endDate,
                                                    @RequestParam(required = false) Integer onTime,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return R.ok(deliveryService.incomeDetail(UserContext.userId(), startDate, endDate, onTime, page, size));
    }

    @Operation(summary = "转单申请")
    @PostMapping("/tasks/{taskId}/transfer")
    public R<DeliveryException> transfer(@PathVariable Long taskId,
                                         @Valid @RequestBody DeliveryExceptionRequest request) {
        return R.ok(deliveryExceptionService.transfer(UserContext.userId(), taskId, request));
    }

    @Operation(summary = "配送异常上报")
    @PostMapping("/tasks/{taskId}/exception")
    public R<DeliveryException> reportException(@PathVariable Long taskId,
                                                @Valid @RequestBody DeliveryExceptionRequest request) {
        return R.ok(deliveryExceptionService.report(UserContext.userId(), taskId, request));
    }

    @Operation(summary = "我提交的异常与转单记录")
    @GetMapping("/exceptions")
    public R<PageResult<DeliveryException>> exceptions(@RequestParam(required = false) Integer type,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return R.ok(deliveryExceptionService.pageMine(UserContext.userId(), type, page, size));
    }
}
