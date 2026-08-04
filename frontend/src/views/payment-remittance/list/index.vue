<template>
  <div class="payment-remittance-list-container">
    <a-card :bordered="false">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <a-form layout="inline" :model="searchForm" class="search-form">
          <a-form-item label="出款编号">
            <a-input v-model:value="searchForm.paymentNo" placeholder="请输入出款编号" allow-clear style="width: 180px" />
          </a-form-item>
          <a-form-item v-if="showRelationFilter" label="关联状态">
            <a-select v-model:value="searchForm.relationStatus" placeholder="请选择" allow-clear style="width: 140px">
              <a-select-option value="UNRELATED">未关联</a-select-option>
              <a-select-option value="PARTIAL">未完全关联</a-select-option>
              <a-select-option value="RELATED">已关联</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item class="button-item">
            <a-space>
              <a-button type="primary" @click="handleSearch">
                <template #icon><SearchOutlined /></template>
                查询
              </a-button>
              <a-button @click="handleReset">
                <template #icon><ReloadOutlined /></template>
                重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 操作栏 -->
      <div class="toolbar">
        <a-button type="primary" @click="handleCreate" v-permission="['business:payment-remittance:create']">
          <template #icon><PlusOutlined /></template>
          创建出款水单
        </a-button>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="remittanceList"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1800 }"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>

          <template v-if="column.key === 'paymentAmount'">
            <span>{{ formatCurrency(record.paymentAmount, record.currency) }}</span>
          </template>

          <template v-if="column.key === 'totalRelatedAmount'">
            <span v-if="record.totalRelatedAmount && record.totalRelatedAmount > 0">
              <a-tag :color="record.totalRelatedAmount >= record.paymentAmount ? 'green' : 'orange'">
                {{ formatCurrency(record.totalRelatedAmount, record.currency) }}
              </a-tag>
            </span>
            <span v-else style="color: #999;">-</span>
          </template>

          <template v-if="column.key === 'unrelatedAmount'">
            <span :style="{ color: getUnrelatedAmount(record) > 0 ? '#ff4d4f' : '#52c41a', fontWeight: 500 }">
              {{ formatCurrency(getUnrelatedAmount(record), record.currency) }}
            </span>
          </template>

          <template v-if="column.key === 'relationStatus'">
            <a-tag :color="getRelationStatusColor(record)">
              {{ getRelationStatusText(record) }}
            </a-tag>
          </template>

          <template v-if="column.key === 'action'">
            <a-space :size="2">
              <a-button type="link" size="small" style="padding: 0 4px" @click="handleView(record as PaymentRemittance)" v-permission="['business:payment-remittance:view']">
                <template #icon><EyeOutlined /></template>
                查看
              </a-button>
              <a-button
                type="link"
                size="small"
                style="padding: 0 4px"
                @click="handleEdit(record as PaymentRemittance)"
                v-if="record.status === 0"
                v-permission="['business:payment-remittance:update']"
              >
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button
                type="link"
                size="small"
                style="padding: 0 4px"
                @click="handleSubmit(record as PaymentRemittance)"
                v-if="record.status === 0"
                v-permission="['business:payment-remittance:submit']"
              >
                <template #icon><SendOutlined /></template>
                提交审核
              </a-button>
              <a-button
                type="link"
                size="small"
                style="padding: 0 4px"
                @click="handleAudit(record as PaymentRemittance)"
                v-if="record.status === 1"
                v-permission="['business:payment-remittance:audit']"
              >
                <template #icon><AuditOutlined /></template>
                审核
              </a-button>
              <a-button
                type="link"
                size="small"
                @click="handleDirectAudit(record as PaymentRemittance)"
                v-if="record.status === 0"
                v-permission="['business:payment-remittance:audit']"
                style="color: #fa8c16; padding: 0 4px"
              >
                <template #icon><ThunderboltOutlined /></template>
                直接审核
              </a-button>
              <a-button
                type="link"
                size="small"
                @click="handleRevokeAudit(record as PaymentRemittance)"
                v-if="record.status === 2"
                v-permission="['business:payment-remittance:revoke-audit']"
                style="color: #fa541c; padding: 0 4px"
              >
                <template #icon><RollbackOutlined /></template>
                反审核
              </a-button>
              <a-button
                type="link"
                size="small"
                style="padding: 0 4px"
                @click="handleDelete(record as PaymentRemittance)"
                v-if="record.status === 0"
                danger
                v-permission="['business:payment-remittance:delete']"
              >
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
              <a-button
                type="link"
                size="small"
                style="padding: 0 4px"
                @click="handleManageForms(record as PaymentRemittance)"
                v-permission="['business:payment-remittance:update']"
              >
                <template #icon><LinkOutlined /></template>
                管理关联
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 创建/编辑弹窗 -->
    <PaymentRemittanceModal
      v-model:visible="modalVisible"
      :remittance-data="currentRemittance"
      @success="loadList"
    />

    <!-- 详情弹窗 -->
    <PaymentRemittanceDetail
      v-model:visible="detailVisible"
      :remittance-id="currentRemittanceId"
    />

    <!-- 审核弹窗 -->
    <AuditModal
      v-model:visible="auditVisible"
      :remittance-id="currentRemittanceId"
      @success="loadList"
    />

    <!-- 管理关联弹窗 -->
    <FormRelationModal
      v-model:visible="relationVisible"
      :remittance-id="currentRemittanceId"
      :remittance-amount="currentRemittanceAmount"
      @success="loadList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { SearchOutlined, PlusOutlined, ReloadOutlined, EyeOutlined, EditOutlined, SendOutlined, AuditOutlined, ThunderboltOutlined, DeleteOutlined, LinkOutlined, RollbackOutlined } from '@ant-design/icons-vue'
import { getPaymentRemittanceList, deletePaymentRemittance, submitPaymentRemittanceAudit, revokePaymentRemittanceAudit } from '@/api/business/paymentRemittance'
import type { PaymentRemittance, PaymentRemittanceQueryParams } from '@/api/business/paymentRemittance'
import PaymentRemittanceModal from './components/PaymentRemittanceModal.vue'
import PaymentRemittanceDetail from './components/PaymentRemittanceDetail.vue'
import AuditModal from './components/AuditModal.vue'
import FormRelationModal from './components/FormRelationModal.vue'
import { formatDate } from '@/utils/common'

const route = useRoute()

// 路由名 -> 状态筛选映射
const routeStatusMap: Record<string, number | undefined> = {
  PaymentRemittanceDraft: 0,
  PaymentRemittancePending: 1,
  PaymentRemittanceAudited: 2,
  PaymentRemittanceUnrelated: undefined
}

const showRelationFilter = computed(() => route.name === 'PaymentRemittanceAudited')

const applyRouteFilter = () => {
  const routeName = route.name as string
  if (routeName === 'PaymentRemittanceUnrelated') {
    searchForm.status = 2
    searchForm.relationStatus = 'UNRELATED'
  } else {
    searchForm.status = routeStatusMap[routeName]
    searchForm.relationStatus = undefined
  }
}

const remittanceList = ref<PaymentRemittance[]>([])
const loading = ref(false)
const searchForm = reactive({
  paymentNo: undefined as string | undefined,
  status: undefined as number | undefined,
  relationStatus: undefined as string | undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const modalVisible = ref(false)
const detailVisible = ref(false)
const auditVisible = ref(false)
const relationVisible = ref(false)
const currentRemittance = ref<Partial<PaymentRemittance>>({})
const currentRemittanceId = ref<number>(0)
const currentRemittanceAmount = ref<number>(0)

const columns = [
  { title: '出款编号', dataIndex: 'paymentNo', key: 'paymentNo', width: 180 },
  { title: '收款人', dataIndex: 'payeeName', key: 'payeeName', width: 150 },
  { title: '出款日期', dataIndex: 'paymentDate', key: 'paymentDate', width: 120 , customRender: ({ text }: any) => text ? formatDate(text, 'yyyy-MM-dd') : '-' },
  { title: '出款金额', dataIndex: 'paymentAmount', key: 'paymentAmount', width: 120 },
  { title: '币种', dataIndex: 'currency', key: 'currency', width: 80 },
  { title: '银行名称', dataIndex: 'bankAccountName', key: 'bankAccountName', width: 150 },
  { title: '已关联金额', dataIndex: 'totalRelatedAmount', key: 'totalRelatedAmount', width: 120 },
  { title: '未关联金额', key: 'unrelatedAmount', width: 120 },
  { title: '关联状态', key: 'relationStatus', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 460, fixed: 'right' as const }
]

const loadList = async () => {
  loading.value = true
  try {
    const params: PaymentRemittanceQueryParams = {
      current: pagination.current,
      size: pagination.pageSize,
      ...searchForm
    }
    const response = await getPaymentRemittanceList(params)
    let data = response.data
    if (data?.code === 200) {
      remittanceList.value = data.data?.records || []
      pagination.total = data.data?.total || 0
    } else if (Array.isArray(data)) {
      remittanceList.value = data
      pagination.total = data.length
    }
  } catch (error) {
    message.error('加载出款水单列表失败')
  } finally {
    loading.value = false
  }
}

watch(() => route.name, () => {
  applyRouteFilter()
  pagination.current = 1
  loadList()
})

const handleSearch = () => { pagination.current = 1; loadList() }
const handleReset = () => { searchForm.paymentNo = undefined; searchForm.relationStatus = undefined; handleSearch() }
const handleTableChange = (pag: any) => { pagination.current = pag.current; pagination.pageSize = pag.pageSize; loadList() }
const handleCreate = () => { currentRemittance.value = {}; modalVisible.value = true }
const handleEdit = (record: PaymentRemittance) => { currentRemittance.value = { ...record }; modalVisible.value = true }
const handleView = (record: PaymentRemittance) => { currentRemittanceId.value = Number(record.id) || 0; detailVisible.value = true }

const handleSubmit = (record: PaymentRemittance) => {
  Modal.confirm({
    title: '确认提交',
    content: `确定要提交出款水单 ${record.paymentNo} 进行审核吗?`,
    onOk: async () => {
      try {
        await submitPaymentRemittanceAudit(record.id!)
        message.success('提交成功')
        loadList()
      } catch (error) { message.error('提交失败') }
    }
  })
}

const handleDirectAudit = async (record: PaymentRemittance) => {
  try {
    await submitPaymentRemittanceAudit(record.id!)
    message.success('已提交审核，请填写审核信息')
    currentRemittanceId.value = Number(record.id) || 0
    auditVisible.value = true
  } catch (error) { message.error('提交审核失败') }
}

const handleAudit = (record: PaymentRemittance) => { currentRemittanceId.value = Number(record.id) || 0; auditVisible.value = true }

const handleRevokeAudit = (record: PaymentRemittance) => {
  Modal.confirm({
    title: '确认反审核',
    content: `确定要对出款水单 ${record.paymentNo} 执行反审核吗？`,
    okText: '确认反审核',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await revokePaymentRemittanceAudit(record.id!)
        message.success('反审核成功')
        loadList()
      } catch (error) { message.error('反审核失败') }
    }
  })
}

const handleDelete = (record: PaymentRemittance) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除出款水单 ${record.paymentNo} 吗?`,
    onOk: async () => {
      try {
        await deletePaymentRemittance(record.id!)
        message.success('删除成功')
        loadList()
      } catch (error) { message.error('删除失败') }
    }
  })
}

const handleManageForms = (record: PaymentRemittance) => {
  currentRemittanceId.value = Number(record.id) || 0
  currentRemittanceAmount.value = Number(record.paymentAmount) || 0
  relationVisible.value = true
}

const getStatusColor = (status: number | undefined) => ({ 0: 'default', 1: 'processing', 2: 'success', 3: 'error' }[status || 0] || 'default')
const getStatusText = (status: number | undefined) => ({ 0: '草稿', 1: '待审核', 2: '已审核', 3: '已驳回' }[status || 0] || '未知')
const getRelationStatusColor = (record: Record<string, any>) => {
  const related = record.totalRelatedAmount || 0
  const total = record.paymentAmount || 0
  if (related <= 0) return 'default'
  if (related >= total) return 'green'
  return 'orange'
}
const getUnrelatedAmount = (record: Record<string, any>): number => {
  const diff = Number(record.paymentAmount || 0) - Number(record.totalRelatedAmount || 0)
  return diff > 0 ? Math.round(diff * 100) / 100 : 0
}
const getRelationStatusText = (record: Record<string, any>) => {
  const related = record.totalRelatedAmount || 0
  const total = record.paymentAmount || 0
  if (related <= 0) return '未关联'
  if (related >= total) return '已关联'
  return '未完全关联'
}
const formatCurrency = (amount: number | undefined, currency: string | undefined) => {
  if (amount === undefined || amount === null) return '-'
  return `${amount.toFixed(2)} ${currency || 'USD'}`
}

onMounted(() => { applyRouteFilter(); loadList() })
</script>

<style scoped>
.payment-remittance-list-container { padding: 24px; }
.search-bar { margin-bottom: 16px; }
.search-form { display: flex; flex-wrap: wrap; align-items: center; }
.search-form :deep(.ant-form-item) { margin-bottom: 8px; }
.button-item { display: flex; align-items: center; }
.toolbar { margin-bottom: 16px; }
</style>