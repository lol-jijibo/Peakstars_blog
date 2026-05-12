const BASE_URL = import.meta.env.VITE_JAVA_API_BASE_URL || '/auth-api'

const CACHE_TTL_MS = 5 * 60 * 1000

const contentCache = {
  techArticles: { data: null, promise: null, timestamp: 0 },
  worldNews: { data: null, promise: null, timestamp: 0 },
  aiHotspots: { data: null, promise: null, timestamp: 0 }
}

async function request(url, options = {}) {
  const response = await fetch(`${BASE_URL}${url}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  })
  const payload = await response.json()
  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || '内容加载失败')
  }
  return payload.data
}

function loadWithCache(cacheKey, url) {
  const target = contentCache[cacheKey]
  if (target.data && (Date.now() - target.timestamp < CACHE_TTL_MS)) {
    return Promise.resolve(target.data)
  }

  if (target.promise) {
    return target.promise
  }

  target.promise = request(url)
    .then((data) => {
      target.data = data
      target.timestamp = Date.now()
      return data
    })
    .finally(() => {
      target.promise = null
    })

  return target.promise
}

export function getTechArticles() {
  return loadWithCache('techArticles', '/api/content/tech-articles')
}

export function invalidateTechArticlesCache() {
  contentCache.techArticles.data = null
  contentCache.techArticles.promise = null
  contentCache.techArticles.timestamp = 0
}

export function getWorldNews() {
  return loadWithCache('worldNews', '/api/content/world-news')
}

export function invalidateWorldNewsCache() {
  contentCache.worldNews.data = null
  contentCache.worldNews.promise = null
  contentCache.worldNews.timestamp = 0
}

export function searchWorldNews(query, page = 0, size = 20) {
  return request(`/api/content/world-news/search?q=${encodeURIComponent(query)}&page=${page}&size=${size}`)
}

export function suggestWorldNews(query, size = 8) {
  return request(`/api/content/world-news/suggest?q=${encodeURIComponent(query)}&size=${size}`)
}

export function getWorldNewsRanking(type, page = 0, size = 6) {
  return request(`/api/content/world-news/ranking?type=${encodeURIComponent(type)}&page=${page}&size=${size}`)
}

export function getPopularWorldNews(size = 4) {
  return request(`/api/content/world-news/popular?size=${size}`)
}

export function getWorldNewsDetail(issueKey) {
  return request(`/api/content/world-news/${encodeURIComponent(issueKey)}`)
}

export function getAiHotspots() {
  return loadWithCache('aiHotspots', '/api/content/ai-hotspots')
}

export function invalidateAiHotspotsCache() {
  contentCache.aiHotspots.data = null
  contentCache.aiHotspots.promise = null
  contentCache.aiHotspots.timestamp = 0
}

export function incrementArticleReadCount(articleKey) {
  return request(`/api/content/tech-articles/${encodeURIComponent(articleKey)}/read`, { method: 'POST' })
}

export function getArticleComments(articleKey) {
  return request(`/api/content/tech-articles/${encodeURIComponent(articleKey)}/comments`)
}

export function addArticleComment(articleKey, data) {
  return request(`/api/content/tech-articles/${encodeURIComponent(articleKey)}/comments`, {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

export function deleteArticleComment(commentId) {
  return request(`/api/content/comments/${encodeURIComponent(commentId)}`, {
    method: 'DELETE'
  })
}
