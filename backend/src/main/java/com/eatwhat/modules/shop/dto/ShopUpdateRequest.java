package com.eatwhat.modules.shop.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 商家编辑店铺信息请求体。
 */
public class ShopUpdateRequest {

    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 100, message = "店铺名称不能超过 100 字")
    private String name;

    @Size(max = 255, message = "Logo 地址过长")
    private String logo;

    @Size(max = 500, message = "店铺简介不能超过 500 字")
    private String description;

    @Size(max = 255, message = "店铺公告不能超过 255 字")
    private String notice;

    @NotBlank(message = "店铺地址不能为空")
    @Size(max = 255, message = "店铺地址过长")
    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    @NotBlank(message = "联系电话不能为空")
    private String phone;

    @NotNull(message = "起送价不能为空")
    @DecimalMin(value = "0", message = "起送价不能为负数")
    private BigDecimal minOrderAmount;

    @NotNull(message = "配送费不能为空")
    @DecimalMin(value = "0", message = "配送费不能为负数")
    private BigDecimal deliveryFee;

    /** 配送费计费方式：0 固定 / 1 按距离阶梯，为空时保持原值 */
    private Integer deliveryFeeType;

    /** 打包费 0-20 元（商家-14） */
    private java.math.BigDecimal packagingFee;

    @NotNull(message = "配送范围不能为空")
    private Integer deliveryRange;

    /** HH:mm 或 HH:mm:ss */
    private String openTime;

    /** HH:mm 或 HH:mm:ss */
    private String closeTime;

    /** 打烊时展示给顾客的提示语（商家-17） */
    @Size(max = 200, message = "休息中提示语不能超过 200 字")
    private String closedTip;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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


    public java.math.BigDecimal getPackagingFee() {
        return packagingFee;
    }

    public void setPackagingFee(java.math.BigDecimal packagingFee) {
        this.packagingFee = packagingFee;
    }
    public Integer getDeliveryFeeType() {
        return deliveryFeeType;
    }

    public void setDeliveryFeeType(Integer deliveryFeeType) {
        this.deliveryFeeType = deliveryFeeType;
    }

    public Integer getDeliveryRange() {
        return deliveryRange;
    }

    public void setDeliveryRange(Integer deliveryRange) {
        this.deliveryRange = deliveryRange;
    }

    public String getOpenTime() {
        return openTime;
    }

    public void setOpenTime(String openTime) {
        this.openTime = openTime;
    }

    public String getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(String closeTime) {
        this.closeTime = closeTime;
    }

    public String getClosedTip() {
        return closedTip;
    }

    public void setClosedTip(String closedTip) {
        this.closedTip = closedTip;
    }
}
