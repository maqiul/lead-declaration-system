import request from '@/utils/request'

// HS商品类型配置API

/**
 * 获取商品类型列表
 * @param params 查询参数
 */
export function getProducts(params: any) {
  return request({
    url: '/system/product/list',
    method: 'get',
    params
  })
}

/**
 * 获取所有启用的商品类型
 */
export function getEnabledProducts() {
  return request({
    url: '/system/product/enabled',
    method: 'get'
  })
}

/**
 * 获取商品类型列表（用于下拉选择）
 */
export function getProductTypes() {
  return request({
    url: '/system/product/types',
    method: 'get'
  })
}

/**
 * 获取单个商品类型详情
 * @param id 商品ID
 */
export function getProductDetail(id: number | string) {
  return request({
    url: `/system/product/${id}`,
    method: 'get'
  })
}

/**
 * 新增商品类型
 * @param data 商品数据
 */
export function addProduct(data: any) {
  return request({
    url: '/system/product',
    method: 'post',
    data
  })
}

/**
 * 更新商品类型
 * @param id 商品ID
 * @param data 更新数据
 */
export function updateProduct(id: number | string, data: any) {
  return request({
    url: `/system/product/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除商品类型
 * @param id 商品ID
 */
export function deleteProduct(id: number | string) {
  return request({
    url: `/system/product/${id}`,
    method: 'delete'
  })
}
