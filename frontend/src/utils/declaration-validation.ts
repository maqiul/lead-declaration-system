/**
 * 申报单提交前完整性校验
 *
 * 与后端 DeclarationFormController.validateSubmitCompleteness
 * 及表单页 FormComposition.handleSubmit 的校验规则保持一致，
 * 作为唯一的前端校验来源，供各列表页“提交”动作复用，避免逻辑散落走样。
 */

export interface DeclarationCompletenessResult {
  /** 是否通过校验 */
  valid: boolean
  /** 未通过时的第一条错误信息 */
  message: string
}

/**
 * 校验申报单是否满足提交所需的完整性。
 *
 * @param form 完整申报单详情（来自 getDeclarationDetail），需包含
 *             consigneeCompany / destinationCountry 等字段，以及
 *             products / cartonProducts 列表。
 */
export function validateDeclarationCompleteness(form: any): DeclarationCompletenessResult {
  const fail = (message: string): DeclarationCompletenessResult => ({ valid: false, message })

  if (!form) return fail('申报单数据不存在，无法提交')
  if (!form.consigneeCompany) return fail('收货人公司名未填写，无法提交')
  if (!form.consigneeAddress) return fail('收货人地址未填写，无法提交')
  if (!form.destinationCountry) return fail('目的地国家未选择，无法提交')
  if (!form.tradeCountry) return fail('贸易国家未选择，无法提交')
  if (!form.transportMode) return fail('运输方式未选择，无法提交')
  if (!form.departureCity) return fail('出发城市未选择，无法提交')
  if (!form.currency) return fail('货币未选择，无法提交')

  const products: any[] = form.products || []
  if (products.length === 0) return fail('请至少添加一个产品后再提交')

  // 每个产品必须已分配到箱子（与后端 unassignedProducts 校验一致）
  const cartonProducts: any[] = form.cartonProducts || []
  const assignedProductIds = new Set<number>()
  cartonProducts.forEach((cp: any) => {
    if (cp.productId != null) assignedProductIds.add(cp.productId)
  })

  const unassigned = products
    .filter((p: any) => p.id == null || !assignedProductIds.has(p.id))
    .map((p: any) => p.productName || '未命名产品')

  if (unassigned.length > 0) {
    return fail(`以下产品未分配箱子：${unassigned.join('、')}，请在箱子信息中关联产品后再提交`)
  }

  return { valid: true, message: '' }
}
