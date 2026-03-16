<template>
  <div class="declaration-form-page">
    <a-card title="出口申报表单">
      <template #extra>
        <a-space>
          <a-button @click="goBack">返回列表</a-button>
          <!-- 只在草稿状态且非只读模式下显示提交按钮 -->
          <a-button v-if="!isReadonly && (!formStatus || formStatus === 0)" type="primary" @click="handleSubmit">提交申报</a-button>
        </a-space>
      </template>
      
      <!-- 基本信息 -->
      <a-card title="基本信息" size="small" class="section-card">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="申报单号">
              <a-input v-model:value="formData.formNo" placeholder="系统自动生成" readonly />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="申报日期">
              <a-date-picker v-model:value="formData.declarationDate" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="发货人公司名">
              <a-input v-model:value="formData.shipperCompany" placeholder="发货人公司名" :readonly="isReadonly" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="发货人地址">
              <a-input v-model:value="formData.shipperAddress" placeholder="发货人地址" :readonly="isReadonly" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="收货人公司名">
              <a-input v-model:value="formData.consigneeCompany" placeholder="收货人公司名" :readonly="isReadonly" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="收货人地址">
              <a-input v-model:value="formData.consigneeAddress" placeholder="收货人地址" :readonly="isReadonly" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="出发城市">
              <a-input v-model:value="formData.departureCity" placeholder="例如：SHANGHAI, CHINA" :readonly="isReadonly" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="到达地区">
              <a-input v-model:value="formData.destinationRegion" placeholder="例如：NEW YORK, USA" :readonly="isReadonly" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="运输方式">
              <a-select 
                v-model:value="formData.transportMode" 
                :options="transportModeOptions"
                placeholder="请选择运输方式" 
                :disabled="isReadonly"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="发票号">
              <a-input 
                v-model:value="formData.invoiceNo" 
                placeholder="请输入发票号，留空则自动生成(ZIYI-yy-mmdd格式)" 
                :readonly="isReadonly"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="币种">
              <a-select v-model:value="formData.currency" style="width: 100%" :disabled="isReadonly">
                <a-select-option value="USD">USD</a-select-option>
                <a-select-option value="EUR">EUR</a-select-option>
                <a-select-option value="CNY">CNY</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
      </a-card>
      
      <!-- 产品明细 -->
      <a-card title="产品明细" size="small" class="section-card">
        <template #extra>
          <a-button v-if="!isReadonly" type="primary" size="small" @click="addProduct">
            <template #icon><PlusOutlined /></template>
            添加产品
          </a-button>
        </template>
        
        <a-table 
          :dataSource="productList" 
          :columns="productColumns" 
          :pagination="false"
          rowKey="id"
          size="small"
          :scroll="{ x: 1200 }"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'productName'">
              <a-input 
                v-if="!isReadonly" 
                v-model:value="record.productName" 
                placeholder="产品名称"
              />
              <span v-else>{{ record.productName }}</span>
            </template>
            
            <template v-else-if="column.key === 'hsCode'">
              <a-select 
                v-if="!isReadonly"
                v-model:value="record.hsCode" 
                style="width: 100%"
                placeholder="选择HS编码"
                :options="hsOptions"
                show-search
                option-filter-prop="label"
                                                @change="(value) => {
                                                  if (value && typeof value === 'string') {
                                                    onHsCodeChange(index, value)
                                                  }
                                                }"
              />
              <span v-else>{{ record.hsCode }}</span>
            </template>
            
            <template v-else-if="column.key === 'quantity'">
              <a-input-number 
                v-if="!isReadonly"
                v-model:value="record.quantity" 
                :min="1"
                @change="() => calculateAmount(record)"
                style="width: 100%"
              />
              <span v-else>{{ record.quantity }}</span>
            </template>
            
            <template v-else-if="column.key === 'unit'">
              <a-select 
                v-if="!isReadonly"
                v-model:value="record.unit" 
                style="width: 100%"
                placeholder="单位"
              >
                <a-select-option value="PCS">PCS</a-select-option>
                <a-select-option value="KGS">KGS</a-select-option>
                <a-select-option value="CBM">CBM</a-select-option>
              </a-select>
              <span v-else>{{ record.unit }}</span>
            </template>
            
            <template v-else-if="column.key === 'unitPrice'">
              <a-input-number 
                v-if="!isReadonly"
                v-model:value="record.unitPrice" 
                :min="0"
                :step="0.01"
                @change="() => calculateAmount(record)"
                style="width: 100%"
              />
              <span v-else>{{ record.unitPrice }}</span>
            </template>
            
            <template v-else-if="column.key === 'grossWeight'">
              <a-input-number 
                v-if="!isReadonly"
                v-model:value="record.grossWeight" 
                :min="0"
                :step="0.001"
                style="width: 100%"
              />
              <span v-else>{{ record.grossWeight }}</span>
            </template>
            
            <template v-else-if="column.key === 'netWeight'">
              <a-input-number 
                v-if="!isReadonly"
                v-model:value="record.netWeight" 
                :min="0"
                :step="0.001"
                style="width: 100%"
              />
              <span v-else>{{ record.netWeight }}</span>
            </template>
            
            <template v-else-if="column.key === 'amount'">
              <span>{{ calculateAmount(record) }}</span>
            </template>
            
            <template v-else-if="column.key === 'declarationElements' && !isReadonly">
              <div v-if="record.declarationElements && record.declarationElements.length > 0">
                <a-button type="link" size="small" @click="toggleElements(index)">
                  {{ expandedElements[index] ? '收起要素' : '填写要素' }}
                </a-button>
                <div v-show="expandedElements[index]" style="margin-top: 10px; padding: 10px; background: #f8f9fa; border-radius: 4px;">
                  <a-row :gutter="12">
                    <a-col 
                      v-for="element in record.declarationElements" 
                      :key="element.key" 
                      :span="element.type === 'textarea' ? 24 : 12"
                      style="margin-bottom: 8px;"
                    >
                      <div style="font-size: 12px; margin-bottom: 4px; color: #666;">
                        {{ element.label }}{{ element.required ? '*' : '' }}
                      </div>
                      <!-- 文本输入框 -->
                      <a-input 
                        v-if="element.type === 'text' && element.editable"
                        v-model:value="element.value"
                        :placeholder="element.placeholder || `请输入${element.label}`"
                        size="small"
                      />
                      <!-- 数字输入框 -->
                      <a-input-number 
                        v-else-if="element.type === 'number' && element.editable"
                        v-model:value="element.value"
                        :placeholder="element.placeholder || `请输入${element.label}`"
                        size="small"
                        style="width: 100%"
                      />
                      <!-- 下拉选择框 -->
                      <a-select 
                        v-else-if="element.type === 'select' && element.editable"
                        v-model:value="element.value"
                        :placeholder="element.placeholder || `请选择${element.label}`"
                        size="small"
                        style="width: 100%"
                      >
                        <a-select-option 
                          v-for="option in getSelectOptions(element)" 
                          :key="option" 
                          :value="option"
                        >
                          {{ option }}
                        </a-select-option>
                      </a-select>
                      <!-- 文本域 -->
                      <a-textarea 
                        v-else-if="element.type === 'textarea' && element.editable"
                        v-model:value="element.value"
                        :placeholder="element.placeholder || `请输入${element.label}`"
                        :rows="2"
                        size="small"
                      />
                      <!-- 只读显示 -->
                      <div v-else style="padding: 4px 8px; background: #fff; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 12px; min-height: 24px; display: flex; align-items: center;">
                        <span v-if="element.value">{{ element.value }}</span>
                        <span v-else-if="element.defaultValue">{{ element.defaultValue }}</span>
                        <span v-else style="color: #ccc;">无默认值</span>
                      </div>
                    </a-col>
                  </a-row>
                </div>
              </div>
              <span v-else>-</span>
            </template>
            
            <template v-else-if="column.key === 'action'">
              <a-button v-if="!isReadonly" type="link" danger @click="removeProduct(index)">删除</a-button>
            </template>
          </template>
        </a-table>
        
        <!-- 总计信息 -->
        <div class="totals-section">
          <a-row :gutter="16">
            <a-col :span="4">
              <div class="total-item">
                <span class="total-label">总数量:</span>
                <span class="total-value">{{ totals.totalQuantity }}</span>
              </div>
            </a-col>
            <a-col :span="5">
              <div class="total-item">
                <span class="total-label">总毛重(KGS):</span>
                <span class="total-value">{{ totals.totalGrossWeight.toFixed(3) }}</span>
              </div>
            </a-col>
            <a-col :span="5">
              <div class="total-item">
                <span class="total-label">总净重(KGS):</span>
                <span class="total-value">{{ totals.totalNetWeight.toFixed(3) }}</span>
              </div>
            </a-col>
            <a-col :span="5">
              <div class="total-item">
                <span class="total-label">总体积(CBM):</span>
                <span class="total-value">{{ totals.totalVolume.toFixed(3) }}</span>
              </div>
            </a-col>
            <a-col :span="5">
              <div class="total-item">
                <span class="total-label">总金额({{ formData.currency }}):</span>
                <span class="total-value">{{ totals.totalAmount.toFixed(2) }}</span>
              </div>
            </a-col>
          </a-row>
        </div>
      </a-card>
      
      <!-- 箱子信息 -->
      <a-card title="箱子信息" size="small" class="section-card">
        <template #extra>
          <a-button v-if="!isReadonly" type="primary" size="small" @click="addCarton">
            <template #icon><PlusOutlined /></template>
            添加箱子
          </a-button>
        </template>
        
        <a-table 
          :dataSource="cartonList" 
          :columns="cartonColumns" 
          :pagination="false"
          rowKey="id"
          size="small"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'cartonNo'">
              <a-input 
                v-if="!isReadonly" 
                v-model:value="record.cartonNo" 
                placeholder="箱号"
              />
              <span v-else>{{ record.cartonNo }}</span>
            </template>
            
            <template v-else-if="column.key === 'quantity'">
              <a-input-number 
                v-if="!isReadonly"
                v-model:value="record.quantity" 
                :min="1"
                style="width: 100%"
              />
              <span v-else>{{ record.quantity }}</span>
            </template>
            
            <template v-else-if="column.key === 'volume'">
              <a-input-number 
                v-if="!isReadonly"
                v-model:value="record.volume" 
                :min="0"
                :step="0.001"
                style="width: 100%"
              />
              <span v-else>{{ record.volume }}</span>
            </template>
            
            <template v-else-if="column.key === 'selectedProducts'">
              <a-select 
                v-if="!isReadonly"
                v-model:value="record.selectedProducts" 
                mode="multiple"
                style="width: 100%"
                placeholder="选择产品"
                :options="productOptions"
              />
              <span v-else>{{ getSelectedProductNames(record.selectedProducts) }}</span>
            </template>
            
            <template v-else-if="column.key === 'action'">
              <a-button v-if="!isReadonly" type="link" danger @click="removeCarton(index)">删除</a-button>
            </template>
          </template>
        </a-table>
      </a-card>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { getDeclarationDetail, addDeclaration, updateDeclaration } from '@/api/business/declaration'
import { getProductTypes } from '@/api/system/product'
import { getTransportModes } from '@/api/system/config'

const route = useRoute()
const router = useRouter()

// 页面状态
const isReadonly = ref(route.query.readonly === 'true')
const formId = ref(route.query.id ? Number(route.query.id) : null)
const formStatus = ref<number | null>(null)  // 新增:用于存储申报单状态

const formData = reactive({
  formNo: '',
  shipperCompany: 'NINGBO ZIYI TECHNOLOGY CO.,LTD',
  shipperAddress: 'XIUFENG, GAOQIAO TOWN, HAISHU DISTRICT, NINGBO, ZHEJIANG, CHINA',
  consigneeCompany: '',
  consigneeAddress: '',
  invoiceNo: '',
  transportMode: undefined as string | undefined,
  departureCity: 'SHANGHAI, CHINA',
  destinationRegion: '',
  currency: 'USD',
  declarationDate: undefined as Dayjs | undefined
})

// 产品列表
const productList = ref<any[]>([])

// 箱子列表
const cartonList = ref<any[]>([])

// HS编码选项
const hsOptions = ref<any[]>([])

// 运输方式选项
const transportModeOptions = ref<any[]>([])

// 申报要素展开状态
const expandedElements = ref<Record<number, boolean>>({})

// 加载运输方式选项
const loadTransportModes = async () => {
  try {
    const response = await getTransportModes()
    if (response.data.code === 200 && response.data.data.length > 0) {
      transportModeOptions.value = response.data.data.map((item: any) => ({
        label: item.label,
        value: item.value
      }))
      console.log('加载运输方式成功:', transportModeOptions.value)
    }
  } catch (error) {
    console.warn('加载运输方式失败:', error)
    // 使用默认运输方式
    transportModeOptions.value = [
      { label: '海运', value: 'SEA' },
      { label: '空运', value: 'AIR' },
      { label: '陆运', value: 'LAND' },
      { label: '快递', value: 'EXPRESS' }
    ]
  }
}

// 加载HS商品类型数据
const loadProductTypes = async () => {
  try {
    const response = await getProductTypes()
    console.log('加载HS商品类型数据:', response.data)
    if (response.data.code === 200 && response.data.data.length > 0) {
      hsOptions.value = response.data.data.map((item: any) => ({
        label: `${item.hsCode} - ${item.englishName}`,
        value: item.hsCode
      }))
      console.log('加载HS商品类型成功:', hsOptions.value)
    } else {
      // 如果API失败，使用默认数据
      hsOptions.value = [
        { label: '39269090 - PLASTIC SEAL', value: '39269090' },
        { label: '85235210 - RFID STICKER', value: '85235210' },
        { label: '48239090 - HANGTAG', value: '48239090' },
        { label: '39232900 - Polybag', value: '39232900' }
      ]
    }
  } catch (error) {
    console.warn('加载HS商品类型失败，使用默认数据:', error)
    // 使用默认数据
    hsOptions.value = [
      { label: '39269090 - PLASTIC SEAL', value: '39269090' },
      { label: '85235210 - RFID STICKER', value: '85235210' },
      { label: '48239090 - HANGTAG', value: '48239090' },
      { label: '39232900 - Polybag', value: '39232900' }
    ]
  }
}

// 产品选项
const productOptions = computed(() => {
  return productList.value.map(item => ({
    label: item.productName,
    value: item.id
  }))
})

// 产品表格列配置
const productColumns = [
  { title: '产品名称', dataIndex: 'productName', key: 'productName', width: 150 },
  { title: 'HS编码', dataIndex: 'hsCode', key: 'hsCode', width: 150 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100 },
  { title: '毛重(KGS)', dataIndex: 'grossWeight', key: 'grossWeight', width: 120 },
  { title: '净重(KGS)', dataIndex: 'netWeight', key: 'netWeight', width: 120 },
  { title: '金额', key: 'amount', width: 120 },
  { title: '申报要素', key: 'declarationElements', width: 120 },
  { title: '操作', key: 'action', width: 80 }
]

// 箱子表格列配置
const cartonColumns = [
  { title: '箱号', dataIndex: 'cartonNo', key: 'cartonNo', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 100 },
  { title: '总体积(CBM)', dataIndex: 'volume', key: 'volume', width: 150 },
  { title: '产品选择', key: 'selectedProducts', width: 200 },
  { title: '操作', key: 'action', width: 80 }
]

// 计算总计
const totals = computed(() => {
  let totalQuantity = 0
  let totalGrossWeight = 0
  let totalNetWeight = 0
  let totalVolume = 0
  let totalAmount = 0
  
  productList.value.forEach(item => {
    totalQuantity += item.quantity || 0
    totalGrossWeight += (item.grossWeight || 0) 
    totalNetWeight += (item.netWeight || 0) 
    totalAmount += (item.unitPrice || 0) * (item.quantity || 0)
  })
  
  // 箱子总体积直接累加（因为输入的就是总体积）
  cartonList.value.forEach(carton => {
    totalVolume += (carton.volume || 0)
  })
  
  return {
    totalQuantity,
    totalGrossWeight,
    totalNetWeight,
    totalVolume,
    totalAmount
  }
})

// 计算单项金额
const calculateAmount = (record: any) => {
  const amount = (record.unitPrice || 0) * (record.quantity || 0)
  // 同时更新record中的amount字段
  record.amount = amount.toFixed(2)
  return record.amount
}

// 获取选中产品名称
const getSelectedProductNames = (selectedIds: number[]) => {
  if (!selectedIds || selectedIds.length === 0) return ''
  return selectedIds.map(id => {
    const product = productList.value.find(p => p.id === id)
    return product ? product.productName : ''
  }).join(', ')
}

// HS编码变更处理
const onHsCodeChange = async (index: number, value: string | number) => {
  const stringValue = typeof value === 'string' ? value : String(value)
  const option = hsOptions.value.find(opt => opt.value === stringValue)
  
  if (option) {
    // 设置产品名称
    const productName = option.label.split(' - ')[1]
    productList.value[index].productName = productName
    
    // 加载对应的申报要素
    try {
      const response = await getProductTypes()
      console.log('API响应数据:', response)
      
      // 检查响应数据结构
      let productTypes = []
      if (response.data && Array.isArray(response.data)) {
        productTypes = response.data
      } else if (response.data && response.data.data && Array.isArray(response.data.data)) {
        productTypes = response.data.data
      } else if (response.data && response.data.records && Array.isArray(response.data.records)) {
        productTypes = response.data.records
      }
      
      console.log('处理后的商品类型数据:', productTypes)
      
      const productType = productTypes.find((item: any) => item.hsCode === stringValue)
      console.log('匹配的商品类型:', productType)
      
      if (productType) {
        // 解析申报要素配置
        let elements = []
        if (productType.elements && productType.elements.length > 0) {
          elements = productType.elements
        } else if (productType.elementsConfig) {
          try {
            // 解析elementsConfig JSON字符串
            elements = JSON.parse(productType.elementsConfig)
            console.log('解析elementsConfig:', elements)
          } catch (parseError) {
            console.error('解析elementsConfig失败:', parseError)
            elements = []
          }
        }
        
        if (elements.length > 0) {
          // 初始化申报要素
          productList.value[index].declarationElements = elements.map((element: any) => ({
            ...element,
            value: element.defaultValue || element.value || ''
          }))
          console.log('设置申报要素:', productList.value[index].declarationElements)
          
          // 测试数据结构
          testDataStructure(productList.value[index].declarationElements)
        } else {
          productList.value[index].declarationElements = []
          console.log('该商品无申报要素配置')
        }
      } else {
        productList.value[index].declarationElements = []
        console.log('未找到匹配的商品类型')
      }
    } catch (error) {
      console.error('加载申报要素失败:', error)
      productList.value[index].declarationElements = []
    }
  }
}

// 添加产品
const addProduct = () => {
  const newId = Math.max(...productList.value.map(p => p.id)) + 1
  productList.value.push({
    id: newId,
    productName: '',
    hsCode: '',
    quantity: 1,
    unit: 'PCS',
    unitPrice: 0,
    amount: '0.00', // 初始化金额字段
    grossWeight: 0,
    netWeight: 0,
    declarationElements: [] // 添加申报要素字段
  })
}

// 删除产品
const removeProduct = (index: number) => {
  productList.value.splice(index, 1)
}

// 添加箱子
const addCarton = () => {
  // 获取当前最大的箱子ID，确保编号连续
  let maxId = 0
  if (cartonList.value.length > 0) {
    maxId = Math.max(...cartonList.value.map(c => c.id))
  }
  const newId = maxId + 1
  
  cartonList.value.push({
    id: newId,
    cartonNo: `CTN${String(newId).padStart(3, '0')}`,
    quantity: 1,
    volume: 0,
    selectedProducts: []
  })
  
  console.log('添加箱子:', { newId, cartonNo: `CTN${String(newId).padStart(3, '0')}`, totalCartons: cartonList.value.length })
}

// 删除箱子
const removeCarton = (index: number) => {
  const removedCarton = cartonList.value[index]
  cartonList.value.splice(index, 1)
  console.log('删除箱子:', { removedId: removedCarton.id, removedCartonNo: removedCarton.cartonNo, remainingCartons: cartonList.value.length })
}

// 返回列表
const goBack = () => {
  router.push('/declaration/manage')
}

// 提交申报
const handleSubmit = async () => {
  try {
    // 验证是否为只读模式(已提交的申报单)
    if (isReadonly.value) {
      message.warning('该申报单已提交，无法编辑')
      return
    }
    
    // 验证必填字段
    if (!formData.consigneeCompany) {
      message.error('请填写收货人公司名')
      return
    }
    
    if (!formData.consigneeAddress) {
      message.error('请填写收货人地址')
      return
    }
    
    if (productList.value.length === 0) {
      message.error('请至少添加一个产品')
      return
    }
    
    // 确保所有产品的金额都已计算
    productList.value.forEach(product => {
      if (product.unitPrice !== undefined && product.quantity !== undefined) {
        product.amount = (product.unitPrice * product.quantity).toFixed(2)
      } else {
        product.amount = '0.00'
      }
    })
    
    // 构建箱子产品关联数据
    const cartonProducts: Array<{cartonId: number, productId: number, quantity: number}> = []
    cartonList.value.forEach(carton => {
      if (carton.selectedProducts && carton.selectedProducts.length > 0) {
        carton.selectedProducts.forEach((productId: number) => {
          // 找到对应的产品
          const product = productList.value.find(p => p.id === productId)
          if (product) {
            cartonProducts.push({
              cartonId: carton.id, // 注意：这里暂时用临时ID，后端需要处理
              productId: productId,
              quantity: product.quantity // 使用产品数量
            })
          }
        })
      }
    })
    
    console.log('箱子产品关联数据:', cartonProducts)
    
    // 构造提交数据
    const submitData = {
      ...formData,
      totalQuantity: totals.value.totalQuantity,
      totalGrossWeight: totals.value.totalGrossWeight,
      totalNetWeight: totals.value.totalNetWeight,
      totalVolume: totals.value.totalVolume,
      totalAmount: totals.value.totalAmount,
      status: 1, // 已提交状态
      products: productList.value.map((product: any) => ({
        ...product,
        // 将 declarationElements 转换为 elementValues (后端需要的格式)
        elementValues: (product.declarationElements || [])
          .filter((elem: any) => elem.value && elem.value.trim())
          .map((elem: any) => ({
            elementName: elem.label,
            elementValue: elem.value
          })),
        // 移除 declarationElements (后端不需要)
        declarationElements: undefined
      })),
      cartons: cartonList.value,
      cartonProducts: cartonProducts // 添加箱子产品关联数据
    }
    
    console.log('提交的数据:', submitData)
    
    if (formId.value) {
      // 更新
      await updateDeclaration(formId.value, submitData)
      message.success('申报单更新成功')
    } else {
      // 新增
      await addDeclaration(submitData)
      message.success('申报单提交成功')
    }
    
    goBack()
  } catch (error) {
    console.error('提交失败:', error)
    message.error('提交失败')
  }
}

// 加载数据
const loadData = async () => {
  // 并行加载配置数据
  await Promise.all([
    loadProductTypes(),
    loadTransportModes()
  ])
  
  if (formId.value) {
    try {
      const response = await getDeclarationDetail(formId.value)
      console.log('=== 申报单详情API响应 ===', response)
      console.log('response.data:', response.data)
      
      // 处理返回的数据
      if (response.data && response.data.code === 200 && response.data.data) {
        const detailData = response.data.data
        console.log('申报单数据:', detailData)
        console.log('产品列表:', detailData.products)
        console.log('箱子列表:', detailData.cartons)
        
        // 更新只读状态：如果申报单已提交(status >= 1)，则设为只读
        const submittedStatus = detailData.status || 0
        if (submittedStatus >= 1) {
          isReadonly.value = true
          formStatus.value = submittedStatus
          console.log('声明: 该申报单已提交(status=' + submittedStatus + '), 设置为只读模式')
          
          // 即使用户通过URL进入编辑页,也弹出警告
          message.warning('该申报单已提交(status=' + submittedStatus + '), 已自动切换为只读模式')
        }
        
        // 填充基本表单数据
        formData.formNo = detailData.formNo || ''
        formData.shipperCompany = detailData.shipperCompany || 'NINGBO ZIYI TECHNOLOGY CO.,LTD'
        formData.shipperAddress = detailData.shipperAddress || 'XIUFENG, GAOQIAO TOWN, HAISHU DISTRICT, NINGBO, ZHEJIANG, CHINA'
        formData.consigneeCompany = detailData.consigneeCompany || ''
        formData.consigneeAddress = detailData.consigneeAddress || ''
        formData.invoiceNo = detailData.invoiceNo || ''
        formData.transportMode = detailData.transportMode
        formData.departureCity = detailData.departureCity || 'SHANGHAI, CHINA'
        formData.destinationRegion = detailData.destinationRegion || ''
        formData.currency = detailData.currency || 'USD'
        formData.declarationDate = detailData.declarationDate ? dayjs(detailData.declarationDate) : undefined
        
        // 填充产品列表
        if (detailData.products && Array.isArray(detailData.products)) {
          productList.value = detailData.products.map((product: any) => ({
            ...product,
            // 处理申报要素值
            declarationElements: (product.elementValues || []).map((ev: any) => ({
              id: ev.id,
              productId: ev.productId,
              elementId: ev.elementId,
              label: ev.label,
              value: ev.value,
              type: ev.type,
              required: ev.required,
              options: ev.options,
              editable: true
            })),
            // 添加体积字段
            volume: product.volume || 0
          }))
          console.log('✅ 加载产品列表成功:', productList.value.length + ' 个产品')
          
          // 打印产品详情用于调试
          productList.value.forEach((p: any, idx: number) => {
            console.log(`产品 ${idx + 1}:`, {
              名称: p.productName,
              HS编码: p.hsCode,
              数量: p.quantity,
              单价: p.unitPrice,
              金额: p.amount,
              体积: p.volume,
              申报要素数量: p.declarationElements?.length || 0
            })
            if (p.declarationElements && p.declarationElements.length > 0) {
              console.log('  申报要素:', p.declarationElements)
            }
          })
        } else {
          console.warn('产品列表为空或不是数组:', detailData.products)
        }
        
        // 填充箱子列表
        if (detailData.cartons && Array.isArray(detailData.cartons)) {
          cartonList.value = detailData.cartons.map((carton: any) => ({
            ...carton,
            // 添加体积字段(如果后端没返回)
            volume: carton.volume || 0,
            // 从 cartonProducts 关联中提取选中的产品
            selectedProducts: (detailData.cartonProducts || [])
              .filter((cp: any) => cp.cartonId === carton.id)
              .map((cp: any) => cp.productId)
          }))
          console.log('✅ 加载箱子列表成功:', cartonList.value.length + ' 个箱子')
          
          // 打印箱子详情用于调试
          cartonList.value.forEach((c: any, idx: number) => {
            console.log(`箱子 ${idx + 1}:`, {
              箱号: c.cartonNo,
              数量: c.quantity,
              体积: c.volume,
              关联产品: c.selectedProducts
            })
          })
        } else {
          console.warn('箱子列表为空或不是数组:', detailData.cartons)
        }
        
        message.success('数据加载成功')
      } else {
        console.error('API返回异常:', response.data)
        message.error('获取申报单详情失败')
      }
    } catch (error: any) {
      console.error('❌ 加载申报单详情失败:', error)
      message.error('加载数据失败: ' + (error.message || '未知错误'))
    }
  }
  // 新增模式下不自动添加默认产品，让用户手动添加
}



// 测试申报要素数据结构
const testDataStructure = (elements: any[]) => {
  console.log('=== 申报要素数据结构测试 ===')
  elements.forEach((element, index) => {
    console.log(`要素 ${index + 1}:`, {
      label: element.label,
      type: element.type,
      editable: element.editable,
      required: element.required,
      hasOptions: !!element.options,
      optionsLength: element.options?.length || 0,
      hasOptionsStr: !!element.optionsStr,
      defaultValue: element.defaultValue,
      currentValue: element.value
    })
  })
}

// 获取下拉框选项
const getSelectOptions = (element: any) => {
  // 优先使用options数组（新版数据结构）
  if (element.options && Array.isArray(element.options)) {
    console.log('使用options数组:', element.label, element.options)
    return element.options
  }
  
  // 兼容旧版optionsStr字符串格式
  if (element.optionsStr) {
    try {
      const options = element.optionsStr.split(',').map((opt: string) => opt.trim()).filter((opt: string) => opt)
      console.log('解析optionsStr:', element.label, options)
      return options
    } catch (error) {
      console.error('解析optionsStr失败:', error)
    }
  }
  
  return []
}

// 切换申报要素展开状态
const toggleElements = (index: number) => {
  expandedElements.value[index] = !expandedElements.value[index]
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.declaration-form-page {
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 64px);
}

.section-card {
  margin-bottom: 20px;
}

.totals-section {
  margin-top: 20px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
}

.total-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.total-label {
  font-weight: 500;
  color: #666;
}

.total-value {
  font-weight: 600;
  color: #333;
  font-size: 16px;
}
</style>