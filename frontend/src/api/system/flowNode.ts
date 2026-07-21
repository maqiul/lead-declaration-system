import request from '@/utils/request'

/** 流程节点接口定义 */
export interface FlowNode {
  id?: number
  nodeKey: string            // BPMN taskDefinitionKey
  nodeName: string           // 节点中文名
  nodeType: string           // userTask / serviceTask
  assignee?: string          // 办理人表达式
  candidateGroups?: string   // 候选组
  targetStatus?: number      // 目标状态
  formSection?: string       // 表单区块
  isSystem?: number          // 是否系统节点 0/1
  processType?: string       // 所属流程类型
  delegateExpression?: string // serviceTask 委托表达式
  rejectToEnd?: number        // 驳回时是否直接结束流程 0-否 1-是
  description?: string       // 说明
  createTime?: string
  updateTime?: string
}

/** 获取所有节点列表（可按流程类型过滤） */
export function getFlowNodeList(processType?: string) {
  return request({ url: '/v1/flow-nodes', method: 'get', params: processType ? { processType } : {} })
}

/** 获取可用于编排的业务节点（可按流程类型过滤） */
export function getFlowNodeUserTasks(processType?: string) {
  return request({ url: '/v1/flow-nodes/user-tasks', method: 'get', params: processType ? { processType } : {} })
}

/** 获取可编排的节点（userTask + serviceTask，可按流程类型过滤） */
export function getFlowNodeOrchestratable(processType?: string) {
  return request({ url: '/v1/flow-nodes/orchestratable', method: 'get', params: processType ? { processType } : {} })
}

/** 获取节点详情 */
export function getFlowNodeDetail(id: number) {
  return request({ url: `/v1/flow-nodes/${id}`, method: 'get' })
}

/** 创建节点 */
export function createFlowNode(data: FlowNode) {
  return request({ url: '/v1/flow-nodes', method: 'post', data })
}

/** 更新节点 */
export function updateFlowNode(id: number, data: Partial<FlowNode>) {
  return request({ url: `/v1/flow-nodes/${id}`, method: 'put', data })
}

/** 删除节点 */
export function deleteFlowNode(id: number) {
  return request({ url: `/v1/flow-nodes/${id}`, method: 'delete' })
}
