import request from '@/utils/request'

// 流程定义相关API
export interface DefinitionQueryParams {
  processName?: string
  processKey?: string
  category?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface ProcessDefinitionForm {
  id?: number
  processName: string
  processKey: string
  description: string
  category: string
}

export interface ProcessDefinition {
  id: number
  processKey: string
  processName: string
  description: string
  category: string
  version: number
  status: number
  createTime: string
  deploymentId: string
}

// 部署流程定义
export function deployProcessDefinition(file: File, processInfo: ProcessDefinitionForm) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('processName', processInfo.processName)
  formData.append('processKey', processInfo.processKey)
  formData.append('category', processInfo.category)
  formData.append('description', processInfo.description)
  
  return request({
    url: '/workflow/definition/deploy',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function getDefinitionList(params: DefinitionQueryParams) {
  return request({
    url: '/workflow/definition',
    method: 'get',
    params
  })
}

// 启用/激活流程定义
export function enableProcessDefinition(id: string) {
  return request({
    url: `/workflow/definition/enable/${id}`,
    method: 'post'
  })
}

// 停用/挂起流程定义
export function disableProcessDefinition(id: string) {
  return request({
    url: `/workflow/definition/disable/${id}`,
    method: 'post'
  })
}

// 删除流程定义
export function deleteProcessDefinition(id: string) {
  return request({
    url: `/workflow/definition/${id}`,
    method: 'delete'
  })
}

// 获取单个流程定义详细内容(XML)
export function getProcessDefinitionXml(processKey: string) {
  return request({
    url: `/workflow/definition/xml/${processKey}`,
    method: 'get'
  })
}

export interface InstanceQueryParams {
  processName?: string
  starterName?: string
  status?: number
}

export interface ProcessInstance {
  id: number
  instanceId: string
  processDefinitionId: string
  processDefinitionKey: string
  processDefinitionName: string
  businessKey: string
  starterId: number
  starterName: string
  currentActivityId: string
  currentActivityName: string
  status: number
  startTime: string
  endTime: string | null
  deleteReason: string | null
}

// 获取我的流程实例
export function getMyProcessInstances(params?: { pageNum?: number; pageSize?: number; processName?: string }) {
  return request({
    url: '/workflow/instances/my',
    method: 'get',
    params
  })
}

// 获取运行中的流程实例
export function getRunningProcessInstances(params?: InstanceQueryParams & { pageNum?: number; pageSize?: number }) {
  return request({
    url: '/workflow/instances/running',
    method: 'get',
    params
  })
}

// 启动流程实例
export function startProcessInstance(processDefinitionKey: string, businessKey?: string, variables?: Record<string, any>) {
  return request({
    url: '/workflow/instance/start',
    method: 'post',
    data: variables,
    params: {
      processDefinitionKey,
      businessKey
    }
  })
}

// 挂起流程实例
export function suspendProcessInstance(instanceId: string) {
  return request({
    url: `/workflow/instance/suspend/${instanceId}`,
    method: 'post'
  })
}

// 激活流程实例
export function activateProcessInstance(instanceId: string) {
  return request({
    url: `/workflow/instance/activate/${instanceId}`,
    method: 'post'
  })
}

// 终止流程实例
export function terminateProcessInstance(instanceId: string, reason: string) {
  return request({
    url: `/workflow/instance/terminate/${instanceId}`,
    method: 'post',
    params: { reason }
  })
}

// 获取流程实例任务列表
export function getTasksByProcessInstance(instanceId: string) {
  return request({
    url: `/workflow/instance/tasks/${instanceId}`,
    method: 'get'
  })
}

// 任务相关API
export interface TaskInstance {
  id: number
  taskId: string
  taskName: string
  processInstanceId: string
  processDefinitionId: string
  activityId: string
  activityName: string
  assigneeId: number
  assigneeName: string
  status: number
  createTime: string
  claimTime: string | null
  endTime: string | null
  dueTime: string | null
  description: string | null
  priority?: number
  processDefinitionName?: string
}

// 获取我的待办任务
export function getMyAssignedTasks(params?: { pageNum?: number; pageSize?: number; taskName?: string; processName?: string }) {
  return request({
    url: '/workflow/tasks/assigned',
    method: 'get',
    params
  })
}

// 获取我的候选任务
export function getMyCandidateTasks(params?: { pageNum?: number; pageSize?: number; taskName?: string; processName?: string }) {
  return request({
    url: '/workflow/tasks/candidate',
    method: 'get',
    params
  })
}

// 获取我的已完成任务
export function getMyCompletedTasks(params?: { pageNum?: number; pageSize?: number; taskName?: string; processName?: string }) {
  return request({
    url: '/workflow/tasks/completed',
    method: 'get',
    params
  })
}

// 签收任务
export function claimTask(taskId: string) {
  return request({
    url: `/workflow/task/claim/${taskId}`,
    method: 'post'
  })
}

// 完成任务
export function completeTask(taskId: string, variables?: Record<string, any>) {
  return request({
    url: `/workflow/task/complete/${taskId}`,
    method: 'post',
    data: variables
  })
}

// 驳回任务
export function rejectTask(taskId: string, targetActivityId: string, reason: string) {
  return request({
    url: `/workflow/task/reject/${taskId}`,
    method: 'post',
    params: {
      targetActivityId,
      reason
    }
  })
}

// 转办任务
export function transferTask(taskId: string, assigneeId: number) {
  return request({
    url: `/workflow/task/transfer/${taskId}`,
    method: 'post',
    params: { assigneeId }
  })
}

// 获取流程图
export function getProcessDiagram(deploymentId: string) {
  return request({
    url: `/workflow/process-definition/${deploymentId}/diagram`,
    method: 'get',
    responseType: 'blob'
  })
}

// 监控相关API
export function getMonitorStats() {
  return request({
    url: '/workflow/monitor/stats',
    method: 'get'
  })
}

export function getAllActiveTasks(params?: { pageNum?: number; pageSize?: number; taskName?: string; processName?: string }) {
  return request({
    url: '/workflow/monitor/tasks',
    method: 'get',
    params
  })
}

export function getMonitorCharts() {
  return request({
    url: '/workflow/monitor/charts',
    method: 'get'
  })
}