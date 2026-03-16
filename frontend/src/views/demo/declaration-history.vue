<template>
  <div class="declaration-history">
    <a-card title="申报单历史记录">
      <!-- 搜索条件 -->
      <div style="margin-bottom: 20px;">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-input-search 
              v-model:value="searchForm.formNo" 
              placeholder="搜索申报单号" 
              @search="loadHistory"
            />
          </a-col>
          <a-col :span="6">
            <a-range-picker 
              v-model:value="searchForm.dateRange" 
              style="width: 100%"
              @change="loadHistory"
            />
          </a-col>
          <a-col :span="6">
            <a-select 
              v-model:value="searchForm.status" 
              placeholder="状态筛选" 
              style="width: 100%"
              @change="loadHistory"
            >
              <a-select-option value="">全部状态</a-select-option>
              <a-select-option :value="0">草稿</a-select-option>
              <a-select-option :value="1">已提交</a-select-option>
              <a-select-option :value="2">已审核</a-select-option>
              <a-select-option :value="3">已完成</a-select-option>
            </a-select>
          </a-col>
          <a-col :span="6">
            <a-button type="primary" @click="loadHistory">查询</a-button>
          </a-col>
        </a-row>
      </div>

      <!-- 数据表格 -->
      <a-table 
        :dataSource="historyList" 
        :columns="columns" 
        :pagination="pagination"
        :loading="loading"
        rowKey="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'formNo'">
            <a @click="viewDetail(record.id)">{{ record.formNo }}</a>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="viewDetail(record.id)">查看详情</a-button>
              <a-button type="link" size="small" @click="editForm(record.id)">编辑</a-button>
              <a-button type="link" size="small" @click="exportForm(record.id)">导出</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal 
      v-model:open="detailVisible" 
      title="申报单详情" 
      width="800px"
      :footer="null"
    >
      <div v-if="currentDetail">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="申报单号">{{ currentDetail.formNo }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="getStatusColor(currentDetail.status)">
              {{ getStatusText(currentDetail.status) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="发货人">{{ currentDetail.shipperCompany }}</a-descriptions-item>
          <a-descriptions-item label="收货人">{{ currentDetail.consigneeCompany }}</a-descriptions-item>
          <a-descriptions-item label="发票号">{{ currentDetail.invoiceNo }}</a-descriptions-item>
          <a-descriptions-item label="申报日期">{{ formatDate(currentDetail.declarationDate) }}</a-descriptions-item>
          <a-descriptions-item label="总金额">{{ currentDetail.totalAmount }} {{ currentDetail.currency }}</a-descriptions-item>
          <a-descriptions-item label="总箱数">{{ currentDetail.totalCartons }}</a-descriptions-item>
        </a-descriptions>

        <!-- 产品明细 -->
        <a-divider>产品明细</a-divider>
        <a-table 
          :dataSource="currentDetail.products" 
          :columns="productColumns" 
          :pagination="false"
          size="small"
        />

        <!-- 箱子信息 -->
        <a-divider>箱子信息</a-divider>
        <a-table 
          :dataSource="currentDetail.cartons" 
          :columns="cartonColumns" 
          :pagination="false"
          size="small"
        />
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'

// 搜索表单
const searchForm = reactive({
  formNo: '',
  dateRange: [],
  status: ''
})

// 历史记录列表
const historyList = ref([])
const loading = ref(false)

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total) => `共 ${total} 条记录`
})

// 表格列配置
const columns = [
  { title: '申报单号', dataIndex: 'formNo', key: 'formNo', width: 150 },
  { title: '发货人', dataIndex: 'shipperCompany', key: 'shipperCompany', width: 200 },
  { title: '收货人', dataIndex: 'consigneeCompany', key: 'consigneeCompany', width: 200 },
  { title: '发票号', dataIndex: 'invoiceNo', key: 'invoiceNo', width: 150 },
  { title: '申报日期', dataIndex: 'declarationDate', key: 'declarationDate', width: 120 },
  { title: '总金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 100 },
  { title: '总箱数', dataIndex: 'totalCartons', key: 'totalCartons', width: 80 },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 150 }
]

// 产品明细列配置
const productColumns = [
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: 'HS编码', dataIndex: 'hsCode', key: 'hsCode' },
  { title: '数量', dataIndex: 'quantity', key: 'quantity' },
  { title: '单位', dataIndex: 'unit', key: 'unit' },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice' },
  { title: '金额', dataIndex: 'amount', key: 'amount' }
]

// 箱子信息列配置
const cartonColumns = [
  { title: '箱号', dataIndex: 'cartonNo', key: 'cartonNo' },
  { title: '数量', dataIndex: 'quantity', key: 'quantity' },
  { title: '单箱体积', dataIndex: 'volume', key: 'volume' },
  { title: '总体积', dataIndex: 'totalVolume', key: 'totalVolume' }
]

// 详情弹窗
const detailVisible = ref(false)
const currentDetail = ref(null)

// 加载历史记录
const loadHistory = async () => {
  try {
    loading.value = true
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      formNo: searchForm.formNo,
      status: searchForm.status,
      startDate: searchForm.dateRange && searchForm.dateRange[0] ? searchForm.dateRange[0].format('YYYY-MM-DD') : '',
      endDate: searchForm.dateRange && searchForm.dateRange[1] ? searchForm.dateRange[1].format('YYYY-MM-DD') : ''
    }

    // 构建查询参数
    const queryParams = new URLSearchParams()
    Object.keys(params).forEach(key => {
      if (params[key] !== '' && params[key] !== null && params[key] !== undefined) {
        queryParams.append(key, params[key])
      }
    })
    
    const response = await fetch(`/api/declaration/form/list?${queryParams.toString()}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    const result = await response.json()
    console.log('查询结果:', result)
    
    if (result.code === 200) {
      historyList.value = result.data.records || []
      pagination.total = result.data.total || 0
    } else {
      message.error('查询失败：' + result.msg)
    }
    
  } catch (error) {
    console.error('加载历史记录失败:', error)
    message.error('加载失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// 表格分页变化
const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadHistory()
}

// 查看详情
const viewDetail = async (id) => {
  try {
    const response = await fetch(`/api/declaration/form/${id}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    const result = await response.json()
    console.log('详情查询结果:', result)
    
    if (result.code === 200) {
      currentDetail.value = result.data
    } else {
      message.error('获取详情失败：' + result.msg)
      return
    }
    
    detailVisible.value = true
  } catch (error) {
    console.error('获取详情失败:', error)
    message.error('获取详情失败：' + error.message)
  }
}

// 编辑申报单
const editForm = (id) => {
  // TODO: 跳转到编辑页面
  console.log('编辑申报单:', id)
  message.info('编辑功能待实现')
}

// 导出申报单
const exportForm = (id) => {
  // TODO: 调用导出功能
  console.log('导出申报单:', id)
  message.info('导出功能待实现')
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    0: '草稿',
    1: '已提交',
    2: '已审核',
    3: '已完成'
  }
  return statusMap[status] || '未知'
}

// 获取状态颜色
const getStatusColor = (status) => {
  const colorMap = {
    0: 'default',
    1: 'processing',
    2: 'success',
    3: 'success'
  }
  return colorMap[status] || 'default'
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN')
}

// 页面加载
onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.declaration-history {
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 64px);
}
</style>