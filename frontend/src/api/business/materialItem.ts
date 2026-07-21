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
 * 上传附件
 * 额外可传 formId + templateId，后端在 id 找不到记录时会按模板兄弟 ensure 一条再上传（资料项懒创建兄底）
 */
export function uploadMaterialFile(
  id: number | string,
  file: File,
  extras?: { formId?: number | string | null; templateId?: number | string | null }
) {
  const formData = new FormData()
  formData.append('file', file)
  const params: Record<string, any> = {}
  if (extras?.formId) params.formId = extras.formId
  if (extras?.templateId) params.templateId = extras.templateId
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
 * 清除附件（保留资料项，清空所有附件）
 */
export function clearMaterialFile(id: number | string) {
  return request({
    url: `/v1/material/items/${id}/file`,
    method: 'delete'
  })
}

/**
 * 删除单个附件
 */
export function deleteMaterialAttachment(itemId: number | string, attachmentId: number | string) {
  return request({
    url: `/v1/material/items/${itemId}/file/${attachmentId}`,
    method: 'delete'
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
 * 获取开票金额计算详情
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
