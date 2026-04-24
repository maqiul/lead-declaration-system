import request from '@/utils/request'

// 申报坕管睆相关API
export interface DeclarationQueryParams {
  pageNum?: number
  pageSize?: number
  current?: number
  size?: number
  formNo?: string
  status?: number
  excludeStatus?: number
  startTime?: string
  endTime?: string
}

export interface DeclarationForm {
  id?: number
  formNo: string
  shipperCompany: string
  shipperAddress: string
  consigneeCompany: string
  consigneeAddress: string
  departureCity?: string
  departureCityChinese?: string
  departureCityEnglish?: string
  destinationRegion?: string
  tradeCountry?: string
  currency: string
  totalQuantity: number
  totalGrossWeight: number
  totalNetWeight: number
  totalVolume: number
  totalAmount: number
  status: number
  createTime?: string
  updateTime?: string
  orgId?: number
}

export interface DeclarationProduct {
  id?: number
  formId?: number
  productName: string
  productChineseName?: string
  productEnglishName?: string
  hsCode: string
  quantity: number
  unit: string
  unitPrice: number
  amount: number
  grossWeight: number
  netWeight: number
  cartons?: number
  volume?: number
  sortOrder?: number
}

export interface DeclarationCarton {
  id?: number
  formId?: number
  cartonNo: string
  quantity: number
  volume: number
  typeChinese?: string
  typeEnglish?: string
  sortOrder?: number
}

export interface CartonProduct {
  id?: number
  cartonId: number
  productId: number
  quantity: number
}

// 统计数杮类型定义
export interface StatisticsData {
  totalForms: number
  monthForms: number
  totalAmount: number
  avgAmount: number
  statusStats: Array<{
    status: string
    count: number
    amount: number
  }>
  productStats: Array<{
    productName: string
    hsCode: string
    count: number
    totalAmount: number
  }>
  destinationStats: Array<{
    destination: string
    count: number
    totalAmount: number
  }>
}

// 获坖申报坕列表
export function getDeclarationList(params: DeclarationQueryParams) {
  return request({
    url: '/v1/declarations',
    method: 'get',
    params
  })
}

// 获坖申报坕详情
export function getDeclarationDetail(id: number, status?: number) {
  return request({
    url: `/v1/declarations/${id}`,
    method: 'get',
    params: { status }
  })
}

// 新增申报坕
export function addDeclaration(data: DeclarationForm) {
  return request({
    url: '/v1/declarations',
    method: 'post',
    data
  })
}

// 保存草稿
export function saveDraft(data: DeclarationForm) {
  return request({
    url: '/v1/declarations/draft',
    method: 'post',
    data
  })
}

// 修改申报
export function updateDeclaration(id: number, data: DeclarationForm) {
  return request({
    url: `/v1/declarations/${id}`,
    method: 'put',
    data
  })
}

// 删除申报
export function deleteDeclaration(id: number, status?: number) {
  return request({
    url: `/v1/declarations/${id}`,
    method: 'delete',
    params: { status }
  })
}

// 提交申报
export function submitDeclaration(id: number) {
  return request({
    url: `/v1/declarations/${id}/submit`,
    method: 'post'
  })
}

// 杝交审核（定金/尾款/杝货坕/财务补充）
export function submitForAudit(id: number, type: 'deposit' | 'balance' | 'pickup' | 'financeUpload') {
  return request({
    url: `/v1/declarations/${id}/submit-audit`,
    method: 'post',
    params: { auditType: type }
  })
}

// ========================================
// 业务发票接口
// ========================================

// 获取业务发票列表
export function getBusinessInvoices(formId: number) {
  return request({
    url: `/v1/declarations/${formId}/business-invoices`,
    method: 'get'
  })
}

// 上传业务发票
export function uploadBusinessInvoice(formId: number, data: FormData) {
  return request({
    url: `/v1/declarations/${formId}/business-invoices`,
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 删除业务发票
export function deleteBusinessInvoice(invoiceId: number) {
  return request({
    url: `/v1/declarations/business-invoices/${invoiceId}`,
    method: 'delete'
  })
}

// 审核申报坕
export function auditDeclaration(id: number, result: number, remark?: string, taskKey?: string) {
  return request({
    url: `/v1/declarations/${id}/audit`,
    method: 'post',
    data: { result, remark, taskKey }
  })
}

// 获坖申报坕的当剝活跃Flowable任务列表
export function getActiveTasks(id: number) {
  return request({
    url: `/v1/declarations/${id}/tasks`,
    method: 'get'
  })
}

// 批針获坖申报坕的活跃任务（列表页用）
export function getBatchActiveTasks(ids: string) {
  return request({
    url: '/v1/declarations/batch-tasks',
    method: 'get',
    params: { ids }
  })
}

// 保存水坕信杯
export function saveRemittance(id: number, data: any) {
  return request({
    url: `/v1/declarations/${id}/remittance`,
    method: 'post',
    data
  })
}

// 获坖水坕信杯
export function getRemittance(remittanceId: number) {
  return request({
    url: `/v1/declarations/remittance/${remittanceId}`,
    method: 'get'
  })
}

// 更新水坕信杯
export function updateRemittance(remittanceId: number, data: any) {
  return request({
    url: `/v1/declarations/remittance/${remittanceId}`,
    method: 'put',
    data
  })
}

// 删除水坕
export function deleteRemittance(remittanceId: number) {
  return request({
    url: `/v1/declarations/remittance/${remittanceId}`,
    method: 'delete'
  })
}

// 获坖申报坕产哝列表
export function getDeclarationProducts(formId: number) {
  return request({
    url: `/v1/declarations/${formId}/products`,
    method: 'get'
  })
}

// 保存申报坕产哝
export function saveDeclarationProducts(formId: number, products: DeclarationProduct[]) {
  return request({
    url: `/v1/declarations/${formId}/products`,
    method: 'post',
    data: products
  })
}

// 获坖申报坕箱孝列表
export function getDeclarationCartons(formId: number) {
  return request({
    url: `/v1/declarations/${formId}/cartons`,
    method: 'get'
  })
}

// 保存申报坕箱孝
export function saveDeclarationCartons(formId: number, cartons: DeclarationCarton[]) {
  return request({
    url: `/v1/declarations/${formId}/cartons`,
    method: 'post',
    data: cartons
  })
}

// 保存箱孝产哝关蝔
export function saveCartonProducts(cartonProducts: CartonProduct[]) {
  return request({
    url: '/v1/declarations/carton-products',
    method: 'post',
    data: cartonProducts
  })
}

// 获坖箱孝产哝关蝔
export function getCartonProducts(cartonId: number) {
  return request({
    url: `/v1/declarations/carton-products/${cartonId}`,
    method: 'get'
  })
}

// 导出申报坕
export function exportDeclaration(id: number) {
  return request({
    url: `/v1/declarations/${id}/export`,
    method: 'get',
    responseType: 'blob'
  })
}

// 导出开票明细生戝坕
export function exportFinanceCalculation(id: number) {
  return request({
    url: `/v1/financial-supplements/form/${id}/export-finance-calculation`,
    method: 'get'
  })
}

// 批針导出申报坕
export function batchExportDeclaration(ids: number[]) {
  return request({
    url: '/v1/declarations/batch-export',
    method: 'post',
    data: { ids },
    responseType: 'blob'
  })
}

// 获坖杝货坕附件列表
export function getPickupAttachments(id: number) {
  return request({
    url: `/v1/declarations/${id}/attachments/pickup`,
    method: 'get'
  })
}

// 保存杝货坕附件
export function savePickupAttachment(id: number, data: any) {
  return request({
    url: `/v1/declarations/${id}/attachments/pickup`,
    method: 'post',
    data
  })
}

// === 财务开票补充 ===
export interface FinancialSupplementQueryParams {
  pageNum?: number
  pageSize?: number
  formNo?: string
  status?: number
}

// 分页获坖财务补充列表
export function getFinancialSupplementList(params: FinancialSupplementQueryParams) {
  return request({
    url: '/v1/financial-supplements',
    method: 'get',
    params
  })
}

// 根杮申报坕ID获坖财务补充记录
export function getFinancialSupplement(formId: number) {
  return request({
    url: `/v1/financial-supplements/form/${formId}`,
    method: 'get'
  })
}

// 创建财务补充记录
export function createFinancialSupplement(data: any) {
  return request({
    url: '/v1/financial-supplements',
    method: 'post',
    data
  })
}

// 更新财务补充记录
export function updateFinancialSupplement(id: number, data: any) {
  return request({
    url: `/v1/financial-supplements/${id}`,
    method: 'put',
    data
  })
}

// 删除附件
export function deleteAttachment(attachmentId: number) {
  return request({
    url: `/v1/declarations/attachments/${attachmentId}`,
    method: 'delete'
  })
}

// 上传文件
export function uploadFile(file: File, type?: string) {
  const formData = new FormData()
  formData.append('file', file)
  if (type) {
    formData.append('type', type)
  }
  return request({
    url: '/v1/files/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获坖申报坕统计数杮
export function getDeclarationStatistics() {
  return request({
    url: '/v1/declarations/statistics',
    method: 'get'
  })
}

// 替杢申报坕附件
export function replaceDeclarationAttachment(formId: number, attachmentId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  
  return request({
    url: `/v1/declarations/${formId}/attachments/${attachmentId}/replace`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获坖申报坕附件列表
export function getDeclarationAttachments(formId: number) {
  return request({
    url: `/v1/declarations/${formId}/attachments`,
    method: 'get'
  })
}

// 針新生戝坕杮
export function regenerateDocuments(id: number) {
  return request({
    url: `/v1/declarations/${id}/regenerate-documents`,
    method: 'post'
  })
}

// 針新生戝全套坕杮
export function regenerateAllDocuments(id: number) {
  return request({
    url: `/v1/declarations/${id}/regenerate-all-documents`,
    method: 'post'
  })
}

// 針新生戝水坕报告
export function regenerateRemittanceReport(id: number, type: number) {
  return request({
    url: `/v1/declarations/${id}/regenerate-remittance-report`,
    method: 'post',
    params: { type }
  })
}

// ========== 杝货坕相关 API ==========
// 杝交杝货坕
export function saveDeliveryOrder(formId: number, data: any) {
  return request({
    url: `/v1/declarations/${formId}/delivery-order`,
    method: 'post',
    data
  })
}

// 获坖杝货坕列表
export function getDeliveryOrders(formId: number) {
  return request({
    url: `/v1/declarations/${formId}/delivery-orders`,
    method: 'get'
  })
}

// 更新杝货坕
export function updateDeliveryOrder(id: number, data: any) {
  return request({
    url: `/v1/declarations/delivery-order/${id}`,
    method: 'put',
    data
  })
}

// 删除杝货坕
export function deleteDeliveryOrder(id: number) {
  return request({
    url: `/v1/declarations/delivery-order/${id}`,
    method: 'delete'
  })
}

// === 财务人员相关接坣 ===

// 获坖开票明细计算详情
export function getCalculationDetail(formId: number) {
  return request({
    url: `/v1/financial-supplements/form/${formId}/calculation-detail`,
    method: 'get'
  })
}


// 审核水坕
export function auditRemittance(id: number, data: { approved: boolean; remark: string }) {
  return request({
    url: `/v1/declarations/remittance/${id}/audit`,
    method: 'post',
    data
  })
}

// 审核杝货坕
export function auditDeliveryOrder(id: number, data: { approved: boolean; remark: string }) {
  return request({
    url: `/v1/declarations/delivery-order/${id}/audit`,
    method: 'post',
    data
  })
}

// 获坖坯用的银行账户列表（用于外汇银行选择）
export function getEnabledBankAccounts(currency?: string) {
  return request({
    url: '/v1/bank-accounts/enabled',
    method: 'get',
    params: currency ? { currency } : undefined
  })
}

// 获坖申报坕的水坕列表
export function getRemittanceListByFormId(formId: number) {
  return request({
    url: `/v1/declarations/${formId}/remittances`,
    method: 'get'
  })
}
// === �˻زݸ���� API ===

/**
 * �����˻زݸ�
 */
export function applyReturnToDraft(id: number, reason: string) {
  return request({
    url: `/v1/declarations/${id}/apply-return`,
    method: 'post',
    data: { reason }
  })
}

/**
 * ����˻زݸ�����
 */
export function auditReturnToDraft(id: number, data: { approved: boolean; remark: string }) {
  return request({
    url: `/v1/declarations/${id}/audit-return`,
    method: 'post',
    data
  })
}

/**
 * ??????????
 */
export function getReturnAuditHistory(id: number) {
  return request({
    url: `/v1/declarations/${id}/return-history`,
    method: 'get'
  })
}

/**
 * ????????????(??? - ?remittance??)
 */
export function getRemittancesByFormId(formId: number) {
  return request({
    url: `/v1/remittances/form/${formId}`,
    method: 'get'
  })
}

