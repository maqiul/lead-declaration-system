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
              <a-select v-model:value="instanceFilter" style="width: 120px" @change="filterInstances">
                <a-select-option value="all">全部</a-select-option>
                <a-select-option value="running">运行中</a-select-option>
                <a-select-option value="completed">已完成</a-select-option>
                <a-select-option value="suspended">已挂起</a-select-option>
              </a-select>
            </template>
            
            <a-table
              :dataSource="instances"
              :columns="instanceColumns"
              :pagination="false"
              :loading="instancesLoading"
              size="small"
              :scroll="{ y: 300 }"
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
                      查看详情
                    </a-button>
                    <a-button 
                      v-if="record.status === 'running'" 
                      type="link" 
                      size="small" 
                      @click="terminateInstance(record as ProcessInstance)"
                    >
                      终止
                    </a-button>
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-card>
        </a-col>

        <!-- 右侧 - 实时任务列表 -->
        <a-col :span="12">
          <a-card title="实时任务监控" :bordered="false">
            <template #extra>
              <a-select v-model:value="taskFilter" style="width: 120px" @change="filterTasks">
                <a-select-option value="all">全部</a-select-option>
                <a-select-option value="active">活跃</a-select-option>
                <a-select-option value="assigned">已指派</a-select-option>
                <a-select-option value="unassigned">未指派</a-select-option>
              </a-select>
            </template>
            
            <a-table
              :dataSource="tasks"
              :columns="taskColumns"
              :pagination="false"
              :loading="tasksLoading"
              size="small"
              :scroll="{ y: 300 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'priority'">
                  <a-tag :color="getPriorityColor(record.priority)">
                    {{ getPriorityText(record.priority) }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'dueDate'">
                  <span :class="{ 'overdue': isTaskOverdue(record.dueDate) }">
                    {{ record.dueDate || '无期限' }}
                  </span>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-button type="link" size="small" @click="viewTaskDetail(record as TaskInstance)">
                    处理任务
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
        <a-col :span="8">
          <a-card title="流程类型分布" :bordered="false">
            <div ref="pieChartRef" style="height: 300px;"></div>
          </a-card>
        </a-col>
        
        <!-- 任务趋势折线图 -->
        <a-col :span="8">
          <a-card title="任务处理趋势" :bordered="false">
            <div ref="lineChartRef" style="height: 300px;"></div>
          </a-card>
        </a-col>
        
        <!-- 性能指标仪表盘 -->
        <a-col :span="8">
          <a-card title="性能指标" :bordered="false">
            <div class="performance-metrics">
              <div class="metric-item">
                <div class="metric-label">平均处理时间</div>
                <div class="metric-value">{{ performance.avgProcessingTime }}分钟</div>
              </div>
              <div class="metric-item">
                <div class="metric-label">流程完成率</div>
                <div class="metric-value">{{ performance.completionRate }}%</div>
              </div>
              <div class="metric-item">
                <div class="metric-label">系统负载</div>
                <a-progress :percent="performance.systemLoad" />
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
          <a-descriptions-item label="实例ID">{{ selectedInstance.id }}</a-descriptions-item>
          <a-descriptions-item label="流程定义">{{ selectedInstance.processDefinitionName }}</a-descriptions-item>
          <a-descriptions-item label="业务KEY">{{ selectedInstance.businessKey }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="getInstanceStatusColor(selectedInstance.status)">
              {{ getInstanceStatusText(selectedInstance.status) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="发起人">{{ selectedInstance.starterName }}</a-descriptions-item>
          <a-descriptions-item label="开始时间">{{ selectedInstance.startTime }}</a-descriptions-item>
          <a-descriptions-item label="结束时间" v-if="selectedInstance.endTime">
            {{ selectedInstance.endTime }}
          </a-descriptions-item>
          <!-- <a-descriptions-item label="持续时间" v-if="selectedInstance.durationInMillis">
            {{ formatDuration(selectedInstance.durationInMillis) }}
          </a-descriptions-item> -->
        </a-descriptions>
        
        <div style="margin-top: 24px;">
          <h4>流程图</h4>
          <div class="process-diagram">
            <a-empty description="流程图展示区域" />
          </div>
        </div>
      </div>
    </a-modal>

    <!-- 任务详情弹窗 -->
    <a-modal
      v-model:open="taskDetailVisible"
      title="任务详情"
      width="600px"
      @ok="handleTaskAction"
    >
      <div v-if="selectedTask">
        <a-descriptions :column="1" bordered>
          <a-descriptions-item label="任务ID">{{ selectedTask.id }}</a-descriptions-item>
          <a-descriptions-item label="任务名称">{{ selectedTask.taskName }}</a-descriptions-item>
          <a-descriptions-item label="流程实例">{{ selectedTask.processInstanceId }}</a-descriptions-item>
          <a-descriptions-item label="指派人">{{ selectedTask.assigneeName || '未指派' }}</a-descriptions-item>
          <!-- <a-descriptions-item label="优先级">
            <a-tag :color="getPriorityColor(selectedTask.priority)">
              {{ getPriorityText(selectedTask.priority) }}
            </a-tag>
          </a-descriptions-item> -->
          <a-descriptions-item label="创建时间">{{ selectedTask.createTime }}</a-descriptions-item>
          <a-descriptions-item label="到期时间" v-if="selectedTask.dueTime">
            <span :class="{ 'overdue': isTaskOverdue(selectedTask.dueTime) }">
              {{ selectedTask.dueTime }}
            </span>
          </a-descriptions-item>
        </a-descriptions>
        
        <a-form layout="vertical" style="margin-top: 24px;">
          <a-form-item label="处理意见">
            <a-textarea v-model:value="taskComment" :rows="3" placeholder="请输入处理意见" />
          </a-form-item>
        </a-form>
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
  CheckCircleOutlined
} from '@ant-design/icons-vue'
import type { ProcessInstance, TaskInstance } from '@/api/workflow'
import { getRunningProcessInstances, getMyAssignedTasks, getTasksByProcessInstance } from '@/api/workflow'

// 类型定义
interface Stats {
  totalProcesses: number
  runningInstances: number
  pendingTasks: number
  completedToday: number
}

interface PerformanceMetrics {
  avgProcessingTime: number
  completionRate: number
  systemLoad: number
}

// 响应式数据
const stats = ref<Stats>({
  totalProcesses: 24,
  runningInstances: 8,
  pendingTasks: 15,
  completedToday: 12
})

const performance = ref<PerformanceMetrics>({
  avgProcessingTime: 45,
  completionRate: 92,
  systemLoad: 65
})

const instances = ref<ProcessInstance[]>([])
const tasks = ref<TaskInstance[]>([])
const instancesLoading = ref(false)
const tasksLoading = ref(false)
const instanceFilter = ref('all')
const taskFilter = ref('all')

const instanceDetailVisible = ref(false)
const taskDetailVisible = ref(false)
const selectedInstance = ref<ProcessInstance | null>(null)
const selectedTask = ref<TaskInstance | null>(null)
const taskComment = ref('')

// 图表引用
const pieChartRef = ref<HTMLDivElement>()
const lineChartRef = ref<HTMLDivElement>()

// 表格列配置
const instanceColumns = [
  {
    title: '流程名称',
    dataIndex: 'processDefinitionName',
    key: 'processDefinitionName',
    ellipsis: true
  },
  {
    title: '业务KEY',
    dataIndex: 'businessKey',
    key: 'businessKey',
    width: 120
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 80
  },
  {
    title: '开始时间',
    dataIndex: 'startTime',
    key: 'startTime',
    width: 150
  },
  {
    title: '操作',
    key: 'action',
    width: 120
  }
]

const taskColumns = [
  {
    title: '任务名称',
    dataIndex: 'name',
    key: 'name',
    ellipsis: true
  },
  {
    title: '流程实例',
    dataIndex: 'processInstanceId',
    key: 'processInstanceId',
    width: 120
  },
  {
    title: '优先级',
    dataIndex: 'priority',
    key: 'priority',
    width: 80
  },
  {
    title: '到期时间',
    dataIndex: 'dueDate',
    key: 'dueDate',
    width: 120
  },
  {
    title: '操作',
    key: 'action',
    width: 80
  }
]

// 方法
const loadData = async () => {
  loadInstances()
  loadTasks()
  loadCharts()
}

const loadInstances = async () => {
  instancesLoading.value = true
  try {
    const response = await getRunningProcessInstances()
    if (response.data?.code === 200) {
      instances.value = response.data.data
    } else {
      message.error(response.data?.message || '加载流程实例失败')
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
    const response = await getMyAssignedTasks()
    if (response.data?.code === 200) {
      tasks.value = response.data.data
    } else {
      message.error(response.data?.message || '加载任务数据失败')
    }
  } catch (error) {
    message.error('加载任务数据失败')
  } finally {
    tasksLoading.value = false
  }
}

const loadCharts = () => {
  // 饼图
  if (pieChartRef.value && document.contains(pieChartRef.value)) {
    try {
      const pieChart = echarts.init(pieChartRef.value)
      pieChart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: '5%' },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          data: [
            { value: 12, name: '请假审批' },
            { value: 8, name: '费用报销' },
            { value: 4, name: '合同审批' }
          ]
        }]
      })
    } catch (error) {
      console.error('饼图初始化失败:', error)
    }
  }
  
  // 折线图
  if (lineChartRef.value && document.contains(lineChartRef.value)) {
    try {
      const lineChart = echarts.init(lineChartRef.value)
      lineChart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
        },
        yAxis: { type: 'value' },
        series: [{
        data: [12, 18, 15, 22, 19, 8, 5],
        type: 'line',
        smooth: true
      }]
    })
    } catch (error) {
      console.error('折线图初始化失败:', error)
    }
  }
}

const filterInstances = () => {
  // 实现过滤逻辑
  message.info(`筛选实例: ${instanceFilter.value}`)
}

const filterTasks = () => {
  // 实现过滤逻辑
  message.info(`筛选任务: ${taskFilter.value}`)
}

const viewInstanceDetail = (record: ProcessInstance) => {
  selectedInstance.value = record
  instanceDetailVisible.value = true
}

const terminateInstance = (record: ProcessInstance) => {
  message.success(`终止实例: ${record.id}`)
}

const viewTaskDetail = (record: TaskInstance) => {
  selectedTask.value = record
  taskDetailVisible.value = true
}

const handleTaskAction = () => {
  message.success('任务处理成功')
  taskDetailVisible.value = false
  taskComment.value = ''
}

const getInstanceStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    'running': 'blue',
    'completed': 'green',
    'suspended': 'orange',
    'terminated': 'red'
  }
  return colors[status] || 'default'
}

const getInstanceStatusText = (status: string) => {
  const texts: Record<string, string> = {
    'running': '运行中',
    'completed': '已完成',
    'suspended': '已挂起',
    'terminated': '已终止'
  }
  return texts[status] || status
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

const isTaskOverdue = (dueDate?: string) => {
  if (!dueDate) return false
  return new Date(dueDate) < new Date()
}

const formatDuration = (millis: number) => {
  const minutes = Math.floor(millis / 60000)
  if (minutes < 60) return `${minutes}分钟`
  const hours = Math.floor(minutes / 60)
  return `${hours}小时${minutes % 60}分钟`
}

// 生命周期
onMounted(() => {
  // 延迟加载数据，确保DOM完全渲染
  setTimeout(() => {
    loadData()
  }, 100)
  
  // 定时刷新数据
  const timer = setInterval(loadData, 30000)
  
  // 监听窗口大小变化
  const resizeHandler = () => {
    if (pieChartRef.value) {
      echarts.getInstanceByDom(pieChartRef.value)?.resize()
    }
    if (lineChartRef.value) {
      echarts.getInstanceByDom(lineChartRef.value)?.resize()
    }
  }
  window.addEventListener('resize', resizeHandler)
  
  // 清理函数
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
  box-sizing: border-box;
  overflow-x: hidden;
}

.stats-cards {
  margin-bottom: 24px;
}

.main-content {
  margin-bottom: 24px;
}

.charts-section {
  margin-bottom: 24px;
}

.process-diagram {
  height: 200px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fafafa;
}

.performance-metrics {
  padding: 16px 0;
}

.metric-item {
  margin-bottom: 16px;
}

.metric-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.metric-value {
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.overdue {
  color: #ff4d4f;
  font-weight: 500;
}

:deep(.ant-card) {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

:deep(.ant-statistic-title) {
  font-size: 14px;
}

:deep(.ant-statistic-content) {
  font-size: 24px;
  font-weight: 500;
}
</style>