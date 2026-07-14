import request from '@/utils/request'

/**
 * 出款水单管理 API
 */

// 出款水单类型
export interface PaymentRemittance {
  id?: number
  formId?: number
  paymentNo?: string
  payeeName?: string
  paymentDate?: string
  paymentAmount?: number
  currency?: string
  bankAccountId?: number
  bankAccountName?: string
  photoUrl?: string
  remarks?: string
  status?: number // 0-草稿 1-待审核 2-已审核 3-已驳回
  auditRemark?: string
  auditBy?: number
  auditByName?: string
  auditTime?: string
  submitTime?: string
  createTime?: string
  updateTime?: string
  totalRelatedAmount?: number
}

// 出款水单查询参数
export interface PaymentRemittanceQueryParams {
  current: number
  size: number
  status?: number
  paymentNo?: string
  relationStatus?: string // UNRELATED-未关联 RELATED-已关联 PARTIAL-未完全关联
}

// 出款水单关联申报单
export interface PaymentRemittanceFormRelation {
  relationId?: number
  formId: number
  formNo?: string
  relationType?: number
  relationAmount?: number
  createTime?: string
}

/** 获取出款水单列表(分页) */
export function getPaymentRemittanceList(params: PaymentRemittanceQueryParams) {
  return request({
    url: '/v1/payment-remittances',
    method: 'get',
    params
  })
}

/** 获取出款水单详情 */
export function getPaymentRemittanceDetail(id: number) {
  return request({
    url: `/v1/payment-remittances/${id}`,
    method: 'get'
  })
}

/** 创建出款水单 */
export function createPaymentRemittance(data: Partial<PaymentRemittance>) {
  return request({
    url: '/v1/payment-remittances',
    method: 'post',
    data
  })
}

/** 更新出款水单(草稿状态) */
export function updatePaymentRemittance(id: number, data: Partial<PaymentRemittance>) {
  return request({
    url: `/v1/payment-remittances/${id}`,
    method: 'put',
    data
  })
}

/** 删除出款水单(草稿状态) */
export function deletePaymentRemittance(id: number) {
  return request({
    url: `/v1/payment-remittances/${id}`,
    method: 'delete'
  })
}

/** 提交出款水单审核 */
export function submitPaymentRemittanceAudit(id: number) {
  return request({
    url: `/v1/payment-remittances/${id}/submit`,
    method: 'post'
  })
}

/** 审核出款水单 */
export function auditPaymentRemittance(id: number, data: {
  approved: boolean
  bankAccountId?: number
  auditRemark?: string
}) {
  return request({
    url: `/v1/payment-remittances/${id}/audit`,
    method: 'post',
    params: data
  })
}

/** 关联申报单 */
export function relateToForm(remittanceId: number, formId: number, amount?: number, relationType?: number) {
  return request({
    url: `/v1/payment-remittances/${remittanceId}/relate-form`,
    method: 'post',
    params: { formId, amount, relationType }
  })
}

/** 取消关联申报单 */
export function unrelateFromForm(remittanceId: number, formId: number) {
  return request({
    url: `/v1/payment-remittances/${remittanceId}/unrelate-form`,
    method: 'delete',
    params: { formId }
  })
}

/** 获取出款水单关联的所有申报单 */
export function getRelatedForms(remittanceId: number) {
  return request({
    url: `/v1/payment-remittances/${remittanceId}/related-forms`,
    method: 'get'
  })
}

/** 获取申报单关联的所有出款水单 */
export function getPaymentRemittancesByFormId(formId: number) {
  return request({
    url: `/v1/payment-remittances/form/${formId}`,
    method: 'get'
  })
}

/** 反审核出款水单 */
export function revokePaymentRemittanceAudit(id: number) {
  return request({
    url: `/v1/payment-remittances/${id}/revoke-audit`,
    method: 'post'
  })
}
