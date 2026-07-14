<template>
  <div class="section-wrapper">
    <a-card v-if="formStatus && formStatus >= 2" id="section-material" title="申报资料" size="small" class="section-card">
      <template #extra>
        <a-space>
          <a-button
            v-if="formStatus === 2 && isMaterialMode && !isReadonly"
            type="primary" size="small"
            @click="$emit('submit-material')"
            :loading="submitting"
            v-permission="['business:declaration:material:submit']"
          >
            <template #icon><UploadOutlined /></template>
            提交资料审核
          </a-button>
          <template v-if="formStatus === 3 && isMaterialAuditMode">
            <a-button type="primary" size="small" @click="$emit('material-audit-approve')" :loading="submitting" v-permission="['business:declaration:audit:material']">
              <template #icon><CheckCircleOutlined /></template> 审核通过
            </a-button>
            <a-button danger size="small" @click="$emit('material-audit-reject')" :loading="submitting" v-permission="['business:declaration:audit:material']">
              <template #icon><CloseCircleOutlined /></template> 审核驳回
            </a-button>
          </template>
        </a-space>
      </template>

      <a-spin :spinning="materialLoading">
        <div class="progress-card">
          <div class="progress-left">
            <div class="progress-title">
              <FileDoneOutlined class="text-blue-500 mr-2" />
              <span v-if="!isMaterialReadonly">资料上传进度</span>
              <span v-else>资料查看</span>
            </div>
            <div class="progress-desc">
              共 <b>{{ coreMaterialItems.length }}</b> 项资料，必填 <b class="text-red-500">{{ materialRequiredCount }}</b> 项，
              已上传 <b :class="materialUploadedCount === materialRequiredCount ? 'text-green-500' : 'text-blue-500'">{{ materialUploadedCount }}</b> 项
            </div>
          </div>
          <div class="progress-right">
            <a-progress type="circle" :percent="materialProgressPercent" :width="60" :stroke-color="materialProgressPercent === 100 ? '#52c41a' : '#1677ff'" />
          </div>
        </div>

        <a-tabs v-model:activeKey="activeStageTab" size="small" class="stage-tabs">
          <a-tab-pane v-for="stage in availableStages" :key="stage.value">
            <template #tab>
              <span>
                {{ stage.label }}
                <a-badge v-if="stageStats[stage.value] && stageStats[stage.value].required > 0"
                  :count="stageStats[stage.value].uploaded + '/' + stageStats[stage.value].required"
                  :number-style="{ backgroundColor: stageStats[stage.value].uploaded >= stageStats[stage.value].required ? '#52c41a' : '#1677ff', fontSize: '11px', boxShadow: 'none' }" class="ml-1" />
                <a-badge v-else-if="stageStats[stage.value]" :count="stageStats[stage.value].total"
                  :number-style="{ backgroundColor: '#8c8c8c', fontSize: '11px', boxShadow: 'none' }" class="ml-1" />
              </span>
            </template>
          </a-tab-pane>
        </a-tabs>

        <div class="toolbar" v-if="isMaterialEditable">
          <a-space>
            <a-button type="primary" size="small" class="material-customize-btn" @click="modalVisible = true"
                      v-permission="['business:declaration:material:customize']">
              <template #icon><PlusOutlined /></template> 新增自定义资料项
            </a-button>
          </a-space>
        </div>

        <a-table :dataSource="activeStageItems" :columns="materialColumns" :pagination="false" :rowKey="materialRowKey"
          size="middle" class="material-table" :expandedRowKeys="materialExpandedKeys" :showExpandColumn="false">
          <template #expandedRowRender="{ record }">
            <div class="schema-inline" v-if="parseMaterialSchema(record.formSchema).length">
              <div class="schema-field"
                v-for="field in parseMaterialSchema(record.formSchema).filter((f: any) => !isInvoiceMaterial(record) || !MATERIAL_FIXED_KEYS.includes(f.key))"
                :key="field.key">
                <label class="schema-label">
                  <span v-if="field.required" class="required-star">*</span>{{ field.label }}
                </label>
                <div v-if="field.type === 'number' && field.key === 'amount' && isInvoiceMaterial(record)" class="schema-input-wrap">
                  <a-input-number :value="getMaterialFieldValue(record, field.key)" @update:value="(v: any) => setMaterialFieldValue(record, field.key, v)"
                    @blur="saveMaterialRowFields(record)" :disabled="!isMaterialEditable" size="small" class="schema-input" :precision="4" />
                  <div v-if="materialPdfMessages[materialRowKey(record)]" class="pdf-amount-hint"
                    :class="'pdf-amount-hint-' + materialPdfMessages[materialRowKey(record)].type">
                    {{ materialPdfMessages[materialRowKey(record)].text }}
                  </div>
                </div>
                <a-input-number v-else-if="field.type === 'number'" :value="getMaterialFieldValue(record, field.key)"
                  @update:value="(v: any) => setMaterialFieldValue(record, field.key, v)" @blur="saveMaterialRowFields(record)"
                  :disabled="!isMaterialEditable" size="small" class="schema-input" :precision="4" />
                <a-date-picker v-else-if="field.type === 'date'" :value="getMaterialFieldValue(record, field.key) || undefined"
                  value-format="YYYY-MM-DD" @update:value="(v: any) => { setMaterialFieldValue(record, field.key, v); saveMaterialRowFields(record) }"
                  :disabled="!isMaterialEditable" size="small" class="schema-input" />
                <a-select v-else-if="field.type === 'select'" :value="getMaterialFieldValue(record, field.key)"
                  @update:value="(v: any) => { setMaterialFieldValue(record, field.key, v); saveMaterialRowFields(record) }"
                  :disabled="!isMaterialEditable" :options="(field.options || []).map((o: string) => ({ label: o, value: o }))"
                  size="small" class="schema-input" allow-clear />
                <a-input v-else :value="getMaterialFieldValue(record, field.key)"
                  @update:value="(v: any) => setMaterialFieldValue(record, field.key, v)" @blur="saveMaterialRowFields(record)"
                  :disabled="!isMaterialEditable" size="small" class="schema-input" :maxlength="200" />
              </div>
            </div>
          </template>

          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <div class="name-cell">
                <div class="name-main">
                  <span class="name-text">{{ record.name }}</span>
                  <a-tag v-if="record.required === 1" color="red" class="ui-tag">必填</a-tag>
                  <a-tag v-else class="ui-tag">选填</a-tag>
                  <a-tag v-if="record.templateId == null" color="blue" class="ui-tag">自定义</a-tag>
                  <a-tag v-if="parseMaterialSchema(record.formSchema).length" color="purple" class="ui-tag"><FormOutlined /> 需填写字段</a-tag>
                  <div class="name-upload-actions" v-if="isMaterialEditable">
                    <a-upload :show-upload-list="false" :before-upload="(f: File) => beforeMaterialUpload(f, record)">
                      <a-button type="primary" size="small" class="material-upload-btn">
                        <template #icon><UploadOutlined v-if="record.status !== 1" /><PlusOutlined v-else /></template>
                        {{ record.status === 1 ? '追加' : '上传' }}
                      </a-button>
                    </a-upload>
                    <a-dropdown v-if="checkPermission(['business:declaration:material:customize'])" :trigger="['click']">
                      <a-button size="small" type="text"><MoreOutlined /></a-button>
                      <template #overlay>
                        <a-menu>
                          <a-menu-item @click="openEditModal(record)"><EditOutlined /> 编辑名称/说明</a-menu-item>
                          <a-menu-item v-if="record.status === 1" @click="$emit('clear-material-file', record)"><DeleteOutlined /> <span class="text-red-500">清除附件</span></a-menu-item>
                          <a-menu-item v-if="record.templateId == null" @click="$emit('delete-material-row', record)"><CloseOutlined /> <span class="text-red-500">删除资料项</span></a-menu-item>
                        </a-menu>
                      </template>
                    </a-dropdown>
                  </div>
                </div>
                <div v-if="record.remark" class="name-remark">{{ record.remark }}</div>

                <!-- 附件列表 -->
                <template v-if="record.attachments && record.attachments.length > 0">
                  <template v-if="isInvoiceMaterial(record)">
                    <div v-for="att in record.attachments" :key="att.id" class="att-invoice-card">
                      <div class="att-row-main">
                        <div class="att-file-name">
                          <FileTextOutlined class="file-icon-sm" />
                          <a @click.prevent="previewFile(att.fileUrl)" class="file-name-sm" style="cursor:pointer" :title="att.fileName">{{ displayAttFileName(att) }}</a>
                        </div>
                        <div class="att-divider-v"></div>
                        <template v-if="isMaterialEditable">
                          <div class="att-field-inline"><span class="att-field-label">金额</span>
                            <a-input-number :value="att.amount ?? undefined" @update:value="(v: any) => saveAttachmentField(record, att, 'amount', v)" placeholder="-" size="small" :precision="2" style="width: 120px" />
                          </div>
                          <div class="att-field-inline"><span class="att-field-label">发票号</span>
                            <a-input :value="att.invoiceNo ?? undefined" @update:value="(v: any) => saveAttachmentField(record, att, 'invoiceNo', v)" @blur="() => saveAttachmentField(record, att, 'invoiceNo', att.invoiceNo)" placeholder="-" size="small" style="width: 180px" :maxlength="100" />
                          </div>
                          <div class="att-field-inline"><span class="att-field-label">日期</span>
                            <a-date-picker :value="att.invoiceDate || undefined" value-format="YYYY-MM-DD" @update:value="(v: any) => saveAttachmentField(record, att, 'invoiceDate', v)" placeholder="-" size="small" style="width: 140px" />
                          </div>
                        </template>
                        <template v-else>
                          <span class="att-val-tag">¥{{ att.amount ?? '-' }}</span>
                          <span class="att-val-tag">{{ att.invoiceNo || '-' }}</span>
                          <span class="att-val-tag">{{ att.invoiceDate || '-' }}</span>
                        </template>
                        <a-popconfirm v-if="isMaterialEditable" title="确定删除？" @confirm="$emit('delete-attachment', record, att)">
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
                  <template v-else>
                    <div v-for="att in record.attachments" :key="att.id" class="att-invoice-card">
                      <div class="att-row-main">
                        <div class="att-file-name">
                          <FileTextOutlined class="file-icon-sm" />
                          <a @click.prevent="previewFile(att.fileUrl)" class="file-name-sm" style="cursor:pointer" :title="att.fileName">{{ displayAttFileName(att) }}</a>
                        </div>
                        <a-popconfirm v-if="isMaterialEditable" title="确定删除？" @confirm="$emit('delete-attachment', record, att)">
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
                    <div class="file-count-hint" v-if="record.attachments.length > 1">共 {{ record.attachments.length }} 份文件</div>
                  </template>
                </template>
                <template v-else-if="record.status === 1 && record.fileUrl">
                  <div class="file-item-row">
                    <FileTextOutlined class="file-icon-sm" />
                    <a @click.prevent="previewFile(record.fileUrl)" class="file-name-sm" style="cursor:pointer">{{ record.fileName || '查看附件' }}</a>
                  </div>
                </template>
                <template v-else>
                  <div class="file-cell file-empty"><CloudUploadOutlined class="file-icon" /><span>尚未上传</span></div>
                </template>
              </div>
            </template>
          </template>
        </a-table>
      </a-spin>
    </a-card>

    <!-- 新增/编辑资料项弹窗（本地 UI 状态） -->
    <a-modal v-model:open="modalVisible" :title="editingRecord ? '编辑资料项' : '新增自定义资料项'"
      @ok="$emit('save-material-row', modalForm)" @cancel="modalVisible = false" :confirm-loading="materialRowSaving"
      width="520px" destroyOnClose>
      <a-form layout="vertical" :model="modalForm">
        <a-form-item label="名称" required>
          <a-input v-model:value="modalForm.name" placeholder="请输入资料名称" :maxlength="100" />
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
              <a-input-number v-model:value="modalForm.sort" :min="0" :max="9999" style="width: 100%" />
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
/**
 * 申报资料 Section
 * - 数据/计算属性通过 inject 获取
 * - API 操作通过 emit 通知父组件
 * - 弹窗等纯 UI 状态保留在组件内部
 */
import { ref, reactive, toRefs } from 'vue'
import { checkPermission } from '@/directives/permission'
import { useFormState } from '../composables/useDeclarationForm'
// import type { MaterialItem } from '@/api/business/materialItem'
import {
  UploadOutlined, CheckCircleOutlined, CloseCircleOutlined,
  FileDoneOutlined, FileTextOutlined, UserOutlined, EditOutlined,
  ClockCircleOutlined, CloudUploadOutlined, DeleteOutlined, PlusOutlined,
  MoreOutlined, FormOutlined, CloseOutlined,
} from '@ant-design/icons-vue'

const emit = defineEmits<{
  'submit-material': []
  'material-audit-approve': []
  'material-audit-reject': []
  'save-material-row': [form: { name: string; required: number; sort: number; remark: string }]
  'clear-material-file': [record: any]
  'delete-material-row': [record: any]
  'delete-attachment': [record: any, att: any]
  'open-add-material-row': []
  'open-edit-material-row': [record: any]
}>()

const state = useFormState()
const {
  formStatus, isMaterialMode, isReadonly, submitting, materialLoading,
  isMaterialReadonly, isMaterialAuditMode, isMaterialEditable,
  coreMaterialItems, materialRequiredCount, materialUploadedCount, materialProgressPercent,
  activeStageTab, availableStages, stageStats, activeStageItems,
  materialColumns, materialExpandedKeys, materialPdfMessages, materialRowSaving,
  materialRowKey, parseMaterialSchema, isInvoiceMaterial,
  getMaterialFieldValue, setMaterialFieldValue, saveMaterialRowFields,
  beforeMaterialUpload, saveAttachmentField, previewFile,
} = toRefs(state) as any

// 常量
const MATERIAL_FIXED_KEYS = ['amount', 'currency', 'invoiceNo', 'invoiceDate']
const displayAttFileName = (att: any): string => att.fileName || '查看附件'

// 本地 UI 状态：弹窗
const modalVisible = ref(false)
const editingRecord = ref<any>(null)
const modalForm = reactive({ name: '', required: 1, sort: 0, remark: '' })

function openEditModal(record: any) {
  editingRecord.value = record
  modalForm.name = record.name || ''
  modalForm.required = record.required ?? 1
  modalForm.sort = record.sort ?? 0
  modalForm.remark = record.remark || ''
  modalVisible.value = true
}
</script>
