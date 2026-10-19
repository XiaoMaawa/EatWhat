package com.eatwhat.modules.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 加入购物车请求体。支持选择菜品规格（单选）与加料（多选，最多 5 项）。
 */
public class AddCartRequest {

    @NotNull(message = "菜品不能为空")
    private Long dishId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最少 1 份")
    @Max(value = 99, message = "数量最多 99 份")
    private Integer quantity;

    /** 规格ID，为空时取该菜品的默认规格（无规格则用菜品原价） */
    private Long specId;

    /** 加料ID列表，单个菜品最多 5 项 */
    @Size(max = 5, message = "单个菜品最多选择 5 项加料")
    private List<Long> addonIds;

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

    public Long getSpecId() {
        return specId;
    }

    public void setSpecId(Long specId) {
        this.specId = specId;
    }

    public List<Long> getAddonIds() {
        return addonIds;
    }

    public void setAddonIds(List<Long> addonIds) {
        this.addonIds = addonIds;
    }
}
