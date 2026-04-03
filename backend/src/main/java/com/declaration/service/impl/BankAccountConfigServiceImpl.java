package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.BankAccountConfigDao;
import com.declaration.entity.BankAccountConfig;
import com.declaration.service.BankAccountConfigService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

/**
 * 银行账户配置服务实现类
 *
 * @author Administrator
 * @since 2026-03-17
 */
@Service
public class BankAccountConfigServiceImpl extends ServiceImpl<BankAccountConfigDao, BankAccountConfig> implements BankAccountConfigService {

    @Override
    @Cacheable(value = "sys:dict:bank-accounts", key = "#currency != null && !#currency.isEmpty() ? #currency : 'ALL'")
    public List<BankAccountConfig> getEnabledList(String currency) {
        LambdaQueryWrapper<BankAccountConfig> wrapper = new LambdaQueryWrapper<BankAccountConfig>();
        wrapper.eq(BankAccountConfig::getStatus, 1);
        
        if (currency != null && !currency.isEmpty()) {
            wrapper.eq(BankAccountConfig::getCurrency, currency);
        }
        
        wrapper.orderByDesc(BankAccountConfig::getIsDefault)
               .orderByAsc(BankAccountConfig::getSort)
               .orderByAsc(BankAccountConfig::getAccountName);
               
        return this.list(wrapper);
    }

    @Override
    @CacheEvict(value = "sys:dict:bank-accounts", allEntries = true)
    public boolean save(BankAccountConfig entity) {
        return super.save(entity);
    }

    @Override
    @CacheEvict(value = "sys:dict:bank-accounts", allEntries = true)
    public boolean updateById(BankAccountConfig entity) {
        return super.updateById(entity);
    }

    @Override
    @CacheEvict(value = "sys:dict:bank-accounts", allEntries = true)
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }
}