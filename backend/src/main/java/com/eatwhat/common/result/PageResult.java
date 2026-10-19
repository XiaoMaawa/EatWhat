package com.eatwhat.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页响应载荷。
 *
 * <pre>
 * { "records": [], "total": 128, "page": 1, "size": 10, "pages": 13 }
 * </pre>
 */
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> records = new ArrayList<>();
    private Long total = 0L;
    private Long page = 1L;
    private Long size = 10L;
    private Long pages = 0L;

    public PageResult() {
    }

    public PageResult(List<T> records, Long total, Long page, Long size) {
        this.records = records == null ? new ArrayList<>() : records;
        this.total = total == null ? 0L : total;
        this.page = page == null ? 1L : page;
        this.size = (size == null || size <= 0) ? 10L : size;
        this.pages = this.size == 0 ? 0L : (this.total + this.size - 1) / this.size;
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        return new PageResult<>(records, total, page, size);
    }

    public static <T> PageResult<T> empty(long page, long size) {
        return new PageResult<>(new ArrayList<>(), 0L, page, size);
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getPages() {
        return pages;
    }

    public void setPages(Long pages) {
        this.pages = pages;
    }
}
