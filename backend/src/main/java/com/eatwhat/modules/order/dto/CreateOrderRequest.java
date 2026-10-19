package com.eatwhat.modules.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建订单请求体。items 为空表示结算该店铺购物车中的全部菜品。
 */
public class CreateOrderRequest {

    @NotNull(message = "店铺不能为空")
    private Long shopId;

    @NotNull(message = "收货地址不能为空")
    private Long addressId;

    @Valid
    private List<Item> items;

    @Size(max = 200, message = "备注不能超过 200 字")
    private String remark;

    /** 餐具份数，0 表示不需要，最多 20 份 */
    @Min(value = 0, message = "餐具份数不能为负")
    @Max(value = 20, message = "餐具份数最多 20 份")
    private Integer tablewareCount;

    public static class Item {
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

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getTablewareCount() {
        return tablewareCount;
    }

    public void setTablewareCount(Integer tablewareCount) {
        this.tablewareCount = tablewareCount;
    }
}
