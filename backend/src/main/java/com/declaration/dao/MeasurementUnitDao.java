package com.declaration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.declaration.entity.MeasurementUnit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 计量单位配置DAO
 *
 * @author Administrator
 * @since 2026-03-14
 */
@Mapper
public interface MeasurementUnitDao extends BaseMapper<MeasurementUnit> {
}