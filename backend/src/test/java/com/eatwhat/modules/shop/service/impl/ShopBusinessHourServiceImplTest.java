package com.eatwhat.modules.shop.service.impl;

import com.eatwhat.common.exception.BizException;
import com.eatwhat.modules.shop.dto.BusinessHourRequest;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.entity.ShopBusinessHour;
import com.eatwhat.modules.shop.mapper.ShopBusinessHourMapper;
import com.eatwhat.modules.shop.service.ShopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商家-08：店铺营业时段单元测试。
 */
@DisplayName("店铺营业时段")
class ShopBusinessHourServiceImplTest {

    private ShopBusinessHourMapper mapper;
    private ShopService shopService;
    private ShopBusinessHourServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(ShopBusinessHourMapper.class);
        shopService = mock(ShopService.class);
        service = new ShopBusinessHourServiceImpl(mapper, shopService);
        Shop shop = new Shop();
        shop.setId(1L);
        when(shopService.currentShop(anyLong())).thenReturn(shop);
    }

    @Test
    @DisplayName("无时段配置时 openNow 返回 null 表示不限制")
    void openNowReturnsNullWhenNoConfig() {
        when(mapper.selectList(any())).thenReturn(List.of());
        assertThat(service.openNow(1L)).isNull();
    }

    @Test
    @DisplayName("当前星期命中时段返回 true")
    void openNowMatchesTodayHour() {
        int today = java.time.LocalDate.now().getDayOfWeek().getValue();
        ShopBusinessHour hour = new ShopBusinessHour();
        hour.setDayOfWeek(today);
        hour.setOpenTime(LocalTime.of(0, 0));
        hour.setCloseTime(LocalTime.of(23, 59));
        when(mapper.selectList(any())).thenReturn(List.of(hour));
        assertThat(service.openNow(1L)).isTrue();
    }

    @Test
    @DisplayName("当前星期不在任何时段内返回 false")
    void openNowMissesTodayHour() {
        int today = java.time.LocalDate.now().getDayOfWeek().getValue();
        int otherDay = today == 7 ? 1 : today + 1;
        ShopBusinessHour hour = new ShopBusinessHour();
        hour.setDayOfWeek(otherDay);
        hour.setOpenTime(LocalTime.of(0, 0));
        hour.setCloseTime(LocalTime.of(23, 59));
        when(mapper.selectList(any())).thenReturn(List.of(hour));
        assertThat(service.openNow(1L)).isFalse();
    }

    @Test
    @DisplayName("跨零点时段在前一天配置时凌晨仍算营业中")
    void openNowHandlesCrossMidnightFromYesterday() {
        int today = java.time.LocalDate.now().getDayOfWeek().getValue();
        int yesterday = today == 1 ? 7 : today - 1;
        ShopBusinessHour hour = new ShopBusinessHour();
        hour.setDayOfWeek(yesterday);
        hour.setOpenTime(LocalTime.of(0, 0));
        hour.setCloseTime(LocalTime.of(23, 59));
        when(mapper.selectList(any())).thenReturn(List.of(hour));
        // 非跨零点，昨天配置不延续
        assertThat(service.openNow(1L)).isFalse();
    }

    @Test
    @DisplayName("保存时段时先清理旧配置再逐条插入")
    void saveClearsThenInserts() {
        BusinessHourRequest request = new BusinessHourRequest();
        BusinessHourRequest.Item item = new BusinessHourRequest.Item();
        item.setDayOfWeek(1);
        item.setOpenTime("09:00");
        item.setCloseTime("21:00");
        request.setItems(List.of(item));
        when(mapper.insert(any(ShopBusinessHour.class))).thenReturn(1);

        List<ShopBusinessHour> saved = service.save(2L, request);

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getOpenTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(saved.get(0).getShopId()).isEqualTo(1L);
        verify(mapper).delete(any());
        verify(mapper).insert(any(ShopBusinessHour.class));
    }

    @Test
    @DisplayName("时段起止相同被拒绝")
    void saveRejectsEqualOpenClose() {
        BusinessHourRequest request = new BusinessHourRequest();
        BusinessHourRequest.Item item = new BusinessHourRequest.Item();
        item.setDayOfWeek(1);
        item.setOpenTime("09:00");
        item.setCloseTime("09:00");
        request.setItems(List.of(item));

        assertThatThrownBy(() -> service.save(2L, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("营业起止时间不能相同");
        verify(mapper, never()).insert(any(ShopBusinessHour.class));
    }

    @Test
    @DisplayName("星期超出 1 至 7 被拒绝")
    void saveRejectsInvalidDayOfWeek() {
        BusinessHourRequest request = new BusinessHourRequest();
        BusinessHourRequest.Item item = new BusinessHourRequest.Item();
        item.setDayOfWeek(8);
        item.setOpenTime("09:00");
        item.setCloseTime("21:00");
        request.setItems(List.of(item));

        assertThatThrownBy(() -> service.save(2L, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("星期取值 1 至 7");
    }

    @Test
    @DisplayName("空时段列表表示清空配置")
    void saveEmptyListClears() {
        BusinessHourRequest request = new BusinessHourRequest();
        request.setItems(List.of());

        List<ShopBusinessHour> saved = service.save(2L, request);

        assertThat(saved).isEmpty();
        verify(mapper).delete(any());
        verify(mapper, never()).insert(any(ShopBusinessHour.class));
    }
}
