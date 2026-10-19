package com.eatwhat.modules.shop.controller;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.common.result.R;
import com.eatwhat.modules.dish.entity.Dish;
import com.eatwhat.modules.review.entity.Review;
import com.eatwhat.modules.review.service.ReviewService;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.service.ShopService;
import com.eatwhat.modules.shop.vo.ShopDetailVO;
import com.eatwhat.security.PublicApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 顾客端店铺浏览接口（无需登录）。
 */
@Tag(name = "店铺浏览")
@PublicApi
@RestController
@RequestMapping("/api/v1/shops")
public class ShopController {

    private final ShopService shopService;
    private final ReviewService reviewService;

    public ShopController(ShopService shopService, ReviewService reviewService) {
        this.shopService = shopService;
        this.reviewService = reviewService;
    }

    @Operation(summary = "店铺列表")
    @GetMapping
    public R<PageResult<Shop>> list(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String sortBy,
                                    @RequestParam(required = false) BigDecimal longitude,
                                    @RequestParam(required = false) BigDecimal latitude) {
        return R.ok(shopService.pageShops(page, size, keyword, sortBy, longitude, latitude));
    }

    @Operation(summary = "店铺详情（含分类与菜品）")
    @GetMapping("/{id}")
    public R<ShopDetailVO> detail(@PathVariable Long id,
                                  @RequestParam(required = false) BigDecimal longitude,
                                  @RequestParam(required = false) BigDecimal latitude) {
        return R.ok(shopService.detail(id, longitude, latitude));
    }

    @Operation(summary = "店铺菜品列表")
    @GetMapping("/{id}/dishes")
    public R<List<Dish>> dishes(@PathVariable Long id,
                                @RequestParam(required = false) Long categoryId,
                                @RequestParam(required = false) String keyword) {
        return R.ok(shopService.listDishes(id, categoryId, keyword));
    }

    @Operation(summary = "店铺评价列表")
    @GetMapping("/{id}/reviews")
    public R<PageResult<Review>> reviews(@PathVariable Long id,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) Integer score) {
        return R.ok(reviewService.pageShopReviews(id, page, size, score));
    }
}
