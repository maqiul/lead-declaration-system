<template>
  <div class="system-config">
    <!-- 搜索区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="配置键">
          <a-input v-model:value="searchForm.configKey" placeholder="请输入配置键" />
        </a-form-item>
        <a-form-item label="配置名称">
          <a-input v-model:value="searchForm.configName" placeholder="请输入配置名称" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch" class="ui-btn-primary">
              <template #icon><search-outlined /></template>
              搜索
            </a-button>
            <a-button @click="handleReset" class="ui-btn-secondary">
              <template #icon><reload-outlined /></template>
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮区域 -->
    <a-card class="ui-card mb-4" :bordered="false">
      <a-space>
        <a-button type="primary" @click="showAddModal" v-permission="['system:config:add']" class="ui-btn-cta">
          <template #icon><plus-outlined /></template>
          新增配置
        </a-button>
        <a-button @click="refreshData" class="ui-btn-secondary">
          <template #icon><reload-outlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 配置分类区域 -->
    <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
      <a-tab-pane key="basic" tab="基本信息">
        <a-card :loading="basicLoading" class="ui-card" :bordered="false">
          <a-form
            :model="basicForm"
            :label-col="{ span: 4 }"
            :wrapper-col="{ span: 16 }"
            @finish="saveBasicConfig"
          >
            <a-form-item label="系统名称" name="system.name">
              <a-input v-model:value="basicForm['system.name']" placeholder="请输入系统名称" />
              <div style="margin-top: 8px; font-size: 12px; color: #666;">
                预览效果：当前系统名称为 "{{ basicForm['system.name'] || '未设置' }}"
              </div>
            </a-form-item>
            <a-form-item label="系统版本" name="system.version">
              <a-input v-model:value="basicForm['system.version']" placeholder="请输入系统版本" />
            </a-form-item>
            <a-form-item label="系统描述" name="system.description">
              <a-textarea v-model:value="basicForm['system.description']" placeholder="请输入系统描述" :rows="3" />
            </a-form-item>
            <a-form-item label="公司名称" name="system.company">
              <a-input v-model:value="basicForm['system.company']" placeholder="请输入公司名称" />
            </a-form-item>
            <a-form-item label="版权信息" name="system.copyright">
              <a-input v-model:value="basicForm['system.copyright']" placeholder="请输入版权信息" />
            </a-form-item>
            <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
              <a-button type="primary" html-type="submit" :loading="basicSaving" v-permission="['system:config:update']" class="ui-btn-primary">保存</a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="ui" tab="界面配置">
        <a-card :loading="uiLoading" class="ui-card" :bordered="false">
          <a-form
            :model="uiForm"
            :label-col="{ span: 4 }"
            :wrapper-col="{ span: 16 }"
            @finish="saveUiConfig"
          >
            <a-form-item label="Logo图片" name="ui.logo">
              <a-input v-model:value="uiForm['ui.logo']" placeholder="请输入Logo图片URL" />
            </a-form-item>
            <a-form-item label="网站图标" name="ui.favicon">
              <a-input v-model:value="uiForm['ui.favicon']" placeholder="请输入网站图标URL" />
            </a-form-item>
            <a-form-item label="主题颜色" name="ui.theme">
              <a-select 
                v-model:value="uiForm['ui.theme']" 
                placeholder="请选择主题颜色"
                :options="themeOptions"
              />
            </a-form-item>
            <a-form-item label="底部文字" name="ui.footer.text">
              <a-textarea v-model:value="uiForm['ui.footer.text']" placeholder="请输入底部显示文字" :rows="2" />
            </a-form-item>
            <a-form-item label="显示底部" name="ui.footer.show">
              <a-switch v-model:checked="uiForm['ui.footer.show']" checked-children="是" un-checked-children="否" />
            </a-form-item>
            <a-form-item label="侧边栏折叠" name="ui.sidebar.collapsed">
              <a-switch v-model:checked="uiForm['ui.sidebar.collapsed']" checked-children="是" un-checked-children="否" />
            </a-form-item>
            <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
              <a-button type="primary" html-type="submit" :loading="uiSaving" v-permission="['system:config:update']" class="ui-btn-primary">保存</a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="business" tab="业务配置">
        <a-card :loading="businessLoading" class="ui-card" :bordered="false">
          <a-form
            :model="businessForm"
            :label-col="{ span: 4 }"
            :wrapper-col="{ span: 16 }"
            @finish="saveBusinessConfig"
          >
            <a-form-item label="启用税务退费" name="business.tax-refund.enabled">
              <a-switch v-model:checked="businessForm['business.tax-refund.enabled']" checked-children="是" un-checked-children="否" />
            </a-form-item>
            <a-form-item label="审批层级" name="business.tax-refund.approval-level">
              <a-select 
                v-model:value="businessForm['business.tax-refund.approval-level']" 
                placeholder="请选择审批层级"
                :options="approvalLevelOptions"
              />
            </a-form-item>
            <a-form-item label="文件上传限制" name="business.file.upload.max-size">
              <a-input v-model:value="businessForm['business.file.upload.max-size']" placeholder="例如: 10MB" />
            </a-form-item>
            <a-form-item label="启用邮件通知" name="business.notification.email-enabled">
              <a-switch v-model:checked="businessForm['business.notification.email-enabled']" checked-children="是" un-checked-children="否" />
            </a-form-item>
            <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
              <a-button type="primary" html-type="submit" :loading="businessSaving" v-permission="['system:config:update']" class="ui-btn-primary">保存</a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="file-upload" tab="文件上传">
        <a-card :loading="fileUploadLoading" class="ui-card" :bordered="false">
          <a-form
            :model="fileUploadForm"
            :label-col="{ span: 4 }"
            :wrapper-col="{ span: 16 }"
            @finish="saveFileUploadConfig"
          >
            <a-form-item label="模板文件路径" name="file.upload.template-path">
              <a-input v-model:value="fileUploadForm['file.upload.template-path']" placeholder="例如: /uploads/templates" />
              <div style="margin-top: 8px; font-size: 12px; color: #666;">
                合同模板文件存储路径(相对路径或绝对路径)
              </div>
            </a-form-item>
            <a-form-item label="合同生成路径" name="file.upload.contract-path">
              <a-input v-model:value="fileUploadForm['file.upload.contract-path']" placeholder="例如: /uploads/contracts" />
              <div style="margin-top: 8px; font-size: 12px; color: #666;">
                生成的合同文件存储路径
              </div>
            </a-form-item>
            <a-form-item label="Excel导出路径" name="file.upload.export-path">
              <a-input v-model:value="fileUploadForm['file.upload.export-path']" placeholder="例如: /uploads/exports" />
              <div style="margin-top: 8px; font-size: 12px; color: #666;">
                Excel导出文件存储路径
              </div>
            </a-form-item>
            <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
              <a-button type="primary" html-type="submit" :loading="fileUploadSaving" v-permission="['system:config:update']" class="ui-btn-primary">保存</a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="all" tab="全部配置">
        <a-card class="ui-card" :bordered="false">
          <a-table
            :dataSource="allConfigs"
            :columns="columns"
            :loading="loading"
            :pagination="pagination"
            :scroll="{ x: 1000 }"
            rowKey="id"
            class="ui-table"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'configType'">
                <a-tag :color="getTypeColor((record as any).configType)" class="ui-tag">
                  {{ getTypeLabel((record as any).configType) }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="(record as any).status === 1 ? 'success' : 'error'" class="ui-tag">
                  {{ (record as any).status === 1 ? '启用' : '禁用' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="editConfig(record)" v-permission="['system:config:update']" class="text-blue-600 font-medium">编辑</a-button>
                  <a-popconfirm
                    title="确定要删除这个配置吗？"
                    @confirm="deleteConfig((record as any).id)"
                  >
                    <a-button type="link" size="small" danger v-permission="['system:config:delete']" class="font-medium">删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>
    </a-tabs>

    <!-- 新增/编辑配置弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      @ok="handleOk"
      @cancel="handleCancel"
      :confirm-loading="confirmLoading"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="配置键" name="configKey">
          <a-input v-model:value="formData.configKey" placeholder="请输入配置键" />
        </a-form-item>
        <a-form-item label="配置名称" name="configName">
          <a-input v-model:value="formData.configName" placeholder="请输入配置名称" />
        </a-form-item>
        <a-form-item label="输入类型" name="inputType">
          <a-select v-model:value="formData.inputType" placeholder="请选择输入类型" @change="handleInputTypeChange">
            <a-select-option :value="1">文本框</a-select-option>
            <a-select-option :value="2">下拉框</a-select-option>
            <a-select-option :value="3">开关</a-select-option>
            <a-select-option :value="4">数字输入框</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="配置值" name="configValue">
          <template v-if="formData.inputType === 1">
            <a-input v-model:value="formData.configValue" placeholder="请输入配置值" />
          </template>
          <template v-else-if="formData.inputType === 2">
            <a-select 
              v-model:value="formData.configValue" 
              :options="parseSelectOptions(formData.selectOptions)"
              placeholder="请选择配置值"
              allowClear
            />
          </template>
          <template v-else-if="formData.inputType === 3">
            <a-switch v-model:checked="formData.configValue" checked-children="开启" un-checked-children="关闭" />
          </template>
          <template v-else-if="formData.inputType === 4">
            <a-input-number v-model:value="formData.configValue" placeholder="请输入数值" style="width: 100%" />
          </template>
          <template v-else>
            <a-textarea v-model:value="formData.configValue" placeholder="请输入配置值" :rows="3" />
          </template>
        </a-form-item>
        <a-form-item 
          v-if="formData.inputType === 2" 
          label="下拉框选项" 
          name="selectOptions"
        >
          <div class="select-options-editor">
            <a-button type="primary" size="small" @click="addSelectOption" style="margin-bottom: 8px;">
              添加选项
            </a-button>
            <div 
              v-for="(option, index) in selectOptionsList" 
              :key="index" 
              class="option-item"
            >
              <a-input 
                v-model:value="option.label" 
                placeholder="显示标签" 
                style="width: 40%; margin-right: 8px;"
              />
              <a-input 
                v-model:value="option.value" 
                placeholder="选项值" 
                style="width: 40%; margin-right: 8px;"
              />
              <a-button 
                type="link" 
                danger 
                size="small" 
                @click="removeSelectOption(index)"
              >
                删除
              </a-button>
            </div>
          </div>
        </a-form-item>
        <a-form-item label="配置类型" name="configType">
          <a-select v-model:value="formData.configType" placeholder="请选择配置类型">
            <a-select-option :value="1">系统配置</a-select-option>
            <a-select-option :value="2">UI配置</a-select-option>
            <a-select-option :value="3">业务配置</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="配置分组" name="configGroup">
          <a-input v-model:value="formData.configGroup" placeholder="请输入配置分组" />
        </a-form-item>
        <a-form-item label="备注说明" name="remark">
          <a-textarea v-model:value="formData.remark" placeholder="请输入备注说明" :rows="2" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { 
  PlusOutlined, 
  ReloadOutlined
} from '@ant-design/icons-vue'
import { 
  getSystemBasicInfo, 
  getUiConfig, 
  getAllConfigs,
  getConfigsByGroup,
  updateConfig,
  addConfig,
  deleteConfig as deleteConfigApi
} from '@/api/system/config'

// 响应式数据
const activeTab = ref<string>('basic')
const loading = ref(false)
const modalVisible = ref(false)
const modalTitle = ref('新增配置')
const confirmLoading = ref(false)
const formRef = ref()

// 各分类表单的loading状态
const basicLoading = ref(false)
const uiLoading = ref(false)
const businessLoading = ref(false)
const basicSaving = ref(false)
const uiSaving = ref(false)
const businessSaving = ref(false)

// localStorage缓存键名
const CACHE_KEYS = {
  basic: 'system_config_basic',
  ui: 'system_config_ui',
  business: 'system_config_business'
}

// 搜索表单
const searchForm = reactive({
  configKey: '',
  configName: ''
})

// 搜索处理方法
const handleSearch = () => {
  // 这里可以添加搜索逻辑
}

const handleReset = () => {
  searchForm.configKey = ''
  searchForm.configName = ''
  // 重置搜索逻辑
}



// 下拉框选项数据
const approvalLevelOptions = ref([
  { label: '1级审批', value: '1' },
  { label: '2级审批', value: '2' },
  { label: '3级审批', value: '3' },
  { label: '4级审批', value: '4' },
  { label: '5级审批', value: '5' }
])

const themeOptions = ref([
  { label: '默认蓝', value: '#1890ff' },
  { label: '科技蓝', value: '#001529' },
  { label: '活力橙', value: '#fa8c16' },
  { label: '清新绿', value: '#52c41a' }
])

// 表单数据
const basicForm = reactive<Record<string, any>>({})
const uiForm = reactive<Record<string, any>>({})
const businessForm = reactive<Record<string, any>>({})
const allConfigs = ref<any[]>([])

const formData = reactive({
  id: undefined as number | undefined,
  configKey: '',
  configName: '',
  configValue: '',
  inputType: 1,
  selectOptions: '',
  configType: 1,
  configGroup: '',
  remark: '',
  status: 1
})

// 下拉框选项管理
const selectOptionsList = ref<Array<{label: string, value: string}>>([])

// 表格配置
const columns = [
  {
    title: '配置键',
    dataIndex: 'configKey',
    key: 'configKey',
    width: 200
  },
  {
    title: '配置名称',
    dataIndex: 'configName',
    key: 'configName',
    width: 150
  },
  {
    title: '配置值',
    dataIndex: 'configValue',
    key: 'configValue',
    ellipsis: true
  },
  {
    title: '配置类型',
    dataIndex: 'configType',
    key: 'configType',
    width: 100
  },
  {
    title: '配置分组',
    dataIndex: 'configGroup',
    key: 'configGroup',
    width: 100
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 80
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
    fixed: 'right' as const
  }
]

const pagination = {
  pageSize: 10,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
}

// 表单验证规则
const formRules = {
  configKey: [
    { required: true, message: '请输入配置键', trigger: 'blur' }
  ] as any[],
  configName: [
    { required: true, message: '请输入配置名称', trigger: 'blur' }
  ] as any[],
  configType: [
    { required: true, message: '请选择配置类型', trigger: 'change' }
  ] as any[],
  configGroup: [
    { required: true, message: '请输入配置分组', trigger: 'blur' }
  ] as any[]
}

// 方法
const refreshData = async () => {
  await loadData()
  if (activeTab.value === 'all') {
    await loadAllConfigs()
  }
}

const handleTabChange = (key: string | number) => {
  const tabKey = typeof key === 'number' ? key.toString() : key
  if (tabKey === 'all') {
    loadAllConfigs()
  }
}

const loadAllConfigs = async () => {
  loading.value = true
  try {
    const response = await getAllConfigs()
    if (response.data?.code === 200) {
      allConfigs.value = response.data.data || []
    }
  } catch (error) {
    message.error('加载配置列表失败')
  } finally {
    loading.value = false
  }
}

// 从localStorage加载缓存数据
const loadFromCache = (cacheKey: string): Record<string, any> | null => {
  try {
    const cached = localStorage.getItem(cacheKey)
    if (cached) {
      return JSON.parse(cached)
    }
  } catch (error) {
    // 读取缓存失败
  }
  return null
}

// 保存数据到localStorage缓存
const saveToCache = (cacheKey: string, data: Record<string, any>) => {
  try {
    localStorage.setItem(cacheKey, JSON.stringify(data))
  } catch (error) {
    // 保存缓存失败
  }
}

// 加载基本信息
const loadBasicConfig = async () => {
  basicLoading.value = true
  try {
    const response = await getSystemBasicInfo()
    if (response.data?.code === 200) {
      const data = response.data.data || {}
      Object.assign(basicForm, data)
      // 保存到缓存
      saveToCache(CACHE_KEYS.basic, data)
    }
  } catch (error) {
    // API失败时从localStorage加载
    const cached = loadFromCache(CACHE_KEYS.basic)
    if (cached) {
      Object.assign(basicForm, cached)
      message.warning('网络异常，已从本地缓存加载基本信息')
    } else {
      message.error('加载基本信息失败')
    }
  } finally {
    basicLoading.value = false
  }
}

// 加载UI配置
const loadUiConfig = async () => {
  uiLoading.value = true
  try {
    const response = await getUiConfig()
    if (response.data?.code === 200) {
      const uiData = response.data.data || {}
      // 处理布尔值
      uiData['ui.footer.show'] = uiData['ui.footer.show'] === 'true' || uiData['ui.footer.show'] === true
      uiData['ui.sidebar.collapsed'] = uiData['ui.sidebar.collapsed'] === 'true' || uiData['ui.sidebar.collapsed'] === true
      Object.assign(uiForm, uiData)
      // 保存到缓存
      saveToCache(CACHE_KEYS.ui, uiData)
    }
  } catch (error) {
    // API失败时从localStorage加载
    const cached = loadFromCache(CACHE_KEYS.ui)
    if (cached) {
      Object.assign(uiForm, cached)
      message.warning('网络异常，已从本地缓存加载界面配置')
    } else {
      message.error('加载界面配置失败')
    }
  } finally {
    uiLoading.value = false
  }
}

// 加载业务配置
const loadBusinessConfig = async () => {
  businessLoading.value = true
  try {
    const response = await getConfigsByGroup('business')
    if (response.data?.code === 200) {
      const configs = response.data.data || []
      const businessData: Record<string, any> = {}
      configs.forEach((config: any) => {
        // 处理布尔值
        if (config.configValue === 'true' || config.configValue === 'false') {
          businessData[config.configKey] = config.configValue === 'true'
        } else {
          businessData[config.configKey] = config.configValue
        }
      })
      Object.assign(businessForm, businessData)
      // 保存到缓存
      saveToCache(CACHE_KEYS.business, businessData)
    }
  } catch (error) {
    // API失败时从localStorage加载
    const cached = loadFromCache(CACHE_KEYS.business)
    if (cached) {
      Object.assign(businessForm, cached)
      message.warning('网络异常，已从本地缓存加载业务配置')
    } else {
      message.error('加载业务配置失败')
    }
  } finally {
    businessLoading.value = false
  }
}

const loadFileUploadConfig = async () => {
  fileUploadLoading.value = true
  try {
    const response = await getConfigsByGroup('file-upload')
    if (response.data?.code === 200) {
      const configs = response.data.data || []
      const fileUploadData: Record<string, any> = {}
      configs.forEach((config: any) => {
        fileUploadData[config.configKey] = config.configValue
      })
      Object.assign(fileUploadForm, fileUploadData)
      console.log('加载文件上传配置:', fileUploadData)
    } else {
      console.warn('获取文件上传配置失败:', response.data?.message)
      // 如果没有配置项，初始化默认值
      Object.assign(fileUploadForm, {
        'file.upload.template-path': '/uploads/templates',
        'file.upload.contract-path': '/uploads/contracts',
        'file.upload.export-path': '/uploads/exports'
      })
    }
  } catch (error) {
    console.error('加载文件上传配置异常:', error)
    message.error('加载文件上传配置失败')
  } finally {
    fileUploadLoading.value = false
  }
}

const loadData = async () => {
  await Promise.all([
    loadBasicConfig(),
    loadUiConfig(),
    loadBusinessConfig(),
    loadFileUploadConfig()
  ])
}

const saveBasicConfig = async () => {
  basicSaving.value = true
  try {
    for (const [key, value] of Object.entries(basicForm)) {
      await updateConfig(key, { configValue: value })
    }
    // 保存成功后更新缓存
    saveToCache(CACHE_KEYS.basic, { ...basicForm })
    message.success('基本信息保存成功')
    
    // 如果修改了系统名称，更新页面标题
    if (basicForm['system.name']) {
      const titleElement = document.getElementById('app-title')
      if (titleElement) {
        titleElement.textContent = basicForm['system.name']
      }
      document.title = basicForm['system.name']
      
      // 显示更新提示
      message.info(`系统名称已更新为: ${basicForm['system.name']}`)
    }
  } catch (error) {
    message.error('保存失败，请检查网络连接后重试')
  } finally {
    basicSaving.value = false
  }
}

const saveUiConfig = async () => {
  uiSaving.value = true
  try {
    for (const [key, value] of Object.entries(uiForm)) {
      await updateConfig(key, { configValue: String(value) })
    }
    // 保存成功后更新缓存
    saveToCache(CACHE_KEYS.ui, { ...uiForm })
    message.success('界面配置保存成功')
  } catch (error) {
    message.error('保存失败，请检查网络连接后重试')
  } finally {
    uiSaving.value = false
  }
}

const saveBusinessConfig = async () => {
  businessSaving.value = true
  try {
    for (const [key, value] of Object.entries(businessForm)) {
      await updateConfig(key, { configValue: String(value) })
    }
    // 保存成功后更新缓存
    saveToCache(CACHE_KEYS.business, { ...businessForm })
    message.success('业务配置保存成功')
  } catch (error) {
    message.error('保存失败，请检查网络连接后重试')
  } finally {
    businessSaving.value = false
  }
}

// 文件上传配置
const fileUploadLoading = ref(false)
const fileUploadSaving = ref(false)
const fileUploadForm = reactive<Record<string, any>>({})

const saveFileUploadConfig = async () => {
  fileUploadSaving.value = true
  try {
    // 确保配置项存在，如果不存在则创建
    const configItems = [
      { key: 'file.upload.template-path', name: '模板文件路径', defaultValue: '/uploads/templates' },
      { key: 'file.upload.contract-path', name: '合同生成路径', defaultValue: '/uploads/contracts' },
      { key: 'file.upload.export-path', name: 'Excel导出路径', defaultValue: '/uploads/exports' }
    ]
    
    // 先检查并创建缺失的配置项
    for (const item of configItems) {
      if (!fileUploadForm[item.key]) {
        fileUploadForm[item.key] = item.defaultValue
      }
      
      try {
        // 尝试更新配置
        await updateConfig(item.key, { configValue: String(fileUploadForm[item.key]) })
      } catch (updateError) {
        console.warn(`更新配置 ${item.key} 失败，尝试创建:`, updateError)
        // 如果更新失败，可能是配置项不存在，尝试创建
        try {
          await addConfig({
            configKey: item.key,
            configName: item.name,
            configValue: String(fileUploadForm[item.key]),
            inputType: 1,
            configType: 1,
            configGroup: 'file-upload',
            status: 1,
            sort: configItems.findIndex(i => i.key === item.key) + 1
          })
        } catch (createError) {
          console.error(`创建配置 ${item.key} 失败:`, createError)
          throw createError
        }
      }
    }
    
    message.success('文件上传配置保存成功')
  } catch (error: any) {
    console.error('保存文件上传配置失败:', error)
    message.error('保存失败：' + (error.message || '请检查网络连接'))
  } finally {
    fileUploadSaving.value = false
  }
}

const showAddModal = () => {
  modalTitle.value = '新增配置'
  Object.assign(formData, {
    id: undefined,
    configKey: '',
    configName: '',
    configValue: '',
    inputType: 1,
    selectOptions: '',
    configType: 1,
    configGroup: '',
    remark: '',
    status: 1
  })
  selectOptionsList.value = []
  modalVisible.value = true
}

const editConfig = (record: any) => {
  modalTitle.value = '编辑配置'
  Object.assign(formData, {
    id: record.id,
    configKey: record.configKey,
    configName: record.configName,
    configValue: record.configValue,
    inputType: record.inputType || 1,
    selectOptions: record.selectOptions || '',
    configType: record.configType,
    configGroup: record.configGroup,
    remark: record.remark,
    status: record.status
  })
  
  // 初始化下拉框选项列表
  if (formData.inputType === 2 && formData.selectOptions) {
    try {
      selectOptionsList.value = JSON.parse(formData.selectOptions)
    } catch (error) {
      selectOptionsList.value = []
    }
  } else {
    selectOptionsList.value = []
  }
  
  modalVisible.value = true
}

const handleOk = async () => {
  try {
    await formRef.value.validateFields()
    confirmLoading.value = true
    
    if (formData.id) {
      await updateConfig(formData.configKey, { configValue: formData.configValue })
      message.success('配置更新成功')
    } else {
      await addConfig(formData)
      message.success('配置添加成功')
    }
    
    modalVisible.value = false
    await loadData()
    if (activeTab.value === 'all') {
      await loadAllConfigs()
    }
  } catch (error) {
    message.error('操作失败')
  } finally {
    confirmLoading.value = false
  }
}

const handleCancel = () => {
  modalVisible.value = false
}

// 输入类型变化处理
const handleInputTypeChange = (value: any) => {
  const numValue = Number(value)
  formData.inputType = numValue
  if (numValue !== 2) {
    selectOptionsList.value = []
    formData.selectOptions = ''
  }
}

// 解析下拉框选项
const parseSelectOptions = (optionsStr: string) => {
  if (!optionsStr) return []
  try {
    return JSON.parse(optionsStr)
  } catch (error) {
    return []
  }
}

// 添加下拉框选项
const addSelectOption = () => {
  selectOptionsList.value.push({ label: '', value: '' })
}

// 删除下拉框选项
const removeSelectOption = (index: number) => {
  selectOptionsList.value.splice(index, 1)
  updateSelectOptionsString()
}

// 更新下拉框选项字符串
const updateSelectOptionsString = () => {
  formData.selectOptions = JSON.stringify(selectOptionsList.value)
}

// 监听下拉框选项变化
watch(selectOptionsList, () => {
  updateSelectOptionsString()
}, { deep: true })

const deleteConfig = async (id: number) => {
  try {
    await deleteConfigApi(id)
    message.success('删除成功')
    await loadAllConfigs()
  } catch (error) {
    message.error('删除失败')
  }
}

const getTypeColor = (type: number) => {
  const colors = { 1: 'blue', 2: 'green', 3: 'orange' }
  return colors[type as keyof typeof colors] || 'default'
}

const getTypeLabel = (type: number) => {
  const labels = { 1: '系统配置', 2: 'UI配置', 3: '业务配置' }
  return labels[type as keyof typeof labels] || '未知'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
/* 页面特有样式已由全局 index.less 覆盖 */
.system-config {
  height: 100%;
  overflow-x: hidden;
}

.select-options-editor {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 12px;
  background-color: #fafafa;
}

.option-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  padding: 8px;
  background: white;
  border-radius: 4px;
  border: 1px solid #e8e8e8;
}

.option-item:last-child {
  margin-bottom: 0;
}

/* 响应式表单布局 */
@media (max-width: 768px) {
  .system-config {
    padding: 16px;
  }
  
  :deep(.ant-card-body) {
    padding: 16px;
  }
  
  :deep(.ant-form-item) {
    margin-bottom: 12px;
  }
}
</style>