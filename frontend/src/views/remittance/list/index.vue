<template>
  <div class="remittance-list-container">
    <a-card :bordered="false">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <a-form layout="inline" :model="searchForm" class="search-form">
          <a-form-item label="水单编号">
            <a-input v-model:value="searchForm.remittanceNo" placeholder="请输入水单编号" allow-clear style="width: 180px" />
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="searchForm.status" placeholder="请选择" allow-clear style="width: 120px">
              <a-select-option :value="0">草稿</a-select-option>
              <a-select-option :value="1">待审核</a-select-option>
              <a-select-option :value="2">已审核</a-select-option>
              <a-select-option :value="3">已驳回</a-select-option>
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
        <a-button type="primary" @click="handleCreate" v-permission="['business:remittance:create']">
          <template #icon><PlusOutlined /></template>
          创建水单
        </a-button>
      </div>

      <!-- 数据权限提示 -->
      <a-alert
        message="数据权限提示"
        description="普通用户只能查看自己创建的水单，管理员和财务经理可以查看所有水单"
        type="info"
        show-icon
        closable
        class="mb-4"
      />

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="remittanceList"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>

          <template v-if="column.key === 'remittanceAmount'">
            <span>{{ formatCurrency(record.remittanceAmount, record.currency) }}</span>
          </template>

          <template v-if="column.key === 'creditedAmount'">
            <span v-if="record.creditedAmount">{{ formatCurrency(record.creditedAmount, record.currency) }}</span>
            <span v-else>-</span>
          </template>

          <template v-if="column.key === 'bankFee'">
            <span v-if="record.bankFee">{{ formatCurrency(record.bankFee, record.currency) }}</span>
            <span v-else>-</span>
          </template>

          <template v-if="column.key === 'totalRelatedAmount'">
            <span v-if="record.totalRelatedAmount && record.totalRelatedAmount > 0">
              <a-tag :color="record.totalRelatedAmount >= record.remittanceAmount ? 'green' : 'orange'">
                {{ formatCurrency(record.totalRelatedAmount, record.currency) }}
              </a-tag>
            </span>
            <span v-else style="color: #999;">-</span>
          </template>

          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record as Remittance)">
                <template #icon><EyeOutlined /></template>
                查看
              </a-button>
              <a-button
                type="link"
                size="small"
                @click="handleEdit(record as Remittance)"
                v-if="record.status === 0"
                v-permission="['business:remittance:update']"
              >
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button
                type="link"
                size="small"
                @click="handleSubmit(record as Remittance)"
                v-if="record.status === 0"
                v-permission="['business:remittance:submit']"
              >
                <template #icon><SendOutlined /></template>
                提交审核
              </a-button>
              <a-button
                type="link"
                size="small"
                @click="handleAudit(record as Remittance)"
                v-if="record.status === 1"
                v-permission="['business:remittance:audit']"
              >
                <template #icon><AuditOutlined /></template>
                审核
              </a-button>
              <a-button
                type="link"
                size="small"
                @click="handleDirectAudit(record as Remittance)"
                v-if="record.status === 0"
                v-permission="['business:remittance:audit']"
                style="color: #fa8c16"
              >
                <template #icon><ThunderboltOutlined /></template>
                直接审核
              </a-button>
              <a-button
                type="link"
                size="small"
                @click="handleDelete(record as Remittance)"
                v-if="record.status === 0"
                danger
                v-permission="['business:remittance:delete']"
              >
                <template #icon><DeleteOutlined /></template>
                删除
              </a-button>
              <a-button
                type="link"
                size="small"
                @click="handleManageForms(record as Remittance)"
                v-permission="['business:remittance:update']"
              >
                <template #icon><LinkOutlined /></template>
                管理关联
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 水单创建/编辑弹窗 -->
    <RemittanceModal
      v-model:visible="modalVisible"
      :remittance-data="currentRemittance"
      @success="loadRemittanceList"
    />

    <!-- 水单详情弹窗 -->
    <RemittanceDetail
      v-model:visible="detailVisible"
      :remittance-id="currentRemittanceId"
    />

    <!-- 水单审核弹窗 -->
    <AuditModal
      v-model:visible="auditVisible"
      :remittance-id="currentRemittanceId"
      @success="loadRemittanceList"
    />

    <!-- 管理关联申报单弹窗 -->
    <FormRelationModal
      v-model:visible="relationVisible"
      :remittance-id="currentRemittanceId"
      :remittance-amount="currentRemittanceAmount"
      @success="loadRemittanceList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SearchOutlined, PlusOutlined, ReloadOutlined, EyeOutlined, EditOutlined, SendOutlined, AuditOutlined, ThunderboltOutlined, DeleteOutlined, LinkOutlined } from '@ant-design/icons-vue'
import { getRemittanceList, deleteRemittance, submitRemittanceAudit } from '@/api/business/remittance'
import type { Remittance, RemittanceQueryParams } from '@/api/business/remittance'
import RemittanceModal from './components/RemittanceModal.vue'
import RemittanceDetail from './components/RemittanceDetail.vue'
import AuditModal from './components/AuditModal.vue'
import FormRelationModal from './components/FormRelationModal.vue'

// 数据定义
const remittanceList = ref<Remittance[]>([])
const loading = ref(false)
const searchForm = reactive({
  remittanceNo: undefined as string | undefined,
  remittanceType: undefined as number | undefined,
  status: undefined as number | undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// 弹窗控制
const modalVisible = ref(false)
const detailVisible = ref(false)
const auditVisible = ref(false)
const relationVisible = ref(false)
const currentRemittance = ref<Partial<Remittance>>({})
const currentRemittanceId = ref<number>(0)

// 表格列定义
const columns = [
  {
    title: '水单编号',
    dataIndex: 'remittanceNo',
    key: 'remittanceNo',
    width: 180
  },
  {
    title: '收汇名称',
    dataIndex: 'remittanceName',
    key: 'remittanceName',
    width: 150
  },
  {
    title: '收汇日期',
    dataIndex: 'remittanceDate',
    key: 'remittanceDate',
    width: 120,
    customRender: ({ text }: any) => {
      if (!text) return '-'
      // 只显示年月日，去掉时间部分
      return text.split(' ')[0]
    }
  },
  {
    title: '收汇金额',
    dataIndex: 'remittanceAmount',
    key: 'remittanceAmount',
    width: 120
  },
  {
    title: '币种',
    dataIndex: 'currency',
    key: 'currency',
    width: 80
  },
  {
    title: '入账金额',
    dataIndex: 'creditedAmount',
    key: 'creditedAmount',
    width: 120
  },
  {
    title: '手续费',
    dataIndex: 'bankFee',
    key: 'bankFee',
    width: 100
  },
  {
    title: '汇率',
    dataIndex: 'taxRate',
    key: 'taxRate',
    width: 100
  },
  {
    title: '银行名称',
    dataIndex: 'bankAccountName',
    key: 'bankAccountName',
    width: 150
  },
  {
    title: '已关联金额',
    dataIndex: 'totalRelatedAmount',
    key: 'totalRelatedAmount',
    width: 120
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100
  },
  {
    title: '操作',
    key: 'action',
    width: 350,
    fixed: 'right' as const
  }
]

// 加载水单列表
const loadRemittanceList = async () => {
  loading.value = true
  try {
    const params: RemittanceQueryParams = {
      current: pagination.current,
      size: pagination.pageSize,
      ...searchForm
    }
    const response = await getRemittanceList(params)
    
    // 兼容多种响应格式
    let data = response.data
    if (data?.code === 200) {
      remittanceList.value = data.data?.records || []
      pagination.total = data.data?.total || 0
    } else if (Array.isArray(data)) {
      remittanceList.value = data
      pagination.total = data.length
    }
  } catch (error) {
    message.error('加载水单列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadRemittanceList()
}

// 重置
const handleReset = () => {
  searchForm.remittanceNo = undefined
  searchForm.status = undefined
  handleSearch()
}

// 表格变化
const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadRemittanceList()
}

// 创建水单
const handleCreate = () => {
  currentRemittance.value = {}
  modalVisible.value = true
}

// 编辑水单
const handleEdit = (record: Remittance) => {
  currentRemittance.value = { ...record }
  modalVisible.value = true
}

// 查看水单
const handleView = (record: Remittance) => {
  currentRemittanceId.value = record.id || 0
  detailVisible.value = true
}

// 提交审核
const handleSubmit = (record: Remittance) => {
  Modal.confirm({
    title: '确认提交',
    content: `确定要提交水单 ${record.remittanceNo} 进行审核吗?`,
    onOk: async () => {
      try {
        await submitRemittanceAudit(record.id!)
        message.success('提交成功')
        loadRemittanceList()
      } catch (error) {
        message.error('提交失败')
      }
    }
  })
}

// 直接审核（先提交再审核）
const handleDirectAudit = async (record: Remittance) => {
  try {
    await submitRemittanceAudit(record.id!)
    message.success('已提交审核，请填写审核信息')
    currentRemittanceId.value = record.id || 0
    auditVisible.value = true
  } catch (error) {
    message.error('提交审核失败')
  }
}

// 审核水单
const handleAudit = (record: Remittance) => {
  currentRemittanceId.value = record.id || 0
  auditVisible.value = true
}

// 删除水单
const handleDelete = (record: Remittance) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除水单 ${record.remittanceNo} 吗?`,
    onOk: async () => {
      try {
        await deleteRemittance(record.id!)
        message.success('删除成功')
        loadRemittanceList()
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

// 管理关联申报单
const currentRemittanceAmount = ref<number>(0)

const handleManageForms = (record: Remittance) => {
  currentRemittanceId.value = record.id || 0
  currentRemittanceAmount.value = record.remittanceAmount || 0
  relationVisible.value = true
}

// 获取状态颜色
const getStatusColor = (status: number | undefined) => {
  const colorMap: Record<number, string> = {
    0: 'default',
    1: 'processing',
    2: 'success',
    3: 'error'
  }
  return colorMap[status || 0] || 'default'
}

// 获取状态文本
const getStatusText = (status: number | undefined) => {
  const textMap: Record<number, string> = {
    0: '草稿',
    1: '待审核',
    2: '已审核',
    3: '已驳回'
  }
  return textMap[status || 0] || '未知'
}

// 格式化货币
const formatCurrency = (amount: number | undefined, currency: string | undefined) => {
  if (amount === undefined || amount === null) return '-'
  return `${amount.toFixed(2)} ${currency || 'USD'}`
}

// 初始化
onMounted(() => {
  loadRemittanceList()
})
</script>

<style scoped>
.remittance-list-container {
  padding: 24px;
}

.search-bar {
  margin-bottom: 16px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.search-form :deep(.ant-form-item) {
  margin-bottom: 8px;
}

.button-item {
  display: flex;
  align-items: center;
}

.toolbar {
  margin-bottom: 16px;
}
</style>
