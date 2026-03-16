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

// 获取申报单列表
export function getDeclarationList(params: DeclarationQueryParams) {
  return request({
    url: '/api/v1/declarations',
    method: 'get',
    params
  })
}

// 获取申报单详情
export function getDeclarationDetail(id: number) {
  return request({
    url: `/api/v1/declarations/${id}`,
    method: 'get'
  })
}

// 新增申报单
export function addDeclaration(data: DeclarationForm) {
  return request({
    url: '/api/v1/declarations',
    method: 'post',
    data
  })
}

// 修改申报单
export function updateDeclaration(id: number, data: DeclarationForm) {
  return request({
    url: `/api/v1/declarations/${id}`,
    method: 'put',
    data
  })
}

// 删除申报单
export function deleteDeclaration(id: number) {
  return request({
    url: `/api/v1/declarations/${id}`,
    method: 'delete'
  })
}

// 提交申报单
export function submitDeclaration(id: number) {
  return request({
    url: `/api/v1/declarations/${id}/submit`,
    method: 'post'
  })
}

// 获取申报单产品列表
export function getDeclarationProducts(formId: number) {
  return request({
    url: `/api/v1/declarations/${formId}/products`,
    method: 'get'
  })
}

// 保存申报单产品
export function saveDeclarationProducts(formId: number, products: DeclarationProduct[]) {
  return request({
    url: `/api/v1/declarations/${formId}/products`,
    method: 'post',
    data: products
  })
}

// 获取申报单箱子列表
export function getDeclarationCartons(formId: number) {
  return request({
    url: `/api/v1/declarations/${formId}/cartons`,
    method: 'get'
  })
}

// 保存申报单箱子
export function saveDeclarationCartons(formId: number, cartons: DeclarationCarton[]) {
  return request({
    url: `/api/v1/declarations/${formId}/cartons`,
    method: 'post',
    data: cartons
  })
}

// 保存箱子产品关联
export function saveCartonProducts(cartonProducts: CartonProduct[]) {
  return request({
    url: '/api/v1/declarations/carton-products',
    method: 'post',
    data: cartonProducts
  })
}

// 获取箱子产品关联
export function getCartonProducts(cartonId: number) {
  return request({
    url: `/api/v1/declarations/carton-products/${cartonId}`,
    method: 'get'
  })
}

// 导出申报单
export function exportDeclaration(id: number) {
  return request({
    url: `/api/v1/declarations/${id}/export`,
    method: 'get',
    responseType: 'blob'
  })
}

// 批量导出申报单
export function batchExportDeclaration(ids: number[]) {
  return request({
    url: '/api/v1/declarations/batch-export',
    method: 'post',
    data: { ids },
    responseType: 'blob'
  })
}