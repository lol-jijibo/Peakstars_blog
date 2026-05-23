const BASE_URL = import.meta.env.VITE_JAVA_API_BASE_URL || '/auth-api'

// 业务目的：统一承接后台管理台接口请求，保持和现有 code/message/data 响应结构一致。
// 业务逻辑：这里集中处理错误抛出与 JSON 解析，Pinia 仓库只负责消费标准化数据。
function getAuthHeaders() {
  const token = localStorage.getItem('interview_demo_access_token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function request(url, options = {}) {
  const response = await fetch(`${BASE_URL}${url}`, {
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeaders(),
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

// 业务目的：封面图片上传不走 JSON，使用 FormData multipart 方式提交。
// 业务逻辑：独立封装避免和 JSON 请求互相干扰，上传成功后直接返回图片 URL。
export async function uploadCoverImage(file, moduleType = '') {
  const formData = new FormData()
  formData.append('file', file)
  if (moduleType) {
    formData.append('moduleType', moduleType)
  }

  const response = await fetch(`${BASE_URL}/api/admin/upload/cover`, {
    method: 'POST',
    headers: { ...getAuthHeaders() },
    body: formData
  })

  const payload = await response.json()
  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || '封面图片上传失败')
  }

  return payload.data
}

/**
 * 业务目的：让后台富文本正文图片和封面图一样统一上传到 MinIO。
 * 业务逻辑：正文图片单独走 multipart 上传接口，成功后直接返回可回填到编辑器的图片地址。
 */
export async function uploadRichTextImage(file, moduleType = '') {
  const formData = new FormData()
  formData.append('file', file)
  if (moduleType) {
    formData.append('moduleType', moduleType)
  }

  const response = await fetch(`${BASE_URL}/api/admin/upload/rich-text-image`, {
    method: 'POST',
    headers: { ...getAuthHeaders() },
    body: formData
  })

  const payload = await response.json()
  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || '正文图片上传失败')
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

export function previewImportedAdminContent(type, payload) {
  return request(`/api/admin/content/import-preview?type=${encodeURIComponent(type)}`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function deleteAdminContent(type, contentKey) {
  return request(`/api/admin/content/${encodeURIComponent(contentKey)}?type=${encodeURIComponent(type)}`, {
    method: 'DELETE'
  })
}

// ── 草稿管理 ──────────────────────────────────────────

export function listAdminDrafts(type) {
  return request(`/api/admin/draft?type=${encodeURIComponent(type)}`)
}

export function saveAdminDraft(params) {
  return request('/api/admin/draft', {
    method: 'POST',
    body: JSON.stringify(params)
  })
}

export function deleteAdminDraft(draftKey) {
  return request(`/api/admin/draft/${encodeURIComponent(draftKey)}`, {
    method: 'DELETE'
  })
}
