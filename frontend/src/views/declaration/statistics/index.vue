<template>
  <div class="declaration-statistics">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">申报统计分析</h2>
      <p class="page-desc">查看申报单数据统计和趋势分析</p>
    </div>
    
    <!-- 筛选区域 -->
    <div class="filter-section">
      <a-card :bordered="false" class="filter-card">
        <div class="filter-content">
          <div class="filter-item">
            <span class="filter-label">时间范围：</span>
            <a-range-picker 
              v-model:value="dateRange" 
              :placeholder="['开始日期', '结束日期']"
              @change="handleDateChange"
              class="date-picker"
            />
          </div>
          <div class="filter-actions">
            <a-button type="primary" @click="loadStatistics" :loading="loading" v-permission="['business:declaration:view']">
              <template #icon>
                <ReloadOutlined />
              </template>
              刷新数据
            </a-button>
            <a-button @click="exportData" v-permission="['business:declaration:export']">
              <template #icon>
                <DownloadOutlined />
              </template>
              导出报表
            </a-button>
          </div>
        </div>
      </a-card>
    </div>
    
    <!-- 加载状态 -->
    <a-spin :spinning="loading" tip="加载中...">
      <!-- 统计概览卡片 -->
      <a-row :gutter="[20, 20]" class="stats-grid">
        <a-col :xs="24" :sm="12" :lg="6" v-for="(stat, index) in statCards" :key="index">
          <div class="stat-card" :class="`stat-card--${stat.theme}`">
            <div class="stat-card-inner">
              <div class="stat-info">
                <span class="stat-label">{{ stat.title }}</span>
                <span class="stat-value">{{ formatNumber(stat.value) }}</span>
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
      <a-row :gutter="[20, 20]" class="chart-section">
        <a-col :xs="24" :lg="12">
          <div class="chart-card">
            <div class="chart-header">
              <h3 class="chart-title">申报状态分布</h3>
              <a-tag color="blue" :bordered="false">实时</a-tag>
            </div>
            <div class="chart-placeholder">
              <BarChartOutlined style="font-size: 48px; color: #3B82F6; margin-bottom: 16px;" />
              <div>状态分布图表</div>
              <div class="chart-desc">按不同状态的申报单数量分布</div>
            </div>
          </div>
        </a-col>
        <a-col :xs="24" :lg="12">
          <div class="chart-card">
            <div class="chart-header">
              <h3 class="chart-title">热门目的地TOP5</h3>
              <a-tag color="purple" :bordered="false">本月</a-tag>
            </div>
            <div class="chart-placeholder">
              <PieChartOutlined style="font-size: 48px; color: #8B5CF6; margin-bottom: 16px;" />
              <div>目的地分布图表</div>
              <div class="chart-desc">申报单目的地国家/地区排名</div>
            </div>
          </div>
        </a-col>
      </a-row>
      
      <!-- 详细统计表 -->
      <div class="detail-section">
        <a-card class="detail-card">
          <div class="card-header">
            <h3 class="card-title">详细统计数据</h3>
            <p class="card-subtitle">按不同维度查看详细的申报单统计数据</p>
          </div>
          
          <a-tabs v-model:activeKey="activeTab" class="detail-tabs">
            <a-tab-pane key="status" tab="按状态统计">
              <div class="table-actions">
                <a-input-search 
                  placeholder="搜索状态..." 
                  style="width: 200px; margin-right: 16px;"
                />
                <a-button type="primary" size="small">导出Excel</a-button>
              </div>
              <a-table 
                :dataSource="statusStats" 
                :columns="statusColumns" 
                :pagination="{ pageSize: 10 }"
                size="middle"
                :scroll="{ x: 600 }"
                :locale="{ emptyText: '暂无数据' }"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'amount'">
                    ${{ formatNumber(record.amount) }}
                  </template>
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="product" tab="热门产品">
              <div class="table-actions">
                <a-input-search 
                  placeholder="搜索产品..." 
                  style="width: 200px; margin-right: 16px;"
                />
                <a-button type="primary" size="small">导出Excel</a-button>
              </div>
              <a-table 
                :dataSource="productStats" 
                :columns="productColumns" 
                :pagination="{ pageSize: 10 }"
                size="middle"
                :scroll="{ x: 800 }"
                :locale="{ emptyText: '暂无数据' }"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'totalAmount'">
                    ${{ formatNumber(record.totalAmount) }}
                  </template>
                </template>
              </a-table>
            </a-tab-pane>
            <a-tab-pane key="destination" tab="目的地统计">
              <div class="table-actions">
                <a-input-search 
                  placeholder="搜索目的地..." 
                  style="width: 200px; margin-right: 16px;"
                />
                <a-button type="primary" size="small">导出Excel</a-button>
              </div>
              <a-table 
                :dataSource="destinationStats" 
                :columns="destinationColumns" 
                :pagination="{ pageSize: 10 }"
                size="middle"
                :scroll="{ x: 600 }"
                :locale="{ emptyText: '暂无数据' }"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'totalAmount'">
                    ${{ formatNumber(record.totalAmount) }}
                  </template>
                </template>
              </a-table>
            </a-tab-pane>
          </a-tabs>
        </a-card>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, markRaw } from 'vue'
import { message } from 'ant-design-vue'
import { getDeclarationStatistics, type StatisticsData } from '@/api/business/declaration'
import { ReloadOutlined, DownloadOutlined, FileTextOutlined, CalendarOutlined, 
         DollarCircleOutlined, BarChartOutlined, PieChartOutlined, RiseOutlined, FallOutlined } from '@ant-design/icons-vue'
import type { Dayjs } from 'dayjs'

// 加载状态
const loading = ref(false)

// 时间范围筛选
const dateRange = ref<[string, string] | undefined>(undefined)

// 统计数据
const statistics = reactive({
  totalForms: 0,
  monthForms: 0,
  totalAmount: 0,
  avgAmount: 0
})

// 统计卡片数据
const statCards = ref([
  { title: '总申报单数', value: 0, icon: markRaw(FileTextOutlined), theme: 'indigo', trend: '', trendUp: true },
  { title: '本月申报数', value: 0, icon: markRaw(CalendarOutlined), theme: 'emerald', trend: '', trendUp: true },
  { title: '总申报金额', value: 0, icon: markRaw(DollarCircleOutlined), theme: 'amber', trend: '', trendUp: true },
  { title: '平均申报金额', value: 0, icon: markRaw(BarChartOutlined), theme: 'rose', trend: '', trendUp: true },
])

// 活动标签页
const activeTab = ref('status')

// 状态统计数据
const statusStats = ref<Array<{ status: string; count: number; amount: number }>>([])

// 产品统计数据
const productStats = ref<Array<{ productName: string; hsCode: string; count: number; totalAmount: number }>>([])

// 目的地统计数据
const destinationStats = ref<Array<{ destination: string; count: number; totalAmount: number }>>([])

// 表格列配置
const statusColumns = [
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '申报单数', dataIndex: 'count', key: 'count' },
  { title: '总金额(USD)', dataIndex: 'amount', key: 'amount' }
]

const productColumns = [
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: 'HS编码', dataIndex: 'hsCode', key: 'hsCode' },
  { title: '申报次数', dataIndex: 'count', key: 'count' },
  { title: '总金额(USD)', dataIndex: 'totalAmount', key: 'totalAmount' }
]

const destinationColumns = [
  { title: '目的地', dataIndex: 'destination', key: 'destination' },
  { title: '申报单数', dataIndex: 'count', key: 'count' },
  { title: '总金额(USD)', dataIndex: 'totalAmount', key: 'totalAmount' }
]

// 格式化数字
const formatNumber = (num: number | string): string => {
  // 确保输入是数字类型
  const numericValue = typeof num === 'string' ? parseFloat(num) : num;
  
  // 处理NaN或无效值
  if (isNaN(numericValue) || numericValue === undefined || numericValue === null) {
    return '0.00';
  }
  
  if (numericValue >= 1000000) {
    return (numericValue / 1000000).toFixed(2) + 'M'
  } else if (numericValue >= 1000) {
    return (numericValue / 1000).toFixed(2) + 'K'
  }
  return numericValue.toFixed(2)
}

// 处理时间范围变化
const handleDateChange = (dates: [string, string] | [Dayjs, Dayjs] | null, dateStrings: [string, string]) => {
  if (dates && dateStrings[0] && dateStrings[1]) {
    dateRange.value = dateStrings
    loadStatistics()
  } else {
    dateRange.value = undefined
    loadStatistics()
  }
}

// 导出数据
const exportData = () => {
  message.info('导出功能开发中...')
  // TODO: 实现导出功能
}

// 加载统计数据
const loadStatistics = async () => {
  loading.value = true
  try {
    console.log('开始加载统计数据...');
    const res = await getDeclarationStatistics()
    console.log('API响应:', res);
    
    if (res.data?.code === 200 && res.data?.data) {
      const data = res.data.data as StatisticsData
      console.log('统计数据:', data);
      
      // 更新概览数据
      statistics.totalForms = data.totalForms || 0
      statistics.monthForms = data.monthForms || 0
      statistics.totalAmount = data.totalAmount !== undefined ? Number(data.totalAmount) : 0
      statistics.avgAmount = data.avgAmount !== undefined ? Number(data.avgAmount) : 0
      
      // 更新统计卡片数据
      statCards.value[0].value = statistics.totalForms
      statCards.value[1].value = statistics.monthForms
      statCards.value[2].value = statistics.totalAmount
      statCards.value[3].value = statistics.avgAmount
      
      console.log('更新后的statCards:', statCards.value);
      
      // 更新详细统计数据
      statusStats.value = data.statusStats || []
      productStats.value = data.productStats || []
      destinationStats.value = data.destinationStats || []
    } else {
      console.error('API返回错误:', res.data);
      message.error(res.data?.message || '加载统计数据失败')
    }
  } catch (error: any) {
    console.error('加载统计数据失败:', error)
    message.error('加载统计数据失败')
    // API调用失败时显示空状态
    statistics.totalForms = 0
    statistics.monthForms = 0
    statistics.totalAmount = 0
    statistics.avgAmount = 0
    
    // 更新统计卡片数据为默认值
    statCards.value[0].value = 0
    statCards.value[1].value = 0
    statCards.value[2].value = 0
    statCards.value[3].value = 0
    
    statusStats.value = []
    productStats.value = []
    destinationStats.value = []
  } finally {
    loading.value = false
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.declaration-statistics {
  min-height: 100%;
  padding: 24px;
}

/* 页面头部 */
.page-header {
  margin-bottom: 24px;
}

.page-title {
  margin: 0 0 4px;
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
}

.page-desc {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

/* 筛选区域 */
.filter-section {
  margin-bottom: 24px;
}

.filter-card {
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 4px 12px rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.filter-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-weight: 500;
  color: #334155;
}

.date-picker {
  width: 240px;
}

.filter-actions {
  display: flex;
  gap: 12px;
}

/* 统计卡片网格 */
.stats-grid {
  margin-bottom: 24px;
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

/* 图表区域 */
.chart-section {
  margin-bottom: 24px;
}

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

.chart-placeholder {
  text-align: center;
  padding: 40px 20px;
  color: #64748b;
}

.chart-desc {
  font-size: 14px;
  margin-top: 8px;
  opacity: 0.7;
}

/* 详细统计区域 */
.detail-section {
  margin-top: 24px;
}

.detail-card {
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 4px 12px rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.card-header {
  margin-bottom: 24px;
}

.card-title {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.card-subtitle {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.detail-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 24px;
}

.detail-tabs :deep(.ant-tabs-tab) {
  font-size: 15px;
  font-weight: 500;
  padding: 12px 0;
  margin-right: 32px;
}

.table-actions {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .declaration-statistics {
    padding: 16px;
  }
  
  .filter-content {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-actions {
    justify-content: center;
  }
  
  .table-actions {
    flex-direction: column;
    align-items: stretch;
  }
  
  .stat-value {
    font-size: 24px;
  }
}

@media (max-width: 576px) {
  .stat-card-inner {
    flex-direction: column;
    text-align: center;
  }
  
  .stat-icon-wrap {
    margin-top: 16px;
  }
}
</style>