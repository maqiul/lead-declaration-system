<template>
  <div class="contract-generation-management">
    <!-- 搜索区域 -->
    <a-card class="search-card">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="合同编号">
          <a-input v-model:value="searchForm.contractNo" placeholder="请输入合同编号" allowClear @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="申报单ID">
          <a-input-number v-model:value="searchForm.declarationFormId" placeholder="请输入申报单ID" allowClear style="width: 150px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSearch">
            <template #icon><SearchOutlined /></template>
            搜索
          </a-button>
          <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮区域 -->
    <a-card class="operation-card">
      <a-space>
        <a-button @click="loadGenerationList">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card>
      <a-table
        :dataSource="generationList"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'declarationFormId'">
            <a-button type="link" size="small" @click="viewDeclarationDetail(record.declarationFormId)">
              #{{ record.declarationFormId }}
            </a-button>
          </template>
          
          <template v-else-if="column.key === 'fileSize'">
            <span>{{ formatFileSize(record.fileSize) }}</span>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button v-permission="['business:contract:download']" type="primary" size="small" @click="handleDownload(record.id)">下载</a-button>
              <a-popconfirm
                title="确定要删除这条记录吗？(仅删除记录，不一定删除实际文件)"
                @confirm="handleDelete(record.id)"
              >
                <a-button v-permission="['business:contract:generation:delete']" type="link" size="small" danger>删除</a-button>
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
import { message } from 'ant-design-vue'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import { getGenerations, downloadContract, deleteContract } from '@/api/business/contract'
import { useRouter } from 'vue-router'

const router = useRouter()

// 搜索表单
const searchForm = reactive({
  contractNo: '',
  declarationFormId: undefined
})

// 表格数据
const generationList = ref([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: '合同编号', dataIndex: 'contractNo', key: 'contractNo' },
  { title: '申报单', key: 'declarationFormId', width: 120 },
  { title: '模板ID', dataIndex: 'templateId', key: 'templateId', width: 100 },
  { title: '文件名', dataIndex: 'fileName', key: 'fileName' },
  { title: '大小', key: 'fileSize', width: 100 },
  { title: '生成时间', dataIndex: 'generatedTime', key: 'generatedTime', width: 180 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const }
]

// 加载列表
const loadGenerationList = async () => {
  loading.value = true
  try {
    const res = await getGenerations({
      current: pagination.current,
      size: pagination.pageSize,
      contractNo: searchForm.contractNo,
      declarationFormId: searchForm.declarationFormId
    })
    generationList.value = res.data.data.records
    pagination.total = res.data.data.total
  } catch (err) {
    message.error('加载生成记录失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadGenerationList()
}

const handleReset = () => {
  searchForm.contractNo = ''
  searchForm.declarationFormId = undefined
  handleSearch()
}

const handleTableChange = (pag: TablePaginationConfig) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadGenerationList()
}

const handleDownload = (id: number) => {
  downloadContract(id)
}

const handleDelete = async (id: number) => {
  try {
    const res = await deleteContract(id)
    if (res.data.code === 200) {
      message.success('删除成功')
      loadGenerationList()
    }
  } catch (err) {
    message.error('删除过程中发生错误')
  }
}

const viewDeclarationDetail = (id: number) => {
  router.push({
    name: 'DeclarationManage',
    query: { keyword: String(id) } // 假设管理页面支持通过ID搜索
  })
}

const formatFileSize = (size: number) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  return (size / (1024 * 1024)).toFixed(2) + ' MB'
}

onMounted(() => {
  loadGenerationList()
})
</script>

<style scoped>
.contract-generation-management {
  padding: 24px;
}
.search-card, .operation-card {
  margin-bottom: 24px;
}
</style>
