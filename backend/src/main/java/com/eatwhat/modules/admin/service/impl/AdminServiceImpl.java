package com.eatwhat.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.constant.OrderStatus;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.admin.entity.OperationLog;
import com.eatwhat.modules.admin.mapper.OperationLogMapper;
import com.eatwhat.modules.admin.service.AdminService;
import com.eatwhat.modules.auth.entity.SysUser;
import com.eatwhat.modules.auth.mapper.SysUserMapper;
import com.eatwhat.modules.delivery.entity.Rider;
import com.eatwhat.modules.delivery.mapper.RiderMapper;
import com.eatwhat.modules.dish.entity.Dish;
import com.eatwhat.modules.dish.mapper.DishMapper;
import com.eatwhat.modules.merchant.entity.Merchant;
import com.eatwhat.modules.merchant.mapper.MerchantMapper;
import com.eatwhat.modules.order.entity.Order;
import com.eatwhat.modules.order.entity.OrderItem;
import com.eatwhat.modules.order.mapper.OrderItemMapper;
import com.eatwhat.modules.order.mapper.OrderMapper;
import com.eatwhat.modules.order.service.OrderService;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.mapper.ShopMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * 平台管理端服务实现。
 */
@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final SysUserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final RiderMapper riderMapper;
    private final ShopMapper shopMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final DishMapper dishMapper;
    private final OrderService orderService;
    private final OperationLogMapper operationLogMapper;

    public AdminServiceImpl(SysUserMapper userMapper,
                            MerchantMapper merchantMapper,
                            RiderMapper riderMapper,
                            ShopMapper shopMapper,
                            OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper,
                            DishMapper dishMapper,
                            OrderService orderService,
                            OperationLogMapper operationLogMapper) {
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.riderMapper = riderMapper;
        this.shopMapper = shopMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.dishMapper = dishMapper;
        this.orderService = orderService;
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public PageResult<SysUser> pageUsers(String keyword, String role, Integer status, int page, int size) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysUser::getPhone, keyword.trim())
                        .or().like(SysUser::getNickname, keyword.trim()))
                .eq(role != null && !role.isBlank(), SysUser::getRole, role)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getId);
        return PageResult.of(userMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Override
    public SysUser userDetail(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.of(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status) {
        if (status == null || (status != CommonConstants.USER_ENABLED && status != CommonConstants.USER_DISABLED)) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "状态只能为 0 或 1");
        }
        SysUser user = userDetail(userId);
        if ("ADMIN".equals(user.getRole())) {
            throw BizException.of(ErrorCode.ROLE_NOT_ALLOWED, "不能禁用管理员账号");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        log.info("管理员变更用户状态 userId={} status={}", userId, status);
    }

    @Override
    public PageResult<Merchant> pageMerchants(Integer auditStatus, String keyword, int page, int size) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>()
                .eq(auditStatus != null, Merchant::getAuditStatus, auditStatus)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(Merchant::getRealName, keyword.trim())
                        .or().like(Merchant::getContactPhone, keyword.trim()))
                .orderByDesc(Merchant::getId);
        Page<Merchant> result = merchantMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(this::fillShopInfo);
        return PageResult.of(result);
    }

    @Override
    public Merchant merchantDetail(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw BizException.of(ErrorCode.MERCHANT_NOT_FOUND);
        }
        fillShopInfo(merchant);
        return merchant;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditMerchant(Long merchantId, Integer auditStatus, String auditRemark) {
        if (auditStatus == null || (auditStatus != CommonConstants.AUDIT_PASSED
                && auditStatus != CommonConstants.AUDIT_REJECTED)) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "审核结果只能为 1（通过）或 2（驳回）");
        }
        Merchant merchant = merchantDetail(merchantId);
        merchant.setAuditStatus(auditStatus);
        merchant.setAuditRemark(auditRemark);
        merchant.setAuditedAt(LocalDateTime.now());
        merchantMapper.updateById(merchant);
        if (auditStatus == CommonConstants.AUDIT_PASSED) {
            // 审核通过即为商家准备好可经营的店铺，避免新商家登录后无店铺可用
            ensureShopForMerchant(merchant);
        }
        log.info("管理员审核商家 merchantId={} auditStatus={}", merchantId, auditStatus);
    }

    /**
     * 商家审核通过后保证其拥有一个店铺（首次通过时自动创建初始店铺）。
     */
    private void ensureShopForMerchant(Merchant merchant) {
        Shop existing = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getMerchantId, merchant.getId())
                .last("LIMIT 1"));
        if (existing != null) {
            return;
        }
        Shop shop = new Shop();
        shop.setMerchantId(merchant.getId());
        shop.setName(merchant.getRealName() + "的店铺");
        shop.setAddress("待完善");
        shop.setPhone(merchant.getContactPhone());
        shop.setDescription("欢迎光临，老板正在准备开店中～");
        shop.setMinOrderAmount(new BigDecimal("20.00"));
        shop.setDeliveryFee(new BigDecimal("3.00"));
        shop.setDeliveryRange(3000);
        shop.setIsOpen(CommonConstants.SHOP_CLOSED);
        shop.setAvgScore(new BigDecimal("5.00"));
        shop.setMonthSales(0);
        shop.setStatus(1);
        shopMapper.insert(shop);
        log.info("商家审核通过，自动创建初始店铺 shopId={} merchantId={}", shop.getId(), merchant.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freezeMerchant(Long merchantId, Integer status) {
        Merchant merchant = merchantDetail(merchantId);
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getMerchantId, merchant.getId())
                .last("LIMIT 1"));
        if (shop == null) {
            throw BizException.of(ErrorCode.SHOP_NOT_FOUND);
        }
        shop.setStatus(status == null || status != 0 ? 1 : 0);
        if (shop.getStatus() == 0) {
            shop.setIsOpen(CommonConstants.SHOP_CLOSED);
        }
        shopMapper.updateById(shop);
        log.info("管理员冻结/解冻店铺 shopId={} status={}", shop.getId(), shop.getStatus());
    }

    @Override
    public PageResult<Rider> pageRiders(Integer auditStatus, String keyword, int page, int size) {
        LambdaQueryWrapper<Rider> wrapper = new LambdaQueryWrapper<Rider>()
                .eq(auditStatus != null, Rider::getAuditStatus, auditStatus)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(Rider::getRealName, keyword.trim())
                        .or().like(Rider::getContactPhone, keyword.trim()))
                .orderByDesc(Rider::getId);
        Page<Rider> result = riderMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(this::fillRiderUser);
        return PageResult.of(result);
    }

    @Override
    public Rider riderDetail(Long riderId) {
        Rider rider = riderMapper.selectById(riderId);
        if (rider == null) {
            throw BizException.of(ErrorCode.RIDER_NOT_FOUND);
        }
        fillRiderUser(rider);
        return rider;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditRider(Long riderId, Integer auditStatus, String auditRemark) {
        if (auditStatus == null || (auditStatus != CommonConstants.AUDIT_PASSED
                && auditStatus != CommonConstants.AUDIT_REJECTED)) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "审核结果只能为 1（通过）或 2（驳回）");
        }
        Rider rider = riderDetail(riderId);
        rider.setAuditStatus(auditStatus);
        rider.setAuditRemark(auditRemark);
        rider.setAuditedAt(LocalDateTime.now());
        riderMapper.updateById(rider);
        log.info("管理员审核骑手 riderId={} auditStatus={}", riderId, auditStatus);
    }

    @Override
    public PageResult<Order> pageOrders(Integer status, String keyword, int page, int size) {
        return orderService.pageAdminOrders(status, keyword, page, size);
    }

    @Override
    public Map<String, Object> orderDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BizException.of(ErrorCode.ORDER_NOT_FOUND);
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        Shop shop = shopMapper.selectById(order.getShopId());
        SysUser user = userMapper.selectById(order.getUserId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("items", items);
        result.put("statusDesc", OrderStatus.descOf(order.getStatus()));
        result.put("shopName", shop == null ? null : shop.getName());
        result.put("userNickname", user == null ? null : user.getNickname());
        result.put("userPhone", user == null ? null : user.getPhone());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void interveneOrder(Long orderId, String action, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BizException.of(ErrorCode.ORDER_NOT_FOUND);
        }
        String act = action == null ? "" : action.trim().toUpperCase();
        switch (act) {
            case "FORCE_REFUND" -> {
                if (Integer.valueOf(CommonConstants.ORDER_CANCELLED).equals(order.getStatus())) {
                    throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "订单已取消");
                }
                orderService.transition(orderId, CommonConstants.ORDER_CANCELLED, "SYSTEM",
                        "平台介入取消：" + (reason == null ? "无" : reason));
            }
            case "FORCE_COMPLETE" -> orderService.transition(orderId, CommonConstants.ORDER_COMPLETED,
                    "SYSTEM", "平台介入完成：" + (reason == null ? "无" : reason));
            default -> throw BizException.of(ErrorCode.PARAM_INVALID, "action 只能是 FORCE_REFUND 或 FORCE_COMPLETE");
        }
    }

    @Override
    public PageResult<Dish> pageDishes(String keyword, Long shopId, int page, int size) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>()
                .eq(shopId != null, Dish::getShopId, shopId)
                .like(keyword != null && !keyword.isBlank(), Dish::getName,
                        keyword == null ? null : keyword.trim())
                .orderByDesc(Dish::getId);
        return PageResult.of(dishMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlineDish(Long dishId) {
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw BizException.of(ErrorCode.DISH_NOT_FOUND);
        }
        dish.setStatus(CommonConstants.DISH_OFF);
        dishMapper.updateById(dish);
        log.info("管理员强制下架菜品 dishId={}", dishId);
    }

    @Override
    public Map<String, Object> dashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        Map<String, Object> todaySum = orderMapper.sumByRange(startOfDay, endOfDay);
        long todayOrders = todaySum == null || todaySum.get("orderCount") == null
                ? 0L : ((Number) todaySum.get("orderCount")).longValue();
        BigDecimal todayAmount = todaySum == null || todaySum.get("totalAmount") == null
                ? BigDecimal.ZERO : new BigDecimal(todaySum.get("totalAmount").toString());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayOrders", todayOrders);
        result.put("todayAmount", todayAmount.setScale(2, RoundingMode.HALF_UP));
        result.put("totalUsers", userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "CONSUMER")));
        result.put("totalMerchants", merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getAuditStatus, CommonConstants.AUDIT_PASSED)));
        result.put("totalRiders", riderMapper.selectCount(new LambdaQueryWrapper<Rider>()
                .eq(Rider::getAuditStatus, CommonConstants.AUDIT_PASSED)));

        List<Map<String, Object>> trend = new ArrayList<>();
        for (Map<String, Object> row : orderMapper.dailyTrend(today.minusDays(6).atStartOfDay())) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", row.get("date") == null ? null : row.get("date").toString());
            point.put("count", row.get("count"));
            point.put("amount", row.get("amount"));
            trend.add(point);
        }
        result.put("orderTrend", trend);
        return result;
    }

    @Override
    public PageResult<OperationLog> pageOperationLogs(String operatorRole, String module, Integer result,
                                                      String startDate, String endDate, int page, int size) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .eq(operatorRole != null && !operatorRole.isBlank(), OperationLog::getOperatorRole, operatorRole)
                .eq(module != null && !module.isBlank(), OperationLog::getModule, module)
                .eq(result != null, OperationLog::getResult, result)
                .ge(startDate != null && !startDate.isBlank(), OperationLog::getCreatedAt,
                        startDate == null || startDate.isBlank() ? null : LocalDateTime.parse(startDate + "T00:00:00"))
                .lt(endDate != null && !endDate.isBlank(), OperationLog::getCreatedAt,
                        endDate == null || endDate.isBlank() ? null : LocalDateTime.parse(endDate + "T00:00:00").plusDays(1))
                .orderByDesc(OperationLog::getId);
        return PageResult.of(operationLogMapper.selectPage(new Page<>(page, size), wrapper));
    }

    private void fillShopInfo(Merchant merchant) {        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getMerchantId, merchant.getId())
                .last("LIMIT 1"));
        if (shop != null) {
            merchant.setShopId(shop.getId());
            merchant.setShopName(shop.getName());
        }
    }

    private void fillRiderUser(Rider rider) {
        SysUser user = userMapper.selectById(rider.getUserId());
        if (user != null) {
            rider.setNickname(user.getNickname());
            rider.setPhone(user.getPhone());
        }
    }
}
