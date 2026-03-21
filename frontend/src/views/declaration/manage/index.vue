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
            @change="loadData"
          >
            <a-select-option value="">全部</a-select-option>
            <a-select-option :value="0">草稿</a-select-option>
            <a-select-option :value="1">待初审</a-select-option>
            <a-select-option :value="2">待付定金</a-select-option>
            <a-select-option :value="3">定金待审</a-select-option>
            <a-select-option :value="4">待付尾款</a-select-option>
            <a-select-option :value="5">尾款待审</a-select-option>
            <a-select-option :value="6">提货单待传</a-select-option>
            <a-select-option :value="7">提货单待审</a-select-option>
            <a-select-option :value="8">已完成</a-select-option>
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
          <a-button type="primary" @click="loadData" v-permission="['business:declaration:list']">查询</a-button>
          <a-button style="margin-left: 8px" @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮 -->
    <a-card class="operation-card">
      <a-space>
        <a-button type="primary" @click="handleAdd" v-permission="['business:declaration:add']">
          <template #icon><plus-outlined /></template>
          新增申报单
        </a-button>
        <a-button @click="handleExport" v-permission="['business:declaration:export']">
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
        :scroll="{ x: 1500 }"
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
              <a-button type="link" size="small" @click="handleView(record as any)" v-permission="['business:declaration:view']">
                <template #icon><EyeOutlined /></template>
                查看
              </a-button>
              <!-- 只有草稿状态才显示数据提交和编辑按钮 -->
              <a-button v-if="record.status === 0" type="link" size="small" @click="handleStatusSubmit(record as any)" v-permission="['business:declaration:submit']">
                <template #icon><SendOutlined /></template>
                提交
              </a-button>
              <a-button v-if="record.status === 0" type="link" size="small" @click="handleEdit(record as any)" v-permission="['business:declaration:update']">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button v-if="[1, 3, 5].includes(record.status)" type="link" size="small" style="color: #faad14;" @click="handleAudit(record as any)" v-permission="['business:declaration:audit']">
                <template #icon><CheckCircleOutlined /></template>
                {{ getAuditBtnText(record.status) }}
              </a-button>
              <!-- 状态 2(待付定金) 或状态 4(待付尾款) 显示付款按钮 -->
              <a-button v-if="record.status === 2" type="link" size="small" style="color: #52c41a;" @click="handlePayment(record as any)" v-permission="['business:declaration:payment']">
                <template #icon><DollarOutlined /></template>
                付定金
              </a-button>
              <a-button v-if="record.status === 4" type="link" size="small" style="color: #52c41a;" @click="handlePayment(record as any)" v-permission="['business:declaration:payment']">
                <template #icon><DollarOutlined /></template>
                付尾款
              </a-button>
              <!-- 提货单待传状态显示提交提货单按钮 -->
              <a-button v-if="record.status === 6" type="link" size="small" style="color: #1890ff;" @click="handlePickupSubmit(record as any)" v-permission="['business:declaration:pickup-submit']">
                <template #icon><UploadOutlined /></template>
                提交提货单
              </a-button>
              <!-- 提货单待审状态显示审核按钮 -->
              <a-button 
                v-if="record.status === 7" 
                type="link" 
                size="small" 
                style="color: #faad14;" 
                @click="handleAudit(record as any)"
                v-permission="['business:declaration:pickup-audit']"
              >
                <template #icon><CheckCircleOutlined /></template>
                提货单审核
              </a-button>
              <!-- 生成合同按钮 -->
<<<<<<< HEAD
              <a-button type="link" size="small" style="color: #722ed1;" @click="handleOpenGenerate(record as any)" v-permission="['business:declaration:audit']">
                <template #icon><FileTextOutlined /></template>
                生成合同
              </a-button>

=======
              <a-button type="link" size="small" style="color: #722ed1;" @click="handleOpenGenerate(record as any)" v-permission="['business:declaration:contract']">
                <template #icon><FileTextOutlined /></template>
                生成合同
              </a-button>
>>>>>>> 974d00a7096735aae9219cfa167a551b72278b5f
              <a-button v-if="record.status >= 2" type="link" size="small" @click="handleDownload(record as any)" v-permission="['business:declaration:download']">
                <template #icon><DownloadOutlined /></template>
                单证
              </a-button>
              <!-- 只有草稿状态才显示删除按钮 -->
              <a-popconfirm
                v-if="record.status === 0"
                title="确定要删除该申报单吗？"
                @confirm="handleDelete(record as any)"
              >
                <a-button type="link" size="small" danger v-permission="['business:declaration:delete']">
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
      title="附件管理" 
      width="700px"
<<<<<<< HEAD
=======
      :footer="null"
>>>>>>> 974d00a7096735aae9219cfa167a551b72278b5f
    >
      <template #footer>
        <a-button @click="attachmentModalVisible = false">关闭</a-button>
      </template>
      
      <a-list :dataSource="currentAttachments" bordered size="small">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <div class="attachment-title">
                  <FileOutlined v-if="isDocumentFile(item.fileName)" style="color: #1890ff; margin-right: 8px;" />
                  <PictureOutlined v-else-if="isImageFile(item.fileName)" style="color: #52c41a; margin-right: 8px;" />
                  <FileUnknownOutlined v-else style="color: #faad14; margin-right: 8px;" />
                  {{ item.fileName }}
                </div>
              </template>
              <template #description>
                <div class="attachment-info">
                  <a-tag :color="getFileTypeColor(item.fileType)">
                    {{ getFileTypeText(item.fileType) }}
                  </a-tag>
                  <span class="file-size">{{ formatFileSize(item.fileSize) }}</span>
                  <span class="create-time">{{ formatDate(item.createTime) }}</span>
                </div>
              </template>
            </a-list-item-meta>
            <template #actions>
              <a-space>
                <a-button type="link" size="small" @click="downloadAttachment(item)" v-permission="['business:declaration:download']">
                  <template #icon><DownloadOutlined /></template>
                  下载
                </a-button>
                <a-button 
<<<<<<< HEAD
                  v-if="currentDeclaration && currentDeclaration.status >= 1" 
                  type="link" 
                  size="small" 
                  style="color: #1890ff;"
                  @click="handleRegenerate(currentDeclaration)"
                  v-permission="['business:declaration:audit']"
                >
                  <template #icon><ReloadOutlined /></template>
                  重新生成
                </a-button>
                <a-button 
=======
>>>>>>> 974d00a7096735aae9219cfa167a551b72278b5f
                  v-if="canReplaceAttachment(item)" 
                  type="link" 
                  size="small" 
                  style="color: #faad14;"
                  @click="showReplaceModal(item)"
                  v-permission="['business:declaration:edit']"
                >
                  <template #icon><UploadOutlined /></template>
                  替换
                </a-button>
              </a-space>
            </template>
          </a-list-item>
        </template>
        <template v-if="currentAttachments.length === 0" #header>
          <div style="text-align: center; color: #999;">暂无自动生成的全套单证或水单文件</div>
        </template>
      </a-list>
    </a-modal>
    
    <!-- 附件替换弹窗 -->
    <a-modal
      v-model:open="replaceModalVisible"
      title="替换附件"
      @ok="handleReplaceAttachment"
      :confirmLoading="replaceLoading"
    >
      <a-form layout="vertical">
        <a-form-item label="当前文件">
          <div>{{ currentReplacingAttachment?.fileName }}</div>
        </a-form-item>
        <a-form-item label="选择新文件" required>
          <a-upload
            :before-upload="beforeReplaceUpload"
            :file-list="replaceFileList"
            :max-count="1"
            accept=".xlsx,.xls,.pdf,.jpg,.jpeg,.png"
          >
            <a-button>
              <template #icon><UploadOutlined /></template>
              选择文件
            </a-button>
          </a-upload>
          <div style="margin-top: 8px; color: #999; font-size: 12px;">
            支持Excel、PDF、图片格式，单个文件不超过10MB
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 生成合同弹窗 -->
    <a-modal
      v-model:open="generateModalVisible"
      title="生成合同"
      @ok="handleConfirmGenerate"
      :confirmLoading="generateLoading"
    >
      <a-form :model="generateForm" layout="vertical">
        <a-form-item label="选择合同模板" required>
          <a-select 
            v-model:value="generateForm.templateId"
            placeholder="请选择合同模板"
            :options="templateOptions"
            :fieldNames="{ label: 'templateName', value: 'id' }"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
<<<<<<< HEAD
import { PlusOutlined, DownloadOutlined, EyeOutlined, EditOutlined, CheckCircleOutlined, DeleteOutlined, DollarOutlined, SendOutlined, UploadOutlined, FileTextOutlined, FileOutlined, PictureOutlined, FileUnknownOutlined, ReloadOutlined } from '@ant-design/icons-vue'
=======
import { PlusOutlined, DownloadOutlined, EyeOutlined, EditOutlined, CheckCircleOutlined, DeleteOutlined, DollarOutlined, SendOutlined, UploadOutlined, FileTextOutlined, FileOutlined, PictureOutlined, FileUnknownOutlined } from '@ant-design/icons-vue'
>>>>>>> 974d00a7096735aae9219cfa167a551b72278b5f
import type { Dayjs } from 'dayjs'
import {
  getDeclarationList,
  deleteDeclaration as deleteDeclarationApi,
  getDeclarationDetail,
<<<<<<< HEAD
  submitDeclaration,
  regenerateDocuments
=======
  submitDeclaration
>>>>>>> 974d00a7096735aae9219cfa167a551b72278b5f
} from '@/api/business/declaration'
import { getEnabledTemplates, generateContract } from '@/api/business/contract'
import { h } from 'vue'

import { useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
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
<<<<<<< HEAD
  { title: '申报单号', dataIndex: 'formNo', key: 'formNo', width: 160 },
  { title: '申报人', dataIndex: 'applicantName', key: 'applicantName', width: 100 },
  { title: '发货人', dataIndex: 'shipperCompany', key: 'shipperCompany', width: 150 },
  { title: '收货人', dataIndex: 'consigneeCompany', key: 'consigneeCompany', width: 150 },
  { title: '申报日期', dataIndex: 'declarationDate', key: 'declarationDate', width: 120 },
  { title: '总金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 100 },
  { title: '总箱数', dataIndex: 'totalCartons', key: 'totalCartons', width: 80 },
=======
  { title: '申报单号', dataIndex: 'formNo', key: 'formNo' },
  { title: '申报人', dataIndex: 'applicantName', key: 'applicantName' },
  { title: '发货人', dataIndex: 'shipperCompany', key: 'shipperCompany' },
  { title: '收货人', dataIndex: 'consigneeCompany', key: 'consigneeCompany' },
  { title: '申报日期', dataIndex: 'declarationDate', key: 'declarationDate' },
  { title: '总金额', dataIndex: 'totalAmount', key: 'totalAmount' },
  { title: '总箱数', dataIndex: 'totalCartons', key: 'totalCartons' },
>>>>>>> 974d00a7096735aae9219cfa167a551b72278b5f
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
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  {
    title: '操作',
    key: 'action',
    width: 300,
    fixed: 'right' as const
  }
]

// 弹窗相关
// 附件下载弹窗
const attachmentModalVisible = ref(false)
const currentAttachments = ref<any[]>([])
const currentDeclaration = ref<DeclarationRecord | null>(null)

// 附件替换弹窗
const replaceModalVisible = ref(false)
const replaceLoading = ref(false)
const currentReplacingAttachment = ref<any>(null)
const replaceFileList = ref<any[]>([])

// 附件替换弹窗
const replaceModalVisible = ref(false)
const replaceLoading = ref(false)
const currentReplacingAttachment = ref<any>(null)
const replaceFileList = ref<any[]>([])

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

// 提交操作 (从列表直接触发流程启动)
const handleStatusSubmit = async (record: DeclarationRecord) => {
  try {
    loading.value = true
    const res = await submitDeclaration(record.id)
    if (res.data && res.data.code === 200) {
      message.success('流程启动完成，已进入部门初审阶段')
      loadData()
    } else {
      message.error('提交失败: ' + (res.data?.message || '未知错误'))
    }
  } catch (error: any) {
    message.error('提交操作失败: ' + (error.message || '网络错误'))
  } finally {
    loading.value = false
  }
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

// 提货单提交操作 - 跳转到申报单详情页的提货单上传模式
const handlePickupSubmit = (record: DeclarationRecord) => {
  router.push(`/declaration/form?id=${record.id}&status=${record.status}&mode=payment&type=pickup`)
}

// 审核申报单
const handleAudit = (record: DeclarationRecord) => {
  router.push(`/declaration/form?id=${record.id}&mode=audit`)
}

// 下载单证
const handleDownload = async (record: DeclarationRecord) => {
  try {
    loading.value = true
    currentDeclaration.value = record
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

<<<<<<< HEAD
// 重新生成单据
const handleRegenerate = (record: DeclarationRecord) => {
  Modal.confirm({
    title: '确认重新生成',
    content: `确定要重新生成申报单 ${record.formNo} 的单据吗？`,
    onOk: async () => {
      try {
        const res = await regenerateDocuments(record.id)
        if (res.data && res.data.code === 200) {
          message.success('单据重新生成成功')
        } else {
          message.error('生成失败: ' + (res.data?.message || '未知错误'))
        }
      } catch (e: any) {
        message.error('生成失败: ' + (e.message || '网络错误'))
      }
    }
  })
}

=======
>>>>>>> 974d00a7096735aae9219cfa167a551b72278b5f
// 生成合同相关
const generateModalVisible = ref(false)
const generateLoading = ref(false)
const templateOptions = ref<any[]>([])
const generateForm = reactive({
  declarationFormId: undefined as number | undefined,
  templateId: undefined as number | undefined
})

const handleOpenGenerate = async (record: DeclarationRecord) => {
  generateForm.declarationFormId = record.id
  generateForm.templateId = undefined
  
  try {
    const res = await getEnabledTemplates()
    if (res.data && res.data.code === 200) {
      templateOptions.value = res.data.data || []
      generateModalVisible.value = true
    } else {
      message.error('获取合同模板失败: ' + (res.data?.message || '未知错误'))
    }
  } catch (error) {
    message.error('获取合同模板失败')
  }
}

const handleConfirmGenerate = async () => {
  if (!generateForm.templateId) {
    message.warning('请选择合同模板')
    return
  }
  if (!generateForm.declarationFormId) return
  
  generateLoading.value = true
  try {
    const res = await generateContract(generateForm.templateId, generateForm.declarationFormId, {})
    if (res.data && res.data.code === 200) {
      const generation = res.data.data
      message.success('合同生成成功')
      generateModalVisible.value = false
      
      // 成功后提示并提供下载
      if (generation && generation.id) {
        Modal.confirm({
          title: '合同已生成',
          content: `合同编号：${generation.contractNo}，是否立即下载？`,
          okText: '下载',
          cancelText: '关闭',
          onOk: () => {
            // 调用下载接口
            window.open(`/api/v1/contract/download/${generation.id}`, '_blank')
          }
        })
      }
    } else {
      message.error('合同生成失败: ' + (res.data?.message || '未知错误'))
    }
  } catch (error) {
    message.error('合同生成请求失败')
  } finally {
    generateLoading.value = false
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
    6: '提货单待传',
    7: '提货单待审',
    8: '已完成'
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
    6: 'warning',
    7: 'processing',
    8: 'success'
  }
  return colorMap[status] || 'default'
}

// 附件相关函数
const formatDate = (dateString: string) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleDateString('zh-CN')
}

// 判断是否为文档文件
const isDocumentFile = (fileName: string) => {
  const docExtensions = ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx', '.txt']
  const ext = fileName.toLowerCase().substring(fileName.lastIndexOf('.'))
  return docExtensions.includes(ext)
}

// 判断是否为图片文件
const isImageFile = (fileName: string) => {
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg']
  const ext = fileName.toLowerCase().substring(fileName.lastIndexOf('.'))
  return imageExtensions.includes(ext)
}

// 获取文件类型颜色
const getFileTypeColor = (fileType: string) => {
  const colorMap: Record<string, string> = {
    'Invoice': 'blue',
    'PackingList': 'green',
    'FullDocuments': 'purple',
    'PickupList': 'orange',
    'Remittance': 'cyan',
    'Contract': 'magenta'
  }
  return colorMap[fileType] || 'default'
}

// 获取文件类型文本
const getFileTypeText = (fileType: string) => {
  const textMap: Record<string, string> = {
    'Invoice': '商业发票',
    'PackingList': '装箱单',
    'FullDocuments': '全套单证',
    'PickupList': '提货单',
    'Remittance': '水单',
    'Contract': '合同'
  }
  return textMap[fileType] || fileType
}

// 获取文件扩展名
const getFileExtension = (fileName: string) => {
  return fileName.substring(fileName.lastIndexOf('.'))
}

// 格式化文件大小
const formatFileSize = (size: number) => {
  if (!size) return '0 KB'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(1) + ' MB'
}

const downloadAttachment = (attachment: any) => {
  if (attachment.fileUrl) {
    window.open(attachment.fileUrl, '_blank')
  }
}

<<<<<<< HEAD



const canReplaceAttachment = (attachment: any) => {
  console.log('Checking attachment replacement:', attachment)
=======
const canReplaceAttachment = (attachment: any) => {
>>>>>>> 974d00a7096735aae9219cfa167a551b72278b5f
  // 只有已完成状态的申报单才能替换附件
  return true // 暂时允许所有状态替换
}

const showReplaceModal = (attachment: any) => {
  currentReplacingAttachment.value = attachment
  replaceFileList.value = []
  replaceModalVisible.value = true
}

const beforeReplaceUpload = (file: any) => {
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    message.error('文件大小不能超过10MB!')
    return false
  }
  
  // 校验文件格式必须与原文件一致
  if (currentReplacingAttachment.value) {
    const originalExt = getFileExtension(currentReplacingAttachment.value.fileName)
    const newExt = getFileExtension(file.name)
    
    if (originalExt.toLowerCase() !== newExt.toLowerCase()) {
      message.error(`文件格式必须与原文件一致 (${originalExt})`)
      return false
    }
  }
  
  replaceFileList.value = [file]
  return false
}

const handleReplaceAttachment = async () => {
  if (!currentReplacingAttachment.value || replaceFileList.value.length === 0) {
    message.warning('请选择要上传的文件')
    return
  }
  
  replaceLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', replaceFileList.value[0])
    
    const response = await fetch(`/api/v1/declarations/${currentReplacingAttachment.value.formId}/attachments/${currentReplacingAttachment.value.id}/replace`, {
      method: 'POST',
      body: formData
    })
    
    const result = await response.json()
    if (result.code === 200) {
      message.success('附件替换成功')
      replaceModalVisible.value = false
      // 重新加载附件列表
      if (currentAttachments.value.length > 0) {
        const formId = currentAttachments.value[0].formId
        const response = await getDeclarationDetail(formId, 8) // 假设已完成状态
        if (response.data && response.data.code === 200) {
          currentAttachments.value = response.data.data.attachments || []
        }
      }
    } else {
      message.error('替换失败: ' + (result.message || '未知错误'))
    }
  } catch (error) {
    message.error('替换失败: 网络错误')
  } finally {
    replaceLoading.value = false
  }
}

// 页面加载
onMounted(() => {
  loadData()
  
  // 拦截来自工作台待办任务的特定参数，并自动开启审核弹窗/进入审核流程
  if (route.query.action === 'audit' && route.query.id) {
    const id = Number(route.query.id)
    if (!isNaN(id)) {
      // 稍微延迟确保数据渲染，不过我们这是路由跳转，此时直接用 params 跳表单详情即可
      setTimeout(() => {
        router.push(`/declaration/form?id=${id}&mode=audit`)
      }, 300)
    }
  }
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

/* 附件列表样式 */
.attachment-title {
  display: flex;
  align-items: center;
  font-weight: 500;
}

.attachment-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
}

.file-size {
  font-size: 12px;
  color: #888;
}

.create-time {
  font-size: 12px;
  color: #888;
}
</style>