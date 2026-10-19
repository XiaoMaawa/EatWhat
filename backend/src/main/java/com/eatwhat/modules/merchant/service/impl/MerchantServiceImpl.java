package com.eatwhat.modules.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.modules.merchant.dto.MerchantApplyRequest;
import com.eatwhat.modules.merchant.entity.Merchant;
import com.eatwhat.modules.merchant.mapper.MerchantMapper;
import com.eatwhat.modules.merchant.service.MerchantService;
import com.eatwhat.modules.order.mapper.OrderMapper;
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

    public MerchantServiceImpl(MerchantMapper merchantMapper, ShopMapper shopMapper, OrderMapper orderMapper) {
        this.merchantMapper = merchantMapper;
        this.shopMapper = shopMapper;
        this.orderMapper = orderMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Merchant apply(Long userId, MerchantApplyRequest request) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, userId)
                .last("LIMIT 1"));
        if (merchant == null) {
            merchant = new Merchant();
            merchant.setUserId(userId);
            merchant.setAuditStatus(CommonConstants.AUDIT_PENDING);
        } else if (Integer.valueOf(CommonConstants.AUDIT_PASSED).equals(merchant.getAuditStatus())) {
            throw BizException.of(ErrorCode.AUDIT_ALREADY_DONE, "商家资质已通过审核，无需重复提交");
        } else {
            merchant.setAuditStatus(CommonConstants.AUDIT_PENDING);
            merchant.setAuditRemark(null);
            merchant.setAuditedAt(null);
        }
        merchant.setRealName(request.getRealName().trim());
        merchant.setIdCard(request.getIdCard().toUpperCase());
        merchant.setLicenseImg(request.getLicenseImg());
        merchant.setContactPhone(request.getContactPhone());
        if (merchant.getId() == null) {
            merchantMapper.insert(merchant);
        } else {
            merchantMapper.updateById(merchant);
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
