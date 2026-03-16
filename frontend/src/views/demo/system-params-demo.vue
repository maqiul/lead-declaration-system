<!-- 系统参数使用示例 -->
<template>
  <div class="system-param-example">
    <h3>系统参数使用示例</h3>
    
    <!-- 直接使用系统参数 -->
    <a-card title="系统信息" style="margin-bottom: 20px;">
      <p><strong>系统名称：</strong>{{ systemName }}</p>
      <p><strong>系统版本：</strong>{{ systemVersion }}</p>
      <p><strong>公司名称：</strong>{{ companyName }}</p>
    </a-card>

    <!-- 使用下拉框参数 -->
    <a-card title="动态下拉框" style="margin-bottom: 20px;">
      <a-form layout="vertical">
        <a-form-item label="审批层级">
          <a-select 
            v-model:value="approvalLevel" 
            :options="approvalLevelOptions"
            placeholder="请选择审批层级"
          />
        </a-form-item>
        
        <a-form-item label="默认部门">
          <a-select 
            v-model:value="defaultDepartment" 
            :options="departmentOptions"
            placeholder="请选择默认部门"
          />
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 动态主题 -->
    <a-card title="主题预览">
      <div :style="{ color: themeColor, fontSize: '18px', fontWeight: 'bold' }">
        当前主题颜色：{{ themeColor }}
      </div>
    </a-card>

    <!-- 刷新按钮 -->
    <a-button @click="refreshParams" :loading="loading">
      刷新系统参数
    </a-button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { 
  getSystemParam, 
  getSystemParamAsync, 
  getSystemSelectOptions, 
  refreshSystemParams,
  loading 
} from '@/utils/system-params'

// 响应式数据
const systemName = ref('')
const systemVersion = ref('')
const companyName = ref('')
const themeColor = ref('')
const approvalLevel = ref('')
const defaultDepartment = ref('')
const approvalLevelOptions = ref<any[]>([])
const departmentOptions = ref<any[]>([])

// 初始化数据
onMounted(async () => {
  await loadSystemParams()
})

async function loadSystemParams() {
  try {
    // 获取系统基本信息
    systemName.value = getSystemParam('system.name', '默认系统')
    systemVersion.value = getSystemParam('system.version', '1.0.0')
    companyName.value = getSystemParam('system.company', '默认公司')
    themeColor.value = getSystemParam('ui.theme', '#1890ff')

    // 异步获取审批层级选项
    approvalLevelOptions.value = await getSystemSelectOptions('business.tax-refund.approval-level')
    
    // 异步获取部门选项
    departmentOptions.value = await getSystemSelectOptions('business.default-department')

    // 设置默认值
    approvalLevel.value = getSystemParam('business.tax-refund.approval-level', '3')
    defaultDepartment.value = getSystemParam('business.default-department', 'tech')
    
  } catch (error) {
    console.error('加载系统参数失败:', error)
  }
}

// 刷新参数
async function refreshParams() {
  await refreshSystemParams()
  await loadSystemParams()
}
</script>

<style scoped>
.system-param-example {
  padding: 20px;
}
</style>