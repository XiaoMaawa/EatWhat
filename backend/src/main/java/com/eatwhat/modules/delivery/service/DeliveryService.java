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
     * 获取当前已通过审核的骑手档案，未通过则抛 RIDER_NOT_APPROVED。
     */
    Rider requireApprovedRider(Long userId);

    /**
     * 可抢的配送任务列表。
     */
    List<DeliveryTask> availableTasks(Long userId);

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
     * 商家备餐完成后创建配送任务。
     */
    DeliveryTask createTask(Long orderId, Long shopId, String pickupAddress, String deliveryAddress, Double distance);
}
