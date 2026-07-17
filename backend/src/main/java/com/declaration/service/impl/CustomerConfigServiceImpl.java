package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.CustomerConfigDao;
import com.declaration.entity.CustomerConfig;
import com.declaration.service.CustomerConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 常用客户配置服务实现类
 */
@Service
public class CustomerConfigServiceImpl extends ServiceImpl<CustomerConfigDao, CustomerConfig> implements CustomerConfigService {

    @Override
    public List<CustomerConfig> getEnabledListByUserId(Long userId) {
        LambdaQueryWrapper<CustomerConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerConfig::getUserId, userId)
               .eq(CustomerConfig::getStatus, 1)
               .orderByAsc(CustomerConfig::getSort)
               .orderByAsc(CustomerConfig::getCustomerName);
        return this.list(wrapper);
    }
}
