import { ref, computed, onMounted, type Ref, type ComputedRef } from 'vue'
import { getEnabledDictItems, type DictOption } from '@/api/system/dict'

// 全局字典缓存（按 dictCode 缓存）
const dictCache = new Map<string, Ref<DictOption[]>>()
const dictLoading = new Map<string, Ref<boolean>>()
const dictPending = new Set<string>()

/**
 * 字典 composable — 自动缓存，同一字典只请求一次
 *
 * 使用方式：
 * ```ts
 * const { options, loading, refresh } = useDict('process_type')
 * ```
 */
export function useDict(dictCode: string) {
  // 初始化缓存
  if (!dictCache.has(dictCode)) {
    dictCache.set(dictCode, ref<DictOption[]>([]))
  }
  if (!dictLoading.has(dictCode)) {
    dictLoading.set(dictCode, ref(false))
  }

  const options = dictCache.get(dictCode)!
  const loading = dictLoading.get(dictCode)!

  /** 加载字典项 */
  async function load() {
    if (dictPending.has(dictCode)) return
    dictPending.add(dictCode)
    loading.value = true
    try {
      const res = await getEnabledDictItems(dictCode)
      if (res.data?.code === 200) {
        const items = res.data.data ?? []
        options.value = items.map((item: any) => ({
          value: item.itemValue,
          label: item.itemLabel,
          color: item.itemColor,
        }))
      }
    } catch (e) {
      console.error(`[useDict] 加载字典失败: ${dictCode}`, e)
    } finally {
      loading.value = false
      dictPending.delete(dictCode)
    }
  }

  /** 刷新字典（清除缓存重新加载） */
  async function refresh() {
    options.value = []
    await load()
  }

  /** 按 value 获取 label */
  function getLabel(value: string): string {
    return options.value.find(o => o.value === value)?.label ?? value
  }

  /** 按 value 获取 color */
  function getColor(value: string): string | undefined {
    return options.value.find(o => o.value === value)?.color
  }

  /** 颜色映射（value -> color） */
  const colorMap: ComputedRef<Record<string, string>> = computed(() => {
    const map: Record<string, string> = {}
    for (const opt of options.value) {
      if (opt.color) map[opt.value] = opt.color
    }
    return map
  })

  /** label 映射（value -> label） */
  const labelMap: ComputedRef<Record<string, string>> = computed(() => {
    const map: Record<string, string> = {}
    for (const opt of options.value) {
      map[opt.value] = opt.label
    }
    return map
  })

  // 组件挂载时自动加载（如果还没加载过）
  onMounted(() => {
    if (options.value.length === 0) {
      load()
    }
  })

  return {
    options,
    loading,
    refresh,
    getLabel,
    getColor,
    colorMap,
    labelMap,
  }
}
