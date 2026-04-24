<template>
  <div class="country-management px-6 py-6 bg-white min-h-full">
    <!-- 搜索区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-form :model="searchForm" layout="inline" class="flex flex-wrap gap-4">
        <a-form-item label="国家名称">
          <a-input v-model:value="searchForm.chineseName" placeholder="请输入国家名称" allow-clear class="ui-input" />
        </a-form-item>
        <a-form-item label="国家编码">
          <a-input v-model:value="searchForm.countryCode" placeholder="请输入国家编码" allow-clear class="ui-input" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchForm.status" placeholder="请选择状态" style="width: 140px" allow-clear class="ui-select">
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

    <!-- 操作区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-space>
        <a-button type="primary" @click="handleAdd" v-permission="['system:country:create']" class="ui-btn-cta">
          <template #icon><plus-outlined /></template>
          新增国家
        </a-button>
        <a-button @click="loadCountryList" class="ui-btn-secondary">
          <template #icon><reload-outlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="ui-card" :bordered="false">
      <a-table
        :columns="columns"
        :data-source="countryList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
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
              <a-button type="link" size="small" @click="handleEdit(record as CountryInfo)" v-permission="['system:country:update']" class="text-blue-600 font-medium">编辑</a-button>
              <a-button type="link" size="small" :danger="record.status === 1" @click="handleToggleStatus(record as CountryInfo)" v-permission="['system:country:update']" class="font-medium">
                {{ record.status === 1 ? '禁用' : '启用' }}
              </a-button>
              <a-popconfirm
                title="确定要删除国家数据吗？"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger v-permission="['system:country:delete']" class="font-medium">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      @ok="handleModalOk"
      :confirm-loading="confirmLoading"
      destroyOnClose
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
      >
        <a-form-item label="国家中文名称" name="chineseName">
          <a-input v-model:value="formData.chineseName" placeholder="请输入国家中文名称" />
        </a-form-item>
        <a-form-item label="国家英文名称" name="englishName">
          <a-input v-model:value="formData.englishName" placeholder="请输入国家英文名称" />
        </a-form-item>
        <a-form-item label="国家编码(ISO-2)" name="countryCode">
          <a-input v-model:value="formData.countryCode" placeholder="请输入国家编码" :maxlength="3" />
        </a-form-item>
        <a-form-item label="缩写/三位编码" name="abbreviation">
          <a-input v-model:value="formData.abbreviation" placeholder="请输入缩写" :maxlength="3" />
        </a-form-item>
        <a-form-item label="大洲" name="continent">
          <a-input v-model:value="formData.continent" placeholder="请输入大洲" />
        </a-form-item>
        <a-form-item label="排序" name="sort">
          <a-input-number v-model:value="formData.sort" :min="0" style="width: 100%" />
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
import { 
  PlusOutlined, 
  ReloadOutlined, 
  SearchOutlined 
} from '@ant-design/icons-vue'
import { 
  getCountryList, 
  addCountry, 
  updateCountry, 
  deleteCountry, 
  toggleCountryStatus 
} from '@/api/system'
import type { TablePaginationConfig } from 'ant-design-vue'
import type { CountryInfo } from '@/api/system'

// 搜索表单
const searchForm = reactive({
  chineseName: '',
  countryCode: '',
  status: undefined as number | undefined
})

// 表格列定义
const columns = [
  {
    title: '中文名称',
    dataIndex: 'chineseName',
    key: 'chineseName'
  },
  {
    title: '英文名称',
    dataIndex: 'englishName',
    key: 'englishName'
  },
  {
    title: '国家编码',
    dataIndex: 'countryCode',
    key: 'countryCode'
  },
  {
    title: '缩写',
    dataIndex: 'abbreviation',
    key: 'abbreviation'
  },
  {
    title: '大洲',
    dataIndex: 'continent',
    key: 'continent'
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status'
  },
  {
    title: '排序',
    dataIndex: 'sort',
    key: 'sort'
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right' as const,
    width: 200
  }
]

// 响应式数据
const loading = ref(false)
const countryList = ref<CountryInfo[]>([])
const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total) => `共 ${total} 条`
})

// 弹窗相关
const modalVisible = ref(false)
const modalTitle = ref('新增国家')
const confirmLoading = ref(false)
const formRef = ref()
const formData = reactive<CountryInfo>({
  id: undefined,
  chineseName: '',
  englishName: '',
  countryCode: '',
  abbreviation: '',
  continent: '',
  sort: 0,
  status: 1
})

const formRules = {
  chineseName: [{ required: true, message: '请输入国家中文名称' }],
  englishName: [{ required: true, message: '请输入国家英文名称' }],
  countryCode: [{ required: true, message: '请输入国家编码' }]
}

// 方法
const loadCountryList = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.pageSize,
      keyword: searchForm.chineseName || searchForm.countryCode || undefined
    }
    const response = await getCountryList(params)
    if (response.data?.code === 200) {
      // MyBatis-Plus Page 返回 records 字段
      const records = response.data.data?.records
      countryList.value = Array.isArray(records) ? records : []
      pagination.total = response.data.data?.total || 0
    }
  } catch (error) {
    message.error('加载国家列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadCountryList()
}

const handleReset = () => {
  searchForm.chineseName = ''
  searchForm.countryCode = ''
  searchForm.status = undefined
  handleSearch()
}

const handleTableChange = (pag: TablePaginationConfig) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadCountryList()
}

const handleAdd = () => {
  modalTitle.value = '新增国家'
  Object.assign(formData, {
    id: undefined,
    chineseName: '',
    englishName: '',
    countryCode: '',
    abbreviation: '',
    continent: '',
    sort: 0,
    status: 1
  })
  modalVisible.value = true
}

const handleEdit = (record: CountryInfo) => {
  modalTitle.value = '编辑国家'
  Object.assign(formData, { 
    ...record,
    id: record.id !== null && record.id !== undefined 
      ? (typeof record.id === 'string' ? parseInt(record.id, 10) : record.id) 
      : undefined
  })
  modalVisible.value = true
}

const handleDelete = async (id: number | string) => {
  try {
    const numericId = typeof id === 'string' ? parseInt(id, 10) : id;
    const response = await deleteCountry(numericId)
    if (response.data?.code === 200) {
      message.success('删除成功')
      loadCountryList()
    } else {
      message.error(response.data?.message || '删除失败')
    }
  } catch (error) {
    message.error('操作失败')
  }
}

const handleModalOk = async () => {
  try {
    await formRef.value?.validateFields()
    confirmLoading.value = true
    
    let response
    if (formData.id) {
      const id = typeof formData.id === 'string' ? parseInt(formData.id, 10) : formData.id;
      response = await updateCountry(id, formData)
    } else {
      response = await addCountry(formData)
    }
    
    if (response.data?.code === 200) {
      message.success('保存成功')
      modalVisible.value = false
      loadCountryList()
    } else {
      message.error(response.data?.message || '保存失败')
    }
  } catch (error) {
    // 验证失败
  } finally {
    confirmLoading.value = false
  }
}

const handleToggleStatus = async (record: CountryInfo) => {
  if (!record.id) return
  try {
    const id = typeof record.id === 'string' ? parseInt(record.id, 10) : record.id;
    const newStatus = record.status === 1 ? 0 : 1
    const response = await toggleCountryStatus(id, newStatus)
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
/* 页面特有样式已由全局 index.less 覆盖 */
</style>