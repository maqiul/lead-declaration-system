<template>
  <!-- 水单信息展示区块（每个环节始终显示在最底部，样式与其他区块一致） -->
  <div v-if="formId" class="section-wrapper">
    <!-- 收汇水单 -->
    <a-card id="section-receipt" title="收汇水单" size="small" class="section-card">
      <template #extra>
        <a-tag v-if="receiptRemittances.length > 0" color="blue">{{ receiptRemittances.length }} 笔</a-tag>
      </template>
      <a-table
        v-if="receiptRemittances.length > 0"
        :dataSource="receiptRemittances"
        :columns="receiptColumns"
        :pagination="false"
        size="small"
        rowKey="id"
        :scroll="{ x: 860 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'remittanceNo'">
            <a @click="showDetail('receipt', record)">{{ record.remittanceNo }}</a>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 2 ? 'success' : record.status === 1 ? 'processing' : 'default'">
              {{ record.status === 0 ? '草稿' : record.status === 1 ? '待审核' : record.status === 2 ? '已审核' : '已驳回' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'remittanceAmount'">
            {{ formatCurrency(record.remittanceAmount, record.currency) }}
          </template>
          <template v-if="column.key === 'relationAmount'">
            {{ formatCurrency(record.relationAmount, record.currency) }}
          </template>
          <template v-if="column.key === 'remarks'">
            <a-tooltip v-if="record.remarks" :title="record.remarks">
              <span class="text-ellipsis">{{ record.remarks }}</span>
            </a-tooltip>
            <span v-else class="text-muted">-</span>
          </template>
        </template>
      </a-table>
      <a-empty v-else description="暂无关联收汇水单" :image-style="{ height: '30px' }" />
    </a-card>

    <!-- 出款水单 -->
    <a-card id="section-payment" title="出款水单" size="small" class="section-card">
      <template #extra>
        <a-tag v-if="paymentRemittances.length > 0" color="blue">{{ paymentRemittances.length }} 笔</a-tag>
      </template>
      <a-table
        v-if="paymentRemittances.length > 0"
        :dataSource="paymentRemittances"
        :columns="paymentColumns"
        :pagination="false"
        size="small"
        rowKey="id"
        :scroll="{ x: 860 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'paymentNo'">
            <a @click="showDetail('payment', record)">{{ record.paymentNo }}</a>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 2 ? 'success' : record.status === 1 ? 'processing' : 'default'">
              {{ record.status === 0 ? '草稿' : record.status === 1 ? '待审核' : record.status === 2 ? '已审核' : '已驳回' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'paymentAmount'">
            {{ formatCurrency(record.paymentAmount, record.currency) }}
          </template>
          <template v-if="column.key === 'relationAmount'">
            {{ formatCurrency(record.relationAmount, record.currency) }}
          </template>
          <template v-if="column.key === 'remarks'">
            <a-tooltip v-if="record.remarks" :title="record.remarks">
              <span class="text-ellipsis">{{ record.remarks }}</span>
            </a-tooltip>
            <span v-else class="text-muted">-</span>
          </template>
        </template>
      </a-table>
      <a-empty v-else description="暂无关联出款水单" :image-style="{ height: '30px' }" />
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      :title="detailType === 'receipt' ? '收汇水单详情' : '出款水单详情'"
      width="600px"
      :footer="null"
    >
      <template v-if="currentRecord">
        <a-descriptions :column="2" bordered size="small">
          <template v-if="detailType === 'receipt'">
            <a-descriptions-item label="水单编号">{{ currentRecord.remittanceNo || '-' }}</a-descriptions-item>
            <a-descriptions-item label="收汇名称">{{ currentRecord.remittanceName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="收汇金额">{{ formatCurrency(currentRecord.remittanceAmount, currentRecord.currency) }}</a-descriptions-item>
            <a-descriptions-item label="关联金额">{{ formatCurrency(currentRecord.relationAmount, currentRecord.currency) }}</a-descriptions-item>
            <a-descriptions-item label="币种">{{ currentRecord.currency || '-' }}</a-descriptions-item>
            <a-descriptions-item label="收汇日期">{{ formatDate(currentRecord.remittanceDate, 'yyyy-MM-dd') || '-' }}</a-descriptions-item>
            <a-descriptions-item label="汇率">{{ currentRecord.taxRate ?? '-' }}</a-descriptions-item>
            <a-descriptions-item label="银行手续费">{{ currentRecord.bankFee != null ? formatCurrency(currentRecord.bankFee, currentRecord.currency) : '-' }}</a-descriptions-item>
            <a-descriptions-item label="入账银行">{{ currentRecord.bankAccountName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="currentRecord.status === 2 ? 'success' : currentRecord.status === 1 ? 'processing' : 'default'">
                {{ currentRecord.status === 0 ? '草稿' : currentRecord.status === 1 ? '待审核' : currentRecord.status === 2 ? '已审核' : '已驳回' }}
              </a-tag>
            </a-descriptions-item>
          </template>
          <template v-else>
            <a-descriptions-item label="出款编号">{{ currentRecord.paymentNo || '-' }}</a-descriptions-item>
            <a-descriptions-item label="收款人">{{ currentRecord.payeeName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="出款金额">{{ formatCurrency(currentRecord.paymentAmount, currentRecord.currency) }}</a-descriptions-item>
            <a-descriptions-item label="关联金额">{{ formatCurrency(currentRecord.relationAmount, currentRecord.currency) }}</a-descriptions-item>
            <a-descriptions-item label="币种">{{ currentRecord.currency || '-' }}</a-descriptions-item>
            <a-descriptions-item label="出款日期">{{ formatDate(currentRecord.paymentDate, 'yyyy-MM-dd') || '-' }}</a-descriptions-item>
            <a-descriptions-item label="入账银行">{{ currentRecord.bankAccountName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="currentRecord.status === 2 ? 'success' : currentRecord.status === 1 ? 'processing' : 'default'">
                {{ currentRecord.status === 0 ? '草稿' : currentRecord.status === 1 ? '待审核' : currentRecord.status === 2 ? '已审核' : '已驳回' }}
              </a-tag>
            </a-descriptions-item>
          </template>
          <a-descriptions-item label="备注" :span="2">{{ currentRecord.remarks || '-' }}</a-descriptions-item>
          <a-descriptions-item label="水单凭证" :span="2">
            <a v-if="currentRecord.photoUrl" @click="previewFile(currentRecord.photoUrl)">
              {{ extractFileName(currentRecord.photoUrl) }}
            </a>
            <span v-else class="text-muted">-</span>
          </a-descriptions-item>
        </a-descriptions>
      </template>
    </a-modal>

    <!-- 文件预览 -->
    <FilePreviewModal v-model:visible="previewVisible" :url="previewUrl" :title="previewTitle" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { getRemittancesByFormId } from '@/api/business/remittance'
import { getPaymentRemittancesByFormId } from '@/api/business/paymentRemittance'
import FilePreviewModal from '@/components/FilePreviewModal.vue'
import { formatDate } from '@/utils/common'

interface Props {
  formId: number | null | undefined
}

const props = defineProps<Props>()

const receiptRemittances = ref<any[]>([])
const paymentRemittances = ref<any[]>([])

const receiptColumns = [
  { title: '水单编号', dataIndex: 'remittanceNo', key: 'remittanceNo', width: 140 },
  { title: '收汇名称', dataIndex: 'remittanceName', key: 'remittanceName', width: 110 },
  { title: '收汇金额', key: 'remittanceAmount', width: 110 },
  { title: '关联金额', key: 'relationAmount', width: 110 },
  { title: '币种', dataIndex: 'currency', key: 'currency', width: 70 },
  { title: '收汇日期', dataIndex: 'remittanceDate', key: 'remittanceDate', width: 100 , customRender: ({ text }: any) => text ? formatDate(text, 'yyyy-MM-dd') : '-' },
  { title: '状态', key: 'status', width: 80 },
  { title: '备注', key: 'remarks', width: 120, ellipsis: true }
]

const paymentColumns = [
  { title: '出款编号', dataIndex: 'paymentNo', key: 'paymentNo', width: 140 },
  { title: '收款人', dataIndex: 'payeeName', key: 'payeeName', width: 110 },
  { title: '出款金额', key: 'paymentAmount', width: 110 },
  { title: '关联金额', key: 'relationAmount', width: 110 },
  { title: '币种', dataIndex: 'currency', key: 'currency', width: 70 },
  { title: '出款日期', dataIndex: 'paymentDate', key: 'paymentDate', width: 100 , customRender: ({ text }: any) => text ? formatDate(text, 'yyyy-MM-dd') : '-' },
  { title: '状态', key: 'status', width: 80 },
  { title: '备注', key: 'remarks', width: 120, ellipsis: true }
]

const formatCurrency = (amount: number | undefined, currency: string | undefined) => {
  if (amount === undefined || amount === null) return '-'
  const sym = currency === 'CNY' ? '¥' : currency === 'USD' ? '$' : currency || ''
  return `${sym}${Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

// 详情弹窗
const detailVisible = ref(false)
const detailType = ref<'receipt' | 'payment'>('receipt')
const currentRecord = ref<any>(null)

const showDetail = (type: 'receipt' | 'payment', record: any) => {
  detailType.value = type
  currentRecord.value = record
  detailVisible.value = true
}

// 文件预览
const previewVisible = ref(false)
const previewUrl = ref('')
const previewTitle = ref('')

const extractFileName = (url: string) => {
  if (!url) return '-'
  return decodeURIComponent(url.split('/').pop() || url)
}

const previewFile = (url: string) => {
  previewUrl.value = url
  previewTitle.value = extractFileName(url)
  previewVisible.value = true
}

const loadRemittances = async () => {
  if (!props.formId) {
    receiptRemittances.value = []
    paymentRemittances.value = []
    return
  }

  try {
    const [receiptRes, paymentRes] = await Promise.all([
      getRemittancesByFormId(props.formId),
      getPaymentRemittancesByFormId(props.formId)
    ])

    const receiptData = receiptRes.data
    receiptRemittances.value = receiptData?.code === 200 ? (receiptData.data || []) : []

    const paymentData = paymentRes.data
    paymentRemittances.value = paymentData?.code === 200 ? (paymentData.data || []) : []
  } catch (error) {
    console.error('加载水单信息失败', error)
    receiptRemittances.value = []
    paymentRemittances.value = []
  }
}

watch(() => props.formId, (newVal) => {
  if (newVal) loadRemittances()
}, { immediate: true })
</script>

<style scoped>
.text-ellipsis {
  display: inline-block;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
.text-muted {
  color: #bbb;
}
</style>