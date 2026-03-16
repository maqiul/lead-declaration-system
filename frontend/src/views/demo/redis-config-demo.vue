<template>
  <div class="redis-config-demo">
    <a-card title="Redis配置存储示例" style="margin-bottom: 20px;">
      <a-form layout="vertical">
        <a-form-item label="配置键">
          <a-input v-model:value="configKey" placeholder="请输入配置键" />
        </a-form-item>
        <a-form-item label="配置值">
          <a-input v-model:value="configValue" placeholder="请输入配置值" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="saveToRedis" :loading="saving">
              保存到Redis
            </a-button>
            <a-button @click="getFromRedis" :loading="loading">
              从Redis获取
            </a-button>
            <a-button @click="clearForm">
              清空
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
      
      <a-divider />
      
      <div>
        <h3>操作结果:</h3>
        <pre>{{ result }}</pre>
      </div>
    </a-card>

    <a-card title="批量操作示例">
      <a-space>
        <a-button @click="batchSaveDemo" :loading="batchSaving">
          批量保存示例数据
        </a-button>
        <a-button @click="batchGetDemo" :loading="batchLoading">
          批量获取示例数据
        </a-button>
      </a-space>
      
      <div style="margin-top: 20px;">
        <h3>批量操作结果:</h3>
        <pre>{{ batchResult }}</pre>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { 
  saveConfigToRedis, 
  getConfigFromRedis, 
  batchSaveConfigsToRedis, 
  batchGetConfigsFromRedis 
} from '@/api/system/config'

// 响应式数据
const configKey = ref('')
const configValue = ref('')
const result = ref('')
const batchResult = ref('')
const saving = ref(false)
const loading = ref(false)
const batchSaving = ref(false)
const batchLoading = ref(false)

// 保存到Redis
const saveToRedis = async () => {
  if (!configKey.value || !configValue.value) {
    message.warning('请填写配置键和配置值')
    return
  }
  
  try {
    saving.value = true
    const response = await saveConfigToRedis(configKey.value, configValue.value)
    if (response.data?.code === 200) {
      result.value = `保存成功: ${configKey.value} = ${configValue.value}`
      message.success('配置已保存到Redis')
    } else {
      result.value = `保存失败: ${response.data?.msg || '未知错误'}`
      message.error('保存失败')
    }
  } catch (error) {
    result.value = `保存异常: ${error}`
    message.error('保存异常')
  } finally {
    saving.value = false
  }
}

// 从Redis获取
const getFromRedis = async () => {
  if (!configKey.value) {
    message.warning('请填写配置键')
    return
  }
  
  try {
    loading.value = true
    const response = await getConfigFromRedis(configKey.value)
    if (response.data?.code === 200) {
      const value = response.data.data
      result.value = `获取成功: ${configKey.value} = ${value || 'null'}`
      configValue.value = value || ''
      message.success('配置获取成功')
    } else {
      result.value = `获取失败: ${response.data?.msg || '未知错误'}`
      message.error('获取失败')
    }
  } catch (error) {
    result.value = `获取异常: ${error}`
    message.error('获取异常')
  } finally {
    loading.value = false
  }
}

// 批量保存示例
const batchSaveDemo = async () => {
  const demoData = {
    'demo.config1': '测试配置1',
    'demo.config2': '测试配置2',
    'demo.config3': '测试配置3',
    'system.theme': 'blue',
    'system.language': 'zh-CN'
  }
  
  try {
    batchSaving.value = true
    const response = await batchSaveConfigsToRedis(demoData)
    if (response.data?.code === 200) {
      batchResult.value = `批量保存成功，共${Object.keys(demoData).length}个配置`
      message.success('批量保存成功')
    } else {
      batchResult.value = `批量保存失败: ${response.data?.msg || '未知错误'}`
      message.error('批量保存失败')
    }
  } catch (error) {
    batchResult.value = `批量保存异常: ${error}`
    message.error('批量保存异常')
  } finally {
    batchSaving.value = false
  }
}

// 批量获取示例
const batchGetDemo = async () => {
  const demoKeys = ['demo.config1', 'demo.config2', 'demo.config3', 'system.theme', 'system.language']
  
  try {
    batchLoading.value = true
    const response = await batchGetConfigsFromRedis(demoKeys)
    if (response.data?.code === 200) {
      const configs = response.data.data
      batchResult.value = JSON.stringify(configs, null, 2)
      message.success('批量获取成功')
    } else {
      batchResult.value = `批量获取失败: ${response.data?.msg || '未知错误'}`
      message.error('批量获取失败')
    }
  } catch (error) {
    batchResult.value = `批量获取异常: ${error}`
    message.error('批量获取异常')
  } finally {
    batchLoading.value = false
  }
}

// 清空表单
const clearForm = () => {
  configKey.value = ''
  configValue.value = ''
  result.value = ''
}
</script>

<style scoped>
.redis-config-demo {
  padding: 20px;
}

pre {
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>