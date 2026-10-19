package com.eatwhat.modules.dish.service;

import com.eatwhat.modules.dish.dto.DishAddonRequest;
import com.eatwhat.modules.dish.dto.DishSpecRequest;
import com.eatwhat.modules.dish.entity.DishAddon;
import com.eatwhat.modules.dish.entity.DishSpec;

import java.util.List;

/**
 * 菜品规格与加料管理服务（商家端 + 顾客端只读）。
 */
public interface DishSpecService {

    /** 商家查询自己菜品下的规格 */
    List<DishSpec> listSpecs(Long userId, Long dishId);

    DishSpec createSpec(Long userId, Long dishId, DishSpecRequest request);

    void updateSpec(Long userId, Long dishId, Long specId, DishSpecRequest request);

    void deleteSpec(Long userId, Long dishId, Long specId);

    /** 商家查询自己菜品下的加料 */
    List<DishAddon> listAddons(Long userId, Long dishId);

    DishAddon createAddon(Long userId, Long dishId, DishAddonRequest request);

    void updateAddon(Long userId, Long dishId, Long addonId, DishAddonRequest request);

    void deleteAddon(Long userId, Long dishId, Long addonId);

    /** 顾客端：按菜品批量取规格（key 为 dishId） */
    java.util.Map<Long, List<DishSpec>> mapSpecsByDish(List<Long> dishIds);

    /** 顾客端：按菜品批量取加料（key 为 dishId） */
    java.util.Map<Long, List<DishAddon>> mapAddonsByDish(List<Long> dishIds);
}
