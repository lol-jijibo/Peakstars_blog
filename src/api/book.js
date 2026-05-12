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
    throw new Error(payload.message || '书籍接口请求失败')
  }

  return payload.data
}

export function getBooks() {
  return request('/api/content/books')
}

export function getBookDetail(bookKey) {
  return request(`/api/content/books/${encodeURIComponent(bookKey)}`)
}

export function getBookChapters(bookKey) {
  return request(`/api/content/books/${encodeURIComponent(bookKey)}/chapters`)
}

export function getBookChapterDetail(bookKey, chapterKey) {
  return request(`/api/content/books/${encodeURIComponent(bookKey)}/chapters/${encodeURIComponent(chapterKey)}`)
}
