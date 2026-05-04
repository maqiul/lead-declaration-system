<template>
  <div class="workflow-monitor">
    <!-- 顶部统计卡片 -->
    <div class="stats-cards">
      <a-row :gutter="16">
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="总流程数"
              :value="stats.totalProcesses"
              :value-style="{ color: '#3f8600' }"
            >
              <template #prefix>
                <branches-outlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="运行中实例"
              :value="stats.runningInstances"
              :value-style="{ color: '#1890ff' }"
            >
              <template #prefix>
                <play-circle-outlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="待处理任务"
              :value="stats.pendingTasks"
              :value-style="{ color: '#fa8c16' }"
            >
              <template #prefix>
                <clock-circle-outlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="今日完成"
              :value="stats.completedToday"
              :value-style="{ color: '#52c41a' }"
            >
              <template #prefix>
                <check-circle-outlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- 中间主要内容区 -->
    <div class="main-content">
      <a-row :gutter="16">
        <!-- 左侧 - 流程实例列表 -->
        <a-col :span="12">
          <a-card title="流程实例监控" :bordered="false">
            <template #extra>
              <a-button type="link" size="small" @click="loadInstances">
                <template #icon><ReloadOutlined /></template>
                刷新
              </a-button>
            </template>
            
            <a-table
              :dataSource="instances"
              :columns="instanceColumns"
              :pagination="false"
              :loading="instancesLoading"
              size="small"
              :scroll="{ y: 300 }"
              rowKey="instanceId"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <a-tag :color="getInstanceStatusColor(record.status)">
                    {{ getInstanceStatusText(record.status) }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-space>
                    <a-button type="link" size="small" @click="viewInstanceDetail(record as ProcessInstance)">
                      <template #icon><EyeOutlined /></template>
                      查看
                    </a-button>
                    <a-button 
                      v-if="record.status === 1" 
                      type="link" 
                      danger
                      size="small" 
                      @click="terminateInstance(record as ProcessInstance)"
                    >
                      <template #icon><StopOutlined /></template>
                      终止
                    </a-button>
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-card>
        </a-col>

        <!-- 右侧 - 实时任务列表 (全局) -->
        <a-col :span="12">
          <a-card title="全系统活跃任务" :bordered="false">
            <template #extra>
              <a-button type="link" size="small" @click="loadTasks">
                <template #icon><ReloadOutlined /></template>
                刷新
              </a-button>
            </template>
            
            <a-table
              :dataSource="tasks"
              :columns="taskColumns"
              :pagination="false"
              :loading="tasksLoading"
              size="small"
              :scroll="{ y: 300 }"
              rowKey="taskId"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'priority'">
                  <a-tag :color="getPriorityColor(record.priority)">
                    {{ getPriorityText(record.priority) }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-button type="link" size="small" @click="viewTaskDetail(record as TaskInstance)">
                    <template #icon><ProfileOutlined /></template>
                    详情
                  </a-button>
                </template>
              </template>
            </a-table>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- 底部图表区域 -->
    <div class="charts-section">
      <a-row :gutter="16">
        <!-- 流程类型分布饼图 -->
        <a-col :span="12">
          <a-card title="流程类型分布" :bordered="false">
            <div ref="pieChartRef" style="height: 300px;"></div>
          </a-card>
        </a-col>
        
        <!-- 性能指标 -->
        <a-col :span="12">
          <a-card title="系统实时负荷" :bordered="false">
            <div class="performance-metrics">
              <div class="metric-item">
                <div class="metric-label">活跃实例密度</div>
                <a-progress :percent="stats.runningInstances * 10" status="active" />
              </div>
              <div class="metric-item">
                <div class="metric-label">待办任务积压</div>
                <a-progress :percent="stats.pendingTasks * 5" :status="stats.pendingTasks > 20 ? 'exception' : 'normal'" />
              </div>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- 实例详情弹窗 -->
    <a-modal
      v-model:open="instanceDetailVisible"
      title="流程实例详情"
      width="800px"
      :footer="null"
    >
      <div v-if="selectedInstance">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="实例ID">{{ selectedInstance.instanceId }}</a-descriptions-item>
          <a-descriptions-item label="流程定义">{{ selectedInstance.processDefinitionName }}</a-descriptions-item>
          <a-descriptions-item label="业务KEY">{{ selectedInstance.businessKey }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="getInstanceStatusColor(selectedInstance.status)">
              {{ getInstanceStatusText(selectedInstance.status) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="发起人">{{ selectedInstance.starterName }}</a-descriptions-item>
          <a-descriptions-item label="开始时间">{{ selectedInstance.startTime }}</a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>

    <!-- 任务详情弹窗 -->
    <a-modal
      v-model:open="taskDetailVisible"
      title="任务详情"
      width="600px"
      :footer="null"
    >
      <div v-if="selectedTask">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="任务ID">{{ selectedTask.taskId }}</a-descriptions-item>
          <a-descriptions-item label="任务名称">{{ selectedTask.taskName }}</a-descriptions-item>
          <a-descriptions-item label="所属流程">{{ selectedTask.processDefinitionName }}</a-descriptions-item>
          <a-descriptions-item label="办理人">{{ selectedTask.assigneeName || '未分配' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ selectedTask.createTime }}</a-descriptions-item>
          <a-descriptions-item label="签收时间">{{ selectedTask.claimTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="截止时间">{{ selectedTask.dueTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="优先级">
            <a-tag :color="getPriorityColor(selectedTask.priority || 0)">
              {{ getPriorityText(selectedTask.priority || 0) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="任务描述" :span="2">{{ selectedTask.description || '暂无描述' }}</a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'
import {
  BranchesOutlined,
  PlayCircleOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  ReloadOutlined,
  EyeOutlined,
  StopOutlined,
  ProfileOutlined
} from '@ant-design/icons-vue'
import type { ProcessInstance, TaskInstance } from '@/api/workflow'
import { 
  getRunningProcessInstances, 
  getMonitorStats, 
  getAllActiveTasks, 
  getMonitorCharts,
  terminateProcessInstance
} from '@/api/workflow'

// 响应式数据
const stats = ref({
  totalProcesses: 0,
  runningInstances: 0,
  pendingTasks: 0,
  completedToday: 0
})

const instances = ref<ProcessInstance[]>([])
const tasks = ref<TaskInstance[]>([])
const instancesLoading = ref(false)
const tasksLoading = ref(false)

const instanceDetailVisible = ref(false)
const taskDetailVisible = ref(false)
const selectedInstance = ref<ProcessInstance | null>(null)
const selectedTask = ref<TaskInstance | null>(null)

// 图表引用
const pieChartRef = ref<HTMLDivElement>()

// 表格列配置
const instanceColumns = [
  { title: '实例ID', dataIndex: 'instanceId', key: 'instanceId', width: 120 },
  { title: '流程名称', dataIndex: 'processDefinitionName', key: 'processDefinitionName', ellipsis: true },
  { title: '业务KEY', dataIndex: 'businessKey', key: 'businessKey', width: 100 },
  { title: '发起人', dataIndex: 'starterName', key: 'starterName', width: 100 },
  { title: '当前节点', dataIndex: 'currentActivityName', key: 'currentActivityName', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 150 },
  { title: '操作', key: 'action', width: 100 }
]

const taskColumns = [
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName', ellipsis: true },
  { title: '办理人', dataIndex: 'assigneeName', key: 'assigneeName', width: 100 },
  { title: '流程', dataIndex: 'processDefinitionName', key: 'processDefinitionName', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 150 },
  { title: '操作', key: 'action', width: 80 }
]

// 方法
const loadAllData = async () => {
  loadStats()
  loadInstances()
  loadTasks()
  loadCharts()
}

const loadStats = async () => {
  try {
    const res = await getMonitorStats()
    if (res.data?.code === 200) {
      stats.value = res.data.data
    }
  } catch (err) {
    console.error('加载统计失败', err)
  }
}

const loadInstances = async () => {
  instancesLoading.value = true
  try {
    const response = await getRunningProcessInstances()
    if (response.data?.code === 200) {
      instances.value = response.data.data?.records || []
    }
  } catch (error) {
    message.error('加载实例数据失败')
  } finally {
    instancesLoading.value = false
  }
}

const loadTasks = async () => {
  tasksLoading.value = true
  try {
    const response = await getAllActiveTasks()
    if (response.data?.code === 200) {
      tasks.value = response.data.data?.records || []
    }
  } catch (error) {
    message.error('加载全量任务失败')
  } finally {
    tasksLoading.value = false
  }
}

const loadCharts = async () => {
  if (!pieChartRef.value) return
  try {
    const res = await getMonitorCharts()
    if (res.data?.code === 200) {
      const chartData = res.data.data.typeDistribution
      const pieChart = echarts.init(pieChartRef.value)
      pieChart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: '5%', left: 'center' },
        series: [{
          name: '流程分布',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
          label: { show: false, position: 'center' },
          emphasis: { label: { show: true, fontSize: 20, fontWeight: 'bold' } },
          data: chartData
        }]
      })
    }
  } catch (err) {
    console.error('加载图表失败', err)
  }
}

const viewInstanceDetail = (record: ProcessInstance) => {
  selectedInstance.value = record
  instanceDetailVisible.value = true
}

const terminateInstance = async (record: ProcessInstance) => {
  try {
    const res = await terminateProcessInstance(record.instanceId, '管理员强制终止')
    if (res.data?.code === 200) {
      message.success('终止成功')
      loadAllData()
    }
  } catch (err) {
    message.error('操作失败')
  }
}

const viewTaskDetail = (record: TaskInstance) => {
  selectedTask.value = record
  taskDetailVisible.value = true
}

const getInstanceStatusColor = (status: number) => {
  const colors: Record<number, string> = { 1: 'blue', 2: 'green', 3: 'orange', 4: 'red' }
  return colors[status] || 'default'
}

const getInstanceStatusText = (status: number) => {
  const texts: Record<number, string> = { 1: '运行中', 2: '已完成', 3: '已挂起', 4: '已终止' }
  return texts[status] || '未知'
}

const getPriorityColor = (priority: number) => {
  if (priority >= 75) return 'red'
  if (priority >= 50) return 'orange'
  return 'green'
}

const getPriorityText = (priority: number) => {
  if (priority >= 75) return '高'
  if (priority >= 50) return '中'
  return '低'
}

// 生命周期
onMounted(() => {
  loadAllData()
  const timer = setInterval(loadAllData, 30000)
  
  const resizeHandler = () => {
    if (pieChartRef.value) echarts.getInstanceByDom(pieChartRef.value)?.resize()
  }
  window.addEventListener('resize', resizeHandler)
  
  onUnmounted(() => {
    clearInterval(timer)
    window.removeEventListener('resize', resizeHandler)
  })
})
</script>

<style scoped>
.workflow-monitor {
  padding: 0;
  background: transparent;
  min-height: 100%;
}

.stats-cards { margin-bottom: 24px; }
.main-content { margin-bottom: 24px; }
.charts-section { margin-bottom: 24px; }

.performance-metrics { padding: 16px 0; }
.metric-item { margin-bottom: 16px; }
.metric-label { font-size: 14px; color: #666; margin-bottom: 4px; }

:deep(.ant-card) {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}
</style>