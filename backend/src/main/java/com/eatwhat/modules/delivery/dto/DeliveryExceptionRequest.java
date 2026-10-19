package com.eatwhat.modules.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 骑手提交转单申请或配送异常上报的请求体。
 */
public class DeliveryExceptionRequest {

    /** 1 转单申请 2 异常上报，为空时按异常上报处理 */
    private Integer type;

    /** 1 联系不上顾客 2 地址错误 3 餐品破损 4 车辆故障 5 天气原因 6 其他 */
    private Integer reasonType;

    @NotBlank(message = "请填写情况说明")
    @Size(min = 5, max = 500, message = "情况说明需在 5 至 500 字之间")
    private String description;

    @Size(max = 3, message = "最多上传 3 张凭证图片")
    private List<String> images;

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

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}
