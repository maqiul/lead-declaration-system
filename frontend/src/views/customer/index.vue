<template>
  <div class="customer-config-management px-6 py-6 bg-white min-h-full">
    <!-- 搜索区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-form :model="searchForm" layout="inline" class="flex flex-wrap gap-4">
        <a-form-item label="关键词">
          <a-input v-model:value="searchForm.keyword" placeholder="收货人公司名" allow-clear class="ui-input" />
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
        <a-button type="primary" @click="openAddModal" v-permission="['customer:config:add']" class="ui-btn-cta">
          <template #icon><PlusOutlined /></template>
          新增客户
        </a-button>
        <a-button @click="loadCustomerList" class="ui-btn-secondary">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="ui-card" :bordered="false">
      <a-table
        :dataSource="customerList"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1000 }"
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
              <a-button type="link" size="small" @click="openEditModal(record as CustomerConfig)" v-permission="['customer:config:update']" class="font-medium text-blue-600">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-popconfirm
                title="确定要删除吗？"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger v-permission="['customer:config:delete']" class="font-medium">
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
      :title="editingId ? '编辑常用客户' : '新增常用客户'"
      @ok="handleSave"
      @cancel="closeModal"
      :confirm-loading="saving"
      width="600px"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 17 }"
      >
        <a-form-item label="收货人公司名" name="customerName">
          <a-input v-model:value="formData.customerName" placeholder="如 ABC TRADING CO.,LTD" />
        </a-form-item>

        <a-form-item label="收货人地址" name="customerAddress">
          <a-textarea v-model:value="formData.customerAddress" placeholder="收货人地址" :rows="2" />
        </a-form-item>

        <a-form-item label="目的国" name="destinationCountry">
          <a-select
            v-model:value="formData.destinationCountry"
            :options="countryOptions"
            placeholder="请选择目的国"
            show-search
            option-filter-prop="label"
            allow-clear
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="贸易国" name="tradeCountry">
          <a-select
            v-model:value="formData.tradeCountry"
            :options="countryOptions"
            placeholder="请选择贸易国"
            show-search
            option-filter-prop="label"
            allow-clear
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="排序" name="sort">
          <a-input-number v-model:value="formData.sort" :min="0" :max="999" placeholder="排序值" />
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
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SearchOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import {
  getCustomerList,
  addCustomer,
  updateCustomer,
  deleteCustomer,
  type CustomerConfig
} from '@/api/system/customerConfig'
import { getEnabledCountries } from '@/api/system'

// 搜索表单
const searchForm = reactive({
  keyword: ''
})

// 表格数据
const customerList = ref<CustomerConfig[]>([])
const loading = ref(false)

// 国家选项（value 统一用英文全名，与申报记录的存储格式一致；申报表单内部会自行将英文名转为下拉 code）
const countryRawList = ref<any[]>([])
const countryOptions = computed(() =>
  countryRawList.value.map((c: any) => ({
    label: `${c.chineseName || ''} / ${c.englishName || ''}`,
    value: c.englishName || c.chineseName
  }))
)

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
    title: '收货人公司名',
    dataIndex: 'customerName',
    key: 'customerName',
    width: 250,
    ellipsis: true
  },
  {
    title: '收货人地址',
    dataIndex: 'customerAddress',
    key: 'customerAddress',
    width: 300,
    ellipsis: true
  },
  {
    title: '目的国',
    dataIndex: 'destinationCountry',
    key: 'destinationCountry',
    width: 120
  },
  {
    title: '贸易国',
    dataIndex: 'tradeCountry',
    key: 'tradeCountry',
    width: 120
  },
  {
    title: '状态',
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
    width: 150
  }
]

// 弹窗相关
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref()

// 表单数据
const formData = reactive({
  customerName: '',
  customerAddress: '',
  destinationCountry: undefined as string | undefined,
  tradeCountry: undefined as string | undefined,
  status: 1,
  sort: 0
})

// 表单验证规则
const formRules = {
  customerName: [{ required: true, message: '请输入收货人公司名' }],
  customerAddress: [{ required: true, message: '请输入收货人地址' }]
}

// 加载国家列表
const loadCountryList = async () => {
  try {
    const response = await getEnabledCountries()
    if (response.data?.code === 200) {
      countryRawList.value = response.data.data || []
    }
  } catch (error) {
    console.warn('加载国家列表失败', error)
  }
}

// 加载列表
const loadCustomerList = async () => {
  try {
    loading.value = true
    const response = await getCustomerList({
      current: pagination.current,
      size: pagination.pageSize,
      keyword: searchForm.keyword
    })

    if (response.data?.code === 200) {
      customerList.value = response.data.data.records || []
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
  loadCustomerList()
}

// 重置搜索
const handleReset = () => {
  searchForm.keyword = ''
  pagination.current = 1
  loadCustomerList()
}

// 表格分页变化
const handleTableChange = (pag: TablePaginationConfig) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadCustomerList()
}

// 打开新增弹窗
const openAddModal = () => {
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

// 打开编辑弹窗
const openEditModal = (record: CustomerConfig) => {
  editingId.value = record.id || null
  formData.customerName = record.customerName
  formData.customerAddress = record.customerAddress
  formData.destinationCountry = record.destinationCountry || undefined
  formData.tradeCountry = record.tradeCountry || undefined
  formData.status = record.status ?? 1
  formData.sort = record.sort ?? 0
  formRef.value?.clearValidate()
  modalVisible.value = true
}

// 关闭弹窗
const closeModal = () => {
  modalVisible.value = false
  resetForm()
}

// 重置表单
const resetForm = () => {
  formData.customerName = ''
  formData.customerAddress = ''
  formData.destinationCountry = undefined
  formData.tradeCountry = undefined
  formData.status = 1
  formData.sort = 0
  // 仅清除校验提示；不用 resetFields()，否则会把表单还原到“首次挂载时捕获的初始值”导致数据清不掉
  formRef.value?.clearValidate()
}

// 保存
const handleSave = async () => {
  try {
    await formRef.value?.validate()

    saving.value = true

    const data: CustomerConfig = {
      customerName: formData.customerName,
      customerAddress: formData.customerAddress,
      destinationCountry: formData.destinationCountry,
      tradeCountry: formData.tradeCountry,
      status: formData.status,
      sort: formData.sort
    }

    let response
    if (editingId.value) {
      response = await updateCustomer(editingId.value, data)
    } else {
      response = await addCustomer(data)
    }

    if (response.data?.code === 200) {
      message.success(editingId.value ? '更新成功' : '新增成功')
      closeModal()
      loadCustomerList()
    } else {
      message.error(response.data?.message || (editingId.value ? '更新失败' : '新增失败'))
    }
  } catch (error) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 删除
const handleDelete = async (id: number) => {
  try {
    const response = await deleteCustomer(id)
    if (response.data?.code === 200) {
      message.success('删除成功')
      loadCustomerList()
    } else {
      message.error(response.data?.message || '删除失败')
    }
  } catch (error) {
    message.error('删除失败')
  }
}

onMounted(() => {
  loadCountryList()
  loadCustomerList()
})
</script>

<style scoped>
/* 页面特有样式已由全局样式覆盖 */
</style>
