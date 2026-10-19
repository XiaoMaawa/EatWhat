package com.eatwhat.modules.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 加入购物车请求体。
 */
public class AddCartRequest {

    @NotNull(message = "菜品不能为空")
    private Long dishId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最少 1 份")
    @Max(value = 99, message = "数量最多 99 份")
    private Integer quantity;

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
}
