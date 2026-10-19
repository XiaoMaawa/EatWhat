package com.eatwhat.modules.content.dto;

import com.eatwhat.modules.content.entity.Announcement;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 公告发布/编辑请求体。
 */
public class AnnouncementRequest {

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 100, message = "标题不能超过 100 个字符")
    private String title;

    @NotBlank(message = "公告正文不能为空")
    private String content;

    private Integer type;

    private String targetRole;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Integer status;

    public Announcement toEntity() {
        Announcement entity = new Announcement();
        entity.setTitle(title);
        entity.setContent(content);
        entity.setType(type);
        entity.setTargetRole(targetRole);
        entity.setStartTime(startTime);
        entity.setEndTime(endTime);
        entity.setStatus(status);
        return entity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
