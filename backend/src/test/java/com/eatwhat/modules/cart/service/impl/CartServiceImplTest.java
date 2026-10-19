package com.eatwhat.modules.cart.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.modules.cart.dto.AddCartRequest;
import com.eatwhat.modules.cart.entity.CartItem;
import com.eatwhat.modules.cart.mapper.CartItemMapper;
import com.eatwhat.modules.cart.vo.CartVO;
import com.eatwhat.modules.dish.entity.Dish;
import com.eatwhat.modules.dish.mapper.DishMapper;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.mapper.ShopMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 购物车服务单元测试。
 * 覆盖加购校验（下架 / 库存 / 跨店）、金额试算与归属校验。
 */
@DisplayName("购物车服务")
class CartServiceImplTest {

    private static final Long USER_ID = 2L;

    private CartItemMapper cartItemMapper;
    private DishMapper dishMapper;
    private ShopMapper shopMapper;
    private CartServiceImpl service;

    @BeforeAll
    static void initMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.eatwhat.modules.cart.mapper.CartItemMapper");
        TableInfoHelper.initTableInfo(assistant, CartItem.class);
    }

    @BeforeEach
    void setUp() {
        cartItemMapper = mock(CartItemMapper.class);
        dishMapper = mock(DishMapper.class);
        shopMapper = mock(ShopMapper.class);
        service = new CartServiceImpl(cartItemMapper, dishMapper, shopMapper);
    }

    private Dish dish(Long id, Long shopId, int status, int stock, String price) {
        Dish d = new Dish();
        d.setId(id);
        d.setShopId(shopId);
        d.setName("宫保鸡丁");
        d.setPrice(new BigDecimal(price));
        d.setStock(stock);
        d.setStatus(status);
        return d;
    }

    private Shop shop(Long id) {
        Shop s = new Shop();
        s.setId(id);
        s.setName("川味小厨");
        s.setMinOrderAmount(new BigDecimal("20.00"));
        s.setDeliveryFee(new BigDecimal("3.00"));
        return s;
    }

    private AddCartRequest addRequest(Long dishId, int quantity) {
        AddCartRequest r = new AddCartRequest();
        r.setDishId(dishId);
        r.setQuantity(quantity);
        return r;
    }

    @Test
    @DisplayName("菜品不存在时拒绝加购")
    void addDishNotFound() {
        when(dishMapper.selectById(1L)).thenReturn(null);
        assertThatThrownBy(() -> service.add(USER_ID, addRequest(1L, 1)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.DISH_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("已下架菜品不能加入购物车")
    void addOffSaleDish() {
        when(dishMapper.selectById(1L)).thenReturn(dish(1L, 1L, CommonConstants.DISH_OFF, 10, "28.00"));
        assertThatThrownBy(() -> service.add(USER_ID, addRequest(1L, 1)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.DISH_OFF_SALE.getCode()));
    }

    @Test
    @DisplayName("库存为零时拒绝加购")
    void addSoldOutDish() {
        when(dishMapper.selectById(1L)).thenReturn(dish(1L, 1L, CommonConstants.DISH_ON, 0, "28.00"));
        assertThatThrownBy(() -> service.add(USER_ID, addRequest(1L, 1)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.STOCK_NOT_ENOUGH.getCode()));
    }

    @Test
    @DisplayName("加购数量超过库存时拒绝")
    void addExceedStock() {
        when(dishMapper.selectById(1L)).thenReturn(dish(1L, 1L, CommonConstants.DISH_ON, 2, "28.00"));
        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        assertThatThrownBy(() -> service.add(USER_ID, addRequest(1L, 3)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.STOCK_NOT_ENOUGH.getCode()));
    }

    @Test
    @DisplayName("首次加购写入新的购物车记录")
    void addNewItem() {
        when(dishMapper.selectById(1L)).thenReturn(dish(1L, 1L, CommonConstants.DISH_ON, 10, "28.00"));
        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        when(shopMapper.selectById(1L)).thenReturn(shop(1L));
        when(cartItemMapper.insert(any(CartItem.class))).thenAnswer(inv -> {
            CartItem item = inv.getArgument(0);
            assertThat(item.getUserId()).isEqualTo(USER_ID);
            assertThat(item.getShopId()).isEqualTo(1L);
            assertThat(item.getQuantity()).isEqualTo(2);
            return 1;
        });

        CartVO vo = service.add(USER_ID, addRequest(1L, 2));
        assertThat(vo).isNotNull();
    }

    @Test
    @DisplayName("重复加购时累加数量")
    void addSameDishAccumulates() {
        CartItem existing = new CartItem();
        existing.setId(5L);
        existing.setUserId(USER_ID);
        existing.setShopId(1L);
        existing.setDishId(1L);
        existing.setQuantity(3);

        when(dishMapper.selectById(1L)).thenReturn(dish(1L, 1L, CommonConstants.DISH_ON, 10, "28.00"));
        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(existing));
        when(shopMapper.selectById(1L)).thenReturn(shop(1L));
        when(cartItemMapper.updateById(any(CartItem.class))).thenAnswer(inv -> {
            CartItem item = inv.getArgument(0);
            assertThat(item.getQuantity()).isEqualTo(5);
            return 1;
        });

        service.add(USER_ID, addRequest(1L, 2));
    }

    @Test
    @DisplayName("加入其他店铺菜品时清空原购物车")
    void addCrossShopClearsCart() {
        CartItem old = new CartItem();
        old.setId(6L);
        old.setUserId(USER_ID);
        old.setShopId(1L);
        old.setDishId(1L);
        old.setQuantity(1);

        when(dishMapper.selectById(9L)).thenReturn(dish(9L, 2L, CommonConstants.DISH_ON, 10, "15.00"));
        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(old));
        when(shopMapper.selectById(2L)).thenReturn(shop(2L));
        when(cartItemMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(cartItemMapper.insert(any(CartItem.class))).thenReturn(1);

        service.add(USER_ID, addRequest(9L, 1));
        org.mockito.Mockito.verify(cartItemMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("空购物车返回零金额")
    void viewEmptyCart() {
        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        CartVO vo = service.view(USER_ID);
        assertThat(vo.getGoodsAmount()).isEqualByComparingTo("0");
        assertThat(vo.getTotalAmount()).isEqualByComparingTo("0");
        assertThat(vo.getItems()).isEmpty();
    }

    @Test
    @DisplayName("购物车金额按售价乘数量累加并加上配送费")
    void viewComputesAmount() {
        CartItem a = new CartItem();
        a.setId(1L);
        a.setUserId(USER_ID);
        a.setShopId(1L);
        a.setDishId(1L);
        a.setQuantity(2);
        CartItem b = new CartItem();
        b.setId(2L);
        b.setUserId(USER_ID);
        b.setShopId(1L);
        b.setDishId(2L);
        b.setQuantity(1);

        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a, b));
        when(shopMapper.selectById(1L)).thenReturn(shop(1L));
        Dish d1 = dish(1L, 1L, CommonConstants.DISH_ON, 10, "28.00");
        d1.setName("宫保鸡丁");
        Dish d2 = dish(2L, 1L, CommonConstants.DISH_ON, 5, "8.00");
        d2.setName("番茄蛋花汤");
        when(dishMapper.selectById(1L)).thenReturn(d1);
        when(dishMapper.selectById(2L)).thenReturn(d2);

        CartVO vo = service.view(USER_ID);
        assertThat(vo.getShopId()).isEqualTo(1L);
        assertThat(vo.getShopName()).isEqualTo("川味小厨");
        assertThat(vo.getGoodsAmount()).isEqualByComparingTo("64.00");
        assertThat(vo.getDeliveryFee()).isEqualByComparingTo("3.00");
        assertThat(vo.getTotalAmount()).isEqualByComparingTo("67.00");
        assertThat(vo.getItems()).hasSize(2);
        assertThat(vo.getItems()).allMatch(CartVO.Item::getAvailable);
    }

    @Test
    @DisplayName("下架或库存不足的菜品标记为不可结算")
    void viewMarksUnavailable() {
        CartItem a = new CartItem();
        a.setId(1L);
        a.setUserId(USER_ID);
        a.setShopId(1L);
        a.setDishId(1L);
        a.setQuantity(9);

        when(cartItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a));
        when(shopMapper.selectById(1L)).thenReturn(shop(1L));
        when(dishMapper.selectById(1L)).thenReturn(dish(1L, 1L, CommonConstants.DISH_ON, 3, "28.00"));

        CartVO vo = service.view(USER_ID);
        assertThat(vo.getItems()).hasSize(1);
        assertThat(vo.getItems().get(0).getAvailable()).isFalse();
    }

    @Test
    @DisplayName("移除他人购物车条目被拒绝")
    void removeOthersItem() {
        CartItem other = new CartItem();
        other.setId(3L);
        other.setUserId(99L);
        other.setShopId(1L);
        other.setDishId(1L);
        other.setQuantity(1);
        when(cartItemMapper.selectById(3L)).thenReturn(other);

        assertThatThrownBy(() -> service.remove(USER_ID, 3L))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.DATA_NOT_OWNED.getCode()));
    }

    @Test
    @DisplayName("清空购物车不校验具体条目")
    void clearAll() {
        when(cartItemMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(3);
        service.remove(USER_ID, null);
        org.mockito.Mockito.verify(cartItemMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("修改数量超过库存时拒绝")
    void updateExceedStock() {
        CartItem item = new CartItem();
        item.setId(1L);
        item.setUserId(USER_ID);
        item.setShopId(1L);
        item.setDishId(1L);
        item.setQuantity(1);
        when(cartItemMapper.selectById(1L)).thenReturn(item);
        when(dishMapper.selectById(1L)).thenReturn(dish(1L, 1L, CommonConstants.DISH_ON, 2, "28.00"));

        assertThatThrownBy(() -> service.updateQuantity(USER_ID, 1L, 5))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.STOCK_NOT_ENOUGH.getCode()));
    }
}
