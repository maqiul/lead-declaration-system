import request from '@/utils/request'
import type { FlowNode } from './flowNode'

// ============================================================
// 流程模板管理 API
// ============================================================

/** 模板-节点编排接口定义 */
export interface FlowTemplateNodeItem {
  id?: number
  templateId?: number
  nodeId: number
  enabled: number
  sortOrder: number
  assignee?: string           // 办理人覆盖
  candidateGroups?: string    // 候选组覆盖
  node?: FlowNode          // 关联的节点详情
  createTime?: string
  updateTime?: string
}

/** 流程模板接口定义 */
export interface FlowTemplate {
  id?: number
  name: string           // 模板名称
  code: string           // 模板编码
  description?: string   // 模板说明
  processType?: string   // 流程类型: declaration/remittance/taxRefund
  declarationType?: string // 申报类型: SELF-内部 EXTERNAL-外部
  isDefault: number      // 是否默认 0/1
  status: number         // 状态 0-禁用 1-启用
  createTime?: string
  updateTime?: string
  steps?: FlowTemplateStep[]         // 步骤列表（向下兼容）
  templateNodes?: FlowTemplateNodeItem[]  // 节点编排列表
}

/** 流程模板步骤接口定义 */
export interface FlowTemplateStep {
  id?: number
  templateId?: number
  stepKey: string        // BPMN任务Key
  stepName: string       // 步骤中文名
  enabled: number        // 1=启用 0=跳过
  targetStatus?: number  // 进入该步骤时 form.status 应设为的值
  formSection?: string   // 对应前端表单区块组件标识
  sortOrder: number      // 排序
  createTime?: string
  updateTime?: string
}

/**
 * 获取所有模板列表（可按流程类型过滤）
 */
export function getFlowTemplateList(processType?: string) {
  return request({
    url: '/v1/flow-templates',
    method: 'get',
    params: processType ? { processType } : {}
  })
}

/**
 * 获取模板详情（含步骤配置）
 */
export function getFlowTemplateDetail(id: number) {
  return request({
    url: `/v1/flow-templates/${id}`,
    method: 'get'
  })
}

/**
 * 获取默认模板
 */
export function getDefaultFlowTemplate() {
  return request({
    url: '/v1/flow-templates/default',
    method: 'get'
  })
}

/**
 * 获取模板 skip 标志 Map
 */
export function getFlowTemplateSkipFlags(id: number) {
  return request({
    url: `/v1/flow-templates/${id}/skip-flags`,
    method: 'get'
  })
}

/**
 * 获取模板的步骤配置
 */
export function getFlowTemplateSteps(id: number) {
  return request({
    url: `/v1/flow-templates/${id}/steps`,
    method: 'get'
  })
}

/**
 * 创建流程模板
 */
export function createFlowTemplate(data: FlowTemplate) {
  return request({
    url: '/v1/flow-templates',
    method: 'post',
    data
  })
}

/**
 * 更新流程模板基本信息
 */
export function updateFlowTemplate(id: number, data: Partial<FlowTemplate>) {
  return request({
    url: `/v1/flow-templates/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除流程模板
 */
export function deleteFlowTemplate(id: number) {
  return request({
    url: `/v1/flow-templates/${id}`,
    method: 'delete'
  })
}

/**
 * 批量更新步骤开关配置（全量替换，向下兼容）
 */
export function saveFlowTemplateSteps(id: number, steps: FlowTemplateStep[]) {
  return request({
    url: `/v1/flow-templates/${id}/steps`,
    method: 'put',
    data: steps
  })
}

/**
 * 获取模板的节点编排配置
 */
export function getFlowTemplateNodes(id: number) {
  return request({
    url: `/v1/flow-templates/${id}/nodes`,
    method: 'get'
  })
}

/**
 * 根据模板编码获取节点编排配置（供申报表单页使用）
 */
export function getFlowTemplateNodesByCode(code: string) {
  return request({
    url: `/v1/flow-templates/by-code/${code}/nodes`,
    method: 'get'
  })
}

/**
 * 根据模板编码获取模板详情
 */
export function getFlowTemplateByCode(code: string) {
  return request({
    url: `/v1/flow-templates/by-code/${code}`,
    method: 'get'
  })
}

/**
 * 保存模板的节点编排（全量替换）
 */
export function saveFlowTemplateNodes(id: number, nodes: FlowTemplateNodeItem[]) {
  return request({
    url: `/v1/flow-templates/${id}/nodes`,
    method: 'put',
    data: nodes
  })
}

/**
 * 预览生成的 BPMN XML
 */
export function previewBpmnXml(id: number) {
  return request({
    url: `/v1/flow-templates/${id}/bpmn-preview`,
    method: 'get'
  })
}

/**
 * 生成并部署 BPMN 到 Flowable
 */
export function deployBpmn(id: number) {
  return request({
    url: `/v1/flow-templates/${id}/deploy-bpmn`,
    method: 'post'
  })
}
