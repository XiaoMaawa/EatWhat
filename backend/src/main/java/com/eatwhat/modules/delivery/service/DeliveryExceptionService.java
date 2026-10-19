package com.eatwhat.modules.delivery.service;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.delivery.dto.DeliveryExceptionRequest;
import com.eatwhat.modules.delivery.entity.DeliveryException;

/**
 * 配送异常与转单申请服务（docs/09 骑手-19 转单申请、骑手-20 配送异常上报）。
 */
public interface DeliveryExceptionService {

    /**
     * 骑手提交转单申请：任务退回订单池，订单回到待配送，其他骑手可重新抢单。
     */
    DeliveryException transfer(Long userId, Long taskId, DeliveryExceptionRequest request);

    /**
     * 骑手上报配送异常（联系不上顾客、地址错误等），仅记录并通知，不改动任务状态。
     */
    DeliveryException report(Long userId, Long taskId, DeliveryExceptionRequest request);

    /**
     * 骑手查看自己提交过的异常与转单记录。
     */
    PageResult<DeliveryException> pageMine(Long userId, Integer type, int page, int size);

    /**
     * 管理端查看全部异常与转单记录。
     */
    PageResult<DeliveryException> pageAll(Integer type, Integer status, String keyword, int page, int size);

    /**
     * 管理员处理异常记录并给出说明。
     */
    void handle(Long adminId, Long exceptionId, String remark);
}
