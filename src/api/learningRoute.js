const BASE_URL = import.meta.env.VITE_JAVA_API_BASE_URL || '/auth-api'

async function request(url, options = {}) {
  const response = await fetch(`${BASE_URL}${url}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  })
  const data = await response.json()
  if (data.code !== 0) {
    throw new Error(data.message || '学习路线加载失败')
  }
  return data.data
}

export function getLearningRoutes() {
  return request('/api/learning-routes')
}

export function getLearningRouteDetail(slug) {
  return request(`/api/learning-routes/${encodeURIComponent(slug)}`)
}
