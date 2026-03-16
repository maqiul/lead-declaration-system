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
          <a-table :columns="processColumns" :data-source="processData" :pagination="false" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="getStatusColor(record.status)" :bordered="false">
                  {{ record.status }}
                </a-tag>
              </template>
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
          </a-list>
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
import { ref, onMounted, markRaw } from 'vue'
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

const router = useRouter()

// 统计卡片
const statCards = ref([
  { title: '用户总数', value: 112893, icon: markRaw(UserOutlined), theme: 'indigo', trend: '+12.5%', trendUp: true },
  { title: '流程实例数', value: 8765, icon: markRaw(ProfileOutlined), theme: 'emerald', trend: '+8.2%', trendUp: true },
  { title: '待办任务', value: 234, icon: markRaw(CheckCircleOutlined), theme: 'amber', trend: '-3.1%', trendUp: false },
  { title: '今日新增', value: 12, icon: markRaw(PlusCircleOutlined), theme: 'rose', trend: '+2', trendUp: true },
])

// 快捷操作
const quickActions = ref([
  { label: '新增用户', icon: markRaw(UserAddOutlined), path: '/system/user', bg: 'linear-gradient(135deg, #EEF2FF, #E0E7FF)' },
  { label: '创建流程', icon: markRaw(FileAddOutlined), path: '/workflow/definition', bg: 'linear-gradient(135deg, #ECFDF5, #D1FAE5)' },
  { label: '处理任务', icon: markRaw(CheckCircleOutlined), path: '/workflow/task', bg: 'linear-gradient(135deg, #FFF7ED, #FED7AA)' },
  { label: '组织管理', icon: markRaw(ApartmentOutlined), path: '/system/org', bg: 'linear-gradient(135deg, #FFF1F2, #FECDD3)' },
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

// 数据
const processData = ref([
  { key: '1', name: '请假审批流程', starter: '张三', status: '运行中', startTime: '2026-03-13 10:00' },
  { key: '2', name: '费用报销流程', starter: '李四', status: '已完成', startTime: '2026-03-13 09:30' },
  { key: '3', name: '合同审批流程', starter: '王五', status: '运行中', startTime: '2026-03-13 08:45' },
  { key: '4', name: '采购申请流程', starter: '赵六', status: '已终止', startTime: '2026-03-13 07:20' },
  { key: '5', name: '项目立项流程', starter: '钱七', status: '运行中', startTime: '2026-03-13 06:15' },
])

const taskData = ref([
  { id: 1, title: '部门经理审批 - 请假申请', description: '申请人：张三，类型：年假，天数：3天', time: '2小时前' },
  { id: 2, title: '财务审核 - 费用报销', description: '申请人：李四，金额：1200元', time: '1天前' },
  { id: 3, title: '法务审查 - 合同审批', description: '合同编号：HT20260313001', time: '2天前' },
  { id: 4, title: '总经理审批 - 重大项目', description: '项目：数字化转型，预算200万', time: '3天前' },
  { id: 5, title: '人事处理 - 入职手续', description: '新员工：孙八，岗位：Java工程师', time: '5天前' },
])

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
const initCharts = () => {
  const colors = {
    indigo: '#6366F1',
    violet: '#8B5CF6',
    emerald: '#10B981',
    amber: '#F59E0B',
    rose: '#F43F5E',
  }

  if (chartRef.value) {
    const barChart = echarts.init(chartRef.value)
    barChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, backgroundColor: 'rgba(15,23,42,0.9)', borderColor: 'transparent', textStyle: { color: '#fff' } },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '8%', containLabel: true },
      xAxis: [{ type: 'category', data: ['请假审批', '费用报销', '合同审批', '采购申请', '项目立项'], axisTick: { show: false }, axisLine: { lineStyle: { color: '#E2E8F0' } }, axisLabel: { color: '#64748B' } }],
      yAxis: [{ type: 'value', axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } }, axisLabel: { color: '#94A3B8' } }],
      series: [{
        name: '流程数量',
        type: 'bar',
        barWidth: '50%',
        data: [120, 200, 150, 80, 70],
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#3B82F6' },
            { offset: 1, color: '#1E40AF' }
          ])
        }
      }]
    })
  }
  
  if (pieChartRef.value) {
    const pieChart = echarts.init(pieChartRef.value)
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
        color: [colors.indigo, colors.violet, colors.emerald, colors.rose],
        data: [
          { value: 48, name: '待审批' },
          { value: 36, name: '处理中' },
          { value: 24, name: '已完成' },
          { value: 12, name: '已拒绝' }
        ]
      } as any]
    })
  }
}

onMounted(() => {
  initCharts()
  window.addEventListener('resize', () => {
    echarts.getInstanceByDom(chartRef.value!)?.resize()
    echarts.getInstanceByDom(pieChartRef.value!)?.resize()
  })
})
</script>

<style scoped>
.dashboard {
  min-height: 100%;
}

/* 欢迎横幅 */
.welcome-banner {
  background: linear-gradient(135deg, #1E40AF 0%, #3B82F6 100%);
  border-radius: 20px;
  padding: 28px 32px;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
  color: white;
}

.welcome-text {
  position: relative;
  z-index: 1;
}

.welcome-title {
  margin: 0 0 4px;
  font-size: 22px;
  font-weight: 700;
  color: white;
}

.welcome-desc {
  margin: 0;
  color: rgba(255, 255, 255, 0.7);
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
  background: rgba(255, 255, 255, 0.08);
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

.stat-card--indigo {
  background: linear-gradient(135deg, #1E40AF, #1D4ED8);
  box-shadow: 0 4px 16px rgba(30, 64, 175, 0.25);
}

.stat-card--emerald {
  background: linear-gradient(135deg, #059669, #10B981);
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.25);
}

.stat-card--amber {
  background: linear-gradient(135deg, #D97706, #F59E0B);
  box-shadow: 0 4px 16px rgba(245, 158, 11, 0.25);
}

.stat-card--rose {
  background: linear-gradient(135deg, #E11D48, #F43F5E);
  box-shadow: 0 4px 16px rgba(244, 63, 94, 0.25);
}

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
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 4px;
  font-weight: 500;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: white;
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
  border-radius: 16px;
  padding: 24px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  height: 100%;
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
  color: #4F46E5;
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
  border-color: rgba(79, 70, 229, 0.2);
  box-shadow: 0 4px 16px rgba(79, 70, 229, 0.08);
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
  color: #4F46E5;
}

.action-label {
  font-size: 13px;
  font-weight: 500;
  color: #475569;
}

/* 覆盖 Ant Design */
:deep(.ant-table) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.ant-table-thead > tr > th) {
  background: #F8FAFC !important;
  font-weight: 600;
  color: #475569;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid #EEF2FF !important;
}

:deep(.ant-list-item) {
  border-bottom: 1px solid #F1F5F9 !important;
}

:deep(.ant-list-item:last-child) {
  border-bottom: none !important;
}
</style>