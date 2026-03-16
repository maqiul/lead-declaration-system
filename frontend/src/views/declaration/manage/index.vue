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
            style="width: 120px"
            @change="loadData"
          >
            <a-select-option value="">全部状态</a-select-option>
            <a-select-option :value="0">草稿</a-select-option>
            <a-select-option :value="1">已提交</a-select-option>
            <a-select-option :value="2">已审核</a-select-option>
            <a-select-option :value="3">已完成</a-select-option>
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
              <a-button v-if="record.status === 1" type="link" size="small" color="warning" @click="handleAudit(record as any)">
                <template #icon><CheckCircleOutlined /></template>
                审核
              </a-button>
              <a-button v-if="record.status >= 2" type="link" size="small" @click="handleDownload(record as any)">
                <template #icon><DownloadOutlined /></template>
                下载
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

    <!-- 表单弹窗 -->
    <a-modal 
      v-model:open="modalVisible" 
      :title="modalTitle" 
      width="80%"
      :footer="null"
    >
      <iframe 
        v-if="formUrl" 
        :src="formUrl" 
        style="width: 100%; height: 600px; border: none;"
        frameborder="0"
      ></iframe>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { PlusOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import type { Dayjs } from 'dayjs'
import {
  getDeclarationList,
  deleteDeclaration as deleteDeclarationApi
} from '@/api/business/declaration'

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
    width: 200,
    fixed: 'right' as const,
    customRender: ({ record }: { record: any }) => {
      const actions = [
        h('a-button', {
          type: 'link',
          size: 'small',
          onClick: () => handleView(record as any)
        }, '查看')
      ]
      
      if (record.status === 0) {
        actions.push(h('a-button', {
          type: 'link',
          size: 'small',
          onClick: () => handleEdit(record as any)
        }, '编辑'))
      }
      
      if (record.status === 1) {
        actions.push(h('a-button', {
          type: 'link',
          size: 'small',
          style: { color: '#faad14' },
          onClick: () => handleAudit(record as any)
        }, '审核'))
      }
      
      if (record.status >= 2) {
        actions.push(h('a-button', {
          type: 'link',
          size: 'small',
          onClick: () => handleDownload(record as any)
        }, '下载'))
      }
      
      actions.push(h('a-popconfirm', {
        title: '确定要删除这条申报单吗？',
        onConfirm: () => handleDelete(record as any)
      }, {
        default: () => h('a-button', {
          type: 'link',
          size: 'small',
          danger: true
        }, '删除')
      }))
      
      return h('a-space', {}, actions)
    }
  }
]

// 弹窗相关
const modalVisible = ref(false)
const modalTitle = ref('')
const formUrl = ref('')

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
  router.push(`/declaration/form?id=${record.id}&readonly=true`)
}

// 编辑申报单
const handleEdit = (record: DeclarationRecord) => {
  // 检查状态,如果已提交则不允许编辑
  if (record.status !== 0) {
    message.warning('只有草稿状态的申报单可以编辑')
    return
  }
  router.push(`/declaration/form?id=${record.id}`)
}

// 审核申报单
const handleAudit = (record: DeclarationRecord) => {
  router.push(`/declaration/form?id=${record.id}&mode=audit`)
}

// 下载单证
const handleDownload = (record: DeclarationRecord) => {
  // 这里暂时直接调用导出接口，后续优化为展示附件列表
  message.loading('正在准备下载...', 0)
  router.push(`/declaration/form?id=${record.id}&readonly=true#attachments`)
}

// 导出申报单
const handleExport = () => {
  message.info('批量导出功能开发中...')
}

// 删除申报单
const handleDelete = async (record: DeclarationRecord) => {
  try {
    await deleteDeclarationApi(record.id)
    message.success('删除成功')
    loadData()
  } catch (error) {
    message.error('删除失败')
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

// 页面加载
onMounted(() => {
  loadData()
})
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

/* 搜索卡片样式 */
:deep(.ant-card.search-card) {
  margin-bottom: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
  border: none;
}

/* 操作按钮卡片样式 */
:deep(.ant-card.operation-card) {
  margin-bottom: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
  border: none;
}

.declaration-manage {
  height: 100%;
  overflow-x: hidden;
}
</style>