import request from '@/utils/request'

// 支付方式相关API

/**
 * 支付方式接口定义
 */
export interface PaymentMethod {
  id?: number | string
  name: string
  chineseName: string
  code: string
  description: string
  status: number
  sort: number
  createTime?: string
}

/**
 * 分页查询支付方式
 * @param params 查询参数 (page, size, keyword, status)
 */
export function getPaymentMethodList(params: any) {
  return request({
    url: '/v1/payment-methods',
    method: 'get',
    params
  })
}

/**
 * 获取启用的支付方式列表
 */
export function getEnabledPaymentMethods() {
  return request({
    url: '/v1/payment-methods/enabled',
    method: 'get'
  })
}

/**
 * 新增支付方式
 * @param data 支付方式数据
 */
export function addPaymentMethod(data: PaymentMethod) {
  return request({
    url: '/v1/payment-methods',
    method: 'post',
    data
  })
}

/**
 * 修改支付方式
 * @param id 支付方式ID
 * @param data 支付方式数据
 */
export function updatePaymentMethod(id: number, data: PaymentMethod) {
  return request({
    url: `/v1/payment-methods/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除支付方式
 * @param id 支付方式ID
 */
export function deletePaymentMethod(id: number) {
  return request({
    url: `/v1/payment-methods/${id}`,
    method: 'delete'
  })
}

/**
 * 启停支付方式状态
 * @param id 支付方式ID
 * @param status 状态 0-禁用 1-启用
 */
export function togglePaymentMethodStatus(id: number, status: number) {
  return request({
    url: `/v1/payment-methods/${id}/toggle-status`,
    method: 'post',
    params: { status }
  })
}
