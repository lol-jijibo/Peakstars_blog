const BASE_URL = import.meta.env.VITE_JAVA_API_BASE_URL || '/auth-api'

// 业务目的：统一承接后台管理台接口请求，保持和现有 code/message/data 响应结构一致。
// 业务逻辑：这里集中处理错误抛出与 JSON 解析，Pinia 仓库只负责消费标准化数据。
async function request(url, options = {}) {
  const response = await fetch(`${BASE_URL}${url}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  })

  const payload = await response.json()
  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || '后台管理接口请求失败')
  }

  return payload.data
}

export function sendAdminHeartbeat(clientId) {
  return request('/api/admin/heartbeat', {
    method: 'POST',
    body: JSON.stringify({ clientId })
  })
}

export function getAdminDashboard() {
  return request('/api/admin/dashboard')
}

export function getAdminContentList(type) {
  return request(`/api/admin/content?type=${encodeURIComponent(type)}`)
}

export function createAdminContent(type, payload) {
  return request(`/api/admin/content?type=${encodeURIComponent(type)}`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function updateAdminContent(type, contentKey, payload) {
  return request(`/api/admin/content/${encodeURIComponent(contentKey)}?type=${encodeURIComponent(type)}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

export function batchSaveAdminContent(type, records) {
  return request(`/api/admin/content/batch?type=${encodeURIComponent(type)}`, {
    method: 'POST',
    body: JSON.stringify({ records })
  })
}

export function deleteAdminContent(type, contentKey) {
  return request(`/api/admin/content/${encodeURIComponent(contentKey)}?type=${encodeURIComponent(type)}`, {
    method: 'DELETE'
  })
}
