package com.eatwhat.modules.user.controller;

import com.eatwhat.common.result.R;
import com.eatwhat.modules.user.dto.AddressRequest;
import com.eatwhat.modules.user.dto.UpdateProfileRequest;
import com.eatwhat.modules.user.entity.UserAddress;
import com.eatwhat.modules.user.service.UserService;
import com.eatwhat.modules.user.vo.UserInfoVO;
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

import java.util.List;
import java.util.Map;

/**
 * 用户中心接口 /users。
 */
@Tag(name = "用户模块")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public R<UserInfoVO> me() {
        return R.ok(userService.currentUser());
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/me")
    public R<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(request);
        return R.ok();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/me/password")
    public R<Void> changePassword(@Valid @RequestBody UpdateProfileRequest.ChangePasswordRequest request) {
        userService.changePassword(request);
        return R.ok();
    }

    @Operation(summary = "收货地址列表")
    @RequireRole("CONSUMER")
    @GetMapping("/me/addresses")
    public R<List<UserAddress>> addresses() {
        return R.ok(userService.listAddresses());
    }

    @Operation(summary = "地址搜索（顾客-16：关键词匹配本人历史地址）")
    @RequireRole("CONSUMER")
    @GetMapping("/me/addresses/search")
    public R<List<UserAddress>> searchAddresses(@RequestParam("kw") String keyword) {
        return R.ok(userService.searchAddresses(keyword));
    }

    @Operation(summary = "新增收货地址")
    @RequireRole("CONSUMER")
    @PostMapping("/me/addresses")
    public R<Map<String, Object>> addAddress(@Valid @RequestBody AddressRequest request) {
        return R.ok(Map.of("id", userService.addAddress(request)));
    }

    @Operation(summary = "修改收货地址")
    @RequireRole("CONSUMER")
    @PutMapping("/me/addresses/{id}")
    public R<Void> updateAddress(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        userService.updateAddress(id, request);
        return R.ok();
    }

    @Operation(summary = "注销账号")
    @RequireRole("CONSUMER")
    @PostMapping("/me/deactivate")
    public R<Void> deactivate() {
        userService.deactivate();
        return R.ok();
    }

    @Operation(summary = "删除收货地址")
    @RequireRole("CONSUMER")
    @DeleteMapping("/me/addresses/{id}")
    public R<Void> deleteAddress(@PathVariable Long id) {
        userService.deleteAddress(id);
        return R.ok();
    }
}
