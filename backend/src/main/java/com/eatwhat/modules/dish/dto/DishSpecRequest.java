package com.eatwhat.modules.dish.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 菜品规格新增/编辑请求。
 */
public class DishSpecRequest {

    @NotBlank(message = "规格名称不能为空")
    @Size(max = 50, message = "规格名称最多 50 个字符")
    private String name;

    @NotNull(message = "规格价格不能为空")
    @DecimalMin(value = "0.00", message = "规格价格不能为负数")
    private BigDecimal price;

    /** 是否默认规格：0 否 1 是 */
    private Integer isDefault;

    private Integer sort;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
