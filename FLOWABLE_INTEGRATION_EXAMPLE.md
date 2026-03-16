# Flowable 前端集成实战示例

## 🎯 集成场景演示

以下是一个完整的 Flowable 与前端页面集成的实战示例，展示了如何在业务系统中无缝集成工作流功能。

## 📋 示例场景：请假审批流程

### 1. 业务页面集成

```vue
<!-- src/views/business/leave-application/index.vue -->
<template>
  <div class="leave-application">
    <a-card title="请假申请">
      <a-form 
        :model="formState" 
        :rules="rules" 
        ref="formRef"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="请假类型" name="leaveType">
              <a-select v-model:value="formState.leaveType">
                <a-select-option value="annual">年假</a-select-option>
                <a-select-option value="sick">病假</a-select-option>
                <a-select-option value="personal">事假</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          
          <a-col :span="12">
            <a-form-item label="请假天数" name="days">
              <a-input-number 
                v-model:value="formState.days" 
                :min="1" 
                :max="30" 
              />
            </a-form-item>
          </a-col>
          
          <a-col :span="24">
            <a-form-item label="请假原因" name="reason">
              <a-textarea 
                v-model:value="formState.reason" 
                :rows="4" 
                placeholder="请输入请假原因"
              />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="submitApplication" :loading="submitting">
              提交申请
            </a-button>
            <a-button @click="resetForm">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { startProcessInstance } from '@/api/workflow'
import { saveLeaveApplication } from '@/api/business'

const router = useRouter()
const submitting = ref(false)
const formRef = ref()

// 表单数据
const formState = reactive({
  leaveType: undefined,
  days: undefined,
  reason: '',
  startDate: '',
  endDate: ''
})

// 表单验证规则
const rules = {
  leaveType: [{ required: true, message: '请选择请假类型' }],
  days: [{ required: true, message: '请输入请假天数' }],
  reason: [{ required: true, message: '请输入请假原因' }]
}

// 提交申请
const submitApplication = async () => {
  try {
    // 1. 表单验证
    await formRef.value?.validateFields()
    
    submitting.value = true
    
    // 2. 保存业务数据
    const businessResponse = await saveLeaveApplication(formState)
    
    if (businessResponse.data?.code !== 200) {
      throw new Error(businessResponse.data?.message || '保存业务数据失败')
    }
    
    const businessId = businessResponse.data.data.id
    
    // 3. 启动审批流程
    const processResponse = await startProcessInstance(
      'leave_approval_process',  // 流程定义KEY
      `LEAVE_${businessId}`,     // 业务KEY
      {
        businessId: businessId,
        applicant: 'currentUser',   // 当前用户
        leaveType: formState.leaveType,
        days: formState.days,
        reason: formState.reason
      }
    )
    
    if (processResponse.data?.code !== 200) {
      throw new Error(processResponse.data?.message || '启动流程失败')
    }
    
    // 4. 关联业务数据和流程实例
    await linkBusinessWithProcess(businessId, processResponse.data.data.instanceId)
    
    message.success('请假申请提交成功，等待审批')
    router.push('/business/leave/list')
    
  } catch (error: any) {
    message.error(error.message || '提交申请失败')
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  formRef.value?.resetFields()
}

// 关联业务和流程
const linkBusinessWithProcess = async (businessId: number, instanceId: string) => {
  // 这里可以调用关联接口
  // 或者在数据库中建立关联关系
  console.log('关联业务ID:', businessId, '流程实例ID:', instanceId)
}
</script>
```

### 2. 审批页面集成

```vue
<!-- src/views/workflow/approval/leave-detail.vue -->
<template>
  <div class="approval-detail">
    <a-page-header
      title="请假审批"
      @back="router.go(-1)"
    >
      <template #extra>
        <a-button v-if="canApprove" @click="showApprovalModal">审批</a-button>
      </template>
    </a-page-header>
    
    <a-row :gutter="16">
      <!-- 业务信息 -->
      <a-col :span="16">
        <a-card title="请假信息">
          <a-descriptions :column="2" bordered>
            <a-descriptions-item label="申请人">
              {{ leaveData.applicantName }}
            </a-descriptions-item>
            <a-descriptions-item label="请假类型">
              {{ getLeaveTypeText(leaveData.leaveType) }}
            </a-descriptions-item>
            <a-descriptions-item label="请假天数">
              {{ leaveData.days }} 天
            </a-descriptions-item>
            <a-descriptions-item label="申请时间">
              {{ leaveData.createTime }}
            </a-descriptions-item>
            <a-descriptions-item label="请假原因" :span="2">
              {{ leaveData.reason }}
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>
      
      <!-- 流程信息 -->
      <a-col :span="8">
        <a-card title="流程状态">
          <div class="process-status">
            <a-steps :current="currentStep" direction="vertical">
              <a-step 
                v-for="(step, index) in processSteps" 
                :key="index"
                :title="step.title"
                :description="step.description"
              >
                <template #icon>
                  <check-circle-outlined v-if="step.completed" />
                  <clock-circle-outlined v-else-if="step.current" />
                  <minus-circle-outlined v-else />
                </template>
              </a-step>
            </a-steps>
          </div>
        </a-card>
      </a-col>
    </a-row>
    
    <!-- 审批历史 -->
    <a-card title="审批历史" style="margin-top: 16px;">
      <a-table 
        :data-source="approvalHistory"
        :columns="historyColumns"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getApprovalStatusColor(record.status)">
              {{ getApprovalStatusText(record.status) }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
    
    <!-- 审批模态框 -->
    <a-modal
      v-model:open="approvalModalVisible"
      title="审批操作"
      @ok="handleApproval"
      @cancel="approvalModalVisible = false"
    >
      <a-form :model="approvalForm" layout="vertical">
        <a-form-item label="审批结果" name="result" required>
          <a-radio-group v-model:value="approvalForm.result">
            <a-radio value="approved">同意</a-radio>
            <a-radio value="rejected">拒绝</a-radio>
          </a-radio-group>
        </a-form-item>
        
        <a-form-item label="审批意见" name="comment">
          <a-textarea 
            v-model:value="approvalForm.comment" 
            :rows="4" 
            placeholder="请输入审批意见"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  CheckCircleOutlined, 
  ClockCircleOutlined, 
  MinusCircleOutlined 
} from '@ant-design/icons-vue'
import { getProcessInstanceDetail, completeTask } from '@/api/workflow'
import { getLeaveApplicationDetail } from '@/api/business'

const router = useRouter()
const route = useRoute()

// 响应式数据
const leaveData = ref<any>({})
const processInstance = ref<any>({})
const approvalHistory = ref<any[]>([])
const approvalModalVisible = ref(false)
const canApprove = ref(false)
const currentStep = ref(0)

// 审批表单
const approvalForm = reactive({
  result: '',
  comment: ''
})

// 流程步骤
const processSteps = ref([
  { title: '提交申请', description: '', completed: true, current: false },
  { title: '部门审批', description: '', completed: false, current: true },
  { title: '人事审批', description: '', completed: false, current: false },
  { title: '完成', description: '', completed: false, current: false }
])

// 历史表格列
const historyColumns = [
  { title: '审批人', dataIndex: 'approver', key: 'approver' },
  { title: '审批环节', dataIndex: 'step', key: 'step' },
  { title: '审批结果', dataIndex: 'status', key: 'status' },
  { title: '审批意见', dataIndex: 'comment', key: 'comment' },
  { title: '审批时间', dataIndex: 'approveTime', key: 'approveTime' }
]

// 获取数据
const loadData = async () => {
  try {
    const instanceId = route.params.instanceId as string
    
    // 获取流程实例详情
    const processResponse = await getProcessInstanceDetail(instanceId)
    processInstance.value = processResponse.data.data
    
    // 获取业务数据
    const businessId = processInstance.value.businessKey?.replace('LEAVE_', '')
    const businessResponse = await getLeaveApplicationDetail(businessId)
    leaveData.value = businessResponse.data.data
    
    // 获取审批历史
    approvalHistory.value = await getApprovalHistory(instanceId)
    
    // 判断当前用户是否有审批权限
    canApprove.value = checkApprovalPermission(processInstance.value)
    
    // 更新流程步骤状态
    updateProcessSteps(processInstance.value)
    
  } catch (error) {
    message.error('加载数据失败')
  }
}

// 处理审批
const handleApproval = async () => {
  try {
    const taskId = processInstance.value.currentTaskId
    const variables = {
      approved: approvalForm.result === 'approved',
      comment: approvalForm.comment
    }
    
    const response = await completeTask(taskId, variables)
    
    if (response.data?.code === 200) {
      message.success('审批完成')
      approvalModalVisible.value = false
      loadData() // 刷新数据
    } else {
      throw new Error(response.data?.message || '审批失败')
    }
  } catch (error: any) {
    message.error(error.message || '审批失败')
  }
}

// 显示审批模态框
const showApprovalModal = () => {
  approvalForm.result = ''
  approvalForm.comment = ''
  approvalModalVisible.value = true
}

// 工具函数
const getLeaveTypeText = (type: string) => {
  const types: Record<string, string> = {
    annual: '年假',
    sick: '病假',
    personal: '事假'
  }
  return types[type] || type
}

const getApprovalStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    approved: 'green',
    rejected: 'red',
    pending: 'orange'
  }
  return colors[status] || 'default'
}

const getApprovalStatusText = (status: string) => {
  const texts: Record<string, string> = {
    approved: '已同意',
    rejected: '已拒绝',
    pending: '审批中'
  }
  return texts[status] || status
}

const checkApprovalPermission = (instance: any) => {
  // 检查当前用户是否是当前任务的办理人
  // 这里需要根据实际的权限控制逻辑实现
  return true
}

const updateProcessSteps = (instance: any) => {
  // 根据流程实例状态更新步骤显示
  // 实际项目中需要根据具体的流程定义来实现
  currentStep.value = instance.currentActivityIndex || 0
}

onMounted(() => {
  loadData()
})
</script>
```

### 3. 我的任务页面

```vue
<!-- src/views/workflow/task/my-tasks.vue -->
<template>
  <div class="my-tasks">
    <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
      <a-tab-pane key="assigned" tab="待办任务">
        <a-table 
          :data-source="assignedTasks"
          :columns="taskColumns"
          :loading="loading.assigned"
          :pagination="pagination"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'processName'">
              <a @click="viewProcessDetail(record)">
                {{ record.processDefinitionName }}
              </a>
            </template>
            <template v-else-if="column.key === 'priority'">
              <a-tag :color="getPriorityColor(record.priority)">
                {{ getPriorityText(record.priority) }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a-button 
                  type="primary" 
                  size="small" 
                  @click="claimTask(record)"
                  v-if="!record.assignee"
                >
                  签收
                </a-button>
                <a-button 
                  type="primary" 
                  size="small" 
                  @click="handleTask(record)"
                >
                  处理
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
      
      <a-tab-pane key="completed" tab="已办任务">
        <a-table 
          :data-source="completedTasks"
          :columns="completedColumns"
          :loading="loading.completed"
          :pagination="pagination"
        />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { 
  getMyAssignedTasks, 
  getMyCompletedTasks, 
  claimTask as claimTaskApi,
  completeTask as completeTaskApi
} from '@/api/workflow'

const router = useRouter()

// 响应式数据
const activeTab = ref('assigned')
const assignedTasks = ref<any[]>([])
const completedTasks = ref<any[]>([])
const loading = reactive({
  assigned: false,
  completed: false
})

// 表格配置
const taskColumns = [
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName' },
  { title: '流程名称', dataIndex: 'processDefinitionName', key: 'processName' },
  { title: '发起人', dataIndex: 'starterName', key: 'starterName' },
  { title: '优先级', dataIndex: 'priority', key: 'priority' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '到期时间', dataIndex: 'dueTime', key: 'dueTime' },
  { title: '操作', key: 'action', fixed: 'right', width: 150 }
]

const completedColumns = [
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName' },
  { title: '流程名称', dataIndex: 'processDefinitionName', key: 'processName' },
  { title: '完成时间', dataIndex: 'endTime', key: 'endTime' },
  { title: '耗时', dataIndex: 'duration', key: 'duration' }
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

// 加载待办任务
const loadAssignedTasks = async () => {
  loading.assigned = true
  try {
    const response = await getMyAssignedTasks({
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    
    if (response.data?.code === 200) {
      assignedTasks.value = response.data.data.records
      pagination.total = response.data.data.total
    }
  } catch (error) {
    message.error('加载待办任务失败')
  } finally {
    loading.assigned = false
  }
}

// 加载已办任务
const loadCompletedTasks = async () => {
  loading.completed = true
  try {
    const response = await getMyCompletedTasks({
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    
    if (response.data?.code === 200) {
      completedTasks.value = response.data.data.records
      pagination.total = response.data.data.total
    }
  } catch (error) {
    message.error('加载已办任务失败')
  } finally {
    loading.completed = false
  }
}

// 签收任务
const claimTask = async (record: any) => {
  try {
    const response = await claimTaskApi(record.taskId)
    if (response.data?.code === 200) {
      message.success('任务签收成功')
      loadAssignedTasks()
    }
  } catch (error) {
    message.error('任务签收失败')
  }
}

// 处理任务
const handleTask = (record: any) => {
  // 根据任务类型跳转到不同的处理页面
  const processKey = record.processDefinitionKey
  const instanceId = record.processInstanceId
  
  if (processKey.includes('leave')) {
    router.push(`/workflow/approval/leave/${instanceId}`)
  } else if (processKey.includes('expense')) {
    router.push(`/workflow/approval/expense/${instanceId}`)
  } else {
    // 通用任务处理页面
    router.push(`/workflow/task/handle/${record.taskId}`)
  }
}

// 查看流程详情
const viewProcessDetail = (record: any) => {
  router.push(`/workflow/instance/detail/${record.processInstanceId}`)
}

// 标签页切换
const handleTabChange = (key: string) => {
  pagination.current = 1
  if (key === 'assigned') {
    loadAssignedTasks()
  } else {
    loadCompletedTasks()
  }
}

// 表格分页变化
const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadAssignedTasks()
}

// 工具函数
const getPriorityColor = (priority: number) => {
  if (priority >= 80) return 'red'
  if (priority >= 50) return 'orange'
  return 'green'
}

const getPriorityText = (priority: number) => {
  if (priority >= 80) return '紧急'
  if (priority >= 50) return '重要'
  return '普通'
}

onMounted(() => {
  loadAssignedTasks()
})
</script>
```

## 🔄 集成要点总结

### 1. 数据流转
```
业务页面 → 保存业务数据 → 启动流程实例 → 关联业务与流程
    ↓
流程引擎执行 → 生成任务 → 前端任务列表 → 用户处理任务
    ↓
完成任务 → 更新业务状态 → 流程继续执行
```

### 2. 关键集成点

1. **流程启动**：业务操作完成后启动对应的工作流
2. **任务处理**：前端页面嵌入任务处理功能
3. **状态同步**：保持业务状态与流程状态的一致性
4. **权限控制**：基于流程的细粒度权限管理
5. **数据关联**：建立业务数据与流程实例的关联关系

### 3. 最佳实践

- 使用统一的错误处理机制
- 实现加载状态和用户反馈
- 建立完善的数据缓存策略
- 设计清晰的用户操作流程
- 提供详细的日志记录

这样的集成方式可以让 Flowable 工作流引擎无缝融入到现有的业务系统中，为用户提供一体化的操作体验。