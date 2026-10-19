package com.eatwhat.modules.dish.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eatwhat.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 菜品套餐组成项（dish_combo_item）。商家-28：把多个菜品组合为套餐并设置套餐价。
 */
@TableName("dish_combo_item")
public class DishComboItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 套餐菜品 ID（dish.id，该菜品的 is_combo=1） */
    private Long comboId;

    /** 组成菜品 ID */
    private Long dishId;

    /** 组成菜品名称快照 */
    private String dishName;

    /** 份数 */
    private Integer quantity;

    private Integer sort;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getComboId() {
        return comboId;
    }

    public void setComboId(Long comboId) {
        this.comboId = comboId;
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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
