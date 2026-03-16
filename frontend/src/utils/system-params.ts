import { ref } from 'vue'
import { getSystemParameters, getConfigValue, getSelectOptions } from '@/api/system/config'
import { message } from 'ant-design-vue'

/**
 * 系统参数Hook
 * 提供系统参数的获取和管理功能
 */

// 系统参数缓存
const systemParams = ref<Record<string, any>>({})
const loading = ref(false)

/**
 * 加载系统参数
 */
export async function loadSystemParams() {
  if (loading.value) return
  
  try {
    loading.value = true
    const response = await getSystemParameters()
    if (response.data?.code === 200) {
      const params = response.data.data || []
      params.forEach((param: any) => {
        systemParams.value[param.configKey] = param.configValue
      })
    }
  } catch (error) {
    console.warn('加载系统参数失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 获取系统参数值
 * @param configKey 配置键
 * @param defaultValue 默认值
 */
export function getSystemParam(configKey: string, defaultValue?: any) {
  return systemParams.value[configKey] ?? defaultValue
}

/**
 * 获取系统参数值（异步）
 * @param configKey 配置键
 * @param defaultValue 默认值
 */
export async function getSystemParamAsync(configKey: string, defaultValue?: any) {
  try {
    // 先从缓存获取
    if (systemParams.value[configKey] !== undefined) {
      return systemParams.value[configKey]
    }
    
    // 缓存中没有则从API获取
    const response = await getConfigValue(configKey)
    if (response.data?.code === 200) {
      const value = response.data.data
      systemParams.value[configKey] = value
      return value
    }
  } catch (error) {
    console.warn(`获取系统参数失败: ${configKey}`, error)
  }
  
  return defaultValue
}

/**
 * 获取下拉框选项
 * @param configKey 配置键
 */
export async function getSystemSelectOptions(configKey: string) {
  try {
    const response = await getSelectOptions(configKey)
    if (response.data?.code === 200) {
      return response.data.data || []
    }
  } catch (error) {
    console.warn(`获取下拉框选项失败: ${configKey}`, error)
    return []
  }
}

/**
 * 刷新系统参数缓存
 */
export async function refreshSystemParams() {
  systemParams.value = {}
  await loadSystemParams()
}

// 初始化时加载系统参数
loadSystemParams()

// 导出响应式数据
export { systemParams, loading }