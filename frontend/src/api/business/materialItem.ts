import request from '@/utils/request'

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
 * 清除附件（保留资料项）
 */
export function clearMaterialFile(id: number | string) {
  return request({
    url: `/v1/material/items/${id}/file`,
    method: 'delete'
  })
}

/**
 * 提交资料（完成 materialSubmit 任务）
 */
export function submitMaterial(formId: number | string) {
  return request({
    url: '/v1/material/items/submit',
    method: 'post',
    params: { formId }
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
