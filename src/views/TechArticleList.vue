<template>
  <div class="article-hub-page" :class="{ 'article-hub-page--restoring': articleListRestoreAnimating }">
    <div class="article-progress-bar" :style="{ width: `${scrollProgress}%` }"></div>

    <header class="article-topbar">
      <button class="article-topbar-brand" type="button" @click="goHome">
        Peak<span>Depth</span>
      </button>

      <nav class="article-topbar-nav" aria-label="技术文章导航">
        <button
          v-for="item in topbarCategoryItems"
          :key="item.key"
          type="button"
          :class="{ active: activeCategoryKey === item.key }"
          @click="selectCategory(item.key)"
        >
          {{ item.label }}
        </button>
        <button
          type="button"
          :class="{ active: activeModeKey === 'vip' }"
          @click="selectMode('vip')"
        >
          VIP
        </button>
      </nav>

      <div class="article-topbar-actions">
        <button class="article-topbar-theme" type="button" :title="isDark ? '切换到浅色' : '切换到深色'" @click="toggleTheme">
          <span class="theme-icon">{{ isDark ? '☀' : '☾' }}</span>
        </button>
        <button class="article-topbar-action" type="button" @click="goHome">返回首页 →</button>
      </div>
    </header>

    <main class="article-hub-main">
      <section class="article-page-header">
        <div class="article-page-header-copy">
          <span class="article-page-kicker">{{ activeCategoryInfo.caption }} · {{ activeModeInfo.label }}</span>
          <h1>技术文章</h1>
        </div>

        <div class="article-mode-pills">
          <button
            v-for="mode in articleModes"
            :key="mode.key"
            type="button"
            class="article-mode-pill"
            :class="{ active: activeModeKey === mode.key }"
            @click="selectMode(mode.key)"
          >
            {{ mode.label }}
            <small>{{ countByMode(mode.key) }}</small>
          </button>
        </div>

        <div class="article-category-pills">
          <button
            v-for="category in sidebarCategories"
            :key="category.key"
            type="button"
            class="article-category-pill"
            :class="{ active: activeCategoryKey === category.key }"
            @click="selectCategory(category.key)"
          >
            {{ category.label }}
            <small>{{ countByCategory(category.key) }}</small>
          </button>
        </div>
      </section>

      <div v-if="totalArticles > 0" class="article-stream-header">
        <span class="article-stream-count">共 {{ totalArticles }} 篇文章</span>
        <div class="article-page-size-selector">
          <span class="page-size-label">每页显示</span>
          <select v-model.number="pageSize" class="page-size-select" @change="onPageSizeChange">
            <option v-for="opt in pageSizeOptions" :key="opt" :value="opt">{{ opt }} 条</option>
          </select>
        </div>
      </div>

      <section v-if="filteredArticles.length" class="article-stream">
        <article
          v-for="article in filteredArticles"
          :key="article.id"
          class="article-row"
          tabindex="0"
          role="link"
          @click="openArticleDetail(article.id)"
          @keydown.enter="openArticleDetail(article.id)"
        >
          <div class="article-row-body">
            <div class="article-row-meta">
              <span class="article-category-badge" :class="[`badge-${article.category}`]">{{ article.categoryLabel }}</span>
              <span v-if="article.featured" class="article-status-badge badge-featured">精选</span>
              <span v-if="article.isVip" class="article-status-badge badge-vip">VIP</span>
              <span v-if="article.isCollected" class="article-status-badge badge-collected">已收藏</span>
              <span v-if="article.isLiked" class="article-status-badge badge-liked">已喜欢</span>
              <span class="article-meta-sep">·</span>
              <span class="article-meta-text">{{ formatLongDate(article.publishedAt) }}</span>
              <span class="article-meta-sep">·</span>
              <span class="article-meta-text" :class="{ 'article-meta-text--read-history': article.lastReadAt }">{{ formatArticleReadMeta(article) }}</span>
            </div>

            <h2 class="article-row-title">{{ article.title }}</h2>
            <p class="article-row-summary">{{ article.summary }}</p>

            <div v-if="article.highlights.length" class="article-row-highlights">
              <span v-for="item in article.highlights" :key="item" class="article-highlight-tag">{{ item }}</span>
            </div>

            <div v-if="article.tags.length" class="article-row-tags">
              <span v-for="tag in article.tags" :key="tag" class="article-tag">{{ tag }}</span>
            </div>

            <div class="article-row-footer">
              <div class="article-author-row">
                <span
                  class="article-author-avatar"
                  :style="{ background: article.author.accent }"
                  aria-hidden="true"
                >
                  {{ article.author.initials }}
                </span>
                <div class="article-author-copy">
                  <strong>{{ article.author.name }}</strong>
                  <span>{{ article.author.role }}</span>
                </div>
              </div>

              <div class="article-row-stats">
                <span>{{ formatCount(article.readCount) }} 阅读</span>
                <span>{{ formatCount(article.likeCount) }} 赞</span>
                <span>{{ formatCount(article.collectCount) }} 藏</span>
                <span>{{ formatCount(article.commentCount) }} 评</span>
              </div>
            </div>
          </div>

          <div v-if="article.coverUrl" class="article-row-cover" :class="`category-${article.category}`">
            <img :src="article.coverUrl" :alt="article.title" loading="lazy" @error="e => e.target.src = '/peakstars-blog-icon.svg'" />
          </div>
        </article>
      </section>

      <section v-else class="article-empty-state">
        <strong>当前筛选下暂无文章</strong>
        <p>请切换浏览方式或分类后继续查看。</p>
      </section>

      <nav v-if="totalPages > 1" class="article-pagination" aria-label="文章分页导航">
        <button
          type="button"
          class="page-btn page-btn--prev"
          :disabled="currentPage <= 0"
          @click="goToPage(currentPage - 1)"
        >
          ← 上一页
        </button>

        <div class="page-numbers">
          <button
            v-for="pageNum in visiblePageNumbers"
            :key="pageNum"
            type="button"
            class="page-btn page-btn--num"
            :class="{ active: pageNum === currentPage }"
            :disabled="pageNum === currentPage"
            @click="goToPage(pageNum)"
          >
            {{ pageNum + 1 }}
          </button>
        </div>

        <button
          type="button"
          class="page-btn page-btn--next"
          :disabled="currentPage >= totalPages - 1"
          @click="goToPage(currentPage + 1)"
        >
          下一页 →
        </button>
      </nav>
    </main>

    <footer class="article-footer">
      <span class="article-footer-brand">Peak<span>Depth</span></span>
      <span class="article-footer-copy">© 2026 PeakDepth · 深度技术内容</span>
    </footer>
  </div>
</template>

<script setup>
import { computed, onActivated, onBeforeUnmount, onDeactivated, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { getTechArticles } from '@/api/content'
import { recommendedAuthors, techArticleCategories } from '@/data/techCategories'
import { useThemeStore } from '@/stores/theme'
import { scrollRestorationMap } from '@/stores/scrollRestoration'

defineOptions({ name: 'TechArticleList' })

const route = useRoute()
const router = useRouter()
const { isDark, toggleTheme } = useThemeStore()
const techArticles = ref([])
const totalArticles = ref(0)
const currentPage = ref(0)
const pageSize = ref(10)
const categoryCounts = ref({})
const scrollProgress = ref(0)
const articleListRestoreAnimating = ref(false)
const LOCAL_TECH_ARTICLE_READ_HISTORY_KEY = 'peakstars_tech_article_read_history'
const localReadHistory = ref(readLocalArticleReadHistory())
let articleListRestoreTimer = null

const pageSizeOptions = [5, 10, 20, 50]

const articleModes = [
  { key: 'recommend', label: '推荐', description: '优先展示精选与最近发布的文章。' },
  { key: 'collect', label: '收藏夹', description: '聚焦用户已经收藏过的内容。' },
  { key: 'vip', label: 'VIP', description: '专门查看深度长文和专题内容。' }
]

const sidebarCategories = techArticleCategories.filter((item) => item.key !== 'vip')
const topbarCategoryItems = sidebarCategories.filter((item) =>
  ['all', 'frontend', 'backend', 'project'].includes(item.key)
)

const activeModeKey = ref(resolveModeKey(route.query.mode))
const activeCategoryKey = ref(resolveCategoryKey(route.query.category))

const apiCategory = computed(() => {
  if (activeModeKey.value === 'vip') return 'vip'
  if (activeModeKey.value === 'collect') return 'collect'
  return activeCategoryKey.value
})

const totalPages = computed(() => {
  if (totalArticles.value <= 0) return 1
  return Math.ceil(totalArticles.value / pageSize.value)
})

const visiblePageNumbers = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  const maxVisible = 5
  let start = Math.max(0, current - Math.floor(maxVisible / 2))
  let end = start + maxVisible
  if (end > total) {
    end = total
    start = Math.max(0, end - maxVisible)
  }
  const pages = []
  for (let i = start; i < end; i++) {
    pages.push(i)
  }
  return pages
})

const normalizedArticles = computed(() =>
  techArticles.value.map((item, index) => normalizeArticle(item, index))
)

const filteredArticles = computed(() => normalizedArticles.value)

const activeModeInfo = computed(
  () => articleModes.find((item) => item.key === activeModeKey.value) || articleModes[0]
)

const activeCategoryInfo = computed(
  () => sidebarCategories.find((item) => item.key === activeCategoryKey.value) || sidebarCategories[0]
)

async function loadArticles() {
  try {
    const data = await getTechArticles({
      page: currentPage.value,
      pageSize: pageSize.value,
      category: apiCategory.value
    })
    techArticles.value = Array.isArray(data.list) ? data.list : []
    totalArticles.value = Number(data.total) || 0
    if (data.categoryCounts) {
      categoryCounts.value = data.categoryCounts
    }
  } catch {
    techArticles.value = []
    totalArticles.value = 0
  }
}

async function refreshArticlesSilently() {
  try {
    const data = await getTechArticles({
      page: currentPage.value,
      pageSize: pageSize.value,
      category: apiCategory.value,
      forceRefresh: true
    })
    if (Array.isArray(data.list) && data.list.length > 0) {
      techArticles.value = data.list
      totalArticles.value = Number(data.total) || 0
    }
  } catch {
    // noop
  }
}

function goToPage(pageNum) {
  const clamped = Math.max(0, Math.min(pageNum, totalPages.value - 1))
  if (clamped !== currentPage.value) {
    currentPage.value = clamped
    loadArticles()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function onPageSizeChange() {
  currentPage.value = 0
  loadArticles()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(async () => {
  document.body.classList.add('article-hub-page')
  await loadArticles()
  window.addEventListener('scroll', handleWindowScroll, { passive: true })
  updateScrollState()
})

onActivated(() => {
  document.body.classList.add('article-hub-page')
  window.addEventListener('scroll', handleWindowScroll, { passive: true })
  localReadHistory.value = readLocalArticleReadHistory()

  const currentPath = route.fullPath
  const saved = scrollRestorationMap.get(currentPath)
  if (saved) {
    if (articleListRestoreTimer) {
      window.clearTimeout(articleListRestoreTimer)
    }
    articleListRestoreAnimating.value = false
    requestAnimationFrame(() => {
      articleListRestoreAnimating.value = true
      window.scrollTo(saved.x, saved.y)
      articleListRestoreTimer = window.setTimeout(() => {
        articleListRestoreAnimating.value = false
        articleListRestoreTimer = null
      }, 560)
    })
  }

  refreshArticlesSilently()
})

onBeforeRouteLeave((to, from) => {
  scrollRestorationMap.set(from.fullPath, { x: window.scrollX, y: window.scrollY })
})

onBeforeUnmount(() => {
  document.body.classList.remove('article-hub-page')
  window.removeEventListener('scroll', handleWindowScroll)
  if (articleListRestoreTimer) {
    window.clearTimeout(articleListRestoreTimer)
    articleListRestoreTimer = null
  }
})

onDeactivated(() => {
  document.body.classList.remove('article-hub-page')
  window.removeEventListener('scroll', handleWindowScroll)
  articleListRestoreAnimating.value = false
  if (articleListRestoreTimer) {
    window.clearTimeout(articleListRestoreTimer)
    articleListRestoreTimer = null
  }
})

watch(
  () => route.query.mode,
  (nextMode) => {
    activeModeKey.value = resolveModeKey(nextMode)
  }
)

watch(
  () => route.query.category,
  (nextCategory) => {
    activeCategoryKey.value = resolveCategoryKey(nextCategory)
  }
)

watch([activeModeKey, activeCategoryKey], ([modeKey, categoryKey]) => {
  const query = {}

  if (modeKey !== 'recommend') {
    query.mode = modeKey
  }

  if (categoryKey !== 'all') {
    query.category = categoryKey
  }

  if (
    String(route.query.mode || 'recommend') !== modeKey ||
    String(route.query.category || 'all') !== categoryKey
  ) {
    router.replace({ path: '/articles', query })
  }
})

watch(filteredArticles, () => {
  updateScrollState()
})

function resolveModeKey(mode) {
  return articleModes.some((item) => item.key === mode) ? mode : 'recommend'
}

function resolveCategoryKey(category) {
  return sidebarCategories.some((item) => item.key === category) ? category : 'all'
}

function selectMode(modeKey) {
  activeModeKey.value = modeKey
  currentPage.value = 0
  loadArticles()
}

function selectCategory(categoryKey) {
  activeCategoryKey.value = categoryKey
  currentPage.value = 0
  loadArticles()
}

function countByCategory(categoryKey) {
  if (activeModeKey.value === 'vip') return categoryKey === 'all' ? (categoryCounts.value.vip || 0) : 0
  if (activeModeKey.value === 'collect') return categoryKey === 'all' ? (categoryCounts.value.collect || 0) : 0
  return Number(categoryCounts.value[categoryKey]) || 0
}

function countByMode(modeKey) {
  if (modeKey === 'vip') return Number(categoryCounts.value.vip) || 0
  if (modeKey === 'collect') return Number(categoryCounts.value.collect) || 0
  return Number(categoryCounts.value.all) || 0
}

function openArticleDetail(articleId) {
  router.push(`/articles/${articleId}`)
}

function goHome() {
  router.push('/home')
}

function updateScrollState() {
  const total = document.documentElement.scrollHeight - window.innerHeight
  scrollProgress.value = total > 0 ? Math.min((window.scrollY / total) * 100, 100) : 0
}

function handleWindowScroll() {
  updateScrollState()
}

function normalizeArticle(item, index) {
  const authorName = item.author?.name || item.authorName || 'PeakDepth'
  const authorMeta = recommendedAuthors.find((author) => author.name === authorName)
  const category = item.category || 'frontend'
  const highlights = Array.isArray(item.highlights) ? item.highlights.filter(Boolean) : []
  const tags = Array.isArray(item.tags) ? item.tags.filter(Boolean) : []
  const articleId = item.id || `tech-article-${index}`
  const localLastReadAt = localReadHistory.value[String(articleId)] || ''
  const lastReadAt = item.lastReadAt || localLastReadAt
  const lastReadAtTime = resolveTimeValue(lastReadAt)

  return {
    id: articleId,
    category,
    categoryLabel: item.categoryLabel || resolveCategoryLabel(category),
    title: item.title || '未命名文章',
    summary:
      item.essence ||
      item.summary ||
      item.subtitle ||
      '围绕真实工程问题拆开背景、判断依据与落地路径。',
    coverUrl: item.coverUrl || '',
    publishedAt: String(item.publishedAt || '').slice(0, 10),
    readCount: Number(item.readCount ?? item.viewCount ?? 0),
    likeCount: Number(item.likeCount || 0),
    collectCount: Number(item.collectCount || 0),
    commentCount: Number(item.commentCount || 0),
    readTime: item.readTime || '',
    lastReadAt,
    lastReadAtTime,
    featured: Boolean(item.featured),
    isVip: Boolean(item.isVip ?? item.vip),
    isCollected: Boolean(item.isCollected ?? item.collected),
    isLiked: Boolean(item.isLiked ?? item.liked),
    inHistory: Boolean(item.inHistory ?? item.history ?? lastReadAt),
    highlights,
    tags,
    author: {
      name: authorName,
      role:
        item.author?.role ||
        item.authorRole ||
        authorMeta?.subtitle ||
        '技术作者',
      initials:
        item.author?.initials ||
        item.authorInitials ||
        authorMeta?.initials ||
        buildInitials(authorName),
      accent:
        item.author?.accent ||
        item.authorAccent ||
        authorMeta?.accent ||
        resolveAccentByCategory(category)
    }
  }
}

function resolveCategoryLabel(category) {
  const labelMap = {
    frontend: '前端工程',
    backend: '后端架构',
    project: '项目业务解析',
    all: '全部文章',
    history: '历史',
    collect: '收藏',
    like: '喜欢'
  }
  return labelMap[category] || '技术文章'
}

function resolveAccentByCategory(category) {
  if (category === 'backend') return 'linear-gradient(135deg, #c84b2f, #0891b2)'
  if (category === 'project') return 'linear-gradient(135deg, #7c3aed, #22c55e)'
  return 'linear-gradient(135deg, #c84b2f, #f59e0b)'
}

function buildInitials(name) {
  const text = String(name || '').trim()
  if (!text) return 'PD'
  return text.length <= 2 ? text : text.slice(0, 2)
}

function formatCount(value) {
  const count = Number(value || 0)
  if (count >= 10000) return `${(count / 10000).toFixed(1).replace('.0', '')}w`
  if (count >= 1000) return `${(count / 1000).toFixed(1).replace('.0', '')}k`
  return `${count}`
}

function formatReadTime(value) {
  const minuteMatch = String(value || '').match(/(\d+)/)
  return minuteMatch ? `${minuteMatch[1]} 分钟阅读` : '1 分钟阅读'
}

function resolveTimeValue(value) {
  if (!value) {
    return 0
  }

  const date = new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? 0 : date.getTime()
}

function formatArticleReadMeta(article) {
  if (article.lastReadAt) {
    return formatHistoryTime(article.lastReadAt)
  }

  return formatReadTime(article.readTime)
}

function formatLongDate(value) {
  const match = String(value || '').match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!match) return value || '待发布'
  return `${match[1]}.${match[2]}.${match[3]}`
}

function formatHistoryTime(value) {
  const date = new Date(String(value || '').replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return ''

  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  if (diffMs >= 0 && diffMs < 60000) return '刚刚阅读'
  if (diffMs >= 0 && diffMs < 3600000) return `${Math.floor(diffMs / 60000)} 分钟前阅读`

  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const startOfTarget = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const diffDays = Math.round((startOfToday - startOfTarget) / 86400000)
  const timeText = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`

  if (diffDays === 0) return `今天 ${timeText} 阅读`
  if (diffDays === 1) return `昨天 ${timeText} 阅读`
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')} ${timeText}`
}

function readLocalArticleReadHistory() {
  try {
    const parsed = JSON.parse(localStorage.getItem(LOCAL_TECH_ARTICLE_READ_HISTORY_KEY) || '{}')
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}
</script>

<style scoped src="../styles/views/TechArticleList.css"></style>
