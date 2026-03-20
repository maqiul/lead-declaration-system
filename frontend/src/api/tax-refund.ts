import request from '@/utils/request'

// 退税申请相关接口

/**
 * 文件上传
 * @param file 文件对象
 */
export function uploadTaxRefundFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', 'TaxRefund')
  
  return request({
    url: '/v1/files/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 关联文件到退税申请
 * @param id 申请ID
 * @param fileUrls 文件URL列表
 */
export function attachFilesToApplication(id: number, fileUrls: string[]) {
  return request({
    url: `/v1/tax-refunds/${id}/attach-files`,
    method: 'post',
    data: fileUrls
  })
}

/**
 * 获取退税申请列表
 * @param params 查询参数
 */
export function getTaxRefundList(params: any) {
  return request({
    url: '/v1/tax-refunds',
    method: 'get',
    params
  })
}

/**
 * 获取退税申请详情
 * @param id 申请ID
 */
export function getTaxRefundDetail(id: number) {
  return request({
    url: `/v1/tax-refunds/${id}`,
    method: 'get'
  })
}

/**
 * 创建退税申请
 * @param data 申请数据
 */
export function createTaxRefund(data: any) {
  return request({
    url: '/v1/tax-refunds',
    method: 'post',
    data
  })
}

/**
 * 更新退税申请
 * @param id 申请ID
 * @param data 申请数据
 */
export function updateTaxRefund(id: number, data: any) {
  return request({
    url: `/v1/tax-refunds/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除退税申请
 * @param id 申请ID
 */
export function deleteTaxRefund(id: number) {
  return request({
    url: `/v1/tax-refunds/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除退税申请
 * @param ids 申请ID数组
 */
export function batchDeleteTaxRefund(ids: number[]) {
  return request({
    url: '/v1/tax-refunds/batch',
    method: 'delete',
    data: ids
  })
}

/**
 * 提交退税申请
 * @param id 申请ID
 */
export function submitTaxRefund(id: number) {
  return request({
    url: `/v1/tax-refunds/${id}/submit`,
    method: 'post'
  })
}

/**
 * 保存草稿
 * @param data 草稿数据
 */
export function saveTaxRefundDraft(data: any) {
  return request({
    url: '/v1/tax-refunds/draft',
    method: 'post',
    data
  })
}

/**
 * 获取可关联的申报单列表
 */
export function getAvailableDeclarations() {
  return request({
    url: '/v1/tax-refunds/declarations',
    method: 'get'
  })
}

/**
 * 财务审核相关接口
 */

/**
 * 获取待审核的退税申请列表
 * @param params 查询参数
 */
export function getPendingReviewList(params: any) {
  return request({
    url: '/v1/tax-refunds/review/pending',
    method: 'get',
    params
  })
}

/**
 * 财务初审
 * @param id 申请ID
 * @param data 审核数据
 */
export function financialFirstReview(id: number, data: any) {
  return request({
    url: `/v1/tax-refunds/${id}/first-review`,
    method: 'post',
    data
  })
}

/**
 * 财务复审
 * @param id 申请ID
 * @param data 审核数据
 */
export function financialFinalReview(id: number, data: any) {
  return request({
    url: `/v1/tax-refunds/${id}/final-review`,
    method: 'post',
    data
  })
}

/**
 * 生成退税文件
 * @param id 申请ID
 */
export function generateTaxRefundFile(id: number) {
  return request({
    url: `/v1/tax-refunds/${id}/generate-file`,
    method: 'post'
  })
}

/**
 * 提交发票
 * @param id 申请ID
 * @param data 发票数据
 */
export function submitInvoice(id: number, data: any) {
  return request({
    url: `/v1/tax-refunds/${id}/invoice`,
    method: 'post',
    data
  })
}

/**
 * 退回补充材料
 * @param id 申请ID
 * @param data 退回原因
 */
export function returnForSupplement(id: number, data: any) {
  return request({
    url: `/v1/tax-refunds/${id}/return`,
    method: 'post',
    data
  })
}

/**
 * 获取退税申请统计信息
 */
export function getTaxRefundStatistics() {
  return request({
    url: '/v1/tax-refunds/statistics',
    method: 'get'
  })
}

/**
 * 导出退税申请列表
 * @param params 查询参数
 */
export function exportTaxRefundList(params: any) {
  return request({
    url: '/v1/tax-refunds/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

// 审核退税申请（统一接口，与DeclarationFormController保持一致）
export function auditTaxRefund(id: number, auditData: { result: number; opinion?: string }) {
  return request({
    url: `/v1/tax-refunds/${id}/audit`,
    method: 'post',
    data: auditData
  })
}

/**
 * 获取退税申请附件列表
 * @param id 申请ID
 */
export function getTaxRefundAttachments(id: number) {
  return request({
    url: `/v1/tax-refunds/${id}/attachments`,
    method: 'get'
  })
}

/**
 * 获取退税申请审批历史
 * @param id 申请ID
 */
export function getTaxRefundAuditHistory(id: number) {
  return request({
    url: `/v1/tax-refunds/${id}/audit-history`,
    method: 'get'
  })
}