<template>
  <div class="task-list">
    <!-- 搜索区域 -->
    <a-card class="search-card" size="small">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="任务名称">
          <a-input v-model:value="searchForm.taskName" placeholder="请输入任务名称" />
        </a-form-item>
        <a-form-item label="流程名称">
          <a-input v-model:value="searchForm.processName" placeholder="请输入流程名称" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch" v-permission="['workflow:task:view']">搜索</a-button>
            <a-button @click="handleReset" v-permission="['workflow:task:view']">重置</a-button>
            <a-button @click="$emit('refresh')" v-permission="['workflow:task:view']">
              <template #icon><reload-outlined /></template>
              刷新
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 任务表格 -->
    <a-card :bordered="false">
      <a-table
        :dataSource="tasks"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        row-key="taskId"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'priority'">
            <a-tag v-if="record.priority >= 80" color="red">高</a-tag>
            <a-tag v-else-if="record.priority >= 50" color="orange">中</a-tag>
            <a-tag v-else color="green">低</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record as Task)" v-permission="['workflow:task:view']">查看</a-button>

              <template v-if="type === 'candidate'">
                <a-button type="link" size="small" @click="$emit('claim', record as Task)" v-permission="['workflow:task:claim']">
                  签收
                </a-button>
              </template>

              <template v-else-if="type === 'assigned'">
                <a-button type="link" size="small" @click="$emit('complete', record as Task)" v-permission="['workflow:task:complete']">
                  处理
                </a-button>
                <a-button type="link" size="small" @click="$emit('reject', record as Task)" v-permission="['workflow:task:reject']" danger>
                  驳回
                </a-button>
                <a-button type="link" size="small" @click="handleTransfer(record as Task)" v-permission="['workflow:task:transfer']">
                  转办
                </a-button>
              </template>

              <template v-else>
                <a-button type="link" size="small" @click="handleViewProcess(record as Task)" v-permission="['workflow:instance:view']">
                  查看流程
                </a-button>
              </template>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 转办弹窗 -->
    <a-modal
      v-model:open="transferVisible"
      title="任务转办"
      :confirm-loading="transferLoading"
      @ok="handleTransferOk"
      @cancel="handleTransferCancel"
    >
      <a-form
        ref="transferFormRef"
        :model="transferForm"
        layout="vertical"
      >
        <a-form-item label="转办给" name="assigneeId">
          <a-select 
            v-model:value="transferForm.assigneeId" 
            placeholder="请选择转办人员"
            :options="userOptions"
          />
        </a-form-item>
        <a-form-item label="转办原因" name="reason">
          <a-textarea v-model:value="transferForm.reason" placeholder="请输入转办原因" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { transferTask } from '@/api/workflow'

// 类型定义
interface Task {
  taskId: string
  taskName: string
  processName: string
  processInstanceId: string
  starterName: string
  priority: number
  createTime: string
  dueTime: string | null
  claimTime: string | null
  activityId: string
  businessKey: string
}

interface SearchForm {
  taskName?: string
  processName?: string
}

// Props定义
const props = defineProps<{
  tasks: Task[]
  loading: boolean
  total: number
  pageNum: number
  pageSize: number
  type?: 'assigned' | 'completed' | 'candidate'
}>()

// Emits定义
const emit = defineEmits<{
  (e: 'refresh'): void
  (e: 'claim', task: Task): void
  (e: 'complete', task: Task): void
  (e: 'reject', task: Task): void
  (e: 'change', pagination: { current: number, pageSize: number }): void
  (e: 'search', params: SearchForm): void
  (e: 'view', task: Task): void
  (e: 'viewProcess', task: Task): void
}>()

// 响应式数据
const searchForm = reactive<SearchForm>({
  taskName: '',
  processName: ''
})

const pagination = reactive({
  current: props.pageNum,
  pageSize: props.pageSize,
  total: props.total,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条记录`
})

// 监听 props 变化更新分页
import { watch } from 'vue'
watch(() => props.total, (val) => {
  pagination.total = val
})
watch(() => props.pageNum, (val) => {
  pagination.current = val
})
watch(() => props.pageSize, (val) => {
  pagination.pageSize = val
})

// 转办相关
const transferVisible = ref(false)
const transferLoading = ref(false)
const transferFormRef = ref()
const currentTask = ref<Task | null>(null)

const transferForm = reactive({
  assigneeId: undefined as number | undefined,
  reason: ''
})

/*
const transferRules = {
  assigneeId: [
    { required: true, message: '请选择转办人员', trigger: 'change' }
  ],
  reason: [
    { required: true, message: '请输入转办原因', trigger: 'blur' }
  ]
}
*/

// 用户选项
const userOptions = ref([])

// 表格列配置
const columns = [
  {
    title: '任务名称',
    dataIndex: 'taskName',
    key: 'taskName',
    width: 150
  },
  {
    title: '流程名称',
    dataIndex: 'processName',
    key: 'processName',
    width: 150
  },
  {
    title: '发起人',
    dataIndex: 'starterName',
    key: 'starterName',
    width: 100
  },
  {
    title: '优先级',
    dataIndex: 'priority',
    key: 'priority',
    width: 80
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180
  },
  {
    title: '到期时间',
    dataIndex: 'dueTime',
    key: 'dueTime',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right' as const,
    width: 150
  }
]

// 如果是已完成任务，移除操作列
if (props.type === 'completed') {
  columns.pop()
  // 添加完成时间列
  columns.push({
    title: '完成时间',
    dataIndex: 'endTime',
    key: 'endTime',
    width: 180
  })
}

// 方法
const handleSearch = () => {
  pagination.current = 1
  emit('search', { ...searchForm })
}

const handleReset = () => {
  searchForm.taskName = ''
  searchForm.processName = ''
  handleSearch()
}

const handleView = (record: Task) => {
  emit('view', record)
}

const handleViewProcess = (record: Task) => {
  message.info(`查看流程: ${record.processName}`)
  // 这里可以跳转到流程实例详情
}

const handleTransfer = (record: Task) => {
  currentTask.value = record
  Object.assign(transferForm, {
    assigneeId: undefined,
    reason: ''
  })
  transferVisible.value = true
}

const handleTransferOk = async () => {
  try {
    await transferFormRef.value?.validateFields()
    
    if (!currentTask.value?.taskId || !transferForm.assigneeId) {
      message.error('请选择转办人员')
      return
    }
    
    transferLoading.value = true
    
    const response = await transferTask(currentTask.value.taskId, transferForm.assigneeId)
    if (response.data?.code === 200) {
      message.success('任务转办成功')
      transferVisible.value = false
      emit('refresh')
    } else {
      message.error(response.data?.message || '转办失败')
    }
  } catch (error: any) {
    message.error(error?.message || '转办失败')
  } finally {
    transferLoading.value = false
  }
}

const handleTransferCancel = () => {
  transferVisible.value = false
  transferFormRef.value?.resetFields()
  currentTask.value = null
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  emit('change', { current: pag.current, pageSize: pag.pageSize })
}
</script>

<style scoped>
.task-list {
  padding: 0;
}
</style>