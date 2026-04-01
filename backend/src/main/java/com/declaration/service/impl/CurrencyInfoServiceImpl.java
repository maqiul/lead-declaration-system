package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.CurrencyInfoDao;
import com.declaration.entity.CurrencyInfo;
import com.declaration.service.CurrencyInfoService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

/**
 * 货币信息服务实现类
 *
 * @author Administrator
 * @since 2026-03-23
 */
@Service
public class CurrencyInfoServiceImpl extends ServiceImpl<CurrencyInfoDao, CurrencyInfo> implements CurrencyInfoService {
    
    @Override
    public CurrencyInfo getByCurrencyCode(String code) {
        return this.getOne(new QueryWrapper<CurrencyInfo>().eq("currency_code", code));
    }
    
    @Override
    public CurrencyInfo getByChineseName(String chineseName) {
        return this.getOne(new QueryWrapper<CurrencyInfo>().eq("chinese_name", chineseName));
    }

    @Override
    @Cacheable(value = "sys:dict:currencies")
    public List<CurrencyInfo> getEnabledList() {
        return this.list(new QueryWrapper<CurrencyInfo>()
                .eq("status", 1)
                .orderByAsc("sort", "chinese_name"));
    }

    @Override
    @CacheEvict(value = "sys:dict:currencies", allEntries = true)
    public boolean save(CurrencyInfo entity) {
        return super.save(entity);
    }

    @Override
    @CacheEvict(value = "sys:dict:currencies", allEntries = true)
    public boolean updateById(CurrencyInfo entity) {
        return super.updateById(entity);
    }

    @Override
    @CacheEvict(value = "sys:dict:currencies", allEntries = true)
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }
}
