<template>
  <div class="bank-account-management px-6 py-6 bg-white min-h-full">
    <!-- 搜索区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-form :model="searchForm" layout="inline" class="flex flex-wrap gap-4">
        <a-form-item label="关键词">
          <a-input v-model:value="searchForm.keyword" placeholder="账户名称/银行名称/账户持有人/账号" allow-clear class="ui-input" />
        </a-form-item>
        <a-form-item label="币种">
          <a-select v-model:value="searchForm.currency" placeholder="请选择币种" allowClear style="width: 140px" class="ui-select">
            <a-select-option value="USD">USD</a-select-option>
            <a-select-option value="EUR">EUR</a-select-option>
            <a-select-option value="CNY">CNY</a-select-option>
            <a-select-option value="GBP">GBP</a-select-option>
            <a-select-option value="JPY">JPY</a-select-option>
          </a-select>
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
        <a-button type="primary" @click="openAddModal" v-permission="['system:bank-account:add']" class="ui-btn-cta">
          <template #icon><PlusOutlined /></template>
          新增银行账户
        </a-button>
        <a-button @click="loadBankAccountList" class="ui-btn-secondary">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="ui-card" :bordered="false">
      <a-table
        :dataSource="bankAccountList"
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
          
          <template v-else-if="column.key === 'isDefault'">
            <a-tag v-if="record.isDefault === 1" color="blue" class="ui-tag">默认</a-tag>
            <span v-else>-</span>
          </template>
          
          <template v-else-if="column.key === 'accountNumber'">
            <span v-if="record.accountNumber">{{ maskAccountNumber(record.accountNumber) }}</span>
            <span v-else>-</span>
          </template>
          
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditModal(record as BankAccountConfig)" v-permission="['system:bank-account:update']" class="font-medium text-blue-600">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button 
                v-if="record.isDefault !== 1" 
                type="link" 
                size="small" 
                @click="setDefault(record as BankAccountConfig)"
                v-permission="['system:bank-account:update']"
                class="font-medium text-blue-600"
              >
                <template #icon><StarOutlined /></template>
                设为默认
              </a-button>
              <a-popconfirm
                title="确定要切换状态吗？"
                @confirm="toggleStatus(record as BankAccountConfig)"
              >
                <a-button 
                  type="link" 
                  size="small" 
                  :danger="record.status === 1"
                  v-permission="['system:bank-account:update']"
                  class="font-medium"
                >
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
                <a-button type="link" size="small" danger v-permission="['system:bank-account:delete']" class="font-medium">
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
      :title="editingId ? '编辑银行账户' : '新增银行账户'"
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
        <a-form-item label="账户名称" name="accountName">
          <a-input
            v-model:value="formData.accountName"
            placeholder="请输入账户名称"
          />
        </a-form-item>

        <a-form-item label="银行名称" name="bankName">
          <a-input
            v-model:value="formData.bankName"
            placeholder="请输入银行名称"
          />
        </a-form-item>

        <a-form-item label="银行代码" name="bankCode">
          <a-input
            v-model:value="formData.bankCode"
            placeholder="请输入银行代码"
            :maxlength="20"
          />
        </a-form-item>

        <a-form-item label="银行账号" name="accountNumber">
          <a-input
            v-model:value="formData.accountNumber"
            placeholder="请输入银行账号"
          />
        </a-form-item>

        <a-form-item label="SWIFT代码" name="swiftCode">
          <a-input
            v-model:value="formData.swiftCode"
            placeholder="请输入SWIFT代码"
            :maxlength="20"
          />
        </a-form-item>

        <a-form-item label="IBAN号码" name="iban">
          <a-input
            v-model:value="formData.iban"
            placeholder="请输入IBAN号码"
            :maxlength="50"
          />
        </a-form-item>

        <a-form-item label="账户持有人" name="accountHolder">
          <a-input
            v-model:value="formData.accountHolder"
            placeholder="请输入账户持有人"
          />
        </a-form-item>

        <a-form-item label="账户币种" name="currency">
          <a-select v-model:value="formData.currency" placeholder="请选择币种">
            <a-select-option value="USD">USD - 美元</a-select-option>
            <a-select-option value="EUR">EUR - 欧元</a-select-option>
            <a-select-option value="CNY">CNY - 人民币</a-select-option>
            <a-select-option value="GBP">GBP - 英镑</a-select-option>
            <a-select-option value="JPY">JPY - 日元</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="支行名称" name="branchName">
          <a-input
            v-model:value="formData.branchName"
            placeholder="请输入支行名称"
          />
        </a-form-item>

        <a-form-item label="支行地址" name="branchAddress">
          <a-textarea
            v-model:value="formData.branchAddress"
            placeholder="请输入支行地址"
            :rows="3"
          />
        </a-form-item>

        <a-form-item label="手续费率" name="serviceFeeRate">
          <a-input-number
            v-model:value="formData.serviceFeeRate"
            :min="0"
            :max="1"
            :step="0.001"
            :precision="4"
            placeholder="如 0.001 表示千分之一"
            style="width: 100%"
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

        <a-form-item label="设为默认" name="isDefault">
          <a-switch 
            v-model:checked="formData.isDefault" 
            checked-children="是" 
            un-checked-children="否" 
          />
        </a-form-item>

        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="备注" name="remarks">
          <a-textarea
            v-model:value="formData.remarks"
            placeholder="请输入备注"
            :rows="3"
            :maxlength="500"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SearchOutlined, EditOutlined, StarOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import request from '@/utils/request'

// API接口定义
interface BankAccountConfig {
  id?: number
  accountName: string
  bankName: string
  bankCode: string
  accountNumber: string
  swiftCode: string
  iban: string
  accountHolder: string
  currency: string
  branchName: string
  branchAddress: string
  isDefault: number
  status: number
  sort: number
  serviceFeeRate: number
  remarks: string
  createTime?: string
}

// API接口函数
const getBankAccounts = (params: any) => {
  return request({
    url: '/v1/bank-accounts',
    method: 'get',
    params
  })
}

const addBankAccount = (data: any) => {
  return request({
    url: '/v1/bank-accounts',
    method: 'post',
    data
  })
}

const updateBankAccount = (id: number, data: any) => {
  return request({
    url: `/v1/bank-accounts/${id}`,
    method: 'put',
    data
  })
}

const deleteBankAccount = (id: number) => {
  return request({
    url: `/v1/bank-accounts/${id}`,
    method: 'delete'
  })
}

const toggleBankAccountStatus = (id: number, status: number) => {
  return request({
    url: `/v1/bank-accounts/${id}/toggle-status`,
    method: 'post',
    params: { status }
  })
}

const setDefaultBankAccount = (id: number) => {
  return request({
    url: `/v1/bank-accounts/${id}/set-default`,
    method: 'post'
  })
}

// 搜索表单
const searchForm = reactive({
  keyword: '',
  currency: undefined as string | undefined,
  status: undefined as number | undefined
})

// 表格数据
const bankAccountList = ref<BankAccountConfig[]>([])
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
    title: '账户名称',
    dataIndex: 'accountName',
    key: 'accountName',
    width: 150
  },
  {
    title: '银行名称',
    dataIndex: 'bankName',
    key: 'bankName',
    width: 120
  },
  {
    title: '银行账号',
    dataIndex: 'accountNumber',
    key: 'accountNumber',
    width: 180
  },
  {
    title: '账户持有人',
    dataIndex: 'accountHolder',
    key: 'accountHolder',
    width: 150
  },
  {
    title: '币种',
    dataIndex: 'currency',
    key: 'currency',
    width: 80
  },
  {
    title: 'SWIFT',
    dataIndex: 'swiftCode',
    key: 'swiftCode',
    width: 120
  },
  {
    title: '默认',
    key: 'isDefault',
    width: 80
  },
  {
    title: '状态',
    key: 'status',
    width: 80
  },
  {
    title: '手续费率',
    dataIndex: 'serviceFeeRate',
    key: 'serviceFeeRate',
    width: 100
  },
  {
    title: '排序',
    dataIndex: 'sort',
    key: 'sort',
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
    width: 220
  }
]

// 弹窗相关
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref()

// 表单数据
const formData = reactive({
  accountName: '',
  bankName: '',
  bankCode: '',
  accountNumber: '',
  swiftCode: '',
  iban: '',
  accountHolder: '',
  currency: 'USD',
  branchName: '',
  branchAddress: '',
  isDefault: false,
  status: 1,
  sort: 0,
  serviceFeeRate: 0,
  remarks: ''
})

// 表单验证规则
const formRules = {
  accountName: [
    { required: true, message: '请输入账户名称' }
  ],
  bankName: [
    { required: true, message: '请输入银行名称' }
  ],
  accountNumber: [
    { required: true, message: '请输入银行账号' }
  ],
  accountHolder: [
    { required: true, message: '请输入账户持有人' }
  ],
  currency: [
    { required: true, message: '请选择币种' }
  ]
}

// 账号脱敏显示
const maskAccountNumber = (accountNumber: string) => {
  if (accountNumber.length <= 4) return accountNumber
  return accountNumber.substring(0, 4) + '****' + accountNumber.substring(accountNumber.length - 4)
}

// 加载银行账户列表
const loadBankAccountList = async () => {
  try {
    loading.value = true
    const response = await getBankAccounts({
      current: pagination.current,
      size: pagination.pageSize,
      keyword: searchForm.keyword,
      currency: searchForm.currency,
      status: searchForm.status
    })
    
    if (response.data?.code === 200) {
      bankAccountList.value = response.data.data.records || []
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
  loadBankAccountList()
}

// 重置搜索
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.currency = undefined
  searchForm.status = undefined
  pagination.current = 1
  loadBankAccountList()
}

// 表格分页变化
const handleTableChange = (pag: TablePaginationConfig) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadBankAccountList()
}

// 打开新增弹窗
const openAddModal = () => {
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

// 打开编辑弹窗
const openEditModal = (record: BankAccountConfig) => {
  editingId.value = record.id || null
  formData.accountName = record.accountName
  formData.bankName = record.bankName
  formData.bankCode = record.bankCode
  formData.accountNumber = record.accountNumber
  formData.swiftCode = record.swiftCode
  formData.iban = record.iban
  formData.accountHolder = record.accountHolder
  formData.currency = record.currency
  formData.branchName = record.branchName
  formData.branchAddress = record.branchAddress
  formData.isDefault = record.isDefault === 1
  formData.status = record.status
  formData.sort = record.sort
  formData.serviceFeeRate = record.serviceFeeRate || 0
  formData.remarks = record.remarks
  modalVisible.value = true
}

// 关闭弹窗
const closeModal = () => {
  modalVisible.value = false
  resetForm()
}

// 重置表单
const resetForm = () => {
  formData.accountName = ''
  formData.bankName = ''
  formData.bankCode = ''
  formData.accountNumber = ''
  formData.swiftCode = ''
  formData.iban = ''
  formData.accountHolder = ''
  formData.currency = 'USD'
  formData.branchName = ''
  formData.branchAddress = ''
  formData.isDefault = false
  formData.status = 1
  formData.sort = 0
  formData.serviceFeeRate = 0
  formData.remarks = ''
  formRef.value?.resetFields()
}

// 保存银行账户
const handleSave = async () => {
  try {
    await formRef.value?.validate()
    
    saving.value = true
    
    const data = {
      accountName: formData.accountName,
      bankName: formData.bankName,
      bankCode: formData.bankCode,
      accountNumber: formData.accountNumber,
      swiftCode: formData.swiftCode,
      iban: formData.iban,
      accountHolder: formData.accountHolder,
      currency: formData.currency,
      branchName: formData.branchName,
      branchAddress: formData.branchAddress,
      isDefault: formData.isDefault ? 1 : 0,
      status: formData.status,
      sort: formData.sort,
      serviceFeeRate: formData.serviceFeeRate,
      remarks: formData.remarks
    }
    
    let response
    if (editingId.value) {
      response = await updateBankAccount(editingId.value, data)
    } else {
      response = await addBankAccount(data)
    }
    
    if (response.data?.code === 200) {
      message.success(editingId.value ? '更新成功' : '新增成功')
      closeModal()
      loadBankAccountList()
    } else {
      message.error(response.data?.message || (editingId.value ? '更新失败' : '新增失败'))
    }
  } catch (error) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 删除银行账户
const handleDelete = async (id: number) => {
  try {
    const response = await deleteBankAccount(id)
    if (response.data?.code === 200) {
      message.success('删除成功')
      loadBankAccountList()
    } else {
      message.error(response.data?.message || '删除失败')
    }
  } catch (error) {
    message.error('删除失败')
  }
}

// 切换状态
const toggleStatus = async (record: BankAccountConfig) => {
  try {
    const newStatus = record.status === 1 ? 0 : 1
    const response = await toggleBankAccountStatus(record.id!, newStatus)
    if (response.data?.code === 200) {
      message.success('操作成功')
      loadBankAccountList()
    } else {
      message.error(response.data?.message || '操作失败')
    }
  } catch (error) {
    message.error('操作失败')
  }
}

// 设置默认账户
const setDefault = async (record: BankAccountConfig) => {
  try {
    const response = await setDefaultBankAccount(record.id!)
    if (response.data?.code === 200) {
      message.success('设置成功')
      loadBankAccountList()
    } else {
      message.error(response.data?.message || '设置失败')
    }
  } catch (error) {
    message.error('设置失败')
  }
}

onMounted(() => {
  loadBankAccountList()
})
</script>

<style scoped>
/* 页面特有样式已由全局 index.less 覆盖 */
</style>