const BASE_URL = import.meta.env.VITE_JAVA_API_BASE_URL || '/auth-api'

// 业务目的：用一份内存缓存承接三个内容模块的列表数据，避免页面和导航重复请求同一接口。
// 业务逻辑：每个模块同时维护已完成数据和进行中的 Promise，保证并发场景下只发起一次网络请求。
const contentCache = {
  techArticles: { data: null, promise: null },
  worldNews: { data: null, promise: null },
  aiHotspots: { data: null, promise: null }
}

async function request(url) {
  const response = await fetch(`${BASE_URL}${url}`, {
    headers: { 'Content-Type': 'application/json' }
  })
  const payload = await response.json()
  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || '内容加载失败')
  }
  return payload.data
}

// 业务目的：复用统一缓存逻辑，减少三个列表接口的重复代码。
// 业务逻辑：命中缓存直接返回结果，命中进行中的 Promise 则复用同一请求，完成后回填缓存。
function loadWithCache(cacheKey, url) {
  const target = contentCache[cacheKey]
  if (target.data) {
    return Promise.resolve(target.data)
  }

  if (target.promise) {
    return target.promise
  }

  target.promise = request(url)
    .then((data) => {
      target.data = data
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

export function getWorldNews() {
  return loadWithCache('worldNews', '/api/content/world-news')
}

export function getAiHotspots() {
  return loadWithCache('aiHotspots', '/api/content/ai-hotspots')
}
