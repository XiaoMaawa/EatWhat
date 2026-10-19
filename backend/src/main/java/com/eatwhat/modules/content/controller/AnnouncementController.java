package com.eatwhat.modules.content.controller;

import com.eatwhat.common.result.R;
import com.eatwhat.modules.content.entity.Announcement;
import com.eatwhat.modules.content.service.AnnouncementService;
import com.eatwhat.security.PublicApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公告查询接口（各端共用）。
 */
@Tag(name = "公告")
@RestController
@RequestMapping("/api/v1/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @Operation(summary = "查询当前有效公告")
    @PublicApi
    @GetMapping
    public R<List<Announcement>> list(@RequestParam(required = false) String role,
                                      @RequestParam(defaultValue = "5") int limit) {
        return R.ok(announcementService.listActive(role, limit));
    }

    @Operation(summary = "公告详情")
    @PublicApi
    @GetMapping("/{id}")
    public R<Announcement> detail(@PathVariable Long id) {
        List<Announcement> list = announcementService.listActive(null, 20);
        Announcement hit = list.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
        if (hit != null) {
            return R.ok(hit);
        }
        // 不在有效列表时回退到全量查询，保证历史公告仍可被读取
        return R.ok(announcementService.pageAll(null, null, 1, 200).getRecords().stream()
                .filter(a -> a.getId().equals(id)).findFirst().orElse(null));
    }
}
