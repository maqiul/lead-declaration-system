package com.declaration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.declaration.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统配置DAO接口
 *
 * @author Administrator
 * @since 2026-03-14
 */
@Mapper
public interface SystemConfigDao extends BaseMapper<SystemConfig> {
}