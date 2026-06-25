import request from '@/utils/request'

/**
 * 资料模板所属环节枚举：
 *  - MATERIAL_SUBMIT     资料上传
 *  - SUPPLEMENT          补充资料
 *  - INVOICE             业务发票
 */
export const MATERIAL_STAGES = [
  { value: 'MATERIAL_SUBMIT', label: '资料上传', color: 'green' },
  { value: 'SUPPLEMENT', label: '补充资料', color: 'orange' },
  { value: 'INVOICE', label: '业务发票', color: 'blue' }
] as const
export type MaterialStage = typeof MATERIAL_STAGES[number]['value']

export const MATERIAL_STAGE_LABEL: Record<MaterialStage, string> = {
  MATERIAL_SUBMIT: '资料上传',
  SUPPLEMENT: '补充资料',
  INVOICE: '业务发票'
}

export const MATERIAL_STAGE_COLOR: Record<MaterialStage, string> = {
  MATERIAL_SUBMIT: 'green',
  SUPPLEMENT: 'orange',
  INVOICE: 'blue'
}

/**
 * 申报资料项模板
 */
export interface MaterialTemplate {
  id?: number | string
  code: string
  name: string
  required: number
  sort: number
  remark?: string
  formSchema?: string | null
  enabled: number
  stage?: MaterialStage
  /** 发票模式: 0-普通附件 1-附件级金额/发票号/日期 */
  invoiceMode?: number
  createTime?: string
  updateTime?: string
}

/**
 * 结构化字段配置项定义
 */
export interface MaterialSchemaField {
  key: string
  label: string
  type: 'text' | 'number' | 'date' | 'select'
  required?: boolean
  options?: string[]
}

/** 发票类默认 schema 预设 */
export const INVOICE_SCHEMA_PRESET: MaterialSchemaField[] = [
  { key: 'amount', label: '发票金额', type: 'number', required: true },
  { key: 'invoiceNo', label: '发票号', type: 'text', required: true },
  { key: 'invoiceDate', label: '开票日期', type: 'date', required: true }
]

/**
 * 获取资料项模板列表
 * @param params.enabled 启用状态筛选
 * @param params.stage   环节筛选
 */
export function getMaterialTemplateList(params?: { enabled?: number; stage?: MaterialStage | string }) {
  return request({
    url: '/v1/material/templates',
    method: 'get',
    params
  })
}

/**
 * 新增资料项模板
 */
export function addMaterialTemplate(data: MaterialTemplate) {
  return request({
    url: '/v1/material/templates',
    method: 'post',
    data
  })
}

/**
 * 修改资料项模板
 */
export function updateMaterialTemplate(data: MaterialTemplate) {
  return request({
    url: '/v1/material/templates',
    method: 'put',
    data
  })
}

/**
 * 删除资料项模板
 */
export function deleteMaterialTemplate(id: number | string) {
  return request({
    url: `/v1/material/templates/${id}`,
    method: 'delete'
  })
}
