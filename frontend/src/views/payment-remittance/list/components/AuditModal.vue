<template>
  <a-modal
    v-model:open="visible"
    title="出款水单审核"
    width="800px"
    :confirm-loading="submitLoading"
    @ok="handleAuditSubmit"
    @cancel="handleCancel"
  >
    <div v-if="remittance">
      <!-- 出款水单基本信息 -->
      <a-card title="出款水单基本信息" size="small" style="margin-bottom: 16px">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="出款编号">{{ remittance.paymentNo }}</a-descriptions-item>
          <a-descriptions-item label="收款人">{{ remittance.payeeName }}</a-descriptions-item>
          <a-descriptions-item label="出款日期">{{ remittance.paymentDate }}</a-descriptions-item>
          <a-descriptions-item label="出款金额">
            <span style="font-size: 16px; font-weight: bold; color: #1890ff">
              {{ remittance.paymentAmount?.toFixed(2) }} {{ remittance.currency || 'USD' }}
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="出款凭证">
            <template v-if="remittance.photoUrl">
              <a-image v-if="isImage(remittance.photoUrl)" :src="remittance.photoUrl" style="width: 100px; height: 60px" />
              <a-button v-else type="link" size="small" @click="previewFile(remittance.photoUrl)">
                <FilePdfOutlined /> 查看文件
              </a-button>
            </template>
            <span v-else>无文件</span>
          </a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- 审核信息 -->
      <a-card :key="'audit-' + modalKey" title="审核信息" size="small">
        <a-form :model="auditForm" layout="vertical">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="汇款银行" required>
                <a-select
                  v-model:value="auditForm.bankAccountId"
                  placeholder="请选择银行"
                  show-search
                  allow-clear
                  :filter-option="filterBankOption"
                  @change="handleBankChange"
                >
                  <a-select-option v-for="bank in bankList" :key="bank.id" :value="Number(bank.id)">
                    {{ bank.bankName }} - {{ bank.accountName }} ({{ bank.currency }})
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item label="审核备注">
            <a-textarea
              v-model:value="auditForm.auditRemark"
              placeholder="请输入审核备注(驳回时必填)"
              :rows="3"
            />
          </a-form-item>
        </a-form>
      </a-card>
    </div>
  </a-modal>

  <!-- 文件预览弹窗 -->
  <FilePreviewModal v-model:visible="previewVisible" :url="previewUrl" />
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { FilePdfOutlined } from '@ant-design/icons-vue'
import { auditPaymentRemittance, getRelatedForms, getPaymentRemittanceDetail } from '@/api/business/paymentRemittance'
import { getEnabledBankAccounts } from '@/api/business/declaration'
import FilePreviewModal from '@/components/FilePreviewModal.vue'

// 文件预览
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
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emit>()

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const submitLoading = ref(false)
const remittance = ref<any>(null)
const bankList = ref<any[]>([])
const initLoading = ref(false)

const modalKey = ref(0)

// 审核表单
const auditForm = reactive({
  bankAccountId: undefined as number | undefined,
  auditRemark: '' as string
})

// 初始化
const init = async () => {
  if (initLoading.value) return
  initLoading.value = true
  try {
  // 先重置表单
  remittance.value = null
  bankList.value = []
  modalKey.value++
  auditForm.bankAccountId = undefined
  auditForm.auditRemark = ''

  // 加载出款水单详情
  if (props.remittanceId) {
    try {
      const response = await getPaymentRemittanceDetail(props.remittanceId)
      let data = response.data
      if (data?.code === 200) {
        remittance.value = data.data
      }
    } catch (error) {
      message.error('加载出款水单详情失败')
    }
  }

  // 根据出款水单关联的申报单获取主体ID，按主体过滤银行
  let entityId: number | undefined
  const currency = remittance.value?.currency || undefined
  if (props.remittanceId) {
    try {
      const res = await getRelatedForms(props.remittanceId)
      const forms = res.data?.data || []
      if (forms.length > 0 && forms[0].entityId) {
        entityId = forms[0].entityId
      }
    } catch {
      // ignore
    }
  }

  // 加载银行列表：有主体按主体+币种，没有主体只按币种
  try {
    const response = entityId
      ? await getEnabledBankAccounts(currency || undefined, entityId)
      : await getEnabledBankAccounts(currency || undefined)
    let data = response.data
    if (data?.code === 200) {
      bankList.value = data.data || []
    }
  } catch (error) {
    console.error('加载银行列表失败', error)
  }

  // 回显已有审核数据
  if (remittance.value) {
    const r = remittance.value
    const bankId = Number(r.bankAccountId)
    if (bankId && bankId !== 0) auditForm.bankAccountId = bankId
    if (r.auditRemark) auditForm.auditRemark = r.auditRemark
  }
  } finally {
    initLoading.value = false
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    init()
  }
})

const handleBankChange = (value: any) => {
  if (value == null || value === 0 || value === '' || value === '0') {
    auditForm.bankAccountId = undefined
    return
  }
  auditForm.bankAccountId = Number(value)
}

// 过滤银行选项
const filterBankOption = (input: string, option: any) => {
  return option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

// 提交审核
const handleAuditSubmit = async () => {
  if (!auditForm.bankAccountId) {
    message.warning('请选择汇款银行')
    return
  }

  submitLoading.value = true
  try {
    await auditPaymentRemittance(props.remittanceId, {
      approved: true,
      bankAccountId: auditForm.bankAccountId,
      auditRemark: auditForm.auditRemark
    })
    message.success('审核成功')
    emit('success')
    visible.value = false
  } catch (error) {
    message.error('审核失败')
  } finally {
    submitLoading.value = false
  }
}

// 取消
const handleCancel = () => {
  visible.value = false
}
</script>
