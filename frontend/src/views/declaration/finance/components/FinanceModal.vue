<template>
  <a-modal
    v-model:visible="visible"
    :title="`财务单证 - ${formNo}`"
    width="1200px"
    :confirmLoading="saveLoading"
    @ok="handleSave"
    @cancel="handleClose"
    :footer="null"
  >
    <a-spin :spinning="loading">
      <!-- 财务补充表单内容 -->
      <a-form :model="formData" layout="vertical" :disabled="saveLoading">
        <!-- 币种基本信息 -->
        <a-row :gutter="16" class="mb-4">
          <a-col :span="24">
            <a-card size="small" title="基本信息">
              <a-form-item label="币种">
                <a-select 
                  v-model:value="formData.currency" 
                  placeholder="请选择币种" 
                  :readonly="!!formData.id" 
                  disabled
                >
                  <a-select-option value="CNY">人民币 (CNY)</a-select-option>
                  <a-select-option value="USD">美元 (USD)</a-select-option>
                  <a-select-option value="EUR">欧元 (EUR)</a-select-option>
                </a-select>
              </a-form-item>
            </a-card>
          </a-col>
        </a-row>

        <!-- 第一部分：发票上传 -->
        <a-divider orientation="left">发票上传</a-divider>
        <a-row :gutter="16" class="mb-4">
          <!-- 货代发票 -->
          <a-col :span="8">
            <a-card size="small" title="货代发票">
              <a-form-item label="货代金额">
                <a-input-number v-model:value="formData.freightAmount" style="width: 100%" :precision="2" placeholder="请输入金额" />
              </a-form-item>
              <a-form-item label="发票号">
                <a-input v-model:value="formData.freightInvoiceNo" placeholder="请输入发票号" />
              </a-form-item>
              <a-form-item label="附件">
                <div v-if="formData.freightFileUrl" class="mb-2">
                  <a :href="formData.freightFileUrl" target="_blank">
                    <DownloadOutlined /> {{ formData.freightFileName }}
                  </a>
                </div>
                <a-upload
                  :show-upload-list="false"
                  :customRequest="e => handleUpload(e, 'freight')"
                >
                  <a-button :loading="uploading.freight">
                    {{ formData.freightFileUrl ? '重新上传' : '上传发票' }}
                  </a-button>
                </a-upload>
              </a-form-item>
            </a-card>
          </a-col>

          <!-- 报关代理发票 -->
          <a-col :span="8">
            <a-card size="small" title="报关代理发票">
              <a-form-item label="报关代理金额">
                <a-input-number v-model:value="formData.customsAmount" style="width: 100%" :precision="2" placeholder="请输入金额" />
              </a-form-item>
              <a-form-item label="发票号">
                <a-input v-model:value="formData.customsInvoiceNo" placeholder="请输入发票号" />
              </a-form-item>
              <a-form-item label="附件">
                <div v-if="formData.customsFileUrl" class="mb-2">
                  <a :href="formData.customsFileUrl" target="_blank">
                    <DownloadOutlined /> {{ formData.customsFileName }}
                  </a>
                </div>
                <a-upload
                  :show-upload-list="false"
                  :customRequest="e => handleUpload(e, 'customs')"
                >
                  <a-button :loading="uploading.customs">
                    {{ formData.customsFileUrl ? '重新上传' : '上传发票' }}
                  </a-button>
                </a-upload>
              </a-form-item>
            </a-card>
          </a-col>

          <!-- 海关回执文件 -->
          <a-col :span="8">
            <a-card size="small" title="海关回执文件">
              <a-form-item label="附件">
                <div v-if="formData.customsReceiptFileUrl" class="mb-2">
                  <a :href="formData.customsReceiptFileUrl" target="_blank">
                    <DownloadOutlined /> {{ formData.customsReceiptFileName }}
                  </a>
                </div>
                <a-upload
                  :show-upload-list="false"
                  :customRequest="e => handleUpload(e, 'customsReceipt')"
                >
                  <a-button :loading="uploading.customsReceipt">
                    {{ formData.customsReceiptFileUrl ? '重新上传' : '上传回执' }}
                  </a-button>
                </a-upload>
              </a-form-item>
            </a-card>
          </a-col>
        </a-row>

        <!-- 第二部分：退税计算 -->
        <a-divider orientation="left">退税计算</a-divider>
        <a-row :gutter="24">
          <a-col :span="10">
            <a-card title="退税参数设置" size="small">
              <a-form layout="vertical">
                <a-form-item label="退税点 (%)">
                    <a-input-number 
                      v-model:value="formData.taxRefundRate" 
                      style="width: 100%" 
                      :min="0" 
                      :max="100" 
                      :precision="2"
                      placeholder="如: 13 表示 13%"
                    />
                  </a-form-item>
                  <a-form-item label="外汇银行">
                    <a-select
                      v-model:value="formData.foreignExchangeBankId"
                      placeholder="请选择外汇银行"
                      style="width: 100%"
                      :loading="bankLoading"
                      @change="handleBankChange"
                    >
                      <a-select-option v-for="bank in bankAccounts" :key="bank.id" :value="bank.id">
                        {{ bank.accountName }} - {{ bank.bankName }}
                        <span v-if="bank.serviceFeeRate != null" style="color: #999; font-size: 12px;"> (手续费: {{ bank.serviceFeeRate }}%)</span>
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                  <a-form-item label="银行手续费率 (%)">
                    <a-input-number 
                      v-model:value="formData.bankFeeRate" 
                      style="width: 100%" 
                      :min="0" 
                      :max="10" 
                      :precision="3"
                      :disabled="true"
                      placeholder="选择银行后自动填充"
                    />
                  </a-form-item>
                  <a-form-item>
                    <a-space>
                      <a-button type="primary" @click="handleGenerateCalculation" :loading="calcLoading">
                        生成开票明细
                      </a-button>
                      <a-button @click="handleSave" :loading="saveLoading">
                        保存参数
                      </a-button>
                    </a-space>
                  </a-form-item>
                </a-form>
              </a-card>
            </a-col>
            <a-col :span="14">
              <a-card title="开票明细计算" size="small" :loading="calcLoading">
                <template v-if="calculationDetail">
                  <div class="calculation-box">
                    <!-- 定金明细 -->
                    <div class="calc-section">
                      <div class="calc-title" style="color: #1890ff;">定金收汇明细</div>
                      <div class="calc-row" v-for="item in calculationDetail.depositDetails" :key="item.remittanceName">
                        <span class="calc-label">{{ item.remittanceName ? String(item.remittanceName) : '定金' }}:</span>
                        <span class="calc-value">{{ formatMoney(item.amount) }} {{ item.currency || 'USD' }} × {{ item.exchangeRate }} = {{ formatMoney(item.cny) }} CNY</span>
                      </div>
                      <div class="calc-row total">
                        <span class="calc-label">定金合计:</span>
                        <span class="calc-value highlight">{{ formatMoney(calculationDetail.depositCny) }} CNY</span>
                      </div>
                    </div>
                    
                    <a-divider />
                    
                    <!-- 尾款明细 -->
                    <div class="calc-section">
                      <div class="calc-title" style="color: #fa8c16;">尾款收汇明细</div>
                      <div class="calc-row" v-for="item in calculationDetail.balanceDetails" :key="item.remittanceName">
                        <span class="calc-label">{{ item.remittanceName ? String(item.remittanceName) : '尾款' }}:</span>
                        <span class="calc-value">{{ formatMoney(item.amount) }} {{ item.currency || 'USD' }} × {{ item.exchangeRate }} = {{ formatMoney(item.cny) }} CNY</span>
                      </div>
                      <div class="calc-row total">
                        <span class="calc-label">尾款合计:</span>
                        <span class="calc-value highlight">{{ formatMoney(calculationDetail.balanceCny) }} CNY</span>
                      </div>
                    </div>
                    
                    <a-divider />
                    
                    <!-- 总货物金额 -->
                    <div class="calc-section">
                      <div class="calc-title">汇总</div>
                      <div class="calc-row">
                        <span class="calc-label">总货物金额 (定金+尾款):</span>
                        <span class="calc-value highlight">{{ formatMoney(calculationDetail.totalGoodsAmount) }} CNY</span>
                      </div>
                    </div>
                    
                    <a-divider />
                    
                    <!-- 开票金额计算 -->
                    <div class="calc-section">
                      <div class="calc-title">开票金额计算</div>
                      <div class="calc-row">
                        <span class="calc-label">货款金额:</span>
                        <span class="calc-value">{{ formatMoney(calculationDetail.totalGoodsAmount) }} CNY</span>
                      </div>
                      <div class="calc-row">
                        <span class="calc-label">退税金额 ({{ calculationDetail.totalGoodsAmount }} × {{ calculationDetail.taxRefundRate }}%):</span>
                        <span class="calc-value" style="color: #52c41a;">+{{ formatMoney(calculationDetail.amountWithTaxRefund - calculationDetail.totalGoodsAmount) }} CNY</span>
                      </div>
                      <div class="calc-row total-with-tax">
                        <span class="calc-label">含税总金额 (货款+退税):</span>
                        <span class="calc-value highlight">{{ formatMoney(calculationDetail.amountWithTaxRefund) }} CNY</span>
                      </div>
                      <div class="calc-row deduct">
                        <span class="calc-label">- 货代发票金额:</span>
                        <span class="calc-value">{{ formatMoney(calculationDetail.freightInvoiceAmount || 0) }} CNY</span>
                      </div>
                      <div class="calc-row deduct">
                        <span class="calc-label">- 海关代理发票金额:</span>
                        <span class="calc-value">{{ formatMoney(calculationDetail.customsInvoiceAmount || 0) }} CNY</span>
                      </div>
                      <div class="calc-row deduct">
                        <span class="calc-label">- 银行手续费 ({{ formatMoney(calculationDetail.totalGoodsAmount) }} × {{ calculationDetail.bankFeeRate }}%):</span>
                        <span class="calc-value">{{ formatMoney(calculationDetail.bankFeeAmount) }} CNY</span>
                      </div>
                    </div>
                    <a-divider />
                    
                    <div class="calc-section">
                      <div class="calc-row final">
                        <span class="calc-label">开票金额:</span>
                        <span class="calc-value final-value">{{ formatMoney(calculationDetail.invoiceAmount) }} CNY</span>
                      </div>
                      <!-- 显示数据库中存储的计算结果 -->
                      <div class="calc-row" v-if="formData.invoiceAmount && formData.invoiceAmount !== calculationDetail.invoiceAmount">
                        <span class="calc-label">数据库存储开票金额:</span>
                        <span class="calc-value final-value" style="color: #722ed1;">{{ formatMoney(formData.invoiceAmount) }} CNY</span>
                      </div>
                      <div class="calc-row" v-if="formData.taxRefundAmount">
                        <span class="calc-label">退税金额:</span>
                        <span class="calc-value" style="color: #52c41a;">+{{ formatMoney(formData.taxRefundAmount) }} CNY</span>
                      </div>
                      <div class="calc-row" v-if="calculationDetail.foreignExchangeBank">
                        <span class="calc-label">外汇银行:</span>
                        <span class="calc-value">{{ calculationDetail.foreignExchangeBank }}</span>
                      </div>
                    </div>
                  </div>
                  <div class="mt-4">
                    <a-button type="primary" @click="handleExportCalculation" :loading="exportLoading">
                      <DownloadOutlined /> 导出Excel
                    </a-button>
                  </div>
                </template>
                <template v-else>
                  <div style="text-align: center; color: #999; padding: 40px 0;">
                    <p>请先设置退税参数，然后点击"生成开票明细"按钮计算开票金额</p>
                  </div>
                </template>
              </a-card>
            </a-col>
          </a-row>
        </a-form>

    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted,  computed } from 'vue'
import { message } from 'ant-design-vue'
import {  DownloadOutlined } from '@ant-design/icons-vue'
import { getFinancialSupplement, createFinancialSupplement, updateFinancialSupplement, exportFinanceCalculation, getEnabledBankAccounts, uploadFile, getDeclarationDetail, getCalculationDetail } from '@/api/business/declaration'

interface Props {
  visible: boolean
  formId: number | string
  formNo: string
}

interface Emit {
  (e: 'update:visible', visible: boolean): void
  (e: 'save-success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emit>()

const visible = ref(props.visible)
const loading = ref(false)
const saveLoading = ref(false)
const calcLoading = ref(false)
const exportLoading = ref(false)
const bankLoading = ref(false)

// 确保 formId 是数字类型
const formIdNum = computed(() => {
  return typeof props.formId === 'string' ? parseInt(props.formId) : props.formId
})

const formData = reactive({
  id: undefined as number | undefined,
  formId: 0, // 会通过 watch 更新
  formNo: props.formNo,
  freightAmount: undefined as number | undefined,
  freightInvoiceNo: '',
  freightFileUrl: '',
  freightFileName: '',
  customsAmount: undefined as number | undefined,
  customsInvoiceNo: '',
  customsFileUrl: '',
  customsFileName: '',
  customsReceiptFileUrl: '',
  customsReceiptFileName: '',
  detailsFileUrl: '',
  detailsFileName: '',
  taxRefundRate: undefined as number | undefined,
  foreignExchangeBankId: undefined as number | undefined,
  foreignExchangeBank: '', // 银行名称
  bankFeeRate: undefined as number | undefined,
  currency: 'CNY',
  totalGoodsAmount: undefined as number | undefined,
  amountWithTaxRefund: undefined as number | undefined,
  bankFeeAmount: undefined as number | undefined,
  invoiceAmount: undefined as number | undefined,
  taxRefundAmount: undefined as number | undefined
})

// 监听 formIdNum 变化，更新 formData.formId
watch(formIdNum, (newVal) => {
  if (newVal) {
    formData.formId = newVal
  }
})

const bankAccounts = ref<any[]>([])
const calculationDetail = ref<any>(null)
const uploading = ref({
  freight: false as boolean,
  customs: false as boolean,
  customsReceipt: false as boolean
})

watch(() => props.visible, (val) => {
  visible.value = val
  if (val) {
    init()
  }
}, { immediate: true })

watch(visible, (val) => {
  emit('update:visible', val)
})

const init = async () => {
    console.log('Initializing finance form...', 'formId:', props.formId, 'formIdNum:', formIdNum.value)
  loading.value = true
  try {
    // 1. 先加载申报单信息以获取币种
    if (formIdNum.value) {
      try {
        const declarationRes = await getDeclarationDetail(formIdNum.value)
        console.log('申报单响应:', declarationRes)
        // 兼容两种响应格式: declarationRes.data.data 或 declarationRes.data
        let declarationData = null
        if (declarationRes.data) {
          if (declarationRes.data.code === 200 && declarationRes.data.data) {
            declarationData = declarationRes.data.data
          } else if (declarationRes.data.id) {
            // 直接就是申报单数据
            declarationData = declarationRes.data
          }
        }
        console.log('申报单数据:', declarationData)
        if (declarationData && declarationData.currency) {
          formData.currency = declarationData.currency
          console.log('设置币种:', declarationData.currency)
        }
      } catch (error) {
        console.warn('获取申报单信息失败:', error)
        // 如果获取失败，使用默认币种
        formData.currency = formData.currency || 'CNY'
      }
    }
    
    // 2. 根据币种加载对应的银行账户列表
    if (formData.currency) {
      const bankRes = await getEnabledBankAccounts(formData.currency)
      console.log('银行账户响应:', bankRes)
      // 兼容多种响应格式
      if (bankRes.data) {
        if (bankRes.data.code === 200 && bankRes.data.data) {
          bankAccounts.value = bankRes.data.data || []
        } else if (Array.isArray(bankRes.data)) {
          bankAccounts.value = bankRes.data
        }
      }
      console.log('银行账户列表:', bankAccounts.value)
    }
    
    // 3. 加载已有财务资料
    if (formIdNum.value) {
      const response = await getFinancialSupplement(formIdNum.value)
      console.log('财务补充响应:', response)
      // 兼容多种响应格式
      let financeData: any = null
      if (response.data) {
        if (response.data.code === 200 && response.data.data) {
          financeData = response.data.data
        } else if (response.data.id) {
          financeData = response.data
        }
      }
      console.log('财务补充数据:', financeData)
      if (financeData) {
        if(financeData.id) formData.id = financeData.id
        // 使用详细字段而不是整个对象覆盖，避免覆盖未提供的字段
        if (financeData.freightAmount !== undefined) formData.freightAmount = financeData.freightAmount
        if (financeData.freightInvoiceNo) formData.freightInvoiceNo = financeData.freightInvoiceNo
        if (financeData.freightFileUrl) formData.freightFileUrl = financeData.freightFileUrl
        if (financeData.freightFileName) formData.freightFileName = financeData.freightFileName
                
        if (financeData.customsAmount !== undefined) formData.customsAmount = financeData.customsAmount
        if (financeData.customsInvoiceNo) formData.customsInvoiceNo = financeData.customsInvoiceNo
        if (financeData.customsFileUrl) formData.customsFileUrl = financeData.customsFileUrl
        if (financeData.customsFileName) formData.customsFileName = financeData.customsFileName
                
        if (financeData.detailsFileUrl) formData.detailsFileUrl = financeData.detailsFileUrl
        if (financeData.detailsFileName) formData.detailsFileName = financeData.detailsFileName
                
        if (financeData.taxRefundRate !== undefined) formData.taxRefundRate = financeData.taxRefundRate
        if (financeData.foreignExchangeBankId) {
          formData.foreignExchangeBankId = financeData.foreignExchangeBankId
          // 当加载已有银行ID时，尝试从银行列表中获取手续费率
          if (bankAccounts.value && bankAccounts.value.length > 0) {
            const selectedBank = bankAccounts.value.find(b => b.id === Number(financeData.foreignExchangeBankId))
            if (selectedBank && selectedBank.serviceFeeRate !== undefined && selectedBank.serviceFeeRate !== null) {
              formData.bankFeeRate = selectedBank.serviceFeeRate * 100  // 将银行配置的小数形式转换为百分比
            } else {
              // 如果银行不存在或没有手续费率，则使用数据库中存储的值
              if (financeData.bankFeeRate !== undefined) {
                formData.bankFeeRate = financeData.bankFeeRate
              }
            }
          } else {
            // 如果银行列表还没加载，暂时使用数据库中存储的值
            if (financeData.bankFeeRate !== undefined) {
              formData.bankFeeRate = financeData.bankFeeRate
            }
          }
        } else if (financeData.bankFeeRate !== undefined) {
          // 如果没有银行ID但有手续费率，直接使用（这种情况应该很少见）
          formData.bankFeeRate = financeData.bankFeeRate
        }
        if (financeData.foreignExchangeBank) formData.foreignExchangeBank = financeData.foreignExchangeBank
        
        // 添加计算结果字段
        if (financeData.totalGoodsAmount !== undefined) formData.totalGoodsAmount = financeData.totalGoodsAmount
        if (financeData.amountWithTaxRefund !== undefined) formData.amountWithTaxRefund = financeData.amountWithTaxRefund
        if (financeData.bankFeeAmount !== undefined) formData.bankFeeAmount = financeData.bankFeeAmount
        if (financeData.invoiceAmount !== undefined) formData.invoiceAmount = financeData.invoiceAmount
        
        // // 确保币种字段正确
        // if (financeData.currency) {
        //   formData.currency = financeData.currency
        // }
        // 解析计算明细JSON（如果存在）或使用后端返回的计算明细
        if (financeData.calculationDetail) {
          try {
            calculationDetail.value = JSON.parse(financeData.calculationDetail);
          } catch (e) {
            console.warn('解析计算明细JSON失败:', e);
            calculationDetail.value = financeData.calculationDetail || null;
          }
        } else {
          calculationDetail.value = financeData.calculationDetail || null;
        }
      }
    }
  } catch (error) {
    console.error('初始化财务补充表单失败:', error)
    message.error('初始化失败')
  } finally {
    loading.value = false
  }
}

const handleUpload = async (info: any, fileType: 'freight' | 'customs' | 'customsReceipt') => {
  try {
    uploading.value[fileType] = true
    const response = await uploadFile(info.file)
    if (response.data && response.data.code === 200 && response.data.data) {
      const fileData = response.data.data
      const fileName = info.file.name
      const fileUrl = fileData.fileUrl
      
      // 直接更新对应字段
      if (fileType === 'freight') {
        formData.freightFileName = fileName
        formData.freightFileUrl = fileUrl
      } else if (fileType === 'customs') {
        formData.customsFileName = fileName
        formData.customsFileUrl = fileUrl
      } else if (fileType === 'customsReceipt') {
        formData.customsReceiptFileName = fileName
        formData.customsReceiptFileUrl = fileUrl
      }
      
      message.success(`${fileName} 上传成功`)
    } else {
      message.error('上传失败: ' + (response.data?.msg || '未知错误'))
    }
  } catch (error: any) {
    console.error('文件上传失败:', error)
    message.error('上传失败: ' + (error.message || '网络错误'))
  } finally {
    uploading.value[fileType] = false
  }
}

const handleBankChange = (value: any) => {
  console.log('银行选择变化:', { 
    value, 
    bankAccounts: bankAccounts.value,
    currency: formData.currency 
  })
  
  if (!value) {
    formData.foreignExchangeBankId = undefined
    formData.foreignExchangeBank = ''
    // 当取消选择银行时，清空手续费率
    formData.bankFeeRate = undefined
    return
  }
  const selectedBank = bankAccounts.value.find(b => b.id === value)
  console.log('找到的银行:', selectedBank)
  
  if (selectedBank) {
    formData.foreignExchangeBankId = selectedBank.id
    formData.foreignExchangeBank = selectedBank.bankName || selectedBank.accountName || ''
    console.log('设置银行信息:', {
      id: selectedBank.id,
      bankName: selectedBank.bankName,
      accountName: selectedBank.accountName,
      bankFeeRate: selectedBank.serviceFeeRate
    })
    // 自动设置银行手续费率（将小数转换为百分比形式）
    if (selectedBank.serviceFeeRate !== undefined && selectedBank.serviceFeeRate !== null) {
      formData.bankFeeRate = selectedBank.serviceFeeRate * 100  // 数据库中存储的是小数形式，显示时转换为百分比
    } else {
      // 如果银行没有设置手续费率，则清空
      formData.bankFeeRate = undefined
    }
  } else {
    console.error('未找到ID为', value, '的银行')
    // 如果找不到银行，清空相关字段
    formData.foreignExchangeBankId = undefined
    formData.foreignExchangeBank = ''
    formData.bankFeeRate = undefined
  }
}

const handleGenerateCalculation = async () => {
  if (!formData.formId) {
    message.warning('表单ID不存在')
    return
  }
  
  try {
    calcLoading.value = true
    
    // 调用计算API获取开票明细（这将在后端自动存储计算结果到数据库）
    const response = await getCalculationDetail(formData.formId)
    if (response.data && response.data.code === 200 && response.data.data) {
      calculationDetail.value = response.data.data
      
      // 同时更新本地formData中的计算结果
      const calcData = response.data.data
      if (calcData.totalGoodsAmount !== undefined) formData.totalGoodsAmount = calcData.totalGoodsAmount
      if (calcData.amountWithTaxRefund !== undefined) formData.amountWithTaxRefund = calcData.amountWithTaxRefund
      if (calcData.bankFeeAmount !== undefined) formData.bankFeeAmount = calcData.bankFeeAmount
      if (calcData.invoiceAmount !== undefined) formData.invoiceAmount = calcData.invoiceAmount
      
      message.success('开票明细计算成功并已保存到数据库')
    } else {
      message.error('获取开票明细失败')
    }
  } catch (error: any) {
    console.error('生成开票明细失败:', error)
    message.error('生成开票明细失败: ' + (error.message || '未知错误'))
  } finally {
    calcLoading.value = false
  }
}

const handleSave = async () => {
  console.log('保存按钮被点击', { 
    formId: formData.formId, 
    formIdNum: formIdNum.value,
    id: formData.id 
  })
  
  if (!formData.formId && formData.formId !== 0) {
    message.warning('表单ID不存在')
    return
  }
  
  try {
    saveLoading.value = true
    const payload = {
      id: formData.id,
      formId: formData.formId,
      formNo: formData.formNo,
      freightAmount: formData.freightAmount,
      freightInvoiceNo: formData.freightInvoiceNo,
      freightFileUrl: formData.freightFileUrl,
      freightFileName: formData.freightFileName,
      customsAmount: formData.customsAmount,
      customsInvoiceNo: formData.customsInvoiceNo,
      customsFileUrl: formData.customsFileUrl,
      customsFileName: formData.customsFileName,
      customsReceiptFileUrl: formData.customsReceiptFileUrl,
      customsReceiptFileName: formData.customsReceiptFileName,
      detailsFileUrl: formData.detailsFileUrl,
      detailsFileName: formData.detailsFileName,
      taxRefundRate: formData.taxRefundRate,
      foreignExchangeBankId: formData.foreignExchangeBankId,
      foreignExchangeBank: formData.foreignExchangeBank,
      bankFeeRate: formData.bankFeeRate,
      currency: formData.currency
    }
    
    let response
    if (formData.id) {
      // 更新
      response = await updateFinancialSupplement(formData.id, payload)
    } else {
      // 新增
      response = await createFinancialSupplement(payload)
    }
    
    if (response.data && response.data.code === 200) {
      // 新增保存成功后，从返回数据中获取ID
      if (!formData.id && response.data.data) {
        formData.id = response.data.data.id
        message.success('保存成功')
        emit('save-success')
        visible.value = false
      } else if (formData.id) {
        // 更新保存成功
        message.success('保存成功')
        emit('save-success')
        visible.value = false
      } else {
        message.error('保存失败: 服务器返回数据异常')
      }
    } else {
      message.error('保存失败: ' + (response.data?.msg || '未知错误'))
    }
  } catch (error) {
    console.error('保存财务补充失败:', error)
    message.error('保存失败')
  } finally {
    saveLoading.value = false
  }
}

const handleExportCalculation = async () => {
  if (!formData.formId) {
    message.warning('表单ID不存在')
    return
  }
  
  try {
    exportLoading.value = true
    // 获取下载链接
    const response = await exportFinanceCalculation(formData.formId)
    const downloadUrl = response.data.data
    
    // 使用 window.location.href 触发下载
    window.location.href = `${downloadUrl}`
    message.success('Excel导出成功')
  } catch (error) {
    console.error('导出Excel失败:', error)
    message.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

const handleClose = () => {
  visible.value = false
}

// 获取完整的文件URL
// const getFullUrl = (url: string) => {
//   if (!url) return ''
//   // 如果已经是完整URL（以http://或https://开头），直接返回
//   if (url.startsWith('http://') || url.startsWith('https://')) {
//     return url
//   }
//   // 否则拼接VITE_API_BASE_URL
//   const baseURL = import.meta.env.VITE_API_BASE_URL || ''
//   return baseURL.replace(/\/+$/, '') + '/' + url.replace(/^\/+/, '')
// }
// }

const formatMoney = (amount: number | undefined) => {
  if (amount === undefined || amount === null) return '0.00'
  return amount.toFixed(2)
}

onMounted(() => {
  if (props.visible) {
    init()
  }
})

defineExpose({
  open: () => {
    visible.value = true
    init()
  }
})
</script>

<style scoped>
.calculation-box {
  background: #f7f7f7;
  padding: 20px;
  border-radius: 8px;
}

.calc-section {
  margin-bottom: 20px;
}

.calc-title {
  font-weight: bold;
  margin-bottom: 12px;
  color: #333;
}

.calc-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #ddd;
}

.calc-row:last-child {
  border-bottom: none;
}

.calc-label {
  color: #666;
}

.calc-value {
  font-weight: 500;
  color: #333;
}

.calc-value.highlight {
  color: #1890ff;
  font-size: 16px;
  font-weight: bold;
}

.calc-value.final-value {
  color: #52c41a;
  font-size: 18px;
  font-weight: bold;
}

.deduct .calc-value {
  color: #ff4d4f;
}

.total-with-tax .calc-value {
  color: #fa8c16;
  font-size: 16px;
  font-weight: bold;
}
</style>
