package com.eatwhat.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.utils.MaskUtils;
import com.eatwhat.common.utils.UserContext;
import com.eatwhat.modules.auth.entity.SysUser;
import com.eatwhat.modules.auth.mapper.SysUserMapper;
import com.eatwhat.modules.user.dto.AddressRequest;
import com.eatwhat.modules.user.dto.UpdateProfileRequest;
import com.eatwhat.modules.user.entity.UserAddress;
import com.eatwhat.modules.user.mapper.UserAddressMapper;
import com.eatwhat.modules.user.service.UserService;
import com.eatwhat.modules.user.vo.UserInfoVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户中心服务实现。
 */
@Service
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final UserAddressMapper addressMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(SysUserMapper userMapper, UserAddressMapper addressMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.addressMapper = addressMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserInfoVO currentUser() {
        SysUser user = requireUser();
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setPhone(MaskUtils.phone(user.getPhone()));
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    @Override
    public void updateProfile(UpdateProfileRequest request) {
        SysUser user = requireUser();
        SysUser update = new SysUser();
        update.setId(user.getId());
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            update.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            update.setAvatar(request.getAvatar());
        }
        userMapper.updateById(update);
        UserContext.LoginUser loginUser = UserContext.get();
        if (loginUser != null && update.getNickname() != null) {
            loginUser.setNickname(update.getNickname());
        }
    }

    @Override
    public void changePassword(UpdateProfileRequest.ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "两次输入的新密码不一致");
        }
        SysUser user = requireUser();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "原密码不正确");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "新密码不能与原密码相同");
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(update);
    }

    @Override
    public List<UserAddress> listAddresses() {
        Long userId = UserContext.userId();
        return addressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addAddress(AddressRequest request) {
        Long userId = UserContext.userId();
        UserAddress address = new UserAddress();
        copy(request, address);
        address.setUserId(userId);
        if (address.getIsDefault() == null) {
            address.setIsDefault(0);
        }
        if (address.getIsDefault() == 1) {
            clearDefault(userId);
        }
        addressMapper.insert(address);
        return address.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAddress(Long id, AddressRequest request) {
        Long userId = UserContext.userId();
        UserAddress exist = addressMapper.selectById(id);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw BizException.of(ErrorCode.ADDRESS_NOT_FOUND);
        }
        UserAddress update = new UserAddress();
        update.setId(id);
        copy(request, update);
        if (update.getIsDefault() != null && update.getIsDefault() == 1) {
            clearDefault(userId);
        }
        addressMapper.updateById(update);
    }

    @Override
    public void deleteAddress(Long id) {
        Long userId = UserContext.userId();
        UserAddress exist = addressMapper.selectById(id);
        if (exist == null || !exist.getUserId().equals(userId)) {
            throw BizException.of(ErrorCode.ADDRESS_NOT_FOUND);
        }
        addressMapper.deleteById(id);

        // 删除的是默认地址时，自动把最近新增的一条设为默认
        if (Integer.valueOf(1).equals(exist.getIsDefault())) {
            UserAddress latest = addressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                    .eq(UserAddress::getUserId, userId)
                    .orderByDesc(UserAddress::getId)
                    .last("LIMIT 1"));
            if (latest != null) {
                addressMapper.update(null, new LambdaUpdateWrapper<UserAddress>()
                        .eq(UserAddress::getId, latest.getId())
                        .set(UserAddress::getIsDefault, 1));
            }
        }
    }

    private void clearDefault(Long userId) {
        addressMapper.update(null, new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, 1)
                .set(UserAddress::getIsDefault, 0));
    }

    private void copy(AddressRequest request, UserAddress target) {
        target.setContactName(request.getContactName());
        target.setPhone(request.getPhone());
        target.setProvince(request.getProvince());
        target.setCity(request.getCity());
        target.setDistrict(request.getDistrict());
        target.setDetail(request.getDetail());
        target.setLongitude(request.getLongitude());
        target.setLatitude(request.getLatitude());
        target.setIsDefault(request.getIsDefault());
        target.setTag(request.getTag());
    }

    private SysUser requireUser() {
        SysUser user = userMapper.selectById(UserContext.userId());
        if (user == null) {
            throw BizException.of(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
