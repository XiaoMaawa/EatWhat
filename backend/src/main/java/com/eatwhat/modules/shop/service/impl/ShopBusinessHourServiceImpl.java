package com.eatwhat.modules.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.utils.TimeUtils;
import com.eatwhat.modules.shop.dto.BusinessHourRequest;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.entity.ShopBusinessHour;
import com.eatwhat.modules.shop.mapper.ShopBusinessHourMapper;
import com.eatwhat.modules.shop.service.ShopBusinessHourService;
import com.eatwhat.modules.shop.service.ShopService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 商家-08：店铺营业时段实现。
 */
@Service
public class ShopBusinessHourServiceImpl implements ShopBusinessHourService {

    private final ShopBusinessHourMapper businessHourMapper;
    private final ShopService shopService;

    public ShopBusinessHourServiceImpl(ShopBusinessHourMapper businessHourMapper,
                                       ShopService shopService) {
        this.businessHourMapper = businessHourMapper;
        this.shopService = shopService;
    }

    @Override
    public List<ShopBusinessHour> listByShop(Long shopId) {
        if (shopId == null) {
            return new ArrayList<>();
        }
        List<ShopBusinessHour> list = businessHourMapper.selectList(
                new LambdaQueryWrapper<ShopBusinessHour>()
                        .eq(ShopBusinessHour::getShopId, shopId)
                        .orderByAsc(ShopBusinessHour::getDayOfWeek)
                        .orderByAsc(ShopBusinessHour::getOpenTime));
        return list == null ? new ArrayList<>() : list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ShopBusinessHour> save(Long userId, BusinessHourRequest request) {
        Shop shop = shopService.currentShop(userId);
        if (shop == null) {
            throw BizException.of(ErrorCode.SHOP_NOT_FOUND);
        }
        List<BusinessHourRequest.Item> items = request == null ? null : request.getItems();
        if (items == null) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "营业时段列表不能为空");
        }
        if (items.size() > 21) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "营业时段最多 21 条");
        }
        // 覆盖式保存：先物理清理旧配置，避免逻辑删除残留导致唯一性判断出错
        businessHourMapper.delete(new LambdaQueryWrapper<ShopBusinessHour>()
                .eq(ShopBusinessHour::getShopId, shop.getId()));
        List<ShopBusinessHour> saved = new ArrayList<>();
        for (BusinessHourRequest.Item item : items) {
            Integer day = item.getDayOfWeek();
            if (day == null || day < 1 || day > 7) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "星期取值 1 至 7");
            }
            LocalTime open = TimeUtils.parseClock(item.getOpenTime());
            LocalTime close = TimeUtils.parseClock(item.getCloseTime());
            if (open == null || close == null) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "营业起止时间不能为空");
            }
            if (open.equals(close)) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "营业起止时间不能相同");
            }
            ShopBusinessHour entity = new ShopBusinessHour();
            entity.setShopId(shop.getId());
            entity.setDayOfWeek(day);
            entity.setOpenTime(open);
            entity.setCloseTime(close);
            businessHourMapper.insert(entity);
            saved.add(entity);
        }
        saved.sort(Comparator.comparing(ShopBusinessHour::getDayOfWeek)
                .thenComparing(ShopBusinessHour::getOpenTime));
        return saved;
    }

    @Override
    public Boolean openNow(Long shopId) {
        List<ShopBusinessHour> list = listByShop(shopId);
        if (list.isEmpty()) {
            return null;
        }
        int today = java.time.LocalDate.now().getDayOfWeek().getValue();
        int yesterday = today == 1 ? 7 : today - 1;
        LocalTime now = LocalTime.now();
        for (ShopBusinessHour hour : list) {
            LocalTime open = hour.getOpenTime();
            LocalTime close = hour.getCloseTime();
            if (open == null || close == null) {
                continue;
            }
            boolean crossMidnight = open.isAfter(close);
            if (hour.getDayOfWeek() != null && hour.getDayOfWeek() == today) {
                if (!crossMidnight && !now.isBefore(open) && now.isBefore(close)) {
                    return true;
                }
                if (crossMidnight && !now.isBefore(open)) {
                    return true;
                }
            }
            // 前一天跨零点时段延续到当天凌晨
            if (hour.getDayOfWeek() != null && hour.getDayOfWeek() == yesterday
                    && crossMidnight && now.isBefore(close)) {
                return true;
            }
        }
        return false;
    }
}
