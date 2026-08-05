import request from '@/utils/request'

/**
 * 资料项附件（多文件）
 */
export interface MaterialAttachment {
  id: number | string
  itemId: number | string
  fileName: string
  fileUrl: string
  fileSize?: number | null
  /** 发票金额（仅发票类资料项） */
  amount?: number | null
  /** 币种 */
  currency?: string | null
  /** 发票号 */
  invoiceNo?: string | null
  /** 开票日期 */
  invoiceDate?: string | null
  /** 上传时所处环节（多环节共享时，后续环节不可删除前序环节上传的附件） */
  stage?: string | null
  /** 所属补交单ID（非空=补交增量，待审核） */
  supplementId?: number | string | null
  /** 扩展字段 JSON */
  extraData?: string | null
  uploadBy?: number | string | null
  uploadTime?: string | null
  /** 上传人显示名 */
  uploadByName?: string | null
  /** 创建人显示名 */
  createByName?: string | null
  /** 更新人显示名 */
  updateByName?: string | null
  createTime?: string
}

/**
 * 申报资料项实例
 */
export interface MaterialItem {
  id?: number | string
  formId: number | string
  templateId?: number | string | null
  code?: string
  name: string
  required: number
  /** 必填环节配置（后端视图层从模板透传，逗号分隔）；非空时按当前环节命中与否判定必填，否则回退 required */
  requiredStages?: string | null
  sort: number
  remark?: string
  formSchema?: string | null
  /** 所属环节：MATERIAL_SUBMIT / INVOICE / FINANCE_SUPPLEMENT（从模板同步） */
  stage?: string
  /** 发票模式: 0-普通附件 1-附件级金额/发票号/日期（从模板同步） */
  invoiceMode?: number
  fileName?: string
  fileUrl?: string
  uploadBy?: number | string
  uploadTime?: string
  amount?: number | null
  currency?: string | null
  invoiceNo?: string | null
  invoiceDate?: string | null
  extraData?: string | null
  status: number // 0-未上传 1-已上传
  /** 所属补交单ID（非空=补交增量，待审核） */
  supplementId?: number | string | null
  createBy?: number | string
  updateBy?: number | string
  createByName?: string
  updateByName?: string
  createTime?: string
  updateTime?: string
  /** 附件列表（后端 viewByFormId 批量加载） */
  attachments?: MaterialAttachment[]
}

/**
 * 获取某申报单的资料项（空时后端会自动同步模板）
 */
export function getMaterialItems(formId: number | string) {
  return request({
    url: '/v1/material/items',
    method: 'get',
    params: { formId }
  })
}

/**
 * 按环节获取启用的资料模板（未保存草稿时预览资料项用，不需要模板管理权限）
 */
export function getMaterialTemplatePreview(stage: string) {
  return request({
    url: '/v1/material/items/template-preview',
    method: 'get',
    params: { stage }
  })
}

/**
 * 单据内手动新增资料项（不入全局模板）
 */
export function addMaterialItem(data: Partial<MaterialItem>) {
  return request({
    url: '/v1/material/items',
    method: 'post',
    data
  })
}

/**
 * 幂等确保模板对应的资料项已落库，返回带 id 的实例。
 * 用于将懒创建视图中的"虚拟项"（id=null）升格为真实记录后再操作
 */
export function ensureMaterialItem(formId: number | string, templateId: number | string) {
  return request({
    url: '/v1/material/items/ensure',
    method: 'post',
    params: { formId, templateId }
  })
}

/**
 * 修改资料项（名称/必填/排序/说明）
 */
export function updateMaterialItem(data: Partial<MaterialItem>) {
  return request({
    url: '/v1/material/items',
    method: 'put',
    data
  })
}

/**
 * 删除资料项（仅手动新增允许）
 */
export function deleteMaterialItem(id: number | string) {
  return request({
    url: `/v1/material/items/${id}`,
    method: 'delete'
  })
}

/**
 * 附件是否允许在指定环节删除：只能删本环节上传的附件（无环节标记的历史附件放行）
 */
export function canDeleteAttachment(att: { stage?: string | null }, stage?: string | null): boolean {
  return !att?.stage || !stage || att.stage === stage
}

/**
 * 上传附件
 * 额外可传 formId + templateId，后端在 id 找不到记录时会按模板兄弟 ensure 一条再上传（资料项懒创建兄底）
 * uploadStage：上传时所处环节，落库后用于跨环节删除保护
 */
export function uploadMaterialFile(
  id: number | string,
  file: File,
  extras?: { formId?: number | string | null; templateId?: number | string | null; uploadStage?: string | null; supplementId?: number | string | null }
) {
  const formData = new FormData()
  formData.append('file', file)
  const params: Record<string, any> = {}
  if (extras?.formId) params.formId = extras.formId
  if (extras?.templateId) params.templateId = extras.templateId
  if (extras?.uploadStage) params.uploadStage = extras.uploadStage
  if (extras?.supplementId) params.supplementId = extras.supplementId
  return request({
    url: `/v1/material/items/${id ?? 0}/upload`,
    method: 'post',
    data: formData,
    params,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 解析发票 PDF 中的金额（仅返回解析结果，不写入任何资料项）
 */
export interface PdfParseResult {
  amount?: number | null
  success: boolean
  errorMsg?: string | null
  textSnippet?: string | null
  /** 发票号码 */
  invoiceNo?: string | null
  /** 开票日期 yyyy-MM-dd */
  invoiceDate?: string | null
}
export function parseInvoicePdf(file: File): Promise<any> {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/v1/material/items/parse-invoice-pdf',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 清除附件（保留资料项；传 stage 时仅清除本环节上传及无环节标记的附件，前序环节文件保留）
 */
export function clearMaterialFile(id: number | string, stage?: string | null) {
  return request({
    url: `/v1/material/items/${id}/file`,
    method: 'delete',
    params: stage ? { stage } : undefined
  })
}

/**
 * 删除单个附件（传 stage 时后端校验：前序环节上传的附件不可在当前环节删除）
 */
export function deleteMaterialAttachment(itemId: number | string, attachmentId: number | string, stage?: string | null) {
  return request({
    url: `/v1/material/items/${itemId}/file/${attachmentId}`,
    method: 'delete',
    params: stage ? { stage } : undefined
  })
}

/**
 * 获取资料项附件列表
 */
export function getMaterialAttachments(itemId: number | string) {
  return request({
    url: `/v1/material/items/${itemId}/files`,
    method: 'get'
  })
}

/**
 * 更新附件结构化字段（金额/发票号/开票日期等）
 */
export function updateMaterialAttachment(itemId: number | string, attachmentId: number | string, data: Partial<MaterialAttachment>) {
  return request({
    url: `/v1/material/items/${itemId}/file/${attachmentId}`,
    method: 'put',
    data
  })
}

/**
 * 提交资料（完成 materialSubmit 任务）
 * @param skipRequiredCheck 为true时必填不全不报错，走豁免流程
 */
export function submitMaterial(formId: number | string, skipRequiredCheck = false) {
  return request({
    url: '/v1/material/items/submit',
    method: 'post',
    params: { formId, skipRequiredCheck }
  })
}

/**
 * 资料审核（完成 materialAudit 任务）
 * @param data.result 1=通过 2=驳回
 */
export function auditMaterial(data: { formId: number | string; result: 1 | 2; remark?: string }) {
  return request({
    url: '/v1/material/items/audit',
    method: 'post',
    data
  })
}

/**
 * 提交补充资料（完成 supplementSubmit 任务）
 */
export function submitSupplement(formId: number | string) {
  return request({
    url: '/v1/material/items/supplement/submit',
    method: 'post',
    params: { formId }
  })
}

/**
 * 补充资料审核（完成 supplementAudit 任务）
 * @param data.result 1=通过 2=驳回
 */
export function auditSupplement(data: { formId: number | string; result: 1 | 2; remark?: string }) {
  return request({
    url: '/v1/material/items/supplement/audit',
    method: 'post',
    data
  })
}

/**
 * 提交申请开票金额（完成 invoiceAmountSubmit 任务）
 */
export function submitInvoiceAmount(formId: number | string) {
  return request({
    url: '/v1/material/items/invoice-amount/submit',
    method: 'post',
    params: { formId }
  })
}

/**
 * 开票金额审核（完成 invoiceAmountAudit 任务）
 * @param data.result 1=通过 2=驳回
 */
export function auditInvoiceAmount(data: { formId: number | string; result: 1 | 2; remark?: string }) {
  return request({
    url: '/v1/material/items/invoice-amount/audit',
    method: 'post',
    data
  })
}

/**
 * 获取开票金额（瘦身版接口，仅返回 invoiceAmount，计算明细不再下发）
 */
export function getInvoiceAmountDetail(formId: number | string) {
  return request({
    url: '/v1/material/items/invoice-amount/calculate',
    method: 'get',
    params: { formId }
  })
}

/**
 * 提交业务发票（完成 invoiceSubmit 任务）
 */
export function submitInvoice(formId: number | string) {
  return request({
    url: '/v1/material/items/invoice/submit',
    method: 'post',
    params: { formId }
  })
}

/**
 * 业务发票审核（完成 invoiceAudit 任务）
 * @param data.result 1=通过 2=驳回
 */
export function auditInvoice(data: { formId: number | string; result: 1 | 2; remark?: string }) {
  return request({
    url: '/v1/material/items/invoice/audit',
    method: 'post',
    data
  })
}

/**
 * 通用阶段提交（字典驱动，stage = form_section 字典的 submitKey）
 * @param skipRequiredCheck 为true时必填不全不报错，走豁免流程
 */
export function submitStage(formId: number | string, stage: string, skipRequiredCheck = false) {
  return request({
    url: '/v1/material/items/stage/submit',
    method: 'post',
    params: { formId, stage, skipRequiredCheck }
  })
}

/**
 * 通用阶段审核（字典驱动，stage = form_section 字典的 auditTaskKey）
 */
export function auditStage(data: {
  formId: number | string
  stage: string
  result: 1 | 2
  remark?: string
}) {
  return request({
    url: '/v1/material/items/stage/audit',
    method: 'post',
    data
  })
}

// ==================== 豁免审批 API ====================

/** 查询指定申报单的豁免记录 */
export function getExemptionList(formId: number | string) {
  return request({
    url: '/v1/material/exemption/list',
    method: 'get',
    params: { formId }
  })
}

/** 查询单条豁免记录详情 */
export function getExemptionDetail(id: number | string) {
  return request({
    url: '/v1/material/exemption/detail',
    method: 'get',
    params: { id }
  })
}

/** 查询所有待审核的豁免记录 */
export function getPendingExemptions() {
  return request({
    url: '/v1/material/exemption/pending',
    method: 'get'
  })
}

/** 审核豁免（通过/驳回） */
export function auditExemption(data: { id: number | string; result: 1 | 2; remark?: string }) {
  return request({
    url: '/v1/material/exemption/audit',
    method: 'post',
    data
  })
}

/** 查询豁免流程当前审核步骤 */
export function getExemptionCurrentTask(exemptionId: number | string) {
  return request({
    url: '/v1/material/exemption/current-task',
    method: 'get',
    params: { exemptionId }
  })
}

/** 批量查询待审核豁免（formId → exemptionId 映射） */
export function getBatchPendingExemptions(formIds: string) {
  return request({
    url: '/v1/material/exemption/batch-pending',
    method: 'get',
    params: { formIds }
  })
}

// ==================== 资料补交流程 API ====================

/** 资料补交记录（独立于主流程的轻量状态机） */
export interface MaterialSupplement {
  id: number | string
  formId: number | string
  reason?: string
  /** -1-草稿（已发起未提交审核，审核人不可见） 0-补交中（待审核） 1-通过 2-驳回 */
  status: number
  initiatorId?: number | string | null
  auditorId?: number | string | null
  auditRemark?: string | null
  createTime?: string | null
  auditTime?: string | null
  initiatorName?: string | null
  auditorName?: string | null
  formNo?: string | null
}

/** 发起资料补交（创建草稿补交单，审核人不可见；上传完增量后需再调提交接口；免弹窗发起，reason 可为空后补） */
export function startMaterialSupplement(data: { formId: number | string; reason: string }) {
  return request({
    url: '/v1/material-supplement',
    method: 'post',
    data
  })
}

/** 更新补交原因（仅草稿态：发起免弹窗，原因可在上传资料过程中内联补填） */
export function updateMaterialSupplementReason(id: number | string, reason: string) {
  return request({
    url: `/v1/material-supplement/${id}/reason`,
    method: 'post',
    data: { reason }
  })
}

/** 提交补交审核：草稿转补交中，审核人才可见 */
export function submitMaterialSupplement(id: number | string) {
  return request({
    url: `/v1/material-supplement/${id}/submit`,
    method: 'post'
  })
}

/** 查询某申报单当前补交单（优先在途，其次草稿；无则 data 为 null） */
export function getCurrentSupplement(formId: number | string) {
  return request({
    url: '/v1/material-supplement/current',
    method: 'get',
    params: { formId }
  })
}

/** 查询某申报单在途的补交单（无则 data 为 null） */
export function getActiveSupplement(formId: number | string) {
  return request({
    url: '/v1/material-supplement/active',
    method: 'get',
    params: { formId }
  })
}

/** 批量查询在途补交单（列表页用，返回 formId -> supplementId 映射） */
export function getBatchActiveSupplements(ids: string) {
  return request({
    url: '/v1/material-supplement/batch-active',
    method: 'get',
    params: { ids }
  })
}

/** 审核人待审补交列表（declarationType 可选：SELF-内部/EXTERNAL-外部，不传查全部） */
export function getPendingSupplements(params?: { declarationType?: string }) {
  return request({
    url: '/v1/material-supplement/pending-list',
    method: 'get',
    params
  })
}

/** 取消草稿补交单（仅草稿态，删除草稿期增量后作废补交单） */
export function cancelMaterialSupplement(id: number | string) {
  return request({
    url: `/v1/material-supplement/${id}/cancel`,
    method: 'post'
  })
}

/** 补交增量明细（supplement_id 命中的资料项 + 附件） */
export function getSupplementIncrements(id: number | string) {
  return request({
    url: `/v1/material-supplement/${id}/increments`,
    method: 'get'
  })
}

/** 补交历史：某申报单每一次补交的记录与文件快照（哪一次补交了哪些文件） */
export function getSupplementHistory(formId: number | string) {
  return request({
    url: '/v1/material-supplement/history',
    method: 'get',
    params: { formId }
  })
}

/** 审核补交：approved=true 增量转正；false 删除增量 */
export function auditMaterialSupplement(id: number | string, approved: boolean, remark?: string) {
  return request({
    url: `/v1/material-supplement/${id}/audit`,
    method: 'post',
    params: remark ? { approved, remark } : { approved }
  })
}
