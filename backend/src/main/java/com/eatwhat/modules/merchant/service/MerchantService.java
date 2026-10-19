package com.eatwhat.modules.merchant.service;

import com.eatwhat.modules.merchant.dto.MerchantApplyRequest;
import com.eatwhat.modules.merchant.entity.Merchant;

import java.util.Map;

/**
 * 商家入驻与资料服务。
 */
public interface MerchantService {

    /**
     * 提交入驻申请（重复提交时重置为待审核）。
     */
    Merchant apply(Long userId, MerchantApplyRequest request);

    /**
     * 查看入驻资料（含审核状态与关联店铺）。
     */
    Map<String, Object> profile(Long userId);

    /**
     * 获取当前商家档案，不存在则抛 MERCHANT_NOT_FOUND。
     */
    Merchant currentMerchant(Long userId);

    /**
     * 获取已通过审核的商家档案，未通过则抛 MERCHANT_NOT_APPROVED。
     */
    Merchant requireApprovedMerchant(Long userId);

    /**
     * 商家经营统计。
     */
    Map<String, Object> stats(Long userId, String type);

    /**
     * 经营分析扩展（商家-51/55/56/57）：客单价、评分统计、评价评分统计、营业时段分析。
     */
    Map<String, Object> analytics(Long userId, String type);

    /**
     * 商家-58：修改联系人姓名与联系电话。
     */
    Merchant updateAccount(Long userId, String realName, String contactPhone);
}
