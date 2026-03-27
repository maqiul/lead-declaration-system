<template>
  <div class="city-info-container">
    <a-card class="search-card" :bordered="false">
      <div class="search-content">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-item label="城市名称">
              <a-input 
                v-model:value="queryParams.cityName" 
                placeholder="请输入城市名称" 
                @pressEnter="handleSearch" 
              />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="国家">
              <a-input 
                v-model:value="queryParams.countryName" 
                placeholder="请输入国家名称" 
                @pressEnter="handleSearch" 
              />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="状态">
              <a-select 
                v-model:value="queryParams.status" 
                placeholder="请选择状态" 
                allow-clear
              >
                <a-select-option :value="1">启用</a-select-option>
                <a-select-option :value="0">禁用</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <div class="search-actions">
          <a-space>
            <a-button type="primary" @click="handleSearch">
              <template #icon><SearchOutlined /></template>
              搜索
            </a-button>
            <a-button @click="handleReset">
              <template #icon><ReloadOutlined /></template>
              重置
            </a-button>
            <a-button 
              v-hasPermi="['system:city-info:add']" 
              type="primary" 
              @click="handleAdd"
            >
              <template #icon><PlusOutlined /></template>
              新增
            </a-button>
          </a-space>
        </div>
      </div>
    </a-card>

    <a-card :bordered="false">
      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'cityName'">
            {{ record.cityName }}
          </template>
          
          <template v-else-if="column.dataIndex === 'cityEnglishName'">
            {{ record.cityEnglishName }}
          </template>
          
          <template v-else-if="column.dataIndex === 'provinceName'">
            {{ record.provinceName }}
          </template>
          
          <template v-else-if="column.dataIndex === 'provinceEnglishName'">
            {{ record.provinceEnglishName }}
          </template>
          
          <template v-else-if="column.dataIndex === 'countryName'">
            {{ record.countryName }}
          </template>
          
          <template v-else-if="column.dataIndex === 'countryEnglishName'">
            {{ record.countryEnglishName }}
          </template>
          
          <template v-else-if="column.dataIndex === 'cityCode'">
            {{ record.cityCode }}
          </template>
          
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ record.createTime }}
          </template>
          
          <template v-else-if="column.key === 'action'">
            <a-space size="middle">
              <a 
                v-hasPermi="['system:city-info:query']" 
                @click="handleView(record)"
              >
                <EyeOutlined />
                详情
              </a>
              <a 
                v-hasPermi="['system:city-info:update']" 
                @click="handleUpdate(record)"
              >
                <EditOutlined />
                修改
              </a>
              <a-popconfirm
                v-hasPermi="['system:city-info:delete']"
                title="确定删除该城市信息吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record.id)"
              >
                <a class="danger-link">
                  <DeleteOutlined />
                  删除
                </a>
              </a-popconfirm>
              <a 
                v-if="record.status === 1"
                v-hasPermi="['system:city-info:update']" 
                @click="handleStatusChange(record, 0)"
              >
                <StopOutlined />
                禁用
              </a>
              <a 
                v-else
                v-hasPermi="['system:city-info:update']" 
                @click="handleStatusChange(record, 1)"
              >
                <CheckCircleOutlined />
                启用
              </a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 城市信息弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :width="700"
      :mask-closable="false"
      :destroy-on-close="true"
      @ok="handleSubmit"
      @cancel="handleCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 18 }"
        :rules="rules"
      >
        <a-form-item label="城市中文名" name="cityName">
          <a-input 
            v-model:value="formData.cityName" 
            placeholder="请输入城市中文名" 
            :maxlength="100"
          />
        </a-form-item>
        
        <a-form-item label="城市英文名" name="cityEnglishName">
          <a-input 
            v-model:value="formData.cityEnglishName" 
            placeholder="请输入城市英文名" 
            :maxlength="200"
          />
        </a-form-item>
        
        <a-form-item label="省份中文名" name="provinceName">
          <a-input 
            v-model:value="formData.provinceName" 
            placeholder="请输入省份中文名" 
            :maxlength="100"
          />
        </a-form-item>
        
        <a-form-item label="省份英文名" name="provinceEnglishName">
          <a-input 
            v-model:value="formData.provinceEnglishName" 
            placeholder="请输入省份英文名" 
            :maxlength="200"
          />
        </a-form-item>
        
        <a-form-item label="国家中文名" name="countryName">
          <a-input 
            v-model:value="formData.countryName" 
            placeholder="请输入国家中文名" 
            :maxlength="100"
            :disabled="isEdit"
          />
        </a-form-item>
        
        <a-form-item label="国家英文名" name="countryEnglishName">
          <a-input 
            v-model:value="formData.countryEnglishName" 
            placeholder="请输入国家英文名" 
            :maxlength="200"
            :disabled="isEdit"
          />
        </a-form-item>
        
        <a-form-item label="城市编码" name="cityCode">
          <a-input 
            v-model:value="formData.cityCode" 
            placeholder="请输入城市编码" 
            :maxlength="20"
          />
        </a-form-item>
        
        <a-form-item label="排序" name="sort">
          <a-input-number 
            v-model:value="formData.sort" 
            placeholder="请输入排序值" 
            :min="0"
            :max="9999"
            style="width: 100%"
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
            placeholder="请输入备注信息" 
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
import { 
  SearchOutlined, 
  ReloadOutlined, 
  PlusOutlined, 
  EyeOutlined, 
  EditOutlined, 
  DeleteOutlined, 
  StopOutlined, 
  CheckCircleOutlined 
} from '@ant-design/icons-vue'
import { 
  getCityList, 
  getCityById, 
  addCity, 
  updateCity, 
  deleteCity, 
  updateCityStatus,
//   getCityProvinces
} from '@/api/system/city-info'

// 表格列配置
const columns: any[] = [
  {
    title: '城市中文名',
    dataIndex: 'cityName',
    width: 120,
    ellipsis: true
  },
  {
    title: '城市英文名',
    dataIndex: 'cityEnglishName',
    width: 150,
    ellipsis: true
  },
  {
    title: '省份中文名',
    dataIndex: 'provinceName',
    width: 120,
    ellipsis: true
  },
  {
    title: '省份英文名',
    dataIndex: 'provinceEnglishName',
    width: 150,
    ellipsis: true
  },
  {
    title: '国家中文名',
    dataIndex: 'countryName',
    width: 100,
    ellipsis: true
  },
  {
    title: '国家英文名',
    dataIndex: 'countryEnglishName',
    width: 150,
    ellipsis: true
  },
  {
    title: '城市编码',
    dataIndex: 'cityCode',
    width: 120,
    ellipsis: true
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 80,
    align: 'center' as const
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 150,
    align: 'center' as const
  },
  {
    title: '操作',
    key: 'action',
    width: 280,
    fixed: 'right',
    align: 'center' as const
  }
]

// 响应式数据
const loading = ref<boolean>(true)
const list = ref<any[]>([])
// const provinceList = ref<string[]>([])
const queryParams = reactive({
  cityName: '',
  provinceName: '',
  countryName: '',
  status: undefined,
  pageNum: 1,
  pageSize: 10
})
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  pageSizeOptions: ['10', '20', '30', '50'],
  showTotal: (total: number) => `共 ${total} 条`
})

// 弹窗相关
const modalVisible = ref<boolean>(false)
const modalTitle = ref<string>('')
const isEdit = ref<boolean>(false)
const formRef = ref()

// 表单数据
const formData = reactive({
  id: undefined,
  cityName: '',
  cityEnglishName: '',
  provinceName: '',
  provinceEnglishName: '',
  countryName: '中国',
  countryEnglishName: 'China',
  cityCode: '',
  sort: 0,
  status: 1,
  remarks: ''
})

// 表单验证规则
const rules: Record<string, any> = {
  cityName: [
    { required: true, message: '请输入城市中文名', trigger: 'blur' }
  ],
  cityEnglishName: [
    { required: true, message: '请输入城市英文名', trigger: 'blur' }
  ],
  provinceName: [
    { required: true, message: '请输入省份中文名', trigger: 'blur' }
  ],
  provinceEnglishName: [
    { required: true, message: '请输入省份英文名', trigger: 'blur' }
  ]
}

// 加载数据
const getList = async () => {
  loading.value = true
  try {
    const response = await getCityList(queryParams)
    if (response.data && response.data.code === 200) {
      list.value = response.data.data?.records || []
      pagination.total = response.data.data?.total || 0
      pagination.current = queryParams.pageNum
      pagination.pageSize = queryParams.pageSize
    } else {
      message.error(response.data?.message || '获取城市信息失败')
    }
  } catch (error) {
    console.error('获取城市信息失败:', error)
    message.error('获取城市信息失败')
  } finally {
    loading.value = false
  }
}

// // 获取省份列表
// const getProvinces = async () => {
//   try {
//     const response = await getCityProvinces()
//     if (response.data && response.data.code === 200) {
//       provinceList.value = response.data.data || []
//     } else {
//       console.error('获取省份列表失败:', response.data?.message)
//     }
//   } catch (error) {
//     console.error('获取省份列表失败:', error)
//   }
// }

// 搜索
const handleSearch = () => {
  queryParams.pageNum = 1
  getList()
}

// 重置
const handleReset = () => {
  queryParams.cityName = ''
  queryParams.provinceName = ''
  queryParams.countryName = ''
  queryParams.status = undefined
  queryParams.pageNum = 1
  getList()
}

// 分页变化
const handleTableChange = (pag: any) => {
  queryParams.pageNum = pag.current
  queryParams.pageSize = pag.pageSize
  getList()
}

// 新增
const handleAdd = () => {
  resetFormData()
  modalTitle.value = '添加城市信息'
  isEdit.value = false
  modalVisible.value = true
}

// 修改
const handleUpdate = async (record: any) => {
  resetFormData()
  try {
    const response = await getCityById(record.id)
    if (response.data && response.data.code === 200) {
      Object.assign(formData, response.data.data)
      modalTitle.value = '修改城市信息'
      isEdit.value = true
      modalVisible.value = true
    } else {
      message.error(response.data?.message || '获取城市信息失败')
    }
  } catch (error) {
    console.error('获取城市信息失败:', error)
    message.error('获取城市信息失败')
  }
}

// 详情
const handleView = async (record: any) => {
  resetFormData()
  try {
    const response = await getCityById(record.id)
    if (response.data && response.data.code === 200) {
      Object.assign(formData, response.data.data)
      modalTitle.value = '城市信息详情'
      isEdit.value = true
      modalVisible.value = true
    } else {
      message.error(response.data?.message || '获取城市信息失败')
    }
  } catch (error) {
    console.error('获取城市信息失败:', error)
    message.error('获取城市信息失败')
  }
}

// 删除
const handleDelete = async (id: number) => {
  try {
    const response = await deleteCity(id)
    if (response.data && response.data.code === 200) {
      message.success('删除成功')
      getList()
    } else {
      message.error(response.data?.message || '删除失败')
    }
  } catch (error) {
    console.error('删除城市信息失败:', error)
    message.error('删除失败')
  }
}

// 状态变更
const handleStatusChange = async (record: any, status: number) => {
  try {
    const response = await updateCityStatus(record.id, status)
    if (response.data && response.data.code === 200) {
      message.success(`${status === 1 ? '启用' : '禁用'}`)
      getList()
    } else {
      message.error(response.data?.message || `操作失败`)
    }
  } catch (error) {
    console.error(`切换状态失败:`, error)
    message.error(`操作失败`)
  }
}

// 提交
const handleSubmit = async () => {
  try {
    await formRef.value.validateFields()
    
    let response
    if (isEdit.value) {
      response = await updateCity(formData.id!, formData)
    } else {
      response = await addCity(formData)
    }
    
    if (response.data && response.data.code === 200) {
      message.success(`${isEdit.value ? '修改' : '添加'}成功`)
      modalVisible.value = false
      getList()
    } else {
      message.error(response.data?.message || `${isEdit.value ? '修改' : '添加'}失败`)
    }
  } catch (error) {
    console.error(`${isEdit.value ? '修改' : '添加'}失败:`, error)
    message.error(`${isEdit.value ? '修改' : '添加'}失败`)
  }
}

// 取消
const handleCancel = () => {
  modalVisible.value = false
}

// 重置表单数据
const resetFormData = () => {
  formData.id = undefined
  formData.cityName = ''
  formData.cityEnglishName = ''
  formData.provinceName = ''
  formData.provinceEnglishName = ''
  formData.countryName = '中国'
  formData.countryEnglishName = 'China'
  formData.cityCode = ''
  formData.sort = 0
  formData.status = 1
  formData.remarks = ''
}

// 页面加载
onMounted(() => {
  getList()
//   getProvinces()
})
</script>

<style scoped>
.city-info-container {
  padding: 16px;
  background-color: #fff;
}

.search-card {
  margin-bottom: 16px;
}

.search-content {
  padding: 8px 0;
}

.search-actions {
  margin-top: 16px;
  text-align: right;
}

.danger-link {
  color: #ff4d4f;
}

.danger-link:hover {
  color: #ff7875;
}
</style>