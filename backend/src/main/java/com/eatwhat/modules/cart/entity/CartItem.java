package com.eatwhat.modules.cart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eatwhat.common.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * 购物车项实体（cart_item）。该表无逻辑删除字段。
 */
@TableName("cart_item")
public class CartItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long shopId;

    private Long dishId;
    private Long specId;

    private String specName;

    /** 加料ID，逗号分隔 */
    private String addonIds;

    /** 加料名称，逗号分隔 */
    private String addonNames;

    /** 加料合计价格 */
    private BigDecimal addonPrice;

    /** 含规格与加料的单价 */
    private BigDecimal unitPrice;

    private Integer quantity;

    /** 冗余快照：加购时菜品单价（非数据库字段，展示用） */
    @TableField(exist = false)
    private BigDecimal price;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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

    public String getAddonIds() {
        return addonIds;
    }

    public void setAddonIds(String addonIds) {
        this.addonIds = addonIds;
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

}
