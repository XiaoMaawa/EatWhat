package com.eatwhat.modules.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 修改购物车项数量请求体。
 */
public class UpdateCartRequest {

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最少 1 份")
    @Max(value = 99, message = "数量最多 99 份")
    private Integer quantity;

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
