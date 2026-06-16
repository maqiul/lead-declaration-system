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
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<String, Object> redisTemplate;

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
    public List<ProductTypeConfig> getEnabledList() {
        try {
            // 尝试从缓存获取
            List<ProductTypeConfig> cachedList = getCachedEnabledList();
            if (cachedList != null) {
                return cachedList;
            }
            
            // 缓存未命中，从数据库查询
            LambdaQueryWrapper<ProductTypeConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProductTypeConfig::getStatus, 1);
            wrapper.orderByAsc(ProductTypeConfig::getSort);
            
            List<ProductTypeConfig> list = list(wrapper);
            list.forEach(this::parseElements);
            
            // 尝试缓存结果（失败不影响业务）
            try {
                cacheEnabledList(list);
            } catch (Exception e) {
                log.warn("缓存HS商品类型列表失败，但不影响业务: {}", e.getMessage());
            }
            
            return list;
        } catch (Exception e) {
            log.error("获取HS商品类型列表失败", e);
            // 如果Redis完全不可用，直接查询数据库
            LambdaQueryWrapper<ProductTypeConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProductTypeConfig::getStatus, 1);
            wrapper.orderByAsc(ProductTypeConfig::getSort);
            
            List<ProductTypeConfig> list = list(wrapper);
            list.forEach(this::parseElements);
            return list;
        }
    }

    @Override
    public ProductTypeConfig getByHsCode(String hsCode) {
        if (hsCode == null || hsCode.isBlank()) {
            return null;
        }
        // 规范化：去掉点号和空格，统一为纯数字编码
        String normalizedCode = hsCode.replaceAll("[.\\s]", "");

        try {
            // 尝试从缓存获取（用规范化后的编码作 key）
            ProductTypeConfig cachedConfig = getCachedByHsCode(normalizedCode);
            if (cachedConfig != null) {
                return cachedConfig;
            }

            // 优先精确匹配
            LambdaQueryWrapper<ProductTypeConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProductTypeConfig::getHsCode, normalizedCode);
            ProductTypeConfig config = getOne(wrapper);

            // 精确匹配失败，尝试去点号后匹配（兼容数据库中带点号存储的情况）
            if (config == null) {
                List<ProductTypeConfig> allEnabled = getEnabledList();
                for (ProductTypeConfig ptc : allEnabled) {
                    String dbNormalized = ptc.getHsCode() != null ? ptc.getHsCode().replaceAll("[.\\s]", "") : "";
                    if (dbNormalized.equals(normalizedCode)) {
                        config = ptc;
                        break;
                    }
                }
            }

            if (config != null) {
                parseElements(config);
                try {
                    cacheByHsCode(normalizedCode, config);
                } catch (Exception e) {
                    log.warn("缓存HS商品类型详情失败，但不影响业务: {}", e.getMessage());
                }
            }
            return config;
        } catch (Exception e) {
            log.error("根据HS编码获取商品类型失败: hsCode={}", hsCode, e);
            LambdaQueryWrapper<ProductTypeConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProductTypeConfig::getHsCode, normalizedCode);
            ProductTypeConfig config = getOne(wrapper);
            if (config != null) {
                parseElements(config);
            }
            return config;
        }
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
    public boolean save(ProductTypeConfig entity) {
        boolean result = super.save(entity);
        if (result) {
            // 清除缓存（失败不影响业务）
            try {
                clearProductTypesCache();
            } catch (Exception e) {
                log.warn("清除HS商品类型缓存失败，但不影响业务: {}", e.getMessage());
            }
        }
        return result;
    }

    @Override
    public boolean updateById(ProductTypeConfig entity) {
        boolean result = super.updateById(entity);
        if (result) {
            // 清除缓存（失败不影响业务）
            try {
                clearProductTypesCache();
            } catch (Exception e) {
                log.warn("清除HS商品类型缓存失败，但不影响业务: {}", e.getMessage());
            }
        }
        return result;
    }

    @Override
    public boolean removeById(Serializable id) {
        boolean result = super.removeById(id);
        if (result) {
            // 清除缓存（失败不影响业务）
            try {
                clearProductTypesCache();
            } catch (Exception e) {
                log.warn("清除HS商品类型缓存失败，但不影响业务: {}", e.getMessage());
            }
        }
        return result;
    }
    
    /**
     * 从缓存获取启用的商品类型列表
     */
    @SuppressWarnings("unchecked")
    private List<ProductTypeConfig> getCachedEnabledList() {
        try {
            String cacheKey = "sys:dict:product-types";
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List) {
                return (List<ProductTypeConfig>) cached;
            }
        } catch (Exception e) {
            log.debug("从缓存获取HS商品类型列表失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 缓存启用的商品类型列表
     */
    private void cacheEnabledList(List<ProductTypeConfig> list) {
        try {
            String cacheKey = "sys:dict:product-types";
            redisTemplate.opsForValue().set(cacheKey, list, java.time.Duration.ofHours(24));
        } catch (Exception e) {
            log.warn("缓存HS商品类型列表失败: {}", e.getMessage());
        }
    }
    
    /**
     * 从缓存根据HS编码获取商品类型
     */
    private ProductTypeConfig getCachedByHsCode(String hsCode) {
        try {
            String cacheKey = "sys:dict:product-types:hscode:" + hsCode;
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof ProductTypeConfig) {
                return (ProductTypeConfig) cached;
            }
        } catch (Exception e) {
            log.debug("从缓存获取HS商品类型详情失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 缓存HS编码对应的商品类型
     */
    private void cacheByHsCode(String hsCode, ProductTypeConfig config) {
        try {
            String cacheKey = "sys:dict:product-types:hscode:" + hsCode;
            redisTemplate.opsForValue().set(cacheKey, config, java.time.Duration.ofHours(24));
        } catch (Exception e) {
            log.warn("缓存HS商品类型详情失败: {}", e.getMessage());
        }
    }
    
    /**
     * 清除商品类型相关缓存
     */
    private void clearProductTypesCache() {
        try {
            // 清除列表缓存
            String listCacheKey = "sys:dict:product-types";
            redisTemplate.delete(listCacheKey);
            
            // 注意：由于Redis不支持通配符删除的高效方式，这里不删除所有hscode缓存
            // 让它们自然过期（24小时）
            log.debug("已清除HS商品类型列表缓存");
        } catch (Exception e) {
            log.warn("清除HS商品类型缓存失败: {}", e.getMessage());
        }
    }
}
