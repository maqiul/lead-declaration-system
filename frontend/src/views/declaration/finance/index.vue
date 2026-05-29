<template>
  <div class="app-container">
    <a-card :bordered="false">
      <div class="mb-4" style="display: flex; justify-content: space-between; align-items: center;">
        <span style="font-size: 16px; font-weight: 600;">退税点维护</span>
        <a-button type="primary" @click="handleAddNew" v-permission="['business:declaration:finance:supplement']">
          <template #icon><PlusOutlined /></template>
          新增退税点
        </a-button>
      </div>
      <a-table
        :columns="columns"
        :data-source="dataList"
        :pagination="pagination"
        :loading="loading"
        :scroll="{ x: 1200 }"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'formNo'">
            <a-button type="link" @click="handleViewDeclaration(record.formId)">
              {{ record.formNo }}
            </a-button>
          </template>

          <template v-if="column.key === 'action'">
            <a-button type="link" @click="handleEdit(record)" v-permission="['business:declaration:finance:supplement']">
              <template #icon><EditOutlined /></template>
              编辑
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 选择申报单（尚无财务单证、且已过资料提交环节） -->
    <a-modal
      v-model:visible="pickModalVisible"
      title="新增退税点 - 选择申报单"
      width="820px"
      :footer="null"
      destroy-on-close
    >
      <a-alert
        type="info"
        show-icon
        message="仅列出资料已提交、尚未维护退税点的申报单"
        style="margin-bottom: 12px"
      />
      <div style="margin-bottom: 12px;">
        <a-input-search
          v-model:value="pickFormNoFilter"
          placeholder="按申报单号筛选"
          enter-button="查询"
          :loading="pickLoading"
          allow-clear
          @search="loadEligibleDeclarations(1)"
        />
      </div>
      <a-table
        :columns="pickColumns"
        :data-source="pickList"
        :pagination="pickPagination"
        :loading="pickLoading"
        row-key="id"
        size="small"
        @change="handlePickTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            {{ getStatusText(record.status) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="handleSelectDeclaration(record)">选择</a-button>
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 退税点编辑弹窗 -->
    <a-modal
      v-model:visible="editModalVisible"
      :title="editRecord?.id ? `退税点设置 - ${editRecord?.formNo || ''}` : `新增退税点 - ${editRecord?.formNo || ''}`"
      width="420px"
      :confirmLoading="saveLoading"
      @ok="handleSave"
      @cancel="editModalVisible = false"
      okText="保存"
      cancelText="取消"
    >
      <a-form layout="vertical" style="padding: 16px 0;">
        <a-form-item v-if="!editRecord?.id" label="申报单号">
          <a-input :value="editRecord?.formNo" disabled />
        </a-form-item>
        <a-form-item label="退税点 (%)">
          <a-input-number
            v-model:value="editTaxRefundRate"
            style="width: 100%"
            :min="0"
            :max="100"
            :precision="2"
            placeholder="如: 13 表示 13%"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { EditOutlined, PlusOutlined } from '@ant-design/icons-vue'
import {
  getFinancialSupplementList,
  getEligibleDeclarationsForFinance,
  createFinancialSupplement,
  updateFinancialSupplement,
} from '@/api/business/declaration'

const router = useRouter()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const loading = ref(false)
const dataList = ref([])

const statusMap: Record<number, string> = {
  0: '草稿', 1: '待初审', 2: '待资料提交', 3: '待资料审核',
  4: '待补充资料提交', 5: '待补充资料审核', 6: '待开票金额提交', 7: '待开票金额审核',
  8: '待发票提交', 9: '待发票审核', 10: '已完成', 11: '退回待审'
}
const getStatusText = (s: number) => statusMap[s] ?? '未知'

const columns = [
  { title: '序号', dataIndex: 'index', key: 'index', width: 50, customRender: ({ index }: { index: number }) => index + 1 },
  { title: '申报单号', dataIndex: 'formNo', key: 'formNo', width: 180 },
  { title: '发货人', dataIndex: 'shipperCompany', key: 'shipperCompany', width: 140, ellipsis: true },
  { title: '收货人', dataIndex: 'consigneeCompany', key: 'consigneeCompany', width: 140, ellipsis: true },
  { title: '申报金额', dataIndex: 'declarationAmount', key: 'declarationAmount', width: 120, customRender: ({ text, record }: any) => {
    if (text == null) return '-'
    const c = record.declarationCurrency || 'CNY'
    return `${c === 'CNY' ? '¥' : c === 'USD' ? '$' : c}${Number(text).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
  }},
  { title: '箱数', dataIndex: 'totalCartons', key: 'totalCartons', width: 60 },
  { title: '退税点(%)', dataIndex: 'taxRefundRate', key: 'taxRefundRate', width: 90, customRender: ({ text }: any) => text != null ? `${text}%` : '未设置' },
  { title: '申请开票金额', dataIndex: 'requestedInvoiceAmount', key: 'requestedInvoiceAmount', width: 120, customRender: ({ text }: any) => text != null ? `¥${Number(text).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}` : '-' },
  { title: '申报状态', dataIndex: 'declarationStatus', key: 'declarationStatus', width: 110, customRender: ({ text }: any) => getStatusText(text) },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const }
]

const getList = async () => {
  loading.value = true
  try {
    const res = await getFinancialSupplementList(queryParams)
    dataList.value = res.data.data.records
    pagination.total = res.data.data.total
  } catch (error) {
    console.error('获取列表失败', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
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

// ========== 选择申报单 ==========
const pickModalVisible = ref(false)
const pickFormNoFilter = ref('')
const pickLoading = ref(false)
const pickList = ref<any[]>([])
const pickPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const pickColumns = [
  { title: '申报单号', dataIndex: 'formNo', key: 'formNo', width: 160 },
  { title: '状态', key: 'status', width: 120 },
  { title: '发货人', dataIndex: 'shipperCompany', key: 'shipperCompany', ellipsis: true },
  { title: '收货人', dataIndex: 'consigneeCompany', key: 'consigneeCompany', ellipsis: true },
  { title: '申报金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 110, customRender: ({ text, record }: any) => {
    if (text == null) return '-'
    const c = record.currency || 'CNY'
    return `${c === 'CNY' ? '¥' : c === 'USD' ? '$' : c}${Number(text).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
  }},
  { title: '操作', key: 'action', width: 72, fixed: 'right' as const }
]

const loadEligibleDeclarations = async (page = pickPagination.current) => {
  pickLoading.value = true
  try {
    const res = await getEligibleDeclarationsForFinance({
      current: page,
      size: pickPagination.pageSize,
      formNo: pickFormNoFilter.value.trim() || undefined
    })
    if (res.data?.code === 200 && res.data.data) {
      pickList.value = res.data.data.records || []
      pickPagination.total = res.data.data.total ?? 0
      pickPagination.current = res.data.data.current ?? page
    } else {
      pickList.value = []
      pickPagination.total = 0
    }
  } catch (e) {
    console.error('加载可选申报单失败', e)
    message.error('加载申报单列表失败')
    pickList.value = []
  } finally {
    pickLoading.value = false
  }
}

const handlePickTableChange = (pag: any) => {
  pickPagination.current = pag.current
  pickPagination.pageSize = pag.pageSize
  loadEligibleDeclarations(pag.current)
}

const handleAddNew = () => {
  pickFormNoFilter.value = ''
  pickPagination.current = 1
  pickModalVisible.value = true
  loadEligibleDeclarations(1)
}

const handleSelectDeclaration = (form: any) => {
  pickModalVisible.value = false
  editRecord.value = { formId: form.id, formNo: form.formNo }
  editTaxRefundRate.value = undefined
  editModalVisible.value = true
}

// ========== 编辑/保存 ==========
const editModalVisible = ref(false)
const editRecord = ref<any>(null)
const editTaxRefundRate = ref<number | undefined>(undefined)
const saveLoading = ref(false)

const handleEdit = (record: any) => {
  editRecord.value = record
  editTaxRefundRate.value = record.taxRefundRate ?? undefined
  editModalVisible.value = true
}

const handleSave = async () => {
  if (!editRecord.value?.id && !editRecord.value?.formId) {
    message.warning('请先选择申报单')
    return
  }
  saveLoading.value = true
  try {
    const payload = {
      formId: editRecord.value.formId,
      formNo: editRecord.value.formNo,
      taxRefundRate: editTaxRefundRate.value,
    }
    if (editRecord.value.id) {
      await updateFinancialSupplement(editRecord.value.id, payload)
    } else {
      await createFinancialSupplement(payload)
    }
    message.success('保存成功')
    editModalVisible.value = false
    getList()
  } catch (error: any) {
    console.error('保存失败', error)
    message.error(error.response?.data?.message || '保存失败')
  } finally {
    saveLoading.value = false
  }
}

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
</style>
