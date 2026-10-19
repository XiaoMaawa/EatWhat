package com.eatwhat.modules.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.entity.ShopFavorite;
import com.eatwhat.modules.shop.mapper.ShopFavoriteMapper;
import com.eatwhat.modules.shop.mapper.ShopMapper;
import com.eatwhat.modules.shop.service.FavoriteService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 店铺收藏实现。
 */
@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final ShopFavoriteMapper favoriteMapper;
    private final ShopMapper shopMapper;

    public FavoriteServiceImpl(ShopFavoriteMapper favoriteMapper, ShopMapper shopMapper) {
        this.favoriteMapper = favoriteMapper;
        this.shopMapper = shopMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Long userId, Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw BizException.of(ErrorCode.SHOP_NOT_FOUND);
        }
        if (isFavorite(userId, shopId)) {
            return;
        }
        ShopFavorite favorite = new ShopFavorite();
        favorite.setUserId(userId);
        favorite.setShopId(shopId);
        favorite.setCreatedAt(LocalDateTime.now());
        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException ignored) {
            // 并发重复收藏，幂等吞掉
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long shopId) {
        favoriteMapper.delete(new LambdaQueryWrapper<ShopFavorite>()
                .eq(ShopFavorite::getUserId, userId)
                .eq(ShopFavorite::getShopId, shopId));
    }

    @Override
    public PageResult<Shop> pageMy(Long userId, int page, int size) {
        Page<ShopFavorite> favoritePage = favoriteMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ShopFavorite>()
                        .eq(ShopFavorite::getUserId, userId)
                        .orderByDesc(ShopFavorite::getId));
        List<Shop> shops = new ArrayList<>();
        if (favoritePage.getRecords() != null) {
            for (ShopFavorite favorite : favoritePage.getRecords()) {
                Shop shop = shopMapper.selectById(favorite.getShopId());
                if (shop != null) {
                    shops.add(shop);
                }
            }
        }
        return PageResult.of(shops, favoritePage.getTotal(), favoritePage.getCurrent(), favoritePage.getSize());
    }

    @Override
    public boolean isFavorite(Long userId, Long shopId) {
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<ShopFavorite>()
                .eq(ShopFavorite::getUserId, userId)
                .eq(ShopFavorite::getShopId, shopId));
        return count != null && count > 0;
    }
}
