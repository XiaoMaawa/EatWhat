package com.eatwhat.modules.shop.service;

import com.eatwhat.modules.shop.dto.BusinessHourRequest;
import com.eatwhat.modules.shop.entity.ShopBusinessHour;

import java.util.List;

/**
 * 商家-08：店铺营业时段服务。
 */
public interface ShopBusinessHourService {

    /**
     * 查询某店铺全部营业时段，按星期与开始时间升序。
     */
    List<ShopBusinessHour> listByShop(Long shopId);

    /**
     * 商家覆盖式保存营业时段。
     */
    List<ShopBusinessHour> save(Long userId, BusinessHourRequest request);

    /**
     * 判断当前时刻是否处于店铺任一营业时段内；无时段配置时返回 null 表示不限制。
     */
    Boolean openNow(Long shopId);
}
