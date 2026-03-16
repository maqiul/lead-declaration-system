package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.SystemConfigDao;
import com.declaration.dto.ConfigOptionDTO;
import com.declaration.entity.SystemConfig;
import com.declaration.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务实现类
 *
 * @author Administrator
 * @since 2026-03-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigDao, SystemConfig> implements SystemConfigService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Cacheable(value = "system-config", key = "#configKey")
    public String getConfigValue(String configKey) {
        SystemConfig config = getOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey)
                .eq(SystemConfig::getStatus, 1));
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    @Cacheable(value = "system-config", key = "#configKey")
    public String getConfigValue(String configKey, String defaultValue) {
        String value = getConfigValue(configKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public Map<String, String> batchGetConfigValues(List<String> configKeys) {
        Map<String, String> result = new HashMap<>();
        List<SystemConfig> configs = list(new LambdaQueryWrapper<SystemConfig>()
                .in(SystemConfig::getConfigKey, configKeys)
                .eq(SystemConfig::getStatus, 1));
        
        configs.forEach(config -> result.put(config.getConfigKey(), config.getConfigValue()));
        return result;
    }

    @Override
    @CacheEvict(value = "system-config", key = "#configKey")
    public boolean updateConfigValue(String configKey, String configValue) {
        SystemConfig config = getOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey));
        
        if (config != null) {
            config.setConfigValue(configValue);
            return updateById(config);
        }
        return false;
    }

    @Override
    public List<SystemConfig> getConfigsByGroup(String configGroup) {
        return list(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigGroup, configGroup)
                .eq(SystemConfig::getStatus, 1)
                .orderByAsc(SystemConfig::getSort));
    }

    @Override
    public Map<String, String> getSystemBasicInfo() {
        List<String> basicKeys = Arrays.asList(
            "system.name",           // 系统名称
            "system.version",        // 系统版本
            "system.description",    // 系统描述
            "system.company",        // 公司名称
            "system.copyright"       // 版权信息
        );
        return batchGetConfigValues(basicKeys);
    }

    @Override
    public Map<String, String> getUiConfig() {
        List<String> uiKeys = Arrays.asList(
            "ui.logo",               // Logo图片URL
            "ui.favicon",            // 网站图标
            "ui.theme",              // 主题颜色
            "ui.footer.text",        // 底部文字
            "ui.footer.show",        // 是否显示底部
            "ui.sidebar.collapsed"   // 侧边栏默认状态
        );
        return batchGetConfigValues(uiKeys);
    }

    @Override
    public List<SystemConfig> getAllEnabledConfigs() {
        return list(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getStatus, 1)
                .orderByAsc(SystemConfig::getConfigGroup)
                .orderByAsc(SystemConfig::getSort));
    }

    @Override
    @Cacheable(value = "config-options", key = "#configKey")
    public List<ConfigOptionDTO> getConfigOptions(String configKey) {
        SystemConfig config = getOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey)
                .eq(SystemConfig::getStatus, 1));
        
        if (config == null) {
            return new ArrayList<>();
        }

        try {
            // 直接解析静态选项
            return parseStaticOptions(config.getSelectOptions());
        } catch (Exception e) {
            log.error("获取配置选项失败: configKey={}, error={}", configKey, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    @CacheEvict(value = "config-options", key = "#configKey")
    public void refreshConfigCache(String configKey) {
        log.info("刷新配置缓存: {}", configKey);
    }

    /**
     * 解析静态配置选项
     */
    private List<ConfigOptionDTO> parseStaticOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            return objectMapper.readValue(optionsJson, 
                new TypeReference<List<ConfigOptionDTO>>() {});
        } catch (Exception e) {
            log.error("解析静态配置选项失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public List<SystemConfig> getSystemParameters() {
        return list(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getIsSystemParam, 1)
                .eq(SystemConfig::getStatus, 1)
                .orderByAsc(SystemConfig::getConfigGroup)
                .orderByAsc(SystemConfig::getSort));
    }

    @Override
    @Cacheable(value = "select-options", key = "#configKey")
    public List<ConfigOptionDTO> getSelectOptions(String configKey) {
        SystemConfig config = getOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey)
                .eq(SystemConfig::getStatus, 1));
        
        if (config == null || config.getInputType() != 2) {
            return new ArrayList<>();
        }
        
        return parseStaticOptions(config.getSelectOptions());
    }

    @Override
    public boolean saveConfigToRedis(String configKey, String configValue) {
        try {
            String redisKey = "system:config:" + configKey;
            redisTemplate.opsForValue().set(redisKey, configValue);
            log.info("配置已存储到Redis: {}", redisKey);
            return true;
        } catch (Exception e) {
            log.error("存储配置到Redis失败: key={}, error={}", configKey, e.getMessage());
            return false;
        }
    }

    @Override
    public String getConfigFromRedis(String configKey) {
        try {
            String redisKey = "system:config:" + configKey;
            Object value = redisTemplate.opsForValue().get(redisKey);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("从Redis获取配置失败: key={}, error={}", configKey, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean batchSaveConfigsToRedis(Map<String, String> configs) {
        try {
            configs.forEach((key, value) -> {
                String redisKey = "system:config:" + key;
                redisTemplate.opsForValue().set(redisKey, value);
            });
            log.info("批量配置已存储到Redis，数量: {}", configs.size());
            return true;
        } catch (Exception e) {
            log.error("批量存储配置到Redis失败: error={}", e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, String> batchGetConfigsFromRedis(List<String> configKeys) {
        Map<String, String> result = new HashMap<>();
        try {
            for (String configKey : configKeys) {
                String redisKey = "system:config:" + configKey;
                Object value = redisTemplate.opsForValue().get(redisKey);
                if (value != null) {
                    result.put(configKey, value.toString());
                }
            }
            log.info("从Redis批量获取配置完成，命中: {}/{}", result.size(), configKeys.size());
            return result;
        } catch (Exception e) {
            log.error("从Redis批量获取配置失败: error={}", e.getMessage());
            return result;
        }
    }
}