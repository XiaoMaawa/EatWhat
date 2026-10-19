package com.eatwhat.modules.aftersale.controller;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.common.result.R;
import com.eatwhat.common.utils.UserContext;
import com.eatwhat.modules.aftersale.dto.AfterSaleHandleRequest;
import com.eatwhat.modules.aftersale.entity.AfterSale;
import com.eatwhat.modules.aftersale.service.AfterSaleService;
import com.eatwhat.modules.merchant.service.MerchantService;
import com.eatwhat.security.RequireRole;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家侧售后接口：售后列表与同意/拒绝处理。
 */
@RestController
@RequestMapping("/api/v1/merchant/after-sales")
@RequireRole({"MERCHANT", "ADMIN"})
public class MerchantAfterSaleController {

    private final AfterSaleService afterSaleService;
    private final MerchantService merchantService;

    public MerchantAfterSaleController(AfterSaleService afterSaleService,
                                       MerchantService merchantService) {
        this.afterSaleService = afterSaleService;
        this.merchantService = merchantService;
    }

    @GetMapping
    public R<PageResult<AfterSale>> list(@RequestParam(required = false) Integer status,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        Long merchantId = merchantService.requireApprovedMerchant(UserContext.userId()).getId();
        return R.ok(afterSaleService.pageMerchant(merchantId, status, page, size));
    }

    @GetMapping("/{id}")
    public R<AfterSale> detail(@PathVariable Long id) {
        return R.ok(afterSaleService.detail(UserContext.userId(), UserContext.role(), id));
    }

    @PostMapping("/{id}/handle")
    public R<Void> handle(@PathVariable Long id, @Valid @RequestBody AfterSaleHandleRequest request) {
        afterSaleService.handleByMerchant(UserContext.userId(), id, request.getAction(),
                request.getReply());
        return R.ok();
    }
}
