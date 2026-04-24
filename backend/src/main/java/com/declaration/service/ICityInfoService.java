package com.declaration.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.CityInfo;

import java.util.List;

/**
 * 城市信息服务接口
 */
public interface ICityInfoService extends IService<CityInfo> {

    /**
     * 获取城市信息分页列表
     *
     * @param pageNum  页码
     * @param pageSize 页大小
     * @param cityName 城市名称
     * @param provinceName 省份名称
     * @param countryName 国家名称
     * @param status 状态
     * @return 城市信息分页数据
     */
    Page<CityInfo> getPageList(int pageNum, int pageSize, String cityName, String provinceName, String countryName, Integer status);

    /**
     * 根据ID获取城市信息
     *
     * @param id 城市ID
     * @return 城市信息
     */
    CityInfo getInfoById(Long id);

    /**
     * 新增城市信息
     *
     * @param cityInfo 城市信息
     */
    void addCityInfo(CityInfo cityInfo);

    /**
     * 修改城市信息
     *
     * @param cityInfo 城市信息
     */
    void updateCityInfo(CityInfo cityInfo);

    /**
     * 删除城市信息
     *
     * @param id 城市ID
     */
    void deleteCityInfo(Long id);

    /**
     * 更新城市状态
     *
     * @param id 城市ID
     * @param status 状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 根据国家获取城市列表
     * @param country 国家名称
     * @return 城市列表
     */
    List<CityInfo> getCitiesByCountry(String country);

    /**
     * 获取所有启用的城市信息
     * @return 启用的城市列表
     */
    List<CityInfo> getEnabledList();
}