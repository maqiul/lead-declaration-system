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
  destinationRegion?: string
  currency: string
  totalQuantity: number
  totalGrossWeight: number
  totalNetWeight: number
  totalVolume: number
  totalAmount: number
  status: number
  createTime?: string
  updateTime?: string
}

export interface DeclarationProduct {
  id?: number
  formId?: number
  productName: string
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

// 提交审核（定金/尾款/提货单）
export function submitForAudit(id: number, type: 'deposit' | 'balance' | 'pickup') {
  return request({
    url: `/v1/declarations/${id}/submit-audit`,
    method: 'post',
    params: { auditType: type }
  })
}

// 审核申报单
export function auditDeclaration(id: number, result: number, remark?: string) {
  return request({
    url: `/v1/declarations/${id}/audit`,
    method: 'post',
    data: { result, remark }
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
<<<<<<< HEAD
}

// 重新生成单据
export function regenerateDocuments(id: number) {
  return request({
    url: `/v1/declarations/${id}/regenerate-documents`,
    method: 'post'
  })
=======
>>>>>>> 974d00a7096735aae9219cfa167a551b72278b5f
}