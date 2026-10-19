package com.eatwhat.modules.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.modules.dish.dto.DishComboRequest;
import com.eatwhat.modules.dish.entity.Dish;
import com.eatwhat.modules.dish.entity.DishComboItem;
import com.eatwhat.modules.dish.mapper.DishComboItemMapper;
import com.eatwhat.modules.dish.mapper.DishMapper;
import com.eatwhat.modules.dish.service.DishComboService;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.service.ShopService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 菜品套餐实现。所有写操作校验套餐与组成菜品均属于当前商家的店铺。
 */
@Service
public class DishComboServiceImpl implements DishComboService {

    private final DishComboItemMapper comboItemMapper;
    private final DishMapper dishMapper;
    private final ShopService shopService;

    public DishComboServiceImpl(DishComboItemMapper comboItemMapper, DishMapper dishMapper,
                                ShopService shopService) {
        this.comboItemMapper = comboItemMapper;
        this.dishMapper = dishMapper;
        this.shopService = shopService;
    }

    @Override
    public List<DishComboItem> listItems(Long userId, Long comboId) {
        Shop shop = shopService.currentShop(userId);
        requireOwnedDish(shop.getId(), comboId);
        return selectItems(comboId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DishComboItem> saveItems(Long userId, Long comboId, DishComboRequest request) {
        Shop shop = shopService.currentShop(userId);
        Dish combo = requireOwnedDish(shop.getId(), comboId);
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "套餐至少需要一个菜品");
        }

        Set<Long> seen = new LinkedHashSet<>();
        List<DishComboItem> items = new ArrayList<>();
        BigDecimal originalAmount = BigDecimal.ZERO;
        int index = 0;
        for (DishComboRequest.Item item : request.getItems()) {
            if (item == null || item.getDishId() == null) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "套餐组成菜品不能为空");
            }
            if (item.getDishId().equals(comboId)) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "套餐不能包含自己");
            }
            if (!seen.add(item.getDishId())) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "同一菜品不能重复加入套餐");
            }
            Dish dish = dishMapper.selectById(item.getDishId());
            if (dish == null) {
                throw BizException.of(ErrorCode.DISH_NOT_FOUND);
            }
            if (!dish.getShopId().equals(shop.getId())) {
                throw BizException.of(ErrorCode.DATA_NOT_OWNED, "套餐只能包含本店铺的菜品");
            }
            int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
            if (quantity < 1 || quantity > 20) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "套餐内菜品份数需在 1 至 20 之间");
            }
            DishComboItem comboItem = new DishComboItem();
            comboItem.setComboId(comboId);
            comboItem.setDishId(dish.getId());
            comboItem.setDishName(dish.getName());
            comboItem.setQuantity(quantity);
            comboItem.setSort(item.getSort() == null ? index : item.getSort());
            items.add(comboItem);
            if (dish.getPrice() != null) {
                originalAmount = originalAmount.add(dish.getPrice().multiply(BigDecimal.valueOf(quantity)));
            }
            index++;
        }

        // 套餐价不高于原价合计，避免「套餐比单点还贵」
        if (combo.getPrice() != null && originalAmount.signum() > 0
                && combo.getPrice().compareTo(originalAmount) > 0) {
            throw BizException.of(ErrorCode.PARAM_INVALID,
                    "套餐价 ¥" + combo.getPrice() + " 高于组成菜品原价合计 ¥" + originalAmount.setScale(2));
        }

        // 全量覆盖：先物理清空旧组成项，再插入新组成项
        comboItemMapper.delete(new LambdaQueryWrapper<DishComboItem>()
                .eq(DishComboItem::getComboId, comboId));
        for (DishComboItem comboItem : items) {
            comboItemMapper.insert(comboItem);
        }
        combo.setIsCombo(1);
        dishMapper.updateById(combo);
        return selectItems(comboId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markCombo(Long userId, Long comboId, Integer isCombo) {
        if (isCombo == null || (isCombo != 0 && isCombo != 1)) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "套餐标记只能为 0 或 1");
        }
        Shop shop = shopService.currentShop(userId);
        Dish combo = requireOwnedDish(shop.getId(), comboId);
        combo.setIsCombo(isCombo);
        dishMapper.updateById(combo);
        if (isCombo == 0) {
            comboItemMapper.delete(new LambdaQueryWrapper<DishComboItem>()
                    .eq(DishComboItem::getComboId, comboId));
        }
    }

    @Override
    public Map<Long, List<DishComboItem>> mapItemsByCombo(List<Long> comboIds) {
        Map<Long, List<DishComboItem>> result = new LinkedHashMap<>();
        if (comboIds == null || comboIds.isEmpty()) {
            return result;
        }
        List<DishComboItem> items = comboItemMapper.selectList(new LambdaQueryWrapper<DishComboItem>()
                .in(DishComboItem::getComboId, comboIds)
                .orderByAsc(DishComboItem::getSort)
                .orderByAsc(DishComboItem::getId));
        for (DishComboItem item : items) {
            result.computeIfAbsent(item.getComboId(), k -> new ArrayList<>()).add(item);
        }
        return result;
    }

    private List<DishComboItem> selectItems(Long comboId) {
        return comboItemMapper.selectList(new LambdaQueryWrapper<DishComboItem>()
                .eq(DishComboItem::getComboId, comboId)
                .orderByAsc(DishComboItem::getSort)
                .orderByAsc(DishComboItem::getId));
    }

    private Dish requireOwnedDish(Long shopId, Long dishId) {
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw BizException.of(ErrorCode.DISH_NOT_FOUND);
        }
        if (!dish.getShopId().equals(shopId)) {
            throw BizException.of(ErrorCode.DATA_NOT_OWNED);
        }
        return dish;
    }
}
