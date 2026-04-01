package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.MeasurementUnitDao;
import com.declaration.entity.MeasurementUnit;
import com.declaration.service.MeasurementUnitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.io.Serializable;
import java.util.List;

/**
 * 计量单位配置服务实现类
 *
 * @author Administrator
 * @since 2026-03-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeasurementUnitServiceImpl extends ServiceImpl<MeasurementUnitDao, MeasurementUnit> implements MeasurementUnitService {

    @Override
    @Cacheable(value = "sys:dict:measurement-units")
    public List<MeasurementUnit> getActiveUnits() {
        return list(new LambdaQueryWrapper<MeasurementUnit>()
                .eq(MeasurementUnit::getStatus, 1)
                .orderByAsc(MeasurementUnit::getSort));
    }

    @Override
    public MeasurementUnit getByUnitCode(String unitCode) {
        return getOne(new LambdaQueryWrapper<MeasurementUnit>()
                .eq(MeasurementUnit::getUnitCode, unitCode)
                .eq(MeasurementUnit::getStatus, 1));
    }

    @Override
    @CacheEvict(value = "sys:dict:measurement-units", allEntries = true)
    public boolean save(MeasurementUnit entity) {
        return super.save(entity);
    }

    @Override
    @CacheEvict(value = "sys:dict:measurement-units", allEntries = true)
    public boolean updateById(MeasurementUnit entity) {
        return super.updateById(entity);
    }

    @Override
    @CacheEvict(value = "sys:dict:measurement-units", allEntries = true)
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }
}