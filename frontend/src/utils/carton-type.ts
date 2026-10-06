/**
 * 箱子类型基础定义
 *
 * 类型选项已字典化（dict_code = carton_type，见 sql/migration/80-carton-type-dict.sql），
 * 运行期通过 composables/useCartonTypes 拉取；本文件只提供内置兜底选项，
 * 用于字典迁移未执行或接口异常时下拉仍可用。
 *
 * 字段语义：
 * - label   中文名，落库 declaration_carton.type_chinese，作为下拉选中键
 * - english 英文复数，落库 type_english，既作箱号前缀（CARTONS1-90 / CASES91-140），
 *           也用于导出单证的包装种类
 *
 * 新增类型请优先在「系统管理 > 字典管理 > 箱子类型」中维护；若需同步兜底项，
 * 英文名必须以复数 S 结尾，因为导出时总箱数为 1 会截掉末尾字母生成单数（CARTONS -> CARTON）。
 */

export interface CartonTypeOption {
  /** 中文名，作为下拉值与 typeChinese */
  label: string
  /** 英文复数，作为箱号前缀与 typeEnglish */
  english: string
}

/** 内置兜底选项（与 carton_type 字典种子数据保持一致） */
export const CARTON_TYPE_FALLBACK: CartonTypeOption[] = [
  { label: '纸箱', english: 'CARTONS' },
  { label: '木箱', english: 'CASES' },
  { label: '托盘', english: 'PALLETS' },
]

/** 新增箱子时的默认类型 */
export const DEFAULT_CARTON_TYPE: CartonTypeOption = CARTON_TYPE_FALLBACK[0]

/** 箱子类型字典编码 */
export const CARTON_TYPE_DICT_CODE = 'carton_type'
