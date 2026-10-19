package com.eatwhat.modules.shop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.result.PageResult;
import com.eatwhat.common.utils.DeliveryFeeUtils;
import com.eatwhat.common.utils.GeoUtils;
import com.eatwhat.common.utils.TimeUtils;
import com.eatwhat.modules.dish.entity.Dish;
import com.eatwhat.modules.dish.entity.DishCategory;
import com.eatwhat.modules.dish.mapper.DishCategoryMapper;
import com.eatwhat.modules.dish.mapper.DishMapper;
import com.eatwhat.modules.merchant.entity.Merchant;
import com.eatwhat.modules.merchant.mapper.MerchantMapper;
import com.eatwhat.modules.review.mapper.ReviewMapper;
import com.eatwhat.modules.shop.dto.ShopUpdateRequest;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.mapper.ShopMapper;
import com.eatwhat.modules.shop.service.ShopService;
import com.eatwhat.modules.shop.vo.ShopDetailVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 店铺服务实现。
 */
@Service
public class ShopServiceImpl implements ShopService {

    private final ShopMapper shopMapper;
    private final MerchantMapper merchantMapper;
    private final DishMapper dishMapper;
    private final DishCategoryMapper categoryMapper;
    private final ReviewMapper reviewMapper;
    private final com.eatwhat.modules.dish.service.DishSpecService dishSpecService;
    private final com.eatwhat.modules.dish.service.DishComboService dishComboService;
    private final com.eatwhat.modules.shop.service.ShopBusinessHourService businessHourService;

    public ShopServiceImpl(ShopMapper shopMapper,
                           MerchantMapper merchantMapper,
                           DishMapper dishMapper,
                           DishCategoryMapper categoryMapper,
                           ReviewMapper reviewMapper,
                           @org.springframework.context.annotation.Lazy com.eatwhat.modules.dish.service.DishSpecService dishSpecService,
                           @org.springframework.context.annotation.Lazy com.eatwhat.modules.dish.service.DishComboService dishComboService,
                           @org.springframework.context.annotation.Lazy com.eatwhat.modules.shop.service.ShopBusinessHourService businessHourService) {
        this.shopMapper = shopMapper;
        this.merchantMapper = merchantMapper;
        this.dishMapper = dishMapper;
        this.categoryMapper = categoryMapper;
        this.reviewMapper = reviewMapper;
        this.dishSpecService = dishSpecService;
        this.dishComboService = dishComboService;
        this.businessHourService = businessHourService;
    }

    @Override
    public PageResult<Shop> pageShops(int page, int size, String keyword, String sortBy,
                                      BigDecimal longitude, BigDecimal latitude) {
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<Shop>()
                .eq(Shop::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            // 顾客-18：按店铺名称与菜品名称关键词搜索
            List<Long> dishShopIds = shopIdsByDishKeyword(kw);
            if (dishShopIds.isEmpty()) {
                wrapper.like(Shop::getName, kw);
            } else {
                wrapper.and(w -> w.like(Shop::getName, kw).or().in(Shop::getId, dishShopIds));
            }
        }
        Page<Shop> result = shopMapper.selectPage(new Page<>(page, size), wrapper);
        List<Shop> records = result.getRecords();
        records.forEach(s -> s.setDistance(GeoUtils.distanceKm(latitude, longitude, s.getLatitude(), s.getLongitude())));

        // 排序：distance 需在内存中排序；其余交给 SQL 不现实（评分/销量需服务端排序）
        String sort = sortBy == null ? "" : sortBy.trim();
        switch (sort) {
            case "score" -> records.sort(Comparator.comparing(
                            (Shop s) -> s.getAvgScore() == null ? BigDecimal.ZERO : s.getAvgScore()).reversed()
                    // 顾客-20：评分相同则按月售降序
                    .thenComparing(Comparator.comparing(
                            (Shop s) -> s.getMonthSales() == null ? 0 : s.getMonthSales()).reversed()));
            case "sales" -> records.sort(Comparator.comparing(
                    (Shop s) -> s.getMonthSales() == null ? 0 : s.getMonthSales()).reversed());
            case "distance" -> records.sort(Comparator.comparing(
                    (Shop s) -> s.getDistance() == null ? BigDecimal.valueOf(999999) : s.getDistance()));
            default -> {
                // 默认按销量降序，与首页推荐一致
                records.sort(Comparator.comparing(
                        (Shop s) -> s.getMonthSales() == null ? 0 : s.getMonthSales()).reversed());
            }
        }
        // 顾客-18：结果里命中的菜品名，供前端高亮匹配词
        records.forEach(s -> s.setMatchedDishNames(
                keyword == null || keyword.isBlank() ? List.of() : matchedDishNames(s.getId(), keyword.trim())));
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 顾客-18：关键词命中的菜品所属店铺 ID 集合。
     */
    private List<Long> shopIdsByDishKeyword(String keyword) {
        List<Dish> dishes = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .like(Dish::getName, keyword)
                .eq(Dish::getStatus, 1)
                .select(Dish::getShopId));
        return dishes.stream()
                .map(Dish::getShopId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 顾客-18：该店铺下命中关键词的菜品名称列表（最多 3 个）。
     */
    private List<String> matchedDishNames(Long shopId, String keyword) {
        List<Dish> dishes = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getShopId, shopId)
                .like(Dish::getName, keyword)
                .eq(Dish::getStatus, 1)
                .orderByDesc(Dish::getMonthSales)
                .last("LIMIT 3"));
        return dishes.stream().map(Dish::getName).collect(Collectors.toList());
    }

    @Override
    public ShopDetailVO detail(Long shopId, BigDecimal longitude, BigDecimal latitude) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw BizException.of(ErrorCode.SHOP_NOT_FOUND);
        }
        ShopDetailVO vo = new ShopDetailVO();
        vo.setId(shop.getId());
        vo.setMerchantId(shop.getMerchantId());
        vo.setName(shop.getName());
        vo.setLogo(shop.getLogo());
        vo.setDescription(shop.getDescription());
        vo.setNotice(shop.getNotice());
        vo.setAddress(shop.getAddress());
        vo.setLongitude(shop.getLongitude());
        vo.setLatitude(shop.getLatitude());
        vo.setPhone(shop.getPhone());
        vo.setMinOrderAmount(shop.getMinOrderAmount());
        vo.setDeliveryFee(shop.getDeliveryFee());
        vo.setDeliveryFeeType(shop.getDeliveryFeeType());
        vo.setPackagingFee(shop.getPackagingFee());
        vo.setDeliveryRange(shop.getDeliveryRange());
        vo.setOpenTime(shop.getOpenTime());
        vo.setCloseTime(shop.getCloseTime());
        vo.setIsOpen(shop.getIsOpen());
        // 商家-08：营业时段之外，顾客端按「休息中」处理
        if (Integer.valueOf(1).equals(shop.getIsOpen())) {
            Boolean weeklyOpen = businessHourService.openNow(shop.getId());
            if (weeklyOpen != null) {
                // 配置了按星期的营业时段，以时段为准
                if (!weeklyOpen) {
                    vo.setIsOpen(0);
                }
            } else if (!TimeUtils.inBusinessHours(shop.getOpenTime(), shop.getCloseTime())) {
                vo.setIsOpen(0);
            }
        }
        vo.setClosedTip(shop.getClosedTip());
        vo.setAvgScore(shop.getAvgScore());
        vo.setMonthSales(shop.getMonthSales());
        vo.setDistance(GeoUtils.distanceKm(latitude, longitude, shop.getLatitude(), shop.getLongitude()));

        List<DishCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<DishCategory>()
                        .eq(DishCategory::getShopId, shopId)
                        .orderByAsc(DishCategory::getSort)
                        .orderByAsc(DishCategory::getId));
        List<Dish> dishes = dishMapper.selectList(
                new LambdaQueryWrapper<Dish>()
                        .eq(Dish::getShopId, shopId)
                        .eq(Dish::getStatus, 1)
                        .orderByAsc(Dish::getSort)
                        .orderByAsc(Dish::getId));
        Map<Long, List<Dish>> grouped = dishes.stream()
                .filter(d -> d.getCategoryId() != null)
                .collect(Collectors.groupingBy(Dish::getCategoryId));

        List<ShopDetailVO.CategoryVO> categoryVos = new ArrayList<>();
        for (DishCategory category : categories) {
            ShopDetailVO.CategoryVO cv = new ShopDetailVO.CategoryVO();
            cv.setId(category.getId());
            cv.setName(category.getName());
            cv.setSort(category.getSort());
            cv.setDishes(grouped.getOrDefault(category.getId(), new ArrayList<>()));
            categoryVos.add(cv);
        }
        // 顾客-27/28：为菜品填充规格与加料，顾客-30：填充近 30 天好评率
        List<Long> dishIds = dishes.stream().map(Dish::getId).collect(Collectors.toList());
        Map<Long, List<com.eatwhat.modules.dish.entity.DishSpec>> specMap =
                dishSpecService.mapSpecsByDish(dishIds);
        Map<Long, List<com.eatwhat.modules.dish.entity.DishAddon>> addonMap =
                dishSpecService.mapAddonsByDish(dishIds);
        // 商家-28：套餐组成项
        List<Long> comboIds = dishes.stream()
                .filter(d -> Integer.valueOf(1).equals(d.getIsCombo()))
                .map(Dish::getId)
                .collect(Collectors.toList());
        Map<Long, List<com.eatwhat.modules.dish.entity.DishComboItem>> comboMap =
                dishComboService.mapItemsByCombo(comboIds);
        for (Dish dish : dishes) {
            dish.setSpecs(specMap.get(dish.getId()));
            dish.setAddons(addonMap.get(dish.getId()));
            dish.setGoodRate(reviewMapper.goodRateByDish(dish.getId()));
            if (Integer.valueOf(1).equals(dish.getIsCombo())) {
                dish.setComboItems(comboMap.get(dish.getId()));
            }
        }
        vo.setCategories(categoryVos);
        return vo;
    }

    @Override
    public List<Dish> listDishes(Long shopId, Long categoryId, String keyword) {
        return dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getShopId, shopId)
                .eq(Dish::getStatus, 1)
                .eq(categoryId != null, Dish::getCategoryId, categoryId)
                .like(keyword != null && !keyword.isBlank(), Dish::getName, keyword == null ? null : keyword.trim())
                .orderByAsc(Dish::getSort)
                .orderByAsc(Dish::getId));
    }

    @Override
    public Shop currentShop(Long userId) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, userId)
                .last("LIMIT 1"));
        if (merchant == null) {
            throw BizException.of(ErrorCode.MERCHANT_NOT_APPROVED, "还没有提交商家入驻申请");
        }
        if (!Integer.valueOf(1).equals(merchant.getAuditStatus())) {
            throw BizException.of(ErrorCode.MERCHANT_NOT_APPROVED,
                    merchant.getAuditStatus() != null && merchant.getAuditStatus() == 2
                            ? "商家资质未通过审核，请修改资料后重新提交"
                            : "商家资质还在审核中，通过后即可开店");
        }
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getMerchantId, merchant.getId())
                .last("LIMIT 1"));
        if (shop == null) {
            throw BizException.of(ErrorCode.SHOP_NOT_FOUND);
        }
        return shop;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShop(Long userId, ShopUpdateRequest request) {
        Shop shop = currentShop(userId);
        shop.setName(request.getName());
        shop.setLogo(request.getLogo());
        shop.setDescription(request.getDescription());
        shop.setNotice(request.getNotice());
        shop.setAddress(request.getAddress());
        shop.setLongitude(request.getLongitude());
        shop.setLatitude(request.getLatitude());
        shop.setPhone(request.getPhone());
        shop.setMinOrderAmount(request.getMinOrderAmount());
        shop.setDeliveryFee(request.getDeliveryFee());
        // 商家-14：打包费 0-20 元
        if (request.getPackagingFee() != null) {
            if (request.getPackagingFee().compareTo(BigDecimal.ZERO) < 0
                    || request.getPackagingFee().compareTo(BigDecimal.valueOf(20)) > 0) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "打包费需在 0 至 20 元之间");
            }
            shop.setPackagingFee(request.getPackagingFee().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getDeliveryFeeType() != null) {
            if (!DeliveryFeeUtils.isValidType(request.getDeliveryFeeType())) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "配送费计费方式只能为 0（固定）或 1（按距离）");
            }
            shop.setDeliveryFeeType(request.getDeliveryFeeType());
        }
        shop.setDeliveryRange(request.getDeliveryRange());
        shop.setOpenTime(TimeUtils.parseClock(request.getOpenTime()));
        shop.setCloseTime(TimeUtils.parseClock(request.getCloseTime()));
        // 商家-17：打烊提示语，空字符串存为 null
        if (request.getClosedTip() != null) {
            String tip = request.getClosedTip().trim();
            shop.setClosedTip(tip.isEmpty() ? null : tip);
        }
        shopMapper.updateById(shop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOpenStatus(Long userId, Integer isOpen) {
        if (isOpen == null || (isOpen != 0 && isOpen != 1)) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "营业状态只能为 0 或 1");
        }
        Shop shop = currentShop(userId);
        shop.setIsOpen(isOpen);
        shopMapper.updateById(shop);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshScoreAndSales(Long shopId, int salesDelta) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            return;
        }
        BigDecimal avg = reviewMapper.avgShopScore(shopId);
        if (avg != null) {
            shop.setAvgScore(avg.setScale(2, RoundingMode.HALF_UP));
        }
        int sales = Objects.requireNonNullElse(shop.getMonthSales(), 0) + salesDelta;
        shop.setMonthSales(Math.max(sales, 0));
        shopMapper.updateById(shop);
    }
}
