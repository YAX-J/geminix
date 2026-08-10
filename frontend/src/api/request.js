import axios from 'axios'

// Axios 实例：默认走 vite proxy(/api)；可被 VITE_API_BASE 覆盖为后端直连地址
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 10000
})

// 请求拦截：附加 JWT Token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('worklog_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：统一解包后端 R{code,message,data}；401 清除登录态并跳转登录页
request.interceptors.response.use(
  (res) => {
    const body = res.data
    // 后端统一响应格式：{ code: 0, message, data }
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) {
        return body.data
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.message || err.message || '请求失败'
    if (status === 401) {
      localStorage.removeItem('worklog_token')
      localStorage.removeItem('worklog_user')
      // 避免在登录页自身重复跳转
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname)}`
      }
    }
    return Promise.reject(new Error(msg))
  }
)

export default request
