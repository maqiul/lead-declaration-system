<template>
  <div class="dashboard">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-text">
        <h2 class="welcome-title">欢迎回来 👋</h2>
        <p class="welcome-desc">这是您的工作台概览，今天也要加油哦</p>
      </div>
      <div class="welcome-decoration">
        <div class="decoration-circle decoration-circle--1"></div>
        <div class="decoration-circle decoration-circle--2"></div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="[20, 20]" class="stat-row">
      <a-col :xs="24" :sm="12" :lg="6" v-for="(stat, index) in statCards" :key="index">
        <div class="stat-card" :class="`stat-card--${stat.theme}`">
          <div class="stat-card-inner">
            <div class="stat-info">
              <span class="stat-label">{{ stat.title }}</span>
              <span class="stat-value">{{ stat.value.toLocaleString() }}</span>
              <span class="stat-trend" v-if="stat.trend">
                <rise-outlined v-if="stat.trendUp" />
                <fall-outlined v-else />
                {{ stat.trend }}
              </span>
            </div>
            <div class="stat-icon-wrap">
              <component :is="stat.icon" class="stat-icon" />
            </div>
          </div>
          <div class="stat-shimmer"></div>
        </div>
      </a-col>
    </a-row>

    <!-- 图表区域 -->
    <a-row :gutter="[20, 20]" style="margin-top: 20px">
      <a-col :xs="24" :lg="12">
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">流程类型统计</h3>
            <a-tag color="blue" :bordered="false">本月</a-tag>
          </div>
          <div ref="chartRef" style="height: 320px"></div>
        </div>
      </a-col>
      
      <a-col :xs="24" :lg="12">
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">待办任务分布</h3>
            <a-tag color="purple" :bordered="false">实时</a-tag>
          </div>
          <div ref="pieChartRef" style="height: 320px"></div>
        </div>
      </a-col>
    </a-row>
    
    <a-row :gutter="[20, 20]" style="margin-top: 20px">
      <!-- 最近流程 -->
      <a-col :xs="24" :lg="12">
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">最近流程</h3>
            <a-button type="link" size="small" @click="goToPage('/workflow/instance')">查看全部</a-button>
          </div>
          <a-table :columns="processColumns" :data-source="processData" :pagination="false" size="small" :loading="processLoading">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="getStatusColor(record.status)" :bordered="false">
                  {{ record.status }}
                </a-tag>
              </template>
            </template>
            <template #emptyText>
              <a-empty description="暂无流程数据" :image="null" />
            </template>
          </a-table>
        </div>
      </a-col>
      
      <!-- 待办任务 -->
      <a-col :xs="24" :lg="12">
        <div class="chart-card">
          <div class="chart-header">
            <h3 class="chart-title">待办任务</h3>
            <a-button type="link" size="small" @click="goToPage('/workflow/task')">查看全部</a-button>
          </div>
          <a-spin :spinning="taskLoading">
            <a-list item-layout="horizontal" :data-source="taskData" :pagination="false">
              <template #renderItem="{ item }">
                <a-list-item class="task-item">
                  <a-list-item-meta :description="item.description">
                    <template #title>
                      <a class="task-link" @click="handleTaskClick(item)">{{ item.title }}</a>
                    </template>
                  </a-list-item-meta>
                  <div>
                    <a-tag :color="getTimeColor(item.time)" :bordered="false" size="small">{{ item.time }}</a-tag>
                  </div>
                </a-list-item>
              </template>
              <template #header v-if="taskData.length === 0 && !taskLoading">
                <a-empty description="暂无待办任务" :image="null" />
              </template>
            </a-list>
          </a-spin>
        </div>
      </a-col>
    </a-row>
    
    <!-- 快捷操作 -->
    <div class="quick-actions" style="margin-top: 20px">
      <h3 class="section-title">快捷操作</h3>
      <a-row :gutter="[16, 16]">
        <a-col :xs="12" :sm="6" v-for="action in quickActions" :key="action.label">
          <div class="action-card" @click="goToPage(action.path)">
            <div class="action-icon-wrap" :style="{ background: action.bg }">
              <component :is="action.icon" class="action-icon" />
            </div>
            <span class="action-label">{{ action.label }}</span>
          </div>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  UserOutlined,
  ProfileOutlined,
  CheckCircleOutlined,
  PlusCircleOutlined,
  UserAddOutlined,
  FileAddOutlined,
  ApartmentOutlined,
  RiseOutlined,
  FallOutlined
} from '@ant-design/icons-vue'
import { getDashboardStats, getDashboardCharts } from '@/api/dashboard'
import { getMyProcessInstances, getMyAssignedTasks, type ProcessInstance, type TaskInstance } from '@/api/workflow'

const router = useRouter()

// 统计卡片
const statCards = ref([
  { title: '用户总数', value: 0, icon: markRaw(UserOutlined), theme: 'orange', trend: '', trendUp: true },
  { title: '总流程数', value: 0, icon: markRaw(ProfileOutlined), theme: 'orange', trend: '', trendUp: true },
  { title: '流转中待办', value: 0, icon: markRaw(CheckCircleOutlined), theme: 'orange-light', trend: '', trendUp: false },
  { title: '今日新增单据', value: 0, icon: markRaw(PlusCircleOutlined), theme: 'orange-deep', trend: '', trendUp: true },
])

const fetchStatsData = async () => {
  try {
    const res: any = await getDashboardStats()
    if (res.data?.code === 200) {
      const data = res.data.data || {} // 添加默认空对象
      statCards.value[0].value = data.userCount || 0
      statCards.value[1].value = data.processInstanceCount || 0
      statCards.value[2].value = data.pendingTaskCount || 0
      statCards.value[3].value = data.todayNewCount || 0
      
      // 使用API返回的趋势数据（如果有），添加空值检查
      if (data.userCountTrend) {
        statCards.value[0].trend = data.userCountTrend
        statCards.value[0].trendUp = !data.userCountTrend.startsWith('-')
      }
      if (data.processInstanceCountTrend) {
        statCards.value[1].trend = data.processInstanceCountTrend
        statCards.value[1].trendUp = !data.processInstanceCountTrend.startsWith('-')
      }
      if (data.pendingTaskCountTrend) {
        statCards.value[2].trend = data.pendingTaskCountTrend
        statCards.value[2].trendUp = !data.pendingTaskCountTrend.startsWith('-')
      }
      if (data.todayNewCountTrend) {
        statCards.value[3].trend = data.todayNewCountTrend
        statCards.value[3].trendUp = !data.todayNewCountTrend.startsWith('-')
      }
    } else {
      // API失败时显示0，不使用Mock数据
      statCards.value[0].value = 0
      statCards.value[1].value = 0
      statCards.value[2].value = 0
      statCards.value[3].value = 0
    }
  } catch (err) {
    // API失败时显示0，不使用Mock数据
    statCards.value[0].value = 0
    statCards.value[1].value = 0
    statCards.value[2].value = 0
    statCards.value[3].value = 0
  }
}

// 快捷操作
const quickActions = ref([
  { label: '新增用户', icon: markRaw(UserAddOutlined), path: '/system/user', bg: 'linear-gradient(135deg, #FFF7E6, #FFE7BA)' },
  { label: '创建流程', icon: markRaw(FileAddOutlined), path: '/workflow/definition', bg: 'linear-gradient(135deg, #FFF1E6, #FFD591)' },
  { label: '处理任务', icon: markRaw(CheckCircleOutlined), path: '/workflow/task', bg: 'linear-gradient(135deg, #FFF7E6, #FFA940)' },
  { label: '组织管理', icon: markRaw(ApartmentOutlined), path: '/system/org', bg: 'linear-gradient(135deg, #FFFBEB, #FFE7BA)' },
])

// 图表引用
const chartRef = ref<HTMLDivElement>()
const pieChartRef = ref<HTMLDivElement>()

// 表格列
const processColumns = [
  { title: '流程名称', dataIndex: 'name', key: 'name' },
  { title: '发起人', dataIndex: 'starter', key: 'starter' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '时间', dataIndex: 'startTime', key: 'startTime' },
]

// 数据与加载状态
const processData = ref<any[]>([])
const taskData = ref<any[]>([])
const processLoading = ref(false)
const taskLoading = ref(false)

// 获取流程状态文本
const getProcessStatusText = (status: number): string => {
  const statusMap: Record<number, string> = {
    0: '运行中',
    1: '已完成',
    2: '已终止',
    3: '挂起'
  }
  return statusMap[status] || '未知'
}

// 格式化时间显示
const formatTime = (timeStr: string): string => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 计算相对时间
const getRelativeTime = (timeStr: string): string => {
  if (!timeStr) return '未知时间'
  const now = new Date()
  const time = new Date(timeStr)
  const diff = now.getTime() - time.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  return formatTime(timeStr)
}

// 获取流程实例数据
const fetchProcessData = async () => {
  processLoading.value = true
  try {
    const res: any = await getMyProcessInstances()
    if (res.data?.code === 200) {
      const instances: ProcessInstance[] = res.data.data?.records || []
      // 取最近5条数据，添加空值检查
      processData.value = instances.slice(0, 5).map((item, index) => ({
        key: String(index + 1),
        name: item?.processDefinitionName || '未知流程',
        starter: item?.starterName || '未知',
        status: getProcessStatusText(item?.status || 0),
        startTime: formatTime(item?.startTime || '')
      })).filter(item => item.name && item.starter) // 过滤掉无效数据
    } else {
      processData.value = []
    }
  } catch (err) {
    processData.value = []
  } finally {
    processLoading.value = false
  }
}

// 获取待办任务数据
const fetchTaskData = async () => {
  taskLoading.value = true
  try {
    const res: any = await getMyAssignedTasks()
    if (res.data?.code === 200) {
      const tasks: TaskInstance[] = res.data.data?.records || []
      // 取最近5条数据，添加空值检查
      taskData.value = tasks.slice(0, 5).map((item) => ({
        id: item?.id || 0,
        taskId: item?.taskId || '',
        title: `${item?.taskName || item?.activityName || '待处理任务'} - ${item?.processDefinitionName || '流程任务'}`,
        description: item?.description || `处理人：${item?.assigneeName || '待签收'}`,
        time: getRelativeTime(item?.createTime || '')
      })).filter(item => item.title && item.description) // 过滤掉无效数据
    } else {
      taskData.value = []
    }
  } catch (err) {
    taskData.value = []
  } finally {
    taskLoading.value = false
  }
}

const getStatusColor = (status: string) => {
  const map: Record<string, string> = { '运行中': 'blue', '已完成': 'green', '已终止': 'red', '挂起': 'orange' }
  return map[status] || 'default'
}

const getTimeColor = (time: string) => {
  if (time.includes('小时')) return 'red'
  if (time.includes('天') && parseInt(time) <= 1) return 'orange'
  return 'default'
}

const handleTaskClick = (_task?: any) => {
  router.push('/workflow/task')
}

const goToPage = (path: string) => {
  router.push(path)
}

// 初始化图表
const initCharts = async () => {
  const colors = {
    orange: '#FFA940',
    orangeLight: '#FFC069',
    orangeDeep: '#FA8C16',
    orangePale: '#FFF7E6',
    amber: '#FFD666',
  }

  try {
    const res: any = await getDashboardCharts();
    if (res.data?.code === 200) {
      const chartData = res.data.data || {}; // 添加默认空对象
      const processChart = chartData.processChart || { categories: [], seriesData: [] };
      const taskPieChart = chartData.taskPieChart || { seriesData: [] };
      
      if (chartRef.value) {
        let barChart = echarts.getInstanceByDom(chartRef.value)
        if (!barChart) {
          barChart = echarts.init(chartRef.value)
        }
        barChart.setOption({
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, backgroundColor: 'rgba(15,23,42,0.9)', borderColor: 'transparent', textStyle: { color: '#fff' } },
          grid: { left: '3%', right: '4%', bottom: '3%', top: '8%', containLabel: true },
          xAxis: [{ type: 'category', data: processChart.categories || [], axisTick: { show: false }, axisLine: { lineStyle: { color: '#E2E8F0' } }, axisLabel: { color: '#64748B' } }],
          yAxis: [{ type: 'value', axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } }, axisLabel: { color: '#94A3B8' } }],
          series: [{
            name: '流程数量',
            type: 'bar',
            barWidth: '50%',
            data: processChart.seriesData || [],
            itemStyle: {
              borderRadius: [4, 4, 0, 0],
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#FFD591' },
                { offset: 1, color: '#FFF7E6' }
              ])
            }
          }]
        })
      }
      
      if (pieChartRef.value) {
        let pieChart = echarts.getInstanceByDom(pieChartRef.value)
        if (!pieChart) {
          pieChart = echarts.init(pieChartRef.value)
        }
        pieChart.setOption({
          tooltip: { trigger: 'item', backgroundColor: 'rgba(30,27,75,0.9)', borderColor: 'transparent', textStyle: { color: '#fff' } },
          legend: { top: '5%', left: 'center', textStyle: { color: '#64748B' } },
          series: [{
            name: '任务分布',
            type: 'pie',
            radius: ['42%', '72%'],
            avoidLabelOverlap: false,
            itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 3 },
            label: { show: false, position: 'center' },
            emphasis: { label: { show: true, fontSize: 18, fontWeight: '600', color: '#1E293B' } },
            labelLine: { show: false },
            color: [colors.orange, colors.orangeLight, colors.orangeDeep, colors.orangePale],
            data: taskPieChart.seriesData || []
          } as any]
        })
      }
    }
  } catch(e) {
    // 图表数据加载失败
  }
}

// 处理窗口缩放
const handleResize = () => {
  if (chartRef.value) {
    echarts.getInstanceByDom(chartRef.value)?.resize()
  }
  if (pieChartRef.value) {
    echarts.getInstanceByDom(pieChartRef.value)?.resize()
  }
}

onMounted(() => {
  fetchStatsData()
  fetchProcessData()
  fetchTaskData()
  initCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.dashboard {
  min-height: 100%;
  background-color: #F0F2F5;
}

/* 欢迎横幅 */
.welcome-banner {
  background: linear-gradient(135deg, #FFF7E6 0%, #FFE7BA 100%);
  border-radius: 12px;
  padding: 28px 32px;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
  color: #873800;
  border: 1px solid #FFD591;
}

.welcome-text {
  position: relative;
  z-index: 1;
}

.welcome-title {
  margin: 0 0 4px;
  font-size: 22px;
  font-weight: 700;
  color: #873800;
}

.welcome-desc {
  margin: 0;
  color: #D46B08;
  font-size: 14px;
}

.welcome-decoration {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 300px;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(250, 140, 22, 0.05);
}

.decoration-circle--1 {
  width: 200px;
  height: 200px;
  right: -40px;
  top: -60px;
}

.decoration-circle--2 {
  width: 120px;
  height: 120px;
  right: 80px;
  bottom: -40px;
}

/* 统计卡片 */
.stat-row {
  margin-bottom: 4px;
}

.stat-card {
  border-radius: 16px;
  padding: 24px;
  position: relative;
  overflow: hidden;
  cursor: default;
  transition: all 0.25s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-card--orange {
  background: #FFFFFF;
  border: 1px solid #FFE7BA;
}

.stat-card--orange .stat-value { color: #FA8C16; }
.stat-card--orange .stat-label { color: #8C8C8C; }
.stat-card--orange .stat-icon-wrap { background: #FFF7E6; }
.stat-card--orange .stat-icon { color: #FA8C16; }

.stat-card--orange-light {
  background: #FFFFFF;
  border: 1px solid #FFE7BA;
}
.stat-card--orange-light .stat-value { color: #FFA940; }
.stat-card--orange-light .stat-label { color: #8C8C8C; }
.stat-card--orange-light .stat-icon-wrap { background: #FFFBE6; }
.stat-card--orange-light .stat-icon { color: #FFA940; }

.stat-card--orange-deep {
  background: #FFFFFF;
  border: 1px solid #FFD591;
}
.stat-card--orange-deep .stat-value { color: #FA8C16; }
.stat-card--orange-deep .stat-label { color: #8C8C8C; }
.stat-card--orange-deep .stat-icon-wrap { background: #FFF7E6; }
.stat-card--orange-deep .stat-icon { color: #FA8C16; }

.stat-card:hover {
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.15);
}

.stat-card-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 1;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 13px;
  color: #8C8C8C;
  margin-bottom: 4px;
  font-weight: 500;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.5px;
  line-height: 1.2;
}

.stat-trend {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 6px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}

.stat-icon {
  font-size: 24px;
  color: white;
}

.stat-shimmer {
  position: absolute;
  top: 0;
  left: -100%;
  width: 50%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.08), transparent);
  animation: shimmer 4s ease-in-out infinite;
}

@keyframes shimmer {
  0%   { transform: translateX(-100%); }
  100% { transform: translateX(400%); }
}

/* 图表卡片 */
.chart-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  height: 100%;
  transition: all 0.25s ease;
}

.chart-card:hover {
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1E293B;
}

/* 任务列表项 */
.task-item {
  transition: background 0.15s;
  border-radius: 8px;
  padding: 10px 8px !important;
}

.task-item:hover {
  background: #F8FAFF;
}

.task-link {
  font-weight: 500;
  color: #1E293B;
  transition: color 0.15s;
  cursor: pointer;
}

.task-link:hover {
  color: var(--color-primary);
}

/* 快捷操作 */
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1E293B;
  margin: 0 0 16px;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 24px 16px;
  border-radius: 16px;
  background: white;
  border: 1px solid rgba(226, 232, 240, 0.6);
  cursor: pointer;
  transition: all 0.25s;
}

.action-card:hover {
  border-color: rgba(37, 99, 235, 0.2);
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.08);
  transform: translateY(-2px);
}

.action-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-icon {
  font-size: 22px;
  color: var(--color-primary);
}

.action-label {
  font-size: 13px;
  font-weight: 500;
  color: #475569;
}

:deep(.ant-list-item) {
  border-bottom: 1px solid #F1F5F9 !important;
}

:deep(.ant-list-item:last-child) {
  border-bottom: none !important;
}
</style>