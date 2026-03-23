<template>
  <div class="tax-refund-list">
    <!-- 搜索卡片 -->
    <a-card class="search-card">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="申请编号">
          <a-input v-model:value="searchForm.applicationNo" placeholder="搜索申请编号" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchForm.status" placeholder="请选择状态" style="width: 120px" allowClear>
            <a-select-option :value="0">草稿</a-select-option>
            <a-select-option :value="1">已提交</a-select-option>
            <a-select-option :value="2">财务初审</a-select-option>
            <a-select-option :value="4">退回补充</a-select-option>
            <a-select-option :value="6">财务复审</a-select-option>
            <a-select-option :value="7">已完成</a-select-option>
            <a-select-option :value="8">已拒绝</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="申请人">
          <a-input v-model:value="searchForm.initiatorName" placeholder="搜索申请人" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="loadData">搜索</a-button>
          <a-button style="margin-left: 8px" @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作栏卡片 -->
    <a-card class="operation-card">
      <a-space>
        <a-button type="primary" @click="handleCreate">
          <template #icon><PlusOutlined /></template>
          新建申请
        </a-button>
        <a-button @click="handleBatchDelete" :disabled="selectedRowKeys.length === 0">
          <template #icon><DeleteOutlined /></template>
          批量删除
        </a-button>
      </a-space>
    </a-card>

    <!-- 申请列表卡片 -->
    <a-card>
      <a-table 
        :dataSource="dataSource" 
        :columns="columns" 
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        @change="handleTableChange"
        rowKey="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'applicationNo'">
            <a @click="handleView(record)" class="application-link">{{ record.applicationNo }}</a>
          </template>
          <template v-else-if="column.key === 'applicationType'">
            <span>{{ getApplicationTypeText(record.applicationType) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)" class="status-tag">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'amount'">
            <span class="amount-text">¥{{ record.amount?.toFixed(2) }}</span>
          </template>
          <template v-else-if="column.key === 'createTime'">
            <span class="date-text">{{ formatDate(record.createTime) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
              <a-button v-if="record.status === 0" type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button v-if="record.status === 0" type="link" size="small" @click="handleSubmit(record)">提交</a-button>
              <a-button 
                v-if="[2, 6].includes(record.status)" 
                type="link" 
                size="small" 
                @click="handleAudit(record)"
                v-permission="['system:tax-refund:approve']"
              >
                审核
              </a-button>
              <a-popconfirm
                v-if="record.status === 0"
                title="确定要删除这个申请吗？"
                @confirm="handleDelete(record)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import type { TableProps } from 'ant-design-vue'
import { getTaxRefundList, deleteTaxRefund, batchDeleteTaxRefund, submitTaxRefund } from '@/api/tax-refund'

const router = useRouter()

// 数据状态
const loading = ref(false)
const dataSource = ref<Array<any>>([])
const selectedRowKeys = ref<number[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条记录`
})

// 搜索表单
const searchForm = reactive({
  applicationNo: '',
  status: undefined,
  initiatorName: ''
})

// 表格列配置
const columns = [
  { title: '申请编号', dataIndex: 'applicationNo', key: 'applicationNo', width: 180 },
  { title: '关联申报单', dataIndex: 'declarationFormId', key: 'declarationFormId', width: 150 },
  { title: '申请人', dataIndex: 'initiatorName', key: 'initiatorName', width: 120 },
  { title: '申请类型', dataIndex: 'applicationType', key: 'applicationType', width: 120 },
  { title: '申请金额', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const }
]

// 表格行选择配置
const rowSelection: TableProps['rowSelection'] = {
  onChange: (selectedKeys: any[]) => {
    selectedRowKeys.value = selectedKeys as number[]
  }
}

// 获取申请类型文本
const getApplicationTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    'EXPORT_REFUND': '出口退税',
    'VAT_REFUND': '增值税退税',
    'OTHER_REFUND': '其他退税'
  }
  return typeMap[type] || type || '未知'
}

// 获取状态文本
const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '草稿',
    1: '已提交',
    2: '财务初审',
    4: '退回补充',
    6: '财务复审',
    7: '已完成',
    8: '已拒绝'
  }
  return statusMap[status] || '未知'
}

// 获取状态颜色
const getStatusColor = (status: number) => {
  const colorMap: Record<number, string> = {
    0: 'default',
    1: 'processing',
    2: 'processing',
    4: 'warning',
    6: 'processing',
    7: 'success',
    8: 'error'
  }
  return colorMap[status] || 'default'
}

// 格式化日期
const formatDate = (date: string) => {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

// 数据加载
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      applicationNo: searchForm.applicationNo,
      status: searchForm.status,
      initiatorName: searchForm.initiatorName
    }
    
    const response = await getTaxRefundList(params)
    if (response.data?.code === 200) {
      dataSource.value = response.data.data.records || response.data.data.list || []
      pagination.total = Number(response.data.data.total) || 0
    } else {
      message.error(response.data?.message || '加载数据失败')
      // 使用空数组而不是模拟数据
      dataSource.value = []
      pagination.total = 0
    }
  } catch (error) {
    message.error('加载数据失败')
    // 使用空数组而不是模拟数据
    dataSource.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.applicationNo = ''
  searchForm.status = undefined
  searchForm.initiatorName = ''
  pagination.current = 1
  loadData()
}

// 表格分页变化
const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

// 新建申请
const handleCreate = () => {
  router.push('/tax-refund/apply')
}

// 查看详情
const handleView = (record: any) => {
  router.push(`/tax-refund/detail/${record.id}?readonly=true`)
}

// 编辑申请
const handleEdit = (record: any) => {
  router.push(`/tax-refund/apply?id=${record.id}`)
}

// 审核申请
const handleAudit = (record: any) => {
  // 跳转到审核页面，传递审核标识
  router.push(`/tax-refund/detail/${record.id}?mode=audit&status=${record.status}`)
}

// 提交申请
const handleSubmit = async (record: any) => {
  try {
    const response = await submitTaxRefund(record.id)
    if (response.data?.code === 200) {
      message.success('申请提交成功')
      loadData()
    } else {
      message.error(response.data?.message || '提交失败')
    }
  } catch (error) {
    message.error('提交失败')
  }
}

// 删除申请
const handleDelete = async (record: any) => {
  try {
    const response = await deleteTaxRefund(record.id)
    if (response.data?.code === 200) {
      message.success('删除成功')
      loadData()
    } else {
      message.error(response.data?.message || '删除失败')
    }
  } catch (error) {
    message.error('删除失败')
  }
}

// 批量删除
const handleBatchDelete = async () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要删除的申请')
    return
  }
  
  try {
    const response = await batchDeleteTaxRefund(selectedRowKeys.value)
    if (response.data?.code === 200) {
      message.success('批量删除成功')
      selectedRowKeys.value = []
      loadData()
    } else {
      message.error(response.data?.message || '批量删除失败')
    }
  } catch (error) {
    message.error('批量删除失败')
  }
}

onMounted(() => {
  loadData()
})

// 监听路由变化，确保每次进入都刷新数据
import { onActivated } from 'vue'
onActivated(() => {
  loadData()
})
</script>

<style scoped>
/* 列表页面样式 - 与 declaration/manage 完全一致 */
.tax-refund-list {
  height: 100%;
  overflow-x: hidden;
}

:deep(.ant-card) {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 4px 12px rgba(79, 70, 229, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

:deep(.ant-card-body) {
  padding: 20px;
}

:deep(.ant-card-head) {
  border-bottom: 1px solid #f1f5f9;
  border-radius: 16px 16px 0 0;
  background: #f8fafc;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
  font-size: 15px;
  color: #1e293b;
}

/* 搜索卡片 & 操作按钮卡片样式 */
:deep(.ant-card.search-card),
:deep(.ant-card.operation-card) {
  margin-bottom: 16px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* 表格样式 */
:deep(.ant-table) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #f8fafc !important;
  font-weight: 600;
  color: #475569;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid #f1f5f9;
}

:deep(.ant-table-row:hover > td) {
  background-color: #f8faff !important;
}

/* 主按钮样式 */
:deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(79, 70, 229, 0.25);
  border-radius: 10px;
}

:deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, #4338ca 0%, #6d28d9 100%);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.35);
  transform: translateY(-1px);
}

.application-link {
  color: #4f46e5;
  font-weight: 500;
  text-decoration: none;
  transition: color 0.2s;
}

.application-link:hover {
  color: #7c3aed;
  text-decoration: underline;
}

.status-tag {
  font-weight: 500;
  border-radius: 6px;
}

.amount-text {
  font-weight: 600;
  color: #4f46e5;
}

.date-text {
  color: #64748b;
  font-size: 13px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  :deep(.ant-card-body) {
    padding: 16px;
  }

  :deep(.ant-form-item) {
    margin-bottom: 12px;
  }
}
</style>