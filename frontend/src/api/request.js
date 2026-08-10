import axios from 'axios'

// Axios 实例：默认走 vite proxy(/api)；可被 VITE_API_BASE 覆盖为后端直连地址
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 10000
})

// 请求拦截：可在此附加 Token（登录模块就绪后接入）
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('worklog_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：统一错误提示
request.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const msg = err.response?.data?.message || err.message || '请求失败'
    console.error('[API]', msg)
    return Promise.reject(new Error(msg))
  }
)

export default request
