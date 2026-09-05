<template>
  <div class="declaration-form-page">
    <a-card :title="(isMaterialMode ? (isReadonly ? '申报资料查看' : '提交申报资料') : isExemptionAuditMode ? '申报单详情 - 豁免审核' : isMaterialSupplementAuditMode ? '申报单详情 - 资料补交审核' : isMaterialAuditMode ? '申报单详情 - 资料审核' : isSupplementMode ? '申报单详情 - 补充资料提交' : isSupplementAuditMode ? '申报单详情 - 补充资料审核' : canSubmitInvoiceAmount ? '申报单详情 - 申请开票金额' : (canAuditInvoiceAmount || isInvoiceAmountAuditMode) ? '申报单详情 - 开票金额审核' : isInvoiceAmountMode ? '申报单详情 - 申请开票金额' : isInvoiceAuditMode ? '申报单详情 - 发票审核' : isInvoiceUploadMode ? '申报单详情 - 上传发票' : '出口申报表单')" >
      <template #extra>
        <a-space>
          <a-button @click="goBack">
            <template #icon><RollbackOutlined /></template>
            返回列表
          </a-button>
          
          <!-- 审核详情按钮 - 所有状态都显示 -->
          <a-button
            @click="showAuditHistory"
            v-permission="['business:declaration:view']"
          >
            <template #icon><HistoryOutlined /></template>
            审核详情
          </a-button>
          
          <!-- 审核模式下的按钮：初审（状态1）/退回待审（状态11）才显示，审核完成后状态变化自动隐藏 -->
          <template v-if="isAudit && (formStatus === 1 || formStatus === 11)">
            <a-button
              type="primary"
              @click="handleApprove"
              :loading="submitting"
              v-permission="['business:declaration:audit:initial', 'business:declaration:audit:return']"
            >
              <template #icon><CheckCircleOutlined /></template>
              {{ getAuditActionText() }}通过
            </a-button>
            <a-button
              danger
              @click="handleReject"
              :loading="submitting"
              v-permission="['business:declaration:audit:initial', 'business:declaration:audit:return']"
            >
              <template #icon><CloseCircleOutlined /></template>
              {{ getAuditActionText() }}驳回
            </a-button>
          </template>

          <!-- 资料提交模式下的按钮：状态 2（待资料提交）时显示 -->
          <template v-else-if="isMaterialMode && !isReadonly">
            <!-- 无豁免申请：显示正常提交按钮（补交草稿入口隐藏，避免与补交提交混淆；豁免状态加载完成前不渲染，避免闪现） -->
            <a-button
              v-if="formStatus === 2 && exemptionLoaded && !pendingExemption && !isSupplementDraftEntry"
              type="primary"
              @click="handleSubmitMaterial"
              :loading="submitting"
              v-permission="['business:declaration:material:submit']"
            >
              <template #icon><UploadOutlined /></template>
              提交资料审核
            </a-button>
          </template>

          <!-- 资料审核模式下的按钮：状态 3（待资料审核）时显示 -->
          <template v-else-if="isMaterialAuditMode && formStatus === 3">
            <a-button
              type="primary"
              @click="handleMaterialAuditApprove"
              :loading="submitting"
              v-permission="['business:declaration:audit:material']"
            >
              <template #icon><CheckCircleOutlined /></template>
              审核通过
            </a-button>
            <a-button
              danger
              @click="handleMaterialAuditReject"
              :loading="submitting"
              v-permission="['business:declaration:audit:material']"
            >
              <template #icon><CloseCircleOutlined /></template>
              审核驳回
            </a-button>
          </template>

          <!-- 补充资料提交：状态 4 时显示（不限 mode，避免从列表进入时漏显） -->
          <template v-else-if="canSubmitSupplement">
            <a-button
              type="primary"
              @click="handleSubmitSupplement"
              :loading="submitting"
              v-permission="['business:declaration:supplement:submit']"
            >
              <template #icon><UploadOutlined /></template>
              提交补充资料
            </a-button>
          </template>

          <!-- 补充资料审核：状态 5 时显示 -->
          <template v-else-if="canAuditSupplement">
            <a-button
              type="primary"
              @click="handleSupplementAuditApprove"
              :loading="submitting"
              v-permission="['business:declaration:audit:supplement']"
            >
              <template #icon><CheckCircleOutlined /></template>
              审核通过
            </a-button>
            <a-button
              danger
              @click="handleSupplementAuditReject"
              :loading="submitting"
              v-permission="['business:declaration:audit:supplement']"
            >
              <template #icon><CloseCircleOutlined /></template>
              审核驳回
            </a-button>
          </template>

          <!-- 申请开票金额：状态 6 时显示（不限 mode，避免无发票提交菜单时漏显） -->
          <template v-else-if="canSubmitInvoiceAmount">
            <a-button
              type="primary"
              @click="handleSubmitInvoiceAmount"
              :loading="submitting"
              v-permission="['business:declaration:invoice-amount:submit']"
            >
              <template #icon><UploadOutlined /></template>
              提交开票金额申请
            </a-button>
          </template>

          <!-- 开票金额审核：状态 7 时显示 -->
          <template v-else-if="canAuditInvoiceAmount">
            <a-button
              type="primary"
              @click="handleInvoiceAmountAuditApprove"
              :loading="submitting"
              v-permission="['business:declaration:audit:invoice-amount']"
            >
              <template #icon><CheckCircleOutlined /></template>
              审核通过
            </a-button>
            <a-button
              danger
              @click="handleInvoiceAmountAuditReject"
              :loading="submitting"
              v-permission="['business:declaration:audit:invoice-amount']"
            >
              <template #icon><CloseCircleOutlined /></template>
              审核驳回
            </a-button>
          </template>

          <!-- 业务发票审核：状态 9 -->
          <template v-else-if="canAuditInvoice">
            <a-button
              type="primary"
              @click="handleInvoiceAuditApprove"
              :loading="submitting"
              v-permission="['business:declaration:audit:invoice']"
            >
              <template #icon><CheckCircleOutlined /></template>
              审核通过
            </a-button>
            <a-button
              danger
              @click="handleInvoiceAuditReject"
              :loading="submitting"
              v-permission="['business:declaration:audit:invoice']"
            >
              <template #icon><CloseCircleOutlined /></template>
              审核驳回
            </a-button>
          </template>

          <!-- 业务发票提交：状态 8（页头快捷入口，与业务发票区块内按钮一致） -->
          <template v-else-if="canSubmitInvoice">
            <a-button
              type="primary"
              @click="handleSubmitInvoice"
              :loading="submitting"
              v-permission="['business:declaration:invoice:submit']"
            >
              <template #icon><UploadOutlined /></template>
              提交发票审核
            </a-button>
          </template>

          <!-- 普通模式下的按钮 -->
          <template v-else>
            <!-- 保存草稿按钮 -->
            <a-button v-if="!isReadonly && (!formStatus || formStatus === 0)" @click="() => handleSaveDraft()" :loading="submitting" v-permission="['business:declaration:create']">
              <template #icon><SaveOutlined /></template>
              保存草稿
            </a-button>
            
            <!-- 只在草稿状态且非只读模式下显示提交按钮 -->
            <a-button v-if="!isReadonly && (!formStatus || formStatus === 0)" type="primary" @click="handleSubmit" :loading="submitting" v-permission="['business:declaration:submit']">
              <template #icon><SendOutlined /></template>
              提交申报
            </a-button>
          </template>

          <!-- 资料补交操作区（页面置顶；状态与方法来自资料区第一个 MaterialManager 实例）；
               只读查看、豁免审核入口不渲染；带区块意图的入口（scrollTo，如开票金额提交）聚焦主流程也不渲染，
               补交管理走列表页（发起/继续/取消）或补交草稿入口 -->
          <template v-if="preMaterialManagerRef && route.query.readonly !== 'true' && !isExemptionAuditMode && !route.query.scrollTo">
            <a-divider type="vertical" />
            <a-tag v-if="preMaterialManagerRef.isDraftSupplement" color="blue">补交草稿</a-tag>
            <!-- 已提交审核（status=0）：展示待审档，避免误读为仍在补交 -->
            <a-tag v-else-if="preMaterialManagerRef.activeSupplement" color="orange">补交待审核</a-tag>
            <a-button
              v-if="preMaterialManagerRef.canSubmitSupplementAudit"
              v-permission="['business:declaration:supplement:initiate']"
              type="primary"
              :loading="preMaterialManagerRef.supplementAuditSubmitting"
              @click="preMaterialManagerRef.handleSubmitSupplementForAudit()"
            >
              <template #icon><SendOutlined /></template>
              提交补交审核
            </a-button>
            <!-- 草稿补交期间可作废：与列表页「取消补交」入口对齐 -->
            <!-- <a-button
              v-if="preMaterialManagerRef.isDraftSupplement && preMaterialManagerRef.canSubmitSupplementAudit"
              v-permission="['business:declaration:supplement:initiate']"
              danger
              @click="preMaterialManagerRef.handleCancelSupplement()"
            >
              取消补交
            </a-button> -->
            <!-- <a-button
              v-if="preMaterialManagerRef.canStartSupplement"
              v-permission="['business:declaration:supplement:initiate']"
              :loading="preMaterialManagerRef.supplementSubmitting"
              @click="preMaterialManagerRef.handleStartSupplement()"
            >
              <template #icon><PlusOutlined /></template>
              发起资料补交
            </a-button> -->
            <!-- 补交历史：每一次补交了哪些文件的留档记录 -->
            <a-button
              v-if="formId"
              @click="preMaterialManagerRef.openSupplementHistory()"
            >
              <template #icon><HistoryOutlined /></template>
              补交记录
            </a-button>
          </template>
        </a-space>
      </template>
      
      <!-- 资料补交审核卡片（页面置顶）：状态与方法来自资料区第一个 MaterialManager 实例 -->
      <SupplementAuditCard
        v-if="preMaterialManagerRef?.currentAuditSupplement"
        :supplement="preMaterialManagerRef.currentAuditSupplement"
        :increments="preMaterialManagerRef.currentIncrements"
        :loading="preMaterialManagerRef.supplementAuditLoading"
        :show-actions="materialManagerMode === 'audit' || !!autoSupplementId"
        @audit="(approved: boolean) => preMaterialManagerRef!.handleAuditSupplement(preMaterialManagerRef!.currentAuditSupplement!, approved)"
        @preview="previewFile"
      />

      <!-- 退回原因提示 -->
      <a-alert
        v-if="formStatus === 11 && returnReason"
        message="退回申请原因"
        :description="returnReason"
        type="warning"
        show-icon
        class="mb-4"
      />
      
      <!-- 基本信息 + 产品明细 + 箱子信息 -->
      <BasicInfoSection
        v-if="hasSection('basic')"
        :supplement-active="basicSupplementActive"
        @add-product="addProduct"
        @remove-product="(i: number) => removeProduct(i)"
        @add-carton="addCarton"
        @remove-carton="(i: number) => removeCarton(i)"
        @upload-product-photo="(f: File, i: number) => beforeProductPhotoUpload(f, i)"
        @remove-product-photo="(i: number) => handleRemoveProductPhoto(i)"
      />

      <!-- 豁免审批状态提示 -->
      <a-alert
        v-if="pendingExemption"
        type="warning"
        show-icon
        style="margin-bottom: 16px"
      >
        <template #message>
          <span>资料豁免审批中</span>
          <a-tag color="orange" style="margin-left: 8px">
            {{ pendingExemption.exemptionType === 'INVOICE' ? '发票类(两步审核)' : pendingExemption.exemptionType === 'MIXED' ? '混合类' : '普通(一步审核)' }}
          </a-tag>
          <a-tag v-if="exemptionTaskInfo" color="blue" style="margin-left: 4px">
            当前: {{ exemptionTaskInfo.taskName || '豁免审核' }} ({{ exemptionTaskInfo.step || 1 }}/{{ exemptionTaskInfo.totalSteps || 1 }})
          </a-tag>
        </template>
        <template #description>
          <div>缺失文件清单：</div>
          <ul style="margin: 4px 0 0 16px; padding: 0">
            <li v-for="(item, idx) in parseExemptionMissingItems(pendingExemption)" :key="idx">
              {{ item.name }}<span v-if="item.stage">（{{ item.stage }}）</span>
            </li>
          </ul>
          <div v-if="pendingExemption.auditRemark" style="margin-top: 4px; color: #999">备注：{{ pendingExemption.auditRemark }}</div>
        </template>
      </a-alert>

      <!-- 资料管理：开票金额之前的环节（资料、补充资料） -->
      <MaterialManager
        v-if="showMaterialManager"
        :form-id="formId!"
        :mode="materialManagerMode"
        :form-status="formStatus"
        :step-status-map="stepStatusMap"
        :section-order-map="sectionOrderMap"
        :section-range="hasSection('invoiceAmount') ? 'pre' : 'all'"
        :stop-before="'invoiceAmount'"
        :force-supplement-mode="isSupplementDraftEntry"
        :supplement-draft-reason="supplementDraftReason"
        :readonly="route.query.readonly === 'true' || isAuditEntry || isExemptionAuditMode"
        :can-operate="canOperateMaterialStage"
        :exemption-count="exemptionHistory.length"
        :has-pending-exemption="!!pendingExemption"
        :exemption-step="exemptionTaskInfo?.step || 1"
        :show-exemption-audit="isExemptionAuditMode"
        :auto-supplement-id="autoSupplementId"
        ref="preMaterialManagerRef"
        @submitted="() => stayAndRefresh()"
        @audited="() => stayAndRefresh()"
        @preview-file="previewFile"
        @view-exemption-history="exemptionHistoryModalVisible = true"
        @exemption-approve="handleExemptionAudit(true)"
        @exemption-reject="handleExemptionAudit(false)"
      />

      <!-- 申请开票金额（插入在补充资料和发票资料之间） -->
      <InvoiceAmountSection
        v-if="hasSection('invoiceAmount')"
        @submit-invoice-amount="handleSubmitInvoiceAmount"
        @invoice-amount-audit-approve="handleInvoiceAmountAuditApprove"
        @invoice-amount-audit-reject="handleInvoiceAmountAuditReject"
        @load-invoice-amount-detail="loadInvoiceAmountDetail"
        @download-invoice-package="handleDownloadInvoicePackage"
      />

      <!-- 资料管理：开票金额及之后的环节（发票资料；补交草稿入口同样开放补交上传，但建单由 pre 实例统一负责） -->
      <MaterialManager
        v-if="showMaterialManager && hasSection('invoiceAmount')"
        :form-id="formId!"
        :mode="postMaterialManagerMode"
        :form-status="formStatus"
        :step-status-map="stepStatusMap"
        :section-order-map="sectionOrderMap"
        section-range="post"
        :stop-before="'invoiceAmount'"
        :force-supplement-mode="isSupplementDraftEntry"
        :supplement-draft-reason="supplementDraftReason"
        :readonly="route.query.readonly === 'true' || isAuditEntry || isExemptionAuditMode"
        :can-operate="canOperateMaterialStage"
        ref="postMaterialManagerRef"
        @submitted="() => stayAndRefresh()"
        @audited="() => stayAndRefresh()"
        @preview-file="previewFile"
      />

      <!-- 业务发票 -->
      <InvoiceSection
        v-if="false"
        @submit-invoice="handleSubmitInvoice"
        @invoice-audit-approve="handleInvoiceAuditApprove"
        @invoice-audit-reject="handleInvoiceAuditReject"
      />

      <!-- 水单信息展示（收汇 + 出款） -->
      <RemittanceDisplaySection :form-id="formId" />

    </a-card>
    <a-modal
  v-model:open="auditHistoryVisible"
  title="审核历史详情"
  width="1200px"
  :footer="null"
>
  <a-table
    :dataSource="auditHistoryList"
    :columns="auditHistoryColumns"
    :loading="auditHistoryLoading"
    rowKey="id"
    size="small"
    :scroll="{ x: 1100 }"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'auditStatus'">
        <a-tag :color="record.auditStatus === 1 ? 'success' : record.auditStatus === 2 ? 'error' : 'processing'">
          {{ record.auditStatus === 1 ? '通过' : record.auditStatus === 2 ? '驳回' : '待审核' }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'businessType'">
        <a-tag color="blue">{{ getBusinessTypeText(record.businessType) }}</a-tag>
      </template>
      <template v-else-if="column.key === 'preStatus'">
        <a-tag>{{ getStatusText(record.preStatus) }}</a-tag>
      </template>
    </template>
  </a-table>
</a-modal>

<!-- 审核意见弹窗 -->
<a-modal
  v-model:open="remarkModalVisible"
  :title="`${remarkAction}审核`"
  width="500px"
  :confirm-loading="remarkSubmitting"
  @ok="handleRemarkSubmit"
>
  <a-form layout="vertical">
    <a-form-item label="审核意见" required>
      <a-textarea
        v-model:value="remarkValue"
        placeholder="请输入审核意见"
        :rows="4"
        :auto-size="{ minRows: 4, maxRows: 8 }"
      />
    </a-form-item>
  </a-form>
</a-modal>

    <!-- 文件预览弹窗 -->
    <FilePreviewModal v-model:visible="previewVisible" :url="previewUrl" />

    <!-- 豁免审批记录弹窗 -->
    <a-modal
      v-model:open="exemptionHistoryModalVisible"
      title="豁免审批记录"
      :footer="null"
      :width="600"
    >
      <a-timeline style="padding: 16px 0 0 8px">
        <a-timeline-item
          v-for="(ex, idx) in exemptionHistory"
          :key="idx"
          :color="ex.status === 0 ? 'blue' : ex.status === 1 ? 'green' : 'red'"
        >
          <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap">
            <a-tag :color="ex.status === 0 ? 'processing' : ex.status === 1 ? 'success' : 'error'" style="margin: 0">
              {{ ex.status === 0 ? '审核中' : ex.status === 1 ? '已通过' : '已驳回' }}
            </a-tag>
            <a-tag color="default" style="margin: 0">
              {{ ex.exemptionType === 'INVOICE' ? '发票类' : ex.exemptionType === 'MIXED' ? '混合类' : '普通' }}
            </a-tag>
            <span style="color: #999; font-size: 12px">{{ formatDate(ex.auditTime || ex.createTime) || '-' }}</span>
          </div>
          <div v-if="ex.missingItems" style="margin-top: 4px; font-size: 12px; color: #666">
            缺失项：{{ parseExemptionMissingItems(ex).map((i: any) => i.name).join('、') }}
          </div>
          <div v-if="ex.auditRemark" style="margin-top: 2px; font-size: 12px; color: #999">
            审核意见：{{ ex.auditRemark }}
          </div>
        </a-timeline-item>
      </a-timeline>
      <a-empty v-if="exemptionHistory.length === 0" description="暂无记录" />
    </a-modal>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onActivated, onUnmounted, watch, h, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal, Textarea } from 'ant-design-vue'
import type { SelectValue } from 'ant-design-vue/lib/select';
import {
  UploadOutlined,
  HistoryOutlined,
  RollbackOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SaveOutlined,
  SendOutlined,
} from '@ant-design/icons-vue'
import dayjs, { Dayjs } from 'dayjs'

import {
  getDeclarationDetail, 
  addDeclaration, 
  updateDeclaration, 
  uploadFile, 
  saveDraft, 
  deleteDeclaration,
  submitDeclaration,
  auditDeclaration,
  getActiveTasks,
  getBatchActiveTasks,
  auditReturnToDraft,
  getReturnAuditHistory,
  
  exportInvoicePackage,
  getAvailableFlowTemplates,
  // 业务发票 API 已废弃，统一使用资料项 INVOICE 环节
} from '@/api/business/declaration'
import {
  getMaterialItems,
  getMaterialTemplatePreview,
  updateMaterialItem,
  uploadMaterialFile,
  deleteMaterialAttachment,
  updateMaterialAttachment,
  ensureMaterialItem,
  submitMaterial,
  submitInvoice,
  auditMaterial,
  auditInvoice,
  parseInvoicePdf,
  submitSupplement,
  auditSupplement,
  submitInvoiceAmount,
  auditInvoiceAmount,
  getInvoiceAmountDetail,
  getExemptionList,
  getExemptionDetail,
  auditExemption,
  getExemptionCurrentTask,
  getSupplementIncrements,
  getCurrentSupplement,
  type MaterialItem,
  type MaterialAttachment
} from '@/api/business/materialItem'
import { getRemittancesByFormId } from '@/api/business/remittance'
import {
  MATERIAL_STAGES,
  splitStages,
  hasStage,
  isItemRequiredInStage,
  type MaterialStage
} from '@/api/system/materialTemplate'
import { getEnabledDictItems } from '@/api/system/dict'
import { getProductTypes } from '@/api/system/product'
import { getEnabledTransportModes } from '@/api/system/transportMode'
import { getEnabledTradeTerms } from '@/api/system/tradeTerm'
import { getEnabledPaymentMethods } from '@/api/system/paymentMethod'
import { getEnabledCountries } from '@/api/system'
import { getEnabledCurrencies } from '@/api/system/currency'
import { getActiveMeasurementUnits, type MeasurementUnit } from '@/api/system/measurement-unit'
import { getCitiesByCountry } from '@/api/system/city-info'
import {  findUnitByCode } from '@/utils/measurement-unit'
import { getEnabledEntityConfigs, type EntityConfig } from '@/api/system/entityConfig'
import { getAllEnabledCustomers, addCustomer, type CustomerConfig } from '@/api/system/customerConfig'
import { getAllEnabledPartyB, type PartyBConfig } from '@/api/system/partyBConfig'
import { getTabKey, type DeclarationTabType } from '@/utils/tabKey'
import { registerTabGuard, setTabMeta } from '@/composables/useTabGuard'
import { getFlowTemplateNodesByCode } from '@/api/system/flowTemplate'
import FilePreviewModal from '@/components/FilePreviewModal.vue'
import BasicInfoSection from './sections/BasicInfoSection.vue'
import InvoiceAmountSection from './sections/InvoiceAmountSection.vue'
import InvoiceSection from './sections/InvoiceSection.vue'
import RemittanceDisplaySection from './sections/RemittanceDisplaySection.vue'
import MaterialManager from './MaterialManager.vue'
import SupplementAuditCard from './SupplementAuditCard.vue'
import { provideFormState } from './composables/useDeclarationForm'
import { formatDate } from '@/utils/common'

// 文件预览
const previewVisible = ref(false)
const previewUrl = ref('')
const previewFile = (url: string) => { if (url) { previewUrl.value = url; previewVisible.value = true } }

// 文件预览 URL 生成函数
const FILE_DOWNLOAD_URL = '/api/v1/files/download'
const getFilePreviewUrl = (id: number | string) => `${FILE_DOWNLOAD_URL}?id=${id}`

const route = useRoute()
const router = useRouter()

// 页面状态
const isAudit = ref(route.query.mode === 'audit')
const isPaymentMode = ref(route.query.mode === 'payment') // 水单提交模式
const isMaterialMode = ref(route.query.mode === 'material') // 资料提交/查看模式（豁免审核已独立拆分，不再复用本分支按钮）
const isMaterialAuditMode = ref(route.query.mode === 'materialAudit') // 资料审核模式
const isExemptionAuditMode = ref(route.query.mode === 'exemptionAudit') // 豁免审核模式
const isMaterialSupplementAuditMode = ref(route.query.mode === 'materialSupplementAudit') // 资料补交审核模式（独立 Flowable 流程）
// 任务中心进入补交审核时携带的补交单ID，用于 MaterialManager 自动定位补交审核弹窗
const autoSupplementId = ref(route.query.supplementId ? Number(route.query.supplementId) : null)
// 列表页发起补交后跳转进入（supplementDraft=1）：强制资料区为补交提交模式，允许上传补交资料
const isSupplementDraftEntry = ref(route.query.supplementDraft === '1')
// 列表页提交按钮跳转进入（autoSubmit=1）：数据加载完成后自动触发提交，一次性标志避免重复触发
const autoSubmitConsumed = ref(false)
// 发起补交弹窗填写的原因：延迟到首次上传补交资料时才创建补交单
const supplementDraftReason = ref(route.query.supplementReason ? String(route.query.supplementReason) : '')
const isInvoiceAuditMode = ref(route.query.mode === 'invoiceAudit') // 发票审核模式
const isInvoiceUploadMode = ref(route.query.mode === 'invoiceUpload') // 发票上传模式
const isSupplementMode = ref(route.query.mode === 'supplement') // 补充资料提交模式
const isSupplementAuditMode = ref(route.query.mode === 'supplementAudit') // 补充资料审核模式
const isInvoiceAmountMode = ref(route.query.mode === 'invoiceAmount') // 申请开票金额提交模式
const isInvoiceAmountAuditMode = ref(route.query.mode === 'invoiceAmountAudit') // 开票金额审核模式
const isReadonly = ref(route.query.readonly === 'true' || isAudit.value)
const formId = ref(route.query.id ? Number(route.query.id) : null)
// 不信任 URL 的 status 参数：初始为 null，数据加载后以后端实时状态覆盖，消除伪造 status 的首屏窗口期
const formStatus = ref<number | null>(null)
const submitting = ref(false)

// 流程模板节点配置（控制区块显示）
const enabledSections = ref<Set<string>>(new Set(['basic', 'material', 'supplement', 'invoiceAmount', 'invoice']))
/** 流程节点完整数据（用于动态映射 nodeKey → targetStatus） */
const templateNodesData = ref<any[]>([])
/** nodeKey → targetStatus 动态映射（从流程配置中获取） */
const stepStatusMap = computed<Map<string, number>>(() => {
  const map = new Map<string, number>()
  templateNodesData.value.forEach((n: any) => {
    if (n.node?.nodeKey && n.node?.targetStatus != null) {
      map.set(n.node.nodeKey, n.node.targetStatus)
    }
  })
  return map
})
/** formSection → 最小 sortOrder（从流程节点推导环节显示顺序） */
const sectionOrderMap = computed<Map<string, number>>(() => {
  const map = new Map<string, number>()
  templateNodesData.value.forEach((n: any) => {
    const section = n.node?.formSection
    if (section) {
      const current = map.get(section) ?? Infinity
      if (n.sortOrder < current) {
        map.set(section, n.sortOrder)
      }
    }
  })
  return map
})
const loadTemplateSections = async (templateCode: string) => {
  try {
    const response = await getFlowTemplateNodesByCode(templateCode)
    if (response.data?.code === 200) {
      const nodes = response.data.data || []
      templateNodesData.value = nodes
      const sections = new Set<string>()
      nodes.forEach((n: any) => {
        if (n.enabled === 1 && n.node?.formSection) {
          sections.add(n.node.formSection)
        }
      })
      if (sections.size > 0) {
        enabledSections.value = sections
      }
    }
  } catch (error) {
    // 加载失败时保持默认显示所有区块
  }
}
const hasSection = (section: string) => enabledSections.value.has(section)

// 主体配置
const entityList = ref<EntityConfig[]>([])
const loadEntityList = async () => {
  try {
    const response = await getEnabledEntityConfigs()
    if (response.data?.code === 200) {
      entityList.value = response.data.data || []
    }
  } catch (error) {
    // ignore
  }
  // 加载完主体列表后，如果 entityId 未设置，根据 shipperCompany 自动匹配
  autoMatchEntity()
}

// 根据发货公司名称自动匹配主体
const autoMatchEntity = () => {
  if (formData.entityId) return // 已有主体ID，不覆盖
  if (!formData.shipperCompany || entityList.value.length === 0) return
  const entity = entityList.value.find(
    e => e.entityName === formData.shipperCompany || e.entityNameCn === formData.shipperCompany
  )
  if (entity) {
    formData.entityId = entity.id
    if (!formData.shipperAddress && entity.entityAddress) {
      formData.shipperAddress = entity.entityAddress
    }
  }
}

// 选择主体后自动填充发货人信息
const handleCompanyChange = (companyName: any) => {
  if (!companyName) {
    formData.entityId = undefined
    formData.shipperAddress = ''
    return
  }
  const entity = entityList.value.find(e => e.entityName === companyName)
  if (entity) {
    formData.entityId = entity.id
    formData.shipperAddress = entity.entityAddress || ''
  } else {
    formData.entityId = undefined
  }
}

// 过滤公司选项（支持英文名和中文名搜索）
const filterCompanyOption = (input: string, option: any) => {
  const label = option.children?.()[0]?.children || ''
  const lowerInput = input.toLowerCase()
  // 通过 value (entityName) 匹配
  if (option.value && String(option.value).toLowerCase().includes(lowerInput)) return true
  // 通过 label 文本匹配
  if (typeof label === 'string' && label.toLowerCase().includes(lowerInput)) return true
  return false
}

// 常用客户
const customerList = ref<CustomerConfig[]>([])
const customerOptions = computed(() => {
  const searchText = formData.consigneeCompany || ''
  const options = customerList.value
    .filter(c => !searchText || c.customerName.toLowerCase().includes(searchText.toLowerCase()))
    .map(c => ({
      value: c.customerName,
      label: c.customerName
    }))
  // 如果输入的内容不在列表中，显示"快速新增"选项
  if (searchText && !customerList.value.some(c => c.customerName.toLowerCase() === searchText.toLowerCase())) {
    options.push({
      value: '__add_new__',
      label: `+ 快速新增: ${searchText}`
    })
  }
  return options
})

// 常用客户筛选（a-auto-complete 不需要 filter-option，已在 computed 中处理）
const filterCustomerOption = () => true

// 选择常用客户后自动填充
const onCustomerSelect = (value: string, displayName?: string) => {
  if (value === '__add_new__') {
    // 打开快速新增弹窗，带入表单已填写的信息（地址/目的国/贸易国），便于直接沉淀为常用客户
    quickAddCustomerName.value = displayName || formData.consigneeCompany || ''
    quickAddCustomerAddress.value = formData.consigneeAddress || ''
    quickAddDestinationCountry.value = formData.destinationCountry || ''
    quickAddTradeCountry.value = formData.tradeCountry || ''
    quickAddCustomerVisible.value = true
    return
  }
  const customer = customerList.value.find(c => c.customerName === value)
  if (customer) {
    formData.consigneeCompany = customer.customerName
    formData.consigneeAddress = customer.customerAddress || ''
    if (customer.destinationCountry) {
      formData.destinationCountry = getCountryCodeByName(customer.destinationCountry)
    }
    if (customer.tradeCountry) {
      formData.tradeCountry = getCountryCodeByName(customer.tradeCountry)
    }
  }
}

// 加载常用客户
const loadCustomers = async () => {
  try {
    const response = await getAllEnabledCustomers()
    if (response.data?.code === 200) {
      customerList.value = response.data.data || []
    }
  } catch (error) {
    console.warn('加载常用客户失败', error)
  }
}

// 乙方配置（仅用于单证乙方/销货方取值，不参与收货人自动填充）
const partyBList = ref<PartyBConfig[]>([])
const partyBOptions = computed(() =>
  partyBList.value.map(p => ({
    value: p.id,
    label: p.partyBName
  }))
)

// 加载乙方配置
const loadPartyBList = async () => {
  try {
    const response = await getAllEnabledPartyB()
    if (response.data?.code === 200) {
      partyBList.value = response.data.data || []
    }
  } catch (error) {
    console.warn('加载乙方配置失败', error)
  }
}

/**
 * 当前选中的乙方（用于单证销货方信息预览）
 * 后端 WriteLongAsString 把 id 下发为字符串，与 PartyBSelector 同口径按字符串比
 */
const selectedPartyB = computed(() => {
  const id = formData.partyBId
  if (id === undefined || id === null || id === '' || id === 0) return undefined
  return partyBList.value.find(p => String(p.id) === String(id))
})

/**
 * 销货方信息摘要片段
 * 字段名与值分开给，否则一排“111 | 15268034063 | 11”根本看不出哪段是地址、哪段是税号
 */
const partyBSummaryParts = computed(() => {
  const p = selectedPartyB.value
  if (!p) return [] as Array<{ label: string; value: string }>
  const list: Array<{ label: string; value?: string }> = [
    { label: '地址', value: p.partyBAddress },
    { label: '联系电话', value: p.contactPhone },
    { label: '纳税人识别号', value: p.taxId },
    { label: '开户银行', value: p.bankName },
    { label: '银行账号', value: p.bankAccount }
  ]
  return list.filter(item => !!item.value).map(item => ({ label: item.label, value: String(item.value) }))
})

/** 销货方信息摘要全文（悬浮提示用） */
const partyBSummary = computed(() =>
  partyBSummaryParts.value.map(item => `${item.label}：${item.value}`).join(' | ')
)

/** 乙方档案在申报页内维护后：刷新下拉，新增项自动选中 */
const handlePartyBSaved = async (payload: { name: string; isNew: boolean }) => {
  await loadPartyBList()
  if (payload.isNew && !formData.partyBId) {
    const created = partyBList.value.find(p => p.partyBName === payload.name)
    if (created?.id) formData.partyBId = created.id
  }
}

// 快速新增客户
const quickAddCustomerVisible = ref(false)
const quickAddCustomerName = ref('')
const quickAddCustomerAddress = ref('')
const quickAddDestinationCountry = ref('')
const quickAddTradeCountry = ref('')
const quickAddCustomerSaving = ref(false)

const handleQuickAddCustomer = async () => {
  if (!quickAddCustomerName.value.trim()) {
    message.warning('请输入客户公司名')
    return
  }
  if (!quickAddCustomerAddress.value.trim()) {
    message.warning('请输入收货人地址')
    return
  }
  if (!quickAddDestinationCountry.value) {
    message.warning('请选择目的国')
    return
  }
  if (!quickAddTradeCountry.value) {
    message.warning('请选择贸易国')
    return
  }
  quickAddCustomerSaving.value = true
  try {
    const response = await addCustomer({
      customerName: quickAddCustomerName.value.trim(),
      customerAddress: quickAddCustomerAddress.value.trim(),
      destinationCountry: getCountryEnglishName(quickAddDestinationCountry.value),
      tradeCountry: getCountryEnglishName(quickAddTradeCountry.value),
      status: 1
    })
    if (response.data?.code === 200) {
      message.success('客户添加成功')
      quickAddCustomerVisible.value = false
      // 重新加载客户列表
      await loadCustomers()
      // 自动选中新添加的客户
      const newCustomer = customerList.value.find(c => c.customerName === quickAddCustomerName.value.trim())
      if (newCustomer) {
        formData.consigneeCompany = newCustomer.customerName
        formData.consigneeAddress = newCustomer.customerAddress || ''
        if (newCustomer.destinationCountry) {
          formData.destinationCountry = getCountryCodeByName(newCustomer.destinationCountry)
        }
        if (newCustomer.tradeCountry) {
          formData.tradeCountry = getCountryCodeByName(newCustomer.tradeCountry)
        }
      }
    } else {
      message.error(response.data?.message || '添加失败')
    }
  } catch (error) {
    message.error('添加失败')
  } finally {
    quickAddCustomerSaving.value = false
  }
}
const returnReason = ref('')
const auditHistoryVisible = ref(false)
const auditHistoryList = ref<any[]>([])
const auditHistoryLoading = ref(false)

// 审核意见相关
const remarkModalVisible = ref(false)
const remarkAction = ref('') // '通过' 或 '驳回'
const remarkValue = ref('')
const remarkSubmitting = ref(false)
let handleRemarkSubmit = () => {} // 占位函数，会被 showRemarkModal 动态替换
const auditHistoryColumns = [
  { title: '状态', key: 'auditStatus', width: 70 },
  { title: '业务类型', key: 'businessType', width: 120 },
  { title: '申请人', dataIndex: 'applicantName', key: 'applicantName', width: 90 },
  { title: '原因', dataIndex: 'applyReason', key: 'applyReason', ellipsis: true, minWidth: 150 },
  { title: '申请时间', dataIndex: 'applyTime', key: 'applyTime', width: 160 , customRender: ({ text }: any) => text ? formatDate(text, 'yyyy-MM-dd HH:mm:ss') : '-' },
  { title: '审核人', dataIndex: 'auditorName', key: 'auditorName', width: 90 },
  { title: '备注', dataIndex: 'auditRemark', key: 'auditRemark', ellipsis: true, minWidth: 150 },
  { title: '审核时间', dataIndex: 'auditTime', key: 'auditTime', width: 160 , customRender: ({ text }: any) => text ? formatDate(text, 'yyyy-MM-dd HH:mm:ss') : '-' },
  { title: '原状态', key: 'preStatus', width: 70 }
]

// 活跃任务状态（用于任务驱动的 UI 判断）
const activeTasks = ref<any[]>([])
/** 当前用户可处理的任务 taskKey 集合（入口推断与审核 taskKey 兕底用，不再依赖 URL 参数） */
const myTaskKeys = ref<Set<string>>(new Set())
/** 推断出的待审核任务 taskKey（通用审核按钮提交时用，替代 URL 的 taskKey 参数） */
const inferredAuditTaskKey = ref<string | null>(null)

// 计量单位列表
const measurementUnits = ref<MeasurementUnit[]>([])

// 基本信息是否只读（审核模式、查看模式、水单提交模式、资料模式、资料审核模式、发票上传模式都只读；
// 补交草稿入口：仅资料区可上传补交增量，其它模块全部只读）
const isFormReadonly = computed(() => isReadonly.value || isAudit.value || isPaymentMode.value || isMaterialMode.value || isMaterialAuditMode.value || isExemptionAuditMode.value || isMaterialSupplementAuditMode.value || isInvoiceAuditMode.value || isInvoiceUploadMode.value || isSupplementMode.value || isSupplementAuditMode.value || isInvoiceAmountMode.value || isInvoiceAmountAuditMode.value || isSupplementDraftEntry.value)

// 运输方式是否锁定（新建弹窗预选后不可修改）
const transportModeLocked = ref(false)

// 当前审核阶段（从 URL taskKey 中获取）

// 获取当前审核阶段文本
const getAuditActionText = () => {
  if (formStatus.value === 11) return '退回'
  return '审核'
}

// 获取业务类型文本
const getBusinessTypeText = (type: string) => {
  const map: Record<string, string> = {
    'DECLARATION_RETURN': '退回草稿',
    'DECLARATION_AUDIT': '申报审核',
    'REMittance_AUDIT': '水单审核',
    'DELIVERY_ORDER_AUDIT': '提货单审核',
    'DECLARATION_SUBMIT': '申报提交',
    'DECLARATION_MATERIAL_AUDIT': '资料审核',
    'DECLARATION_SUPPLEMENT_AUDIT': '补充资料审核',
    'DECLARATION_INVOICE_AMOUNT_AUDIT': '开票金额审核',
    'DECLARATION_INVOICE_AUDIT': '业务发票审核'
  }
  return map[type] || type
}

// 获取状态文本
const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '草稿',
    1: '待初审',
    2: '待资料提交',
    3: '待资料审核',
    4: '待补充资料提交',
    5: '待补充资料审核',
    6: '待开票金额提交',
    7: '待开票金额审核',
    8: '待发票提交',
    9: '待发票审核',
    10: '已完成',
    11: '退回待审'
  }
  return statusMap[status] || '未知'
}

// 审核通过
const handleApprove = async () => {
  if (!formId.value) return
  
  // 显示审核意见输入框
  const remark = await showRemarkModal('通过', '已核对数据，通过')
  if (!remark) return // 用户取消
  
  submitting.value = true
  try {
    if (formStatus.value === 11) {
      // 退回申请审核通过
      console.log('执行退回申请审核通过:', { formId: formId.value, remark })
      await auditReturnToDraft(formId.value, { approved: true, remark })
      message.success('退回审核已通过，单据已重置为草稿')
    } else {
      // 普通业务审核通过：taskKey 优先 URL 显式参数（老链接兼容），其次用推断出的待审任务，都没有则由后端智能匹配
      const taskKey = (route.query.taskKey as string) || inferredAuditTaskKey.value || undefined
      console.log('执行审核通过操作:', { formId: formId.value, taskKey, result: 1, remark })
      await auditDeclaration(formId.value, 1, remark, taskKey)
      message.success(`${getAuditActionText()}已通过`)
      if (formStatus.value === 1) {
        message.info('全套单证已自动生成')
      }
    }
    // 提交/审核成功：停留当前详情页并刷新最新状态
    stayAndRefresh()
  } catch (error) {
    console.error('审核操作失败:', error)
    message.error('审批操作失败')
  } finally {
    submitting.value = false
  }
}

// 驳回
const handleReject = async () => {
  if (!formId.value) return
  
  // 显示审核意见输入框
  const remark = await showRemarkModal('驳回', '数据填写错误')
  if (!remark) return // 用户取消
  
  submitting.value = true
  try {
    if (formStatus.value === 11) {
      // 退回申请审核驳回
      console.log('执行退回申请审核驳回:', { formId: formId.value, remark })
      await auditReturnToDraft(formId.value, { approved: false, remark })
      message.success('退回审核已驳回，单据恢复原状态')
    } else {
      // 普通业务审核驳回：taskKey 兜底逻辑同审核通过
      const taskKey = (route.query.taskKey as string) || inferredAuditTaskKey.value || undefined
      console.log('执行驳回操作:', { formId: formId.value, taskKey, result: 2, remark })
      await auditDeclaration(formId.value, 2, remark, taskKey)
      message.success(`${getAuditActionText()}已驳回`)
    }
    // 提交/审核成功：停留当前详情页并刷新最新状态
    stayAndRefresh()
  } catch (error) {
    console.error('驳回操作失败:', error)
    message.error('审批操作失败')
  } finally {
    submitting.value = false
  }
}


// ========================================
// 申报资料相关逻辑（从 MaterialSubmitModal 迁移）
// ========================================

interface MaterialSchemaField {
  key: string
  label: string
  type: 'text' | 'number' | 'date' | 'select'
  required?: boolean
  options?: string[]
}

const MATERIAL_FIXED_KEYS = ['amount', 'currency', 'invoiceNo', 'invoiceDate']

const materialItems = ref<MaterialItem[]>([])
const materialLoading = ref(false)
const materialExpandedKeys = ref<(number | string)[]>([])
const materialRowSaving = ref(false)

const materialRowKey = (record: MaterialItem) => (record.id ?? `tpl-${record.templateId}`) as any

// ---------- 发票 PDF 金额解析状态 ----------
// 资料项 key -> 解析提示信息（用于展示失败原因 / 跳过解析的原因）
const materialPdfMessages = reactive<Record<string, { type: 'success' | 'warn' | 'info'; text: string }>>({})
// 已解析过的文件指纹缓存，避免同一文件重复上传解析

/** 资料项是否为发票类（根据数据库 invoiceMode 字段判断） */
const isInvoiceMaterial = (item: MaterialItem): boolean =>
  item.invoiceMode === 1

/** 生成文件指纹，用于去重解析 */
const buildFileSignature = (file: File): string => `${file.name}|${file.size}|${file.lastModified}`

interface ParsedCache {
  amount: number | null
  invoiceNo: string | null
  invoiceDate: string | null
  message: { type: 'success' | 'warn' | 'info'; text: string } | null
}
const parsedFileSignatures = new Map<string, ParsedCache>()

/**
 * 尝试解析发票 PDF 的金额，并自动回填到对应附件的结构化字段。
 * 非 PDF / 非发票类资料项，直接跳过。
 */
const tryParseInvoicePdf = async (file: File, record: MaterialItem) => {
  // 非发票类资料项，不解析
  if (!isInvoiceMaterial(record)) return
  // 非 PDF：提示 + 跳过
  if (file.type !== 'application/pdf') {
    const key = materialRowKey(record)
    materialPdfMessages[key] = { type: 'info', text: '图片类发票暂不支持自动识别金额，请手动核对' }
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
        const errText = data?.errorMsg || 'PDF 金额识别失败，请确保手动填写金额正确'
        parsedMsg = { type: 'warn', text: errText }
      }
      // 提取发票号和开票日期（无论金额是否成功都尝试）
      if (data?.invoiceNo) parsedInvoiceNo = data.invoiceNo
      if (data?.invoiceDate) parsedInvoiceDate = data.invoiceDate
    } catch (e) {
      parsedMsg = { type: 'warn', text: 'PDF 解析请求失败，请手动核对金额' }
    }
    parsedFileSignatures.set(signature, { amount: parsedAmt, invoiceNo: parsedInvoiceNo, invoiceDate: parsedInvoiceDate, message: parsedMsg })
  }

  // ---------- 自动回填发票号和开票日期（如果空就回填）----------
  if (parsedInvoiceNo || parsedInvoiceDate) {
    const currentRecord2 = materialItems.value.find((i) => materialRowKey(i) === materialRowKey(record))
    if (currentRecord2?.attachments?.length) {
      const latestAtt2 = currentRecord2.attachments[0]
      const patchNoDate: Partial<import('@/api/business/materialItem').MaterialAttachment> = {}
      if (parsedInvoiceNo && !latestAtt2.invoiceNo) {
        latestAtt2.invoiceNo = parsedInvoiceNo
        patchNoDate.invoiceNo = parsedInvoiceNo
      }
      if (parsedInvoiceDate && !latestAtt2.invoiceDate) {
        latestAtt2.invoiceDate = parsedInvoiceDate
        patchNoDate.invoiceDate = parsedInvoiceDate
      }
      if (Object.keys(patchNoDate).length > 0 && currentRecord2.id && latestAtt2.id) {
        updateMaterialAttachment(currentRecord2.id!, latestAtt2.id, patchNoDate).catch(() => {})
      }
    }
  }

  // ---------- 自动回填金额到对应附件 ----------
  if (parsedAmt == null || parsedAmt <= 0) return
  // 重新获取最新资料项（loadMaterialItems 后数组已刷新）
  const currentRecord = materialItems.value.find((i) => materialRowKey(i) === materialRowKey(record))
  if (!currentRecord || !currentRecord.attachments?.length) return
  // 找到最新上传的附件（后端按 createTime DESC 排序，第一个是最新的）
  const latestAtt = currentRecord.attachments[0]
  if (!latestAtt) return

  const currentAttAmt = Number(latestAtt.amount ?? 0)
  if (currentAttAmt <= 0) {
    // 用户尚未填写金额 → 自动回填并持久化
    latestAtt.amount = parsedAmt
    try {
      await updateMaterialAttachment(currentRecord.id!, latestAtt.id, { amount: parsedAmt })
      message.success(`已自动填入 PDF 识别金额 ¥${parsedAmt.toFixed(2)}`)
    } catch (e) {
      // 保存失败时仅保留解析提示
    }
  } else if (Math.abs(currentAttAmt - parsedAmt) > 0.009) {
    // 用户已填且与 PDF 不一致 → 提示
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

const materialColumns = [
  { title: '资料项', key: 'name', dataIndex: 'name' }
]

/** 核心资料项（排除非资料提交环节，多环节时任一环节参与资料提交即保留） */
const coreMaterialItems = computed(() =>
  materialItems.value.filter(isSubmitStageItem)
)
const materialRequiredCount = computed(() => coreMaterialItems.value.filter((i) => i.required === 1).length)
const materialUploadedCount = computed(() =>
  coreMaterialItems.value.filter((i) => i.required === 1 && i.status === 1).length
)
const materialProgressPercent = computed(() => {
  if (materialRequiredCount.value === 0) return coreMaterialItems.value.length === 0 ? 0 : 100
  return Math.round((materialUploadedCount.value / materialRequiredCount.value) * 100)
})

// ---------- 按环节（stage）分组资料项 ----------
const DEFAULT_STAGE: MaterialStage = 'MATERIAL_SUBMIT'
/** 项是否参与资料提交环节（stage 支持多环节逗号分隔，任一环节不在排除集合即参与） */
const isSubmitStageItem = (item: MaterialItem) =>
  splitStages(item.stage).some(s => !excludedStages.value.has(s))

// 动态环节列表（从 form_section 字典加载，fallback 为硬编码 MATERIAL_STAGES）
interface DynamicStage {
  value: string
  label: string
  templateStage: string
  sortOrder: number
}
const dynamicStages = ref<DynamicStage[]>(
  MATERIAL_STAGES.map(s => ({ value: s.value, label: s.label, templateStage: s.value, sortOrder: 0 }))
)
/** 非资料提交环节（基础资料、补充资料、发票等独立管理的环节） */
const excludedStages = computed<Set<string>>(() => {
  // 有 submitKey + templateStage 的是资料提交环节，其余为非资料提交环节
  const submitStages = dynamicStages.value.filter(s => !!s.templateStage)
  // 如果只有默认三环节（fallback），用硬编码排除
  if (submitStages.length <= 3 && dynamicStages.value.length <= 3) {
    return new Set(['BASIC', 'SUPPLEMENT', 'INVOICE'])
  }
  // 动态模式下，排除没有 submitKey 的环节（它们由独立组件管理）
  // 这里暂保留硬编码排除，因为基础资料、补充资料和发票有独立 UI
  return new Set(['BASIC', 'SUPPLEMENT', 'INVOICE'])
})

const loadMaterialStages = async () => {
  try {
    const res = await getEnabledDictItems('form_section')
    const items = res.data?.data || []
    const stages: DynamicStage[] = []
    for (const item of items) {
      if (!item.remark) continue
      try {
        const config = JSON.parse(item.remark)
        if (config.templateStage) {
          stages.push({
            value: config.templateStage,
            label: config.sectionTitle || item.itemLabel || item.itemValue,
            templateStage: config.templateStage,
            sortOrder: item.sortOrder ?? 99
          })
        }
      } catch { /* skip */ }
    }
    if (stages.length > 0) {
      stages.sort((a, b) => a.sortOrder - b.sortOrder)
      dynamicStages.value = stages
    }
  } catch {
    // 字典加载失败，保持 fallback
  }
}

const getStageItems = (stage: MaterialStage) =>
  materialItems.value.filter((i) => hasStage(i.stage, stage))

const activeStageTab = ref<string>(DEFAULT_STAGE)

/** 当前激活环节对应的资料项 */
const activeStageItems = computed(() =>
  getStageItems(activeStageTab.value as MaterialStage)
)

/** 有资料项的环节列表（控制 tab 显示） */
const availableStages = computed(() =>
  dynamicStages.value.filter((s) => !excludedStages.value.has(s.value) && getStageItems(s.value as MaterialStage).length > 0)
)

/** 每个环节的进度统计 */
const stageStats = computed(() => {
  const map: Record<string, { total: number; required: number; uploaded: number }> = {}
  for (const s of dynamicStages.value) {
    const items = getStageItems(s.value as MaterialStage)
    const req = items.filter((i) => i.required === 1).length
    const upl = items.filter((i) => i.required === 1 && i.status === 1).length
    map[s.value] = { total: items.length, required: req, uploaded: upl }
  }
  return map
})

// 资料模式下只读：URL readonly=true 或 状态大于 2 (资料提交后)
const isMaterialReadonly = computed(() => {
  if (route.query.readonly === 'true') return true
  if (formStatus.value != null && formStatus.value > 2) return true
  return false
})
// 资料模块可编辑条件：
// 1. 资料提交模式 + 状态=2 + 非只读 → MATERIAL_SUBMIT 环节可编辑
// 2. 发票上传模式 + 状态=4 + 非只读 + INVOICE 环节标签页 → 业务发票可编辑
const isMaterialEditable = computed(() => {
  if (isMaterialReadonly.value) return false
  if (isMaterialMode.value && formStatus.value === 2) return true
  return false
})


/** 补充资料环节资料项 */
const supplementItems = computed(() => getStageItems('SUPPLEMENT'))

/** 业务发票环节资料项 */
const invoiceStageItems = computed(() => getStageItems('INVOICE'))

/** 补充资料审核通过之后（业务 status > 5，即进入开票金额及后续环节） */
const isAfterSupplementStage = computed(() => {
  const s = formStatus.value
  return s != null && s > 5
})

/** 已进入补充资料之后的只读查阅模式（开票金额、发票提交等） */
const isPostSupplementReadonlyMode = computed(() =>
  isAfterSupplementStage.value
  || isInvoiceAmountMode.value
  || isInvoiceAmountAuditMode.value
  || isInvoiceUploadMode.value
  || isInvoiceAuditMode.value
)

/** 补充资料区域是否显示（补充资料流程时显示，且模板配置了该区块） */
const showSupplementSection = computed(() => {
  if (!hasSection('supplement')) return false
  if (supplementItems.value.length === 0) return false
  if (isSupplementMode.value && formStatus.value === 4) return true
  if (isSupplementAuditMode.value && formStatus.value === 5) return true
  if (isMaterialMode.value && formStatus.value != null && formStatus.value >= 4) return true
  if (isMaterialAuditMode.value && formStatus.value != null && formStatus.value >= 5) return true
  if (isPostSupplementReadonlyMode.value && formStatus.value != null && formStatus.value >= 4) {
    return true
  }
  return false
})

/** 业务发票区域是否显示（发票环节进行中、已完成查阅均展示；模板未配置跳过） */
const showInvoiceSection = computed(() => {
  if (!hasSection('invoice')) return false
  if (invoiceStageItems.value.length === 0) return false
  const s = formStatus.value
  if (s == null) return false
  // 发票上传/审核入口
  if (isInvoiceUploadMode.value && (s === 8 || s === 9)) return true
  if (isInvoiceAuditMode.value && s === 9) return true
  // 待提交/待审核（不限 mode，与 canSubmitInvoice / canAuditInvoice 一致）
  if (canSubmitInvoice.value || canAuditInvoice.value) return true
  // 已进入发票环节及之后：只读查阅（含 status=10 已完成）
  if (s >= 8 && isPostSupplementReadonlyMode.value) return true
  if (s >= 8 && route.query.readonly === 'true') return true
  if (s >= 10) return true
  return false
})

// ============================================================
// 统一资料管理组件 MaterialManager 配置
// ============================================================

/** 是否显示 MaterialManager（状态 >= 2 且配置了任一资料区块） */
const showMaterialManager = computed(() => {
  const s = formStatus.value
  if (s == null || s < 2) return false
  return hasSection('material') || hasSection('supplement') || hasSection('invoice')
})

/** MaterialManager 模式：仅审核类入口（审核列表/任务中心跳转）才渲染审核按钮；
 *  普通入口（提交人视角）即使状态处于待审也按 submit 渲染，避免提交人看到/操作自己提交资料的审核按钮 */
const materialManagerMode = computed<'submit' | 'audit'>(() => {
  if (isSupplementDraftEntry.value) return 'submit'
  return isAuditEntry.value ? 'audit' : 'submit'
})

/** 第二个 MaterialManager（开票金额及之后环节）的模式：补交草稿入口同样强制 submit，
 *  开放开票金额/业务发票等后置环节的补交增量上传；其它情况沿用正常规则 */
const postMaterialManagerMode = computed<'submit' | 'audit'>(() =>
  isSupplementDraftEntry.value ? 'submit' : materialManagerMode.value
)

/** MaterialManager 可操作性判断：从流程配置动态匹配 nodeKey → targetStatus */
const canOperateMaterialStage = (section: { submitKey?: string; auditTaskKey?: string }): boolean => {
  if (route.query.readonly === 'true' || isAudit.value) return false
  // 补交审核模式：仅补交审核操作可用，其它环节的提交/审核按钮一律隐藏
  if (isMaterialSupplementAuditMode.value) return false
  // 豁免审核入口：仅豁免审核操作可用，环节提交/审核按钮一律隐藏（豁免审核完成后也不例外，
  // 避免通过后 pendingExemption 解除时环节提交按钮重新出现）
  if (isExemptionAuditMode.value) return false
  // 豁免审批中：资料区域只读
  if (pendingExemption.value) return false
  const s = formStatus.value
  if (s == null) return false
  const map = stepStatusMap.value
  // 动态检查：当前状态是否命中该环节的提交节点 targetStatus
  if (section.submitKey && map.get(section.submitKey) === s) return true
  // 审核节点命中仅在审核入口生效（供审核通过/驳回按钮显隐）；
  // 提交人视角（submit 模式）不可复用审核节点命中，否则状态进入待资料审核（如豁免通过后自动推进）
  // 时，环节提交按钮会借 auditTaskKey 命中重新出现
  if (materialManagerMode.value === 'audit' && section.auditTaskKey && map.get(section.auditTaskKey) === s) return true
  return false
}

/** 补充资料进度统计 */
const supplementStats = computed(() => {
  const items = supplementItems.value
  const required = items.filter(i => i.required === 1).length
  const uploaded = items.filter(i => i.required === 1 && i.status === 1).length
  return { total: items.length, required, uploaded }
})

/** 业务发票进度统计 */
const invoiceStats = computed(() => {
  const items = invoiceStageItems.value
  const required = items.filter(i => i.required === 1).length
  const uploaded = items.filter(i => i.required === 1 && i.status === 1).length
  return { total: items.length, required, uploaded }
})

/** 状态=4 时可提交补充资料（非审核/只读场景） */
const canSubmitSupplement = computed(() => {
  if (formStatus.value !== 4) return false
  if (route.query.readonly === 'true') return false
  if (supplementDraftInProgress.value) return false
  if (isSupplementAuditMode.value || isMaterialAuditMode.value || isMaterialSupplementAuditMode.value || isAudit.value) return false
  return true
})

/** 状态=5 时可审核补充资料 */
const canAuditSupplement = computed(() => {
  if (formStatus.value !== 5) return false
  if (route.query.readonly === 'true') return false
  if (supplementDraftInProgress.value) return false
  if (isMaterialMode.value || isMaterialSupplementAuditMode.value) return false
  return true
})

/** 补充资料可编辑条件：与可提交一致 */
const isSupplementEditable = computed(() => canSubmitSupplement.value)

/** 状态=6 时可提交开票金额（不限 mode，与补充资料提交逻辑一致；模板未配置或自用申报跳过） */
const canSubmitInvoiceAmount = computed(() => {
  if (!hasSection('invoiceAmount')) return false
  if (formData.declarationType === 'SELF') return false
  if (formStatus.value !== 6) return false
  if (route.query.readonly === 'true') return false
  if (supplementDraftInProgress.value) return false
  if (isInvoiceAmountAuditMode.value || isInvoiceAuditMode.value || isInvoiceUploadMode.value) return false
  if (isMaterialAuditMode.value || isSupplementAuditMode.value || isMaterialSupplementAuditMode.value || isAudit.value) return false
  // 仅资料/补充/开票金额等业务入口，不因 URL 上残留的 mode 拦截
  return true
})

/** 状态=7 时可审核开票金额（模板未配置或自用申报跳过） */
const canAuditInvoiceAmount = computed(() => {
  if (!hasSection('invoiceAmount')) return false
  if (formData.declarationType === 'SELF') return false
  if (formStatus.value !== 7) return false
  if (route.query.readonly === 'true') return false
  if (supplementDraftInProgress.value) return false
  if (isMaterialMode.value || isSupplementMode.value || isMaterialSupplementAuditMode.value) return false
  return true
})

/** 申请开票金额区域：模板配置了该区块且补充资料审过后（status > 5）一律展示；自用申报隐藏 */
const showInvoiceAmountSection = computed(() => hasSection('invoiceAmount') && formData.declarationType !== 'SELF' && isAfterSupplementStage.value)

/** 申请开票金额可编辑（刷新计算、提交前确认） */
const isInvoiceAmountEditable = computed(() => canSubmitInvoiceAmount.value)

/** 开票金额计算详情数据 */
const invoiceAmountCalcDetail = ref<Record<string, any> | null>(null)
const invoiceAmountLoading = ref(false)

/** 支出合计 */
const calcExpenseTotal = computed(() => {
  const d = invoiceAmountCalcDetail.value
  if (!d) return 0
  return Number(d.totalInvoiceDeduction || 0) + Number(d.bankFeeAmount || 0) + Number(d.internalBankFee || 0)
})
/** 关联水单列表 */
const invoiceAmountRemittances = ref<any[]>([])
const remittanceColumns = [
  { title: '水单编号', dataIndex: 'remittanceNo', key: 'remittanceNo', width: 140 },
  { title: '收汇名称', dataIndex: 'remittanceName', key: 'remittanceName', width: 120, ellipsis: true },
  { title: '收汇金额', key: 'remittanceAmount', width: 130, customRender: ({ record }: any) => {
    const c = record.currency || 'USD'
    const sym = c === 'CNY' ? '¥' : c === 'USD' ? '$' : c
    return `${sym}${Number(record.remittanceAmount || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
  }},
  { title: '关联金额', key: 'relationAmount', width: 130, customRender: ({ record }: any) => {
    const c = record.currency || 'USD'
    const sym = c === 'CNY' ? '¥' : c === 'USD' ? '$' : c
    return `${sym}${Number(record.relationAmount || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
  }},
  { title: '汇率', dataIndex: 'taxRate', key: 'taxRate', width: 80, customRender: ({ text }: any) => text != null ? Number(text).toFixed(4) : '-' },
  { title: '收汇日期', dataIndex: 'remittanceDate', key: 'remittanceDate', width: 110, customRender: ({ text }: any) => text ? String(text).split(' ')[0] : '-' },
  { title: '状态', key: 'status', width: 80, customRender: ({ text }: any) => text === 0 ? '草稿' : text === 1 ? '待审核' : '已审核' }
]

/** 状态=8 时可提交业务发票（不限 mode；模板未配置跳过） */
const canSubmitInvoice = computed(() => {
  if (!hasSection('invoice')) return false
  if (formStatus.value !== 8) return false
  if (route.query.readonly === 'true') return false
  if (supplementDraftInProgress.value) return false
  if (isInvoiceAuditMode.value || isMaterialAuditMode.value || isSupplementAuditMode.value || isMaterialSupplementAuditMode.value || isAudit.value) return false
  return true
})

/** 状态=9 时可审核业务发票（模板未配置跳过） */
const canAuditInvoice = computed(() => {
  if (!hasSection('invoice')) return false
  if (formStatus.value !== 9) return false
  if (route.query.readonly === 'true') return false
  if (supplementDraftInProgress.value) return false
  if (isMaterialMode.value || isSupplementMode.value || isMaterialSupplementAuditMode.value) return false
  return true
})

/** 业务发票可编辑条件：状态=8 且可提交 */
const isInvoiceEditable = computed(() => canSubmitInvoice.value)

const parseMaterialSchema = (schema?: string | null): MaterialSchemaField[] => {
  if (!schema) return []
  try {
    const arr = JSON.parse(schema)
    return Array.isArray(arr) ? (arr as MaterialSchemaField[]) : []
  } catch {
    return []
  }
}

const getMaterialFieldValue = (record: MaterialItem, key: string): any => {
  if (MATERIAL_FIXED_KEYS.includes(key)) return (record as any)[key]
  if (!record.extraData) return undefined
  try {
    const obj = JSON.parse(record.extraData)
    return obj[key]
  } catch {
    return undefined
  }
}

const setMaterialFieldValue = (record: MaterialItem, key: string, val: any) => {
  if (MATERIAL_FIXED_KEYS.includes(key)) {
    ;(record as any)[key] = val == null || val === '' ? null : val
    return
  }
  let obj: any = {}
  if (record.extraData) {
    try {
      obj = JSON.parse(record.extraData)
    } catch {
      obj = {}
    }
  }
  if (val == null || val === '') delete obj[key]
  else obj[key] = val
  record.extraData = Object.keys(obj).length ? JSON.stringify(obj) : null
}

const loadMaterialItems = async () => {
  // 未保存草稿：加载 BASIC 环节启用模板作为虚拟项预览（上传时会自动保存草稿后再落库）
  if (!formId.value) {
    try {
      materialLoading.value = true
      const res = await getMaterialTemplatePreview('BASIC')
      if (res.data?.code === 200) {
        materialItems.value = (res.data.data || []).map((t: any) => ({
          formId: '',
          templateId: t.id,
          code: t.code,
          name: t.name,
          required: t.required,
          requiredStages: t.requiredStages,
          sort: t.sort ?? 0,
          remark: t.remark,
          formSchema: t.formSchema,
          stage: t.stage,
          invoiceMode: t.invoiceMode,
          status: 0
        })).sort((a: MaterialItem, b: MaterialItem) => (a.sort ?? 0) - (b.sort ?? 0))
      }
    } catch { /* 静默：预览失败不阻断表单填写 */ } finally {
      materialLoading.value = false
    }
    return
  }
  try {
    materialLoading.value = true
    const res = await getMaterialItems(formId.value)
    if (res.data?.code === 200) {
      materialItems.value = (res.data.data || []).slice().sort(
        (a: MaterialItem, b: MaterialItem) => (a.sort ?? 0) - (b.sort ?? 0)
      )
      materialExpandedKeys.value = materialItems.value
        .filter((i) => parseMaterialSchema(i.formSchema).length > 0)
        .map((i) => (i.id ?? `tpl-${i.templateId}`) as any)
    }
  } catch (e) {
    message.error('加载资料项失败')
  } finally {
    materialLoading.value = false
  }
}

// ==================== 豁免审批 ====================
const pendingExemption = ref<any>(null)
const exemptionTaskInfo = ref<any>(null)
const exemptionHistory = ref<any[]>([])
const exemptionHistoryModalVisible = ref(false)
/** 豁免状态是否已加载完成：加载完成前不渲染依赖豁免状态的按钮，避免异步窗口期误显示 */
const exemptionLoaded = ref(false)

const loadExemptionStatus = async () => {
  if (!formId.value) {
    exemptionLoaded.value = true
    return
  }
  try {
    const res = await getExemptionList(formId.value)
    if (res.data?.code === 200) {
      const list = res.data.data || []
      exemptionHistory.value = list
      pendingExemption.value = list.find((e: any) => e.status === 0) || null
      // 加载当前流程步骤信息
      if (pendingExemption.value) {
        const taskRes = await getExemptionCurrentTask(pendingExemption.value.id)
        if (taskRes.data?.code === 200) {
          exemptionTaskInfo.value = taskRes.data.data
        }
      } else {
        exemptionTaskInfo.value = null
      }
    }
  } catch { /* silent */ } finally {
    exemptionLoaded.value = true
  }
}

const parseExemptionMissingItems = (exemption: any): any[] => {
  if (!exemption?.missingItems) return []
  try {
    return typeof exemption.missingItems === 'string'
      ? JSON.parse(exemption.missingItems)
      : exemption.missingItems
  } catch { return [] }
}

const handleExemptionAudit = (approved: boolean) => {
  if (!pendingExemption.value) return

  if (approved) {
    // 通过：直接确认
    Modal.confirm({
      title: '确认通过豁免审核？',
      content: '通过后主流程将继续推进。',
      okText: '确认通过',
      onOk: async () => {
        try {
          submitting.value = true
          const res = await auditExemption({ id: pendingExemption.value.id, result: 1, remark: '' })
          if (res.data?.code === 200) {
            message.success('豁免审核通过')
            stayAndRefresh()
          } else {
            message.error(res.data?.message || '操作失败')
          }
        } catch (e) {
          message.error('操作失败')
        } finally {
          submitting.value = false
        }
      }
    })
  } else {
    // 驳回：弹窗输入原因
    let remark = ''
    Modal.confirm({
      title: '确认驳回豁免申请？',
      content: () => h('div', [
        h('div', { style: 'margin-bottom:8px;color:#d46b08;' }, '驳回后申报人需补充资料后重新提交豁免申请。'),
        h(Textarea, {
          rows: 3,
          maxlength: 500,
          placeholder: '请输入驳回原因（必填）',
          'onUpdate:value': (v: string) => { remark = v }
        })
      ]),
      okText: '确认驳回',
      okButtonProps: { danger: true },
      onOk: async () => {
        if (!remark.trim()) {
          message.warning('请填写驳回原因')
          return Promise.reject()
        }
        try {
          submitting.value = true
          const res = await auditExemption({ id: pendingExemption.value.id, result: 2, remark })
          if (res.data?.code === 200) {
            message.success('豁免已驳回')
            stayAndRefresh()
          } else {
            message.error(res.data?.message || '操作失败')
          }
        } catch (e) {
          message.error('操作失败')
        } finally {
          submitting.value = false
        }
      }
    })
  }
}

const saveMaterialRowFields = async (record: MaterialItem) => {
  if (!isMaterialEditable.value) return
  try {
    // 虚拟项（id 为空）先升格为真实记录
    const id = await resolveMaterialItemId(record)
    if (!id) return
    const res = await updateMaterialItem({
      id,
      name: record.name,
      required: record.required,
      sort: record.sort,
      remark: record.remark,
      amount: record.amount,
      currency: record.currency,
      invoiceNo: record.invoiceNo,
      invoiceDate: record.invoiceDate,
      extraData: record.extraData
    })
    if (res.data?.code === 200) {
      record.id = id
      message.success('已保存')
      await loadMaterialItems()
    } else {
      message.error(res.data?.message || '保存失败')
    }
  } catch (e) {
    message.error('保存失败')
  }
}

const beforeMaterialUpload = async (file: File, record: MaterialItem, stage?: string) => {
  try {
    // 未保存草稿：先自动保存拿到 formId，再继续上传（方案3）
    // 注意：此处延迟 URL 同步——router.replace 会因 fullPath 变化触发整页重新挂载，
    // 若在上传前替换 URL，新组件挂载时附件尚未落库，造成“文件没加载出来”
    if (!formId.value) {
      await handleSaveDraft({ deferUrlSync: true })
      if (!formId.value) return false
    }
    const id = await resolveMaterialItemId(record)
    if (!id) return false
    // 同时传 formId + templateId：后端在 id 找不到实例时会自动按模板 ensure 一条再上传
    const res = await uploadMaterialFile(id, file, {
      formId: formId.value,
      templateId: record.templateId ?? null,
      // 记录上传时所处环节，用于跨环节删除保护
      uploadStage: stage ?? null,
      // 补交模式：新附件打补交增量标记（基础资料框补交上传）
      supplementId: preMaterialManagerRef.value?.activeSupplement?.id ?? null
    })
    if (res.data?.code === 200) {
      if (res.data.data?.id) record.id = res.data.data.id
      message.success('上传成功')
      await loadMaterialItems()
      // 发票类资料项：触发 PDF 金额解析（非阻塞，后台异步执行）
      if (isInvoiceMaterial(record)) {
        tryParseInvoicePdf(file, record).catch(() => { /* 静默处理，已在 tryParseInvoicePdf 内部展示提示 */ })
      }
    } else {
      message.error(res.data?.message || '上传失败')
    }
  } catch (e) {
    message.error('上传失败')
  } finally {
    // 自动保存场景：上传结束后再补 URL 同步（重新挂载时附件已落库，可正常展示）
    syncDraftUrl()
  }
  return false
}

/** 将新建草稿的 id 同步到 URL（延迟同步场景专用，已同步过则跳过） */
const syncDraftUrl = () => {
  if (formId.value && String(route.query.id ?? '') !== String(formId.value)) {
    // 只回填 id（刷新/分享需要）；status 不再写入 URL，表单页以后端实时状态为准
    router.replace({
      path: route.path,
      query: { ...route.query, id: formId.value }
    })
  }
}

/**
 * 将虚拟项（id=null, templateId 非空）升格为真实记录。返回真实 id。
 * 对手动新增的项（已有 id）直接返回原 id。
 */
const resolveMaterialItemId = async (record: MaterialItem): Promise<number | string | null> => {
  if (record.id) return record.id
  if (!record.templateId || !formId.value) {
    message.error('无法定位资料项模板')
    return null
  }
  try {
    const res = await ensureMaterialItem(formId.value, record.templateId)
    // 调试：记录 ensure 返回，方便定位“资料项不存在”类问题
    // eslint-disable-next-line no-console
    console.log('[material ensure] response =', res?.data, 'templateId =', record.templateId)
    if (res.data?.code === 200 && res.data.data?.id) {
      record.id = res.data.data.id
      return record.id as number
    }
    message.error(res.data?.message || '创建资料项失败')
    return null
  } catch (e) {
    // eslint-disable-next-line no-console
    console.error('[material ensure] error', e)
    message.error('创建资料项失败')
    return null
  }
}

/** 删除单个附件（stage：当前操作环节，后端据此拦截前序环节上传的附件） */
const handleDeleteAttachment = async (record: MaterialItem, att: MaterialAttachment, stage?: string) => {
  try {
    const res = await deleteMaterialAttachment(record.id!, att.id, stage)
    if (res.data?.code === 200) {
      message.success('已删除')
      await loadMaterialItems()
    } else {
      message.error(res.data?.message || '删除失败')
    }
  } catch (e) {
    message.error('删除失败')
  }
}

/** 保存附件的结构化字段（金额/发票号/开票日期） */
const saveAttachmentField = async (record: MaterialItem, att: MaterialAttachment, field: string, value: any) => {
  if (!record.id || !att.id) return
  // 先更新本地数据
  ;(att as any)[field] = value ?? null
  try {
    const payload: Partial<MaterialAttachment> = { [field]: value ?? null }
    const res = await updateMaterialAttachment(record.id, att.id, payload)
    if (res.data?.code !== 200) {
      message.error(res.data?.message || '保存失败')
    }
  } catch (e) {
    message.error('保存失败')
  }
}

// openAddMaterialRow / openEditMaterialRow / handleSaveMaterialRow / resetMaterialRowForm
// 资料行自定义功能暂未启用
/* unused */

const validateMaterialSchemaFields = (): string | null => {
  for (const item of materialItems.value) {
    // 跳过非资料提交环节（多环节时任一环节参与资料提交即校验）
    if (!isSubmitStageItem(item)) continue
    const schema = parseMaterialSchema(item.formSchema)
    if (!schema.length) continue
    const isInvoice = isInvoiceMaterial(item)
    for (const f of schema) {
      if (!f.required) continue
      // 发票类资料项的固定字段已移至附件级别，跳过
      if (isInvoice && MATERIAL_FIXED_KEYS.includes(f.key)) continue
      const v = getMaterialFieldValue(item, f.key)
      if (v == null || v === '') {
        return `资料「${item.name}」的「${f.label}」为必填项`
      }
    }
  }
  return null
}

const handleSubmitMaterial = async () => {
  if (!formId.value) return
  // 只校验资料提交阶段的项，不包含非资料提交环节（必填按资料提交环节判定）
  const submitItems = materialItems.value.filter(isSubmitStageItem)
  const missing = submitItems.filter((i) => isItemRequiredInStage(i, 'MATERIAL_SUBMIT') && i.status !== 1)

  const schemaMissing = validateMaterialSchemaFields()
  if (schemaMissing) {
    message.warning(schemaMissing)
    return
  }

  // 检查发票类资料项每个附件是否填写了金额和发票号
  const invoiceFieldMissing: string[] = []
  for (const item of submitItems) {
    if (!isInvoiceMaterial(item)) continue
    if (!item.attachments?.length) continue
    for (let i = 0; i < item.attachments.length; i++) {
      const att = item.attachments[i]
      if (att.amount == null || Number(att.amount) < 0) {
        invoiceFieldMissing.push(`「${item.name}」第${i + 1}份附件未填写金额`)
      }
      if (!att.invoiceNo) {
        invoiceFieldMissing.push(`「${item.name}」第${i + 1}份附件未填写发票号`)
      }
    }
  }
  if (invoiceFieldMissing.length > 0) {
    message.warning(invoiceFieldMissing.join('、'))
    return
  }

  const doSubmit = async (skipRequiredCheck: boolean) => {
    try {
      submitting.value = true
      const res = await submitMaterial(formId.value!, skipRequiredCheck)
      if (res.data?.code === 200) {
        message.success(skipRequiredCheck ? '资料已提交，等待豁免审核' : '资料提交成功，等待审核')
        stayAndRefresh()
      } else {
        message.error(res.data?.message || '提交失败')
      }
    } catch (e) {
      message.error('提交失败')
    } finally {
      submitting.value = false
    }
  }

  if (missing.length > 0) {
    // 必填不全：弹确认框允许强制提交（走豁免流程）
    Modal.confirm({
      title: `还有 ${missing.length} 项必填资料未上传`,
      content: `缺失项：${missing.map((m) => m.name).join('、')}。\n确认提交？系统将创建豁免审批流程，审核通过后主流程继续。`,
      okText: '确认提交',
      cancelText: '取消',
      onOk: () => doSubmit(true)
    })
    return
  }

  Modal.confirm({
    title: '确认提交资料审核？',
    content: '提交后将进入资料审核流程，无法修改。',
    okText: '确认提交',
    onOk: () => doSubmit(false)
  })
}

// 资料审核通过
const handleMaterialAuditApprove = () => {
  if (!formId.value) return
  let remark = ''
  Modal.confirm({
    title: '确认通过资料审核？',
    content: () => h('div', [
      h('div', { style: 'margin-bottom:8px;color:#666;' }, '通过后流程将进入补充资料提交阶段。'),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: '请输入审核意见（可选）',
        'onUpdate:value': (v: string) => { remark = v }
      })
    ]),
    okText: '确认通过',
    onOk: async () => {
      try {
        submitting.value = true
        const res = await auditMaterial({ formId: formId.value!, result: 1, remark })
        if (res.data?.code === 200) {
          message.success('资料审核已通过')
          stayAndRefresh()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e) {
        message.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// 资料审核驳回
const handleMaterialAuditReject = () => {
  if (!formId.value) return
  let remark = ''
  Modal.confirm({
    title: '确认驳回资料审核？',
    content: () => h('div', [
      h('div', { style: 'margin-bottom:8px;color:#d46b08;' }, '驳回后申报人将重新提交资料。请填写驳回原因。'),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: '请输入驳回原因（必填）',
        'onUpdate:value': (v: string) => { remark = v }
      })
    ]),
    okText: '确认驳回',
    okButtonProps: { danger: true },
    onOk: async () => {
      if (!remark.trim()) {
        message.warning('驳回请填写审核意见')
        return Promise.reject()
      }
      try {
        submitting.value = true
        const res = await auditMaterial({ formId: formId.value!, result: 2, remark })
        if (res.data?.code === 200) {
          message.success('已驳回，申报人需重新提交资料')
          stayAndRefresh()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e) {
        message.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// ========================================
// 补充资料提交/审核逻辑
// ========================================

const handleSubmitSupplement = async () => {
  if (!formId.value) return
  const missing = supplementItems.value.filter((i) => isItemRequiredInStage(i, 'SUPPLEMENT') && i.status !== 1)
  if (missing.length > 0) {
    message.warning(`还有 ${missing.length} 项必填补充资料未上传：${missing.map((m) => m.name).join('、')}`)
    return
  }
  Modal.confirm({
    title: '确认提交补充资料审核？',
    content: '提交后将进入补充资料审核流程，无法修改。',
    okText: '确认提交',
    onOk: async () => {
      try {
        submitting.value = true
        const res = await submitSupplement(formId.value!)
        if (res.data?.code === 200) {
          message.success('补充资料提交成功，等待审核')
          stayAndRefresh()
        } else {
          message.error(res.data?.message || '提交失败')
        }
      } catch (e) {
        message.error('提交失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleSupplementAuditApprove = () => {
  if (!formId.value) return
  let remark = ''
  Modal.confirm({
    title: '确认通过补充资料审核？',
    content: () => h('div', [
      h('div', { style: 'margin-bottom:8px;color:#666;' }, '通过后流程将进入申请开票金额阶段。'),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: '请输入审核意见（可选）',
        'onUpdate:value': (v: string) => { remark = v }
      })
    ]),
    okText: '确认通过',
    onOk: async () => {
      try {
        submitting.value = true
        const res = await auditSupplement({ formId: formId.value!, result: 1, remark })
        if (res.data?.code === 200) {
          message.success('补充资料审核已通过')
          stayAndRefresh()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e) {
        message.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleSupplementAuditReject = () => {
  if (!formId.value) return
  let remark = ''
  Modal.confirm({
    title: '确认驳回补充资料？',
    content: () => h('div', [
      h('div', { style: 'margin-bottom:8px;color:#d46b08;' }, '驳回后申报人将重新提交补充资料。请填写驳回原因。'),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: '请输入驳回原因（必填）',
        'onUpdate:value': (v: string) => { remark = v }
      })
    ]),
    okText: '确认驳回',
    okButtonProps: { danger: true },
    onOk: async () => {
      if (!remark.trim()) {
        message.warning('驳回请填写审核意见')
        return Promise.reject()
      }
      try {
        submitting.value = true
        const res = await auditSupplement({ formId: formId.value!, result: 2, remark })
        if (res.data?.code === 200) {
          message.success('已驳回，申报人需重新提交补充资料')
          stayAndRefresh()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e) {
        message.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// ========================================
// 申请开票金额提交/审核逻辑
// ========================================

const loadInvoiceAmountDetail = async () => {
  if (!formId.value) return
  invoiceAmountLoading.value = true
  try {
    // 并发加载开票金额（瘦身接口，仅 invoiceAmount，供下载开票文件 20% 上限校验）和水单列表
    const [calcRes, remRes] = await Promise.all([
      getInvoiceAmountDetail(formId.value!),
      getRemittancesByFormId(formId.value!)
    ])
    if (calcRes.data?.code === 200) {
      invoiceAmountCalcDetail.value = calcRes.data.data || null
    } else {
      invoiceAmountCalcDetail.value = null
    }
    if (remRes.data?.code === 200) {
      invoiceAmountRemittances.value = remRes.data.data || []
    } else {
      invoiceAmountRemittances.value = []
    }
  } catch (e) {
    message.error('加载开票金额详情失败')
  } finally {
    invoiceAmountLoading.value = false
  }
}

/** 下载开票文件包（开票通知书 + 合同） */
const handleDownloadInvoicePackage = async () => {
  if (!formId.value) return
  try {
    const res = await exportInvoicePackage(formId.value)
    const downloadUrl = res.data?.data
    if (downloadUrl) {
      window.location.href = downloadUrl
      message.success('开票文件包下载中...')
    } else {
      message.warning('暂无计算数据')
    }
  } catch (e: any) {
    message.error('下载失败: ' + (e.message || '未知错误'))
  }
}

const handleSubmitInvoiceAmount = async () => {
  if (!formId.value) return
  Modal.confirm({
    title: '确认提交开票金额申请？',
    content: '提交后系统将自动计算开票金额并进入审核流程。请确保：1) 外汇水单已关联 2) 商品退税率已在商品配置中维护（未配置按 0% 计算）。',
    okText: '确认提交',
    onOk: async () => {
      try {
        submitting.value = true
        const res = await submitInvoiceAmount(formId.value!)
        if (res.data?.code === 200) {
          message.success('开票金额申请已提交，等待审核')
          stayAndRefresh()
        } else {
          message.error(res.data?.message || '提交失败')
        }
      } catch (e: any) {
        message.error(e?.message || '提交失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleInvoiceAmountAuditApprove = () => {
  if (!formId.value) return
  let remark = ''
  Modal.confirm({
    title: '确认通过开票金额审核？',
    content: () => h('div', [
      h('div', { style: 'margin-bottom:8px;color:#666;' }, '通过后流程将进入业务发票提交阶段。'),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: '请输入审核意见（可选）',
        'onUpdate:value': (v: string) => { remark = v }
      })
    ]),
    okText: '确认通过',
    onOk: async () => {
      try {
        submitting.value = true
        const res = await auditInvoiceAmount({ formId: formId.value!, result: 1, remark })
        if (res.data?.code === 200) {
          message.success('开票金额审核已通过')
          stayAndRefresh()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e) {
        message.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleInvoiceAmountAuditReject = () => {
  if (!formId.value) return
  let remark = ''
  Modal.confirm({
    title: '确认驳回开票金额？',
    content: () => h('div', [
      h('div', { style: 'margin-bottom:8px;color:#d46b08;' }, '驳回后申报人将重新提交开票金额申请。请填写驳回原因。'),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: '请输入驳回原因（必填）',
        'onUpdate:value': (v: string) => { remark = v }
      })
    ]),
    okText: '确认驳回',
    okButtonProps: { danger: true },
    onOk: async () => {
      if (!remark.trim()) {
        message.warning('驳回请填写审核意见')
        return Promise.reject()
      }
      try {
        submitting.value = true
        const res = await auditInvoiceAmount({ formId: formId.value!, result: 2, remark })
        if (res.data?.code === 200) {
          message.success('已驳回，申报人需重新提交开票金额申请')
          stayAndRefresh()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e) {
        message.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// ========================================
// 业务发票相关逻辑
// ========================================

// 发票审核通过
const handleInvoiceAuditApprove = () => {
  if (!formId.value) return
  let remark = ''
  Modal.confirm({
    title: '确认通过发票审核？',
    content: () => h('div', [
      h('div', { style: 'margin-bottom:8px;color:#666;' }, '通过后流程将进入已完成阶段。'),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: '请输入审核意见（可选）',
        'onUpdate:value': (v: string) => { remark = v }
      })
    ]),
    okText: '确认通过',
    onOk: async () => {
      try {
        submitting.value = true
        const res = await auditInvoice({ formId: formId.value!, result: 1, remark })
        if (res.data?.code === 200) {
          message.success('发票审核已通过')
          stayAndRefresh()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e) {
        message.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// 发票审核驳回
const handleInvoiceAuditReject = () => {
  if (!formId.value) return
  let remark = ''
  Modal.confirm({
    title: '确认驳回发票审核？',
    content: () => h('div', [
      h('div', { style: 'margin-bottom:8px;color:#d46b08;' }, '驳回后申报人需重新上传业务发票。请填写驳回原因。'),
      h(Textarea, {
        rows: 3,
        maxlength: 500,
        placeholder: '请输入驳回原因（必填）',
        'onUpdate:value': (v: string) => { remark = v }
      })
    ]),
    okText: '确认驳回',
    okButtonProps: { danger: true },
    onOk: async () => {
      if (!remark.trim()) {
        message.warning('驳回请填写审核意见')
        return Promise.reject()
      }
      try {
        submitting.value = true
        const res = await auditInvoice({ formId: formId.value!, result: 2, remark })
        if (res.data?.code === 200) {
          message.success('已驳回，申报人需重新提交发票')
          stayAndRefresh()
        } else {
          message.error(res.data?.message || '操作失败')
        }
      } catch (e) {
        message.error('操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleSubmitInvoice = async () => {
  if (!formId.value) return
  // 检查 INVOICE 阶段的资料项是否已上传附件
  const invoiceItems = getStageItems('INVOICE')
  const hasAttachment = invoiceItems.some(item => item.attachments && item.attachments.length > 0)
  if (invoiceItems.length === 0 || !hasAttachment) {
    message.warning('请至少上传一份业务发票附件后再提交')
    return
  }
  Modal.confirm({
    title: '确认提交发票审核？',
    content: '提交后将进入发票审核流程，无法修改。',
    okText: '确认提交',
    onOk: async () => {
      try {
        submitting.value = true
        const res = await submitInvoice(formId.value!)
        if (res.data?.code === 200) {
          message.success('发票提交成功，等待审核')
          stayAndRefresh()
        } else {
          message.error(res.data?.message || '提交失败')
        }
      } catch (e) {
        message.error('提交失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// 业务发票相关函数已废弃，统一使用资料项 INVOICE 环节

// 金额手动输入后，标记为用户修改
const handleAmountChange = (record: any) => {
  record.amountUserModified = true
}

// 数量或单价变化时，如果金额未锁定（未被用户手动修改过），自动计算金额
const handleQuantityOrPriceChange = (record: any) => {
  // 如果金额已被用户手动修改过（无论保存与否），不再自动计算
  if (!record.amountUserModified && !record.amountLocked) {
    const quantity = record.quantity || 0
    const unitPrice = record.unitPrice || 0
    record.amount = quantity * unitPrice
  }
}

// 发票上传/删除/提交函数已废弃，统一使用资料项 INVOICE 环节



const formData = reactive({
  formNo: '',
  entityId: undefined as number | undefined,
  // 后端 WriteLongAsString：Long 以字符串下发，回填后本页持有的是字符串 id
  partyBId: undefined as number | string | undefined,
  shipperCompany: 'NINGBO ZIYI TECHNOLOGY CO.,LTD',
  shipperAddress: 'XIUFENG, GAOQIAO TOWN, HAISHU DISTRICT, NINGBO, ZHEJIANG, CHINA',
  consigneeCompany: '',
  consigneeAddress: '',
  invoiceNo: '',
  transportMode: undefined as string | undefined,
  tradeTerm: undefined as string | undefined,
  miscFee: undefined as number | undefined,
  arrivalPort: '',
  paymentMethod: undefined as string | undefined,
  departureCity: '',
  departureCityChinese: '',
  departureCityEnglish: '',
  destinationCountry: undefined as string | undefined,
  tradeCountry: undefined as string | undefined,
  currency: undefined as string | undefined,
  declarationDate: undefined as Dayjs | undefined,
  declarationType: 'EXTERNAL' as string,
  templateCode: undefined as string | undefined,
  orgId: undefined as number | undefined
})

// 产品列表
const productList = ref<any[]>([])

// 箱子列表
const cartonList = ref<any[]>([])

// 图片上传限制
const ALLOWED_PHOTO_TYPES = ['image/jpeg', 'image/jpg', 'image/png']

// HS编码选项
const hsOptions = ref<any[]>([])

// 支付方式选项
const paymentMethodOptions = ref<any[]>([])

// 运输方式选项
const transportModeOptions = ref<any[]>([])

// 贸易方式选项
const tradeTermOptions = ref<any[]>([])

// 国家选项
const countryOptions = ref<any[]>([])

// // 国家自动完成选项
// const countryAutoCompleteOptions = computed(() => {
//   return countryOptions.value.map(option => ({
//     label: option.label,
//     value: option.value
//   }))
// })

// // 包含自定义选项的国家选项
// const countryOptionsWithCustom = computed(() => {
//   // 如果当前值不在标准选项中，添加为自定义选项
//   if (formData.destinationCountry && !countryOptions.value.some(opt => opt.value === formData.destinationCountry)) {
//     return [
//       {
//         label: formData.destinationCountry,
//         value: formData.destinationCountry,
//         isCustom: true
//       },
//       ...countryOptions.value
//     ];
//   }
//   return countryOptions.value;
// });

// 国家选择过滤函数
const filterCountrySelectOption = (input: string, option: any) => {
  if (!input) return true;
  const lowerInput = input.toLowerCase();
  return (
    (option.label && option.label.toLowerCase().includes(lowerInput)) ||
    (option.value && option.value.toLowerCase().includes(lowerInput))
  );
};


// 货币选项
// 城市选项
const cityOptions = ref<any[]>([])

// 加载城市信息
const loadCities = async (countryName?: string) => {
  if (!countryName) {
    // 如果没有指定国家，默认加载中国城市
    countryName = '中国'
  }
  
  try {
    const response = await getCitiesByCountry(countryName)
    if (response.data && response.data.code === 200) {
      cityOptions.value = response.data.data.map((city: any) => ({
        label: `${city.cityChineseName || city.cityName} (${city.cityEnglishName}), ${city.countryEnglishName}`,
        value: city.cityName || city.cityChineseName, // 使用中文城市名作为值
        cityChineseName: city.cityName || city.cityChineseName,
        cityEnglishName: city.cityEnglishName,
        countryName: city.countryName
      }))
      console.log('城市信息加载成功:', cityOptions.value.length + '个城市')
    } else {
      console.warn('城市信息加载失败:', response.data?.message || '未知错误')
      cityOptions.value = []
    }
  } catch (error) {
    console.error('加载城市信息失败:', error)
    cityOptions.value = []
  }
}

const currencyOptions = ref<any[]>([])

// 加载运输方式选项
const loadTransportModes = async () => {
  try {
    const response = await getEnabledTransportModes()
    if (response.data.code === 200 && response.data.data.length > 0) {
      transportModeOptions.value = response.data.data.map((item: any) => ({
        label: item.chineseName || item.name,
        value: item.name || item.name,
        code: item.code || item.name
      }))
      console.log('加载运输方式成功:', transportModeOptions.value)
    }
  } catch (error) {
    console.warn('加载运输方式失败:', error)
    // 使用默认运输方式
    transportModeOptions.value = [
      { label: '海运', value: 'SEA', code: 'SEA' },
      { label: '空运', value: 'AIR', code: 'AIR' },
      { label: '陆运', value: 'LAND', code: 'TRUCK' },
      { label: '快递', value: 'EXPRESS', code: 'EXPRESS' }
    ]
  }
}

// 加载贸易方式选项
const loadTradeTerms = async () => {
  try {
    const response = await getEnabledTradeTerms()
    if (response.data.code === 200 && response.data.data.length > 0) {
      tradeTermOptions.value = response.data.data.map((item: any) => ({
        label: item.code ? `${item.code} (${item.chineseName || item.name})` : (item.chineseName || item.name),
        value: item.code,
        groupName: item.groupName,
        transportModes: item.transportModes || []
      }))
      console.log('加载贸易方式成功:', tradeTermOptions.value)
    }
  } catch (error) {
    console.warn('加载贸易方式失败:', error)
  }
}

// 加载支付方式选项
const loadPaymentMethods = async () => {
  try {
    const response = await getEnabledPaymentMethods()
    if (response.data.code === 200 && response.data.data.length > 0) {
      paymentMethodOptions.value = response.data.data.map((item: any) => ({
        label: item.code ? `${item.code} (${item.chineseName || item.name})` : (item.chineseName || item.name),
        value: item.code || item.name
      }))
      console.log('加载支付方式成功:', paymentMethodOptions.value)
    }
  } catch (error) {
    console.warn('加载支付方式失败:', error)
    // 使用默认支付方式
    paymentMethodOptions.value = [
      { label: 'T/T (电汇)', value: 'T/T' },
      { label: 'L/C (信用证)', value: 'L/C' },
      { label: 'D/P (付款交单)', value: 'D/P' },
      { label: 'D/A (承兑交单)', value: 'D/A' }
    ]
  }
}

// 加载货币选项
const loadCurrencies = async () => {
  try {
    const response = await getEnabledCurrencies()
    if (response.data.code === 200 && response.data.data.length > 0) {
      currencyOptions.value = response.data.data.map((item: any) => ({
        label: `${item.currencyCode} - ${item.chineseName || item.currencyName}`,
        value: item.currencyCode
      }))
      // 新建表单时，默认使用配置中的第一个币种
      if (!formId.value && currencyOptions.value.length > 0) {
        formData.currency = currencyOptions.value[0].value
      }
      console.log('加载货币数据成功:', currencyOptions.value)
    } else {
      // 如果API失败，使用默认数据
      currencyOptions.value = [
        { label: 'USD - 美元', value: 'USD' },
        { label: 'EUR - 欧元', value: 'EUR' },
        { label: 'CNY - 人民币', value: 'CNY' }
      ]
      if (!formId.value && currencyOptions.value.length > 0) {
        formData.currency = currencyOptions.value[0].value
      }
    }
  } catch (error) {
    console.warn('加载货币数据失败:', error)
    // 使用默认数据作为后备
    currencyOptions.value = [
      { label: 'USD - 美元', value: 'USD' },
      { label: 'EUR - 欧元', value: 'EUR' },
      { label: 'CNY - 人民币', value: 'CNY' }
    ]
    if (!formId.value && currencyOptions.value.length > 0) {
      formData.currency = currencyOptions.value[0].value
    }
  }
}

// 加载国家选项
const loadCountries = async () => {
  try {
    const response = await getEnabledCountries()
    if (response.data.code === 200 && response.data.data.length > 0) {
      countryOptions.value = response.data.data.map((item: any) => ({
        label: `${item.chineseName} / ${item.englishName}`,  // 显示中英文名称
        value: item.countryCode,   // 使用国家代码作为值
        englishName: item.englishName,  // 保存英文全名用于提交时转换
        chineseName: item.chineseName   // 保存中文名称
      }))
      console.log('加载国家数据成功:', countryOptions.value)
    } else {
      // 如果 API 失败，使用默认数据
      countryOptions.value = [
        { label: 'China', value: 'CHN', englishName: 'China' },
        { label: 'United States', value: 'USA', englishName: 'United States' },
        { label: 'United Kingdom', value: 'GBR', englishName: 'United Kingdom' },
        { label: 'Germany', value: 'DEU', englishName: 'Germany' },
        { label: 'France', value: 'FRA', englishName: 'France' },
        { label: 'Japan', value: 'JPN', englishName: 'Japan' },
        { label: 'South Korea', value: 'KOR', englishName: 'South Korea' }
      ]
    }
  } catch (error) {
    console.error('加载国家数据失败:', error)
    // 使用默认数据作为后备
  }
}

// 加载计量单位列表
const loadMeasurementUnits = async () => {
  try {
    const response = await getActiveMeasurementUnits()
    if (response.data && response.data.code === 200) {
      measurementUnits.value = response.data.data || []
      console.log('加载计量单位成功:', measurementUnits.value.length + ' 个单位')
    }
  } catch (error) {
    console.error('加载计量单位失败:', error)
  }
}


// // 监听国家变化，动态加载对应的城市
// watch(() => formData.destinationCountry, async (newCountryCode) => {
//   if (newCountryCode && newCountryCode.trim() !== '') {
//     // 尝试找到国家的英文名称
//     const selectedCountry = countryOptions.value.find(country => country.value === newCountryCode);
//     if (selectedCountry) {
//       await loadCities(selectedCountry.englishName || selectedCountry.chineseName);
//     } else {
//       // 如果找不到对应的国家选项，尝试直接使用国家代码
//       await loadCities(newCountryCode);
//     }
//   } else {
//     // 清空城市选项
//     cityOptions.value = [];
//   }
// });

// // 国家输入变化处理函数
// const handleCountryInputChange = (value: string) => {
//   // 如果输入的值匹配某个国家的中文名或英文名，自动选择对应的国家
//   if (value) {
//     const matchedCountry = countryOptions.value.find(option => 
//       option.chineseName.toLowerCase().includes(value.toLowerCase()) ||
//       option.englishName.toLowerCase().includes(value.toLowerCase()) ||
//       option.label.toLowerCase().includes(value.toLowerCase())
//     );
    
//     if (matchedCountry) {
//       formData.destinationCountry = matchedCountry.value;
//     } else {
//       // 如果没有匹配项，可以考虑让用户输入自定义值
//       // 但在这里我们只接受有效的国家代码
//       // formData.destinationCountry = value; // 不推荐直接赋值用户输入
//     }
//   }
// };

// 根据国家代码获取英文全名
const getCountryEnglishName = (countryCode: string): string => {
  const country = countryOptions.value.find(item => item.value === countryCode);
  return country ? country.englishName : countryCode;
}

// 根据国家名称（英文/中文）反查国家代码；已是代码或找不到则原样返回
// 用于将存储层的英文全名转为表单下拉的 value(code)
const getCountryCodeByName = (name: string): string => {
  if (!name) return name;
  const country = countryOptions.value.find(
    (item: any) => item.value === name || item.englishName === name || item.chineseName === name
  );
  return country ? country.value : name;
}

// 根据国家代码获取中文全名
// const getCountryChineseName = (countryCode: string): string => {
//   const country = countryOptions.value.find(item => item.value === countryCode);
//   return country ? country.chineseName : countryCode;
// }

// 加载HS商品类型数据
const loadProductTypes = async () => {
  try {
    const response = await getProductTypes()
    console.log('加载HS商品类型数据:', response.data)
    if (response.data.code === 200 && response.data.data.length > 0) {
      hsOptions.value = response.data.data.map((item: any) => ({
        label: `${item.hsCode}`,
        value: item.hsCode,
        chineseName: item.chineseName,
        englishName: item.englishName
      }))
      console.log('加载HS商品类型成功:', hsOptions.value)
    } else {
      // 如果API失败，使用默认数据
      hsOptions.value = [
      ]
    }
  } catch (error) {
    console.warn('加载HS商品类型失败，使用默认数据:', error)
    // 使用默认数据
    hsOptions.value = [
    ]
  }
}

// 产品选项
const productOptions = computed(() => {
  return productList.value.map((item, index) => ({
    label: `${index + 1}. ${item.productName} (HS: ${item.hsCode || '未设置'})`,
    value: item.id
  }))
})

/**
 * 箱子产品选择变更时，同步 productDetails（数量/毛重/净重）
 * - 新增产品: 自动填充默认值（数量=产品总数量-其他箱子已分配量，毛重/净重=产品原始值）
 * - 已有产品: 保持用户手动设置的值不变
 * - 移除产品: 从 productDetails 中删除
 */
function syncProductDetails(carton: any, selectedProductIds: any) {
  const ids: number[] = Array.isArray(selectedProductIds) ? selectedProductIds : []
  if (!carton.productDetails) carton.productDetails = []
  
  // 当前已有的 productDetails map
  const existingMap = new Map<number, any>()
  carton.productDetails.forEach((d: any) => existingMap.set(d.productId, d))
  
  // 构建新的 productDetails
  const newDetails: any[] = []
  ids.forEach(pid => {
    if (existingMap.has(pid)) {
      // 保留已有设置
      newDetails.push(existingMap.get(pid))
    } else {
      // 新增产品，设置默认值
      const product = productList.value.find(p => p.id === pid)
      const defaultQty = Math.max(0, (product?.quantity || 0) - getAllocatedQuantity(pid, carton))
      const defaultGross = Math.max(0, ((product?.grossWeight || 0) - getAllocatedWeight(pid, 'grossWeight', carton)))
      const defaultNet = Math.max(0, ((product?.netWeight || 0) - getAllocatedWeight(pid, 'netWeight', carton)))
      newDetails.push({
        productId: pid,
        quantity: defaultQty,
        grossWeight: defaultGross > 0 ? parseFloat(defaultGross.toFixed(3)) : null,
        netWeight: defaultNet > 0 ? parseFloat(defaultNet.toFixed(3)) : null
      })
    }
  })
  carton.productDetails = newDetails
}

/** 计算其他箱子已分配的某产品数量 */
function getAllocatedQuantity(productId: number, excludeCarton: any): number {
  let total = 0
  cartonList.value.forEach(c => {
    if (c === excludeCarton) return
    c.productDetails?.forEach((d: any) => {
      if (d.productId === productId) total += (d.quantity || 0)
    })
  })
  return total
}

/** 计算其他箱子已分配的某产品重量（毛重/净重） */
function getAllocatedWeight(productId: number, field: 'grossWeight' | 'netWeight', excludeCarton: any): number {
  let total = 0
  cartonList.value.forEach(c => {
    if (c === excludeCarton) return
    c.productDetails?.forEach((d: any) => {
      if (d.productId === productId) total += (d[field] || 0)
    })
  })
  return total
}

/** 获取某产品在某箱子中的最大可分配数量 */
function getMaxQuantity(productId: number, carton: any): number {
  const product = productList.value.find(p => p.id === productId)
  const total = product?.quantity || 0
  const allocated = getAllocatedQuantity(productId, carton)
  return Math.max(0, total - allocated)
}

/** 获取某产品在某箱子中的最大可分配重量 */
function getMaxWeight(productId: number, field: 'grossWeight' | 'netWeight', carton: any): number {
  const product = productList.value.find(p => p.id === productId)
  const total = product?.[field] || 0
  const allocated = getAllocatedWeight(productId, field, carton)
  return Math.max(0, parseFloat((total - allocated).toFixed(3)))
}

/** 校验所有箱子产品分配是否超配或不足（提交时调用） */
function validateCartonProducts(): string[] {
  const errors: string[] = []

  // 校验产品主表：毛重必须大于净重
  productList.value.forEach((product) => {
    const name = product.productName || `产品${product.id}`
    const gw = Number(product.grossWeight) || 0
    const nw = Number(product.netWeight) || 0
    if (gw > 0 && nw > 0 && gw <= nw) {
      errors.push(`${name}: 毛重(${gw})必须大于净重(${nw})`)
    }
  })

  // 校验箱子产品明细：每项的毛重必须大于净重
  cartonList.value.forEach((carton, ci) => {
    if (!carton.productDetails?.length) return
    carton.productDetails.forEach((detail: any, _di: number) => {
      const gw = Number(detail.grossWeight) || 0
      const nw = Number(detail.netWeight) || 0
      if (gw > 0 && nw > 0 && gw <= nw) {
        const product = productList.value.find(p => p.id === detail.productId)
        const pName = product?.productName || `产品${detail.productId}`
        errors.push(`箱子${carton.cartonNo || (ci + 1)} - ${pName}: 毛重(${gw})必须大于净重(${nw})`)
      }
    })
  })

  // 按产品ID汇总所有箱子的分配量
  const allocationMap = new Map<number, { quantity: number, grossWeight: number, netWeight: number }>()
  
  cartonList.value.forEach((carton) => {
    if (!carton.productDetails?.length) return
    carton.productDetails.forEach((detail: any) => {
      if (!allocationMap.has(detail.productId)) {
        allocationMap.set(detail.productId, { quantity: 0, grossWeight: 0, netWeight: 0 })
      }
      const alloc = allocationMap.get(detail.productId)!
      // 明细值可能为字符串（后端 JSON/输入框），必须转数值后累加，否则字符串拼接导致 toFixed 报错
      alloc.quantity += (Number(detail.quantity) || 0)
      alloc.grossWeight += (Number(detail.grossWeight) || 0)
      alloc.netWeight += (Number(detail.netWeight) || 0)
    })
  })
  
  // 对比每个产品的总量（超配 + 不足）
  // 重量保留 3 位小数：多箱累加存在浮点尾差（如 85.38000000000001），直接比较会误报超配/不足
  const round3 = (v: number) => Math.round(v * 1000) / 1000
  productList.value.forEach((product) => {
    const alloc = allocationMap.get(product.id!)
    const name = product.productName || `产品${product.id}`
    const totalQty = round3(Number(product.quantity) || 0)
    const totalGross = round3(Number(product.grossWeight) || 0)
    const totalNet = round3(Number(product.netWeight) || 0)
    const allocQty = round3(alloc?.quantity || 0)
    const allocGross = round3(alloc?.grossWeight || 0)
    const allocNet = round3(alloc?.netWeight || 0)
    // 校验数量
    if (allocQty > totalQty) {
      errors.push(`${name}: 数量超配 (已分配${allocQty}/总量${totalQty})`)
    } else if (allocQty < totalQty) {
      errors.push(`${name}: 数量不足 (已分配${allocQty}/总量${totalQty})`)
    }
    // 校验毛重
    if (totalGross > 0) {
      if (allocGross > totalGross) {
        errors.push(`${name}: 毛重超配 (已分配${allocGross.toFixed(3)}/总量${totalGross.toFixed(3)})`)
      } else if (allocGross < totalGross) {
        errors.push(`${name}: 毛重不足 (已分配${allocGross.toFixed(3)}/总量${totalGross.toFixed(3)})`)
      }
    }
    // 校验净重
    if (totalNet > 0) {
      if (allocNet > totalNet) {
        errors.push(`${name}: 净重超配 (已分配${allocNet.toFixed(3)}/总量${totalNet.toFixed(3)})`)
      } else if (allocNet < totalNet) {
        errors.push(`${name}: 净重不足 (已分配${allocNet.toFixed(3)}/总量${totalNet.toFixed(3)})`)
      }
    }
  })
  
  return errors
}

// 产品自动完成选项
const productAutoCompleteOptions = computed(() => {
  // 从HS选项中提取产品信息，用于自动完成功能
  const options: { label: string; value: string }[] = [];
  
  hsOptions.value.forEach(option => {
    // 如果有中文和英文名称，分别添加选项
    if (option.chineseName && option.englishName) {
      options.push(
        { label: `${option.chineseName}`, value: option.chineseName },
        { label: `${option.englishName}`, value: option.englishName },
        { label: `${option.chineseName} / ${option.englishName}`, value: `${option.chineseName} / ${option.englishName}` }
      );
    } else if (option.englishName) {
      options.push({ label: `${option.englishName}`, value: option.englishName });
    } else {
      // 备选方案：从label中解析
      const labelParts = option.label.split(' - ');
      if (labelParts.length >= 2) {
        const namePart = labelParts.slice(1).join(' - ');
        options.push({ label: namePart, value: namePart });
      }
    }
  });
  
  return options;
});

// 包含自定义选项的产品自动完成选项
const productAutoCompleteOptionsWithCustom = computed(() => {
  const baseOptions = [...productAutoCompleteOptions.value];
  
  // 如果当前产品名称不在基础选项中，添加为自定义选项
  if (productList.value) {
    productList.value.forEach(product => {
      if (product.productName && 
          !baseOptions.some(opt => opt.value === product.productName)) {
        baseOptions.unshift({
          label: product.productName,
          value: product.productName,
        });
      }
    });
  }
  
  return baseOptions;
});

// 产品选项过滤函数 - 已移至 BasicInfoSection

// 产品表格列配置
const productColumns = [
  { title: '产品中文名', dataIndex: 'productChineseName', key: 'productChineseName', width: 120 },
  { title: '产品英文名', dataIndex: 'productEnglishName', key: 'productEnglishName', width: 120 },
  { title: 'HS编码', dataIndex: 'hsCode', key: 'hsCode', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 80 },
  { title: '毛重(KGS)', dataIndex: 'grossWeight', key: 'grossWeight', width: 100 },
  { title: '净重(KGS)', dataIndex: 'netWeight', key: 'netWeight', width: 100 },
  { title: '关联箱号', key: 'cartonInfo', width: 120 },
  { title: '金额', key: 'amount', width: 100 },
  { title: '产品照片', key: 'productPhoto', width: 100 },
  { title: '申报要素', key: 'declarationElements', width: 100 },
  { title: '操作', key: 'action', width: 80 }
]

// 箱子表格列配置
const cartonColumns = [
  { title: '箱号', dataIndex: 'cartonNo', key: 'cartonNo', width: 160 },
  { title: '类型', dataIndex: 'typeChinese', key: 'typeChinese', width: 100 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 100 },
  { title: '长(cm)', dataIndex: 'lengthCm', key: 'lengthCm', width: 90 },
  { title: '宽(cm)', dataIndex: 'widthCm', key: 'widthCm', width: 90 },
  { title: '高(cm)', dataIndex: 'heightCm', key: 'heightCm', width: 90 },
  { title: '单箱体积(CBM)', key: 'unitVolume', width: 120 },
  { title: '总体积(CBM)', dataIndex: 'volume', key: 'volume', width: 150 },
  { title: '产品选择(数量/毛重/净重)', key: 'selectedProducts', width: 500 },
  { title: '操作', key: 'action', width: 80 }
]

// 业务发票已合并至资料模块 INVOICE 环节，以下变量已废弃

// 计算总计
const totals = computed(() => {
  let totalQuantity = 0
  let totalGrossWeight = 0
  let totalNetWeight = 0
  let totalVolume = 0
  let totalAmount = 0
  
  productList.value.forEach(item => {
    totalQuantity += item.quantity || 0
    totalGrossWeight += (item.grossWeight || 0) 
    totalNetWeight += (item.netWeight || 0) 
    totalAmount += parseFloat(item.amount) || 0
  })
  
  // 箱子总体积直接累加（体积由单箱长宽高×数量自动计算，或手动填写）；全局体积保留4位小数
  cartonList.value.forEach(carton => {
    totalVolume += (carton.volume || 0)
  })
  totalVolume = Math.round(totalVolume * 10000) / 10000
  
  return {
    totalQuantity,
    totalGrossWeight,
    totalNetWeight,
    totalVolume,
    totalAmount
  }
})

// 单价和金额均由用户手动填写，不自动计算

// 处理单位变更
const handleUnitChange = (record: any) => {
  console.log('单位变更:', record.unitCode, record);
  // 根据 unitCode 查找对应的单位名称，同时设置 unit 和 unitCode
  const selectedUnit = findUnitByCode(measurementUnits.value, record.unitCode);
  if (selectedUnit) {
    record.unit = selectedUnit.unitNameEn;  // 设置英文名称
    // record.unitCode 已经由 v-model 自动设置了
  }
};

// 处理金额变更


// HS 编码变更处理
const onHsCodeChange = async (index: number, value: string | number) => {
  const stringValue = typeof value === 'string' ? value : String(value)
  const option = hsOptions.value.find(opt => opt.value === stringValue)
  
  if (option) {
    // 设置产品名称（英文名）
    const productName = option.label.split(' - ')[1]
    productList.value[index].productName = productName
    
    // 加载对应的申报要素
    try {
      const response = await getProductTypes()
      console.log('API响应数据:', response)
      
      // 检查响应数据结构
      let productTypes = []
      if (response.data && Array.isArray(response.data)) {
        productTypes = response.data
      } else if (response.data && response.data.data && Array.isArray(response.data.data)) {
        productTypes = response.data.data
      } else if (response.data && response.data.records && Array.isArray(response.data.records)) {
        productTypes = response.data.records
      }
      
      console.log('处理后的商品类型数据:', productTypes)
      
      const productType = productTypes.find((item: any) => item.hsCode === stringValue)
      console.log('匹配的商品类型:', productType)
      
      if (productType) {
        // 设置HS产品中文名
        if (productType.chineseName) {
          console.log('设置产品中文名:', productType.chineseName)
          productList.value[index].productChineseName = productType.chineseName
        } else {
          // 从选项标签中提取中文名（如果有）
          const chineseMatch = option.label.match(/^([^\s-]+.*?)\s*-\s*(.+)$/)
          if (chineseMatch && chineseMatch[1]) {
            productList.value[index].productChineseName = chineseMatch[1].trim()
          }
        }
        if (productType.englishName) {
          console.log('设置产品英文名:', productType.englishName)
          productList.value[index].productEnglishName = productType.englishName
        } else {
          // 从选项标签中提取英文名（如果有）
          const englishMatch = option.label.match(/-?\s*([^\s-]+?)\s*$/)
          if (englishMatch && englishMatch[1]) {
            productList.value[index].productEnglishName = englishMatch[1].trim()
          }
        }
        
        // 更新产品名称
        updateProductName(productList.value[index]);
        
        // 解析申报要素配置
        let elements = []
        if (productType.elements && productType.elements.length > 0) {
          elements = productType.elements
        } else if (productType.elementsConfig) {
          try {
            // 解析elementsConfig JSON字符串
            elements = JSON.parse(productType.elementsConfig)
            console.log('解析elementsConfig:', elements)
          } catch (parseError) {
            console.error('解析elementsConfig失败:', parseError)
            elements = []
          }
        }
        
        if (elements.length > 0) {
          // 初始化申报要素，空值自动填充为"无"
          productList.value[index].declarationElements = elements.map((element: any) => ({
            ...element,
            value: element.defaultValue || element.value || '无'
          }))
          console.log('设置申报要素:', productList.value[index].declarationElements)
          
          // 测试数据结构
          testDataStructure(productList.value[index].declarationElements)
        } else {
          productList.value[index].declarationElements = []
          console.log('该商品无申报要素配置')
        }
      } else {
        productList.value[index].declarationElements = []
        console.log('未找到匹配的商品类型')
      }
    } catch (error) {
      console.error('加载申报要素失败:', error)
      productList.value[index].declarationElements = []
    }
  }
}

// // 获取产品名称(通过ID)
// const getProductNameById = (id: number) => {
//   const product = productList.value.find(p => p.id === id)
//   return product ? product.productName : '未知产品'
// }

// 获取产品显示名称(带序号,用于查看模式)
const getProductDisplayById = (id: number) => {
  const index = productList.value.findIndex(p => p.id === id)
  const product = productList.value.find(p => p.id === id)
  if (!product) return '未知产品'
  const productIndex = index >= 0 ? index + 1 : '?'
  return `${productIndex}. ${product.productName}`
}

// 获取产品关联的箱子信息
const getProductCartonInfo = (product: any) => {
  return cartonList.value.filter(carton => 
    carton.selectedProducts && carton.selectedProducts.includes(product.id)
  )
}

// 产品照片上传前验证
const beforeProductPhotoUpload = async (file: any, productIndex: number) => {
  const isJPGorPNG = ALLOWED_PHOTO_TYPES.includes(file.type)
  if (!isJPGorPNG) {
    message.error('只能上传 JPG/JPEG/PNG 格式的图片!')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('上传图片大小不能超过 2MB!')
    return false
  }
  
  try {
    const res = await uploadFile(file, 'ProductPhoto')
    if (res.data && res.data.code === 200) {
      const attachment = res.data.data
      productList.value[productIndex].imageId = attachment.id
      productList.value[productIndex].productPhoto = attachment.fileUrl
      // 设置单个文件对象
      productList.value[productIndex].photoFile = {
        uid: '-1',
        name: file.name,
        status: 'done',
        url: attachment.fileUrl
      }
      message.success('产品图片上传成功')
    }
  } catch (error) {
    message.error('产品图片上传失败')
  }
  
  // 返回false阻止自动上传 (因为我们已经手动上传了)
  return false
}

// 移除产品照片
const handleRemoveProductPhoto = (productIndex: number) => {
  productList.value[productIndex].productPhoto = ''
  productList.value[productIndex].imageId = null
  productList.value[productIndex].photoFile = null
}

// 添加产品
const addProduct = () => {
  const newId = productList.value.length > 0 ? Math.max(...productList.value.map(p => p.id)) + 1 : 1
  productList.value.push({
    id: newId,
    productName: '',
    productChineseName: '',
    productEnglishName: '',
    hsCode: '',
    quantity: 1,
    unitCode: '01',  // 默认单位代码（个）
    unitPrice: 0,
    amount: 0, // 金额由用户手动填写
    amountLocked: 0, // 0-未锁定, 1-锁定
    amountUserModified: false, // 前端标记：用户是否手动修改过
    grossWeight: 0,
    netWeight: 0,
    cartons: 1,    // 默认 1 箱
    volume: 0,     // 默认 0
    imageId: null, // 产品图片 ID
    productPhoto: '', // 产品照片 URL
    photoFile: null, // 上传文件对象
    declarationElements: [] // 添加申报要素字段
  })
}

// 删除产品
const removeProduct = (index: number) => {
  productList.value.splice(index, 1)
}

// 添加箱子
const addCarton = () => {
  // 获取当前最大的箱子ID，确保编号连续
  let maxId = 0
  if (cartonList.value.length > 0) {
    maxId = Math.max(...cartonList.value.map(c => c.id))
  }
  const newId = maxId + 1
  // 箱号占位默认值：首箱即正确格式；非首箱由 BasicInfoSection 的增删监听立即按
  // “类型英文前缀+连续区间”全表重排覆盖
  
  cartonList.value.push({
    id: newId,
    cartonNo: 'CARTONS1',
    quantity: 1,
    lengthCm: null,
    widthCm: null,
    heightCm: null,
    volume: 0,
    typeChinese: '纸箱', // 默认类型
    typeEnglish: 'CARTONS', // 默认类型
    selectedProducts: [],
    productDetails: []
  })
  
  console.log('添加箱子:', { newId, cartonNo: `CTN${String(newId).padStart(3, '0')}`, totalCartons: cartonList.value.length })
}

// 删除箱子
const removeCarton = (index: number) => {
  const removedCarton = cartonList.value[index]
  cartonList.value.splice(index, 1)
  console.log('删除箱子:', { removedId: removedCarton.id, removedCartonNo: removedCarton.cartonNo, remainingCartons: cartonList.value.length })
}

// 更新产品名称，将其设置为中文名和英文名的组合
const updateProductName = (record: any) => {
  const chineseName = record.productChineseName || '';
  const englishName = record.productEnglishName || '';
  
  if (chineseName && englishName) {
    record.productName = `${chineseName} / ${englishName}`;
  } else if (chineseName) {
    record.productName = chineseName;
  } else if (englishName) {
    record.productName = englishName;
  }
}

// 出发城市选择变化
const onDepartureCityChange = (value: SelectValue) => {
  // 查找对应的城市信息并更新中文名和英文名字段
  if (value !== undefined && value !== null && typeof value === 'string') { // 确保值存在且为字符串类型再处理
    const selectedCity = cityOptions.value.find((city: any) => city.value === value);
    if (selectedCity) {
      formData.departureCity = value; // 更新英文格式的城市名称
      formData.departureCityChinese = selectedCity.cityChineseName;
      formData.departureCityEnglish = selectedCity.cityEnglishName;
    }
  } else {
    // 当选择被清空时，重置相关字段
    formData.departureCity = '';
    formData.departureCityChinese = '';
    formData.departureCityEnglish = '';
  }
}

// // 箱子类型选择变化
// const onCartonTypeChange = (record: any, value: string) => {
//   // 根据中文类型设置英文类型
//   if (value === '箱子') {
//     record.typeEnglish = 'Carton';
//   } else if (value === '托盘') {
//     record.typeEnglish = 'Pallet';
//   }
// }

// 返回列表
const goBack = () => {
  // 固定跳所属类型的申报录入列表：history.back 会落到来源页，可能串到其它类型或其它模块
  router.push(entryListPath.value)
}

// 提交/审核成功后：停留当前详情页，原地重新加载最新状态（不再跳回列表）
const stayAndRefresh = async () => {
  await loadData()
  // 同步刷新两个资料区实例（环节状态、补交单、审核按钮显隐均由其内部数据驱动）
  preMaterialManagerRef.value?.refresh()
  postMaterialManagerRef.value?.refresh()
}

// 两个 MaterialManager 实例引用，供原地刷新时同步重载内部数据
const preMaterialManagerRef = ref<InstanceType<typeof MaterialManager> | null>(null)
const postMaterialManagerRef = ref<InstanceType<typeof MaterialManager> | null>(null)
/** 审核类入口（审核人视角）：页面整体只读，禁止上传/提交资料（豁免审核除外，其资料只读由 pendingExemption 单独控制） */
const isAuditEntry = computed(() =>
  isAudit.value || isMaterialAuditMode.value || isMaterialSupplementAuditMode.value
  || isSupplementAuditMode.value || isInvoiceAmountAuditMode.value || isInvoiceAuditMode.value
)
/** 补交进行中（资料区状态）：开放基础资料（BASIC）框的补交上传；
 *  只读查看态、审核类入口、豁免审核入口与豁免审批中一律禁止（豁免中主流程阻塞，不允许补交） */
const basicSupplementActive = computed(() =>
  route.query.readonly !== 'true' && !isAuditEntry.value && !isExemptionAuditMode.value
  && !pendingExemption.value && !!preMaterialManagerRef.value?.supplementActive
)
/** 补交草稿入口（supplementDraft=1）：页头主流程提交/审核按钮隐藏，操作聚焦补交本身。
 *  其它入口（如开票金额提交）主流程照常——补交是独立流程，残留的草稿补交单不应劫持主流程入口 */
const supplementDraftInProgress = computed(() => isSupplementDraftEntry.value)

// 自动刷新：每 5s 静默探测最新状态/任务，仅在确有变化时整页重载
let autoRefreshTimer: number | null = null
let autoRefreshBusy = false
/** 单次轮询体：静默探测最新状态/任务，仅在确有变化时才整页刷新，避免频繁重载打扰用户 */
const runAutoRefreshTick = async () => {
  // 有未保存编辑时不重载：整页刷新会冲掉用户正在填的内容，
  // 并把脏标记连同内容一起清零，导致关闭标签时既不问也不提示
  if (!formId.value || submitting.value || autoRefreshBusy || formDirty.value) return
  autoRefreshBusy = true
  try {
    const res = await getDeclarationDetail(formId.value)
    const latest = res.data?.data
    if (!latest) return
    const latestStatus = Number(latest.status ?? 0)
    if (latestStatus !== Number(formStatus.value ?? 0)) {
      // 状态变化：整体重载（重算只读态、审批按钮、环节展示，含资料区实例）
      await stayAndRefresh()
    } else if (latestStatus >= 1 && latestStatus <= 9) {
      // 状态未变：静默比对活跃任务，仅任务清单变化时才重载（审批人变化/环节推进）
      const taskRes = await getActiveTasks(formId.value)
      const newTasks = taskRes.data?.data || []
      const sig = (arr: any[]) => arr.map((t: any) => t.taskKey).sort().join(',')
      if (sig(newTasks) !== sig(activeTasks.value)) {
        activeTasks.value = newTasks
        await stayAndRefresh()
      }
    }
    // 其余情况（草稿/已完成/退回等状态未变）：本轮静默跳过，不做任何重载
  } catch (e) {
    // 轮询失败静默忽略，不打扰用户
  } finally {
    autoRefreshBusy = false
  }
}
/** 页面从后台回到前台：立即补一次刷新（后台期间轮询被跳过，避免积压后连弹提示） */
const handleVisibilityChange = () => {
  if (!document.hidden) runAutoRefreshTick()
}
const startAutoRefresh = () => {
  if (autoRefreshTimer) clearInterval(autoRefreshTimer)
  autoRefreshTimer = window.setInterval(() => {
    // 页面在后台（切走标签页/锁屏）时跳过轮询：浏览器会节流定时器，
    // 回前台后积压补跳会连续发起请求，失败时堆一堆重复提示
    if (document.hidden) return
    runAutoRefreshTick()
  }, 5000)
  document.addEventListener('visibilitychange', handleVisibilityChange)
}
const stopAutoRefresh = () => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
  document.removeEventListener('visibilitychange', handleVisibilityChange)
}

// 保存草稿
const handleSaveDraft = async (options?: { deferUrlSync?: boolean }) => {
  const deferUrlSync = options?.deferUrlSync === true
  // 草稿保存：超配仅 warning 不阻断
  const cartonWarnings = validateCartonProducts()
  if (cartonWarnings.length > 0) {
    cartonWarnings.forEach(w => message.warning(w))
  }
  
  submitting.value = true
  try {
    // 将关联箱子的 cartons 和 volume 赋值到产品中，并确保单位完整
    productList.value.forEach(product => {
      // 保证单位信息完整
      if (!product.unit && product.unitCode) {
        const selectedUnit = findUnitByCode(measurementUnits.value, product.unitCode)
        if (selectedUnit) {
          product.unit = selectedUnit.unitNameEn || 'PCS'
        } else {
          product.unit = 'PCS'
        }
      }
      
      const relatedCarton = cartonList.value.find(carton => 
        carton.selectedProducts && carton.selectedProducts.includes(product.id)
      )
      if (relatedCarton) {
        product.cartons = relatedCarton.quantity || 1
        product.volume = relatedCarton.volume || 0
      }
    })
    
    // 构建箱子产品关联数据（使用 productDetails 中的数量/毛重/净重）
    const cartonProducts: Array<{cartonId: number, productId: number, quantity: number, grossWeight?: number | null, netWeight?: number | null}> = []
    cartonList.value.forEach(carton => {
      // 优先使用 productDetails，回退到 selectedProducts
      if (carton.productDetails && carton.productDetails.length > 0) {
        carton.productDetails.forEach((detail: any) => {
          cartonProducts.push({
            cartonId: carton.id,
            productId: detail.productId,
            quantity: detail.quantity || 0,
            grossWeight: detail.grossWeight ?? null,
            netWeight: detail.netWeight ?? null
          })
        })
      } else if (carton.selectedProducts && carton.selectedProducts.length > 0) {
        carton.selectedProducts.forEach((productId: number) => {
          const product = productList.value.find(p => p.id === productId)
          if (product) {
            cartonProducts.push({
              cartonId: carton.id,
              productId: productId,
              quantity: product.quantity,
              grossWeight: product.grossWeight ?? null,
              netWeight: product.netWeight ?? null
            })
          }
        })
      }
    })
    
    // 确保所有产品的金额都已计算，但保留手动输入的金额
    // 保存时：如果用户手动修改过金额，则永久锁定，下次进入不再自动计算
    productList.value.forEach(product => {
      product.amount = product.amount != null ? parseFloat(product.amount).toFixed(2) : '0.00'
      // 转换为后端格式: true -> 1, false -> 0
      product.amountLocked = (product.amountUserModified || product.amountLocked) ? 1 : 0
    })

    // 构建保存数据
    const draftData = {
      ...formData,
      // 关键：与提交一致，将国家代码转换为英文全名后再入库（避免草稿存成纯 code）
      destinationCountry: formData.destinationCountry ? getCountryEnglishName(formData.destinationCountry) : '',
      tradeCountry: formData.tradeCountry ? getCountryEnglishName(formData.tradeCountry) : '',
      id: formId.value,
      status: 0,
      totalQuantity: totals.value.totalQuantity,
      totalGrossWeight: totals.value.totalGrossWeight,
      totalNetWeight: totals.value.totalNetWeight,
      totalVolume: totals.value.totalVolume,
      totalAmount: totals.value.totalAmount,
      products: productList.value.map((product: any) => ({
        ...product,
        id: product.id ? Number(product.id) : undefined,  // 确保 ID 是数字类型
        imageId: product.imageId ? Number(product.imageId) : null,  // 确保 imageId 是数字类型
        productPhoto: product.productPhoto, // 显式包含图片 URL
        elementValues: (product.declarationElements || []).map((elem: any) => ({
          elementName: elem.label,
          elementValue: elem.value && elem.value.trim() ? elem.value : '无'
        }))
      })),
      cartons: cartonList.value.map(carton => ({
        ...carton,
        id: carton.id ? Number(carton.id) : undefined,  // 确保ID是数字类型
        formId: formId.value ? Number(formId.value) : undefined
      })),
      cartonProducts: cartonProducts // 添加箱子产品关联数据
    }
    
    console.log('保存草稿数据:', draftData)
    // 是否首次落库（新建）：新建单保存后按约定跳回申报录入列表
    const createdDraft = !formId.value
    const response = await saveDraft(draftData as any)
    
    if (response.data && response.data.code === 200) {
      const newDraftId = response.data.data
      message.success('草稿保存成功')
      
      console.log('新草稿ID:', newDraftId.formId)
      console.log('当前草稿ID:', formId.value)
      // 如果是新草稿，更新ID和URL，避免重复创建
      if (!formId.value) {
        console.log('更新草稿ID:', newDraftId.formId)
        formId.value = newDraftId.formId
        console.log('当前草稿ID:', formId.value)
        formData.formNo = newDraftId.formNo
        formStatus.value = 0
        // 不再改写本页地址：新建单由下方直接跳回申报录入列表；
        // 延迟同步场景（上传前自动保存、关闭标签守卫）保持原地址，避免 keep-alive 重建页面实例
      }
      // 刷新基础资料区“资料”框（保存后按申报单加载真实资料项实例）
      await loadMaterialItems()
      // 落库成功后清脏标记（上面两次写回都属于程序化变更）
      markSynced()
      if (createdDraft && !deferUrlSync) {
        router.push(entryListPath.value)
      }
      return true
    } else {
      message.error(response.data.message || '保存草稿失败')
    }
  } catch (error: any) {
    console.error('保存草稿失败:', error)
    message.error('保存失败: ' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
  return false
}

// ========== 标签页草稿保护 ==========
// 新建申报存在未保存内容时，关闭标签前由 layout 询问是否先保存为草稿
const formDirty = ref(false)
let dirtyTrackingOn = false
let unregisterTabGuard: (() => void) | null = null

/** 本实例所属表单路径：路径前缀即申报类型的权威口径（切走标签后 route 会变，只能取装载时的值） */
const ownFormPath = route.path

/**
 * 归属申报类型：表单路径前缀优先，其次单据字段（兼容 /declaration 老入口）
 * 标签徽标与“返回列表”去向都以此为准，避免梓熠/理德与集洛互相串页
 */
const ownDeclarationType = computed<DeclarationTabType>(() => {
  if (ownFormPath.startsWith('/declaration-self')) return 'SELF'
  if (ownFormPath.startsWith('/declaration-external')) return 'EXTERNAL'
  return formData.declarationType === 'SELF' ? 'SELF' : 'EXTERNAL'
})

/** 申报录入列表路由：两套申报各自一套菜单，跳错前缀等于跳错模块 */
const entryListPath = computed(() =>
  ownDeclarationType.value === 'SELF' ? '/declaration-self/entry' : '/declaration-external/entry'
)

/** 把已解析出的申报类型与发票号回写到所属标签：类型定徽标，发票号替代标签上的数据库编号 */
const applyTabMeta = () => {
  if (!isOwnRoute.value) return
  setTabMeta(getTabKey(route), {
    bizType: ownDeclarationType.value,
    invoiceNo: String(formData.invoiceNo || '').trim()
  })
}

// 发票号是手录字段，输一个字符就要同步到标签，否则标签上一直挂着无辨识度的 #id
watch(() => formData.invoiceNo, () => { applyTabMeta() })

watch([formData, productList, cartonList], () => {
  if (dirtyTrackingOn) formDirty.value = true
}, { deep: true })

/**
 * 标记“已与后端同步”：装载或保存成功后调用
 * 保存与装载会深度改写 formData/productList（金额、单位、资料区等），
 * 必须短延后一拍再清脏标记，否则会被深度监听误判为用户编辑
 */
const markSynced = () => {
  dirtyTrackingOn = false
  nextTick(() => {
    formDirty.value = false
    dirtyTrackingOn = true
  })
}

/**
 * 装载静默期收尾：只恢复跟踪，已经填过就不清脏
 * 固定延时到、期间用户已经开始录入时，无条件清脏会把未保存内容抹掉痕迹，
 * 表现为关闭标签时不再询问是否存草稿
 */
const endLoadSilence = () => {
  if (formDirty.value) {
    dirtyTrackingOn = true
    return
  }
  markSynced()
}

/** 本实例装载时的完整地址：表单页的 keep-alive 缓存键就由此派生，地址不再是我就说明我没在呈现 */
const ownFullPath = route.fullPath

/** 地址是否仍指向本实例：keep-alive 切走后 useRoute() 会指向别的页面，其派生值不可再用 */
const isOwnRoute = computed(() => route.fullPath === ownFullPath)

/**
 * 本页只读态快照
 * isFormReadonly 由全局 route 的 query 派生，本页被缓存（切走）后它会跟着当前呈现的那张单变化；
 * 脏判定若直接读它，就会出现“另一张新建单被判成只读、关闭时既不跳转也不弹窗”
 */
const ownReadonly = ref(false)
watch([isOwnRoute, isFormReadonly], () => {
  if (isOwnRoute.value) ownReadonly.value = isFormReadonly.value
}, { immediate: true })

/**
 * 新建未落库时的内容兜底判定
 * formDirty 靠深度监听维护，而装载静默期、模板默认值回填、子组件内部状态（资料/水单等）
 * 都可能让一笔改动没被记到，一漏记就是关闭时静默丢内容；
 * 只要还没存过草稿、页面上已有实质录入，就当作有未保存的东西
 * （发货公司/运输方式/申报类型等预置值不计入，避免新建空白单也弹提示）
 */
const hasUnsavedContent = () => {
  if (formId.value) return false
  return !!(formData.consigneeCompany || formData.consigneeAddress || formData.invoiceNo
    || formData.arrivalPort || formData.miscFee || formData.partyBId
    || productList.value.length || cartonList.value.length)
}

/** 按当前路由所属标签注册守卫（一个实例一份，带本实例自己的地址） */
const syncTabGuard = () => {
  unregisterTabGuard?.()
  unregisterTabGuard = registerTabGuard({
    tabKey: getTabKey(route),
    fullPath: route.fullPath,
    // 脏判定 = 非只读 且（监听记到的改动 或 新建单上已有实质内容）
    isDirty: () => !ownReadonly.value && (formDirty.value || hasUnsavedContent()),
    save: async () => {
      // 只读页无需保存（进来时本标签已置顶，ownReadonly 已是本页真实取值）
      if (ownReadonly.value) return true
      // 延迟 URL 同步：关闭标签场景下无需把地址改写为带 id，避免标签重建
      const ok = await handleSaveDraft({ deferUrlSync: true })
      // 保存未成功（校验不过/请求失败）时中止关闭，避免内容静默丢失
      return ok === true
    }
  })
}

// keep-alive 下标签可能在多个缓存实例间切换（同一张单的多个入口地址），
// 每次重新激活都重新注册，保证守卫始终属于标签当前呈现的那个实例；
// 地址改写会换缓存键并重挂载新实例，新实例在装载时就会把守卫挂到改写后的标签键上，无需在这里迁移
onActivated(() => {
  syncTabGuard()
  // 标签可能已从持久化快照重建，徽标与发票号需重新回写
  applyTabMeta()
})

// 提交申报
const handleSubmit = async () => {
  try {
    // 验证是否为只读模式(已提交的申报单)
    if (isReadonly.value) {
      message.warning('该申报单已提交，无法编辑')
      return
    }
    
    // 验证必填字段
    if (!formData.consigneeCompany) {
      message.error('请填写收货人公司名')
      return
    }
    
    if (!formData.consigneeAddress) {
      message.error('请填写收货人地址')
      return
    }
    if (!formData.destinationCountry) {
      message.error('请选择目的地国家')
      return
    }
    if (!formData.tradeCountry) {
      message.error('请选择贸易国家')
      return
    }
    if (!formData.transportMode) {
      message.error('请选择运输方式')
      return
    }
    // 贸易方式校验：C组和D组需要到达港口
    if (formData.tradeTerm) {
      const tradeTermOption = tradeTermOptions.value.find((opt: any) => opt.value === formData.tradeTerm)
      if (tradeTermOption && (tradeTermOption.groupName === 'C组' || tradeTermOption.groupName === 'D组')) {
        if (!formData.arrivalPort || !formData.arrivalPort.trim()) {
          message.error('选择C组或D组贸易方式时，必须填写到达港口')
          return
        }
      }
    }
    if (!formData.departureCity) {
      message.error('请选择出发口岸')
      return
    }
    if(!formData.currency){
      message.error('请选择货币')
      return
    }

    // 基础资料环节（新建申报）必填校验：必填不全直接拦截，不走豁免流程
    const missingBasic = (materialItems.value || []).filter(
      (i: MaterialItem) => hasStage(i.stage, 'BASIC') && isItemRequiredInStage(i, 'BASIC') && i.status !== 1
    )
    if (missingBasic.length > 0) {
      message.error(`还有 ${missingBasic.length} 项必填基础资料未上传：${missingBasic.map((m: MaterialItem) => m.name).join('、')}`)
      return
    }

    if (productList.value.length === 0) {
      message.error('请至少添加一个产品')
      return
    }    
    // 检查所有产品是否都关联了箱子（同时检查 productDetails 和 selectedProducts）
    const unassignedProducts = productList.value.filter(product => {
      return !cartonList.value.some(carton => {
        // 优先检查 productDetails（主要数据源），回退到 selectedProducts
        if (carton.productDetails && carton.productDetails.length > 0) {
          return carton.productDetails.some((d: any) => d.productId === product.id)
        }
        return carton.selectedProducts && carton.selectedProducts.includes(product.id)
      })
    })
    
    if(!formData.transportMode){
      message.error('请填写运输方式')
      return
    }
    if (unassignedProducts.length > 0) {
      const productNames = unassignedProducts.map(p => p.productName || '未命名产品').join('、')
      message.error(`以下产品未分配箱子: ${productNames}，请在箱子信息中选择关联产品`)
      return
    }
    
    // 校验箱子产品数量/毛重/净重分配不超总量
    const cartonErrors = validateCartonProducts()
    if (cartonErrors.length > 0) {
      cartonErrors.forEach(err => message.error(err))
      return
    }
    
    submitting.value = true
    
    // 确保所有产品的金额都已计算，但保留手动输入的金额
    productList.value.forEach(product => {
      // 直接使用用户填写的金额
      product.amount = product.amount != null ? parseFloat(product.amount).toFixed(2) : '0.00'
    })
    
    // 将关联箱子的 cartons 和 volume 赋值到产品中，并确保单位完整
    productList.value.forEach(product => {
      // 保证单位信息完整
      if (!product.unit && product.unitCode) {
        const selectedUnit = findUnitByCode(measurementUnits.value, product.unitCode)
        if (selectedUnit) {
          product.unit = selectedUnit.unitNameEn || 'PCS'
        } else {
          product.unit = 'PCS'
        }
      }
      
      const relatedCarton = cartonList.value.find(carton => 
        carton.selectedProducts && carton.selectedProducts.includes(product.id)
      )
      if (relatedCarton) {
        product.cartons = relatedCarton.quantity || 1
        product.volume = relatedCarton.volume || 0
      }
    })
    
    // 构建箱子产品关联数据（使用 productDetails 中的数量/毛重/净重）
    const cartonProducts: Array<{cartonId: number, productId: number, quantity: number, grossWeight?: number | null, netWeight?: number | null}> = []
    cartonList.value.forEach(carton => {
      // 优先使用 productDetails，回退到 selectedProducts
      if (carton.productDetails && carton.productDetails.length > 0) {
        carton.productDetails.forEach((detail: any) => {
          cartonProducts.push({
            cartonId: carton.id,
            productId: detail.productId,
            quantity: detail.quantity || 0,
            grossWeight: detail.grossWeight ?? null,
            netWeight: detail.netWeight ?? null
          })
        })
      } else if (carton.selectedProducts && carton.selectedProducts.length > 0) {
        carton.selectedProducts.forEach((productId: number) => {
          const product = productList.value.find(p => p.id === productId)
          if (product) {
            cartonProducts.push({
              cartonId: carton.id,
              productId: productId,
              quantity: product.quantity,
              grossWeight: product.grossWeight ?? null,
              netWeight: product.netWeight ?? null
            })
          }
        })
      }
    })
    
    // 构造提交数据
    const submitData = {
      ...formData,
      // 关键修复：将国家代码转换为英文全名
      destinationCountry: formData.destinationCountry ? getCountryEnglishName(formData.destinationCountry) : '',
      tradeCountry: formData.tradeCountry ? getCountryEnglishName(formData.tradeCountry) : '',
      totalQuantity: totals.value.totalQuantity,
      totalGrossWeight: totals.value.totalGrossWeight,
      totalNetWeight: totals.value.totalNetWeight,
      totalVolume: totals.value.totalVolume,
      totalAmount: totals.value.totalAmount,
      status: 0, // 初始保存为草稿状态，由后续 /submit 启动流程并改为1
      products: productList.value.map((product: any) => {
        let finalAmount = product.amount != null ? parseFloat(product.amount).toFixed(2) : '0.00';
        return {
          ...product,
          amount: finalAmount,
          elementValues: (product.declarationElements || []).map((elem: any) => ({
            elementName: elem.label,
            elementValue: elem.value && elem.value.trim() ? elem.value : '无'
          }))
        };
      }),
      cartons: cartonList.value,
      cartonProducts: cartonProducts 
    }
    
    console.log('提交的数据:', submitData)
    console.log('表单ID:', formId.value)
    console.log('表单状态:', formStatus.value)
    
    // 如果是从草稿提交，我们需要告诉后端这个表单原本在草稿表
    // 或者后端可以在 submit 逻辑中自动处理
    
    let finalId = formId.value
    if (formId.value && formStatus.value == 0) {
      // 更新正式表单
      await updateDeclaration(formId.value, submitData as any)
      message.success('申报单更新成功')
    } else {
      // 新增正式表单 (包括从草稿提交)
      const res = await addDeclaration(submitData as any)
      if (res.data && res.data.code === 200) {
        finalId = res.data.data
        // 如果是从草稿提交成功，手动删除草稿
        if (formId.value && formStatus.value === 0) {
            try {
                await deleteDeclaration(formId.value, 0)
            } catch (e) {
                console.error('删除旧草稿失败:', e)
            }
        }
        message.success('申报单保存成功')
      } else {
        throw new Error(res.data.message || '保存失败')
      }
    }

    // 关键修复：显式调用提交接口启动 Flowable 流程
    if (finalId) {
      console.log('正在启动流程, ID:', finalId)
      const submitRes = await submitDeclaration(finalId)
      if (submitRes.data && submitRes.data.code === 200) {
        message.success('流程启动完成，已进入部门初审阶段')
      } else {
        message.warning('表单已保存，但流程启动失败: ' + (submitRes.data?.message || '未知错误'))
      }
    }
    
    // 提交成功停留当前详情页：新建申报先回填 formId 并同步 URL（整页重挂载加载最新状态）
    if (finalId && String(formId.value ?? '') !== String(finalId)) {
      formId.value = finalId
      router.replace({
        path: route.path,
        query: { ...route.query, id: String(finalId) }
      })
    } else {
      stayAndRefresh()
    }
  } catch (error) {
    console.error('提交失败:', error)
    message.error('提交失败')
  } finally {
    submitting.value = false
  }
}

/** 入口校正：URL 的 mode 入口与后端实时数据比对，无效入口降级只读查看，防止伪造 URL 参数渲染审核/提交入口。
 *  豁免审核/补交审核入口在各自反查处已按记录状态校正，此处仅处理主流程任务绑定类入口 */
const sanitizeEntryByBackend = () => {
  const taskKeys = new Set(activeTasks.value.map((t: any) => t.taskKey).filter(Boolean))
  const downgrade = (flag: { value: boolean }, reason: string) => {
    flag.value = false
    isReadonly.value = true
    console.warn('入口校正：' + reason + '，已降级为只读查看')
  }
  // 初审审核入口：仅在待初审(1)/提货单待审(7)/退回待审(11)状态有效（提货单审核复用 audit 入口）
  if (isAudit.value && formStatus.value !== 1 && formStatus.value !== 7 && formStatus.value !== 11) {
    downgrade(isAudit, 'mode=audit 但当前状态=' + formStatus.value)
  }
  // 主流程任务绑定入口：对应 Flowable 任务存在才有效
  const taskBoundEntries: Array<[{ value: boolean }, string, string]> = [
    [isMaterialAuditMode, 'materialAudit', '资料审核'],
    [isSupplementAuditMode, 'supplementAudit', '补充资料审核'],
    [isInvoiceAuditMode, 'invoiceAudit', '发票审核'],
    [isInvoiceAmountAuditMode, 'invoiceAmountAudit', '开票金额审核'],
    [isSupplementMode, 'supplementSubmit', '补充资料提交'],
    [isInvoiceUploadMode, 'invoiceSubmit', '发票上传'],
    [isInvoiceAmountMode, 'invoiceAmountSubmit', '开票金额提交'],
  ]
  for (const [flag, taskKey, label] of taskBoundEntries) {
    if (flag.value && !taskKeys.has(taskKey)) {
      downgrade(flag, label + '入口但无 ' + taskKey + ' 任务')
    }
  }
  // 资料提交入口：补交草稿入口例外（补交不占用主流程任务）
  if (isMaterialMode.value && !isSupplementDraftEntry.value && !taskKeys.has('materialSubmit')) {
    downgrade(isMaterialMode, '资料入口但无 materialSubmit 任务')
  }
}

/** 入口推断：URL 未显式指定 mode 时，按当前用户可处理的任务推断操作入口（URL 只带 id 的新跳转形态）。
 *  推断命中则置对应 mode；无任务命中则不干预，保持按状态的可编辑/只读语义 */
const inferEntryFromBackend = () => {
  const keys = myTaskKeys.value
  if (keys.size === 0) return
  // 优先级与后端审核智能匹配一致：审核类优先，其次提交类
  const inferMap: Array<[string, { value: boolean }]> = [
    ['deptAudit', isAudit],
    ['materialAudit', isMaterialAuditMode],
    ['supplementAudit', isSupplementAuditMode],
    ['invoiceAudit', isInvoiceAuditMode],
    ['invoiceAmountAudit', isInvoiceAmountAuditMode],
    ['materialSubmit', isMaterialMode],
    ['supplementSubmit', isSupplementMode],
    ['invoiceSubmit', isInvoiceUploadMode],
    ['invoiceAmountSubmit', isInvoiceAmountMode],
  ]
  for (const [taskKey, flag] of inferMap) {
    if (keys.has(taskKey)) {
      flag.value = true
      // 审核类任务记录 taskKey，供通用审核按钮提交时使用
      if (taskKey.endsWith('Audit')) inferredAuditTaskKey.value = taskKey
      console.log('入口推断：当前用户持有任务 ' + taskKey)
      return
    }
  }
}

// 加载数据
const loadData = async () => {
  // 程序化装载（首屏/自动刷新/审核后原地重载）不得计为用户未保存的编辑
  dirtyTrackingOn = false
  try {
    await loadDataRaw()
  } finally {
    markSynced()
  }
}

const loadDataRaw = async () => {
  // 豁免审核模式：通过 exemptionId 反查 formId
  if (!formId.value && route.query.exemptionId) {
    try {
      const exDetail = await getExemptionDetail(route.query.exemptionId as string)
      if (exDetail.data?.code === 200 && exDetail.data.data) {
        formId.value = exDetail.data.data.formId
        formStatus.value = 2 // 豁免审核时主流程状态必然是2
        // 入口校正：仅豁免记录待审核（status=0）时保留豁免审核入口，否则降级只读查看
        // （新跳转只带 exemptionId 不带 mode，此处主动置位审核模式）
        if (exDetail.data.data.status !== 0) {
          isExemptionAuditMode.value = false
          isReadonly.value = true
        } else {
          isExemptionAuditMode.value = true
        }
      }
    } catch (e) {
      console.error('加载豁免记录失败', e)
    }
  }

  // 资料补交审核模式：通过 supplementId 反查 formId（补交不阻塞主流程，状态以申报单实际状态为准）
  if (!formId.value && route.query.supplementId) {
    try {
            const supRes = await getSupplementIncrements(route.query.supplementId as string)
      if (supRes.data?.code === 200 && supRes.data.data?.supplement) {
        formId.value = Number(supRes.data.data.supplement.formId)
        // 入口校正：仅补交单待审核（status=0）时保留补交审核入口，否则降级只读查看
        // （新跳转只带 supplementId 不带 mode，此处主动置位审核模式）
        if (supRes.data.data.supplement.status !== 0) {
          isMaterialSupplementAuditMode.value = false
          isReadonly.value = true
        } else {
          isMaterialSupplementAuditMode.value = true
        }
      }
    } catch (e) {
      console.error('加载资料补交记录失败', e)
    }
  }

  // 并行加载配置数据
  await Promise.all([
    loadProductTypes(),
    loadTransportModes(),
    loadTradeTerms(),
    loadPaymentMethods(),
    loadCountries(),
    loadCurrencies(),
    loadCities()
  ])
  
  if (formId.value) {
    try {
      const response = await getDeclarationDetail(formId.value)
      console.log('=== 申报单详情 API 响应 ===', response)
      console.log('response.data:', response.data)
      
      // 处理返回的数据
      if (response.data && response.data.code === 200 && response.data.data) {
        const detailData = response.data.data
        console.log('申报单数据:', detailData)
        console.log('产品列表:', detailData.products)
        console.log('箱子列表:', detailData.cartons)
        
        // 更新状态和只读模式
        const submittedStatus = detailData.status || 0
        formStatus.value = submittedStatus
        console.log('🔄 更新 formStatus 为:', submittedStatus)
        
        // 如果申报单已提交（status >= 1），查询活跃任务
        if (submittedStatus >= 1 && submittedStatus <= 9 && formId.value) {
          try {
            const taskRes = await getActiveTasks(formId.value)
            activeTasks.value = taskRes.data?.data || []
            console.log('📋 活跃任务:', activeTasks.value)
          } catch (e) {
            console.warn('获取活跃任务失败', e)
            activeTasks.value = []
          }
        } else {
          activeTasks.value = []
        }

        // 查询当前用户可处理的任务（入口推断/审核 taskKey 用）
        try {
          const myRes = await getBatchActiveTasks(String(formId.value))
          const myPayload = myRes.data?.data
          myTaskKeys.value = new Set<string>(((myPayload?.myTasks ?? {}) as Record<string, string[]>)[String(formId.value)] || [])
        } catch (e) {
          console.warn('获取用户可处理任务失败', e)
          myTaskKeys.value = new Set()
        }

        // 入口处理：URL 显式指定 mode 时校正合法性；否则按用户任务推断入口（新跳转只带 id）
        if (route.query.mode) {
          sanitizeEntryByBackend()
        } else if (route.query.readonly !== 'true' && !route.query.supplementDraft
          && !route.query.autoSubmit && !route.query.exemptionId && !route.query.supplementId) {
          inferEntryFromBackend()
        }
        
        // 只读状态判断：
        // 1. 如果 URL 参数 readonly=true，保持只读
        // 2. 如果是审核模式 (isAudit)，保持只读
        // 3. 如果是水单提交模式、资料模式或发票上传模式，由各自区域内部判断
        // 4. 否则根据状态判断：状态 0/2 可编辑，其他只读
        if (route.query.readonly === 'true' || isAudit.value || isMaterialSupplementAuditMode.value || isExemptionAuditMode.value) {
          isReadonly.value = true
          console.log('查看模式或审核模式, 设置为只读')
        } else if (!isPaymentMode.value && !isMaterialMode.value && !isInvoiceUploadMode.value) {
          const editableStatuses = [0, 2]
          if (!editableStatuses.includes(submittedStatus)) {
            isReadonly.value = true
            console.log('申报单状态=' + submittedStatus + ', 设置为只读模式')
          } else {
            isReadonly.value = false
            console.log('申报单状态=' + submittedStatus + ', 可编辑模式')
          }
        } else if (isMaterialMode.value) {
          // 资料模式：申报单基本信息只读，资料区域根据 readonly 和状态判断
          // readonly 保留为 URL 传入值，资料模块通过 isMaterialEditable 控制可编辑性
          console.log('资料模式：申报单基本信息只读，资料区域按状态判定')
        } else if (isInvoiceUploadMode.value) {
          // 发票上传模式：申报单基本信息只读，但发票区域可操作
          isReadonly.value = true
          console.log('发票上传模式：申报单只读，发票区域可操作')
        } else if (isMaterialAuditMode.value || isInvoiceAuditMode.value) {
          // 审核模式：整个表单只读
          isReadonly.value = true
          console.log('审核模式：申报单只读')
        }
        
        // 填充基本表单数据
        formData.formNo = detailData.formNo || ''
        formData.entityId = detailData.entityId || undefined
        formData.partyBId = detailData.partyBId || undefined
        formData.shipperCompany = detailData.shipperCompany || 'NINGBO ZIYI TECHNOLOGY CO.,LTD'
        formData.shipperAddress = detailData.shipperAddress || 'XIUFENG, GAOQIAO TOWN, HAISHU DISTRICT, NINGBO, ZHEJIANG, CHINA'
        formData.consigneeCompany = detailData.consigneeCompany || ''
        formData.consigneeAddress = detailData.consigneeAddress || ''
        formData.invoiceNo = detailData.invoiceNo || ''
        formData.transportMode = detailData.transportMode
        formData.tradeTerm = detailData.tradeTerm || undefined
        formData.miscFee = detailData.miscFee != null ? Number(detailData.miscFee) : undefined
        formData.arrivalPort = detailData.arrivalPort || ''
        formData.paymentMethod = detailData.paymentMethod
        formData.departureCity = detailData.departureCity || 'SHANGHAI, CHINA'
                formData.departureCityChinese = detailData.departureCityChinese || '上海'
                formData.departureCityEnglish = detailData.departureCityEnglish || 'SHANGHAI, CHINA'
        formData.destinationCountry = getCountryCodeByName(detailData.destinationCountry || '')
        formData.tradeCountry = getCountryCodeByName(detailData.tradeCountry || '')
        formData.currency = detailData.currency || currencyOptions.value[0]?.value || 'USD'
        formData.declarationDate = detailData.declarationDate ? dayjs(detailData.declarationDate) : undefined
        formData.declarationType = detailData.declarationType || 'EXTERNAL'
        formData.templateCode = detailData.templateCode || undefined
        // 以单据真实类型刷新标签徽标（老链接路径前缀推不出时尤为必要）
        applyTabMeta()
        
        // 根据模板配置加载区块显示
        if (formData.templateCode) {
          await loadTemplateSections(formData.templateCode)
        }
        
        // 如果 entityId 为空，根据发货公司名称自动匹配主体
        autoMatchEntity()
        
        // 填充产品列表
        const productsRaw = detailData.products
        if (Array.isArray(productsRaw)) {
          productList.value = productsRaw.map((product: any) => ({
            ...product,
            amountLocked: product.amountLocked === 1, // 后端 1/0 转前端 true/false
            amountUserModified: product.amountLocked === 1, // 如果已锁定，说明之前是用户手动修改的
            // 处理申报要素值 - 注意后端字段名是 elementName 和 elementValue
            declarationElements: (product.elementValues || []).map((ev: any) => ({
              id: ev.id,
              productId: ev.productId,
              elementId: ev.elementId || ev.elementName,  // 兼容两种字段名
              label: ev.elementName || ev.label,  // 优先使用elementName
              value: ev.elementValue || ev.value,  // 优先使用elementValue
              type: ev.type || 'text',  // 默认为text类型
              required: ev.required || false,
              options: ev.options || [],
              editable: true
            })),
            // 处理产品照片 - imageId 为 0/"0"/null 均视为无图片
            productPhoto: (product.imageId && String(product.imageId) !== '0')
              ? getFilePreviewUrl(product.imageId) 
              : (product.productPhoto || ''),
            photoFile: (product.imageId && String(product.imageId) !== '0') ? {
              uid: String(product.imageId),
              name: 'product.jpg',
              status: 'done',
              url: getFilePreviewUrl(product.imageId)
            } : null,
            // 添加体积字段
            volume: product.volume || 0
          }))
          console.log('加载产品列表成功:', productList.value.length + ' 个产品')
          
          // 打印产品详情用于调试
          productList.value.forEach((p: any, idx: number) => {
            console.log(`产品 ${idx + 1}:`, {
              名称: p.productName,
              HS编码: p.hsCode,
              数量: p.quantity,
              单价: p.unitPrice,
              金额: p.amount,
              体积: p.volume,
              申报要素数量: p.declarationElements?.length || 0
            })
            if (p.declarationElements && p.declarationElements.length > 0) {
              console.log('  申报要素:', p.declarationElements)
            }
          })
        } else {
          console.warn('产品列表为空或不是数组:', detailData.products)
        }
        
        // 填充箱子列表
        const cartonsRaw = detailData.cartons
        if (Array.isArray(cartonsRaw)) {
          cartonList.value = cartonsRaw.map((carton: any) => {
            // 从 cartonProducts 关联中提取选中的产品及其详情
            const relatedCps = (detailData.cartonProducts || [])
              .filter((cp: any) => cp.cartonId === carton.id)
            return {
              ...carton,
              // 添加体积字段(如果后端没返回)
              volume: carton.volume || 0,
              // 从 cartonProducts 关联中提取选中的产品 ID 列表
              selectedProducts: relatedCps.map((cp: any) => cp.productId),
              // 从 cartonProducts 关联中提取每个产品的数量/毛重/净重
              productDetails: relatedCps.map((cp: any) => ({
                productId: cp.productId,
                quantity: cp.quantity ?? 0,
                grossWeight: cp.grossWeight ?? null,
                netWeight: cp.netWeight ?? null
              }))
            }
          })
          console.log('加载箱子列表成功:', cartonList.value.length + ' 个箱子')
          
          // 打印箱子详情用于调试
          cartonList.value.forEach((c: any, idx: number) => {
            console.log(`箱子 ${idx + 1}:`, {
              箱号: c.cartonNo,
              数量: c.quantity,
              体积: c.volume,
              关联产品: c.selectedProducts,
              产品详情: c.productDetails
            })
          })
        } else {
          console.warn('箱子列表为空或不是数组:', detailData.cartons)
        }
        

        // 加载申报资料（状态 >=2 资料模块即可浏览/操作）
        if (formId.value && submittedStatus >= 2) {
          await loadMaterialItems()
          await loadExemptionStatus()
          // 申报资料区仅展示「资料上传」环节；业务发票在下方独立区域编辑
          const stages = availableStages.value
          if (stages.length > 0) {
            activeStageTab.value = stages[0].value
          } else {
            activeStageTab.value = DEFAULT_STAGE
          }
          // 豁免审核模式：自动滚动到资料区域；补交审核不滚动（审核卡片已置顶展示）
          if (isExemptionAuditMode.value) {
            nextTick(() => {
              setTimeout(() => {
                const el = document.querySelector('.material-manager')
                if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
              }, 200)
            })
          } else {
            scrollToQuerySection()
          }
        } else if (formId.value) {
          // 草稿阶段：加载资料项供基础资料区“资料”框预先上传（资料项来自资料模板）
          await loadMaterialItems()
        }

        // 补充资料审过后（status>5）：任意入口进入都加载开票金额详情（自用申报跳过）
        if (formId.value && submittedStatus > 5 && formData.declarationType !== 'SELF') {
          await loadInvoiceAmountDetail()
          if (route.query.scrollTo === 'invoice-amount') {
            scrollToQuerySection()
          }
        }
                
        // 如果是退回待审状态（status=11），加载最新的退回申请原因
        if (formId.value && submittedStatus === 11) {
          try {
            const historyRes = await getReturnAuditHistory(formId.value)
            if (historyRes.data && historyRes.data.code === 200) {
              const historyList = historyRes.data.data || []
              // 获取最新的待审核记录（auditStatus=0）或最后一条记录
              const latestRecord = historyList.find((r: any) => r.auditStatus === 0) || historyList[0]
              if (latestRecord) {
                returnReason.value = latestRecord.applyReason || '未填写原因'
                console.log('加载退回原因成功:', returnReason.value)
              }
            }
          } catch (e) {
            console.warn('加载退回原因失败:', e)
          }
        }
        
        // 出发口岸始终为中国口岸，cityOptions 已在初始化 loadCities() 中加载为中国城市，
        // 不能用目的国重新加载覆盖（否则出发口岸下拉会变空/失配）

        message.success('数据加载成功')

        // 列表页提交按钮跳转进入（autoSubmit=1）：数据就绪后自动触发提交，
        // 完整复用编辑页 handleSubmit 的校验与提交逻辑
        if (route.query.autoSubmit === '1' && !autoSubmitConsumed.value) {
          autoSubmitConsumed.value = true
          nextTick(() => { handleSubmit() })
        }
      } else {
        console.error('API返回异常:', response.data)
        message.error('获取申报单详情失败')
      }
    } catch (error: any) {
      console.error('加载申报单详情失败:', error)
      message.error('加载数据失败：' + (error.message || '未知错误'))
    }
  }
}

// 显示审核历史
const showAuditHistory = async () => {
  if (!formId.value) {
    message.warning('请先选择申报单')
    return
  }
  
  auditHistoryVisible.value = true
  auditHistoryLoading.value = true
  try {
    const res = await getReturnAuditHistory(formId.value)
    if (res.data && res.data.code === 200) {
      auditHistoryList.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载审核历史失败:', error)
    message.error('加载审核历史失败')
  } finally {
    auditHistoryLoading.value = false
  }
}
//
// // 加载活跃任务
// const loadActiveTasks = async () => {
//   if (!formId.value) return
//   try {
//     const res = await getActiveTasks(formId.value)
//     if (res.data && res.data.code === 200) {
//       activeTasks.value = res.data.data || []
//     }
//   } catch (error) {
//     console.warn('加载活跃任务失败:', error)
//     activeTasks.value = []
//   }
// }

// 显示审核意见弹窗
const showRemarkModal = (action: string, defaultRemark: string): Promise<string> => {
  return new Promise((resolve) => {
    remarkAction.value = action
    remarkValue.value = defaultRemark
    remarkModalVisible.value = true
    
    // 临时覆盖 handleRemarkSubmit 函数，用于当前 promise 的 resolve
    const originalHandleRemarkSubmit = handleRemarkSubmit
    handleRemarkSubmit = () => {
      if (!remarkValue.value.trim()) {
        message.warning('请输入审核意见')
        return
      }
      resolve(remarkValue.value.trim())
      remarkModalVisible.value = false
      remarkValue.value = ''
      // 恢复原始函数
      handleRemarkSubmit = originalHandleRemarkSubmit
    }
  })
}

// 测试申报要素数据结构
const testDataStructure = (elements: any[]) => {
  console.log('=== 申报要素数据结构测试 ===')
  elements.forEach((element, index) => {
    console.log(`要素 ${index + 1}:`, {
      label: element.label,
      type: element.type,
      editable: element.editable,
      required: element.required,
      hasOptions: !!element.options,
      optionsLength: element.options?.length || 0,
      hasOptionsStr: !!element.optionsStr,
      defaultValue: element.defaultValue,
      currentValue: element.value
    })
  })
}

watch(
  () => [showInvoiceAmountSection.value, formId.value] as const,
  ([show, id]) => {
    if (show && id) {
      loadInvoiceAmountDetail()
    }
  },
  { immediate: true }
)

/** 根据 URL ?scrollTo= 滚动到对应区块（material / supplement / invoice / invoice-amount） */
const scrollToQuerySection = () => {
  const scrollToSection = route.query.scrollTo as string
  if (!scrollToSection) return
  nextTick(() => {
    setTimeout(() => {
      const element = document.getElementById(`section-${scrollToSection}`)
      if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    }, 150)
  })
}

// ============================================================
// 向 Section 子组件提供共享状态
// ============================================================
provideFormState({
  // 核心数据
  formData, formId, formStatus, submitting,
  // 模式标志
  isMaterialMode, isMaterialAuditMode, isExemptionAuditMode, isMaterialSupplementAuditMode, isSupplementMode, isSupplementAuditMode,
  isInvoiceAmountMode, isInvoiceAmountAuditMode, isInvoiceUploadMode, isInvoiceAuditMode,
  isReadonly, isAudit,
  // 表单只读
  isFormReadonly,
  // 运输方式锁定（从新建弹窗预选后不可改）
  transportModeLocked,
  // 配置数据
  entityList, productList, cartonList,
  cityOptions, countryOptions, currencyOptions,
  transportModeOptions, tradeTermOptions, paymentMethodOptions, productOptions,
  productAutoCompleteOptionsWithCustom,
  hsOptions, measurementUnits,
  productColumns, cartonColumns,
  // 总计
  totals,
  // 产品/箱子操作
  handleCompanyChange, filterCompanyOption,
  onDepartureCityChange, filterCountrySelectOption,
  // 常用客户
  customerList, customerOptions, filterCustomerOption, onCustomerSelect,
  // 乙方配置
  partyBList, partyBOptions, partyBSummary, partyBSummaryParts, handlePartyBSaved,
  quickAddCustomerVisible, quickAddCustomerName, quickAddCustomerAddress,
  quickAddDestinationCountry, quickAddTradeCountry, quickAddCustomerSaving, handleQuickAddCustomer,
  handleQuantityOrPriceChange, handleUnitChange, handleAmountChange,
  updateProductName, onHsCodeChange,
  getProductCartonInfo, getProductDisplayById,
  syncProductDetails, getMaxQuantity, getMaxWeight, validateCartonProducts,
  // 资料项
  materialItems, supplementItems, invoiceStageItems,
  materialLoading, materialColumns, materialExpandedKeys, materialPdfMessages,
  materialRowKey, materialRowSaving,
  // 资料计算
  coreMaterialItems, materialRequiredCount, materialUploadedCount, materialProgressPercent,
  activeStageTab, availableStages, stageStats, activeStageItems,
  isMaterialReadonly, isMaterialEditable,
  // 资料操作
  parseMaterialSchema, isInvoiceMaterial,
  getMaterialFieldValue, setMaterialFieldValue, saveMaterialRowFields,
  beforeMaterialUpload, saveAttachmentField, previewFile, handleDeleteAttachment,
  // 补充资料
  showSupplementSection, canSubmitSupplement, canAuditSupplement,
  isSupplementEditable, supplementStats,
  // 开票金额
  showInvoiceAmountSection, canSubmitInvoiceAmount, canAuditInvoiceAmount,
  invoiceAmountLoading, isInvoiceAmountEditable,
  invoiceAmountCalcDetail, calcExpenseTotal,
  invoiceAmountRemittances, remittanceColumns,
  // 业务发票
  showInvoiceSection, canSubmitInvoice, canAuditInvoice,
  isInvoiceEditable, invoiceStats,
  // 模板区块控制
  hasSection, enabledSections,
  MATERIAL_FIXED_KEYS: ['amount', 'currency', 'invoiceNo', 'invoiceDate'],
})

onMounted(async () => {
  loadMaterialStages()
  // 新申报单初始化：申报类型与模板均可推导，新建 URL 可只带运输方式（甚至不带参数）
  if (!formId.value) {
    const templateFromQuery = route.query.template as string
    // 兼容两种传参写法：type（列表页标准）与 templateType（历史/手工 URL）
    const typeFromQuery = (route.query.type || route.query.templateType) as string
    const transportFromQuery = route.query.transport as string
    // 申报类型解析：declarationType > type/templateType（老链接兼容）> 路由前缀推导
    // （declaration-self 路由即 SELF 申报，declaration-external 即 EXTERNAL）
    const dtFromQuery = route.query.declarationType as string
    const resolvedType = (dtFromQuery && ['SELF', 'EXTERNAL'].includes(dtFromQuery))
      ? dtFromQuery
      : (typeFromQuery && ['SELF', 'EXTERNAL'].includes(typeFromQuery) ? typeFromQuery : null)
    formData.declarationType = resolvedType ?? (route.path.startsWith('/declaration-self') ? 'SELF' : 'EXTERNAL')
    applyTabMeta()
    // 模板解析：URL 显式指定时校验与申报类型一致（不一致自动纠偏）；
    // 未指定时按申报类型自动匹配默认模板（与列表页新建选择规则一致）
    let effectiveTemplate = templateFromQuery || ''
    try {
      const tplRes = await getAvailableFlowTemplates('declaration')
      if (tplRes.data?.code === 200) {
        const tpls = (tplRes.data.data || []).filter((t: any) => t.status === 1)
        if (effectiveTemplate) {
          const bound = tpls.find((t: any) => t.code === effectiveTemplate)
          if (bound && bound.declarationType && bound.declarationType !== formData.declarationType) {
            // 模板一致性纠偏：URL 模板与申报类型不一致时（如集洛单误带内部模板），自动换选匹配模板
            const matched = tpls.filter((t: any) => (t.declarationType || 'EXTERNAL') === formData.declarationType)
            const swap = matched.find((t: any) => t.isDefault === 1) || matched[0]
            if (swap) effectiveTemplate = swap.code
          }
        } else {
          const matched = tpls.filter((t: any) => (t.declarationType || 'EXTERNAL') === formData.declarationType)
          if (matched.length > 0) {
            effectiveTemplate = (matched.find((t: any) => t.isDefault === 1) || matched[0]).code
          } else {
            message.warning(`没有与当前申报类型（${formData.declarationType}）匹配的流程模板，请联系管理员配置`)
          }
        }
      }
    } catch {
      // 校验失败保持 URL 模板，提交时后端会按申报类型兼容纠偏
    }
    if (effectiveTemplate) {
      formData.templateCode = effectiveTemplate
      // 根据模板配置加载区块显示
      loadTemplateSections(effectiveTemplate)
    }
    // 运输方式从 URL 预选并锁定
    if (transportFromQuery) {
      formData.transportMode = transportFromQuery
      transportModeLocked.value = true
    }
    // 不再根据用户组织类型自动判断，默认使用初始值 EXTERNAL
    // 新建未保存：加载 BASIC 环节模板预览，支持上传时自动保存草稿
    loadMaterialItems()
  }
  loadData()
  loadCountries()
  loadMeasurementUnits()
  loadEntityList()
  loadCustomers()
  loadPartyBList()
  startAutoRefresh()
  // 草稿跟踪静默期：初始装载与异步回填（预选运输方式、模板默认值等）全部落定后才识别用户编辑
  setTimeout(endLoadSilence, 500)
  syncTabGuard()

  // 补交草稿入口校验：supplementDraft=1 仅在确有在途补交单时有效。
  // 补交已通过/取消后该参数可能残留在多标签页 URL，刷新会误入补交模式，需立即失效并清理参数
  if (isSupplementDraftEntry.value && formId.value) {
    try {
      const suppRes = await getCurrentSupplement(formId.value)
      if (!(suppRes.data?.code === 200 && suppRes.data.data)) {
        isSupplementDraftEntry.value = false
        const q = { ...route.query }
        delete q.supplementDraft
        router.replace({ path: route.path, query: q })
      }
    } catch { /* 校验失败保持入口，由内部兑底逻辑处理 */ }
  }
})

onUnmounted(() => {
  stopAutoRefresh()
  unregisterTabGuard?.()
  unregisterTabGuard = null
})
</script>

<style scoped>
/* 统一UI风格 - 与系统管理页面完全一致 */
:deep(.ant-card) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

:deep(.ant-card-body) {
  padding: 24px;
}

:deep(.ant-card-head) {
  border-bottom: 1px solid #e8e8e8;
  border-radius: 8px 8px 0 0;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
  font-size: 16px;
}

/* 表格样式 */
:deep(.ant-table) {
  border-radius: 12px;
  overflow: hidden;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #FAFBFC !important;
  font-weight: 600;
  color: #FA8C16;
  font-size: 13px;
  text-transform: none;
  letter-spacing: normal;
  border-bottom: 2px solid #F0F0F0 !important;
}

:deep(.ant-table-cell) {
  border-bottom: 1px solid #f1f5f9;
}

/* 主按钮样式已通过全局CSS优化，这里保持基础覆盖以确保一致性 */
:deep(.ant-btn-primary) {
  background: #FA8C16 !important;
  border-radius: 8px !important;
  box-shadow: 0 4px 6px -1px rgba(250, 140, 22, 0.2) !important;
}

:deep(.ant-btn-primary:hover) {
  background: #D46B08 !important;
  box-shadow: 0 10px 15px -3px rgba(250, 140, 22, 0.3) !important;
  transform: translateY(-1px);
}

/* 产品表格特定样式 */
:deep(.product-table .ant-table-thead > tr > th) {
  background: #FAFBFC !important;
}

/* 箱子表格特定样式 */
:deep(.carton-table .ant-table-thead > tr > th) {
  background: #FAFBFC !important;
}

/* 数值显示样式 */
:deep(.value-display) {
  font-size: 14px;
  color: #1e293b;
  font-weight: 600;
}

/* 箱子产品选择显示样式 */
:deep(.products-display) {
  padding: 4px 0;
}

:deep(.readonly-element-value) {
  padding: 6px 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  min-height: 32px;
  display: flex;
  align-items: center;
  color: #1e293b;
  font-weight: 500;
}

/* 响应式布局 */
@media (max-width: 768px) {
  :deep(.ant-card-body) {
    padding: 16px;
  }
}

.declaration-form-page {
  height: 100%;
  overflow-x: auto;
  min-width: 900px;
}

:deep(.section-card) {
  margin-bottom: 24px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
  border: 1px solid #E2E8F0;
  background: white;
}

:deep(.ant-card-head) {
  background: #FAFBFC;
  border-bottom: 1px solid #E2E8F0;
  min-height: 48px;
}

:deep(.ant-card-head-title) {
  font-size: 15px;
  font-weight: 700;
  color: #FA8C16;
}

:deep(.totals-section) {
  margin-top: 24px;
  padding: 24px;
  background: #FAFBFC;
  border-radius: 12px;
  border: 1px solid #E2E8F0;
}

:deep(.total-item) {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

:deep(.total-label) {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

:deep(.total-value) {
  font-weight: 800;
  color: #FA8C16;
  font-size: 18px;
  letter-spacing: -0.5px;
}

/* 水单图片上传样式 */
:deep(.remittance-photo-cell) {
  display: flex;
  justify-content: center;
  align-items: center;
}

:deep(.remittance-photo) {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 6px;
}

:deep(.no-photo) {
  color: #ccc;
  font-size: 12px;
}

:deep(.photo-wrapper) {
  position: relative;
  width: 48px;
  height: 48px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
}

:deep(.photo-wrapper .remittance-photo) {
  width: 100%;
  height: 100%;
  display: block;
}

:deep(.photo-overlay) {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s ease;
}

:deep(.photo-wrapper:hover .photo-overlay) {
  opacity: 1;
}

:deep(.upload-placeholder) {
  width: 48px;
  height: 48px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fafafa;
}

:deep(.upload-placeholder:hover) {
  border-color: #FA8C16;
  background: #FFF7E6;
}

/* 计算详情样式 */
:deep(.calculation-box) {
  background: #f7f7f7;
  padding: 20px;
  border-radius: 8px;
}

:deep(.calc-section) {
  margin-bottom: 20px;
}

:deep(.calc-title) {
  font-weight: bold;
  margin-bottom: 12px;
  color: #333;
}

:deep(.calc-row) {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #ddd;
}

:deep(.calc-row:last-child) {
  border-bottom: none;
}

:deep(.calc-label) {
  color: #666;
}

:deep(.calc-value) {
  font-weight: 500;
}

:deep(.calc-value.highlight) {
  color: #FA8C16;
  font-size: 16px;
  font-weight: bold;
}

:deep(.calc-value.final-value) {
  color: #D46B08;
  font-size: 18px;
  font-weight: bold;
}

:deep(.calc-row.deduct .calc-value) {
  color: #ff4d4f;
}

:deep(.calc-row.total-with-tax .calc-value) {
  color: #fa8c16;
  font-size: 16px;
  font-weight: bold;
}

:deep(.calc-row.final .calc-label) {
  font-weight: bold;
}

:deep(.calc-row.final .calc-value) {
  font-size: 20px;
  font-weight: bold;
}

/* 财务汇总信息增强样式 */
:deep(.finance-summary-card) {
  background-color: #fafbfc;
}

:deep(.summary-stat-item) {
  background: #fff;
  padding: 12px 16px;
  border-radius: 6px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: all 0.2s ease;
}

:deep(.summary-stat-item:hover) {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  transform: translateY(-1px);
}

:deep(.summary-stat-final) {
  background: linear-gradient(135deg, #FFF7E6, #fff);
}

:deep(.stat-label) {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

:deep(.stat-value) {
  font-size: 18px;
  font-weight: bold;
  line-height: 1.3;
}

:deep(.stat-unit) {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

/* ========== 申报资料模块样式 ========== */
:deep(.progress-card) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #f0f7ff 0%, #fafcff 100%);
  border: 1px solid #dbe9ff;
  border-radius: 8px;
}
:deep(.progress-title) {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}
:deep(.progress-desc) {
  font-size: 13px;
  color: #6b7280;
}
:deep(.progress-desc b) {
  font-size: 14px;
}
:deep(.progress-left) {
  flex: 1;
}
:deep(.progress-icon) {
  color: #1677ff;
  font-size: 16px;
}
:deep(.progress-right) {
  flex-shrink: 0;
  margin-left: 16px;
}
:deep(.progress-right) :deep(.ant-progress-text) {
  font-weight: 600;
}

:deep(.toolbar) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

:deep(.material-table) :deep(.ant-table-thead > tr > th) {
  background: #fafbfc;
  font-weight: 600;
  color: #374151;
}
:deep(.material-table) :deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
}

:deep(.name-cell .name-main) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 28px;
}
:deep(.name-cell .name-upload-actions) {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  flex-shrink: 0;
}
:deep(.name-cell .name-text) {
  font-weight: 500;
  color: #111827;
}
:deep(.name-cell .name-remark) {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

:deep(.file-cell) {
  display: flex;
  align-items: center;
  gap: 8px;
}
:deep(.file-cell .file-icon) {
  font-size: 18px;
}
:deep(.file-cell.file-uploaded .file-icon) {
  color: #52c41a;
}
:deep(.file-cell.file-empty) {
  color: #9ca3af;
}
:deep(.file-cell.file-empty .file-icon) {
  color: #d1d5db;
}
:deep(.file-cell .file-info) {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}
:deep(.file-cell .file-name) {
  color: #1677ff;
  font-weight: 500;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
:deep(.file-cell .file-time) {
  font-size: 11px;
  color: #9ca3af;
}

:deep(.schema-inline) {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
  padding: 10px 16px;
  background: #fafbfc;
  border-left: 3px solid #91caff;
}
:deep(.schema-field) {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 240px;
}
:deep(.schema-label) {
  font-size: 13px;
  color: #4b5563;
  white-space: nowrap;
}
:deep(.required-star) {
  color: #ff4d4f;
  margin-right: 2px;
}
:deep(.schema-input) {
  min-width: 160px;
  flex: 1;
}
/* 发票 PDF 解析提示包裹（纵向布局：输入框 + 提示） */
:deep(.schema-input-wrap) {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 180px;
}
:deep(.schema-input-wrap .schema-input) {
  width: 100%;
}
:deep(.pdf-amount-hint) {
  font-size: 12px;
  line-height: 1.4;
  padding: 3px 8px;
  border-radius: 4px;
  border-left: 3px solid #d9d9d9;
}
:deep(.pdf-amount-hint-success) {
  color: #389e0d;
  background: #f6ffed;
  border-left-color: #52c41a;
}
:deep(.pdf-amount-hint-warn) {
  color: #d46b08;
  background: #fff7e6;
  border-left-color: #fa8c16;
}
:deep(.pdf-amount-hint-info) {
  color: #0958d9;
  background: #e6f4ff;
  border-left-color: #1677ff;
}

/* 多附件展示 */
:deep(.file-cell-multi) {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
:deep(.file-item-row) {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 0;
}
:deep(.file-icon-sm) {
  color: #4f6ef7;
  font-size: 15px;
  flex-shrink: 0;
}
:deep(.file-name-sm) {
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #1677ff;
  font-weight: 500;
}
:deep(.file-name-sm:hover) {
  color: #4096ff;
  text-decoration: underline;
}
:deep(.file-delete-btn) {
  color: #ff4d4f;
  cursor: pointer;
  font-size: 12px;
  opacity: 0.6;
  transition: opacity 0.2s;
}
:deep(.file-delete-btn:hover) {
  opacity: 1;
}
:deep(.file-count-hint) {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 2px;
}

/* 附件卡片 */
:deep(.att-card) {
  margin-top: 6px;
  padding: 6px 10px;
  background: #fafafa;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
}

/* 颜色工具类 */
:deep(.text-red-500) { color: #ff4d4f; }
:deep(.text-blue-500) { color: #1677ff; }
:deep(.text-green-500) { color: #52c41a; }

/* 发票类附件卡片 */
:deep(.att-invoice-card) {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 6px;
  padding: 10px 14px 8px;
  margin-bottom: 8px;
}
:deep(.att-row-main) {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
:deep(.att-file-name) {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
:deep(.att-divider-v) {
  width: 1px;
  height: 20px;
  background: #e5e7eb;
  flex-shrink: 0;
}
:deep(.att-field-inline) {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
:deep(.att-field-label) {
  font-size: 12px;
  color: #8c8c8c;
  white-space: nowrap;
}
:deep(.att-val-tag) {
  display: inline-block;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #333;
}
:deep(.file-delete-btn) {
  color: #ff4d4f;
  cursor: pointer;
  font-size: 13px;
  opacity: 0.45;
  transition: opacity 0.15s;
  margin-left: auto;
  flex-shrink: 0;
}
:deep(.file-delete-btn:hover) {
  opacity: 1;
}
:deep(.att-row-meta) {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #9ca3af;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed #f0f0f0;
}
:deep(.att-row-meta .anticon) {
  font-size: 12px;
  margin-right: 2px;
}
:deep(.att-meta-dot) {
  display: inline-block;
  width: 3px;
  height: 3px;
  background: #d9d9d9;
  border-radius: 50%;
  margin: 0 4px;
}
:deep(.file-meta-inline) {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: #8c8c8c;
  margin: 0 6px;
  flex-shrink: 0;
}
:deep(.meta-icon) {
  font-size: 11px;
  color: #bfbfbf;
}

/* 开票金额计算详情 */
:deep(.calc-detail-wrap) {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}
:deep(.calc-section) {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}
:deep(.calc-section:last-of-type) {
  border-bottom: none;
}
:deep(.calc-section-title) {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}
:deep(.calc-income .calc-section-title) {
  color: #16a34a;
}
:deep(.calc-expense .calc-section-title) {
  color: #dc2626;
}
:deep(.calc-row) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0 4px 16px;
  font-size: 13px;
  line-height: 24px;
}
:deep(.calc-row.calc-subtotal) {
  border-top: 1px dashed #e8e8e8;
  margin-top: 6px;
  padding-top: 8px;
  font-weight: 600;
}
:deep(.calc-row.calc-highlight) {
  background: #fffbe6;
  border-radius: 4px;
  padding: 4px 8px;
  margin: 4px 0;
}
:deep(.calc-label) {
  color: #4b5563;
  flex-shrink: 0;
  margin-right: 12px;
}
:deep(.calc-value) {
  font-family: 'SFMono-Regular', Consolas, monospace;
  color: #1f2937;
  text-align: right;
}
:deep(.calc-result) {
  background: linear-gradient(135deg, #f0f7ff 0%, #fafcff 100%);
  padding: 14px 16px;
}
:deep(.calc-result .calc-row) {
  padding-left: 0;
  font-size: 14px;
  font-weight: 600;
}
:deep(.calc-steps) {
  padding: 4px 0;
}
:deep(.calc-step-item) {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 3px 0;
  font-size: 12px;
  color: #6b7280;
  line-height: 20px;
}
:deep(.calc-step-no) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #f3f4f6;
  color: #9ca3af;
  font-size: 11px;
  flex-shrink: 0;
}
:deep(.calc-step-text) {
  font-family: 'SFMono-Regular', Consolas, monospace;
}
</style>