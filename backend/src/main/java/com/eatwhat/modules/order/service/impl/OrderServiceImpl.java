package com.eatwhat.modules.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.constant.OrderStatus;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.result.PageResult;
import com.eatwhat.common.utils.BizNoGenerator;
import com.eatwhat.common.utils.DeliveryFeeUtils;
import com.eatwhat.common.utils.GeoUtils;
import com.eatwhat.common.utils.UserContext;
import com.eatwhat.modules.content.service.SensitiveWordService;
import com.eatwhat.modules.cart.entity.CartItem;
import com.eatwhat.modules.cart.mapper.CartItemMapper;
import com.eatwhat.modules.cart.service.CartService;
import com.eatwhat.modules.delivery.entity.DeliveryTask;
import com.eatwhat.modules.delivery.entity.Rider;
import com.eatwhat.modules.delivery.mapper.DeliveryTaskMapper;
import com.eatwhat.modules.delivery.mapper.RiderMapper;
import com.eatwhat.modules.delivery.service.DeliveryService;
import com.eatwhat.modules.dish.entity.Dish;
import com.eatwhat.modules.dish.entity.DishAddon;
import com.eatwhat.modules.dish.entity.DishComboItem;
import com.eatwhat.modules.dish.entity.DishSpec;
import com.eatwhat.modules.dish.mapper.DishMapper;
import com.eatwhat.modules.merchant.entity.Merchant;
import com.eatwhat.modules.merchant.mapper.MerchantMapper;
import com.eatwhat.modules.message.service.MessageService;
import com.eatwhat.modules.order.dto.CreateOrderRequest;
import com.eatwhat.modules.order.entity.Order;
import com.eatwhat.modules.order.entity.OrderItem;
import com.eatwhat.modules.order.entity.OrderStatusLog;
import com.eatwhat.modules.order.mapper.OrderItemMapper;
import com.eatwhat.modules.order.mapper.OrderMapper;
import com.eatwhat.modules.order.mapper.OrderStatusLogMapper;
import com.eatwhat.modules.order.service.OrderService;
import com.eatwhat.modules.order.vo.OrderDetailVO;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.mapper.ShopMapper;
import com.eatwhat.modules.user.entity.UserAddress;
import com.eatwhat.modules.user.mapper.UserAddressMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单服务实现。
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final ShopMapper shopMapper;
    private final DishMapper dishMapper;
    private final CartItemMapper cartItemMapper;
    private final com.eatwhat.modules.dish.mapper.DishSpecMapper specMapper;
    private final com.eatwhat.modules.dish.mapper.DishAddonMapper addonMapper;
    private final CartService cartService;
    private final UserAddressMapper addressMapper;
    private final RiderMapper riderMapper;
    private final DeliveryTaskMapper deliveryTaskMapper;
    private final MerchantMapper merchantMapper;
    private final DeliveryService deliveryService;
    private final MessageService messageService;
    private final com.eatwhat.modules.dish.service.DishComboService dishComboService;
    private final com.eatwhat.modules.shop.service.ShopBusinessHourService businessHourService;

    /** 敏感词服务；单元测试环境下为空，调用前须判空 */
    @Autowired(required = false)
    private SensitiveWordService sensitiveWordService;

    public OrderServiceImpl(OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper,
                            OrderStatusLogMapper statusLogMapper,
                            ShopMapper shopMapper,
                            DishMapper dishMapper,
                            CartItemMapper cartItemMapper,
            com.eatwhat.modules.dish.mapper.DishSpecMapper specMapper,
            com.eatwhat.modules.dish.mapper.DishAddonMapper addonMapper,
                            CartService cartService,
                            UserAddressMapper addressMapper,
                            RiderMapper riderMapper,
                            DeliveryTaskMapper deliveryTaskMapper,
                            MerchantMapper merchantMapper,
                            @Lazy DeliveryService deliveryService,
                            @Lazy MessageService messageService,
                            com.eatwhat.modules.dish.service.DishComboService dishComboService,
                            com.eatwhat.modules.shop.service.ShopBusinessHourService businessHourService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.statusLogMapper = statusLogMapper;
        this.shopMapper = shopMapper;
        this.dishMapper = dishMapper;
        this.cartItemMapper = cartItemMapper;
        this.specMapper = specMapper;
        this.addonMapper = addonMapper;
        this.cartService = cartService;
        this.addressMapper = addressMapper;
        this.riderMapper = riderMapper;
        this.deliveryTaskMapper = deliveryTaskMapper;
        this.merchantMapper = merchantMapper;
        this.deliveryService = deliveryService;
        this.messageService = messageService;
        this.dishComboService = dishComboService;
        this.businessHourService = businessHourService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(Long userId, CreateOrderRequest request) {
        Shop shop = shopMapper.selectById(request.getShopId());
        if (shop == null) {
            throw BizException.of(ErrorCode.SHOP_NOT_FOUND);
        }
        if (!Integer.valueOf(CommonConstants.SHOP_OPEN).equals(shop.getIsOpen())
                || !Integer.valueOf(1).equals(shop.getStatus())) {
            throw BizException.of(ErrorCode.SHOP_CLOSED);
        }
        // 商家-08：营业时段之外不可下单（优先使用按星期的时段配置）
        Boolean weeklyOpen = businessHourService.openNow(shop.getId());
        if (weeklyOpen != null) {
            if (!weeklyOpen) {
                throw BizException.of(ErrorCode.SHOP_CLOSED, "商家当前不在营业时段内");
            }
        } else if (!com.eatwhat.common.utils.TimeUtils.inBusinessHours(shop.getOpenTime(), shop.getCloseTime())) {
            throw BizException.of(ErrorCode.SHOP_CLOSED, "商家当前不在营业时段内");
        }

        UserAddress address = addressMapper.selectById(request.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw BizException.of(ErrorCode.ADDRESS_NOT_FOUND);
        }

        // 组装下单明细：显式 items 优先，否则取该店铺购物车
        List<CreateOrderRequest.Item> requested = request.getItems();
        if (requested == null || requested.isEmpty()) {
            List<CartItem> cartItems = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, userId)
                    .eq(CartItem::getShopId, shop.getId()));
            if (cartItems.isEmpty()) {
                throw BizException.of(ErrorCode.CART_EMPTY);
            }
            requested = new ArrayList<>();
            for (CartItem cartItem : cartItems) {
                CreateOrderRequest.Item item = new CreateOrderRequest.Item();
                item.setDishId(cartItem.getDishId());
                item.setQuantity(cartItem.getQuantity());
                item.setSpecId(cartItem.getSpecId());
                item.setAddonIds(parseIds(cartItem.getAddonIds()));
                requested.add(item);
            }
        }

        BigDecimal goodsAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (CreateOrderRequest.Item requestItem : requested) {
            Dish dish = dishMapper.selectById(requestItem.getDishId());
            if (dish == null) {
                throw BizException.of(ErrorCode.DISH_NOT_FOUND);
            }
            if (!dish.getShopId().equals(shop.getId())) {
                throw BizException.of(ErrorCode.CART_CROSS_SHOP);
            }
            if (!Integer.valueOf(CommonConstants.DISH_ON).equals(dish.getStatus())) {
                throw BizException.of(ErrorCode.DISH_OFF_SALE, "菜品【" + dish.getName() + "】已下架");
            }
            if (dish.getStock() == null || dish.getStock() < requestItem.getQuantity()) {
                throw BizException.of(ErrorCode.STOCK_NOT_ENOUGH, "菜品【" + dish.getName() + "】库存不足");
            }

            // 顾客-27/28：规格决定单价，加料叠加价格
            DishSpec spec = resolveSpec(dish, requestItem.getSpecId());
            BigDecimal unitPrice = spec == null || spec.getPrice() == null ? dish.getPrice() : spec.getPrice();
            List<DishAddon> addons = resolveAddons(dish, requestItem.getAddonIds());
            BigDecimal addonPrice = BigDecimal.ZERO;
            List<String> addonNames = new ArrayList<>();
            for (DishAddon addon : addons) {
                addonNames.add(addon.getName());
                addonPrice = addonPrice.add(addon.getPrice() == null ? BigDecimal.ZERO : addon.getPrice());
            }
            BigDecimal finalPrice = unitPrice.add(addonPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = finalPrice.multiply(BigDecimal.valueOf(requestItem.getQuantity()));
            goodsAmount = goodsAmount.add(subtotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setDishId(dish.getId());
            orderItem.setDishName(dish.getName());
            orderItem.setDishImage(dish.getImage());
            orderItem.setSpecName(spec == null ? null : spec.getName());
            orderItem.setAddonNames(addonNames.isEmpty() ? null : String.join(",", addonNames));
            orderItem.setAddonPrice(addonPrice.setScale(2, RoundingMode.HALF_UP));
            orderItem.setPrice(finalPrice);
            orderItem.setQuantity(requestItem.getQuantity());
            orderItem.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
            items.add(orderItem);
        }

        BigDecimal minOrderAmount = shop.getMinOrderAmount() == null ? BigDecimal.ZERO : shop.getMinOrderAmount();
        if (goodsAmount.compareTo(minOrderAmount) < 0) {
            throw BizException.of(ErrorCode.BELOW_MIN_AMOUNT,
                    "未达到起送金额 ¥" + minOrderAmount.setScale(2, RoundingMode.HALF_UP));
        }

        // 配送范围校验，同时把距离用于阶梯配送费计算
        BigDecimal distanceKm = null;
        if (address.getLatitude() != null && address.getLongitude() != null
                && shop.getLatitude() != null && shop.getLongitude() != null) {
            distanceKm = GeoUtils.distanceKm(shop.getLatitude(), shop.getLongitude(),
                    address.getLatitude(), address.getLongitude());
            if (distanceKm != null && shop.getDeliveryRange() != null
                    && distanceKm.multiply(BigDecimal.valueOf(1000))
                    .compareTo(BigDecimal.valueOf(shop.getDeliveryRange())) > 0) {
                throw BizException.of(ErrorCode.OUT_OF_DELIVERY_RANGE);
            }
        }

        BigDecimal baseFee = shop.getDeliveryFee() == null ? BigDecimal.ZERO : shop.getDeliveryFee();
        BigDecimal deliveryFee = DeliveryFeeUtils.calculate(baseFee, shop.getDeliveryFeeType(), distanceKm);
        // 商家-14：打包费随订单快照
        BigDecimal packagingFee = shop.getPackagingFee() == null ? BigDecimal.ZERO : shop.getPackagingFee();
        BigDecimal totalAmount = goodsAmount.add(deliveryFee).add(packagingFee).setScale(2, RoundingMode.HALF_UP);

        Order order = new Order();
        order.setOrderNo(BizNoGenerator.orderNo());
        order.setUserId(userId);
        order.setShopId(shop.getId());
        order.setMerchantId(shop.getMerchantId());
        order.setStatus(CommonConstants.ORDER_PENDING_PAYMENT);
        order.setGoodsAmount(goodsAmount.setScale(2, RoundingMode.HALF_UP));
        order.setDeliveryFee(deliveryFee.setScale(2, RoundingMode.HALF_UP));
        order.setPackagingFee(packagingFee.setScale(2, RoundingMode.HALF_UP));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(totalAmount);
        order.setReceiverName(address.getContactName());
        order.setReceiverPhone(address.getPhone());
        order.setReceiverAddress(fullAddress(address));
        order.setReceiverLng(address.getLongitude());
        order.setReceiverLat(address.getLatitude());
        checkRemark(request.getRemark());
        order.setRemark(request.getRemark());
        order.setTablewareCount(request.getTablewareCount() == null ? 0 : request.getTablewareCount());
        orderMapper.insert(order);

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
            deductDishStock(item.getDishId(), item.getQuantity());
        }

        // 清空该店铺购物车
        cartService.clearByShop(userId, shop.getId());

        writeLog(order.getId(), null, CommonConstants.ORDER_PENDING_PAYMENT, userId, "CONSUMER", "创建订单");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("totalAmount", order.getTotalAmount());
        result.put("status", order.getStatus());
        result.put("statusDesc", OrderStatus.descOf(order.getStatus()));
        result.put("expireAt", order.getCreatedAt() == null ? null
                : order.getCreatedAt().plusMinutes(CommonConstants.ORDER_PAY_TIMEOUT_MINUTES));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> pay(Long userId, Long orderId) {
        Order order = requireOwnedOrder(userId, orderId);
        if (!Integer.valueOf(CommonConstants.ORDER_PENDING_PAYMENT).equals(order.getStatus())) {
            throw BizException.of(ErrorCode.ORDER_ALREADY_PAID);
        }
        LocalDateTime expireAt = order.getCreatedAt() == null ? LocalDateTime.now()
                : order.getCreatedAt().plusMinutes(CommonConstants.ORDER_PAY_TIMEOUT_MINUTES);
        if (expireAt.isBefore(LocalDateTime.now())) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "订单已超时，请重新下单");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("payUrl", "mock://pay?orderNo=" + order.getOrderNo()
                + "&amount=" + order.getTotalAmount().toPlainString());
        result.put("expireAt", expireAt);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payCallback(String orderNo, String tradeNo, BigDecimal amount) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw BizException.of(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!Integer.valueOf(CommonConstants.ORDER_PENDING_PAYMENT).equals(order.getStatus())) {
            // 幂等：已处理过的回调直接忽略
            log.info("支付回调重复处理 orderNo={} status={}", orderNo, order.getStatus());
            return;
        }
        order.setStatus(CommonConstants.ORDER_PENDING_ACCEPT);
        order.setPayType(1);
        order.setPayTime(LocalDateTime.now());
        order.setTradeNo(tradeNo == null ? BizNoGenerator.tradeNo() : tradeNo);
        orderMapper.updateById(order);
        writeLog(order.getId(), CommonConstants.ORDER_PENDING_PAYMENT, CommonConstants.ORDER_PENDING_ACCEPT,
                CommonConstants.SYSTEM_OPERATOR_ID, "SYSTEM", "支付成功，等待商家接单");
    }

    @Override
    public PageResult<Order> pageMyOrders(Long userId, Integer status, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getId);
        Page<Order> result = orderMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(this::fillSummary);
        return PageResult.of(result);
    }

    @Override
    public OrderDetailVO detail(Long userId, String role, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BizException.of(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!"ADMIN".equals(role)) {
            if (!order.getUserId().equals(userId)) {
                // 商家只能查看自己店铺的订单；骑手只能查看自己已接单的订单
                boolean allowed = false;
                if ("MERCHANT".equals(role)) {
                    Long merchantId = currentMerchantId(userId);
                    allowed = merchantId != null && merchantId.equals(order.getMerchantId());
                } else if ("RIDER".equals(role) && order.getRiderId() != null) {
                    Rider rider = riderMapper.selectById(order.getRiderId());
                    allowed = rider != null && rider.getUserId().equals(userId);
                }
                if (!allowed) {
                    throw BizException.of(ErrorCode.DATA_NOT_OWNED);
                }
            }
        }

        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(OrderStatus.descOf(order.getStatus()));
        vo.setGoodsAmount(order.getGoodsAmount());
        vo.setDeliveryFee(order.getDeliveryFee());
        vo.setPackagingFee(order.getPackagingFee());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayType(order.getPayType());
        vo.setTradeNo(order.getTradeNo());
        vo.setPayTime(order.getPayTime());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setCompletedAt(order.getCompletedAt());
        vo.setExpireAt(order.getCreatedAt() == null ? null
                : order.getCreatedAt().plusMinutes(CommonConstants.ORDER_PAY_TIMEOUT_MINUTES));
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setRemark(order.getRemark());
        vo.setTablewareCount(order.getTablewareCount());
        vo.setCancelReason(order.getCancelReason());

        Shop shop = shopMapper.selectById(order.getShopId());
        if (shop != null) {
            OrderDetailVO.ShopInfo shopInfo = new OrderDetailVO.ShopInfo();
            shopInfo.setId(shop.getId());
            shopInfo.setName(shop.getName());
            shopInfo.setLogo(shop.getLogo());
            shopInfo.setPhone(shop.getPhone());
            vo.setShop(shopInfo);
        }
        if (order.getRiderId() != null) {
            Rider rider = riderMapper.selectById(order.getRiderId());
            if (rider != null) {
                OrderDetailVO.RiderInfo riderInfo = new OrderDetailVO.RiderInfo();
                riderInfo.setId(rider.getId());
                riderInfo.setName(rider.getRealName());
                riderInfo.setPhone(rider.getContactPhone());
                riderInfo.setTotalOrders(rider.getTotalOrders());
                vo.setRider(riderInfo);
            }
        }

        // 预计送达时间：下单时间 + 30 分钟备餐 + 配送距离每公里 3 分钟
        if (order.getCreatedAt() != null) {
            BigDecimal km = resolveDistanceKm(order);
            long minutes = 30L + (km == null ? 0L : km.multiply(BigDecimal.valueOf(3)).setScale(0, RoundingMode.CEILING).longValue());
            vo.setEstimatedArrivalAt(order.getCreatedAt().plusMinutes(minutes));
        }

        vo.setItems(orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId)));

        List<OrderStatusLog> logs = statusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, orderId)
                .orderByAsc(OrderStatusLog::getId));
        logs.forEach(l -> l.setStatusDesc(OrderStatus.descOf(l.getToStatus())));
        vo.setStatusLogs(logs);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transition(Long orderId, int targetStatus, String role, String remark) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BizException.of(ErrorCode.ORDER_NOT_FOUND);
        }
        int from = order.getStatus();
        OrderStatus.assertTransition(from, targetStatus, role);

        order.setStatus(targetStatus);
        if (targetStatus == CommonConstants.ORDER_COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
        }
        orderMapper.updateById(order);
        writeLog(orderId, from, targetStatus, UserContext.userId() == null
                ? CommonConstants.SYSTEM_OPERATOR_ID : UserContext.userId(), role, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long orderId, String reason) {
        Order order = requireOwnedOrder(userId, orderId);
        int from = order.getStatus();
        if (!OrderStatus.consumerCancelableStatuses().contains(from)) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "当前订单状态不可取消");
        }
        restoreStock(order.getId());
        order.setStatus(CommonConstants.ORDER_CANCELLED);
        order.setCancelReason(reason == null || reason.isBlank() ? "用户主动取消" : reason.trim());
        orderMapper.updateById(order);
        writeLog(orderId, from, CommonConstants.ORDER_CANCELLED, userId, "CONSUMER", order.getCancelReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long userId, Long orderId) {
        Order order = requireOwnedOrder(userId, orderId);
        if (!Integer.valueOf(CommonConstants.ORDER_DELIVERING).equals(order.getStatus())) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "当前订单状态不可确认收货");
        }
        order.setStatus(CommonConstants.ORDER_COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        writeLog(orderId, CommonConstants.ORDER_DELIVERING, CommonConstants.ORDER_COMPLETED,
                userId, "CONSUMER", "用户确认收货");
    }

    @Override
    public Order getById(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    @Override
    public PageResult<Order> pageMerchantOrders(Long merchantId, Integer status, String keyword,
                                                LocalDate startDate, LocalDate endDate,
                                                int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId)
                .eq(status != null, Order::getStatus, status)
                .like(keyword != null && !keyword.isBlank(), Order::getOrderNo,
                        keyword == null ? null : keyword.trim())
                .ge(startDate != null, Order::getCreatedAt,
                        startDate == null ? null : startDate.atStartOfDay())
                .lt(endDate != null, Order::getCreatedAt,
                        endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .orderByDesc(Order::getId);
        Page<Order> result = orderMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(this::fillSummary);
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void merchantAccept(Long merchantId, Long orderId) {
        Order order = requireMerchantOrder(merchantId, orderId);
        transition(orderId, CommonConstants.ORDER_PREPARING, "MERCHANT", "商家已接单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void merchantReject(Long merchantId, Long orderId, String reason) {
        Order order = requireMerchantOrder(merchantId, orderId);
        int from = order.getStatus();
        if (!Integer.valueOf(CommonConstants.ORDER_PENDING_ACCEPT).equals(from)) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "仅待接单订单可拒单");
        }
        restoreStock(orderId);
        order.setStatus(CommonConstants.ORDER_REFUNDING);
        order.setCancelReason(reason == null || reason.isBlank() ? "商家拒单" : reason.trim());
        orderMapper.updateById(order);
        writeLog(orderId, from, CommonConstants.ORDER_REFUNDING, UserContext.userId(), "MERCHANT",
                order.getCancelReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void merchantReady(Long merchantId, Long orderId) {
        Order order = requireMerchantOrder(merchantId, orderId);
        transition(orderId, CommonConstants.ORDER_PENDING_DELIVERY, "MERCHANT", "商家备餐完成，等待骑手接单");

        Shop shop = shopMapper.selectById(order.getShopId());
        String pickup = shop == null ? "" : shop.getAddress();
        Double distance = null;
        if (shop != null && shop.getLatitude() != null && shop.getLongitude() != null
                && order.getReceiverLng() != null && order.getReceiverLat() != null) {
            distance = GeoUtils.distanceKm(shop.getLatitude(), shop.getLongitude(),
                    order.getReceiverLat(), order.getReceiverLng()).doubleValue();
        }
        DeliveryTask task = deliveryService.createTask(orderId, order.getShopId(), pickup,
                order.getReceiverAddress(), distance);
        log.info("订单 {} 备餐完成，配送任务 {}", orderId, task.getId());
    }

    @Override
    public Map<String, Object> receipt(Long merchantId, Long orderId) {
        Order order = requireMerchantOrder(merchantId, orderId);
        Shop shop = shopMapper.selectById(order.getShopId());
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        List<Map<String, Object>> lines = new ArrayList<>();
        for (OrderItem item : items) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("name", item.getDishName());
            line.put("specName", item.getSpecName());
            line.put("addonNames", item.getAddonNames());
            line.put("quantity", item.getQuantity());
            line.put("price", item.getPrice());
            line.put("subtotal", item.getSubtotal());
            lines.add(line);
        }
        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("shopName", shop == null ? null : shop.getName());
        receipt.put("shopPhone", shop == null ? null : shop.getPhone());
        receipt.put("shopAddress", shop == null ? null : shop.getAddress());
        receipt.put("orderNo", order.getOrderNo());
        receipt.put("createdAt", order.getCreatedAt());
        receipt.put("status", order.getStatus());
        receipt.put("items", lines);
        receipt.put("goodsAmount", order.getGoodsAmount());
        receipt.put("deliveryFee", order.getDeliveryFee());
        receipt.put("packagingFee", order.getPackagingFee());
        receipt.put("discountAmount", order.getDiscountAmount());
        receipt.put("totalAmount", order.getTotalAmount());
        receipt.put("tablewareCount", order.getTablewareCount());
        receipt.put("remark", order.getRemark());
        receipt.put("receiverName", order.getReceiverName());
        receipt.put("receiverPhone", order.getReceiverPhone());
        receipt.put("receiverAddress", order.getReceiverAddress());
        receipt.put("printTime", LocalDateTime.now());
        return receipt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(CommonConstants.ORDER_PAY_TIMEOUT_MINUTES);
        List<Order> timeoutOrders = orderMapper.selectTimeoutOrders(deadline, 100);
        for (Order order : timeoutOrders) {
            restoreStock(order.getId());
            order.setStatus(CommonConstants.ORDER_CANCELLED);
            order.setCancelReason("超时未支付，系统自动关闭");
            orderMapper.updateById(order);
            writeLog(order.getId(), CommonConstants.ORDER_PENDING_PAYMENT, CommonConstants.ORDER_CANCELLED,
                    CommonConstants.SYSTEM_OPERATOR_ID, "SYSTEM", "超时未支付，系统自动关闭");
        }
        if (!timeoutOrders.isEmpty()) {
            log.info("自动关闭超时未支付订单 {} 笔", timeoutOrders.size());
        }
        return timeoutOrders.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceCancel(Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BizException.of(ErrorCode.ORDER_NOT_FOUND);
        }
        int from = order.getStatus();
        if (from == CommonConstants.ORDER_CANCELLED) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "订单已取消，无需重复操作");
        }
        if (from == CommonConstants.ORDER_COMPLETED) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "订单已完成，请走售后流程退款");
        }
        // 未支付订单没有实际扣款，其他状态都视为已支付，需要退回库存
        restoreStock(orderId);
        String cancelReason = reason == null || reason.isBlank() ? "平台强制取消" : reason.trim();
        order.setStatus(CommonConstants.ORDER_CANCELLED);
        order.setCancelReason(cancelReason);
        orderMapper.updateById(order);
        writeLog(orderId, from, CommonConstants.ORDER_CANCELLED,
                UserContext.userId() == null ? CommonConstants.SYSTEM_OPERATOR_ID : UserContext.userId(),
                "SYSTEM", "平台强制取消：" + cancelReason);
    }

    @Override
    public PageResult<Order> pageAdminOrders(Integer status, String keyword, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(status != null, Order::getStatus, status)
                .like(keyword != null && !keyword.isBlank(), Order::getOrderNo,
                        keyword == null ? null : keyword.trim())
                .orderByDesc(Order::getId);
        Page<Order> result = orderMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(this::fillSummary);
        return PageResult.of(result);
    }

    /**
     * 归还订单中所有菜品的库存（取消 / 拒单 / 超时关闭）。
     */
    private void restoreStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            releaseDishStock(item.getDishId(), item.getQuantity());
        }
    }

    /**
     * 扣减菜品库存（商家-28：套餐同时扣减组成菜品的库存）。
     */
    private void deductDishStock(Long dishId, Integer quantity) {
        int rows = dishMapper.deductStock(dishId, quantity);
        if (rows == 0) {
            Dish dish = dishMapper.selectById(dishId);
            throw BizException.of(ErrorCode.STOCK_NOT_ENOUGH,
                    "菜品【" + (dish == null ? dishId : dish.getName()) + "】库存不足");
        }
        Dish dish = dishMapper.selectById(dishId);
        if (dish != null && Integer.valueOf(1).equals(dish.getIsCombo())) {
            for (DishComboItem comboItem : dishComboService.mapItemsByCombo(List.of(dishId))
                    .getOrDefault(dishId, List.of())) {
                int need = comboItem.getQuantity() == null ? 1 : comboItem.getQuantity();
                int subRows = dishMapper.deductStock(comboItem.getDishId(), need * quantity);
                if (subRows == 0) {
                    throw BizException.of(ErrorCode.STOCK_NOT_ENOUGH,
                            "套餐【" + dish.getName() + "】中的菜品【" + comboItem.getDishName() + "】库存不足");
                }
            }
        }
    }

    /**
     * 归还菜品库存（商家-28：套餐同时归还组成菜品的库存）。
     */
    private void releaseDishStock(Long dishId, Integer quantity) {
        dishMapper.restoreStock(dishId, quantity);
        Dish dish = dishMapper.selectById(dishId);
        if (dish != null && Integer.valueOf(1).equals(dish.getIsCombo())) {
            for (DishComboItem comboItem : dishComboService.mapItemsByCombo(List.of(dishId))
                    .getOrDefault(dishId, List.of())) {
                int need = comboItem.getQuantity() == null ? 1 : comboItem.getQuantity();
                dishMapper.restoreStock(comboItem.getDishId(), need * quantity);
            }
        }
    }

    private Order requireOwnedOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BizException.of(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw BizException.of(ErrorCode.DATA_NOT_OWNED);
        }
        return order;
    }

    private Order requireMerchantOrder(Long merchantId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw BizException.of(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getMerchantId().equals(merchantId)) {
            throw BizException.of(ErrorCode.DATA_NOT_OWNED);
        }
        return order;
    }

    /**
     * 按登录用户 ID 查询其商家主键；非商家账号返回 null。
     */
    /**
     * 订单备注敏感词检测。
     */
    /** 解析菜品规格：显式指定则校验归属，否则取默认规格，无规格返回 null */
    private DishSpec resolveSpec(Dish dish, Long specId) {
        if (specId != null) {
            DishSpec spec = specMapper.selectById(specId);
            if (spec == null || !spec.getDishId().equals(dish.getId())) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "菜品规格不存在");
            }
            return spec;
        }
        return specMapper.selectOne(new LambdaQueryWrapper<DishSpec>()
                .eq(DishSpec::getDishId, dish.getId())
                .eq(DishSpec::getIsDefault, 1)
                .last("LIMIT 1"));
    }

    /** 解析加料列表：校验数量上限与归属 */
    private List<DishAddon> resolveAddons(Dish dish, List<Long> addonIds) {
        if (addonIds == null || addonIds.isEmpty()) {
            return new ArrayList<>();
        }
        if (addonIds.size() > 5) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "单个菜品最多选择 5 项加料");
        }
        List<DishAddon> addons = addonMapper.selectList(new LambdaQueryWrapper<DishAddon>()
                .in(DishAddon::getId, addonIds));
        if (addons.size() != addonIds.size()) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "存在无效的加料选项");
        }
        for (DishAddon addon : addons) {
            if (!addon.getDishId().equals(dish.getId())) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "加料不属于当前菜品");
            }
        }
        return addons;
    }

    /** 把逗号分隔的 ID 串解析为列表 */
    private List<Long> parseIds(String joined) {
        List<Long> ids = new ArrayList<>();
        if (joined == null || joined.isBlank()) {
            return ids;
        }
        for (String part : joined.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                ids.add(Long.valueOf(trimmed));
            }
        }
        return ids;
    }
    private void checkRemark(String remark) {
        if (sensitiveWordService == null || remark == null || remark.isBlank()) {
            return;
        }
        List<String> hits = sensitiveWordService.hits(remark, "REMARK");
        if (hits != null && !hits.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_INVALID,
                    "订单备注包含敏感词：" + String.join("、", hits) + "，请修改后再提交");
        }
    }

    private Long currentMerchantId(Long userId) {
        if (userId == null) {
            return null;
        }
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, userId)
                .last("LIMIT 1"));
        return merchant == null ? null : merchant.getId();
    }

    private void writeLog(Long orderId, Integer fromStatus, Integer toStatus,
                          Long operatorId, String operatorRole, String remark) {
        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderId(orderId);
        statusLog.setFromStatus(fromStatus);
        statusLog.setToStatus(toStatus);
        statusLog.setOperatorId(operatorId);
        statusLog.setOperatorRole(operatorRole);
        statusLog.setRemark(remark);
        statusLogMapper.insert(statusLog);
        notifyStatusChange(orderId, toStatus, remark);
    }

    /**
     * 订单状态变更时给顾客推送一条站内消息。
     * 消息属于旁路能力，内部已吞异常，不影响订单主流程。
     */
    private void notifyStatusChange(Long orderId, Integer toStatus, String remark) {
        if (orderId == null || toStatus == null) {
            return;
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getUserId() == null) {
            return;
        }
        String title;
        String content;
        String no = order.getOrderNo() == null ? "" : order.getOrderNo();
        if (toStatus == CommonConstants.ORDER_PENDING_PAYMENT) {
            title = "订单创建成功";
            content = "订单 " + no + " 已创建，请在 15 分钟内完成支付，超时自动取消。";
        } else if (toStatus == CommonConstants.ORDER_PENDING_ACCEPT) {
            title = "支付成功";
            content = "订单 " + no + " 已支付成功，等待商家接单。";
        } else if (toStatus == CommonConstants.ORDER_PREPARING) {
            title = "商家已接单";
            content = "商家已接下订单 " + no + "，正在为你准备餐品。";
        } else if (toStatus == CommonConstants.ORDER_PENDING_DELIVERY) {
            title = "餐品已出餐";
            content = "订单 " + no + " 已出餐，正在等待骑手接单配送。";
        } else if (toStatus == CommonConstants.ORDER_DELIVERING) {
            title = "骑手已取餐";
            content = "骑手已取走订单 " + no + " 的餐品，正在配送途中。";
        } else if (toStatus == CommonConstants.ORDER_COMPLETED) {
            title = "订单已完成";
            content = "订单 " + no + " 已送达完成，别忘了给商家和骑手一个评价哦。";
        } else if (toStatus == CommonConstants.ORDER_CANCELLED) {
            title = "订单已取消";
            content = "订单 " + no + " 已取消" + (remark == null || remark.isBlank() ? "。" : "：" + remark);
        } else if (toStatus == CommonConstants.ORDER_REFUNDING) {
            title = "订单退款中";
            content = "订单 " + no + " 已发起退款" + (remark == null || remark.isBlank() ? "。" : "：" + remark);
        } else {
            return;
        }
        messageService.send(order.getUserId(), 1, title, content, order.getId(), order.getOrderNo(), null);
        notifyRider(order, toStatus, title, content);
    }

    /**
     * 骑手-31：订单状态变更时同步给承接该订单的骑手推送一条站内消息。
     * 未分配骑手时静默跳过，异常同样旁路吞掉，不影响订单主流程。
     */
    private void notifyRider(Order order, Integer toStatus, String title, String content) {
        try {
            DeliveryTask task = deliveryTaskMapper.selectOne(new LambdaQueryWrapper<DeliveryTask>()
                    .eq(DeliveryTask::getOrderId, order.getId())
                    .isNotNull(DeliveryTask::getRiderId)
                    .orderByDesc(DeliveryTask::getId)
                    .last("LIMIT 1"));
            if (task == null || task.getRiderId() == null) {
                return;
            }
            Rider rider = riderMapper.selectById(task.getRiderId());
            if (rider == null || rider.getUserId() == null) {
                return;
            }
            messageService.send(rider.getUserId(), 1, title, content,
                    order.getId(), order.getOrderNo(), task.getId());
        } catch (Exception e) {
            log.warn("推送骑手订单消息失败，orderId={}, status={}", order.getId(), toStatus, e);
        }
    }

    /**
     * 补齐列表页需要的展示字段：店铺名、菜名摘要、状态中文、支付截止时间。
     */
    private void fillSummary(Order order) {
        order.setStatusDesc(OrderStatus.descOf(order.getStatus()));
        if (order.getCreatedAt() != null) {
            order.setExpireAt(order.getCreatedAt().plusMinutes(CommonConstants.ORDER_PAY_TIMEOUT_MINUTES));
        }
        if (order.getShopId() != null) {
            Shop shop = shopMapper.selectById(order.getShopId());
            if (shop != null) {
                order.setShopName(shop.getName());
                order.setShopLogo(shop.getLogo());
                order.setShopPhone(shop.getPhone());
            }
        }
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        if (!items.isEmpty()) {
            OrderItem first = items.get(0);
            int totalQuantity = items.stream()
                    .mapToInt(i -> i.getQuantity() == null ? 0 : i.getQuantity())
                    .sum();
            order.setItemSummary(items.size() == 1
                    ? first.getDishName() + " ×" + totalQuantity
                    : first.getDishName() + " 等 " + items.size() + " 件");
        }
    }

    /**
     * 估算订单配送距离（公里）：优先用订单是否还有对应配送任务的距离，否则用店铺与收货地址坐标计算。
     */
    private BigDecimal resolveDistanceKm(Order order) {
        Shop shop = shopMapper.selectById(order.getShopId());
        if (shop == null || shop.getLatitude() == null || shop.getLongitude() == null) {
            return null;
        }
        UserAddress address = addressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, order.getUserId())
                .eq(UserAddress::getIsDefault, 1)
                .last("LIMIT 1"));
        if (address == null || address.getLatitude() == null || address.getLongitude() == null) {
            return null;
        }
        return GeoUtils.distanceKm(shop.getLatitude(), shop.getLongitude(),
                address.getLatitude(), address.getLongitude());
    }

    private String fullAddress(UserAddress address) {
        StringBuilder sb = new StringBuilder();
        if (address.getProvince() != null) {
            sb.append(address.getProvince());
        }
        if (address.getCity() != null) {
            sb.append(address.getCity());
        }
        if (address.getDistrict() != null) {
            sb.append(address.getDistrict());
        }
        if (address.getDetail() != null) {
            sb.append(address.getDetail());
        }
        return sb.toString();
    }
}
