export function formatReadHistoryTime(value, fallback = '刚刚阅读') {
  const date = new Date(String(value || '').replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) {
    return value || fallback
  }

  const now = new Date()
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const startOfTarget = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const diffDays = Math.round((startOfToday - startOfTarget) / 86400000)
  const timeText = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`

  if (diffDays === 0) {
    return `今天 ${timeText}`
  }
  if (diffDays === 1) {
    return `昨天 ${timeText}`
  }
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${timeText}`
}
