import request from '@/utils/request'

/**
 * 获取计量单位列表
 */
export function getMeasurementUnits() {
  return request({
    url: '/system/measurement-units/list',
    method: 'get'
  })
}

/**
 * 获取启用的计量单位
 */
export function getActiveMeasurementUnits() {
  return request({
    url: '/system/measurement-units/active',
    method: 'get'
  })
}

/**
 * 根据单位代码获取单位信息
 * @param unitCode 单位代码
 */
export function getMeasurementUnitByCode(unitCode: string) {
  return request({
    url: `/system/measurement-units/code/${unitCode}`,
    method: 'get'
  })
}