package com.eatwhat.modules.content.service;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.content.entity.SysConfig;

import java.util.List;

/**
 * 平台参数服务。
 */
public interface SysConfigService {

    /**
     * 管理端分页查询参数。
     */
    PageResult<SysConfig> pageAll(String keyword, String group, int page, int size);

    /**
     * 按分组列出参数。
     */
    List<SysConfig> listByGroup(String group);

    /**
     * 新增参数。
     */
    SysConfig create(SysConfig config);

    /**
     * 修改参数值。
     */
    SysConfig update(Long id, String value, String name, String remark);

    /**
     * 删除参数。
     */
    void delete(Long id);

    /**
     * 读取参数值，不存在时返回默认值。
     */
    String getValue(String key, String defaultValue);

    /**
     * 读取整型参数，解析失败返回默认值。
     */
    int getInt(String key, int defaultValue);
}
