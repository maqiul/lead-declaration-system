<template>
  <div class="app-container">
    <a-card :bordered="false" class="mb-4">
      <a-form layout="inline" :model="queryParams" @finish="handleQuery">
        <a-form-item label="申报单号">
          <a-input v-model:value="queryParams.formNo" placeholder="请输入申报单号" allow-clear />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="queryParams.status" placeholder="请选择状态" style="width: 120px" allow-clear>
            <a-select-option :value="0">待上传</a-select-option>
            <a-select-option :value="1">已上传</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="币种">
          <a-select v-model:value="queryParams.currency" placeholder="请选择币种" style="width: 120px" allow-clear>
            <a-select-option value="CNY">人民币 (CNY)</a-select-option>
            <a-select-option value="USD">美元 (USD)</a-select-option>
            <a-select-option value="EUR">欧元 (EUR)</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">
              <template #icon><SearchOutlined /></template>
              查询
            </a-button>
            <a-button @click="resetQuery">
              <template #icon><ReloadOutlined /></template>
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card :bordered="false">
      <a-table
        :columns="columns"
        :data-source="dataList"
        :pagination="pagination"
        :loading="loading"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'formNo'">
            <a-button type="link" @click="handleViewDeclaration(record.formId)">
              {{ record.formNo }}
            </a-button>
          </template>

          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'success' : 'warning'">
              {{ record.status === 1 ? '已上传' : '待上传' }}
            </a-tag>
          </template>

          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="handleEdit(record)" v-permission="['business:declaration:finance:supplement']" v-if="record.status === 0">
                <template #icon><UploadOutlined /></template>
                去上传
              </a-button>
              <a-button type="link" @click="handleView(record)" v-permission="['business:declaration:finance:supplement']" v-else>
                <template #icon><EyeOutlined /></template>
                查看
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 使用统一的财务补充弹窗 -->
    <FinanceModal
      ref="financeModalRef"
      :visible="modalVisible"
      :formId="currentRecord?.formId"
      :formNo="currentRecord?.formNo"
      :declarationStatus="currentRecord?.declarationStatus"
      @update:visible="modalVisible = $event"
      @save-success="handleSaveSuccess"
    />

    <!-- 审核驳回原因弹窗 -->
    <a-modal
      v-model:visible="rejectModalVisible"
      title="驳回原因"
      @ok="handleConfirmReject"
      :confirmLoading="rejectLoading"
    >
      <a-form layout="vertical">
        <a-form-item label="驳回原因" required>
          <a-textarea v-model:value="rejectRemark" :rows="4" placeholder="请输入驳回原因" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { SearchOutlined, ReloadOutlined, UploadOutlined, EyeOutlined } from '@ant-design/icons-vue'
import {
  getFinancialSupplementList,
  auditDeliveryOrder,
  getDeliveryOrders,
} from '@/api/business/declaration'
import FinanceModal from './components/FinanceModal.vue'

const router = useRouter()
const route = useRoute()

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  formNo: '',
  status: undefined as number | undefined,
  currency: undefined as string | undefined
})

// 页面状态 - 处理路由参数
const selectedFormNo = ref(route.query.formNo ? String(route.query.formNo) : '')

// 如果有选中的申报单号，设置查询条件
if (selectedFormNo.value) {
  queryParams.formNo = selectedFormNo.value
}

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const loading = ref(false)
const dataList = ref([])

// 表格列定义
const columns = [
  { title: '序号', dataIndex: 'index', key: 'index', width: 80, customRender: ({ index }: { index: number }) => index + 1 },
  { title: '申报单号', dataIndex: 'formNo', key: 'formNo' },
  { title: '货代金额', dataIndex: 'freightAmount', key: 'freightAmount', customRender: ({ text }: any) => formatMoney(text) },
  { title: '报关代理金额', dataIndex: 'customsAmount', key: 'customsAmount', customRender: ({ text }: any) => formatMoney(text) },
  { title: '开票金额', dataIndex: 'detailsAmount', key: 'detailsAmount', customRender: ({ text }: any) => formatMoney(text) },
  { title: '币种', dataIndex: 'currency', key: 'currency' },
  { title: '外汇银行', dataIndex: 'foreignExchangeBank', key: 'foreignExchangeBank' },
  // { title: '状态', dataIndex: 'status', key: 'status', customRender: ({ text }: { text: number }) => text === 1 ? '已上传' : '待上传' },
  { title: '生成时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '操作', key: 'action', width: 150, fixed: 'right' as const }
]


// 获取列表数据
const getList = async () => {
  loading.value = true
  console.log('开始获取财务补充列表数据...')
  try {
    const res = await getFinancialSupplementList(queryParams)
    console.log('API响应:', res)
    dataList.value = res.data.data.records
    pagination.total = res.data.data.total
    console.log('数据加载完成:', {
      recordsCount: dataList.value.length,
      totalCount: pagination.total
    })
  } catch (error) {
    console.error('获取列表失败', error)
    message.error('获取数据失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  pagination.current = 1
  getList()
}

const resetQuery = () => {
  queryParams.formNo = ''
  queryParams.status = undefined
  handleQuery()
}

const handleTableChange = (pag: any) => {
  queryParams.pageNum = pag.current
  queryParams.pageSize = pag.pageSize
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  getList()
}

const handleViewDeclaration = (formId: number) => {
  router.push(`/declaration/form?id=${formId}&mode=view`)
}

// 弹窗相关
const modalVisible = ref(false)
// const saveLoading = ref(false)
const currentRecord = ref<any>(null)
// const activeTab = ref('invoices')
// const isReadOnly = ref(false)
const financeModalRef = ref()

const formData = reactive({
  id: undefined as number | undefined,
  formId: undefined as number | undefined,
  freightAmount: undefined as number | undefined,
  freightInvoiceNo: '',
  freightFileName: '',
  freightFileUrl: '',
  customsAmount: undefined as number | undefined,
  customsInvoiceNo: '',
  customsFileName: '',
  customsFileUrl: '',
  detailsAmount: undefined as number | undefined,
  detailsInvoiceNo: '',
  detailsFileName: '',
  detailsFileUrl: '',
  taxRefundRate: 13 as number | undefined,
  foreignExchangeBankId: undefined as number | undefined,
  foreignExchangeBank: '',
  bankFeeRate: 0.1 as number | undefined
})

// const uploading = reactive({
//   freight: false,
//   customs: false,
//   details: false
// })

// 银行账户
// const bankAccounts = ref<any[]>([])
// const bankLoading = ref(false)

// // 开票明细计算
// const calculationDetail = ref<any>(null)
// const calcLoading = ref(false)

// 提货单列表
const deliveryOrderList = ref<any[]>([])
const deliveryLoading = ref(false)

// 驳回弹窗
const rejectModalVisible = ref(false)
const rejectRemark = ref('')
const rejectLoading = ref(false)
const rejectTarget = ref<{ type: 'delivery'; item: any } | null>(null)

// // 加载银行账户列表
// const loadBankAccounts = async () => {
//   bankLoading.value = true
//   console.log('开始加载银行账户列表...')
//   try {
//     const res = await getEnabledBankAccounts()
//     console.log('银行账户API响应:', res)
//     bankAccounts.value = res.data || []
//     console.log('银行账户数据:', bankAccounts.value)
//     console.log('银行账户数量:', bankAccounts.value.length)
//   } catch (error) {
//     console.error('加载银行账户失败', error)
//     message.error('加载银行账户失败')
//   } finally {
//     bankLoading.value = false
//   }
// }

// 加载提货单列表
const loadDeliveryOrders = async (formId: number) => {
  deliveryLoading.value = true
  try {
    const res = await getDeliveryOrders(formId)
    deliveryOrderList.value = res.data || []
  } catch (error) {
    console.error('加载提货单列表失败', error)
    deliveryOrderList.value = []
  } finally {
    deliveryLoading.value = false
  }
}

const handleEdit = async (record: any) => {
  // 直接传递记录信息给FinanceModal组件，它会自行加载数据
  currentRecord.value = { ...record }
  // 使用nextTick确保响应式更新完成后再调用open
  await nextTick()
  if (financeModalRef.value) {
    financeModalRef.value.open()
  }
}

// 查看财务单证详情
const handleView = async (record: any) => {
  // 设置为只读模式并打开弹窗
  currentRecord.value = { ...record };
  // 使用nextTick确保响应式更新完成后再调用open
  await nextTick()
  if (financeModalRef.value) {
    financeModalRef.value.open()
  }
}

// // 银行选择变更
// const handleBankChange = (value: any) => {
//   console.log('银行选择变更:', value)
//   console.log('银行账户列表:', bankAccounts.value)
  
//   if (!value) {
//     formData.foreignExchangeBankId = undefined
//     formData.foreignExchangeBank = ''
//     return
//   }
  
//   const bank = bankAccounts.value.find(b => b.id === value)
//   console.log('找到的银行:', bank)
  
//   if (bank) {
//     formData.foreignExchangeBankId = bank.id  // 保存银行ID到数据库
//     formData.foreignExchangeBank = bank.accountName  // 保存银行名称用于显示
//     // 自动填充银行手续费率
//     console.log('银行手续费率:', bank.serviceFeeRate, '类型:', typeof bank.serviceFeeRate)
//     if (bank.serviceFeeRate != null) {
//       formData.bankFeeRate = bank.serviceFeeRate
//       console.log('设置手续费率:', bank.serviceFeeRate)
//     } else {
//       console.log('银行手续费率为null')
//     }
//     console.log('选择银行:', bank)
//   } else {
//     console.log('未找到对应银行')
//   }
// }

// // 通用上传处理
// const handleUpload = async (options: any, type: 'freight' | 'customs' | 'details') => {
//   const { file } = options
  
//   if (file.size > 20 * 1024 * 1024) {
//     message.error('文件大小不能超过 20MB')
//     return
//   }
  
//   uploading[type] = true
//   try {
//     const res = await uploadFile(file) as any
//     if (res.code === 200) {
//       if (type === 'freight') {
//         formData.freightFileUrl = res.data.url
//         formData.freightFileName = res.data.fileName
//       } else if (type === 'customs') {
//         formData.customsFileUrl = res.data.url
//         formData.customsFileName = res.data.fileName
//       } else if (type === 'details') {
//         formData.detailsFileUrl = res.data.url
//         formData.detailsFileName = res.data.fileName
//       }
//       message.success(`${file.name} 上传成功`)
//     }
//   } catch (error) {
//     console.error('上传出错', error)
//     message.error(`${file.name} 上传失败`)
//   } finally {
//     uploading[type] = false
//   }
// }

// // 生成开票明细
// const handleGenerateCalculation = async () => {
//   if (!formData.id || !formData.formId) return
  
//   // 先保存当前参数 - 只发送需要的字段
//   saveLoading.value = true
//   try {
//     const saveData = {
//       freightAmount: formData.freightAmount,
//       freightInvoiceNo: formData.freightInvoiceNo,
//       freightFileName: formData.freightFileName,
//       freightFileUrl: formData.freightFileUrl,
//       customsAmount: formData.customsAmount,
//       customsInvoiceNo: formData.customsInvoiceNo,
//       customsFileName: formData.customsFileName,
//       customsFileUrl: formData.customsFileUrl,
//       detailsAmount: formData.detailsAmount,
//       detailsInvoiceNo: formData.detailsInvoiceNo,
//       detailsFileName: formData.detailsFileName,
//       detailsFileUrl: formData.detailsFileUrl,
//       taxRefundRate: formData.taxRefundRate,
//       foreignExchangeBankId: formData.foreignExchangeBankId,
//       foreignExchangeBank: formData.foreignExchangeBank,
//       bankFeeRate: formData.bankFeeRate
//     }
//     await updateFinancialSupplement(formData.id, saveData)
//   } catch (err) {
//     message.error('保存参数失败')
//     saveLoading.value = false
//     return
//   }
//   saveLoading.value = false

//   // 获取计算详情
//   calcLoading.value = true
//   try {
//     const res = await getCalculationDetail(formData.formId)
//     calculationDetail.value = res.data
//     message.success('开票明细生成成功')
//   } catch (error) {
//     console.error('获取开票明细失败', error)
//     message.error('获取开票明细失败')
//   } finally {
//     calcLoading.value = false
//   }
// }

// // 导出开票明细
// const handleExportCalculation = async () => {
//   if (!formData.formId) return
  
//   try {
//     const res = await exportFinanceCalculation(formData.formId)
//     const blob = new Blob([res as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
//     const url = window.URL.createObjectURL(blob)
//     const link = document.createElement('a')
//     link.href = url
//     link.download = `开票明细_${currentRecord.value?.formNo || formData.formId}.xlsx`
//     link.click()
//     window.URL.revokeObjectURL(url)
//     message.success('导出成功')
//   } catch (error) {
//     console.error('导出失败', error)
//     message.error('导出失败')
//   }
// }

// // 仅保存
// const handleSave = async () => {
//   if (!formData.id) return
//   saveLoading.value = true
//   try {
//     const saveData = {
//       freightAmount: formData.freightAmount,
//       freightInvoiceNo: formData.freightInvoiceNo,
//       freightFileName: formData.freightFileName,
//       freightFileUrl: formData.freightFileUrl,
//       customsAmount: formData.customsAmount,
//       customsInvoiceNo: formData.customsInvoiceNo,
//       customsFileName: formData.customsFileName,
//       customsFileUrl: formData.customsFileUrl,
//       detailsAmount: formData.detailsAmount,
//       detailsInvoiceNo: formData.detailsInvoiceNo,
//       detailsFileName: formData.detailsFileName,
//       detailsFileUrl: formData.detailsFileUrl,
//       taxRefundRate: formData.taxRefundRate,
//       foreignExchangeBankId: formData.foreignExchangeBankId,
//       foreignExchangeBank: formData.foreignExchangeBank,
//       bankFeeRate: formData.bankFeeRate
//     }
//     await updateFinancialSupplement(formData.id, saveData)
//     message.success('保存成功')
//     getList()
//   } catch (error) {
//     console.error('保存失败', error)
//     message.error('保存失败')
//   } finally {
//     saveLoading.value = false
//   }
// }


const confirmAuditDeliveryOrder = async (item: any, approved: boolean, remark: string) => {
  try {
    await auditDeliveryOrder(item.id, { approved, remark })
    message.success(approved ? '审核通过' : '已驳回')
    if (formData.formId) {
      loadDeliveryOrders(formData.formId)
    }
  } catch (error) {
    console.error('审核失败', error)
    message.error('审核失败')
  }
}

// 确认驳回
const handleConfirmReject = async () => {
  if (!rejectRemark.value.trim()) {
    message.warning('请输入驳回原因')
    return
  }

  if (!rejectTarget.value) return

  rejectLoading.value = true
  try {
    if (rejectTarget.value.type === 'delivery') {
      await confirmAuditDeliveryOrder(rejectTarget.value.item, false, rejectRemark.value)
    }
    rejectModalVisible.value = false
  } finally {
    rejectLoading.value = false
  }
}

// 保存成功回调
const handleSaveSuccess = () => {
  modalVisible.value = false
  message.success('保存成功')
  getList() // 重新加载列表
}

// 辅助函数
const formatMoney = (value: number | null | undefined): string => {
  if (value === null || value === undefined) return '-'
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// const getRemittanceHeader = (item: any) => {
//   const typeText = item.type === 1 ? '定金' : '尾款'
//   const statusText = getAuditStatusText(item.auditStatus)
//   return `${typeText} - ${formatMoney(item.amount)} ${item.currency} [${statusText}]`
// }

// const getAuditStatusColor = (status: number | undefined) => {
//   switch (status) {
//     case 0: return 'orange'
//     case 1: return 'green'
//     case 2: return 'red'
//     default: return 'default'
//   }
// }

// const getAuditStatusText = (status: number | undefined) => {
//   switch (status) {
//     case 0: return '待审核'
//     case 1: return '已通过'
//     case 2: return '已驳回'
//     default: return '未知'
//   }
// }

onMounted(() => {
  getList()
})
</script>

<style scoped>
.app-container {
  padding: 24px;
}
.mb-4 {
  margin-bottom: 16px;
}
.mb-2 {
  margin-bottom: 8px;
}
.mt-3 {
  margin-top: 12px;
}
.mt-4 {
  margin-top: 16px;
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.calculation-box {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
}

.calc-section {
  margin-bottom: 8px;
}

.calc-title {
  font-weight: 600;
  color: #1890ff;
  margin-bottom: 8px;
}

.calc-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px dashed #e8e8e8;
}

.calc-row:last-child {
  border-bottom: none;
}

.calc-row.total {
  background: #e6f7ff;
  padding: 8px;
  border-radius: 4px;
  margin-top: 8px;
}

.calc-row.deduct {
  color: #ff4d4f;
}

.calc-row.final {
  background: #f6ffed;
  padding: 12px;
  border-radius: 4px;
  font-size: 16px;
}

.calc-label {
  color: #666;
}

.calc-value {
  font-weight: 500;
}

.calc-value.highlight {
  color: #1890ff;
  font-weight: 600;
}

.calc-value.final-value {
  color: #52c41a;
  font-weight: 700;
  font-size: 18px;
}
</style>
