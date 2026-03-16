import axios, { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import { getToken } from '@/utils/auth'

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
      message.error(res.message || 'Error')
      
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
    message.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service