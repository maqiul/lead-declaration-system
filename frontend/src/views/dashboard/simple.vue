<template>
  <div class="dashboard">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-text">
        <h2 class="welcome-title">欢迎回来 </h2>
        <p class="welcome-desc">这是您的工作台概览，今天也要加油哦</p>
      </div>
      <div class="welcome-decoration">
        <div class="decoration-circle decoration-circle--1"></div>
        <div class="decoration-circle decoration-circle--2"></div>
      </div>
    </div>

    <!-- 菜单统计卡片 -->
    <a-row :gutter="[20, 20]" class="stat-row">
      <a-col :xs="24" :sm="12" :lg="8" v-for="(stat, index) in menuStats" :key="index">
        <div class="stat-card" :class="`stat-card--${stat.theme}`" @click="goTo(stat.path)">
          <div class="stat-card-inner">
            <div class="stat-info">
              <span class="stat-label">{{ stat.menuName }}</span>
              <span class="stat-value">{{ stat.count.toLocaleString() }}</span>
            </div>
            <div class="stat-icon-wrap">
              <component :is="getIcon(stat.icon)" class="stat-icon" />
            </div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 30天预警模块 -->
    <div class="warning-section" style="margin-top: 24px">
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">
            <WarningOutlined style="color: #faad14; margin-right: 8px" />
            30天预警
          </h3>
          <a-badge :count="warningCount" :number-style="{ backgroundColor: '#faad14' }" />
        </div>
        <a-table
          v-if="warningList.length"
          :columns="warningColumns"
          :data-source="warningList"
          :pagination="{ pageSize: 10, size: 'small', showTotal: (total: number) => `共 ${total} 条` }"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'formNo'">
              <a-button type="link" @click="goToWarning(record as WarningItem)">
                {{ record.formNo }}
              </a-button>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">{{ getStatusLabel(record.status) }}</a-tag>
            </template>
            <template v-if="column.key === 'totalAmount'">
              {{ record.currencySymbol || '' }}{{ record.totalAmount?.toFixed(2) || '0.00' }}
            </template>
            <template v-if="column.key === 'createTime'">
              {{ formatDate(record.createTime) }}
            </template>
            <template v-if="column.key === 'daysOverdue'">
              <span style="color: #ff4d4f; font-weight: 600">{{ record.daysOverdue }} 天</span>
            </template>
          </template>
        </a-table>
        <a-empty v-else description="暂无超期预警记录" />
      </div>
    </div>

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
import {
  EditOutlined,
  UploadOutlined,
  FileSearchOutlined,
  DollarOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  WarningOutlined,
  PlusCircleOutlined,
  UnorderedListOutlined
} from '@ant-design/icons-vue'
import type { Component } from 'vue'
import { getDeclarationStats } from '@/api/dashboard'

const router = useRouter()

// 菜单统计
interface MenuStat {
  menuName: string
  path: string
  icon: string
  theme: string
  count: number
}
const menuStats = ref<MenuStat[]>([])

// 30天预警
interface WarningItem {
  id: number
  formNo: string
  shipperCompany: string
  status: number
  createTime: string
  totalAmount: number
  destinationCountry: string
  daysOverdue: number
  declarantName: string
  currency: string
  currencySymbol: string
}
const warningCount = ref(0)
const warningList = ref<WarningItem[]>([])

const iconMap: Record<string, Component> = {
  EditOutlined: markRaw(EditOutlined),
  UploadOutlined: markRaw(UploadOutlined),
  FileSearchOutlined: markRaw(FileSearchOutlined),
  DollarOutlined: markRaw(DollarOutlined),
  FileTextOutlined: markRaw(FileTextOutlined),
  FolderOpenOutlined: markRaw(FolderOpenOutlined),
}

function getIcon(name: string): Component {
  return iconMap[name] || markRaw(FileTextOutlined)
}

const warningColumns = [
  { title: '申报单号', key: 'formNo', dataIndex: 'formNo' },
  { title: '申报人', key: 'declarantName', dataIndex: 'declarantName' },
  { title: '发货人', key: 'shipperCompany', dataIndex: 'shipperCompany' },
  { title: '目的国', key: 'destinationCountry', dataIndex: 'destinationCountry' },
  { title: '总金额', key: 'totalAmount', dataIndex: 'totalAmount' },
  { title: '状态', key: 'status', dataIndex: 'status' },
  { title: '创建时间', key: 'createTime', dataIndex: 'createTime' },
  { title: '超期天数', key: 'daysOverdue', dataIndex: 'daysOverdue' },
]

const statusMap: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '待初审', color: 'blue' },
  2: { label: '待资料提交', color: 'cyan' },
  3: { label: '待资料审核', color: 'processing' },
  4: { label: '待补充资料', color: 'orange' },
  5: { label: '待补充审核', color: 'warning' },
  6: { label: '待开票金额', color: 'purple' },
  7: { label: '待开票审核', color: 'magenta' },
  8: { label: '待发票提交', color: 'geekblue' },
  9: { label: '待发票审核', color: 'volcano' },
  10: { label: '已归档', color: 'success' },
  11: { label: '退回待审', color: 'error' },
}

function getStatusColor(status: number): string {
  return statusMap[status]?.color || 'default'
}

function getStatusLabel(status: number): string {
  return statusMap[status]?.label || '未知'
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const quickActions = [
  { label: '新建申报', icon: markRaw(PlusCircleOutlined), path: '/declaration/entry', bg: 'linear-gradient(135deg, #3B82F6, #1D4ED8)' },
  { label: '资料提交', icon: markRaw(UploadOutlined), path: '/declaration/material', bg: 'linear-gradient(135deg, #10B981, #059669)' },
  { label: '发票提交', icon: markRaw(FileTextOutlined), path: '/declaration/invoice', bg: 'linear-gradient(135deg, #F59E0B, #D97706)' },
  { label: '归档查询', icon: markRaw(UnorderedListOutlined), path: '/declaration/archive', bg: 'linear-gradient(135deg, #8B5CF6, #7C3AED)' },
]

async function loadStats() {
  try {
    const res = await getDeclarationStats() as any
    const data = res.data?.data || res.data || {}
    menuStats.value = data.menuStats || []

    const list = (data.warningList || []) as any[]
    warningCount.value = data.warningCount || list.length
    warningList.value = list.map((item: any) => ({
      ...item,
      daysOverdue: Math.floor((Date.now() - new Date(item.createTime).getTime()) / 86400000),
    }))
  } catch {
    // 加载失败静默处理
  }
}

const goTo = (path: string) => {
  router.push(path)
}

// 根据申报单状态跳转到对应菜单
const goToWarning = (record: WarningItem) => {
  const status = record.status
  let path = '/declaration/entry'
  
  if (status === 2 || status === 3) {
    path = '/declaration/material'
  } else if (status === 4 || status === 5) {
    path = '/declaration/supplement'
  } else if (status === 6 || status === 7) {
    path = '/declaration/invoice-amount'
  } else if (status === 8 || status === 9) {
    path = '/declaration/invoice'
  } else if (status === 10) {
    path = '/declaration/archive'
  }
  // status 0, 1, 11 都跳转到申报录入
  
  router.push(`${path}?id=${record.id}`)
}

onMounted(() => {
  loadStats()
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
  cursor: pointer;
  transition: all 0.25s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.stat-card--blue {
  background: linear-gradient(135deg, #1E40AF, #3B82F6);
  box-shadow: 0 4px 16px rgba(30, 64, 175, 0.25);
}

.stat-card--green {
  background: linear-gradient(135deg, #059669, #10B981);
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.25);
}

.stat-card--orange {
  background: linear-gradient(135deg, #D97706, #F59E0B);
  box-shadow: 0 4px 16px rgba(245, 158, 11, 0.25);
}

.stat-card--purple {
  background: linear-gradient(135deg, #7C3AED, #8B5CF6);
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.25);
}

.stat-card--cyan {
  background: linear-gradient(135deg, #0891B2, #06B6D4);
  box-shadow: 0 4px 16px rgba(6, 182, 212, 0.25);
}

.stat-card--default {
  background: linear-gradient(135deg, #475569, #64748B);
  box-shadow: 0 4px 16px rgba(100, 116, 139, 0.25);
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
  gap: 8px;
}

.stat-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: white;
  line-height: 1;
}

.stat-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon {
  font-size: 26px;
  color: white;
}

/* 图表卡片 */
.chart-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1E293B;
}

/* 快捷操作 */
.section-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1E293B;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px 16px;
  background: white;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.action-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.action-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-icon {
  font-size: 22px;
  color: white;
}

.action-label {
  font-size: 13px;
  color: #475569;
  font-weight: 500;
}
</style>
