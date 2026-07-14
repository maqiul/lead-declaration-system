import request from '@/utils/request'

// ============================================================
// 字典管理 API
// ============================================================

/** 字典类型接口定义 */
export interface SysDict {
  id?: number
  dictCode: string
  dictName: string
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 字典项接口定义 */
export interface SysDictItem {
  id?: number
  dictCode: string
  itemValue: string
  itemLabel: string
  itemColor?: string
  sortOrder: number
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 字典选项（前端统一格式） */
export interface DictOption {
  value: string
  label: string
  color?: string
}

// ============================================================
// 字典类型 CRUD
// ============================================================

/** 获取字典类型列表 */
export function getDictList() {
  return request({ url: '/v1/sys-dicts', method: 'get' })
}

/** 创建字典类型 */
export function createDict(data: SysDict) {
  return request({ url: '/v1/sys-dicts', method: 'post', data })
}

/** 更新字典类型 */
export function updateDict(id: number, data: Partial<SysDict>) {
  return request({ url: `/v1/sys-dicts/${id}`, method: 'put', data })
}

/** 删除字典类型 */
export function deleteDict(id: number) {
  return request({ url: `/v1/sys-dicts/${id}`, method: 'delete' })
}

// ============================================================
// 字典项 CRUD
// ============================================================

/** 获取字典项列表 */
export function getDictItems(dictCode: string) {
  return request({ url: `/v1/sys-dicts/${dictCode}/items`, method: 'get' })
}

/** 创建字典项 */
export function createDictItem(dictCode: string, data: SysDictItem) {
  return request({ url: `/v1/sys-dicts/${dictCode}/items`, method: 'post', data })
}

/** 更新字典项 */
export function updateDictItem(id: number, data: Partial<SysDictItem>) {
  return request({ url: `/v1/sys-dicts/items/${id}`, method: 'put', data })
}

/** 删除字典项 */
export function deleteDictItem(id: number) {
  return request({ url: `/v1/sys-dicts/items/${id}`, method: 'delete' })
}

/** 公开接口：获取启用状态的字典项 */
export function getEnabledDictItems(code: string) {
  return request({ url: '/v1/sys-dicts/items', method: 'get', params: { code } })
}
