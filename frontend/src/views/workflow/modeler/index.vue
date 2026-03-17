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
          
          <a-collapse-panel key="2" header="活动 (Tasks)">
            <div class="palette-item" draggable="true" @dragstart="handleDragStart($event, 'user-task')">
              <div class="item-icon">👤</div>
              <div class="item-label">用户任务 (User Task)</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="handleDragStart($event, 'service-task')">
              <div class="item-icon">⚙️</div>
              <div class="item-label">服务任务 (Service Task)</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="handleDragStart($event, 'script-task')">
              <div class="item-icon">📝</div>
              <div class="item-label">脚本任务 (Script Task)</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="handleDragStart($event, 'business-rule-task')">
              <div class="item-icon">⚖️</div>
              <div class="item-label">业务规则任务</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="handleDragStart($event, 'receive-task')">
              <div class="item-icon">📥</div>
              <div class="item-label">接收任务</div>
            </div>
          </a-collapse-panel>
          
          <a-collapse-panel key="3" header="网关 & 容器">
            <div class="palette-item" draggable="true" @dragstart="handleDragStart($event, 'exclusive-gateway')">
              <div class="item-icon">🔀</div>
              <div class="item-label">排他网关</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="handleDragStart($event, 'parallel-gateway')">
              <div class="item-icon">🔗</div>
              <div class="item-label">并行网关</div>
            </div>
            <div class="palette-item" draggable="true" @dragstart="handleDragStart($event, 'call-activity')">
              <div class="item-icon">📞</div>
              <div class="item-label">外部子流程 (Call Activity)</div>
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

          <a-tab-pane key="properties" tab="配置">
            <a-form layout="vertical">
              <!-- 用户任务配置 -->
              <template v-if="selectedElement && selectedElement.type === 'user-task'">
                <a-divider orientation="left">办理人配置</a-divider>
                <a-form-item label="任务类型">
                  <a-radio-group v-model:value="assignmentType" @change="handleAssignmentTypeChange">
                    <a-radio value="assignee">指定用户</a-radio>
                    <a-radio value="candidateUsers">候选用户</a-radio>
                    <a-radio value="candidateGroups">候选组/部门</a-radio>
                  </a-radio-group>
                </a-form-item>
                
                <a-form-item label="指派给" v-if="assignmentType === 'assignee'">
                  <a-input v-model:value="selectedElement.assignee" placeholder="e.g. ${starterId}" />
                </a-form-item>
                
                <a-form-item label="候选用户" v-if="assignmentType === 'candidateUsers'">
                  <a-select v-model:value="selectedElement.candidateUsers" mode="tags" placeholder="输入用户ID" />
                </a-form-item>
                
                <a-form-item label="候选组" v-if="assignmentType === 'candidateGroups'">
                  <a-select v-model:value="selectedElement.candidateGroups" mode="tags" placeholder="输入组ID" />
                </a-form-item>

                <a-divider orientation="left">其他配置</a-divider>
                <a-form-item label="支付日期" name="paymentDate" required>
                  <a-date-picker 
                    v-model:value="selectedElement.paymentDate"
                    style="width: 100%" 
                    placeholder="选择支付日期"
                  />
                </a-form-item>
              </template>

              <!-- 服务任务配置 -->
              <template v-else-if="selectedElement.type === 'service-task'">
                <a-divider orientation="left">服务配置</a-divider>
                <a-form-item label="代理表达式">
                  <a-input v-model:value="selectedElement.delegateExpression" placeholder="${declarationServiceTask}" />
                </a-form-item>
              </template>

              <!-- 监听器配置 -->
              <a-divider orientation="left">监听器</a-divider>
              <a-form-item label="任务监听器" v-if="selectedElement.type === 'user-task'">
                <a-input v-model:value="selectedElement.taskListener" placeholder="${declarationTaskListener}" />
              </a-form-item>
              <a-form-item label="执行监听器">
                <a-input v-model:value="selectedElement.executionListener" placeholder="${declarationTaskListener}" />
              </a-form-item>
            </a-form>
          </a-tab-pane>
          
          <a-tab-pane key="xml" tab="XML 源码">
            <div class="xml-preview">
              <pre><code>{{ generateBpmnXml() }}</code></pre>
            </div>
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
  candidateGroups?: string[] | string
  delegateExpression?: string
  taskListener?: string
  executionListener?: string
  paymentDate?: string // Added paymentDate property
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
import { deployProcessDefinition } from '@/api/workflow'

// BPMN XML 生成逻辑
const generateBpmnXml = () => {
  const processId = modelInfo.key || 'process_' + Date.now()
  const processName = modelInfo.name || '未命名流程'
  
  let xml = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" 
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
             xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
             xmlns:flowable="http://flowable.org/bpmn" 
             targetNamespace="http://www.flowable.org/processdef">
  <process id="${processId}" name="${processName}" isExecutable="true">
`

  // 生成节点
  elements.value.forEach(el => {
    if (el.type === 'start-event') {
      xml += `    <startEvent id="${el.id}" name="${el.name || ''}" flowable:initiator="starterId"></startEvent>\n`
    } else if (el.type === 'end-event') {
      let listeners = ''
      if (el.executionListener) listeners = `      <extensionElements><flowable:executionListener event="end" delegateExpression="${el.executionListener}"></flowable:executionListener></extensionElements>`
      xml += `    <endEvent id="${el.id}" name="${el.name || ''}">${listeners}</endEvent>\n`
    } else if (el.type === 'user-task') {
      let assignment = ''
      if (el.assignee) assignment = ` flowable:assignee="${el.assignee}"`
      if (el.candidateUsers) assignment = ` flowable:candidateUsers="${Array.isArray(el.candidateUsers) ? el.candidateUsers.join(',') : el.candidateUsers}"`
      if (el.candidateGroups) assignment = ` flowable:candidateGroups="${Array.isArray(el.candidateGroups) ? el.candidateGroups.join(',') : el.candidateGroups}"`
      
      let listener = ''
      if (el.taskListener) listener = `      <extensionElements><flowable:taskListener event="create" delegateExpression="${el.taskListener}"></flowable:taskListener></extensionElements>`
      
      xml += `    <userTask id="${el.id}" name="${el.name || ''}"${assignment}>\n${listener}    </userTask>\n`
    } else if (el.type === 'service-task') {
      xml += `    <serviceTask id="${el.id}" name="${el.name || ''}" flowable:delegateExpression="${el.delegateExpression || ''}"></serviceTask>\n`
    } else if (el.type === 'exclusive-gateway') {
      xml += `    <exclusiveGateway id="${el.id}" name="${el.name || ''}"></exclusiveGateway>\n`
    } else if (el.type === 'parallel-gateway') {
      xml += `    <parallelGateway id="${el.id}" name="${el.name || ''}"></parallelGateway>\n`
    }
  })

  // 生成连接线
  connections.value.forEach(conn => {
    xml += `    <sequenceFlow id="${conn.id}" sourceRef="${conn.sourceId}" targetRef="${conn.targetId}"></sequenceFlow>\n`
  })

  xml += `  </process>\n</definitions>`
  return xml
}

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
    selectedElement.value.candidateGroups = [] as string[]
  }
}

// 拖拽处理
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

// ID 计数器，防止冲突
let idCounter = 0

const createElement = (type: string, x: number, y: number) => {
  const timestamp = Date.now()
  const uniqueId = `${timestamp}_${++idCounter}`
  
  const element: ProcessElement = {
    id: `element_${uniqueId}`,
    type,
    name: getElementDefaultName(type),
    x,
    y,
    width: 100,
    height: 80,
    assignee: '',
    candidateUsers: [] as string[],
    candidateGroups: [] as string[],
    ports: [
      { id: `${uniqueId}_input`, type: 'input' as const, x: 50, y: 0 },
      { id: `${uniqueId}_output`, type: 'output' as const, x: 50, y: 80 }
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
    'business-rule-task': '⚖️',
    'receive-task': '📥',
    'call-activity': '📞',
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

const deployModel = async () => {
  if (!modelInfo.key || !modelInfo.name) {
    saveModel() // 如果没有信息先弹窗
    return
  }
  
  const xml = generateBpmnXml()
  const blob = new Blob([xml], { type: 'text/xml' })
  const file = new File([blob], `${modelInfo.key}.bpmn20.xml`, { type: 'text/xml' })
  
  try {
    message.loading({ content: '部署中...', key: 'deploy' })
    await deployProcessDefinition(file, {
      name: modelInfo.name,
      processKey: modelInfo.key,
      category: modelInfo.category,
      description: modelInfo.description
    })
    message.success({ content: '流程部署成功', key: 'deploy' })
  } catch (error) {
    message.error({ content: '流程部署失败', key: 'deploy' })
  }
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
  // 初始化一个完整的申报生命周期流程 (示例)
  createElement('start-event', 200, 30)       // 0: Start
  createElement('user-task', 200, 120)        // 1: 部门初审 (deptAudit)
  createElement('exclusive-gateway', 200, 220)// 2: 网关
  createElement('service-task', 50, 210)      // 3: 自动生成单证 (genContractTask)
  createElement('user-task', 50, 300)        // 4: 定金审核 (depositAudit)
  createElement('user-task', 50, 400)        // 5: 尾款审核 (balanceAudit)
  createElement('end-event', 200, 520)        // 6: End
  
  setTimeout(() => {
    if (elements.value.length >= 7) {
      elements.value[1].name = '部门经理初审'
      elements.value[1].candidateGroups = ['DEPT_MANAGER']
      elements.value[1].taskListener = '${declarationTaskListener}'
      
      elements.value[2].name = '初审通过?'
      
      elements.value[3].name = '自动生成单证'
      elements.value[3].delegateExpression = '${declarationServiceTask}'
      
      elements.value[4].name = '财务经理定金审核'
      elements.value[4].candidateGroups = ['FINANCE_MANAGER']
      elements.value[4].taskListener = '${declarationTaskListener}'
      
      elements.value[5].name = '尾款支付审核'
      elements.value[5].candidateGroups = ['FINANCE_MANAGER']
      elements.value[5].taskListener = '${declarationTaskListener}'
      
      // 连接
      const addConn = (sIdx: number, tIdx: number, label?: string) => {
        connections.value.push({
          id: `flow_${sIdx}_${tIdx}`,
          sourceId: elements.value[sIdx].id,
          targetId: elements.value[tIdx].id,
          sourcePortId: elements.value[sIdx].ports[1].id,
          targetPortId: elements.value[tIdx].ports[0].id
        })
      }
      
      addConn(0, 1)
      addConn(1, 2)
      addConn(2, 3) // 通过
      addConn(3, 4)
      addConn(4, 5)
      addConn(5, 6)
    }
  }, 200)
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