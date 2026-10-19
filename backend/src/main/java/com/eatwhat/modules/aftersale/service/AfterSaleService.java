package com.eatwhat.modules.aftersale.service;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.aftersale.dto.AfterSaleApplyRequest;
import com.eatwhat.modules.aftersale.entity.AfterSale;

/**
 * 售后服务。
 */
public interface AfterSaleService {

    /** 顾客提交退款申请或申诉 */
    AfterSale apply(Long userId, AfterSaleApplyRequest request);

    /** 顾客售后列表 */
    PageResult<AfterSale> pageMine(Long userId, Integer status, int page, int size);

    /** 顾客售后详情（含进度节点） */
    AfterSale detail(Long userId, String role, Long afterSaleId);

    /** 顾客撤销售后申请（商家未处理时） */
    void cancel(Long userId, Long afterSaleId);

    /** 商家售后列表 */
    PageResult<AfterSale> pageMerchant(Long merchantId, Integer status, int page, int size);

    /** 商家同意 / 拒绝售后 */
    void handleByMerchant(Long userId, Long afterSaleId, String action, String reply);

    /** 平台售后争议列表 */
    PageResult<AfterSale> pageAdmin(Integer status, String keyword, int page, int size);

    /** 平台裁定：support > 0 支持顾客，否则支持商家 */
    void arbitrate(Long adminId, Long afterSaleId, boolean supportCustomer, String verdict);

    /** 顾客申请平台介入，把商家拒绝的售后转入待仲裁 */
    void escalate(Long userId, Long afterSaleId);
}
