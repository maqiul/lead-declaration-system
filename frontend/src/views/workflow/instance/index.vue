<template>
  <div class="process-instance">
    <!-- 搜索区域 -->
    <a-card class="search-card">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="流程名称">
          <a-input v-model:value="searchForm.processName" placeholder="请输入流程名称" />
        </a-form-item>
        <a-form-item label="发起人">
          <a-input v-model:value="searchForm.starterName" placeholder="请输入发起人" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchForm.status" style="width: 120px" placeholder="请选择状态">
            <a-select-option :value="0">运行中</a-select-option>
            <a-select-option :value="1">已完成</a-select-option>
            <a-select-option :value="2">已终止</a-select-option>
            <a-select-option :value="3">已挂起</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSearch">搜索</a-button>
          <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 表格区域 -->
    <a-card>
      <a-table
        :dataSource="tableData"
        :columns="columns"
        :pagination="pagination"
        :loading="loading"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag v-if="record.status === 0" color="blue">运行中</a-tag>
            <a-tag v-else-if="record.status === 1" color="green">已完成</a-tag>
            <a-tag v-else-if="record.status === 2" color="red">已终止</a-tag>
            <a-tag v-else color="orange">已挂起</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record as ProcessInstance)">查看</a-button>
              <a-button 
                v-if="record.status === 0" 
                type="link" 
                size="small" 
                @click="handleSuspend(record.id)"
              >
                挂起
              </a-button>
              <a-button 
                v-if="record.status === 3" 
                type="link" 
                size="small" 
                @click="handleActivate(record.id)"
              >
                激活
              </a-button>
              <a-popconfirm
                v-if="record.status === 0"
                title="确定要终止这个流程实例吗？"
                @confirm="handleTerminate(record.id)"
              >
                <a-button type="link" size="small" danger>终止</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 流程实例详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="流程实例详情"
      width="900px"
      :footer="null"
    >
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="basic" tab="基本信息">
          <a-descriptions :column="2" bordered>
            <a-descriptions-item label="流程实例ID">{{ currentInstance?.instanceId }}</a-descriptions-item>
            <a-descriptions-item label="流程名称">{{ currentInstance?.processName }}</a-descriptions-item>
            <a-descriptions-item label="流程KEY">{{ currentInstance?.processKey }}</a-descriptions-item>
            <a-descriptions-item label="业务KEY">{{ currentInstance?.businessKey }}</a-descriptions-item>
            <a-descriptions-item label="发起人">{{ currentInstance?.starterName }}</a-descriptions-item>
            <a-descriptions-item label="当前节点">{{ currentInstance?.currentActivityName }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag v-if="currentInstance?.status === 0" color="blue">运行中</a-tag>
              <a-tag v-else-if="currentInstance?.status === 1" color="green">已完成</a-tag>
              <a-tag v-else-if="currentInstance?.status === 2" color="red">已终止</a-tag>
              <a-tag v-else color="orange">已挂起</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="开始时间">{{ currentInstance?.startTime }}</a-descriptions-item>
            <a-descriptions-item label="结束时间" v-if="currentInstance?.endTime">
              {{ currentInstance?.endTime }}
            </a-descriptions-item>
          </a-descriptions>
        </a-tab-pane>
        
        <a-tab-pane key="tasks" tab="任务列表">
          <a-table
            :dataSource="taskData"
            :columns="taskColumns"
            :pagination="false"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag v-if="record.status === 0" color="blue">待办</a-tag>
                <a-tag v-else-if="record.status === 1" color="green">已办</a-tag>
                <a-tag v-else color="orange">其他</a-tag>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        
        <a-tab-pane key="diagram" tab="流程图">
          <div class="process-diagram">
            <a-empty description="流程图展示区域" />
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getRunningProcessInstances, suspendProcessInstance, activateProcessInstance, terminateProcessInstance } from '@/api/workflow'

// 类型定义
interface ProcessInstance {
  id: number
  instanceId: string
  processKey: string
  processName: string
  businessKey: string
  starterName: string
  currentActivityName: string
  status: number
  startTime: string
  endTime: string | null
}

interface Task {
  taskId: string
  taskName: string
  assigneeName: string
  status: number
  createTime: string
  endTime: string | null
}

interface SearchForm {
  processName?: string
  starterName?: string
  status?: number
}

// 响应式数据
const loading = ref(false)
const tableData = ref<ProcessInstance[]>([])
const activeTab = ref('basic')
const detailVisible = ref(false)
const currentInstance = ref<ProcessInstance | null>(null)
const taskData = ref<Task[]>([])

// 搜索表单
const searchForm = reactive<SearchForm>({
  processName: '',
  starterName: '',
  status: undefined
})

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条记录`
})

// 表格列配置
const columns = [
  {
    title: '流程实例ID',
    dataIndex: 'instanceId',
    key: 'instanceId',
    width: 200
  },
  {
    title: '流程名称',
    dataIndex: 'processName',
    key: 'processName'
  },
  {
    title: '业务KEY',
    dataIndex: 'businessKey',
    key: 'businessKey'
  },
  {
    title: '发起人',
    dataIndex: 'starterName',
    key: 'starterName'
  },
  {
    title: '当前节点',
    dataIndex: 'currentActivityName',
    key: 'currentActivityName'
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100
  },
  {
    title: '开始时间',
    dataIndex: 'startTime',
    key: 'startTime',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right' as const,
    width: 200
  }
]

// 任务表格列配置
const taskColumns = [
  {
    title: '任务ID',
    dataIndex: 'taskId',
    key: 'taskId',
    width: 200
  },
  {
    title: '任务名称',
    dataIndex: 'taskName',
    key: 'taskName'
  },
  {
    title: '办理人',
    dataIndex: 'assigneeName',
    key: 'assigneeName'
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180
  }
]

// 方法
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      processName: searchForm.processName,
      starterName: searchForm.starterName,
      status: searchForm.status
    }
    
    const response = await getRunningProcessInstances()
    if (response.data?.code === 200) {
      tableData.value = response.data.data.records
      pagination.total = Number(response.data.data.total) || 0
    } else {
      message.error(response.data?.message || '加载数据失败')
    }
  } catch (error) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadTaskData = async (_instanceId: string) => {
  // 模拟加载任务数据
  taskData.value = [
    {
      taskId: 'task_001',
      taskName: '部门经理审批',
      assigneeName: '王经理',
      status: 1,
      createTime: '2026-03-13 10:00:00',
      endTime: '2026-03-13 10:30:00'
    },
    {
      taskId: 'task_002',
      taskName: '人事审批',
      assigneeName: '赵人事',
      status: 0,
      createTime: '2026-03-13 10:30:00',
      endTime: null
    }
  ]
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  searchForm.processName = ''
  searchForm.starterName = ''
  searchForm.status = undefined
  handleSearch()
}

const handleView = (record: ProcessInstance) => {
  currentInstance.value = record
  loadTaskData(record.instanceId)
  detailVisible.value = true
  activeTab.value = 'basic'
}

const handleSuspend = async (instanceId: string) => {
  try {
    const response = await suspendProcessInstance(instanceId)
    if (response.data?.code === 200) {
      message.success('流程实例挂起成功')
      loadData()
    } else {
      message.error(response.data?.message || '挂起失败')
    }
  } catch (error) {
    message.error('挂起失败')
  }
}

const handleActivate = async (instanceId: string) => {
  try {
    const response = await activateProcessInstance(instanceId)
    if (response.data?.code === 200) {
      message.success('流程实例激活成功')
      loadData()
    } else {
      message.error(response.data?.message || '激活失败')
    }
  } catch (error) {
    message.error('激活失败')
  }
}

const handleTerminate = async (instanceId: string) => {
  try {
    const response = await terminateProcessInstance(instanceId, '用户手动终止')
    if (response.data?.code === 200) {
      message.success('流程实例终止成功')
      loadData()
    } else {
      message.error(response.data?.message || '终止失败')
    }
  } catch (error) {
    message.error('终止失败')
  }
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

// 生命周期
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.process-instance {
  padding: 0;
  background: transparent;
  min-height: 100%;
  box-sizing: border-box;
  overflow-x: hidden;
}

.search-card {
  margin-bottom: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

.process-diagram {
  height: 400px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fafafa 0%, #f0f0f0 100%);
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
}

:deep(.ant-card) {
  border-radius: 8px;
}

:deep(.ant-card-body) {
  padding: 24px;
}

:deep(.ant-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 600;
}

:deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

:deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
}

:deep(.ant-tabs) {
  background: white;
  border-radius: 8px;
  padding: 16px;
}
</style>