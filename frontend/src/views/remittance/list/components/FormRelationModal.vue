<template>
  <a-modal
    v-model:open="visible"
    title="管理关联申报单"
    width="900px"
    :footer="null"
  >
    <a-card title="已关联的申报单" size="small" style="margin-bottom: 16px">
      <template #extra>
        <a-space>
          <span style="font-size: 12px; color: #999;">
            水单金额: <b>{{ remittanceAmount?.toFixed(2) }}</b> |
            已关联: <b style="color: #fa8c16;">{{ totalRelatedAmount.toFixed(2) }}</b> |
            剩余: <b :style="{ color: remainingAmount > 0 ? '#52c41a' : '#ff4d4f' }">{{ remainingAmount.toFixed(2) }}</b>
          </span>
          <a-button type="primary" size="small" @click="showAddForm">
            <template #icon><PlusOutlined /></template>
            添加关联
          </a-button>
        </a-space>
      </template>
      <a-table
        :columns="formColumns"
        :data-source="relatedForms"
        :loading="loading"
        row-key="formId"
        size="small"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'relationType'">
            <a-tag :color="record.relationType === 1 ? 'blue' : 'green'">
              {{ record.relationType === 1 ? '主关联' : '副关联' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'formNo'">
            <a-tag color="blue">{{ record.formNo }}</a-tag>
          </template>
          <template v-if="column.key === 'formDate'">
            {{ record.formDate || '-' }}
          </template>
          <template v-if="column.key === 'totalAmount'">
            {{ record.totalAmount != null ? Number(record.totalAmount).toFixed(2) : '-' }} {{ record.currency || '' }}
          </template>
          <template v-if="column.key === 'relationAmount'">
            {{ record.relationAmount != null ? Number(record.relationAmount).toFixed(2) : '-' }} {{ record.currency || '' }}
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" danger @click="handleUnrelate(record.formId)">取消关联</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 添加关联表单 -->
    <a-card v-if="addFormVisible" title="添加关联申报单" size="small">
      <a-form :model="addForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="16">
            <a-form-item label="选择申报单" required>
              <a-select
                v-model:value="addForm.formId"
                show-search
                placeholder="请输入申报单号搜索"
                :filter-option="false"
                :loading="declarationLoading"
                @search="handleSearchDeclaration"
                @change="handleDeclarationChange"
              >
                <a-select-option v-for="item in declarationOptions" :key="item.id" :value="item.id">
                  <div style="display: flex; justify-content: space-between;">
                    <span style="font-weight: bold;">{{ item.formNo }}</span>
                    <span>{{ item.formDate }} | {{ item.totalAmount?.toFixed(2) }} {{ item.currency }}</span>
                  </div>
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="关联类型">
              <a-select v-model:value="addForm.relationType">
                <a-select-option :value="1">主关联</a-select-option>
                <a-select-option :value="2">副关联</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="关联金额">
              <a-input-number v-model:value="addForm.relationAmount" :min="0" :precision="2" style="width: 100%" placeholder="留空默认 min(申报金额, 水单剩余)" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleRelate" :loading="relateLoading">确认关联</a-button>
            <a-button @click="addFormVisible = false">取消</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { getRelatedForms, relateToForm, unrelateFromForm } from '@/api/business/remittance'
import { getDeclarationList } from '@/api/business/declaration'

interface Props {
  visible: boolean
  remittanceId: number
  remittanceAmount: number
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

const loading = ref(false)
const relatedForms = ref<any[]>([])
const addFormVisible = ref(false)
const relateLoading = ref(false)

// 申报单选择相关
const declarationLoading = ref(false)
const declarationOptions = ref<any[]>([])
const selectedDeclaration = ref<any>(null) // 当前选中的申报单（包含 totalAmount）
const addForm = reactive({
  formId: undefined as number | undefined,
  relationType: 1,
  relationAmount: undefined as number | undefined
})

const formColumns = [
  { title: '申报单号', dataIndex: 'formNo', key: 'formNo', width: 140 },
  { title: '申报日期', dataIndex: 'formDate', key: 'formDate', width: 120 },
  { title: '总金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120 },
  { title: '关联类型', dataIndex: 'relationType', key: 'relationType', width: 100 },
  { title: '关联金额', dataIndex: 'relationAmount', key: 'relationAmount', width: 120 },
  { title: '关联时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 100 }
]

// 搜索申报单
const handleSearchDeclaration = async (value: string) => {
  declarationLoading.value = true
  try {
    const response = await getDeclarationList({
      current: 1,
      size: 20,
      formNo: value || undefined,
      minStatus: 2 // 只查询已提交的申报单（status>=2，排除草稿/待初审）
    } as any)
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

// 选择申报单变更：自动将关联金额预填为 min(申报金额, 水单剩余)
const handleDeclarationChange = (value: any) => {
  selectedDeclaration.value = declarationOptions.value.find((it: any) => it.id === value) || null
  const declTotal = Number(selectedDeclaration.value?.totalAmount) || 0
  const remaining = remainingAmount.value
  addForm.relationAmount = declTotal > remaining ? remaining : declTotal
}

const loadRelatedForms = async () => {
  if (props.remittanceId) {
    loading.value = true
    try {
      const response = await getRelatedForms(props.remittanceId)
      let data = response.data
      if (data?.code === 200) {
        relatedForms.value = data.data || []
      }
    } catch (error) {
      console.error('加载关联数据失败', error)
    } finally {
      loading.value = false
    }
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    loadRelatedForms()
    addFormVisible.value = false
    // 初始加载一些申报单供选择
    handleSearchDeclaration('')
  }
})

const showAddForm = () => {
  addForm.formId = undefined
  addForm.relationType = 1
  addForm.relationAmount = undefined
  selectedDeclaration.value = null
  addFormVisible.value = true
}

// 已关联金额合计
const totalRelatedAmount = computed(() => {
  return relatedForms.value.reduce((sum: number, item: any) => sum + (Number(item.relationAmount) || 0), 0)
})

// 剩余可分配金额
const remainingAmount = computed(() => {
  return Math.max(0, (props.remittanceAmount || 0) - totalRelatedAmount.value)
})

const handleRelate = async () => {
  if (!addForm.formId) {
    message.warning('请选择申报单')
    return
  }

  // 关联金额为空时的默认值：min(申报单金额, 水单剩余可分配金额)
  let relationAmount = addForm.relationAmount
  if (relationAmount == null || relationAmount <= 0) {
    const declTotal = Number(selectedDeclaration.value?.totalAmount) || 0
    const remaining = remainingAmount.value
    relationAmount = declTotal > remaining ? remaining : declTotal
  }

  // 金额必须 >0，否则让用户手动填（避免后端报错）
  if (!relationAmount || relationAmount <= 0) {
    message.warning('关联金额无法自动确定（申报单金额或水单剩余为 0），请手动填写')
    return
  }

  // 前端校验：关联金额不能超过剩余可分配金额
  if (relationAmount > remainingAmount.value + 0.01) {
    message.warning(`关联金额超出水单可分配余额！水单金额: ${props.remittanceAmount}, 已关联: ${totalRelatedAmount.value.toFixed(2)}, 剩余: ${remainingAmount.value.toFixed(2)}`)
    return
  }

  relateLoading.value = true
  try {
    await relateToForm(props.remittanceId, addForm.formId, relationAmount, addForm.relationType)
    message.success('关联成功')
    addFormVisible.value = false
    loadRelatedForms()
    emit('success')
  } catch (error) {
    message.error('关联失败')
  } finally {
    relateLoading.value = false
  }
}

const handleUnrelate = (formId: number) => {
  Modal.confirm({
    title: '确认取消关联',
    content: '确定要取消关联该申报单吗?',
    onOk: async () => {
      try {
        await unrelateFromForm(props.remittanceId, formId)
        message.success('取消关联成功')
        loadRelatedForms()
        emit('success')
      } catch (error) {
        message.error('取消关联失败')
      }
    }
  })
}
</script>
