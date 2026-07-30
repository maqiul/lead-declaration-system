import request from '@/utils/request'

/**
 * 资料模板所属环节枚举：
 *  - BASIC               基础资料（草稿/新建阶段上传，独立于资料提交环节）
 *  - MATERIAL_SUBMIT     资料上传
 *  - SUPPLEMENT          补充资料
 *  - INVOICE             业务发票
 */
export const MATERIAL_STAGES = [
  { value: 'BASIC', label: '基础资料', color: 'purple' },
  { value: 'MATERIAL_SUBMIT', label: '资料上传', color: 'green' },
  { value: 'SUPPLEMENT', label: '补充资料', color: 'orange' },
  { value: 'INVOICE', label: '业务发票', color: 'blue' }
] as const
/** 资料环节类型：默认四环节 + 字典动态扩展 */
export type MaterialStage = typeof MATERIAL_STAGES[number]['value'] | (string & {})

export const MATERIAL_STAGE_LABEL: Record<MaterialStage, string> = {
  BASIC: '基础资料',
  MATERIAL_SUBMIT: '资料上传',
  SUPPLEMENT: '补充资料',
  INVOICE: '业务发票'
}

export const MATERIAL_STAGE_COLOR: Record<MaterialStage, string> = {
  BASIC: 'purple',
  MATERIAL_SUBMIT: 'green',
  SUPPLEMENT: 'orange',
  INVOICE: 'blue'
}

/** stage 多环节支持：拆分逗号分隔的 stage 值为环节数组（空值默认 MATERIAL_SUBMIT） */
export function splitStages(stage?: string | null): string[] {
  const v = (stage || 'MATERIAL_SUBMIT').split(',').map(s => s.trim()).filter(Boolean)
  return v.length ? v : ['MATERIAL_SUBMIT']
}

/** stage 值（可能多环节逗号分隔）是否包含目标环节 */
export function hasStage(stage: string | null | undefined, target: string): boolean {
  return splitStages(stage).includes(target)
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
  /** 所属环节，多环节时为逗号分隔字符串，如 'BASIC,MATERIAL_SUBMIT' */
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
