import request from '@/utils/request'

/**
 * 水单管理API
 */

// 水单数据类型
export interface Remittance {
  id?: number
  remittanceNo?: string
  /**
   * @deprecated 不再有定金/尾款分类,保留仅用于数据兼容
   */
  remittanceType?: number // 已废弃: 1-定金 2-尾款
  remittanceName: string
  remittanceDate: string
  remittanceAmount: number
  currency?: string
  taxRate?: number
  bankAccountId?: number
  bankAccountName?: string
  bankFeeRate?: number
  bankFee?: number
  creditedAmount?: number
  remarks?: string
  photoUrl?: string
  status?: number // 0-草稿 1-待审核 2-已审核 3-已驳回
  auditRemark?: string
  auditBy?: number
  auditByName?: string
  auditTime?: string
  submitTime?: string
  createTime?: string
  updateTime?: string
}

// 水单查询参数
export interface RemittanceQueryParams {
  current: number
  size: number
  /**
   * @deprecated 不再有定金/尾款分类,保留仅用于数据兼容
   */
  remittanceType?: number // 已废弃
  status?: number
  remittanceNo?: string
}

// 水单关联申报单
export interface RemittanceFormRelation {
  relationId?: number
  formId: number
  formNo?: string
  relationType?: number
  relationAmount?: number
  createTime?: string
}

/**
 * 获取水单列表(分页)
 */
export function getRemittanceList(params: RemittanceQueryParams) {
  return request({
    url: '/v1/remittances',
    method: 'get',
    params
  })
}

/**
 * 获取水单详情
 */
export function getRemittanceDetail(id: number) {
  return request({
    url: `/v1/remittances/${id}`,
    method: 'get'
  })
}

/**
 * 创建水单
 */
export function createRemittance(data: Partial<Remittance>) {
  return request({
    url: '/v1/remittances',
    method: 'post',
    data
  })
}

/**
 * 更新水单(草稿状态)
 */
export function updateRemittance(id: number, data: Partial<Remittance>) {
  return request({
    url: `/v1/remittances/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除水单(草稿状态)
 */
export function deleteRemittance(id: number) {
  return request({
    url: `/v1/remittances/${id}`,
    method: 'delete'
  })
}

/**
 * 提交水单审核
 */
export function submitRemittanceAudit(id: number) {
  return request({
    url: `/v1/remittances/${id}/submit`,
    method: 'post'
  })
}

/**
 * 审核水单
 */
export function auditRemittance(id: number, data: {
  approved: boolean
  bankAccountId?: number
  taxRate?: number
  auditRemark?: string
}) {
  return request({
    url: `/v1/remittances/${id}/audit`,
    method: 'post',
    params: data
  })
}

/**
 * 关联申报单
 */
export function relateToForm(remittanceId: number, formId: number, amount?: number, relationType?: number) {
  return request({
    url: `/v1/remittances/${remittanceId}/relate-form`,
    method: 'post',
    params: { formId, amount, relationType }
  })
}

/**
 * 取消关联申报单
 */
export function unrelateFromForm(remittanceId: number, formId: number) {
  return request({
    url: `/v1/remittances/${remittanceId}/unrelate-form`,
    method: 'delete',
    params: { formId }
  })
}

/**
 * 获取水单关联的所有申报单
 */
export function getRelatedForms(remittanceId: number) {
  return request({
    url: `/v1/remittances/${remittanceId}/related-forms`,
    method: 'get'
  })
}

/**
 * 获取申报单关联的所有水单
 */
export function getRemittancesByFormId(formId: number) {
  return request({
    url: `/v1/remittances/form/${formId}`,
    method: 'get'
  })
}

/**
 * 计算银行手续费
 */
export function calculateBankFee(bankAccountId: number, amount: number) {
  return request({
    url: '/v1/remittances/calculate-fee',
    method: 'get',
    params: { bankAccountId, amount }
  })
}
