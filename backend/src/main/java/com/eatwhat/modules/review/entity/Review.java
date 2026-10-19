package com.eatwhat.modules.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eatwhat.common.entity.CreatedEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

/**
 * 订单评价实体（review）。
 */
@TableName("review")
public class Review extends CreatedEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long userId;

    private Long shopId;

    private Long riderId;

    private Integer shopScore;

    private Integer deliveryScore;

    private String content;

    /** 逗号分隔的图片地址 */
    private String images;

    private Integer isAnonymous;

    private String reply;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repliedAt;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    /** 评价人昵称（非数据库字段） */
    @TableField(exist = false)
    private String nickname;

    /** 评价人头像（非数据库字段） */
    @TableField(exist = false)
    private String avatar;

    /** 店铺名称（非数据库字段） */
    @TableField(exist = false)
    private String shopName;

    /** 订单号（非数据库字段） */
    @TableField(exist = false)
    private String orderNo;

    /** 图片列表（非数据库字段） */
    @TableField(exist = false)
    private java.util.List<String> imageList;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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

    public Long getRiderId() {
        return riderId;
    }

    public void setRiderId(Long riderId) {
        this.riderId = riderId;
    }

    public Integer getShopScore() {
        return shopScore;
    }

    public void setShopScore(Integer shopScore) {
        this.shopScore = shopScore;
    }

    public Integer getDeliveryScore() {
        return deliveryScore;
    }

    public void setDeliveryScore(Integer deliveryScore) {
        this.deliveryScore = deliveryScore;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public Integer getIsAnonymous() {
        return isAnonymous;
    }

    public void setIsAnonymous(Integer isAnonymous) {
        this.isAnonymous = isAnonymous;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public LocalDateTime getRepliedAt() {
        return repliedAt;
    }

    public void setRepliedAt(LocalDateTime repliedAt) {
        this.repliedAt = repliedAt;
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

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public java.util.List<String> getImageList() {
        return imageList;
    }

    public void setImageList(java.util.List<String> imageList) {
        this.imageList = imageList;
    }
}
