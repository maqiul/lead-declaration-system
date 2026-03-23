package com.declaration.task;

import com.declaration.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 操作日志清理任务
 *
 * @author Administrator
 * @since 2026-03-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogCleanTask {

    private final OperationLogService operationLogService;

    @Value("${system.log.keep-days:30}")
    private Integer keepDays;

    /**
     * 每天凌晨2点执行日志清理
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredLogs() {
        try {
            log.info("开始清理{}天前的操作日志", keepDays);
            operationLogService.cleanExpiredLogs(keepDays);
            log.info("操作日志清理完成");
        } catch (Exception e) {
            log.error("清理操作日志失败: {}", e.getMessage(), e);
        }
    }
}