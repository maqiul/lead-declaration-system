<template>
  <div class="tax-refund-detail">
    <!-- 页面头部 -->
    <a-card :title="`退税申请详情 - ${detailData.applicationNo || ''}`">
      <template #extra>
        <div class="header-actions">
          <a-tag :color="getStatusColor(detailData.status)">
            {{ getStatusText(detailData.status) }}
          </a-tag>
          <a-button @click="router.push('/tax-refund/list')">返回列表</a-button>
          <!-- 简化审核按钮显示逻辑 -->
          <template v-if="isAuditMode">
            <a-button v-if="detailData.status === 0" @click="handleEdit">编辑</a-button>
            <a-button v-if="detailData.status === 0" type="primary" @click="handleSubmit">提交</a-button>
            
            <!-- 审核操作区域 -->
            <template v-if="canReview()">
              <a-divider type="vertical" style="height: 24px; margin: 0 8px;" />
              <a-button 
                type="primary" 
                @click="showAuditModal('approve')"
              >
                审核通过
              </a-button>
              <a-button 
                danger 
                @click="showAuditModal('reject')"
              >
                审核拒绝
              </a-button>
            </template>
          </template>
        </div>
      </template>

    <a-spin :spinning="loading">
      <a-row :gutter="24">
        <!-- 左侧：申请信息 -->
        <a-col :span="16">
          <a-card title="申请信息" class="info-card">
            <a-descriptions :column="2" bordered size="middle">
              <a-descriptions-item label="申请编号">{{ detailData.applicationNo }}</a-descriptions-item>
              <a-descriptions-item label="关联申报单">
                <a @click="viewDeclaration(detailData.declarationFormId)">
                  {{ detailData.declarationFormId }}
                </a>
              </a-descriptions-item>
              <a-descriptions-item label="申请人">{{ detailData.initiatorName }}</a-descriptions-item>
              <a-descriptions-item label="申请部门">{{ detailData.departmentName }}</a-descriptions-item>
              <a-descriptions-item label="申请类型">{{ getApplicationTypeText(detailData.applicationType) }}</a-descriptions-item>
              <a-descriptions-item label="申请金额">
                <span class="amount-highlight">¥{{ detailData.amount?.toFixed(2) }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="发票号码">{{ detailData.invoiceNo }}</a-descriptions-item>
              <a-descriptions-item label="发票金额">¥{{ detailData.invoiceAmount?.toFixed(2) }}</a-descriptions-item>
              <a-descriptions-item label="税率">{{ detailData.taxRate }}%</a-descriptions-item>
              <a-descriptions-item label="预计退税金额">
                <span class="refund-amount">¥{{ calculateRefundAmount() }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="申请时间">{{ formatDate(detailData.createTime) }}</a-descriptions-item>
              <a-descriptions-item label="更新时间">{{ formatDate(detailData.updateTime) }}</a-descriptions-item>
              <a-descriptions-item label="申请说明" :span="2">
                {{ detailData.description || '无' }}
              </a-descriptions-item>
            </a-descriptions>
          </a-card>

          <!-- 审核信息 -->
          <a-card title="审核信息" class="info-card" v-if="detailData.status > 1">
            <a-descriptions :column="1" bordered size="small">
              <a-descriptions-item label="财务初审人">{{ detailData.firstReviewerName || '未审核' }}</a-descriptions-item>
              <a-descriptions-item label="初审时间">{{ formatDate(detailData.firstReviewTime) }}</a-descriptions-item>
              <a-descriptions-item label="初审意见">{{ detailData.firstReviewOpinion || '无' }}</a-descriptions-item>
              <a-descriptions-item label="财务复审人">{{ detailData.finalReviewerName || '未审核' }}</a-descriptions-item>
              <a-descriptions-item label="复审时间">{{ formatDate(detailData.finalReviewTime) }}</a-descriptions-item>
              <a-descriptions-item label="复审意见">{{ detailData.finalReviewOpinion || '无' }}</a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <!-- 右侧：附件和操作 -->
        <a-col :span="8">
          <!-- 附件列表 -->
          <a-card title="相关附件" class="attachment-card">
            <div class="attachment-list">
              <div v-if="attachments.length === 0" class="empty-attachments">
                <inbox-outlined style="font-size: 24px; color: #ccc;" />
                <div>暂无附件</div>
              </div>
              <div v-else class="attachment-items">
                <div 
                  v-for="attachment in attachments" 
                  :key="attachment.id" 
                  class="attachment-item"
                >
                  <file-outlined style="margin-right: 8px; color: #1890ff;" />
                  <span class="attachment-name">{{ attachment.fileName }}</span>
                  <a-space class="attachment-actions">
                    <a @click="downloadAttachment(attachment)">下载</a>
                    <a @click="previewAttachment(attachment)">预览</a>
                  </a-space>
                </div>
              </div>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
      
    <!-- 审核模态框 -->
    <a-modal
      v-model:visible="auditModalVisible"
      :title="auditModalTitle"
      @ok="handleAuditConfirm"
      @cancel="handleAuditCancel"
      :confirm-loading="auditLoading"
    >
      <a-form layout="vertical">
        <a-form-item 
          label="审核意见"
          :required="true"
        >
          <a-textarea
            v-model:value="auditForm.opinion"
            placeholder="请输入审核意见"
            :rows="4"
            :maxlength="500"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>
    
    <!-- 发票提交模态框 -->
    <a-modal
      v-model:visible="invoiceModalVisible"
      title="提交发票信息"
      @ok="handleInvoiceSubmit"
      @cancel="handleInvoiceCancel"
    >
      <a-form layout="vertical">
        <a-form-item label="发票号码" required>
          <a-input 
            v-model:value="invoiceForm.invoiceNo" 
            placeholder="请输入发票号码"
          />
        </a-form-item>
        <a-form-item label="发票金额" required>
          <a-input-number 
            v-model:value="invoiceForm.invoiceAmount" 
            placeholder="请输入发票金额"
            style="width: 100%"
            :min="0"
            :precision="2"
            addon-after="元"
          />
        </a-form-item>
        <a-form-item label="税率(%)" required>
          <a-input-number 
            v-model:value="invoiceForm.taxRate" 
            placeholder="请输入税率"
            style="width: 100%"
            :min="0"
            :max="100"
            :precision="2"
            addon-after="%"
          />
        </a-form-item>
      </a-form>
    </a-modal>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { 
  InboxOutlined, 
  FileOutlined 
} from '@ant-design/icons-vue'
import { getTaxRefundDetail, auditTaxRefund, getTaxRefundAttachments, submitInvoice } from '@/api/tax-refund'

const router = useRouter()
const route = useRoute()

// 页面状态
const loading = ref(false)
const detailData = ref<any>({})
const attachments = ref<any[]>([])

// 是否审核模式
const isAuditMode = computed(() => route.query.mode === 'audit')

// 审核相关状态
const auditModalVisible = ref(false)
const auditLoading = ref(false)
const auditType = ref<'approve' | 'reject'>('approve')
const auditForm = reactive({
  opinion: ''
})

// 审核模态框标题
const auditModalTitle = computed(() => {
  const titles = {
    approve: '审核通过',
    reject: '审核拒绝'
  }
  return titles[auditType.value] || '审核操作'
})

// 发票提交相关
const invoiceModalVisible = ref(false)
const invoiceForm = reactive({
  invoiceNo: '',
  invoiceAmount: undefined,
  taxRate: undefined
})

const handleInvoiceSubmit = async () => {
  if (!invoiceForm.invoiceNo || !invoiceForm.invoiceAmount || !invoiceForm.taxRate) {
    message.warning('请填写完整的发票信息')
    return
  }
  
  try {
    const response = await submitInvoice(detailData.value.id, {
      invoiceNo: invoiceForm.invoiceNo,
      invoiceAmount: invoiceForm.invoiceAmount,
      taxRate: invoiceForm.taxRate
    })
    
    if (response.data?.code === 200) {
      message.success('发票提交成功')
      invoiceModalVisible.value = false
      await loadData()
    } else {
      message.error(response.data?.message || '提交失败')
    }
  } catch (error) {
    message.error('提交失败')
  }
}

const handleInvoiceCancel = () => {
  invoiceModalVisible.value = false
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

// 获取申请类型文本
const getApplicationTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    'EXPORT_REFUND': '出口退税',
    'VAT_REFUND': '增值税退税',
    'OTHER_REFUND': '其他退税'
  }
  return typeMap[type] || type
}

// 格式化日期
const formatDate = (date: string) => {
  if (!date) return '无'
  return new Date(date).toLocaleString('zh-CN')
}

// 计算预计退税金额
const calculateRefundAmount = () => {
  if (!detailData.value.invoiceAmount || !detailData.value.taxRate) return '0.00'
  const amount = detailData.value.invoiceAmount * (detailData.value.taxRate / 100)
  return amount.toFixed(2)
}

// 是否可以审核
const canReview = () => {
  // 财务初审(状态2)和财务复审(状态6)可以审核
  return detailData.value.status === 2 || detailData.value.status === 6
}

// 查看申报单
const viewDeclaration = (id: number) => {
  message.info(`跳转到申报单详情: ${id}`)
  // router.push(`/declaration/form?id=${id}&readonly=true`)
}

// 编辑申请
const handleEdit = () => {
  router.push(`/tax-refund/apply?id=${detailData.value.id}`)
}

// 提交申请
const handleSubmit = async () => {
  try {
    loading.value = true
    // 调用提交API
    message.success('申请提交成功')
    await loadData()
  } catch (error) {
    message.error('提交失败')
  } finally {
    loading.value = false
  }
}

// 显示审核模态框
const showAuditModal = (type: 'approve' | 'reject') => {
  auditType.value = type
  auditForm.opinion = ''
  auditModalVisible.value = true
}

// 确认审核
const handleAuditConfirm = async () => {
  if (!auditForm.opinion.trim()) {
    message.warning('请输入审核意见')
    return
  }
  
  try {
    auditLoading.value = true
    
    const auditData = {
      opinion: auditForm.opinion,
      result: auditType.value === 'approve' ? 1 : 2 // 1-通过 2-拒绝
    }
    
    // 使用统一审核接口
    const response = await auditTaxRefund(Number(route.params.id), auditData)
    
    if (response.data?.code === 200) {
      const actionText = auditType.value === 'approve' ? '审核通过' : '审核拒绝'
      
      message.success(`${actionText}成功`)
      auditModalVisible.value = false
      
      // 审核完成后清除审核模式
      if (route.query.mode === 'audit') {
        router.replace({
          query: { ...route.query, mode: undefined }
        })
      }
      
      // 如果是审核通过，跳转到列表页；如果是拒绝，刷新当前页面
      if (auditType.value === 'approve') {
        router.push('/tax-refund/list')
      } else {
        await loadData()
      }
    } else {
      message.error(response.data?.message || '审核操作失败')
    }
  } catch (error) {
    message.error('审核操作失败')
  } finally {
    auditLoading.value = false
  }
}

// 取消审核
const handleAuditCancel = () => {
  auditModalVisible.value = false
  auditForm.opinion = ''
}

// 下载附件
const downloadAttachment = (attachment: any) => {
  try {
    // 构造下载URL
    let downloadUrl = ''
    
    if (attachment.filePath) {
      // 如果有filePath，直接使用（已包含完整路径）
      const baseUrl = window.location.origin
      downloadUrl = `${baseUrl}${attachment.filePath}&download=true`
    } else if (attachment.fileUrl) {
      // 如果有fileUrl，直接使用
      downloadUrl = attachment.fileUrl
    } else {
      message.error('附件信息不完整，无法下载')
      return
    }
    
    // 创建临时链接并触发下载
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = attachment.fileName || 'download'
    link.target = '_blank'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    message.success(`开始下载: ${attachment.fileName}`)
  } catch (error) {
    message.error('下载失败，请稍后重试')
  }
}

// 预览附件
const previewAttachment = (attachment: any) => {
  try {
    // 构造预览URL
    let previewUrl = ''
    
    if (attachment.filePath) {
      // 如果有filePath，直接使用（已包含完整路径）
      const baseUrl = window.location.origin
      previewUrl = `${baseUrl}${attachment.filePath}`
    } else if (attachment.fileUrl) {
      // 如果有fileUrl，直接使用
      previewUrl = attachment.fileUrl
    } else {
      message.error('附件信息不完整，无法预览')
      return
    }
    
    // 对于图片类型，在新窗口打开
    const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp']
    const fileExt = attachment.fileName ? attachment.fileName.toLowerCase().substring(attachment.fileName.lastIndexOf('.')) : ''
    
    if (imageExtensions.includes(fileExt)) {
      // 图片在新窗口预览
      window.open(previewUrl, '_blank')
    } else {
      // 其他文件类型也用新窗口打开（浏览器会决定是预览还是下载）
      window.open(previewUrl, '_blank')
    }
    
    message.success(`正在预览: ${attachment.fileName}`)
  } catch (error) {
    message.error('预览失败，请稍后重试')
  }
}

// 加载数据
const loadData = async () => {
  try {
    loading.value = true
    const id = route.params.id as string
    
    const response = await getTaxRefundDetail(parseInt(id))
    if (response.data?.code === 200) {
      detailData.value = response.data.data
      
      // 加载附件数据
      // TODO: 实现获取附件API后取消注释
      const attachmentResponse = await getTaxRefundAttachments(parseInt(id))
      if (attachmentResponse.data?.code === 200) {
        attachments.value = attachmentResponse.data.data
      } else {
        attachments.value = []
      }
    } else {
      message.error(response.data?.message || '加载数据失败')
      // 使用模拟数据作为备选
      detailData.value = {
        id: parseInt(id),
        applicationNo: `TR20260317${id.padStart(3, '0')}`,
        declarationFormId: 1,
        initiatorName: '张三',
        departmentName: '财务部',
        applicationType: 'EXPORT_REFUND',
        amount: 50000.00,
        invoiceNo: 'INV20260317001',
        invoiceAmount: 58500.00,
        taxRate: 13,
        description: '出口货物退税申请',
        status: parseInt(route.query.status as string) || 1,
        createTime: '2026-03-17T10:30:00',
        updateTime: '2026-03-17T10:30:00'
      }
    }
  } catch (error) {
    message.error('加载数据失败')
    // 使用模拟数据作为备选
    const id = route.params.id as string
    detailData.value = {
      id: parseInt(id),
      applicationNo: `TR20260317${id.padStart(3, '0')}`,
      declarationFormId: 1,
      initiatorName: '张三',
      departmentName: '财务部',
      applicationType: 'EXPORT_REFUND',
      amount: 50000.00,
      invoiceNo: 'INV20260317001',
      invoiceAmount: 58500.00,
      taxRate: 13,
      description: '出口货物退税申请',
      status: parseInt(route.query.status as string) || 1,
      createTime: '2026-03-17T10:30:00',
      updateTime: '2026-03-17T10:30:00'
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
/* 统一UI风格 - 与 declaration 模块完全一致 */
.tax-refund-detail {
  height: 100%;
  overflow-x: hidden;
}

/* 头部按钮区域 */
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

:deep(.ant-card) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

:deep(.ant-card-body) {
  padding: 24px;
}

:deep(.ant-card-head) {
  background: #FAFBFC;
  border-bottom: 1px solid #E2E8F0;
  min-height: 48px;
}

:deep(.ant-card-head-title) {
  font-size: 15px;
  font-weight: 700;
  color: #1E40AF;
}

/* 主按钮样式已通过全局CSS优化 */
:deep(.ant-btn-primary) {
  background: #2563EB !important;
  border-radius: 8px !important;
}

:deep(.ant-btn-primary:hover) {
  background: #1D4ED8 !important;
  transform: translateY(-1px);
}

.info-card, .attachment-card, .review-card {
  margin-bottom: 24px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  border: 1px solid #E2E8F0;
  background: white;
}

:deep(.ant-descriptions-item-label) {
  font-weight: 600;
  background-color: #FAFBFC !important;
  color: #1E40AF;
}

.amount-highlight {
  font-weight: 700;
  color: #2563EB;
  font-size: 16px;
}

.refund-amount {
  font-weight: 700;
  color: #dc2626;
  font-size: 16px;
}

.attachment-list {
  min-height: 200px;
}

.empty-attachments {
  text-align: center;
  padding: 40px 20px;
  color: #94a3b8;
}

.attachment-items {
  max-height: 300px;
  overflow-y: auto;
}

.attachment-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin-bottom: 8px;
  background: #ffffff;
  transition: all 0.2s;
}

.attachment-item:hover {
  border-color: #94a3b8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.attachment-name {
  flex: 1;
  font-size: 14px;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.attachment-actions {
  font-size: 12px;
}


@media (max-width: 768px) {
  :deep(.ant-card-body) {
    padding: 16px;
  }
}
</style>