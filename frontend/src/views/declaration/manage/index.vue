<template>
  <div class="declaration-manage">
    <!-- 搜索区域 -->
    <a-card class="search-card">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="申报单号">
          <a-input-search 
            v-model:value="searchForm.formNo" 
            placeholder="搜索申报单号" 
            @search="loadData"
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-select 
            v-model:value="searchForm.status" 
            placeholder="状态筛选" 
            style="width: 140px"
            allowClear
            @change="loadData"
          >
            <a-select-option :value="0">草稿</a-select-option>
            <a-select-option :value="1">待初审</a-select-option>
            <a-select-option :value="2">待付定金</a-select-option>
            <a-select-option :value="3">定金待审</a-select-option>
            <a-select-option :value="4">待付尾款</a-select-option>
            <a-select-option :value="5">尾款待审</a-select-option>
            <a-select-option :value="6">已完成</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="日期">
          <a-range-picker 
            v-model:value="searchForm.dateRange" 
            style="width: 200px"
            @change="loadData"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="loadData">查询</a-button>
          <a-button style="margin-left: 8px" @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮 -->
    <a-card class="operation-card">
      <a-space>
        <a-button type="primary" @click="handleAdd">
          <template #icon><plus-outlined /></template>
          新增申报单
        </a-button>
        <a-button @click="handleExport">
          <template #icon><download-outlined /></template>
          导出
        </a-button>
      </a-space>
    </a-card>

    <!-- 数据表格 -->
    <a-card>
      <a-table 
        :dataSource="dataSource" 
        :columns="columns" 
        :loading="loading"
        :pagination="pagination"
        rowKey="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'formNo'">
            <a @click="handleView(record as any)">{{ record.formNo }}</a>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record as any)">
                <template #icon><EyeOutlined /></template>
                查看
              </a-button>
              <!-- 只有草稿状态才显示编辑按钮 -->
              <a-button v-if="record.status === 0" type="link" size="small" @click="handleEdit(record as any)">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button v-if="[1, 3, 5].includes(record.status)" type="link" size="small" style="color: #faad14;" @click="handleAudit(record as any)">
                <template #icon><CheckCircleOutlined /></template>
                {{ getAuditBtnText(record.status) }}
              </a-button>
              <!-- 状态 2(待付定金) 或状态 4(待付尾款) 显示付款按钮 -->
              <a-button v-if="record.status === 2" type="link" size="small" style="color: #52c41a;" @click="handlePayment(record as any)">
                <template #icon><DollarOutlined /></template>
                付定金
              </a-button>
              <a-button v-if="record.status === 4" type="link" size="small" style="color: #52c41a;" @click="handlePayment(record as any)">
                <template #icon><DollarOutlined /></template>
                付尾款
              </a-button>
              <a-button v-if="record.status >= 2" type="link" size="small" @click="handleDownload(record as any)">
                <template #icon><DownloadOutlined /></template>
                单证
              </a-button>
              <!-- 只有草稿状态才显示删除按钮 -->
              <a-popconfirm
                title="确定要删除该申报单吗？"
                @confirm="handleDelete(record as any)"
              >
                <a-button type="link" size="small" danger>
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 附件下载弹窗 -->
    <a-modal 
      v-model:open="attachmentModalVisible" 
      title="附件下载" 
      width="600px"
      :footer="null"
    >
      <a-list :dataSource="currentAttachments" bordered size="small">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta :title="item.fileName">
              <template #description>
                {{ item.fileType }} | {{ item.createTime }}
              </template>
            </a-list-item-meta>
            <template #actions>
              <a :href="item.fileUrl" target="_blank" download>下载</a>
            </template>
          </a-list-item>
        </template>
        <template v-if="currentAttachments.length === 0" #header>
          <div style="text-align: center; color: #999;">暂无自动生成的全套单证或水单文件</div>
        </template>
      </a-list>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { PlusOutlined, DownloadOutlined, EyeOutlined, EditOutlined, CheckCircleOutlined, DeleteOutlined, DollarOutlined } from '@ant-design/icons-vue'
import type { Dayjs } from 'dayjs'
import {
  getDeclarationList,
  deleteDeclaration as deleteDeclarationApi,
  getDeclarationDetail
} from '@/api/business/declaration'
import { h } from 'vue'

const router = useRouter()
const searchForm = reactive({
  formNo: '',
  status: '',
  dateRange: undefined as [Dayjs, Dayjs] | undefined
})

// 表格数据
interface DeclarationRecord {
  id: number
  formNo: string
  shipperCompany?: string
  consigneeCompany?: string
  declarationDate?: string
  totalAmount?: number
  totalCartons?: number
  status: number
  createTime?: string
}

const dataSource = ref<DeclarationRecord[]>([])
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
  { title: '申报单号', dataIndex: 'formNo', key: 'formNo' },
  { title: '发货人', dataIndex: 'shipperCompany', key: 'shipperCompany' },
  { title: '收货人', dataIndex: 'consigneeCompany', key: 'consigneeCompany' },
  { title: '申报日期', dataIndex: 'declarationDate', key: 'declarationDate' },
  { title: '总金额', dataIndex: 'totalAmount', key: 'totalAmount' },
  { title: '总箱数', dataIndex: 'totalCartons', key: 'totalCartons' },
  { 
    title: '状态', 
    key: 'status', 
    width: 100,
    customRender: ({ record }: { record: any }) => {
      const statusText = getStatusText(record.status)
      const statusColor = getStatusColor(record.status)
      return h('a-tag', { color: statusColor }, statusText)
    }
  },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  {
    title: '操作',
    key: 'action',
    width: 280,
    fixed: 'right' as const
  }
]

// 弹窗相关
// 附件下载弹窗
const attachmentModalVisible = ref(false)
const currentAttachments = ref<any[]>([])

// 加载数据
const loadData = async () => {
  try {
    loading.value = true
    
    const params = {
      current: pagination.current,
      size: pagination.pageSize,
      formNo: searchForm.formNo || undefined,
      status: searchForm.status !== '' ? Number(searchForm.status) : undefined
    }
    
    const response = await getDeclarationList(params)
    
    if (response.data.code === 200) {
      dataSource.value = response.data.data.records as any[]
      pagination.total = response.data.data.total
    } else {
      dataSource.value = []
      pagination.total = 0
    }
    
  } catch (error: any) {
    console.error('加载数据失败:', error)
    message.error('加载数据失败: ' + (error.message || '未知错误'))
    // 出错时使用空数据
    dataSource.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchForm.formNo = ''
  searchForm.status = ''
  searchForm.dateRange = undefined
  pagination.current = 1
  loadData()
}

// 表格分页变化
const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

// 新增申报单
const handleAdd = () => {
  router.push('/declaration/form')
}

// 查看详情
const handleView = (record: DeclarationRecord) => {
  router.push(`/declaration/form?id=${record.id}&readonly=true&status=${record.status}`)
}

// 编辑申报单
const handleEdit = (record: DeclarationRecord) => {
  // 检查状态,如果已提交则不允许编辑
  if (record.status !== 0) {
    message.warning('只有草稿状态的申报单可以编辑')
    return
  }
  router.push(`/declaration/form?id=${record.id}&status=${record.status}`)
}

// 付款操作（定金/尾款）- 跳转到水单提交模式
const handlePayment = (record: DeclarationRecord) => {
  router.push(`/declaration/form?id=${record.id}&status=${record.status}&mode=payment`)
}

// 审核申报单
const handleAudit = (record: DeclarationRecord) => {
  router.push(`/declaration/form?id=${record.id}&mode=audit`)
}

// 下载单证
const handleDownload = async (record: DeclarationRecord) => {
  try {
    loading.value = true
    const response = await getDeclarationDetail(record.id, record.status)
    if (response.data && response.data.code === 200) {
      currentAttachments.value = response.data.data.attachments || []
      attachmentModalVisible.value = true
    } else {
      message.error('获取附件列表失败')
    }
  } catch (error) {
    message.error('获取附件列表失败')
  } finally {
    loading.value = false
  }
}

// 导出申报单
const handleExport = () => {
  message.info('批量导出功能开发中...')
}

// 删除申报单
const handleDelete = async (record: DeclarationRecord) => {
  try {
    await deleteDeclarationApi(record.id, record.status)
    message.success('删除成功')
    loadData()
  } catch (error) {
    message.error('删除失败')
  }
}

// 获取审核按钮文本
const getAuditBtnText = (status: number) => {
  if (status === 1) return '初审'
  if (status === 3) return '定金审核'
  if (status === 5) return '尾款审核'
  return '审核'
}

// 获取状态文本
const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '草稿',
    1: '待初审',
    2: '待付定金',
    3: '定金待审',
    4: '定金已过/待尾款',
    5: '尾款待审',
    6: '已完成'
  }
  return statusMap[status] || '未知'
}

// 获取状态颜色
const getStatusColor = (status: number) => {
  const colorMap: Record<number, string> = {
    0: 'default',
    1: 'processing',
    2: 'warning',
    3: 'processing',
    4: 'warning',
    5: 'processing',
    6: 'success'
  }
  return colorMap[status] || 'default'
}

// 页面加载
onMounted(() => {
  loadData()
})
</script>

<style scoped>
/* 列表页面样式 */
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

/* 搜索卡片 & 操作按钮卡片样式 */
:deep(.ant-card.search-card),
:deep(.ant-card.operation-card) {
  margin-bottom: 16px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.declaration-manage {
  height: 100%;
  overflow-x: hidden;
}
</style>