<template>
  <div class="section-wrapper">
    <a-card v-if="showSupplementSection" id="section-supplement" title="补充资料" size="small" class="section-card">
      <template #extra>
        <a-space>
          <a-button
            v-if="canSubmitSupplement"
            type="primary" size="small"
            @click="$emit('submit-supplement')"
            :loading="submitting"
            v-permission="['business:declaration:supplement:submit']"
          >
            <template #icon><UploadOutlined /></template>
            提交补充资料
          </a-button>

          <template v-if="canAuditSupplement">
            <a-button
              type="primary" size="small"
              @click="$emit('supplement-audit-approve')"
              :loading="submitting"
              v-permission="['business:declaration:audit:supplement']"
            >
              <template #icon><CheckCircleOutlined /></template>
              审核通过
            </a-button>
            <a-button
              danger size="small"
              @click="$emit('supplement-audit-reject')"
              :loading="submitting"
              v-permission="['business:declaration:audit:supplement']"
            >
              <template #icon><CloseCircleOutlined /></template>
              审核驳回
            </a-button>
          </template>
        </a-space>
      </template>

      <a-spin :spinning="materialLoading">
        <!-- 进度卡片 -->
        <div class="progress-card">
          <div class="progress-left">
            <div class="progress-title">
              <FileDoneOutlined class="text-blue-500 mr-2" />
              <span v-if="isSupplementEditable">补充资料上传进度</span>
              <span v-else>补充资料查看</span>
            </div>
            <div class="progress-desc">
              共 <b>{{ supplementStats.total }}</b> 项资料，必填 <b class="text-red-500">{{ supplementStats.required }}</b> 项，
              已上传 <b :class="supplementStats.uploaded === supplementStats.required ? 'text-green-500' : 'text-blue-500'">{{ supplementStats.uploaded }}</b> 项
            </div>
          </div>
          <div class="progress-right">
            <a-progress
              type="circle"
              :percent="supplementStats.required === 0 ? (supplementStats.total === 0 ? 0 : 100) : Math.round((supplementStats.uploaded / supplementStats.required) * 100)"
              :width="60"
              :stroke-color="supplementStats.uploaded >= supplementStats.required && supplementStats.required > 0 ? '#52c41a' : '#1677ff'"
            />
          </div>
        </div>

        <a-table :dataSource="supplementItems" :columns="materialColumns" :pagination="false" rowKey="id" size="small" class="material-table" :expandIcon="() => null">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <div class="name-cell">
                <div class="name-main">
                  <span class="name-text">{{ (record as any).name }}</span>
                  <a-tag v-if="(record as any).required === 1" color="red" class="ui-tag">必填</a-tag>
                  <a-tag v-else class="ui-tag">选填</a-tag>
                  <div class="name-upload-actions" v-if="isSupplementEditable">
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
                  <!-- 发票类：每个附件带金额/发票号/日期字段 -->
                  <template v-if="isInvoiceMaterial(record as any)">
                    <div v-for="att in record.attachments" :key="att.id" class="att-invoice-card">
                      <div class="att-row-main">
                        <div class="att-file-name">
                          <FileTextOutlined class="file-icon-sm" />
                          <a @click.prevent="previewFile(att.fileUrl)" class="file-name-sm" style="cursor:pointer" :title="att.fileName">{{ displayAttFileName(att) }}</a>
                        </div>
                        <div class="att-divider-v"></div>
                        <template v-if="isSupplementEditable">
                          <div class="att-field-inline">
                            <span class="att-field-label">金额</span>
                            <a-input-number :value="att.amount ?? undefined" @update:value="(v: any) => saveAttachmentField(record as any, att, 'amount', v)" placeholder="-" size="small" :precision="2" style="width: 120px" />
                          </div>
                          <div class="att-field-inline">
                            <span class="att-field-label">发票号</span>
                            <a-input :value="att.invoiceNo ?? undefined" @update:value="(v: any) => saveAttachmentField(record as any, att, 'invoiceNo', v)" @blur="() => saveAttachmentField(record as any, att, 'invoiceNo', att.invoiceNo)" placeholder="-" size="small" style="width: 180px" :maxlength="100" />
                          </div>
                          <div class="att-field-inline">
                            <span class="att-field-label">日期</span>
                            <a-date-picker :value="att.invoiceDate || undefined" value-format="YYYY-MM-DD" @update:value="(v: any) => saveAttachmentField(record as any, att, 'invoiceDate', v)" placeholder="-" size="small" style="width: 140px" />
                          </div>
                        </template>
                        <template v-else>
                          <span class="att-val-tag">¥{{ att.amount ?? '-' }}</span>
                          <span class="att-val-tag">{{ att.invoiceNo || '-' }}</span>
                          <span class="att-val-tag">{{ att.invoiceDate || '-' }}</span>
                        </template>
                        <a-popconfirm v-if="isSupplementEditable" title="确定删除？" @confirm="handleDeleteAttachment(record as any, att)">
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
                  <!-- 非发票类：简单格式 -->
                  <template v-else>
                    <div v-for="att in record.attachments" :key="att.id" class="att-invoice-card">
                      <div class="att-row-main">
                        <div class="att-file-name">
                          <FileTextOutlined class="file-icon-sm" />
                          <a @click.prevent="previewFile(att.fileUrl)" class="file-name-sm" style="cursor:pointer" :title="att.fileName">{{ displayAttFileName(att) }}</a>
                        </div>
                        <a-popconfirm v-if="isSupplementEditable" title="确定删除？" @confirm="handleDeleteAttachment(record as any, att)">
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
 * 补充资料 Section
 * - 数据/计算属性通过 inject 获取（由 FormComposition provide）
 * - 操作类事件通过 emit 通知父组件处理（API 调用留在父组件）
 */
import { toRefs } from 'vue'
import { useFormState } from '../composables/useDeclarationForm'
// import type { MaterialItem } from '@/api/business/materialItem'
import {
  UploadOutlined, CheckCircleOutlined, CloseCircleOutlined,
  FileDoneOutlined, FileTextOutlined, UserOutlined, EditOutlined,
  ClockCircleOutlined, CloudUploadOutlined, DeleteOutlined, PlusOutlined,
} from '@ant-design/icons-vue'

// ---- emit：操作类事件（父组件监听后调用对应 API） ----
const emit = defineEmits<{
  'submit-supplement': []
  'supplement-audit-approve': []
  'supplement-audit-reject': []
}>()

// ---- inject：共享状态（只读数据 + 工具函数） ----
const state = useFormState()
const {
  // 数据
  showSupplementSection, canSubmitSupplement, canAuditSupplement,
  submitting, materialLoading,
  isSupplementEditable, supplementStats, supplementItems, materialColumns,
  // 工具函数（含数据操作，非直接 API 调用）
  beforeMaterialUpload, saveAttachmentField, previewFile, handleDeleteAttachment,
  isInvoiceMaterial,
} = toRefs(state) as any

// 纯工具函数（无状态依赖，本地定义即可）
const displayAttFileName = (att: any): string => att.fileName || '查看附件'
</script>

