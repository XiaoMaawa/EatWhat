package com.eatwhat.modules.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eatwhat.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 骑手实体（rider）。
 */
@TableName("rider")
public class Rider extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String realName;

    @JsonIgnore
    private String idCard;

    private String healthImg;

    /**
     * 健康证有效期至（骑手-05，到期前 30 天首页提醒）。
     */
    private LocalDate healthExpire;

    private String contactPhone;

    /**
     * 常用配送区域（骑手-28）。
     */
    private String serviceArea;

    /**
     * 1 接单中 / 0 已暂停接单。
     */
    private Integer accepting;

    /**
     * 接单半径（公里）。
     */
    private BigDecimal maxDistance;

    /**
     * 最大同时接单数。
     */
    private Integer maxOrders;

    /** 0 待审核 1 通过 2 驳回 */
    private Integer auditStatus;

    private String auditRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditedAt;

    /** 累计配送单量 */
    private Integer totalOrders;

    /** 累计收益 */
    private BigDecimal totalIncome;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    /** 骑手昵称（非数据库字段） */
    @TableField(exist = false)
    private String nickname;

    /** 骑手手机号（非数据库字段） */
    @TableField(exist = false)
    private String phone;

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

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getHealthImg() {
        return healthImg;
    }

    public void setHealthImg(String healthImg) {
        this.healthImg = healthImg;
    }

    public LocalDate getHealthExpire() {
        return healthExpire;
    }

    public void setHealthExpire(LocalDate healthExpire) {
        this.healthExpire = healthExpire;
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

    public Integer getAccepting() {
        return accepting;
    }

    public void setAccepting(Integer accepting) {
        this.accepting = accepting;
    }

    public BigDecimal getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(BigDecimal maxDistance) {
        this.maxDistance = maxDistance;
    }

    public Integer getMaxOrders() {
        return maxOrders;
    }

    public void setMaxOrders(Integer maxOrders) {
        this.maxOrders = maxOrders;
    }

    public Integer getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(Integer auditStatus) {
        this.auditStatus = auditStatus;
    }

    public String getAuditRemark() {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark) {
        this.auditRemark = auditRemark;
    }

    public LocalDateTime getAuditedAt() {
        return auditedAt;
    }

    public void setAuditedAt(LocalDateTime auditedAt) {
        this.auditedAt = auditedAt;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
