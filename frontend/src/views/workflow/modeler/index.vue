<template>
  <div class="workflow-modeler">
    <!-- 顶部工具栏 -->
    <div class="modeler-header">
      <div class="header-left">
        <a-button type="text" @click="$router.back()">
          <template #icon><arrow-left-outlined /></template>
        </a-button>
        <span class="modeler-title">{{ modelInfo.name || '创建新流程' }}</span>
        <a-tag v-if="modelInfo.key" color="blue">{{ modelInfo.key }}</a-tag>
      </div>
      <div class="header-actions">
        <a-space>
          <a-upload
            :show-upload-list="false"
            :before-upload="handleImportBpmn"
            accept=".bpmn,.xml"
          >
            <a-button>
              <template #icon><upload-outlined /></template>
              导入BPMN
            </a-button>
          </a-upload>
          <a-button @click="handleDownload">
            <template #icon><download-outlined /></template>
            下载BPMN
          </a-button>
          <a-button type="primary" @click="handleSave" :loading="saving">
            <template #icon><save-outlined /></template>
            保存并部署
          </a-button>
        </a-space>
      </div>
    </div>

    <div class="modeler-container">
      <!-- 画布区域 -->
      <div class="canvas-container" ref="canvasRef"></div>

      <!-- 快捷工具栏 (左侧浮动) -->
      <div class="floating-toolbar">
         <a-tooltip title="垂直居中" placement="right">
           <a-button shape="circle" @click="handleZoom('fit-viewport')">
             <template #icon><aim-outlined /></template>
           </a-button>
         </a-tooltip>
         <a-tooltip title="放大" placement="right">
           <a-button shape="circle" @click="handleZoom('zoom-in')">
             <template #icon><plus-outlined /></template>
           </a-button>
         </a-tooltip>
         <a-tooltip title="缩小" placement="right">
           <a-button shape="circle" @click="handleZoom('zoom-out')">
             <template #icon><minus-outlined /></template>
           </a-button>
         </a-tooltip>
         <a-divider style="margin: 8px 0" />
         <a-tooltip title="键盘快捷键" placement="right">
           <a-button shape="circle" @click="showShortcuts = true">
             <template #icon><question-outlined /></template>
           </a-button>
         </a-tooltip>
      </div>

      <!-- 右侧属性面板 -->
      <div class="properties-side-panel" :class="{ collapsed: panelCollapsed }">
        <div class="panel-toggle" @click="panelCollapsed = !panelCollapsed">
          <right-outlined v-if="panelCollapsed" />
          <left-outlined v-else />
        </div>
        
        <div class="panel-content" v-show="!panelCollapsed">
          <div class="panel-header">
            <h4>属性配置</h4>
            <a-tag color="purple">{{ selectedElementType?.replace('bpmn:', '') || '全局' }}</a-tag>
          </div>
          
          <div class="panel-body">
            <a-form layout="vertical">
              <!-- 全局流程属性 (未选择任何元素时显示) -->
              <template v-if="!selectedElement">
                <a-form-item label="流程名称">
                  <a-input v-model:value="modelInfo.name" placeholder="请输入流程名称" @change="updateProcessAttribute('name', modelInfo.name)" />
                </a-form-item>
                <a-form-item label="流程KEY (ID)">
                  <a-input v-model:value="modelInfo.key" placeholder="请输入唯一标识" @change="updateProcessAttribute('id', modelInfo.key)" />
                </a-form-item>
                <a-form-item label="业务分类">
                  <a-select v-model:value="modelInfo.category" placeholder="请选择分类">
                    <a-select-option value="HR">人事</a-select-option>
                    <a-select-option value="FINANCE">财务</a-select-option>
                    <a-select-option value="IT">IT</a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item label="描述">
                  <a-textarea v-model:value="modelInfo.description" placeholder="请输入详细描述" :rows="3" />
                </a-form-item>
              </template>

              <!-- 选中的节点属性 -->
              <template v-else>
                <a-form-item label="节点名称">
                  <a-input v-model:value="elementProperties.name" @change="updateElementProperty('name', elementProperties.name)" />
                </a-form-item>
                <a-form-item label="节点ID">
                  <a-input v-model:value="elementProperties.id" @change="updateElementProperty('id', elementProperties.id)" />
                </a-form-item>

                <!-- 用户任务特有属性 -->
                <template v-if="selectedElementType === 'bpmn:UserTask'">
                  <a-divider>任务配置</a-divider>
                  <a-form-item label="受理人 (Assignee)">
                    <a-input v-model:value="elementProperties.assignee" placeholder="e.g. ${starterId}" @change="updateFlowableProperty('assignee', elementProperties.assignee)" />
                  </a-form-item>
                  <a-form-item label="候选组 (Candidate Groups)">
                    <a-input v-model:value="elementProperties.candidateGroups" placeholder="多个逗号分隔" @change="updateFlowableProperty('candidateGroups', elementProperties.candidateGroups)" />
                  </a-form-item>
                  <a-form-item label="任务监听器 (Listener)">
                    <a-input v-model:value="elementProperties.taskListener" placeholder="Java 类名或表达式" @change="updateFlowableExtension('taskListener', elementProperties.taskListener)" />
                  </a-form-item>
                </template>

                <!-- 服务任务特有属性 -->
                <template v-if="selectedElementType === 'bpmn:ServiceTask'">
                  <a-divider>服务逻辑</a-divider>
                  <a-form-item label="代理表达式 (Delegate Expression)">
                    <a-input v-model:value="elementProperties.delegateExpression" placeholder="${myServiceBean}" @change="updateFlowableProperty('delegateExpression', elementProperties.delegateExpression)" />
                  </a-form-item>
                </template>

                <!-- 序列流条件 -->
                <template v-if="selectedElementType === 'bpmn:SequenceFlow'">
                   <a-divider>流转条件</a-divider>
                   <a-form-item label="条件表达式">
                     <a-input v-model:value="elementProperties.condition" placeholder="${approved == true}" @change="updateSequenceFlowCondition(elementProperties.condition)" />
                   </a-form-item>
                </template>
              </template>
            </a-form>
          </div>
        </div>
      </div>
    </div>

    <!-- 弹窗：快捷键说明 -->
    <a-modal v-model:open="showShortcuts" title="键盘快捷键" :footer="null">
      <ul class="shortcut-list">
        <li><span>Ctrl + Z</span> 撤销</li>
        <li><span>Ctrl + Y / Ctrl + Shift + Z</span> 重做</li>
        <li><span>Ctrl + C</span> 复制</li>
        <li><span>Ctrl + V</span> 粘贴</li>
        <li><span>Delete / Backspace</span> 删除元素</li>
        <li><span>Direct Editing</span> 双击元素编辑名称</li>
      </ul>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  ArrowLeftOutlined, 
  DownloadOutlined, 
  SaveOutlined,
  UploadOutlined,
  PlusOutlined,
  MinusOutlined,
  AimOutlined,
  QuestionOutlined,
  LeftOutlined,
  RightOutlined
} from '@ant-design/icons-vue'
// @ts-ignore
import Modeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css'

import { deployProcessDefinition, getProcessDefinitionXml } from '@/api/workflow'

const router = useRouter()
const route = useRoute()

// 响应式数据
const canvasRef = ref<HTMLElement>()
const bpmnModeler = ref<any>(null)
const panelCollapsed = ref(false)
const showShortcuts = ref(false)
const saving = ref(false)

// 选中的模型/元素信息
const modelInfo = reactive({
  id: undefined as number | undefined,
  name: '未命名流程',
  key: 'process_' + Date.now(),
  category: 'IT',
  description: ''
})

const selectedElement = ref<any>(null)
const selectedElementType = ref<string>('')
const elementProperties = reactive<Record<string, any>>({
  id: '',
  name: '',
  assignee: '',
  candidateGroups: '',
  delegateExpression: '',
  taskListener: '',
  condition: ''
})

// 默认 XML 模板
const defaultXml = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL" 
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" 
             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC" 
             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI" 
             xmlns:flowable="http://flowable.org/bpmn" 
             targetNamespace="http://www.flowable.org/processdef">
  <process id="process_1" name="新流程" isExecutable="true">
    <startEvent id="startEvent_1" name="开始" flowable:initiator="starterId" />
  </process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="process_1">
      <bpmndi:BPMNShape id="startEvent_1_di" bpmnElement="startEvent_1">
        <omgdc:Bounds x="150" y="150" width="36" height="36" />
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>`

// 初始化 Modeler
const initModeler = async () => {
  if (!canvasRef.value) return

  bpmnModeler.value = new Modeler({
    container: canvasRef.value,
    keyboard: {
      bindTo: document
    }
  })

  // 监听选择变更
  bpmnModeler.value.on('selection.changed', (e: any) => {
    const newSelection = e.newSelection[0]
    updatePropertiesFromElement(newSelection)
  })

  // 监听属性变更 (同步回由于双击产生的修改)
  bpmnModeler.value.on('element.changed', (e: any) => {
    const { element } = e
    if (selectedElement.value && selectedElement.value.id === element.id) {
       updatePropertiesFromElement(element)
    }
  })

  // 载入数据
  try {
    const processKey = route.query.processKey as string
    const id = route.query.id as string
    
    if (processKey) {
      const res = await getProcessDefinitionXml(processKey)
      if (res.data?.code === 200 && res.data.data) {
        await bpmnModeler.value.importXML(res.data.data)
        // 尝试从 XML 中提取流程名称和 ID
        const definitions = bpmnModeler.value.getDefinitions()
        const process = definitions.rootElements.find((e: any) => e.$type === 'bpmn:Process')
        if (process) {
          modelInfo.name = process.name || modelInfo.name
          modelInfo.key = process.id || modelInfo.key
        }
        if (id) modelInfo.id = Number(id)
      } else {
        await bpmnModeler.value.importXML(defaultXml)
      }
    } else {
      await bpmnModeler.value.importXML(defaultXml)
    }
  } catch (error) {
    message.error('载入流程图失败，已加载默认模板')
    await bpmnModeler.value.importXML(defaultXml)
  }
}

// 同步元素属性到 reactive 对象
const updatePropertiesFromElement = (element: any) => {
  selectedElement.value = element
  if (!element) {
    selectedElementType.value = ''
    return
  }
  
  const businessObject = element.businessObject
  selectedElementType.value = businessObject.$type
  
  elementProperties.id = businessObject.id
  elementProperties.name = businessObject.name || ''
  
  // Flowable 扩展属性
  elementProperties.assignee = businessObject.get('flowable:assignee') || ''
  elementProperties.candidateGroups = businessObject.get('flowable:candidateGroups') || ''
  elementProperties.delegateExpression = businessObject.get('flowable:delegateExpression') || ''
  
  // 监听器 (简化版：查找第一个 taskListener)
  const extensionElements = businessObject.get('extensionElements')
  if (extensionElements && extensionElements.values) {
    const listener = extensionElements.values.find((v: any) => v.$type === 'flowable:TaskListener')
    elementProperties.taskListener = listener ? (listener.delegateExpression || listener.class) : ''
  } else {
    elementProperties.taskListener = ''
  }

  // 条件表达式
  if (businessObject.conditionExpression) {
    elementProperties.condition = businessObject.conditionExpression.body || ''
  } else {
    elementProperties.condition = ''
  }
}

// 更新标准属性
const updateElementProperty = (key: string, value: any) => {
  if (!selectedElement.value) return
  const modeling = bpmnModeler.value.get('modeling')
  modeling.updateProperties(selectedElement.value, { [key]: value })
}

// 更新 Flowable 扩展属性
const updateFlowableProperty = (key: string, value: any) => {
  if (!selectedElement.value) return
  const modeling = bpmnModeler.value.get('modeling')
  modeling.updateModdleProperties(selectedElement.value, selectedElement.value.businessObject, { [`flowable:${key}`]: value })
}

// 更新 Flowable 扩展元素 (如监听器)
const updateFlowableExtension = (_name: string, value: any) => {
  if (!selectedElement.value) return
  const moddle = bpmnModeler.value.get('moddle')
  const modeling = bpmnModeler.value.get('modeling')
  const businessObject = selectedElement.value.businessObject
  
  let extensionElements = businessObject.get('extensionElements')
  if (!extensionElements) {
    extensionElements = moddle.create('bpmn:ExtensionElements', { values: [] })
    modeling.updateProperties(selectedElement.value, { extensionElements })
  }

  // 简单处理：如果为空则移除，否则覆盖第一个
  if (!value) {
    extensionElements.values = extensionElements.values.filter((v: any) => v.$type !== `flowable:TaskListener`)
  } else {
    let listener = extensionElements.values.find((v: any) => v.$type === `flowable:TaskListener`)
    if (!listener) {
      listener = moddle.create('flowable:TaskListener', { event: 'create' })
      extensionElements.values.push(listener)
    }
    // 判断是类还是表达式
    if (value.startsWith('$')) {
      listener.delegateExpression = value
    } else {
      listener.class = value
    }
  }
}

// 更新条件
const updateSequenceFlowCondition = (condition: string) => {
  if (!selectedElement.value || selectedElementType.value !== 'bpmn:SequenceFlow') return
  const moddle = bpmnModeler.value.get('moddle')
  const modeling = bpmnModeler.value.get('modeling')
  
  if (!condition) {
    modeling.updateProperties(selectedElement.value, { conditionExpression: undefined })
    return
  }

  const conditionExpression = moddle.create('bpmn:FormalExpression', {
    body: condition
  })
  modeling.updateProperties(selectedElement.value, { conditionExpression })
}

// 更新全局流程属性
const updateProcessAttribute = (key: string, value: any) => {
  const definitions = bpmnModeler.value.getDefinitions()
  const process = definitions.rootElements.find((e: any) => e.$type === 'bpmn:Process')
  if (process) {
    process[key] = value
  }
}

// 工具栏操作
const handleZoom = (action: string) => {
  if (action === 'fit-viewport') {
    bpmnModeler.value.get('canvas').zoom('fit-viewport')
  } else {
    bpmnModeler.value.get('editorActions').trigger(action)
  }
}

const handleDownload = async () => {
  try {
    const { xml } = await bpmnModeler.value.saveXML({ format: true })
    const encodedData = encodeURIComponent(xml)
    const link = document.createElement('a')
    link.setAttribute('href', 'data:application/bpmn20-xml;charset=UTF-8,' + encodedData)
    link.setAttribute('download', `${modelInfo.key}.bpmn20.xml`)
    link.click()
  } catch (err) {
    message.error('导出 XML 失败')
  }
}

// 导入BPMN文件
const handleImportBpmn = (file: File) => {
  const reader = new FileReader()
  reader.onload = async (e) => {
    try {
      const xml = e.target?.result as string
      if (!xml) {
        message.error('文件内容为空')
        return
      }
      
      await bpmnModeler.value.importXML(xml)
      
      // 从导入的XML中提取流程信息
      const definitions = bpmnModeler.value.getDefinitions()
      const process = definitions.rootElements.find((p: any) => p.$type === 'bpmn:Process')
      if (process) {
        modelInfo.name = process.name || '导入的流程'
        modelInfo.key = process.id || 'imported_process'
      }
      
      // 居中显示
      bpmnModeler.value.get('canvas').zoom('fit-viewport')
      message.success('BPMN文件导入成功')
    } catch (err) {
      console.error('导入失败:', err)
      message.error('BPMN文件格式错误，导入失败')
    }
  }
  reader.readAsText(file)
  return false // 阻止默认上传行为
}

const handleSave = async () => {
  if (!modelInfo.name || !modelInfo.key) {
    message.warning('请先完善流程名称和唯一标识')
    return
  }

  saving.value = true
  try {
    // 强制同步 XML 中的 process id
    const definitions = bpmnModeler.value.getDefinitions()
    const process = definitions.rootElements.find((e: any) => e.$type === 'bpmn:Process')
    if (process) {
      process.id = modelInfo.key
      process.name = modelInfo.name
    }

    const { xml } = await bpmnModeler.value.saveXML({ format: true })
    
    const blob = new Blob([xml], { type: 'text/xml' })
    const file = new File([blob], `${modelInfo.key}.bpmn20.xml`, { type: 'text/xml' })
    
    const payload = {
      processName: modelInfo.name,
      processKey: modelInfo.key,
      category: modelInfo.category,
      description: modelInfo.description
    }

    const res = await deployProcessDefinition(file, payload)
    if (res.data?.code === 200) {
      message.success('流程部署成功！')
      router.push('/workflow/definition')
    } else {
      message.error(res.data?.message || '部署失败')
    }
  } catch (err) {
    message.error('保存过程中出错')
    console.error(err)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  nextTick(() => {
    initModeler()
  })
})

onUnmounted(() => {
  if (bpmnModeler.value) {
    bpmnModeler.value.destroy()
  }
})
</script>

<style scoped>
.workflow-modeler {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f8f9fa;
  overflow: hidden;
}

.modeler-header {
  height: 60px;
  min-height: 60px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid #e8eaec;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.modeler-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f1f1f;
}

.modeler-container {
  flex: 1;
  display: flex;
  position: relative;
  overflow: hidden;
  min-height: 0;
}

.canvas-container {
  flex: 1;
  min-width: 0;
  height: 100%;
  background-image: 
    radial-gradient(circle, #dcdcdc 1px, transparent 1px);
  background-size: 20px 20px;
  overflow: hidden;
}

.canvas-container :deep(.djs-container) {
  width: 100% !important;
  height: 100% !important;
}

.floating-toolbar {
  position: absolute;
  left: 20px;
  bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  padding: 10px;
  border-radius: 30px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  z-index: 10;
}

.properties-side-panel {
  width: 350px;
  min-width: 350px;
  flex-shrink: 0;
  height: 100%;
  background: white;
  border-left: 1px solid #e8eaec;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  display: flex;
  flex-direction: column;
  z-index: 20;
  box-shadow: -2px 0 8px rgba(0,0,0,0.05);
}

.properties-side-panel.collapsed {
  width: 0;
  min-width: 0;
  border-left: none;
  box-shadow: none;
  overflow: visible;
}

.properties-side-panel.collapsed .panel-content {
  display: none;
}

.properties-side-panel.collapsed .panel-toggle {
  left: -32px;
}

.panel-toggle {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 32px;
  height: 64px;
  background: white;
  border: 1px solid #e8eaec;
  border-radius: 8px 0 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #1890ff;
  box-shadow: -2px 0 8px rgba(0,0,0,0.1);
  z-index: 21;
  font-size: 16px;
  transition: all 0.3s;
}

.panel-toggle:hover {
  background: #f5f5f5;
  color: #40a9ff;
}

.panel-content {
  width: 350px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.panel-header h4 {
  margin: 0;
  font-size: 16px;
  color: #262626;
}

.panel-body {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  min-height: 0;
}

.shortcut-list {
  padding: 0;
  list-style: none;
}

.shortcut-list li {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f0f0;
}

.shortcut-list span {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
}

/* 覆盖 bpmn-js 默认样式使其更美观 */
:deep(.bjs-powered-by) {
  display: none;
}

:deep(.djs-palette) {
  top: 20px;
  left: 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border: 1px solid #e8eaec;
}

:deep(.djs-context-pad) {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
</style>