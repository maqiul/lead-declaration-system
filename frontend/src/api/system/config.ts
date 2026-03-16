import request from '@/utils/request'

// 系统配置相关API

/**
 * 获取系统基本信息
 */
export function getSystemBasicInfo() {
  return request({
    url: '/system/config/basic-info',
    method: 'get'
  })
}

/**
 * 获取UI配置
 */
export function getUiConfig() {
  return request({
    url: '/system/config/ui-config',
    method: 'get'
  })
}

/**
 * 根据分组获取配置
 * @param group 配置分组
 */
export function getConfigsByGroup(group: string) {
  return request({
    url: `/system/config/group/${group}`,
    method: 'get'
  })
}

/**
 * 根据配置键获取配置值
 * @param configKey 配置键
 */
export function getConfigValue(configKey: string) {
  return request({
    url: `/system/config/${configKey}`,
    method: 'get'
  })
}

/**
 * 更新配置值
 * @param configKey 配置键
 * @param data 配置数据
 */
export function updateConfig(configKey: string, data: any) {
  return request({
    url: `/system/config/${configKey}`,
    method: 'put',
    data
  })
}

/**
 * 获取所有启用的配置
 */
export function getAllConfigs() {
  return request({
    url: '/system/config/all',
    method: 'get'
  })
}

/**
 * 新增配置
 * @param data 配置数据
 */
export function addConfig(data: any) {
  return request({
    url: '/system/config',
    method: 'post',
    data
  })
}

/**
 * 修改配置
 * @param data 配置数据
 */
export function updateConfigFull(data: any) {
  return request({
    url: '/system/config',
    method: 'put',
    data
  })
}

/**
 * 删除配置
 * @param id 配置ID
 */
export function deleteConfig(id: number) {
  return request({
    url: `/system/config/${id}`,
    method: 'delete'
  })
}

/**
 * 获取配置选项
 * @param configKey 配置键
 */
export function getConfigOptions(configKey: string) {
  return request({
    url: `/system/config/options/${configKey}`,
    method: 'get'
  })
}

/**
 * 刷新配置缓存
 * @param configKey 配置键
 */
export function refreshConfigCache(configKey: string) {
  return request({
    url: `/system/config/refresh-cache/${configKey}`,
    method: 'post'
  })
}

/**
 * 获取系统内置参数
 */
export function getSystemParameters() {
  return request({
    url: '/system/config/system-params',
    method: 'get'
  })
}

/**
 * 获取下拉框选项
 * @param configKey 配置键
 */
export function getSelectOptions(configKey: string) {
  return request({
    url: `/system/config/select-options/${configKey}`,
    method: 'get'
  })
}

/**
 * 获取运输方式选项
 */
export function getTransportModes() {
  return request({
    url: '/system/config/select-options/Transport-details',
    method: 'get'
  })
}

/**
 * 将配置存储到Redis
 * @param configKey 配置键
 * @param configValue 配置值
 */
export function saveConfigToRedis(configKey: string, configValue: string) {
  return request({
    url: `/system/config/redis/${configKey}`,
    method: 'post',
    data: { configValue }
  })
}

/**
 * 从Redis获取配置
 * @param configKey 配置键
 */
export function getConfigFromRedis(configKey: string) {
  return request({
    url: `/system/config/redis/${configKey}`,
    method: 'get'
  })
}

/**
 * 批量将配置存储到Redis
 * @param configs 配置映射
 */
export function batchSaveConfigsToRedis(configs: Record<string, string>) {
  return request({
    url: '/system/config/redis/batch-save',
    method: 'post',
    data: configs
  })
}

/**
 * 从Redis批量获取配置
 * @param configKeys 配置键列表
 */
export function batchGetConfigsFromRedis(configKeys: string[]) {
  return request({
    url: '/system/config/redis/batch-get',
    method: 'post',
    data: configKeys
  })
}