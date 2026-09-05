package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.PartyBConfigDao;
import com.declaration.entity.PartyBConfig;
import com.declaration.service.PartyBConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 乙方配置服务实现类
 */
@Service
public class PartyBConfigServiceImpl extends ServiceImpl<PartyBConfigDao, PartyBConfig> implements PartyBConfigService {

    @Override
    public List<PartyBConfig> getEnabledListByUserId(Long userId) {
        LambdaQueryWrapper<PartyBConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PartyBConfig::getUserId, userId)
               .eq(PartyBConfig::getStatus, 1)
               .orderByAsc(PartyBConfig::getSort)
               .orderByAsc(PartyBConfig::getPartyBName);
        return this.list(wrapper);
    }
}
