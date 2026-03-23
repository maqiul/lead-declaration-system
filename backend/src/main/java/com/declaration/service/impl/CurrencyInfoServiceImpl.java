package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.CurrencyInfoDao;
import com.declaration.entity.CurrencyInfo;
import com.declaration.service.CurrencyInfoService;
import org.springframework.stereotype.Service;

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
}
