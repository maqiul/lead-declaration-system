<!-- 系统参数实时预览页面 -->
<template>
  <div class="system-preview">
    <a-page-header
      title="系统参数实时预览"
      @back="router.go(-1)"
    >
      <template #extra>
        <a-button @click="refreshPreview">
          <reload-outlined /> 刷新预览
        </a-button>
      </template>
    </a-page-header>

    <a-row :gutter="24">
      <!-- 系统信息预览 -->
      <a-col :span="12">
        <a-card title="系统信息预览" :loading="loading">
          <a-descriptions bordered size="small" :column="1">
            <a-descriptions-item label="页面标题">
              <span :style="{ color: systemParams['ui.theme'] || '#1890ff', fontWeight: 'bold' }">
                {{ systemParams['system.name'] || '未设置' }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item label="系统版本">
              {{ systemParams['system.version'] || '未设置' }}
            </a-descriptions-item>
            <a-descriptions-item label="公司名称">
              {{ systemParams['system.company'] || '未设置' }}
            </a-descriptions-item>
            <a-descriptions-item label="系统描述">
              {{ systemParams['system.description'] || '未设置' }}
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>

      <!-- UI配置预览 -->
      <a-col :span="12">
        <a-card title="界面配置预览" :loading="loading">
          <div class="ui-preview">
            <div 
              class="preview-header" 
              :style="{ backgroundColor: systemParams['ui.theme'] || '#1890ff' }"
            >
              <span class="preview-logo">
                {{ (systemParams['system.name'] || '系统').substring(0, 2) }}
              </span>
              <span class="preview-title">{{ systemParams['system.name'] || '系统名称' }}</span>
            </div>
            
            <div class="preview-content">
              <p>这是模拟的系统界面预览</p>
              <p>当前主题色：<span :style="{ color: systemParams['ui.theme'] || '#1890ff' }">
                {{ systemParams['ui.theme'] || '#1890ff' }}
              </span></p>
            </div>
            
            <div 
              v-if="systemParams['ui.footer.show'] !== 'false'" 
              class="preview-footer"
            >
              {{ systemParams['ui.footer.text'] || '默认底部信息' }}
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 下拉框参数预览 -->
    <a-row :gutter="24" style="margin-top: 24px;">
      <a-col :span="24">
        <a-card title="下拉框参数预览" :loading="loading">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="审批层级选择">
                <a-select 
                  v-model:value="approvalLevel" 
                  :options="approvalLevelOptions"
                  placeholder="请选择审批层级"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="默认部门选择">
                <a-select 
                  v-model:value="defaultDepartment" 
                  :options="departmentOptions"
                  placeholder="请选择默认部门"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="系统语言">
                <a-select 
                  v-model:value="language" 
                  :options="languageOptions"
                  placeholder="请选择系统语言"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { 
  getSystemParam, 
  getSystemParamAsync, 
  getSystemSelectOptions, 
  refreshSystemParams,
  loading 
} from '@/utils/system-params'

const router = useRouter()

// 响应式数据
const systemParams = ref<Record<string, string>>({})
const approvalLevel = ref('')
const defaultDepartment = ref('')
const language = ref('')
const approvalLevelOptions = ref<any[]>([])
const departmentOptions = ref<any[]>([])
const languageOptions = ref<any[]>([])

// 初始化数据
onMounted(async () => {
  await loadPreviewData()
})

async function loadPreviewData() {
  try {
    // 获取所有系统参数
    systemParams.value = {
      'system.name': getSystemParam('system.name', '默认系统'),
      'system.version': getSystemParam('system.version', '1.0.0'),
      'system.company': getSystemParam('system.company', '默认公司'),
      'system.description': getSystemParam('system.description', '默认描述'),
      'ui.theme': getSystemParam('ui.theme', '#1890ff'),
      'ui.footer.text': getSystemParam('ui.footer.text', '默认底部信息'),
      'ui.footer.show': getSystemParam('ui.footer.show', 'true')
    }

    // 获取下拉框选项
    approvalLevelOptions.value = await getSystemSelectOptions('business.tax-refund.approval-level')
    departmentOptions.value = await getSystemSelectOptions('business.default-department')
    languageOptions.value = await getSystemSelectOptions('ui.language')

    // 设置默认值
    approvalLevel.value = getSystemParam('business.tax-refund.approval-level', '3')
    defaultDepartment.value = getSystemParam('business.default-department', 'tech')
    language.value = getSystemParam('ui.language', 'zh-CN')

  } catch (error) {
    console.error('加载预览数据失败:', error)
    message.error('加载预览数据失败')
  }
}

// 刷新预览
async function refreshPreview() {
  await refreshSystemParams()
  await loadPreviewData()
  message.success('预览数据已刷新')
}
</script>

<style scoped>
.system-preview {
  padding: 24px;
  background-color: #f5f5f5;
  min-height: calc(100vh - 64px);
}

.ui-preview {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.preview-header {
  height: 48px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  color: white;
}

.preview-logo {
  width: 32px;
  height: 32px;
  background: rgba(255,255,255,0.2);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-weight: bold;
}

.preview-title {
  font-size: 16px;
  font-weight: 500;
}

.preview-content {
  padding: 24px;
  background: white;
  min-height: 120px;
}

.preview-footer {
  padding: 12px 24px;
  background: #f0f0f0;
  border-top: 1px solid #e8e8e8;
  text-align: center;
  color: #666;
  font-size: 12px;
}
</style>