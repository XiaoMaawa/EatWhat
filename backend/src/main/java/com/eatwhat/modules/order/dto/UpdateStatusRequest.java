package com.eatwhat.modules.order.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 订单状态变更请求体。
 */
public class UpdateStatusRequest {

    @NotNull(message = "目标状态不能为空")
    private Integer targetStatus;

    private String reason;

    public Integer getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(Integer targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
