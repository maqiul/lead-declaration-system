import os

apply_path = r'd:\lead-declaration-system\frontend\src\views\tax-refund\apply\index.vue'

with open(apply_path, 'r', encoding='utf-8') as f:
    apply_content = f.read()

apply_target1 = '''    <!-- 基本信息卡片 -->
    <a-card :title="isEdit ? '编辑退税申请' : '新建退税申请'" class="main-card">
      <template #extra>
        <a-space>
          <a-button @click="handleBackToList">返回列表</a-button>
          <a-button @click="handleSaveDraft" :loading="submitting">保存草稿</a-button>
          <a-button type="primary" @click="handleSubmit" :loading="submitting">提交申请</a-button>
        </a-space>
      </template>
      
      <a-form :model="formData" :rules="rules" ref="formRef" layout="vertical" class="application-form">'''

apply_repl1 = '''    <!-- 页面标题区域 -->
    <a-page-header
      :title="isEdit ? '编辑退税申请' : '新建退税申请'"
      @back="handleBackToList"
      class="custom-page-header"
    >
      <template #extra>
        <a-space>
          <a-button @click="handleSaveDraft" :loading="submitting">保存草稿</a-button>
          <a-button type="primary" @click="handleSubmit" :loading="submitting">提交申请</a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-form :model="formData" :rules="rules" ref="formRef" layout="vertical" class="application-form">
      <a-card title="基本信息" class="section-card">'''

apply_target2 = '''        <a-row :gutter="24">
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

        <a-form-item label="发票附件">
          <a-upload
            v-model:file-list="fileList"
            :before-upload="beforeUpload"
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
          <div class="upload-hint">支持PDF、JPG、PNG格式，单个文件不超过2MB</div>
        </a-form-item>
      </a-form>
    </a-card>'''

apply_repl2 = '''      </a-card>

      <a-card title="退税明细" class="section-card">
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

      <a-card title="附件上传" class="section-card">
        <a-form-item label="发票附件" extra="支持PDF、JPG、PNG格式，单个文件不超过2MB">
          <a-upload
            v-model:file-list="fileList"
            :before-upload="beforeUpload"
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
    </a-form>'''

apply_target3 = '''.search-card, .main-card {
  margin-bottom: 20px;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

:deep(.ant-card) {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.03);
  border: 1px solid rgba(226, 232, 240, 0.6);
  margin-bottom: 20px;
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
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.application-form {
  padding: 20px 0;
}

:deep(.ant-form-item-label > label) {
  font-weight: 600;
  color: #334155;
}

:deep(.ant-input-lg),
:deep(.ant-input-number-lg),
:deep(.ant-select-lg .ant-select-selector) {
  border-radius: 8px;
  padding: 8px 12px;
}

:deep(.ant-input-number-lg .ant-input-number-input) {
  height: 40px;
}

:deep(.ant-upload-picture-card-wrapper) {
  display: block;
}

:deep(.ant-upload.ant-upload-select-picture-card) {
  width: 104px;
  height: 104px;
  border-radius: 8px;
  border: 2px dashed #d9d9d9;
  background: #fafafa;
  transition: all 0.3s;
}

:deep(.ant-upload.ant-upload-select-picture-card:hover) {
  border-color: #4096ff;
}

.upload-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #64748b;
}

:deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(30, 64, 175, 0.2);
  border-radius: 8px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

:deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 100%);
  box-shadow: 0 4px 12px rgba(30, 64, 175, 0.3);
  transform: translateY(-1px);
}

/* 响应式布局 */
@media (max-width: 768px) {
  :deep(.ant-card-body) {
    padding: 16px;
  }
  
  .application-form {
    padding: 16px 0;
  }
}'''

apply_repl3 = '''.custom-page-header {
  background: white;
  border-radius: 16px;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.03);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.section-card {
  margin-bottom: 24px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 4px 12px rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.6);
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

.application-form {
  padding: 0;
}

:deep(.ant-upload.ant-upload-select-picture-card) {
  border-radius: 8px;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .tax-refund-application {
    padding: 0;
  }
}'''

apply_content = apply_content.replace(apply_target1, apply_repl1)
apply_content = apply_content.replace(apply_target2, apply_repl2)
apply_content = apply_content.replace(apply_target3, apply_repl3)

with open(apply_path, 'w', encoding='utf-8') as f:
    f.write(apply_content)

print('Apply page updated')
