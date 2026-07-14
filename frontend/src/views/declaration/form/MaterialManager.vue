<template>
  <div class="material-manager">
    <a-spin :spinning="loading">
      <!-- 动态渲染每个环节 -->
      <template v-for="section in visibleSections" :key="section.itemValue">
        <a-card
          :title="section.config.sectionTitle || section.label"
          size="small"
          class="section-card"
          :id="'section-' + section.itemValue"
        >
          <template #extra>
            <!-- submit 模式：提交按钮 -->
            <a-button
              v-if="mode === 'submit' && canOperateSection(section) && section.config.submitKey"
              type="primary"
              size="small"
              :loading="submittingKey === section.config.submitKey"
              @click="handleSubmit(section)"
            >
              <template #icon><UploadOutlined /></template>
              {{ section.config.btnText || '提交审核' }}
            </a-button>
            <!-- audit 模式：通过/驳回按钮 -->
            <a-space v-if="mode === 'audit' && canOperateSection(section) && section.config.auditTaskKey">
              <a-button
                type="primary"
                size="small"
                :loading="submittingKey === section.config.auditTaskKey"
                @click="handleAudit(section, true)"
              >
                <template #icon><CheckCircleOutlined /></template>
                审核通过
              </a-button>
              <a-button
                danger
                size="small"
                :loading="submittingKey === section.config.auditTaskKey"
                @click="handleAudit(section, false)"
              >
                <template #icon><CloseCircleOutlined /></template>
                审核驳回
              </a-button>
            </a-space>
          </template>

          <!-- 进度卡片 -->
          <div class="progress-card">
            <div class="progress-left">
              <div class="progress-title">
                <FileDoneOutlined class="progress-icon" />
                <span>{{ section.config.cardTitle || section.label + '进度' }}</span>
              </div>
              <div class="progress-desc">
                共 <b>{{ getSectionStats(section).total }}</b> 项，
                必填 <b class="text-red-500">{{ getSectionStats(section).required }}</b> 项，
                已上传 <b :class="getSectionStats(section).uploaded === getSectionStats(section).required ? 'text-green-500' : 'text-blue-500'">{{ getSectionStats(section).uploaded }}</b> 项
              </div>
            </div>
            <div class="progress-right">
              <a-progress
                type="circle"
                :percent="getSectionStats(section).percent"
                :width="60"
                :stroke-color="getSectionStats(section).percent === 100 ? '#52c41a' : (section.config.btnColor || '#1677ff')"
              />
            </div>
          </div>

          <!-- 工具栏：新增自定义资料项 -->
          <div class="toolbar" v-if="isEditableSection(section) && checkPermission(['business:declaration:material:customize'])">
            <a-space>
              <a-button type="primary" size="small" @click="openAddModal">
                <template #icon><PlusOutlined /></template> 新增自定义资料项
              </a-button>
            </a-space>
          </div>

          <!-- 资料列表 -->
          <a-table
            :dataSource="getSectionItems(section)"
            :columns="columns"
            :pagination="false"
            rowKey="id"
            size="middle"
            class="material-table"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <div class="name-cell">
                  <div class="name-main">
                    <span class="name-text">{{ record.name }}</span>
                    <a-tag v-if="(record as MaterialItem).required === 1" color="red">必填</a-tag>
                    <a-tag v-else>选填</a-tag>
                    <a-tag v-if="(record as MaterialItem).templateId == null" color="blue">自定义</a-tag>
                    <!-- 上传按钮 + 下拉菜单 -->
                    <div class="name-upload-actions" v-if="isEditableSection(section)">
                      <a-upload :show-upload-list="false" :before-upload="(f: File) => handleUpload(f, record as MaterialItem)">
                        <a-button type="primary" size="small" class="material-upload-btn">
                          <template #icon><UploadOutlined v-if="(record as MaterialItem).status !== 1" /><PlusOutlined v-else /></template>
                          {{ (record as MaterialItem).status === 1 ? '追加' : '上传' }}
                        </a-button>
                      </a-upload>
                      <a-dropdown v-if="checkPermission(['business:declaration:material:customize'])" :trigger="['click']">
                        <a-button size="small" type="text"><MoreOutlined /></a-button>
                        <template #overlay>
                          <a-menu>
                            <a-menu-item @click="openEditModal(record as MaterialItem)"><EditOutlined /> 编辑名称/说明</a-menu-item>
                            <a-menu-item v-if="(record as MaterialItem).status === 1" @click="handleClearFile(record as MaterialItem)"><DeleteOutlined /> <span class="text-red-500">清除附件</span></a-menu-item>
                            <a-menu-item v-if="(record as MaterialItem).templateId == null" @click="handleDeleteRow(record as MaterialItem)"><CloseOutlined /> <span class="text-red-500">删除资料项</span></a-menu-item>
                          </a-menu>
                        </template>
                      </a-dropdown>
                    </div>
                  </div>
                </div>
                <div v-if="record.remark" class="name-remark">{{ record.remark }}</div>
                <!-- 附件列表 -->
                <template v-if="(record as MaterialItem).attachments?.length">
                  <!-- 发票类附件 -->
                  <template v-if="isInvoiceMode(record as MaterialItem)">
                    <div v-for="att in (record as MaterialItem).attachments" :key="att.id" class="att-invoice-card">
                      <div class="att-row-main">
                        <div class="att-file-name">
                          <FileTextOutlined class="file-icon-sm" />
                          <a @click.prevent="$emit('previewFile', att.fileUrl)" class="file-name-sm" style="cursor:pointer" :title="att.fileName">{{ displayAttFileName(att) }}</a>
                        </div>
                        <div class="att-divider-v"></div>
                        <template v-if="isEditableSection(section)">
                          <div class="att-field-inline"><span class="att-field-label">金额</span>
                            <a-input-number :value="att.amount ?? undefined" @update:value="(v: any) => saveAttachmentField(record as MaterialItem, att, 'amount', v)" placeholder="-" size="small" :precision="2" style="width:120px" />
                          </div>
                          <div class="att-field-inline"><span class="att-field-label">发票号</span>
                            <a-input :value="att.invoiceNo ?? undefined" @update:value="(v: any) => saveAttachmentField(record as MaterialItem, att, 'invoiceNo', v)" placeholder="-" size="small" style="width:180px" :maxlength="100" />
                          </div>
                          <div class="att-field-inline"><span class="att-field-label">日期</span>
                            <a-date-picker :value="att.invoiceDate || undefined" value-format="YYYY-MM-DD" @update:value="(v: any) => saveAttachmentField(record as MaterialItem, att, 'invoiceDate', v)" placeholder="-" size="small" style="width:140px" />
                          </div>
                        </template>
                        <template v-else>
                          <span class="att-val-tag">¥{{ att.amount ?? '-' }}</span>
                          <span class="att-val-tag">{{ att.invoiceNo || '-' }}</span>
                          <span class="att-val-tag">{{ att.invoiceDate || '-' }}</span>
                        </template>
                        <a-popconfirm v-if="isEditableSection(section)" title="确定删除？" @confirm="handleDeleteAttachment(record as MaterialItem, att)">
                          <DeleteOutlined class="file-delete-btn" />
                        </a-popconfirm>
                      </div>
                      <div class="att-row-meta">
                        <span><UserOutlined /> 创建 {{ att.createByName || '-' }}</span>
                        <span class="att-meta-dot"></span>
                        <span><EditOutlined /> 更新 {{ att.updateByName || '-' }}</span>
                        <span class="att-meta-dot"></span>
                        <span><ClockCircleOutlined /> {{ att.uploadTime ? att.uploadTime.substring(0, 16) : '-' }}</span>
                      </div>
                    </div>
                  </template>
                  <!-- 普通附件 -->
                  <template v-else>
                    <div v-for="att in (record as MaterialItem).attachments" :key="att.id" class="att-invoice-card">
                      <div class="att-row-main">
                        <div class="att-file-name">
                          <FileTextOutlined class="file-icon-sm" />
                          <a @click.prevent="$emit('previewFile', att.fileUrl)" class="file-name-sm" style="cursor:pointer" :title="att.fileName">{{ displayAttFileName(att) }}</a>
                        </div>
                        <a-popconfirm v-if="isEditableSection(section)" title="确定删除？" @confirm="handleDeleteAttachment(record as MaterialItem, att)">
                          <DeleteOutlined class="file-delete-btn" />
                        </a-popconfirm>
                      </div>
                      <div class="att-row-meta">
                        <span><UserOutlined /> 创建 {{ att.createByName || '-' }}</span>
                        <span class="att-meta-dot"></span>
                        <span><EditOutlined /> 更新 {{ att.updateByName || '-' }}</span>
                        <span class="att-meta-dot"></span>
                        <span><ClockCircleOutlined /> {{ att.uploadTime ? att.uploadTime.substring(0, 16) : '-' }}</span>
                      </div>
                    </div>
                    <div v-if="((record as MaterialItem).attachments?.length ?? 0) > 1" class="file-count-hint">共 {{ (record as MaterialItem).attachments!.length }} 份文件</div>
                  </template>
                </template>
                <template v-else-if="record.status === 1 && record.fileUrl">
                  <div class="file-item-row">
                    <FileTextOutlined class="file-icon-sm" />
                    <a
                      @click.prevent="$emit('previewFile', record.fileUrl)"
                      class="file-name-sm"
                      style="cursor:pointer"
                    >{{ record.fileName || '查看附件' }}</a>
                  </div>
                </template>
                <template v-else>
                  <div class="file-cell file-empty">
                    <CloudUploadOutlined class="file-icon" />
                    <span>尚未上传</span>
                  </div>
                </template>
              </template>
            </template>
          </a-table>
        </a-card>
      </template>

      <!-- 空状态 -->
      <a-empty v-if="!loading && visibleSections.length === 0" description="暂无资料项" />
    </a-spin>

    <!-- 新增/编辑自定义资料项弹窗 -->
    <a-modal v-model:open="modalVisible" :title="editingRecord ? '编辑资料项' : '新增自定义资料项'" @ok="handleSaveModal" :width="560">
      <a-form layout="vertical" style="margin-top:16px">
        <a-form-item label="资料名称">
          <a-input v-model:value="modalForm.name" placeholder="请输入资料名称" :maxlength="200" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="是否必填">
              <a-radio-group v-model:value="modalForm.required" button-style="solid">
                <a-radio :value="1">必填</a-radio><a-radio :value="0">选填</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序">
              <a-input-number v-model:value="modalForm.sort" :min="0" :max="9999" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="说明">
          <a-textarea v-model:value="modalForm.remark" :rows="3" :maxlength="500" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { message, Modal, Textarea } from 'ant-design-vue'
import { h } from 'vue'
import {
  UploadOutlined, FileDoneOutlined, FileTextOutlined, CloudUploadOutlined,
  CheckCircleOutlined, CloseCircleOutlined, PlusOutlined,
  MoreOutlined, EditOutlined, DeleteOutlined, CloseOutlined,
  UserOutlined, ClockCircleOutlined,
} from '@ant-design/icons-vue'
import {
  getMaterialItems, submitStage, auditStage,
  uploadMaterialFile, ensureMaterialItem,
  addMaterialItem, updateMaterialItem, deleteMaterialItem,
  clearMaterialFile, deleteMaterialAttachment, updateMaterialAttachment,
  parseInvoicePdf,
  type MaterialItem,
} from '@/api/business/materialItem'
import { checkPermission } from '@/directives/permission'
import { getEnabledDictItems } from '@/api/system/dict'

// ==================== Props / Emits ====================
const props = withDefaults(defineProps<{
  formId: number | string
  /** 组件模式：submit=提交模式 / audit=审核模式 */
  mode?: 'submit' | 'audit'
  /** 当前申报单状态（由父组件传入，用于流程进度过滤） */
  formStatus?: number | null
  /** nodeKey → targetStatus 动态映射（从流程配置中获取） */
  stepStatusMap?: Map<string, number>
  /** formSection → sortOrder（从流程节点推导的环节显示顺序） */
  sectionOrderMap?: Map<string, number>
  /** 环节渲染范围：all=全部, pre=渲染到 stopBefore 之前, post=从 stopBefore 开始 */
  sectionRange?: 'all' | 'pre' | 'post'
  /** 分段边界环节（配合 sectionRange 使用） */
  stopBefore?: string
  /** 是否可操作（由父组件根据权限和状态控制） */
  canOperate?: (section: { submitKey?: string; auditTaskKey?: string }) => boolean
}>(), {
  mode: 'submit',
  formStatus: null,
  sectionRange: 'all',
  stopBefore: ''
})

const emit = defineEmits<{
  (e: 'submitted', submitKey: string): void
  (e: 'audited', auditTaskKey: string, approved: boolean): void
  (e: 'previewFile', fileUrl: string): void
}>()

// ==================== 类型 ====================
interface SectionConfig {
  sectionTitle?: string
  cardTitle?: string
  submitKey?: string
  auditTaskKey?: string
  btnText?: string
  btnColor?: string
  templateStage?: string
  auditBt?: string
  attachmentMode?: string
  requireAnyAttachment?: boolean
  checkSchema?: boolean
  checkApprovedRemittance?: boolean
}

interface SectionInfo {
  itemValue: string
  label: string
  sortOrder: number
  config: SectionConfig
}

// ==================== 状态 ====================
const loading = ref(false)
const submittingKey = ref<string | null>(null)
const allItems = ref<MaterialItem[]>([])
const sections = ref<SectionInfo[]>([])

// ---------- 发票 PDF 解析状态 ----------
const materialRowKey = (record: MaterialItem) => (record.id ?? `tpl-${record.templateId}`) as any
const materialPdfMessages = reactive<Record<string, { type: 'success' | 'warn' | 'info'; text: string }>>({})
const buildFileSignature = (file: File): string => `${file.name}|${file.size}|${file.lastModified}`

interface ParsedCache {
  amount: number | null
  invoiceNo: string | null
  invoiceDate: string | null
  message: { type: 'success' | 'warn' | 'info'; text: string } | null
}
const parsedFileSignatures = new Map<string, ParsedCache>()

const isInvoiceMaterial = (item: MaterialItem): boolean => item.invoiceMode === 1

// ==================== 表格列 ====================
const columns = [
  { title: '资料项', key: 'name', dataIndex: 'name' }
]

// ==================== 加载数据 ====================
const loadData = async () => {
  if (!props.formId) return
  loading.value = true
  try {
    // 并行加载字典和资料项
    const [dictRes, itemsRes] = await Promise.all([
      getEnabledDictItems('form_section'),
      getMaterialItems(props.formId)
    ])

    // 解析字典：根据模式筛选有对应 key 的环节
    const dictItems = dictRes.data?.data || []
    const parsedSections: SectionInfo[] = []
    for (const item of dictItems) {
      if (!item.remark) continue
      try {
        const config: SectionConfig = JSON.parse(item.remark)
        // submit 模式需要 submitKey，audit 模式需要 auditTaskKey
        if (props.mode === 'submit' && (!config.submitKey || !config.templateStage)) continue
        if (props.mode === 'audit' && !config.auditTaskKey) continue
        parsedSections.push({
          itemValue: item.itemValue,
          label: item.itemLabel || item.itemValue,
          sortOrder: item.sortOrder ?? 99,
          config
        })
      } catch {
        // remark 非 JSON，跳过
      }
    }
    // 排序：优先用流程节点的 sortOrder，其次用字典的 sortOrder
    const orderMap = props.sectionOrderMap
    parsedSections.sort((a, b) => {
      const oa = orderMap?.get(a.itemValue) ?? a.sortOrder
      const ob = orderMap?.get(b.itemValue) ?? b.sortOrder
      return oa - ob
    })
    sections.value = parsedSections

    // 资料项
    allItems.value = itemsRes.data?.data || []
  } catch (e: any) {
    message.error('加载资料项失败: ' + (e.message || ''))
  } finally {
    loading.value = false
  }
}

// ==================== 按环节分组 ====================
const getSectionItems = (section: SectionInfo): MaterialItem[] => {
  return allItems.value
    .filter(item => {
      const stage = item.stage || 'MATERIAL_SUBMIT'
      return stage === section.config.templateStage
    })
    .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
}

/** 只展示流程进度内且有资料项的环节（动态过滤，不硬编码状态值） */
const visibleSections = computed(() => {
  const s = props.formStatus
  const map = props.stepStatusMap

  // 1. 基础过滤：流程进度内有资料项的环节
  let result: SectionInfo[]
  if (s == null || !map || map.size === 0) {
    result = sections.value.filter(s => getSectionItems(s).length > 0)
  } else {
    let maxVisibleSort = -1
    for (const sec of sections.value) {
      const keys = [sec.config.submitKey, sec.config.auditTaskKey].filter(Boolean) as string[]
      for (const key of keys) {
        const ts = map.get(key)
        if (ts != null && ts <= s) {
          maxVisibleSort = Math.max(maxVisibleSort, sec.sortOrder)
        }
      }
    }
    result = sections.value.filter(s => s.sortOrder <= maxVisibleSort && getSectionItems(s).length > 0)
  }

  // 2. 分段过滤：根据 sectionRange 和 stopBefore 控制渲染范围
  if (props.sectionRange !== 'all' && props.stopBefore) {
    const orderMap = props.sectionOrderMap
    const boundary = orderMap?.get(props.stopBefore) ?? Infinity
    if (props.sectionRange === 'pre') {
      result = result.filter(sec => {
        const order = orderMap?.get(sec.itemValue) ?? sec.sortOrder
        return order < boundary
      })
    } else if (props.sectionRange === 'post') {
      result = result.filter(sec => {
        const order = orderMap?.get(sec.itemValue) ?? sec.sortOrder
        return order >= boundary
      })
    }
  }

  return result
})

// ==================== 进度统计 ====================
const getSectionStats = (section: SectionInfo) => {
  const items = getSectionItems(section)
  const total = items.length
  const required = items.filter(i => i.required === 1).length
  const uploaded = items.filter(i => i.required === 1 && i.status === 1).length
  const percent = required > 0 ? Math.round((uploaded / required) * 100) : (total > 0 ? 100 : 0)
  return { total, required, uploaded, percent }
}

// ==================== 可操作性判断 ====================
/** 控制提交/审核按钮显示 */
const canOperateSection = (section: SectionInfo): boolean => {
  if (props.canOperate) return props.canOperate(section.config)
  return true
}
/** 控制编辑操作（上传/删除/修改）：仅 submit 模式且可操作时为 true */
const isEditableSection = (section: SectionInfo): boolean => {
  return props.mode === 'submit' && canOperateSection(section)
}

// ==================== 提交（submit 模式） ====================
const handleSubmit = (section: SectionInfo) => {
  const submitKey = section.config.submitKey!
  const items = getSectionItems(section)

  // 前端校验：必填项是否已上传
  const missing = items.filter((i: MaterialItem) => i.required === 1 && i.status !== 1)
  if (missing.length > 0) {
    message.warning(`还有 ${missing.length} 项必填资料未上传：${missing.map((m: MaterialItem) => m.name).join('、')}`)
    return
  }

  Modal.confirm({
    title: `确认${section.config.btnText || '提交'}？`,
    content: '提交后将进入审核流程，无法修改。',
    okText: '确认提交',
    onOk: async () => {
      try {
        submittingKey.value = submitKey
        const res = await submitStage(props.formId, submitKey)
        if (res.data?.code === 200) {
          message.success(`${section.label}提交成功，等待审核`)
          emit('submitted', submitKey)
          await loadData()
        } else {
          message.error(res.data?.message || '提交失败')
        }
      } catch (e: any) {
        message.error(e?.message || '提交失败')
      } finally {
        submittingKey.value = null
      }
    }
  })
}

// ==================== 审核（audit 模式） ====================
const handleAudit = (section: SectionInfo, approved: boolean) => {
  const auditTaskKey = section.config.auditTaskKey!
  let auditRemark = ''

  const title = approved
    ? `确认通过${section.label}审核？`
    : `确认驳回${section.label}？`
  const hint = approved
    ? '通过后将进入下一阶段。'
    : '驳回后申报人将重新提交。请填写驳回原因。'

  Modal.confirm({
    title,
    content: () => h('div', [
      h('div', { style: `margin-bottom:8px;color:${approved ? '#666' : '#d46b08'};` }, hint),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: approved ? '审核意见（可选）' : '驳回原因（必填）',
        'onUpdate:value': (v: string) => { auditRemark = v }
      })
    ]),
    okText: approved ? '确认通过' : '确认驳回',
    okButtonProps: approved ? {} : { danger: true },
    onOk: async () => {
      if (!approved && !auditRemark.trim()) {
        message.warning('驳回请填写审核意见')
        return Promise.reject()
      }
      try {
        submittingKey.value = auditTaskKey
        const res = await auditStage({
          formId: props.formId,
          stage: auditTaskKey,
          result: approved ? 1 : 2,
          remark: auditRemark
        })
        if (res.data?.code === 200) {
          message.success(approved ? `${section.label}审核已通过` : `${section.label}已驳回`)
          emit('audited', auditTaskKey, approved)
          await loadData()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e: any) {
        message.error(e?.message || '操作失败')
      } finally {
        submittingKey.value = null
      }
    }
  })
}

// ==================== 工具函数 ====================
const isInvoiceMode = (item: MaterialItem): boolean => item.invoiceMode === 1
const displayAttFileName = (att: any): string => att.fileName || '查看附件'

// ==================== 上传附件 ====================
/** 将虚拟项（id=null）升格为真实记录 */
const resolveItemId = async (record: MaterialItem): Promise<number | string | null> => {
  if (record.id) return record.id
  if (!record.templateId || !props.formId) {
    message.error('无法定位资料项模板')
    return null
  }
  try {
    const res = await ensureMaterialItem(props.formId, record.templateId)
    if (res.data?.code === 200 && res.data.data?.id) {
      record.id = res.data.data.id
      return record.id as number
    }
    message.error(res.data?.message || '创建资料项失败')
    return null
  } catch (e) {
    message.error('创建资料项失败')
    return null
  }
}

/**
 * 尝试解析发票 PDF，自动回填金额/发票号/开票日期到附件结构化字段
 */
const tryParseInvoicePdf = async (file: File, record: MaterialItem) => {
  if (!isInvoiceMaterial(record)) return
  if (file.type !== 'application/pdf') {
    const key = materialRowKey(record)
    materialPdfMessages[key] = { type: 'info', text: '图片类发票暂不支持自动识别，请手动填写' }
    return
  }
  const signature = buildFileSignature(file)
  let parsedAmt: number | null = null
  let parsedInvoiceNo: string | null = null
  let parsedInvoiceDate: string | null = null
  let parsedMsg: { type: 'success' | 'warn' | 'info'; text: string } | null = null

  const cached = parsedFileSignatures.get(signature)
  if (cached) {
    parsedAmt = cached.amount
    parsedInvoiceNo = cached.invoiceNo
    parsedInvoiceDate = cached.invoiceDate
    parsedMsg = cached.message
  } else {
    try {
      const res: any = await parseInvoicePdf(file)
      const data = res?.data?.data
      if (data?.success && data.amount != null) {
        parsedAmt = Number(data.amount)
        parsedMsg = { type: 'success', text: `PDF 识别金额：¥${parsedAmt.toFixed(2)}` }
      } else {
        parsedMsg = { type: 'warn', text: data?.errorMsg || 'PDF 金额识别失败，请手动填写' }
      }
      if (data?.invoiceNo) parsedInvoiceNo = data.invoiceNo
      if (data?.invoiceDate) parsedInvoiceDate = data.invoiceDate
    } catch {
      parsedMsg = { type: 'warn', text: 'PDF 解析请求失败，请手动核对' }
    }
    parsedFileSignatures.set(signature, { amount: parsedAmt, invoiceNo: parsedInvoiceNo, invoiceDate: parsedInvoiceDate, message: parsedMsg })
  }

  // 自动回填发票号和开票日期
  if (parsedInvoiceNo || parsedInvoiceDate) {
    const currentRecord = allItems.value.find((i) => materialRowKey(i) === materialRowKey(record))
    if (currentRecord?.attachments?.length) {
      const latestAtt = currentRecord.attachments[0]
      const patch: Partial<any> = {}
      if (parsedInvoiceNo && !latestAtt.invoiceNo) {
        latestAtt.invoiceNo = parsedInvoiceNo
        patch.invoiceNo = parsedInvoiceNo
      }
      if (parsedInvoiceDate && !latestAtt.invoiceDate) {
        latestAtt.invoiceDate = parsedInvoiceDate
        patch.invoiceDate = parsedInvoiceDate
      }
      if (Object.keys(patch).length > 0 && currentRecord.id && latestAtt.id) {
        updateMaterialAttachment(currentRecord.id!, latestAtt.id, patch).catch(() => {})
      }
    }
  }

  // 自动回填金额
  if (parsedAmt == null || parsedAmt <= 0) return
  const currentRecord2 = allItems.value.find((i) => materialRowKey(i) === materialRowKey(record))
  if (!currentRecord2 || !currentRecord2.attachments?.length) return
  const latestAtt2 = currentRecord2.attachments[0]
  if (!latestAtt2) return

  const currentAttAmt = Number(latestAtt2.amount ?? 0)
  if (currentAttAmt <= 0) {
    latestAtt2.amount = parsedAmt
    try {
      await updateMaterialAttachment(currentRecord2.id!, latestAtt2.id, { amount: parsedAmt })
      message.success(`已自动填入 PDF 识别金额 ¥${parsedAmt.toFixed(2)}`)
    } catch { /* 静默 */ }
  } else if (Math.abs(currentAttAmt - parsedAmt) > 0.009) {
    materialPdfMessages[materialRowKey(record)] = {
      type: 'warn',
      text: `PDF 识别 ¥${parsedAmt.toFixed(2)}，与填写 ¥${currentAttAmt.toFixed(2)} 不一致`
    }
  } else {
    materialPdfMessages[materialRowKey(record)] = {
      type: 'success',
      text: `PDF 识别 ¥${parsedAmt.toFixed(2)}，与填写一致`
    }
  }
  if (parsedMsg) materialPdfMessages[materialRowKey(record)] = parsedMsg
}

const handleUpload = async (file: File, record: MaterialItem) => {
  try {
    const id = await resolveItemId(record)
    if (!id) return false
    const res = await uploadMaterialFile(id, file, {
      formId: props.formId,
      templateId: record.templateId ?? null
    })
    if (res.data?.code === 200) {
      if (res.data.data?.id) record.id = res.data.data.id
      message.success('上传成功')
      await loadData()
      // 发票类资料项：触发 PDF 识别（非阻塞）
      if (isInvoiceMaterial(record)) {
        tryParseInvoicePdf(file, record).catch(() => {})
      }
    } else {
      message.error(res.data?.message || '上传失败')
    }
  } catch (e: any) {
    message.error(e?.message || '上传失败')
  }
  return false
}

// ==================== 新增/编辑自定义资料项 ====================
const modalVisible = ref(false)
const editingRecord = ref<MaterialItem | null>(null)
const modalForm = reactive({ name: '', required: 1, sort: 0, remark: '' })

const openAddModal = () => {
  editingRecord.value = null
  modalForm.name = ''
  modalForm.required = 1
  modalForm.sort = 0
  modalForm.remark = ''
  modalVisible.value = true
}

const openEditModal = (record: MaterialItem) => {
  editingRecord.value = record
  modalForm.name = record.name || ''
  modalForm.required = record.required ?? 1
  modalForm.sort = record.sort ?? 0
  modalForm.remark = record.remark || ''
  modalVisible.value = true
}

const handleSaveModal = async () => {
  if (!modalForm.name.trim()) {
    message.warning('请输入资料名称')
    return
  }
  try {
    if (editingRecord.value) {
      // 编辑已有项
      const id = await resolveItemId(editingRecord.value)
      if (!id) return
      const res = await updateMaterialItem({
        id,
        name: modalForm.name,
        required: modalForm.required,
        sort: modalForm.sort,
        remark: modalForm.remark,
      })
      if (res.data?.code === 200) {
        message.success('已保存')
      } else {
        message.error(res.data?.message || '保存失败')
        return
      }
    } else {
      // 新增自定义项
      const res = await addMaterialItem({
        formId: props.formId as any,
        name: modalForm.name,
        required: modalForm.required,
        sort: modalForm.sort,
        remark: modalForm.remark,
        stage: 'MATERIAL_SUBMIT', // 自定义项默认在资料提交阶段
      })
      if (res.data?.code === 200) {
        message.success('已新增')
      } else {
        message.error(res.data?.message || '新增失败')
        return
      }
    }
    modalVisible.value = false
    await loadData()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  }
}

// ==================== 清除附件 ====================
const handleClearFile = (record: MaterialItem) => {
  Modal.confirm({
    title: '确定清除此附件吗？',
    okText: '确认',
    okType: 'danger',
    onOk: async () => {
      try {
        const id = await resolveItemId(record)
        if (!id) return
        await clearMaterialFile(id)
        message.success('已清除')
        await loadData()
      } catch (e: any) {
        message.error(e?.message || '清除失败')
      }
    }
  })
}

// ==================== 删除资料项 ====================
const handleDeleteRow = (record: MaterialItem) => {
  Modal.confirm({
    title: '确定删除此自定义资料项？',
    okText: '确认',
    okType: 'danger',
    onOk: async () => {
      try {
        if (!record.id) { message.warning('资料项尚未落库'); return }
        await deleteMaterialItem(record.id)
        message.success('已删除')
        await loadData()
      } catch (e: any) {
        message.error(e?.message || '删除失败')
      }
    }
  })
}

// ==================== 删除单个附件 ====================
const handleDeleteAttachment = async (record: MaterialItem, att: any) => {
  try {
    if (!record.id || !att.id) return
    await deleteMaterialAttachment(record.id, att.id)
    message.success('已删除')
    await loadData()
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
}

// ==================== 保存附件结构化字段 ====================
const saveAttachmentField = async (record: MaterialItem, att: any, field: string, value: any) => {
  try {
    if (!record.id || !att.id) return
    await updateMaterialAttachment(record.id, att.id, { [field]: value })
    // 同步更新本地数据，避免重渲染时回退旧值
    att[field] = value
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  }
}

// ==================== 生命周期 ====================
onMounted(loadData)
watch(() => props.formId, loadData)

/** 供父组件调用刷新 */
const refresh = () => loadData()
defineExpose({ refresh })
</script>

<style scoped>
.material-manager {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
