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
/** 资料环节类型：默认三环节 + 字典动态扩展 */
export type MaterialStage = typeof MATERIAL_STAGES[number]['value'] | (string & {})

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
 * 绑定规则（流程 + 运输方式）
 */
export interface MaterialTemplateBinding {
  id?: number | string
  templateId?: number | string
  flowTemplateCode?: string
  transportModeCode?: string
  /** 是否必填（undefined=模板默认, 1=必填, 0=选填） */
  required?: number
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
  /** 发票分类: DEDUCTION-扣款 INPUT-进项 */
  invoiceCategory?: string
  /** 绑定规则（空数组或未设置 = 全部适用） */
  bindings?: MaterialTemplateBinding[]
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

/**
 * 获取模板绑定规则
 */
export function getTemplateBindings(id: number | string) {
  return request({
    url: `/v1/material/templates/${id}/bindings`,
    method: 'get'
  })
}

/**
 * 设置模板绑定规则
 */
export function saveTemplateBindings(id: number | string, bindings: MaterialTemplateBinding[]) {
  return request({
    url: `/v1/material/templates/${id}/bindings`,
    method: 'put',
    data: bindings
  })
}
