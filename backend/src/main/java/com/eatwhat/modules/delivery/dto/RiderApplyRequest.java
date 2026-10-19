package com.eatwhat.modules.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 骑手认证申请请求体。
 */
public class RiderApplyRequest {

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "姓名过长")
    private String realName;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;

    @NotBlank(message = "健康证不能为空")
    @Size(max = 255, message = "图片地址过长")
    private String healthImg;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;

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

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
}
