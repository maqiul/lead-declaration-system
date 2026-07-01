<template>
  <a-modal
    v-model:open="visible"
    :title="readonly ? '查看20%产品明细' : '设置20%产品明细'"
    width="900px"
    @ok="handleConfirm"
    @cancel="handleCancel"
    :confirm-loading="confirming"
    :ok-text="readonly ? '确认并下载' : '确认并下载'"
    cancel-text="取消"
  >
    <a-alert v-if="!readonly" message="请通过HS编码选择产品，并设置20%部分的数量和单价。80%部分将自动按开票金额比例生成。" type="info" show-icon style="margin-bottom: 12px;" />
    <a-alert v-else message="当前为只读模式，仅审核人员可修改数据。" type="info" show-icon style="margin-bottom: 12px;" />

    <a-table
      :data-source="items"
      :columns="readonly ? readonlyColumns : columns"
      :pagination="false"
      size="small"
      bordered
      row-key="key"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.dataIndex === 'hsCode'">
          <a-select
            v-model:value="record.hsCode"
            size="small"
            style="width: 100%"
            placeholder="选择HS编码"
            :options="hsOptions"
            show-search
            option-filter-prop="label"
            :disabled="readonly"
            @change="(val: any) => onHsCodeChange(record as SplitItem, val)"
          />
        </template>
        <template v-if="column.dataIndex === 'productName'">
          <a-input v-model:value="record.productName" size="small" placeholder="产品名称" :disabled="readonly" />
        </template>
        <template v-if="column.dataIndex === 'quantity'">
          <a-input-number v-model:value="record.quantity" size="small" :min="0" :precision="2" style="width: 100%;" :disabled="readonly" @change="calcRowAmount(record as SplitItem)" />
        </template>
        <template v-if="column.dataIndex === 'unitPrice'">
          <a-input-number v-model:value="record.unitPrice" size="small" :min="0" :precision="9" style="width: 100%;" :disabled="readonly" @change="calcRowAmount(record as SplitItem)" />
        </template>
        <template v-if="column.dataIndex === 'amount'">
          <a-input-number v-model:value="record.amount" size="small" :min="0" :precision="2" style="width: 100%;" :disabled="readonly" @change="onAmountManual(record as SplitItem)" />
        </template>
        <template v-if="column.dataIndex === 'action'">
          <a-button type="link" danger size="small" @click="removeRow(index)" :disabled="readonly">
            <template #icon><DeleteOutlined /></template>
          </a-button>
        </template>
      </template>
    </a-table>

    <div v-if="!readonly" style="margin-top: 8px;">
      <a-button type="dashed" block @click="addRow">
        <template #icon><PlusOutlined /></template>
        新增一行
      </a-button>
    </div>

    <div style="margin-top: 12px; text-align: right; font-size: 14px;">
      <span>合计金额：<b :style="{ color: isOverLimit ? '#ff4d4f' : '#1677ff', fontSize: '16px' }">{{ formatAmt(totalAmount) }}</b> CNY</span>
      <span style="margin-left: 8px; color: #999;">/ 上限 {{ formatAmt(maxAmount20) }} CNY</span>
      <span style="margin-left: 16px; color: #666;">（大写：{{ totalAmountChinese }}）</span>
    </div>
    <a-alert v-if="isOverLimit" message="20%产品合计金额已超过开票金额的20%，请调整后重新提交。" type="error" show-icon style="margin-top: 8px;" />
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { getInvoiceSplitItems, saveInvoiceSplitItems } from '@/api/business/declaration'
import { getProductTypes } from '@/api/system/product'

export interface SplitItem {
  key: number
  hsCode: string
  productName: string
  spec: string
  quantity: number
  unitPrice: number
  amount: number
  amountManual?: boolean
}

interface ProductTaxDetail {
  productName?: string
  hsCode?: string
  quantity?: number
  unit?: string
  cnyAmount?: number
}

const props = defineProps<{
  formId: number
  calcDetail: any
  readonly?: boolean
}>()

const emit = defineEmits<{
  (e: 'confirm', items: SplitItem[]): void
}>()

const visible = ref(false)
const confirming = ref(false)
let keyCounter = 0

const items = ref<SplitItem[]>([])

// HS编码下拉选项
interface HsOption {
  label: string
  value: string
  chineseName: string
  englishName: string
}
const hsOptions = ref<HsOption[]>([])

const columns = [
  { title: 'HS编码', dataIndex: 'hsCode', width: 150 },
  { title: '产品名称', dataIndex: 'productName', width: 160 },
  { title: '数量', dataIndex: 'quantity', width: 100 },
  { title: '单价(CNY)', dataIndex: 'unitPrice', width: 120 },
  { title: '金额(CNY)', dataIndex: 'amount', width: 110 },
  { title: '操作', dataIndex: 'action', width: 60, align: 'center' as const },
]

// 只读模式下不显示操作列
const readonlyColumns = columns.filter(c => c.dataIndex !== 'action')

const totalAmount = computed(() => {
  return items.value.reduce((sum, item) => sum + (item.amount || 0), 0)
})

const totalAmountChinese = computed(() => {
  return convertToChinese(totalAmount.value)
})

// 20%金额上限 = 开票金额 × 0.2
const maxAmount20 = computed(() => {
  const invoiceAmt = Number(props.calcDetail?.invoiceAmount) || 0
  return Number((invoiceAmt * 0.2).toFixed(2))
})

const isOverLimit = computed(() => {
  return totalAmount.value > maxAmount20.value + 0.001 // 容差
})

function formatAmt(val: number | undefined | null): string {
  if (val == null || isNaN(val)) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function calcRowAmount(record: SplitItem) {
  if (!record.amountManual) {
    record.amount = Number(((record.quantity || 0) * (record.unitPrice || 0)).toFixed(2))
  }
}

function onAmountManual(record: SplitItem) {
  record.amountManual = true
}

function addRow() {
  items.value.push({
    key: keyCounter++,
    hsCode: '',
    productName: '',
    spec: '',
    quantity: 0,
    unitPrice: 0,
    amount: 0,
  })
}

function removeRow(index: number) {
  items.value.splice(index, 1)
}

/** HS编码选择后自动填充产品名称 */
function onHsCodeChange(record: SplitItem, hsCode: string) {
  const opt = hsOptions.value.find(o => o.value === hsCode)
  if (opt) {
    record.productName = opt.chineseName || opt.englishName || ''
  }
}

/** 加载HS编码选项 */
async function loadHsOptions() {
  if (hsOptions.value.length > 0) return
  try {
    const res = await getProductTypes()
    if (res.data?.code === 200 && Array.isArray(res.data.data)) {
      hsOptions.value = res.data.data.map((item: any) => ({
        label: item.hsCode,
        value: item.hsCode,
        chineseName: item.chineseName || '',
        englishName: item.englishName || '',
      }))
    }
  } catch {
    // ignore
  }
}

function buildItemFromProductTaxDetail(pd: ProductTaxDetail, invoiceRatio: number): SplitItem {
  const qty = pd.quantity || 0
  const cnyAmt = pd.cnyAmount || 0
  const originalUnitPrice = qty > 0 ? Number((cnyAmt / qty).toFixed(9)) : 0
  // 按 invoiceAmount 比例分配 20%
  const unitPrice20 = Number((originalUnitPrice * invoiceRatio * 0.2).toFixed(9))
  return {
    key: keyCounter++,
    hsCode: pd.hsCode || '',
    productName: pd.productName || pd.hsCode || '',
    spec: '',
    quantity: qty,
    unitPrice: unitPrice20,
    amount: Number((qty * unitPrice20).toFixed(2)),
  }
}

async function open() {
  visible.value = true
  items.value = []
  keyCounter = 0

  // 加载HS编码选项
  await loadHsOptions()

  // 1. 优先加载已保存的20%数据
  try {
    const res = await getInvoiceSplitItems(props.formId)
    const savedItems = res.data?.data
    if (Array.isArray(savedItems) && savedItems.length > 0) {
      items.value = savedItems.map((item: any) => ({
        key: keyCounter++,
        hsCode: item.hsCode || '',
        productName: item.productName || '',
        spec: item.spec || '',
        quantity: Number(item.quantity) || 0,
        unitPrice: Number(item.unitPrice) || 0,
        amount: Number(item.amount) || 0,
        amountManual: true,  // 已保存数据视为手动调整过
      }))
      return
    }
  } catch {
    // ignore
  }

  // 2. 无保存记录时，从 calcDetail 预填（按开票金额比例 × 0.2）
  const products: ProductTaxDetail[] = props.calcDetail?.productTaxDetails || []
  if (products.length > 0) {
    // 计算 invoiceAmount / totalGoodsAmount 比例
    const totalGoodsAmt = Number(props.calcDetail?.totalGoodsAmount) || 0
    const invoiceAmt = Number(props.calcDetail?.invoiceAmount) || 0
    const invoiceRatio = (invoiceAmt > 0 && totalGoodsAmt > 0) ? invoiceAmt / totalGoodsAmt : 1
    items.value = products.map(pd => buildItemFromProductTaxDetail(pd, invoiceRatio))
  } else {
    addRow()
  }
}

async function handleConfirm() {
  if (items.value.length === 0) {
    addRow()
    return
  }
  if (isOverLimit.value) {
    return
  }
  confirming.value = true
  try {
    // 保存到数据库
    await saveInvoiceSplitItems(props.formId, items.value)
    emit('confirm', [...items.value])
    visible.value = false
  } catch (e: any) {
    console.error('保存20%拆分数据失败', e)
  } finally {
    confirming.value = false
  }
}

function handleCancel() {
  visible.value = false
}

/** 金额转中文大写 */
function convertToChinese(amount: number): string {
  if (!amount && amount !== 0) return '-'
  const cnNums = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
  const cnIntRadice = ['', '拾', '佰', '仟']
  const cnIntUnits = ['', '万', '亿', '兆']
  const cnDecUnits = ['角', '分']
  const cnInteger = '整'
  const cnIntLast = '元'

  if (amount === 0) return '零元整'
  let numStr = amount.toFixed(2)
  let [intStr, decStr] = numStr.split('.')
  let result = ''

  if (intStr && intStr !== '0') {
    let intVal = BigInt(intStr)
    let intPart = intVal.toString()
    let zeroFlag = false
    for (let i = 0; i < intPart.length; i++) {
      let d = parseInt(intPart[i])
      let pos = intPart.length - 1 - i
      let unitIdx = pos % 4
      let groupIdx = Math.floor(pos / 4)
      if (d === 0) {
        zeroFlag = true
        if (unitIdx === 0 && groupIdx > 0) {
          result += cnIntUnits[groupIdx]
          zeroFlag = false
        }
      } else {
        if (zeroFlag) { result += '零'; zeroFlag = false }
        result += cnNums[d] + cnIntRadice[unitIdx]
        if (unitIdx === 0 && groupIdx > 0) result += cnIntUnits[groupIdx]
      }
    }
    result += cnIntLast
  }

  if (decStr) {
    let jiao = parseInt(decStr[0]) || 0
    let fen = parseInt(decStr[1]) || 0
    if (jiao > 0) result += cnNums[jiao] + cnDecUnits[0]
    if (fen > 0) result += cnNums[fen] + cnDecUnits[1]
  } else {
    result += cnInteger
  }
  if (!decStr || (parseInt(decStr[0]) === 0 && parseInt(decStr[1]) === 0)) {
    result += cnInteger
  }
  return result
}

defineExpose({ open })
</script>
