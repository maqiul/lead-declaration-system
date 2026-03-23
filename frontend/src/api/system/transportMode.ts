import request from '@/utils/request'

// 运输方式相关API

/**
 * 运输方式接口定义
 */
export interface TransportMode {
  id?: number
  name: string
  chineseName: string
  code: string
  description: string
  status: number
  sort: number
  createTime?: string
}

/**
 * 分页查询运输方式
 * @param params 查询参数 (page, size, keyword, status)
 */
export function getTransportModeList(params: any) {
  return request({
    url: '/v1/transport-modes',
    method: 'get',
    params
  })
}

/**
 * 获取启用的运输方式列表
 */
export function getEnabledTransportModes() {
  return request({
    url: '/v1/transport-modes/enabled',
    method: 'get'
  })
}

/**
 * 新增运输方式
 * @param data 运输方式数据
 */
export function addTransportMode(data: TransportMode) {
  return request({
    url: '/v1/transport-modes',
    method: 'post',
    data
  })
}

/**
 * 修改运输方式
 * @param id 运输方式ID
 * @param data 运输方式数据
 */
export function updateTransportMode(id: number, data: TransportMode) {
  return request({
    url: `/v1/transport-modes/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除运输方式
 * @param id 运输方式ID
 */
export function deleteTransportMode(id: number) {
  return request({
    url: `/v1/transport-modes/${id}`,
    method: 'delete'
  })
}

/**
 * 切换运输方式状态
 * @param id 运输方式ID
 */
export function toggleTransportModeStatus(id: number) {
  return request({
    url: `/v1/transport-modes/${id}/toggle-status`,
    method: 'post'
  })
}
