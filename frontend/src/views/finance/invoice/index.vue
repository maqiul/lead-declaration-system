<template>
  <div class="finance-invoice-container">
    <a-card :bordered="false" class="mb-4">
      <a-form layout="inline" :model="queryParam" @finish="handleSearch">
        <a-form-item label="申报单">
          <a-select
            v-model:value="queryParam.formId"
            placeholder="全部"
            allow-clear
            show-search
            :filter-option="filterDeclaration"
            :options="declarationList"
            style="width: 200px"
            option-filter-prop="label"
          >
            <template #option="{ record }">
              <span>{{ record.formNo }}</span>
              <span class="ml-2" style="color: #888">{{ record.shipperCompany || '-' }}</span>
            </template>
          </a-select>
        </a-form-item>
        <a-form-item label="发票类型">
          <a-select v-model:value="queryParam.invoiceType" placeholder="全部" allow-clear style="width: 120px">
            <a-select-option :value="1">进项发票</a-select-option>
            <a-select-option :value="2">出项发票</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="发票号码">
          <a-input v-model:value="queryParam.invoiceNo" placeholder="请输入发票号" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit">
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
    </a-card>

    <a-card :bordered="false">
      <div class="toolbar mb-4">
        <a-space>
          <a-button type="primary" @click="openModal()" v-permission="['finance:invoice:create']">
            <template #icon><PlusOutlined /></template>
            录入发票
          </a-button>
        </a-space>
      </div>

      <a-table
        :columns="columns"
        :data-source="dataList"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'formNo'">
            <span>{{ record.formNo || '-' }}</span>
          </template>
          <template v-if="column.key === 'invoiceType'">
            <a-tag :color="record.invoiceType === 1 ? 'orange' : 'blue'">
              {{ record.invoiceType === 1 ? '进项' : '出项' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'invoiceDate'">
            {{ record.invoiceDate || '-' }}
          </template>
          <template v-if="column.key === 'amount'">
            ¥{{ record.amount?.toFixed(2) || '0.00' }}
          </template>
          <template v-if="column.key === 'taxAmount'">
            ¥{{ record.taxAmount?.toFixed(2) || '0.00' }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="openModal(record)" v-permission="['finance:invoice:update']">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button 
                v-if="record.fileUrl" 
                type="link"
                @click="downloadFile(record.id)"
              >
                <template #icon><DownloadOutlined /></template>
                下载
              </a-button>
              <a-popconfirm title="确定删除?" @confirm="handleDelete(record.id)">
                <a-button type="link" danger v-permission="['finance:invoice:delete']">
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 录入弹窗 -->
    <a-modal v-model:open="modalVisible" :title="modalTitle" @ok="handleSave" width="700px">
      <a-form :model="form" layout="vertical">
        <a-form-item label="关联申报单" required>
          <a-select
            v-model:value="form.formId"
            placeholder="请选择申报单"
            show-search
            :filter-option="filterDeclaration"
            :options="declarationList"
            option-filter-prop="label"
          >
            <template #option="{ record }">
              <span>{{ record.formNo }}</span>
              <span class="ml-2" style="color: #888">{{ record.shipperCompany || '-' }}</span>
            </template>
          </a-select>
        </a-form-item>
        <a-form-item label="发票类型" required>
          <a-radio-group v-model:value="form.invoiceType">
            <a-radio :value="1">进项发票</a-radio>
            <a-radio :value="2">出项发票</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="发票号码" required>
          <a-input v-model:value="form.invoiceNo" />
        </a-form-item>
        <a-form-item label="发票名称">
          <a-input v-model:value="form.invoiceName" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="金额 (不含税)">
              <a-input-number v-model:value="form.amount" style="width: 100%" :precision="2" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="税额">
              <a-input-number v-model:value="form.taxAmount" style="width: 100%" :precision="2" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="开票日期">
          <a-date-picker v-model:value="form.invoiceDate" style="width: 100%" />
        </a-form-item>
        <a-form-item label="发票文件">
          <a-upload
            :file-list="invoiceFileList"
            :before-upload="beforeUpload"
            @remove="handleRemoveFile"
            accept=".pdf,.jpg,.jpeg,.png"
            :max-count="1"
          >
            <a-button>
              <template #icon><UploadOutlined /></template>
              选择文件
            </a-button>
          </a-upload>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="form.remarks" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, UploadOutlined, DownloadOutlined, SearchOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { getFinanceInvoiceList, saveFinanceInvoice, deleteFinanceInvoice, uploadInvoiceFile, downloadInvoiceFile } from '@/api/finance/invoice'
import { getDeclarationList } from '@/api/business/declaration'

const loading = ref(false)
const dataList = ref<any[]>([])
const modalVisible = ref(false)
const isEdit = ref(false)
const declarationList = ref<any[]>([]) // 申报单列表
const invoiceFileList = ref<any[]>([])
const tempInvoiceFile = ref<any>(null)

const queryParam = reactive({ pageNum: 1, pageSize: 10, invoiceType: undefined, invoiceNo: '', formId: undefined })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const form = reactive<any>({ id: undefined, formId: undefined, invoiceType: 1, invoiceNo: '', invoiceName: '', amount: null, taxAmount: null, invoiceDate: undefined, remarks: '' })

const modalTitle = computed(() => isEdit.value ? '编辑发票' : '录入发票')

const columns = [
  { title: '申报单号', dataIndex: 'formNo', key: 'formNo', width: 150 },
  { title: '发票名称', dataIndex: 'invoiceName', key: 'invoiceName', width: 150 },
  { title: '发票号码', dataIndex: 'invoiceNo', key: 'invoiceNo', width: 150 },
  { title: '类型', dataIndex: 'invoiceType', key: 'invoiceType', width: 100 },
  { title: '开票日期', dataIndex: 'invoiceDate', key: 'invoiceDate', width: 120 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '税额', dataIndex: 'taxAmount', key: 'taxAmount', width: 120 },
  { title: '备注', dataIndex: 'remarks', key: 'remarks' },
  { title: '操作', key: 'action', width: 150 }
]

const loadData = async () => {
  loading.value = true
  try {
    const res = await getFinanceInvoiceList(queryParam)
    if (res.data.code === 200) {
      dataList.value = res.data.data.records
      pagination.total = res.data.data.total
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParam.pageNum = 1
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  queryParam.formId = undefined
  queryParam.invoiceType = undefined
  queryParam.invoiceNo = ''
  handleSearch()
}

const handleTableChange = (pag: any) => {
  queryParam.pageNum = pag.current
  pagination.current = pag.current
  loadData()
}

const openModal = (record?: any) => {
  isEdit.value = !!record?.id
  if (record) {
    Object.assign(form, record)
    form.invoiceDate = record.invoiceDate ? dayjs(record.invoiceDate) : undefined
    // 如果有文件，显示在上传列表中
    if (record.fileName) {
      invoiceFileList.value = [{ uid: '-1', name: record.fileName, status: 'done', url: record.fileUrl }]
    } else {
      invoiceFileList.value = []
    }
  } else {
    Object.assign(form, { id: undefined, formId: undefined, invoiceType: 1, invoiceNo: '', invoiceName: '', amount: null, taxAmount: null, invoiceDate: undefined, remarks: '' })
    invoiceFileList.value = []
    tempInvoiceFile.value = null
  }
  // 加载申报单列表
  loadDeclarations()
  modalVisible.value = true
}

// 加载申报单列表
const loadDeclarations = async () => {
  try {
    const res = await getDeclarationList({ pageNum: 1, pageSize: 1000, status: undefined })
    if (res.data.code === 200) {
      declarationList.value = res.data.data.records.map((item: any) => ({
        value: item.id,
        label: item.formNo,
        record: item
      }))
    }
  } catch (error) {
    console.error('加载申报单列表失败', error)
  }
}

// 申报单过滤函数
const filterDeclaration = (input: string, option: any) => {
  return option.record.formNo.toLowerCase().includes(input.toLowerCase()) ||
         (option.record.shipperCompany && option.record.shipperCompany.toLowerCase().includes(input.toLowerCase()))
}

// 文件上传前校验
const beforeUpload = (file: File) => {
  const isAllowed = file.type === 'application/pdf' || 
                    file.type === 'image/jpeg' || 
                    file.type === 'image/png'
  if (!isAllowed) {
    message.error('只支持PDF、JPG、PNG格式')
    return false
  }
  tempInvoiceFile.value = file
  invoiceFileList.value = [{ uid: '-1', name: file.name, status: 'done' }]
  return false // 阻止自动上传
}

// 移除文件
const handleRemoveFile = () => {
  invoiceFileList.value = []
  tempInvoiceFile.value = null
  return true
}

const handleSave = async () => {
  if (!form.formId) return message.warning('请关联申报单')
  if (!form.invoiceNo) return message.warning('请输入发票号码')

  try {
    // 先保存发票基本信息
    const payload = { ...form, invoiceDate: form.invoiceDate ? dayjs(form.invoiceDate).format('YYYY-MM-DD') : undefined }
    await saveFinanceInvoice(payload)
    
    // 如果是新增且有文件，需要获取刚保存的发票ID来上传文件
    if (!isEdit.value && tempInvoiceFile.value) {
      // 重新查询获取最新发票
      const res = await getFinanceInvoiceList({ pageNum: 1, pageSize: 1, formId: form.formId })
      if (res.data.code === 200 && res.data.data.records.length > 0) {
        const latestInvoice = res.data.data.records[0]
        const formData = new FormData()
        formData.append('file', tempInvoiceFile.value)
        await uploadInvoiceFile(latestInvoice.id, formData)
      }
    } else if (isEdit.value && tempInvoiceFile.value) {
      // 编辑且有新文件
      const formData = new FormData()
      formData.append('file', tempInvoiceFile.value)
      await uploadInvoiceFile(form.id, formData)
    }
    
    message.success('保存成功')
    modalVisible.value = false
    loadData()
  } catch (error) {
    message.error('保存失败')
  }
}

const handleDelete = async (id: number) => {
  await deleteFinanceInvoice(id)
  message.success('删除成功')
  loadData()
}

// 下载发票文件
const downloadFile = async (invoiceId: number) => {
  try {
    const response = await downloadInvoiceFile(invoiceId)
    // 创建blob并下载
    const blob = new Blob([response.data])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `invoice_${invoiceId}.pdf`
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    message.error('文件下载失败')
  }
}

onMounted(() => {
  loadData()
  loadDeclarations()
})
</script>

<style scoped>
.finance-invoice-container {
  padding: 24px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
}
</style>
