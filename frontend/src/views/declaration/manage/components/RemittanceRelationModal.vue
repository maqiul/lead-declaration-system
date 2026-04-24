<template>
  <a-modal
    v-model:open="visible"
    :title="`关联水单 - ${formNo}`"
    width="1000px"
    :footer="null"
  >
    <a-card title="已关联的水单" size="small" style="margin-bottom: 16px">
      <template #extra>
        <a-button type="primary" size="small" @click="showAddRemittance">
          <template #icon><PlusOutlined /></template>
          添加关联
        </a-button>
      </template>
      <a-table
        :columns="remittanceColumns"
        :data-source="relatedRemittances"
        :loading="loading"
        row-key="id"
        size="small"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'remittanceAmount'">
            {{ record.remittanceAmount ? record.remittanceAmount.toFixed(2) : '-' }} {{ record.currency || 'USD' }}
          </template>
          <template v-if="column.key === 'relationAmount'">
            {{ record.relationAmount ? record.relationAmount.toFixed(2) : '-' }}
          </template>
          <template v-if="column.key === 'relationType'">
            <a-tag :color="record.relationType === 1 ? 'blue' : 'green'">
              {{ record.relationType === 1 ? '主关联' : '副关联' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" danger @click="handleUnrelate(record.id)">取消关联</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 添加关联表单 -->
    <a-card v-if="addFormVisible" title="添加关联水单" size="small">
      <a-form :model="addForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="16">
            <a-form-item label="选择水单" required>
              <a-select
                v-model:value="addForm.remittanceId"
                show-search
                placeholder="请输入水单编号搜索"
                :filter-option="false"
                :loading="remittanceLoading"
                @search="handleSearchRemittance"
              >
                <a-select-option v-for="item in remittanceOptions" :key="item.id" :value="item.id">
                  <div style="display: flex; justify-content: space-between;">
                    <span style="font-weight: bold;">{{ item.remittanceNo }}</span>
                    <span>{{ item.remittanceName }} | {{ item.remittanceAmount?.toFixed(2) }} {{ item.currency }}</span>
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
              <a-input-number v-model:value="addForm.relationAmount" :min="0" :precision="2" style="width: 100%" placeholder="留空则默认水单全额" />
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
import { getRemittancesByFormId, relateToForm, unrelateFromForm, getRemittanceList } from '@/api/business/remittance'

interface Props {
  visible: boolean
  formId: number
  formNo: string
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
const relatedRemittances = ref<any[]>([])
const addFormVisible = ref(false)
const relateLoading = ref(false)

// 水单选择相关
const remittanceLoading = ref(false)
const remittanceOptions = ref<any[]>([])
const addForm = reactive({
  remittanceId: undefined as number | undefined,
  relationType: 1,
  relationAmount: undefined as number | undefined
})

const remittanceColumns = [
  { title: '水单编号', dataIndex: 'remittanceNo', key: 'remittanceNo', width: 160 },
  { title: '收汇名称', dataIndex: 'remittanceName', key: 'remittanceName', width: 140 },
  { title: '收汇日期', dataIndex: 'remittanceDate', key: 'remittanceDate', width: 120 },
  { title: '收汇金额', dataIndex: 'remittanceAmount', key: 'remittanceAmount', width: 140 },
  { title: '关联金额', dataIndex: 'relationAmount', key: 'relationAmount', width: 120 },
  { title: '关联类型', dataIndex: 'relationType', key: 'relationType', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '银行名称', dataIndex: 'bankAccountName', key: 'bankAccountName', width: 150 },
  { title: '汇率', dataIndex: 'taxRate', key: 'taxRate', width: 100 },
  { title: '操作', key: 'action', width: 100 }
]

// 搜索水单
const handleSearchRemittance = async (value: string) => {
  remittanceLoading.value = true
  try {
    const response = await getRemittanceList({
      current: 1,
      size: 20,
      remittanceNo: value || undefined
    })
    let data = response.data
    if (data?.code === 200) {
      // 只显示已审核的水单可以关联
      remittanceOptions.value = (data.data?.records || data.data || []).filter((r: any) => r.status === 2)
    } else if (Array.isArray(data)) {
      remittanceOptions.value = data.filter((r: any) => r.status === 2)
    }
  } catch (error) {
    console.error('搜索水单失败', error)
  } finally {
    remittanceLoading.value = false
  }
}

const loadRelatedRemittances = async () => {
  if (props.formId) {
    loading.value = true
    try {
      const response = await getRemittancesByFormId(props.formId)
      let data = response.data
      if (data?.code === 200) {
        relatedRemittances.value = data.data || []
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
    loadRelatedRemittances()
    addFormVisible.value = false
    // 初始加载一些水单供选择
    handleSearchRemittance('')
  }
})

const showAddRemittance = () => {
  addForm.remittanceId = undefined
  addForm.relationType = 1
  addForm.relationAmount = undefined
  addFormVisible.value = true
}

const handleRelate = async () => {
  if (!addForm.remittanceId) {
    message.warning('请选择水单')
    return
  }

  relateLoading.value = true
  try {
    await relateToForm(addForm.remittanceId, props.formId, addForm.relationAmount, addForm.relationType)
    message.success('关联成功')
    addFormVisible.value = false
    loadRelatedRemittances()
    emit('success')
  } catch (error) {
    message.error('关联失败')
  } finally {
    relateLoading.value = false
  }
}

const handleUnrelate = (remittanceId: number) => {
  Modal.confirm({
    title: '确认取消关联',
    content: '确定要取消关联该水单吗?',
    onOk: async () => {
      try {
        await unrelateFromForm(remittanceId, props.formId)
        message.success('取消关联成功')
        loadRelatedRemittances()
        emit('success')
      } catch (error) {
        message.error('取消关联失败')
      }
    }
  })
}

// 获取状态颜色
const getStatusColor = (status: number | undefined) => {
  const colorMap: Record<number, string> = {
    0: 'default',
    1: 'processing',
    2: 'success',
    3: 'error'
  }
  return colorMap[status || 0] || 'default'
}

// 获取状态文本
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
