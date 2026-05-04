<template>
  <div class="remittance-audit-container">
    <a-card :bordered="false">
      <a-alert message="水单审核" description="请对待审核的水单进行审核,填写汇率并选择汇款银行" type="info" show-icon style="margin-bottom: 16px" />

      <!-- 搜索栏 -->
      <div class="search-bar">
        <a-form layout="inline" :model="searchForm" class="search-form">
          <a-form-item label="水单编号">
            <a-input v-model:value="searchForm.remittanceNo" placeholder="请输入水单编号" allow-clear style="width: 180px" />
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
          <template v-if="column.key === 'remittanceAmount'">
            <span>{{ record.remittanceAmount?.toFixed(2) }} {{ record.currency || 'USD' }}</span>
          </template>

          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record as Remittance)">
                <template #icon><EyeOutlined /></template>
                查看
              </a-button>
              <a-button type="primary" size="small" @click="handleAudit(record as Remittance)">
                <template #icon><AuditOutlined /></template>
                审核
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 水单审核弹窗 -->
    <a-modal
      v-model:open="auditVisible"
      title="水单审核"
      width="800px"
      :confirm-loading="submitLoading"
      @ok="handleAuditSubmit"
      @cancel="auditVisible = false"
    >
      <a-form :model="auditForm" layout="vertical" v-if="currentRemittance">
        <!-- 水单基本信息 -->
        <a-card title="水单基本信息" size="small" style="margin-bottom: 16px">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="水单编号">
                <span>{{ currentRemittance.remittanceNo }}</span>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="水单类型">
                <a-tag :color="currentRemittance.remittanceType === 1 ? 'blue' : 'green'">
                  {{ currentRemittance.remittanceType === 1 ? '定金' : '尾款' }}
                </a-tag>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="收汇名称">
                <span>{{ currentRemittance.remittanceName }}</span>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="收汇日期">
                <span>{{ currentRemittance.remittanceDate }}</span>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="收汇金额">
                <span style="font-size: 16px; font-weight: bold; color: #1890ff">
                  {{ currentRemittance.remittanceAmount?.toFixed(2) }} {{ currentRemittance.currency || 'USD' }}
                </span>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="水单文件">
                <template v-if="currentRemittance.photoUrl">
                  <a-image
                    v-if="isImage(currentRemittance.photoUrl)"
                    :src="currentRemittance.photoUrl"
                    style="width: 100px; height: 60px"
                  />
                  <a-button v-else type="link" size="small" @click="openFile(currentRemittance.photoUrl)">
                    <FilePdfOutlined /> 查看文件
                  </a-button>
                </template>
                <span v-else>无文件</span>
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>

        <!-- 审核信息 -->
        <a-card title="审核信息" size="small">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="汇率" required>
                <a-input-number
                  v-model:value="auditForm.taxRate"
                  :min="0"
                  :max="99"
                  :precision="4"
                  :step="0.0001"
                  placeholder="请输入汇率，如 7.2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="汇款银行" required>
                <a-select
                  v-model:value="auditForm.bankAccountId"
                  placeholder="请选择银行"
                  show-search
                  :filter-option="filterBankOption"
                  @change="handleBankChange"
                >
                  <a-select-option v-for="bank in bankList" :key="bank.id" :value="bank.id">
                    {{ bank.bankName }} - {{ bank.accountName }} ({{ bank.currency }})
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="银行手续费率">
                <a-input-number
                  :value="auditForm.bankFeeRate"
                  disabled
                  style="width: 100%"
                  addon-after="%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="银行手续费">
                <a-input-number
                  :value="auditForm.bankFee"
                  disabled
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="入账金额">
                <a-input-number
                  :value="auditForm.creditedAmount"
                  disabled
                  style="width: 100%"
                  :formatter="(value: any) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-divider />

          <a-form-item label="审核备注">
            <a-textarea
              v-model:value="auditForm.auditRemark"
              placeholder="请输入审核备注(选填)"
              :rows="3"
            />
          </a-form-item>
        </a-card>
      </a-form>
    </a-modal>

    <!-- 水单详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="水单详情"
      width="800px"
      :footer="null"
    >
      <a-descriptions v-if="currentRemittance" bordered :column="3">
        <a-descriptions-item label="水单编号">{{ currentRemittance.remittanceNo }}</a-descriptions-item>
        <a-descriptions-item label="水单类型">
          <a-tag :color="currentRemittance.remittanceType === 1 ? 'blue' : 'green'">
            {{ currentRemittance.remittanceType === 1 ? '定金' : '尾款' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="收汇名称">{{ currentRemittance.remittanceName }}</a-descriptions-item>
        <a-descriptions-item label="收汇日期">{{ currentRemittance.remittanceDate }}</a-descriptions-item>
        <a-descriptions-item label="收汇金额">{{ currentRemittance.remittanceAmount }} {{ currentRemittance.currency }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{ getStatusText(currentRemittance.status) }}</a-descriptions-item>
        <a-descriptions-item label="汇率" :span="3">{{ currentRemittance.taxRate || '-' }}</a-descriptions-item>
        <a-descriptions-item label="银行名称" :span="3">{{ currentRemittance.bankAccountName }}</a-descriptions-item>
        <a-descriptions-item label="手续费">{{ currentRemittance.bankFee }}</a-descriptions-item>
        <a-descriptions-item label="入账金额">{{ currentRemittance.creditedAmount }}</a-descriptions-item>
        <a-descriptions-item label="审核备注">{{ currentRemittance.auditRemark || '-' }}</a-descriptions-item>
        <a-descriptions-item label="水单文件" :span="3">
          <template v-if="currentRemittance.photoUrl">
            <a-image v-if="isImage(currentRemittance.photoUrl)" :src="currentRemittance.photoUrl" style="max-width: 300px" />
            <a-button v-else type="link" @click="openFile(currentRemittance.photoUrl)">
              <FilePdfOutlined /> 查看文件
            </a-button>
          </template>
          <span v-else>无文件</span>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, ReloadOutlined, EyeOutlined, AuditOutlined, FilePdfOutlined } from '@ant-design/icons-vue'
import { getRemittanceList, auditRemittance } from '@/api/business/remittance'
import { getEnabledBankAccounts } from '@/api/business/declaration'
import type { Remittance, RemittanceQueryParams } from '@/api/business/remittance'

// 文件类型判断
const isImage = (url: string) => /\.(jpe?g|png|gif|webp|bmp|svg)(\?.*)?$/i.test(url || '')
const openFile = (url: string) => { if (url) window.open(url, '_blank') }
const getFileExt = (url: string) => {
  if (!url) return ''
  const parts = url.split('?')[0].split('.')
  return (parts[parts.length - 1] || '').toUpperCase()
}

// 数据定义
const remittanceList = ref<Remittance[]>([])
const loading = ref(false)
const searchForm = reactive({
  remittanceNo: undefined as string | undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// 弹窗控制
const auditVisible = ref(false)
const detailVisible = ref(false)
const submitLoading = ref(false)
const currentRemittance = ref<Remittance | null>(null)

// 审核表单
const auditForm = reactive({
  taxRate: undefined as number | undefined,
  bankAccountId: undefined as number | undefined,
  bankFeeRate: undefined as number | undefined,
  bankFee: undefined as number | undefined,
  creditedAmount: undefined as number | undefined,
  auditRemark: undefined as string | undefined
})

// 银行列表
const bankList = ref<any[]>([])

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
    width: 120
  },
  {
    title: '收汇金额',
    dataIndex: 'remittanceAmount',
    key: 'remittanceAmount',
    width: 150
  },
  {
    title: '提交时间',
    dataIndex: 'submitTime',
    key: 'submitTime',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
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
      status: 1, // 只查询待审核的
      ...searchForm
    }
    const response = await getRemittanceList(params)
    
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

// 加载银行列表
const loadBankList = async () => {
  try {
    const response = await getEnabledBankAccounts()
    let data = response.data
    if (data?.code === 200) {
      bankList.value = data.data || []
    }
  } catch (error) {
    console.error('加载银行列表失败', error)
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
  handleSearch()
}

// 表格变化
const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadRemittanceList()
}

// 查看水单
const handleView = (record: Remittance) => {
  currentRemittance.value = record
  detailVisible.value = true
}

// 审核水单
const handleAudit = (record: Remittance) => {
  currentRemittance.value = record
  auditForm.taxRate = undefined
  auditForm.bankAccountId = undefined
  auditForm.bankFeeRate = undefined
  auditForm.bankFee = undefined
  auditForm.creditedAmount = undefined
  auditForm.auditRemark = undefined
  auditVisible.value = true
}

// 银行选择变更
const handleBankChange = (value: any) => {
  const bank = bankList.value.find(b => b.id === value)
  if (bank && currentRemittance.value) {
    // 手续费率(转换为百分比显示)
    auditForm.bankFeeRate = bank.serviceFeeRate * 100
    
    // 计算手续费
    const amount = currentRemittance.value.remittanceAmount || 0
    auditForm.bankFee = parseFloat((amount * bank.serviceFeeRate).toFixed(2))
    
    // 计算入账金额
    auditForm.creditedAmount = parseFloat((amount - auditForm.bankFee).toFixed(2))
  }
}

// 过滤银行选项
const filterBankOption = (input: string, option: any) => {
  return option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

// 提交审核
const handleAuditSubmit = async () => {
  if (!auditForm.taxRate) {
    message.warning('请填写汇率')
    return
  }
  if (!auditForm.bankAccountId) {
    message.warning('请选择汇款银行')
    return
  }

  submitLoading.value = true
  try {
    await auditRemittance(currentRemittance.value!.id!, {
      approved: true,
      bankAccountId: auditForm.bankAccountId,
      taxRate: auditForm.taxRate,
      auditRemark: auditForm.auditRemark
    })
    message.success('审核成功')
    auditVisible.value = false
    loadRemittanceList()
  } catch (error) {
    message.error('审核失败')
  } finally {
    submitLoading.value = false
  }
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

// 初始化
onMounted(() => {
  loadRemittanceList()
  loadBankList()
})
</script>

<style scoped>
.remittance-audit-container {
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
</style>
