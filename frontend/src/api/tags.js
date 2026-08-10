import request from './request'

// 标签管理接口
export function getTagList() {
  return request.get('/tags')
}

export function createTag(name, color) {
  return request.post('/tags', null, { params: { name, color } })
}

export function updateTag(id, data) {
  return request.put(`/tags/${id}`, null, { params: data })
}

export function deleteTag(id) {
  return request.delete(`/tags/${id}`)
}
