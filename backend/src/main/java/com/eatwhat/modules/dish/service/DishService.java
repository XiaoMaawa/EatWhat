package com.eatwhat.modules.dish.service;

import com.eatwhat.modules.dish.dto.CategoryRequest;
import com.eatwhat.modules.dish.dto.DishRequest;
import com.eatwhat.modules.dish.entity.Dish;
import com.eatwhat.modules.dish.entity.DishCategory;

import java.util.List;

/**
 * 菜品与分类管理服务（商家端）。
 */
public interface DishService {

    List<DishCategory> listCategories(Long userId);

    DishCategory createCategory(Long userId, CategoryRequest request);

    void updateCategory(Long userId, Long categoryId, CategoryRequest request);

    void deleteCategory(Long userId, Long categoryId);

    List<Dish> listDishes(Long userId, Long categoryId, String keyword, Integer status);

    Dish createDish(Long userId, DishRequest request);

    void updateDish(Long userId, Long dishId, DishRequest request);

    void deleteDish(Long userId, Long dishId);

    void updateDishStatus(Long userId, Long dishId, Integer status);

    void batchUpdateStatus(Long userId, List<Long> ids, Integer status);
}
