package com.eatwhat.modules.user.service;

import com.eatwhat.modules.user.dto.AddressRequest;
import com.eatwhat.modules.user.dto.UpdateProfileRequest;
import com.eatwhat.modules.user.entity.UserAddress;
import com.eatwhat.modules.user.vo.UserInfoVO;

import java.util.List;

/**
 * 用户中心服务。
 */
public interface UserService {

    /** 获取当前用户信息 */
    UserInfoVO currentUser();

    /** 修改个人资料 */
    void updateProfile(UpdateProfileRequest request);

    /** 修改密码；传入当前访问令牌，修改成功后该令牌失效 */
    void changePassword(UpdateProfileRequest.ChangePasswordRequest request, String accessToken);

    /** 地址列表 */
    List<UserAddress> listAddresses();

    /** 顾客-16：按关键词搜索本人历史地址 */
    List<UserAddress> searchAddresses(String keyword);

    /** 新增地址，返回地址 ID */
    Long addAddress(AddressRequest request);

    /** 修改地址 */
    void updateAddress(Long id, AddressRequest request);

    /** 删除地址 */
    void deleteAddress(Long id);

    /**
     * 注销账号：仅限消费者，存在进行中订单时拒绝。注销后账号立即失效，历史数据保留。
     */
    void deactivate();
}
