<template>
  <a-modal
    v-model:open="visible"
    title="水单审核"
    width="800px"
    :confirm-loading="submitLoading"
    @ok="handleAuditSubmit"
    @cancel="handleCancel"
  >
    <div v-if="remittance">
      <!-- 水单基本信息 -->
      <a-card title="水单基本信息" size="small" style="margin-bottom: 16px">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="水单编号">{{ remittance.remittanceNo }}</a-descriptions-item>
          <a-descriptions-item label="收汇名称">{{ remittance.remittanceName }}</a-descriptions-item>
          <a-descriptions-item label="收汇日期">{{ remittance.remittanceDate }}</a-descriptions-item>
          <a-descriptions-item label="收汇金额">
            <span style="font-size: 16px; font-weight: bold; color: #1890ff">
              {{ remittance.remittanceAmount?.toFixed(2) }} {{ remittance.currency || 'USD' }}
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="水单文件">
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
      <a-card title="审核信息" size="small">
        <a-form :model="auditForm" layout="vertical">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="汇率" required>
                <a-input-number
                  v-model:value="auditForm.taxRate"
                  :min="0"
                  :max="99"
                  :precision="4"
                  :step="0.0001"
                  placeholder="请输入汇率，如 7.2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="汇款银行" required>
                <a-select
                  v-model:value="auditForm.bankAccountId"
                  placeholder="请选择银行"
                  show-search
                  :filter-option="filterBankOption"
                  @change="handleBankChange"
                >
                  <a-select-option v-for="bank in bankList" :key="bank.id" :value="bank.id">
                    {{ bank.bankName }} - {{ bank.accountName }} ({{ bank.currency }})
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="银行手续费率">
                <a-input-number
                  v-model:value="auditForm.bankFeeRate"
                  :min="0"
                  :max="100"
                  :precision="4"
                  style="width: 100%"
                  addon-after="%"
                  @change="recalcCreditedAmount"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="银行手续费">
                <a-input-number
                  v-model:value="auditForm.bankFee"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                  @change="recalcCreditedAmount"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="入账金额">
                <a-input-number
                  :value="auditForm.creditedAmount"
                  disabled
                  style="width: 100%"
                />
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
import { auditRemittance, getRelatedForms } from '@/api/business/remittance'
import { getEnabledBankAccounts } from '@/api/business/declaration'
import { getRemittanceDetail } from '@/api/business/remittance'
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

// 审核表单
const auditForm = reactive({
  taxRate: undefined as number | undefined,
  bankAccountId: undefined as number | undefined,
  bankFeeRate: undefined as number | undefined,
  bankFee: undefined as number | undefined,
  creditedAmount: undefined as number | undefined,
  auditRemark: undefined as string | undefined
})

// 初始化
const init = async () => {
  // 加载水单详情
  if (props.remittanceId) {
    try {
      const response = await getRemittanceDetail(props.remittanceId)
      let data = response.data
      if (data?.code === 200) {
        remittance.value = data.data
      }
    } catch (error) {
      message.error('加载水单详情失败')
    }
  }

  // 根据水单关联的申报单获取主体ID，按主体过滤银行
  let entityId: number | undefined
  if (props.remittanceId) {
    try {
      const res = await getRelatedForms(props.remittanceId)
      const forms = res.data?.data || []
      entityId = forms.length > 0 ? forms[0].entityId : undefined
    } catch {
      // ignore
    }
  }

  // 加载银行列表
  try {
    const response = await getEnabledBankAccounts(undefined, entityId || undefined)
    let data = response.data
    if (data?.code === 200) {
      bankList.value = data.data || []
    }
  } catch (error) {
    console.error('加载银行列表失败', error)
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    init()
    // 重置表单
    auditForm.taxRate = undefined
    auditForm.bankAccountId = undefined
    auditForm.bankFeeRate = undefined
    auditForm.bankFee = undefined
    auditForm.creditedAmount = undefined
    auditForm.auditRemark = undefined
  }
})

// 重新计算入账金额
const recalcCreditedAmount = () => {
  if (remittance.value) {
    const amount = remittance.value.remittanceAmount || 0
    const fee = auditForm.bankFee || 0
    auditForm.creditedAmount = parseFloat((amount - fee).toFixed(2))
  }
}

// 银行选择变更
const handleBankChange = (value: any) => {
  const bank = bankList.value.find(b => b.id === value)
  if (bank && remittance.value) {
    // 只设置手续费率，手续费由用户手动填写
    auditForm.bankFeeRate = bank.serviceFeeRate * 100
    // 不再自动计算 bankFee，让用户自己填写
  }
}

// 过滤银行选项
const filterBankOption = (input: string, option: any) => {
  return option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

// 提交审核
const handleAuditSubmit = async () => {
  if (!auditForm.taxRate) {
    message.warning('请填写汇率')
    return
  }
  if (!auditForm.bankAccountId) {
    message.warning('请选择汇款银行')
    return
  }

  submitLoading.value = true
  try {
    await auditRemittance(props.remittanceId, {
      approved: true,
      bankAccountId: auditForm.bankAccountId,
      taxRate: auditForm.taxRate,
      bankFee: auditForm.bankFee,
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
