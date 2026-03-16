# Flowable 与前端页面集成指南

## 🎯 概述

本文档详细介绍如何将 Flowable 工作流引擎与前端页面完美结合，实现完整的可视化工作流管理系统。

## 🏗️ 系统架构

### 整体架构图
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Vue 3 前端    │◄──►│  Spring Boot    │◄──►│   Flowable      │
│  (TypeScript)   │    │     后端        │    │   工作流引擎    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
       │                       │                       │
       ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  流程设计器     │    │  流程定义管理   │    │  流程实例执行   │
│  流程监控大屏   │    │  流程实例监控   │    │  任务调度引擎   │
│  任务处理中心   │    │  任务分配管理   │    │  历史数据存储   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🔗 集成方式详解

### 1. RESTful API 对接

#### 后端 Controller 层
```java
@RestController
@RequestMapping("/workflow")
public class WorkflowController {
    
    // 流程定义相关
    @PostMapping("/deploy")                    // 部署流程
    @GetMapping("/definitions")                // 获取流程定义列表
    
    // 流程实例相关
    @PostMapping("/instance/start")            // 启动流程实例
    @GetMapping("/instances/running")          // 获取运行中实例
    @PostMapping("/instance/suspend/{id}")     // 挂起实例
    @PostMapping("/instance/activate/{id}")    // 激活实例
    @PostMapping("/instance/terminate/{id}")   // 终止实例
    
    // 任务相关
    @GetMapping("/tasks/assigned")             // 我的待办任务
    @GetMapping("/tasks/candidate")            // 我的候选任务
    @PostMapping("/task/claim/{id}")           // 签收任务
    @PostMapping("/task/complete/{id}")        // 完成任务
}
```

#### 前端 API 封装
```typescript
// src/api/workflow/index.ts
import request from '@/utils/request'

// 流程定义
export function getProcessDefinitions() {
  return request({
    url: '/workflow/definitions',
    method: 'get'
  })
}

export function deployProcess(file: File, processInfo: any) {
  const formData = new FormData()
  formData.append('file', file)
  // ... 其他参数
  return request({
    url: '/workflow/deploy',
    method: 'post',
    data: formData
  })
}

// 流程实例
export function startProcessInstance(key: string, businessKey?: string) {
  return request({
    url: '/workflow/instance/start',
    method: 'post',
    params: { processDefinitionKey: key, businessKey }
  })
}

// 任务管理
export function getMyAssignedTasks() {
  return request({
    url: '/workflow/tasks/assigned',
    method: 'get'
  })
}
```

### 2. 数据模型映射

#### 后端实体类
```java
// 流程定义实体
@Data
public class ProcessDefinition {
    private Long id;
    private String processKey;
    private String processName;
    private String category;
    private Integer version;
    private Integer status;
    private String bpmnXml;
}

// 流程实例实体
@Data
public class ProcessInstance {
    private Long id;
    private String instanceId;
    private String processDefinitionId;
    private String businessKey;
    private Integer status;
    private Date startTime;
}

// 任务实例实体
@Data
public class TaskInstance {
    private Long id;
    private String taskId;
    private String taskName;
    private String assigneeId;
    private Integer status;
    private Date createTime;
}
```

#### 前端 TypeScript 接口
```typescript
// 流程定义接口
export interface ProcessDefinition {
  id: number;
  processKey: string;
  processName: string;
  category: string;
  version: number;
  status: number;
  bpmnXml?: string;
}

// 流程实例接口
export interface ProcessInstance {
  id: number;
  instanceId: string;
  processDefinitionId: string;
  businessKey: string;
  status: number;
  startTime: string;
}

// 任务接口
export interface TaskInstance {
  id: number;
  taskId: string;
  taskName: string;
  assigneeId: number;
  status: number;
  createTime: string;
}
```

### 3. 页面组件集成

#### 流程设计器页面
```vue
<!-- src/views/workflow/modeler/index.vue -->
<template>
  <div class="workflow-modeler">
    <!-- 工具栏 -->
    <div class="toolbar">
      <a-button @click="saveModel">保存</a-button>
      <a-button @click="deployModel">部署</a-button>
    </div>
    
    <!-- 画布区域 -->
    <div class="canvas-container">
      <div 
        ref="canvas" 
        @drop="handleDrop"
        @dragover="handleDragOver"
      >
        <!-- 流程元素 -->
        <div 
          v-for="element in elements" 
          :key="element.id"
          :style="getElementStyle(element)"
          @click="selectElement(element)"
        >
          {{ element.name }}
        </div>
      </div>
    </div>
    
    <!-- 属性面板 -->
    <div class="properties-panel" v-if="selectedElement">
      <a-form>
        <a-form-item label="名称">
          <a-input v-model:value="selectedElement.name" />
        </a-form-item>
        <a-form-item label="指派给">
          <a-select v-model:value="selectedElement.assignee">
            <a-select-option value="user1">用户1</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { deployProcess } from '@/api/workflow'

const elements = ref([])
const selectedElement = ref(null)

const deployModel = async () => {
  try {
    const response = await deployProcess(bpmnFile, processInfo)
    if (response.data?.code === 200) {
      message.success('部署成功')
    }
  } catch (error) {
    message.error('部署失败')
  }
}
</script>
```

#### 流程监控页面
```vue
<!-- src/views/workflow/monitor/index.vue -->
<template>
  <div class="workflow-monitor">
    <!-- 统计卡片 -->
    <a-row :gutter="16">
      <a-col :span="6">
        <a-card>
          <div class="stat-item">
            <h3>{{ stats.totalProcesses }}</h3>
            <p>总流程数</p>
          </div>
        </a-card>
      </a-col>
      <!-- 其他统计项 -->
    </a-row>
    
    <!-- 实例列表 -->
    <a-table 
      :data-source="instances"
      :columns="instanceColumns"
      :loading="loading"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="getStatusColor(record.status)">
            {{ getStatusText(record.status) }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button @click="viewInstanceDetail(record)">查看详情</a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>
```

## 🔄 业务流程集成

### 1. 流程启动集成
```typescript
// 在业务页面中启动流程
const startBusinessProcess = async (businessData: any) => {
  try {
    // 1. 保存业务数据
    const businessResponse = await saveBusinessData(businessData)
    
    // 2. 启动对应的流程实例
    const processResponse = await startProcessInstance(
      'leave_approval_process',  // 流程定义KEY
      businessResponse.data.id   // 业务KEY
    )
    
    // 3. 关联业务数据和流程实例
    await linkBusinessWithProcess(
      businessResponse.data.id,
      processResponse.data.instanceId
    )
    
    message.success('流程启动成功')
  } catch (error) {
    message.error('流程启动失败')
  }
}
```

### 2. 任务处理集成
```typescript
// 任务处理页面
const completeTask = async (taskId: string, formData: any) => {
  try {
    // 1. 完成任务
    await completeTaskApi(taskId, {
      approved: formData.approved,
      comment: formData.comment
    })
    
    // 2. 更新业务状态
    await updateBusinessStatus(formData.businessId, 
      formData.approved ? 'approved' : 'rejected')
    
    message.success('任务处理完成')
    router.push('/workflow/tasks')
  } catch (error) {
    message.error('任务处理失败')
  }
}
```

### 3. 权限控制集成
```typescript
// 基于流程的角色权限控制
const checkProcessPermission = (processKey: string, action: string) => {
  const currentUser = getCurrentUser()
  const userRoles = currentUser.roles
  
  // 根据流程和动作检查权限
  return hasPermission(userRoles, processKey, action)
}

// 在页面中使用
onMounted(() => {
  if (!checkProcessPermission('leave_process', 'start')) {
    message.error('您没有启动此流程的权限')
    router.back()
  }
})
```

## 📊 数据同步机制

### 1. 实时数据推送
```typescript
// WebSocket 连接
const connectWebSocket = () => {
  const ws = new WebSocket('ws://localhost:8080/websocket')
  
  ws.onmessage = (event) => {
    const data = JSON.parse(event.data)
    
    // 根据消息类型更新相应数据
    switch (data.type) {
      case 'PROCESS_INSTANCE_UPDATE':
        updateProcessInstance(data.payload)
        break
      case 'TASK_ASSIGNED':
        updateTaskList(data.payload)
        break
    }
  }
}
```

### 2. 轮询机制
```typescript
// 定时轮询更新数据
const startPolling = () => {
  setInterval(async () => {
    try {
      const response = await getRunningProcessInstances()
      instances.value = response.data.data
    } catch (error) {
      console.error('轮询更新失败:', error)
    }
  }, 5000) // 每5秒更新一次
}
```

## 🔧 最佳实践

### 1. 错误处理
```typescript
// 统一错误处理
const handleWorkflowError = (error: any) => {
  if (error.response?.status === 401) {
    message.error('请先登录')
    router.push('/login')
  } else if (error.response?.status === 403) {
    message.error('权限不足')
  } else {
    message.error(error.message || '操作失败')
  }
}
```

### 2. 加载状态管理
```typescript
// Loading 状态控制
const loading = ref({
  processList: false,
  taskList: false,
  deploying: false
})

const loadProcessList = async () => {
  loading.value.processList = true
  try {
    const response = await getProcessDefinitions()
    processList.value = response.data.data
  } finally {
    loading.value.processList = false
  }
}
```

### 3. 数据缓存
```typescript
// 简单的数据缓存机制
const cache = new Map()

const getCachedData = async (key: string, apiCall: Function) => {
  if (cache.has(key) && Date.now() - cache.get(key).timestamp < 300000) {
    return cache.get(key).data
  }
  
  const data = await apiCall()
  cache.set(key, {
    data,
    timestamp: Date.now()
  })
  
  return data
}
```

## 🛠️ 开发调试

### 1. API 调试
```typescript
// 开发环境下打印 API 请求日志
if (import.meta.env.DEV) {
  axios.interceptors.request.use(config => {
    console.log('🚀 API Request:', config.url, config.data)
    return config
  })
  
  axios.interceptors.response.use(response => {
    console.log('✅ API Response:', response.config.url, response.data)
    return response
  })
}
```

### 2. 流程测试
```typescript
// 流程测试工具
const testProcessFlow = async (processKey: string) => {
  // 1. 部署测试流程
  await deployTestProcess(processKey)
  
  // 2. 启动流程实例
  const instance = await startProcessInstance(processKey)
  
  // 3. 模拟任务处理
  const tasks = await getProcessTasks(instance.instanceId)
  for (const task of tasks) {
    await completeTask(task.taskId, { approved: true })
  }
  
  console.log('✅ 流程测试完成')
}
```

## 📈 性能优化

### 1. 虚拟滚动
```vue
<!-- 大量数据的虚拟滚动 -->
<virtual-list
  :data="processInstances"
  :item-height="60"
  :height="400"
>
  <template #default="{ item }">
    <process-instance-item :instance="item" />
  </template>
</virtual-list>
```

### 2. 懒加载
```typescript
// 组件懒加载
const ProcessDesigner = defineAsyncComponent(() => 
  import('@/views/workflow/modeler/index.vue')
)
```

## 🔒 安全考虑

### 1. CSRF 防护
```typescript
// 请求头添加 CSRF token
axios.defaults.headers.common['X-CSRF-TOKEN'] = getCsrfToken()
```

### 2. 数据校验
```typescript
// 前端数据校验
const validateProcessData = (data: any) => {
  const schema = {
    processKey: { type: 'string', required: true },
    processName: { type: 'string', required: true, maxLength: 100 }
  }
  
  return validate(data, schema)
}
```

## 🎨 用户体验优化

### 1. 操作反馈
```typescript
// 加载状态提示
const handleProcessAction = async (action: string, params: any) => {
  const loadingMsg = message.loading(`${action}中...`, 0)
  
  try {
    await performAction(action, params)
    message.success(`${action}成功`)
  } catch (error) {
    message.error(`${action}失败: ${error.message}`)
  } finally {
    loadingMsg()
  }
}
```

### 2. 快捷键支持
```typescript
// 键盘快捷键
const setupShortcuts = () => {
  document.addEventListener('keydown', (e) => {
    if (e.ctrlKey && e.key === 's') {
      e.preventDefault()
      saveCurrentModel()
    }
  })
}
```

这个集成方案提供了完整的 Flowable 与前端页面结合的最佳实践，涵盖了从基础 API 对接到高级功能实现的所有方面。