package com.eatwhat.modules.content.service;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.content.entity.Announcement;

import java.util.List;

/**
 * 平台公告服务。
 */
public interface AnnouncementService {

    /**
     * 管理端分页查询公告。
     */
    PageResult<Announcement> pageAll(String keyword, Integer status, int page, int size);

    /**
     * 端上查询当前有效公告。
     *
     * @param role ALL / CONSUMER / MERCHANT / RIDER，null 视为 ALL
     */
    List<Announcement> listActive(String role, int limit);

    /**
     * 新建公告。
     */
    Announcement create(Long publisherId, Announcement announcement);

    /**
     * 编辑公告。
     */
    Announcement update(Long id, Announcement announcement);

    /**
     * 上架 / 下架公告。
     */
    void changeStatus(Long id, Integer status);

    /**
     * 删除公告（逻辑删除）。
     */
    void delete(Long id);
}
