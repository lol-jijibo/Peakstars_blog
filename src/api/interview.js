// API 基础地址配置
// 开发时指向本地后端，生产时改为实际部署地址
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:3000'

/**
 * 通用请求封装
 */
async function request(url, options = {}) {
  try {
    const response = await fetch(`${BASE_URL}${url}`, {
      headers: { 'Content-Type': 'application/json' },
      ...options
    })
    const data = await response.json()
    if (data.code !== 0) {
      throw new Error(data.message || '请求失败')
    }
    return data.data
  } catch (err) {
    console.error(`API 请求失败 [${url}]:`, err.message)
    throw err
  }
}

/**
 * 获取面经列表
 * @param {Object} params - { category, keyword, page, pageSize }
 */
export function getInterviews(params = {}) {
  const query = new URLSearchParams()
  if (params.category && params.category !== 'all') query.set('category', params.category)
  if (params.keyword) query.set('keyword', params.keyword)
  if (params.page) query.set('page', params.page)
  if (params.pageSize) query.set('pageSize', params.pageSize)
  const qs = query.toString()
  return request(`/api/interviews${qs ? '?' + qs : ''}`)
}

/**
 * 获取面经详情
 * @param {number|string} id
 */
export function getInterviewDetail(id) {
  return request(`/api/interviews/${id}`)
}

/**
 * 点赞
 * @param {number|string} id
 */
export function likeInterview(id) {
  return request(`/api/interviews/${id}/like`, { method: 'POST' })
}

/**
 * 收藏
 * @param {number|string} id
 */
export function collectInterview(id) {
  return request(`/api/interviews/${id}/collect`, { method: 'POST' })
}

/**
 * 获取分类列表
 */
export function getCategories() {
  return request('/api/categories')
}
