<template>
  <div class="country-management">
    <!-- 搜索区域 -->
    <a-card class="search-card">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="关键词">
          <a-input v-model:value="searchForm.keyword" placeholder="国家代码/中文名称/英文名称" />
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
          新增国家
        </a-button>
        <a-button @click="loadCountryList">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card>
      <a-table
        :dataSource="countryList"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        rowKey="id"
      >
        <template #status="{ record }">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">
            {{ record.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        
        <template #action="{ record }">
          <a-space>
            <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
            <a-popconfirm
              title="确定要切换状态吗？"
              @confirm="toggleStatus(record)"
            >
              <a-button 
                type="link" 
                size="small" 
                :danger="record.status === 1"
              >
                {{ record.status === 1 ? '禁用' : '启用' }}
              </a-button>
            </a-popconfirm>
            <a-popconfirm
              title="确定要删除吗？"
              @confirm="handleDelete(record.id)"
            >
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table>
    </a-card>

    <!-- 编辑弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="editingId ? '编辑国家信息' : '新增国家信息'"
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
        <a-form-item label="国家代码" name="countryCode">
          <a-input
            v-model:value="formData.countryCode"
            placeholder="请输入3位国家代码(如: CHN)"
            :maxlength="10"
          />
        </a-form-item>

        <a-form-item label="中文名称" name="chineseName">
          <a-input
            v-model:value="formData.chineseName"
            placeholder="请输入中文名称"
          />
        </a-form-item>

        <a-form-item label="英文名称" name="englishName">
          <a-input
            v-model:value="formData.englishName"
            placeholder="请输入英文名称"
          />
        </a-form-item>

        <a-form-item label="简称/缩写" name="abbreviation">
          <a-input
            v-model:value="formData.abbreviation"
            placeholder="请输入简称或缩写"
            :maxlength="10"
          />
        </a-form-item>

        <a-form-item label="电话区号" name="phoneCode">
          <a-input
            v-model:value="formData.phoneCode"
            placeholder="请输入电话区号"
            :maxlength="10"
          />
        </a-form-item>

        <a-form-item label="货币代码" name="currencyCode">
          <a-input
            v-model:value="formData.currencyCode"
            placeholder="请输入货币代码"
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
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import request from '@/utils/request'

// API接口定义
interface CountryInfo {
  id?: number
  countryCode: string
  chineseName: string
  englishName: string
  abbreviation: string
  phoneCode: string
  currencyCode: string
  sort: number
  status: number
  remarks: string
  createTime?: string
}

// API接口函数
const getCountries = (params: any) => {
  return request({
    url: '/v1/countries',
    method: 'get',
    params
  })
}

const addCountry = (data: any) => {
  return request({
    url: '/v1/countries',
    method: 'post',
    data
  })
}

const updateCountry = (id: number, data: any) => {
  return request({
    url: `/v1/countries/${id}`,
    method: 'put',
    data
  })
}

const deleteCountry = (id: number) => {
  return request({
    url: `/v1/countries/${id}`,
    method: 'delete'
  })
}

const toggleCountryStatus = (id: number, status: number) => {
  return request({
    url: `/v1/countries/${id}/toggle-status`,
    method: 'post',
    params: { status }
  })
}

// 搜索表单
const searchForm = reactive({
  keyword: '',
  status: undefined as number | undefined
})

// 表格数据
const countryList = ref<CountryInfo[]>([])
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
    title: '国家代码',
    dataIndex: 'countryCode',
    key: 'countryCode',
    width: 120
  },
  {
    title: '中文名称',
    dataIndex: 'chineseName',
    key: 'chineseName',
    width: 150
  },
  {
    title: '英文名称',
    dataIndex: 'englishName',
    key: 'englishName',
    width: 150
  },
  {
    title: '简称',
    dataIndex: 'abbreviation',
    key: 'abbreviation',
    width: 100
  },
  {
    title: '电话区号',
    dataIndex: 'phoneCode',
    key: 'phoneCode',
    width: 100
  },
  {
    title: '货币代码',
    dataIndex: 'currencyCode',
    key: 'currencyCode',
    width: 100
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    slots: { customRender: 'status' }
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
    width: 180,
    slots: { customRender: 'action' }
  }
]

// 弹窗相关
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref()

// 表单数据
const formData = reactive({
  countryCode: '',
  chineseName: '',
  englishName: '',
  abbreviation: '',
  phoneCode: '',
  currencyCode: '',
  sort: 0,
  status: 1,
  remarks: ''
})

// 表单验证规则
const formRules = {
  countryCode: [
    { required: true, message: '请输入国家代码' },
    { pattern: /^[A-Z]{3}$/, message: '请输入3位大写字母' }
  ],
  chineseName: [
    { required: true, message: '请输入中文名称' }
  ],
  englishName: [
    { required: true, message: '请输入英文名称' }
  ]
}

// 加载国家列表
const loadCountryList = async () => {
  try {
    loading.value = true
    const response = await getCountries({
      current: pagination.current,
      size: pagination.pageSize,
      keyword: searchForm.keyword,
      status: searchForm.status
    })
    
    if (response.data?.code === 200) {
      countryList.value = response.data.data.records || []
      pagination.total = response.data.data.total || 0
    } else {
      message.error(response.data?.message || '加载失败')
    }
  } catch (error) {
    console.error('加载国家列表失败:', error)
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadCountryList()
}

// 重置搜索
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = undefined
  pagination.current = 1
  loadCountryList()
}

// 表格分页变化
const handleTableChange = (pag: TablePaginationConfig) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadCountryList()
}

// 打开新增弹窗
const openAddModal = () => {
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

// 打开编辑弹窗
const openEditModal = (record: CountryInfo) => {
  editingId.value = record.id || null
  formData.countryCode = record.countryCode
  formData.chineseName = record.chineseName
  formData.englishName = record.englishName
  formData.abbreviation = record.abbreviation
  formData.phoneCode = record.phoneCode
  formData.currencyCode = record.currencyCode
  formData.sort = record.sort
  formData.status = record.status
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
  formData.countryCode = ''
  formData.chineseName = ''
  formData.englishName = ''
  formData.abbreviation = ''
  formData.phoneCode = ''
  formData.currencyCode = ''
  formData.sort = 0
  formData.status = 1
  formData.remarks = ''
  formRef.value?.resetFields()
}

// 保存国家信息
const handleSave = async () => {
  try {
    await formRef.value?.validate()
    
    saving.value = true
    
    const data = {
      countryCode: formData.countryCode,
      chineseName: formData.chineseName,
      englishName: formData.englishName,
      abbreviation: formData.abbreviation,
      phoneCode: formData.phoneCode,
      currencyCode: formData.currencyCode,
      sort: formData.sort,
      status: formData.status,
      remarks: formData.remarks
    }
    
    let response
    if (editingId.value) {
      response = await updateCountry(editingId.value, data)
    } else {
      response = await addCountry(data)
    }
    
    if (response.data?.code === 200) {
      message.success(editingId.value ? '更新成功' : '新增成功')
      closeModal()
      loadCountryList()
    } else {
      message.error(response.data?.message || (editingId.value ? '更新失败' : '新增失败'))
    }
  } catch (error) {
    console.error('保存失败', error)
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 删除国家信息
const handleDelete = async (id: number) => {
  try {
    const response = await deleteCountry(id)
    if (response.data?.code === 200) {
      message.success('删除成功')
      loadCountryList()
    } else {
      message.error(response.data?.message || '删除失败')
    }
  } catch (error) {
    message.error('删除失败')
  }
}

// 切换状态
const toggleStatus = async (record: CountryInfo) => {
  try {
    const newStatus = record.status === 1 ? 0 : 1
    const response = await toggleCountryStatus(record.id!, newStatus)
    if (response.data?.code === 200) {
      message.success('操作成功')
      loadCountryList()
    } else {
      message.error(response.data?.message || '操作失败')
    }
  } catch (error) {
    message.error('操作失败')
  }
}

onMounted(() => {
  loadCountryList()
})
</script>

<style scoped>
.country-management {
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
}

.operation-card {
  margin-bottom: 16px;
}
</style>