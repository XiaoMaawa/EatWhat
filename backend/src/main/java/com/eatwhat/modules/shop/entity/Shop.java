package com.eatwhat.modules.shop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eatwhat.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 店铺实体（shop）。
 */
@TableName("shop")
public class Shop extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long merchantId;

    private String name;

    private String logo;

    private String description;

    private String notice;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String phone;

    /** 起送价 */
    private BigDecimal minOrderAmount;

    /** 配送费 */
    private BigDecimal deliveryFee;

    /** 配送费计费方式：0 固定 / 1 按距离阶梯 */
    private Integer deliveryFeeType;

    /** 打包费（商家-14） */
    private java.math.BigDecimal packagingFee;

    /** 配送范围（米） */
    private Integer deliveryRange;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime openTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime closeTime;

    /** 1 营业 0 打烊 */
    private Integer isOpen;

    /** 打烊时展示给顾客的提示语（商家-17） */
    private String closedTip;

    /** 平均评分 */
    private BigDecimal avgScore;

    /** 月销量 */
    private Integer monthSales;

    /** 1 正常 0 冻结 */
    private Integer status;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    /** 与收货地址的距离（公里，非数据库字段） */
    @TableField(exist = false)
    private BigDecimal distance;

    /** 商家审核状态（非数据库字段，后台展示用） */
    @TableField(exist = false)
    private Integer merchantAuditStatus;

    /** 顾客-18：关键词命中的菜品名称（非数据库字段，供列表高亮） */
    @TableField(exist = false)
    private List<String> matchedDishNames;

    public List<String> getMatchedDishNames() {
        return matchedDishNames;
    }

    public void setMatchedDishNames(List<String> matchedDishNames) {
        this.matchedDishNames = matchedDishNames;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

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

    public LocalTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(LocalTime openTime) {
        this.openTime = openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalTime closeTime) {
        this.closeTime = closeTime;
    }

    public Integer getIsOpen() {
        return isOpen;
    }

    public void setIsOpen(Integer isOpen) {
        this.isOpen = isOpen;
    }

    public String getClosedTip() {
        return closedTip;
    }

    public void setClosedTip(String closedTip) {
        this.closedTip = closedTip;
    }

    public BigDecimal getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(BigDecimal avgScore) {
        this.avgScore = avgScore;
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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public BigDecimal getDistance() {
        return distance;
    }

    public void setDistance(BigDecimal distance) {
        this.distance = distance;
    }

    public Integer getMerchantAuditStatus() {
        return merchantAuditStatus;
    }

    public void setMerchantAuditStatus(Integer merchantAuditStatus) {
        this.merchantAuditStatus = merchantAuditStatus;
    }
}
