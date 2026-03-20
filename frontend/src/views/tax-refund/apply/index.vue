<template>
  <div class="tax-refund-application">

    <a-card :title="isEdit ? '编辑退税申请' : '新建退税申请'">
      <template #extra>
        <a-space>
          <a-button @click="handleBackToList">返回列表</a-button>
          <a-button @click="handleSaveDraft" :loading="submitting">保存草稿</a-button>
          <a-button type="primary" @click="handleSubmit" :loading="submitting">提交申请</a-button>
        </a-space>
      </template>

      <a-form :model="formData" :rules="rules" ref="formRef" layout="vertical" class="application-form">
        <a-card title="基本信息" class="section-card" size="small">
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="关联申报单" name="declarationFormId">
                <a-select 
                  v-model:value="formData.declarationFormId" 
                  placeholder="请选择关联的申报单"
                  :options="declarationOptions"
                  show-search
                  option-filter-prop="label"
                  @change="handleDeclarationChange"
                  size="large"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="申请类型" name="applicationType">
                <a-select 
                  v-model:value="formData.applicationType" 
                  placeholder="请选择申请类型"
                  :options="typeOptions"
                  size="large"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>

        <a-card title="退税明细" class="section-card" size="small">
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="申请金额" name="amount">
                <a-input-number 
                  v-model:value="formData.amount" 
                  placeholder="请输入申请金额"
                  style="width: 100%"
                  :min="0"
                  :precision="2"
                  size="large"
                  addon-after="元"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="发票号码" name="invoiceNo">
                <a-input 
                  v-model:value="formData.invoiceNo" 
                  placeholder="请输入发票号码"
                  size="large"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="发票金额" name="invoiceAmount">
                <a-input-number 
                  v-model:value="formData.invoiceAmount" 
                  placeholder="请输入发票金额"
                  style="width: 100%"
                  :min="0"
                  :precision="2"
                  size="large"
                  addon-after="元"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="税率(%)" name="taxRate">
                <a-input-number 
                  v-model:value="formData.taxRate" 
                  placeholder="请输入税率"
                  style="width: 100%"
                  :min="0"
                  :max="100"
                  :precision="2"
                  size="large"
                  addon-after="%"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item label="申请说明" name="description">
            <a-textarea 
              v-model:value="formData.description" 
              placeholder="请输入申请说明"
              :rows="4"
              size="large"
            />
          </a-form-item>
        </a-card>

        <a-card title="附件上传" class="section-card" size="small">
          <a-form-item label="发票附件" extra="支持PDF、JPG、PNG格式，单个文件不超过2MB">
            <a-upload
              v-model:file-list="fileList"
              :before-upload="beforeUpload"
              :customRequest="customRequest"
              :remove="handleRemove"
              accept=".pdf,.jpg,.jpeg,.png"
              list-type="picture-card"
              :max-count="5"
            >
              <div v-if="fileList && fileList.length < 5">
                <PlusOutlined />
                <div style="margin-top: 8px">上传附件</div>
              </div>
            </a-upload>
          </a-form-item>
        </a-card>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { UploadProps } from 'ant-design-vue'
import { createTaxRefund, updateTaxRefund, saveTaxRefundDraft, getAvailableDeclarations, uploadTaxRefundFile, attachFilesToApplication, submitTaxRefund } from '@/api/tax-refund'

const router = useRouter()
const route = useRoute()

// 页面状态
const isEdit = ref(!!route.query.id)
const submitting = ref(false)
const formRef = ref()


// 表单数据
const formData = reactive({
  id: undefined as number | undefined,
  declarationFormId: undefined,
  applicationType: undefined,
  amount: undefined,
  invoiceNo: '',
  invoiceAmount: undefined,
  taxRate: undefined,
  description: ''
})

// 文件列表
const fileList = ref<UploadProps['fileList']>([])

// 选项数据
const declarationOptions = ref<Array<{label: string, value: number}>>([])
const typeOptions = ref([
  { label: '出口退税', value: 'EXPORT_REFUND' },
  { label: '增值税退税', value: 'VAT_REFUND' },
  { label: '其他退税', value: 'OTHER_REFUND' }
])

// 表单规则
const rules = {
  declarationFormId: [{ required: true, message: '请选择关联申报单' }],
  applicationType: [{ required: true, message: '请选择申请类型' }],
  amount: [{ required: true, message: '请输入申请金额' }],
  invoiceNo: [{ required: true, message: '请输入发票号码' }],
  invoiceAmount: [{ required: true, message: '请输入发票金额' }]
}



// 获取申报单列表
const loadDeclarations = async () => {
  try {
    const response = await getAvailableDeclarations()
    if (response.data?.code === 200) {
      declarationOptions.value = response.data.data.map((item: any) => ({
        label: `${item.declarationNo} - ${item.productName}`,
        value: item.id
      }))
    } else {
      message.error(response.data?.message || '获取申报单列表失败')
    }
  } catch (error) {
    message.error('获取申报单列表失败')
  }
}

// 申报单选择变化
const handleDeclarationChange = (_value: any) => {
  // 可以根据选择的申报单自动填充相关信息
}

// 文件上传前处理
const beforeUpload = (file: any) => {
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('文件大小不能超过2MB!')
    return false
  }
  return true
}

// 自定义上传处理
const customRequest = async (options: any) => {
  const { file, onSuccess, onError, onProgress } = options
  
  try {
    // 模拟进度
    onProgress({ percent: 30 })
    
    // 使用专门的退税文件上传API
    const response = await uploadTaxRefundFile(file)
    
    onProgress({ percent: 100 })
    
    if (response.data?.code === 200) {
      // Ant Design Vue Upload 组件会自动处理文件列表
      // 我们只需要调用 onSuccess，不要手动操作 fileList
      onSuccess(response.data, file)
      message.success('文件上传成功')
    } else {
      onError(new Error(response.data?.message || '上传失败'))
      message.error(response.data?.message || '上传失败')
    }
  } catch (error: any) {
    onError(error)
    message.error(error.message || '上传失败')
  }
}

// 文件移除
const handleRemove = (file: any) => {
  if (fileList.value) {
    const index = fileList.value.indexOf(file)
    if (index > -1) {
      fileList.value.splice(index, 1)
    }
  }
}

// 保存草稿
const handleSaveDraft = async () => {
  try {
    await formRef.value?.validateFields()
    submitting.value = true
    
    const draftData = {
      ...formData,
      status: 0 // 草稿状态
    }
    
    const response = await saveTaxRefundDraft(draftData)
    if (response.data?.code === 200) {
      message.success('草稿保存成功')
    } else {
      message.error(response.data?.message || '保存失败')
    }
  } catch (error) {
    message.error('表单验证失败')
  } finally {
    submitting.value = false
  }
}

// 返回列表
const handleBackToList = () => {
  router.push('/tax-refund/list')
}

// 提交申请
const handleSubmit = async () => {
  try {
    await formRef.value?.validateFields()
    submitting.value = true
    
    // 收集上传的文件URL
    const fileUrls: string[] = []
    if (fileList.value && fileList.value.length > 0) {
      fileList.value.forEach((fileItem: any) => {
        if (fileItem.status === 'done' && fileItem.response?.data?.fileUrl) {
          fileUrls.push(fileItem.response.data.fileUrl)
        }
      })
    }
    
    let applicationId = formData.id
    
    if (isEdit.value && formData.id) {
      // 编辑模式：先更新
      const response = await updateTaxRefund(formData.id, {
        ...formData,
        status: 0 // 先保存为草稿
      })
      if (response.data?.code !== 200) {
        message.error(response.data?.message || '更新失败')
        return
      }
    } else {
      // 新建模式：先创建草稿
      const response = await createTaxRefund({
        ...formData,
        status: 0 // 先保存为草稿
      })
      if (response.data?.code === 200) {
        applicationId = response.data.data.id
      } else {
        message.error(response.data?.message || '创建失败')
        return
      }
    }
    
    // 关联附件
    if (fileUrls.length > 0 && applicationId) {
      await attachFilesToApplication(applicationId, fileUrls)
    }
    
    // 调用提交接口启动流程
    if (applicationId) {
      const submitResponse = await submitTaxRefund(applicationId)
      if (submitResponse.data?.code === 200) {
        message.success('申请提交成功')
        router.push('/tax-refund/list')
      } else {
        message.error(submitResponse.data?.message || '提交失败')
      }
    }
  } catch (error) {
    message.error('表单验证失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadDeclarations()
})
</script>

<style scoped>
/* 统一UI风格 - 与 declaration/form 完全一致 */
.tax-refund-application {
  height: 100%;
  overflow-x: hidden;
}

:deep(.ant-card) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

:deep(.ant-card-body) {
  padding: 24px;
}

:deep(.ant-card-head) {
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
  min-height: 48px;
}

:deep(.ant-card-head-title) {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

/* 主按钮样式 */
:deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(30, 64, 175, 0.25);
  border-radius: 10px;
}

:deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 100%);
  box-shadow: 0 4px 12px rgba(30, 64, 175, 0.35);
  transform: translateY(-1px);
}


.section-card {
  margin-bottom: 24px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 4px 12px rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.application-form {
  padding: 0;
}

:deep(.ant-form-item-label > label) {
  font-weight: 600;
  color: #334155;
}

:deep(.ant-upload.ant-upload-select-picture-card) {
  border-radius: 8px;
}

/* 响应式布局 */
@media (max-width: 768px) {
  :deep(.ant-card-body) {
    padding: 16px;
  }
}
</style>