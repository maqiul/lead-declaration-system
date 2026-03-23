package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.CountryInfoDao;
import com.declaration.entity.CountryInfo;
import com.declaration.service.CountryInfoService;
import org.springframework.stereotype.Service;

/**
 * 国家信息服务实现类
 *
 * @author Administrator
 * @since 2026-03-17
 */
@Service
public class CountryInfoServiceImpl extends ServiceImpl<CountryInfoDao, CountryInfo> implements CountryInfoService {
    @Override
    public CountryInfo getCountryInfoByCode(String code) {
        return this.getOne(new QueryWrapper<CountryInfo>().eq("country_code", code));
    }
    @Override
    public CountryInfo getCountryInfoByName(String name) {
        return this.getOne(new QueryWrapper<CountryInfo>().eq("chinese_name", name));
    }
    @Override
    public CountryInfo getCountryInfoByEnglishName(String englishName) {
        return this.getOne(new QueryWrapper<CountryInfo>().eq("english_name", englishName));
    }
}