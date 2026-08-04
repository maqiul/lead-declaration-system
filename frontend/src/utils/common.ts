/**
 * 通用工具函数
 */

/**
 * 格式化日期
 * @param date 日期
 * @param format 格式，默认为 yyyy-MM-dd HH:mm:ss
 * @returns 格式化后的日期字符串
 *
 * 说明：
 * 1. 纯日期输入（如后端 LocalDate 序列化的 '2026-07-16'）自动只显示日期，不追加 00:00:00
 * 2. 时间部分全为零（午夜）时也省略时间部分，避免出现 00:00:00
 * 3. 占位符一次性替换，避免链式 replace 误伤已替换的数字内容
 */
export const formatDate = (date: string | Date | null | undefined, format = 'yyyy-MM-dd HH:mm:ss'): string => {
  if (!date) {
    return ''
  }

  // 纯日期值（无时间部分）：强制只按日期格式展示
  if (typeof date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(date.trim())) {
    format = 'yyyy-MM-dd'
  }

  const d = new Date(date)
  if (isNaN(d.getTime())) {
    return String(date)
  }
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')

  // 午夜时间（00:00:00）省略时间部分，避免显示 00:00:00
  if (hours === '00' && minutes === '00' && seconds === '00') {
    format = format.split(' ')[0]
  }

  return format.replace(/yyyy|MM|dd|HH|mm|ss/g, token => {
    switch (token) {
      case 'yyyy': return String(year)
      case 'MM': return month
      case 'dd': return day
      case 'HH': return hours
      case 'mm': return minutes
      case 'ss': return seconds
      default: return token
    }
  })
}

/**
 * 深拷贝对象
 * @param obj 要深拷贝的对象
 * @returns 拷贝后的对象
 */
export const deepClone = <T>(obj: T): T => {
  if (obj === null || typeof obj !== 'object') {
    return obj
  }

  if (obj instanceof Date) {
    return new Date(obj.getTime()) as unknown as T
  }

  if (obj instanceof Array) {
    return obj.map(item => deepClone(item)) as unknown as T
  }

  const clonedObj = {} as T
  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) {
      clonedObj[key] = deepClone(obj[key])
    }
  }

  return clonedObj
}