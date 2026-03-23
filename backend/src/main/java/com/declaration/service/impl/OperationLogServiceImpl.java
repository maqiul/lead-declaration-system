package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.OperationLogMapper;
import com.declaration.entity.OperationLog;
import com.declaration.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 操作日志Service实现类
 *
 * @author Administrator
 * @since 2026-03-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Async
    @Override
    public void saveOperationLog(OperationLog log) {
        try {
            this.save(log);
            log.info("操作日志记录成功: 用户={}, 操作={}, 业务={}", 
                     log.getUsername(), log.getOperationType(), log.getBusinessType());
        } catch (Exception e) {
            log.error("操作日志记录失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void cleanExpiredLogs(Integer days) {
        try {
            LocalDateTime expireTime = LocalDateTime.now().minusDays(days);
            LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.lt(OperationLog::getCreateTime, expireTime);
            
            int deletedCount = this.baseMapper.delete(wrapper);
            log.info("清理过期操作日志完成，删除记录数: {}", deletedCount);
        } catch (Exception e) {
            log.error("清理过期操作日志失败: {}", e.getMessage(), e);
        }
    }
}