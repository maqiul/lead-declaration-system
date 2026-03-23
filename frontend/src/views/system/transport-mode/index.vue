<template>
  <div class="transport-mode-management">
    <!-- 搜索区域 -->
    <a-card class="search-card">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="关键词">
          <a-input v-model:value="searchForm.keyword" placeholder="代码/英文名称/中文名称" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchForm.status" placeholder="请选择状态" allowClear style="width: 100px">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSearch">搜索</a-button>
          <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮区域 -->
    <a-card class="operation-card">
      <a-space>
        <a-button type="primary" @click="openAddModal">
          <template #icon><PlusOutlined /></template>
          新增运输方式
        </a-button>
        <a-button @click="loadTransportModeList">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card>
      <a-table
        :dataSource="transportModeList"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        rowKey="id"
      />
    </a-card>

    <!-- 编辑弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="editingId ? '编辑运输方式' : '新增运输方式'"
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
        <a-form-item label="英文名称" name="name">
          <a-input
            v-model:value="formData.name"
            placeholder="请输入英文名称"
          />
        </a-form-item>

        <a-form-item label="中文名称" name="chineseName">
          <a-input
            v-model:value="formData.chineseName"
            placeholder="请输入中文名称"
          />
        </a-form-item>

        <a-form-item label="代码" name="code">
          <a-input
            v-model:value="formData.code"
            placeholder="请输入代码"
            :maxlength="20"
          />
        </a-form-item>

        <a-form-item label="描述" name="description">
          <a-textarea
            v-model:value="formData.description"
            placeholder="请输入描述"
            :rows="3"
            :maxlength="500"
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
import { ref, reactive, onMounted, h } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import {
  getTransportModeList,
  addTransportMode,
  updateTransportMode,
  deleteTransportMode,
  toggleTransportModeStatus,
  type TransportMode
} from '@/api/system/transportMode'

// 搜索表单
const searchForm = reactive({
  keyword: '',
  status: undefined as number | undefined
})

// 表格数据
const transportModeList = ref<TransportMode[]>([])
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
    title: '排序号',
    dataIndex: 'sort',
    key: 'sort',
    width: 80
  },
  {
    title: '英文名称',
    dataIndex: 'name',
    key: 'name',
    width: 150
  },
  {
    title: '中文名称',
    dataIndex: 'chineseName',
    key: 'chineseName',
    width: 150
  },
  {
    title: '代码',
    dataIndex: 'code',
    key: 'code',
    width: 100
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
    width: 200,
    ellipsis: true
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    customRender: ({ record }: { record: TransportMode }) => {
      return h('a-tag', { 
        color: record.status === 1 ? 'green' : 'red'
      }, record.status === 1 ? '启用' : '禁用')
    }
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
    width: 180,
    customRender: ({ record }: { record: TransportMode }) => {
      return h('a-space', {}, [
        h('a-button', {
          type: 'link',
          size: 'small',
          onClick: () => openEditModal(record)
        }, '编辑'),
        h('a-popconfirm', {
          title: '确定要切换状态吗？',
          onConfirm: () => toggleStatus(record)
        }, {
          default: () => h('a-button', {
            type: 'link',
            size: 'small',
            danger: record.status === 1
          }, record.status === 1 ? '禁用' : '启用')
        }),
        h('a-popconfirm', {
          title: '确定要删除吗？',
          onConfirm: () => handleDelete(record.id!)
        }, {
          default: () => h('a-button', {
            type: 'link',
            size: 'small',
            danger: true
          }, '删除')
        })
      ])
    }
  }
]

// 弹窗相关
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref()

// 表单数据
const formData = reactive({
  name: '',
  chineseName: '',
  code: '',
  description: '',
  sort: 0,
  status: 1
})

// 表单验证规则
const formRules = {
  name: [
    { required: true, message: '请输入英文名称' }
  ]
}

// 加载运输方式列表
const loadTransportModeList = async () => {
  try {
    loading.value = true
    const response = await getTransportModeList({
      page: pagination.current,
      size: pagination.pageSize,
      keyword: searchForm.keyword,
      status: searchForm.status
    })
    
    if (response.data?.code === 200) {
      transportModeList.value = response.data.data.records || []
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
  loadTransportModeList()
}

// 重置搜索
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = undefined
  pagination.current = 1
  loadTransportModeList()
}

// 表格分页变化
const handleTableChange = (pag: TablePaginationConfig) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadTransportModeList()
}

// 打开新增弹窗
const openAddModal = () => {
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

// 打开编辑弹窗
const openEditModal = (record: TransportMode) => {
  editingId.value = record.id || null
  formData.name = record.name
  formData.chineseName = record.chineseName
  formData.code = record.code
  formData.description = record.description
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
  formData.name = ''
  formData.chineseName = ''
  formData.code = ''
  formData.description = ''
  formData.sort = 0
  formData.status = 1
  formRef.value?.resetFields()
}

// 保存运输方式
const handleSave = async () => {
  try {
    await formRef.value?.validate()
    
    saving.value = true
    
    const data = {
      name: formData.name,
      chineseName: formData.chineseName,
      code: formData.code,
      description: formData.description,
      sort: formData.sort,
      status: formData.status
    }
    
    let response
    if (editingId.value) {
      response = await updateTransportMode(editingId.value, data as TransportMode)
    } else {
      response = await addTransportMode(data as TransportMode)
    }
    
    if (response.data?.code === 200) {
      message.success(editingId.value ? '更新成功' : '新增成功')
      closeModal()
      loadTransportModeList()
    } else {
      message.error(response.data?.message || (editingId.value ? '更新失败' : '新增失败'))
    }
  } catch (error) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 删除运输方式
const handleDelete = async (id: number) => {
  try {
    const response = await deleteTransportMode(id)
    if (response.data?.code === 200) {
      message.success('删除成功')
      loadTransportModeList()
    } else {
      message.error(response.data?.message || '删除失败')
    }
  } catch (error) {
    message.error('删除失败')
  }
}

// 切换状态
const toggleStatus = async (record: TransportMode) => {
  try {
    const response = await toggleTransportModeStatus(record.id!)
    if (response.data?.code === 200) {
      message.success('操作成功')
      loadTransportModeList()
    } else {
      message.error(response.data?.message || '操作失败')
    }
  } catch (error) {
    message.error('操作失败')
  }
}

onMounted(() => {
  loadTransportModeList()
})
</script>

<style scoped>
.transport-mode-management {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.operation-card {
  margin-bottom: 16px;
}
</style>
