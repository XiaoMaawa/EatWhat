package com.eatwhat.modules.admin.controller;

import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.result.PageResult;
import com.eatwhat.common.result.R;
import com.eatwhat.common.utils.UserContext;
import com.eatwhat.modules.admin.entity.OperationLog;
import com.eatwhat.modules.admin.service.AdminService;
import com.eatwhat.modules.aftersale.entity.AfterSale;
import com.eatwhat.modules.aftersale.service.AfterSaleService;
import com.eatwhat.modules.auth.entity.SysUser;
import com.eatwhat.modules.delivery.entity.DeliveryException;
import com.eatwhat.modules.delivery.entity.Rider;
import com.eatwhat.modules.delivery.service.DeliveryExceptionService;
import com.eatwhat.modules.dish.entity.Dish;
import com.eatwhat.modules.merchant.entity.Merchant;
import com.eatwhat.modules.message.service.MessageService;
import com.eatwhat.modules.order.entity.Order;
import com.eatwhat.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 平台管理端接口。
 */
@Tag(name = "平台管理")
@RequireRole("ADMIN")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;
    private final AfterSaleService afterSaleService;
    private final MessageService messageService;
    private final DeliveryExceptionService deliveryExceptionService;

    public AdminController(AdminService adminService, AfterSaleService afterSaleService,
                           MessageService messageService,
                           DeliveryExceptionService deliveryExceptionService) {
        this.adminService = adminService;
        this.afterSaleService = afterSaleService;
        this.messageService = messageService;
        this.deliveryExceptionService = deliveryExceptionService;
    }

    @Operation(summary = "用户列表")
    @GetMapping("/admins")
    public R<PageResult<SysUser>> admins(@RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return R.ok(adminService.pageAdmins(keyword, status, page, size));
    }

    @PostMapping("/admins")
    public R<SysUser> createAdmin(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String nickname = body.get("nickname");
        String password = body.get("password");
        if (phone == null || phone.isBlank()) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "手机号不能为空");
        }
        if (password == null || password.isBlank()) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "初始密码不能为空");
        }
        return R.ok(adminService.createAdmin(phone.trim(), nickname, password));
    }

    @GetMapping("/users")
    public R<PageResult<SysUser>> users(@RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String role,
                                        @RequestParam(required = false) Integer status,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return R.ok(adminService.pageUsers(keyword, role, status, page, size));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/users/{id}")
    public R<SysUser> user(@PathVariable Long id) {
        return R.ok(adminService.userDetail(id));
    }

    @Operation(summary = "用户历史订单")
    @GetMapping("/users/{id}/orders")
    public R<PageResult<Order>> userOrders(@PathVariable Long id,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return R.ok(adminService.pageUserOrders(id, status, page, size));
    }

    @Operation(summary = "启用/禁用用户")
    @PatchMapping("/users/{id}/status")
    public R<Void> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminService.updateUserStatus(id, body == null ? null : body.get("status"));
        return R.ok();
    }

    @Operation(summary = "商家列表")
    @GetMapping("/merchants")
    public R<PageResult<Merchant>> merchants(@RequestParam(required = false) Integer auditStatus,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return R.ok(adminService.pageMerchants(auditStatus, keyword, page, size));
    }

    @Operation(summary = "商家详情")
    @GetMapping("/merchants/{id}")
    public R<Merchant> merchant(@PathVariable Long id) {
        return R.ok(adminService.merchantDetail(id));
    }

    @Operation(summary = "审核商家资质")
    @PatchMapping("/merchants/{id}/audit")
    public R<Void> auditMerchant(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (body == null) {
            return R.ok();
        }
        Integer auditStatus = body.get("auditStatus") == null ? null
                : Integer.valueOf(body.get("auditStatus").toString());
        Object remark = body.get("auditRemark");
        adminService.auditMerchant(id, auditStatus, remark == null ? null : remark.toString());
        return R.ok();
    }

    @Operation(summary = "冻结/解冻商家")
    @PatchMapping("/merchants/{id}/freeze")
    public R<Void> freezeMerchant(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminService.freezeMerchant(id, body == null ? null : body.get("status"));
        return R.ok();
    }

    @Operation(summary = "骑手列表")
    @GetMapping("/riders")
    public R<PageResult<Rider>> riders(@RequestParam(required = false) Integer auditStatus,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return R.ok(adminService.pageRiders(auditStatus, keyword, page, size));
    }

    @Operation(summary = "骑手详情")
    @GetMapping("/riders/{id}")
    public R<Rider> rider(@PathVariable Long id) {
        return R.ok(adminService.riderDetail(id));
    }

    @Operation(summary = "审核骑手认证")
    @PatchMapping("/riders/{id}/audit")
    public R<Void> auditRider(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (body == null) {
            return R.ok();
        }
        Integer auditStatus = body.get("auditStatus") == null ? null
                : Integer.valueOf(body.get("auditStatus").toString());
        Object remark = body.get("auditRemark");
        adminService.auditRider(id, auditStatus, remark == null ? null : remark.toString());
        return R.ok();
    }

    @Operation(summary = "开启/关闭骑手接单权限")
    @PatchMapping("/riders/{id}/accepting")
    public R<Void> updateRiderAccepting(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminService.updateRiderAccepting(id, body == null ? null : body.get("accepting"));
        return R.ok();
    }

    @Operation(summary = "异常订单筛选")
    @GetMapping("/orders/abnormal")
    public R<PageResult<Order>> abnormalOrders(@RequestParam String type,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return R.ok(adminService.pageAbnormalOrders(type, page, size));
    }

    @Operation(summary = "平台订单列表")
    @GetMapping("/orders")
    public R<PageResult<Order>> orders(@RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return R.ok(adminService.pageOrders(status, keyword, page, size));
    }

    @Operation(summary = "平台订单详情")
    @GetMapping("/orders/{id}")
    public R<Map<String, Object>> order(@PathVariable Long id) {
        return R.ok(adminService.orderDetail(id));
    }

    @Operation(summary = "平台介入订单")
    @PostMapping("/orders/{id}/intervene")
    public R<Void> intervene(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminService.interveneOrder(id, body == null ? null : body.get("action"),
                body == null ? null : body.get("reason"));
        return R.ok();
    }

    @Operation(summary = "配送异常与转单记录")
    @GetMapping("/delivery-exceptions")
    public R<PageResult<DeliveryException>> deliveryExceptions(@RequestParam(required = false) Integer type,
                                                               @RequestParam(required = false) Integer status,
                                                               @RequestParam(required = false) String keyword,
                                                               @RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        return R.ok(deliveryExceptionService.pageAll(type, status, keyword, page, size));
    }

    @Operation(summary = "处理配送异常")
    @PostMapping("/delivery-exceptions/{id}/handle")
    public R<Void> handleDeliveryException(@PathVariable Long id, @RequestBody Map<String, String> body) {
        deliveryExceptionService.handle(UserContext.userId(), id, body == null ? null : body.get("remark"));
        return R.ok();
    }

    @Operation(summary = "平台菜品列表")
    @GetMapping("/dishes")
    public R<PageResult<Dish>> dishes(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Long shopId,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return R.ok(adminService.pageDishes(keyword, shopId, page, size));
    }

    @Operation(summary = "强制下架菜品（需填写违规原因）")
    @PatchMapping("/dishes/{id}/offline")
    public R<Void> offlineDish(@PathVariable Long id,
                               @RequestBody(required = false) Map<String, Object> body) {
        String reason = body == null || body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        adminService.offlineDish(id, reason);
        return R.ok();
    }

    @Operation(summary = "恢复菜品上架")
    @PatchMapping("/dishes/{id}/online")
    public R<Void> onlineDish(@PathVariable Long id) {
        adminService.onlineDish(id);
        return R.ok();
    }

    @Operation(summary = "平台数据看板")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard() {
        return R.ok(adminService.dashboard());
    }

    @Operation(summary = "平台统计看板")
    @GetMapping("/statistics")
    public R<Map<String, Object>> statistics(@RequestParam(defaultValue = "day") String period) {
        return R.ok(adminService.statistics(period));
    }
    @Operation(summary = "操作日志")
    @GetMapping("/operation-logs")
    public R<PageResult<OperationLog>> operationLogs(@RequestParam(required = false) String operatorRole,
                                                     @RequestParam(required = false) String module,
                                                     @RequestParam(required = false) Integer result,
                                                     @RequestParam(required = false) String startDate,
                                                     @RequestParam(required = false) String endDate,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return R.ok(adminService.pageOperationLogs(operatorRole, module, result, startDate, endDate, page, size));
    }

    @Operation(summary = "售后列表")
    @GetMapping("/after-sales")
    public R<PageResult<AfterSale>> afterSales(@RequestParam(required = false) Integer status,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return R.ok(afterSaleService.pageAdmin(status, keyword, page, size));
    }

    @Operation(summary = "售后详情")
    @GetMapping("/after-sales/{id}")
    public R<AfterSale> afterSale(@PathVariable Long id) {
        return R.ok(afterSaleService.detail(UserContext.userId(), UserContext.role(), id));
    }

    @Operation(summary = "售后仲裁")
    @PostMapping("/after-sales/{id}/arbitrate")
    public R<Void> arbitrate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object support = body.get("supportCustomer");
        boolean supportCustomer = support instanceof Boolean
                ? (Boolean) support
                : "true".equalsIgnoreCase(String.valueOf(support));
        Object verdict = body.get("verdict");
        afterSaleService.arbitrate(UserContext.userId(), id, supportCustomer,
                verdict == null ? null : String.valueOf(verdict));
        return R.ok();
    }

    @Operation(summary = "发布系统公告")
    @PostMapping("/notices")
    public R<Map<String, Object>> publishNotice(@RequestBody Map<String, Object> body) {
        Object title = body.get("title");
        Object content = body.get("content");
        if (title == null || String.valueOf(title).isBlank()) {
            throw com.eatwhat.common.exception.BizException.of(
                    com.eatwhat.common.exception.ErrorCode.PARAM_MISSING, "公告标题不能为空");
        }
        if (content == null || String.valueOf(content).isBlank()) {
            throw com.eatwhat.common.exception.BizException.of(
                    com.eatwhat.common.exception.ErrorCode.PARAM_MISSING, "公告内容不能为空");
        }
        String t = String.valueOf(title).trim();
        String ct = String.valueOf(content).trim();
        if (t.length() > 100) {
            throw com.eatwhat.common.exception.BizException.of(
                    com.eatwhat.common.exception.ErrorCode.PARAM_INVALID, "公告标题不能超过 100 字");
        }
        if (ct.length() > 500) {
            throw com.eatwhat.common.exception.BizException.of(
                    com.eatwhat.common.exception.ErrorCode.PARAM_INVALID, "公告内容不能超过 500 字");
        }
        messageService.broadcast(UserContext.userId(), t, ct);
        return R.ok(Map.of("published", true));
    }
}
