package com.eatwhat.modules.dish.service;

import com.eatwhat.modules.dish.dto.DishComboRequest;
import com.eatwhat.modules.dish.entity.DishComboItem;

import java.util.List;
import java.util.Map;

/**
 * 菜品套餐服务（商家-28）。套餐本身是一条 dish 记录（is_combo=1，price 为套餐价），
 * 组成项存放在 dish_combo_item。
 */
public interface DishComboService {

    /** 查看某套餐的组成项（商家端） */
    List<DishComboItem> listItems(Long userId, Long comboId);

    /** 全量覆盖式保存套餐组成项（商家端） */
    List<DishComboItem> saveItems(Long userId, Long comboId, DishComboRequest request);

    /** 把菜品标记为套餐或普通菜品 */
    void markCombo(Long userId, Long comboId, Integer isCombo);

    /** 批量查询套餐组成项（顾客端店铺详情用） */
    Map<Long, List<DishComboItem>> mapItemsByCombo(List<Long> comboIds);
}
