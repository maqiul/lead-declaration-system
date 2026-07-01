import request from '@/utils/request'

// 主体配置相关API

/**
 * 主体配置接口定义
 */
export interface EntityConfig {
  id?: number
  entityName: string          // 公司英文名
  entityAddress: string       // 英文地址
  entityNameCn: string        // 公司中文名
  entityAddressCn: string     // 中文地址
  taxId: string               // 纳税人识别号
  phone: string               // 电话
  bankAccount: string          // 开户银行
  invoiceTemplate: string     // 发票模板文件名
  packingListTemplate: string // 装箱单模板文件名
  fullDocumentsTemplate: string // 海关附件模板文件名
  pickupListTemplate: string  // 提货单模板文件名
  remittanceTemplate: string  // 水单模板文件名
  isDefault: number           // 是否默认主体 0/1
  status: number              // 状态 0-禁用 1-启用
  sort: number                // 排序
  createTime?: string
  updateTime?: string
}

/**
 * 分页查询主体配置
 */
export function getEntityConfigList(params: any) {
  return request({
    url: '/v1/entity-configs',
    method: 'get',
    params
  })
}

/**
 * 获取启用的主体列表（下拉用）
 */
export function getEnabledEntityConfigs() {
  return request({
    url: '/v1/entity-configs/enabled',
    method: 'get'
  })
}

/**
 * 获取默认主体
 */
export function getDefaultEntityConfig() {
  return request({
    url: '/v1/entity-configs/default',
    method: 'get'
  })
}

/**
 * 获取主体详情
 */
export function getEntityConfigDetail(id: number) {
  return request({
    url: `/v1/entity-configs/${id}`,
    method: 'get'
  })
}

/**
 * 新增主体配置
 */
export function addEntityConfig(data: EntityConfig) {
  return request({
    url: '/v1/entity-configs',
    method: 'post',
    data
  })
}

/**
 * 修改主体配置
 */
export function updateEntityConfig(id: number, data: EntityConfig) {
  return request({
    url: `/v1/entity-configs/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除主体配置
 */
export function deleteEntityConfig(id: number) {
  return request({
    url: `/v1/entity-configs/${id}`,
    method: 'delete'
  })
}

/**
 * 切换主体状态
 */
export function toggleEntityConfigStatus(id: number, status: number) {
  return request({
    url: `/v1/entity-configs/${id}/toggle-status`,
    method: 'post',
    params: { status }
  })
}

/**
 * 设为默认主体
 */
export function setDefaultEntityConfig(id: number) {
  return request({
    url: `/v1/entity-configs/${id}/set-default`,
    method: 'post'
  })
}
