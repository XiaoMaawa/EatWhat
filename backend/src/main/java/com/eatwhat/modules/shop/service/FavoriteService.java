package com.eatwhat.modules.shop.service;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.shop.entity.Shop;

/**
 * 店铺收藏服务。
 */
public interface FavoriteService {

    /**
     * 收藏店铺，重复收藏幂等。
     */
    void add(Long userId, Long shopId);

    /**
     * 取消收藏，未收藏时幂等返回。
     */
    void remove(Long userId, Long shopId);

    /**
     * 我的收藏店铺列表（分页）。
     */
    PageResult<Shop> pageMy(Long userId, int page, int size);

    /**
     * 是否已收藏该店铺。
     */
    boolean isFavorite(Long userId, Long shopId);
}
