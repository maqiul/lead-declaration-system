<template>
  <div class="material-manager">
    <!-- 补交横幅：发起免弹窗直接进入补交模式，原因可选、内联补填 -->
    <div
      v-if="activeSupplement && (mode === 'submit' || forceSupplementMode)"
      class="supp-banner"
      :class="{ 'supp-banner--draft': isDraftSupplement }"
    >
      <div class="supp-banner-icon"><FolderAddOutlined /></div>
      <div class="supp-banner-main">
        <div class="supp-banner-title">
          <span class="supp-banner-name">资料补交</span>
          <a-tag v-if="isDraftSupplement" color="blue">草稿 · 审核人尚不可见</a-tag>
          <a-tag v-else color="orange">补交中 · 待审核</a-tag>
          <span class="supp-banner-rule">存量资料只增不改</span>
        </div>
        <div class="supp-banner-tip">
          <template v-if="isDraftSupplement">请上传补交资料，完成后点「提交补交审核」（提交时填写补交原因），增量资料审核通过后生效。</template>
          <template v-else>补交的增量资料需审核通过后才生效。</template>
        </div>
        <div v-if="!isDraftSupplement && activeSupplement.reason" class="supp-banner-reason-text">补交原因：{{ activeSupplement.reason }}</div>
      </div>
    </div>
    <a-spin :spinning="loading">
      <!-- 动态渲染每个环节 -->
      <template v-for="(section, sectionIdx) in visibleSections" :key="section.itemValue">
        <a-card
          :title="section.config.sectionTitle || section.label"
          size="small"
          class="section-card"
          :id="'section-' + section.itemValue"
        >
          <template #extra>
            <a-space align="center">
              <!-- 豁免审批记录按钮（仅在第一个环节卡片显示） -->
              <a-button
                v-if="sectionIdx === 0 && exemptionCount && exemptionCount > 0"
                size="small"
                @click="emit('viewExemptionHistory')"
              >
                <template #icon><HistoryOutlined /></template>
                豁免审批记录 ({{ exemptionCount }})
              </a-button>
              <!-- 豁免审核按钮（仅在第一个环节卡片且豁免审核模式显示） -->
              <a-button
                v-if="sectionIdx === 0 && showExemptionAudit && hasPendingExemption"
                type="primary"
                size="small"
                @click="emit('exemptionApprove')"
              >
                <template #icon><CheckCircleOutlined /></template>
                {{ exemptionStep === 2 ? '豁免复核通过' : '豁免通过' }}
              </a-button>
              <a-button
                v-if="sectionIdx === 0 && showExemptionAudit && hasPendingExemption"
                danger
                size="small"
                @click="emit('exemptionReject')"
              >
                <template #icon><CloseCircleOutlined /></template>
                {{ exemptionStep === 2 ? '豁免复核驳回' : '豁免驳回' }}
              </a-button>
              <!-- 资料补交状态标签与操作（仅第一个环节卡片） -->
              <a-tag v-if="sectionIdx === 0 && activeSupplement && isDraftSupplement" color="blue">补交草稿</a-tag>
              <a-tag v-else-if="sectionIdx === 0 && activeSupplement" color="orange">资料补交中</a-tag>
              <a-button
                v-if="sectionIdx === 0 && isDraftSupplement && (mode === 'submit' || forceSupplementMode)"
                v-permission="['business:declaration:supplement:initiate']"
                type="primary"
                size="small"
                :loading="supplementAuditSubmitting"
                @click="handleSubmitSupplementForAudit"
              >
                <template #icon><SendOutlined /></template>
                提交补交审核
              </a-button>
              <a-button
                v-if="sectionIdx === 0 && canStartSupplement"
                v-permission="['business:declaration:supplement:initiate']"
                size="small"
                :loading="supplementSubmitting"
                @click="handleStartSupplement"
              >
                <template #icon><PlusOutlined /></template>
                发起资料补交
              </a-button>
              <!-- 补交历史：每一次补交了哪些文件的留档记录 -->
              <a-button
                v-if="sectionIdx === 0 && formId"
                size="small"
                @click="openSupplementHistory"
              >
                <template #icon><HistoryOutlined /></template>
                补交记录
              </a-button>
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
              <a-button
                v-if="mode === 'audit' && canOperateSection(section) && section.config.auditTaskKey"
                type="primary"
                size="small"
                :loading="submittingKey === section.config.auditTaskKey"
                @click="handleAudit(section, true)"
              >
                <template #icon><CheckCircleOutlined /></template>
                审核通过
              </a-button>
              <a-button
                v-if="mode === 'audit' && canOperateSection(section) && section.config.auditTaskKey"
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

          <!-- 补交审核卡片（第一个环节内联展示：头部操作按钮 + 申请信息 + 增量明细） -->
          <div v-if="sectionIdx === 0 && currentAuditSupplement" class="supp-audit-card">
            <div class="supp-audit-head">
              <div class="supp-audit-title">
                <span class="supp-audit-icon"><AuditOutlined /></span>
                <span class="supp-audit-name">资料补交审核</span>
                <a-tag color="orange">待审核</a-tag>
              </div>
              <a-space v-if="mode === 'audit' || autoSupplementId">
                <a-button
                  v-permission="['business:declaration:audit:material']"
                  danger
                  size="small"
                  @click="handleAuditSupplement(currentAuditSupplement, false)"
                >
                  <template #icon><CloseCircleOutlined /></template>
                  驳回
                </a-button>
                <a-button
                  v-permission="['business:declaration:audit:material']"
                  type="primary"
                  size="small"
                  class="supp-audit-btn-pass"
                  @click="handleAuditSupplement(currentAuditSupplement, true)"
                >
                  <template #icon><CheckCircleOutlined /></template>
                  审核通过
                </a-button>
              </a-space>
            </div>
            <div class="supp-audit-info">
              <div class="supp-audit-reason">
                <span class="supp-audit-label">补交原因</span>
                <span class="supp-audit-value">{{ currentAuditSupplement.reason || '未填写原因' }}</span>
              </div>
              <div class="supp-audit-info-grid">
                <span class="supp-audit-info-item"><UserOutlined class="supp-audit-info-icon" />发起人：{{ currentAuditSupplement.initiatorName || '-' }}</span>
                <span class="supp-audit-info-item"><ClockCircleOutlined class="supp-audit-info-icon" />发起时间：{{ currentAuditSupplement.createTime ? currentAuditSupplement.createTime.substring(0, 16) : '-' }}</span>
              </div>
            </div>
            <a-spin :spinning="supplementAuditLoading">
              <template v-if="currentIncrements">
                <div v-if="currentIncrements.items?.length" class="supp-incr-body">
                  <div class="supp-incr-section-title">补交新增资料项（{{ currentIncrements.items.length }}）<span class="supp-incr-section-hint">下方列表中对应橙色「补交待审核」标签</span></div>
                  <a-tag v-for="it in currentIncrements.items" :key="it.id" color="orange" style="margin-bottom:4px">{{ it.name }}</a-tag>
                </div>
                <div v-if="currentIncrements.attachments?.length" class="supp-incr-body">
                  <div class="supp-incr-section-title">补交上传文件（{{ currentIncrements.attachments.length }}）</div>
                  <div v-for="att in currentIncrements.attachments" :key="att.id" class="supp-incr-file">
                    <span class="supp-incr-file-icon"><FileTextOutlined /></span>
                    <a @click.prevent="emit('previewFile', att.fileUrl)" class="supp-incr-file-name" :title="att.fileName">{{ att.fileName }}</a>
                    <span class="supp-incr-file-time">{{ att.uploadTime ? att.uploadTime.substring(0, 16) : '' }}</span>
                  </div>
                </div>
                <a-empty v-if="!currentIncrements.items?.length && !currentIncrements.attachments?.length" description="暂无增量数据" :image-style="{ height: '40px' }" />
              </template>
            </a-spin>
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
                    <a-tag v-if="isItemRequiredIn(record as MaterialItem, section)" color="red">必填</a-tag>
                    <a-tag v-else>选填</a-tag>
                    <a-tag v-if="(record as MaterialItem).templateId == null" color="blue">自定义</a-tag>
                    <a-tag v-if="hasSupplementMark((record as MaterialItem).supplementId)" color="orange">补交待审核</a-tag>
                    <!-- 上传按钮 + 下拉菜单（补交状态允许向同一资料项追加增量文件；原文件不可删除/修改，由下方锁控制） -->
                    <div class="name-upload-actions" v-if="isEditableSection(section)">
                      <a-upload :show-upload-list="false" :before-upload="(f: File) => handleUpload(f, record as MaterialItem, section.config.templateStage || '')">
                        <a-button type="primary" size="small" class="material-upload-btn">
                          <template #icon><UploadOutlined v-if="(record as MaterialItem).status !== 1" /><PlusOutlined v-else /></template>
                          {{ (record as MaterialItem).status === 1 ? '追加' : '上传' }}
                        </a-button>
                      </a-upload>
                      <a-dropdown v-if="checkPermission(['business:declaration:material:customize'])" :trigger="['click']">
                        <a-button size="small" type="text"><MoreOutlined /></a-button>
                        <template #overlay>
                          <a-menu>
                            <a-menu-item v-if="!isRowLocked(record as MaterialItem)" @click="openEditModal(record as MaterialItem)"><EditOutlined /> 编辑名称/说明</a-menu-item>
                            <a-menu-item v-if="(record as MaterialItem).status === 1 && !isRowLocked(record as MaterialItem)" @click="handleClearFile(record as MaterialItem, section.config.templateStage || '')"><DeleteOutlined /> <span class="text-red-500">清除附件</span></a-menu-item>
                            <a-menu-item v-if="(record as MaterialItem).templateId == null && !isRowLocked(record as MaterialItem)" @click="handleDeleteRow(record as MaterialItem)"><CloseOutlined /> <span class="text-red-500">删除资料项</span></a-menu-item>
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
                    <div v-for="att in (record as MaterialItem).attachments" :key="att.id" class="att-invoice-card" :class="{ 'att-increment': hasSupplementMark(att.supplementId) }">
                      <div class="att-row-main">
                        <div class="att-file-name">
                          <FileTextOutlined class="file-icon-sm" />
                          <a @click.prevent="$emit('previewFile', att.fileUrl)" class="file-name-sm" style="cursor:pointer" :title="att.fileName">{{ displayAttFileName(att) }}</a>
                          <a-tag v-if="hasSupplementMark(att.supplementId)" color="orange" style="margin-left:4px">补交待审核</a-tag>
                        </div>
                        <div class="att-divider-v"></div>
                        <!-- 补交锁定：存量附件（非增量）字段只读 -->
                        <template v-if="isEditableSection(section) && !isAttLocked(att)">
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
                        <a-popconfirm v-if="isEditableSection(section) && !isAttLocked(att) && canDeleteAttachment(att, section.config.templateStage || '')" title="确定删除？" @confirm="handleDeleteAttachment(record as MaterialItem, att, section.config.templateStage || '')">
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
                    <div v-for="att in (record as MaterialItem).attachments" :key="att.id" class="att-invoice-card" :class="{ 'att-increment': hasSupplementMark(att.supplementId) }">
                      <div class="att-row-main">
                        <div class="att-file-name">
                          <FileTextOutlined class="file-icon-sm" />
                          <a @click.prevent="$emit('previewFile', att.fileUrl)" class="file-name-sm" style="cursor:pointer" :title="att.fileName">{{ displayAttFileName(att) }}</a>
                          <a-tag v-if="hasSupplementMark(att.supplementId)" color="orange" style="margin-left:4px">补交待审核</a-tag>
                        </div>
                        <a-popconfirm v-if="isEditableSection(section) && !isAttLocked(att) && canDeleteAttachment(att, section.config.templateStage || '')" title="确定删除？" @confirm="handleDeleteAttachment(record as MaterialItem, att, section.config.templateStage || '')">
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

    <!-- 提交补交审核弹窗：提交时填写补交原因 -->
    <a-modal v-model:open="supplementSubmitVisible" title="提交补交审核" :confirm-loading="supplementAuditSubmitting" :width="520" @ok="handleConfirmSubmitSupplement">
      <a-alert type="info" show-icon message="提交后补交资料将进入审核，审核通过前增量资料不会对外生效。" style="margin-bottom:16px" />
      <a-form layout="vertical">
        <a-form-item label="补交原因" required>
          <a-textarea v-model:value="submitReason" :rows="3" :maxlength="500" placeholder="请说明需要补交资料的原因，便于审核人了解补交背景" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 补交记录弹窗：每一次补交的记录与文件快照 -->
    <a-modal v-model:open="supplementHistoryVisible" title="补交记录" :footer="null" :width="720">
      <a-spin :spinning="supplementHistoryLoading">
        <a-empty v-if="!supplementHistory.length" description="暂无补交记录" />
        <div v-for="record in supplementHistory" :key="record.id" class="supp-history-card">
          <div class="supp-history-head">
            <a-tag v-if="record.status === -1" color="blue">草稿</a-tag>
            <a-tag v-else-if="record.status === 0" color="orange">补交中</a-tag>
            <a-tag v-else-if="record.status === 1" color="green">已通过</a-tag>
            <a-tag v-else-if="record.status === 2" color="red">已驳回</a-tag>
            <span class="supp-history-time">发起：{{ record.createTime ? record.createTime.substring(0, 16) : '-' }} · {{ record.initiatorName || '-' }}</span>
            <span v-if="record.auditTime" class="supp-history-time">审核：{{ record.auditTime.substring(0, 16) }} · {{ record.auditorName || '-' }}</span>
          </div>
          <div v-if="record.reason" class="supp-history-reason">补交原因：{{ record.reason }}</div>
          <div v-if="record.auditRemark" class="supp-history-reason">审核备注：{{ record.auditRemark }}</div>
          <div class="supp-history-files">
            <template v-if="record.files?.length">
              <div v-for="f in record.files" :key="f.id" class="supp-history-file">
                <FileTextOutlined style="margin-right:4px" />
                <a @click.prevent="$emit('previewFile', f.fileUrl)" style="cursor:pointer">{{ f.fileName || '查看附件' }}</a>
                <span class="supp-history-meta">{{ f.itemName || '-' }}</span>
                <span class="supp-history-meta">{{ f.uploadByName || '-' }} {{ f.uploadTime ? f.uploadTime.substring(0, 16) : '' }}</span>
              </div>
            </template>
            <span v-else style="color:#999">本次补交无文件</span>
          </div>
        </div>
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { message, Modal, Textarea } from 'ant-design-vue'
import { h } from 'vue'
import {
  UploadOutlined, FileDoneOutlined, FileTextOutlined, CloudUploadOutlined,
  CheckCircleOutlined, CloseCircleOutlined, PlusOutlined, SendOutlined,
  MoreOutlined, EditOutlined, DeleteOutlined, CloseOutlined,
  UserOutlined, ClockCircleOutlined, HistoryOutlined, FolderAddOutlined,
  AuditOutlined,
} from '@ant-design/icons-vue'
import {
  getMaterialItems, submitStage, auditStage,
  uploadMaterialFile, ensureMaterialItem,
  addMaterialItem, updateMaterialItem, deleteMaterialItem,
  clearMaterialFile, deleteMaterialAttachment, updateMaterialAttachment,
  parseInvoicePdf, canDeleteAttachment,
  getCurrentSupplement, startMaterialSupplement, submitMaterialSupplement,
  updateMaterialSupplementReason,
  getPendingSupplements, getSupplementIncrements, auditMaterialSupplement,
  getSupplementHistory,
  type MaterialItem, type MaterialSupplement,
} from '@/api/business/materialItem'
import { checkPermission } from '@/directives/permission'
import { getEnabledDictItems } from '@/api/system/dict'
import { hasStage, isItemRequiredInStage } from '@/api/system/materialTemplate'

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
  /** 豁免审批记录数量（>0 时显示按钮） */
  exemptionCount?: number
  /** 是否有待审核的豁免申请 */
  hasPendingExemption?: boolean
  /** 豁免审核当前步骤（1 或 2） */
  exemptionStep?: number
  /** 是否显示豁免审核按钮（仅豁免审核模式为 true） */
  showExemptionAudit?: boolean
  /** 任务中心进入补交审核时携带的补交单ID：加载完成后自动打开补交审核弹窗并定位该补交单 */
  autoSupplementId?: number | null
  /** 强制补交提交模式（列表页发起补交后跳转进入，即使当前环节是审核态也允许上传补交资料） */
  forceSupplementMode?: boolean
  /** 补交原因（列表页发起弹窗填写，延迟到首次上传补交资料时才创建补交单） */
  supplementDraftReason?: string
}>(), {
  mode: 'submit',
  formStatus: null,
  sectionRange: 'all',
  stopBefore: '',
  autoSupplementId: null,
  forceSupplementMode: false,
  supplementDraftReason: ''
})

const emit = defineEmits<{
  (e: 'submitted', submitKey: string): void
  (e: 'audited', auditTaskKey: string, approved: boolean): void
  (e: 'previewFile', fileUrl: string): void
  (e: 'viewExemptionHistory'): void
  (e: 'exemptionApprove'): void
  (e: 'exemptionReject'): void
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

// ---------- 资料补交流程状态 ----------
/** 当前申报单在途的补交单（null=无补交） */
const activeSupplement = ref<MaterialSupplement | null>(null)
/** 补交模式：存在在途补交时，存量资料只增不改 */
const inSupplementMode = computed(() => activeSupplement.value != null)
/** 草稿补交：已发起未提交审核，审核人不可见 */
const isDraftSupplement = computed(() => activeSupplement.value?.status === -1)
const supplementSubmitting = ref(false)
// 提交补交审核弹窗：提交时填写补交原因
const supplementSubmitVisible = ref(false)
const submitReason = ref('')
// 补交记录弹窗：每一次补交了哪些文件的留档
const supplementHistoryVisible = ref(false)
const supplementHistoryLoading = ref(false)
const supplementHistory = ref<any[]>([])
const openSupplementHistory = async () => {
  supplementHistoryVisible.value = true
  supplementHistoryLoading.value = true
  try {
    const res = await getSupplementHistory(props.formId as number)
    supplementHistory.value = res.data?.code === 200 ? (res.data.data || []) : []
  } catch (e: any) {
    message.error(e?.message || '加载补交记录失败')
  } finally {
    supplementHistoryLoading.value = false
  }
}
const supplementAuditLoading = ref(false)
const pendingSupplements = ref<MaterialSupplement[]>([])
/** 当前申报单待审的补交单：优先按任务中心携带的补交单ID定位，否则按申报单匹配 */
const currentAuditSupplement = computed<MaterialSupplement | null>(() => {
  if (props.autoSupplementId) {
    return pendingSupplements.value.find(s => Number(s.id) === Number(props.autoSupplementId)) ?? null
  }
  return pendingSupplements.value.find(s => Number(s.formId) === Number(props.formId)) ?? null
})
const currentIncrements = ref<{ supplement?: MaterialSupplement; items?: MaterialItem[]; attachments?: any[] } | null>(null)

/** 补交锁定态：存在补交单，或强制补交模式尚未建单的窗口期（存量资料只增不改） */
const supplementLockActive = computed(() => inSupplementMode.value || supplementUploadActive.value)
/** 存量资料项锁定：补交中且非增量 */
const isRowLocked = (record: MaterialItem): boolean =>
  supplementLockActive.value && !hasSupplementMark(record.supplementId)
/** 存量附件锁定：补交中且非增量 */
const isAttLocked = (att: any): boolean =>
  supplementLockActive.value && !hasSupplementMark(att?.supplementId)
/** 补交标记判定：后端 Fastjson 配置 WriteNullNumberAsZero+WriteLongAsString 会把 null 序列化为 "0"，
 *  必须按数值判定（>0 才是有效补交单ID），直接判真值会把 "0" 误认为有标记 */
const hasSupplementMark = (v: any): boolean => Number(v) > 0

/** 资料审核已通过：当前状态已越过资料审核节点的目标状态（动态映射，无配置时回退 3） */
const materialAuditPassed = computed(() => {
  const s = props.formStatus
  if (s == null) return false
  const map = props.stepStatusMap
  const auditKey = sections.value[0]?.config.auditTaskKey
  const ts = Number((auditKey && map?.get(auditKey)) ?? map?.get('materialAudit') ?? 3)
  return s > ts
})

/** 可发起补交：submit 模式、已过资料审核、无在途补交、有发起权限 */
const canStartSupplement = computed(() =>
  props.mode === 'submit'
  && materialAuditPassed.value
  && !inSupplementMode.value
  && checkPermission(['business:declaration:supplement:initiate'])
)

/** 确保草稿补交单存在（强制补交模式下无单时兑底创建，原因为空允许后补） */
const ensureDraftSupplement = async (): Promise<MaterialSupplement | null> => {
  if (activeSupplement.value) return activeSupplement.value
  if (!props.forceSupplementMode || !props.formId) {
    message.error('补交单尚未创建，请先点「发起资料补交」')
    return null
  }
  try {
    const res = await startMaterialSupplement({ formId: props.formId, reason: props.supplementDraftReason || '' })
    if (res.data?.code === 200 && res.data.data) {
      activeSupplement.value = res.data.data
      return activeSupplement.value
    }
    // 创建失败（如已存在补交单）：回读当前补交单兼容
    const curRes = await getCurrentSupplement(props.formId)
    activeSupplement.value = curRes.data?.data || null
    if (!activeSupplement.value) {
      message.error(res.data?.message || '创建补交单失败')
    }
    return activeSupplement.value
  } catch (e: any) {
    message.error('创建补交单失败: ' + (e?.message || '未知错误'))
    return null
  }
}

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

    // 补交流程状态：submit 模式（或强制补交模式）且已过资料提交时查当前补交单（含草稿）；audit 模式查待审列表
    if ((props.mode === 'submit' || props.forceSupplementMode) && (props.formStatus ?? 0) >= 3) {
      try {
        const suppRes = await getCurrentSupplement(props.formId)
        activeSupplement.value = suppRes.data?.data || null
      } catch {
        activeSupplement.value = null
      }
      // 强制补交模式（列表页发起）：无补交单时直接自动建草稿单，不再延迟到首次上传，
      // 保证进入补交即有单，存量资料锁定立即生效
      if (props.forceSupplementMode && !activeSupplement.value) {
        await ensureDraftSupplement()
      }
    } else {
      activeSupplement.value = null
    }
    if (props.mode === 'audit' || props.autoSupplementId) {
      try {
        const pendRes = await getPendingSupplements()
        pendingSupplements.value = pendRes.data?.data || []
      } catch {
        pendingSupplements.value = []
      }
      // 列表页/任务中心/审核菜单进入：定位待审补交单后内联加载增量明细（审核按钮组在环节卡片头部）
      if (currentAuditSupplement.value) {
        supplementAuditLoading.value = true
        try {
          await loadSupplementIncrements(currentAuditSupplement.value)
        } finally {
          supplementAuditLoading.value = false
        }
      }
    }
  } catch (e: any) {
    message.error('加载资料项失败: ' + (e.message || ''))
  } finally {
    loading.value = false
  }
}

// ==================== 按环节分组 ====================
const getSectionItems = (section: SectionInfo): MaterialItem[] => {
  return allItems.value
    // stage 支持多环节逗号分隔，包含匹配
    .filter(item => hasStage(item.stage, section.config.templateStage || ''))
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
/** 按环节判定资料项是否必填：模板配置了必填环节（requiredStages）时，以当前环节是否命中为准；否则回退 required 字段 */
const isItemRequiredIn = (item: MaterialItem, section: SectionInfo): boolean =>
  isItemRequiredInStage(item, section.config.templateStage || '')

const getSectionStats = (section: SectionInfo) => {
  const items = getSectionItems(section)
  const total = items.length
  const required = items.filter(i => isItemRequiredIn(i, section)).length
  const uploaded = items.filter(i => isItemRequiredIn(i, section) && i.status === 1).length
  const percent = required > 0 ? Math.round((uploaded / required) * 100) : (total > 0 ? 100 : 0)
  return { total, required, uploaded, percent }
}

// ==================== 可操作性判断 ====================
/** 控制提交/审核按钮显示 */
const canOperateSection = (section: SectionInfo): boolean => {
  if (props.canOperate) return props.canOperate(section.config)
  return true
}
/** 补交上传激活态：强制补交模式下，草稿已建或尚未建单的窗口期（需开放上传入口才能触发建单） */
const supplementUploadActive = computed(() =>
  props.forceSupplementMode
  && (isDraftSupplement.value || !activeSupplement.value)
)
/** 控制编辑操作（上传/删除/修改）：仅 submit 模式且可操作时为 true；
 *  补交草稿强制模式下仅第一个环节（申报资料）开放上传增量，其它环节维持正常规则 */
const isEditableSection = (section: SectionInfo): boolean => {
  if (supplementUploadActive.value) {
    // 补交只限第一个环节（与补交标签/提交按钮的 sectionIdx===0 口径一致）；存量资料由 isRowLocked/isAttLocked 锁死
    return sections.value.length > 0 && section.itemValue === sections.value[0].itemValue
  }
  return props.mode === 'submit' && canOperateSection(section)
}

// ==================== 提交（submit 模式） ====================
const handleSubmit = (section: SectionInfo) => {
  const submitKey = section.config.submitKey!
  const items = getSectionItems(section)

  // 前端校验：必填项是否已上传（按当前环节判定）
  const missing = items.filter((i: MaterialItem) => isItemRequiredIn(i, section) && i.status !== 1)

  const doSubmit = async (skipRequiredCheck: boolean) => {
    try {
      submittingKey.value = submitKey
      const res = await submitStage(props.formId, submitKey, skipRequiredCheck)
      if (res.data?.code === 200) {
        message.success(skipRequiredCheck
          ? `${section.label}已提交，等待豁免审核`
          : `${section.label}提交成功，等待审核`)
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

  if (missing.length > 0) {
    // 必填不全：弹确认框允许强制提交（走豁免流程）
    Modal.confirm({
      title: `还有 ${missing.length} 项必填资料未上传`,
      content: `缺失项：${missing.map((m: MaterialItem) => m.name).join('、')}。\n确认提交？系统将创建豁免审批流程，审核通过后主流程继续。`,
      okText: '确认提交',
      cancelText: '取消',
      onOk: () => doSubmit(true)
    })
    return
  }

  Modal.confirm({
    title: `确认${section.config.btnText || '提交'}？`,
    content: '提交后将进入审核流程，无法修改。',
    okText: '确认提交',
    onOk: () => doSubmit(false)
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

const handleUpload = async (file: File, record: MaterialItem, stage?: string) => {
  try {
    // 补交草稿入口且尚未建单：首次上传时延迟创建补交单
    if (props.forceSupplementMode && !activeSupplement.value) {
      const supp = await ensureDraftSupplement()
      if (!supp) return false
    }
    const id = await resolveItemId(record)
    if (!id) return false
    const res = await uploadMaterialFile(id, file, {
      formId: props.formId,
      templateId: record.templateId ?? null,
      uploadStage: stage || null,
      // 补交模式：新附件打补交增量标记
      supplementId: activeSupplement.value?.id ?? null
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
      // 补交草稿入口且尚未建单：新增增量项时延迟创建补交单
      if (props.forceSupplementMode && !activeSupplement.value) {
        const supp = await ensureDraftSupplement()
        if (!supp) return
      }
      // 新增自定义项（补交模式下携带 supplementId 作为增量）
      const res = await addMaterialItem({
        formId: props.formId as any,
        name: modalForm.name,
        required: modalForm.required,
        sort: modalForm.sort,
        remark: modalForm.remark,
        stage: 'MATERIAL_SUBMIT', // 自定义项默认在资料提交阶段
        supplementId: activeSupplement.value?.id ?? null,
      } as any)
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
const handleClearFile = (record: MaterialItem, stage?: string) => {
  Modal.confirm({
    title: '确定清除此附件吗？',
    okText: '确认',
    okType: 'danger',
    onOk: async () => {
      try {
        const id = await resolveItemId(record)
        if (!id) return
        await clearMaterialFile(id, stage || null)
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
const handleDeleteAttachment = async (record: MaterialItem, att: any, stage?: string) => {
  try {
    if (!record.id || !att.id) return
    await deleteMaterialAttachment(record.id, att.id, stage || null)
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

// ==================== 资料补交流程 ====================
/** 发起资料补交：免弹窗直接创建草稿补交单，原因在提交审核时填写 */
const handleStartSupplement = async () => {
  try {
    supplementSubmitting.value = true
    const res = await startMaterialSupplement({ formId: props.formId, reason: '' })
    if (res.data?.code === 200) {
      message.success('资料补交已发起，请上传补交资料，完成后点「提交补交审核」')
      await loadData()
    } else {
      message.error(res.data?.message || '发起补交失败')
    }
  } catch (e: any) {
    message.error(e?.message || '发起补交失败')
  } finally {
    supplementSubmitting.value = false
  }
}

/** 提交补交审核：打开弹窗填写补交原因，确认后草稿转补交中，审核人才可见 */
const supplementAuditSubmitting = ref(false)
const handleSubmitSupplementForAudit = () => {
  const supp = activeSupplement.value
  if (!supp) return
  submitReason.value = supp.reason || ''
  supplementSubmitVisible.value = true
}
const handleConfirmSubmitSupplement = async () => {
  const supp = activeSupplement.value
  if (!supp) return
  const reason = submitReason.value.trim()
  if (!reason) {
    message.warning('请填写补交原因')
    return
  }
  try {
    supplementAuditSubmitting.value = true
    // 原因有变化先保存（提交审核时填写，后端仅草稿态可改），再提交审核
    if (reason !== (supp.reason || '')) {
      const rRes = await updateMaterialSupplementReason(supp.id, reason)
      if (rRes.data?.code !== 200) {
        message.error(rRes.data?.message || '保存补交原因失败')
        return
      }
      supp.reason = reason
    }
    const res = await submitMaterialSupplement(supp.id)
    if (res.data?.code === 200) {
      message.success('补交资料已提交审核')
      supplementSubmitVisible.value = false
      await loadData()
    } else {
      message.error(res.data?.message || '提交补交审核失败')
    }
  } catch (e: any) {
    message.error(e?.message || '提交补交审核失败')
  } finally {
    supplementAuditSubmitting.value = false
  }
}

/** 查看补交增量明细 */
const loadSupplementIncrements = async (record: MaterialSupplement) => {
  try {
    const res = await getSupplementIncrements(record.id)
    if (res.data?.code === 200) {
      currentIncrements.value = res.data.data
    } else {
      message.error(res.data?.message || '加载增量明细失败')
    }
  } catch (e: any) {
    message.error(e?.message || '加载增量明细失败')
  }
}

/** 审核补交：通过→增量转正；驳回→删除增量 */
const handleAuditSupplement = (record: MaterialSupplement, approved: boolean) => {
  let remark = ''
  Modal.confirm({
    title: approved ? '确认通过该补交申请？' : '确认驳回该补交申请？',
    content: () => h('div', [
      h('div', { style: `margin-bottom:8px;color:${approved ? '#666' : '#d46b08'};` },
        approved ? '通过后补交的增量资料将正式生效。' : '驳回后补交的增量资料将被清除。请填写驳回原因。'),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: approved ? '审核意见（可选）' : '驳回原因（必填）',
        'onUpdate:value': (v: string) => { remark = v }
      })
    ]),
    okText: approved ? '确认通过' : '确认驳回',
    okButtonProps: approved ? {} : { danger: true },
    onOk: async () => {
      if (!approved && !remark.trim()) {
        message.warning('驳回请填写审核意见')
        return Promise.reject()
      }
      try {
        const res = await auditMaterialSupplement(record.id, approved, remark.trim() || undefined)
        if (res.data?.code === 200) {
          message.success(approved ? '补交审核通过，增量资料已生效' : '补交已驳回，增量资料已清除')
          // 刷新待审列表与增量明细
          const pendRes = await getPendingSupplements()
          pendingSupplements.value = pendRes.data?.data || []
          currentIncrements.value = null
          await loadData()
        } else {
          message.error(res.data?.message || '审核失败')
          return Promise.reject()
        }
      } catch (e: any) {
        message.error(e?.message || '审核失败')
        return Promise.reject()
      }
    }
  })
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
/* 补交增量附件高亮：浅橙底色 + 橙色边框，与存量文件区分 */
.att-invoice-card.att-increment {
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 4px;
}
/* 补交记录弹窗：每次补交一张卡片 */
.supp-history-card {
  border: 1px solid #eee;
  border-radius: 6px;
  padding: 10px 14px;
  margin-bottom: 10px;
}
.supp-history-head {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.supp-history-time {
  color: #999;
  font-size: 12px;
}
.supp-history-reason {
  color: #666;
  margin-top: 4px;
  font-size: 13px;
}
.supp-history-files {
  margin-top: 8px;
}
.supp-history-file {
  margin-bottom: 4px;
}
.supp-history-meta {
  color: #999;
  font-size: 12px;
  margin-left: 8px;
}
/* 补交审核卡片：头部标题+操作按钮，申请信息，增量明细 */
.supp-audit-card {
  margin: 4px 0 14px;
  background: #fffbf2;
  border: 1px solid #ffe7ba;
  border-radius: 10px;
  padding: 0 16px 12px;
}
.supp-audit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 0;
  border-bottom: 1px dashed #ffe7ba;
}
.supp-audit-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.supp-audit-icon {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  color: #fff;
  background: linear-gradient(135deg, #ffa940, #fa8c16);
  border-radius: 7px;
}
.supp-audit-name {
  font-size: 14px;
  font-weight: 600;
  color: #d46b08;
}
/* 审核通过按钮：绿色主按钮 */
.supp-audit-btn-pass {
  background: #52c41a;
  border-color: #52c41a;
}
.supp-audit-btn-pass:hover,
.supp-audit-btn-pass:focus {
  background: #73d13d !important;
  border-color: #73d13d !important;
}
/* 申请信息：原因突出展示 + 发起人/时间一行 */
.supp-audit-info {
  padding: 10px 0 2px;
}
.supp-audit-reason {
  background: #fff;
  border-left: 3px solid #fa8c16;
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 13px;
  color: #333;
}
.supp-audit-label {
  color: #999;
  margin-right: 8px;
}
.supp-audit-info-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 28px;
  margin-top: 8px;
  font-size: 13px;
  color: #595959;
}
.supp-audit-info-icon {
  color: #fa8c16;
  margin-right: 4px;
}
/* 增量明细分区 */
.supp-incr-body {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #ffe7ba;
}
.supp-incr-section-title {
  color: #666;
  font-size: 13px;
  margin-bottom: 6px;
}
.supp-incr-section-hint {
  color: #bbb;
  font-size: 12px;
  margin-left: 6px;
}
.supp-incr-file {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 6px 10px;
  margin-bottom: 6px;
  transition: border-color 0.2s;
}
.supp-incr-file:hover {
  border-color: #ffd591;
}
.supp-incr-file-icon {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fa8c16;
  background: #fff7e6;
  border-radius: 5px;
}
.supp-incr-file-name {
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.supp-incr-file-time {
  color: #999;
  font-size: 12px;
  margin-left: auto;
  flex-shrink: 0;
}
/* 补交横幅：橙色系卡片，图标 + 状态标签 + 原因内联补填 */
.supp-banner {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
}
/* 补交中（待审核）：更醒目的深一档底色 */
.supp-banner:not(.supp-banner--draft) {
  background: #fff1d6;
  border-color: #ffc069;
}
.supp-banner-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #fff;
  background: #fa8c16;
  border-radius: 8px;
}
.supp-banner-main {
  flex: 1;
  min-width: 0;
}
.supp-banner-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.supp-banner-name {
  font-size: 14px;
  font-weight: 600;
  color: #d46b08;
}
.supp-banner-rule {
  color: #ad6800;
  font-size: 12px;
  background: #ffe7ba;
  border-radius: 4px;
  padding: 1px 6px;
}
.supp-banner-tip {
  margin-top: 4px;
  color: #8c8c8c;
  font-size: 13px;
}
.supp-banner-reason-text {
  margin-top: 6px;
  color: #595959;
  font-size: 13px;
}
</style>
