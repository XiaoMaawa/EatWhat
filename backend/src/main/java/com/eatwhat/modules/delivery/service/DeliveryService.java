package com.eatwhat.modules.delivery.service;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.delivery.dto.RiderApplyRequest;
import com.eatwhat.modules.delivery.entity.DeliveryTask;
import com.eatwhat.modules.delivery.entity.Rider;

import java.util.List;
import java.util.Map;

/**
 * 骑手与配送任务服务。
 */
public interface DeliveryService {

    /**
     * 骑手提交认证申请。
     */
    Rider apply(Long userId, RiderApplyRequest request);

    /**
     * 骑手查看自己的认证信息（含审核状态）。
     */
    Rider profile(Long userId);
    /**
     * 楠戞墜鏇存柊璧勬枡锛氳仈绯荤數璇濄€佸父鐢ㄩ厤閫佸尯鍩熴€佸仴搴疯瘉鏈夋晥鏈燂紙楠戞墜-28 / 楠戞墜-05锛夈€?     */
    Rider updateProfile(Long userId, com.eatwhat.modules.delivery.dto.RiderProfileRequest request);

    /**
     * 鍋ュ悍璇佸埌鏈熸彁閱掞紙楠戞墜-05锛夛細璺濆埌鏈?<= 30 澶╂垨宸茶繃鏈熸椂杩斿洖鎻愰啋锛屽惁鍒欒繑鍥?null銆?     */
    java.util.Map<String, Object> healthReminder(Long userId);

    /**
     * 获取当前已通过审核的骑手档案，未通过则抛 RIDER_NOT_APPROVED。
     */
    Rider requireApprovedRider(Long userId);

    /**
     * 可抢的配送任务列表。
     *
     * @param sortBy 排序方式：distance 按距离升序 / fee 按配送费降序，空则按默认
     * @param minDistance 最小配送距离（公里，可空）
     * @param maxDistance 最大配送距离（公里，可空）
     * @param minFee 最低配送费（元，可空）
     * @param maxFee 最高配送费（元，可空）
     */
    List<DeliveryTask> availableTasks(Long userId, String sortBy, java.math.BigDecimal minDistance,
                                      java.math.BigDecimal maxDistance, java.math.BigDecimal minFee,
                                      java.math.BigDecimal maxFee);

    /**
     * 更新骑手接单设置：接单开关、接单半径、同时接单上限。
     */
    void updateSettings(Long userId, Integer accepting, java.math.BigDecimal maxDistance, Integer maxOrders);

    /**
     * 抢单。
     */
    void grab(Long userId, Long taskId);

    /**
     * 骑手取餐。
     */
    void pickup(Long userId, Long taskId);

    /**
     * 骑手送达。
     */
    void deliver(Long userId, Long taskId);

    /**
     * 骑手任务列表。
     */
    PageResult<DeliveryTask> listTasks(Long userId, String scope, int page, int size);

    /**
     * 骑手统计。
     */
    Map<String, Object> stats(Long userId);

    /**
     * 骑手收入明细（按订单逐单，含准时标记），日期区间与准时过滤可选。
     */
    PageResult<DeliveryTask> incomeDetail(Long userId, String startDate, String endDate,
                                          Integer onTime, int page, int size);

    /**
     * 商家备餐完成后创建配送任务。
     */
    DeliveryTask createTask(Long orderId, Long shopId, String pickupAddress, String deliveryAddress, Double distance);
}
