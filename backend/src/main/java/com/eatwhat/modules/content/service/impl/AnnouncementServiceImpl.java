package com.eatwhat.modules.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.content.entity.Announcement;
import com.eatwhat.modules.content.mapper.AnnouncementMapper;
import com.eatwhat.modules.content.service.AnnouncementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台公告服务实现。
 */
@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    private static final int STATUS_OFFLINE = 0;
    private static final int STATUS_PUBLISHED = 1;

    private final AnnouncementMapper announcementMapper;

    public AnnouncementServiceImpl(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    @Override
    public PageResult<Announcement> pageAll(String keyword, Integer status, int page, int size) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Announcement::getTitle, keyword.trim());
        }
        if (status != null) {
            wrapper.eq(Announcement::getStatus, status);
        }
        wrapper.orderByDesc(Announcement::getId);
        Page<Announcement> result = announcementMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result);
    }

    @Override
    public List<Announcement> listActive(String role, int limit) {
        LocalDateTime now = LocalDateTime.now();
        int max = limit <= 0 ? 5 : Math.min(limit, 20);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, STATUS_PUBLISHED)
                .le(Announcement::getStartTime, now)
                .and(w -> w.isNull(Announcement::getEndTime).or().ge(Announcement::getEndTime, now));
        if (role != null && !role.isBlank()) {
            wrapper.in(Announcement::getTargetRole, "ALL", role);
        }
        wrapper.orderByDesc(Announcement::getStartTime).last("LIMIT " + max);
        return announcementMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Announcement create(Long publisherId, Announcement announcement) {
        validate(announcement);
        announcement.setId(null);
        announcement.setPublisherId(publisherId);
        if (announcement.getStatus() == null) {
            announcement.setStatus(STATUS_PUBLISHED);
        }
        if (announcement.getStartTime() == null) {
            announcement.setStartTime(LocalDateTime.now());
        }
        announcementMapper.insert(announcement);
        return announcement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Announcement update(Long id, Announcement announcement) {
        Announcement exist = announcementMapper.selectById(id);
        if (exist == null) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "公告不存在");
        }
        validate(announcement);
        exist.setTitle(announcement.getTitle());
        exist.setContent(announcement.getContent());
        exist.setType(announcement.getType());
        exist.setTargetRole(announcement.getTargetRole());
        exist.setStartTime(announcement.getStartTime());
        exist.setEndTime(announcement.getEndTime());
        if (announcement.getStatus() != null) {
            exist.setStatus(announcement.getStatus());
        }
        announcementMapper.updateById(exist);
        return exist;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != STATUS_OFFLINE && status != STATUS_PUBLISHED)) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "公告状态只能为 0（下线）或 1（发布）");
        }
        Announcement exist = announcementMapper.selectById(id);
        if (exist == null) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "公告不存在");
        }
        exist.setStatus(status);
        announcementMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Announcement exist = announcementMapper.selectById(id);
        if (exist == null) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "公告不存在");
        }
        announcementMapper.deleteById(id);
    }

    private void validate(Announcement announcement) {
        if (announcement.getTitle() == null || announcement.getTitle().isBlank()) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "公告标题不能为空");
        }
        if (announcement.getContent() == null || announcement.getContent().isBlank()) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "公告正文不能为空");
        }
        if (announcement.getType() == null) {
            announcement.setType(1);
        } else if (announcement.getType() < 1 || announcement.getType() > 3) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "公告类型只能为 1/2/3");
        }
        if (announcement.getTargetRole() == null || announcement.getTargetRole().isBlank()) {
            announcement.setTargetRole("ALL");
        }
        String role = announcement.getTargetRole();
        if (!"ALL".equals(role) && !"CONSUMER".equals(role)
                && !"MERCHANT".equals(role) && !"RIDER".equals(role)) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "可见范围只能为 ALL/CONSUMER/MERCHANT/RIDER");
        }
        if (announcement.getStartTime() != null && announcement.getEndTime() != null
                && announcement.getEndTime().isBefore(announcement.getStartTime())) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "失效时间不能早于生效时间");
        }
    }
}
