import request from '@/utils/request'

// 常用客户配置相关API

/**
 * 常用客户接口定义
 */
export interface CustomerConfig {
  id?: number
  userId?: number
  customerName: string        // 收货人公司名
  customerAddress: string     // 收货人地址
  destinationCountry?: string // 目的国
  tradeCountry?: string       // 贸易国
  sort?: number
  status?: number
  createTime?: string
  updateTime?: string
}

/**
 * 分页查询当前用户的客户
 */
export function getCustomerList(params: any) {
  return request({
    url: '/v1/customers',
    method: 'get',
    params
  })
}

/**
 * 获取当前用户所有启用客户（下拉用）
 */
export function getAllEnabledCustomers() {
  return request({
    url: '/v1/customers/all',
    method: 'get'
  })
}

/**
 * 获取客户详情
 */
export function getCustomerDetail(id: number) {
  return request({
    url: `/v1/customers/${id}`,
    method: 'get'
  })
}

/**
 * 新增客户
 */
export function addCustomer(data: CustomerConfig) {
  return request({
    url: '/v1/customers',
    method: 'post',
    data
  })
}

/**
 * 修改客户
 */
export function updateCustomer(id: number, data: CustomerConfig) {
  return request({
    url: `/v1/customers/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除客户
 */
export function deleteCustomer(id: number) {
  return request({
    url: `/v1/customers/${id}`,
    method: 'delete'
  })
}
