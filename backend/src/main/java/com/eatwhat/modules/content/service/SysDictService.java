package com.eatwhat.modules.content.service;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.content.entity.SysDict;

import java.util.List;
import java.util.Map;

/**
 * 数据字典服务。
 */
public interface SysDictService {

    /**
     * 按类型查询启用中的字典项（按 sort 升序），结果缓存 10 分钟。
     *
     * @param dictType 字典类型
     * @return 字典项列表
     */
    List<SysDict> listByType(String dictType);

    /**
     * 一次性查询多个类型的字典项，便于前端初始化。
     *
     * @param dictTypes 字典类型列表
     * @return 类型 → 字典项列表
     */
    Map<String, List<SysDict>> mapByTypes(List<String> dictTypes);

    /**
     * 分页查询字典项。
     */
    PageResult<SysDict> pageAll(String dictType, String keyword, Integer status, int page, int size);

    /**
     * 查询全部已配置的字典类型名称。
     */
    List<String> listTypes();

    SysDict create(SysDict dict);

    SysDict update(Long id, SysDict dict);

    void delete(Long id);

    /**
     * 清空字典缓存（写操作后调用）。
     */
    void evictCache();
}
