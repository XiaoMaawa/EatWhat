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
     * 查看购物车，并按收货地址的距离计算阶梯配送费。
     *
     * @param addressId 收货地址 ID，为空时使用店铺固定配送费
     */
    CartVO view(Long userId, Long addressId);

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

    /**
     * 再来一单：把历史订单的菜品重新加入购物车。
     *
     * @return 加入后的购物车（含店铺信息）
     */
    CartVO reorder(Long userId, Long orderId);
}
