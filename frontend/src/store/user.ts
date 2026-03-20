import { defineStore } from 'pinia'
import { login, getInfo } from '@/api/user'
import { getToken, setToken, removeToken } from '@/utils/auth'
import router, { asyncRoutes } from '@/router'
import { filterAsyncRoutes } from '@/utils/route'

interface UserState {
  token: string | null
  name: string
  avatar: string
  roles: string[]
  permissions: string[]
  routes: any[]
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: getToken(),
    name: '',
    avatar: '',
    roles: [],
    permissions: [],
    routes: []
  }),

  actions: {
    // 登录
    async login(userInfo: { username: string; password: string }) {
      const { username, password } = userInfo
      const response = await login({ username: username.trim(), password: password })
      // 后端返回格式: { code: 200, m.essage: "登录成功", data: "token-string" }
      const token = response.data.data
      this.token = token
      setToken(token)
    },

    // 获取用户信息
    async getUserInfo() {
      const response = await getInfo()
      // 后端返回格式: { code: 200, message: "success", data: { id, username, ... } }
      const data = response.data.data
      
      if (!data) {
        throw new Error('Verification failed, please Login again.')
      }
      
      const { roles, permissions } = data
      
      // roles must be a non-empty array
      if (!roles || roles.length <= 0) {
        this.roles = ['ROLE_DEFAULT']
      } else {
        this.roles = roles
      }
      
      this.permissions = permissions
      this.name = data.nickname || data.username
      this.avatar = data.avatar
    },

    // 生成路由
    async generateRoutes() {
      let accessedRoutes
      if (this.roles.includes('admin')) {
        accessedRoutes = asyncRoutes || []
      } else {
        accessedRoutes = filterAsyncRoutes(asyncRoutes, this.roles)
      }
      
      console.log('准备添加的路由:', accessedRoutes)
      this.routes = accessedRoutes
      
      accessedRoutes.forEach((route: any) => {
        console.log('添加路由:', route.path, route.name)
        // 如果路由有 children，先添加父路由，再添加子路由
        if (route.children && route.children.length > 0) {
          // 添加父路由
          router.addRoute(route)
          // 添加子路由，使用父路由的 name 作为 parentName
          if (route.name) {
            route.children.forEach((child: any) => {
              console.log('  添加子路由:', child.path, child.name)
              router.addRoute(route.name!, child)
            })
          }
        } else {
          router.addRoute(route)
        }
      })
      
      // 添加404路由
      router.addRoute({ path: '/:pathMatch(.*)*', redirect: '/404' })
      
      // 打印所有已注册的路由
      console.log('当前所有路由:')
      router.getRoutes().forEach(r => {
        console.log(`  ${r.path} -> ${String(r.name)}`)
      })
    },

    // 重置token
    async resetToken() {
      this.token = ''
      this.roles = []
      this.permissions = []
      removeToken()
    }
  }
})