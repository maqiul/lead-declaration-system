<template>
  <div class="product-maintenance">
    <!-- 搜索区域 -->
    <a-card class="search-card">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="HS编码">
          <a-input v-model:value="searchForm.hsCode" placeholder="请输入HS编码" />
        </a-form-item>
        <a-form-item label="商品名称">
          <a-input v-model:value="searchForm.productName" placeholder="请输入商品名称" />
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
          <template #icon><plus-outlined /></template>
          新增商品
        </a-button>
        <a-button @click="loadProductList">
          <template #icon><reload-outlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card>

      <a-table
        :dataSource="productList"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'declarationElements'">
            <a-button type="link" size="small" @click="viewElements(record)">
              查看申报要素
            </a-button>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditModal(record)">
                编辑
              </a-button>
              <a-popconfirm
                title="确定要删除该商品吗？"
                @confirm="handleDelete((record as any).id)"
              >
                <a-button type="link" danger size="small">
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
      :width="800"
      :confirm-loading="saving"
      @ok="handleSave"
      @cancel="closeModal"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 20 }"
      >
        <a-form-item label="HS编码" name="hsCode">
          <a-input
            v-model:value="formData.hsCode"
            placeholder="请输入10位HS编码"
            :maxlength="10"
          />
        </a-form-item>

        <a-form-item label="英文名称" name="englishName">
          <a-input
            v-model:value="formData.englishName"
            placeholder="请输入英文名称"
          />
        </a-form-item>

        <a-form-item label="中文名称" name="chineseName">
          <a-input
            v-model:value="formData.chineseName"
            placeholder="请输入中文名称"
          />
        </a-form-item>

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
              <span style="color: #999; margin-left: 8px;">{{ unit.unitType }}</span>
            </a-select-option>
          </a-select>
          <div style="margin-top: 4px; font-size: 12px; color: #666;">
            当前选择：{{ selectedUnitInfo }}
          </div>
        </a-form-item>

        <a-form-item label="申报要素">
          <div class="elements-container">
            <div
              v-for="(element, index) in formData.elements"
              :key="index"
              class="element-item"
            >
              <a-row :gutter="8" align="middle">
                <a-col :span="2">
                  <a-input v-model:value="element.key" placeholder="序号" size="small" />
                </a-col>
                <a-col :span="4">
                  <a-input v-model:value="element.label" placeholder="标签" size="small" />
                </a-col>
                <a-col :span="3">
                  <a-select v-model:value="element.type" placeholder="类型" size="small">
                    <a-select-option value="text">文本</a-select-option>
                    <a-select-option value="select">下拉选择</a-select-option>
                    <a-select-option value="checkbox">复选框</a-select-option>
                  </a-select>
                </a-col>
                <a-col :span="5">
                  <a-input v-model:value="element.defaultValue" placeholder="默认值" size="small" />
                </a-col>
                <a-col :span="4">
                  <a-input v-model:value="element.placeholder" placeholder="提示文字" size="small" />
                </a-col>
                <a-col :span="3">
                  <a-checkbox v-model:checked="element.editable">可编辑</a-checkbox>
                </a-col>
                <a-col :span="2">
                  <a-checkbox v-model:checked="element.required">必填</a-checkbox>
                </a-col>
                <a-col :span="1">
                  <a-button type="link" danger size="small" @click="removeElement(index)">
                    <delete-outlined />
                  </a-button>
                </a-col>
              </a-row>
              <a-row v-if="element.type === 'select'" :gutter="8" style="margin-top: 8px;">
                <a-col :span="24">
                  <a-input
                    v-model:value="element.optionsStr"
                    placeholder="选项（用逗号分隔，如：是,否）"
                    size="small"
                  />
                </a-col>
              </a-row>
            </div>
            <a-button type="dashed" block @click="addElement">
              <plus-outlined /> 添加申报要素
            </a-button>
            <div style="margin-top: 12px; font-size: 12px; color: #8c8c8c;">
              说明：勾选"可编辑"的字段会在申报时显示给用户填写，未勾选的字段使用默认值且不可修改
            </div>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 查看申报要素弹窗 -->
    <a-modal
      v-model:open="elementsModalVisible"
      title="申报要素详情"
      :footer="null"
      :width="700"
    >
      <a-table
        :dataSource="currentElements"
        :columns="elementColumns"
        :pagination="false"
        size="small"
        rowKey="key"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'editable'">
            <a-tag :color="(record as any).editable ? 'green' : 'default'">
              {{ (record as any).editable ? '可编辑' : '只读' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'required'">
            <a-tag :color="(record as any).required ? 'red' : 'default'">
              {{ (record as any).required ? '必填' : '选填' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'options'">
            <div v-if="(record as any).type === 'select' && (record as any).options">
              <a-tag v-for="opt in (record as any).options" :key="opt" color="blue">{{ opt }}</a-tag>
            </div>
            <span v-else>-</span>
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
  DeleteOutlined
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
    title: '英文名称',
    dataIndex: 'englishName',
    key: 'englishName',
    width: 250
  },
  {
    title: '中文名称',
    dataIndex: 'chineseName',
    key: 'chineseName',
    width: 150
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
    width: 120,
    slots: { customRender: 'declarationElements' }
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
    width: 150,
    slots: { customRender: 'action' }
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
    title: '类型',
    dataIndex: 'type',
    key: 'type',
    width: 80
  },
  {
    title: '默认值',
    dataIndex: 'defaultValue',
    key: 'defaultValue',
    width: 120
  },
  {
    title: '可编辑',
    key: 'editable',
    width: 80,
    slots: { customRender: 'editable' }
  },
  {
    title: '必填',
    key: 'required',
    width: 80,
    slots: { customRender: 'required' }
  },
  {
    title: '选项',
    key: 'options',
    width: 150,
    slots: { customRender: 'options' }
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
  englishName: [
    { required: true, message: '请输入英文名称' }
  ],
  chineseName: [
    { required: true, message: '请输入中文名称' }
  ]
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
  pagination.current = 1
  loadProductList()
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
  const selectedUnit = unitOptions.value.find(unit => unit.unitCode === value)
  if (selectedUnit) {
    formData.unitCode = selectedUnit.unitCode
    formData.unitType = selectedUnit.unitType
    formData.unitName = selectedUnit.unitName
    formData.unitNameEn = selectedUnit.unitNameEn
    selectedUnitInfo.value = `${selectedUnit.unitName}/${selectedUnit.unitNameEn}(${selectedUnit.unitCode}) - ${selectedUnit.unitType}`
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
  formData.hsCode = record.hsCode
  formData.englishName = record.englishName
  formData.chineseName = record.chineseName
  formData.unitCode = record.unitCode || ''
  formData.unitType = record.unitType || ''
  formData.unitName = record.unitName || ''
  formData.unitNameEn = record.unitNameEn || ''
  
  // 更新选中单位信息显示
  if (record.unitCode) {
    const selectedUnit = unitOptions.value.find(unit => unit.unitCode === record.unitCode)
    if (selectedUnit) {
      selectedUnitInfo.value = `${selectedUnit.unitName}/${selectedUnit.unitNameEn}(${selectedUnit.unitCode}) - ${selectedUnit.unitType}`
      // 同步更新表单数据
      formData.unitType = selectedUnit.unitType
      formData.unitName = selectedUnit.unitName
      formData.unitNameEn = selectedUnit.unitNameEn
    }
  } else {
    selectedUnitInfo.value = ''
  }
  // 解析申报要素
  if (record.elements && record.elements.length > 0) {
    formData.elements = record.elements.map((el: any) => ({
      key: el.key,
      label: el.label,
      type: el.type,
      defaultValue: el.defaultValue || el.value,  // 兼容旧数据
      placeholder: el.placeholder,
      editable: el.editable !== undefined ? el.editable : false,  // 默认不可编辑
      optionsStr: el.options ? el.options.join(',') : '',
      required: el.required || false
    }))
  } else {
    formData.elements = []
  }
  modalVisible.value = true
}

// 关闭弹窗
const closeModal = () => {
  modalVisible.value = false
  resetFormData()
}

// 重置表单数据
const resetFormData = () => {
  formData.hsCode = ''
  formData.englishName = ''
  formData.chineseName = ''
  formData.unitCode = ''
  formData.unitType = ''
  formData.unitName = ''
  formData.unitNameEn = ''
  formData.elements = []
  selectedUnitInfo.value = ''
  formRef.value?.resetFields()
}

// 添加申报要素
const addElement = () => {
  formData.elements.push({
    key: String(formData.elements.length),
    label: '',
    type: 'text',
    defaultValue: '',
    placeholder: '',
    editable: false,  // 默认不可编辑
    options: [],
    optionsStr: '',
    required: false
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
    
    // 处理申报要素
    const elements = formData.elements.map(el => ({
      key: el.key,
      label: el.label,
      type: el.type,
      value: el.defaultValue,  // 使用defaultValue作为value
      defaultValue: el.defaultValue,
      placeholder: el.placeholder,
      editable: el.editable,  // 保留editable字段
      options: el.type === 'select' && el.optionsStr ? el.optionsStr.split(',').map((s: string) => s.trim()) : undefined,
      required: el.required
    }))

    const data = {
      hsCode: formData.hsCode,
      englishName: formData.englishName,
      chineseName: formData.chineseName,
      elements: elements,
      status: 1,
      sort: 0
    }

    let response
    if (editingId.value) {
      response = await updateProduct(editingId.value, data)
    } else {
      response = await addProduct(data)
    }
    if (response.data?.code === 200) {
      message.success(editingId.value ? '更新成功' : '新增成功')
      closeModal()
      loadProductList()
    } else {
      message.error(response.data?.message || '保存失败')
    }
  } catch (error) {
    console.error('保存失败', error)
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
    } else {
      message.error(response.data?.message || '删除失败')
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
.product-maintenance {
  height: 100%;
  overflow-x: hidden;
}

.search-card {
  margin-bottom: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

.operation-card {
  margin-bottom: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

:deep(.ant-card) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.ant-card-body) {
  padding: 24px;
}

:deep(.ant-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 600;
}

:deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

:deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
}

:deep(.ant-input-affix-wrapper:focus),
:deep(.ant-input:focus) {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
  border-color: #667eea;
}

:deep(.ant-select-focused:not(.ant-select-disabled).ant-select:not(.ant-select-customize-input) .ant-select-selector) {
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
  border-color: #667eea;
}

/* HS商品维护页面特有样式 */
.elements-container {
  .element-item {
    padding: 12px;
    margin-bottom: 8px;
    background: #fafafa;
    border-radius: 4px;
    border: 1px solid #e8e8e8;
  }
}

/* 响应式表单布局 */
@media (max-width: 768px) {
  .product-maintenance {
    padding: 16px;
  }
  
  :deep(.ant-card-body) {
    padding: 16px;
  }
  
  :deep(.ant-form-item) {
    margin-bottom: 12px;
  }
}
</style>
