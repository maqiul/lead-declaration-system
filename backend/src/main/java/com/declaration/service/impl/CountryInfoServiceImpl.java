package com.declaration.service.impl;

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
}