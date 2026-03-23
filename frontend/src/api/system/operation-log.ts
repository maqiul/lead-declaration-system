import request from '@/utils/request'

/**
 * 分页查询操作日志
 */
export function getOperationLogs(params: any) {
  return request({
    url: '/v1/operation-log/page',
    method: 'get',
    params
  })
}

/**
 * 根据ID查询操作日志详情
 */
export function getOperationLogById(id: number) {
  return request({
    url: `/v1/operation-log/${id}`,
    method: 'get'
  })
}

/**
 * 删除操作日志
 */
export function deleteOperationLog(id: number) {
  return request({
    url: `/v1/operation-log/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除操作日志
 */
export function deleteOperationLogs(ids: number[]) {
  return request({
    url: '/v1/operation-log/batch',
    method: 'delete',
    data: ids
  })
}

/**
 * 清理过期日志
 */
export function cleanExpiredLogs(days: number) {
  return request({
    url: '/v1/operation-log/clean',
    method: 'delete',
    params: { days }
  })
}

/**
 * 获取操作日志统计信息
 */
export function getOperationLogStats() {
  return request({
    url: '/v1/operation-log/stats',
    method: 'get'
  })
}