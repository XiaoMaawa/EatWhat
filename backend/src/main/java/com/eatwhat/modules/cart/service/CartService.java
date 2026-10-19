package com.eatwhat.modules.cart.service;

import com.eatwhat.modules.cart.dto.AddCartRequest;
import com.eatwhat.modules.cart.vo.CartVO;

/**
 * 购物车服务。
 */
public interface CartService {

    /**
     * 查看购物车（一次只保留同一店铺的菜品）。
     */
    CartVO view(Long userId);

    /**
     * 加入购物车，跨店铺时先清空旧店铺。
     */
    CartVO add(Long userId, AddCartRequest request);

    /**
     * 修改数量。
     */
    CartVO updateQuantity(Long userId, Long cartItemId, Integer quantity);

    /**
     * 删除单项或清空（cartItemId 为空表示清空）。
     */
    void remove(Long userId, Long cartItemId);

    /**
     * 结算后清空指定店铺的购物车。
     */
    void clearByShop(Long userId, Long shopId);
}
