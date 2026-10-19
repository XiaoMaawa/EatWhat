package com.eatwhat.modules.delivery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eatwhat.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 配送异常与转单申请（delivery_exception）。
 */
@TableName("delivery_exception")
public class DeliveryException extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 转单申请 */
    public static final int TYPE_TRANSFER = 1;
    /** 异常上报 */
    public static final int TYPE_REPORT = 2;

    /** 待处理 */
    public static final int STATUS_PENDING = 0;
    /** 已处理 */
    public static final int STATUS_HANDLED = 1;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long orderId;

    private String orderNo;

    private Long riderId;

    /** 1 转单申请 2 异常上报 */
    private Integer type;

    /** 1 联系不上顾客 2 地址错误 3 餐品破损 4 车辆故障 5 天气原因 6 其他 */
    private Integer reasonType;

    private String description;

    /** 凭证图片，逗号分隔 */
    private String images;

    /** 0 待处理 1 已处理 */
    private Integer status;

    private String handleRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handledAt;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    /** 类型描述（非数据库字段） */
    @TableField(exist = false)
    private String typeDesc;

    /** 原因描述（非数据库字段） */
    @TableField(exist = false)
    private String reasonDesc;

    /** 状态描述（非数据库字段） */
    @TableField(exist = false)
    private String statusDesc;

    /** 凭证图片列表（非数据库字段） */
    @TableField(exist = false)
    private List<String> imageList;

    /** 骑手姓名（非数据库字段） */
    @TableField(exist = false)
    private String riderName;

    /** 店铺名称（非数据库字段） */
    @TableField(exist = false)
    private String shopName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
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

    public Long getRiderId() {
        return riderId;
    }

    public void setRiderId(Long riderId) {
        this.riderId = riderId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getReasonType() {
        return reasonType;
    }

    public void setReasonType(Integer reasonType) {
        this.reasonType = reasonType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getHandleRemark() {
        return handleRemark;
    }

    public void setHandleRemark(String handleRemark) {
        this.handleRemark = handleRemark;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getTypeDesc() {
        return type == null ? null : (type == TYPE_TRANSFER ? "转单申请" : "异常上报");
    }

    public void setTypeDesc(String typeDesc) {
        this.typeDesc = typeDesc;
    }

    public String getReasonDesc() {
        if (reasonType == null) {
            return null;
        }
        switch (reasonType) {
            case 1:
                return "联系不上顾客";
            case 2:
                return "地址错误";
            case 3:
                return "餐品破损";
            case 4:
                return "车辆故障";
            case 5:
                return "天气原因";
            default:
                return "其他";
        }
    }

    public void setReasonDesc(String reasonDesc) {
        this.reasonDesc = reasonDesc;
    }

    public String getStatusDesc() {
        return status == null ? null : (status == STATUS_HANDLED ? "已处理" : "待处理");
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }

    public List<String> getImageList() {
        if (imageList == null && images != null && !images.isBlank()) {
            imageList = new ArrayList<>(Arrays.asList(images.split(",")));
        }
        return imageList;
    }

    public void setImageList(List<String> imageList) {
        this.imageList = imageList;
        this.images = imageList == null || imageList.isEmpty() ? null : String.join(",", imageList);
    }

    public String getRiderName() {
        return riderName;
    }

    public void setRiderName(String riderName) {
        this.riderName = riderName;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }
}
