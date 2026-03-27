package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.CityInfoDao;
import com.declaration.entity.CityInfo;
import com.declaration.service.ICityInfoService;
import com.declaration.utils.OrganizationUtils;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 城市信息服务实现类
 */
@Service
public class CityInfoServiceImpl extends ServiceImpl<CityInfoDao, CityInfo> implements ICityInfoService {

    @Override
    public Page<CityInfo> getPageList(int pageNum, int pageSize, String cityName, String provinceName, String countryName, Integer status) {
        Page<CityInfo> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<CityInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(CityInfo::getStatus, 1).or().eq(CityInfo::getStatus, 0)); // 只查询启用或禁用的数据
        
        if (cityName != null && !cityName.trim().isEmpty()) {
            wrapper.like(CityInfo::getCityName, cityName.trim());
        }
        if (provinceName != null && !provinceName.trim().isEmpty()) {
            wrapper.like(CityInfo::getProvinceName, provinceName.trim());
        }
        if (countryName != null && !countryName.trim().isEmpty()) {
            wrapper.like(CityInfo::getCountryName, countryName.trim());
        }
        if (status != null) {
            wrapper.eq(CityInfo::getStatus, status);
        }
        
        wrapper.orderByAsc(CityInfo::getSort).orderByDesc(CityInfo::getId);
        
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public CityInfo getInfoById(Long id) {
        return getById(id);
    }

    @Override
    public void addCityInfo(CityInfo cityInfo) {
        // 设置创建信息
        cityInfo.setCreateTime(LocalDateTime.now());
        cityInfo.setCreateBy(StpUtil.getLoginIdAsString());
        cityInfo.setUpdateTime(LocalDateTime.now());
        cityInfo.setUpdateBy(StpUtil.getLoginIdAsString());
        
        save(cityInfo);
    }

    @Override
    public void updateCityInfo(CityInfo cityInfo) {
        // 设置更新信息
        cityInfo.setUpdateTime(LocalDateTime.now());
        cityInfo.setUpdateBy(StpUtil.getLoginIdAsString());
        
        updateById(cityInfo);
    }

    @Override
    public void deleteCityInfo(Long id) {
        removeById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        CityInfo cityInfo = new CityInfo();
        cityInfo.setId(id);
        cityInfo.setStatus(status);
        cityInfo.setUpdateTime(LocalDateTime.now());
        cityInfo.setUpdateBy(StpUtil.getLoginIdAsString());
        
        updateById(cityInfo);
    }

    // @Override
    // public List<String> getProvinceList() {
    //     return baseMapper.selectProvinceList();
    // }

    // @Override
    // public List<CityInfo> searchCities(String keyword) {
    //     return baseMapper.searchCities(keyword);
    // }
}