package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.ProductTypeConfigDao;
import com.declaration.entity.DeclarationElement;
import com.declaration.entity.ProductTypeConfig;
import com.declaration.service.ProductTypeConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.List;

/**
 * HS商品类型配置服务实现
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTypeConfigServiceImpl extends ServiceImpl<ProductTypeConfigDao, ProductTypeConfig> implements ProductTypeConfigService {

    private final ObjectMapper objectMapper;

    @Override
    public IPage<ProductTypeConfig> getPage(Integer pageNum, Integer pageSize, String keyword) {
        Page<ProductTypeConfig> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<ProductTypeConfig> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ProductTypeConfig::getHsCode, keyword)
                    .or()
                    .like(ProductTypeConfig::getEnglishName, keyword)
                    .or()
                    .like(ProductTypeConfig::getChineseName, keyword);
        }
        
        wrapper.orderByAsc(ProductTypeConfig::getSort);
        wrapper.orderByDesc(ProductTypeConfig::getCreateTime);
        
        IPage<ProductTypeConfig> result = page(page, wrapper);
        
        // 解析申报要素JSON
        result.getRecords().forEach(this::parseElements);
        
        return result;
    }

    @Override
    @Cacheable(value = "sys:dict:product-types")
    public List<ProductTypeConfig> getEnabledList() {
        LambdaQueryWrapper<ProductTypeConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductTypeConfig::getStatus, 1);
        wrapper.orderByAsc(ProductTypeConfig::getSort);
        
        List<ProductTypeConfig> list = list(wrapper);
        list.forEach(this::parseElements);
        return list;
    }

    @Override
    @Cacheable(value = "sys:dict:product-types:hscode", key = "#hsCode")
    public ProductTypeConfig getByHsCode(String hsCode) {
        LambdaQueryWrapper<ProductTypeConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductTypeConfig::getHsCode, hsCode);
        
        ProductTypeConfig config = getOne(wrapper);
        if (config != null) {
            parseElements(config);
        }
        return config;
    }

    /**
     * 解析申报要素JSON
     */
    private void parseElements(ProductTypeConfig config) {
        if (StringUtils.hasText(config.getElementsConfig())) {
            try {
                List<DeclarationElement> elements = objectMapper.readValue(
                        config.getElementsConfig(),
                        new TypeReference<List<DeclarationElement>>() {}
                );
                config.setElements(elements);
            } catch (JsonProcessingException e) {
                log.error("解析申报要素JSON失败", e);
            }
        }
    }

    @Override
    @CacheEvict(value = "sys:dict:product-types", allEntries = true)
    public boolean save(ProductTypeConfig entity) {
        return super.save(entity);
    }

    @Override
    @CacheEvict(value = "sys:dict:product-types", allEntries = true)
    public boolean updateById(ProductTypeConfig entity) {
        return super.updateById(entity);
    }

    @Override
    @CacheEvict(value = "sys:dict:product-types", allEntries = true)
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }
}
