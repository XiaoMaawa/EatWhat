package com.eatwhat.modules.admin.service;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.auth.entity.SysUser;
import com.eatwhat.modules.dish.entity.Dish;
import com.eatwhat.modules.merchant.entity.Merchant;
import com.eatwhat.modules.delivery.entity.Rider;
import com.eatwhat.modules.order.entity.Order;

import java.util.Map;

/**
 * 平台管理端服务。
 */
public interface AdminService {

    /**
     * 用户列表。
     */
    PageResult<SysUser> pageUsers(String keyword, String role, Integer status, int page, int size);

    /**
     * 用户详情。
     */
    SysUser userDetail(Long userId);

    /**
     * 启用 / 禁用用户。
     */
    void updateUserStatus(Long userId, Integer status);

    /**
     * 商家列表。
     */
    PageResult<Merchant> pageMerchants(Integer auditStatus, String keyword, int page, int size);

    /**
     * 商家详情。
     */
    Merchant merchantDetail(Long merchantId);

    /**
     * 审核商家资质。
     */
    void auditMerchant(Long merchantId, Integer auditStatus, String auditRemark);

    /**
     * 冻结 / 解冻商家店铺。
     */
    void freezeMerchant(Long merchantId, Integer status);

    /**
     * 骑手列表。
     */
    PageResult<Rider> pageRiders(Integer auditStatus, String keyword, int page, int size);

    /**
     * 骑手详情。
     */
    Rider riderDetail(Long riderId);

    /**
     * 审核骑手认证。
     */
    void auditRider(Long riderId, Integer auditStatus, String auditRemark);

    /**
     * 平台订单列表。
     */
    PageResult<Order> pageOrders(Integer status, String keyword, int page, int size);

    /**
     * 订单详情（含明细）。
     */
    Map<String, Object> orderDetail(Long orderId);

    /**
     * 平台介入：强制退款或强制完成。
     */
    void interveneOrder(Long orderId, String action, String reason);

    /**
     * 平台菜品列表。
     */
    PageResult<Dish> pageDishes(String keyword, Long shopId, int page, int size);

    /**
     * 强制下架菜品。
     */
    void offlineDish(Long dishId);

    /**
     * 首页看板。
     */
    Map<String, Object> dashboard();
}
