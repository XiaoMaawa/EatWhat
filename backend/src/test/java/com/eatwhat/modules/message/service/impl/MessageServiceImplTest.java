package com.eatwhat.modules.message.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatwhat.common.constant.CommonConstants;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.auth.entity.SysUser;
import com.eatwhat.modules.auth.mapper.SysUserMapper;
import com.eatwhat.modules.message.entity.UserMessage;
import com.eatwhat.modules.message.mapper.UserMessageMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 消息服务单元测试。
 * 覆盖未读统计、归属校验、已读标记、消息发送与公告广播。
 */
@DisplayName("消息服务")
class MessageServiceImplTest {

    private static final Long USER_ID = 2L;
    private static final Long MESSAGE_ID = 100L;

    private UserMessageMapper messageMapper;
    private SysUserMapper userMapper;
    private MessageServiceImpl service;

    @BeforeAll
    static void initMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.eatwhat.modules.message.mapper.UserMessageMapper");
        TableInfoHelper.initTableInfo(assistant, UserMessage.class);
    }

    @BeforeEach
    void setUp() {
        messageMapper = mock(UserMessageMapper.class);
        userMapper = mock(SysUserMapper.class);
        service = new MessageServiceImpl(messageMapper, userMapper);
    }

    private UserMessage message(Long id, Long userId, int type, int isRead) {
        UserMessage m = new UserMessage();
        m.setId(id);
        m.setUserId(userId);
        m.setType(type);
        m.setIsRead(isRead);
        m.setTitle("订单已接单");
        m.setContent("商家已接下你的订单");
        return m;
    }

    @Test
    @DisplayName("未读统计：汇总总未读与各类型未读")
    void unreadCount() {
        when(messageMapper.selectCount(any())).thenReturn(3L);

        Map<String, Object> result = service.unreadCount(USER_ID);

        // 总未读 + 订单 + 系统 + 售后，共调用 4 次
        assertThat(result.get("total")).isEqualTo(3L);
        assertThat(result.get("order")).isEqualTo(3L);
        assertThat(result.get("system")).isEqualTo(3L);
        assertThat(result.get("afterSale")).isEqualTo(3L);
        verify(messageMapper, times(4)).selectCount(any());
    }

    @Test
    @DisplayName("消息分页：列表回填类型中文描述")
    void pageFillsTypeDesc() {
        Page<UserMessage> page = new Page<>(1, 10);
        List<UserMessage> records = new ArrayList<>();
        records.add(message(1L, USER_ID, 1, 0));
        records.add(message(2L, USER_ID, 2, 1));
        records.add(message(3L, USER_ID, 3, 0));
        page.setRecords(records);
        page.setTotal(3);
        when(messageMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<UserMessage> result = service.page(USER_ID, null, null, 1, 10);

        assertThat(result.getTotal()).isEqualTo(3);
        assertThat(result.getRecords()).extracting(UserMessage::getTypeDesc)
                .containsExactly("订单消息", "系统公告", "售后消息");
    }

    @Test
    @DisplayName("标记已读：他人消息抛无权操作")
    void markReadRejectsOtherUser() {
        when(messageMapper.selectById(MESSAGE_ID)).thenReturn(message(MESSAGE_ID, 99L, 1, 0));

        assertThatThrownBy(() -> service.markRead(USER_ID, MESSAGE_ID))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(ErrorCode.DATA_NOT_OWNED.getCode());
    }

    @Test
    @DisplayName("标记已读：消息不存在抛无权操作")
    void markReadRejectsMissing() {
        when(messageMapper.selectById(MESSAGE_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.markRead(USER_ID, MESSAGE_ID))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("标记已读：已读消息重复标记不再更新")
    void markReadIsIdempotent() {
        when(messageMapper.selectById(MESSAGE_ID)).thenReturn(message(MESSAGE_ID, USER_ID, 1, 1));

        service.markRead(USER_ID, MESSAGE_ID);

        verify(messageMapper, times(0)).update(isNull(), any());
    }

    @Test
    @DisplayName("标记已读：未读消息写入已读状态")
    void markReadUpdatesUnread() {
        when(messageMapper.selectById(MESSAGE_ID)).thenReturn(message(MESSAGE_ID, USER_ID, 1, 0));

        service.markRead(USER_ID, MESSAGE_ID);

        verify(messageMapper, times(1)).update(isNull(), any());
    }

    @Test
    @DisplayName("删除消息：归属校验通过后逻辑删除")
    void removeOwnMessage() {
        when(messageMapper.selectById(MESSAGE_ID)).thenReturn(message(MESSAGE_ID, USER_ID, 1, 0));

        service.remove(USER_ID, MESSAGE_ID);

        verify(messageMapper, times(1)).deleteById(MESSAGE_ID);
    }

    @Test
    @DisplayName("删除消息：他人消息被拒绝")
    void removeRejectsOtherUser() {
        when(messageMapper.selectById(MESSAGE_ID)).thenReturn(message(MESSAGE_ID, 99L, 1, 0));

        assertThatThrownBy(() -> service.remove(USER_ID, MESSAGE_ID))
                .isInstanceOf(BizException.class);

        verify(messageMapper, times(0)).deleteById(any(Long.class));
    }

    @Test
    @DisplayName("发送消息：写入未读记录并带业务关联")
    void sendWritesMessage() {
        service.send(USER_ID, 3, "商家已同意售后", "卖家同意退款", 11L, "EW001", 22L);

        ArgumentCaptor<UserMessage> captor = ArgumentCaptor.forClass(UserMessage.class);
        verify(messageMapper).insert(captor.capture());
        UserMessage saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getType()).isEqualTo(3);
        assertThat(saved.getIsRead()).isEqualTo(0);
        assertThat(saved.getOrderId()).isEqualTo(11L);
        assertThat(saved.getOrderNo()).isEqualTo("EW001");
        assertThat(saved.getBizId()).isEqualTo(22L);
    }

    @Test
    @DisplayName("发送消息：接收人为空时直接忽略")
    void sendIgnoresNullUser() {
        service.send(null, 1, "标题", "内容", null, null, null);

        verify(messageMapper, times(0)).insert(any(UserMessage.class));
    }

    @Test
    @DisplayName("发送消息：写库异常被吞掉，不影响主业务")
    void sendSwallowsException() {
        when(messageMapper.insert(any(UserMessage.class))).thenThrow(new RuntimeException("db down"));

        service.send(USER_ID, 1, "标题", "内容", null, null, null);

        // 没有抛出异常即通过
        verify(messageMapper, times(1)).insert(any(UserMessage.class));
    }

    @Test
    @DisplayName("广播公告：发送给所有正常状态的消费者")
    void broadcastToAllConsumers() {
        SysUser u1 = new SysUser();
        u1.setId(2L);
        SysUser u2 = new SysUser();
        u2.setId(3L);
        when(userMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(new ArrayList<>(List.of(u1, u2)));

        service.broadcast(1L, "系统维护", "今晚维护");

        ArgumentCaptor<UserMessage> captor = ArgumentCaptor.forClass(UserMessage.class);
        verify(messageMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(UserMessage::getUserId)
                .containsExactly(2L, 3L);
        assertThat(captor.getAllValues()).extracting(UserMessage::getType)
                .containsOnly(2);
    }

    @Test
    @DisplayName("广播公告：无用户时不写入任何消息")
    void broadcastWithNoUsers() {
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

        service.broadcast(1L, "系统维护", "今晚维护");

        verify(messageMapper, times(0)).insert(any(UserMessage.class));
    }

    @Test
    @DisplayName("常量约定：消息类型与用户启用状态")
    void constants() {
        assertThat(MessageServiceImpl.TYPE_ORDER).isEqualTo(1);
        assertThat(MessageServiceImpl.TYPE_SYSTEM).isEqualTo(2);
        assertThat(MessageServiceImpl.TYPE_AFTER_SALE).isEqualTo(3);
        assertThat(CommonConstants.USER_ENABLED).isEqualTo(1);
    }
}
