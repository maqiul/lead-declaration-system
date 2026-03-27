import request from '@/utils/request'

export interface CityInfo {
  id?: number
  cityName?: string
  cityEnglishName?: string
  provinceName?: string
  provinceEnglishName?: string
  countryName?: string
  countryEnglishName?: string
  cityCode?: string
  sort?: number
  status?: number
  remarks?: string
}

export interface CityQueryParams {
  country?: string
  province?: string
}

/**
 * 获取城市信息列表
 */
export const getCities = (params?: CityQueryParams) => {
  return request({
    url: '/v1/cities',
    method: 'get',
    params
  })
}

/**
 * 获取城市信息列表
 */
export const getCitiesByCountry = (countryName?: string) => {
  return request({
    url: `/v1/cities/${countryName}/cities`,
    method: 'get',
  })
}

/**
 * 根据ID获取城市信息
 */
export const getCityById = (id: number) => {
  return request({
    url: `/v1/cities/${id}`,
    method: 'get'
  })
}

/**
 * 获取城市信息列表（分页）
 */
export const getCityList = (params?: any) => {
  return request({
    url: '/v1/cities',
    method: 'get',
    params
  })
}

/**
 * 搜索城市
 */
export const searchCities = (keyword: string) => {
  return request({
    url: `/v1/cities/search`,
    method: 'get',
    params: { keyword }
  })
}

/**
 * 新增城市信息
 */
export const addCity = (data: CityInfo) => {
  return request({
    url: '/v1/cities',
    method: 'post',
    data
  })
}

/**
 * 修改城市信息
 */
export const updateCity = (id: number, data: CityInfo) => {
  return request({
    url: `/v1/cities/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除城市信息
 */
export const deleteCity = (id: number) => {
  return request({
    url: `/v1/cities/${id}`,
    method: 'delete'
  })
}

/**
 * 更新城市状态
 */
export const updateCityStatus = (id: number, status: number) => {
  return request({
    url: `/v1/cities/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 获取城市省份列表
 */
export const getCityProvinces = () => {
  return request({
    url: '/v1/cities/provinces',
    method: 'get'
  })
}