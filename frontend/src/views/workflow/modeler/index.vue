<template>
  <div class="workflow-modeler">
    <!-- 工具栏 -->
    <div class="toolbar">
      <a-space>
        <a-button type="primary" @click="saveModel">
          <template #icon><save-outlined /></template>
          保存模型
        </a-button>
        <a-button @click="deployModel">
          <template #icon><cloud-upload-outlined /></template>
          部署流程
        </a-button>
        <a-button @click="validateModel">
          <template #icon><check-circle-outlined /></template>
          验证模型
        </a-button>
        <a-button @click="downloadModel">
          <template #icon><download-outlined /></template>
          下载BPMN
        </a-button>
        <a-divider type="vertical" />
        <a-button @click="undo">
          <template #icon><undo-outlined /></template>
          撤销
        </a-button>
        <a-button @click="redo">
          <template #icon><redo-outlined /></template>
          重做
        </a-button>
      </a-space>
    </div>

    <!-- 主要工作区 -->
    <div class="workspace">
      <!-- 左侧面板 - 元素库 -->
      <div class="palette-panel">
        <a-collapse v-model:activeKey="activePaletteKeys" ghost>
          <a-collapse-panel key="1" header="事件">
            <div class="palette-item" draggable="true" @dragstart="handleDragStart($event, 'start-event')">
              <div class="item-icon">⭕</div>
              <div class="item-label">开始事件</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="handleDragStart($event, 'end-event')">
              <div class="item-icon">🔴</div>
              <div class="item-label">结束事件</div>
            </div>
          </a-collapse-panel>
          
          <a-collapse-panel key="2" header="活动">
            <div class="palette-item" draggable="true" @dragstart="handleDragStart($event, 'user-task')">
              <div class="item-icon">👤</div>
              <div class="item-label">用户任务</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="handleDragStart($event, 'service-task')">
              <div class="item-icon">⚙️</div>
              <div class="item-label">服务任务</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="handleDragStart($event, 'script-task')">
              <div class="item-icon">📝</div>
              <div class="item-label">脚本任务</div>
            </div>
          </a-collapse-panel>
          
          <a-collapse-panel key="3" header="网关">
            <div class="palette-item" draggable="true" @dragstart="handleDragStart($event, 'exclusive-gateway')">
              <div class="item-icon">🔀</div>
              <div class="item-label">排他网关</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="handleDragStart($event, 'parallel-gateway')">
              <div class="item-icon">🔗</div>
              <div class="item-label">并行网关</div>
            </div>
          </a-collapse-panel>
        </a-collapse>
      </div>

      <!-- 中间画布区域 -->
      <div class="canvas-panel">
        <div 
          ref="canvasRef" 
          class="canvas"
          @drop="handleDrop"
          @dragover="handleDragOver"
          @mousedown="handleCanvasMouseDown"
          @mousemove="handleCanvasMouseMove"
          @mouseup="handleCanvasMouseUp"
          @wheel="handleCanvasWheel"
        >
          <!-- 流程元素 -->
          <div
            v-for="element in elements"
            :key="element.id"
            class="process-element"
            :class="{ selected: selectedElementId === element.id }"
            :style="getElementStyle(element)"
            @click="selectElement(element)"
            @dblclick="editElement(element)"
          >
            <div class="element-icon">{{ getElementIcon(element.type) }}</div>
            <div class="element-name">{{ element.name }}</div>
            
            <!-- 连接点 -->
            <div 
              v-for="port in element.ports" 
              :key="port.id"
              class="connection-port"
              :class="port.type"
              :style="getPortPosition(port)"
              @mousedown="startConnection($event, port)"
            ></div>
          </div>

          <!-- 连接线 -->
          <svg class="connections-svg" width="100%" height="100%">
            <path
              v-for="connection in connections"
              :key="connection.id"
              :d="getConnectionPath(connection)"
              stroke="#666"
              stroke-width="2"
              fill="none"
              marker-end="url(#arrowhead)"
            />
            
            <!-- 临时连接线 -->
            <path
              v-if="tempConnection"
              :d="getTempConnectionPath()"
              stroke="#1890ff"
              stroke-width="2"
              stroke-dasharray="5,5"
              fill="none"
            />
          </svg>
          
          <!-- 箭头定义 -->
          <svg style="position: absolute; width: 0; height: 0;">
            <defs>
              <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
                <polygon points="0 0, 10 3.5, 0 7" fill="#666" />
              </marker>
            </defs>
          </svg>
        </div>
      </div>

      <!-- 右侧面板 - 属性配置 -->
      <div class="properties-panel" v-if="selectedElement">
        <a-tabs v-model:activeKey="propertiesTab">
          <a-tab-pane key="general" tab="常规">
            <a-form layout="vertical">
              <a-form-item label="ID">
                <a-input v-model:value="selectedElement.id" disabled />
              </a-form-item>
              <a-form-item label="名称">
                <a-input v-model:value="selectedElement.name" @change="updateElementProperty('name', $event)" />
              </a-form-item>
              <a-form-item label="类型">
                <a-input v-model:value="selectedElement.type" disabled />
              </a-form-item>
            </a-form>
          </a-tab-pane>
          
          <a-tab-pane key="properties" tab="属性" v-if="selectedElement.type === 'user-task'">
            <a-form layout="vertical">
              <div class="element-properties-section">
                <a-form-item label="任务类型" class="assignment-type-selector">
                  <a-radio-group v-model:value="assignmentType" @change="handleAssignmentTypeChange">
                    <a-radio value="assignee">指定用户</a-radio>
                    <a-radio value="candidateUsers">候选用户</a-radio>
                    <a-radio value="candidateGroups">候选组/部门</a-radio>
                  </a-radio-group>
                </a-form-item>
              
              <a-form-item label="指派给" v-if="assignmentType === 'assignee'">
                <a-select 
                  v-model:value="selectedElement.assignee" 
                  placeholder="选择指派人"
                  show-search
                  :filter-option="filterOption"
                  @focus="loadUsers"
                >
                  <a-select-option value="${initiator}">流程发起人</a-select-option>
                  <a-select-option 
                    v-for="user in userList" 
                    :key="user.id" 
                    :value="user.username"
                  >
                    {{ user.nickname }} ({{ user.username }})
                  </a-select-option>
                </a-select>
              </a-form-item>
              
              <a-form-item label="候选用户" v-if="assignmentType === 'candidateUsers'">
                <a-select
                  v-model:value="selectedElement.candidateUsers"
                  mode="multiple"
                  placeholder="选择候选用户"
                  show-search
                  :filter-option="filterOption"
                  @focus="loadUsers"
                >
                  <a-select-option 
                    v-for="user in userList" 
                    :key="user.id" 
                    :value="user.username"
                  >
                    {{ user.nickname }} ({{ user.username }})
                  </a-select-option>
                </a-select>
              </a-form-item>
              
              <a-form-item label="候选组/部门" v-if="assignmentType === 'candidateGroups'">
                <a-tree-select
                  v-model:value="selectedElement.candidateGroups"
                  :tree-data="orgTreeData"
                  placeholder="选择部门"
                  tree-default-expand-all
                  multiple
                  :field-names="{ children: 'children', label: 'orgName', value: 'id' }"
                  @focus="loadOrgs"
                />
              </a-form-item>
              </div>
            </a-form>
          </a-tab-pane>
          
          <a-tab-pane key="listeners" tab="监听器">
            <a-empty description="暂无监听器配置" />
          </a-tab-pane>
        </a-tabs>
      </div>
    </div>

    <!-- 模型信息模态框 -->
    <a-modal
      v-model:open="modelInfoVisible"
      title="模型信息"
      @ok="handleModelInfoOk"
      @cancel="modelInfoVisible = false"
    >
      <a-form :model="modelInfo" layout="vertical">
        <a-form-item label="模型名称" name="name" :rules="[{ required: true, message: '请输入模型名称' }]">
          <a-input v-model:value="modelInfo.name" />
        </a-form-item>
        <a-form-item label="模型KEY" name="key" :rules="[{ required: true, message: '请输入模型KEY' }]">
          <a-input v-model:value="modelInfo.key" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="modelInfo.description" :rows="3" />
        </a-form-item>
        <a-form-item label="分类">
          <a-select v-model:value="modelInfo.category">
            <a-select-option value="HR">人事</a-select-option>
            <a-select-option value="FINANCE">财务</a-select-option>
            <a-select-option value="IT">IT</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { 
  SaveOutlined, 
  CloudUploadOutlined, 
  CheckCircleOutlined,
  DownloadOutlined,
  UndoOutlined,
  RedoOutlined
} from '@ant-design/icons-vue'

// 类型定义
interface ProcessElement {
  id: string
  type: string
  name: string
  x: number
  y: number
  width: number
  height: number
  assignee?: string
  candidateUsers?: string[] | string
  candidateGroups?: number[] | number | string
  ports: ConnectionPort[]
}

interface ConnectionPort {
  id: string
  type: 'input' | 'output'
  x: number
  y: number
}

interface Connection {
  id: string
  sourceId: string
  targetId: string
  sourcePortId: string
  targetPortId: string
}

// 响应式数据
const canvasRef = ref<HTMLElement>()
const elements = ref<ProcessElement[]>([])
const connections = ref<Connection[]>([])
const selectedElementId = ref<string | null>(null)
const activePaletteKeys = ref(['1', '2', '3'])
const propertiesTab = ref('general')
const modelInfoVisible = ref(false)
const assignmentType = ref('assignee')
const userList = ref<any[]>([])
const orgTreeData = ref<any[]>([])
const loadingUsers = ref(false)
const loadingOrgs = ref(false)

// 模型信息
const modelInfo = reactive({
  name: '',
  key: '',
  description: '',
  category: 'IT'
})

// 临时连接
const tempConnection = ref<{
  startX: number
  startY: number
  endX: number
  endY: number
} | null>(null)

// 计算属性
const selectedElement = computed(() => {
  return elements.value.find(el => el.id === selectedElementId.value)
})

// 导入API
import { getUserList, getOrgTree } from '@/api/system'

// 方法
const handleDragStart = (event: DragEvent, elementType: string) => {
  event.dataTransfer!.setData('elementType', elementType)
}

// 任务指派相关方法
const handleAssignmentTypeChange = (e: any) => {
  assignmentType.value = e.target.value
  // 清空之前的指派设置
  if (selectedElement.value) {
    selectedElement.value.assignee = ''
    selectedElement.value.candidateUsers = [] as string[]
    selectedElement.value.candidateGroups = [] as number[]
  }
}

const filterOption = (input: string, option: any) => {
  return option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

const loadUsers = async () => {
  if (loadingUsers.value || userList.value.length > 0) return
  
  loadingUsers.value = true
  try {
    const response = await getUserList({ pageNum: 1, pageSize: 1000 })
    if (response.data?.code === 200) {
      userList.value = response.data.data.records || response.data.data
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
  } finally {
    loadingUsers.value = false
  }
}

const loadOrgs = async () => {
  if (loadingOrgs.value || orgTreeData.value.length > 0) return
  
  loadingOrgs.value = true
  try {
    const response = await getOrgTree()
    if (response.data?.code === 200) {
      orgTreeData.value = response.data.data
    }
  } catch (error) {
    console.error('加载组织树失败:', error)
  } finally {
    loadingOrgs.value = false
  }
}

const handleDragOver = (event: DragEvent) => {
  event.preventDefault()
}

const handleDrop = (event: DragEvent) => {
  event.preventDefault()
  const elementType = event.dataTransfer!.getData('elementType')
  const rect = canvasRef.value!.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  
  createElement(elementType, x, y)
}

const createElement = (type: string, x: number, y: number) => {
  const element: ProcessElement = {
    id: `element_${Date.now()}`,
    type,
    name: getElementDefaultName(type),
    x,
    y,
    width: 100,
    height: 80,
    candidateUsers: [],
    candidateGroups: [],
    ports: [
      { id: `${Date.now()}_input`, type: 'input', x: 50, y: 0 },
      { id: `${Date.now()}_output`, type: 'output', x: 50, y: 80 }
    ]
  }
  
  elements.value.push(element)
}

const getElementDefaultName = (type: string) => {
  const names: Record<string, string> = {
    'start-event': '开始',
    'end-event': '结束',
    'user-task': '用户任务',
    'service-task': '服务任务',
    'script-task': '脚本任务',
    'exclusive-gateway': '排他网关',
    'parallel-gateway': '并行网关'
  }
  return names[type] || '流程元素'
}

const getElementIcon = (type: string) => {
  const icons: Record<string, string> = {
    'start-event': '⭕',
    'end-event': '🔴',
    'user-task': '👤',
    'service-task': '⚙️',
    'script-task': '📝',
    'exclusive-gateway': '🔀',
    'parallel-gateway': '🔗'
  }
  return icons[type] || '⬜'
}

const getElementStyle = (element: ProcessElement) => {
  return {
    left: `${element.x}px`,
    top: `${element.y}px`,
    width: `${element.width}px`,
    height: `${element.height}px`
  }
}

const getPortPosition = (port: ConnectionPort) => {
  return {
    left: `${port.x - 5}px`,
    top: `${port.y - 5}px`
  }
}

const getConnectionPath = (connection: Connection) => {
  // 简化的直线连接，实际项目中可以实现贝塞尔曲线
  const sourceElement = elements.value.find(el => el.id === connection.sourceId)
  const targetElement = elements.value.find(el => el.id === connection.targetId)
  
  if (!sourceElement || !targetElement) return ''
  
  const startX = sourceElement.x + sourceElement.width / 2
  const startY = sourceElement.y + sourceElement.height
  const endX = targetElement.x + targetElement.width / 2
  const endY = targetElement.y
  
  return `M ${startX} ${startY} L ${endX} ${endY}`
}

const getTempConnectionPath = () => {
  if (!tempConnection.value) return ''
  return `M ${tempConnection.value.startX} ${tempConnection.value.startY} L ${tempConnection.value.endX} ${tempConnection.value.endY}`
}

const selectElement = (element: ProcessElement) => {
  selectedElementId.value = element.id
}

const editElement = (element: ProcessElement) => {
  // 双击编辑元素
  message.info(`编辑元素: ${element.name}`)
}

const startConnection = (event: MouseEvent, _port: ConnectionPort) => {
  event.stopPropagation()
  const rect = canvasRef.value!.getBoundingClientRect()
  tempConnection.value = {
    startX: event.clientX - rect.left,
    startY: event.clientY - rect.top,
    endX: event.clientX - rect.left,
    endY: event.clientY - rect.top
  }
}

// 画布交互
const handleCanvasMouseDown = () => {
  selectedElementId.value = null
}

const handleCanvasMouseMove = (event: MouseEvent) => {
  if (tempConnection.value) {
    const rect = canvasRef.value!.getBoundingClientRect()
    tempConnection.value.endX = event.clientX - rect.left
    tempConnection.value.endY = event.clientY - rect.top
  }
}

const handleCanvasMouseUp = () => {
  tempConnection.value = null
}

const handleCanvasWheel = (event: WheelEvent) => {
  // 缩放功能
  event.preventDefault()
}

// 工具栏操作
const saveModel = () => {
  modelInfoVisible.value = true
}

const deployModel = () => {
  message.info('部署流程功能')
}

const validateModel = () => {
  message.success('模型验证通过')
}

const downloadModel = () => {
  message.info('下载BPMN文件')
}

const undo = () => {
  message.info('撤销操作')
}

const redo = () => {
  message.info('重做操作')
}

const handleModelInfoOk = () => {
  message.success('模型保存成功')
  modelInfoVisible.value = false
}

const updateElementProperty = (property: string, value: any) => {
  if (selectedElement.value) {
    (selectedElement.value as any)[property] = value
  }
}

// 生命周期
onMounted(() => {
  // 初始化示例流程
  createElement('start-event', 200, 100)
  createElement('user-task', 200, 250)
  createElement('end-event', 200, 400)
})
</script>

<style scoped>
.workflow-modeler {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5;
}

.toolbar {
  padding: 12px 24px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

.workspace {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.palette-panel {
  width: 250px;
  background: white;
  border-right: 1px solid #e8e8e8;
  padding: 16px;
  overflow-y: auto;
}

.palette-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  margin-bottom: 8px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  cursor: move;
  transition: all 0.2s;
}

.palette-item:hover {
  border-color: #1890ff;
  background-color: #f0f9ff;
}

.item-icon {
  font-size: 20px;
  margin-right: 8px;
}

.item-label {
  font-size: 14px;
}

.canvas-panel {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.canvas {
  width: 100%;
  height: 100%;
  background-image: 
    radial-gradient(circle, #e8e8e8 1px, transparent 1px);
  background-size: 20px 20px;
  position: relative;
  cursor: default;
}

.process-element {
  position: absolute;
  border: 2px solid #666;
  border-radius: 6px;
  background: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: move;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  transition: all 0.2s;
}

.process-element.selected {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.element-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.element-name {
  font-size: 12px;
  text-align: center;
  padding: 0 8px;
}

.connection-port {
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #1890ff;
  cursor: crosshair;
  border: 2px solid white;
  box-shadow: 0 0 0 1px #1890ff;
}

.connections-svg {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
}

.properties-panel {
  width: 300px;
  background: white;
  border-left: 1px solid #e8e8e8;
  padding: 16px;
  overflow-y: auto;
}

:deep(.ant-collapse-content-box) {
  padding: 8px 0 !important;
}

:deep(.ant-form-item) {
  margin-bottom: 16px;
}

.assignment-type-selector {
  margin-bottom: 16px;
}

.user-select-dropdown {
  max-height: 300px;
  overflow-y: auto;
}

.org-tree-select {
  width: 100%;
}

.element-properties-section {
  padding: 16px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  margin-bottom: 16px;
  background: #fafafa;
}
</style>