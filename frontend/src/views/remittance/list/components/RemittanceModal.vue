<template>
  <a-modal
    v-model:open="visible"
    :title="isEdit ? '编辑水单' : '创建水单'"
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
          <a-form-item label="币种" name="currency">
            <a-select v-model:value="formData.currency" placeholder="请选择">
              <a-select-option value="USD">USD - 美元</a-select-option>
              <a-select-option value="CNY">CNY - 人民币</a-select-option>
              <a-select-option value="EUR">EUR - 欧元</a-select-option>
              <a-select-option value="GBP">GBP - 英镑</a-select-option>
              <a-select-option value="JPY">JPY - 日元</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="收汇名称" name="remittanceName" :rules="[{ required: true, message: '请输入收汇名称' }]">
            <a-input v-model:value="formData.remittanceName" placeholder="请输入收汇名称" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="收汇日期" name="remittanceDate" :rules="[{ required: true, message: '请选择收汇日期' }]">
            <a-date-picker v-model:value="formData.remittanceDate" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="收汇金额" name="remittanceAmount" :rules="[{ required: true, message: '请输入收汇金额' }]">
            <a-input-number
              v-model:value="formData.remittanceAmount"
              :min="0"
              :precision="2"
              placeholder="请输入收汇金额"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="关联申报单">
        <div style="margin-bottom: 8px;">
          <a-select
            show-search
            placeholder="搜索申报单号添加关联"
            :filter-option="false"
            :loading="declarationLoading"
            @search="handleSearchDeclaration"
            @change="handleAddRelation"
            :value="undefined"
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
                :max="formData.remittanceAmount || 999999999"
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
          <span v-if="formData.remittanceAmount && totalRelationAmount > formData.remittanceAmount" style="color: #ff4d4f; font-size: 12px;">❗ 分配金额超过水单金额</span>
        </div>
        <div v-if="relatedForms.length === 0" style="color: #999; font-size: 12px; margin-top: 4px;">
          提示: 可在创建时直接关联申报单并设置分配金额
        </div>
      </a-form-item>

      <a-form-item label="备注">
        <a-textarea v-model:value="formData.remarks" placeholder="请输入备注" :rows="3" />
      </a-form-item>

      <a-form-item label="水单文件" name="photoUrl" :rules="[{ required: true, message: '请上传水单文件' }]">
        <a-upload
          :before-upload="beforeUpload"
          :file-list="fileList"
          @remove="handleRemove"
          :max-count="1"
        >
          <a-button v-if="!formData.photoUrl">
            <UploadOutlined />
            上传文件
          </a-button>
        </a-upload>
        <div v-if="formData.photoUrl" style="margin-top: 8px">
          <a-tag color="blue">
            <FileOutlined /> {{ getFileExtension(formData.photoUrl) }}
          </a-tag>
          <a-button type="link" size="small" @click="previewFile">
            查看文件
          </a-button>
        </div>
        <div v-if="!formData.photoUrl" style="margin-top: 8px; color: #ff4d4f; font-size: 12px;">
          * 水单文件为必填项，支持图片、PDF等格式
        </div>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined, FileOutlined } from '@ant-design/icons-vue'
import { createRemittance, updateRemittance, submitRemittanceAudit, relateToForm, unrelateFromForm, getRelatedForms } from '@/api/business/remittance'
import { uploadFile, getDeclarationList } from '@/api/business/declaration'
import type { Remittance } from '@/api/business/remittance'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'

interface Props {
  visible: boolean
  remittanceData: Partial<Remittance>
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

// 申报单选择相关
const declarationLoading = ref(false)
const declarationOptions = ref<any[]>([])
const existingRelations = ref<any[]>([]) // 已有关联关系（数据库中的）

// 关联申报单列表（包含金额）
const relatedForms = ref<any[]>([])

// 关联表格列定义
const relationColumns = [
  { title: '申报单号', dataIndex: 'formNo', key: 'formNo', width: 150 },
  { title: '申报单金额', key: 'totalAmount', width: 150 },
  { title: '关联金额', key: 'relationAmount', width: 150 },
  { title: '操作', key: 'action', width: 80 }
]

// 计算分配金额合计
const totalRelationAmount = computed(() => {
  return relatedForms.value.reduce((sum, item) => sum + (item.relationAmount || 0), 0)
})

// 添加关联申报单
const handleAddRelation = (formId: any) => {
  if (!formId) return
  const form = declarationOptions.value.find((item: any) => item.id === formId)
  if (form && !relatedForms.value.some((r: any) => r.formId === formId)) {
    relatedForms.value.push({
      formId: form.id,
      formNo: form.formNo,
      totalAmount: form.totalAmount,
      currency: form.currency,
      shipperCompany: form.shipperCompany,
      relationAmount: formData.remittanceAmount || undefined,
      isNew: true
    })
  }
}

// 移除关联
const handleRemoveRelation = (index: number) => {
  relatedForms.value.splice(index, 1)
}

// 表单数据
const formData = reactive({
  id: undefined as number | undefined,
  remittanceName: '',
  remittanceDate: undefined as Dayjs | string | undefined,
  remittanceAmount: undefined as number | undefined,
  currency: 'USD',
  remarks: '',
  photoUrl: ''
})

// 初始化表单
const initForm = async () => {
  if (props.remittanceData && props.remittanceData.id) {
    // 编辑模式
    Object.assign(formData, {
      id: props.remittanceData.id,
      remittanceName: props.remittanceData.remittanceName || '',
      remittanceDate: props.remittanceData.remittanceDate ? dayjs(props.remittanceData.remittanceDate) : undefined,
      remittanceAmount: props.remittanceData.remittanceAmount,
      currency: props.remittanceData.currency || 'USD',
      remarks: props.remittanceData.remarks || '',
      photoUrl: props.remittanceData.photoUrl || ''
    })
    if (props.remittanceData.photoUrl) {
      fileList.value = [{ uid: '-1', name: '水单照片', url: props.remittanceData.photoUrl }]
    }
    
    // 加载已关联的申报单
    await loadRelatedForms(props.remittanceData.id)
  } else {
    // 创建模式
    Object.assign(formData, {
      id: undefined,
      remittanceName: '',
      remittanceDate: undefined,
      remittanceAmount: undefined,
      currency: 'USD',
      remarks: '',
      photoUrl: ''
    })
    fileList.value = []
    relatedForms.value = []
  }
  
  // 初始加载一些申报单
  handleSearchDeclaration('')
}

// 加载已关联的申报单
const loadRelatedForms = async (remittanceId: number) => {
  try {
    const response = await getRelatedForms(remittanceId)
    let data = response.data
    if (data?.code === 200) {
      const forms = data.data || []
      existingRelations.value = forms.map((item: any) => ({ formId: item.formId, relationAmount: item.relationAmount }))
      relatedForms.value = forms.map((item: any) => ({
        formId: item.formId,
        formNo: item.formNo,
        totalAmount: item.totalAmount,
        currency: item.currency,
        relationAmount: item.relationAmount,
        isNew: false
      }))
    }
  } catch (error) {
    console.error('加载关联申报单失败', error)
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    initForm()
  }
})

// 搜索申报单
const handleSearchDeclaration = async (value: string) => {
  declarationLoading.value = true
  try {
    const response = await getDeclarationList({
      current: 1,
      size: 20,
      formNo: value || undefined,
      status: 2 // 只查询状态为已完成 的申报单
    })
    let data = response.data
    if (data?.code === 200) {
      declarationOptions.value = data.data?.records || data.data || []
    } else if (Array.isArray(data)) {
      declarationOptions.value = data
    }
  } catch (error) {
    console.error('搜索申报单失败', error)
  } finally {
    declarationLoading.value = false
  }
}

// 上传前处理
const beforeUpload = async (file: File) => {
  try {
    const response = await uploadFile(file, 'remittance')
    let data = response.data
    if (data?.code === 200) {
      formData.photoUrl = data.data?.url || data.data
      message.success('上传成功')
    } else {
      message.error('上传失败')
    }
  } catch (error) {
    message.error('上传失败')
  }
  return false // 阻止默认上传
}

// 移除文件
const handleRemove = () => {
  formData.photoUrl = ''
  fileList.value = []
}

// 获取文件扩展名
const getFileExtension = (url: any) => {
  if (!url || typeof url !== 'string') return 'FILE'
  const parts = url.split('.')
  const ext = parts[parts.length - 1]?.split('?')[0]?.toUpperCase()
  return ext || 'FILE'
}

// 预览文件
const previewFile = () => {
  if (formData.photoUrl) {
    window.open(formData.photoUrl, '_blank')
  }
}

// 提交表单
const handleSubmit = async (submitAudit: boolean = false) => {
  // 表单验证
  try {
    await formRef.value?.validateFields()
  } catch (validationError) {
    // 表单验证失败，不继续
    return
  }

  submitLoading.value = true
  try {
    // 前端校验：关联金额不能超过水单金额
    if (formData.remittanceAmount && relatedForms.value.length > 0 && totalRelationAmount.value > formData.remittanceAmount + 0.01) {
      message.warning(`分配金额合计 ${totalRelationAmount.value.toFixed(2)} 超过水单金额 ${formData.remittanceAmount}，请调整`)
      submitLoading.value = false
      return
    }

    const data = {
      ...formData,
      remittanceDate: formData.remittanceDate ? dayjs(formData.remittanceDate).format('YYYY-MM-DD') : undefined
    }

    let remittanceId: number
    
    if (isEdit.value) {
      await updateRemittance(formData.id!, data)
      remittanceId = formData.id!
      message.success('更新成功')
    } else {
      const result = await createRemittance(data)
      let resultData = result.data
      remittanceId = resultData?.data?.id || resultData?.id
      message.success('创建成功')
    }

    // 处理关联申报单（失败不影响主流程）
    if (remittanceId && relatedForms.value.length > 0) {
      try {
        const currentFormIds = relatedForms.value.map((r: any) => r.formId)
        const existingFormIds = existingRelations.value.map((r: any) => r.formId)
        
        // 添加新的关联（带金额）
        for (const form of relatedForms.value) {
          if (!existingFormIds.includes(form.formId)) {
            await relateToForm(remittanceId, form.formId, form.relationAmount)
          } else {
            // 已存在的关联，检查金额是否变化，如果变了就先取消再重新关联
            const existingRelation = existingRelations.value.find((r: any) => r.formId === form.formId)
            if (existingRelation && existingRelation.relationAmount !== form.relationAmount) {
              await unrelateFromForm(remittanceId, form.formId)
              await relateToForm(remittanceId, form.formId, form.relationAmount)
            }
          }
        }
        // 移除已取消的关联
        for (const existingFormId of existingFormIds) {
          if (!currentFormIds.includes(existingFormId)) {
            await unrelateFromForm(remittanceId, existingFormId)
          }
        }
      } catch (relationError) {
        console.error('关联关系更新失败', relationError)
        message.warning('水单已保存，但关联关系更新失败，请手动管理关联')
      }
    } else if (remittanceId && relatedForms.value.length === 0 && existingRelations.value.length > 0) {
      // 全部移除
      try {
        for (const r of existingRelations.value) {
          await unrelateFromForm(remittanceId, r.formId)
        }
      } catch (e) {
        console.error('取消关联失败', e)
      }
    }

    // 提交审核
    if (submitAudit && remittanceId) {
      try {
        await submitRemittanceAudit(remittanceId)
        message.success('已提交审核')
      } catch (submitError) {
        console.error('提交审核失败', submitError)
        message.warning('水单已保存，但提交审核失败，请手动提交')
      }
    }

    // 先关闭弹窗，再通知父组件刷新列表
    visible.value = false
    await nextTick()
    emit('success')
  } catch (error: any) {
    console.error('操作失败', error)
    message.error(error?.response?.data?.message || error?.message || (isEdit.value ? '更新失败' : '创建失败'))
  } finally {
    submitLoading.value = false
  }
}

// 取消
const handleCancel = () => {
  visible.value = false
}
</script>
