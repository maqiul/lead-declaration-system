import request from '@/utils/request'

// 申报单管理相关API
export interface DeclarationQueryParams {
  pageNum?: number
  pageSize?: number
  formNo?: string
  status?: number
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

// 统计数据类型定义
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

// 获取申报单列表
export function getDeclarationList(params: DeclarationQueryParams) {
  return request({
    url: '/v1/declarations',
    method: 'get',
    params
  })
}

// 获取申报单详情
export function getDeclarationDetail(id: number, status?: number) {
  return request({
    url: `/v1/declarations/${id}`,
    method: 'get',
    params: { status }
  })
}

// 新增申报单
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

// 修改申报单
export function updateDeclaration(id: number, data: DeclarationForm) {
  return request({
    url: `/v1/declarations/${id}`,
    method: 'put',
    data
  })
}

// 删除申报单
export function deleteDeclaration(id: number, status?: number) {
  return request({
    url: `/v1/declarations/${id}`,
    method: 'delete',
    params: { status }
  })
}

// 提交申报单
export function submitDeclaration(id: number) {
  return request({
    url: `/v1/declarations/${id}/submit`,
    method: 'post'
  })
}

// 提交审核（定金/尾款/提货单/财务补充）
export function submitForAudit(id: number, type: 'deposit' | 'balance' | 'pickup' | 'financeUpload') {
  return request({
    url: `/v1/declarations/${id}/submit-audit`,
    method: 'post',
    params: { auditType: type }
  })
}

// 审核申报单
export function auditDeclaration(id: number, result: number, remark?: string, taskKey?: string) {
  return request({
    url: `/v1/declarations/${id}/audit`,
    method: 'post',
    data: { result, remark, taskKey }
  })
}

// 获取申报单的当前活跃Flowable任务列表
export function getActiveTasks(id: number) {
  return request({
    url: `/v1/declarations/${id}/tasks`,
    method: 'get'
  })
}

// 批量获取申报单的活跃任务（列表页用）
export function getBatchActiveTasks(ids: string) {
  return request({
    url: '/v1/declarations/batch-tasks',
    method: 'get',
    params: { ids }
  })
}

// 保存水单信息
export function saveRemittance(id: number, data: any) {
  return request({
    url: `/v1/declarations/${id}/remittance`,
    method: 'post',
    data
  })
}

// 获取水单信息
export function getRemittance(remittanceId: number) {
  return request({
    url: `/v1/declarations/remittance/${remittanceId}`,
    method: 'get'
  })
}

// 更新水单信息
export function updateRemittance(remittanceId: number, data: any) {
  return request({
    url: `/v1/declarations/remittance/${remittanceId}`,
    method: 'put',
    data
  })
}

// 删除水单
export function deleteRemittance(remittanceId: number) {
  return request({
    url: `/v1/declarations/remittance/${remittanceId}`,
    method: 'delete'
  })
}

// 获取申报单产品列表
export function getDeclarationProducts(formId: number) {
  return request({
    url: `/v1/declarations/${formId}/products`,
    method: 'get'
  })
}

// 保存申报单产品
export function saveDeclarationProducts(formId: number, products: DeclarationProduct[]) {
  return request({
    url: `/v1/declarations/${formId}/products`,
    method: 'post',
    data: products
  })
}

// 获取申报单箱子列表
export function getDeclarationCartons(formId: number) {
  return request({
    url: `/v1/declarations/${formId}/cartons`,
    method: 'get'
  })
}

// 保存申报单箱子
export function saveDeclarationCartons(formId: number, cartons: DeclarationCarton[]) {
  return request({
    url: `/v1/declarations/${formId}/cartons`,
    method: 'post',
    data: cartons
  })
}

// 保存箱子产品关联
export function saveCartonProducts(cartonProducts: CartonProduct[]) {
  return request({
    url: '/v1/declarations/carton-products',
    method: 'post',
    data: cartonProducts
  })
}

// 获取箱子产品关联
export function getCartonProducts(cartonId: number) {
  return request({
    url: `/v1/declarations/carton-products/${cartonId}`,
    method: 'get'
  })
}

// 导出申报单
export function exportDeclaration(id: number) {
  return request({
    url: `/v1/declarations/${id}/export`,
    method: 'get',
    responseType: 'blob'
  })
}

// 导出开票明细生成单
export function exportFinanceCalculation(id: number) {
  return request({
    url: `/v1/financial-supplements/form/${id}/export-finance-calculation`,
    method: 'get'
  })
}

// 批量导出申报单
export function batchExportDeclaration(ids: number[]) {
  return request({
    url: '/v1/declarations/batch-export',
    method: 'post',
    data: { ids },
    responseType: 'blob'
  })
}

// 获取提货单附件列表
export function getPickupAttachments(id: number) {
  return request({
    url: `/v1/declarations/${id}/attachments/pickup`,
    method: 'get'
  })
}

// 保存提货单附件
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

// 分页获取财务补充列表
export function getFinancialSupplementList(params: FinancialSupplementQueryParams) {
  return request({
    url: '/v1/financial-supplements',
    method: 'get',
    params
  })
}

// 根据申报单ID获取财务补充记录
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

// 获取申报单统计数据
export function getDeclarationStatistics() {
  return request({
    url: '/v1/declarations/statistics',
    method: 'get'
  })
}

// 替换申报单附件
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

// 获取申报单附件列表
export function getDeclarationAttachments(formId: number) {
  return request({
    url: `/v1/declarations/${formId}/attachments`,
    method: 'get'
  })
}

// 重新生成单据
export function regenerateDocuments(id: number) {
  return request({
    url: `/v1/declarations/${id}/regenerate-documents`,
    method: 'post'
  })
}

// 重新生成全套单据
export function regenerateAllDocuments(id: number) {
  return request({
    url: `/v1/declarations/${id}/regenerate-all-documents`,
    method: 'post'
  })
}

// 重新生成水单报告
export function regenerateRemittanceReport(id: number, type: number) {
  return request({
    url: `/v1/declarations/${id}/regenerate-remittance-report`,
    method: 'post',
    params: { type }
  })
}

// ========== 提货单相关 API ==========
// 提交提货单
export function saveDeliveryOrder(formId: number, data: any) {
  return request({
    url: `/v1/declarations/${formId}/delivery-order`,
    method: 'post',
    data
  })
}

// 获取提货单列表
export function getDeliveryOrders(formId: number) {
  return request({
    url: `/v1/declarations/${formId}/delivery-orders`,
    method: 'get'
  })
}

// 更新提货单
export function updateDeliveryOrder(id: number, data: any) {
  return request({
    url: `/v1/declarations/delivery-order/${id}`,
    method: 'put',
    data
  })
}

// 删除提货单
export function deleteDeliveryOrder(id: number) {
  return request({
    url: `/v1/declarations/delivery-order/${id}`,
    method: 'delete'
  })
}

// === 财务人员相关接口 ===

// 获取开票明细计算详情
export function getCalculationDetail(formId: number) {
  return request({
    url: `/v1/financial-supplements/form/${formId}/calculation-detail`,
    method: 'get'
  })
}


// 审核水单
export function auditRemittance(id: number, data: { approved: boolean; remark: string }) {
  return request({
    url: `/v1/declarations/remittance/${id}/audit`,
    method: 'post',
    data
  })
}

// 审核提货单
export function auditDeliveryOrder(id: number, data: { approved: boolean; remark: string }) {
  return request({
    url: `/v1/declarations/delivery-order/${id}/audit`,
    method: 'post',
    data
  })
}

// 获取启用的银行账户列表（用于外汇银行选择）
export function getEnabledBankAccounts(currency?: string) {
  return request({
    url: '/v1/bank-accounts/enabled',
    method: 'get',
    params: currency ? { currency } : undefined
  })
}

// 获取申报单的水单列表
export function getRemittanceListByFormId(formId: number) {
  return request({
    url: `/v1/declarations/${formId}/remittances`,
    method: 'get'
  })
}