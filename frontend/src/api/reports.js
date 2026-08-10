import request from './request'

// 日报接口（后端就绪后启用，见 stores/reports.js 的 mock 开关）
export function getReportList(params) {
  return request.get('/reports', { params })
}

export function getReport(id) {
  return request.get(`/reports/${id}`)
}

export function createReport(data) {
  return request.post('/reports', data)
}

export function updateReport(id, data) {
  return request.put(`/reports/${id}`, data)
}

export function deleteReport(id) {
  return request.delete(`/reports/${id}`)
}
