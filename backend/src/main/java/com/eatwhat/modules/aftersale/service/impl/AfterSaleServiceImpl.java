package com.eatwhat.modules.aftersale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.constant.OrderStatus;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.result.PageResult;
import com.eatwhat.common.utils.UserContext;
import com.eatwhat.modules.aftersale.dto.AfterSaleApplyRequest;
import com.eatwhat.modules.aftersale.entity.AfterSale;
import com.eatwhat.modules.aftersale.mapper.AfterSaleMapper;
import com.eatwhat.modules.aftersale.service.AfterSaleService;
import com.eatwhat.modules.auth.entity.SysUser;
import com.eatwhat.modules.auth.mapper.SysUserMapper;
import com.eatwhat.modules.merchant.entity.Merchant;
import com.eatwhat.modules.merchant.mapper.MerchantMapper;
import com.eatwhat.modules.order.entity.Order;
import com.eatwhat.modules.order.mapper.OrderMapper;
import com.eatwhat.modules.order.service.OrderService;
import com.eatwhat.modules.shop.entity.Shop;
import com.eatwhat.modules.shop.mapper.ShopMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 售后服务实现：顾客申请 → 商家处理 → 顾客可申请平台介入 → 平台裁定。
 */
@Service
public class AfterSaleServiceImpl implements AfterSaleService {

    private static final Logger log = LoggerFactory.getLogger(AfterSaleServiceImpl.class);

    /** 售后类型 */
    private static final int TYPE_REFUND = 1;
    private static final int TYPE_APPEAL = 2;

    /** 售后状态 */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_REJECTED = 2;
    private static final int STATUS_CANCELLED = 3;
    private static final int STATUS_ARBITRATING = 4;
    private static final int STATUS_SUPPORT_CUSTOMER = 5;
    private static final int STATUS_SUPPORT_MERCHANT = 6;

    private final AfterSaleMapper afterSaleMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final ShopMapper shopMapper;
    private final MerchantMapper merchantMapper;
    private final SysUserMapper userMapper;

    public AfterSaleServiceImpl(AfterSaleMapper afterSaleMapper, OrderMapper orderMapper,
                               OrderService orderService, ShopMapper shopMapper,
                               MerchantMapper merchantMapper, SysUserMapper userMapper) {
        this.afterSaleMapper = afterSaleMapper;
        this.orderMapper = orderMapper;
        this.orderService = orderService;
        this.shopMapper = shopMapper;
        this.merchantMapper = merchantMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AfterSale apply(Long userId, AfterSaleApplyRequest request) {
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw BizException.of(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!userId.equals(order.getUserId())) {
            throw BizException.of(ErrorCode.DATA_NOT_OWNED);
        }
        // 退款：已支付未接单；申诉：已完成（7 天内）
        int type = request.getType() == null ? inferType(order) : request.getType();
        if (type != TYPE_REFUND && type != TYPE_APPEAL) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "售后类型只能是 1 退款或 2 申诉");
        }
        if (type == TYPE_REFUND) {
            if (!Integer.valueOf(CommonConstants.ORDER_PENDING_ACCEPT).equals(order.getStatus())) {
                throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "仅已支付未接单的订单可申请退款");
            }
        } else {
            if (!Integer.valueOf(CommonConstants.ORDER_COMPLETED).equals(order.getStatus())) {
                throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "仅已完成的订单可提交申诉");
            }
            if (order.getCompletedAt() != null
                    && order.getCompletedAt().plusDays(CommonConstants.REVIEW_VALID_DAYS).isBefore(LocalDateTime.now())) {
                throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL,
                        "订单完成后超过 " + CommonConstants.REVIEW_VALID_DAYS + " 天，无法再申诉");
            }
        }

        Long exists = afterSaleMapper.selectCount(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getOrderId, order.getId())
                .eq(AfterSale::getType, type)
                .in(AfterSale::getStatus, STATUS_PENDING, STATUS_APPROVED, STATUS_ARBITRATING,
                        STATUS_SUPPORT_CUSTOMER));
        if (exists != null && exists > 0) {
            throw BizException.of(ErrorCode.DATA_CONFLICT, "该订单已有处理中的售后申请，请勿重复提交");
        }

        AfterSale afterSale = new AfterSale();
        afterSale.setAfterSaleNo(generateNo());
        afterSale.setOrderId(order.getId());
        afterSale.setOrderNo(order.getOrderNo());
        afterSale.setUserId(userId);
        afterSale.setShopId(order.getShopId());
        afterSale.setMerchantId(order.getMerchantId());
        afterSale.setType(type);
        afterSale.setReason(request.getReason().trim());
        afterSale.setImages(joinImages(request.getImages()));
        // 退款申请全额退，申诉不预设金额
        afterSale.setAmount(type == TYPE_REFUND
                ? (order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                : BigDecimal.ZERO);
        afterSale.setStatus(STATUS_PENDING);
        afterSaleMapper.insert(afterSale);
        afterSale.setImageList(splitImages(afterSale.getImages()));
        fillDesc(afterSale, true);
        return afterSale;
    }

    @Override
    public PageResult<AfterSale> pageMine(Long userId, Integer status, int page, int size) {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getUserId, userId)
                .eq(status != null, AfterSale::getStatus, status)
                .orderByDesc(AfterSale::getId);
        Page<AfterSale> result = afterSaleMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(a -> {
            fillDesc(a, false);
            Shop shop = shopMapper.selectById(a.getShopId());
            if (shop != null) {
                a.setShopName(shop.getName());
            }
        });
        return PageResult.of(result);
    }

    @Override
    public AfterSale detail(Long userId, String role, Long afterSaleId) {
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw BizException.of(ErrorCode.DATA_CONFLICT, "售后申请不存在");
        }
        boolean allowed = "ADMIN".equals(role) || userId.equals(afterSale.getUserId());
        if (!allowed && "MERCHANT".equals(role)) {
            Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                    .eq(Merchant::getUserId, userId).last("LIMIT 1"));
            allowed = merchant != null && merchant.getId().equals(afterSale.getMerchantId());
        }
        if (!allowed) {
            throw BizException.of(ErrorCode.DATA_NOT_OWNED);
        }
        fillDesc(afterSale, true);
        afterSale.setProgress(buildProgress(afterSale));
        return afterSale;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long afterSaleId) {
        AfterSale afterSale = requireOwned(userId, afterSaleId);
        if (!Integer.valueOf(STATUS_PENDING).equals(afterSale.getStatus())) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "商家已处理，无法撤销申请");
        }
        afterSale.setStatus(STATUS_CANCELLED);
        afterSaleMapper.updateById(afterSale);
    }

    @Override
    public PageResult<AfterSale> pageMerchant(Long merchantId, Integer status, int page, int size) {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getMerchantId, merchantId)
                .eq(status != null, AfterSale::getStatus, status)
                .orderByDesc(AfterSale::getId);
        Page<AfterSale> result = afterSaleMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(a -> {
            fillDesc(a, false);
            Shop shop = shopMapper.selectById(a.getShopId());
            if (shop != null) {
                a.setShopName(shop.getName());
            }
        });
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleByMerchant(Long userId, Long afterSaleId, String action, String reply) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, userId).last("LIMIT 1"));
        if (merchant == null) {
            throw BizException.of(ErrorCode.MERCHANT_NOT_FOUND);
        }
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw BizException.of(ErrorCode.DATA_CONFLICT, "售后申请不存在");
        }
        if (!merchant.getId().equals(afterSale.getMerchantId())) {
            throw BizException.of(ErrorCode.DATA_NOT_OWNED);
        }
        if (!Integer.valueOf(STATUS_PENDING).equals(afterSale.getStatus())) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "该售后申请已处理");
        }
        String act = action == null ? "" : action.trim().toUpperCase();
        if ("APPROVE".equals(act)) {
            afterSale.setStatus(STATUS_APPROVED);
            afterSale.setRefundedAt(LocalDateTime.now());
            // 同意退款：订单流转到退款中，由平台/定时任务完成退款
            if (Integer.valueOf(TYPE_REFUND).equals(afterSale.getType())) {
                orderService.transition(afterSale.getOrderId(), CommonConstants.ORDER_REFUNDING,
                        "MERCHANT", "商家同意退款：" + (reply == null ? "无" : reply));
            }
        } else if ("REJECT".equals(act)) {
            if (reply == null || reply.trim().length() < 10) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "拒绝售后需填写不少于 10 字的理由");
            }
            afterSale.setStatus(STATUS_REJECTED);
        } else {
            throw BizException.of(ErrorCode.PARAM_INVALID, "action 只能是 APPROVE 或 REJECT");
        }
        afterSale.setMerchantReply(reply);
        afterSale.setMerchantHandledAt(LocalDateTime.now());
        afterSaleMapper.updateById(afterSale);
    }

    @Override
    public PageResult<AfterSale> pageAdmin(Integer status, String keyword, int page, int size) {
        LambdaQueryWrapper<AfterSale> wrapper = new LambdaQueryWrapper<AfterSale>()
                .eq(status != null, AfterSale::getStatus, status)
                .like(keyword != null && !keyword.isBlank(), AfterSale::getOrderNo,
                        keyword == null ? null : keyword.trim())
                .orderByDesc(AfterSale::getId);
        Page<AfterSale> result = afterSaleMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(a -> fillDesc(a, true));
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void arbitrate(Long adminId, Long afterSaleId, boolean supportCustomer, String verdict) {
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw BizException.of(ErrorCode.DATA_CONFLICT, "售后申请不存在");
        }
        if (!Integer.valueOf(STATUS_REJECTED).equals(afterSale.getStatus())
                && !Integer.valueOf(STATUS_ARBITRATING).equals(afterSale.getStatus())) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "该售后申请不在待仲裁状态");
        }
        if (supportCustomer) {
            afterSale.setStatus(STATUS_SUPPORT_CUSTOMER);
            afterSale.setRefundedAt(LocalDateTime.now());
            orderService.transition(afterSale.getOrderId(), CommonConstants.ORDER_REFUNDING,
                    "SYSTEM", "平台裁定支持顾客，全额退款：" + (verdict == null ? "无" : verdict));
        } else {
            afterSale.setStatus(STATUS_SUPPORT_MERCHANT);
        }
        afterSale.setAdminId(adminId);
        afterSale.setAdminVerdict(verdict);
        afterSale.setAdminVerdictAt(LocalDateTime.now());
        afterSaleMapper.updateById(afterSale);
        log.info("售后 {} 裁定完成，支持顾客={}", afterSale.getId(), supportCustomer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void escalate(Long userId, Long afterSaleId) {
        AfterSale afterSale = requireOwned(userId, afterSaleId);
        if (!Integer.valueOf(STATUS_REJECTED).equals(afterSale.getStatus())) {
            throw BizException.of(ErrorCode.ORDER_STATUS_ILLEGAL, "仅商家拒绝的售后可申请平台介入");
        }
        afterSale.setStatus(STATUS_ARBITRATING);
        afterSaleMapper.updateById(afterSale);
    }

    private AfterSale requireOwned(Long userId, Long afterSaleId) {
        AfterSale afterSale = afterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw BizException.of(ErrorCode.DATA_CONFLICT, "售后申请不存在");
        }
        if (!userId.equals(afterSale.getUserId())) {
            throw BizException.of(ErrorCode.DATA_NOT_OWNED);
        }
        return afterSale;
    }

    private int inferType(Order order) {
        if (Integer.valueOf(CommonConstants.ORDER_COMPLETED).equals(order.getStatus())) {
            return TYPE_APPEAL;
        }
        return TYPE_REFUND;
    }

    private String generateNo() {
        String time = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(LocalDateTime.now());
        int random = java.util.concurrent.ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "AS" + time + random;
    }

    private String joinImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        List<String> cleaned = new ArrayList<>();
        for (String image : images) {
            if (image != null && !image.isBlank()) {
                cleaned.add(image.trim());
            }
        }
        return cleaned.isEmpty() ? null : String.join(",", cleaned);
    }

    private List<String> splitImages(String images) {
        if (images == null || images.isBlank()) {
            return new ArrayList<>();
        }
        List<String> list = new ArrayList<>();
        for (String image : images.split(",")) {
            if (!image.isBlank()) {
                list.add(image.trim());
            }
        }
        return list;
    }

    private void fillDesc(AfterSale afterSale, boolean withNames) {
        afterSale.setImageList(splitImages(afterSale.getImages()));
        afterSale.setStatusDesc(statusDesc(afterSale.getStatus()));
        afterSale.setTypeDesc(Integer.valueOf(TYPE_REFUND).equals(afterSale.getType()) ? "退款" : "申诉");
        if (!withNames) {
            return;
        }
        Order order = orderMapper.selectById(afterSale.getOrderId());
        if (order != null) {
            afterSale.setOrderAmount(order.getTotalAmount());
        }
        Shop shop = shopMapper.selectById(afterSale.getShopId());
        if (shop != null) {
            afterSale.setShopName(shop.getName());
        }
        SysUser user = userMapper.selectById(afterSale.getUserId());
        if (user != null) {
            afterSale.setNickname(user.getNickname());
        }
    }

    private String statusDesc(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case STATUS_PENDING -> "待商家处理";
            case STATUS_APPROVED -> "商家已同意";
            case STATUS_REJECTED -> "商家已拒绝";
            case STATUS_CANCELLED -> "已撤销";
            case STATUS_ARBITRATING -> "待平台仲裁";
            case STATUS_SUPPORT_CUSTOMER -> "平台支持顾客";
            case STATUS_SUPPORT_MERCHANT -> "平台支持商家";
            default -> "未知状态";
        };
    }

    /**
     * 退款进度节点：提交 → 商家处理 → （被拒时有平台仲裁）→ 退款完成。
     */
    private List<Map<String, Object>> buildProgress(AfterSale afterSale) {
        List<Map<String, Object>> progress = new ArrayList<>();
        progress.add(node("提交申请", afterSale.getCreatedAt(), true, afterSale.getReason()));

        Integer status = afterSale.getStatus();
        boolean handled = status != null && status != STATUS_PENDING && status != STATUS_CANCELLED;
        progress.add(node("商家处理", afterSale.getMerchantHandledAt(), handled,
                afterSale.getMerchantReply()));

        boolean escalated = Integer.valueOf(STATUS_ARBITRATING).equals(status)
                || Integer.valueOf(STATUS_SUPPORT_CUSTOMER).equals(status)
                || Integer.valueOf(STATUS_SUPPORT_MERCHANT).equals(status);
        if (escalated) {
            progress.add(node("平台仲裁", afterSale.getAdminVerdictAt(), true, afterSale.getAdminVerdict()));
        }
        boolean refunded = Integer.valueOf(STATUS_SUPPORT_CUSTOMER).equals(status)
                || (Integer.valueOf(STATUS_APPROVED).equals(status)
                && Integer.valueOf(TYPE_REFUND).equals(afterSale.getType()));
        progress.add(node("退款完成", afterSale.getRefundedAt(), refunded, null));
        return progress;
    }

    private Map<String, Object> node(String title, LocalDateTime time, boolean done, String remark) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("title", title);
        node.put("time", time);
        node.put("done", done);
        node.put("remark", remark);
        return node;
    }

    /** 供管理端复用：读取状态码常量 */
    public static int statusApproved() {
        return STATUS_APPROVED;
    }

    /** 供管理端复用：读取状态码常量 */
    public static int statusArbitrating() {
        return STATUS_ARBITRATING;
    }

    /** 供管理端复用：读取状态码常量 */
    public static int statusRefundedRef() {
        return STATUS_SUPPORT_CUSTOMER;
    }

    /** 订单状态中文描述，供控制层返回 */
    public static String orderStatusDesc(Integer status) {
        return OrderStatus.descOf(status);
    }

    /** 当前登录用户ID透传（保持与其它服务一致） */
    public static Long currentUserId() {
        return UserContext.userId();
    }
}
