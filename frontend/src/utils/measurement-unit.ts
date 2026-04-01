/**
 * 计量单位工具函数
 * 根据数量自动选择单数或复数形式
 */

import type { MeasurementUnit } from '@/api/system/measurement-unit'

/**
 * 根据数量获取单位的英文显示
 * @param unit 单位对象
 * @param quantity 数量
 * @returns 英文单位（单数或复数）
 */
export function getUnitDisplayText(unit: MeasurementUnit | string, quantity?: number): string {
  if (typeof unit === 'string') {
    return unit;
  }
  
  if (!unit) {
    return '';
  }
  
  // 数量为 1 时使用单数形式，否则使用复数形式
  const useSingular = quantity === 1;
  
  if (useSingular && unit.unitNameEnSingular) {
    return unit.unitNameEnSingular;
  }
  
  return unit.unitNameEn || '';
}

/**
 * 格式化单位选项供 Select 组件使用
 * @param units 单位列表
 * @returns Select 选项数组
 */
export function formatUnitOptions(units: MeasurementUnit[]) {
  return units.map(unit => ({
    value: unit.unitCode,
    label: `${unit.unitName} (${unit.unitNameEn})`,
    unit: unit // 保留完整单位信息
  }));
}

/**
 * 根据单位代码查找单位对象
 * @param units 单位列表
 * @param unitCode 单位代码
 * @returns 匹配的单位对象，未找到返回 null
 */
export function findUnitByCode(units: MeasurementUnit[], unitCode: string): MeasurementUnit | null {
  return units.find(unit => unit.unitCode === unitCode) || null;
}
