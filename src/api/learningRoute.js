const BASE_URL = import.meta.env.VITE_JAVA_API_BASE_URL || '/auth-api'
const learningRouteListCache = {
  data: null,
  promise: null
}
const learningRouteDetailCache = new Map()
const learningRouteDetailPromiseCache = new Map()

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
  if (learningRouteListCache.data) {
    return Promise.resolve(learningRouteListCache.data)
  }

  if (learningRouteListCache.promise) {
    return learningRouteListCache.promise
  }

  learningRouteListCache.promise = request('/api/learning-routes')
    .then((data) => {
      learningRouteListCache.data = data
      return data
    })
    .finally(() => {
      learningRouteListCache.promise = null
    })

  return learningRouteListCache.promise
}

export function getLearningRouteDetail(slug) {
  if (learningRouteDetailCache.has(slug)) {
    return Promise.resolve(learningRouteDetailCache.get(slug))
  }

  if (learningRouteDetailPromiseCache.has(slug)) {
    return learningRouteDetailPromiseCache.get(slug)
  }

  const pending = request(`/api/learning-routes/${encodeURIComponent(slug)}`)
    .then((data) => {
      learningRouteDetailCache.set(slug, data)
      return data
    })
    .finally(() => {
      learningRouteDetailPromiseCache.delete(slug)
    })

  learningRouteDetailPromiseCache.set(slug, pending)
  return pending
}

export function getCachedLearningRouteDetail(slug) {
  return learningRouteDetailCache.get(slug) || null
}

export function primeLearningRouteDetail(slug, detail) {
  learningRouteDetailCache.set(slug, detail)
}

export function prefetchLearningRouteDetail(slug) {
  return getLearningRouteDetail(slug).catch(() => null)
}
