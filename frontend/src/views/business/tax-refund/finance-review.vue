<template>
  <div class="finance-review">
    <!-- 发货清单组件 -->
    <a-card title="发货清单" style="margin-bottom: 20px;">
      <ShippingListDemo />
    </a-card>
    <a-page-header
      title="税务退费审核"
      @back="router.go(-1)"
    />
    
    <a-row :gutter="16">
      <!-- 申请信息 -->
      <a-col :span="16">
        <a-card title="申请信息">
          <a-descriptions :column="2" bordered>
            <a-descriptions-item label="申请编号">
              {{ applicationData.applicationNo }}
            </a-descriptions-item>
            <a-descriptions-item label="申请人">
              {{ applicationData.initiatorName }}
            </a-descriptions-item>
            <a-descriptions-item label="申请部门">
              {{ applicationData.departmentName }}
            </a-descriptions-item>
            <a-descriptions-item label="申请类型">
              {{ getApplicationTypeText(applicationData.applicationType) }}
            </a-descriptions-item>
            <a-descriptions-item label="申请金额">
              ¥{{ applicationData.amount }}
            </a-descriptions-item>
            <a-descriptions-item label="申请时间">
              {{ applicationData.createTime }}
            </a-descriptions-item>
            <a-descriptions-item label="申请说明" :span="2">
              {{ applicationData.description }}
            </a-descriptions-item>
          </a-descriptions>
          
          <!-- 附件列表 -->
          <div v-if="applicationData.attachments && applicationData.attachments.length > 0" 
               style="margin-top: 16px;">
            <h4>相关附件：</h4>
            <a-space>
              <a-button 
                v-for="attachment in applicationData.attachments" 
                :key="attachment.id"
                @click="downloadAttachment(attachment)"
              >
                <paper-clip-outlined /> {{ attachment.fileName }}
              </a-button>
            </a-space>
          </div>
        </a-card>
        
        <!-- 审核表单 -->
        <a-card title="审核操作" style="margin-top: 16px;">
          <a-form 
            :model="reviewForm" 
            :rules="reviewRules" 
            ref="reviewFormRef"
            layout="vertical"
          >
            <a-form-item label="审核结果" name="result" required>
              <a-radio-group v-model:value="reviewForm.result">
                <a-radio value="approved">通过</a-radio>
                <a-radio value="rejected">拒绝</a-radio>
                <a-radio value="returned">退回补充</a-radio>
              </a-radio-group>
            </a-form-item>
            
            <a-form-item 
              v-if="reviewForm.result === 'returned'" 
              label="退回原因" 
              name="returnReason"
              required
            >
              <a-textarea 
                v-model:value="reviewForm.returnReason" 
                :rows="3" 
                placeholder="请输入退回原因和需要补充的信息"
              />
            </a-form-item>
            
            <a-form-item 
              v-else 
              label="审核意见" 
              name="opinion"
            >
              <a-textarea 
                v-model:value="reviewForm.opinion" 
                :rows="3" 
                placeholder="请输入审核意见"
              />
            </a-form-item>
            
            <a-form-item>
              <a-space>
                <a-button 
                  type="primary" 
                  @click="submitReview"
                  :loading="submitting"
                >
                  提交审核
                </a-button>
                <a-button @click="resetForm">重置</a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
      
      <!-- 流程信息 -->
      <a-col :span="8">
        <a-card title="流程状态">
          <a-steps direction="vertical" :current="getCurrentStep()">
            <a-step 
              v-for="(step, index) in processSteps" 
              :key="index"
              :title="step.title"
              :description="step.description"
            >
              <template #icon>
                <check-circle-outlined v-if="step.completed" />
                <clock-circle-outlined v-else-if="step.current" />
                <minus-circle-outlined v-else />
              </template>
            </a-step>
          </a-steps>
        </a-card>
        
        <a-card title="操作历史" style="margin-top: 16px;">
          <a-timeline>
            <a-timeline-item 
              v-for="history in operationHistory" 
              :key="history.id"
              :color="getTimelineColor(history.action)"
            >
              <p>{{ history.operatorName }} - {{ getActionText(history.action) }}</p>
              <p style="font-size: 12px; color: #8c8c8c;">{{ history.operateTime }}</p>
              <p v-if="history.remark" style="margin-top: 8px;">{{ history.remark }}</p>
            </a-timeline-item>
          </a-timeline>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import ShippingListDemo from '@/views/demo/simple-shipping-demo.vue'
import { 
  CheckCircleOutlined, 
  ClockCircleOutlined, 
  MinusCircleOutlined,
  PaperClipOutlined
} from '@ant-design/icons-vue'
import { 
  getTaxRefundApplicationDetail,
  submitFinanceReview,
  generateFinancialDocument
} from '@/api/business/tax-refund'

const router = useRouter()
const route = useRoute()

// 响应式数据
const submitting = ref(false)
const reviewFormRef = ref()
const applicationData = ref<any>({})
const operationHistory = ref<any[]>([])

// 审核表单
const reviewForm = reactive({
  result: '',
  opinion: '',
  returnReason: ''
})

// 表单验证规则
const reviewRules = {
  result: [{ required: true, message: '请选择审核结果' }],
  opinion: [{ required: true, message: '请输入审核意见' }],
  returnReason: [{ required: true, message: '请输入退回原因' }]
}

// 流程步骤
const processSteps = ref([
  { title: '部门提交', description: '申请已提交', completed: true, current: false },
  { title: '财务初审', description: '等待审核', completed: false, current: true },
  { title: '文件生成', description: '生成财务文件', completed: false, current: false },
  { title: '退回补充', description: '需要补充材料', completed: false, current: false },
  { title: '发票提交', description: '提交发票信息', completed: false, current: false },
  { title: '财务复审', description: '最终审核', completed: false, current: false },
  { title: '文件归档', description: '流程结束', completed: false, current: false }
])

// 获取应用类型文本
const getApplicationTypeText = (type: string) => {
  const types: Record<string, string> = {
    vat_refund: '增值税退费',
    income_tax_refund: '所得税退费',
    other_refund: '其他退费'
  }
  return types[type] || type
}

// 获取当前步骤
const getCurrentStep = () => {
  const status = applicationData.value.status
  const stepMap: Record<number, number> = {
    1: 1, // 部门提交
    2: 1, // 财务初审
    3: 2, // 文件生成
    4: 3, // 退回补充
    5: 4, // 发票提交
    6: 5, // 财务复审
    7: 6, // 已完成
    8: 6  // 已拒绝
  }
  return stepMap[status] || 0
}

// 获取操作历史颜色
const getTimelineColor = (action: string) => {
  const colors: Record<string, string> = {
    submit: 'blue',
    approve: 'green',
    reject: 'red',
    return: 'orange',
    complete: 'green'
  }
  return colors[action] || 'gray'
}

// 获取操作文本
const getActionText = (action: string) => {
  const actions: Record<string, string> = {
    submit: '提交申请',
    approve: '审核通过',
    reject: '审核拒绝',
    return: '退回补充',
    complete: '流程完成'
  }
  return actions[action] || action
}

// 提交审核
const submitReview = async () => {
  try {
    // 根据审核结果设置必填字段
    if (reviewForm.result === 'returned') {
      await reviewFormRef.value?.validateFields(['result', 'returnReason'])
    } else {
      await reviewFormRef.value?.validateFields(['result', 'opinion'])
    }
    
    submitting.value = true
    
    const reviewData = {
      result: reviewForm.result,
      opinion: reviewForm.opinion,
      returnReason: reviewForm.returnReason,
      taskId: route.query.taskId // 从路由参数获取任务ID
    }
    
    const response = await submitFinanceReview(applicationData.value.id, reviewData)
    
    if (response.data?.code === 200) {
      message.success('审核提交成功')
      
      // 根据审核结果执行不同操作
      if (reviewForm.result === 'approved') {
        // 如果是初审通过，生成财务文件
        if (route.query.reviewType === 'first') {
          await generateDocument()
        }
        // 完成任务并继续流程
        await completeTask({ approved: true, opinion: reviewForm.opinion })
      } else if (reviewForm.result === 'rejected') {
        // 拒绝申请
        await completeTask({ approved: false, opinion: reviewForm.opinion })
      } else if (reviewForm.result === 'returned') {
        // 退回补充
        await completeTask({ 
          approved: false, 
          returned: true, 
          returnReason: reviewForm.returnReason 
        })
      }
      
      router.push('/workflow/tasks')
    }
  } catch (error: any) {
    message.error(error.message || '审核提交失败')
  } finally {
    submitting.value = false
  }
}

// 生成财务文件
const generateDocument = async () => {
  try {
    const response = await generateFinancialDocument(applicationData.value.id)
    if (response.data?.code === 200) {
      message.success('财务文件生成成功')
      // 更新申请数据
      applicationData.value.generatedFilePath = response.data.data.filePath
    }
  } catch (error) {
    message.error('生成财务文件失败')
  }
}

// 完成任务
const completeTask = async (variables: any) => {
  // 调用Flowable任务完成API
  console.log('完成任务，变量:', variables)
  // 这里应该调用实际的API
}

// 重置表单
const resetForm = () => {
  reviewFormRef.value?.resetFields()
  reviewForm.result = ''
  reviewForm.opinion = ''
  reviewForm.returnReason = ''
}

// 下载附件
const downloadAttachment = (attachment: any) => {
  // 实现文件下载逻辑
  window.open(attachment.fileUrl)
}

// 加载申请详情
const loadApplicationDetail = async () => {
  const applicationId = route.params.id
  if (applicationId) {
    try {
      const response = await getTaxRefundApplicationDetail(applicationId)
      if (response.data?.code === 200) {
        applicationData.value = response.data.data
        
        // 加载操作历史
        operationHistory.value = response.data.data.operationHistory || []
        
        // 更新流程步骤状态
        updateProcessSteps(response.data.data.status)
      }
    } catch (error) {
      message.error('加载申请详情失败')
    }
  }
}

// 更新流程步骤显示
const updateProcessSteps = (status: number) => {
  const currentStep = getCurrentStep()
  processSteps.value.forEach((step, index) => {
    step.completed = index < currentStep
    step.current = index === currentStep
  })
}

onMounted(() => {
  loadApplicationDetail()
})
</script>

<style scoped>
.finance-review {
  padding: 0;
  background: transparent;
  min-height: 100%;
  box-sizing: border-box;
  overflow-x: hidden;
}

:deep(.ant-card) {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(31, 38, 135, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

:deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
}

:deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

:deep(.ant-btn:not(.ant-btn-primary):not(.ant-btn-link)) {
  border-radius: 8px;
  border-color: #d9d9d9;
  transition: all 0.3s ease;
}

:deep(.ant-btn:not(.ant-btn-primary):not(.ant-btn-link):hover) {
  border-color: #667eea;
  color: #667eea;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2);
}

:deep(.ant-btn-link) {
  color: #667eea;
}

:deep(.ant-btn-link:hover) {
  color: #5a6fd8;
}
</style>