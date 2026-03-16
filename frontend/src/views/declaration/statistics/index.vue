<template>
  <div class="declaration-statistics">
    <!-- 统计概览 -->
    <a-row :gutter="16" class="stats-overview">
      <a-col :span="6">
        <a-card class="stat-card">
          <a-statistic
            title="总申报单数"
            :value="statistics.totalForms"
            :value-style="{ color: '#1890ff' }"
          />
        </a-card>
      </a-col>
        <a-col :span="6">
          <a-card class="stat-card">
            <a-statistic
              title="本月申报数"
              :value="statistics.monthForms"
              :value-style="{ color: '#52c41a' }"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-card">
            <a-statistic
              title="总申报金额"
              :value="statistics.totalAmount"
              :precision="2"
              suffix="USD"
              :value-style="{ color: '#fa8c16' }"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-card">
            <a-statistic
              title="平均申报金额"
              :value="statistics.avgAmount"
              :precision="2"
              suffix="USD"
              :value-style="{ color: '#722ed1' }"
            />
          </a-card>
        </a-col>
      </a-row>
      
      <!-- 详细统计表 -->
      <a-card title="详细统计" class="detail-card">
        <a-tabs v-model:activeKey="activeTab">
          <a-tab-pane key="status" tab="按状态统计">
            <a-table 
              :dataSource="statusStats" 
              :columns="statusColumns" 
              :pagination="false"
              size="small"
            />
          </a-tab-pane>
          <a-tab-pane key="product" tab="热门产品">
            <a-table 
              :dataSource="productStats" 
              :columns="productColumns" 
              :pagination="false"
              size="small"
            />
          </a-tab-pane>
          <a-tab-pane key="destination" tab="目的地统计">
            <a-table 
              :dataSource="destinationStats" 
              :columns="destinationColumns" 
              :pagination="false"
              size="small"
            />
          </a-tab-pane>
        </a-tabs>
      </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'

// 统计数据
const statistics = reactive({
  totalForms: 128,
  monthForms: 25,
  totalAmount: 285600.50,
  avgAmount: 2231.25
})

// 活动标签页
const activeTab = ref('status')

// 状态统计数据
const statusStats = ref([
  { status: '草稿', count: 12, amount: 25600.00 },
  { status: '已提交', count: 45, amount: 98500.00 },
  { status: '已审核', count: 38, amount: 112300.00 },
  { status: '已完成', count: 33, amount: 49200.50 }
])

// 产品统计数据
const productStats = ref([
  { productName: 'PLASTIC SEAL', hsCode: '39269090', count: 85, totalAmount: 85600.00 },
  { productName: 'RFID STICKER', hsCode: '85235210', count: 62, totalAmount: 78200.00 },
  { productName: 'HANGTAG', hsCode: '48239090', count: 45, totalAmount: 42300.00 },
  { productName: 'Polybag', hsCode: '39232900', count: 38, totalAmount: 35600.00 }
])

// 目的地统计数据
const destinationStats = ref([
  { destination: 'USA', count: 45, totalAmount: 125600.00 },
  { destination: 'EUROPE', count: 32, totalAmount: 89200.00 },
  { destination: 'ASIA', count: 28, totalAmount: 45300.00 },
  { destination: 'OTHER', count: 23, totalAmount: 25500.50 }
])

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
</script>

<style scoped>
/* 列表页面样式 - 与系统管理页面统一 */
:deep(.ant-card) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

:deep(.ant-card-body) {
  padding: 24px;
}

:deep(.ant-card-head) {
  border-bottom: 1px solid #e8e8e8;
  border-radius: 8px 8px 0 0;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
  font-size: 16px;
}

/* 表格样式 - 与系统管理完全一致 */
:deep(.ant-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 600;
  color: #333;
}

:deep(.ant-table-cell) {
  border-bottom: 1px solid #f0f0f0;
}

/* 主按钮样式 - 与系统管理完全一致 */
:deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

:deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
}

.declaration-statistics {
  height: 100%;
  overflow-x: hidden;
}

.stats-overview {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
  border: none;
}

.detail-card {
  margin-top: 16px;
  border: none;
}

:deep(.ant-statistic-content) {
  font-size: 24px !important;
  font-weight: bold;
}

:deep(.ant-tabs-tab) {
  font-size: 16px;
  font-weight: 500;
}
</style>