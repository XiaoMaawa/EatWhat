package com.eatwhat.modules.cart.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车视图。
 */
public class CartVO {

    private Long shopId;
    private String shopName;
    private BigDecimal minOrderAmount;
    private BigDecimal deliveryFee;

    /** 打包费（商家-14，随店铺展示） */
    private BigDecimal packagingFee;
    private List<Item> items = new ArrayList<>();
    private BigDecimal goodsAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public static class Item {
        private Long id;
        private Long dishId;
        private String dishName;
        private String dishImage;
        private Long specId;

        private String specName;

        private String addonNames;

        private BigDecimal addonPrice;

        /** 含规格与加料的单价 */
        private BigDecimal unitPrice;
        private BigDecimal price;
        private Integer quantity;
        private Integer stock;
        /** 是否可结算（在售且库存充足） */
        private Boolean available;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getDishId() {
            return dishId;
        }

        public void setDishId(Long dishId) {
            this.dishId = dishId;
        }

        public String getDishName() {
            return dishName;
        }

        public void setDishName(String dishName) {
            this.dishName = dishName;
        }

        public String getDishImage() {
            return dishImage;
        }

        public void setDishImage(String dishImage) {
            this.dishImage = dishImage;
        }
        public Long getSpecId() {
            return specId;
        }

        public void setSpecId(Long specId) {
            this.specId = specId;
        }

        public String getSpecName() {
            return specName;
        }

        public void setSpecName(String specName) {
            this.specName = specName;
        }

        public String getAddonNames() {
            return addonNames;
        }

        public void setAddonNames(String addonNames) {
            this.addonNames = addonNames;
        }

        public BigDecimal getAddonPrice() {
            return addonPrice;
        }

        public void setAddonPrice(BigDecimal addonPrice) {
            this.addonPrice = addonPrice;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }


        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Integer getStock() {
            return stock;
        }

        public void setStock(Integer stock) {
            this.stock = stock;
        }

        public Boolean getAvailable() {
            return available;
        }

        public void setAvailable(Boolean available) {
            this.available = available;
        }
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public BigDecimal getPackagingFee() {
        return packagingFee;
    }

    public void setPackagingFee(BigDecimal packagingFee) {
        this.packagingFee = packagingFee;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public BigDecimal getGoodsAmount() {
        return goodsAmount;
    }

    public void setGoodsAmount(BigDecimal goodsAmount) {
        this.goodsAmount = goodsAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
