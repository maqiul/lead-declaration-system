<template>
  <a-modal
    v-model:open="visible"
    title="业务发票审核"
    :width="800"
    :footer="null"
    destroyOnClose
    @cancel="handleClose"
  >
    <a-spin :spinning="loading">
      <a-alert
        class="mb-3"
        type="info"
        show-icon
        message="请审阅申报人提交的全部业务发票，通过后流程将结束并进入财务环节；驳回后申报人将重新提交。"
      />

      <a-table
        :dataSource="invoices"
        :columns="columns"
        :pagination="false"
        rowKey="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'fileName'">
            <a v-if="record.fileUrl" :href="getFileUrl(record.fileUrl)" target="_blank">
              {{ record.fileName || '查看附件' }}
            </a>
            <span v-else class="text-red-500">无附件</span>
          </template>
          <template v-else-if="column.key === 'amount'">
            {{ record.amount ?? '-' }}
          </template>
          <template v-else-if="column.key === 'taxAmount'">
            {{ record.taxAmount ?? '-' }}
          </template>
        </template>
      </a-table>

      <a-form class="mt-4" layout="vertical" :model="form">
        <a-form-item label="审核意见">
          <a-textarea
            v-model:value="form.remark"
            :rows="3"
            :maxlength="500"
            placeholder="请输入审核意见（驳回时建议填写驳回原因）"
          />
        </a-form-item>
      </a-form>

      <div class="flex justify-end">
        <a-space>
          <a-button @click="handleClose">取消</a-button>
          <a-button danger :loading="rejecting" @click="handleAudit(2)">驳回</a-button>
          <a-button type="primary" :loading="approving" @click="handleAudit(1)">
            审核通过
          </a-button>
        </a-space>
      </div>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { getBusinessInvoices } from '@/api/business/declaration'
import { auditInvoice } from '@/api/business/materialItem'

const props = defineProps<{
  open: boolean
  formId: number | string | null
}>()

const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'audited'): void
}>()

const visible = ref(false)
const loading = ref(false)
const approving = ref(false)
const rejecting = ref(false)
const invoices = ref<any[]>([])

const form = reactive({ remark: '' })

const columns = [
  { title: '发票名称', key: 'invoiceName', dataIndex: 'invoiceName' },
  { title: '发票号', key: 'invoiceNo', dataIndex: 'invoiceNo' },
  { title: '金额', key: 'amount', dataIndex: 'amount' },
  { title: '税额', key: 'taxAmount', dataIndex: 'taxAmount' },
  { title: '附件', key: 'fileName', dataIndex: 'fileName' }
]

const getFileUrl = (url: string): string => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  const base = (import.meta as any).env?.VITE_API_BASE_URL || ''
  return base + url
}

watch(
  () => props.open,
  (v) => {
    visible.value = v
    if (v && props.formId) {
      form.remark = ''
      loadInvoices()
    }
  }
)
watch(visible, (v) => {
  if (!v) emit('update:open', false)
})

const loadInvoices = async () => {
  if (!props.formId) return
  try {
    loading.value = true
    const res = await getBusinessInvoices(Number(props.formId))
    if (res.data?.code === 200) {
      invoices.value = res.data.data || []
    }
  } catch (e) {
    message.error('加载业务发票失败')
  } finally {
    loading.value = false
  }
}

const handleAudit = (result: 1 | 2) => {
  if (result === 2 && !form.remark.trim()) {
    message.warning('驳回请填写审核意见')
    return
  }
  if (!invoices.value || invoices.value.length === 0) {
    message.warning('暂无业务发票，无法审核')
    return
  }
  Modal.confirm({
    title: result === 1 ? '确认通过发票审核吗？' : '确认驳回发票审核吗？',
    content: result === 1 ? '通过后流程将结束，可进入财务流程。' : '驳回后申报人将重新提交发票。',
    onOk: async () => {
      try {
        if (result === 1) approving.value = true
        else rejecting.value = true
        const res = await auditInvoice({
          formId: props.formId!,
          result,
          remark: form.remark
        })
        if (res.data?.code === 200) {
          message.success(result === 1 ? '审核通过' : '已驳回')
          emit('audited')
          handleClose()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e) {
        message.error('操作失败')
      } finally {
        approving.value = false
        rejecting.value = false
      }
    }
  })
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped></style>
