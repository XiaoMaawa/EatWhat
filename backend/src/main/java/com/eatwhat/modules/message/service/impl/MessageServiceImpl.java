package com.eatwhat.modules.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.auth.entity.SysUser;
import com.eatwhat.modules.auth.mapper.SysUserMapper;
import com.eatwhat.modules.message.entity.UserMessage;
import com.eatwhat.modules.message.mapper.UserMessageMapper;
import com.eatwhat.modules.message.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户消息服务实现。
 */
@Service
public class MessageServiceImpl implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageServiceImpl.class);

    /** 消息类型：订单消息 */
    public static final int TYPE_ORDER = 1;
    /** 消息类型：系统公告 */
    public static final int TYPE_SYSTEM = 2;
    /** 消息类型：售后消息 */
    public static final int TYPE_AFTER_SALE = 3;

    private final UserMessageMapper messageMapper;
    private final SysUserMapper userMapper;

    public MessageServiceImpl(UserMessageMapper messageMapper, SysUserMapper userMapper) {
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PageResult<UserMessage> page(Long userId, Integer type, Integer isRead, int page, int size) {
        LambdaQueryWrapper<UserMessage> wrapper = new LambdaQueryWrapper<UserMessage>()
                .eq(UserMessage::getUserId, userId)
                .eq(type != null, UserMessage::getType, type)
                .eq(isRead != null, UserMessage::getIsRead, isRead)
                .orderByDesc(UserMessage::getId);
        Page<UserMessage> result = messageMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(m -> m.setTypeDesc(typeDesc(m.getType())));
        return PageResult.of(result);
    }

    @Override
    public Map<String, Object> unreadCount(Long userId) {
        Map<String, Object> map = new HashMap<>();
        map.put("total", countUnread(userId, null));
        map.put("order", countUnread(userId, TYPE_ORDER));
        map.put("system", countUnread(userId, TYPE_SYSTEM));
        map.put("afterSale", countUnread(userId, TYPE_AFTER_SALE));
        return map;
    }

    private long countUnread(Long userId, Integer type) {
        return messageMapper.selectCount(new LambdaQueryWrapper<UserMessage>()
                .eq(UserMessage::getUserId, userId)
                .eq(UserMessage::getIsRead, 0)
                .eq(type != null, UserMessage::getType, type));
    }

    @Override
    public void markRead(Long userId, Long messageId) {
        UserMessage message = messageMapper.selectById(messageId);
        if (message == null || !userId.equals(message.getUserId())) {
            throw BizException.of(ErrorCode.DATA_NOT_OWNED, "消息不存在");
        }
        if (Integer.valueOf(1).equals(message.getIsRead())) {
            return;
        }
        messageMapper.update(null, new LambdaUpdateWrapper<UserMessage>()
                .eq(UserMessage::getId, messageId)
                .set(UserMessage::getIsRead, 1)
                .set(UserMessage::getReadAt, LocalDateTime.now()));
    }

    @Override
    public int markAllRead(Long userId) {
        return messageMapper.update(null, new LambdaUpdateWrapper<UserMessage>()
                .eq(UserMessage::getUserId, userId)
                .eq(UserMessage::getIsRead, 0)
                .set(UserMessage::getIsRead, 1)
                .set(UserMessage::getReadAt, LocalDateTime.now()));
    }

    @Override
    public void remove(Long userId, Long messageId) {
        UserMessage message = messageMapper.selectById(messageId);
        if (message == null || !userId.equals(message.getUserId())) {
            throw BizException.of(ErrorCode.DATA_NOT_OWNED, "消息不存在");
        }
        messageMapper.deleteById(messageId);
    }

    @Override
    public void send(Long userId, Integer type, String title, String content,
                     Long orderId, String orderNo, Long bizId) {
        if (userId == null) {
            return;
        }
        try {
            UserMessage message = new UserMessage();
            message.setUserId(userId);
            message.setType(type == null ? TYPE_ORDER : type);
            message.setTitle(title);
            message.setContent(content);
            message.setOrderId(orderId);
            message.setOrderNo(orderNo);
            message.setBizId(bizId);
            message.setIsRead(0);
            messageMapper.insert(message);
        } catch (Exception e) {
            // 消息属于旁路能力，写入失败不能影响主业务
            log.warn("写用户消息失败：{}", e.getMessage());
        }
    }

    @Override
    public void broadcast(Long operatorId, String title, String content) {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, CommonConstants.USER_ENABLED)
                .eq(SysUser::getRole, "CONSUMER")
                .select(SysUser::getId));
        for (SysUser user : users) {
            send(user.getId(), TYPE_SYSTEM, title, content, null, null, operatorId);
        }
        log.info("公告已广播给 {} 位用户：{}", users.size(), title);
    }

    private String typeDesc(Integer type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case TYPE_ORDER:
                return "订单消息";
            case TYPE_SYSTEM:
                return "系统公告";
            case TYPE_AFTER_SALE:
                return "售后消息";
            default:
                return "消息";
        }
    }
}
