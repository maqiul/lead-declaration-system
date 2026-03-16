<template>
  <div class="shipping-list-demo">
    <a-card title="发货清单管理">
      <div class="toolbar">
        <a-space>
          <a-input-search 
            v-model:value="searchText" 
            placeholder="搜索产品名称" 
            style="width: 200px"
            @search="handleSearch"
          />
          <a-button type="primary" @click="showAddModal">
            <template #icon><plus-outlined /></template>
            新增产品
          </a-button>
          <a-button @click="exportData">
            <template #icon><download-outlined /></template>
            导出Excel
          </a-button>
        </a-space>
      </div>

      <a-table
        :dataSource="filteredData"
        :columns="columns"
        :pagination="pagination"
        :scroll="{ x: 1200 }"
        rowKey="id"
        class="shipping-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'actions'">
            <a-space>
              <a-button type="link" size="small" @click="editItem(record)">
                编辑
              </a-button>
              <a-popconfirm
                title="确定要删除这个产品吗？"
                @confirm="deleteItem(record.id)"
              >
                <a-button type="link" size="small" danger>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑产品' : '新增产品'"
      @ok="handleSave"
      @cancel="handleCancel"
      :confirm-loading="saving"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="产品名称" name="productName">
          <a-input v-model:value="formData.productName" placeholder="请输入产品名称" />
        </a-form-item>
        
        <a-form-item label="数量" name="quantity">
          <a-input-group compact>
            <a-input-number 
              v-model:value="formData.quantity" 
              :min="1"
              style="width: 70%"
            />
            <a-select 
              v-model:value="formData.unit" 
              style="width: 30%"
              :options="unitOptions"
            />
          </a-input-group>
        </a-form-item>
        
        <a-form-item label="箱数" name="cartons">
          <a-input-number 
            v-model:value="formData.cartons" 
            :min="0"
            placeholder="箱数(可选)"
          />
        </a-form-item>
        
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="毛重(KGS)" name="grossWeight">
              <a-input-number 
                v-model:value="formData.grossWeight" 
                :min="0"
                :step="0.01"
                placeholder="毛重"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="净重(KGS)" name="netWeight">
              <a-input-number 
                v-model:value="formData.netWeight" 
                :min="0"
                :step="0.01"
                placeholder="净重"
              />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-form-item label="体积(CBM)" name="volume">
          <a-input-number 
            v-model:value="formData.volume" 
            :min="0"
            :step="0.01"
            placeholder="体积(立方米)"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'

// 表单引用
const formRef = ref<FormInstance>()

// 响应式数据
const searchText = ref('')
const modalVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

// 定义数据类型
interface ShippingItem {
  id: number
  productName: string
  quantity: number
  unit: string
  cartons?: number
  grossWeight: number
  netWeight: number
  volume?: number
}

// 示例数据
const shippingData = ref<ShippingItem[]>([
  {
    id: 1,
    productName: 'PLASTIC SEAL',
    quantity: 2000,
    unit: 'PCS',
    cartons: 1,
    grossWeight: 1.20,
    netWeight: 1.14,
    volume: 0.03
  },
  {
    id: 2,
    productName: 'RFID STICKER',
    quantity: 2500,
    unit: 'PCS',
    grossWeight: 2.68,
    netWeight: 2.56
  },
  {
    id: 3,
    productName: 'HANGTAG',
    quantity: 2000,
    unit: 'PCS',
    grossWeight: 8.34,
    netWeight: 7.96
  },
  {
    id: 4,
    productName: 'RFID STICKER',
    quantity: 80000,
    unit: 'PCS',
    cartons: 5,
    grossWeight: 82.70,
    netWeight: 81.50,
    volume: 0.15
  }
])

// 过滤后的数据
const filteredData = computed(() => {
  if (!searchText.value) return shippingData.value
  return shippingData.value.filter(item => 
    item.productName.toLowerCase().includes(searchText.value.toLowerCase())
  )
})

import type { ColumnsType } from 'ant-design-vue/es/table'

// 表格列配置
const columns: ColumnsType = [
  {
    title: '产品名称',
    dataIndex: 'productName',
    key: 'productName',
    fixed: 'left' as const,
    width: 150
  },
  {
    title: '数量',
    key: 'quantity',
    width: 120,
    customRender: ({ record }: any) => {
      return `${record.quantity} ${record.unit}`
    }
  },
  {
    title: '箱数',
    dataIndex: 'cartons',
    key: 'cartons',
    width: 100,
    customRender: ({ text }: any) => {
      return text ? `${text} CARTONS` : '-'
    }
  },
  {
    title: '毛重(KGS)',
    dataIndex: 'grossWeight',
    key: 'grossWeight',
    width: 120,
    customRender: ({ text }: any) => {
      return text ? text.toFixed(2) : '-'
    }
  },
  {
    title: '净重(KGS)',
    dataIndex: 'netWeight',
    key: 'netWeight',
    width: 120,
    customRender: ({ text }: any) => {
      return text ? text.toFixed(2) : '-'
    }
  },
  {
    title: '体积(CBM)',
    dataIndex: 'volume',
    key: 'volume',
    width: 120,
    customRender: ({ text }: any) => {
      return text ? text.toFixed(2) : '-'
    }
  },
  {
    title: '操作',
    key: 'actions',
    fixed: 'right',
    width: 120
  }
]

// 分页配置
const pagination = {
  pageSize: 10,
  showTotal: (total: number) => `共 ${total} 条记录`
}

// 单位选项
const unitOptions = [
  { label: 'PCS', value: 'PCS' },
  { label: 'SETS', value: 'SETS' },
  { label: 'SUITS', value: 'SUITS' },
  { label: 'ITEMS', value: 'ITEMS' },
  { label: 'KILOS', value: 'KILOS' },
  { label: 'GRAMS', value: 'GRAMS' },
  { label: 'TONS', value: 'TONS' },
  { label: 'CBM', value: 'CBM' },
  { label: 'SQM', value: 'SQM' },
  { label: 'METERS', value: 'METERS' }
]

// 表单数据类型
interface FormData {
  productName: string
  quantity: number
  unit: string
  cartons?: number
  grossWeight: number
  netWeight: number
  volume?: number
}

// 表单数据
const formData = reactive<FormData>({
  productName: '',
  quantity: 0,
  unit: 'PCS',
  cartons: undefined,
  grossWeight: 0,
  netWeight: 0,
  volume: undefined
})

// 表单验证规则
const formRules = {
  productName: [
    { required: true, message: '请输入产品名称' }
  ],
  quantity: [
    { required: true, message: '请输入数量' }
  ],
  grossWeight: [
    { required: true, message: '请输入毛重' }
  ],
  netWeight: [
    { required: true, message: '请输入净重' }
  ]
}

// 搜索处理
const handleSearch = () => {
  // 搜索逻辑已在computed中实现
}

// 显示新增弹窗
const showAddModal = () => {
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

// 编辑项目
const editItem = (record: any) => {
  editingId.value = record.id
  Object.assign(formData, {
    productName: record.productName,
    quantity: record.quantity,
    unit: record.unit,
    cartons: record.cartons,
    grossWeight: record.grossWeight,
    netWeight: record.netWeight,
    volume: record.volume
  })
  modalVisible.value = true
}

// 删除项目
const deleteItem = (id: number) => {
  const index = shippingData.value.findIndex(item => item.id === id)
  if (index > -1) {
    shippingData.value.splice(index, 1)
    message.success('删除成功')
  }
}

// 保存数据
const handleSave = async () => {
  try {
    await formRef.value?.validate()
    saving.value = true
    
    // 确保必填字段有值
    if (!formData.quantity || !formData.grossWeight || !formData.netWeight) {
      message.error('请填写完整的必填信息')
      return
    }
    
    if (editingId.value) {
      // 编辑模式
      const index = shippingData.value.findIndex(item => item.id === editingId.value)
      if (index > -1) {
        shippingData.value[index] = { ...formData, id: editingId.value }
        message.success('更新成功')
      }
    } else {
      // 新增模式
      const newItem: ShippingItem = {
        ...formData,
        id: Date.now() // 简单的ID生成
      }
      shippingData.value.push(newItem)
      message.success('新增成功')
    }
    
    modalVisible.value = false
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    saving.value = false
  }
}

// 取消操作
const handleCancel = () => {
  modalVisible.value = false
  resetForm()
}

// 重置表单
const resetForm = () => {
  Object.assign(formData, {
    productName: '',
    quantity: undefined,
    unit: 'PCS',
    cartons: undefined,
    grossWeight: undefined,
    netWeight: undefined,
    volume: undefined
  })
  formRef.value?.resetFields()
}

// 导出数据
const exportData = () => {
  // 这里可以实现Excel导出功能
  message.info('导出功能待实现')
}

onMounted(() => {
  // 组件挂载后的初始化逻辑
})
</script>

<style scoped lang="less">
.shipping-list-demo {
  padding: 24px;
  background: #f5f5f5;
  min-height: calc(100vh - 64px);
  
  .toolbar {
    margin-bottom: 16px;
    padding: 16px;
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
  }
  
  :deep(.shipping-table) {
    .ant-table {
      border-radius: 8px;
      overflow: hidden;
    }
    
    .ant-table-thead > tr > th {
      background-color: #fafafa;
      font-weight: 600;
    }
    
    .ant-table-tbody > tr:hover > td {
      background-color: #f0f8ff;
    }
  }
  
  :deep(.ant-btn-primary) {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
  }
}
</style>