import request from '@/utils/request'

// 乙方配置相关API

/**
 * 乙方配置接口定义
 */
export interface PartyBConfig {
  id?: number
  userId?: number
  partyBName: string        // 乙方公司名称
  partyBAddress: string     // 乙方公司地址
  contactPerson?: string    // 联系人
  contactPhone?: string     // 联系电话
  bankName?: string         // 开户银行
  bankAccount?: string      // 银行账号
  taxId?: string            // 纳税人识别号
  sort?: number
  status?: number
  createTime?: string
  updateTime?: string
}

/**
 * 分页查询当前用户的乙方
 */
export function getPartyBList(params: any) {
  return request({
    url: '/v1/party-b-configs',
    method: 'get',
    params
  })
}

/**
 * 获取当前用户所有启用乙方（下拉用）
 */
export function getAllEnabledPartyB() {
  return request({
    url: '/v1/party-b-configs/all',
    method: 'get'
  })
}

/**
 * 获取乙方详情
 */
export function getPartyBDetail(id: number) {
  return request({
    url: `/v1/party-b-configs/${id}`,
    method: 'get'
  })
}

/**
 * 新增乙方
 */
export function addPartyB(data: PartyBConfig) {
  return request({
    url: '/v1/party-b-configs',
    method: 'post',
    data
  })
}

/**
 * 修改乙方
 */
export function updatePartyB(id: number, data: PartyBConfig) {
  return request({
    url: `/v1/party-b-configs/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除乙方
 */
export function deletePartyB(id: number) {
  return request({
    url: `/v1/party-b-configs/${id}`,
    method: 'delete'
  })
}
