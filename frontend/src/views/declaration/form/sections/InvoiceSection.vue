<template>
  <div class="section-wrapper">
    <a-card v-if="showInvoiceSection" id="section-invoice" :title="isInvoiceEditable ? '业务发票 (可编辑)' : '业务发票'" size="small" class="section-card">
      <template #extra>
        <a-space>
          <a-button
            v-if="canSubmitInvoice"
            type="primary" size="small"
            @click="$emit('submit-invoice')"
            :loading="submitting"
            v-permission="['business:declaration:invoice:submit']"
          >
            <template #icon><UploadOutlined /></template>
            提交发票审核
          </a-button>
          <template v-if="canAuditInvoice">
            <a-button
              type="primary" size="small"
              @click="$emit('invoice-audit-approve')"
              :loading="submitting"
              v-permission="['business:declaration:audit:invoice']"
            >
              <template #icon><CheckCircleOutlined /></template>
              审核通过
            </a-button>
            <a-button
              danger size="small"
              @click="$emit('invoice-audit-reject')"
              :loading="submitting"
              v-permission="['business:declaration:audit:invoice']"
            >
              <template #icon><CloseCircleOutlined /></template>
              审核驳回
            </a-button>
          </template>
        </a-space>
      </template>

      <a-spin :spinning="materialLoading">
        <div class="progress-card">
          <div class="progress-left">
            <div class="progress-title">
              <FileDoneOutlined class="text-blue-500 mr-2" />
              <span v-if="isInvoiceEditable">业务发票上传进度</span>
              <span v-else>业务发票查看</span>
            </div>
            <div class="progress-desc">
              共 <b>{{ invoiceStats.total }}</b> 项资料，必填 <b class="text-red-500">{{ invoiceStats.required }}</b> 项，
              已上传 <b :class="invoiceStats.uploaded === invoiceStats.required ? 'text-green-500' : 'text-blue-500'">{{ invoiceStats.uploaded }}</b> 项
            </div>
          </div>
          <div class="progress-right">
            <a-progress
              type="circle"
              :percent="invoiceStats.required === 0 ? (invoiceStats.total === 0 ? 0 : 100) : Math.round((invoiceStats.uploaded / invoiceStats.required) * 100)"
              :width="60"
              :stroke-color="invoiceStats.uploaded >= invoiceStats.required && invoiceStats.required > 0 ? '#52c41a' : '#1677ff'"
            />
          </div>
        </div>

        <a-alert v-if="isInvoiceEditable" type="info" show-icon message="请上传业务发票相关附件，支持上传多份发票" style="margin-bottom: 12px" />

        <a-table :dataSource="invoiceStageItems" :columns="materialColumns" :pagination="false" rowKey="id" size="small" class="material-table" :expandIcon="() => null">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <div class="name-cell">
                <div class="name-main">
                  <span class="name-text">{{ (record as any).name }}</span>
                  <a-tag v-if="(record as any).required === 1" color="red" class="ui-tag">必填</a-tag>
                  <a-tag v-else class="ui-tag">选填</a-tag>
                  <div class="name-upload-actions" v-if="isInvoiceEditable">
                    <a-upload :show-upload-list="false" :before-upload="(f: File) => beforeMaterialUpload(f, record as any)">
                      <a-button type="primary" size="small" class="material-upload-btn">
                        <template #icon><UploadOutlined v-if="(record as any).status !== 1" /><PlusOutlined v-else /></template>
                        {{ (record as any).status === 1 ? '追加' : '上传' }}
                      </a-button>
                    </a-upload>
                  </div>
                </div>
                <div v-if="(record as any).remark" class="name-remark">{{ (record as any).remark }}</div>
                <!-- 附件列表 -->
                <template v-if="record.attachments && record.attachments.length > 0">
                  <div v-for="att in record.attachments" :key="att.id" class="att-invoice-card">
                    <div class="att-row-main">
                      <div class="att-file-name">
                        <FileTextOutlined class="file-icon-sm" />
                        <a @click.prevent="previewFile(att.fileUrl)" class="file-name-sm" style="cursor:pointer" :title="att.fileName">{{ displayAttFileName(att) }}</a>
                      </div>
                      <div class="att-divider-v"></div>
                      <template v-if="isInvoiceEditable">
                        <div class="att-field-inline">
                          <span class="att-field-label">金额</span>
                          <a-input-number :value="att.amount ?? undefined" @update:value="(v: any) => saveAttachmentField(record as any, att, 'amount', v)" placeholder="-" size="small" :precision="2" style="width: 120px" />
                        </div>
                        <div class="att-field-inline">
                          <span class="att-field-label">发票号</span>
                          <a-input :value="att.invoiceNo ?? undefined" @update:value="(v: any) => saveAttachmentField(record as any, att, 'invoiceNo', v)" placeholder="-" size="small" style="width: 180px" :maxlength="100" />
                        </div>
                        <div class="att-field-inline">
                          <span class="att-field-label">日期</span>
                          <a-date-picker :value="att.invoiceDate ? dayjs(att.invoiceDate) : undefined" @update:value="(v: any) => saveAttachmentField(record as any, att, 'invoiceDate', v ? v.format('YYYY-MM-DD') : null)" size="small" style="width: 150px" />
                        </div>
                      </template>
                      <template v-else>
                        <span class="att-val-tag">¥{{ att.amount ?? '-' }}</span>
                        <span class="att-val-tag">{{ att.invoiceNo || '-' }}</span>
                        <span class="att-val-tag">{{ att.invoiceDate || '-' }}</span>
                      </template>
                      <a-popconfirm v-if="isInvoiceEditable" title="确定删除？" @confirm="handleDeleteAttachment(record as any, att)">
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
                <template v-else>
                  <div class="file-cell file-empty"><CloudUploadOutlined class="file-icon" /><span>尚未上传</span></div>
                </template>
              </div>
            </template>
          </template>
        </a-table>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
/**
 * 业务发票 Section
 * - 数据/计算属性通过 inject 获取
 * - 操作类事件通过 emit 通知父组件
 */
import { toRefs } from 'vue'
import dayjs from 'dayjs'
import { useFormState } from '../composables/useDeclarationForm'
// import type { MaterialItem } from '@/api/business/materialItem'
import {
  UploadOutlined, CheckCircleOutlined, CloseCircleOutlined,
  FileDoneOutlined, FileTextOutlined, UserOutlined, EditOutlined,
  ClockCircleOutlined, CloudUploadOutlined, DeleteOutlined, PlusOutlined,
} from '@ant-design/icons-vue'

const emit = defineEmits<{
  'submit-invoice': []
  'invoice-audit-approve': []
  'invoice-audit-reject': []
}>()

const state = useFormState()
const {
  showInvoiceSection, canSubmitInvoice, canAuditInvoice,
  submitting, materialLoading, isInvoiceEditable,
  invoiceStats, invoiceStageItems, materialColumns,
  beforeMaterialUpload, previewFile, handleDeleteAttachment, saveAttachmentField,
} = toRefs(state) as any

const displayAttFileName = (att: any): string => att.fileName || '查看附件'
</script>
