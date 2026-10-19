package com.eatwhat.modules.dish.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eatwhat.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;

/**
 * 菜品实体（dish）。
 */
@TableName("dish")
public class Dish extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shopId;

    private Long categoryId;

    private String name;

    private String image;

    private String description;

    private BigDecimal price;

    /** 库存，0 表示售罄 */
    private Integer stock;

    /** 月销量 */
    private Integer monthSales;

    /** 0 下架 1 在售 2 售罄 */
    private Integer status;

    private Integer sort;

    /** 商家-28：是否套餐 0 否 1 是 */
    private Integer isCombo;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    /** 分类名称（非数据库字段） */
    @TableField(exist = false)
    private String categoryName;

    /** 规格列表（非数据库字段，顾客端菜品详情用） */
    @TableField(exist = false)
    private java.util.List<DishSpec> specs;

    /** 加料列表（非数据库字段，顾客端菜品详情用） */
    @TableField(exist = false)
    private java.util.List<DishAddon> addons;

    /** 近 30 天好评率（非数据库字段，保留一位小数） */
    @TableField(exist = false)
    private java.math.BigDecimal goodRate;

    /** 商家-28：套餐组成项（非数据库字段） */
    @TableField(exist = false)
    private java.util.List<DishComboItem> comboItems;

    /** 商家-28：套餐内菜品原价合计（非数据库字段） */
    @TableField(exist = false)
    private java.math.BigDecimal comboOriginalAmount;

    /** 所属店铺名称（非数据库字段，后台展示用） */
    @TableField(exist = false)
    private String shopName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getMonthSales() {
        return monthSales;
    }

    public void setMonthSales(Integer monthSales) {
        this.monthSales = monthSales;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }
    public java.util.List<DishSpec> getSpecs() {
        return specs;
    }

    public void setSpecs(java.util.List<DishSpec> specs) {
        this.specs = specs;
    }

    public java.util.List<DishAddon> getAddons() {
        return addons;
    }

    public void setAddons(java.util.List<DishAddon> addons) {
        this.addons = addons;
    }

    public java.math.BigDecimal getGoodRate() {
        return goodRate;
    }

    public void setGoodRate(java.math.BigDecimal goodRate) {
        this.goodRate = goodRate;
    }

    public Integer getIsCombo() {
        return isCombo;
    }

    public void setIsCombo(Integer isCombo) {
        this.isCombo = isCombo;
    }

    public java.util.List<DishComboItem> getComboItems() {
        return comboItems;
    }

    public void setComboItems(java.util.List<DishComboItem> comboItems) {
        this.comboItems = comboItems;
    }

    public java.math.BigDecimal getComboOriginalAmount() {
        return comboOriginalAmount;
    }

    public void setComboOriginalAmount(java.math.BigDecimal comboOriginalAmount) {
        this.comboOriginalAmount = comboOriginalAmount;
    }

}
