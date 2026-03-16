/**
 * 权限工具函数
 */

import { useUserStore } from '@/store/user'

/**
 * 检查是否有指定权限
 * @param permission 权限标识
 * @returns boolean
 */
export function hasPermission(permission: string): boolean {
  const userStore = useUserStore()
  return userStore.permissions.includes(permission)
}

/**
 * 检查是否有任一权限
 * @param permissions 权限标识数组
 * @returns boolean
 */
export function hasAnyPermission(permissions: string[]): boolean {
  const userStore = useUserStore()
  return permissions.some(permission => userStore.permissions.includes(permission))
}

/**
 * 检查是否有所有权限
 * @param permissions 权限标识数组
 * @returns boolean
 */
export function hasAllPermissions(permissions: string[]): boolean {
  const userStore = useUserStore()
  return permissions.every(permission => userStore.permissions.includes(permission))
}

/**
 * 检查是否有指定角色
 * @param role 角色标识
 * @returns boolean
 */
export function hasRole(role: string): boolean {
  const userStore = useUserStore()
  return userStore.roles.includes(role)
}

/**
 * 检查是否有任一角色
 * @param roles 角色标识数组
 * @returns boolean
 */
export function hasAnyRole(roles: string[]): boolean {
  const userStore = useUserStore()
  return roles.some(role => userStore.roles.includes(role))
}

/**
 * 检查是否有所有角色
 * @param roles 角色标识数组
 * @returns boolean
 */
export function hasAllRoles(roles: string[]): boolean {
  const userStore = useUserStore()
  return roles.every(role => userStore.roles.includes(role))
}

/**
 * 判断是否为管理员
 * @returns boolean
 */
export function isAdmin(): boolean {
  return hasRole('admin') || hasRole('ROLE_ADMIN')
}

/**
 * 获取用户信息
 * @returns 用户信息对象
 */
export function getUserInfo() {
  const userStore = useUserStore()
  return {
    name: userStore.name,
    avatar: userStore.avatar,
    roles: userStore.roles,
    permissions: userStore.permissions
  }
}