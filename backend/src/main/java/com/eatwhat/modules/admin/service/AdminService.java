package com.eatwhat.modules.admin.service;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.admin.entity.OperationLog;
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
     * 管理员账号列表。
     */
    PageResult<SysUser> pageAdmins(String keyword, Integer status, int page, int size);

    /**
     * 新增管理员账号。
     */
    SysUser createAdmin(String phone, String nickname, String password);

    /**
     * 用户列表。
     */
    PageResult<SysUser> pageUsers(String keyword, String role, Integer status, int page, int size);

    /**
     * 用户详情。
     */
    SysUser userDetail(Long userId);

    /**
     * 指定用户的历史订单。
     */
    PageResult<Order> pageUserOrders(Long userId, Integer status, int page, int size);

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
     * 开启 / 关闭骑手接单权限（不影响进行中订单）。
     */
    void updateRiderAccepting(Long riderId, Integer accepting);

    /**
     * 恢复被下架菜品。
     */
    void onlineDish(Long dishId);

    /**
     * 异常订单筛选：timeoutAccept / timeoutDeliver / refunding。
     */
    PageResult<Order> pageAbnormalOrders(String type, int page, int size);

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
     * 违规下架菜品，需填写不少于 5 字的违规原因（管理员-34）。
     */
    void offlineDish(Long dishId, String reason);

    /**
     * 首页看板。
     */
    Map<String, Object> dashboard();

    /**
     * 统计看板：交易额、订单量、完成率、用户增长与商家排行。
     *
     * @param period day / week / month，默认 day
     */
    Map<String, Object> statistics(String period);

    /**
     * 操作日志分页查询。
     */
    PageResult<OperationLog> pageOperationLogs(String operatorRole, String module, Integer result,
                                               String startDate, String endDate, int page, int size);
}
