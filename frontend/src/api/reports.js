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

/** 周报导出（Markdown 文件下载，需登录） */
export function exportWeekly(date) {
  return request.get('/reports/export/weekly', { params: { date }, responseType: 'blob' })
}

/** 月报导出（Markdown 文件下载，需登录） */
export function exportMonthly(date) {
  return request.get('/reports/export/monthly', { params: { date }, responseType: 'blob' })
}

/** 日报 Excel 导出（xlsx 下载） */
export function exportReportsExcel() {
  return request.get('/reports/export/excel', { responseType: 'blob' })
}
