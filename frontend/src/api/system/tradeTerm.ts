import request from '@/utils/request'

// 贸易方式(Incoterms)相关API

export interface TradeTerm {
  id?: number | string
  code: string
  name: string
  chineseName: string
  transportScope: string
  groupName: string
  description: string
  sort: number
  status: number
  createTime?: string
  /** 关联的运输方式代码列表 */
  transportModes?: string[]
}

export function getTradeTermList(params: any) {
  return request({
    url: '/v1/trade-terms',
    method: 'get',
    params
  })
}

export function getEnabledTradeTerms() {
  return request({
    url: '/v1/trade-terms/enabled',
    method: 'get'
  })
}

export function addTradeTerm(data: TradeTerm) {
  return request({
    url: '/v1/trade-terms',
    method: 'post',
    data
  })
}

export function updateTradeTerm(id: number, data: TradeTerm) {
  return request({
    url: `/v1/trade-terms/${id}`,
    method: 'put',
    data
  })
}

export function deleteTradeTerm(id: number) {
  return request({
    url: `/v1/trade-terms/${id}`,
    method: 'delete'
  })
}

export function toggleTradeTermStatus(id: number) {
  return request({
    url: `/v1/trade-terms/${id}/toggle-status`,
    method: 'post'
  })
}
