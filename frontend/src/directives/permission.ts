import { App } from 'vue'

/**
 * 权限检查方法
 * @param {Array} value 权限值数组
 * @returns {Boolean}
 */
export function checkPermission(value: string[]): boolean {
  if (!value || value.length === 0) {
    return false
  }
  
  const userStore = useUserStore()
  const { permissions } = userStore
  
  if (!permissions || permissions.length === 0) {
    return false
  }
  
  return permissions.some(permission => value.includes(permission))
}

/**
 * 角色检查方法
 * @param {Array} value 角色值数组
 * @returns {Boolean}
 */
export function checkRole(value: string[]): boolean {
  if (!value || value.length === 0) {
    return false
  }
  
  const userStore = useUserStore()
  const { roles } = userStore
  
  if (!roles || roles.length === 0) {
    return false
  }
  
  return roles.some(role => value.includes(role))
}

/**
 * 权限指令
 */
export const permission = {
  mounted(el: HTMLElement, binding: any) {
    const { value } = binding
    
    if (value && value instanceof Array && value.length > 0) {
      const hasPermission = checkPermission(value)
      
      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      throw new Error('权限指令值必须是数组格式，例如 v-permission="[\'user:query\', \'user:add\']"')
    }
  }
}

/**
 * 角色指令
 */
export const role = {
  mounted(el: HTMLElement, binding: any) {
    const { value } = binding
    
    if (value && value instanceof Array && value.length > 0) {
      const hasRole = checkRole(value)
      
      if (!hasRole) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      throw new Error('角色指令值必须是数组格式，例如 v-role="[\'admin\', \'editor\']"')
    }
  }
}

// 注册全局指令
export function setupPermissionDirective(app: App) {
  app.directive('permission', permission)
  app.directive('role', role)
}

// 导入store
import { useUserStore } from '@/store/user'