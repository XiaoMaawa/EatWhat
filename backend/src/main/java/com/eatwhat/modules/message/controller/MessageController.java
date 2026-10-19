package com.eatwhat.modules.message.controller;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.common.result.R;
import com.eatwhat.common.utils.UserContext;
import com.eatwhat.modules.message.entity.UserMessage;
import com.eatwhat.modules.message.service.MessageService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 消息中心接口：订单消息、系统公告、已读标记与未读计数。
 */
@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /** 消息列表（可按类型/已读状态筛选） */
    @GetMapping
    public R<PageResult<UserMessage>> page(@RequestParam(required = false) Integer type,
                                           @RequestParam(required = false) Integer isRead,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return R.ok(messageService.page(UserContext.userId(), type, isRead, page, size));
    }

    /** 未读消息计数 */
    @GetMapping("/unread-count")
    public R<Map<String, Object>> unreadCount() {
        return R.ok(messageService.unreadCount(UserContext.userId()));
    }

    /** 标记单条已读 */
    @PutMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        messageService.markRead(UserContext.userId(), id);
        return R.ok();
    }

    /** 全部标为已读 */
    @PostMapping("/read-all")
    public R<Map<String, Object>> markAllRead() {
        int count = messageService.markAllRead(UserContext.userId());
        return R.ok(Map.of("count", count));
    }

    /** 删除消息 */
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        messageService.remove(UserContext.userId(), id);
        return R.ok();
    }
}
