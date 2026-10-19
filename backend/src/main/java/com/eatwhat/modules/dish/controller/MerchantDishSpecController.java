package com.eatwhat.modules.dish.controller;

import com.eatwhat.common.result.R;
import com.eatwhat.modules.dish.dto.DishAddonRequest;
import com.eatwhat.modules.dish.dto.DishSpecRequest;
import com.eatwhat.modules.dish.entity.DishAddon;
import com.eatwhat.modules.dish.entity.DishSpec;
import com.eatwhat.modules.dish.service.DishSpecService;
import com.eatwhat.security.RequireRole;
import com.eatwhat.common.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家端菜品规格与加料管理接口。
 */
@Tag(name = "商家-菜品规格与加料")
@RestController
@RequestMapping("/api/v1/merchant/dishes/{dishId}")
@RequireRole({"MERCHANT", "ADMIN"})
public class MerchantDishSpecController {

    private final DishSpecService dishSpecService;

    public MerchantDishSpecController(DishSpecService dishSpecService) {
        this.dishSpecService = dishSpecService;
    }

    @Operation(summary = "规格列表")
    @GetMapping("/specs")
    public R<List<DishSpec>> listSpecs(@PathVariable Long dishId) {
        return R.ok(dishSpecService.listSpecs(UserContext.userId(), dishId));
    }

    @Operation(summary = "新增规格")
    @PostMapping("/specs")
    public R<DishSpec> createSpec(@PathVariable Long dishId, @Valid @RequestBody DishSpecRequest request) {
        return R.ok(dishSpecService.createSpec(UserContext.userId(), dishId, request));
    }

    @Operation(summary = "编辑规格")
    @PutMapping("/specs/{specId}")
    public R<Void> updateSpec(@PathVariable Long dishId, @PathVariable Long specId,
                              @Valid @RequestBody DishSpecRequest request) {
        dishSpecService.updateSpec(UserContext.userId(), dishId, specId, request);
        return R.ok();
    }

    @Operation(summary = "删除规格")
    @DeleteMapping("/specs/{specId}")
    public R<Void> deleteSpec(@PathVariable Long dishId, @PathVariable Long specId) {
        dishSpecService.deleteSpec(UserContext.userId(), dishId, specId);
        return R.ok();
    }

    @Operation(summary = "加料列表")
    @GetMapping("/addons")
    public R<List<DishAddon>> listAddons(@PathVariable Long dishId) {
        return R.ok(dishSpecService.listAddons(UserContext.userId(), dishId));
    }

    @Operation(summary = "新增加料")
    @PostMapping("/addons")
    public R<DishAddon> createAddon(@PathVariable Long dishId, @Valid @RequestBody DishAddonRequest request) {
        return R.ok(dishSpecService.createAddon(UserContext.userId(), dishId, request));
    }

    @Operation(summary = "编辑加料")
    @PutMapping("/addons/{addonId}")
    public R<Void> updateAddon(@PathVariable Long dishId, @PathVariable Long addonId,
                               @Valid @RequestBody DishAddonRequest request) {
        dishSpecService.updateAddon(UserContext.userId(), dishId, addonId, request);
        return R.ok();
    }

    @Operation(summary = "删除加料")
    @DeleteMapping("/addons/{addonId}")
    public R<Void> deleteAddon(@PathVariable Long dishId, @PathVariable Long addonId) {
        dishSpecService.deleteAddon(UserContext.userId(), dishId, addonId);
        return R.ok();
    }
}
