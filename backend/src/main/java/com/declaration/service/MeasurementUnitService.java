package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.MeasurementUnit;

import java.util.List;

/**
 * 计量单位配置服务接口
 *
 * @author Administrator
 * @since 2026-03-14
 */
public interface MeasurementUnitService extends IService<MeasurementUnit> {

    /**
     * 获取所有启用的计量单位
     * @return 计量单位列表
     */
    List<MeasurementUnit> getActiveUnits();

    /**
     * 根据单位代码获取单位信息
     * @param unitCode 单位代码
     * @return 计量单位
     */
    MeasurementUnit getByUnitCode(String unitCode);
}