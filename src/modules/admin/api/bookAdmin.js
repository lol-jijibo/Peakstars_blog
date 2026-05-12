const BASE_URL = import.meta.env.VITE_JAVA_API_BASE_URL || '/auth-api'

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
    throw new Error(payload.message || '书籍管理接口请求失败')
  }

  return payload.data
}

function uploadFile(url, file, fieldName = 'file') {
  const formData = new FormData()
  formData.append(fieldName, file)

  return fetch(`${BASE_URL}${url}`, {
    method: 'POST',
    body: formData
  })
    .then((response) => response.json())
    .then((payload) => {
      if (payload.code !== 0) {
        throw new Error(payload.message || '文件上传失败')
      }
      return payload.data
    })
}

export function createBookImportJob(file) {
  return uploadFile('/api/admin/books/import-jobs', file)
}

export function createBookImportJobFromFile(file) {
  return uploadFile('/api/admin/books/import-jobs/file', file)
}

export function createBookImportJobFromExternal(payload) {
  return request('/api/admin/books/import-jobs/external', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function getBookImportJob(jobKey) {
  return request(`/api/admin/books/import-jobs/${encodeURIComponent(jobKey)}`)
}

export function listRecentImportJobs(limit = 20) {
  return request(`/api/admin/books/import-jobs?limit=${limit}`)
}

export function getBookImportJobChapters(jobKey) {
  return request(`/api/admin/books/import-jobs/${encodeURIComponent(jobKey)}/chapters`)
}

export function updateBookImportJobChapter(jobKey, tempChapterKey, payload) {
  return request(`/api/admin/books/import-jobs/${encodeURIComponent(jobKey)}/chapters/${encodeURIComponent(tempChapterKey)}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

export function publishBookImportJob(jobKey) {
  return request(`/api/admin/books/import-jobs/${encodeURIComponent(jobKey)}/publish`, {
    method: 'POST'
  })
}

export function getAdminBooks() {
  return request('/api/admin/books')
}
