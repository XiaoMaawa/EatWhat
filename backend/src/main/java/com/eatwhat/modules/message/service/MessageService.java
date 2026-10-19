package com.eatwhat.modules.message.service;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.message.entity.UserMessage;

import java.util.Map;

/**
 * 用户消息服务：订单节点消息、系统公告、已读标记与未读计数。
 */
public interface MessageService {

    /**
     * 分页查询当前用户消息。
     *
     * @param userId  用户ID
     * @param type    消息类型，null 表示全部
     * @param isRead  0 未读 / 1 已读，null 表示全部
     * @param page    页码
     * @param size    每页条数
     */
    PageResult<UserMessage> page(Long userId, Integer type, Integer isRead, int page, int size);

    /**
     * 未读消息统计。
     *
     * @return total 总未读，order 订单消息未读，system 系统公告未读，afterSale 售后消息未读
     */
    Map<String, Object> unreadCount(Long userId);

    /**
     * 标记单条消息为已读。
     */
    void markRead(Long userId, Long messageId);

    /**
     * 标记全部消息为已读。
     *
     * @return 实际标记条数
     */
    int markAllRead(Long userId);

    /**
     * 删除单条消息（逻辑删除）。
     */
    void remove(Long userId, Long messageId);

    /**
     * 发送一条消息（内部调用，失败不影响主流程）。
     */
    void send(Long userId, Integer type, String title, String content, Long orderId, String orderNo, Long bizId);

    /**
     * 广播系统公告给所有启用用户（管理员发布公告时使用）。
     */
    void broadcast(Long operatorId, String title, String content);
}
