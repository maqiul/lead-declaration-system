<template>
  <a-modal
    v-model:open="visible"
    :title="isEdit ? '编辑出款水单' : '创建出款水单'"
    width="800px"
    :confirm-loading="submitLoading"
    @cancel="handleCancel"
  >
    <template #footer>
      <a-button @click="handleCancel">取消</a-button>
      <a-button type="primary" :loading="submitLoading" @click="handleSubmit(false)">保存</a-button>
      <a-button type="primary" :loading="submitLoading" @click="handleSubmit(true)" style="background: #fa8c16; border-color: #fa8c16;">保存并提交审核</a-button>
    </template>
    <a-form :model="formData" layout="vertical" ref="formRef">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="收款人" name="payeeName" :rules="[{ required: true, message: '请输入收款人' }]">
            <a-input v-model:value="formData.payeeName" placeholder="请输入收款人" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="币种" name="currency">
            <a-select v-model:value="formData.currency" :options="currencyOptions" placeholder="请选择" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="出款日期" name="paymentDate" :rules="[{ required: true, message: '请选择出款日期' }]">
            <a-date-picker v-model:value="formData.paymentDate" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="出款金额" name="paymentAmount" :rules="[{ required: true, message: '请输入出款金额' }]">
            <a-input-number v-model:value="formData.paymentAmount" :min="0" :precision="2" placeholder="请输入出款金额" style="width: 100%" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="关联申报单">
        <div style="margin-bottom: 8px;">
          <a-select
            v-if="visible"
            :key="selectKey"
            v-model:value="selectingFormId"
            show-search
            placeholder="搜索申报单号添加关联"
            :filter-option="false"
            :loading="declarationLoading"
            allow-clear
            @search="handleSearchDeclaration"
            @change="handleAddRelation"
            style="width: 100%"
          >
            <a-select-option v-for="item in declarationOptions" :key="item.id" :value="item.id" :disabled="relatedForms.some((r: any) => r.formId === item.id)">
              <div style="display: flex; justify-content: space-between;">
                <span style="font-weight: bold;">{{ item.formNo }}</span>
                <span>{{ item.shipperCompany }} | {{ item.totalAmount?.toFixed(2) }} {{ item.currency }}</span>
              </div>
            </a-select-option>
          </a-select>
        </div>
        <a-table
          v-if="relatedForms.length > 0"
          :columns="relationColumns"
          :data-source="relatedForms"
          row-key="formId"
          size="small"
          :pagination="false"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'formNo'">
              <a-tag color="blue">{{ record.formNo }}</a-tag>
            </template>
            <template v-if="column.key === 'totalAmount'">
              {{ record.totalAmount?.toFixed(2) }} {{ record.currency }}
            </template>
            <template v-if="column.key === 'relationAmount'">
              <a-input-number
                v-model:value="record.relationAmount"
                :min="0"
                :max="formData.paymentAmount || 999999999"
                :precision="2"
                placeholder="关联金额"
                size="small"
                style="width: 120px"
              />
            </template>
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" danger @click="handleRemoveRelation(index)">移除</a-button>
            </template>
          </template>
        </a-table>
        <div v-if="relatedForms.length > 0" style="margin-top: 8px; display: flex; justify-content: space-between;">
          <span style="color: #999; font-size: 12px;">已关联 {{ relatedForms.length }} 个申报单，分配金额合计: {{ totalRelationAmount.toFixed(2) }}</span>
          <span v-if="formData.paymentAmount && totalRelationAmount > formData.paymentAmount" style="color: #ff4d4f; font-size: 12px;">❗ 分配金额超过水单金额</span>
        </div>
      </a-form-item>

      <a-form-item label="备注">
        <a-textarea v-model:value="formData.remarks" placeholder="请输入备注" :rows="3" />
      </a-form-item>

      <a-form-item label="出款凭证" name="photoUrl" :rules="[{ required: true, message: '请上传出款凭证' }]">
        <a-upload
          :before-upload="beforeUpload"
          :file-list="fileList"
          accept="image/jpeg,image/jpg,image/png,image/gif,image/webp,image/bmp,application/pdf"
          @remove="handleRemove"
          :max-count="1"
        >
          <a-button v-if="!formData.photoUrl">
            <UploadOutlined />
            上传文件
          </a-button>
        </a-upload>
        <div v-if="formData.photoUrl" style="margin-top: 8px">
          <a-tag color="blue"><FileOutlined /> {{ getFileExtension(formData.photoUrl) }}</a-tag>
          <a-button type="link" size="small" @click="previewFile">查看文件</a-button>
        </div>
      </a-form-item>
    </a-form>
  </a-modal>

  <FilePreviewModal v-model:visible="previewVisible" :url="previewUrl" />
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined, FileOutlined } from '@ant-design/icons-vue'
import { createPaymentRemittance, updatePaymentRemittance, submitPaymentRemittanceAudit, relateToForm, unrelateFromForm, getRelatedForms } from '@/api/business/paymentRemittance'
import { uploadFile, getDeclarationList } from '@/api/business/declaration'
import { getEnabledCurrencies } from '@/api/system/currency'
import type { PaymentRemittance } from '@/api/business/paymentRemittance'
import FilePreviewModal from '@/components/FilePreviewModal.vue'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'

interface Props {
  visible: boolean
  remittanceData: Partial<PaymentRemittance>
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

const isEdit = computed(() => !!props.remittanceData?.id)
const formRef = ref()
const submitLoading = ref(false)
const fileList = ref<any[]>([])

const declarationLoading = ref(false)
const declarationOptions = ref<any[]>([])
const existingRelations = ref<any[]>([])
const selectingFormId = ref<number | undefined>(undefined)
const selectKey = ref(0)
const relatedForms = ref<any[]>([])

const relationColumns = [
  { title: '申报单号', dataIndex: 'formNo', key: 'formNo', width: 150 },
  { title: '申报单金额', key: 'totalAmount', width: 150 },
  { title: '关联金额', key: 'relationAmount', width: 150 },
  { title: '操作', key: 'action', width: 80 }
]

const totalRelationAmount = computed(() => relatedForms.value.reduce((sum, item) => sum + (item.relationAmount || 0), 0))

const handleAddRelation = (formId: any) => {
  if (!formId) return
  const form = declarationOptions.value.find((item: any) => item.id === formId)
  if (!form) return
  if (relatedForms.value.some((r: any) => r.formId === formId)) return

  const remittanceTotal = Number(formData.paymentAmount) || 0
  const alreadyRelated = relatedForms.value.reduce((sum: number, it: any) => sum + (Number(it.relationAmount) || 0), 0)
  const declTotal = Number(form.totalAmount) || 0

  let defaultAmount: number
  if (remittanceTotal <= 0) {
    defaultAmount = declTotal
  } else {
    const remaining = Math.max(0, remittanceTotal - alreadyRelated)
    defaultAmount = declTotal > remaining ? remaining : declTotal
  }

  relatedForms.value.push({
    formId: form.id,
    formNo: form.formNo,
    totalAmount: form.totalAmount,
    currency: form.currency,
    shipperCompany: form.shipperCompany,
    relationAmount: defaultAmount,
    isNew: true
  })
  selectingFormId.value = undefined
}

const handleRemoveRelation = (index: number) => { relatedForms.value.splice(index, 1) }

const currencyOptions = ref<any[]>([])
const defaultCurrency = ref('USD')
const loadCurrencies = async () => {
  try {
    const response = await getEnabledCurrencies()
    if (response.data.code === 200 && response.data.data.length > 0) {
      currencyOptions.value = response.data.data.map((item: any) => ({
        label: `${item.currencyCode} - ${item.chineseName || item.currencyName}`,
        value: item.currencyCode
      }))
      defaultCurrency.value = currencyOptions.value[0]?.value || 'USD'
    }
  } catch (error) {
    console.warn('加载货币数据失败:', error)
  }
}

const formData = reactive({
  id: undefined as number | undefined,
  payeeName: '',
  paymentDate: undefined as Dayjs | string | undefined,
  paymentAmount: undefined as number | undefined,
  currency: undefined as string | undefined,
  remarks: '',
  photoUrl: ''
})

const initForm = async () => {
  if (props.remittanceData && props.remittanceData.id) {
    Object.assign(formData, {
      id: props.remittanceData.id,
      payeeName: props.remittanceData.payeeName || '',
      paymentDate: props.remittanceData.paymentDate ? dayjs(props.remittanceData.paymentDate) : undefined,
      paymentAmount: props.remittanceData.paymentAmount,
      currency: props.remittanceData.currency || defaultCurrency.value,
      remarks: props.remittanceData.remarks || '',
      photoUrl: props.remittanceData.photoUrl || ''
    })
    if (props.remittanceData.photoUrl) {
      fileList.value = [{ uid: '-1', name: `凭证.${getFileExtension(props.remittanceData.photoUrl).toLowerCase()}`, url: props.remittanceData.photoUrl }]
    }
    await loadRelatedForms(props.remittanceData.id)
  } else {
    Object.assign(formData, { id: undefined, payeeName: '', paymentDate: undefined, paymentAmount: undefined, currency: defaultCurrency.value, remarks: '', photoUrl: '' })
    fileList.value = []
    relatedForms.value = []
  }
  handleSearchDeclaration('')
}

const loadRelatedForms = async (remittanceId: number) => {
  try {
    const response = await getRelatedForms(remittanceId)
    let data = response.data
    if (data?.code === 200) {
      const forms = data.data || []
      existingRelations.value = forms.map((item: any) => ({ formId: item.formId, relationAmount: item.relationAmount }))
      relatedForms.value = forms.map((item: any) => ({
        formId: item.formId, formNo: item.formNo, totalAmount: item.totalAmount, currency: item.currency, relationAmount: item.relationAmount, isNew: false
      }))
    }
  } catch (error) { console.error('加载关联申报单失败', error) }
}

watch(() => props.visible, (val) => {
  if (val) { selectingFormId.value = undefined; declarationOptions.value = []; selectKey.value++; loadCurrencies(); initForm() }
  else { selectingFormId.value = undefined }
})

const handleSearchDeclaration = async (value: string) => {
  declarationLoading.value = true
  try {
    const response = await getDeclarationList({ current: 1, size: 20, formNo: value || undefined, minStatus: 2 } as any)
    let data = response.data
    if (data?.code === 200) { declarationOptions.value = data.data?.records || data.data || [] }
    else if (Array.isArray(data)) { declarationOptions.value = data }
  } catch (error) { console.error('搜索申报单失败', error) }
  finally { declarationLoading.value = false }
}

const beforeUpload = async (file: File) => {
  const ext = (file.name.split('.').pop() || '').toLowerCase()
  const allowedExts = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'pdf']
  if (!allowedExts.includes(ext)) { message.error('仅支持图片或 PDF 文件'); return false }
  if (file.size / 1024 / 1024 > 20) { message.error('文件大小不能超过 20MB'); return false }
  try {
    const response = await uploadFile(file, 'payment-remittance')
    let data = response.data
    if (data?.code === 200) {
      const url = data.data?.fileUrl || data.data?.url
      if (!url) { message.error('上传成功但未获取到文件地址'); return false }
      formData.photoUrl = url
      fileList.value = [{ uid: String(Date.now()), name: file.name, url }]
      message.success('上传成功')
    } else { message.error(data?.message || '上传失败') }
  } catch (error) { message.error('上传失败') }
  return false
}

const handleRemove = () => { formData.photoUrl = ''; fileList.value = [] }
const getFileExtension = (url: any) => { if (!url || typeof url !== 'string') return 'FILE'; const parts = url.split('.'); return parts[parts.length - 1]?.split('?')[0]?.toUpperCase() || 'FILE' }

const previewVisible = ref(false)
const previewUrl = ref('')
const previewFile = () => { if (formData.photoUrl) { previewUrl.value = formData.photoUrl; previewVisible.value = true } }

const handleSubmit = async (submitAudit: boolean = false) => {
  try { await formRef.value?.validateFields() } catch { return }

  submitLoading.value = true
  try {
    if (formData.paymentAmount && relatedForms.value.length > 0 && totalRelationAmount.value > formData.paymentAmount + 0.01) {
      message.warning(`分配金额合计 ${totalRelationAmount.value.toFixed(2)} 超过水单金额 ${formData.paymentAmount}`)
      submitLoading.value = false; return
    }

    const data = { ...formData, paymentDate: formData.paymentDate ? dayjs(formData.paymentDate).format('YYYY-MM-DD') : undefined }
    let remittanceId: number

    if (isEdit.value) {
      await updatePaymentRemittance(formData.id!, data)
      remittanceId = formData.id!
      message.success('更新成功')
    } else {
      const result = await createPaymentRemittance(data)
      let resultData = result.data
      remittanceId = resultData?.data?.id || resultData?.id
      message.success('创建成功')
    }

    if (remittanceId && relatedForms.value.length > 0) {
      try {
        const currentFormIds = relatedForms.value.map((r: any) => r.formId)
        const existingFormIds = existingRelations.value.map((r: any) => r.formId)
        for (const form of relatedForms.value) {
          if (!existingFormIds.includes(form.formId)) {
            await relateToForm(remittanceId, form.formId, form.relationAmount)
          } else {
            const existingRelation = existingRelations.value.find((r: any) => r.formId === form.formId)
            if (existingRelation && existingRelation.relationAmount !== form.relationAmount) {
              await unrelateFromForm(remittanceId, form.formId)
              await relateToForm(remittanceId, form.formId, form.relationAmount)
            }
          }
        }
        for (const existingFormId of existingFormIds) { if (!currentFormIds.includes(existingFormId)) { await unrelateFromForm(remittanceId, existingFormId) } }
      } catch (e) { message.warning('水单已保存，但关联关系更新失败') }
    }

    if (submitAudit && remittanceId) {
      try { await submitPaymentRemittanceAudit(remittanceId); message.success('已提交审核') }
      catch (e) { message.warning('水单已保存，但提交审核失败') }
    }

    visible.value = false; await nextTick(); emit('success')
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || (isEdit.value ? '更新失败' : '创建失败'))
  } finally { submitLoading.value = false }
}

const handleCancel = () => { visible.value = false }
</script>
