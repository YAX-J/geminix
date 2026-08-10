import request from './request'

// 认证接口
export function loginApi(data) {
  return request.post('/auth/login', data)
}

export function registerApi(data) {
  return request.post('/auth/register', data)
}

export function meApi() {
  return request.get('/auth/me')
}
