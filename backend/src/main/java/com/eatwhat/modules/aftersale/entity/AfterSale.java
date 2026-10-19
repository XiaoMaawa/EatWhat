package com.eatwhat.modules.aftersale.entity;

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

/**
 * 售后申请实体（after_sale）。同时承载「退款申请」与「订单申诉」两类诉求。
 */
@TableName("after_sale")
public class AfterSale extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String afterSaleNo;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private Long shopId;

    private Long merchantId;

    /** 1 退款 2 申诉 */
    private Integer type;

    private String reason;

    /** 凭证图片，逗号分隔 */
    private String images;

    private BigDecimal amount;

    /** 0 待商家处理 1 商家同意 2 商家拒绝 3 已撤销 4 待平台仲裁 5 支持顾客 6 支持商家 */
    private Integer status;

    private String merchantReply;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime merchantHandledAt;

    private String adminVerdict;

    private Long adminId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime adminVerdictAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime refundedAt;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    /** 状态中文描述（非数据库字段） */
    @TableField(exist = false)
    private String statusDesc;

    /** 售后类型中文描述（非数据库字段） */
    @TableField(exist = false)
    private String typeDesc;

    /** 申请凭证图片数组（非数据库字段） */
    @TableField(exist = false)
    private java.util.List<String> imageList;

    /** 订单实付金额（非数据库字段） */
    @TableField(exist = false)
    private BigDecimal orderAmount;

    /** 顾客昵称（非数据库字段，管理端列表用） */
    @TableField(exist = false)
    private String nickname;

    /** 店铺名称（非数据库字段） */
    @TableField(exist = false)
    private String shopName;

    /** 售后进度节点（非数据库字段） */
    @TableField(exist = false)
    private java.util.List<java.util.Map<String, Object>> progress;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAfterSaleNo() {
        return afterSaleNo;
    }

    public void setAfterSaleNo(String afterSaleNo) {
        this.afterSaleNo = afterSaleNo;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
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

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMerchantReply() {
        return merchantReply;
    }

    public void setMerchantReply(String merchantReply) {
        this.merchantReply = merchantReply;
    }

    public LocalDateTime getMerchantHandledAt() {
        return merchantHandledAt;
    }

    public void setMerchantHandledAt(LocalDateTime merchantHandledAt) {
        this.merchantHandledAt = merchantHandledAt;
    }

    public String getAdminVerdict() {
        return adminVerdict;
    }

    public void setAdminVerdict(String adminVerdict) {
        this.adminVerdict = adminVerdict;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public LocalDateTime getAdminVerdictAt() {
        return adminVerdictAt;
    }

    public void setAdminVerdictAt(LocalDateTime adminVerdictAt) {
        this.adminVerdictAt = adminVerdictAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getStatusDesc() {
        return statusDesc;
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }

    public String getTypeDesc() {
        return typeDesc;
    }

    public void setTypeDesc(String typeDesc) {
        this.typeDesc = typeDesc;
    }

    public java.util.List<String> getImageList() {
        return imageList;
    }

    public void setImageList(java.util.List<String> imageList) {
        this.imageList = imageList;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public java.util.List<java.util.Map<String, Object>> getProgress() {
        return progress;
    }

    public void setProgress(java.util.List<java.util.Map<String, Object>> progress) {
        this.progress = progress;
    }
}
