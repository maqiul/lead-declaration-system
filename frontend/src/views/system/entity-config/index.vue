<template>
  <div class="entity-config-management px-6 py-6 bg-white min-h-full">
    <!-- 搜索区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-form :model="searchForm" layout="inline" class="flex flex-wrap gap-4">
        <a-form-item label="关键词">
          <a-input v-model:value="searchForm.keyword" placeholder="公司英文名/中文名" allow-clear class="ui-input" />
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
        <a-button type="primary" @click="openAddModal" v-permission="['system:entity-config:add']" class="ui-btn-cta">
          <template #icon><PlusOutlined /></template>
          新增主体
        </a-button>
        <a-button @click="loadEntityConfigList" class="ui-btn-secondary">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="ui-card" :bordered="false">
      <a-table
        :dataSource="entityConfigList"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1270 }"
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

          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditModal(record as EntityConfig)" v-permission="['system:entity-config:update']" class="font-medium text-blue-600">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button
                v-if="record.isDefault !== 1"
                type="link"
                size="small"
                @click="setDefault(record as EntityConfig)"
                v-permission="['system:entity-config:update']"
                class="font-medium text-blue-600"
              >
                <template #icon><StarOutlined /></template>
                设为默认
              </a-button>
              <a-popconfirm
                title="确定要切换状态吗？"
                @confirm="toggleStatus(record as EntityConfig)"
              >
                <a-button
                  type="link"
                  size="small"
                  :danger="record.status === 1"
                  v-permission="['system:entity-config:update']"
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
                <a-button type="link" size="small" danger v-permission="['system:entity-config:delete']" class="font-medium">
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
      :title="editingId ? '编辑主体配置' : '新增主体配置'"
      @ok="handleSave"
      @cancel="closeModal"
      :confirm-loading="saving"
      width="700px"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-divider orientation="left">基本信息</a-divider>

        <a-form-item label="公司英文名" name="entityName">
          <a-input v-model:value="formData.entityName" placeholder="如 NINGBO ZIYI TECHNOLOGY CO.,LTD" />
        </a-form-item>

        <a-form-item label="英文地址" name="entityAddress">
          <a-textarea v-model:value="formData.entityAddress" placeholder="英文地址" :rows="2" />
        </a-form-item>

        <a-form-item label="公司中文名" name="entityNameCn">
          <a-input v-model:value="formData.entityNameCn" placeholder="公司中文名" />
        </a-form-item>

        <a-form-item label="中文地址" name="entityAddressCn">
          <a-textarea v-model:value="formData.entityAddressCn" placeholder="中文地址" :rows="2" />
        </a-form-item>

        <a-form-item label="纳税人识别号" name="taxId">
          <a-input v-model:value="formData.taxId" placeholder="纳税人识别号" />
        </a-form-item>

        <a-form-item label="电话" name="phone">
          <a-input v-model:value="formData.phone" placeholder="电话" />
        </a-form-item>

        <a-form-item label="开户银行" name="bankAccount">
          <a-input v-model:value="formData.bankAccount" placeholder="开户银行" />
        </a-form-item>

        <a-divider orientation="left">模板配置（留空则使用系统默认）</a-divider>

        <a-form-item label="发票模板" name="invoiceTemplate">
          <a-input v-model:value="formData.invoiceTemplate" placeholder="发票模板文件名，如 temple.xlsx" />
        </a-form-item>

        <a-form-item label="装箱单模板" name="packingListTemplate">
          <a-input v-model:value="formData.packingListTemplate" placeholder="装箱单模板文件名" />
        </a-form-item>

        <a-form-item label="海关附件模板" name="fullDocumentsTemplate">
          <a-input v-model:value="formData.fullDocumentsTemplate" placeholder="海关附件模板文件名，如 alltemple_template.xlsx" />
        </a-form-item>

        <a-form-item label="提货单模板" name="pickupListTemplate">
          <a-input v-model:value="formData.pickupListTemplate" placeholder="提货单模板文件名" />
        </a-form-item>

        <a-form-item label="水单模板" name="remittanceTemplate">
          <a-input v-model:value="formData.remittanceTemplate" placeholder="水单模板文件名，如 remittance_template.xlsx" />
        </a-form-item>

        <a-divider orientation="left">其他</a-divider>

        <a-form-item label="排序" name="sort">
          <a-input-number v-model:value="formData.sort" :min="0" :max="999" placeholder="排序值" />
        </a-form-item>

        <a-form-item label="设为默认" name="isDefault">
          <a-switch v-model:checked="formData.isDefault" checked-children="是" un-checked-children="否" />
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
import { PlusOutlined, ReloadOutlined, SearchOutlined, EditOutlined, StarOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import {
  getEntityConfigList,
  addEntityConfig,
  updateEntityConfig,
  deleteEntityConfig,
  toggleEntityConfigStatus,
  setDefaultEntityConfig,
  type EntityConfig
} from '@/api/system/entityConfig'

// 搜索表单
const searchForm = reactive({
  keyword: '',
  status: undefined as number | undefined
})

// 表格数据
const entityConfigList = ref<EntityConfig[]>([])
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
    title: '公司英文名',
    dataIndex: 'entityName',
    key: 'entityName',
    width: 250,
    ellipsis: true
  },
  {
    title: '公司中文名',
    dataIndex: 'entityNameCn',
    key: 'entityNameCn',
    width: 180
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
    width: 240
  }
]

// 弹窗相关
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref()

// 表单数据
const formData = reactive({
  entityName: '',
  entityAddress: '',
  entityNameCn: '',
  entityAddressCn: '',
  taxId: '',
  phone: '',
  bankAccount: '',
  invoiceTemplate: '',
  packingListTemplate: '',
  fullDocumentsTemplate: '',
  pickupListTemplate: '',
  remittanceTemplate: '',
  isDefault: false,
  status: 1,
  sort: 0
})

// 表单验证规则
const formRules = {
  entityName: [{ required: true, message: '请输入公司英文名' }],
  entityAddress: [{ required: true, message: '请输入英文地址' }]
}

// 加载列表
const loadEntityConfigList = async () => {
  try {
    loading.value = true
    const response = await getEntityConfigList({
      current: pagination.current,
      size: pagination.pageSize,
      keyword: searchForm.keyword,
      status: searchForm.status
    })

    if (response.data?.code === 200) {
      entityConfigList.value = response.data.data.records || []
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
  loadEntityConfigList()
}

// 重置搜索
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = undefined
  pagination.current = 1
  loadEntityConfigList()
}

// 表格分页变化
const handleTableChange = (pag: TablePaginationConfig) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadEntityConfigList()
}

// 打开新增弹窗
const openAddModal = () => {
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

// 打开编辑弹窗
const openEditModal = (record: EntityConfig) => {
  editingId.value = record.id || null
  formData.entityName = record.entityName
  formData.entityAddress = record.entityAddress
  formData.entityNameCn = record.entityNameCn
  formData.entityAddressCn = record.entityAddressCn
  formData.taxId = record.taxId || ''
  formData.phone = record.phone || ''
  formData.bankAccount = record.bankAccount || ''
  formData.invoiceTemplate = record.invoiceTemplate || ''
  formData.packingListTemplate = record.packingListTemplate || ''
  formData.fullDocumentsTemplate = record.fullDocumentsTemplate || ''
  formData.pickupListTemplate = record.pickupListTemplate || ''
  formData.remittanceTemplate = record.remittanceTemplate || ''
  formData.isDefault = record.isDefault === 1
  formData.status = record.status
  formData.sort = record.sort
  modalVisible.value = true
}

// 关闭弹窗
const closeModal = () => {
  modalVisible.value = false
  resetForm()
}

// 重置表单
const resetForm = () => {
  formData.entityName = ''
  formData.entityAddress = ''
  formData.entityNameCn = ''
  formData.entityAddressCn = ''
  formData.taxId = ''
  formData.phone = ''
  formData.bankAccount = ''
  formData.invoiceTemplate = ''
  formData.packingListTemplate = ''
  formData.fullDocumentsTemplate = ''
  formData.pickupListTemplate = ''
  formData.remittanceTemplate = ''
  formData.isDefault = false
  formData.status = 1
  formData.sort = 0
  formRef.value?.resetFields()
}

// 保存
const handleSave = async () => {
  try {
    await formRef.value?.validate()

    saving.value = true

    const data: EntityConfig = {
      entityName: formData.entityName,
      entityAddress: formData.entityAddress,
      entityNameCn: formData.entityNameCn,
      entityAddressCn: formData.entityAddressCn,
      taxId: formData.taxId || '',
      phone: formData.phone || '',
      bankAccount: formData.bankAccount || '',
      invoiceTemplate: formData.invoiceTemplate || '',
      packingListTemplate: formData.packingListTemplate || '',
      fullDocumentsTemplate: formData.fullDocumentsTemplate || '',
      pickupListTemplate: formData.pickupListTemplate || '',
      remittanceTemplate: formData.remittanceTemplate || '',
      isDefault: formData.isDefault ? 1 : 0,
      status: formData.status,
      sort: formData.sort
    }

    let response
    if (editingId.value) {
      response = await updateEntityConfig(editingId.value, data)
    } else {
      response = await addEntityConfig(data)
    }

    if (response.data?.code === 200) {
      message.success(editingId.value ? '更新成功' : '新增成功')
      closeModal()
      loadEntityConfigList()
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
    const response = await deleteEntityConfig(id)
    if (response.data?.code === 200) {
      message.success('删除成功')
      loadEntityConfigList()
    } else {
      message.error(response.data?.message || '删除失败')
    }
  } catch (error) {
    message.error('删除失败')
  }
}

// 切换状态
const toggleStatus = async (record: EntityConfig) => {
  try {
    const newStatus = record.status === 1 ? 0 : 1
    const response = await toggleEntityConfigStatus(record.id!, newStatus)
    if (response.data?.code === 200) {
      message.success('操作成功')
      loadEntityConfigList()
    } else {
      message.error(response.data?.message || '操作失败')
    }
  } catch (error) {
    message.error('操作失败')
  }
}

// 设为默认
const setDefault = async (record: EntityConfig) => {
  try {
    const response = await setDefaultEntityConfig(record.id!)
    if (response.data?.code === 200) {
      message.success('设置成功')
      loadEntityConfigList()
    } else {
      message.error(response.data?.message || '设置失败')
    }
  } catch (error) {
    message.error('设置失败')
  }
}

onMounted(() => {
  loadEntityConfigList()
})
</script>

<style scoped>
/* 页面特有样式已由全局 index.less 覆盖 */
</style>
