import request from './request'

// 问题接口（后端就绪后启用，见 stores/issues.js 的 mock 开关）
export function getIssueList(params) {
  return request.get('/issues', { params })
}

export function createIssue(data) {
  return request.post('/issues', data)
}

export function updateIssue(id, data) {
  return request.put(`/issues/${id}`, data)
}

export function deleteIssue(id) {
  return request.delete(`/issues/${id}`)
}

/** 状态切换：标记已解决 / 重新打开 */
export function toggleIssueStatus(id, status) {
  return request.patch(`/issues/${id}/status`, null, { params: { status } })
}
