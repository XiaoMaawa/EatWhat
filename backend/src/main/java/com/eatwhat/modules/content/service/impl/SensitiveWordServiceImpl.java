package com.eatwhat.modules.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.content.entity.SensitiveWord;
import com.eatwhat.modules.content.mapper.SensitiveWordMapper;
import com.eatwhat.modules.content.service.SensitiveWordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 敏感词服务实现。
 * <p>
 * 词库规模在几千条量级，直接使用简单包含匹配并按需缓存，避免引入额外组件。
 */
@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private static final int STATUS_ENABLED = 1;
    private static final int MAX_WORD_LENGTH = 50;

    private final SensitiveWordMapper sensitiveWordMapper;

    /** 词库缓存：写操作后置空，下次查询重建 */
    private volatile List<SensitiveWord> cache = null;

    public SensitiveWordServiceImpl(SensitiveWordMapper sensitiveWordMapper) {
        this.sensitiveWordMapper = sensitiveWordMapper;
    }

    @Override
    public PageResult<SensitiveWord> pageAll(String keyword, Integer status, int page, int size) {
        LambdaQueryWrapper<SensitiveWord> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SensitiveWord::getWord, keyword.trim());
        }
        if (status != null) {
            wrapper.eq(SensitiveWord::getStatus, status);
        }
        wrapper.orderByDesc(SensitiveWord::getId);
        Page<SensitiveWord> result = sensitiveWordMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SensitiveWord create(String word, String category) {
        String trimmed = validateWord(word);
        Long exists = sensitiveWordMapper.selectCount(new LambdaQueryWrapper<SensitiveWord>()
                .eq(SensitiveWord::getWord, trimmed));
        if (exists != null && exists > 0) {
            throw BizException.of(ErrorCode.DATA_CONFLICT, "该敏感词已存在");
        }
        SensitiveWord entity = new SensitiveWord();
        entity.setWord(trimmed);
        entity.setCategory(normalizeCategory(category));
        entity.setStatus(STATUS_ENABLED);
        sensitiveWordMapper.insert(entity);
        cache = null;
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SensitiveWord update(Long id, String word, String category, Integer status) {
        SensitiveWord exist = sensitiveWordMapper.selectById(id);
        if (exist == null) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "敏感词不存在");
        }
        if (word != null && !word.isBlank()) {
            String trimmed = validateWord(word);
            Long dup = sensitiveWordMapper.selectCount(new LambdaQueryWrapper<SensitiveWord>()
                    .eq(SensitiveWord::getWord, trimmed)
                    .ne(SensitiveWord::getId, id));
            if (dup != null && dup > 0) {
                throw BizException.of(ErrorCode.DATA_CONFLICT, "该敏感词已存在");
            }
            exist.setWord(trimmed);
        }
        if (category != null && !category.isBlank()) {
            exist.setCategory(normalizeCategory(category));
        }
        if (status != null) {
            if (status != 0 && status != STATUS_ENABLED) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "状态只能为 0（停用）或 1（启用）");
            }
            exist.setStatus(status);
        }
        sensitiveWordMapper.updateById(exist);
        cache = null;
        return exist;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SensitiveWord exist = sensitiveWordMapper.selectById(id);
        if (exist == null) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "敏感词不存在");
        }
        sensitiveWordMapper.deleteById(id);
        cache = null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchImport(List<String> words, String category) {
        if (words == null || words.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "导入词条不能为空");
        }
        String cat = normalizeCategory(category);
        int success = 0;
        for (String raw : words) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String trimmed = raw.trim();
            if (trimmed.length() > MAX_WORD_LENGTH) {
                continue;
            }
            Long exists = sensitiveWordMapper.selectCount(new LambdaQueryWrapper<SensitiveWord>()
                    .eq(SensitiveWord::getWord, trimmed));
            if (exists != null && exists > 0) {
                continue;
            }
            SensitiveWord entity = new SensitiveWord();
            entity.setWord(trimmed);
            entity.setCategory(cat);
            entity.setStatus(STATUS_ENABLED);
            sensitiveWordMapper.insert(entity);
            success++;
        }
        cache = null;
        return success;
    }

    @Override
    public List<String> hits(String text, String category) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        String cat = category == null || category.isBlank() ? "ALL" : category;
        List<SensitiveWord> words = loadEnabled();
        if (words.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> hitWords = new ArrayList<>();
        for (SensitiveWord w : words) {
            boolean sameScope = "ALL".equals(w.getCategory()) || cat.equals(w.getCategory());
            if (!sameScope) {
                continue;
            }
            if (text.contains(w.getWord())) {
                hitWords.add(w.getWord());
            }
        }
        return hitWords;
    }

    private List<SensitiveWord> loadEnabled() {
        List<SensitiveWord> local = cache;
        if (local == null) {
            synchronized (this) {
                local = cache;
                if (local == null) {
                    local = sensitiveWordMapper.selectList(new LambdaQueryWrapper<SensitiveWord>()
                            .eq(SensitiveWord::getStatus, STATUS_ENABLED));
                    cache = local;
                }
            }
        }
        return local;
    }

    private String validateWord(String word) {
        if (word == null || word.isBlank()) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "敏感词不能为空");
        }
        String trimmed = word.trim();
        if (trimmed.length() > MAX_WORD_LENGTH) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "敏感词长度不能超过 50 个字符");
        }
        return trimmed;
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "ALL";
        }
        String cat = category.trim().toUpperCase();
        if (!"ALL".equals(cat) && !"REVIEW".equals(cat)
                && !"REMARK".equals(cat) && !"NICKNAME".equals(cat)) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "适用范围只能为 ALL/REVIEW/REMARK/NICKNAME");
        }
        return cat;
    }
}
