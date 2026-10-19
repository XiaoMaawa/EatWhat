package com.eatwhat.modules.content.service;

import com.eatwhat.common.result.PageResult;
import com.eatwhat.modules.content.entity.SensitiveWord;

import java.util.List;

/**
 * 敏感词服务。
 */
public interface SensitiveWordService {

    /**
     * 管理端分页查询敏感词。
     */
    PageResult<SensitiveWord> pageAll(String keyword, Integer status, int page, int size);

    /**
     * 新增敏感词。
     */
    SensitiveWord create(String word, String category);

    /**
     * 修改敏感词。
     */
    SensitiveWord update(Long id, String word, String category, Integer status);

    /**
     * 删除敏感词。
     */
    void delete(Long id);

    /**
     * 批量导入敏感词，返回成功条数。
     */
    int batchImport(List<String> words, String category);

    /**
     * 文本命中检测。
     *
     * @param text     待检测文本
     * @param category ALL / REVIEW / REMARK / NICKNAME
     * @return 命中的敏感词列表，未命中返回空列表
     */
    List<String> hits(String text, String category);
}
