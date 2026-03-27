<template>
  <div class="api-test-page">
    <a-page-header
      title="API 接口测试"
      sub-title="后端服务连通性与接口验证工具"
    >
      <template #extra>
        <a-button key="1" type="primary" @click="testBackendConnection">
          <template #icon><api-outlined /></template>
          测试后端连接
        </a-button>
        <a-button key="2" @click="openSwaggerDoc">
          <template #icon><link-outlined /></template>
          Swagger 文档
        </a-button>
      </template>
    </a-page-header>

    <div class="api-test-content">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-card title="模型验证测试" class="test-card">
            <template #extra>
              <a-tag color="blue">HTTP POST</a-tag>
            </template>
            <p>测试后端模型验证逻辑，验证字段必填项与格式校验。</p>
            <a-form layout="vertical">
              <a-form-item label="测试接口路径">
                <a-input v-model:value="apiUrl" placeholder="/api/system/user" />
              </a-form-item>
              <a-form-item label="测试 JSON 数据">
                <a-textarea v-model:value="testJson" :rows="10" />
              </a-form-item>
              <a-button type="primary" :loading="testing" block @click="sendTestRequest">
                发送测试请求
              </a-button>
            </a-form>
          </a-card>
        </a-col>

        <a-col :span="12">
          <a-card title="测试结果" class="result-card">
            <div v-if="testResult">
              <a-descriptions :column="1" bordered size="small">
                <a-descriptions-item label="状态码">
                  <a-tag :color="testResult.status === 200 ? 'success' : 'error'">
                    {{ testResult.status }}
                  </a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="响应耗时">
                  {{ testResult.duration }} ms
                </a-descriptions-item>
              </a-descriptions>
              
              <div class="json-result">
                <h4>响应 Body:</h4>
                <pre>{{ formatJson(testResult.data) }}</pre>
              </div>
              
              <div v-if="testResult.error" class="error-result">
                <h4>错误详情:</h4>
                <a-alert :message="testResult.error" type="error" show-icon />
              </div>
            </div>
            <a-empty v-else description="暂无测试结果" />
          </a-card>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { ApiOutlined, LinkOutlined } from '@ant-design/icons-vue'
import axios from 'axios'

const apiUrl = ref('/api/system/user')
const testJson = ref(JSON.stringify({
  username: '',
  nickName: '测试用户',
  email: 'invalid-email',
  phone: '123'
}, null, 2))

const testing = ref(false)
const testResult = ref<any>(null)
const swaggerUrl = 'http://localhost:8080/swagger-ui/index.html'

// 发送测试请求
const sendTestRequest = async () => {
  testing.value = true
  const startTime = Date.now()
  try {
    const data = JSON.parse(testJson.value)
    const response = await axios.post(apiUrl.value, data)
    const duration = Date.now() - startTime
    
    testResult.value = {
      status: response.status,
      data: response.data,
      duration
    }
    message.success('请求发送成功')
  } catch (error: any) {
    const duration = Date.now() - startTime
    testResult.value = {
      status: error.response?.status || 0,
      data: error.response?.data || null,
      error: error.message,
      duration
    }
    
    message.error(`请求失败: ${error.message}`)
  } finally {
    testing.value = false
  }
}

// 测试后端连接
const testBackendConnection = async () => {
  try {
    const response = await axios.get('http://localhost:8080/actuator/health', {
      timeout: 5000
    })
    message.success(`后端服务正常运行: ${response.data.status}`)
  } catch (error: any) {
    message.error(`后端服务连接失败: ${error.message}`)
  }
}

// 打开Swagger文档
const openSwaggerDoc = () => {
  window.open(swaggerUrl, '_blank')
}

// 格式化JSON显示
const formatJson = (data: any) => {
  if (typeof data === 'string') {
    try {
      return JSON.stringify(JSON.parse(data), null, 2)
    } catch {
      return data
    }
  }
  return JSON.stringify(data, null, 2)
}
</script>

<style scoped>
.api-test-page {
  padding: 20px;
}

:deep(.ant-card) {
  border-radius: 8px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

:deep(.ant-btn-primary) {
  background-color: #1890ff;
  border-color: #1890ff;
}

pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  background-color: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
}
</style>