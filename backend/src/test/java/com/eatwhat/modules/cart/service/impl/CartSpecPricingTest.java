package com.eatwhat.modules.cart.service.impl;

import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.modules.cart.dto.AddCartRequest;
import com.eatwhat.modules.cart.mapper.CartItemMapper;
import com.eatwhat.modules.dish.entity.Dish;
import com.eatwhat.modules.dish.entity.DishAddon;
import com.eatwhat.modules.dish.entity.DishSpec;
import com.eatwhat.modules.dish.mapper.DishAddonMapper;
import com.eatwhat.modules.dish.mapper.DishMapper;
import com.eatwhat.modules.dish.mapper.DishSpecMapper;
import com.eatwhat.modules.order.mapper.OrderItemMapper;
import com.eatwhat.modules.order.mapper.OrderMapper;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.mapper.ShopMapper;
import com.eatwhat.modules.user.mapper.UserAddressMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 购物车规格与加料计价测试（顾客-27 / 顾客-28）。
 */
class CartSpecPricingTest {

    private CartItemMapper cartItemMapper;
    private DishMapper dishMapper;
    private ShopMapper shopMapper;
    private UserAddressMapper addressMapper;
    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;
    private DishSpecMapper specMapper;
    private DishAddonMapper addonMapper;
    private CartServiceImpl service;

    @BeforeEach
    void setUp() {
        cartItemMapper = mock(CartItemMapper.class);
        dishMapper = mock(DishMapper.class);
        shopMapper = mock(ShopMapper.class);
        addressMapper = mock(UserAddressMapper.class);
        orderMapper = mock(OrderMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        specMapper = mock(DishSpecMapper.class);
        addonMapper = mock(DishAddonMapper.class);
        service = new CartServiceImpl(cartItemMapper, dishMapper, shopMapper, addressMapper,
                orderMapper, orderItemMapper, specMapper, addonMapper);
    }

    private Dish rice(Long id, String price) {
        Dish dish = new Dish();
        dish.setId(id);
        dish.setShopId(1L);
        dish.setName("米饭（大份）");
        dish.setPrice(new BigDecimal(price));
        dish.setStock(100);
        dish.setStatus(1);
        return dish;
    }

    private DishSpec spec(Long id, Long dishId, String name, String price, int isDefault) {
        DishSpec spec = new DishSpec();
        spec.setId(id);
        spec.setDishId(dishId);
        spec.setName(name);
        spec.setPrice(new BigDecimal(price));
        spec.setIsDefault(isDefault);
        return spec;
    }

    private DishAddon addon(Long id, Long dishId, String name, String price) {
        DishAddon addon = new DishAddon();
        addon.setId(id);
        addon.setDishId(dishId);
        addon.setName(name);
        addon.setPrice(new BigDecimal(price));
        return addon;
    }

    @Test
    @DisplayName("显式规格与加料：单价 = 规格价 + 加料价之和")
    void shouldPriceWithSpecAndAddons() {
        Dish dish = rice(5L, "3.00");
        when(dishMapper.selectById(5L)).thenReturn(dish);
        when(cartItemMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(specMapper.selectById(1L)).thenReturn(spec(1L, 5L, "大份", "3.00", 1));
        when(addonMapper.selectList(any())).thenReturn(Arrays.asList(
                addon(1L, 5L, "加米饭", "1.00"),
                addon(2L, 5L, "加卤蛋", "2.00")));

        AddCartRequest request = new AddCartRequest();
        request.setDishId(5L);
        request.setQuantity(2);
        request.setSpecId(1L);
        request.setAddonIds(Arrays.asList(1L, 2L));

        // view() 依赖 shop 与购物车数据，这里只验证解析逻辑不抛异常且单价正确
        // 通过反射以外的直接方式：调用后从 mock 捕获插入的实体
        org.mockito.ArgumentCaptor<com.eatwhat.modules.cart.entity.CartItem> captor =
                org.mockito.ArgumentCaptor.forClass(com.eatwhat.modules.cart.entity.CartItem.class);
        try {
            service.add(2L, request);
        } catch (RuntimeException ignore) {
            // view() 在无店铺数据时会抛异常，此处只关心插入的购物车项
        }
        org.mockito.Mockito.verify(cartItemMapper).insert(captor.capture());
        com.eatwhat.modules.cart.entity.CartItem saved = captor.getValue();
        assertEquals("大份", saved.getSpecName());
        assertEquals("加米饭,加卤蛋", saved.getAddonNames());
        assertEquals(new BigDecimal("3.00"), saved.getAddonPrice());
        assertEquals(new BigDecimal("6.00"), saved.getUnitPrice());
        assertEquals(2, saved.getQuantity());
    }

    @Test
    @DisplayName("未指定规格时取默认规格定价")
    void shouldUseDefaultSpecWhenSpecIdMissing() {
        Dish dish = rice(5L, "3.00");
        when(dishMapper.selectById(5L)).thenReturn(dish);
        when(cartItemMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(specMapper.selectOne(any())).thenReturn(spec(2L, 5L, "中份", "2.50", 1));

        AddCartRequest request = new AddCartRequest();
        request.setDishId(5L);
        request.setQuantity(1);

        org.mockito.ArgumentCaptor<com.eatwhat.modules.cart.entity.CartItem> captor =
                org.mockito.ArgumentCaptor.forClass(com.eatwhat.modules.cart.entity.CartItem.class);
        try {
            service.add(2L, request);
        } catch (RuntimeException ignore) {
            // 忽略 view() 阶段异常
        }
        org.mockito.Mockito.verify(cartItemMapper).insert(captor.capture());
        assertEquals("中份", captor.getValue().getSpecName());
        assertEquals(new BigDecimal("2.50"), captor.getValue().getUnitPrice());
    }

    @Test
    @DisplayName("无规格无加料时按菜品原价")
    void shouldUseDishPriceWhenNoSpec() {
        Dish dish = rice(7L, "12.00");
        when(dishMapper.selectById(7L)).thenReturn(dish);
        when(cartItemMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(specMapper.selectOne(any())).thenReturn(null);

        AddCartRequest request = new AddCartRequest();
        request.setDishId(7L);
        request.setQuantity(1);

        org.mockito.ArgumentCaptor<com.eatwhat.modules.cart.entity.CartItem> captor =
                org.mockito.ArgumentCaptor.forClass(com.eatwhat.modules.cart.entity.CartItem.class);
        try {
            service.add(2L, request);
        } catch (RuntimeException ignore) {
            // 忽略 view() 阶段异常
        }
        org.mockito.Mockito.verify(cartItemMapper).insert(captor.capture());
        assertEquals(new BigDecimal("12.00"), captor.getValue().getUnitPrice());
        assertEquals(0, captor.getValue().getAddonPrice().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("规格不属于该菜品时拒绝")
    void shouldRejectForeignSpec() {
        Dish dish = rice(5L, "3.00");
        when(dishMapper.selectById(5L)).thenReturn(dish);
        when(cartItemMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(specMapper.selectById(4L)).thenReturn(spec(4L, 6L, "大份", "2.00", 1));

        AddCartRequest request = new AddCartRequest();
        request.setDishId(5L);
        request.setQuantity(1);
        request.setSpecId(4L);

        BizException ex = assertThrows(BizException.class, () -> service.add(2L, request));
        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        assertEquals("菜品规格不存在", ex.getMessage());
    }

    @Test
    @DisplayName("加料不属于该菜品时拒绝")
    void shouldRejectForeignAddon() {
        Dish dish = rice(5L, "3.00");
        when(dishMapper.selectById(5L)).thenReturn(dish);
        when(cartItemMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(specMapper.selectById(1L)).thenReturn(spec(1L, 5L, "大份", "3.00", 1));
        when(addonMapper.selectList(any())).thenReturn(Collections.singletonList(
                addon(4L, 6L, "加米饭", "1.00")));

        AddCartRequest request = new AddCartRequest();
        request.setDishId(5L);
        request.setQuantity(1);
        request.setSpecId(1L);
        request.setAddonIds(Collections.singletonList(4L));

        BizException ex = assertThrows(BizException.class, () -> service.add(2L, request));
        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        assertEquals("加料不属于当前菜品", ex.getMessage());
    }

    @Test
    @DisplayName("加料超过 5 项时拒绝")
    void shouldRejectTooManyAddons() {
        Dish dish = rice(5L, "3.00");
        when(dishMapper.selectById(5L)).thenReturn(dish);
        when(cartItemMapper.selectList(any())).thenReturn(new ArrayList<>());
        when(specMapper.selectById(1L)).thenReturn(spec(1L, 5L, "大份", "3.00", 1));

        AddCartRequest request = new AddCartRequest();
        request.setDishId(5L);
        request.setQuantity(1);
        request.setSpecId(1L);
        request.setAddonIds(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L));

        BizException ex = assertThrows(BizException.class, () -> service.add(2L, request));
        assertEquals(ErrorCode.PARAM_INVALID.getCode(), ex.getCode());
        assertEquals("单个菜品最多选择 5 项加料", ex.getMessage());
    }

    @Test
    @DisplayName("相同规格与加料合并数量，不同规格分行")
    void shouldMergeSameSelectionOnly() {
        Dish dish = rice(5L, "3.00");
        when(dishMapper.selectById(5L)).thenReturn(dish);

        com.eatwhat.modules.cart.entity.CartItem existing = new com.eatwhat.modules.cart.entity.CartItem();
        existing.setId(10L);
        existing.setUserId(2L);
        existing.setShopId(1L);
        existing.setDishId(5L);
        existing.setSpecId(1L);
        existing.setAddonIds("1");
        existing.setQuantity(1);
        existing.setUnitPrice(new BigDecimal("4.00"));
        when(cartItemMapper.selectList(any())).thenReturn(new ArrayList<>(Collections.singletonList(existing)));
        when(specMapper.selectById(1L)).thenReturn(spec(1L, 5L, "大份", "3.00", 1));
        when(addonMapper.selectList(any())).thenReturn(Collections.singletonList(addon(1L, 5L, "加米饭", "1.00")));

        AddCartRequest request = new AddCartRequest();
        request.setDishId(5L);
        request.setQuantity(2);
        request.setSpecId(1L);
        request.setAddonIds(Collections.singletonList(1L));

        try {
            service.add(2L, request);
        } catch (RuntimeException ignore) {
            // 忽略 view() 阶段异常
        }
        org.mockito.Mockito.verify(cartItemMapper).updateById(existing);
        assertEquals(3, existing.getQuantity());
        assertEquals(new BigDecimal("4.00"), existing.getUnitPrice());
    }

    @Test
    @DisplayName("Shop 与规格同时存在时购物车视图按快照单价计算小计")
    void shouldComputeSubtotalFromSnapshot() {
        Shop shop = new Shop();
        shop.setId(1L);
        shop.setName("川味小厨");
        shop.setMinOrderAmount(new BigDecimal("20.00"));
        shop.setDeliveryFee(new BigDecimal("3.00"));
        shop.setDeliveryFeeType(0);
        when(shopMapper.selectById(1L)).thenReturn(shop);

        Dish dish = rice(5L, "3.00");
        when(dishMapper.selectById(5L)).thenReturn(dish);

        com.eatwhat.modules.cart.entity.CartItem item = new com.eatwhat.modules.cart.entity.CartItem();
        item.setId(11L);
        item.setUserId(2L);
        item.setShopId(1L);
        item.setDishId(5L);
        item.setQuantity(2);
        item.setSpecName("大份");
        item.setAddonNames("加米饭");
        item.setAddonPrice(new BigDecimal("1.00"));
        item.setUnitPrice(new BigDecimal("4.00"));
        when(cartItemMapper.selectList(any())).thenReturn(new ArrayList<>(Collections.singletonList(item)));

        com.eatwhat.modules.cart.vo.CartVO vo = service.view(2L);

        assertNotNull(vo);
        assertEquals(1, vo.getItems().size());
        assertEquals(new BigDecimal("4.00"), vo.getItems().get(0).getUnitPrice());
        assertEquals("大份", vo.getItems().get(0).getSpecName());
        assertEquals("加米饭", vo.getItems().get(0).getAddonNames());
        assertEquals(0, vo.getGoodsAmount().compareTo(new BigDecimal("8.00")));
        List<?> items = vo.getItems();
        assertEquals(1, items.size());
    }
}
