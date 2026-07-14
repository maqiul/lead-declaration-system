import { computed, provide, inject, type InjectionKey, type Ref } from 'vue'
import type { FlowTemplateStep } from '@/api/system/flowTemplate'

// ============================================================
// 类型定义
// ============================================================

/** 表单数据主结构（与后端 DeclarationForm 对应） */
export interface DeclarationFormData {
  id?: number
  formNo?: string
  status?: number
  declarationType?: string
  entityId?: number
  shipperCompany?: string
  shipperAddress?: string
  businessType?: string
  tradeTerms?: string
  paymentMethod?: string
  currency?: string
  exchangeRate?: number
  destinationCountry?: string
  destinationPort?: string
  loadingPort?: string
  transportMode?: string
  departureDate?: string
  vesselName?: string
  voyageNo?: string
  containerNo?: string
  sealNo?: string
  cartonCount?: number
  grossWeight?: number
  netWeight?: number
  volume?: number
  totalAmount?: number
  totalAmountCny?: number
  applicantName?: string
  applicantPhone?: string
  applicantEmail?: string
  remark?: string
  products?: any[]
  [key: string]: any
}

/** Section 组件可访问的共享状态（使用 Record 允许动态字段） */
export type FormSharedState = Record<string, any> & {
  // 核心数据（类型化字段，方便快速访问）
  formData: DeclarationFormData
  formId: Ref<number | null>
  formStatus: Ref<number | null>
  submitting: Ref<boolean>
}

// ============================================================
// Provide / Inject
// ============================================================

const FORM_STATE_KEY: InjectionKey<FormSharedState> = Symbol('declarationForm')

/** FormComposition 调用：向所有子组件提供表单共享状态 */
export function provideFormState(state: FormSharedState) {
  provide(FORM_STATE_KEY, state)
  return state
}

/** Section 组件调用：获取表单共享状态 */
export function useFormState(): FormSharedState {
  const state = inject(FORM_STATE_KEY)
  if (!state) {
    throw new Error('useFormState must be used within a component that calls provideFormState')
  }
  return state
}

// ============================================================
// Section 可见性逻辑（基于流程模板步骤配置）
// ============================================================

export type SectionKey = 'basic' | 'material' | 'supplement' | 'invoiceAmount' | 'invoice'

/**
 * 根据流程模板步骤配置，计算每个 section 是否应该显示
 * @param steps 当前模板的步骤配置列表
 * @returns 每个 section 是否启用
 */
export function useSectionVisibility(steps: Ref<FlowTemplateStep[]>) {
  const enabledSections = computed(() => {
    const map = new Map<string, boolean>()
    for (const step of steps.value) {
      if (step.formSection) {
        // 只要此 section 下有一个步骤启用，整个 section 就显示
        if (step.enabled === 1) {
          map.set(step.formSection, true)
        } else if (!map.has(step.formSection)) {
          map.set(step.formSection, false)
        }
      }
    }
    return map
  })

  const isSectionEnabled = (section: SectionKey): boolean => {
    // basic 始终显示
    if (section === 'basic') return true
    return enabledSections.value.get(section) ?? false
  }

  return { enabledSections, isSectionEnabled }
}

/**
 * 兼容旧逻辑的 section 可见性（不依赖模板配置时，按 formStatus 判断）
 * 新版 FormComposition 中应优先使用 useSectionVisibility
 */
export function useLegacySectionVisibility(formStatus: Ref<number | null>, formData: DeclarationFormData) {
  const showMaterialSection = computed(() => {
    return formStatus.value != null && formStatus.value >= 2
  })

  const showSupplementSection = computed(() => {
    // 简化版 - 有补充资料项且状态 >= 4 时显示
    return formStatus.value != null && formStatus.value >= 4
  })

  const showInvoiceAmountSection = computed(() => {
    return formStatus.value != null && formStatus.value >= 6 && formData.declarationType !== 'SELF'
  })

  const showInvoiceSection = computed(() => {
    return formStatus.value != null && formStatus.value >= 8 && formData.declarationType !== 'SELF'
  })

  return {
    showMaterialSection,
    showSupplementSection,
    showInvoiceAmountSection,
    showInvoiceSection,
  }
}
