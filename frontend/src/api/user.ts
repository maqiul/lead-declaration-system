import request from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
}

export interface UserInfo {
  id: number
  username: string
  nickname?: string
  avatar?: string
  roles: string[]
  permissions: string[]
}

// 用户登录
export function login(data: LoginParams) {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}

// 获取用户信息
export function getInfo() {
  return request({
    url: '/user/info',
    method: 'get'
  })
}

// 用户登出
export function logout() {
  return request({
    url: '/user/logout',
    method: 'post'
  })
}

// 获取用户路由菜单
export function getUserRoutes() {
  return request({
    url: '/user/routes',
    method: 'get'
  })
}

// 获取用户权限标识
export function getUserPermissions() {
  return request({
    url: '/user/permissions',
    method: 'get'
  })
}

// 修改密码
export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request({
    url: '/user/change-password',
    method: 'post',
    data
  })
}