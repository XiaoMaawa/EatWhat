package com.eatwhat.modules.dish.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 套餐配置请求体（商家-28）。套餐本身仍是一条 dish 记录（is_combo=1），
 * 价格取套餐价，组成项记录在 dish_combo_item。
 */
public class DishComboRequest {

    @NotEmpty(message = "套餐至少需要一个菜品")
    @Valid
    private List<Item> items;

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    /**
     * 套餐组成项。
     */
    public static class Item {

        @NotNull(message = "组成菜品不能为空")
        private Long dishId;

        @NotNull(message = "份数不能为空")
        @Min(value = 1, message = "份数最少 1 份")
        @Max(value = 20, message = "份数最多 20 份")
        private Integer quantity;

        private Integer sort;

        public Long getDishId() {
            return dishId;
        }

        public void setDishId(Long dishId) {
            this.dishId = dishId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Integer getSort() {
            return sort;
        }

        public void setSort(Integer sort) {
            this.sort = sort;
        }
    }
}
