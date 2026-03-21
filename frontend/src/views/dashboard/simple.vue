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
    
    <!-- 快捷操作 -->
    <div class="quick-actions" style="margin-top: 20px">
      <h3 class="section-title">快捷操作</h3>
      <a-row :gutter="[16, 16]">
        <a-col :xs="12" :sm="6" v-for="action in quickActions" :key="action.label">
          <div class="action-card" @click="goTo(action.path)">
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
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'
import {
  UserOutlined,
  ProfileOutlined,
  CheckCircleOutlined,
  PlusCircleOutlined,
  RiseOutlined,
  FallOutlined,
  FileOutlined,
  UserAddOutlined,
<<<<<<< HEAD
=======
  FileAddOutlined,
>>>>>>> 974d00a7096735aae9219cfa167a551b72278b5f
  ApartmentOutlined
} from '@ant-design/icons-vue'
import { getDashboardStats, getDashboardCharts } from '@/api/dashboard'

const router = useRouter()

// 图表引用
const chartRef = ref<HTMLDivElement>()
const pieChartRef = ref<HTMLDivElement>()

// 快捷操作
const quickActions = ref([
  { label: '新增用户', icon: markRaw(UserAddOutlined), path: '/system/user', bg: 'linear-gradient(135deg, #EEF2FF, #E0E7FF)' },
  { label: '退税申请', icon: markRaw(FileOutlined), path: '/tax-refund/list', bg: 'linear-gradient(135deg, #ECFDF5, #D1FAE5)' },
  { label: '申报管理', icon: markRaw(ProfileOutlined), path: '/declaration/manage', bg: 'linear-gradient(135deg, #FFF7ED, #FED7AA)' },
  { label: '组织管理', icon: markRaw(ApartmentOutlined), path: '/system/org', bg: 'linear-gradient(135deg, #FFF1F2, #FECDD3)' },
])

// 统计卡片
const statCards = ref([
  { title: '用户总数', value: 0, icon: markRaw(UserOutlined), theme: 'indigo', trend: '', trendUp: true },
  { title: '总流程数', value: 0, icon: markRaw(ProfileOutlined), theme: 'emerald', trend: '', trendUp: true },
  { title: '流转中待办', value: 0, icon: markRaw(CheckCircleOutlined), theme: 'amber', trend: '', trendUp: false },
  { title: '今日新增单据', value: 0, icon: markRaw(PlusCircleOutlined), theme: 'rose', trend: '', trendUp: true },
])

// 加载统计数据
const loadStats = async () => {
  try {
    const response = await getDashboardStats()
    if (response.data?.code === 200) {
      const data = response.data.data || {}
      statCards.value[0].value = data.userCount || 0
      statCards.value[1].value = data.processInstanceCount || 0
      statCards.value[2].value = data.pendingTaskCount || 0
      statCards.value[3].value = data.todayNewCount || 0
      
      // 使用API返回的趋势数据（如果有）
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
      // API失败时使用默认值
      useDefaultStats()
    }
  } catch (error) {
    useDefaultStats()
  }
}

// 使用默认统计数据
const useDefaultStats = () => {
  statCards.value[0].value = 128
  statCards.value[1].value = 45
  statCards.value[2].value = 12
  statCards.value[3].value = 3
}

// 初始化图表
const initCharts = async () => {
  const colors = {
    indigo: '#6366F1',
    violet: '#8B5CF6',
    emerald: '#10B981',
    amber: '#F59E0B',
    rose: '#F43F5E',
  }

  try {
    const res: any = await getDashboardCharts();
    if (res.data?.code === 200) {
      const chartData = res.data.data || {};
      const processChart = chartData.processChart || { categories: [], seriesData: [] };
      const taskPieChart = chartData.taskPieChart || { seriesData: [] };
      
      if (chartRef.value) {
        const barChart = echarts.init(chartRef.value)
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
            data: taskPieChart.seriesData || []
          } as any]
        })
      }
    }
  } catch(e) {
    // 图表加载失败时不显示错误，保持页面正常
  }
}

const goTo = (path: string) => {
  try {
    router.push(path)
    message.success('页面跳转成功')
  } catch (error) {
    message.error('页面跳转失败')
  }
}

onMounted(() => {
  loadStats()
  initCharts()
  
  // 监听窗口大小变化，重新调整图表
  window.addEventListener('resize', () => {
    if (chartRef.value) {
      echarts.getInstanceByDom(chartRef.value)?.resize()
    }
    if (pieChartRef.value) {
      echarts.getInstanceByDom(pieChartRef.value)?.resize()
    }
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
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
  transition: left 1.5s;
}

.stat-card:hover .stat-shimmer {
  left: 100%;
}

.quick-links {
  max-width: 400px;
  margin: 20px auto 0;
}

/* 图表卡片 */
.chart-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 4px 12px rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.6);
  transition: all 0.3s ease;
}

.chart-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border-color: rgba(100, 116, 139, 0.2);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

/* 快速导航样式 */
.nav-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 4px 12px rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.6);
  margin-top: 20px;
}

.nav-card :deep(.ant-card-head) {
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  padding: 0 24px;
}

.nav-card :deep(.ant-card-head-title) {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  padding: 16px 0;
}

.nav-subtitle {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 400;
}

.nav-button {
  border-radius: 12px;
  font-weight: 500;
  transition: all 0.2s ease;
  height: 48px;
}

.nav-button--primary {
  background: linear-gradient(135deg, #3B82F6, #1D4ED8);
  border: none;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.nav-button--primary:hover {
  background: linear-gradient(135deg, #2563EB, #1E40AF);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.4);
  transform: translateY(-1px);
}

.nav-button--secondary {
  background: white;
  border: 1px solid rgba(226, 232, 240, 0.8);
  color: #64748B;
}

.nav-button--secondary:hover {
  border-color: #3B82F6;
  color: #3B82F6;
  background: rgba(59, 130, 246, 0.05);
  transform: translateY(-1px);
}

.nav-button :deep(.anticon) {
  font-size: 18px;
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
</style>