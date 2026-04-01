package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.CountryInfo;

import java.util.List;

/**
 * 国家信息服务接口
 *
 * @author Administrator
 * @since 2026-03-17
 */
public interface CountryInfoService extends IService<CountryInfo> {
    CountryInfo getCountryInfoByCode(String code);
    CountryInfo getCountryInfoByName(String name);
    CountryInfo getCountryInfoByEnglishName(String englishName);

    /**
     * 获取所有启用的国家信息
     * @return 国家信息列表
     */
    List<CountryInfo> getEnabledList();
}