import request from '@/utils/request'

// 用户管理相关API
export interface UserQueryParams {
  pageNum?: number
  pageSize?: number
  username?: string
  phone?: string
  status?: number
}

export interface UserForm {
  id?: number
  username: string
  nickname: string
  phone: string
  email: string
  orgId: number
  roleIds: number[]
  status: number
}

// 获取用户列表
export function getUserList(params: UserQueryParams) {
  return request({
    url: '/user/page',
    method: 'get',
    params
  })
}

// 新增用户
export function addUser(data: UserForm) {
  return request({
    url: '/user',
    method: 'post',
    data
  })
}

// 修改用户
export function updateUser(id: number, data: UserForm) {
  return request({
    url: `/user/${id}`,
    method: 'put',
    data
  })
}

// 删除用户
export function deleteUser(id: number) {
  return request({
    url: `/user/${id}`,
    method: 'delete'
  })
}

// 获取用户详情
export function getUser(id: number) {
  return request({
    url: `/user/${id}`,
    method: 'get'
  })
}

// 重置用户密码
export function resetUserPwd(userId: number) {
  return request({
    url: `/user/${userId}/password`,
    method: 'put'
  })
}

// 角色管理相关API
export interface RoleQueryParams {
  pageNum?: number
  pageSize?: number
  roleName?: string
  roleCode?: string
  status?: number
}

export interface RoleForm {
  id?: number
  roleName: string
  roleCode: string
  description: string
  dataScope: number
  status: number
}

// 获取角色列表
export function getRoleList(params: RoleQueryParams) {
  return request({
    url: '/role/page',
    method: 'get',
    params
  })
}

// 新增角色
export function addRole(data: RoleForm) {
  return request({
    url: '/role',
    method: 'post',
    data
  })
}

// 修改角色
export function updateRole(id: number, data: RoleForm) {
  return request({
    url: `/role/${id}`,
    method: 'put',
    data
  })
}

// 删除角色
export function deleteRole(id: number) {
  return request({
    url: `/role/${id}`,
    method: 'delete'
  })
}

// 为管理员角色分配所有权限
export function assignAllPermissionsToAdmin(roleId: number) {
  return request({
    url: `/role/admin/all-permissions/${roleId}`,
    method: 'post'
  })
}

// 获取角色菜单权限
export function getRoleMenus(roleId: number) {
  return request({
    url: `/role/menu/${roleId}`,
    method: 'get'
  })
}

// 更新角色菜单权限
export function updateRoleMenus(data: { roleId: number; menuIds: number[] }) {
  return request({
    url: `/role/menu`,
    method: 'post',
    data
  })
}

// 组织管理相关API
export interface OrgQueryParams {
  orgName?: string
  status?: number
}

export interface OrgForm {
  id?: number
  orgName: string
  orgCode: string
  parentId: number | null
  leader: string
  phone: string
  email: string
  sort: number
  status: number
}

// 获取组织树
export function getOrgTree(params?: OrgQueryParams) {
  return request({
    url: '/org/tree',
    method: 'get',
    params
  })
}

// 新增组织
export function addOrg(data: OrgForm) {
  return request({
    url: '/org',
    method: 'post',
    data
  })
}

// 修改组织
export function updateOrg(id: number, data: OrgForm) {
  return request({
    url: `/org/${id}`,
    method: 'put',
    data
  })
}

// 删除组织
export function deleteOrg(id: number) {
  return request({
    url: `/org/${id}`,
    method: 'delete'
  })
}

// 菜单管理相关API
export interface MenuQueryParams {
  menuName?: string
  status?: number
}

export interface MenuForm {
  id?: number
  menuName: string
  menuCode: string
  parentId: number | null
  menuType: number
  path: string
  component: string
  permission: string
  icon: string
  sort: number
  isExternal: number
  isCache: number
  isShow: number
  status: number
}

// 获取菜单列表
export function getMenuList(params?: MenuQueryParams) {
  return request({
    url: '/system/menu/page',
    method: 'get',
    params
  })
}



// 获取菜单树
export function getMenuTree() {
  return request({
    url: '/system/menu/tree',
    method: 'get'
  })
}

// 获取用户路由菜单（登录后使用）
export function getUserRoutes() {
  return request({
    url: '/system/menu/routes',
    method: 'get'
  })
}


// 新增菜单
export function addMenu(data: MenuForm) {
  return request({
    url: '/system/menu',
    method: 'post',
    data
  })
}

// 修改菜单
export function updateMenu(id: number, data: MenuForm) {
  return request({
    url: `/system/menu/${id}`,
    method: 'put',
    data
  })
}

// 删除菜单
export function deleteMenu(id: number) {
  return request({
    url: `/system/menu/${id}`,
    method: 'delete'
  })
}

// 个人中心相关API
export interface ProfileForm {
  nickname: string
  phone?: string
  email?: string
}

export interface ChangePasswordForm {
  oldPassword: string
  newPassword: string
}

// 更新个人资料
export function updateUserProfile(data: ProfileForm) {
  return request({
    url: '/user/profile',
    method: 'put',
    data
  })
}

// 修改密码
export function changePassword(data: ChangePasswordForm) {
  return request({
    url: '/user/password',
    method: 'put',
    data
  })
}

// 上传头像
export function uploadAvatar(data: FormData) {
  return request({
    url: '/user/avatar',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}