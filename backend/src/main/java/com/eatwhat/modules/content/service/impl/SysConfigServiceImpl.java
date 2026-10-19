package com.eatwhat.modules.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eatwhat.common.exception.BizException;
import com.eatwhat.common.exception.ErrorCode;
import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.content.entity.SysConfig;
import com.eatwhat.modules.content.mapper.SysConfigMapper;
import com.eatwhat.modules.content.service.SysConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台参数服务实现。
 * <p>
 * 参数读取频繁但写入很少，使用本地缓存，写操作后整体失效。
 */
@Service
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    /** key -> value 缓存 */
    private volatile Map<String, String> cache = null;

    public SysConfigServiceImpl(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    @Override
    public PageResult<SysConfig> pageAll(String keyword, String group, int page, int size) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(SysConfig::getConfigKey, kw)
                    .or().like(SysConfig::getConfigName, kw));
        }
        if (group != null && !group.isBlank()) {
            wrapper.eq(SysConfig::getConfigGroup, group.trim().toUpperCase());
        }
        wrapper.orderByAsc(SysConfig::getConfigGroup).orderByAsc(SysConfig::getId);
        Page<SysConfig> result = sysConfigMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result);
    }

    @Override
    public List<SysConfig> listByGroup(String group) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (group != null && !group.isBlank()) {
            wrapper.eq(SysConfig::getConfigGroup, group.trim().toUpperCase());
        }
        wrapper.orderByAsc(SysConfig::getId);
        return sysConfigMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysConfig create(SysConfig config) {
        if (config.getConfigKey() == null || config.getConfigKey().isBlank()) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "参数键不能为空");
        }
        if (config.getConfigValue() == null) {
            throw BizException.of(ErrorCode.PARAM_MISSING, "参数值不能为空");
        }
        String key = config.getConfigKey().trim();
        Long exists = sysConfigMapper.selectCount(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key));
        if (exists != null && exists > 0) {
            throw BizException.of(ErrorCode.DATA_CONFLICT, "该参数键已存在");
        }
        config.setId(null);
        config.setConfigKey(key);
        if (config.getConfigName() == null || config.getConfigName().isBlank()) {
            config.setConfigName(key);
        }
        if (config.getConfigGroup() == null || config.getConfigGroup().isBlank()) {
            config.setConfigGroup("PLATFORM");
        }
        sysConfigMapper.insert(config);
        cache = null;
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysConfig update(Long id, String value, String name, String remark) {
        SysConfig exist = sysConfigMapper.selectById(id);
        if (exist == null) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "参数不存在");
        }
        if (value != null) {
            exist.setConfigValue(value);
        }
        if (name != null && !name.isBlank()) {
            exist.setConfigName(name);
        }
        if (remark != null) {
            exist.setRemark(remark);
        }
        sysConfigMapper.updateById(exist);
        cache = null;
        return exist;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysConfig exist = sysConfigMapper.selectById(id);
        if (exist == null) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "参数不存在");
        }
        sysConfigMapper.deleteById(id);
        cache = null;
    }

    @Override
    public String getValue(String key, String defaultValue) {
        if (key == null || key.isBlank()) {
            return defaultValue;
        }
        String value = loadCache().get(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String value = getValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Map<String, String> loadCache() {
        Map<String, String> local = cache;
        if (local == null) {
            synchronized (this) {
                local = cache;
                if (local == null) {
                    List<SysConfig> list = sysConfigMapper.selectList(new LambdaQueryWrapper<>());
                    Map<String, String> map = new HashMap<>();
                    for (SysConfig c : list) {
                        map.put(c.getConfigKey(), c.getConfigValue());
                    }
                    cache = map;
                    local = map;
                }
            }
        }
        return local;
    }
}
