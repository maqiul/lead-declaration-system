package com.declaration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.declaration.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志Mapper接口
 *
 * @author Administrator
 * @since 2026-03-23
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}