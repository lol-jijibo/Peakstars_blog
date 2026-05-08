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
      <!-- 页面标题区 -->
      <section class="article-page-header">
        <div class="article-page-header-copy">
          <span class="article-page-kicker">{{ activeCategoryInfo.caption }} · {{ activeModeInfo.label }}</span>
          <h1>技术文章</h1>
        </div>

        <!-- 浏览模式切换 -->
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

        <!-- 分类筛选 -->
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

      <!-- 文章流列表 -->
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
            <!-- 元信息行：分类 + 状态徽标 + 日期 + 阅读时长 -->
            <div class="article-row-meta">
              <span class="article-category-badge" :class="[`badge-${article.category}`]">{{ article.categoryLabel }}</span>
              <span v-if="article.featured" class="article-status-badge badge-featured">精选</span>
              <span v-if="article.isVip" class="article-status-badge badge-vip">VIP</span>
              <span v-if="article.isCollected" class="article-status-badge badge-collected">已收藏</span>
              <span v-if="article.isLiked" class="article-status-badge badge-liked">已喜欢</span>
              <span class="article-meta-sep">·</span>
              <span class="article-meta-text">{{ formatLongDate(article.publishedAt) }}</span>
              <span class="article-meta-sep">·</span>
              <span class="article-meta-text">{{ formatReadTime(article.readTime) }}</span>
            </div>

            <!-- 标题 -->
            <h2 class="article-row-title">{{ article.title }}</h2>

            <!-- 摘要 -->
            <p class="article-row-summary">{{ article.summary }}</p>

            <!-- 亮点 -->
            <div v-if="article.highlights.length" class="article-row-highlights">
              <span v-for="item in article.highlights" :key="item" class="article-highlight-tag">{{ item }}</span>
            </div>

            <!-- 标签 -->
            <div v-if="article.tags.length" class="article-row-tags">
              <span v-for="tag in article.tags" :key="tag" class="article-tag">{{ tag }}</span>
            </div>

            <!-- 底部：作者 + 数据 -->
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

          <!-- 封面图 -->
          <div v-if="article.coverUrl" class="article-row-cover" :class="`category-${article.category}`">
            <img :src="article.coverUrl" :alt="article.title" loading="lazy" />
          </div>
        </article>
      </section>

      <!-- 空状态 -->
      <section v-else class="article-empty-state">
        <strong>当前筛选下暂无文章</strong>
        <p>请切换浏览方式或分类后继续查看。</p>
      </section>
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

// keep-alive 通过组件 name 匹配缓存目标
defineOptions({ name: 'TechArticleList' })

const route = useRoute()
const router = useRouter()
const { isDark, toggleTheme } = useThemeStore()
const techArticles = ref([])
const scrollProgress = ref(0)
const articleListRestoreAnimating = ref(false)
let articleListRestoreTimer = null

/**
 * 浏览模式：推荐 / 收藏夹 / VIP
 */
const articleModes = [
  { key: 'recommend', label: '推荐', description: '优先展示精选与最近发布的文章。' },
  { key: 'collect', label: '收藏夹', description: '聚焦用户已经收藏过的内容。' },
  { key: 'vip', label: 'VIP', description: '专门查看深度长文和专题内容。' }
]

const sidebarCategories = techArticleCategories.filter((item) => item.key !== 'vip')
const topbarCategoryItems = sidebarCategories.filter((item) =>
  ['all', 'frontend', 'backend'].includes(item.key)
)

const activeModeKey = ref(resolveModeKey(route.query.mode))
const activeCategoryKey = ref(resolveCategoryKey(route.query.category))

const normalizedArticles = computed(() =>
  techArticles.value.map((item, index) => normalizeArticle(item, index))
)

const filteredArticles = computed(() => {
  const baseList = normalizedArticles.value.filter((article) =>
    matchesMode(article, activeModeKey.value)
  )
  const categoryList = baseList.filter((article) =>
    matchesCategory(article, activeCategoryKey.value)
  )

  return [...categoryList].sort((left, right) => {
    if (Number(right.featured) !== Number(left.featured)) {
      return Number(right.featured) - Number(left.featured)
    }

    return String(right.publishedAt).localeCompare(String(left.publishedAt))
  })
})

const activeModeInfo = computed(
  () => articleModes.find((item) => item.key === activeModeKey.value) || articleModes[0]
)

const activeCategoryInfo = computed(
  () => sidebarCategories.find((item) => item.key === activeCategoryKey.value) || sidebarCategories[0]
)

async function loadArticles() {
  try {
    const list = await getTechArticles()
    techArticles.value = Array.isArray(list) ? list : []
  } catch {
    techArticles.value = []
  }
}

/** 静默刷新文章列表，不阻塞 UI，数据回来后无缝替换 */
async function refreshArticlesSilently() {
  try {
    const list = await getTechArticles()
    if (Array.isArray(list) && list.length > 0) {
      techArticles.value = list
    }
  } catch {
    // 静默刷新失败不影响当前展示
  }
}

onMounted(async () => {
  document.body.classList.add('article-hub-page')
  await loadArticles()
  window.addEventListener('scroll', handleWindowScroll, { passive: true })
  updateScrollState()
})

/**
 * 从 keep-alive 缓存恢复时：
 * - 如果已有数据，不再重新请求（避免白屏闪烁）
 * - 恢复滚动位置
 * - 后台静默刷新数据，确保阅读数/评论数等状态最新
 */
onActivated(() => {
  document.body.classList.add('article-hub-page')
  window.addEventListener('scroll', handleWindowScroll, { passive: true })

  // 恢复滚动位置（优先从 scrollRestorationMap，回退到 router savedPosition）
  const currentPath = route.fullPath
  const saved = scrollRestorationMap.get(currentPath)
  if (saved) {
    if (articleListRestoreTimer) {
      window.clearTimeout(articleListRestoreTimer)
    }
    articleListRestoreAnimating.value = false
    requestAnimationFrame(() => {
      /**
       * 业务目的: 技术文章列表从详情页返回时补一层和详情页一致的柔和入场反馈。
       * 业务逻辑: 在恢复列表滚动位置前重新触发淡入上浮动画，避免 keep-alive 直接复用造成一闪一现。
       */
      articleListRestoreAnimating.value = true
      window.scrollTo(saved.x, saved.y)
      articleListRestoreTimer = window.setTimeout(() => {
        articleListRestoreAnimating.value = false
        articleListRestoreTimer = null
      }, 560)
    })
  }

  // 数据为空时同步加载；已有数据时后台静默刷新，不阻塞渲染
  if (techArticles.value.length === 0) {
    loadArticles()
  } else {
    // 静默刷新：不阻塞 UI，数据回来后无缝替换
    refreshArticlesSilently()
  }
})

/**
 * 业务目的: 在离开技术文章列表时精准记住用户点击文章前的浏览位置。
 * 业务逻辑: 使用列表页自身的 fullPath 作为键保存滚动坐标，避免切到详情页后被新路由覆盖。
 */
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

/** 被 keep-alive 缓存挂起时，清理全局副作用 */
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

function matchesMode(article, modeKey) {
  if (modeKey === 'vip') return article.isVip
  if (modeKey === 'collect') return article.isCollected
  return true
}

function matchesCategory(article, categoryKey) {
  if (categoryKey === 'all') return true
  if (categoryKey === 'history') return article.inHistory
  if (categoryKey === 'collect') return article.isCollected
  if (categoryKey === 'like') return article.isLiked
  return article.category === categoryKey
}

function selectMode(modeKey) {
  activeModeKey.value = modeKey
}

function selectCategory(categoryKey) {
  activeCategoryKey.value = categoryKey
}

function countByCategory(categoryKey) {
  return normalizedArticles.value.filter((article) => {
    return matchesMode(article, activeModeKey.value) && matchesCategory(article, categoryKey)
  }).length
}

function countByMode(modeKey) {
  return normalizedArticles.value.filter((article) => matchesMode(article, modeKey)).length
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

  return {
    id: item.id || `tech-article-${index}`,
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
    readTime: item.readTime || '6 min',
    featured: Boolean(item.featured),
    isVip: Boolean(item.isVip ?? item.vip),
    isCollected: Boolean(item.isCollected ?? item.collected),
    isLiked: Boolean(item.isLiked ?? item.liked),
    inHistory: Boolean(item.inHistory ?? item.history),
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
    frontend: '前端',
    backend: '后端',
    all: '全部文章',
    history: '历史',
    collect: '收藏',
    like: '喜欢'
  }
  return labelMap[category] || '技术文章'
}

function resolveAccentByCategory(category) {
  if (category === 'backend') return 'linear-gradient(135deg, #c84b2f, #0891b2)'
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
  return minuteMatch ? `${minuteMatch[1]} 分钟阅读` : '6 分钟阅读'
}

function formatLongDate(value) {
  const match = String(value || '').match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!match) return value || '待发布'
  return `${match[1]}.${match[2]}.${match[3]}`
}
</script>

<style scoped src="../styles/views/TechArticleList.css"></style>
