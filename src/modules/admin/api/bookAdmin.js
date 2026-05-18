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

export function updateBookImportJobMetadata(jobKey, payload) {
  return request(`/api/admin/books/import-jobs/${encodeURIComponent(jobKey)}/metadata`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

export function updateBookImportJobChapter(jobKey, tempChapterKey, payload) {
  return request(`/api/admin/books/import-jobs/${encodeURIComponent(jobKey)}/chapters/${encodeURIComponent(tempChapterKey)}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

export function updateBookImportJobChapters(jobKey, chapters) {
  return request(`/api/admin/books/import-jobs/${encodeURIComponent(jobKey)}/chapters`, {
    method: 'PUT',
    body: JSON.stringify({ chapters })
  })
}

export function publishBookImportJob(jobKey) {
  return request(`/api/admin/books/import-jobs/${encodeURIComponent(jobKey)}/publish`, {
    method: 'POST'
  })
}

export function approveBookImportJobs(jobKeys) {
  return request('/api/admin/books/import-jobs/batch/approve', {
    method: 'POST',
    body: JSON.stringify({ jobKeys })
  })
}

export function rejectBookImportJobs(jobKeys, reason = '') {
  return request('/api/admin/books/import-jobs/batch/reject', {
    method: 'POST',
    body: JSON.stringify({ jobKeys, reason })
  })
}

export function publishBookImportJobs(jobKeys) {
  return request('/api/admin/books/import-jobs/batch/publish', {
    method: 'POST',
    body: JSON.stringify({ jobKeys })
  })
}

export function getAdminBooks() {
  return request('/api/admin/books')
}

export function updateAdminBookCategory(bookKey, category) {
  return request(`/api/admin/books/${encodeURIComponent(bookKey)}/category`, {
    method: 'PUT',
    body: JSON.stringify({ category })
  })
}

export function deleteAdminBook(bookKey) {
  return request(`/api/admin/books/${encodeURIComponent(bookKey)}`, {
    method: 'DELETE'
  })
}

export function deleteImportJob(jobKey) {
  return request(`/api/admin/books/import-jobs/${encodeURIComponent(jobKey)}`, {
    method: 'DELETE'
  })
}

export function restoreImportJob(jobKey) {
  return request(`/api/admin/books/import-jobs/${encodeURIComponent(jobKey)}/restore`, {
    method: 'POST'
  })
}

export function batchDeleteImportJobs(jobKeys) {
  return request('/api/admin/books/import-jobs/batch/delete', {
    method: 'POST',
    body: JSON.stringify({ jobKeys })
  })
}

export function deleteImportJobsByCategory(category) {
  return request(`/api/admin/books/import-jobs/category/${encodeURIComponent(category)}`, {
    method: 'DELETE'
  })
}

export function repairImportJobCover(jobKey) {
  return request(`/api/admin/books/import-jobs/${encodeURIComponent(jobKey)}/repair-cover`, {
    method: 'POST'
  })
}
