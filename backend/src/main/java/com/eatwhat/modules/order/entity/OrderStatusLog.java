package com.eatwhat.modules.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eatwhat.common.entity.CreatedEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 订单状态流转日志实体（order_status_log）。
 * 该表只有 created_at，无 updated_at / deleted。
 */
@TableName("order_status_log")
public class OrderStatusLog extends CreatedEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    /** 原状态，首次创建时为 null */
    private Integer fromStatus;

    private Integer toStatus;

    /** 操作者 ID，0 表示系统 */
    private Long operatorId;

    /** CONSUMER / MERCHANT / RIDER / ADMIN / SYSTEM */
    private String operatorRole;

    private String remark;

    /** 状态中文描述（非数据库字段） */
    @TableField(exist = false)
    private String statusDesc;

    /** 前端展示用的时间别名（非数据库字段） */
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAtAlias;

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

    public Integer getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(Integer fromStatus) {
        this.fromStatus = fromStatus;
    }

    public Integer getToStatus() {
        return toStatus;
    }

    public void setToStatus(Integer toStatus) {
        this.toStatus = toStatus;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorRole() {
        return operatorRole;
    }

    public void setOperatorRole(String operatorRole) {
        this.operatorRole = operatorRole;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getStatusDesc() {
        return statusDesc;
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
    }

    public LocalDateTime getCreatedAtAlias() {
        return createdAtAlias;
    }

    public void setCreatedAtAlias(LocalDateTime createdAtAlias) {
        this.createdAtAlias = createdAtAlias;
    }
}
