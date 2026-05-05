<template>
  <a-modal
    v-model:open="visible"
    title="水单详情"
    width="800px"
    :footer="null"
  >
    <a-spin :spinning="loading">
      <a-descriptions v-if="remittance" bordered :column="2">
        <a-descriptions-item label="水单编号">{{ remittance.remittanceNo }}</a-descriptions-item>
        <a-descriptions-item label="水单类型">
          <a-tag :color="remittance.remittanceType === 1 ? 'blue' : 'green'">
            {{ remittance.remittanceType === 1 ? '定金' : '尾款' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="收汇名称">{{ remittance.remittanceName }}</a-descriptions-item>
        <a-descriptions-item label="收汇日期">{{ remittance.remittanceDate }}</a-descriptions-item>
        <a-descriptions-item label="收汇金额">
          <span style="font-size: 16px; font-weight: bold; color: #1890ff">
            {{ remittance.remittanceAmount?.toFixed(2) }} {{ remittance.currency }}
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(remittance.status)">
            {{ getStatusText(remittance.status) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="汇率">{{ remittance.taxRate || '-' }}</a-descriptions-item>
        <a-descriptions-item label="银行名称">{{ remittance.bankAccountName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="手续费率">{{ remittance.bankFeeRate ? remittance.bankFeeRate + '%' : '-' }}</a-descriptions-item>
        <a-descriptions-item label="手续费">{{ remittance.bankFee ? remittance.bankFee.toFixed(2) : '-' }}</a-descriptions-item>
        <a-descriptions-item label="入账金额">{{ remittance.creditedAmount ? remittance.creditedAmount.toFixed(2) : '-' }}</a-descriptions-item>
        <a-descriptions-item label="提交时间">{{ remittance.submitTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="审核人">{{ remittance.auditByName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="审核时间">{{ remittance.auditTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="审核备注" :span="2">{{ remittance.auditRemark || '-' }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ remittance.remarks || '-' }}</a-descriptions-item>
        <a-descriptions-item label="水单文件" :span="2">
          <template v-if="remittance.photoUrl">
            <a-image v-if="isImage(remittance.photoUrl)" :src="remittance.photoUrl" style="max-width: 400px" />
            <a-button v-else type="link" @click="openFile(remittance.photoUrl)">
              <FilePdfOutlined /> 查看文件
            </a-button>
          </template>
          <span v-else>无文件</span>
        </a-descriptions-item>
      </a-descriptions>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { FilePdfOutlined } from '@ant-design/icons-vue'
import { getRemittanceDetail } from '@/api/business/remittance'

// 文件类型判断
const isImage = (url: string) => /\.(jpe?g|png|gif|webp|bmp|svg)(\?.*)?$/i.test(url || '')
const openFile = (url: string) => { if (url) window.open(url, '_blank') }

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
      const response = await getRemittanceDetail(props.remittanceId)
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
  const colorMap: Record<number, string> = {
    0: 'default',
    1: 'processing',
    2: 'success',
    3: 'error'
  }
  return colorMap[status || 0] || 'default'
}

const getStatusText = (status: number | undefined) => {
  const textMap: Record<number, string> = {
    0: '草稿',
    1: '待审核',
    2: '已审核',
    3: '已驳回'
  }
  return textMap[status || 0] || '未知'
}
</script>
