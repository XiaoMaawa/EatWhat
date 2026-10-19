package com.eatwhat.modules.aftersale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 商家处理售后请求体。
 */
public class AfterSaleHandleRequest {

    /** APPROVE 同意 / REJECT 拒绝 */
    @NotBlank(message = "请选择处理结果")
    private String action;

    @Size(max = 500, message = "处理意见不能超过 500 字")
    private String reply;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }
}
