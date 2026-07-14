<template>
  <a-modal
    v-model:visible="visible"
    :title="`财务单证 - ${formNo}`"
    width="1200px"
    :confirmLoading="saveLoading"
    @ok="handleSave"
    @cancel="handleClose"
    okText="保存"
    cancelText="关闭"
  >
    <a-spin :spinning="loading">
      <!-- 财务补充表单内容 -->
      <a-form :model="formData" layout="vertical" :disabled="saveLoading">
        <!-- 第一部分：发票上传 -->
        <a-divider orientation="left">发票上传</a-divider>
        <a-alert
          type="info"
          show-icon
          message="货代发票与报关代理发票由申报资料提交环节录入，本页面仅展示。如需修改请到申报资料页。"
          style="margin-bottom: 12px;"
        />
        <a-row :gutter="16" class="mb-4">
          <!-- 货代发票（只读） -->
          <a-col :span="8">
            <a-card size="small" title="货代发票（来自申报资料）">
              <a-form-item label="货代金额">
                <a-input-number v-model:value="formData.freightAmount" style="width: 100%" :precision="2" placeholder="未录入" disabled />
              </a-form-item>
              <a-form-item label="发票号">
                <a-input v-model:value="formData.freightInvoiceNo" placeholder="未录入" disabled />
              </a-form-item>
              <a-form-item label="附件">
                <div v-if="formData.freightFileUrl">
                  <a @click.prevent="previewFile(formData.freightFileUrl)" style="cursor:pointer">
                    <DownloadOutlined /> {{ formData.freightFileName || '查看附件' }}
                  </a>
                </div>
                <span v-else style="color: #999">未上传</span>
              </a-form-item>
            </a-card>
          </a-col>

          <!-- 报关代理发票（只读） -->
          <a-col :span="8">
            <a-card size="small" title="报关代理发票（来自申报资料）">
              <a-form-item label="报关代理金额">
                <a-input-number v-model:value="formData.customsAmount" style="width: 100%" :precision="2" placeholder="未录入" disabled />
              </a-form-item>
              <a-form-item label="发票号">
                <a-input v-model:value="formData.customsInvoiceNo" placeholder="未录入" disabled />
              </a-form-item>
              <a-form-item label="附件">
                <div v-if="formData.customsFileUrl">
                  <a @click.prevent="previewFile(formData.customsFileUrl)" style="cursor:pointer">
                    <DownloadOutlined /> {{ formData.customsFileName || '查看附件' }}
                  </a>
                </div>
                <span v-else style="color: #999">未上传</span>
              </a-form-item>
            </a-card>
          </a-col>

          <!-- 海关回执文件 -->
          <a-col :span="8">
            <a-card size="small" title="海关回执文件">
              <a-form-item label="附件">
                <div v-if="formData.customsReceiptFileUrl" class="mb-2">
                  <a @click.prevent="previewFile(formData.customsReceiptFileUrl)" style="cursor:pointer">
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
            <a-card title="商品退税率明细" size="small">
              <template v-if="calculationDetail && calculationDetail.productTaxDetails && calculationDetail.productTaxDetails.length > 0">
                <div v-for="(item, idx) in calculationDetail.productTaxDetails" :key="idx" class="calc-row" style="border-bottom: 1px dashed #ddd; padding: 8px 0;">
                  <div>
                    <div style="font-weight: 500;">{{ item.productName || item.hsCode || '商品' + (Number(idx) + 1) }}</div>
                    <div style="font-size: 12px; color: #999;">HS: {{ item.hsCode || '-' }} | 汇率: {{ item.exchangeRate }}</div>
                    <div style="font-size: 12px; color: #666;">原币: {{ formatMoney(item.amount) }} × {{ item.exchangeRate }} = {{ formatMoney(item.cnyAmount) }} CNY</div>
                  </div>
                  <div style="text-align: right;">
                    <div style="font-size: 12px;" :style="{ color: item.taxRefundRate > 0 ? '#52c41a' : '#ff4d4f' }">退税率 {{ item.taxRefundRate }}%</div>
                    <div style="font-weight: 500;">{{ formatMoney(item.cnyAmount) }} × (1 + {{ item.taxRefundRate }}%) = {{ formatMoney(item.amountWithTaxRefund) }} CNY</div>
                  </div>
                </div>
                <div class="calc-row total" style="margin-top: 8px;">
                  <span class="calc-label">退税加成合计:</span>
                  <span class="calc-value highlight">{{ formatMoney(calculationDetail.amountWithTaxRefund) }} CNY</span>
                </div>
              </template>
              <template v-else>
                <a-empty :description="calculationDetail ? '无商品退税信息' : '点击右侧按钮计算开票金额'" />
              </template>
              <a-form-item class="mt-4">
                <a-button type="primary" @click="handleGenerateCalculation" :loading="calcLoading" block>
                  计算开票金额
                </a-button>
              </a-form-item>
            </a-card>
          </a-col>
            <a-col :span="14">
              <a-card title="开票明细计算" size="small" :loading="calcLoading">
                <template v-if="calculationDetail">
                  <div class="calculation-box">
                    <!-- 收汇明细 -->
                    <div class="calc-section">
                      <div class="calc-title" style="color: #1890ff;">收汇明细</div>
                      <div v-for="item in calculationDetail.remittanceDetails" :key="item.remittanceName">
                        <div class="calc-row">
                          <span class="calc-label">{{ item.remittanceName || '收汇' }}:</span>
                          <span class="calc-value">
                            {{ formatMoney(item.amount) }} {{ item.currency || 'USD' }}
                            <span v-if="item.relationAmount && item.fullAmount && item.relationAmount !== item.fullAmount" style="color: #999; font-size: 11px;">(水单全额: {{ formatMoney(item.fullAmount) }})</span>
                            × {{ item.taxRate }} = {{ formatMoney(item.cnyAmount) }} CNY
                          </span>
                        </div>
                      </div>
                      <div class="calc-row total" style="margin-top: 8px;">
                        <span class="calc-label">收汇合计:</span>
                        <span class="calc-value highlight">{{ formatMoney(calculationDetail.totalGoodsAmount) }} CNY</span>
                        <span v-if="calculationDetail.weightedExchangeRate" style="margin-left: 12px; color: #999; font-size: 12px;">加权平均汇率: {{ calculationDetail.weightedExchangeRate }}</span>
                      </div>
                    </div>
                    
                    <a-divider />
                    
                    <!-- 总货物金额 -->
                    <div class="calc-section">
                      <div class="calc-row">
                        <span class="calc-label">总货物金额:</span>
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
                        <span class="calc-label">退税加成合计:</span>
                        <span class="calc-value" style="color: #52c41a;">含税总计 {{ formatMoney(calculationDetail.amountWithTaxRefund) }} CNY (+{{ formatMoney(calculationDetail.amountWithTaxRefund - calculationDetail.totalGoodsAmount) }})</span>
                      </div>
                      <div class="calc-row total-with-tax">
                        <span class="calc-label">含税总金额 (货款+退税):</span>
                        <span class="calc-value highlight">{{ formatMoney(calculationDetail.amountWithTaxRefund) }} CNY</span>
                      </div>
                      <div class="calc-row deduct" v-for="(ded, didx) in (calculationDetail.invoiceDeductionItems || [])" :key="'ded-'+didx">
                        <span class="calc-label">- {{ ded.name }}:</span>
                        <span class="calc-value">{{ formatMoney(ded.amount) }} CNY</span>
                      </div>
                      <div class="calc-row deduct" v-if="!calculationDetail.invoiceDeductionItems || calculationDetail.invoiceDeductionItems.length === 0">
                        <span class="calc-label">发票扣减项:</span>
                        <span class="calc-value text-gray-400">无</span>
                      </div>
                      <div class="calc-row deduct" style="border-bottom: 2px solid #ff4d4f;">
                        <span class="calc-label" style="font-weight: bold;">- 发票扣减合计:</span>
                        <span class="calc-value" style="font-weight: bold;">{{ formatMoney(calculationDetail.totalInvoiceDeduction || 0) }} CNY</span>
                      </div>
                      <div class="calc-row deduct">
                        <span class="calc-label">- 银行手续费:</span>
                        <span class="calc-value">{{ formatMoney(calculationDetail.bankFeeAmount) }} CNY</span>
                      </div>
                      <div class="calc-row deduct">
                        <span class="calc-label">- 内部操作手续费:</span>
                        <span class="calc-value">{{ formatMoney(calculationDetail.internalBankFee) }} CNY</span>
                      </div>
                      <div class="calc-row deduct" style="border-bottom: 1px solid #d9d9d9;">
                        <span class="calc-label" style="font-weight: bold;">- 手续费合计:</span>
                        <span class="calc-value" style="font-weight: bold;">{{ formatMoney(calculationDetail.totalFeeAmount || 0) }} CNY</span>
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
                    <p>点击左侧“计算开票金额”按钮查看计算结果</p>
                    <p style="font-size: 12px; color: #bbb;">退税率取自商品配置，未配置按 0% 计算</p>
                  </div>
                </template>
              </a-card>
            </a-col>
          </a-row>
        </a-form>

    </a-spin>
  </a-modal>

  <!-- 文件预览弹窗 -->
  <FilePreviewModal v-model:visible="previewVisible" :url="previewUrl" />
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import { getFinancialSupplement, createFinancialSupplement, updateFinancialSupplement, exportFinanceCalculation, uploadFile, getDeclarationDetail, getCalculationDetail } from '@/api/business/declaration'
import { getMaterialItems } from '@/api/business/materialItem'
import FilePreviewModal from '@/components/FilePreviewModal.vue'

// 文件预览
const previewVisible = ref(false)
const previewUrl = ref('')
const previewFile = (url: string) => { if (url) { previewUrl.value = url; previewVisible.value = true } }

// 货代/报关代理发票的资料项 code
const MATERIAL_CODE_FREIGHT = 'FREIGHT_INVOICE'
const MATERIAL_CODE_CUSTOMS = 'CUSTOMS_AGENT_INVOICE'

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
  // taxRefundRate: undefined as number | undefined, // 已废弃，退税现由商品配置驱动
  currency: 'CNY',
  totalGoodsAmount: undefined as number | undefined,
  amountWithTaxRefund: undefined as number | undefined,
  bankFeeAmount: undefined as number | undefined,
  invoiceAmount: undefined as number | undefined,
  taxRefundAmount: undefined as number | undefined
})

// 监听 formIdNum 变化，更新 formData.formId 并重新加载数据
watch(formIdNum, (newVal) => {
  if (newVal) {
    formData.formId = newVal
    // 当 formId 变化时，重新加载数据
    if (visible.value) {
      init()
    }
  }
})

const calculationDetail = ref<any>(null)
const uploading = ref({
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
  
  // 先重置表单数据，避免旧数据残留
  formData.id = undefined
  formData.formId = formIdNum.value
  formData.formNo = props.formNo
  formData.freightAmount = undefined
  formData.freightInvoiceNo = ''
  formData.freightFileName = ''
  formData.freightFileUrl = ''
  formData.customsAmount = undefined
  formData.customsInvoiceNo = ''
  formData.customsFileName = ''
  formData.customsFileUrl = ''
  formData.customsReceiptFileUrl = ''
  formData.customsReceiptFileName = ''
  formData.detailsFileUrl = ''
  formData.detailsFileName = ''
  // formData.taxRefundRate 已废弃
  formData.totalGoodsAmount = undefined
  formData.amountWithTaxRefund = undefined
  formData.bankFeeAmount = undefined
  formData.invoiceAmount = undefined
  formData.taxRefundAmount = undefined
  calculationDetail.value = null

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
        } else {
          formData.currency = 'CNY' // 默认币种
        }
      } catch (error) {
        console.warn('获取申报单信息失败:', error)
        formData.currency = 'CNY'
      }
    }

    // 2. 加载已有财务资料
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
        if (financeData.freightAmount !== undefined && financeData.freightAmount !== null) formData.freightAmount = financeData.freightAmount
        if (financeData.freightInvoiceNo) formData.freightInvoiceNo = financeData.freightInvoiceNo
        if (financeData.freightFileUrl) formData.freightFileUrl = financeData.freightFileUrl
        if (financeData.freightFileName) formData.freightFileName = financeData.freightFileName

        if (financeData.customsAmount !== undefined && financeData.customsAmount !== null) formData.customsAmount = financeData.customsAmount
        if (financeData.customsInvoiceNo) formData.customsInvoiceNo = financeData.customsInvoiceNo
        if (financeData.customsFileUrl) formData.customsFileUrl = financeData.customsFileUrl
        if (financeData.customsFileName) formData.customsFileName = financeData.customsFileName

        if (financeData.customsReceiptFileUrl) formData.customsReceiptFileUrl = financeData.customsReceiptFileUrl
        if (financeData.customsReceiptFileName) formData.customsReceiptFileName = financeData.customsReceiptFileName

        if (financeData.detailsFileUrl) formData.detailsFileUrl = financeData.detailsFileUrl
        if (financeData.detailsFileName) formData.detailsFileName = financeData.detailsFileName

        // taxRefundRate 已废弃，退税现由商品配置驱动

        // 添加计算结果字段
        if (financeData.totalGoodsAmount !== undefined && financeData.totalGoodsAmount !== null) formData.totalGoodsAmount = financeData.totalGoodsAmount
        if (financeData.amountWithTaxRefund !== undefined && financeData.amountWithTaxRefund !== null) formData.amountWithTaxRefund = financeData.amountWithTaxRefund
        if (financeData.bankFeeAmount !== undefined && financeData.bankFeeAmount !== null) formData.bankFeeAmount = financeData.bankFeeAmount
        if (financeData.invoiceAmount !== undefined && financeData.invoiceAmount !== null) formData.invoiceAmount = financeData.invoiceAmount
        if (financeData.taxRefundAmount !== undefined && financeData.taxRefundAmount !== null) formData.taxRefundAmount = financeData.taxRefundAmount

        // 解析计算明细JSON（如果存在）或使用后端返回的计算明细
        if (financeData.calculationDetail) {
          try {
            calculationDetail.value = JSON.parse(financeData.calculationDetail);
          } catch (e) {
            console.warn('解析计算明细JSON失败:', e);
            calculationDetail.value = null;
          }
        } else {
          calculationDetail.value = null;
        }
      }
    }

    // 3. 从申报资料项读取货代/报关代理发票（只读展示，覆盖 financial_supplement 中的历史值）
    if (formIdNum.value) {
      try {
        const materialRes = await getMaterialItems(formIdNum.value)
        const materialData: any = (materialRes && materialRes.data) ? materialRes.data : null
        const items: any[] = materialData && materialData.code === 200
          ? (materialData.data || [])
          : (Array.isArray(materialData) ? materialData : [])
        const freight = items.find(i => i && i.code === MATERIAL_CODE_FREIGHT)
        const customs = items.find(i => i && i.code === MATERIAL_CODE_CUSTOMS)
        if (freight) {
          formData.freightAmount = freight.amount != null ? Number(freight.amount) : undefined
          formData.freightInvoiceNo = freight.invoiceNo || ''
          formData.freightFileUrl = freight.fileUrl || ''
          formData.freightFileName = freight.fileName || ''
        } else {
          formData.freightAmount = undefined
          formData.freightInvoiceNo = ''
          formData.freightFileUrl = ''
          formData.freightFileName = ''
        }
        if (customs) {
          formData.customsAmount = customs.amount != null ? Number(customs.amount) : undefined
          formData.customsInvoiceNo = customs.invoiceNo || ''
          formData.customsFileUrl = customs.fileUrl || ''
          formData.customsFileName = customs.fileName || ''
        } else {
          formData.customsAmount = undefined
          formData.customsInvoiceNo = ''
          formData.customsFileUrl = ''
          formData.customsFileName = ''
        }
      } catch (error) {
        console.warn('加载资料项发票失败，按空值展示:', error)
      }
    }
    console.log('初始化完成，最终formData:', formData)
  } catch (error) {
    console.error('初始化财务补充表单失败:', error)
    message.error('初始化失败')
  } finally {
    loading.value = false
  }
}

const handleUpload = async (info: any, fileType: 'customsReceipt') => {
  try {
    uploading.value[fileType] = true
    const response = await uploadFile(info.file)
    if (response.data && response.data.code === 200 && response.data.data) {
      const fileData = response.data.data
      const fileName = info.file.name
      const fileUrl = fileData.fileUrl

      if (fileType === 'customsReceipt') {
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
      // 货代/报关代理发票由申报资料提交环节录入，该页不再写入 financial_supplement
      customsReceiptFileUrl: formData.customsReceiptFileUrl,
      customsReceiptFileName: formData.customsReceiptFileName,
      detailsFileUrl: formData.detailsFileUrl,
      detailsFileName: formData.detailsFileName,
      // taxRefundRate 已废弃，退税现由商品配置驱动
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
