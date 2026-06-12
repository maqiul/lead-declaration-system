package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.EntityConfigDao;
import com.declaration.entity.EntityConfig;
import com.declaration.service.EntityConfigService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

/**
 * 主体配置服务实现类
 *
 * @author Administrator
 * @since 2026-04-28
 */
@Service
public class EntityConfigServiceImpl extends ServiceImpl<EntityConfigDao, EntityConfig> implements EntityConfigService {

    @Override
    @Cacheable(value = "sys:dict:entity-configs", key = "'ENABLED'")
    public List<EntityConfig> getEnabledList() {
        LambdaQueryWrapper<EntityConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EntityConfig::getStatus, 1)
               .orderByDesc(EntityConfig::getIsDefault)
               .orderByAsc(EntityConfig::getSort)
               .orderByAsc(EntityConfig::getEntityName);
        return this.list(wrapper);
    }

    @Override
    public EntityConfig getDefault() {
        // 先查默认主体
        LambdaQueryWrapper<EntityConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EntityConfig::getStatus, 1)
               .eq(EntityConfig::getIsDefault, 1)
               .last("LIMIT 1");
        EntityConfig entity = this.getOne(wrapper);
        if (entity != null) {
            return entity;
        }
        // 无默认主体时返回第一个启用的
        LambdaQueryWrapper<EntityConfig> fallback = new LambdaQueryWrapper<>();
        fallback.eq(EntityConfig::getStatus, 1)
                .orderByAsc(EntityConfig::getSort)
                .last("LIMIT 1");
        return this.getOne(fallback);
    }

    @Override
    @CacheEvict(value = "sys:dict:entity-configs", allEntries = true)
    public boolean save(EntityConfig entity) {
        return super.save(entity);
    }

    @Override
    @CacheEvict(value = "sys:dict:entity-configs", allEntries = true)
    public boolean updateById(EntityConfig entity) {
        return super.updateById(entity);
    }

    @Override
    @CacheEvict(value = "sys:dict:entity-configs", allEntries = true)
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }
}
