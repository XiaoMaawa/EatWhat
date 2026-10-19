package com.eatwhat.modules.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.modules.merchant.dto.MerchantApplyRequest;
import com.eatwhat.modules.merchant.entity.Merchant;
import com.eatwhat.modules.merchant.mapper.MerchantMapper;
import com.eatwhat.modules.merchant.service.MerchantService;
import com.eatwhat.modules.order.mapper.OrderMapper;
import com.eatwhat.modules.review.mapper.ReviewMapper;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.mapper.ShopMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家服务实现。
 */
@Service
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;
    private final ShopMapper shopMapper;
    private final OrderMapper orderMapper;
    private final ReviewMapper reviewMapper;

    public MerchantServiceImpl(MerchantMapper merchantMapper, ShopMapper shopMapper, OrderMapper orderMapper,
                               ReviewMapper reviewMapper) {
        this.merchantMapper = merchantMapper;
        this.shopMapper = shopMapper;
        this.orderMapper = orderMapper;
        this.reviewMapper = reviewMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Merchant apply(Long userId, MerchantApplyRequest request) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, userId)
                .last("LIMIT 1"));
        boolean resubmit = false;
        if (merchant == null) {
            merchant = new Merchant();
            merchant.setUserId(userId);
            merchant.setAuditStatus(CommonConstants.AUDIT_PENDING);
        } else if (Integer.valueOf(CommonConstants.AUDIT_PASSED).equals(merchant.getAuditStatus())) {
            throw BizException.of(ErrorCode.AUDIT_ALREADY_DONE, "商家资质已通过审核，无需重复提交");
        } else {
            // 商家-04：驳回后重新提交，重置为待审核并清空上一轮的驳回意见与审核时间
            merchant.setAuditStatus(CommonConstants.AUDIT_PENDING);
            resubmit = true;
        }
        merchant.setRealName(request.getRealName().trim());
        merchant.setIdCard(request.getIdCard().toUpperCase());
        merchant.setLicenseImg(request.getLicenseImg());
        merchant.setContactPhone(request.getContactPhone());
        if (merchant.getId() == null) {
            merchantMapper.insert(merchant);
        } else {
            merchantMapper.updateById(merchant);
            if (resubmit) {
                // update-strategy 为 not_null，置空字段必须走显式 UpdateWrapper
                merchantMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Merchant>()
                        .eq(Merchant::getId, merchant.getId())
                        .set(Merchant::getAuditRemark, null)
                        .set(Merchant::getAuditedAt, null));
                merchant.setAuditRemark(null);
                merchant.setAuditedAt(null);
            }
        }
        return merchant;
    }

    @Override
    public Map<String, Object> profile(Long userId) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, userId)
                .last("LIMIT 1"));
        if (merchant == null) {
            // 尚未提交入驻申请：返回未入驻状态，避免前端把 404 当成系统异常
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("applied", false);
            empty.put("auditStatus", null);
            empty.put("auditRemark", null);
            empty.put("shop", null);
            return empty;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applied", true);
        result.put("id", merchant.getId());
        result.put("realName", merchant.getRealName());
        result.put("auditStatus", merchant.getAuditStatus());
        result.put("auditRemark", merchant.getAuditRemark());
        result.put("contactPhone", merchant.getContactPhone());
        result.put("licenseImg", merchant.getLicenseImg());
        // 商家-04：回填身份证号，便于驳回后按原资料修改再提交
        result.put("idCard", merchant.getIdCard());
        // 商家-04：驳回后允许重新提交，前端据此提示「按驳回意见修改后重新提交」
        result.put("canResubmit",
                Integer.valueOf(CommonConstants.AUDIT_REJECTED).equals(merchant.getAuditStatus()));
        result.put("submittedAt", merchant.getCreatedAt());
        result.put("auditedAt", merchant.getAuditedAt());

        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getMerchantId, merchant.getId())
                .last("LIMIT 1"));
        if (shop != null) {
            Map<String, Object> shopInfo = new LinkedHashMap<>();
            shopInfo.put("id", shop.getId());
            shopInfo.put("name", shop.getName());
            shopInfo.put("logo", shop.getLogo());
            shopInfo.put("isOpen", shop.getIsOpen());
            shopInfo.put("status", shop.getStatus());
            result.put("shop", shopInfo);
        } else {
            result.put("shop", null);
        }
        return result;
    }

    @Override
    public Merchant currentMerchant(Long userId) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, userId)
                .last("LIMIT 1"));
        if (merchant == null) {
            throw BizException.of(ErrorCode.MERCHANT_NOT_FOUND);
        }
        return merchant;
    }

    @Override
    public Merchant requireApprovedMerchant(Long userId) {
        Merchant merchant = currentMerchant(userId);
        if (!Integer.valueOf(CommonConstants.AUDIT_PASSED).equals(merchant.getAuditStatus())) {
            throw BizException.of(ErrorCode.MERCHANT_NOT_APPROVED);
        }
        return merchant;
    }

    @Override
    public Map<String, Object> stats(Long userId, String type) {
        Merchant merchant = requireApprovedMerchant(userId);
        LocalDateTime start;
        LocalDateTime end = LocalDateTime.now();
        String scope = type == null ? "day" : type.trim();
        LocalDate today = LocalDate.now();
        switch (scope) {
            case "week" -> start = today.minusDays(6).atStartOfDay();
            case "month" -> start = today.withDayOfMonth(1).atStartOfDay();
            default -> start = today.atStartOfDay();
        }

        Map<String, Object> sum = orderMapper.sumByMerchantAndRange(merchant.getId(), start, end);
        long orderCount = toLong(sum == null ? null : sum.get("orderCount"));
        BigDecimal totalAmount = toDecimal(sum == null ? null : sum.get("totalAmount"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOrders", orderCount);
        result.put("totalAmount", totalAmount.setScale(2, RoundingMode.HALF_UP));
        result.put("avgOrderAmount", orderCount == 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : totalAmount.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP));

        List<Map<String, Object>> hotDishes = new ArrayList<>();
        for (Map<String, Object> row : orderMapper.topDishes(merchant.getId(), start, end, 5)) {
            Map<String, Object> dish = new LinkedHashMap<>();
            dish.put("dishId", row.get("dishId"));
            dish.put("dishName", row.get("dishName"));
            dish.put("sales", toLong(row.get("sales")));
            hotDishes.add(dish);
        }
        result.put("hotDishes", hotDishes);
        return result;
    }

    @Override
    public Map<String, Object> analytics(Long userId, String type) {
        Merchant merchant = requireApprovedMerchant(userId);
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getMerchantId, merchant.getId())
                .last("LIMIT 1"));
        if (shop == null) {
            throw BizException.of(ErrorCode.SHOP_NOT_FOUND);
        }
        LocalDateTime end = LocalDateTime.now();
        String scope = type == null ? "month" : type.trim();
        LocalDate today = LocalDate.now();
        LocalDateTime start;
        switch (scope) {
            case "day" -> start = today.atStartOfDay();
            case "week" -> start = today.minusDays(6).atStartOfDay();
            default -> start = today.minusDays(29).atStartOfDay();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", scope);
        result.put("startDate", start.toLocalDate().toString());
        result.put("endDate", today.toString());

        // 商家-55 客单价统计
        Map<String, Object> sum = orderMapper.sumByMerchantAndRange(merchant.getId(), start, end.plusDays(1));
        long orderCount = toLong(sum == null ? null : sum.get("orderCount"));
        BigDecimal totalAmount = toDecimal(sum == null ? null : sum.get("totalAmount"));
        result.put("totalOrders", orderCount);
        result.put("totalAmount", totalAmount.setScale(2, RoundingMode.HALF_UP));
        result.put("avgOrderAmount", orderCount == 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : totalAmount.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP));

        // 商家-56 评分统计（综合 + 各维度）
        Map<String, Object> score = reviewMapper.scoreSummaryByShop(shop.getId());
        Map<String, Object> scoreStat = new LinkedHashMap<>();
        if (score != null) {
            scoreStat.put("avgShopScore", toDecimal(score.get("avgShopScore")));
            scoreStat.put("avgDeliveryScore", toDecimal(score.get("avgDeliveryScore")));
            scoreStat.put("avgTasteScore", toDecimal(score.get("avgTasteScore")));
            scoreStat.put("totalReviews", toLong(score.get("totalReviews")));
            scoreStat.put("repliedReviews", toLong(score.get("repliedReviews")));
        }
        result.put("score", scoreStat);

        // 商家-51 评价评分统计（近 30 天维度平均分与评价总数）
        Map<String, Object> recent = reviewMapper.scoreSummaryLast30Days(shop.getId());
        Map<String, Object> recentStat = new LinkedHashMap<>();
        if (recent != null) {
            recentStat.put("avgShopScore", toDecimal(recent.get("avgShopScore")));
            recentStat.put("avgDeliveryScore", toDecimal(recent.get("avgDeliveryScore")));
            recentStat.put("totalReviews", toLong(recent.get("totalReviews")));
        }
        result.put("recentReviews", recentStat);

        // 商家-57 营业时段分析：按小时订单分布与高峰时段
        int[] hours = new int[24];
        for (Map<String, Object> row : orderMapper.hourlyOrders(merchant.getId(), start, end.plusDays(1))) {
            int hour = (int) toLong(row.get("hour"));
            if (hour >= 0 && hour < 24) {
                hours[hour] = (int) toLong(row.get("orderCount"));
            }
        }
        List<Map<String, Object>> hourly = new ArrayList<>();
        int maxHour = 0;
        long maxCount = 0;
        for (int h = 0; h < 24; h++) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("hour", String.format("%02d:00", h));
            point.put("orderCount", hours[h]);
            hourly.add(point);
            if (hours[h] > maxCount) {
                maxCount = hours[h];
                maxHour = h;
            }
        }
        result.put("hourly", hourly);
        result.put("peakHour", maxCount > 0 ? String.format("%02d:00", maxHour) : null);
        result.put("peakHourOrders", maxCount);
        return result;
    }

    @Override
    public Merchant updateAccount(Long userId, String realName, String contactPhone) {
        Merchant merchant = requireApprovedMerchant(userId);
        String name = realName == null ? "" : realName.trim();
        String phone = contactPhone == null ? "" : contactPhone.trim();
        if (name.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "联系人姓名不能为空");
        }
        if (name.length() > 50) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "联系人姓名不能超过 50 个字符");
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "联系电话格式不正确");
        }
        merchant.setRealName(name);
        merchant.setContactPhone(phone);
        merchantMapper.updateById(merchant);
        // 商家-58：店铺联系电话同步，避免顾客端看到的还是旧号码
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getMerchantId, merchant.getId())
                .last("LIMIT 1"));
        if (shop != null) {
            shopMapper.update(null, new LambdaUpdateWrapper<Shop>()
                    .eq(Shop::getId, shop.getId())
                    .set(Shop::getPhone, phone));
        }
        return merchant;
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }
}
