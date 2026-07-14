<template>
  <div class="flow-template-management">
    <!-- 搜索区域 -->
    <a-card class="ui-card search-card" :bordered="false">
      <a-form layout="inline">
        <a-form-item label="所属流程">
          <a-select v-model:value="filterProcessType" :options="processTypeOptions" style="width: 160px" placeholder="请选择流程类型" class="ui-select" @change="loadTemplates" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="loadTemplates" class="ui-btn-primary">
              <template #icon><ReloadOutlined /></template>
              查询
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 操作按钮区域 -->
    <a-card class="ui-card operation-card" :bordered="false">
      <a-space>
        <a-button type="primary" @click="openAddModal" v-permission="['system:flow-template:add']" class="ui-btn-cta">
          <template #icon><PlusOutlined /></template>
          新增模板
        </a-button>
        <a-button @click="loadTemplates" class="ui-btn-secondary">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
        <a-button danger @click="handleMigratePreview" v-permission="['business:declaration:resume:flow']" class="ui-btn-secondary">
          <template #icon><SwapOutlined /></template>
          一键迁移流程
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="ui-card" :bordered="false">
      <a-table
        :dataSource="templateList"
        :columns="tplColumns"
        :loading="loadingTemplates"
        :pagination="false"
        rowKey="id"
        size="small"
        class="ui-table"
        :scroll="{ x: 1100 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="openNodeEditor(record as FlowTemplate)" class="font-medium">{{ record.name }}</a>
          </template>

          <template v-else-if="column.key === 'processType'">
            <a-tag :color="processTypeColorMap[record.processType] || 'default'">
              {{ processTypeLabelMap[record.processType] || record.processType || '-' }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'declarationType'">
            <a-tag v-if="record.declarationType" :color="record.declarationType === 'SELF' ? 'blue' : 'green'">
              {{ record.declarationType === 'SELF' ? '梓熠、理德' : '集洛' }}
            </a-tag>
            <span v-else class="cell-muted">-</span>
          </template>

          <template v-else-if="column.key === 'isDefault'">
            <a-tag v-if="record.isDefault === 1" color="blue">默认</a-tag>
            <span v-else class="cell-muted">-</span>
          </template>

          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'success' : 'error'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'nodeCount'">
            <a-tag color="geekblue">{{ getNodeCount(record as FlowTemplate) }} 个节点</a-tag>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space :size="2">
              <a-button type="link" size="small" @click="openNodeEditor(record as FlowTemplate)"
                v-permission="['system:flow-template:update']"
                style="padding: 0 4px">
                <template #icon><SettingOutlined /></template>
                编排流程
              </a-button>
              <a-button type="link" size="small" @click="openEditModal(record as FlowTemplate)"
                v-permission="['system:flow-template:update']"
                style="padding: 0 4px">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-popconfirm title="确定删除该模板？" @confirm="handleDelete((record as FlowTemplate).id!)" okText="确定" cancelText="取消">
                <a-button type="link" size="small" danger v-permission="['system:flow-template:delete']" style="padding: 0 4px">
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑模板信息弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑模板' : '新增模板'"
      @ok="handleModalSubmit"
      :confirmLoading="modalLoading"
      :width="480"
      destroyOnClose
    >
      <a-form :model="modalForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" style="margin-top: 16px">
        <a-form-item label="模板名称" required>
          <a-input v-model:value="modalForm.name" placeholder="如：标准流程、简化流程" :maxlength="100" />
        </a-form-item>
        <a-form-item label="流程类型" required>
          <a-select v-model:value="modalForm.processType" placeholder="选择流程类型" :disabled="isEdit">
            <a-select-option v-for="opt in processTypeOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </a-select-option>
          </a-select>
          <div class="tpl-hint">创建后不可修改</div>
        </a-form-item>
        <a-form-item label="申报类型" v-if="modalForm.processType === 'declaration'">
          <a-select v-model:value="modalForm.declarationType" placeholder="选择申报类型">
            <a-select-option value="EXTERNAL">集洛</a-select-option>
            <a-select-option value="SELF">梓熠、理德</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="模板编码" required>
          <a-input v-model:value="modalForm.code" placeholder="英文编码，如 STANDARD" :maxlength="50" :disabled="isEdit" />
          <div class="tpl-hint">创建后不可修改</div>
        </a-form-item>
        <a-form-item label="模板说明">
          <a-textarea v-model:value="modalForm.description" placeholder="可选" :rows="3" />
        </a-form-item>
        <a-form-item label="设为默认">
          <a-switch v-model:checked="modalDefaultChecked" checked-children="是" un-checked-children="否" />
          <span class="tpl-switch-hint">新申报单默认使用此模板</span>
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="modalForm.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 流程编排弹窗 -->
    <a-modal
      v-model:open="nodeEditorVisible"
      :title="'流程编排 - ' + (editingTemplate?.name ?? '')"
      :width="920"
      :footer="null"
      destroyOnClose
      class="node-editor-modal"
    >
      <!-- 模板摘要 -->
      <div class="editor-summary">
        <div class="editor-summary-info">
          <div><span class="summary-label">编码:</span> <span class="summary-value">{{ editingTemplate?.code }}</span></div>
          <div><span class="summary-label">类型:</span>
            <a-tag :color="processTypeColorMap[editingTemplate?.processType || ''] || 'default'" size="small">
              {{ processTypeLabelMap[editingTemplate?.processType || ''] || editingTemplate?.processType || '-' }}
            </a-tag>
          </div>
          <div><span class="summary-label">说明:</span> <span class="summary-desc">{{ editingTemplate?.description || '-' }}</span></div>
        </div>
        <a-space>
          <a-button size="small" @click="openAddNodeModal" v-permission="['system:flow-template:update']">
            <template #icon><PlusOutlined /></template>
            添加节点
          </a-button>
          <a-button size="small" @click="handlePreviewBpmn">
            <template #icon><CodeOutlined /></template>
            预览BPMN
          </a-button>
          <a-popconfirm title="确定生成并部署到 Flowable？" @confirm="handleDeployBpmn" okText="部署" cancelText="取消">
            <a-button size="small" :loading="deploying"
              v-permission="['system:flow-template:update']"
              class="deploy-btn">
              <template #icon><SendOutlined /></template>
              部署BPMN
            </a-button>
          </a-popconfirm>
        </a-space>
      </div>

      <!-- 节点编排表格 -->
      <a-spin :spinning="loadingNodes">
        <a-table
          :dataSource="nodeList"
          :columns="nodeColumns"
          :pagination="false"
          rowKey="nodeId"
          size="small"
          class="ui-table node-editor-table"
          :scroll="{ x: 1060 }"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'sortOrder'">
              <div class="sort-cell">
                <span class="sort-index">{{ index + 1 }}</span>
                <div class="sort-btns">
                  <a-button
                    size="small" type="text" class="sort-btn"
                    :disabled="index === 0" @click="moveNode(index, -1)"
                  ><UpOutlined /></a-button>
                  <a-button
                    size="small" type="text" class="sort-btn"
                    :disabled="index === nodeList.length - 1" @click="moveNode(index, 1)"
                  ><DownOutlined /></a-button>
                </div>
              </div>
            </template>

            <template v-else-if="column.key === 'nodeKey'">
              <code class="node-key-tag">{{ record.node?.nodeKey }}</code>
            </template>

            <template v-else-if="column.key === 'nodeType'">
              <a-tag :color="record.node?.nodeType === 'userTask' ? 'blue' : 'orange'" size="small">
                {{ record.node?.nodeType === 'userTask' ? '用户' : '服务' }}
              </a-tag>
            </template>

            <template v-else-if="column.key === 'targetStatus'">
              <a-tag v-if="record.node?.targetStatus != null" color="geekblue" class="status-tag">{{ record.node?.targetStatus }}</a-tag>
              <span v-else class="cell-muted">-</span>
            </template>

            <template v-else-if="column.key === 'formSection'">
              <a-tag v-if="record.node?.formSection" :color="sectionColorMap[record.node?.formSection] || 'default'">
                {{ record.node?.formSection }}
              </a-tag>
              <span v-else class="cell-muted">-</span>
            </template>

            <template v-else-if="column.key === 'assignee'">
              <template v-if="record.node?.nodeType === 'userTask'">
                <AutoComplete
                  v-model:value="record.assignee"
                  :options="assigneeOptions"
                  :placeholder="record.node?.assignee || '选择或输入'"
                  size="small"
                  style="width: 140px"
                  allowClear
                  :filterOption="(input: string, option: any) => option.label.toLowerCase().includes(input.toLowerCase())"
                />
              </template>
              <span v-else class="cell-muted">-</span>
            </template>

            <template v-else-if="column.key === 'candidateGroups'">
              <template v-if="record.node?.nodeType === 'userTask'">
                <a-input
                  v-model:value="record.candidateGroups"
                  :placeholder="record.node?.candidateGroups || '如 MATERIAL_AUDITOR'"
                  size="small"
                  style="width: 140px"
                  allowClear
                />
              </template>
              <span v-else class="cell-muted">-</span>
            </template>

            <template v-else-if="column.key === 'enabled'">
              <a-switch
                :checked="record.enabled === 1"
                @change="(val: any) => { record.enabled = val ? 1 : 0 }"
                checked-children="启用"
                un-checked-children="跳过"
                size="small"
              />
            </template>

            <template v-else-if="column.key === 'action'">
              <a-popconfirm title="从编排中移除此节点？" @confirm="removeNode(index)" okText="移除" cancelText="取消">
                <a-button type="link" size="small" danger class="remove-btn">
                  <template #icon><DeleteOutlined /></template>
                  移除
                </a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table>

        <a-alert
          v-if="nodeList.some(n => n.enabled === 0)"
          type="warning" show-icon style="margin-top: 12px"
          message="部分节点已设置为跳过，申报流程中这些环节将被自动跳过。"
        />
      </a-spin>

      <!-- 底部操作栏 -->
      <div class="editor-footer">
        <a-button @click="nodeEditorVisible = false" class="footer-btn">关闭</a-button>
        <a-button type="primary" :loading="savingNodes" @click="handleSaveNodes"
          v-permission="['system:flow-template:update']" class="footer-btn footer-btn-primary">
          <template #icon><SaveOutlined /></template>
          保存编排
        </a-button>
      </div>
    </a-modal>

    <!-- 添加节点弹窗（嵌套在编排弹窗内） -->
    <a-modal
      v-model:open="addNodeModalVisible"
      title="添加节点到流程"
      @ok="handleAddNodeConfirm"
      :width="480"
      destroyOnClose
    >
      <div class="py-2">
        <div class="text-sm text-gray-500 mb-3">选择要添加到流程中的节点：</div>
        <a-select
          v-model:value="addNodeSelectedIds"
          mode="multiple"
          placeholder="选择节点（可多选）"
          style="width: 100%"
          :options="availableNodeOptions"
        />
      </div>
    </a-modal>

    <!-- BPMN 预览弹窗 -->
    <a-modal
      v-model:open="bpmnPreviewVisible"
      title="BPMN XML 预览"
      :footer="null"
      :width="720"
    >
      <pre class="bpmn-preview">{{ bpmnPreviewXml }}</pre>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, ReloadOutlined, CodeOutlined, SendOutlined, SaveOutlined, DeleteOutlined,
  UpOutlined, DownOutlined, EditOutlined, SettingOutlined, SwapOutlined
} from '@ant-design/icons-vue'
import {
  getFlowTemplateList, createFlowTemplate,
  updateFlowTemplate, deleteFlowTemplate,
  getFlowTemplateNodes, saveFlowTemplateNodes,
  previewBpmnXml, deployBpmn,
  type FlowTemplate, type FlowTemplateNodeItem
} from '@/api/system/flowTemplate'
import { AutoComplete, Modal } from 'ant-design-vue'
import { getFlowNodeOrchestratable, type FlowNode } from '@/api/system/flowNode'
import { getUserList } from '@/api/system'
import { migrateDeclarationFlowBatch } from '@/api/business/declaration'
import { useDict } from '@/composables/useDict'

// ============================================================
// 字典
// ============================================================
const { options: processTypeOptions, colorMap: processTypeColorMap, labelMap: processTypeLabelMap } = useDict('process_type')
const { colorMap: formSectionColorMap } = useDict('form_section')

// ============================================================
// 模板列表
// ============================================================
const loadingTemplates = ref(false)
const templateList = ref<FlowTemplate[]>([])
const filterProcessType = ref('declaration')

const tplColumns = [
  { title: '模板名称', key: 'name', dataIndex: 'name', width: 180 },
  { title: '编码', dataIndex: 'code', width: 120 },
  { title: '流程类型', key: 'processType', width: 100, align: 'center' as const },
  { title: '申报类型', key: 'declarationType', width: 90, align: 'center' as const },
  { title: '说明', dataIndex: 'description', ellipsis: true },
  { title: '默认', key: 'isDefault', width: 70, align: 'center' as const },
  { title: '状态', key: 'status', width: 70, align: 'center' as const },
  { title: '节点数', key: 'nodeCount', width: 90, align: 'center' as const },
  { title: '操作', key: 'action', width: 280, align: 'center' as const, fixed: 'right' as const },
]

function getNodeCount(tpl: FlowTemplate): number {
  return tpl.templateNodes?.length ?? tpl.steps?.length ?? 0
}

async function loadTemplates() {
  loadingTemplates.value = true
  try {
    const res = await getFlowTemplateList(filterProcessType.value)
    if (res.data?.code === 200) {
      templateList.value = res.data.data ?? []
    }
  } finally {
    loadingTemplates.value = false
  }
}

// ============================================================
// 新增/编辑模板弹窗
// ============================================================
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)

const modalForm = ref({
  name: '', code: '', description: '',
  processType: 'declaration' as string,
  declarationType: 'EXTERNAL' as string,
  isDefault: 0 as number, status: 1 as number,
})

const modalDefaultChecked = computed({
  get: () => modalForm.value.isDefault === 1,
  set: (val: boolean) => { modalForm.value.isDefault = val ? 1 : 0 }
})

function openAddModal() {
  isEdit.value = false
  editId.value = null
  modalForm.value = { name: '', code: '', description: '', processType: filterProcessType.value, declarationType: 'EXTERNAL', isDefault: 0, status: 1 }
  modalVisible.value = true
}

function openEditModal(tpl: FlowTemplate) {
  isEdit.value = true
  editId.value = tpl.id ?? null
  modalForm.value = {
    name: tpl.name, code: tpl.code,
    description: tpl.description ?? '',
    processType: tpl.processType ?? 'declaration',
    declarationType: tpl.declarationType ?? 'EXTERNAL',
    isDefault: tpl.isDefault, status: tpl.status,
  }
  modalVisible.value = true
}

async function handleModalSubmit() {
  if (!modalForm.value.name.trim()) { message.warning('请填写模板名称'); return }
  if (!modalForm.value.code.trim()) { message.warning('请填写模板编码'); return }

  modalLoading.value = true
  try {
    let res: any
    if (isEdit.value && editId.value) {
      res = await updateFlowTemplate(editId.value, modalForm.value)
    } else {
      res = await createFlowTemplate(modalForm.value as FlowTemplate)
    }
    if (res.data?.code === 200) {
      message.success(isEdit.value ? '模板更新成功' : '模板创建成功')
      modalVisible.value = false
      await loadTemplates()
    } else {
      message.error(res.data?.message ?? '操作失败')
    }
  } finally {
    modalLoading.value = false
  }
}

async function handleDelete(id: number) {
  const res = await deleteFlowTemplate(id)
  if (res.data?.code === 200) {
    message.success('模板删除成功')
    await loadTemplates()
  } else {
    message.error(res.data?.message ?? '删除失败')
  }
}

// ============================================================
// 流程编排弹窗
// ============================================================
const nodeEditorVisible = ref(false)
const editingTemplate = ref<FlowTemplate | null>(null)
const loadingNodes = ref(false)
const savingNodes = ref(false)
const nodeList = ref<FlowTemplateNodeItem[]>([])

const sectionColorMap = formSectionColorMap

const nodeColumns = [
  { title: '排序', key: 'sortOrder', width: 80, align: 'center' as const },
  { title: '节点Key', key: 'nodeKey', width: 150 },
  { title: '节点名称', dataIndex: ['node', 'nodeName'], width: 110 },
  { title: '类型', key: 'nodeType', width: 70, align: 'center' as const },
  { title: 'Status', key: 'targetStatus', width: 70, align: 'center' as const },
  { title: '审批人', key: 'assignee', width: 160 },
  { title: '候选组', key: 'candidateGroups', width: 160 },
  { title: '启用', key: 'enabled', width: 90, align: 'center' as const },
  { title: '操作', key: 'action', width: 70, align: 'center' as const, fixed: 'right' as const },
]

async function openNodeEditor(tpl: FlowTemplate) {
  editingTemplate.value = tpl
  nodeEditorVisible.value = true
  await loadAssigneeOptions()
  await loadNodes(tpl.id!)
}

// 审批人下拉选项（用户 + 常用表达式）
const assigneeOptions = ref<{ label: string; value: string }[]>([])

async function loadAssigneeOptions() {
  const expressionOptions = [
    { label: '提交人 (${starterId})', value: '${starterId}' }
  ]
  try {
    const res = await getUserList({ current: 1, size: 100 })
    if (res.data?.code === 200) {
      const records = res.data.data?.records ?? res.data.data ?? []
      const userOpts = records.map((u: any) => ({
        label: `${u.nickname || u.username} (ID: ${u.id})`,
        value: String(u.id)
      }))
      assigneeOptions.value = [...expressionOptions, ...userOpts]
      return
    }
  } catch { /* ignore */ }
  assigneeOptions.value = expressionOptions
}

async function loadNodes(templateId: number) {
  loadingNodes.value = true
  try {
    const res = await getFlowTemplateNodes(templateId)
    if (res.data?.code === 200) {
      nodeList.value = res.data.data ?? []
    }
  } finally {
    loadingNodes.value = false
  }
}

async function handleSaveNodes() {
  if (!editingTemplate.value?.id) return
  savingNodes.value = true
  try {
    nodeList.value.forEach((n, i) => { n.sortOrder = i + 1 })
    const res = await saveFlowTemplateNodes(editingTemplate.value.id, nodeList.value)
    if (res.data?.code === 200) {
      message.success('节点编排保存成功')
      await loadTemplates()
    } else {
      message.error(res.data?.message ?? '保存失败')
    }
  } finally {
    savingNodes.value = false
  }
}

function moveNode(index: number, direction: number) {
  const target = index + direction
  if (target < 0 || target >= nodeList.value.length) return
  const temp = nodeList.value[index]
  nodeList.value[index] = nodeList.value[target]
  nodeList.value[target] = temp
  nodeList.value = [...nodeList.value]
}

function removeNode(index: number) {
  nodeList.value.splice(index, 1)
}

// ============================================================
// 添加节点弹窗
// ============================================================
const addNodeModalVisible = ref(false)
const addNodeSelectedIds = ref<number[]>([])
const allUserTaskNodes = ref<FlowNode[]>([])

const availableNodeOptions = computed(() => {
  const usedIds = new Set(nodeList.value.map(n => n.nodeId))
  return allUserTaskNodes.value
    .filter(n => !usedIds.has(n.id!))
    .map(n => ({
      value: n.id,
      label: `${n.nodeType === 'serviceTask' ? '[服务]' : '[用户]'} ${n.nodeName} (${n.nodeKey})`
    }))
})

async function openAddNodeModal() {
  addNodeSelectedIds.value = []
  // 根据模板的 processType 获取可编排节点（userTask + serviceTask）
  const processType = editingTemplate.value?.processType || 'declaration'
  const res = await getFlowNodeOrchestratable(processType)
  if (res.data?.code === 200) {
    allUserTaskNodes.value = res.data.data ?? []
  }
  addNodeModalVisible.value = true
}

function handleAddNodeConfirm() {
  if (addNodeSelectedIds.value.length === 0) {
    message.warning('请至少选择一个节点')
    return
  }
  const maxSort = nodeList.value.length
  addNodeSelectedIds.value.forEach((nodeId, i) => {
    const node = allUserTaskNodes.value.find(n => n.id === nodeId)
    nodeList.value.push({
      nodeId,
      enabled: 1,
      sortOrder: maxSort + i + 1,
      node,
    })
  })
  addNodeModalVisible.value = false
}

// ============================================================
// BPMN 预览与部署
// ============================================================
const bpmnPreviewVisible = ref(false)
const bpmnPreviewXml = ref('')
const deploying = ref(false)

async function handlePreviewBpmn() {
  if (!editingTemplate.value?.id) return
  try {
    const res = await previewBpmnXml(editingTemplate.value.id)
    if (res.data?.code === 200) {
      bpmnPreviewXml.value = res.data.data
      bpmnPreviewVisible.value = true
    } else {
      message.error(res.data?.message ?? '生成失败')
    }
  } catch (e: any) {
    message.error('预览失败: ' + (e.message || e))
  }
}

async function handleDeployBpmn() {
  if (!editingTemplate.value?.id) return
  deploying.value = true
  try {
    const res = await deployBpmn(editingTemplate.value.id)
    if (res.data?.code === 200) {
      message.success('BPMN 已生成并部署')
    } else {
      message.error(res.data?.message ?? '部署失败')
    }
  } finally {
    deploying.value = false
  }
}

// ============================================================
// 一键迁移流程
// ============================================================
const migrating = ref(false)

async function handleMigratePreview() {
  migrating.value = true
  try {
    const res = await migrateDeclarationFlowBatch(true)
    if (res.data?.code === 200) {
      const data = res.data.data
      const total = data.totalCandidates ?? 0
      const successCount = data.successCount ?? 0
      const skippedCount = data.skippedCount ?? 0
      const failedCount = data.failedCount ?? 0

      if (total === 0) {
        message.info('没有需要迁移的申报单')
        return
      }

      const detail = [
        `扫描到 ${total} 条申报单`,
        `可迁移: ${successCount} 条`,
        `已跳过: ${skippedCount} 条`,
        `失败: ${failedCount} 条`
      ].join('\n')

      Modal.confirm({
        title: '流程迁移预览',
        content: detail + '\n\n确认执行迁移？',
        okText: '确认迁移',
        okType: 'danger',
        cancelText: '取消',
        onOk: () => handleMigrateExecute()
      })
    } else {
      message.error(res.data?.message ?? '迁移预览失败')
    }
  } catch (e: any) {
    message.error('迁移预览失败: ' + (e.message || e))
  } finally {
    migrating.value = false
  }
}

async function handleMigrateExecute() {
  migrating.value = true
  try {
    const res = await migrateDeclarationFlowBatch(false)
    if (res.data?.code === 200) {
      const data = res.data.data
      message.success(`迁移完成: 成功 ${data.successCount} 条, 跳过 ${data.skippedCount} 条, 失败 ${data.failedCount} 条`)
    } else {
      message.error(res.data?.message ?? '迁移执行失败')
    }
  } catch (e: any) {
    message.error('迁移执行失败: ' + (e.message || e))
  } finally {
    migrating.value = false
  }
}

// ============================================================
// 初始化
// ============================================================
onMounted(loadTemplates)
</script>

<style scoped>
.cell-muted {
  color: rgba(0, 0, 0, 0.25);
}

/* 编排弹窗摘要区 */
.editor-summary {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  padding: 12px;
  background: #eff6ff;
  border-radius: 8px;
  font-size: 13px;
  align-items: center;
  justify-content: space-between;
}
.editor-summary-info {
  display: flex;
  gap: 20px;
}
.summary-label {
  color: rgba(0, 0, 0, 0.45);
}
.summary-value {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
}
.summary-desc {
  color: rgba(0, 0, 0, 0.65);
}

/* 弹窗表单提示 */
.tpl-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.25);
  margin-top: 4px;
}
.tpl-switch-hint {
  margin-left: 8px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.25);
}

/* 部署按钮 - 蓝色描边强调 */
.deploy-btn {
  color: #1677ff;
  border-color: #1677ff;
  background: transparent;
}
.deploy-btn:hover {
  color: #fff !important;
  background: #1677ff !important;
  border-color: #1677ff !important;
}

.bpmn-preview {
  max-height: 500px;
  overflow: auto;
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 排序列 */
.sort-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}
.sort-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #e6f4ff;
  color: #1677ff;
  font-size: 11px;
  font-weight: 600;
}
.sort-btns {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.sort-btn {
  width: 20px !important;
  height: 16px !important;
  font-size: 10px !important;
  padding: 0 !important;
  color: #8c8c8c !important;
}
.sort-btn:not(:disabled):hover {
  color: #1677ff !important;
  background: #e6f4ff !important;
}

/* 节点Key标签 */
.node-key-tag {
  display: inline-block;
  font-size: 12px;
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  padding: 1px 6px;
  border-radius: 4px;
  color: #595959;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

/* Status标签 */
.status-tag {
  font-weight: 600;
  min-width: 28px;
  text-align: center;
}

/* 移除按钮 */
.remove-btn {
  font-size: 12px !important;
  padding: 0 4px !important;
}

/* 底部操作栏 */
.editor-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
.footer-btn {
  min-width: 80px;
  height: 36px;
  border-radius: 6px;
}
.footer-btn-primary {
  min-width: 110px;
}
</style>
