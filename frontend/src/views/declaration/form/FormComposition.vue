<template>
  <div class="declaration-form-page">
    <a-card :title="(isMaterialMode ? (isReadonly ? '申报资料查看' : '提交申报资料') : isMaterialAuditMode ? '申报单详情 - 资料审核' : isSupplementMode ? '申报单详情 - 补充资料提交' : isSupplementAuditMode ? '申报单详情 - 补充资料审核' : canSubmitInvoiceAmount ? '申报单详情 - 申请开票金额' : (canAuditInvoiceAmount || isInvoiceAmountAuditMode) ? '申报单详情 - 开票金额审核' : isInvoiceAmountMode ? '申报单详情 - 申请开票金额' : isInvoiceAuditMode ? '申报单详情 - 发票审核' : isInvoiceUploadMode ? '申报单详情 - 上传发票' : '出口申报表单')" >
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
          
          <!-- 审核模式下的按钮 -->
          <template v-if="isAudit">
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
            <a-button
              v-if="formStatus === 2"
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
            <a-button v-if="!isReadonly && (!formStatus || formStatus === 0)" @click="handleSaveDraft" :loading="submitting" v-permission="['business:declaration:create']">
              <template #icon><SaveOutlined /></template>
              保存草稿
            </a-button>
            
            <!-- 只在草稿状态且非只读模式下显示提交按钮 -->
            <a-button v-if="!isReadonly && (!formStatus || formStatus === 0)" type="primary" @click="handleSubmit" :loading="submitting" v-permission="['business:declaration:submit']">
              <template #icon><SendOutlined /></template>
              提交申报
            </a-button>
          </template>
        </a-space>
      </template>
      
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
        @add-product="addProduct"
        @remove-product="(i: number) => removeProduct(i)"
        @add-carton="addCarton"
        @remove-carton="(i: number) => removeCarton(i)"
        @upload-product-photo="(f: File, i: number) => beforeProductPhotoUpload(f, i)"
        @remove-product-photo="(i: number) => handleRemoveProductPhoto(i)"
      />

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
        :can-operate="canOperateMaterialStage"
        @submitted="() => goBack()"
        @audited="() => goBack()"
        @preview-file="previewFile"
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

      <!-- 资料管理：开票金额及之后的环节（发票资料） -->
      <MaterialManager
        v-if="showMaterialManager && hasSection('invoiceAmount')"
        :form-id="formId!"
        :mode="materialManagerMode"
        :form-status="formStatus"
        :step-status-map="stepStatusMap"
        :section-order-map="sectionOrderMap"
        section-range="post"
        :stop-before="'invoiceAmount'"
        :can-operate="canOperateMaterialStage"
        @submitted="() => goBack()"
        @audited="() => goBack()"
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

    <!-- 20%拆分产品设置弹窗 -->
    <InvoiceSplitModal ref="invoiceSplitModalRef" :form-id="formId!" :calc-detail="invoiceAmountCalcDetail" :readonly="!hasFinancePermission" @confirm="handleSplitConfirm" />

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, h, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal, Textarea } from 'ant-design-vue'
import { checkPermission } from '@/directives/permission'
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
  auditReturnToDraft,
  getReturnAuditHistory,
  
  exportInvoicePackage,
  getInvoiceSplitItems,
  // 业务发票 API 已废弃，统一使用资料项 INVOICE 环节
} from '@/api/business/declaration'
import {
  getMaterialItems,
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
  type MaterialItem,
  type MaterialAttachment
} from '@/api/business/materialItem'
import { getRemittancesByFormId } from '@/api/business/remittance'
import {
  MATERIAL_STAGES,
  type MaterialStage
} from '@/api/system/materialTemplate'
import { getEnabledDictItems } from '@/api/system/dict'
import { getProductTypes } from '@/api/system/product'
import { getEnabledTransportModes } from '@/api/system/transportMode'
import { getEnabledPaymentMethods } from '@/api/system/paymentMethod'
import { getEnabledCountries } from '@/api/system'
import { getEnabledCurrencies } from '@/api/system/currency'
import { getActiveMeasurementUnits, type MeasurementUnit } from '@/api/system/measurement-unit'
import { getCitiesByCountry } from '@/api/system/city-info'
import {  findUnitByCode } from '@/utils/measurement-unit'
import { getEnabledEntityConfigs, type EntityConfig } from '@/api/system/entityConfig'
import { getFlowTemplateNodesByCode } from '@/api/system/flowTemplate'
import FilePreviewModal from '@/components/FilePreviewModal.vue'
import InvoiceSplitModal from './InvoiceSplitModal.vue'
import BasicInfoSection from './sections/BasicInfoSection.vue'
import InvoiceAmountSection from './sections/InvoiceAmountSection.vue'
import InvoiceSection from './sections/InvoiceSection.vue'
import RemittanceDisplaySection from './sections/RemittanceDisplaySection.vue'
import MaterialManager from './MaterialManager.vue'
import { provideFormState } from './composables/useDeclarationForm'

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
const isMaterialMode = ref(route.query.mode === 'material') // 资料提交/查看模式
const isMaterialAuditMode = ref(route.query.mode === 'materialAudit') // 资料审核模式
const isInvoiceAuditMode = ref(route.query.mode === 'invoiceAudit') // 发票审核模式
const isInvoiceUploadMode = ref(route.query.mode === 'invoiceUpload') // 发票上传模式
const isSupplementMode = ref(route.query.mode === 'supplement') // 补充资料提交模式
const isSupplementAuditMode = ref(route.query.mode === 'supplementAudit') // 补充资料审核模式
const isInvoiceAmountMode = ref(route.query.mode === 'invoiceAmount') // 申请开票金额提交模式
const isInvoiceAmountAuditMode = ref(route.query.mode === 'invoiceAmountAudit') // 开票金额审核模式
const isReadonly = ref(route.query.readonly === 'true' || isAudit.value)
const formId = ref(route.query.id ? Number(route.query.id) : null)
const formStatus = ref<number | null>(route.query.status ? Number(route.query.status) : null)
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
  { title: '申请时间', dataIndex: 'applyTime', key: 'applyTime', width: 160 },
  { title: '审核人', dataIndex: 'auditorName', key: 'auditorName', width: 90 },
  { title: '备注', dataIndex: 'auditRemark', key: 'auditRemark', ellipsis: true, minWidth: 150 },
  { title: '审核时间', dataIndex: 'auditTime', key: 'auditTime', width: 160 },
  { title: '原状态', key: 'preStatus', width: 70 }
]

// 活跃任务状态（用于任务驱动的 UI 判断）
const activeTasks = ref<any[]>([])

// 计量单位列表
const measurementUnits = ref<MeasurementUnit[]>([])

// 基本信息是否只读（审核模式、查看模式、水单提交模式、资料模式、资料审核模式、发票上传模式都只读）
const isFormReadonly = computed(() => isReadonly.value || isAudit.value || isPaymentMode.value || isMaterialMode.value || isMaterialAuditMode.value || isInvoiceAuditMode.value || isInvoiceUploadMode.value || isSupplementMode.value || isSupplementAuditMode.value || isInvoiceAmountMode.value || isInvoiceAmountAuditMode.value)

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
      // 普通业务审核通过
      const taskKey = route.query.taskKey as string
      console.log('执行审核通过操作:', { formId: formId.value, taskKey, result: 1, remark })
      await auditDeclaration(formId.value, 1, remark, taskKey)
      message.success(`${getAuditActionText()}已通过`)
      if (formStatus.value === 1) {
        message.info('全套单证已自动生成')
      }
    }
    // 直接跳转到列表页，无需刷新当前页面数据
    goBack()
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
      // 普通业务审核驳回
      const taskKey = route.query.taskKey as string
      console.log('执行驳回操作:', { formId: formId.value, taskKey, result: 2, remark })
      await auditDeclaration(formId.value, 2, remark, taskKey)
      message.success(`${getAuditActionText()}已驳回`)
    }
    // 直接跳转到列表页，无需刷新当前页面数据
    goBack()
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

/** 核心资料项（排除非资料提交环节） */
const coreMaterialItems = computed(() =>
  materialItems.value.filter(i => !excludedStages.value.has(getItemStage(i)))
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
const getItemStage = (item: MaterialItem): MaterialStage =>
  (item.stage as MaterialStage) || DEFAULT_STAGE

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
/** 非资料提交环节（补充资料、发票等独立管理的环节） */
const excludedStages = computed<Set<string>>(() => {
  // 有 submitKey + templateStage 的是资料提交环节，其余为非资料提交环节
  const submitStages = dynamicStages.value.filter(s => !!s.templateStage)
  // 如果只有默认三环节（fallback），用硬编码排除
  if (submitStages.length <= 3 && dynamicStages.value.length <= 3) {
    return new Set(['SUPPLEMENT', 'INVOICE'])
  }
  // 动态模式下，排除没有 submitKey 的环节（它们由独立组件管理）
  // 这里暂保留硬编码排除，因为补充资料和发票有独立 UI
  return new Set(['SUPPLEMENT', 'INVOICE'])
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
  materialItems.value.filter((i) => getItemStage(i) === stage)

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

/** MaterialManager 模式：从流程配置动态判断 submit/audit */
const materialManagerMode = computed<'submit' | 'audit'>(() => {
  const s = formStatus.value
  if (s == null) return 'submit'
  const map = stepStatusMap.value
  // 检查当前状态是否匹配某个 auditTaskKey 的 targetStatus
  for (const [nodeKey, targetStatus] of map) {
    if (targetStatus === s && nodeKey.toLowerCase().includes('audit')) {
      return 'audit'
    }
  }
  return 'submit'
})

/** MaterialManager 可操作性判断：从流程配置动态匹配 nodeKey → targetStatus */
const canOperateMaterialStage = (section: { submitKey?: string; auditTaskKey?: string }): boolean => {
  if (route.query.readonly === 'true' || isAudit.value) return false
  const s = formStatus.value
  if (s == null) return false
  const map = stepStatusMap.value
  // 动态检查：当前状态是否匹配该 submitKey/auditTaskKey 对应的 targetStatus
  if (section.submitKey && map.get(section.submitKey) === s) return true
  if (section.auditTaskKey && map.get(section.auditTaskKey) === s) return true
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
  if (isSupplementAuditMode.value || isMaterialAuditMode.value || isAudit.value) return false
  return true
})

/** 状态=5 时可审核补充资料 */
const canAuditSupplement = computed(() => {
  if (formStatus.value !== 5) return false
  if (route.query.readonly === 'true') return false
  if (isMaterialMode.value) return false
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
  if (isInvoiceAmountAuditMode.value || isInvoiceAuditMode.value || isInvoiceUploadMode.value) return false
  if (isMaterialAuditMode.value || isSupplementAuditMode.value || isAudit.value) return false
  // 仅资料/补充/开票金额等业务入口，不因 URL 上残留的 mode 拦截
  return true
})

/** 状态=7 时可审核开票金额（模板未配置或自用申报跳过） */
const canAuditInvoiceAmount = computed(() => {
  if (!hasSection('invoiceAmount')) return false
  if (formData.declarationType === 'SELF') return false
  if (formStatus.value !== 7) return false
  if (route.query.readonly === 'true') return false
  if (isMaterialMode.value || isSupplementMode.value) return false
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
  if (isInvoiceAuditMode.value || isMaterialAuditMode.value || isSupplementAuditMode.value || isAudit.value) return false
  return true
})

/** 状态=9 时可审核业务发票（模板未配置跳过） */
const canAuditInvoice = computed(() => {
  if (!hasSection('invoice')) return false
  if (formStatus.value !== 9) return false
  if (route.query.readonly === 'true') return false
  if (isMaterialMode.value || isSupplementMode.value) return false
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
  if (!formId.value) return
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

const beforeMaterialUpload = async (file: File, record: MaterialItem) => {
  try {
    const id = await resolveMaterialItemId(record)
    if (!id) return false
    // 同时传 formId + templateId：后端在 id 找不到实例时会自动按模板 ensure 一条再上传
    const res = await uploadMaterialFile(id, file, {
      formId: formId.value,
      templateId: record.templateId ?? null
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
  }
  return false
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

/** 删除单个附件 */
const handleDeleteAttachment = async (record: MaterialItem, att: MaterialAttachment) => {
  try {
    const res = await deleteMaterialAttachment(record.id!, att.id)
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
    // 跳过非资料提交环节
    const stage = getItemStage(item)
    if (excludedStages.value.has(stage)) continue
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
  // 只校验资料提交阶段的项，不包含非资料提交环节
  const submitItems = materialItems.value.filter((i) => !excludedStages.value.has(getItemStage(i)))
  const missing = submitItems.filter((i) => i.required === 1 && i.status !== 1)
  if (missing.length > 0) {
    message.warning(`还有 ${missing.length} 项必填资料未上传：${missing.map((m) => m.name).join('、')}`)
    return
  }
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

  const doSubmit = async () => {
    try {
      submitting.value = true
      const res = await submitMaterial(formId.value!)
      if (res.data?.code === 200) {
        message.success('资料提交成功，等待审核')
        goBack()
      } else {
        message.error(res.data?.message || '提交失败')
      }
    } catch (e) {
      message.error('提交失败')
    } finally {
      submitting.value = false
    }
  }

  const confirmText = '提交后将进入资料审核流程，无法修改。'

  Modal.confirm({
    title: '确认提交资料审核？',
    content: confirmText,
    okText: '确认提交',
    onOk: doSubmit
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
          goBack()
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
          goBack()
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
  const missing = supplementItems.value.filter((i) => i.required === 1 && i.status !== 1)
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
          goBack()
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
          goBack()
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
          goBack()
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
    // 并发加载计算详情和水单列表
    const [calcRes, remRes] = await Promise.all([
      getInvoiceAmountDetail(formId.value!),
      getRemittancesByFormId(formId.value!)
    ])
    if (calcRes.data?.code === 200) {
      invoiceAmountCalcDetail.value = calcRes.data.data
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

// 20%拆分弹窗
const invoiceSplitModalRef = ref<InstanceType<typeof InvoiceSplitModal> | null>(null)

/** 是否有财务权限（可编辑20%数据） */
const hasFinancePermission = computed(() => {
  return checkPermission(['business:declaration:finance:supplement'])
})

/** 下载开票文件包(80%+20%) */
const handleDownloadInvoicePackage = async () => {
  if (!formId.value) return

  if (hasFinancePermission.value) {
    // 有财务权限：打开弹窗，可编辑+下载
    invoiceSplitModalRef.value?.open()
  } else {
    // 无财务权限：检查是否已配置20%数据，已配置则直接下载
    try {
      const res = await getInvoiceSplitItems(formId.value)
      const savedItems = res.data?.data
      if (!Array.isArray(savedItems) || savedItems.length === 0) {
        message.warning('请先联系财务人员录入20%产品数据')
        return
      }
    } catch {
      message.warning('请先联系财务人员录入20%产品数据')
      return
    }
    // 直接下载
    await doDownloadInvoicePackage([])
  }
}

/** 执行下载开票文件包 */
const doDownloadInvoicePackage = async (splitItems: any[]) => {
  if (!formId.value) return
  try {
    const res = await exportInvoicePackage(formId.value!, splitItems)
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

/** 20%弹窗确认回调 */
const handleSplitConfirm = async (splitItems: any[]) => {
  await doDownloadInvoicePackage(splitItems)
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
          goBack()
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
          goBack()
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
          goBack()
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
          goBack()
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
          goBack()
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
          goBack()
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
  shipperCompany: 'NINGBO ZIYI TECHNOLOGY CO.,LTD',
  shipperAddress: 'XIUFENG, GAOQIAO TOWN, HAISHU DISTRICT, NINGBO, ZHEJIANG, CHINA',
  consigneeCompany: '',
  consigneeAddress: '',
  invoiceNo: '',
  transportMode: undefined as string | undefined,
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
        value: item.name || item.name
      }))
      console.log('加载运输方式成功:', transportModeOptions.value)
    }
  } catch (error) {
    console.warn('加载运输方式失败:', error)
    // 使用默认运输方式
    transportModeOptions.value = [
      { label: '海运', value: 'SEA' },
      { label: '空运', value: 'AIR' },
      { label: '陆运', value: 'LAND' },
      { label: '快递', value: 'EXPRESS' }
    ]
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
      alloc.quantity += (detail.quantity || 0)
      alloc.grossWeight += (detail.grossWeight || 0)
      alloc.netWeight += (detail.netWeight || 0)
    })
  })
  
  // 对比每个产品的总量（超配 + 不足）
  productList.value.forEach((product) => {
    const alloc = allocationMap.get(product.id!)
    const name = product.productName || `产品${product.id}`
    const totalQty = Number(product.quantity) || 0
    const totalGross = Number(product.grossWeight) || 0
    const totalNet = Number(product.netWeight) || 0
    const allocQty = alloc?.quantity || 0
    const allocGross = alloc?.grossWeight || 0
    const allocNet = alloc?.netWeight || 0
    // 校验数量
    if (allocQty > totalQty) {
      errors.push(`${name}: 数量超配 (已分配${allocQty}/总量${totalQty})`)
    } else if (allocQty < totalQty) {
      errors.push(`${name}: 数量不足 (已分配${allocQty}/总量${totalQty})`)
    }
    // 校验毛重
    if (totalGross > 0) {
      if (allocGross > totalGross) {
        errors.push(`${name}: 毛重超配 (已分配${allocGross.toFixed(3)}/总量${totalGross})`)
      } else if (allocGross < totalGross) {
        errors.push(`${name}: 毛重不足 (已分配${allocGross.toFixed(3)}/总量${totalGross})`)
      }
    }
    // 校验净重
    if (totalNet > 0) {
      if (allocNet > totalNet) {
        errors.push(`${name}: 净重超配 (已分配${allocNet.toFixed(3)}/总量${totalNet})`)
      } else if (allocNet < totalNet) {
        errors.push(`${name}: 净重不足 (已分配${allocNet.toFixed(3)}/总量${totalNet})`)
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
  { title: '箱号', dataIndex: 'cartonNo', key: 'cartonNo', width: 120 },
  { title: '类型', dataIndex: 'typeChinese', key: 'typeChinese', width: 100 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 100 },
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
  
  // 箱子总体积直接累加（因为输入的就是总体积）
  cartonList.value.forEach(carton => {
    totalVolume += (carton.volume || 0)
  })
  
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
  
  cartonList.value.push({
    id: newId,
    cartonNo: `CTN${String(newId).padStart(3, '0')}`,
    quantity: 1,
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
  // 智能返回：优先返回上一页，无历史时返回申报录入
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push(formData.declarationType === 'SELF' ? '/declaration-self/entry' : '/declaration-external/entry')
  }
}

// 保存草稿
const handleSaveDraft = async () => {
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
        router.replace({
          path: route.path,
          query: { ...route.query, id: newDraftId, status: 0 }
        })
      }
    } else {
      message.error(response.data.message || '保存草稿失败')
    }
  } catch (error: any) {
    console.error('保存草稿失败:', error)
    message.error('保存失败: ' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

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
    if (!formData.departureCity) {
      message.error('请选择出发城市')
      return
    }
    if(!formData.currency){
      message.error('请选择货币')
      return
    }

    if (productList.value.length === 0) {
      message.error('请至少添加一个产品')
      return
    }    
    // 检查所有产品是否都关联了箱子
    const unassignedProducts = productList.value.filter(product => {
      // 检查这个产品是否在任何箱子的产品列表中
      return !cartonList.value.some(carton => 
        carton.selectedProducts && carton.selectedProducts.includes(product.id)
      )
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
    
    goBack()
  } catch (error) {
    console.error('提交失败:', error)
    message.error('提交失败')
  } finally {
    submitting.value = false
  }
}

// 加载数据
const loadData = async () => {
  // 并行加载配置数据
  await Promise.all([
    loadProductTypes(),
    loadTransportModes(),
    loadPaymentMethods(),
    loadCountries(),
    loadCurrencies(),
    loadCities()
  ])
  
  if (formId.value) {
    try {
      const response = await getDeclarationDetail(formId.value, formStatus.value ?? undefined)
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
        
        // 只读状态判断：
        // 1. 如果 URL 参数 readonly=true，保持只读
        // 2. 如果是审核模式 (isAudit)，保持只读
        // 3. 如果是水单提交模式、资料模式或发票上传模式，由各自区域内部判断
        // 4. 否则根据状态判断：状态 0/2 可编辑，其他只读
        if (route.query.readonly === 'true' || isAudit.value) {
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
        formData.shipperCompany = detailData.shipperCompany || 'NINGBO ZIYI TECHNOLOGY CO.,LTD'
        formData.shipperAddress = detailData.shipperAddress || 'XIUFENG, GAOQIAO TOWN, HAISHU DISTRICT, NINGBO, ZHEJIANG, CHINA'
        formData.consigneeCompany = detailData.consigneeCompany || ''
        formData.consigneeAddress = detailData.consigneeAddress || ''
        formData.invoiceNo = detailData.invoiceNo || ''
        formData.transportMode = detailData.transportMode
        formData.paymentMethod = detailData.paymentMethod
        formData.departureCity = detailData.departureCity || 'SHANGHAI, CHINA'
                formData.departureCityChinese = detailData.departureCityChinese || '上海'
                formData.departureCityEnglish = detailData.departureCityEnglish || 'SHANGHAI, CHINA'
        formData.destinationCountry = detailData.destinationCountry || ''
        formData.tradeCountry = detailData.tradeCountry || ''
        formData.currency = detailData.currency || currencyOptions.value[0]?.value || 'USD'
        formData.declarationDate = detailData.declarationDate ? dayjs(detailData.declarationDate) : undefined
        formData.declarationType = detailData.declarationType || 'EXTERNAL'
        formData.templateCode = detailData.templateCode || undefined
        
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
          // 申报资料区仅展示「资料上传」环节；业务发票在下方独立区域编辑
          const stages = availableStages.value
          if (stages.length > 0) {
            activeStageTab.value = stages[0].value
          } else {
            activeStageTab.value = DEFAULT_STAGE
          }
          scrollToQuerySection()
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
        
        // 根据已加载的国家信息加载对应的城市
        if (formData.destinationCountry) {
          const selectedCountry = countryOptions.value.find(country => country.value === formData.destinationCountry);
          if (selectedCountry) {
            loadCities(selectedCountry.englishName || selectedCountry.chineseName);
          }
        }
        
        message.success('数据加载成功')
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
  isMaterialMode, isMaterialAuditMode, isSupplementMode, isSupplementAuditMode,
  isInvoiceAmountMode, isInvoiceAmountAuditMode, isInvoiceUploadMode, isInvoiceAuditMode,
  isReadonly, isAudit,
  // 表单只读
  isFormReadonly,
  // 运输方式锁定（从新建弹窗预选后不可改）
  transportModeLocked,
  // 配置数据
  entityList, productList, cartonList,
  cityOptions, countryOptions, currencyOptions,
  transportModeOptions, paymentMethodOptions, productOptions,
  productAutoCompleteOptionsWithCustom,
  hsOptions, measurementUnits,
  productColumns, cartonColumns,
  // 总计
  totals,
  // 产品/箱子操作
  handleCompanyChange, filterCompanyOption,
  onDepartureCityChange, filterCountrySelectOption,
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

onMounted(() => {
  loadMaterialStages()
  // 新申报单时：优先使用 URL 中的 template 参数，回退到 type 参数，最后默认 EXTERNAL
  if (!formId.value) {
    const templateFromQuery = route.query.template as string
    const typeFromQuery = route.query.type as string
    const transportFromQuery = route.query.transport as string
    if (templateFromQuery) {
      formData.templateCode = templateFromQuery
      // 根据模板配置加载区块显示
      loadTemplateSections(templateFromQuery)
    }
    if (typeFromQuery && ['SELF', 'EXTERNAL'].includes(typeFromQuery)) {
      formData.declarationType = typeFromQuery
    }
    // 运输方式从 URL 预选并锁定
    if (transportFromQuery) {
      formData.transportMode = transportFromQuery
      transportModeLocked.value = true
    }
    // 不再根据用户组织类型自动判断，默认使用初始值 EXTERNAL
  }
  loadData()
  loadCountries()
  loadMeasurementUnits()
  loadEntityList()
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
