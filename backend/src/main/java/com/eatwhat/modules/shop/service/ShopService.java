package com.eatwhat.modules.shop.service;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.dish.entity.Dish;
import com.eatwhat.modules.shop.dto.ShopUpdateRequest;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.vo.ShopDetailVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 店铺服务。
 */
public interface ShopService {

    /**
     * 顾客端店铺列表（仅展示营业中、状态正常的店铺）。
     */
    PageResult<Shop> pageShops(int page, int size, String keyword, String sortBy,
                               BigDecimal longitude, BigDecimal latitude);

    /**
     * 店铺详情，含分类与菜品。
     */
    ShopDetailVO detail(Long shopId, BigDecimal longitude, BigDecimal latitude);

    /**
     * 店铺菜品列表（可按分类过滤）。
     */
    List<Dish> listDishes(Long shopId, Long categoryId, String keyword);

    /**
     * 商家查看自己的店铺。
     */
    Shop currentShop(Long userId);

    /**
     * 商家更新店铺信息。
     */
    void updateShop(Long userId, ShopUpdateRequest request);

    /**
     * 商家切换营业状态。
     */
    void updateOpenStatus(Long userId, Integer isOpen);

    /**
     * 刷新店铺评分与月销量（评价后调用）。
     */
    void refreshScoreAndSales(Long shopId, int salesDelta);
}
