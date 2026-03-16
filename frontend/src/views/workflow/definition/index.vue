<template>
  <div class="process-definition">
    <!-- 搜索区域 -->
    <a-card class="search-card">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="流程名称">
          <a-input v-model:value="searchForm.processName" placeholder="请输入流程名称" />
        </a-form-item>
        <a-form-item label="流程KEY">
          <a-input v-model:value="searchForm.processKey" placeholder="请输入流程KEY" />
        </a-form-item>
        <a-form-item label="分类">
          <a-select v-model:value="searchForm.category" style="width: 120px" placeholder="请选择分类">
            <a-select-option value="HR">人事</a-select-option>
            <a-select-option value="FINANCE">财务</a-select-option>
            <a-select-option value="IT">IT</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchForm.status" style="width: 120px" placeholder="请选择状态">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">停用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSearch">搜索</a-button>
          <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮区域 -->
    <a-card class="operation-card">
      <a-space>
        <a-button type="primary" @click="handleDeploy">
          <template #icon><upload-outlined /></template>
          部署流程
        </a-button>
        <a-button @click="handleBatchDisable" :disabled="selectedRowKeys.length === 0">
          <template #icon><stop-outlined /></template>
          批量停用
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card>
      <a-table
        :dataSource="tableData"
        :columns="columns"
        :pagination="pagination"
        :loading="loading"
        :row-selection="rowSelection"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '启用' : '停用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
              <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button 
                v-if="record.status === 1" 
                type="link" 
                size="small" 
                @click="handleDisable(record.id)"
              >
                停用
              </a-button>
              <a-button 
                v-else 
                type="link" 
                size="small" 
                @click="handleEnable(record.id)"
              >
                启用
              </a-button>
              <a-popconfirm
                title="确定要删除这个流程定义吗？"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 流程部署弹窗 -->
    <a-modal
      v-model:open="deployVisible"
      title="部署流程"
      :confirm-loading="deployLoading"
      @ok="handleDeployOk"
      @cancel="handleDeployCancel"
      width="800px"
    >
      <a-form
        ref="deployFormRef"
        :model="deployForm"
        :rules="deployRules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="流程名称" name="processName">
              <a-input v-model:value="deployForm.processName" placeholder="请输入流程名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="流程KEY" name="processKey">
              <a-input v-model:value="deployForm.processKey" placeholder="请输入流程KEY" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-form-item label="流程分类" name="category">
          <a-select v-model:value="deployForm.category" placeholder="请选择流程分类">
            <a-select-option value="HR">人事</a-select-option>
            <a-select-option value="FINANCE">财务</a-select-option>
            <a-select-option value="IT">IT</a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item label="流程描述" name="description">
          <a-textarea v-model:value="deployForm.description" placeholder="请输入流程描述" :rows="3" />
        </a-form-item>
        
        <a-form-item label="BPMN文件" name="bpmnFile">
          <a-upload
            v-model:file-list="fileList"
            :before-upload="beforeUpload"
            :max-count="1"
            accept=".bpmn,.xml"
          >
            <a-button>
              <template #icon><upload-outlined /></template>
              上传BPMN文件
            </a-button>
          </a-upload>
          <div style="margin-top: 8px; color: #999; font-size: 12px;">
            支持.bpmn和.xml格式文件
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 流程查看弹窗 -->
    <a-modal
      v-model:open="viewVisible"
      title="流程详情"
      width="800px"
      :footer="null"
    >
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="流程名称">{{ currentProcess?.processName }}</a-descriptions-item>
        <a-descriptions-item label="流程KEY">{{ currentProcess?.processKey }}</a-descriptions-item>
        <a-descriptions-item label="分类">{{ currentProcess?.category }}</a-descriptions-item>
        <a-descriptions-item label="版本">{{ currentProcess?.version }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="currentProcess?.status === 1 ? 'green' : 'red'">
            {{ currentProcess?.status === 1 ? '启用' : '停用' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ currentProcess?.createTime }}</a-descriptions-item>
        <a-descriptions-item label="描述" :span="2">{{ currentProcess?.description }}</a-descriptions-item>
      </a-descriptions>
      
      <div style="margin-top: 24px">
        <h4>BPMN流程图</h4>
        <div class="bpmn-preview">
          <a-empty description="流程图预览区域" />
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined, StopOutlined } from '@ant-design/icons-vue'
import type { TableProps, UploadProps } from 'ant-design-vue'
import { getProcessDefinitions, deployProcessDefinition } from '@/api/workflow'

// 类型定义
interface ProcessDefinition {
  id: number
  processKey: string
  processName: string
  description: string
  category: string
  version: number
  status: number
  createTime: string
}

interface SearchForm {
  processName?: string
  processKey?: string
  category?: string
  status?: number
}

// 响应式数据
const loading = ref(false)
const tableData = ref<ProcessDefinition[]>([])
const selectedRowKeys = ref<number[]>([])
const fileList = ref<any[]>([])

// 搜索表单
const searchForm = reactive<SearchForm>({
  processName: '',
  processKey: '',
  category: undefined,
  status: undefined
})

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
  {
    title: '流程名称',
    dataIndex: 'processName',
    key: 'processName'
  },
  {
    title: '流程KEY',
    dataIndex: 'processKey',
    key: 'processKey'
  },
  {
    title: '分类',
    dataIndex: 'category',
    key: 'category'
  },
  {
    title: '版本',
    dataIndex: 'version',
    key: 'version'
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status'
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime'
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right' as const,
    width: 250
  }
]

// 表格行选择配置
const rowSelection: TableProps['rowSelection'] = {
  onChange: (selectedKeys: number[]) => {
    selectedRowKeys.value = selectedKeys
  }
}

// 部署弹窗相关
const deployVisible = ref(false)
const deployLoading = ref(false)
const deployFormRef = ref()

// 部署表单数据
const deployForm = reactive({
  processName: '',
  processKey: '',
  category: undefined as string | undefined,
  description: ''
})

// 部署表单验证规则
const deployRules = {
  processName: [
    { required: true, message: '请输入流程名称', trigger: 'blur' }
  ],
  processKey: [
    { required: true, message: '请输入流程KEY', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '流程KEY格式不正确', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择流程分类', trigger: 'change' }
  ],
  bpmnFile: [
    { required: true, message: '请上传BPMN文件', trigger: 'change' }
  ]
}

// 查看弹窗相关
const viewVisible = ref(false)
const currentProcess = ref<ProcessDefinition | null>(null)

// 方法
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      processName: searchForm.processName,
      processKey: searchForm.processKey,
      category: searchForm.category,
      status: searchForm.status
    }
    
    const response = await getProcessDefinitions(params)
    if (response.data?.code === 200) {
      tableData.value = response.data.data.records
      pagination.total = Number(response.data.data.total) || 0
    } else {
      message.error(response.data?.message || '加载数据失败')
    }
  } catch (error) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  searchForm.processName = ''
  searchForm.processKey = ''
  searchForm.category = undefined
  searchForm.status = undefined
  handleSearch()
}

const handleDeploy = () => {
  Object.assign(deployForm, {
    processName: '',
    processKey: '',
    category: undefined,
    description: ''
  })
  fileList.value = []
  deployVisible.value = true
}

const handleView = (record: ProcessDefinition) => {
  currentProcess.value = record
  viewVisible.value = true
}

const handleEdit = (record: ProcessDefinition) => {
  message.info(`编辑流程: ${record.processName}`)
}

// TODO: 后端暂未提供删除、启用、停用流程定义的接口
const handleDelete = async (id: number) => {
  message.info('此功能暂未实现')
}

const handleDisable = async (id: number) => {
  message.info('此功能暂未实现')
}

const handleEnable = async (id: number) => {
  message.info('此功能暂未实现')
}

const handleBatchDisable = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要停用的流程')
    return
  }
  
  // 模拟批量停用
  message.success('批量停用成功')
  selectedRowKeys.value = []
  loadData()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isBpmn = file.type === 'application/xml' || file.name.endsWith('.bpmn') || file.name.endsWith('.xml')
  if (!isBpmn) {
    message.error('只能上传.bpmn或.xml格式的文件!')
    return false
  }
  
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('文件大小不能超过2MB!')
    return false
  }
  
  return true
}

const handleDeployOk = async () => {
  try {
    await deployFormRef.value?.validateFields()
    deployLoading.value = true
    
    if (fileList.value.length === 0) {
      message.error('请上传BPMN文件')
      deployLoading.value = false
      return
    }
    
    const file = fileList.value[0].originFileObj
    const response = await deployProcessDefinition(file, deployForm)
    
    if (response.data?.code === 200) {
      message.success('流程部署成功')
      deployVisible.value = false
      loadData()
    } else {
      message.error(response.data?.message || '流程部署失败')
    }
  } catch (error) {
    message.error('流程部署失败')
  } finally {
    deployLoading.value = false
  }
}

const handleDeployCancel = () => {
  deployVisible.value = false
  deployFormRef.value?.resetFields()
  fileList.value = []
}

// 生命周期
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.process-definition {
  padding: 0;
  background: transparent;
  min-height: 100%;
  box-sizing: border-box;
  overflow-x: hidden;
}

.search-card {
  margin-bottom: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

.operation-card {
  margin-bottom: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

.bpmn-preview {
  height: 300px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fafafa 0%, #f0f0f0 100%);
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
}

:deep(.ant-card) {
  border-radius: 8px;
}

:deep(.ant-card-body) {
  padding: 24px;
}

:deep(.ant-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 600;
}

:deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

:deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
}

:deep(.ant-upload) {
  border-radius: 6px;
}

:deep(.ant-descriptions) {
  background: white;
  border-radius: 8px;
  padding: 16px;
}
</style>