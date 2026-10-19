package com.eatwhat.modules.admin.service;

import com.eatwhat.modules.admin.entity.OperationLog;
import com.eatwhat.modules.admin.mapper.OperationLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务。
 * <p>日志写入失败不影响主流程，仅记录到本地日志文件，避免因为审计落库异常阻断业务。</p>
 */
@Service
public class OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogService.class);

    /** 请求参数最多留存的字符数，避免 TEXT 字段被超长内容撑爆 */
    private static final int MAX_PARAMS_LENGTH = 1000;

    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * 记录一条操作日志。
     */
    public void record(OperationLog entry) {
        if (entry == null) {
            return;
        }
        try {
            entry.setRequestParams(truncate(entry.getRequestParams(), MAX_PARAMS_LENGTH));
            entry.setAction(truncate(entry.getAction(), 100));
            entry.setErrorMsg(truncate(entry.getErrorMsg(), 500));
            operationLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("写入操作日志失败：{}", e.getMessage());
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
