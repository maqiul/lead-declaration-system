<template>
  <div class="currency-management px-6 py-6 bg-white min-h-full">
    <!-- 搜索区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-form :model="searchForm" layout="inline" class="flex flex-wrap gap-4">
        <a-form-item label="关键词">
          <a-input v-model:value="searchForm.keyword" placeholder="货币代码/中文名称/英文名称" allow-clear class="ui-input" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchForm.status" placeholder="请选择状态" allowClear style="width: 140px" class="ui-select">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch" class="ui-btn-primary">
              <template #icon><SearchOutlined /></template>
              查询
            </a-button>
            <a-button @click="handleReset" class="ui-btn-secondary">
              <template #icon><ReloadOutlined /></template>
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-space>
        <a-button type="primary" @click="openAddModal" v-permission="['system:currency:create']" class="ui-btn-cta">
          <template #icon><PlusOutlined /></template>
          新增货币
        </a-button>
        <a-button @click="loadCurrencyList" class="ui-btn-secondary">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="ui-card" :bordered="false">
      <a-table
        :dataSource="currencyList"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
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
              <a-button type="link" size="small" @click="openEditModal(record as CurrencyInfo)" v-permission="['system:currency:update']" class="font-medium text-blue-600">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-popconfirm
                title="确定要切换状态吗？"
                @confirm="toggleStatus(record as CurrencyInfo)"
              >
                <a-button type="link" size="small" :danger="record.status === 1" v-permission="['system:currency:update']" class="font-medium">
                  <template #icon>
                    <component :is="record.status === 1 ? 'StopOutlined' : 'CheckCircleOutlined'" />
                  </template>
                  {{ record.status === 1 ? '禁用' : '启用' }}
                </a-button>
              </a-popconfirm>
              <a-popconfirm
                title="确定要删除吗？"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger v-permission="['system:currency:delete']" class="font-medium">
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
      v-model:visible="modalVisible"
      :title="editingId ? '编辑货币信息' : '新增货币信息'"
      @ok="handleSave"
      @cancel="closeModal"
      :confirm-loading="saving"
      width="600px"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="货币代码" name="currencyCode">
          <a-input
            v-model:value="formData.currencyCode"
            placeholder="请输入货币代码(如: USD)"
            :maxlength="10"
          />
        </a-form-item>

        <a-form-item label="中文名称" name="chineseName">
          <a-input
            v-model:value="formData.chineseName"
            placeholder="请输入中文名称(如: 美元)"
          />
        </a-form-item>

        <a-form-item label="英文名称" name="currencyName">
          <a-input
            v-model:value="formData.currencyName"
            placeholder="请输入英文名称(如: US Dollar)"
          />
        </a-form-item>

        <a-form-item label="中文单位" name="unitCn">
          <a-input
            v-model:value="formData.unitCn"
            placeholder="请输入中文单位(如: 元)"
            :maxlength="10"
          />
        </a-form-item>

        <a-form-item label="货币符号" name="symbol">
          <a-input
            v-model:value="formData.symbol"
            placeholder="请输入货币符号(如: $)"
            :maxlength="10"
          />
        </a-form-item>

        <a-form-item label="排序" name="sort">
          <a-input-number
            v-model:value="formData.sort"
            :min="0"
            :max="999"
            placeholder="请输入排序值"
          />
        </a-form-item>

        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'

import { PlusOutlined, ReloadOutlined, SearchOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import {
  getCurrencyList,
  addCurrency,
  updateCurrency,
  deleteCurrency,
  toggleCurrencyStatus,
  type CurrencyInfo
} from '@/api/system/currency'

// 搜索表单
const searchForm = reactive({
  keyword: '',
  status: undefined as number | undefined
})

// 表格数据
const currencyList = ref<CurrencyInfo[]>([])
const loading = ref(false)

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条记录`
})

// 表格列配置
const columns = [
  {
    title: '序号',
    key: 'index',
    width: 60,
    customRender: ({ index }: any) => index + 1 + (pagination.current - 1) * pagination.pageSize
  },
  {
    title: '货币代码',
    dataIndex: 'currencyCode',
    key: 'currencyCode',
    width: 100
  },
  {
    title: '中文名称',
    dataIndex: 'chineseName',
    key: 'chineseName',
    width: 120
  },
  {
    title: '英文名称',
    dataIndex: 'currencyName',
    key: 'currencyName',
    width: 150
  },
  {
    title: '中文单位',
    dataIndex: 'unitCn',
    key: 'unitCn',
    width: 80
  },
  {
    title: '货币符号',
    dataIndex: 'symbol',
    key: 'symbol',
    width: 80
  },
  {
    title: '排序',
    dataIndex: 'sort',
    key: 'sort',
    width: 80
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 80
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right' as const,
    width: 200
  }
]

// 弹窗相关
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref()

// 表单数据
const formData = reactive({
  currencyCode: '',
  chineseName: '',
  currencyName: '',
  unitCn: '',
  symbol: '',
  sort: 0,
  status: 1
})

// 表单验证规则
const formRules = {
  currencyCode: [
    { required: true, message: '请输入货币代码' },
    { pattern: /^[A-Z]{3}$/, message: '请输入3位大写字母(ISO 4217标准)' }
  ],
  chineseName: [
    { required: true, message: '请输入中文名称' }
  ],
  unitCn: [
    { required: true, message: '请输入中文单位' }
  ]
}

// 加载货币列表
const loadCurrencyList = async () => {
  try {
    loading.value = true
    const response = await getCurrencyList({
      current: pagination.current,
      size: pagination.pageSize,
      keyword: searchForm.keyword,
      status: searchForm.status
    })
    
    if (response.data?.code === 200) {
      currencyList.value = response.data.data.records || []
      pagination.total = response.data.data.total || 0
    } else {
      message.error(response.data?.message || '加载失败')
    }
  } catch (error) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadCurrencyList()
}

// 重置搜索
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = undefined
  pagination.current = 1
  loadCurrencyList()
}

// 表格分页变化
const handleTableChange = (pag: TablePaginationConfig) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadCurrencyList()
}

// 打开新增弹窗
const openAddModal = () => {
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

// 打开编辑弹窗
const openEditModal = (record: CurrencyInfo) => {
  editingId.value = record.id || null
  formData.currencyCode = record.currencyCode
  formData.chineseName = record.chineseName
  formData.currencyName = record.currencyName
  formData.unitCn = record.unitCn
  formData.symbol = record.symbol
  formData.sort = record.sort
  formData.status = record.status
  modalVisible.value = true
}

// 关闭弹窗
const closeModal = () => {
  modalVisible.value = false
  resetForm()
}

// 重置表单
const resetForm = () => {
  formData.currencyCode = ''
  formData.chineseName = ''
  formData.currencyName = ''
  formData.unitCn = ''
  formData.symbol = ''
  formData.sort = 0
  formData.status = 1
  formRef.value?.resetFields()
}

// 保存货币信息
const handleSave = async () => {
  try {
    await formRef.value?.validate()
    
    saving.value = true
    
    const data = {
      currencyCode: formData.currencyCode,
      chineseName: formData.chineseName,
      currencyName: formData.currencyName,
      unitCn: formData.unitCn,
      symbol: formData.symbol,
      sort: formData.sort,
      status: formData.status
    }
    
    let response
    if (editingId.value) {
      response = await updateCurrency(editingId.value, data as CurrencyInfo)
    } else {
      response = await addCurrency(data as CurrencyInfo)
    }
    
    if (response.data?.code === 200) {
      message.success(editingId.value ? '更新成功' : '新增成功')
      closeModal()
      loadCurrencyList()
    } else {
      message.error(response.data?.message || (editingId.value ? '更新失败' : '新增失败'))
    }
  } catch (error) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 删除货币信息
const handleDelete = async (id: number) => {
  try {
    const response = await deleteCurrency(id)
    if (response.data?.code === 200) {
      message.success('删除成功')
      loadCurrencyList()
    } else {
      message.error(response.data?.message || '删除失败')
    }
  } catch (error) {
    message.error('删除失败')
  }
}

// 切换状态
const toggleStatus = async (record: CurrencyInfo) => {
  try {
    const response = await toggleCurrencyStatus(record.id!)
    if (response.data?.code === 200) {
      message.success('操作成功')
      loadCurrencyList()
    } else {
      message.error(response.data?.message || '操作失败')
    }
  } catch (error) {
    message.error('操作失败')
  }
}

onMounted(() => {
  loadCurrencyList()
})
</script>

<style scoped>
/* 页面特有样式已由全局 index.less 覆盖 */
</style>

