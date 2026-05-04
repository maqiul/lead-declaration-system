<template>
  <div class="material-template-management px-6 py-6 bg-slate-50 min-h-full">
    <!-- 说明条 -->
    <a-alert
      class="mb-4"
      message="申报资料项模板"
      description="配置初审通过后需要申报人提交的资料项。启用的项会在申报单进入『待资料提交』节点时自动同步到每张申报单，管理员也可在单据内单独调整。"
      type="info"
      show-icon
    />

    <!-- 操作按钮区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-space>
        <a-button type="primary" @click="openAddModal" v-permission="['system:material:template:add']" class="ui-btn-cta">
          <template #icon><plus-outlined /></template>
          新增资料项
        </a-button>
        <a-button @click="loadList" class="ui-btn-secondary">
          <template #icon><reload-outlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="ui-card" :bordered="false">
      <a-table
        :dataSource="list"
        :columns="columns"
        :loading="loading"
        :pagination="false"
        rowKey="id"
        class="ui-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'required'">
            <a-tag :color="record.required === 1 ? 'red' : 'default'" class="ui-tag">
              {{ record.required === 1 ? '必填' : '选填' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'enabled'">
            <a-tag :color="record.enabled === 1 ? 'success' : 'error'" class="ui-tag">
              {{ record.enabled === 1 ? '启用' : '停用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditModal(record as MaterialTemplate)" v-permission="['system:material:template:edit']" class="text-blue-600 font-medium">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-popconfirm
                :title="record.enabled === 1 ? '停用后新申报单将不再同步此项（已同步单据不受影响），确定继续吗？' : '启用后新申报单会自动同步此项，确定继续吗？'"
                @confirm="toggleEnabled(record as MaterialTemplate)"
              >
                <a-button type="link" size="small" :danger="record.enabled === 1" v-permission="['system:material:template:edit']" class="font-medium">
                  <template #icon>
                    <component :is="record.enabled === 1 ? 'StopOutlined' : 'CheckCircleOutlined'" />
                  </template>
                  {{ record.enabled === 1 ? '停用' : '启用' }}
                </a-button>
              </a-popconfirm>
              <a-popconfirm
                title="删除后不可恢复，且历史单据内已同步的项保持不变。确定删除？"
                @confirm="handleDelete(record as MaterialTemplate)"
              >
                <a-button type="link" size="small" danger v-permission="['system:material:template:delete']" class="font-medium">
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑资料项' : '新增资料项'"
      @ok="handleSave"
      @cancel="closeModal"
      :confirm-loading="saving"
      width="600px"
      destroyOnClose
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="编码" name="code">
              <a-input
                v-model:value="formData.code"
                placeholder="如 CONTRACT（全局唯一）"
                :maxlength="50"
                :disabled="!!editingId"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="名称" name="name">
              <a-input v-model:value="formData.name" placeholder="如 合同" :maxlength="100" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="是否必填" name="required">
              <a-radio-group v-model:value="formData.required" button-style="solid">
                <a-radio :value="1">必填</a-radio>
                <a-radio :value="0">选填</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序" name="sort">
              <a-input-number v-model:value="formData.sort" :min="0" :max="9999" class="w-full" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="说明 / 填报指引" name="remark">
          <a-textarea v-model:value="formData.remark" placeholder="对申报人展示的说明" :rows="3" :maxlength="500" />
        </a-form-item>

        <a-form-item label="结构化字段" name="formSchema">
          <div class="mb-2">
            <a-space wrap>
              <a-button size="small" type="dashed" @click="addSchemaField">
                <template #icon><PlusOutlined /></template>
                新增字段
              </a-button>
              <a-button size="small" @click="applyInvoicePreset">使用发票预设</a-button>
              <a-button size="small" danger ghost @click="clearSchema" v-if="schemaFields.length">
                清空
              </a-button>
              <a-tooltip title="在这里配置的字段，申报人提交资料时除上传附件外还需填写。key 为 amount/currency/invoiceNo/invoiceDate 时将存入固定列（方便汇总），其他 key 存入扩展字段。">
                <question-circle-outlined class="text-gray-400" />
              </a-tooltip>
            </a-space>
          </div>

          <div v-if="!schemaFields.length" class="text-gray-400 text-xs py-4 text-center border border-dashed rounded">
            暂未配置结构化字段。如是普通附件类资料（如合同/装箱单）留空即可；发票类可点“使用发票预设”。
          </div>

          <a-table
            v-else
            :dataSource="schemaFields"
            :pagination="false"
            size="small"
            rowKey="_rid"
            :columns="schemaColumns"
            bordered
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'key'">
                <a-input
                  v-model:value="record.key"
                  placeholder="如 amount / invoiceNo"
                  size="small"
                  :maxlength="50"
                />
                <div v-if="FIXED_KEY_SET.has(record.key)" class="text-xs text-blue-500">
                  固定列
                </div>
              </template>
              <template v-else-if="column.key === 'label'">
                <a-input
                  v-model:value="record.label"
                  placeholder="如 发票金额"
                  size="small"
                  :maxlength="50"
                />
              </template>
              <template v-else-if="column.key === 'type'">
                <a-select
                  v-model:value="record.type"
                  size="small"
                  :options="TYPE_OPTIONS"
                  style="width: 100%"
                  @change="onTypeChange(record as SchemaFieldRow)"
                />
              </template>
              <template v-else-if="column.key === 'required'">
                <a-switch v-model:checked="record.required" size="small" />
              </template>
              <template v-else-if="column.key === 'options'">
                <a-select
                  v-if="record.type === 'select'"
                  v-model:value="record.options"
                  mode="tags"
                  size="small"
                  style="width: 100%"
                  placeholder="回车添加选项"
                  :token-separators="[',', '，']"
                />
                <span v-else class="text-gray-300">—</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a-button
                    type="link"
                    size="small"
                    :disabled="index === 0"
                    @click="moveField(index as number, -1)"
                  >↑</a-button>
                  <a-button
                    type="link"
                    size="small"
                    :disabled="index === schemaFields.length - 1"
                    @click="moveField(index as number, 1)"
                  >↓</a-button>
                  <a-button type="link" size="small" danger @click="removeField(index as number)">
                    删除
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>

          <div class="text-xs text-gray-400 mt-2">
            提示：key 为 <code>amount</code>/<code>currency</code>/<code>invoiceNo</code>/<code>invoiceDate</code> 将存入实例表固定列（支持 SQL 汇总），其他 key 存入扩展字段 extra_data。
          </div>
        </a-form-item>

        <a-form-item label="状态" name="enabled">
          <a-radio-group v-model:value="formData.enabled" button-style="solid">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">停用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, QuestionCircleOutlined, EditOutlined, StopOutlined, CheckCircleOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import type { RuleObject } from 'ant-design-vue/es/form/interface'
import {
  getMaterialTemplateList,
  addMaterialTemplate,
  updateMaterialTemplate,
  deleteMaterialTemplate,
  INVOICE_SCHEMA_PRESET,
  type MaterialTemplate,
  type MaterialSchemaField
} from '@/api/system/materialTemplate'

interface SchemaFieldRow extends MaterialSchemaField {
  _rid: number
}

const FIXED_KEY_SET = new Set(['amount', 'currency', 'invoiceNo', 'invoiceDate'])
const TYPE_OPTIONS = [
  { label: '文本', value: 'text' },
  { label: '数字', value: 'number' },
  { label: '日期', value: 'date' },
  { label: '下拉', value: 'select' }
]

let _ridSeq = 0
const genRid = () => ++_ridSeq

const list = ref<MaterialTemplate[]>([])
const loading = ref(false)

const columns = [
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '编码', dataIndex: 'code', key: 'code', width: 200 },
  { title: '名称', dataIndex: 'name', key: 'name', width: 200 },
  { title: '必填', dataIndex: 'required', key: 'required', width: 100 },
  { title: '启用', dataIndex: 'enabled', key: 'enabled', width: 100 },
  { title: '说明', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', fixed: 'right' as const, width: 220 }
]

const modalVisible = ref(false)
const editingId = ref<number | string | null>(null)
const saving = ref(false)
const formRef = ref()

const defaultForm = (): MaterialTemplate => ({
  code: '',
  name: '',
  required: 1,
  sort: 0,
  remark: '',
  formSchema: '',
  enabled: 1
})

const formData = reactive<MaterialTemplate>(defaultForm())

// 可视化结构化字段表格数据
const schemaFields = ref<SchemaFieldRow[]>([])
const schemaColumns = [
  { title: '字段 key', key: 'key', width: 160 },
  { title: '显示名称', key: 'label', width: 160 },
  { title: '类型', key: 'type', width: 110 },
  { title: '必填', key: 'required', width: 70 },
  { title: '可选项 (仅 select)', key: 'options' },
  { title: '操作', key: 'action', width: 130 }
]

const schemaToRows = (schema?: string | null): SchemaFieldRow[] => {
  if (!schema) return []
  try {
    const arr = JSON.parse(schema)
    if (!Array.isArray(arr)) return []
    return arr.map((f: MaterialSchemaField) => ({
      _rid: genRid(),
      key: f.key || '',
      label: f.label || '',
      type: (f.type as any) || 'text',
      required: !!f.required,
      options: Array.isArray(f.options) ? [...f.options] : []
    }))
  } catch {
    return []
  }
}

const rowsToSchema = (rows: SchemaFieldRow[]): string => {
  const cleaned = rows
    .filter((r) => r.key && r.key.trim())
    .map((r) => {
      const out: MaterialSchemaField = {
        key: r.key.trim(),
        label: (r.label || r.key).trim(),
        type: r.type,
        required: !!r.required
      }
      if (r.type === 'select' && r.options && r.options.length) {
        out.options = r.options
      }
      return out
    })
  return cleaned.length ? JSON.stringify(cleaned) : ''
}

const addSchemaField = () => {
  schemaFields.value.push({
    _rid: genRid(),
    key: '',
    label: '',
    type: 'text',
    required: false,
    options: []
  })
}

const removeField = (i: number) => {
  schemaFields.value.splice(i, 1)
}

const moveField = (i: number, delta: number) => {
  const j = i + delta
  if (j < 0 || j >= schemaFields.value.length) return
  const tmp = schemaFields.value[i]
  schemaFields.value[i] = schemaFields.value[j]
  schemaFields.value[j] = tmp
}

const onTypeChange = (row: SchemaFieldRow) => {
  if (row.type !== 'select') row.options = []
}

const formRules: Record<string, RuleObject | RuleObject[]> = {
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const loadList = async () => {
  try {
    loading.value = true
    const response = await getMaterialTemplateList()
    if (response.data?.code === 200) {
      list.value = response.data.data || []
    }
  } catch (error) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

const openAddModal = () => {
  editingId.value = null
  Object.assign(formData, defaultForm())
  schemaFields.value = []
  modalVisible.value = true
}

const openEditModal = (record: MaterialTemplate) => {
  editingId.value = record.id ?? null
  Object.assign(formData, defaultForm(), record)
  schemaFields.value = schemaToRows(record.formSchema)
  modalVisible.value = true
}

const closeModal = () => {
  modalVisible.value = false
}

const handleSave = async () => {
  try {
    await formRef.value?.validate()

    // 校验结构化字段
    const keys = new Set<string>()
    for (const r of schemaFields.value) {
      if (!r.key || !r.key.trim()) {
        message.warning('结构化字段的 key 不能为空')
        return
      }
      if (keys.has(r.key.trim())) {
        message.warning(`字段 key “${r.key}”重复`)
        return
      }
      keys.add(r.key.trim())
      if (r.type === 'select' && (!r.options || !r.options.length)) {
        message.warning(`字段“${r.label || r.key}”为下拉类型，请至少配置一个选项`)
        return
      }
    }
    formData.formSchema = rowsToSchema(schemaFields.value)

    saving.value = true

    let response
    if (editingId.value) {
      response = await updateMaterialTemplate({ ...formData, id: editingId.value })
    } else {
      response = await addMaterialTemplate({ ...formData })
    }

    if (response.data?.code === 200) {
      message.success('操作成功')
      closeModal()
      loadList()
    } else {
      message.error(response.data?.message || '操作失败')
    }
  } catch (error: any) {
    if (error?.errorFields) return // 表单校验失败
    message.error('操作失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = async (record: MaterialTemplate) => {
  try {
    const response = await deleteMaterialTemplate(record.id!)
    if (response.data?.code === 200) {
      message.success('删除成功')
      loadList()
    }
  } catch (error) {
    message.error('删除失败')
  }
}

const toggleEnabled = async (record: MaterialTemplate) => {
  try {
    const target: MaterialTemplate = { ...record, enabled: record.enabled === 1 ? 0 : 1 }
    const response = await updateMaterialTemplate(target)
    if (response.data?.code === 200) {
      message.success('状态更新成功')
      loadList()
    }
  } catch (error) {
    message.error('操作失败')
  }
}

// 应用发票预设
const applyInvoicePreset = () => {
  schemaFields.value = INVOICE_SCHEMA_PRESET.map((f) => ({
    _rid: genRid(),
    key: f.key,
    label: f.label,
    type: f.type,
    required: !!f.required,
    options: Array.isArray(f.options) ? [...f.options] : []
  }))
}
const clearSchema = () => {
  schemaFields.value = []
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
/* 页面特有样式已由全局 index.less 覆盖 */
</style>
