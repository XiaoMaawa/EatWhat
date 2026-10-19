package com.eatwhat.modules.cart.controller;

import com.eatwhat.common.result.R;
import com.eatwhat.common.utils.UserContext;
import com.eatwhat.modules.cart.dto.AddCartRequest;
import com.eatwhat.modules.cart.dto.UpdateCartRequest;
import com.eatwhat.modules.cart.service.CartService;
import com.eatwhat.modules.cart.vo.CartVO;
import com.eatwhat.security.RequireRole;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 购物车接口（仅顾客）。
 */
@Tag(name = "购物车")
@RequireRole("CONSUMER")
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(summary = "查看购物车")
    @GetMapping
    public R<CartVO> view(@RequestParam(required = false) Long addressId) {
        return R.ok(cartService.view(UserContext.userId(), addressId));
    }

    @Operation(summary = "加入购物车")
    @PostMapping
    public R<CartVO> add(@Valid @RequestBody AddCartRequest request) {
        return R.ok(cartService.add(UserContext.userId(), request));
    }

    @Operation(summary = "修改购物车数量")
    @PutMapping("/{id}")
    public R<CartVO> update(@PathVariable Long id, @Valid @RequestBody UpdateCartRequest request) {
        return R.ok(cartService.updateQuantity(UserContext.userId(), id, request.getQuantity()));
    }

    @Operation(summary = "再来一单")
    @PostMapping("/reorder/{orderId}")
    public R<CartVO> reorder(@PathVariable Long orderId) {
        return R.ok(cartService.reorder(UserContext.userId(), orderId));
    }

    @Operation(summary = "删除购物车项")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        cartService.remove(UserContext.userId(), id);
        return R.ok();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping
    public R<Void> clear() {
        cartService.remove(UserContext.userId(), null);
        return R.ok();
    }
}
