package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.OperationLog;

/**
 * 操作日志Service接口
 *
 * @author Administrator
 * @since 2026-03-23
 */
public interface OperationLogService extends IService<OperationLog> {
    
    /**
     * 记录操作日志
     * @param log 操作日志对象
     */
    void saveOperationLog(OperationLog log);
    
    /**
     * 清理过期日志
     * @param days 保留天数
     */
    void cleanExpiredLogs(Integer days);
}