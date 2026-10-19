package com.eatwhat.modules.aftersale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 提交售后申请（退款 / 申诉）请求体。
 */
public class AfterSaleApplyRequest {

    @NotNull(message = "订单不能为空")
    private Long orderId;

    /** 1 退款 2 申诉，为空时按订单状态自动推断 */
    private Integer type;

    @NotBlank(message = "请填写申请原因")
    @Size(min = 5, max = 500, message = "申请原因需 5 到 500 字")
    private String reason;

    @Size(max = 3, message = "凭证图片最多 3 张")
    private List<String> images;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}
