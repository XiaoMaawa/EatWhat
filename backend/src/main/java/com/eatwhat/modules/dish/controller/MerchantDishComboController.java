package com.eatwhat.modules.dish.controller;

import com.eatwhat.common.result.R;
import com.eatwhat.common.utils.UserContext;
import com.eatwhat.modules.dish.dto.DishComboRequest;
import com.eatwhat.modules.dish.entity.DishComboItem;
import com.eatwhat.modules.dish.service.DishComboService;
import com.eatwhat.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 商家端菜品套餐管理接口（商家-28）。
 */
@Tag(name = "商家-菜品套餐")
@RestController
@RequestMapping("/api/v1/merchant/dishes/{dishId}")
@RequireRole({"MERCHANT", "ADMIN"})
public class MerchantDishComboController {

    private final DishComboService dishComboService;

    public MerchantDishComboController(DishComboService dishComboService) {
        this.dishComboService = dishComboService;
    }

    @Operation(summary = "套餐组成项")
    @GetMapping("/combo")
    public R<List<DishComboItem>> list(@PathVariable Long dishId) {
        return R.ok(dishComboService.listItems(UserContext.userId(), dishId));
    }

    @Operation(summary = "保存套餐组成项（全量覆盖）")
    @PutMapping("/combo")
    public R<List<DishComboItem>> save(@PathVariable Long dishId,
                                       @Valid @RequestBody DishComboRequest request) {
        return R.ok(dishComboService.saveItems(UserContext.userId(), dishId, request));
    }

    @Operation(summary = "标记/取消套餐")
    @PatchMapping("/combo/flag")
    public R<Void> mark(@PathVariable Long dishId, @RequestBody Map<String, Integer> body) {
        dishComboService.markCombo(UserContext.userId(), dishId,
                body == null ? null : body.get("isCombo"));
        return R.ok();
    }
}
