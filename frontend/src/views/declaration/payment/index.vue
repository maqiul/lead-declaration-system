<template>
  <div class="payment-container">
    <a-page-header
      :title="isDeposit ? '定金支付凭证上传' : '尾款支付凭证上传'"
      @back="() => $router.go(-1)"
    >
      <template #extra>
        <a-button key="1" type="primary" :loading="submitting" @click="handleSubmit">
          提交并完成任务
        </a-button>
      </template>
    </a-page-header>

    <a-row :gutter="24">
      <!-- 左侧：申报单基础信息 (只读) -->
      <a-col :span="10">
        <a-card title="申报单摘要" :bordered="false">
          <a-skeleton :loading="loading">
            <a-descriptions :column="1" bordered size="small">
              <a-descriptions-item label="申报编号">{{ declaration.formNo }}</a-descriptions-item>
              <a-descriptions-item label="出口商">{{ declaration.exporterName }}</a-descriptions-item>
              <a-descriptions-item label="合同号">{{ declaration.contractNo }}</a-descriptions-item>
              <a-descriptions-item label="总金额">{{ declaration.totalAmount }} {{ declaration.currency }}</a-descriptions-item>
              <a-descriptions-item label="申报日期">{{ declaration.declarationDate }}</a-descriptions-item>
            </a-descriptions>
          </a-skeleton>
        </a-card>
      </a-col>

      <!-- 右侧：支付凭证上传 -->
      <a-col :span="14">
        <a-card :title="isDeposit ? '定金明细' : '尾款明细'" :bordered="false">
          <a-form :model="paymentForm" layout="vertical">
            <a-form-item label="支付金额" name="amount" required>
              <a-input-number 
                v-model:value="paymentForm.amount" 
                style="width: 100%" 
                :precision="2"
                placeholder="请输入支付金额"
              />
            </a-form-item>
            
            <a-form-item label="支付日期" name="paymentDate" required>
              <a-date-picker 
                v-model:value="(paymentForm.paymentDate as any)"
                style="width: 100%" 
                placeholder="选择支付日期"
              />
            </a-form-item>

            <a-form-item label="上传凭证 (图片/PDF)" name="vouchers">
              <a-upload
                v-model:file-list="fileList"
                action="/api/v1/files/upload"
                list-type="picture-card"
                :headers="headers"
                @change="handleFileChange"
              >
                <div v-if="fileList.length < 5">
                  <plus-outlined />
                  <div style="margin-top: 8px">上传</div>
                </div>
              </a-upload>
            </a-form-item>

            <a-form-item label="备注" name="remark">
              <a-textarea v-model:value="paymentForm.remark" :rows="4" placeholder="补充说明（可选）" />
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { getDeclarationDetail } from '@/api/business/declaration'
import { completeTask } from '@/api/workflow'
import { useUserStore } from '@/store/user'
import type { Dayjs } from 'dayjs'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const submitting = ref(false)
const declaration = ref<any>({})
const fileList = ref<any[]>([])

// 路由参数：id (申报单ID), taskId (工作流任务ID), type (deposit/balance)
const formId = route.query.id as string
const taskId = route.query.taskId as string
const isDeposit = computed(() => route.query.type === 'deposit')

const paymentForm = reactive({
  amount: 0,
  paymentDate: null as Dayjs | null,
  remark: '',
  vouchers: [] as string[]
})

const headers = {
  satoken: (userStore.token || '') as string
}

const loadData = async () => {
  if (!formId) return
  loading.value = true
  try {
    const response = await getDeclarationDetail(Number(formId))
    if (response.data?.code === 200) {
      declaration.value = response.data.data
      // 默认填入建议金额 (示例逻辑)
      paymentForm.amount = isDeposit.value ? (declaration.value.totalAmount * 0.3) : (declaration.value.totalAmount * 0.7)
    }
  } catch (error) {
    message.error('加载申报单详情失败')
  } finally {
    loading.value = false
  }
}

const handleFileChange = ({ fileList: newFileList }: any) => {
  fileList.value = newFileList
}

const handleSubmit = async () => {
  if (!taskId) {
    message.error('任务ID缺失')
    return
  }
  
  if (fileList.value.length === 0) {
    message.warning('请至少上传一张支付凭证')
    return
  }

  submitting.value = true
  try {
    // 提取成功上传的文件ID或URL
    const voucherIds = fileList.value
      .filter(f => f.status === 'done')
      .map(f => f.response?.data?.id || f.url)

    // 完成工作流任务，并传递业务参数
    await completeTask(taskId, {
      approved: true,
      paymentAmount: paymentForm.amount,
      paymentDate: paymentForm.paymentDate,
      vouchers: voucherIds,
      remark: paymentForm.remark
    })
    
    message.success('支付凭证已提交，流程已流转')
    router.push('/workflow/task')
  } catch (error) {
    message.error('提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.payment-container {
  padding: 0;
}
.hint {
  font-size: 12px;
  color: #8c8c8c;
}
</style>
