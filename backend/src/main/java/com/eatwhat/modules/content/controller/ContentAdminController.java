package com.eatwhat.modules.content.controller;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.common.result.R;
import com.eatwhat.common.utils.UserContext;
import com.eatwhat.modules.content.dto.AnnouncementRequest;
import com.eatwhat.modules.content.entity.Announcement;
import com.eatwhat.modules.content.entity.SensitiveWord;
import com.eatwhat.modules.content.entity.SysConfig;
import com.eatwhat.modules.content.entity.SysDict;
import com.eatwhat.modules.content.service.AnnouncementService;
import com.eatwhat.modules.content.service.SensitiveWordService;
import com.eatwhat.modules.content.service.SysConfigService;
import com.eatwhat.modules.content.service.SysDictService;
import com.eatwhat.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 内容管理接口：公告、敏感词、平台参数。
 */
@Tag(name = "内容管理")
@RestController
@RequestMapping("/api/v1/admin")
@RequireRole("ADMIN")
public class ContentAdminController {

    private final AnnouncementService announcementService;
    private final SensitiveWordService sensitiveWordService;
    private final SysConfigService sysConfigService;
    private final SysDictService sysDictService;

    public ContentAdminController(AnnouncementService announcementService,
                                  SensitiveWordService sensitiveWordService,
                                  SysConfigService sysConfigService,
                                  SysDictService sysDictService) {
        this.announcementService = announcementService;
        this.sensitiveWordService = sensitiveWordService;
        this.sysConfigService = sysConfigService;
        this.sysDictService = sysDictService;
    }

    // ==================== 公告 ====================

    @Operation(summary = "公告列表")
    @GetMapping("/announcements")
    public R<PageResult<Announcement>> announcements(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return R.ok(announcementService.pageAll(keyword, status, page, size));
    }

    @Operation(summary = "发布公告")
    @PostMapping("/announcements")
    public R<Announcement> createAnnouncement(@Valid @RequestBody AnnouncementRequest request) {
        return R.ok(announcementService.create(UserContext.userId(), request.toEntity()));
    }

    @Operation(summary = "编辑公告")
    @PutMapping("/announcements/{id}")
    public R<Announcement> updateAnnouncement(@PathVariable Long id,
                                              @Valid @RequestBody AnnouncementRequest request) {
        return R.ok(announcementService.update(id, request.toEntity()));
    }

    @Operation(summary = "上下线公告")
    @PatchMapping("/announcements/{id}/status")
    public R<Void> changeAnnouncementStatus(@PathVariable Long id,
                                            @RequestBody Map<String, Integer> body) {
        announcementService.changeStatus(id, body == null ? null : body.get("status"));
        return R.ok();
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/announcements/{id}")
    public R<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.delete(id);
        return R.ok();
    }

    // ==================== 敏感词 ====================

    @Operation(summary = "敏感词列表")
    @GetMapping("/sensitive-words")
    public R<PageResult<SensitiveWord>> sensitiveWords(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Integer status,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return R.ok(sensitiveWordService.pageAll(keyword, status, page, size));
    }

    @Operation(summary = "新增敏感词")
    @PostMapping("/sensitive-words")
    public R<SensitiveWord> createSensitiveWord(@RequestBody Map<String, String> body) {
        String word = body == null ? null : body.get("word");
        String category = body == null ? null : body.get("category");
        return R.ok(sensitiveWordService.create(word, category));
    }

    @Operation(summary = "修改敏感词")
    @PutMapping("/sensitive-words/{id}")
    public R<SensitiveWord> updateSensitiveWord(@PathVariable Long id,
                                                @RequestBody Map<String, String> body) {
        String word = body == null ? null : body.get("word");
        String category = body == null ? null : body.get("category");
        Integer status = null;
        if (body != null && body.get("status") != null) {
            try {
                status = Integer.valueOf(body.get("status"));
            } catch (NumberFormatException ignored) {
                status = null;
            }
        }
        return R.ok(sensitiveWordService.update(id, word, category, status));
    }

    @Operation(summary = "删除敏感词")
    @DeleteMapping("/sensitive-words/{id}")
    public R<Void> deleteSensitiveWord(@PathVariable Long id) {
        sensitiveWordService.delete(id);
        return R.ok();
    }

    @Operation(summary = "批量导入敏感词")
    @PostMapping("/sensitive-words/import")
    public R<Map<String, Object>> importSensitiveWords(@RequestBody Map<String, Object> body) {
        List<String> words = new java.util.ArrayList<>();
        Object raw = body == null ? null : body.get("words");
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o != null) {
                    words.add(String.valueOf(o));
                }
            }
        }
        String category = body == null || body.get("category") == null
                ? null : String.valueOf(body.get("category"));
        int count = sensitiveWordService.batchImport(words, category);
        return R.ok(Map.of("imported", count));
    }

    @Operation(summary = "文本敏感词检测")
    @PostMapping("/sensitive-words/check")
    public R<Map<String, Object>> checkSensitiveWords(@RequestBody Map<String, String> body) {
        String text = body == null ? null : body.get("text");
        String category = body == null ? null : body.get("category");
        List<String> hitWords = sensitiveWordService.hits(text, category);
        return R.ok(Map.of("hit", !hitWords.isEmpty(), "words", hitWords));
    }

    // ==================== 平台参数 ====================

    @Operation(summary = "参数列表")
    @GetMapping("/configs")
    public R<PageResult<SysConfig>> configs(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String group,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "50") int size) {
        return R.ok(sysConfigService.pageAll(keyword, group, page, size));
    }

    @Operation(summary = "新增参数")
    @PostMapping("/configs")
    public R<SysConfig> createConfig(@RequestBody SysConfig config) {
        return R.ok(sysConfigService.create(config));
    }

    @Operation(summary = "修改参数")
    @PutMapping("/configs/{id}")
    public R<SysConfig> updateConfig(@PathVariable Long id,
                                     @RequestBody SysConfig config) {
        return R.ok(sysConfigService.update(id, config.getConfigValue(),
                config.getConfigName(), config.getRemark()));
    }

    @Operation(summary = "删除参数")
    @DeleteMapping("/configs/{id}")
    public R<Void> deleteConfig(@PathVariable Long id) {
        sysConfigService.delete(id);
        return R.ok();
    }

    // ==================== 数据字典 ====================

    @Operation(summary = "字典项列表")
    @GetMapping("/dicts")
    public R<PageResult<SysDict>> dicts(@RequestParam(required = false) String dictType,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Integer status,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return R.ok(sysDictService.pageAll(dictType, keyword, status, page, size));
    }

    @Operation(summary = "字典类型列表")
    @GetMapping("/dicts/types")
    public R<List<String>> dictTypes() {
        return R.ok(sysDictService.listTypes());
    }

    @Operation(summary = "新增字典项")
    @PostMapping("/dicts")
    public R<SysDict> createDict(@RequestBody SysDict dict) {
        return R.ok(sysDictService.create(dict));
    }

    @Operation(summary = "修改字典项")
    @PutMapping("/dicts/{id}")
    public R<SysDict> updateDict(@PathVariable Long id, @RequestBody SysDict dict) {
        return R.ok(sysDictService.update(id, dict));
    }

    @Operation(summary = "删除字典项")
    @DeleteMapping("/dicts/{id}")
    public R<Void> deleteDict(@PathVariable Long id) {
        sysDictService.delete(id);
        return R.ok();
    }
}
