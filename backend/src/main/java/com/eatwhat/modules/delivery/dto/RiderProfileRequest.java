package com.eatwhat.modules.delivery.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 骑手资料更新请求体（骑手-28）。
 */
public class RiderProfileRequest {

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;

    @Size(max = 200, message = "常用配送区域过长")
    private String serviceArea;

    /** 常驻位置经度（GCJ-02，地图选点） */
    private java.math.BigDecimal longitude;

    /** 常驻位置纬度（GCJ-02，地图选点） */
    private java.math.BigDecimal latitude;

    /** 健康证有效期（yyyy-MM-dd），骑手-05 */
    private String healthExpire;

    public java.math.BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(java.math.BigDecimal longitude) {
        this.longitude = longitude;
    }

    public java.math.BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(java.math.BigDecimal latitude) {
        this.latitude = latitude;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getServiceArea() {
        return serviceArea;
    }

    public void setServiceArea(String serviceArea) {
        this.serviceArea = serviceArea;
    }

    public String getHealthExpire() {
        return healthExpire;
    }

    public void setHealthExpire(String healthExpire) {
        this.healthExpire = healthExpire;
    }
}