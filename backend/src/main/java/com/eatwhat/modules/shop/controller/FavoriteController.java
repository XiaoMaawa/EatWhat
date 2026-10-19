package com.eatwhat.modules.shop.controller;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.common.result.R;
import com.eatwhat.common.utils.UserContext;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.service.FavoriteService;
import com.eatwhat.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 店铺收藏接口。
 */
@Tag(name = "店铺收藏")
@RequireRole("CONSUMER")
@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @Operation(summary = "我的收藏店铺")
    @GetMapping
    public R<PageResult<Shop>> mine(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        return R.ok(favoriteService.pageMy(UserContext.userId(), page, size));
    }

    @Operation(summary = "收藏店铺")
    @PostMapping("/{shopId}")
    public R<Void> add(@PathVariable Long shopId) {
        favoriteService.add(UserContext.userId(), shopId);
        return R.ok();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{shopId}")
    public R<Void> remove(@PathVariable Long shopId) {
        favoriteService.remove(UserContext.userId(), shopId);
        return R.ok();
    }

    @Operation(summary = "是否已收藏")
    @GetMapping("/{shopId}")
    public R<Map<String, Object>> status(@PathVariable Long shopId) {
        boolean favorite = favoriteService.isFavorite(UserContext.userId(), shopId);
        return R.ok(Map.of("favorite", favorite));
    }
}
