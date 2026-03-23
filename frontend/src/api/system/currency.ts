import request from '@/utils/request'

// 货币管理相关API

/**
 * 货币信息接口定义
 */
export interface CurrencyInfo {
  id?: number
  currencyCode: string       // 货币代码
  currencyName: string       // 英文名称
  chineseName: string        // 中文名称
  unitCn: string             // 中文单位
  symbol: string             // 货币符号
  status: number             // 状态
  sort: number               // 排序
  createTime?: string
  updateTime?: string
}

/**
 * 分页查询货币列表
 * @param params 查询参数 (page, size, keyword, status)
 */
export function getCurrencyList(params: any) {
  return request({
    url: '/v1/currencies',
    method: 'get',
    params
  })
}

/**
 * 获取启用的货币列表
 */
export function getEnabledCurrencies() {
  return request({
    url: '/v1/currencies/enabled',
    method: 'get'
  })
}

/**
 * 获取货币详情
 * @param id 货币ID
 */
export function getCurrencyDetail(id: number) {
  return request({
    url: `/v1/currencies/${id}`,
    method: 'get'
  })
}

/**
 * 新增货币
 * @param data 货币数据
 */
export function addCurrency(data: CurrencyInfo) {
  return request({
    url: '/v1/currencies',
    method: 'post',
    data
  })
}

/**
 * 修改货币
 * @param id 货币ID
 * @param data 货币数据
 */
export function updateCurrency(id: number, data: CurrencyInfo) {
  return request({
    url: `/v1/currencies/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除货币
 * @param id 货币ID
 */
export function deleteCurrency(id: number) {
  return request({
    url: `/v1/currencies/${id}`,
    method: 'delete'
  })
}

/**
 * 切换货币状态
 * @param id 货币ID
 */
export function toggleCurrencyStatus(id: number) {
  return request({
    url: `/v1/currencies/${id}/toggle-status`,
    method: 'post'
  })
}
