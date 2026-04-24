<template>
  <div class="process-definition">
    <!-- 搜索区域 -->
    <a-card class="search-card">
      <a-form :model="searchForm" layout="inline">
        <a-form-item label="流程名称">
          <a-input v-model:value="searchForm.processName" placeholder="请输入流程名称" />
        </a-form-item>
        <a-form-item label="流程KEY">
          <a-input v-model:value="searchForm.processKey" placeholder="请输入流程KEY" />
        </a-form-item>
        <a-form-item label="分类">
          <a-select v-model:value="searchForm.category" style="width: 120px" placeholder="请选择分类">
            <a-select-option value="HR">人事</a-select-option>
            <a-select-option value="FINANCE">财务</a-select-option>
            <a-select-option value="IT">IT</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="searchForm.status" style="width: 120px" placeholder="请选择状态">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">停用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch" v-permission="['workflow:definition:view']">搜索</a-button>
            <a-button @click="handleReset" v-permission="['workflow:definition:view']">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮区域 -->
    <a-card class="operation-card">
      <a-space>
        <a-button type="primary" @click="handleDeploy" v-permission="['workflow:definition:deploy']">
          <template #icon><upload-outlined /></template>
          部署流程
        </a-button>
        <a-button @click="handleBatchDisable" :disabled="selectedRowKeys.length === 0" :loading="batchDisableLoading" v-permission="['workflow:definition:update']">
          <template #icon><stop-outlined /></template>
          批量停用
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card>
      <a-table
        :dataSource="tableData"
        :columns="columns"
        :pagination="pagination"
        :loading="loading"
        :row-selection="rowSelection"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '启用' : '停用' }}
            </a-tag>
          </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="handleView(record as ProcessDefinition)" v-permission="['workflow:definition:view']">查看</a-button>
                <a-button type="link" size="small" @click="handleEdit(record as ProcessDefinition)" v-permission="['workflow:definition:update']">编辑</a-button>
                <a-button 
                  v-if="record.status === 1" 
                  type="link" 
                  size="small" 
                  @click="handleDisable(record.id)"
                  v-permission="['workflow:definition:update']"
                >
                  停用
                </a-button>
                <a-button 
                  v-else 
                  type="link" 
                  size="small" 
                  @click="handleEnable(record.id)"
                  v-permission="['workflow:definition:update']"
                >
                  启用
                </a-button>
                <a-popconfirm
                  title="确定要删除这个流程定义吗？"
                  @confirm="handleDelete(record.id)"
                >
                  <a-button type="link" size="small" danger v-permission="['workflow:definition:delete']">删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
        </template>
      </a-table>
    </a-card>

    <!-- 流程部署弹窗 -->
    <a-modal
      v-model:open="deployVisible"
      title="部署流程"
      :confirm-loading="deployLoading"
      @ok="handleDeployOk"
      @cancel="handleDeployCancel"
      width="800px"
    >
      <a-form
        ref="deployFormRef"
        :model="deployForm"
          :rules="deployRules as any"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="流程名称" name="processName">
              <a-input v-model:value="deployForm.processName" placeholder="请输入流程名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="流程KEY" name="processKey">
              <a-input v-model:value="deployForm.processKey" placeholder="请输入流程KEY" />
            </a-form-item>
          </a-col>
        </a-row>
        
        <a-form-item label="流程分类" name="category">
          <a-select v-model:value="deployForm.category" placeholder="请选择流程分类">
            <a-select-option value="HR">人事</a-select-option>
            <a-select-option value="FINANCE">财务</a-select-option>
            <a-select-option value="IT">IT</a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item label="流程描述" name="description">
          <a-textarea v-model:value="deployForm.description" placeholder="请输入流程描述" :rows="3" />
        </a-form-item>
        
        <a-form-item label="BPMN文件" name="bpmnFile">
          <a-upload
            v-model:file-list="fileList"
            :before-upload="beforeUpload"
            :customRequest="() => {}"
            :max-count="1"
            accept=".bpmn,.xml"
          >
            <a-button>
              <template #icon><upload-outlined /></template>
              上传BPMN文件
            </a-button>
          </a-upload>
          <div style="margin-top: 8px; color: #999; font-size: 12px;">
            支持.bpmn和.xml格式文件
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 流程查看弹窗 -->
    <a-modal
      v-model:open="viewVisible"
      title="流程详情"
      width="800px"
      :footer="null"
    >
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="流程名称">{{ currentProcess?.processName }}</a-descriptions-item>
        <a-descriptions-item label="流程KEY">{{ currentProcess?.processKey }}</a-descriptions-item>
        <a-descriptions-item label="分类">{{ currentProcess?.category }}</a-descriptions-item>
        <a-descriptions-item label="版本">{{ currentProcess?.version }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="currentProcess?.status === 1 ? 'green' : 'red'">
            {{ currentProcess?.status === 1 ? '启用' : '停用' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ currentProcess?.createTime }}</a-descriptions-item>
        <a-descriptions-item label="描述" :span="2">{{ currentProcess?.description }}</a-descriptions-item>
      </a-descriptions>
      
      <div style="margin-top: 24px">
        <h4>BPMN流程图</h4>
        <div v-show="!xmlParseError" class="bpmn-preview" ref="bpmnCanvas" style="height: 400px; border: 1px solid #e8e8e8; border-radius: 4px;"></div>
        <div v-if="xmlParseError" style="height: 400px; border: 1px solid #e8e8e8; border-radius: 4px; padding: 20px; background: #fafafa; overflow-y: auto;">
          <a-result status="warning" title="无法预览图形">
            <template #subTitle>
              该 BPMN 文件仅包含执行逻辑（无 &lt;bpmndi:BPMNDiagram&gt; 坐标数据），因此无法生成对应的图形。<br/>
              如果在建模工具中重新绘制并保存，即可在此处正常预览。
            </template>
          </a-result>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UploadOutlined, StopOutlined } from '@ant-design/icons-vue'
import type { TableProps } from 'ant-design-vue'
import { getDefinitionList, deployProcessDefinition, disableProcessDefinition, enableProcessDefinition, deleteProcessDefinition, getProcessDefinitionXml } from '@/api/workflow'
// @ts-ignore
import BpmnViewer from 'bpmn-js/lib/NavigatedViewer'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css'

// 类型定义
interface ProcessDefinition {
  id: number
  processKey: string
  processName: string
  description: string
  category: string
  version: number
  status: number
  createTime: string
  bpmnXml?: string
}

interface SearchForm {
  processName?: string
  processKey?: string
  category?: string
  status?: number
}

// 响应式数据
const router = useRouter()
const loading = ref(false)
const tableData = ref<ProcessDefinition[]>([])
const selectedRowKeys = ref<number[]>([])
const fileList = ref<any[]>([])

// 搜索表单
const searchForm = reactive<SearchForm>({
  processName: '',
  processKey: '',
  category: undefined,
  status: undefined
})

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条记录`
})

// 表格列配置
const columns = [
  {
    title: '流程名称',
    dataIndex: 'processName',
    key: 'processName'
  },
  {
    title: '流程KEY',
    dataIndex: 'processKey',
    key: 'processKey'
  },
  {
    title: '分类',
    dataIndex: 'category',
    key: 'category'
  },
  {
    title: '版本',
    dataIndex: 'version',
    key: 'version'
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status'
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime'
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right' as const,
    width: 250
  }
]

// 表格行选择配置
const rowSelection: TableProps['rowSelection'] = {
  onChange: (selectedKeys: any[]) => {
    selectedRowKeys.value = selectedKeys as number[]
  }
}

// 部署弹窗相关
const deployVisible = ref(false)
const deployLoading = ref(false)
const deployFormRef = ref()

// 部署表单数据
const deployForm = reactive({
  processName: '',
  processKey: '',
  category: undefined as string | undefined,
  description: ''
})

// 部署表单验证规则
const deployRules = {
  processName: [
    { required: true, message: '请输入流程名称', trigger: 'blur' }
  ],
  processKey: [
    { required: true, message: '请输入流程KEY', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '流程KEY只能以英文字母开头，包含字母、数字和下划线', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择流程分类', trigger: 'change' }
  ]
}

// 查看弹窗相关
const viewVisible = ref(false)
const currentProcess = ref<ProcessDefinition | null>(null)
const bpmnCanvas = ref<HTMLElement | null>(null)
const xmlParseError = ref(false)
let bpmnViewer: any = null

const initBpmnViewer = async (xml: string) => {
  xmlParseError.value = false;
  
  if (!bpmnCanvas.value) {
    message.error('流程图容器未初始化');
    return;
  }
  
  if (!bpmnViewer) {
    bpmnViewer = new BpmnViewer({
      container: bpmnCanvas.value
    })
  }
  try {
    const { warnings } = await bpmnViewer.importXML(xml)
    if (warnings && warnings.length) {
      console.warn('BPMN 渲染警告:', warnings)
    }
    // 让图形居中适应屏幕
    bpmnViewer.get('canvas').zoom('fit-viewport')
  } catch (err: any) {
    console.error('BPMN 渲染失败:', err)
    if (err.message && err.message.includes('no diagram to display')) {
      xmlParseError.value = true;
      message.warning('该 BPMN 文件缺少图形坐标，无法预览图画');
    } else {
      message.error('流程图渲染失败，请检查XML格式')
    }
  }
}

// 方法
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      processName: searchForm.processName,
      processKey: searchForm.processKey,
      category: searchForm.category,
      status: searchForm.status
    }
    
    const response = await getDefinitionList(params)
    if (response.data?.code === 200) {
      tableData.value = response.data.data.records
      pagination.total = Number(response.data.data.total) || 0
    } else {
      message.error(response.data?.message || '加载数据失败')
    }
  } catch (error) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  searchForm.processName = ''
  searchForm.processKey = ''
  searchForm.category = undefined
  searchForm.status = undefined
  handleSearch()
}

const handleDeploy = () => {
  Object.assign(deployForm, {
    processName: '',
    processKey: '',
    category: undefined,
    description: ''
  })
  fileList.value = []
  deployVisible.value = true
}

const handleView = async (record: ProcessDefinition) => {
  currentProcess.value = record
  viewVisible.value = true
  
  // 等待弹窗DOM加载完成后初始化 BpmnViewer
  setTimeout(async () => {
    try {
      const res = await getProcessDefinitionXml(record.processKey);
      if (res.data?.code === 200 && res.data?.data) {
        initBpmnViewer(res.data.data);
      } else {
        message.warning('该流程缺少 BPMN XML 源码文件数据，无法预览。');
      }
    } catch (err) {
      message.error('拉取流程图数据失败');
    }
  }, 300)
}

const handleEdit = (record: ProcessDefinition) => {
  router.push(`/workflow/modeler?processKey=${record.processKey}&id=${record.id}`)
}

const handleDelete = async (id: number) => {
  try {
    const res = await deleteProcessDefinition(id.toString());
    if (res.data?.code === 200) {
      message.success('删除成功');
      loadData();
    } else {
      message.error(res.data?.message || '删除失败');
    }
  } catch (err) {
    message.error('删除过程发生错误');
  }
}

const handleDisable = async (id: number) => {
  try {
    const res = await disableProcessDefinition(id.toString());
    if (res.data?.code === 200) {
      message.success('停用成功');
      loadData();
    } else {
      message.error(res.data?.message || '停用失败');
    }
  } catch (err) {
    message.error('请求失败');
  }
}

const handleEnable = async (id: number) => {
  try {
    const res = await enableProcessDefinition(id.toString());
    if (res.data?.code === 200) {
      message.success('启用成功');
      loadData();
    } else {
      message.error(res.data?.message || '启用失败');
    }
  } catch (err) {
    message.error('请求失败');
  }
}

const batchDisableLoading = ref(false)

const handleBatchDisable = async () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要停用的流程')
    return
  }
  
  batchDisableLoading.value = true
  try {
    // 使用 Promise.allSettled 并发调用停用API
    const results = await Promise.allSettled(
      selectedRowKeys.value.map(id => disableProcessDefinition(id.toString()))
    )
    
    // 统计成功和失败的数量
    const successCount = results.filter(
      r => r.status === 'fulfilled' && r.value?.data?.code === 200
    ).length
    const failCount = results.length - successCount
    
    if (failCount === 0) {
      message.success(`批量停用成功，共停用 ${successCount} 个流程`)
    } else if (successCount === 0) {
      message.error('批量停用失败，请检查流程状态')
    } else {
      message.warning(`部分停用成功：${successCount} 个成功，${failCount} 个失败`)
    }
    
    // 清空选择并刷新列表
    selectedRowKeys.value = []
    loadData()
  } catch (error) {
    message.error('批量停用过程发生错误')
  } finally {
    batchDisableLoading.value = false
  }
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

const beforeUpload = (file: File) => {
  const isBpmn = file.type === 'application/xml' || file.name.endsWith('.bpmn') || file.name.endsWith('.xml')
  if (!isBpmn) {
    message.error('只能上传.bpmn或.xml格式的文件!')
    return false
  }
  
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('文件大小不能超过2MB!')
    return false
  }
  
  // 阻止默认上传，我们自己用 fileList 接住
  return false
}

const handleDeployOk = async () => {
  try {
    await deployFormRef.value?.validateFields()
    deployLoading.value = true
    
    if (fileList.value.length === 0) {
      message.error('请上传BPMN文件')
      deployLoading.value = false
      return
    }
    
    const payload = {
      processName: deployForm.processName,
      processKey: deployForm.processKey,
      category: deployForm.category || '',
      description: deployForm.description
    };
    
    // Ant Design Vue 在 beforeUpload 返回 false 时，file本身就是源文件
    const fileItem = fileList.value[0]
    const file = fileItem.originFileObj || fileItem
    
    const response = await deployProcessDefinition(file, payload)
    
    if (response.data?.code === 200) {
      message.success('流程部署成功')
      deployVisible.value = false
      fileList.value = []
      deployFormRef.value?.resetFields()
      loadData()
    } else {
      message.error(response.data?.message || '流程部署失败')
    }
  } catch (error: any) {
    console.error('部署报错:', error);
    message.error(error.message || '流程部署失败')
  } finally {
    deployLoading.value = false
  }
}

const handleDeployCancel = () => {
  deployVisible.value = false
  deployFormRef.value?.resetFields()
  fileList.value = []
}

// 生命周期
onMounted(() => {
  loadData()
})
</script>

<style scoped>
/* 页面特有样式已由全局 index.less 覆盖 */
.process-definition {
  padding: 24px;
  background: transparent;
  min-height: 100%;
}

.bpmn-preview {
  height: 400px;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.02);
}
</style>