<template>
  <div class="declaration-form">
    <a-card title="出口申报表单">
      <a-form :model="formData" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        
        <!-- 基本信息部分 -->
        <a-card title="基本信息" size="small" style="margin-bottom: 20px;">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="发货人公司名">
                <a-input v-model:value="formData.shipperCompany" placeholder="发货人公司名" readonly />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="发货人地址">
                <a-input v-model:value="formData.shipperAddress" placeholder="发货人地址" :rows="3" readonly />
              </a-form-item>
            </a-col>
          </a-row>
          
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="收货人公司名">
                <a-input v-model:value="formData.consigneeCompany" placeholder="请输入收货人公司名" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="收货人地址">
                <a-input v-model:value="formData.consigneeAddress" placeholder="请输入收货人地址" />
              </a-form-item>
            </a-col>
          </a-row>
          
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="发票号码">
                <a-input v-model:value="formData.invoiceNo" placeholder="自动生成" disabled />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="日期">
                <a-date-picker v-model:value="formData.date" style="width: 100%" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="运输方式">
                <a-select v-model:value="formData.transportMode">
                  <a-select-option value="TRUCK">卡车</a-select-option>
                  <a-select-option value="SHIP">海运</a-select-option>
                  <a-select-option value="AIR">空运</a-select-option>
                  <a-select-option value="RAIL">铁路</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="出发城市">
                <a-input v-model:value="formData.departureCity" placeholder="例如: SHANGHAI, CHINA" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="到达地区">
                <a-input v-model:value="formData.destinationRegion" placeholder="例如: MOSCOW, RUSSIA" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>



        <!-- 箱子信息部分 -->
        <a-card title="箱子信息" size="small" style="margin-bottom: 20px;">
          <a-table 
            :dataSource="cartonList" 
            :columns="cartonColumns" 
            :pagination="false"
            size="small"
            rowKey="id"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'cartonNo'">
                <a-input v-model:value="record.cartonNo" placeholder="箱号" />
              </template>
              <template v-else-if="column.key === 'quantity'">
                <a-input-number 
                  v-model:value="record.quantity" 
                  :min="1"
                  placeholder="箱子数量"
                />
              </template>
              <template v-else-if="column.key === 'volume'">
                <a-input-number 
                  v-model:value="record.volume" 
                  :min="0"
                  :step="0.01"
                  addon-after="CBM"
                />
              </template>
              <template v-else-if="column.key === 'products'">
                <div>
                  <!-- 调试信息 -->
                  <div style="font-size: 12px; color: #666; margin-bottom: 5px;">
                    产品选项数量: {{ productOptions.length }}
                  </div>
                  <a-select
                    v-model:value="record.selectedProducts"
                    mode="multiple"
                    placeholder="选择装箱产品"
                    :options="productOptions"
                    style="width: 100%"
                  />
                </div>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" danger @click="removeCarton(index)">删除</a-button>
              </template>
            </template>
          </a-table>
          
          <div style="margin-top: 10px;">
            <a-button type="dashed" block @click="addCarton">
              <plus-outlined /> 添加箱子
            </a-button>
          </div>
        </a-card>

        <!-- 产品明细部分 -->
        <a-card title="产品明细" size="small" style="margin-bottom: 20px;">
          <a-table 
            :dataSource="productList" 
            :columns="productColumns" 
            :pagination="false"
            size="small"
            rowKey="id"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'productName'">
                <span>{{ record.productName }}</span>
              </template>
              <template v-else-if="column.key === 'hsCode'">
                <a-select 
                  v-model:value="record.hsCode" 
                  show-search
                  placeholder="选择HS编码"
                  :options="hsOptions"
                  :dropdown-style="{ minWidth: '200px' }"
                  @change="(value) => handleHsCodeChange(value, index)"
                />
              </template>
              <template v-else-if="column.key === 'declarationElements'">
                <div v-if="record.declarationElements && record.declarationElements.length > 0">
                  <div v-for="(element, eleIndex) in record.declarationElements" :key="eleIndex" style="margin-bottom: 5px">
                    <span style="font-weight: bold; font-size: 12px;">{{ element.name }}:</span>
                    <a-input 
                      v-model:value="element.value" 
                      :placeholder="element.description"
                      size="small"
                      style="margin-left: 8px; width: calc(100% - 80px)"
                    />
                  </div>
                </div>
                <span v-else style="color: #999; font-size: 12px;">请选择HS编码</span>
              </template>
              <template v-else-if="column.key === 'quantity'">
                <a-input-number 
                  v-model:value="record.quantity" 
                  :min="1"
                  @change="calculateAmount(index)"
                />
              </template>
              <template v-else-if="column.key === 'unit'">
                <a-select v-model:value="record.unit" style="width: 100px">
                  <a-select-option value="PCS">PCS</a-select-option>
                  <a-select-option value="SET">SET</a-select-option>
                  <a-select-option value="KG">KG</a-select-option>
                </a-select>
              </template>
              <template v-else-if="column.key === 'grossWeight'">
                <a-input-number 
                  v-model:value="record.grossWeight" 
                  :min="0"
                  :step="0.1"
                  addon-after="KGS"
                />
              </template>
              <template v-else-if="column.key === 'netWeight'">
                <a-input-number 
                  v-model:value="record.netWeight" 
                  :min="0"
                  :step="0.1"
                  addon-after="KGS"
                />
              </template>
              <template v-else-if="column.key === 'unitPrice'">
                <a-input-number 
                  v-model:value="record.unitPrice" 
                  :min="0"
                  :step="0.01"
                  @change="calculateAmount(index)"
                />
              </template>
              <template v-else-if="column.key === 'amount'">
                <a-input v-model:value="record.amount" readonly />
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" danger @click="removeProduct(index)">删除</a-button>
              </template>
            </template>
          </a-table>
          
          <div style="margin-top: 10px;">
            <a-button type="dashed" block @click="addProduct">
              <plus-outlined /> 添加产品
            </a-button>
          </div>
        </a-card>

        <!-- 包装信息部分 -->
        <a-card title="包装信息" size="small" style="margin-bottom: 20px;">
          <a-row :gutter="16">
            <a-col :span="6">
              <a-form-item label="总箱数">
                <a-input-number v-model:value="formData.totalCartons" :min="1" />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="总毛重(KGS)">
                <a-input v-model:value="formData.totalGrossWeight" readonly />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="总净重(KGS)">
                <a-input v-model:value="formData.totalNetWeight" readonly />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="总体积(CBM)">
                <a-input v-model:value="formData.totalVolume" readonly />
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>

        <!-- 金额汇总部分 -->
        <a-card title="金额汇总" size="small">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="总金额">
                <a-input v-model:value="formData.totalAmount" readonly />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="币种">
                <a-select v-model:value="formData.currency" style="width: 100px">
                  <a-select-option value="USD">USD</a-select-option>
                  <a-select-option value="EUR">EUR</a-select-option>
                  <a-select-option value="CNY">CNY</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="付款方式">
                <a-select v-model:value="formData.paymentTerms">
                  <a-select-option value="T/T">电汇(T/T)</a-select-option>
                  <a-select-option value="L/C">信用证(L/C)</a-select-option>
                  <a-select-option value="D/P">付款交单(D/P)</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>

        <!-- 操作按钮 -->
        <div style="text-align: center; margin-top: 20px;">
          <a-space>
            <a-button @click="resetForm">重置</a-button>
            <a-button type="primary" @click="generateInvoice">生成发票</a-button>
            <a-button type="primary" @click="exportExcel">导出Excel</a-button>
            <a-button type="default" @click="saveToDatabase">保存到数据库</a-button>
            <a-button type="default" @click="goToHistory">查看历史记录</a-button>
          </a-space>
        </div>
      </a-form>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import * as XLSX from 'xlsx'
import { getEnabledProducts } from '@/api/system/product'

// 保存申报单到数据库
const saveToDatabase = async () => {
  try {
    // 准备保存的数据
    const saveData = {
      shipperCompany: formData.shipperCompany,
      shipperAddress: formData.shipperAddress,
      consigneeCompany: formData.consigneeCompany,
      consigneeAddress: formData.consigneeAddress,
      invoiceNo: formData.invoiceNo,
      declarationDate: formData.date ? new Date(formData.date).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
      transportMode: formData.transportMode,
      departureCity: formData.departureCity,
      destinationRegion: formData.destinationRegion,
      currency: formData.currency,
      totalQuantity: formData.totalQuantity || 0,
      totalAmount: parseFloat(formData.totalAmount) || 0,
      totalCartons: formData.totalCartons || 0,
      totalGrossWeight: parseFloat(formData.totalGrossWeight) || 0,
      totalNetWeight: parseFloat(formData.totalNetWeight) || 0,
      totalVolume: parseFloat(formData.totalVolume) || 0,
      status: 0, // 草稿状态
      products: productList.value.map((product, index) => ({
        productName: product.productName,
        hsCode: product.hsCode,
        quantity: product.quantity,
        unit: product.unit,
        unitPrice: parseFloat(product.unitPrice) || 0,
        amount: parseFloat(product.amount) || 0,
        grossWeight: parseFloat(product.grossWeight) || 0,
        netWeight: parseFloat(product.netWeight) || 0,
        cartons: product.cartons || 1,
        volume: parseFloat(product.volume) || 0,
        sortOrder: index,
        elementValues: product.declarationElements ? product.declarationElements.map(element => ({
          elementName: element.name,
          elementValue: element.value
        })) : []
      })),
      cartons: cartonList.value.map((carton, index) => ({
        cartonNo: carton.cartonNo,
        quantity: carton.quantity,
        volume: parseFloat(carton.volume) || 0,
        totalVolume: (parseFloat(carton.volume) || 0) * (carton.quantity || 1),
        sortOrder: index
      }))
    }

    console.log('准备保存的数据:', saveData)
    
    // 调用后端接口保存
    const response = await fetch('/api/declaration/form/save', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(saveData)
    })
    
    const result = await response.json()
    console.log('保存结果:', result)
    
    if (result.code === 200) {
      message.success('保存成功！申报单号：' + result.data.formNo)
    } else {
      message.error('保存失败：' + result.msg)
    }
  } catch (error) {
    console.error('保存申报单失败:', error)
    message.error('保存失败：' + error.message)
  }
}

// 跳转到历史记录页面
const goToHistory = () => {
  // 跳转到历史记录页面
  window.location.hash = '#/demo/declaration-history'
  // 或者使用 router 跳转
  // router.push('/demo/declaration-history')
}

// 表单数据
const formData = reactive({
  // 发货人信息（默认值）
  shipperCompany: 'NINGBO ZIYI TECHNOLOGY CO.,LTD',
  shipperAddress: 'XIUFENG, GAOQIAO TOWN, HAISHU DISTRICT, NINGBO, ZHEJIANG, CHINA',
  // 收货人信息（需要手动输入）
  consigneeCompany: '',
  consigneeAddress: '',
  invoiceNo: 'ZIYI-25-1229',
  date: null,
  transportMode: 'TRUCK',
  departureCity: 'SHANGHAI, CHINA',
  destinationRegion: '',
  totalCartons: 6,
  totalGrossWeight: '93.16',
  totalNetWeight: '86.50',
  totalVolume: '0.182',
  totalAmount: '0.00',
  currency: 'USD',
  paymentTerms: 'T/T'
})

// 产品列表
const productList = ref([
  {
    id: 1,
    productName: 'PLASTIC SEAL',
    hsCode: '39269090',
    quantity: 2000,
    unit: 'PCS',
    grossWeight: 1.2,
    netWeight: 1.14,
    unitPrice: 0.05,
    amount: '100.00'
  },
  {
    id: 2,
    productName: 'RFID STICKER',
    hsCode: '85235210',
    quantity: 2500,
    unit: 'PCS',
    grossWeight: 2.68,
    netWeight: 2.56,
    unitPrice: 0.12,
    amount: '300.00'
  },
  {
    id: 3,
    productName: 'HANGTAG',
    hsCode: '48239090',
    quantity: 2000,
    unit: 'PCS',
    grossWeight: 8.34,
    netWeight: 7.96,
    unitPrice: 0.08,
    currency: 'USD',
    amount: '160.00'
  },
  {
    id: 4,
    productName: 'RFID STICKER',
    hsCode: '85235210',
    quantity: 80000,
    unit: 'PCS',
    grossWeight: 82.7,
    netWeight: 81.5,
    unitPrice: 0.10,
    currency: 'USD',
    amount: '8000.00'
  }
])

// 箱子列表
const cartonList = ref([
  {
    id: 1,
    cartonNo: 'CTN001',
    quantity: 1,
    volume: 0.09,
    selectedProducts: [1, 2, 3]
  },
  {
    id: 2,
    cartonNo: 'CTN002',
    quantity: 5,
    volume: 0.152,
    selectedProducts: [4]
  }
])

// HS商品数据
const hsProducts = ref([])
const hsOptions = ref([])

// 申报要素数据
const declarationElements = ref([])

// 产品表格列配置
const productColumns = [
  { title: '产品名称', dataIndex: 'productName', key: 'productName', width: 150 },
  { title: 'HS编码', dataIndex: 'hsCode', key: 'hsCode', width: 150 },
  { title: '申报要素', key: 'declarationElements', width: 200 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
  { title: '毛重(KGS)', dataIndex: 'grossWeight', key: 'grossWeight', width: 120 },
  { title: '净重(KGS)', dataIndex: 'netWeight', key: 'netWeight', width: 120 },
  { title: '单价(' + formData.currency + ')', dataIndex: 'unitPrice', key: 'unitPrice', width: 100 },
  { title: '金额(' + formData.currency + ')', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '操作', key: 'action', width: 80 }
]

// 产品选项（用于箱子选择产品）
const productOptions = computed(() => {
  const options = productList.value.map(product => ({
    label: `${product.productName} (${product.quantity} ${product.unit})`,
    value: product.id
  }))
  console.log('Product Options:', options) // 调试信息
  return options
})

// 箱子表格列配置
const cartonColumns = [
  { title: '箱号', dataIndex: 'cartonNo', key: 'cartonNo', width: 120 },
  { title: '箱子数量', dataIndex: 'quantity', key: 'quantity', width: 120 },
  { title: '单箱体积(CBM)', dataIndex: 'volume', key: 'volume', width: 150 },
  { title: '装箱产品', dataIndex: 'products', key: 'products', width: 250 },
  { title: '操作', key: 'action', width: 80 }
]

// 添加产品
const addProduct = () => {
  const newProduct = {
    id: Date.now(),
    productName: '',
    hsCode: '',
    declarationElements: [],
    quantity: 1,
    unit: 'PCS',
    grossWeight: 0,
    netWeight: 0,
    unitPrice: 0,
    amount: '0.00'
  }
  productList.value.push(newProduct)
  console.log('Added new product, productList length:', productList.value.length)
}

// 添加箱子
const addCarton = () => {
  const newCarton = {
    id: Date.now(),
    cartonNo: `CTN${String(cartonList.value.length + 1).padStart(3, '0')}`,
    quantity: 1,
    volume: 0,
    selectedProducts: []
  }
  cartonList.value.push(newCarton)
}

// 删除箱子
const removeCarton = (index) => {
  cartonList.value.splice(index, 1)
}

// 删除产品
const removeProduct = (index) => {
  productList.value.splice(index, 1)
  calculateTotals()
}

// HS编码变化处理
// 计算单项金额
const calculateAmount = (index) => {
  const product = productList.value[index]
  if (product.quantity && product.unitPrice) {
    product.amount = (product.quantity * product.unitPrice).toFixed(2)
  } else {
    product.amount = '0.00'
  }
  calculateTotals()
}

// 计算总计
const calculateTotals = () => {
  // 计算总金额
  let totalAmount = 0
  productList.value.forEach(product => {
    totalAmount += parseFloat(product.amount) || 0
  })
  formData.totalAmount = totalAmount.toFixed(2)

  // 计算总箱数（所有箱子数量之和）
  let totalCartons = 0
  cartonList.value.forEach(carton => {
    totalCartons += carton.quantity || 0
  })
  formData.totalCartons = totalCartons

  // 基于产品计算总重量（直接相加，因为是整体重量）
  let totalGrossWeight = 0
  let totalNetWeight = 0
  productList.value.forEach(product => {
    totalGrossWeight += (product.grossWeight || 0)
    totalNetWeight += (product.netWeight || 0)
  })
  
  // 计算总体积（箱子数量 × 单箱体积）
  let totalVolume = 0
  cartonList.value.forEach(carton => {
    totalVolume += (carton.quantity || 0) * (carton.volume || 0)
  })
  
  formData.totalGrossWeight = totalGrossWeight.toFixed(2)
  formData.totalNetWeight = totalNetWeight.toFixed(2)
  formData.totalVolume = totalVolume.toFixed(3)

  // 计算总数量
  let totalQuantity = 0
  productList.value.forEach(product => {
    totalQuantity += product.quantity || 0
  })
  
  // 这里可以添加更多的统计计算逻辑
}

// 重置表单
const resetForm = () => {
  productList.value = []
  cartonList.value = []
  Object.assign(formData, {
    shipper: '',
    consignee: '',
    invoiceNo: 'ZIYI-' + new Date().getFullYear() + '-' + Math.random().toString(36).substr(2, 4).toUpperCase(),
    date: null,
    transportMode: 'TRUCK',
    totalCartons: 0,
    totalGrossWeight: '0.00',
    totalNetWeight: '0.00',
    totalVolume: '0.00',
    totalAmount: '0.00',
    currency: 'USD',
    paymentTerms: 'T/T'
  })
  
  // 添加默认产品和箱子
  addProduct()
  addCarton()
}

// 生成发票
const generateInvoice = () => {
  message.success('发票生成成功！')
  // 这里可以调用后端API生成正式发票
}

// 导出Excel
const exportExcel = async () => {
  try {
    console.log('箱子列表数据:', cartonList.value)
    console.log('箱子列表长度:', cartonList.value.length)
    console.log('产品列表数据:', productList.value)
    console.log('导出时箱子数据:', cartonList.value.map(carton => ({
      cartonNo: carton.cartonNo,
      quantity: carton.quantity,
      volume: carton.volume,
      selectedProducts: carton.selectedProducts
    })))
    
    // 准备导出数据
    const exportData = {
      shipperCompany: formData.shipperCompany,
      shipperAddress: formData.shipperAddress,
      consigneeCompany: formData.consigneeCompany,
      consigneeAddress: formData.consigneeAddress,
      invoiceNo: formData.invoiceNo,
      date: formatDate(formData.date),
      transportMode: formData.transportMode,
      departureCity: formData.departureCity,
      destinationRegion: formData.destinationRegion,
      currency: formData.currency,
      products: productList.value.map(product => {
        // 获取产品所在的箱子信息
        const carton = cartonList.value.find(c => c.selectedProducts && c.selectedProducts.includes(product.id))
        return {
          productName: product.productName,
          hsCode: product.hsCode,  // 添加HS编码
          declarationElements: product.declarationElements || [],  // 添加申报要素
          quantity: product.quantity,
          unit: product.unit,
          unitPrice: product.unitPrice,
          amount: product.amount,
          grossWeight: product.grossWeight,
          netWeight: product.netWeight,
          cartons: getCartonCount(product.id),
          volume: getCartonVolume(product.id),
          // 添加箱子信息
          cartonInfo: carton ? {
            cartonNo: carton.cartonNo,
            cartonQuantity: carton.quantity,
            cartonVolume: carton.volume
          } : null
        }
      }),
      totalQuantity: productList.value.reduce((sum, product) => sum + product.quantity, 0),
      totalAmount: productList.value.reduce((sum, product) => sum + parseFloat(product.amount || 0), 0),
      totalAmountWords: formatTotalAmountWords(
        productList.value.reduce((sum, product) => sum + parseFloat(product.amount || 0), 0),
        formData.currency
      ),
      totalCartons: cartonList.value.reduce((sum, carton) => sum + (carton.quantity || 0), 0),
      totalGrossWeight: productList.value.reduce((sum, product) => sum + product.grossWeight, 0),
      totalNetWeight: productList.value.reduce((sum, product) => sum + product.netWeight, 0),
      totalVolume: cartonList.value.reduce((sum, carton) => sum + (carton.volume * (carton.quantity || 1)), 0)
    }
    
    console.log('准备发送的数据:', exportData)
    console.log('箱子数据是否为空:', !exportData.cartons || exportData.cartons.length === 0)
    
    // 调用后端接口下载文件
    const response = await fetch('/api/excel/export-documents', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(exportData)
    })
    
    console.log('响应状态:', response.status)
    console.log('响应头:', response.headers)
    
    if (response.ok) {
      // 获取文件名
      const contentDisposition = response.headers.get('Content-Disposition')
      let filename = 'Export_Documents.xlsx'
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
        if (filenameMatch && filenameMatch[1]) {
          filename = decodeURIComponent(filenameMatch[1].replace(/['"]/g, ''))
        }
      }
      
      // 下载文件
      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      
      message.success('Excel导出成功！')
    } else {
      const errorText = await response.text()
      message.error('导出失败：' + errorText)
    }
  } catch (error) {
    console.error('导出Excel失败:', error)
    message.error('导出Excel失败：' + error.message)
  }
}

// 获取产品对应的箱数
const getCartonCount = (productId) => {
  const carton = cartonList.value.find(c => c.selectedProducts && c.selectedProducts.includes(productId))
  return carton ? carton.quantity || 1 : 1
}

// 获取产品对应的单箱体积
const getCartonVolume = (productId) => {
  const carton = cartonList.value.find(c => c.selectedProducts && c.selectedProducts.includes(productId))
  return carton ? carton.volume || 0 : 0
}

// 格式化日期为 March 11, 2026 格式
const formatDate = (date) => {
  if (!date) return ''
  const months = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ]
  const d = new Date(date)
  const month = months[d.getMonth()]
  const day = d.getDate()
  const year = d.getFullYear()
  return `${month}. ${day}, ${year}`
}

// 数字转英文大写
const convertNumberToWords = (num) => {
  if (num === 0) return 'ZERO'
  
  const ones = ['', 'ONE', 'TWO', 'THREE', 'FOUR', 'FIVE', 'SIX', 'SEVEN', 'EIGHT', 'NINE']
  const teens = ['TEN', 'ELEVEN', 'TWELVE', 'THIRTEEN', 'FOURTEEN', 'FIFTEEN', 'SIXTEEN', 'SEVENTEEN', 'EIGHTEEN', 'NINETEEN']
  const tens = ['', '', 'TWENTY', 'THIRTY', 'FORTY', 'FIFTY', 'SIXTY', 'SEVENTY', 'EIGHTY', 'NINETY']
  const scales = ['', 'THOUSAND', 'MILLION', 'BILLION']
  
  const convertHundreds = (n) => {
    let result = ''
    if (n >= 100) {
      result += ones[Math.floor(n / 100)] + ' HUNDRED'
      n %= 100
      if (n > 0) result += ' '
    }
    if (n >= 20) {
      result += tens[Math.floor(n / 10)]
      if (n % 10 > 0) result += '-' + ones[n % 10]
    } else if (n >= 10) {
      result += teens[n - 10]
    } else if (n > 0) {
      result += ones[n]
    }
    return result
  }
  
  let result = ''
  let scaleIndex = 0
  
  while (num > 0) {
    const chunk = num % 1000
    if (chunk !== 0) {
      const chunkWords = convertHundreds(chunk)
      if (scaleIndex > 0) {
        result = chunkWords + ' ' + scales[scaleIndex] + ' ' + result
      } else {
        result = chunkWords
      }
    }
    num = Math.floor(num / 1000)
    scaleIndex++
  }
  
  return result.trim()
}

// 格式化总金额为 SAY ... USD ONLY 格式
const formatTotalAmountWords = (amount, currency) => {
  const words = convertNumberToWords(Math.floor(amount))
  const decimalPart = Math.round((amount - Math.floor(amount)) * 100)
  
  let result = 'SAY '
  
  if (words) {
    result += words
    if (decimalPart > 0) {
      result += ' AND '
      result += convertNumberToWords(decimalPart)
      result += ' CENTS'
    }
  } else if (decimalPart > 0) {
    result += convertNumberToWords(decimalPart) + ' CENTS'
  } else {
    result += 'ZERO'
  }
  
  result += ' ' + currency + ' ONLY'
  return result
}

// TODO: 后续实现Excel导出功能

// 加载HS商品数据
const loadHsProducts = async () => {
  try {
    const res = await getEnabledProducts()
    if (res.code === 200) {
      hsProducts.value = res.data || []
      // 转换为下拉选项格式
      hsOptions.value = hsProducts.value.map(item => ({
        label: `${item.hsCode} - ${item.englishName}`,
        value: item.hsCode,
        product: item
      }))
    }
  } catch (error) {
    console.error('加载HS商品数据失败:', error)
    message.error('加载HS商品数据失败')
  }
}

// 根据HS编码获取商品信息
const getProductByHsCode = (hsCode) => {
  return hsProducts.value.find(item => item.hsCode === hsCode)
}

// 获取申报要素
const getDeclarationElements = (hsCode) => {
  const product = getProductByHsCode(hsCode)
  if (product && product.elementsConfig) {
    try {
      return JSON.parse(product.elementsConfig)
    } catch (e) {
      console.error('解析申报要素失败:', e)
      return []
    }
  }
  return []
}

// 处理HS编码变更
const handleHsCodeChange = (value, index) => {
  const product = productList.value[index]
  if (value) {
    const hsProduct = getProductByHsCode(value)
    if (hsProduct) {
      // 自动填充产品名称
      product.productName = hsProduct.englishName
      // 设置默认单位
      product.unit = hsProduct.unitCode || 'PCS'
      // 获取申报要素
      const elements = getDeclarationElements(value)
      product.declarationElements = elements.map(element => ({
        ...element,
        value: ''
      }))
    }
  } else {
    product.productName = ''
    product.declarationElements = []
  }
  calculateAmount(index)
}

// 初始化
onMounted(() => {
  formData.invoiceNo = 'ZIYI-' + new Date().getFullYear() + '-' + Math.random().toString(36).substr(2, 4).toUpperCase()
  formData.date = new Date()
  calculateTotals()
  // 加载HS商品数据
  loadHsProducts()
})
</script>

<style scoped>
.declaration-form {
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 64px);
}

:deep(.ant-card) {
  margin-bottom: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

:deep(.ant-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 600;
}
</style>