<template>
  <a-modal
    v-model:open="visible"
    title="资料审核"
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
        message="请审阅申报人提交的全部资料，通过后流程将结束并进入财务环节；驳回后申报人将重新提交。"
      />

      <a-table
        :dataSource="items"
        :columns="columns"
        :pagination="false"
        rowKey="id"
        size="small"
        class="ui-table"
        :expandedRowKeys="expandedKeys"
        @expand="onExpand"
      >
        <template #expandedRowRender="{ record }">
          <a-descriptions
            v-if="parseSchema(record.formSchema).length"
            size="small"
            :column="2"
            bordered
          >
            <a-descriptions-item
              v-for="f in parseSchema(record.formSchema)"
              :key="f.key"
              :label="f.label"
            >
              <span v-if="formatFieldValue(record, f.key) !== ''">
                {{ formatFieldValue(record, f.key) }}
              </span>
              <span v-else class="text-red-400">未填写</span>
            </a-descriptions-item>
          </a-descriptions>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div class="flex items-center gap-2">
              <span class="font-medium">{{ record.name }}</span>
              <a-tag v-if="record.required === 1" color="red">必填</a-tag>
              <a-tag v-else color="default">选填</a-tag>
              <a-tag v-if="record.templateId == null" color="blue">自定义</a-tag>
              <a-tag v-if="parseSchema(record.formSchema).length" color="purple">含结构化字段</a-tag>
            </div>
            <div v-if="record.remark" class="text-xs text-gray-400 mt-1">{{ record.remark }}</div>
          </template>
          <template v-else-if="column.key === 'file'">
            <template v-if="record.status === 1 && record.fileUrl">
              <a :href="record.fileUrl" target="_blank">{{ record.fileName || '查看附件' }}</a>
            </template>
            <template v-else>
              <span class="text-red-500">未上传</span>
            </template>
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
import {
  getMaterialItems,
  auditMaterial,
  type MaterialItem
} from '@/api/business/materialItem'

interface SchemaField {
  key: string
  label: string
  type: 'text' | 'number' | 'date' | 'select'
  required?: boolean
  options?: string[]
}

const FIXED_KEYS = ['amount', 'currency', 'invoiceNo', 'invoiceDate']

const parseSchema = (schema?: string | null): SchemaField[] => {
  if (!schema) return []
  try {
    const arr = JSON.parse(schema)
    return Array.isArray(arr) ? (arr as SchemaField[]) : []
  } catch {
    return []
  }
}

const formatFieldValue = (record: MaterialItem, key: string): string => {
  let v: any
  if (FIXED_KEYS.includes(key)) {
    v = (record as any)[key]
  } else if (record.extraData) {
    try {
      v = JSON.parse(record.extraData)[key]
    } catch {
      v = undefined
    }
  }
  if (v == null || v === '') return ''
  return String(v)
}

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
const items = ref<MaterialItem[]>([])

const form = reactive({ remark: '' })

const expandedKeys = ref<(number | string)[]>([])

const onExpand = (expanded: boolean, record: MaterialItem) => {
  const key = record.id as any
  if (expanded) {
    if (!expandedKeys.value.includes(key)) expandedKeys.value.push(key)
  } else {
    expandedKeys.value = expandedKeys.value.filter((k) => k !== key)
  }
}

watch(
  () => props.open,
  (v) => {
    visible.value = v
    if (v && props.formId) {
      form.remark = ''
      loadItems()
    }
  }
)
watch(visible, (v) => {
  if (!v) emit('update:open', false)
})

const columns = [
  { title: '资料项', key: 'name', dataIndex: 'name' },
  { title: '附件', key: 'file', dataIndex: 'fileName' }
]

const loadItems = async () => {
  if (!props.formId) return
  try {
    loading.value = true
    const res = await getMaterialItems(props.formId)
    if (res.data?.code === 200) {
      items.value = (res.data.data || []).slice().sort(
        (a: MaterialItem, b: MaterialItem) => (a.sort ?? 0) - (b.sort ?? 0)
      )
      expandedKeys.value = items.value
        .filter((i) => parseSchema(i.formSchema).length > 0)
        .map((i) => i.id as any)
    }
  } catch (e) {
    message.error('加载资料项失败')
  } finally {
    loading.value = false
  }
}

const handleAudit = (result: 1 | 2) => {
  if (result === 2 && !form.remark.trim()) {
    message.warning('驳回请填写审核意见')
    return
  }
  Modal.confirm({
    title: result === 1 ? '确认通过资料审核吗？' : '确认驳回资料审核吗？',
    content: result === 1 ? '通过后流程将结束，财务可以进入开票流程。' : '驳回后申报人将重新提交资料。',
    onOk: async () => {
      try {
        if (result === 1) approving.value = true
        else rejecting.value = true
        const res = await auditMaterial({
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
