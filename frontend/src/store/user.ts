import { defineStore } from 'pinia'
import { login, getInfo, logout as logoutApi } from '@/api/user'
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
  orgId: number | null
  orgType: string
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: getToken(),
    name: '',
    avatar: '',
    roles: [],
    permissions: [],
    routes: [],
    orgId: null,
    orgType: 'EXTERNAL'
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
      this.orgId = data.orgId || null
      this.orgType = data.orgType || 'EXTERNAL'
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

    /**
     * 退出登录
     * 先请服务端销毁 token：is-share=false 下每次登录都会新建一个 token，
     * 退出不清理会让同账号会话数堆积到 maxLoginCount 后把最早那个顶下线（表现为“token 突然失效”）
     */
    async logout() {
      try {
        await logoutApi()
      } catch (error) {
        // 凭证已失效或网络异常：本地照样清状态，不能把用户困在系统里
        console.warn('服务端退出失败', error)
      }
      await this.resetToken()
    },

    // 重置token
    async resetToken() {
      this.token = ''
      this.roles = []
      this.permissions = []
      this.orgId = null
      this.orgType = 'EXTERNAL'
      removeToken()
    }
  }
})