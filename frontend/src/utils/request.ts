import axios, { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import { getToken, removeToken, TOKEN_NAME } from '@/utils/auth'

// 错误提示去重：轮询/批量请求在会话失效、后端重启等场景会连续失败，
// 短时间内相同内容的错误提示只弹一次，避免页面堆一堆重复通知
let lastErrorMsg = ''
let lastErrorTime = 0
const showErrorOnce = (msg: string) => {
  const now = Date.now()
  if (msg === lastErrorMsg && now - lastErrorTime < 3000) return
  lastErrorMsg = msg
  lastErrorTime = now
  message.error(msg)
}

/** 取出本次请求实际携带的凭证，用于判定“失效的是哪条 token” */
const pickRequestToken = (config?: InternalAxiosRequestConfig): string | null => {
  const headers = config?.headers as unknown as { get?: (name: string) => unknown } | undefined
  const raw = headers?.get ? headers.get(TOKEN_NAME) : (headers as Record<string, unknown> | undefined)?.[TOKEN_NAME]
  return typeof raw === 'string' && raw ? raw : null
}

/**
 * 会话失效统一入口
 * keep-alive 下多个缓存页同时在轮询，token 失效后会连续返回 401，这里只处理第一次：
 * 清掉本地凭证 + 一条轻提示就够，不再弹模态框打断当前页面（挡屏，还会误伤正在填的表单）；
 * 用户下次切页时由路由守卫发现凭证已空，自然带到登录页并带上 redirect
 */
let sessionExpiredHandled = false
const handleSessionExpired = (usedToken?: string | null) => {
  if (sessionExpiredHandled) return
  sessionExpiredHandled = true
  // 只有当前仍在用这条凭证时才清本地凭证：后台轮询在途的间隙里用户可能已在另一标签重登，
  // 此时 401 甩的是失效的旧 token，无条件清除会把刚拿到的新凭证一起抹掉（表现为“刚登录又掉线”）
  if (!usedToken || getToken() === usedToken) {
    removeToken()
  }
  showErrorOnce('登录状态已失效，请重新登录')
}

/** 把后端业务码挂到 Error 上，供路由守卫区分“凭证无效”与“网络抖动” */
const buildBizError = (msg: string, code?: number) => {
  const err = new Error(msg) as Error & { code?: number }
  err.code = code
  return err
}

// 创建axios实例
const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 添加token到请求头
    const token = getToken()
    if (token) {
      config.headers.set(TOKEN_NAME, token)
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    
    // 如果自定义代码不是200，则将其判断为错误
    if (res.code !== 200) {
      // 401: 未登录/凭证失效 → 走统一的会话失效处理，不再刷提示
      if (res.code === 401) {
        handleSessionExpired(pickRequestToken(response.config))
        return Promise.reject(buildBizError(res.message || '用户未登录', 401))
      }
      showErrorOnce(res.message || 'Error')
      return Promise.reject(buildBizError(res.message || 'Error', res.code))
    } else {
      // 返回完整的响应对象，包含code、message、data等字段
      return response
    }
  },
  (error) => {
    // HTTP 401（后端未登录异常统一返回 401）同样归入会话失效处理
    if (error.response?.status === 401 || error.response?.data?.code === 401) {
      handleSessionExpired(pickRequestToken(error.config))
      return Promise.reject(error)
    }
    // 优先使用后端返回的业务错误信息（同样走去重）
    const backendMsg = error.response?.data?.message
    showErrorOnce(backendMsg || error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service