<template>
  <div class="party-b-config-management px-6 py-6 bg-white min-h-full">
    <!-- 搜索区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-form :model="searchForm" layout="inline" class="flex flex-wrap gap-4">
        <a-form-item label="关键词">
          <a-input v-model:value="searchForm.keyword" placeholder="乙方公司名称" allow-clear class="ui-input" />
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
        <a-button type="primary" @click="openAddModal" v-permission="['party-b:config:add']" class="ui-btn-cta">
          <template #icon><PlusOutlined /></template>
          新增乙方
        </a-button>
        <a-button @click="loadPartyBList" class="ui-btn-secondary">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="ui-card" :bordered="false">
      <a-table
        :dataSource="partyBList"
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

          <template v-else-if="column.key === 'bankInfo'">
            {{ [record.bankName, record.bankAccount].filter(Boolean).join(' ') || '-' }}
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditModal(record as PartyBConfig)" v-permission="['party-b:config:update']" class="font-medium text-blue-600">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-popconfirm
                title="确定要删除吗？"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger v-permission="['party-b:config:delete']" class="font-medium">
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
      :title="editingId ? '编辑乙方配置' : '新增乙方配置'"
      @ok="handleSave"
      @cancel="closeModal"
      :confirm-loading="saving"
      width="640px"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="公司名称" name="partyBName">
          <a-input v-model:value="formData.partyBName" placeholder="如 宁波某某贸易有限公司" />
        </a-form-item>

        <a-form-item label="公司地址" name="partyBAddress">
          <a-textarea v-model:value="formData.partyBAddress" placeholder="公司地址" :rows="2" />
        </a-form-item>

        <a-form-item label="联系人" name="contactPerson">
          <a-input v-model:value="formData.contactPerson" placeholder="业务联系人" />
        </a-form-item>

        <a-form-item label="联系电话" name="contactPhone">
          <a-input v-model:value="formData.contactPhone" placeholder="联系电话" />
        </a-form-item>

        <a-form-item label="纳税人识别号" name="taxId">
          <a-input v-model:value="formData.taxId" placeholder="统一社会信用代码/纳税人识别号" />
        </a-form-item>

        <a-form-item label="开户银行" name="bankName">
          <a-input v-model:value="formData.bankName" placeholder="开户银行名称" />
        </a-form-item>

        <a-form-item label="银行账号" name="bankAccount">
          <a-input v-model:value="formData.bankAccount" placeholder="银行账号" />
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
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SearchOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import {
  getPartyBList,
  addPartyB,
  updatePartyB,
  deletePartyB,
  type PartyBConfig
} from '@/api/system/partyBConfig'
import { formatDate } from '@/utils/common'

// 搜索表单
const searchForm = reactive({
  keyword: ''
})

// 表格数据
const partyBList = ref<PartyBConfig[]>([])
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
    title: '公司名称',
    dataIndex: 'partyBName',
    key: 'partyBName',
    width: 220,
    ellipsis: true
  },
  {
    title: '公司地址',
    dataIndex: 'partyBAddress',
    key: 'partyBAddress',
    width: 240,
    ellipsis: true
  },
  {
    title: '联系人',
    dataIndex: 'contactPerson',
    key: 'contactPerson',
    width: 100
  },
  {
    title: '联系电话',
    dataIndex: 'contactPhone',
    key: 'contactPhone',
    width: 130
  },
  {
    title: '纳税人识别号',
    dataIndex: 'taxId',
    key: 'taxId',
    width: 180,
    ellipsis: true
  },
  {
    title: '开户行及账号',
    key: 'bankInfo',
    width: 240,
    ellipsis: true
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
    width: 180,
    customRender: ({ text }: any) => text ? formatDate(text, 'yyyy-MM-dd HH:mm:ss') : '-'
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
  partyBName: '',
  partyBAddress: '',
  contactPerson: '',
  contactPhone: '',
  taxId: '',
  bankName: '',
  bankAccount: '',
  status: 1,
  sort: 0
})

// 表单验证规则
const formRules = {
  partyBName: [{ required: true, message: '请输入乙方公司名称' }],
  partyBAddress: [{ required: true, message: '请输入乙方公司地址' }]
}

// 加载列表
const loadPartyBList = async () => {
  try {
    loading.value = true
    const response = await getPartyBList({
      current: pagination.current,
      size: pagination.pageSize,
      keyword: searchForm.keyword
    })

    if (response.data?.code === 200) {
      partyBList.value = response.data.data.records || []
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
  loadPartyBList()
}

// 重置搜索
const handleReset = () => {
  searchForm.keyword = ''
  pagination.current = 1
  loadPartyBList()
}

// 表格分页变化
const handleTableChange = (pag: TablePaginationConfig) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadPartyBList()
}

// 打开新增弹窗
const openAddModal = () => {
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

// 打开编辑弹窗
const openEditModal = (record: PartyBConfig) => {
  editingId.value = record.id || null
  formData.partyBName = record.partyBName
  formData.partyBAddress = record.partyBAddress
  formData.contactPerson = record.contactPerson || ''
  formData.contactPhone = record.contactPhone || ''
  formData.taxId = record.taxId || ''
  formData.bankName = record.bankName || ''
  formData.bankAccount = record.bankAccount || ''
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
  formData.partyBName = ''
  formData.partyBAddress = ''
  formData.contactPerson = ''
  formData.contactPhone = ''
  formData.taxId = ''
  formData.bankName = ''
  formData.bankAccount = ''
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

    const data: PartyBConfig = {
      partyBName: formData.partyBName,
      partyBAddress: formData.partyBAddress,
      contactPerson: formData.contactPerson,
      contactPhone: formData.contactPhone,
      taxId: formData.taxId,
      bankName: formData.bankName,
      bankAccount: formData.bankAccount,
      status: formData.status,
      sort: formData.sort
    }

    let response
    if (editingId.value) {
      response = await updatePartyB(editingId.value, data)
    } else {
      response = await addPartyB(data)
    }

    if (response.data?.code === 200) {
      message.success(editingId.value ? '更新成功' : '新增成功')
      closeModal()
      loadPartyBList()
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
    const response = await deletePartyB(id)
    if (response.data?.code === 200) {
      message.success('删除成功')
      loadPartyBList()
    } else {
      message.error(response.data?.message || '删除失败')
    }
  } catch (error) {
    message.error('删除失败')
  }
}

onMounted(() => {
  loadPartyBList()
})
</script>

<style scoped>
/* 页面特有样式已由全局样式覆盖 */
</style>
