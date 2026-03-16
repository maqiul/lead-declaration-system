package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.SystemConfig;

import com.declaration.dto.ConfigOptionDTO;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 *
 * @author Administrator
 * @since 2026-03-14
 */
public interface SystemConfigService extends IService<SystemConfig> {

    /**
     * 根据配置键获取配置值
     * @param configKey 配置键
     * @return 配置值
     */
    String getConfigValue(String configKey);

    /**
     * 根据配置键获取配置值，支持默认值
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getConfigValue(String configKey, String defaultValue);

    /**
     * 批量获取配置值
     * @param configKeys 配置键列表
     * @return 配置键值对
     */
    Map<String, String> batchGetConfigValues(List<String> configKeys);

    /**
     * 更新配置值
     * @param configKey 配置键
     * @param configValue 配置值
     * @return 是否成功
     */
    boolean updateConfigValue(String configKey, String configValue);

    /**
     * 根据分组获取配置列表
     * @param configGroup 配置分组
     * @return 配置列表
     */
    List<SystemConfig> getConfigsByGroup(String configGroup);

    /**
     * 获取系统基本信息配置
     * @return 系统基本信息
     */
    Map<String, String> getSystemBasicInfo();

    /**
     * 获取UI配置
     * @return UI配置信息
     */
    Map<String, String> getUiConfig();

    /**
     * 获取所有启用的配置
     * @return 配置列表
     */
    List<SystemConfig> getAllEnabledConfigs();

    /**
     * 根据配置键获取配置选项
     * @param configKey 配置键
     * @return 配置选项列表
     */
    List<ConfigOptionDTO> getConfigOptions(String configKey);

    /**
     * 刷新配置缓存
     * @param configKey 配置键
     */
    void refreshConfigCache(String configKey);

    /**
     * 获取系统内置参数
     * @return 系统内置参数列表
     */
    List<SystemConfig> getSystemParameters();

    /**
     * 获取下拉框选项
     * @param configKey 配置键
     * @return 下拉框选项列表
     */
    List<ConfigOptionDTO> getSelectOptions(String configKey);

    /**
     * 将配置存储到Redis
     * @param configKey 配置键
     * @param configValue 配置值
     * @return 是否成功
     */
    boolean saveConfigToRedis(String configKey, String configValue);

    /**
     * 从Redis获取配置
     * @param configKey 配置键
     * @return 配置值
     */
    String getConfigFromRedis(String configKey);

    /**
     * 批量将配置存储到Redis
     * @param configs 配置映射
     * @return 是否成功
     */
    boolean batchSaveConfigsToRedis(Map<String, String> configs);

    /**
     * 从Redis批量获取配置
     * @param configKeys 配置键列表
     * @return 配置映射
     */
    Map<String, String> batchGetConfigsFromRedis(List<String> configKeys);
}