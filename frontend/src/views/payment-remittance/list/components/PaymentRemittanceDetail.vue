<template>
  <a-modal
    v-model:open="visible"
    title="出款水单详情"
    width="800px"
    :footer="null"
  >
    <a-spin :spinning="loading">
      <a-descriptions v-if="remittance" bordered :column="2">
        <a-descriptions-item label="出款编号">{{ remittance.paymentNo }}</a-descriptions-item>
        <a-descriptions-item label="收款人">{{ remittance.payeeName }}</a-descriptions-item>
        <a-descriptions-item label="出款日期">{{ remittance.paymentDate }}</a-descriptions-item>
        <a-descriptions-item label="出款金额">
          <span style="font-size: 16px; font-weight: bold; color: #1890ff">
            {{ remittance.paymentAmount?.toFixed(2) }} {{ remittance.currency }}
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(remittance.status)">
            {{ getStatusText(remittance.status) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="银行名称">{{ remittance.bankAccountName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="提交时间">{{ remittance.submitTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="审核人">{{ remittance.auditByName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="审核时间">{{ remittance.auditTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="审核备注" :span="2">{{ remittance.auditRemark || '-' }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ remittance.remarks || '-' }}</a-descriptions-item>
        <a-descriptions-item label="出款凭证" :span="2">
          <template v-if="remittance.photoUrl">
            <a-image v-if="isImage(remittance.photoUrl)" :src="remittance.photoUrl" style="max-width: 400px" />
            <a-button v-else type="link" @click="previewFile(remittance.photoUrl)">
              <FilePdfOutlined /> 查看文件
            </a-button>
          </template>
          <span v-else>无文件</span>
        </a-descriptions-item>
      </a-descriptions>
    </a-spin>
  </a-modal>

  <FilePreviewModal v-model:visible="previewVisible" :url="previewUrl" />
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { FilePdfOutlined } from '@ant-design/icons-vue'
import { getPaymentRemittanceDetail } from '@/api/business/paymentRemittance'
import FilePreviewModal from '@/components/FilePreviewModal.vue'

const previewVisible = ref(false)
const previewUrl = ref('')
const isImage = (url: string) => /\.(jpe?g|png|gif|webp|bmp|svg)(\?.*)?$/i.test(url || '')
const previewFile = (url: string) => { if (url) { previewUrl.value = url; previewVisible.value = true } }

interface Props {
  visible: boolean
  remittanceId: number
}

interface Emit {
  (e: 'update:visible', visible: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emit>()

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const loading = ref(false)
const remittance = ref<any>(null)

const loadDetail = async () => {
  if (props.remittanceId) {
    loading.value = true
    try {
      const response = await getPaymentRemittanceDetail(props.remittanceId)
      let data = response.data
      if (data?.code === 200) {
        remittance.value = data.data
      }
    } catch (error) {
      console.error('加载详情失败', error)
    } finally {
      loading.value = false
    }
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    loadDetail()
  }
})

const getStatusColor = (status: number | undefined) => {
  const colorMap: Record<number, string> = { 0: 'default', 1: 'processing', 2: 'success', 3: 'error' }
  return colorMap[status || 0] || 'default'
}

const getStatusText = (status: number | undefined) => {
  const textMap: Record<number, string> = { 0: '草稿', 1: '待审核', 2: '已审核', 3: '已驳回' }
  return textMap[status || 0] || '未知'
}
</script>
