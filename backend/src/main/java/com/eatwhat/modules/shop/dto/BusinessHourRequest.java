package com.eatwhat.modules.shop.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 商家-08：店铺营业时段设置请求。
 */
public class BusinessHourRequest {

    /** 营业时段列表，空列表表示清空（回落到店铺整体 openTime/closeTime） */
    @Valid
    @Size(max = 21, message = "营业时段最多 21 条")
    private List<Item> items;

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public static class Item {

        @NotNull(message = "星期不能为空")
        @Min(value = 1, message = "星期取值 1 至 7")
        @Max(value = 7, message = "星期取值 1 至 7")
        private Integer dayOfWeek;

        /** HH:mm 或 HH:mm:ss */
        @NotNull(message = "开始营业时间不能为空")
        private String openTime;

        @NotNull(message = "结束营业时间不能为空")
        private String closeTime;

        public Integer getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(Integer dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public String getOpenTime() {
            return openTime;
        }

        public void setOpenTime(String openTime) {
            this.openTime = openTime;
        }

        public String getCloseTime() {
            return closeTime;
        }

        public void setCloseTime(String closeTime) {
            this.closeTime = closeTime;
        }
    }
}
