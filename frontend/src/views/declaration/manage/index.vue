<template>
  <div class="declaration-manage">
    <a-card title="申报单管理">
      <!-- 搜索区域 -->
      <div class="search-area">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-input-search 
              v-model:value="searchForm.formNo" 
              placeholder="搜索申报单号" 
              @search="loadData"
            />
          </a-col>
          <a-col :span="6">
            <a-select 
              v-model:value="searchForm.status" 
              placeholder="状态筛选" 
              style="width: 100%"
              @change="loadData"
            >
              <a-select-option value="">全部状态</a-select-option>
              <a-select-option :value="0">草稿</a-select-option>
              <a-select-option :value="1">已提交</a-select-option>
              <a-select-option :value="2">已审核</a-select-option>
              <a-select-option :value="3">已完成</a-select-option>
            </a-select>
          </a-col>
          <a-col :span="6">
            <a-range-picker 
              v-model:value="searchForm.dateRange" 
              style="width: 100%"
              @change="loadData"
            />
          </a-col>
          <a-col :span="6">
            <a-button type="primary" @click="loadData">查询</a-button>
            <a-button style="margin-left: 8px" @click="resetSearch">重置</a-button>
          </a-col>
        </a-row>
      </div>

      <!-- 操作按钮 -->
      <div class="operation-area" style="margin: 16px 0;">
        <a-button type="primary" @click="handleAdd">
          <template #icon><plus-outlined /></template>
          新增申报单
        </a-button>
        <a-button @click="handleExport" style="margin-left: 8px;">
          <template #icon><download-outlined /></template>
          导出
        </a-button>
      </div>

      <!-- 数据表格 -->
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
            <a @click="handleView(record.id)">{{ record.formNo }}</a>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record.id)">查看</a-button>
              <!-- 只有草稿状态才显示编辑按钮 -->
              <a-button v-if="record.status === 0" type="link" size="small" @click="handleEdit(record.id)">编辑</a-button>
              <a-button type="link" size="small" @click="handleExportSingle(record.id)">导出</a-button>
              <!-- 只有草稿状态才显示删除按钮 -->
              <a-popconfirm
                v-if="record.status === 0"
                title="确定要删除这个申报单吗？"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
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
import {
  getDeclarationList,
  deleteDeclaration as deleteDeclarationApi
} from '@/api/business/declaration'

const router = useRouter()
const searchForm = reactive({
  formNo: '',
  status: '',
  dateRange: []
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
  { title: '申报单号', dataIndex: 'formNo', key: 'formNo', width: 150 },
  { title: '发货人', dataIndex: 'shipperCompany', key: 'shipperCompany', width: 200 },
  { title: '收货人', dataIndex: 'consigneeCompany', key: 'consigneeCompany', width: 200 },
  { title: '申报日期', dataIndex: 'declarationDate', key: 'declarationDate', width: 120 },
  { title: '总金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 100 },
  { title: '总箱数', dataIndex: 'totalCartons', key: 'totalCartons', width: 80 },
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
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'action',
    width: 200,
    fixed: 'right' as const,
    customRender: ({ record }: { record: any }) => {
      return [
        h('a-button', {
          type: 'link',
          size: 'small',
          onClick: () => handleView(record.id)
        }, '查看'),
        h('a-button', {
          type: 'link',
          size: 'small',
          onClick: () => handleEdit(record.id)
        }, '编辑'),
        h('a-popconfirm', {
          title: '确定要删除这条申报单吗？',
          onConfirm: () => handleDelete(record.id)
        }, {
          default: () => h('a-button', {
            type: 'link',
            size: 'small',
            danger: true
          }, '删除')
        })
      ]
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
  searchForm.dateRange = []
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
const handleView = (id: number) => {
  router.push(`/declaration/form?id=${id}&readonly=true`)
}

// 编辑申报单
const handleEdit = (id: number) => {
  // 检查状态,如果已提交则不允许编辑
  const record = dataSource.value.find((item: any) => item.id === id)
  if (!record || record.status !== 0) {
    message.warning('只有草稿状态的申报单可以编辑')
    return
  }
  router.push(`/declaration/form?id=${id}`)
}

// 导出申报单
const handleExport = () => {
  message.info('批量导出功能开发中...')
}

// 导出单个申报单
const handleExportSingle = (id: number) => {
  message.info(`导出申报单 ${id} 功能开发中...`)
}

// 删除申报单
const handleDelete = async (id: number) => {
  try {
    await deleteDeclarationApi(id)
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
.declaration-manage {
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 64px);
}

.search-area {
  background: white;
  padding: 16px;
  border-radius: 4px;
  margin-bottom: 16px;
}

.operation-area {
  background: white;
  padding: 16px;
  border-radius: 4px;
}
</style>