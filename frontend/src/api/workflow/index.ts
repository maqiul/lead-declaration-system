import request from '@/utils/request'

// 流程定义相关API
export interface DefinitionQueryParams {
  processName?: string
  processKey?: string
  category?: string
}

export interface ProcessDefinitionForm {
  id?: number
  name: string
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
  formData.append('name', processInfo.name)
  formData.append('processKey', processInfo.processKey)
  formData.append('category', processInfo.category)
  formData.append('description', processInfo.description)
  
  return request({
    url: '/workflow/deploy',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取流程定义列表
export function getProcessDefinitions(params?: DefinitionQueryParams) {
  return request({
    url: '/workflow/definitions',
    method: 'get',
    params
  })
}

// 流程定义相关API
export interface DefinitionQueryParams {
  pageNum?: number
  pageSize?: number
  processName?: string
  processKey?: string
  category?: string
  status?: number
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
}

// 获取流程定义列表
export function getDefinitionList(params: DefinitionQueryParams) {
  return request({
    url: '/workflow/definition',
    method: 'get',
    params
  })
}



// 流程实例相关API
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
export function getMyProcessInstances() {
  return request({
    url: '/workflow/instances/my',
    method: 'get'
  })
}

// 获取运行中的流程实例
export function getRunningProcessInstances() {
  return request({
    url: '/workflow/instances/running',
    method: 'get'
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
}

// 获取我的待办任务
export function getMyAssignedTasks() {
  return request({
    url: '/workflow/tasks/assigned',
    method: 'get'
  })
}

// 获取我的候选任务
export function getMyCandidateTasks() {
  return request({
    url: '/workflow/tasks/candidate',
    method: 'get'
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