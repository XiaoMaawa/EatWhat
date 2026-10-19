package com.eatwhat.modules.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.utils.DeliveryFeeUtils;
import com.eatwhat.common.utils.GeoUtils;
import com.eatwhat.modules.cart.dto.AddCartRequest;
import com.eatwhat.modules.cart.entity.CartItem;
import com.eatwhat.modules.cart.mapper.CartItemMapper;
import com.eatwhat.modules.cart.service.CartService;
import com.eatwhat.modules.cart.vo.CartVO;
import com.eatwhat.modules.dish.entity.Dish;
import com.eatwhat.modules.dish.mapper.DishMapper;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.mapper.ShopMapper;
import com.eatwhat.modules.user.entity.UserAddress;
import com.eatwhat.modules.user.mapper.UserAddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 购物车服务实现。
 */
@Service
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;
    private final DishMapper dishMapper;
    private final ShopMapper shopMapper;
    private final UserAddressMapper addressMapper;

    public CartServiceImpl(CartItemMapper cartItemMapper, DishMapper dishMapper, ShopMapper shopMapper,
                          UserAddressMapper addressMapper) {
        this.cartItemMapper = cartItemMapper;
        this.dishMapper = dishMapper;
        this.shopMapper = shopMapper;
        this.addressMapper = addressMapper;
    }

    @Override
    public CartVO view(Long userId) {
        return view(userId, null);
    }

    @Override
    public CartVO view(Long userId, Long addressId) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getId));
        CartVO vo = new CartVO();
        vo.setGoodsAmount(BigDecimal.ZERO);
        vo.setTotalAmount(BigDecimal.ZERO);
        if (items.isEmpty()) {
            return vo;
        }

        Long shopId = items.get(0).getShopId();
        Shop shop = shopMapper.selectById(shopId);
        if (shop != null) {
            vo.setShopId(shop.getId());
            vo.setShopName(shop.getName());
            vo.setMinOrderAmount(shop.getMinOrderAmount());
            vo.setDeliveryFee(shop.getDeliveryFee());
        }

        Map<Long, Dish> dishCache = new HashMap<>();
        for (CartItem item : items) {
            if (!shopId.equals(item.getShopId())) {
                continue;
            }
            Dish dish = dishCache.computeIfAbsent(item.getDishId(), dishMapper::selectById);
            if (dish == null) {
                continue;
            }
            CartVO.Item voItem = new CartVO.Item();
            voItem.setId(item.getId());
            voItem.setDishId(dish.getId());
            voItem.setDishName(dish.getName());
            voItem.setDishImage(dish.getImage());
            voItem.setPrice(dish.getPrice());
            voItem.setQuantity(item.getQuantity());
            voItem.setStock(dish.getStock());
            boolean onSale = Integer.valueOf(CommonConstants.DISH_ON).equals(dish.getStatus());
            boolean enough = dish.getStock() != null && dish.getStock() >= item.getQuantity();
            voItem.setAvailable(onSale && enough);
            vo.getItems().add(voItem);

            BigDecimal subtotal = dish.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            vo.setGoodsAmount(vo.getGoodsAmount().add(subtotal));
        }
        BigDecimal deliveryFee = resolveDeliveryFee(userId, shop, addressId, vo.getDeliveryFee());
        vo.setDeliveryFee(deliveryFee);
        vo.setTotalAmount(vo.getGoodsAmount().add(deliveryFee).setScale(2, RoundingMode.HALF_UP));
        return vo;
    }

    /**
     * 计算配送费：按距离阶梯计价时，用收货地址与店铺坐标的距离算；否则用固定配送费。
     */
    private BigDecimal resolveDeliveryFee(Long userId, Shop shop, Long addressId, BigDecimal defaultFee) {
        if (shop == null) {
            return defaultFee == null ? BigDecimal.ZERO : defaultFee.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal baseFee = shop.getDeliveryFee() == null ? BigDecimal.ZERO : shop.getDeliveryFee();
        if (addressId == null || !Integer.valueOf(DeliveryFeeUtils.TYPE_BY_DISTANCE).equals(shop.getDeliveryFeeType())) {
            return baseFee.setScale(2, RoundingMode.HALF_UP);
        }
        UserAddress address = addressMapper.selectById(addressId);
        // 地址不存在或不属于当前用户时，退回固定配送费，不泄露他人地址信息
        if (address == null || !userId.equals(address.getUserId())) {
            return baseFee.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal km = GeoUtils.distanceKm(shop.getLatitude(), shop.getLongitude(),
                address.getLatitude(), address.getLongitude());
        return DeliveryFeeUtils.calculate(baseFee, shop.getDeliveryFeeType(), km);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartVO add(Long userId, AddCartRequest request) {
        Dish dish = dishMapper.selectById(request.getDishId());
        if (dish == null) {
            throw BizException.of(ErrorCode.DISH_NOT_FOUND);
        }
        if (!Integer.valueOf(CommonConstants.DISH_ON).equals(dish.getStatus())) {
            throw BizException.of(ErrorCode.DISH_OFF_SALE);
        }
        if (dish.getStock() == null || dish.getStock() <= 0) {
            throw BizException.of(ErrorCode.STOCK_NOT_ENOUGH);
        }

        List<CartItem> existing = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId));
        boolean crossShop = existing.stream()
                .anyMatch(item -> !item.getShopId().equals(dish.getShopId()));
        if (crossShop) {
            // 一次只能结算一家店铺，切换店铺时清空原购物车
            cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
        }

        CartItem current = existing.stream()
                .filter(item -> item.getDishId().equals(dish.getId()))
                .findFirst()
                .orElse(null);
        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        if (current != null) {
            int target = current.getQuantity() + quantity;
            if (target > dish.getStock()) {
                throw BizException.of(ErrorCode.STOCK_NOT_ENOUGH);
            }
            current.setQuantity(target);
            cartItemMapper.updateById(current);
        } else {
            if (quantity > dish.getStock()) {
                throw BizException.of(ErrorCode.STOCK_NOT_ENOUGH);
            }
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setShopId(dish.getShopId());
            item.setDishId(dish.getId());
            item.setQuantity(quantity);
            cartItemMapper.insert(item);
        }
        return view(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartVO updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        CartItem item = requireOwned(userId, cartItemId);
        Dish dish = dishMapper.selectById(item.getDishId());
        if (dish == null) {
            throw BizException.of(ErrorCode.DISH_NOT_FOUND);
        }
        if (dish.getStock() == null || quantity > dish.getStock()) {
            throw BizException.of(ErrorCode.STOCK_NOT_ENOUGH);
        }
        item.setQuantity(quantity);
        cartItemMapper.updateById(item);
        return view(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long cartItemId) {
        if (cartItemId == null) {
            cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
            return;
        }
        requireOwned(userId, cartItemId);
        cartItemMapper.deleteById(cartItemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearByShop(Long userId, Long shopId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getShopId, shopId));
    }

    private CartItem requireOwned(Long userId, Long cartItemId) {
        CartItem item = cartItemMapper.selectById(cartItemId);
        if (item == null) {
            throw BizException.of(ErrorCode.CART_EMPTY, "购物车项不存在");
        }
        if (!item.getUserId().equals(userId)) {
            throw BizException.of(ErrorCode.DATA_NOT_OWNED);
        }
        return item;
    }
}
