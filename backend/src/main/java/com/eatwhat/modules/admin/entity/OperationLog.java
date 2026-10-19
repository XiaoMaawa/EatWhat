package com.eatwhat.modules.admin.entity;

import com.eatwhat.common.entity.CreatedEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 后台操作日志。
 * <p>对应 operation_log 表，记录管理员与商家的关键写操作，用于审计追溯。</p>
 */
@TableName("operation_log")
public class OperationLog extends CreatedEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人角色 */
    private String operatorRole;

    /** 模块 */
    private String module;

    /** 操作描述 */
    private String action;

    /** 目标类型 */
    private String targetType;

    /** 目标ID */
    private Long targetId;

    /** HTTP 方法 */
    private String requestMethod;

    /** 请求路径 */
    private String requestUri;

    /** 请求参数（脱敏） */
    private String requestParams;

    /** 客户端 IP */
    private String ip;

    /** 1 成功 0 失败 */
    private Integer result;

    /** 错误信息 */
    private String errorMsg;

    /** 耗时（毫秒） */
    private Long costMs;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorRole() {
        return operatorRole;
    }

    public void setOperatorRole(String operatorRole) {
        this.operatorRole = operatorRole;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getResult() {
        return result;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Long getCostMs() {
        return costMs;
    }

    public void setCostMs(Long costMs) {
        this.costMs = costMs;
    }
}
