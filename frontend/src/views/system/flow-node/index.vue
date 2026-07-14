<template>
  <div class="flow-node-management">
    <!-- 搜索区域 -->
    <a-card class="ui-card search-card" :bordered="false">
      <a-form layout="inline">
        <a-form-item label="所属流程">
          <a-select v-model:value="filterProcessType" :options="processTypeOptions" style="width: 160px" placeholder="请选择流程类型" class="ui-select" @change="loadNodes" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="loadNodes" class="ui-btn-primary">
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
        <a-button type="primary" @click="openAddModal" v-permission="['system:flow-node:add']" class="ui-btn-cta">
          <template #icon><PlusOutlined /></template>
          新增节点
        </a-button>
        <a-button @click="loadNodes" class="ui-btn-secondary">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="ui-card" :bordered="false">
      <a-table
        :dataSource="nodeList"
        :columns="columns"
        :loading="loading"
        :pagination="false"
        rowKey="id"
        size="small"
        class="ui-table"
        :scroll="{ x: 1100 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'nodeKey'">
            <code class="node-key-code">{{ record.nodeKey }}</code>
          </template>

          <template v-else-if="column.key === 'nodeType'">
            <a-tag :color="nodeTypeColorMap[record.nodeType] || 'default'">
              {{ nodeTypeLabelMap[record.nodeType] || record.nodeType }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'targetStatus'">
            <a-tag v-if="record.targetStatus != null" color="geekblue">{{ record.targetStatus }}</a-tag>
            <span v-else class="cell-muted">-</span>
          </template>

          <template v-else-if="column.key === 'formSection'">
            <a-tag v-if="record.formSection" :color="formSectionColorMap[record.formSection] || 'default'">
              {{ record.formSection }}
            </a-tag>
            <span v-else class="cell-muted">-</span>
          </template>

          <template v-else-if="column.key === 'assignee'">
            <code v-if="record.assignee" style="font-size: 12px">{{ record.assignee }}</code>
            <span v-else class="cell-muted">-</span>
          </template>

          <template v-else-if="column.key === 'candidateGroups'">
            <a-tag v-if="record.candidateGroups" color="purple">{{ record.candidateGroups }}</a-tag>
            <span v-else class="cell-muted">-</span>
          </template>

          <template v-else-if="column.key === 'isSystem'">
            <a-tag :color="record.isSystem === 1 ? 'warning' : 'success'">
              {{ record.isSystem === 1 ? '系统内置' : '自定义' }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space :size="2">
              <a-button
                type="link" size="small"
                @click="openEditModal(record as FlowNode)"
                v-permission="['system:flow-node:update']"
                style="padding: 0 4px"
              >
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-popconfirm
                v-if="record.isSystem !== 1"
                title="确定删除该节点？"
                @confirm="handleDelete(record.id)"
                okText="确定" cancelText="取消"
              >
                <a-button
                  type="link" size="small" danger
                  v-permission="['system:flow-node:delete']"
                  style="padding: 0 4px"
                >
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </a-popconfirm>
              <a-tooltip v-else title="系统内置节点不可删除">
                <a-button type="link" size="small" disabled>
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </a-tooltip>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑节点' : '新增节点'"
      @ok="handleModalSubmit"
      :confirmLoading="modalLoading"
      :width="560"
      destroyOnClose
    >
      <a-form
        :model="modalForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        class="mt-4"
      >
        <a-form-item label="所属流程" required>
          <a-select v-model:value="modalForm.processType" placeholder="选择流程类型">
            <a-select-option v-for="opt in processTypeOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="节点Key" required>
          <a-input
            v-model:value="modalForm.nodeKey"
            placeholder="BPMN任务Key，如 deptAudit"
            :maxlength="50"
            :disabled="isEdit && editIsSystem === 1"
          />
          <div class="text-xs text-gray-400 mt-1">对应 Flowable 的 taskDefinitionKey</div>
        </a-form-item>
        <a-form-item label="节点名称" required>
          <a-input v-model:value="modalForm.nodeName" placeholder="如：初审、资料提交" :maxlength="100" />
        </a-form-item>
        <a-form-item label="节点类型">
          <a-radio-group v-model:value="modalForm.nodeType" :disabled="isEdit && editIsSystem === 1">
            <a-radio v-for="opt in nodeTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="办理人" v-if="modalForm.nodeType === 'userTask'">
          <a-input v-model:value="modalForm.assignee" placeholder="如 ${starterId}" :maxlength="100" />
        </a-form-item>
        <a-form-item label="候选组" v-if="modalForm.nodeType === 'userTask'">
          <a-input v-model:value="modalForm.candidateGroups" placeholder="如 MATERIAL_AUDITOR" :maxlength="255" />
          <div class="text-xs text-gray-400 mt-1">多个组用逗号分隔</div>
        </a-form-item>
        <a-form-item label="委托表达式" v-if="modalForm.nodeType === 'serviceTask'">
          <a-input v-model:value="modalForm.delegateExpression" placeholder="如 ${declarationServiceTask}" :maxlength="100" />
          <div class="text-xs text-gray-400 mt-1">Spring Bean 表达式，用于 Flowable serviceTask 委托</div>
        </a-form-item>
        <a-form-item label="目标状态">
          <a-input-number
            v-model:value="modalForm.targetStatus"
            :min="0" :max="99"
            placeholder="到达此节点时的 status 值"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="表单区块">
          <a-select v-model:value="modalForm.formSection" placeholder="对应前端表单区块" allowClear>
            <a-select-option v-for="opt in formSectionOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="节点说明">
          <a-textarea v-model:value="modalForm.description" placeholder="可选" :rows="2" :maxlength="500" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import {
  getFlowNodeList, createFlowNode, updateFlowNode, deleteFlowNode,
  type FlowNode
} from '@/api/system/flowNode'
import { useDict } from '@/composables/useDict'

// 字典
const { options: processTypeOptions } = useDict('process_type')
const { options: nodeTypeOptions, colorMap: nodeTypeColorMap, labelMap: nodeTypeLabelMap } = useDict('node_type')
const { options: formSectionOptions, colorMap: formSectionColorMap } = useDict('form_section')

const loading = ref(false)
const nodeList = ref<FlowNode[]>([])
const filterProcessType = ref('declaration')

const columns = [
  { title: '节点Key', key: 'nodeKey', dataIndex: 'nodeKey', width: 160 },
  { title: '节点名称', dataIndex: 'nodeName', width: 120 },
  { title: '类型', key: 'nodeType', width: 90, align: 'center' as const },
  { title: '目标Status', key: 'targetStatus', width: 90, align: 'center' as const },
  { title: '表单区块', key: 'formSection', width: 110, align: 'center' as const },
  { title: '办理人', key: 'assignee', width: 120 },
  { title: '候选组', key: 'candidateGroups', width: 140 },
  { title: '来源', key: 'isSystem', width: 80, align: 'center' as const },
  { title: '操作', key: 'action', width: 160, align: 'center' as const, fixed: 'right' as const },
]

async function loadNodes() {
  loading.value = true
  try {
    const res = await getFlowNodeList(filterProcessType.value)
    if (res.data?.code === 200) {
      nodeList.value = res.data.data ?? []
    }
  } finally {
    loading.value = false
  }
}

// 弹窗
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const editIsSystem = ref(0)

const modalForm = ref({
  nodeKey: '',
  nodeName: '',
  nodeType: 'userTask',
  assignee: '' as string | undefined,
  candidateGroups: '' as string | undefined,
  targetStatus: undefined as number | undefined,
  formSection: undefined as string | undefined,
  processType: 'declaration' as string,
  delegateExpression: '' as string | undefined,
  description: '' as string | undefined,
})

function openAddModal() {
  isEdit.value = false
  editId.value = null
  editIsSystem.value = 0
  modalForm.value = {
    nodeKey: '', nodeName: '', nodeType: 'userTask',
    assignee: undefined, candidateGroups: undefined,
    targetStatus: undefined, formSection: undefined,
    processType: filterProcessType.value,
    delegateExpression: undefined,
    description: undefined,
  }
  modalVisible.value = true
}

function openEditModal(record: FlowNode) {
  isEdit.value = true
  editId.value = record.id ?? null
  editIsSystem.value = record.isSystem ?? 0
  modalForm.value = {
    nodeKey: record.nodeKey,
    nodeName: record.nodeName,
    nodeType: record.nodeType,
    assignee: record.assignee,
    candidateGroups: record.candidateGroups,
    targetStatus: record.targetStatus,
    formSection: record.formSection,
    processType: record.processType || 'declaration',
    delegateExpression: record.delegateExpression,
    description: record.description,
  }
  modalVisible.value = true
}

async function handleModalSubmit() {
  if (!modalForm.value.nodeKey.trim()) { message.warning('请填写节点Key'); return }
  if (!modalForm.value.nodeName.trim()) { message.warning('请填写节点名称'); return }

  modalLoading.value = true
  try {
    if (isEdit.value && editId.value) {
      const res = await updateFlowNode(editId.value, modalForm.value)
      if (res.data?.code === 200) {
        message.success('节点更新成功')
        modalVisible.value = false
        await loadNodes()
      } else {
        message.error(res.data?.message ?? '更新失败')
      }
    } else {
      const res = await createFlowNode(modalForm.value as FlowNode)
      if (res.data?.code === 200) {
        message.success('节点创建成功')
        modalVisible.value = false
        await loadNodes()
      } else {
        message.error(res.data?.message ?? '创建失败')
      }
    }
  } finally {
    modalLoading.value = false
  }
}

async function handleDelete(id: number) {
  const res = await deleteFlowNode(id)
  if (res.data?.code === 200) {
    message.success('节点删除成功')
    await loadNodes()
  } else {
    message.error(res.data?.message ?? '删除失败')
  }
}

onMounted(loadNodes)
</script>

<style scoped>
.flow-node-management {
  /* content wrapper already provides bg + padding */
}
.node-key-code {
  font-size: 12px;
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  color: rgba(0, 0, 0, 0.65);
}
.cell-muted {
  color: rgba(0, 0, 0, 0.25);
}
</style>
