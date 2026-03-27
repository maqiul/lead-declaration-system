<template>
  <div class="declaration-history">
    <!-- 搜索条件 -->
    <a-card class="search-card">
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
          <a-button type="primary" @click="loadHistory" v-permission="['business:declaration:list']">查询</a-button>
        </a-col>
      </a-row>
    </a-card>

    <!-- 数据表格 -->
    <a-card>
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
              <a-button type="link" size="small" @click="viewDetail(record.id)" v-permission="['business:declaration:query']">查看详情</a-button>
              <a-button type="link" size="small" @click="editForm(record.id)" v-permission="['business:declaration:edit']">编辑</a-button>
              <a-button type="link" size="small" @click="exportForm(record.id)" v-permission="['business:declaration:export']">导出</a-button>
            </a-space>
          </template>
        </template>
        <!-- 空状态 -->
        <template #emptyText>
          <a-empty description="暂无历史记录" />
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

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { 
  getDeclarationList, 
  getDeclarationDetail, 
  exportDeclaration,
  type DeclarationQueryParams 
} from '@/api/business/declaration'

import type { Dayjs } from 'dayjs'

const router = useRouter()

// 搜索表单
const searchForm = reactive({
  formNo: '',
  dateRange: undefined as [Dayjs, Dayjs] | undefined,
  status: '' as string | number
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
  showTotal: (total: number) => `共 ${total} 条记录`
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
const currentDetail = ref<any>(null)

// 加载历史记录
const loadHistory = async () => {
  try {
    loading.value = true
    
    // 构建查询参数
    const params: DeclarationQueryParams = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    
    // 添加可选的筛选条件
    if (searchForm.formNo) {
      params.formNo = searchForm.formNo
    }
    if (searchForm.status !== '' && searchForm.status !== undefined) {
      params.status = Number(searchForm.status)
    }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startTime = searchForm.dateRange[0]?.format?.('YYYY-MM-DD') || ''
      params.endTime = searchForm.dateRange[1]?.format?.('YYYY-MM-DD') || ''
    }
    
    const result = await getDeclarationList(params) as any
    console.log('查询结果:', result)
    
    if (result.code === 200) {
      historyList.value = result.data?.records || result.data?.list || []
      pagination.total = result.data?.total || 0
    } else {
      message.error('查询失败：' + (result.msg || '未知错误'))
    }
    
  } catch (error: any) {
    console.error('加载历史记录失败:', error)
    message.error('加载失败：' + (error.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

// 表格分页变化
const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadHistory()
}

// 查看详情
const viewDetail = async (id: number) => {
  try {
    const result = await getDeclarationDetail(id) as any
    console.log('详情查询结果:', result)
    
    if (result.code === 200) {
      currentDetail.value = result.data
      detailVisible.value = true
    } else {
      message.error('获取详情失败：' + (result.msg || '未知错误'))
    }
  } catch (error: any) {
    console.error('获取详情失败:', error)
    message.error('获取详情失败：' + (error.message || '网络错误'))
  }
}

// 编辑申报单
const editForm = (id: number) => {
  router.push(`/declaration/edit/${id}`)
}

// 导出申报单
const exportForm = async (id: number) => {
  try {
    message.loading({ content: '正在导出...', key: 'export' })
    const blob = await exportDeclaration(id) as any
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `申报单_${id}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    message.success({ content: '导出成功', key: 'export' })
  } catch (error: any) {
    console.error('导出失败:', error)
    message.error({ content: '导出失败：' + (error.message || '网络错误'), key: 'export' })
  }
}

// 获取状态文本
const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '草稿',
    1: '已提交',
    2: '已审核',
    3: '已完成'
  }
  return statusMap[status] || '未知'
}

// 获取状态颜色
const getStatusColor = (status: number) => {
  const colorMap: Record<number, string> = {
    0: 'default',
    1: 'processing',
    2: 'success',
    3: 'success'
  }
  return colorMap[status] || 'default'
}

// 格式化日期
const formatDate = (date: string | Date | null | undefined) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN')
}

// 页面加载
onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
/* 页面特有样式已由全局 index.less 覆盖 */
.declaration-history {
  height: 100%;
  overflow-x: hidden;
}
</style>