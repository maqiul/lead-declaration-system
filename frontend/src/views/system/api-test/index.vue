<template>
  <div class="api-test-page">
    <a-card title="后端API连接测试" style="margin: 24px;">
      <a-space direction="vertical" style="width: 100%;">
        <!-- 后端文档链接 -->
        <a-alert
          message="后端Swagger文档"
          :description="swaggerUrl"
          type="info"
          show-icon
        >
          <template #action>
            <a-button type="primary" @click="openSwaggerDoc">
              <template #icon><LinkOutlined /></template>
              打开文档
            </a-button>
          </template>
        </a-alert>

        <!-- 连接测试 -->
        <a-card title="API连接测试">
          <a-form :model="testForm" layout="vertical">
            <a-form-item label="测试端点">
              <a-input v-model:value="testForm.endpoint" placeholder="/user/login" />
            </a-form-item>
            <a-form-item label="请求方法">
              <a-select v-model:value="testForm.method" style="width: 120px">
                <a-select-option value="GET">GET</a-select-option>
                <a-select-option value="POST">POST</a-select-option>
                <a-select-option value="PUT">PUT</a-select-option>
                <a-select-option value="DELETE">DELETE</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="请求数据 (JSON)">
              <a-textarea 
                v-model:value="testForm.data" 
                :rows="4" 
                placeholder='{"username": "admin", "password": "admin"}'
              />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="testApi" :loading="testing">
                <template #icon><ApiOutlined /></template>
                发送测试请求
              </a-button>
              <a-button style="margin-left: 8px" @click="testBackendConnection">
                <template #icon><WifiOutlined /></template>
                测试后端连接
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>

        <!-- 测试结果 -->
        <a-card title="测试结果" v-if="testResult">
          <a-descriptions bordered :column="1">
            <a-descriptions-item label="状态码">
              <a-tag :color="testResult.status >= 200 && testResult.status < 300 ? 'green' : 'red'">
                {{ testResult.status }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="响应时间">
              <a-tag color="blue">{{ testResult.duration }}ms</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="响应数据">
              <pre style="background: #f5f5f5; padding: 12px; border-radius: 4px; max-height: 300px; overflow: auto;">
{{ formatJson(testResult.data) }}
              </pre>
            </a-descriptions-item>
            <a-descriptions-item label="错误信息" v-if="testResult.error">
              <a-tag color="red">{{ testResult.error }}</a-tag>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>

        <!-- 快速测试按钮 -->
        <a-card title="快速测试">
          <a-space wrap>
            <a-button @click="quickTest('/user/login', 'POST', {username: 'admin', password: 'admin'})">
              登录接口测试
            </a-button>
            <a-button @click="quickTest('/user/info', 'GET')">
              用户信息接口测试
            </a-button>
            <a-button @click="quickTest('/system/user/list?pageNum=1&pageSize=10', 'GET')">
              用户列表接口测试
            </a-button>
            <a-button @click="quickTest('/workflow/definition/list?pageNum=1&pageSize=10', 'GET')">
              流程定义接口测试
            </a-button>
          </a-space>
        </a-card>
      </a-space>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { LinkOutlined, ApiOutlined, WifiOutlined } from '@ant-design/icons-vue'
import axios from 'axios'

const swaggerUrl = 'http://localhost:8080/doc.html#/SwaggerModels/-v3-api-docs'

// 测试表单
const testForm = reactive({
  endpoint: '/user/login',
  method: 'POST',
  data: '{\n  "username": "admin",\n  "password": "admin"\n}'
})

// 测试状态
const testing = ref(false)
const testResult = ref<any>(null)

// 快速测试方法
const quickTest = (endpoint: string, method: string, data?: any) => {
  testForm.endpoint = endpoint
  testForm.method = method
  if (data) {
    testForm.data = JSON.stringify(data, null, 2)
  }
  testApi()
}

// 测试API
const testApi = async () => {
  testing.value = true
  const startTime = Date.now()
  
  try {
    let requestData: any = {}
    
    // 解析请求数据
    if (testForm.data.trim()) {
      try {
        requestData = JSON.parse(testForm.data)
      } catch (e) {
        throw new Error('请求数据格式不正确，请输入有效的JSON')
      }
    }
    
    // 构造请求配置
    const config: any = {
      method: testForm.method.toLowerCase(),
      url: `/api${testForm.endpoint}`,
      timeout: 10000
    }
    
    if (testForm.method === 'GET') {
      config.params = requestData
    } else {
      config.data = requestData
    }
    
    // 发送请求
    const response = await axios(config)
    
    testResult.value = {
      status: response.status,
      data: response.data,
      duration: Date.now() - startTime
    }
    
    message.success('请求成功')
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
  min-height: 100vh;
  background-color: #f0f2f5;
}

pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>