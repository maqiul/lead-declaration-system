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

    <!-- 快捷操作：内部/外部申报流程（上移到顶部） -->
    <div class="workflow-section">
      <h3 class="section-title">申报流程快捷入口</h3>
      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :lg="12" v-for="group in visibleWorkflowGroups" :key="group.type">
          <div
            class="workflow-group"
            :class="{
              'workflow-group--active': group.type === userOrgType,
              'workflow-group--self': group.type === 'SELF',
              'workflow-group--external': group.type === 'EXTERNAL'
            }"
          >
            <div class="workflow-group-header">
              <div class="workflow-group-title">
                <span class="workflow-group-icon">
                  <component :is="group.icon" />
                </span>
                <span class="workflow-group-label">{{ group.label }}</span>
              </div>
            </div>
            <div class="workflow-group-body">
              <div class="workflow-steps-row">
                <div class="workflow-step" v-for="step in group.steps" :key="step.label" @click="goTo(step.path)">
                  <div class="workflow-step-icon" :style="{ background: step.bg }">
                    <component :is="step.icon" />
                  </div>
                  <span class="workflow-step-label">{{ step.label }}</span>
                </div>
              </div>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>

    <!-- 菜单统计卡片：内部/外部合并展示 -->
    <div style="margin-top: 24px">
      <h3 class="section-title">申报作业</h3>
      <a-row :gutter="[20, 20]" class="stat-row">
        <a-col :xs="24" :sm="12" :lg="8" v-for="(stat, index) in mergedStats" :key="index">
          <div class="stat-card" :class="`stat-card--${stat.theme}`">
            <div class="stat-card-inner">
              <div class="stat-info">
                <span class="stat-label">{{ stat.menuName }}</span>
                <div class="stat-counts">
                  <span v-if="canViewInternal" class="stat-count stat-count--internal" @click.stop="goTo(toDtPath(stat.path, 'SELF'))">
                    <span class="stat-count-tag">梓熠、理德</span>
                    <span class="stat-count-num">{{ stat.internal.toLocaleString() }}</span>
                  </span>
                  <span v-if="canViewExternal" class="stat-count stat-count--external" @click.stop="goTo(toDtPath(stat.path, 'EXTERNAL'))">
                    <span class="stat-count-tag">集洛</span>
                    <span class="stat-count-num">{{ stat.external.toLocaleString() }}</span>
                  </span>
                </div>
              </div>
              <div class="stat-icon-wrap">
                <component :is="getIcon(stat.icon)" class="stat-icon" />
              </div>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>

    <!-- 30天预警模块 -->
    <div class="warning-section" style="margin-top: 24px">
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">
            <WarningOutlined style="color: #faad14; margin-right: 8px" />
            30天预警
          </h3>
          <a-badge :count="filteredWarningCount" :number-style="{ backgroundColor: '#faad14' }" />
        </div>
        <a-tabs
          v-if="warningTabs.length > 1"
          v-model:activeKey="warningTab"
          size="small"
          style="margin-bottom: 12px"
        >
          <a-tab-pane v-for="tab in warningTabs" :key="tab.key" :tab="tab.label" />
        </a-tabs>
        <a-table
          v-if="filteredWarningList.length"
          :columns="warningColumns"
          :data-source="filteredWarningList"
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
              {{ fmtDateTime(record.createTime) || '-' }}
            </template>
            <template v-if="column.key === 'daysOverdue'">
              <span style="color: #ff4d4f; font-weight: 600">{{ record.daysOverdue }} 天</span>
            </template>
          </template>
        </a-table>
        <a-empty v-else description="暂无超期预警记录" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, markRaw, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import {
  EditOutlined,
  UploadOutlined,
  FileSearchOutlined,
  DollarOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  WarningOutlined,
  PlusCircleOutlined,
  ShopOutlined,
  TeamOutlined,
  AccountBookOutlined,
  FileAddOutlined
} from '@ant-design/icons-vue'
import type { Component } from 'vue'
import { getDeclarationStats } from '@/api/dashboard'
import { formatDate as fmtDateTime } from '@/utils/common'

const router = useRouter()
const userStore = useUserStore()
const userOrgType = computed(() => userStore.orgType === 'INTERNAL' ? 'SELF' : 'EXTERNAL')

// 权限标志
const canViewInternal = ref(false)
const canViewExternal = ref(false)

// 菜单统计
interface MenuStat {
  menuName: string
  path: string
  icon: string
  theme: string
  count: number
}
const internalMenuStats = ref<MenuStat[]>([])
const externalMenuStats = ref<MenuStat[]>([])

// 合并内部/外部统计
interface MergedStat {
  menuName: string
  path: string
  icon: string
  theme: string
  internal: number
  external: number
  total: number
}
const mergedStats = computed<MergedStat[]>(() => {
  const int = internalMenuStats.value
  const ext = externalMenuStats.value
  // 按 menuName 匹配，避免数组顺序不一致导致数据错位
  const intMap = new Map(int.map(s => [s.menuName, s]))
  const extMap = new Map(ext.map(s => [s.menuName, s]))
  // 合并所有 menuName（保持顺序）
  const names = [...new Set([...int.map(s => s.menuName), ...ext.map(s => s.menuName)])]
  return names.map(name => {
    const i = intMap.get(name)
    const e = extMap.get(name)
    const ref = i || e!
    return {
      menuName: name,
      path: ref.path,
      icon: ref.icon,
      theme: ref.theme,
      internal: i?.count ?? 0,
      external: e?.count ?? 0,
      total: (i?.count ?? 0) + (e?.count ?? 0),
    }
  })
})

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
  declarationType?: string
}
const warningCount = ref(0)
const warningList = ref<WarningItem[]>([])
const warningTab = ref<string>('all')

const warningTabs = computed(() => {
  const tabs: { key: string; label: string }[] = []
  if (canViewInternal.value && canViewExternal.value) {
    tabs.push({ key: 'all', label: '全部' })
  }
  if (canViewInternal.value) {
    tabs.push({ key: 'SELF', label: '梓熠、理德' })
  }
  if (canViewExternal.value) {
    tabs.push({ key: 'EXTERNAL', label: '集洛' })
  }
  return tabs
})

const filteredWarningList = computed(() => {
  if (warningTab.value === 'all') return warningList.value
  return warningList.value.filter(item => item.declarationType === warningTab.value)
})

const filteredWarningCount = computed(() => filteredWarningList.value.length)

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
  { title: '创建时间', key: 'createTime', dataIndex: 'createTime' , customRender: ({ text }: any) => text ? fmtDateTime(text, 'yyyy-MM-dd HH:mm:ss') : '-' },
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

// 申报流程分组
interface WorkflowStep {
  label: string
  icon: Component
  path: string
  bg: string
}
interface WorkflowGroup {
  type: 'SELF' | 'EXTERNAL'
  label: string
  icon: Component
  steps: WorkflowStep[]
}

// 从后端 menuStats 动态生成申报流程快捷入口
const stepGradients = [
  'linear-gradient(135deg, #3B82F6, #1D4ED8)',
  'linear-gradient(135deg, #10B981, #059669)',
  'linear-gradient(135deg, #6366F1, #4F46E5)',
  'linear-gradient(135deg, #EC4899, #DB2777)',
  'linear-gradient(135deg, #F59E0B, #D97706)',
  'linear-gradient(135deg, #8B5CF6, #7C3AED)',
]

const stepIconFallback: Record<string, Component> = {
  '申报录入': markRaw(PlusCircleOutlined),
  '资料提交': markRaw(UploadOutlined),
  '补充资料': markRaw(FileAddOutlined),
  '开票金额': markRaw(AccountBookOutlined),
  '发票提交': markRaw(FileTextOutlined),
  '归档查询': markRaw(FolderOpenOutlined),
}

const workflowGroups = computed<WorkflowGroup[]>(() => {
  const buildSteps = (stats: MenuStat[], prefix: string) =>
    stats.map((s, i) => ({
      label: s.menuName,
      icon: stepIconFallback[s.menuName] || getIcon(s.icon),
      path: toDtPath(s.path, prefix === '/declaration-self' ? 'SELF' : 'EXTERNAL'),
      bg: stepGradients[i % stepGradients.length],
    }))
  return [
    { type: 'SELF' as const, label: '梓熠、理德申报', icon: markRaw(ShopOutlined), steps: buildSteps(internalMenuStats.value, '/declaration-self') },
    { type: 'EXTERNAL' as const, label: '集洛申报', icon: markRaw(TeamOutlined), steps: buildSteps(externalMenuStats.value, '/declaration-external') },
  ]
})

const visibleWorkflowGroups = computed(() =>
  workflowGroups.value.filter(g =>
    g.type === 'SELF' ? canViewInternal.value : canViewExternal.value
  )
)

async function loadStats() {
  try {
    const res = await getDeclarationStats() as any
    const data = res.data?.data || res.data || {}

    // 权限标志
    canViewInternal.value = data.canViewInternal ?? false
    canViewExternal.value = data.canViewExternal ?? false

    // 根据权限设置默认预警 tab
    if (canViewInternal.value && canViewExternal.value) {
      warningTab.value = 'all'
    } else if (canViewInternal.value) {
      warningTab.value = 'SELF'
    } else if (canViewExternal.value) {
      warningTab.value = 'EXTERNAL'
    }

    // 分组统计
    internalMenuStats.value = data.internalMenuStats || []
    externalMenuStats.value = data.externalMenuStats || []

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

/** 将后端返回的旧路径 + declarationType 转为新路径前缀 */
const toDtPath = (basePath: string, dt: string) => {
  const child = basePath.replace(/^\/declaration\/?/, '')
  const prefix = dt === 'SELF' ? '/declaration-self' : '/declaration-external'
  return `${prefix}/${child}`
}

// 根据申报单状态跳转到对应菜单
const goToWarning = (record: WarningItem) => {
  const dt = (record as any).declarationType || 'EXTERNAL'
  const prefix = dt === 'SELF' ? '/declaration-self' : '/declaration-external'
  const status = record.status
  let child = 'entry'
  
  if (status === 2 || status === 3) {
    child = 'material'
  } else if (status === 4 || status === 5) {
    child = 'supplement'
  } else if (status === 6 || status === 7) {
    child = 'invoice-amount'
  } else if (status === 8 || status === 9) {
    child = 'invoice'
  } else if (status === 10) {
    child = 'archive'
  }
  // status 0, 1, 11 都跳转到申报录入
  
  router.push(`${prefix}/${child}?id=${record.id}&declarationType=${dt}`)
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

/* 申报流程区域 */
.workflow-section {
  margin-bottom: 24px;
}

.section-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1E293B;
}

.workflow-group {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid #E2E8F0;
  transition: all 0.25s;
}

.workflow-group:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.workflow-group--active {
  border-color: #3B82F6;
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.15);
}

.workflow-group--self {
  border-left: 3px solid #3B82F6;
}

.workflow-group--external {
  border-left: 3px solid #10B981;
}

.workflow-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;
}

.workflow-group-header:hover {
  background: #F8FAFC;
}

.workflow-group-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.workflow-group-icon {
  font-size: 18px;
  color: #3B82F6;
}

.workflow-group--external .workflow-group-icon {
  color: #10B981;
}

.workflow-group-label {
  font-size: 15px;
  font-weight: 600;
  color: #1E293B;
}

.workflow-group-arrow {
  font-size: 12px;
  color: #94A3B8;
  transition: transform 0.25s;
}

.workflow-group-arrow.arrow-up {
  transform: rotate(180deg);
}

.workflow-group-body {
  padding: 4px 16px 16px;
}

.workflow-steps-row {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  overflow-x: auto;
}

.workflow-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 12px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  flex: 1 1 0;
  min-width: 0;
}

.workflow-step:hover {
  background: #F1F5F9;
  transform: translateY(-2px);
}

.workflow-step-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
}

.workflow-step-label {
  font-size: 12px;
  color: #475569;
  font-weight: 500;
  text-align: center;
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

/* 内部/外部 数量 */
.stat-counts {
  display: flex;
  gap: 12px;
  margin-top: 4px;
}

.stat-count {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  cursor: pointer;
  padding: 2px 8px 2px 4px;
  border-radius: 6px;
  transition: background 0.2s;
}

.stat-count:hover {
  background: rgba(255, 255, 255, 0.18);
}

.stat-count-tag {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.65);
  font-weight: 500;
}

.stat-count-num {
  font-size: 28px;
  font-weight: 700;
  color: white;
  line-height: 1.2;
}

.stat-count--internal .stat-count-tag {
  color: rgba(255, 255, 255, 0.75);
}

.stat-count--external .stat-count-tag {
  color: rgba(255, 255, 255, 0.55);
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
</style>