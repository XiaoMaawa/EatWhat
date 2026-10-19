package com.eatwhat.modules.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.modules.dish.dto.DishAddonRequest;
import com.eatwhat.modules.dish.dto.DishSpecRequest;
import com.eatwhat.modules.dish.entity.Dish;
import com.eatwhat.modules.dish.entity.DishAddon;
import com.eatwhat.modules.dish.entity.DishSpec;
import com.eatwhat.modules.dish.mapper.DishAddonMapper;
import com.eatwhat.modules.dish.mapper.DishMapper;
import com.eatwhat.modules.dish.mapper.DishSpecMapper;
import com.eatwhat.modules.dish.service.DishSpecService;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.service.ShopService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜品规格与加料实现。商家端写操作全部校验菜品归属本店铺；顾客端提供批量只读。
 */
@Service
public class DishSpecServiceImpl implements DishSpecService {

    private final DishSpecMapper specMapper;
    private final DishAddonMapper addonMapper;
    private final DishMapper dishMapper;
    private final ShopService shopService;

    public DishSpecServiceImpl(DishSpecMapper specMapper, DishAddonMapper addonMapper,
                              DishMapper dishMapper, ShopService shopService) {
        this.specMapper = specMapper;
        this.addonMapper = addonMapper;
        this.dishMapper = dishMapper;
        this.shopService = shopService;
    }

    @Override
    public List<DishSpec> listSpecs(Long userId, Long dishId) {
        requireOwnedDish(userId, dishId);
        return specMapper.selectList(new LambdaQueryWrapper<DishSpec>()
                .eq(DishSpec::getDishId, dishId)
                .orderByAsc(DishSpec::getSort)
                .orderByAsc(DishSpec::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DishSpec createSpec(Long userId, Long dishId, DishSpecRequest request) {
        requireOwnedDish(userId, dishId);
        DishSpec spec = new DishSpec();
        spec.setDishId(dishId);
        applySpec(spec, request);
        specMapper.insert(spec);
        if (Integer.valueOf(1).equals(spec.getIsDefault())) {
            clearOtherDefaults(dishId, spec.getId());
        }
        return spec;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSpec(Long userId, Long dishId, Long specId, DishSpecRequest request) {
        requireOwnedDish(userId, dishId);
        DishSpec spec = requireOwnedSpec(dishId, specId);
        applySpec(spec, request);
        specMapper.updateById(spec);
        if (Integer.valueOf(1).equals(spec.getIsDefault())) {
            clearOtherDefaults(dishId, specId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSpec(Long userId, Long dishId, Long specId) {
        requireOwnedDish(userId, dishId);
        requireOwnedSpec(dishId, specId);
        specMapper.deleteById(specId);
    }

    @Override
    public List<DishAddon> listAddons(Long userId, Long dishId) {
        requireOwnedDish(userId, dishId);
        return addonMapper.selectList(new LambdaQueryWrapper<DishAddon>()
                .eq(DishAddon::getDishId, dishId)
                .orderByAsc(DishAddon::getSort)
                .orderByAsc(DishAddon::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DishAddon createAddon(Long userId, Long dishId, DishAddonRequest request) {
        requireOwnedDish(userId, dishId);
        DishAddon addon = new DishAddon();
        addon.setDishId(dishId);
        applyAddon(addon, request);
        addonMapper.insert(addon);
        return addon;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddon(Long userId, Long dishId, Long addonId, DishAddonRequest request) {
        requireOwnedDish(userId, dishId);
        DishAddon addon = requireOwnedAddon(dishId, addonId);
        applyAddon(addon, request);
        addonMapper.updateById(addon);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAddon(Long userId, Long dishId, Long addonId) {
        requireOwnedDish(userId, dishId);
        requireOwnedAddon(dishId, addonId);
        addonMapper.deleteById(addonId);
    }

    @Override
    public Map<Long, List<DishSpec>> mapSpecsByDish(List<Long> dishIds) {
        Map<Long, List<DishSpec>> result = new LinkedHashMap<>();
        if (dishIds == null || dishIds.isEmpty()) {
            return result;
        }
        List<DishSpec> specs = specMapper.selectList(new LambdaQueryWrapper<DishSpec>()
                .in(DishSpec::getDishId, dishIds)
                .orderByAsc(DishSpec::getSort)
                .orderByAsc(DishSpec::getId));
        for (DishSpec spec : specs) {
            result.computeIfAbsent(spec.getDishId(), k -> new ArrayList<>()).add(spec);
        }
        return result;
    }

    @Override
    public Map<Long, List<DishAddon>> mapAddonsByDish(List<Long> dishIds) {
        Map<Long, List<DishAddon>> result = new LinkedHashMap<>();
        if (dishIds == null || dishIds.isEmpty()) {
            return result;
        }
        List<DishAddon> addons = addonMapper.selectList(new LambdaQueryWrapper<DishAddon>()
                .in(DishAddon::getDishId, dishIds)
                .orderByAsc(DishAddon::getSort)
                .orderByAsc(DishAddon::getId));
        for (DishAddon addon : addons) {
            result.computeIfAbsent(addon.getDishId(), k -> new ArrayList<>()).add(addon);
        }
        return result;
    }

    private void applySpec(DishSpec spec, DishSpecRequest request) {
        if (request.getPrice() == null || request.getPrice().signum() < 0) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "规格价格不能为负数");
        }
        spec.setName(request.getName().trim());
        spec.setPrice(request.getPrice());
        spec.setIsDefault(request.getIsDefault() == null ? 0 : request.getIsDefault());
        spec.setSort(request.getSort() == null ? 0 : request.getSort());
    }

    private void applyAddon(DishAddon addon, DishAddonRequest request) {
        if (request.getPrice() == null || request.getPrice().signum() < 0) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "加料价格不能为负数");
        }
        addon.setName(request.getName().trim());
        addon.setPrice(request.getPrice());
        addon.setSort(request.getSort() == null ? 0 : request.getSort());
    }

    /** 同一菜品只允许一个默认规格 */
    private void clearOtherDefaults(Long dishId, Long keepId) {
        specMapper.update(null, new LambdaUpdateWrapper<DishSpec>()
                .eq(DishSpec::getDishId, dishId)
                .ne(DishSpec::getId, keepId)
                .set(DishSpec::getIsDefault, 0));
    }

    private Dish requireOwnedDish(Long userId, Long dishId) {
        Shop shop = shopService.currentShop(userId);
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw BizException.of(ErrorCode.DISH_NOT_FOUND);
        }
        if (!dish.getShopId().equals(shop.getId())) {
            throw BizException.of(ErrorCode.DATA_NOT_OWNED);
        }
        return dish;
    }

    private DishSpec requireOwnedSpec(Long dishId, Long specId) {
        DishSpec spec = specMapper.selectById(specId);
        if (spec == null || !spec.getDishId().equals(dishId)) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "菜品规格不存在");
        }
        return spec;
    }

    private DishAddon requireOwnedAddon(Long dishId, Long addonId) {
        DishAddon addon = addonMapper.selectById(addonId);
        if (addon == null || !addon.getDishId().equals(dishId)) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "菜品加料不存在");
        }
        return addon;
    }

}
