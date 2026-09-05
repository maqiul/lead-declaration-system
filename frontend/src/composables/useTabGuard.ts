/**
 * 标签页桥接（页面 → 标签栏）
 *
 * 1. 关闭守卫：页面（目前用于新建申报）注册守卫后，关闭对应标签前会先询问“是否保存为草稿”，
 *    避免误关标签导致已填写内容丢失。守卫随组件卸载自动注销。
 * 2. 展示元信息：表单页可回写自己所属标签的申报类型，用于在标签栏区分梓熠/理德与集洛。
 */

import type { DeclarationTabType } from '@/utils/tabKey'

export interface TabGuard {
  /** 所属标签键，由 utils/tabKey#getTabKey 计算 */
  tabKey: string
  /** 注册时本实例自己的完整地址：同标签键可能缓存多个实例，靠它挑出该置顶到哪个页面 */
  fullPath: string
  /** 是否存在未保存内容 */
  isDirty: () => boolean
  /** 保存草稿，返回 true 表示保存成功、可以继续关闭 */
  save: () => Promise<boolean>
}

/** 每条注册带序号，便于实例自身重注册时只顶掉自己那一份 */
interface GuardEntry {
  id: number
  guard: TabGuard
}

const guards = new Map<string, GuardEntry[]>()
let guardSeq = 0

/**
 * 注册守卫（按实例登记，实例各自一份）
 * 不能按标签键覆盖式注册：keep-alive 下同键可能挂着多个缓存实例（重复点“新建申报”、
 * /form 与 /form-v2 共用新建槽位），后装载的空白实例一旦顶掉已填内容的实例，
 * 关闭标签就再也判不到未保存，表现为“不跳转也不弹窗”
 */
export function registerTabGuard(guard: TabGuard): () => void {
  const id = ++guardSeq
  const list = guards.get(guard.tabKey) || []
  list.push({ id, guard })
  guards.set(guard.tabKey, list)
  return () => {
    const current = guards.get(guard.tabKey)
    if (!current) return
    const rest = current.filter(entry => entry.id !== id)
    if (rest.length) {
      guards.set(guard.tabKey, rest)
    } else {
      guards.delete(guard.tabKey)
    }
  }
}

/** 丢弃某标签键下的守卫（标签关闭、缓存实例作废时调用） */
export function dropTabGuards(tabKey: string): void {
  guards.delete(tabKey)
}

/**
 * 取出某标签键下全部存活实例的守卫（按注册先后排序，越靠后越新）
 * 关闭判定取“任一实例有未保存内容即算未保存”：宁可多问一次，也不能漏报丢内容
 */
export function findTabGuards(tabKey: string): TabGuard[] {
  return (guards.get(tabKey) || []).map(entry => entry.guard)
}

/**
 * 按完整地址兜底认领守卫
 * 标签栏的 tab.key 可能是历史快照里按旧算法算的，与本页注册的键对不上；
 * 地址相同就一定是同一个页面，不能因为键不匹配就漏掉未保存提示
 */
export function findTabGuardByPath(fullPath: string): TabGuard | undefined {
  for (const list of guards.values()) {
    for (let i = list.length - 1; i >= 0; i--) {
      if (list[i].guard.fullPath === fullPath) return list[i].guard
    }
  }
  return undefined
}

// ========== 标签展示元信息 ==========

/** 页面可回写给所属标签的展示元信息：申报类型徽标、发票号 */
export interface TabMeta {
  bizType?: DeclarationTabType
  invoiceNo?: string
}

type MetaSetter = (tabKey: string, meta: TabMeta) => void

let metaSetter: MetaSetter | null = null

/** 由 layout 注册标签元信息写入器（返回注销函数） */
export function registerTabMetaSetter(fn: MetaSetter): () => void {
  metaSetter = fn
  return () => {
    if (metaSetter === fn) metaSetter = null
  }
}

/** 页面回写所属标签的展示元信息（标签不存在时静默忽略） */
export function setTabMeta(tabKey: string, meta: TabMeta): void {
  metaSetter?.(tabKey, meta)
}
