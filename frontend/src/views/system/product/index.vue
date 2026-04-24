<template>
  <div class="product-maintenance px-6 py-8 bg-white min-h-full font-['Source_Sans_3']">
    <!-- 搜索区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-form :model="searchForm" layout="inline" class="flex flex-wrap gap-4">
        <a-form-item label="HS编码">
          <a-input v-model:value="searchForm.hsCode" placeholder="请输入HS编码" allow-clear class="ui-input" />
        </a-form-item>
        <a-form-item label="商品名称">
          <a-input v-model:value="searchForm.productName" placeholder="请输入商品名称" allow-clear class="ui-input" />
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

    <!-- 操作按钮区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-space>
        <a-button type="primary" @click="openAddModal" v-permission="['system:product:create']" class="ui-btn-cta">
          <template #icon><plus-outlined /></template>
          新增商品
        </a-button>
        <a-button @click="loadProductList" class="ui-btn-secondary">
          <template #icon><reload-outlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="ui-card" :bordered="false">
      <a-table
        :dataSource="productList"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        rowKey="id"
        class="ui-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'declarationElements'">
            <a-button type="link" size="small" @click="viewElements(record)" class="text-blue-600 font-medium">
              查看申报要素
            </a-button>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditModal(record)" v-permission="['system:product:update']" class="text-blue-600 font-medium">
                编辑
              </a-button>
              <a-popconfirm
                title="确定要删除该商品吗？"
                @confirm="handleDelete((record as any).id)"
              >
                <a-button type="link" danger size="small" v-permission="['system:product:delete']" class="font-medium">
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
      :title="editingId ? '编辑商品' : '新增商品'"
      :width="900"
      :confirm-loading="saving"
      @ok="handleSave"
      @cancel="closeModal"
      destroyOnClose
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
        class="px-2"
      >
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="HS编码" name="hsCode">
              <a-input v-model:value="formData.hsCode" placeholder="请输入10位HS编码" :maxlength="10" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="计量单位" name="unitCode">
              <a-select
                v-model:value="formData.unitCode"
                placeholder="请选择计量单位"
                show-search
                option-filter-prop="label"
                @change="handleUnitChange"
              >
                <a-select-option 
                  v-for="unit in unitOptions" 
                  :key="unit.unitCode" 
                  :value="unit.unitCode"
                  :label="`${unit.unitName}/${unit.unitNameEn}(${unit.unitCode})`"
                >
                  {{ unit.unitName }}/{{ unit.unitNameEn }}({{ unit.unitCode }})
                  <span class="text-slate-400 ml-2 italic text-xs">{{ unit.unitType }}</span>
                </a-select-option>
              </a-select>
              <div v-if="selectedUnitInfo" class="mt-1 text-xs text-blue-500 font-medium">
                {{ selectedUnitInfo }}
              </div>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="中文名称" name="chineseName">
              <a-input v-model:value="formData.chineseName" placeholder="请输入中文名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="英文名称" name="englishName">
              <a-input v-model:value="formData.englishName" placeholder="请输入英文名称" />
            </a-form-item>
          </a-col>
        </a-row>

        <div class="mt-6 mb-4 flex items-center justify-between border-b border-[#E2E8F0] pb-3">
          <span class="text-[#1E40AF] font-bold font-lexend text-lg tracking-wide">申报要素配置</span>
          <a-button type="dashed" size="small" @click="addElement" class="ui-btn-secondary">
            <plus-outlined /> 添加要素
          </a-button>
        </div>
        
        <div class="elements-container max-h-[400px] overflow-y-auto pr-2">
          <div
            v-for="(element, index) in formData.elements"
            :key="index"
            class="element-item p-5 mb-4 bg-white border border-[#E2E8F0] rounded-xl shadow-sm hover:border-[#3B82F6] hover:shadow-md transition-all duration-200"
          >
            <a-row :gutter="12" align="middle">
              <a-col :span="2">
                <a-input v-model:value="element.key" placeholder="序号" />
              </a-col>
              <a-col :span="5">
                <a-input v-model:value="element.label" placeholder="字段标签" />
              </a-col>
              <a-col :span="4">
                <a-select v-model:value="element.type" placeholder="类型" class="w-full">
                  <a-select-option value="text">文本输入</a-select-option>
                  <a-select-option value="select">下拉选择</a-select-option>
                  <a-select-option value="checkbox">复选框</a-select-option>
                </a-select>
              </a-col>
              <a-col :span="5">
                <a-input v-model:value="element.defaultValue" placeholder="默认值/值" />
              </a-col>
              <a-col :span="6">
                <div class="flex items-center gap-4">
                  <a-checkbox v-model:checked="element.editable" class="text-xs">可编辑</a-checkbox>
                  <a-checkbox v-model:checked="element.required" class="text-xs">必填</a-checkbox>
                  <a-button type="link" danger size="small" @click="removeElement(index)" class="ml-auto">
                    <delete-outlined />
                  </a-button>
                </div>
              </a-col>
            </a-row>
            <div v-if="element.type === 'select'" class="mt-3 bg-slate-50 p-2 rounded border border-dashed border-slate-300">
               <a-input
                v-model:value="element.optionsStr"
                placeholder="下拉选项（用英文逗号分隔，如：是,否）"
                size="small"
                class="ui-input"
              />
            </div>
          </div>
          <div v-if="formData.elements.length === 0" class="text-center py-8 text-slate-400 italic">
            暂未配置申报要素，点击上方按钮添加。
          </div>
        </div>
      </a-form>
    </a-modal>

    <!-- 查看申报要素弹窗 -->
    <a-modal
      v-model:open="elementsModalVisible"
      title="申报要素详情"
      :footer="null"
      :width="800"
      destroyOnClose
    >
      <a-table
        :dataSource="currentElements"
        :columns="elementColumns"
        :pagination="false"
        size="small"
        rowKey="key"
        class="ui-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'editable'">
            <a-tag :color="(record as any).editable ? 'success' : 'default'">
              {{ (record as any).editable ? '可操作' : '锁定' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'required'">
            <a-tag :color="(record as any).required ? 'error' : 'default'">
              {{ (record as any).required ? '必填' : '选填' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'options'">
            <div v-if="(record as any).type === 'select' && (record as any).options">
              <a-tag v-for="opt in (record as any).options" :key="opt" color="blue" class="m-0.5">{{ opt }}</a-tag>
            </div>
            <span v-else class="text-slate-400 italic">-</span>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  DeleteOutlined,
  SearchOutlined
} from '@ant-design/icons-vue'
import {
  getProducts,
  addProduct,
  updateProduct,
  deleteProduct as deleteProductApi
} from '@/api/system/product'

// 表单引用
const formRef = ref<FormInstance>()

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const productList = ref<any[]>([])

// 搜索表单
const searchForm = reactive({
  hsCode: '',
  productName: ''
})
const modalVisible = ref(false)
const elementsModalVisible = ref(false)
const editingId = ref<number | null>(null)
const currentElements = ref<any[]>([])

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// 单位配置
const unitOptions = ref([
  { unitCode: '01', unitName: '个', unitNameEn: 'PCS', unitType: '数量单位' },
  { unitCode: '02', unitName: '台', unitNameEn: 'SETS', unitType: '数量单位' },
  { unitCode: '03', unitName: '套', unitNameEn: 'SUITS', unitType: '数量单位' },
  { unitCode: '04', unitName: '件', unitNameEn: 'ITEMS', unitType: '数量单位' },
  { unitCode: '05', unitName: '只', unitNameEn: 'ONLY', unitType: '数量单位' },
  { unitCode: '06', unitName: '张', unitNameEn: 'SHEETS', unitType: '数量单位' },
  { unitCode: '07', unitName: '支', unitNameEn: 'BRANCHES', unitType: '数量单位' },
  { unitCode: '08', unitName: '根', unitNameEn: 'ROOTS', unitType: '数量单位' },
  { unitCode: '09', unitName: '条', unitNameEn: 'STRIPS', unitType: '数量单位' },
  { unitCode: '10', unitName: '块', unitNameEn: 'BLOCKS', unitType: '数量单位' },
  { unitCode: '11', unitName: '千克', unitNameEn: 'KILOS', unitType: '重量单位' },
  { unitCode: '12', unitName: '克', unitNameEn: 'GRAMS', unitType: '重量单位' },
  { unitCode: '13', unitName: '吨', unitNameEn: 'TONS', unitType: '重量单位' },
  { unitCode: '14', unitName: '立方米', unitNameEn: 'CBM', unitType: '体积单位' },
  { unitCode: '15', unitName: '平方米', unitNameEn: 'SQM', unitType: '面积单位' },
  { unitCode: '16', unitName: '米', unitNameEn: 'METERS', unitType: '长度单位' },
  { unitCode: '17', unitName: '厘米', unitNameEn: 'CM', unitType: '长度单位' },
  { unitCode: '18', unitName: '毫米', unitNameEn: 'MM', unitType: '长度单位' },
  { unitCode: '19', unitName: '升', unitNameEn: 'LITERS', unitType: '容量单位' },
  { unitCode: '20', unitName: '毫升', unitNameEn: 'ML', unitType: '容量单位' }
])

const selectedUnitInfo = ref('')

// 表格列配置
const columns: any[] = [
  {
    title: '序号',
    key: 'serialNumber',
    width: 60,
    customRender: ({ index }: any) => index + 1 + (pagination.current - 1) * pagination.pageSize
  },
  {
    title: 'HS编码',
    dataIndex: 'hsCode',
    key: 'hsCode',
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
    width: 250
  },
  {
    title: '计量单位',
    key: 'unit',
    width: 150,
    customRender: ({ record }: any) => {
      if (record.unitName && record.unitNameEn) {
        return `${record.unitName}/${record.unitNameEn}(${record.unitCode})`
      } else if (record.unitName) {
        return `${record.unitName}(${record.unitCode})`
      } else {
        return '-'
      }
    }
  },
  {
    title: '申报要素',
    key: 'declarationElements',
    width: 120
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right' as const,
    width: 180
  }
]

// 申报要素表格列配置
const elementColumns: any[] = [
  {
    title: '序号',
    dataIndex: 'key',
    key: 'key',
    width: 60
  },
  {
    title: '字段名称',
    dataIndex: 'label',
    key: 'label',
    width: 120
  },
  {
    title: '描述值',
    dataIndex: 'defaultValue',
    key: 'defaultValue',
    width: 120
  },
  {
    title: '可编辑',
    key: 'editable',
    width: 80
  },
  {
    title: '必填',
    key: 'required',
    width: 80
  },
  {
    title: '下拉选项',
    key: 'options',
    width: 180
  }
]

// 表单数据
const formData = reactive({
  hsCode: '',
  englishName: '',
  chineseName: '',
  unitCode: '',
  unitType: '',
  unitName: '',
  unitNameEn: '',
  elements: [] as any[]
})

// 表单验证规则
const formRules = {
  hsCode: [
    { required: true, message: '请输入HS编码' },
    { pattern: /^\d{10}$/, message: 'HS编码必须是10位数字' }
  ],
  chineseName: [{ required: true, message: '请输入中文名称' }]
}

// 加载商品列表
const loadProductList = async () => {
  try {
    loading.value = true
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      hsCode: searchForm.hsCode,
      productName: searchForm.productName
    }
    const response = await getProducts(params)
    if (response.data?.code === 200) {
      productList.value = response.data.data.records || []
      pagination.total = Number(response.data.data.total) || 0
    }
  } catch (error) {
    message.error('加载商品列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索处理
const handleSearch = () => {
  pagination.current = 1
  loadProductList()
}

// 重置搜索
const handleReset = () => {
  searchForm.hsCode = ''
  searchForm.productName = ''
  handleSearch()
}

// 表格分页变化
const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadProductList()
}

// 单位选择变化处理
const handleUnitChange = (value: any) => {
  if (!value) return
  const unit = unitOptions.value.find(u => u.unitCode === value)
  if (unit) {
    formData.unitCode = unit.unitCode
    formData.unitType = unit.unitType
    formData.unitName = unit.unitName
    formData.unitNameEn = unit.unitNameEn
    selectedUnitInfo.value = `${unit.unitName}/${unit.unitNameEn}(${unit.unitCode}) - ${unit.unitType}`
  }
}

// 打开新增弹窗
const openAddModal = () => {
  editingId.value = null
  resetFormData()
  modalVisible.value = true
}

// 打开编辑弹窗
const openEditModal = (record: any) => {
  editingId.value = record.id
  Object.assign(formData, {
    hsCode: record.hsCode,
    englishName: record.englishName,
    chineseName: record.chineseName,
    unitCode: record.unitCode || '',
    unitType: record.unitType || '',
    unitName: record.unitName || '',
    unitNameEn: record.unitNameEn || '',
    elements: record.elements ? record.elements.map((el: any) => ({
      ...el,
      optionsStr: el.options ? el.options.join(',') : '',
      editable: el.editable !== undefined ? el.editable : false,
      required: el.required || false
    })) : []
  })
  
  if (formData.unitCode) {
    const unit = unitOptions.value.find(u => u.unitCode === formData.unitCode)
    if (unit) selectedUnitInfo.value = `${unit.unitName}/${unit.unitNameEn}(${unit.unitCode}) - ${unit.unitType}`
  }
  modalVisible.value = true
}

// 关闭弹窗
const closeModal = () => {
  modalVisible.value = false
}

// 重置表单数据
const resetFormData = () => {
  Object.assign(formData, {
    hsCode: '',
    englishName: '',
    chineseName: '',
    unitCode: '',
    unitType: '',
    unitName: '',
    unitNameEn: '',
    elements: []
  })
  selectedUnitInfo.value = ''
}

// 添加申报要素
const addElement = () => {
  formData.elements.push({
    key: String(formData.elements.length + 1),
    label: '',
    type: 'text',
    defaultValue: '',
    placeholder: '',
    editable: false,
    required: false,
    optionsStr: ''
  })
}

// 删除申报要素
const removeElement = (index: number) => {
  formData.elements.splice(index, 1)
}

// 保存商品
const handleSave = async () => {
  try {
    await formRef.value?.validate()
    saving.value = true
    
    const elements = formData.elements.map(el => ({
      ...el,
      options: el.type === 'select' && el.optionsStr ? el.optionsStr.split(',').map((s: string) => s.trim()) : []
    }))

    const data = { ...formData, elements }

    let response
    if (editingId.value) {
      response = await updateProduct(editingId.value, data as any)
    } else {
      response = await addProduct(data as any)
    }
    
    if (response.data?.code === 200) {
      message.success('保存成功')
      closeModal()
      loadProductList()
    }
  } catch (error) {
    // 验证失败
  } finally {
    saving.value = false
  }
}

// 删除商品
const handleDelete = async (id: number) => {
  try {
    const response = await deleteProductApi(id)
    if (response.data?.code === 200) {
      message.success('删除成功')
      loadProductList()
    }
  } catch (error) {
    message.error('删除失败')
  }
}

// 查看申报要素
const viewElements = (record: any) => {
  currentElements.value = record.elements || []
  elementsModalVisible.value = true
}

onMounted(() => {
  loadProductList()
})
</script>

<style scoped>
/* 页面特有样式已由全局 index.less 覆盖 */

/* 申报要素编辑区域滚动条 */
.elements-container::-webkit-scrollbar {
  width: 6px;
}
.elements-container::-webkit-scrollbar-thumb {
  background: #CBD5E1;
  border-radius: 3px;
}
.elements-container::-webkit-scrollbar-thumb:hover {
  background: #94A3B8;
}
</style>

