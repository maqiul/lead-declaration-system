import axios, { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import { getToken } from '@/utils/auth'

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
      config.headers.set('satoken', token)
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
      showErrorOnce(res.message || 'Error')
      
      // 401: 未登录
      if (res.code === 401) {
        // TODO: 跳转到登录页
      }
      
      return Promise.reject(new Error(res.message || 'Error'))
    } else {
      // 返回完整的响应对象，包含code、message、data等字段
      return response
    }
  },
  (error) => {
    // 优先使用后端返回的业务错误信息（同样走去重）
    const backendMsg = error.response?.data?.message
    showErrorOnce(backendMsg || error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service