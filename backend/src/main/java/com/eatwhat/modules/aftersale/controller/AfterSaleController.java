package com.eatwhat.modules.aftersale.controller;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.common.result.R;
import com.eatwhat.common.utils.UserContext;
import com.eatwhat.modules.aftersale.dto.AfterSaleApplyRequest;
import com.eatwhat.modules.aftersale.entity.AfterSale;
import com.eatwhat.modules.aftersale.service.AfterSaleService;
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
 * 顾客侧售后接口：申请退款 / 申诉、我的售后、详情、撤销、申请平台介入。
 */
@RestController
@RequestMapping("/api/v1/after-sales")
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    public AfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    @PostMapping
    @RequireRole("CONSUMER")
    public R<AfterSale> apply(@Valid @RequestBody AfterSaleApplyRequest request) {
        return R.ok(afterSaleService.apply(UserContext.userId(), request));
    }

    @GetMapping
    @RequireRole("CONSUMER")
    public R<PageResult<AfterSale>> mine(@RequestParam(required = false) Integer status,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return R.ok(afterSaleService.pageMine(UserContext.userId(), status, page, size));
    }

    @GetMapping("/{id}")
    public R<AfterSale> detail(@PathVariable Long id) {
        return R.ok(afterSaleService.detail(UserContext.userId(), UserContext.role(), id));
    }

    @PostMapping("/{id}/cancel")
    @RequireRole("CONSUMER")
    public R<Void> cancel(@PathVariable Long id) {
        afterSaleService.cancel(UserContext.userId(), id);
        return R.ok();
    }

    @PostMapping("/{id}/escalate")
    @RequireRole("CONSUMER")
    public R<Void> escalate(@PathVariable Long id) {
        afterSaleService.escalate(UserContext.userId(), id);
        return R.ok();
    }
}
