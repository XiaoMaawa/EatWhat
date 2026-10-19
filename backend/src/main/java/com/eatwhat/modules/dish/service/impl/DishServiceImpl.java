package com.eatwhat.modules.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.modules.dish.dto.CategoryRequest;
import com.eatwhat.modules.dish.dto.DishRequest;
import com.eatwhat.modules.dish.entity.Dish;
import com.eatwhat.modules.dish.entity.DishCategory;
import com.eatwhat.modules.dish.mapper.DishCategoryMapper;
import com.eatwhat.modules.dish.mapper.DishMapper;
import com.eatwhat.modules.dish.service.DishService;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.service.ShopService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 菜品与分类管理实现。所有写操作均校验店铺归属。
 */
@Service
public class DishServiceImpl implements DishService {

    private final DishMapper dishMapper;
    private final DishCategoryMapper categoryMapper;
    private final ShopService shopService;

    public DishServiceImpl(DishMapper dishMapper, DishCategoryMapper categoryMapper, ShopService shopService) {
        this.dishMapper = dishMapper;
        this.categoryMapper = categoryMapper;
        this.shopService = shopService;
    }

    @Override
    public List<DishCategory> listCategories(Long userId) {
        Shop shop = shopService.currentShop(userId);
        return categoryMapper.selectList(new LambdaQueryWrapper<DishCategory>()
                .eq(DishCategory::getShopId, shop.getId())
                .orderByAsc(DishCategory::getSort)
                .orderByAsc(DishCategory::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DishCategory createCategory(Long userId, CategoryRequest request) {
        Shop shop = shopService.currentShop(userId);
        String name = request.getName().trim();
        requireUniqueCategoryName(shop.getId(), name, null);
        DishCategory category = new DishCategory();
        category.setShopId(shop.getId());
        category.setName(name);
        category.setSort(request.getSort() == null ? 0 : request.getSort());
        categoryMapper.insert(category);
        return category;
    }

    /**
     * 商家-18 / 商家-19：同一店铺内分类名称唯一（忽略首尾空格与已逻辑删除的分类）。
     */
    private void requireUniqueCategoryName(Long shopId, String name, Long excludeId) {
        Long exists = categoryMapper.selectCount(new LambdaQueryWrapper<DishCategory>()
                .eq(DishCategory::getShopId, shopId)
                .eq(DishCategory::getName, name)
                .ne(excludeId != null, DishCategory::getId, excludeId));
        if (exists != null && exists > 0) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "同一店铺内分类名称不能重复：" + name);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long userId, Long categoryId, CategoryRequest request) {
        Shop shop = shopService.currentShop(userId);
        DishCategory category = requireOwnedCategory(shop.getId(), categoryId);
        String name = request.getName().trim();
        requireUniqueCategoryName(shop.getId(), name, categoryId);
        category.setName(name);
        if (request.getSort() != null) {
            category.setSort(request.getSort());
        }
        categoryMapper.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long userId, Long categoryId) {
        Shop shop = shopService.currentShop(userId);
        requireOwnedCategory(shop.getId(), categoryId);
        Long count = dishMapper.selectCount(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getCategoryId, categoryId));
        if (count != null && count > 0) {
            throw BizException.of(ErrorCode.CATEGORY_NOT_EMPTY);
        }
        categoryMapper.deleteById(categoryId);
    }

    @Override
    public List<Dish> listDishes(Long userId, Long categoryId, String keyword, Integer status) {
        Shop shop = shopService.currentShop(userId);
        return dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getShopId, shop.getId())
                .eq(categoryId != null, Dish::getCategoryId, categoryId)
                .eq(status != null, Dish::getStatus, status)
                .like(keyword != null && !keyword.isBlank(), Dish::getName, keyword == null ? null : keyword.trim())
                .orderByAsc(Dish::getSort)
                .orderByDesc(Dish::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Dish createDish(Long userId, DishRequest request) {
        Shop shop = shopService.currentShop(userId);
        requireOwnedCategory(shop.getId(), request.getCategoryId());
        validatePriceAndStock(request);

        Dish dish = new Dish();
        dish.setShopId(shop.getId());
        applyRequest(dish, request);
        dish.setMonthSales(0);
        dish.setStatus(request.getStock() != null && request.getStock() <= 0 ? 2 : 1);
        dishMapper.insert(dish);
        return dish;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDish(Long userId, Long dishId, DishRequest request) {
        Shop shop = shopService.currentShop(userId);
        Dish dish = requireOwnedDish(shop.getId(), dishId);
        requireOwnedCategory(shop.getId(), request.getCategoryId());
        validatePriceAndStock(request);

        applyRequest(dish, request);
        dish.setStatus(request.getStock() != null && request.getStock() <= 0 ? 2 : 1);
        dishMapper.updateById(dish);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDish(Long userId, Long dishId) {
        Shop shop = shopService.currentShop(userId);
        requireOwnedDish(shop.getId(), dishId);
        dishMapper.deleteById(dishId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDishStatus(Long userId, Long dishId, Integer status) {
        if (status == null || status < 0 || status > 2) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "菜品状态只能为 0/1/2");
        }
        Shop shop = shopService.currentShop(userId);
        Dish dish = requireOwnedDish(shop.getId(), dishId);
        dish.setStatus(status);
        dishMapper.updateById(dish);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(Long userId, List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "请至少选择一个菜品");
        }
        // 商家-30：单次最多 50 个
        if (ids.size() > 50) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "单次批量操作最多 50 个菜品");
        }
        if (status == null || status < 0 || status > 2) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "菜品状态只能为 0/1/2");
        }
        Shop shop = shopService.currentShop(userId);
        List<Dish> dishes = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getShopId, shop.getId())
                .in(Dish::getId, ids));
        if (dishes.size() != ids.size()) {
            throw BizException.of(ErrorCode.DATA_NOT_OWNED, "存在不属于本店铺的菜品");
        }
        for (Dish dish : dishes) {
            dish.setStatus(status);
            dishMapper.updateById(dish);
        }
    }

    private void validatePriceAndStock(DishRequest request) {
        if (request.getPrice() == null || request.getPrice().signum() < 0) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "价格不能为负数");
        }
        if (request.getStock() == null || request.getStock() < 0) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "库存不能为负数");
        }
    }

    private void applyRequest(Dish dish, DishRequest request) {
        dish.setCategoryId(request.getCategoryId());
        dish.setName(request.getName().trim());
        dish.setImage(request.getImage());
        dish.setDescription(request.getDescription());
        dish.setPrice(request.getPrice());
        dish.setStock(request.getStock());
        dish.setSort(request.getSort() == null ? 0 : request.getSort());
    }

    private DishCategory requireOwnedCategory(Long shopId, Long categoryId) {
        DishCategory category = categoryMapper.selectById(categoryId);
        if (category == null || !category.getShopId().equals(shopId)) {
            throw BizException.of(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return category;
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
