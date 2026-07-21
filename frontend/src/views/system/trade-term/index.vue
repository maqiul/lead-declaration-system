<template>
  <div class="trade-term-management px-6 py-6 bg-slate-50 min-h-full">
    <!-- 搜索区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-form :model="searchForm" layout="inline" class="flex flex-wrap gap-4">
        <a-form-item label="关键词">
          <a-input v-model:value="searchForm.keyword" placeholder="代码/英文名称/中文名称/分组" allow-clear class="ui-input" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchForm.status" placeholder="请选择状态" allow-clear style="width: 140px" class="ui-select">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch" class="ui-btn-primary">
              <template #icon><search-outlined /></template>
              查询
            </a-button>
            <a-button @click="handleReset" class="ui-btn-secondary">
              <template #icon><reload-outlined /></template>
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-space>
        <a-button type="primary" @click="openAddModal" v-permission="['system:tradeterm:create']" class="ui-btn-cta">
          <template #icon><plus-outlined /></template>
          新增贸易方式
        </a-button>
        <a-button @click="loadTradeTermList" class="ui-btn-secondary">
          <template #icon><reload-outlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="ui-card" :bordered="false">
      <a-table
        :dataSource="tradeTermList"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1400 }"
        @change="handleTableChange"
        rowKey="id"
        class="ui-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'success' : 'error'" class="ui-tag">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditModal(record as TradeTerm)" v-permission="['system:tradeterm:update']" class="text-blue-600 font-medium">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-popconfirm
                title="确定要切换状态吗？"
                @confirm="toggleStatus(record as TradeTerm)"
              >
                <a-button type="link" size="small" :danger="record.status === 1" class="font-medium">
                  <template #icon>
                    <component :is="record.status === 1 ? 'StopOutlined' : 'CheckCircleOutlined'" />
                  </template>
                  {{ record.status === 1 ? '禁用' : '启用' }}
                </a-button>
              </a-popconfirm>
              <a-popconfirm
                title="确定要删除吗？"
                @confirm="handleDelete(record.id!)"
              >
                <a-button type="link" size="small" danger v-permission="['system:tradeterm:delete']" class="font-medium">
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
      :title="editingId ? '编辑贸易方式' : '新增贸易方式'"
      @ok="handleSave"
      @cancel="closeModal"
      :confirm-loading="saving"
      width="650px"
      destroyOnClose
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="代码" name="code">
              <a-input v-model:value="formData.code" placeholder="如 FOB" :maxlength="20" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="英文名称" name="name">
              <a-input v-model:value="formData.name" placeholder="请输入英文名称" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="中文名称" name="chineseName">
              <a-input v-model:value="formData.chineseName" placeholder="请输入中文名称" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="分组" name="groupName">
              <a-select v-model:value="formData.groupName" placeholder="请选择分组" allow-clear>
                <a-select-option value="E组">E组</a-select-option>
                <a-select-option value="F组">F组</a-select-option>
                <a-select-option value="C组">C组</a-select-option>
                <a-select-option value="D组">D组</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="适用运输方式" name="transportScope">
              <a-select v-model:value="formData.transportScope" placeholder="请选择" allow-clear>
                <a-select-option value="任何运输方式">任何运输方式</a-select-option>
                <a-select-option value="仅海运/内河">仅海运/内河</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="排序" name="sort">
              <a-input-number v-model:value="formData.sort" :min="0" :max="999" class="w-full" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="关联运输方式" name="transportModes">
          <a-select
            v-model:value="formData.transportModes"
            mode="multiple"
            placeholder="请选择关联的运输方式"
            style="width: 100%"
            :options="transportModeOptions"
          />
        </a-form-item>

        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formData.description" placeholder="请输入描述" :rows="3" :maxlength="500" />
        </a-form-item>

        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formData.status" button-style="solid">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SearchOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import type { RuleObject } from 'ant-design-vue/es/form/interface'
import {
  getTradeTermList,
  addTradeTerm,
  updateTradeTerm,
  deleteTradeTerm,
  toggleTradeTermStatus,
  type TradeTerm
} from '@/api/system/tradeTerm'
import { getEnabledTransportModes } from '@/api/system/transportMode'

const searchForm = reactive({
  keyword: '',
  status: undefined as number | undefined
})

const tradeTermList = ref<TradeTerm[]>([])
const loading = ref(false)
const transportModeList = ref<any[]>([])

const transportModeOptions = computed(() =>
  transportModeList.value.map((item: any) => ({
    label: `${item.code} - ${item.chineseName || item.name}`,
    value: item.code
  }))
)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 70 },
  { title: '代码', dataIndex: 'code', key: 'code', width: 80 },
  { title: '英文名称', dataIndex: 'name', key: 'name', width: 180 },
  { title: '中文名称', dataIndex: 'chineseName', key: 'chineseName', width: 150 },
  { title: '分组', dataIndex: 'groupName', key: 'groupName', width: 80 },
  { title: '适用运输方式', dataIndex: 'transportScope', key: 'transportScope', width: 130 },
  { title: '关联运输方式', key: 'transportModes', width: 180,
    customRender: ({ record }: any) => {
      const modes = record.transportModes || []
      return modes.length > 0 ? modes.join(', ') : '-'
    }
  },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', fixed: 'right' as const, width: 200 }
]

const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref()

const formData = reactive({
  code: '',
  name: '',
  chineseName: '',
  transportScope: '',
  groupName: '',
  description: '',
  sort: 0,
  status: 1,
  transportModes: [] as string[]
})

const formRules: Record<string, RuleObject | RuleObject[]> = {
  code: [{ required: true, message: '请输入代码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入英文名称', trigger: 'blur' }],
  chineseName: [{ required: true, message: '请输入中文名称', trigger: 'blur' }]
}

const loadTradeTermList = async () => {
  try {
    loading.value = true
    const response = await getTradeTermList({
      page: pagination.current,
      size: pagination.pageSize,
      keyword: searchForm.keyword,
      status: searchForm.status
    })
    if (response.data?.code === 200) {
      tradeTermList.value = response.data.data.records || []
      pagination.total = response.data.data.total || 0
    }
  } catch (error) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadTradeTermList()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = undefined
  handleSearch()
}

const handleTableChange = (pag: TablePaginationConfig) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadTradeTermList()
}

const openAddModal = () => {
  editingId.value = null
  Object.assign(formData, {
    code: '', name: '', chineseName: '', transportScope: '', groupName: '', description: '', sort: 0, status: 1, transportModes: []
  })
  modalVisible.value = true
}

const openEditModal = (record: TradeTerm) => {
  editingId.value = record.id ? (typeof record.id === 'string' ? parseInt(record.id, 10) : record.id) : null
  Object.assign(formData, { ...record, transportModes: record.transportModes || [] })
  modalVisible.value = true
}

const closeModal = () => {
  modalVisible.value = false
}

const handleSave = async () => {
  try {
    await formRef.value?.validate()
    saving.value = true
    let response
    if (editingId.value) {
      const id = typeof editingId.value === 'string' ? parseInt(editingId.value, 10) : editingId.value
      response = await updateTradeTerm(id, formData as any)
    } else {
      response = await addTradeTerm(formData as any)
    }
    if (response.data?.code === 200) {
      message.success('操作成功')
      closeModal()
      loadTradeTermList()
    } else {
      message.error(response.data?.message || '操作失败')
    }
  } catch (error) {
    // 验证失败
  } finally {
    saving.value = false
  }
}

const handleDelete = async (id: number | string) => {
  try {
    const numericId = typeof id === 'string' ? parseInt(id, 10) : id
    const response = await deleteTradeTerm(numericId)
    if (response.data?.code === 200) {
      message.success('删除成功')
      loadTradeTermList()
    }
  } catch (error) {
    message.error('删除失败')
  }
}

const toggleStatus = async (record: TradeTerm) => {
  try {
    const id = record.id ? (typeof record.id === 'string' ? parseInt(record.id, 10) : record.id) : 0
    const response = await toggleTradeTermStatus(id)
    if (response.data?.code === 200) {
      message.success('状态更新成功')
      loadTradeTermList()
    }
  } catch (error) {
    message.error('操作失败')
  }
}

const loadTransportModes = async () => {
  try {
    const response = await getEnabledTransportModes()
    if (response.data?.code === 200) {
      transportModeList.value = response.data.data || []
    }
  } catch (error) {
    console.error('加载运输方式失败', error)
  }
}

onMounted(() => {
  loadTradeTermList()
  loadTransportModes()
})
</script>

<style scoped>
</style>
