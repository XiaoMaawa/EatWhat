package com.eatwhat.modules.merchant.controller;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.common.result.R;
import com.eatwhat.common.utils.UserContext;
import com.eatwhat.modules.dish.dto.CategoryRequest;
import com.eatwhat.modules.dish.dto.DishRequest;
import com.eatwhat.modules.dish.entity.Dish;
import com.eatwhat.modules.dish.entity.DishCategory;
import com.eatwhat.modules.dish.service.DishService;
import com.eatwhat.modules.merchant.dto.MerchantApplyRequest;
import com.eatwhat.modules.merchant.entity.Merchant;
import com.eatwhat.modules.merchant.service.MerchantService;
import com.eatwhat.modules.order.entity.Order;
import com.eatwhat.modules.order.service.OrderService;
import com.eatwhat.modules.review.entity.Review;
import com.eatwhat.modules.review.service.ReviewService;
import com.eatwhat.modules.shop.dto.ShopUpdateRequest;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.service.ShopService;
import com.eatwhat.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 商家端接口：入驻、店铺、菜品、订单、统计。
 */
@Tag(name = "商家端")
@RequireRole({"MERCHANT", "ADMIN"})
@RestController
@RequestMapping("/api/v1/merchant")
public class MerchantController {

    private final MerchantService merchantService;
    private final ShopService shopService;
    private final DishService dishService;
    private final OrderService orderService;
    private final ReviewService reviewService;

    public MerchantController(MerchantService merchantService,
                              ShopService shopService,
                              DishService dishService,
                              OrderService orderService,
                              ReviewService reviewService) {
        this.merchantService = merchantService;
        this.shopService = shopService;
        this.dishService = dishService;
        this.orderService = orderService;
        this.reviewService = reviewService;
    }

    // ------------------------------------------------------------------ 入驻

    @Operation(summary = "提交入驻申请")
    @PostMapping("/apply")
    public R<Merchant> apply(@Valid @RequestBody MerchantApplyRequest request) {
        return R.ok(merchantService.apply(UserContext.userId(), request));
    }

    @Operation(summary = "查看入驻资料")
    @GetMapping("/profile")
    public R<Map<String, Object>> profile() {
        return R.ok(merchantService.profile(UserContext.userId()));
    }

    @Operation(summary = "商家-58：修改联系人姓名与联系电话")
    @PatchMapping("/account")
    public R<Merchant> updateAccount(@RequestBody Map<String, String> body) {
        return R.ok(merchantService.updateAccount(UserContext.userId(),
                body == null ? null : body.get("realName"),
                body == null ? null : body.get("contactPhone")));
    }

    // ------------------------------------------------------------------ 店铺

    @Operation(summary = "查看我的店铺")
    @GetMapping("/shop")
    public R<Shop> shop() {
        return R.ok(shopService.currentShop(UserContext.userId()));
    }

    @Operation(summary = "更新店铺信息")
    @PutMapping("/shop")
    public R<Void> updateShop(@Valid @RequestBody ShopUpdateRequest request) {
        shopService.updateShop(UserContext.userId(), request);
        return R.ok();
    }

    @Operation(summary = "切换营业状态")
    @PatchMapping("/shop/status")
    public R<Void> updateShopStatus(@RequestBody Map<String, Integer> body) {
        shopService.updateOpenStatus(UserContext.userId(), body == null ? null : body.get("isOpen"));
        return R.ok();
    }

    // ------------------------------------------------------------------ 菜品分类

    @Operation(summary = "分类列表")
    @GetMapping("/dish-categories")
    public R<List<DishCategory>> categories() {
        return R.ok(dishService.listCategories(UserContext.userId()));
    }

    @Operation(summary = "新增分类")
    @PostMapping("/dish-categories")
    public R<DishCategory> createCategory(@Valid @RequestBody CategoryRequest request) {
        return R.ok(dishService.createCategory(UserContext.userId(), request));
    }

    @Operation(summary = "修改分类")
    @PutMapping("/dish-categories/{id}")
    public R<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        dishService.updateCategory(UserContext.userId(), id, request);
        return R.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/dish-categories/{id}")
    public R<Void> deleteCategory(@PathVariable Long id) {
        dishService.deleteCategory(UserContext.userId(), id);
        return R.ok();
    }

    // ------------------------------------------------------------------ 菜品

    @Operation(summary = "菜品列表")
    @GetMapping("/dishes")
    public R<List<Dish>> dishes(@RequestParam(required = false) Long categoryId,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) Integer status) {
        return R.ok(dishService.listDishes(UserContext.userId(), categoryId, keyword, status));
    }

    @Operation(summary = "新增菜品")
    @PostMapping("/dishes")
    public R<Dish> createDish(@Valid @RequestBody DishRequest request) {
        return R.ok(dishService.createDish(UserContext.userId(), request));
    }

    @Operation(summary = "修改菜品")
    @PutMapping("/dishes/{id}")
    public R<Void> updateDish(@PathVariable Long id, @Valid @RequestBody DishRequest request) {
        dishService.updateDish(UserContext.userId(), id, request);
        return R.ok();
    }

    @Operation(summary = "删除菜品")
    @DeleteMapping("/dishes/{id}")
    public R<Void> deleteDish(@PathVariable Long id) {
        dishService.deleteDish(UserContext.userId(), id);
        return R.ok();
    }

    @Operation(summary = "修改菜品状态")
    @PatchMapping("/dishes/{id}/status")
    public R<Void> updateDishStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        dishService.updateDishStatus(UserContext.userId(), id, body == null ? null : body.get("status"));
        return R.ok();
    }

    @Operation(summary = "批量修改菜品状态")
    @PatchMapping("/dishes/batch-status")
    public R<Void> batchUpdateStatus(@RequestBody Map<String, Object> body) {
        if (body == null) {
            return R.ok();
        }
        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) body.get("ids");
        List<Long> ids = rawIds == null ? List.of()
                : rawIds.stream().map(o -> Long.valueOf(o.toString())).toList();
        Object status = body.get("status");
        dishService.batchUpdateStatus(UserContext.userId(), ids,
                status == null ? null : Integer.valueOf(status.toString()));
        return R.ok();
    }

    // ------------------------------------------------------------------ 订单

    @Operation(summary = "订单列表")
    @GetMapping("/orders")
    public R<PageResult<Order>> orders(@RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) String orderNo,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        Merchant merchant = merchantService.requireApprovedMerchant(UserContext.userId());
        return R.ok(orderService.pageMerchantOrders(merchant.getId(), status, orderNo,
                startDate, endDate, page, size));
    }

    @Operation(summary = "接单")
    @PatchMapping("/orders/{id}/accept")
    public R<Void> accept(@PathVariable Long id) {
        Merchant merchant = merchantService.requireApprovedMerchant(UserContext.userId());
        orderService.merchantAccept(merchant.getId(), id);
        return R.ok();
    }

    @Operation(summary = "拒单")
    @PatchMapping("/orders/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        Merchant merchant = merchantService.requireApprovedMerchant(UserContext.userId());
        orderService.merchantReject(merchant.getId(), id, body == null ? null : body.get("reason"));
        return R.ok();
    }

    @Operation(summary = "备餐完成")
    @PatchMapping("/orders/{id}/ready")
    public R<Void> ready(@PathVariable Long id) {
        Merchant merchant = merchantService.requireApprovedMerchant(UserContext.userId());
        orderService.merchantReady(merchant.getId(), id);
        return R.ok();
    }

    @Operation(summary = "订单小票打印数据（商家-43）")
    @GetMapping("/orders/{id}/receipt")
    public R<Map<String, Object>> receipt(@PathVariable Long id) {
        Merchant merchant = merchantService.requireApprovedMerchant(UserContext.userId());
        return R.ok(orderService.receipt(merchant.getId(), id));
    }

    // ------------------------------------------------------------------ 评价与统计

    @Operation(summary = "本店评价列表")
    @GetMapping("/reviews")
    public R<PageResult<Review>> reviews(@RequestParam(required = false) Integer score,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        Shop current = shopService.currentShop(UserContext.userId());
        return R.ok(reviewService.pageShopReviews(current.getId(), page, size, score));
    }

    @Operation(summary = "商家回复评价")
    @PostMapping("/reviews/{id}/reply")
    public R<Void> replyReview(@PathVariable Long id, @RequestBody Map<String, String> body) {
        reviewService.reply(UserContext.userId(), id, body == null ? null : body.get("reply"));
        return R.ok();
    }

    @Operation(summary = "经营统计")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats(@RequestParam(defaultValue = "day") String type) {
        return R.ok(merchantService.stats(UserContext.userId(), type));
    }

    @Operation(summary = "经营分析（客单价/评分统计/营业时段）")
    @GetMapping("/analytics")
    public R<Map<String, Object>> analytics(@RequestParam(defaultValue = "month") String type) {
        return R.ok(merchantService.analytics(UserContext.userId(), type));
    }
}
