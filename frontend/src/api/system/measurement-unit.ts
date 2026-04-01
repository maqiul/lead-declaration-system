import request from '@/utils/request'

export interface MeasurementUnit {
  id?: number
  unitCode: string
  unitName: string
  unitNameEn: string
  unitNameEnSingular: string
  unitType: string
  description?: string
  status: number
  sort: number
}

export interface MeasurementUnitQueryParams {
  pageNum?: number
  pageSize?: number
  keyword?: string
  unitType?: string
  status?: number
}

// 获取计量单位列表
export function getMeasurementUnitList(params: MeasurementUnitQueryParams) {
  return request({
    url: '/v1/system/measurement-units',
    method: 'get',
    params
  })
}

// 获取启用的计量单位列表
export function getActiveMeasurementUnits() {
  return request({
    url: '/v1/system/measurement-units/active',
    method: 'get'
  })
}

// 根据代码获取计量单位
export function getMeasurementUnitByCode(unitCode: string) {
  return request({
    url: `/v1/system/measurement-units/code/${unitCode}`,
    method: 'get'
  })
}

// 新增计量单位
export function addMeasurementUnit(data: MeasurementUnit) {
  return request({
    url: '/v1/system/measurement-units',
    method: 'post',
    data
  })
}

// 更新计量单位
export function updateMeasurementUnit(data: MeasurementUnit) {
  return request({
    url: '/v1/system/measurement-units',
    method: 'put',
    data
  })
}

// 删除计量单位
export function deleteMeasurementUnit(id: number) {
  return request({
    url: `/v1/system/measurement-units/${id}`,
    method: 'delete'
  })
}

// 切换计量单位状态
export function toggleMeasurementUnitStatus(id: number, status: number) {
  return request({
    url: '/v1/system/measurement-units',
    method: 'put',
    data: { id, status }
  })
}
