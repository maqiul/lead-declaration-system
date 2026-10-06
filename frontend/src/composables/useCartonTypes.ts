import { onMounted, ref } from 'vue'
import { getEnabledDictItems } from '@/api/system/dict'
import {
  CARTON_TYPE_DICT_CODE,
  CARTON_TYPE_FALLBACK,
  DEFAULT_CARTON_TYPE,
  type CartonTypeOption,
} from '@/utils/carton-type'

// 模块级缓存：箱子类型字典全局只拉一次，多个表单组件共享
const cartonTypeOptions = ref<CartonTypeOption[]>([...CARTON_TYPE_FALLBACK])
const cartonTypeLoading = ref(false)
let cartonTypeLoaded = false
let cartonTypePending: Promise<void> | null = null

/**
 * 拉取 carton_type 字典
 * 字典项约定：item_value = 英文前缀，item_label = 中文名
 * 字典为空或接口异常时保留内置兜底选项，保证下拉始终可用
 */
async function loadCartonTypes(force = false) {
  if (cartonTypeLoaded && !force) return
  if (cartonTypePending) return cartonTypePending

  cartonTypeLoading.value = true
  cartonTypePending = getEnabledDictItems(CARTON_TYPE_DICT_CODE)
    .then((res: any) => {
      const items = res.data?.code === 200 ? (res.data.data ?? []) : []
      const options = items
        .filter((item: any) => item.itemLabel && item.itemValue)
        .map((item: any) => ({ label: String(item.itemLabel), english: String(item.itemValue).toUpperCase() }))
      cartonTypeOptions.value = options.length > 0 ? options : [...CARTON_TYPE_FALLBACK]
      cartonTypeLoaded = true
    })
    .catch((e: unknown) => {
      console.error('[useCartonTypes] 箱子类型字典加载失败，回退内置选项', e)
      cartonTypeOptions.value = [...CARTON_TYPE_FALLBACK]
    })
    .finally(() => {
      cartonTypeLoading.value = false
      cartonTypePending = null
    })

  return cartonTypePending
}

/**
 * 按中文名取英文前缀
 * 依次回退：字典选项 -> 内置兜底 -> 该箱子已有值 -> 默认类型
 * @param typeChinese 箱子中文名（a-select 回调值类型较宽，故用 unknown）
 * @param currentEnglish 该箱子已有的 typeEnglish，用于字典未覆盖的历史数据
 */
export function getCartonEnglish(typeChinese?: unknown, currentEnglish?: string | null): string {
  const matched = cartonTypeOptions.value.find(option => option.label === typeChinese)
    || CARTON_TYPE_FALLBACK.find(option => option.label === typeChinese)
  return matched?.english || String(currentEnglish || DEFAULT_CARTON_TYPE.english)
}

/**
 * 箱子类型 composable —— 组件挂载时自动加载字典选项
 *
 * 使用方式：
 * ```ts
 * const { options: cartonTypes, getEnglish } = useCartonTypes()
 * ```
 */
export function useCartonTypes() {
  onMounted(() => {
    loadCartonTypes()
  })

  return {
    /** 下拉选项：label 为中文名（选中值），english 为英文前缀 */
    options: cartonTypeOptions,
    loading: cartonTypeLoading,
    /** 新增箱子时的默认类型 */
    defaultType: DEFAULT_CARTON_TYPE,
    getEnglish: getCartonEnglish,
    /** 字典管理页改动后可调用刷新 */
    refresh: () => loadCartonTypes(true),
  }
}
