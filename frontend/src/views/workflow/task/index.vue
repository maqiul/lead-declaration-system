<template>
  <div class="my-task">
    <!-- 任务分类标签页 -->
    <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
      <a-tab-pane key="assigned" tab="待办任务">
        <task-list 
          :tasks="assignedTasks" 
          :loading="loading" 
          @refresh="loadAssignedTasks"
          @claim="handleClaim"
          @complete="handleComplete"
        />
      </a-tab-pane>
      <a-tab-pane key="completed" tab="已办任务">
        <task-list 
          :tasks="completedTasks" 
          :loading="loading" 
          type="completed"
          @refresh="loadCompletedTasks"
        />
      </a-tab-pane>
      <a-tab-pane key="candidate" tab="候选任务">
        <task-list 
          :tasks="candidateTasks" 
          :loading="loading" 
          type="candidate"
          @refresh="loadCandidateTasks"
          @claim="handleClaim"
        />
      </a-tab-pane>
    </a-tabs>

    <!-- 任务处理弹窗 -->
    <a-modal
      v-model:open="taskModalVisible"
      :title="modalTitle"
      :confirm-loading="modalLoading"
      @ok="handleTaskModalOk"
      @cancel="handleTaskModalCancel"
      width="600px"
    >
      <div v-if="currentTask">
        <a-descriptions :column="2" bordered size="small" style="margin-bottom: 24px">
          <a-descriptions-item label="任务名称">{{ currentTask.taskName }}</a-descriptions-item>
          <a-descriptions-item label="流程名称">{{ currentTask.processName }}</a-descriptions-item>
          <a-descriptions-item label="发起人">{{ currentTask.starterName }}</a-descriptions-item>
          <a-descriptions-item label="优先级">
            <a-tag v-if="currentTask.priority >= 80" color="red">高</a-tag>
            <a-tag v-else-if="currentTask.priority >= 50" color="orange">中</a-tag>
            <a-tag v-else color="green">低</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ currentTask.createTime }}</a-descriptions-item>
          <a-descriptions-item label="到期时间" v-if="currentTask.dueTime">
            {{ currentTask.dueTime }}
          </a-descriptions-item>
        </a-descriptions>
        
        <a-form
          ref="taskFormRef"
          :model="taskFormData"
          layout="vertical"
        >
          <a-form-item label="处理意见" name="comment">
            <a-textarea 
              v-model:value="taskFormData.comment" 
              placeholder="请输入处理意见" 
              :rows="4"
            />
          </a-form-item>
          
          <template v-if="modalType === 'complete'">
            <a-form-item label="处理结果">
              <a-radio-group v-model:value="taskFormData.result">
                <a-radio value="approve">同意</a-radio>
                <a-radio value="reject">驳回</a-radio>
              </a-radio-group>
            </a-form-item>
            
            <a-form-item v-if="taskFormData.result === 'reject'" label="驳回原因">
              <a-input v-model:value="taskFormData.rejectReason" placeholder="请输入驳回原因" />
            </a-form-item>
          </template>
        </a-form>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import TaskList from './components/TaskList.vue'
import { getMyAssignedTasks, getMyCandidateTasks, claimTask, completeTask } from '@/api/workflow'

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
}

// 响应式数据
const activeTab = ref('assigned')
const loading = ref(false)
const assignedTasks = ref<Task[]>([])
const completedTasks = ref<Task[]>([])
const candidateTasks = ref<Task[]>([])

// 任务弹窗相关
const taskModalVisible = ref(false)
const modalLoading = ref(false)
const modalTitle = ref('')
const modalType = ref('')
const taskFormRef = ref()
const currentTask = ref<Task | null>(null)

// 任务表单数据
const taskFormData = reactive({
  comment: '',
  result: 'approve',
  rejectReason: ''
})

// 方法
const loadAssignedTasks = async () => {
  loading.value = true
  try {
    const response = await getMyAssignedTasks()
    if (response.data?.code === 200) {
      assignedTasks.value = response.data.data.map((item: any) => ({
        taskId: item.taskId,
        taskName: item.taskName,
        processName: item.processDefinitionName,
        processInstanceId: item.processInstanceId,
        starterName: item.starterName,
        priority: 80,
        createTime: item.createTime,
        dueTime: item.dueTime,
        claimTime: item.claimTime
      }))
    } else {
      message.error(response.data?.message || '加载待办任务失败')
    }
  } catch (error) {
    message.error('加载待办任务失败')
  } finally {
    loading.value = false
  }
}

const loadCompletedTasks = async () => {
  loading.value = true
  try {
    // 模拟已办任务数据
    completedTasks.value = [
      {
        taskId: 'task_003',
        taskName: '人事审批',
        processName: '入职流程',
        processInstanceId: 'instance_003',
        starterName: '王五',
        priority: 70,
        createTime: '2026-03-12 14:00:00',
        dueTime: '2026-03-13 17:00:00',
        claimTime: '2026-03-12 14:05:00'
      }
    ]
  } catch (error) {
    message.error('加载已办任务失败')
  } finally {
    loading.value = false
  }
}

const loadCandidateTasks = async () => {
  loading.value = true
  try {
    const response = await getMyCandidateTasks()
    if (response.data?.code === 200) {
      candidateTasks.value = response.data.data.map((item: any) => ({
        taskId: item.taskId,
        taskName: item.taskName,
        processName: item.processDefinitionName,
        processInstanceId: item.processInstanceId,
        starterName: item.starterName,
        priority: 90,
        createTime: item.createTime,
        dueTime: item.dueTime,
        claimTime: item.claimTime
      }))
    } else {
      message.error(response.data?.message || '加载候选任务失败')
    }
  } catch (error) {
    message.error('加载候选任务失败')
  } finally {
    loading.value = false
  }
}

const handleTabChange = (key: string) => {
  switch (key) {
    case 'assigned':
      loadAssignedTasks()
      break
    case 'completed':
      loadCompletedTasks()
      break
    case 'candidate':
      loadCandidateTasks()
      break
  }
}

const handleClaim = (task: Task) => {
  currentTask.value = task
  modalTitle.value = `签收任务 - ${task.taskName}`
  modalType.value = 'claim'
  taskFormData.comment = ''
  taskModalVisible.value = true
}

const handleComplete = (task: Task) => {
  currentTask.value = task
  modalTitle.value = `处理任务 - ${task.taskName}`
  modalType.value = 'complete'
  Object.assign(taskFormData, {
    comment: '',
    result: 'approve',
    rejectReason: ''
  })
  taskModalVisible.value = true
}

const handleTaskModalOk = async () => {
  try {
    modalLoading.value = true
    
    // 根据不同类型处理任务
    if (modalType.value === 'claim') {
      const response = await claimTask(currentTask.value?.taskId || '')
      if (response.data?.code === 200) {
        message.success('任务签收成功')
      } else {
        throw new Error(response.data?.message || '签收失败')
      }
    } else if (modalType.value === 'complete') {
      const response = await completeTask(currentTask.value?.taskId || '', { comment: taskFormData.comment })
      if (response.data?.code === 200) {
        message.success('任务处理成功')
      } else {
        throw new Error(response.data?.message || '处理失败')
      }
    }
    
    taskModalVisible.value = false
    modalLoading.value = false
    
    // 刷新当前标签页数据
    handleTabChange(activeTab.value)
  } catch (error) {
    modalLoading.value = false
    message.error('操作失败')
  }
}

const handleTaskModalCancel = () => {
  taskModalVisible.value = false
  taskFormRef.value?.resetFields()
  currentTask.value = null
}

// 生命周期
onMounted(() => {
  loadAssignedTasks()
})
</script>

<style scoped>
.my-task {
  padding: 0;
  background: transparent;
  min-height: 100%;
  box-sizing: border-box;
  overflow-x: hidden;
}

:deep(.ant-tabs-content) {
  padding-top: 16px;
}
</style>