package com.eatwhat.modules.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.constant.RoleConstants;
import com.eatwhat.common.constant.OrderStatus;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.admin.entity.OperationLog;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    public AdminServiceImpl(SysUserMapper userMapper,
                            MerchantMapper merchantMapper,
                            RiderMapper riderMapper,
                            ShopMapper shopMapper,
                            OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper,
                            DishMapper dishMapper,
                            OrderService orderService,
                            OperationLogMapper operationLogMapper,
                            PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.riderMapper = riderMapper;
        this.shopMapper = shopMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.dishMapper = dishMapper;
        this.orderService = orderService;
        this.operationLogMapper = operationLogMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<SysUser> pageAdmins(String keyword, Integer status, int page, int size) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysUser::getPhone, keyword.trim())
                        .or().like(SysUser::getNickname, keyword.trim()))
                .eq(SysUser::getRole, RoleConstants.ADMIN)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getId);
        return PageResult.of(userMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Override
    public SysUser createAdmin(String phone, String nickname, String password) {
        if (phone == null || !phone.matches("1[3-9]\\d{9}")) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "手机号格式不正确");
        }
        if (password == null || password.length() < 6 || password.length() > 20) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "密码长度需在 6 至 20 位之间");
        }
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone));
        if (count != null && count > 0) {
            throw BizException.of(ErrorCode.PHONE_ALREADY_EXISTS);
        }
        SysUser admin = new SysUser();
        admin.setPhone(phone);
        admin.setNickname(nickname == null || nickname.isBlank() ? "管理员" + phone.substring(7) : nickname);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(RoleConstants.ADMIN);
        admin.setStatus(CommonConstants.USER_ENABLED);
        userMapper.insert(admin);
        return admin;
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
    public PageResult<Order> pageUserOrders(Long userId, Integer status, int page, int size) {
        userDetail(userId);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getId);
        Page<Order> result = orderMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(this::fillOrderSummary);
        return PageResult.of(result);
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
    @Transactional(rollbackFor = Exception.class)
    public void updateRiderAccepting(Long riderId, Integer accepting) {
        if (accepting == null || (accepting != 0 && accepting != 1)) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "接单权限只能为 0（关闭）或 1（开启）");
        }
        Rider rider = riderDetail(riderId);
        rider.setAccepting(accepting);
        riderMapper.updateById(rider);
        log.info("管理员变更骑手接单权限 riderId={} accepting={}", riderId, accepting);
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
            case "FORCE_CANCEL" -> orderService.forceCancel(orderId, reason);
            default -> throw BizException.of(ErrorCode.PARAM_INVALID,
                    "action 只能是 FORCE_REFUND、FORCE_COMPLETE 或 FORCE_CANCEL");
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
    @Transactional(rollbackFor = Exception.class)
    public void onlineDish(Long dishId) {
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null) {
            throw BizException.of(ErrorCode.DISH_NOT_FOUND);
        }
        // 库存为 0 时恢复成「售罄」而不是「在售」，避免上架即超卖
        Integer stock = dish.getStock();
        dish.setStatus(stock == null || stock <= 0 ? CommonConstants.DISH_SOLD_OUT : CommonConstants.DISH_ON);
        dishMapper.updateById(dish);
        log.info("管理员恢复菜品 dishId={} status={}", dishId, dish.getStatus());
    }

    @Override
    public PageResult<Order> pageAbnormalOrders(String type, int page, int size) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        String normalized = type == null ? "" : type.trim().toLowerCase();
        switch (normalized) {
            // 超时未接单：支付后停留待接单超过 15 分钟
            case "timeout_accept" -> wrapper.eq(Order::getStatus, CommonConstants.ORDER_PENDING_ACCEPT)
                    .lt(Order::getPayTime, now.minusMinutes(CommonConstants.ORDER_PAY_TIMEOUT_MINUTES));
            // 超时未送达：配送中超过 60 分钟仍未完成
            case "timeout_deliver" -> wrapper.eq(Order::getStatus, CommonConstants.ORDER_DELIVERING)
                    .lt(Order::getCreatedAt, now.minusMinutes(60));
            // 退款中：商家拒单或用户申诉后停留在退款中
            case "refunding" -> wrapper.eq(Order::getStatus, CommonConstants.ORDER_REFUNDING);
            // 待配送超时：备餐完成但长时间没人抢单
            case "timeout_take" -> wrapper.eq(Order::getStatus, CommonConstants.ORDER_PENDING_DELIVERY)
                    .lt(Order::getCreatedAt, now.minusMinutes(30));
            default -> throw BizException.of(ErrorCode.PARAM_INVALID,
                    "筛选类型只能是 timeout_accept、timeout_take、timeout_deliver 或 refunding");
        }
        wrapper.orderByAsc(Order::getCreatedAt);
        Page<Order> result = orderMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(this::fillOrderSummary);
        return PageResult.of(result);
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
    public Map<String, Object> statistics(String period) {
        LocalDate today = LocalDate.now();
        String p = period == null ? "day" : period.trim().toLowerCase();
        LocalDateTime start;
        LocalDateTime prevStart;
        LocalDateTime prevEnd;
        switch (p) {
            case "week" -> {
                start = today.minusDays(6).atStartOfDay();
                prevStart = today.minusDays(13).atStartOfDay();
                prevEnd = today.minusDays(6).atStartOfDay();
            }
            case "month" -> {
                start = today.minusDays(29).atStartOfDay();
                prevStart = today.minusDays(59).atStartOfDay();
                prevEnd = today.minusDays(29).atStartOfDay();
            }
            case "day" -> {
                start = today.atStartOfDay();
                prevStart = today.minusDays(1).atStartOfDay();
                prevEnd = today.atStartOfDay();
            }
            default -> throw BizException.of(ErrorCode.PARAM_INVALID, "统计周期只能是 day、week 或 month");
        }
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        Map<String, Object> current = orderMapper.statsByRange(start, end);
        Map<String, Object> previous = orderMapper.statsByRange(prevStart, prevEnd);

        BigDecimal amount = readAmount(current, "totalAmount");
        BigDecimal prevAmount = readAmount(previous, "totalAmount");
        long orderCount = readLong(current, "orderCount");
        long prevCount = readLong(previous, "orderCount");
        long completed = readLong(current, "completedCount");
        long prevCompleted = readLong(previous, "completedCount");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", p);
        result.put("startDate", start.toLocalDate().toString());
        result.put("endDate", today.toString());
        result.put("totalAmount", amount.setScale(2, RoundingMode.HALF_UP));
        result.put("prevAmount", prevAmount.setScale(2, RoundingMode.HALF_UP));
        result.put("amountGrowth", growthRate(amount, prevAmount));
        result.put("orderCount", orderCount);
        result.put("prevOrderCount", prevCount);
        result.put("orderGrowth", growthRate(BigDecimal.valueOf(orderCount), BigDecimal.valueOf(prevCount)));
        result.put("completedCount", completed);
        result.put("completeRate", orderCount == 0 ? 0.0
                : BigDecimal.valueOf(completed * 100.0 / orderCount).setScale(2, RoundingMode.HALF_UP).doubleValue());
        result.put("prevCompleteRate", prevCount == 0 ? 0.0
                : BigDecimal.valueOf(prevCompleted * 100.0 / prevCount).setScale(2, RoundingMode.HALF_UP).doubleValue());

        Map<String, Long> newUserMap = new LinkedHashMap<>();
        for (Map<String, Object> row : orderMapper.dailyNewUsers("CONSUMER", start)) {
            newUserMap.put(String.valueOf(row.get("date")), readLong(row, "count"));
        }
        Map<String, Long> activeUserMap = new LinkedHashMap<>();
        for (Map<String, Object> row : orderMapper.dailyActiveUsers(start, end)) {
            activeUserMap.put(String.valueOf(row.get("date")), readLong(row, "activeUsers"));
        }
        Map<String, Object> orderTrendMap = new LinkedHashMap<>();
        for (Map<String, Object> row : orderMapper.dailyTrend(start)) {
            orderTrendMap.put(String.valueOf(row.get("date")), row);
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        long newUserTotal = 0;
        long activeUserTotal = 0;
        int days = p.equals("day") ? 1 : (p.equals("week") ? 7 : 30);
        for (int i = days - 1; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date);
            Map<String, Object> orderRow = orderTrendMap.get(date) instanceof Map
                    ? (Map<String, Object>) orderTrendMap.get(date) : null;
            point.put("count", orderRow == null ? 0L : readLong(orderRow, "count"));
            point.put("amount", orderRow == null ? BigDecimal.ZERO : readAmount(orderRow, "amount"));
            long newUsers = newUserMap.getOrDefault(date, 0L);
            point.put("newUsers", newUsers);
            newUserTotal += newUsers;
            long activeUsers = activeUserMap.getOrDefault(date, 0L);
            point.put("activeUsers", activeUsers);
            activeUserTotal += activeUsers;
            trend.add(point);
        }
        result.put("trend", trend);
        result.put("newUsers", newUserTotal);
        result.put("activeUsers", orderMapper.countActiveUsers(start, end));

        List<Map<String, Object>> topList = new ArrayList<>();
        for (Map<String, Object> row : orderMapper.topMerchants(start, end, 10)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("merchantId", row.get("merchantId"));
            item.put("shopName", row.get("shopName") == null ? "未知店铺" : row.get("shopName"));
            item.put("orderCount", readLong(row, "orderCount"));
            item.put("amount", readAmount(row, "amount").setScale(2, RoundingMode.HALF_UP));
            topList.add(item);
        }
        result.put("topMerchants", topList);
        return result;
    }

    /**
     * 环比增长率（百分比，保留两位）。上期为 0 时：本期也为 0 记 0，否则记 100。
     */
    private double growthRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current == null || current.compareTo(BigDecimal.ZERO) == 0 ? 0.0 : 100.0;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private BigDecimal readAmount(Map<String, Object> row, String key) {
        if (row == null || row.get(key) == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(row.get(key).toString());
    }

    private long readLong(Map<String, Object> row, String key) {
        if (row == null || row.get(key) == null) {
            return 0L;
        }
        return ((Number) row.get(key)).longValue();
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

    private void fillShopInfo(Merchant merchant) {
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getMerchantId, merchant.getId())
                .last("LIMIT 1"));
        if (shop != null) {
            merchant.setShopId(shop.getId());
            merchant.setShopName(shop.getName());
        }
    }

    /**
     * 订单列表补充店铺名称与状态文案，避免管理端列表只看到一堆数字 ID。
     */
    private void fillOrderSummary(Order order) {
        order.setStatusDesc(OrderStatus.descOf(order.getStatus()));
        if (order.getShopId() == null) {
            return;
        }
        Shop shop = shopMapper.selectById(order.getShopId());
        if (shop != null) {
            order.setShopName(shop.getName());
            order.setShopLogo(shop.getLogo());
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
