<template>
  <div class="contract-template-management">
    <!-- 搜索区域 -->
    <a-card class="search-card">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="关键词">
          <a-input v-model:value="searchForm.keyword" placeholder="模板名称/编码" allowClear @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item label="模板类型">
          <a-select v-model:value="searchForm.templateType" placeholder="请选择类型" allowClear style="width: 150px">
            <a-select-option value="EXPORT">EXPORT-出口合同</a-select-option>
            <a-select-option value="IMPORT">IMPORT-进口合同</a-select-option>
            <a-select-option value="OTHER">OTHER-其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">
              <template #icon><SearchOutlined /></template>
              搜索
            </a-button>
            <a-button @click="handleReset">
              <template #icon><ReloadOutlined /></template>
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮区域 -->
    <a-card class="operation-card">
      <a-space>
        <a-button v-permission="['business:contract:template:create']" type="primary" @click="openAddModal">
          <template #icon><PlusOutlined /></template>
          新增模板
        </a-button>
        <a-button @click="loadTemplateList">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card>
      <a-table
        :dataSource="templateList"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'templateType'">
            <a-tag :color="getTypeColor(record.templateType)">
              {{ record.templateType }}
            </a-tag>
          </template>
          
          <template v-else-if="column.key === 'status'">
            <a-badge :status="record.status === 1 ? 'success' : 'error'" :text="record.status === 1 ? '启用' : '禁用'" />
          </template>
          
          <template v-else-if="column.key === 'fileSize'">
            <span>{{ formatFileSize(record.fileSize) }}</span>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button v-permission="['business:contract:template:update']" type="link" size="small" @click="openEditModal(record)">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-upload
                name="file"
                :showUploadList="false"
                :customRequest="(options: any) => handleUpload(options, record.id)"
              >
                <a-button v-permission="['business:contract:template:upload']" type="link" size="small">
                  <template #icon><UploadOutlined /></template>
                  上传文件
                </a-button>
              </a-upload>
              <a-button v-permission="['business:contract:download']" v-if="record.filePath" type="link" size="small" @click="handleDownload(record)">
                <template #icon><DownloadOutlined /></template>
                下载
              </a-button>
              <a-popconfirm
                title="确定要删除这个模板吗？"
                @confirm="handleDelete(record.id)"
              >
                <a-button v-permission="['business:contract:template:delete']" type="link" size="small" danger>
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑合同模板' : '新增合同模板'"
      @ok="handleSave"
      @cancel="closeModal"
      :confirm-loading="saving"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
      >
        <a-form-item label="模板名称" name="templateName">
          <a-input v-model:value="formData.templateName" placeholder="例如：标准出口合同" />
        </a-form-item>
        
        <a-form-item label="模板编码" name="templateCode">
          <a-input v-model:value="formData.templateCode" placeholder="例如：TPL_EXPORT_STD" />
        </a-form-item>

        <a-form-item label="模板类型" name="templateType">
          <a-select v-model:value="formData.templateType" placeholder="请选择类型">
            <a-select-option value="EXPORT">EXPORT-出口合同</a-select-option>
            <a-select-option value="IMPORT">IMPORT-进口合同</a-select-option>
            <a-select-option value="OTHER">OTHER-其他</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="排序" name="sort">
          <a-input-number v-model:value="formData.sort" :min="0" style="width: 100%" />
        </a-form-item>

        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formData.description" :rows="3" placeholder="模板用途说明..." />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SearchOutlined, EditOutlined, UploadOutlined, DownloadOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import { getTemplates, uploadTemplate } from '@/api/business/contract'
import request from '@/utils/request'

// 搜索表单
const searchForm = reactive({
  keyword: '',
  templateType: undefined
})

// 表格数据
const templateList = ref([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: '模板名称', dataIndex: 'templateName', key: 'templateName' },
  { title: '编码', dataIndex: 'templateCode', key: 'templateCode' },
  { title: '类型', key: 'templateType', width: 120 },
  { title: '文件名', dataIndex: 'fileName', key: 'fileName' },
  { title: '大小', key: 'fileSize', width: 100 },
  { title: '状态', key: 'status', width: 100 },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 250, fixed: 'right' as const }
]

// 弹窗相关
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref()
const formData = reactive({
  templateName: '',
  templateCode: '',
  templateType: 'EXPORT',
  status: 1,
  sort: 1,
  description: ''
})

const formRules = {
  templateName: [{ required: true, message: '请输入模板名称' }],
  templateCode: [{ required: true, message: '请输入模板编码' }],
  templateType: [{ required: true, message: '请选择模板类型' }]
}

// 加载列表
const loadTemplateList = async () => {
  loading.value = true
  try {
    const res = await getTemplates({
      current: pagination.current,
      size: pagination.pageSize,
      keyword: searchForm.keyword,
      templateType: searchForm.templateType
    })
    templateList.value = res.data.data.records
    pagination.total = res.data.data.total
  } catch (err) {
    message.error('加载模板列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadTemplateList()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.templateType = undefined
  handleSearch()
}

const handleTableChange = (pag: TablePaginationConfig) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadTemplateList()
}

// 操作方法
const openAddModal = () => {
  editingId.value = null
  Object.assign(formData, {
    templateName: '',
    templateCode: '',
    templateType: 'EXPORT',
    status: 1,
    sort: 1,
    description: ''
  })
  modalVisible.value = true
}

const openEditModal = (record: any) => {
  editingId.value = record.id
  Object.assign(formData, {
    templateName: record.templateName,
    templateCode: record.templateCode,
    templateType: record.templateType,
    status: record.status,
    sort: record.sort,
    description: record.description
  })
  modalVisible.value = true
}

const closeModal = () => {
  modalVisible.value = false
}

const handleSave = async () => {
  try {
    await formRef.value.validate()
    saving.value = true
    
    const url = editingId.value ? `/v1/contract/template/${editingId.value}` : '/v1/contract/template'
    const method = editingId.value ? 'put' : 'post'
    
    const res = await request({
      url,
      method,
      data: formData
    })
    
    if (res.data.code === 200) {
      message.success('保存成功')
      modalVisible.value = false
      loadTemplateList()
    } else {
      message.error(res.data.message || '保存失败')
    }
  } catch (err) {
    // 验证失败
  } finally {
    saving.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    const res = await request({
      url: `/v1/contract/template/${id}`,
      method: 'delete'
    })
    if (res.data.code === 200) {
      message.success('删除成功')
      loadTemplateList()
    }
  } catch (err) {
    message.error('删除失败')
  }
}

const handleUpload = async (options: any, templateId: number) => {
  const { file } = options
  const formData = new FormData()
  formData.append('file', file)
  formData.append('templateId', String(templateId))
  
  try {
    const res = await uploadTemplate(formData)
    if (res.data.code === 200) {
      message.success('模板文件上传成功')
      loadTemplateList()
    } else {
      message.error(res.data.message || '上传失败')
    }
  } catch (err) {
    message.error('上传过程中发生错误')
  }
}

const handleDownload = (record: any) => {
  // 使用统一的下载方式
  if (record.id) {
    // 如果是合同生成记录，使用合同下载接口
    window.open(`${(import.meta as any).env.VITE_APP_BASE_API}/v1/contract/download/${record.id}`, '_blank')
  } else if (record.filePath) {
    // 如果是模板文件，使用文件下载接口
    window.open(`${(import.meta as any).env.VITE_APP_BASE_API}/v1/files/download?path=${record.filePath}`, '_blank')
  }
}

// 辅助方法
const getTypeColor = (type: string) => {
  const map: any = {
    'EXPORT': 'blue',
    'IMPORT': 'green',
    'OTHER': 'orange'
  }
  return map[type] || 'default'
}

const formatFileSize = (size: number) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  return (size / (1024 * 1024)).toFixed(2) + ' MB'
}

onMounted(() => {
  loadTemplateList()
})
</script>

<style scoped>
/* 页面特有样式已由全局 index.less 覆盖 */
.contract-template-management {
  padding: 24px;
}
</style>

