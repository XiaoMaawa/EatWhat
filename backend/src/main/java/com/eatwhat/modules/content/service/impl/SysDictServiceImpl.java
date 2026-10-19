package com.eatwhat.modules.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.content.entity.SysDict;
import com.eatwhat.modules.content.mapper.SysDictMapper;
import com.eatwhat.modules.content.service.SysDictService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据字典服务实现。
 *
 * <p>字典项整体走内存缓存（默认 10 分钟过期），避免每次下拉框都打数据库；
 * 管理端写操作后立即失效，保证配置改动马上生效。</p>
 */
@Service
public class SysDictServiceImpl implements SysDictService {

    private static final Logger log = LoggerFactory.getLogger(SysDictServiceImpl.class);

    /** 缓存有效期：10 分钟 */
    private static final long CACHE_TTL_MILLIS = 10 * 60 * 1000L;

    private final SysDictMapper dictMapper;

    /** dictType → 缓存条目 */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public SysDictServiceImpl(SysDictMapper dictMapper) {
        this.dictMapper = dictMapper;
    }

    @Override
    public List<SysDict> listByType(String dictType) {
        if (dictType == null || dictType.isBlank()) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "字典类型不能为空");
        }
        String type = dictType.trim();
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get(type);
        if (entry != null && entry.expireAt > now) {
            return entry.items;
        }
        List<SysDict> items = loadByType(type);
        cache.put(type, new CacheEntry(items, now + CACHE_TTL_MILLIS));
        return items;
    }

    @Override
    public Map<String, List<SysDict>> mapByTypes(List<String> dictTypes) {
        Map<String, List<SysDict>> result = new LinkedHashMap<>();
        if (dictTypes == null || dictTypes.isEmpty()) {
            return result;
        }
        for (String type : dictTypes) {
            if (type == null || type.isBlank()) {
                continue;
            }
            result.put(type.trim(), listByType(type));
        }
        return result;
    }

    @Override
    public PageResult<SysDict> pageAll(String dictType, String keyword, Integer status, int page, int size) {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<SysDict>()
                .eq(dictType != null && !dictType.isBlank(), SysDict::getDictType,
                        dictType == null ? null : dictType.trim())
                .eq(status != null, SysDict::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysDict::getDictLabel, keyword.trim())
                        .or().like(SysDict::getDictValue, keyword.trim()))
                .orderByAsc(SysDict::getDictType)
                .orderByAsc(SysDict::getSort)
                .orderByAsc(SysDict::getId);
        return PageResult.of(dictMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Override
    public List<String> listTypes() {
        List<SysDict> all = dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .select(SysDict::getDictType)
                .groupBy(SysDict::getDictType)
                .orderByAsc(SysDict::getDictType));
        List<String> types = new ArrayList<>(all.size());
        for (SysDict dict : all) {
            if (dict.getDictType() != null) {
                types.add(dict.getDictType());
            }
        }
        return types;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDict create(SysDict dict) {
        validate(dict);
        Long exists = dictMapper.selectCount(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, dict.getDictType().trim())
                .eq(SysDict::getDictValue, dict.getDictValue().trim()));
        if (exists != null && exists > 0) {
            throw BizException.of(ErrorCode.DATA_CONFLICT, "该字典类型下已存在相同字典值");
        }
        dict.setId(null);
        dict.setDictType(dict.getDictType().trim());
        dict.setDictValue(dict.getDictValue().trim());
        dict.setDictLabel(dict.getDictLabel().trim());
        dict.setSort(dict.getSort() == null ? 0 : dict.getSort());
        dict.setStatus(dict.getStatus() == null ? 1 : dict.getStatus());
        dictMapper.insert(dict);
        evictCache();
        log.info("新增字典项 type={} value={}", dict.getDictType(), dict.getDictValue());
        return dict;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDict update(Long id, SysDict dict) {
        SysDict exists = requireDict(id);
        if (dict.getDictType() != null && !dict.getDictType().isBlank()
                && !dict.getDictType().trim().equals(exists.getDictType())) {
            // 类型变更会破坏其他引用，直接拒绝
            throw BizException.of(ErrorCode.PARAM_INVALID, "字典类型不允许修改，请新增字典项");
        }
        if (dict.getDictValue() != null && !dict.getDictValue().isBlank()) {
            String value = dict.getDictValue().trim();
            Long conflict = dictMapper.selectCount(new LambdaQueryWrapper<SysDict>()
                    .eq(SysDict::getDictType, exists.getDictType())
                    .eq(SysDict::getDictValue, value)
                    .ne(SysDict::getId, id));
            if (conflict != null && conflict > 0) {
                throw BizException.of(ErrorCode.DATA_CONFLICT, "该字典类型下已存在相同字典值");
            }
            exists.setDictValue(value);
        }
        if (dict.getDictLabel() != null && !dict.getDictLabel().isBlank()) {
            exists.setDictLabel(dict.getDictLabel().trim());
        }
        if (dict.getSort() != null) {
            exists.setSort(dict.getSort());
        }
        if (dict.getStatus() != null) {
            if (dict.getStatus() != 0 && dict.getStatus() != 1) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "状态只能为 0（停用）或 1（启用）");
            }
            exists.setStatus(dict.getStatus());
        }
        if (dict.getRemark() != null) {
            exists.setRemark(dict.getRemark());
        }
        dictMapper.updateById(exists);
        evictCache();
        return exists;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireDict(id);
        dictMapper.deleteById(id);
        evictCache();
        log.info("删除字典项 id={}", id);
    }

    @Override
    public void evictCache() {
        cache.clear();
    }

    private SysDict requireDict(Long id) {
        SysDict dict = id == null ? null : dictMapper.selectById(id);
        if (dict == null) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "字典项不存在");
        }
        return dict;
    }

    private List<SysDict> loadByType(String dictType) {
        List<SysDict> items = dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, dictType)
                .eq(SysDict::getStatus, 1)
                .orderByAsc(SysDict::getSort)
                .orderByAsc(SysDict::getId));
        return items == null ? Collections.emptyList() : Collections.unmodifiableList(items);
    }

    private void validate(SysDict dict) {
        if (dict == null) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "字典项不能为空");
        }
        if (dict.getDictType() == null || dict.getDictType().isBlank()) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "字典类型不能为空");
        }
        if (dict.getDictType().trim().length() > 64) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "字典类型不能超过 64 个字符");
        }
        if (dict.getDictLabel() == null || dict.getDictLabel().isBlank()) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "字典标签不能为空");
        }
        if (dict.getDictLabel().trim().length() > 100) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "字典标签不能超过 100 个字符");
        }
        if (dict.getDictValue() == null || dict.getDictValue().isBlank()) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "字典值不能为空");
        }
        if (dict.getDictValue().trim().length() > 64) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "字典值不能超过 64 个字符");
        }
    }

    /**
     * 缓存条目，items 不可变因此可安全共享。
     */
    private static final class CacheEntry {
        private final List<SysDict> items;
        private final long expireAt;

        private CacheEntry(List<SysDict> items, long expireAt) {
            this.items = items;
            this.expireAt = expireAt;
        }
    }
}
