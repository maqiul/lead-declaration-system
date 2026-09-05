/**
 * 多标签页标识工具
 *
 * 由 layout 与业务页面共用：标签归并键、标题、申报类型、缓存键基座都必须集中在
 * 这里计算，否则两处口径会不一致（页面注册关闭守卫时要算出自己归属的标签键）。
 */

/** 申报表单类页面：路径以 /form 或 /form-v2 结尾 */
const FORM_PATH_RE = /\/form(-v2)?$/

export const isFormTabPath = (path: string): boolean => FORM_PATH_RE.test(path)

/** 表单页业务标识：优先单据 id，其次豁免/补交入口 id，均无则视为新建 */
const getFormBizId = (query: Record<string, any>): string => {
  const raw = query?.id || query?.exemptionId || query?.supplementId
  return raw ? String(raw) : ''
}

/** 新建单标签键的后缀 */
const NEW_FORM_TAB_SUFFIX = '#new'

/**
 * 新建单标签槽位路径
 * 同一套申报的 /form 与 /form-v2 归入同一个槽位，否则同一套申报会挂出两个「新建申报」
 */
const getNewFormScopePath = (path: string): string => path.replace(/-v2$/, '')

/**
 * 新建单标签键（按申报类型各自独立）
 * 梓熠/理德与集洛各保留一个新建标签、互不顶替；同一套内重复点新建才复用那一个
 */
export const getNewFormTabKey = (path: string): string => `${getNewFormScopePath(path)}${NEW_FORM_TAB_SUFFIX}`

/** 标签键是否属于新建单（供旧快照按当前口径重建标签键） */
export const isNewFormTabKey = (key: string): boolean => key.endsWith(NEW_FORM_TAB_SUFFIX)

/**
 * 标签唯一键
 * - 表单页（已有单据）：路径 + 业务标识，同一张单的只读/编辑/资料提交等入口共用一个标签
 * - 表单页（新建）：按申报类型独立的新建槽位，每套申报只保留一个「新建申报」
 * - 其余页面：仅路径，避免搜索条件等 query 变化产生重复标签
 */
export const getTabKey = (route: { path: string; query?: Record<string, any> }): string => {
  if (!isFormTabPath(route.path)) return route.path
  const bizId = getFormBizId(route.query || {})
  return bizId ? `${route.path}#${bizId}` : getNewFormTabKey(route.path)
}

/**
 * 表单页标签标题：直接区分“新建申报 / 查看申报”，其余页面返回 null 由调用方走通用标题
 * 数据库 id 对用户没有辨识度，页面回写发票号后一律以发票号替代标签上的编号
 */
export const getFormTabTitle = (route: { path: string; query?: Record<string, any> }, invoiceNo?: string): string | null => {
  if (!isFormTabPath(route.path)) return null
  const bizId = getFormBizId(route.query || {})
  const no = String(invoiceNo || '').trim()
  return bizId ? `查看申报 ${no || `#${bizId}`}` : (no ? `新建申报 ${no}` : '新建申报')
}

/**
 * 由标签记录的完整地址重算表单标题
 * 发票号不在 URL 里（页面内存状态），layout 拿到回写后需据此就地换文案；非表单页返回 null
 */
export const getFormTabTitleByFullPath = (fullPath: string, invoiceNo?: string): string | null => {
  const [path, search = ''] = fullPath.split('?')
  if (!isFormTabPath(path)) return null
  const query: Record<string, string> = {}
  new URLSearchParams(search).forEach((value, key) => { query[key] = value })
  return getFormTabTitle({ path, query }, invoiceNo)
}

/** 申报类型：SELF=梓熠、理德申报，EXTERNAL=集洛申报 */
export type DeclarationTabType = 'SELF' | 'EXTERNAL'

/** 标签上的申报类型短文案（与表单内“申报类型”标签同源，缩写便于在标签栏扫读） */
export const getDeclarationTypeLabel = (type: DeclarationTabType): string =>
  type === 'SELF' ? '梓熠/理德' : '集洛'

/**
 * 从路由推导申报类型
 * 两套菜单物理隔离，路径前缀即权威口径；老链接（/declaration/*）回退到 query，均无则返回空
 */
export const getFormTabDeclarationType = (
  route: { path: string; query?: Record<string, any> }
): DeclarationTabType | '' => {
  if (!isFormTabPath(route.path)) return ''
  if (route.path.startsWith('/declaration-self')) return 'SELF'
  if (route.path.startsWith('/declaration-external')) return 'EXTERNAL'
  const raw = String(route.query?.declarationType || route.query?.type || '')
  return raw === 'SELF' || raw === 'EXTERNAL' ? raw : ''
}

/**
 * keep-alive 缓存键基座
 * 表单页按完整地址区分（同一张单的不同入口各自保留编辑状态），其余页面按路径区分
 *
 * 新建入口的 URL 不再携带一次性代号：同类型的新建标签本来就只有一个槽位，
 * 地址固定才能让“已开着新建页时再点新建”回到同一个缓存实例（只跳转、不把已填内容刷掉）
 */
export const getRouteCacheBase = (route: { path: string; fullPath: string }): string =>
  isFormTabPath(route.path) ? route.fullPath : route.path
