<template>
  <div class="article-hub-page">
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

      <button class="article-topbar-theme" type="button" :title="isDark ? '切换到浅色' : '切换到深色'" @click="toggleTheme">
        <span class="theme-icon">{{ isDark ? '☀' : '☾' }}</span>
      </button>
      <button class="article-topbar-action" type="button" @click="goHome">返回首页 →</button>
    </header>

    <main class="article-hub-main">
      <section class="article-hub-hero">
        <div class="article-hub-hero-copy">
          <span class="article-hub-kicker">{{ activeCategoryInfo.caption }} · {{ activeModeInfo.label }}</span>
          <h1>PeakDepth 技术文章</h1>
          <p>{{ activeCategoryDescription }}</p>

          <div class="article-hub-hero-stats">
            <div v-for="item in overviewStats" :key="item.label" class="article-hub-stat">
              <strong>{{ item.value }}</strong>
              <span>{{ item.label }}</span>
            </div>
          </div>
        </div>

        <div class="article-hub-hero-panel">
          <span class="article-hub-hero-panel-label">浏览方式</span>
          <div class="article-mode-switch">
            <button
              v-for="mode in articleModes"
              :key="mode.key"
              class="article-mode-switch-item"
              :class="{ active: activeModeKey === mode.key }"
              type="button"
              @click="selectMode(mode.key)"
            >
              <strong>{{ mode.label }}</strong>
              <span>{{ countByMode(mode.key) }} 篇</span>
            </button>
          </div>
        </div>
      </section>

      <div v-if="filteredArticles.length" class="article-hub-shell">
        <div class="article-hub-main-column">
          <article
            v-if="heroArticle"
            class="article-feature-card"
            tabindex="0"
            role="link"
            @click="openArticleDetail(heroArticle.id)"
            @keydown.enter="openArticleDetail(heroArticle.id)"
          >
            <div class="article-feature-copy">
              <div class="article-feature-meta">
                <span class="article-category-badge">{{ heroArticle.categoryLabel }}</span>
                <span class="article-feature-series">{{ formatHeroDate(heroArticle.publishedAt) }}</span>
              </div>

              <h2 class="article-feature-title">{{ heroArticle.title }}</h2>
              <p class="article-feature-summary">{{ heroArticle.summary }}</p>

              <div class="article-feature-highlights">
                <span v-for="item in heroArticle.highlights.slice(0, 3)" :key="item">{{ item }}</span>
              </div>

              <div class="article-feature-footer">
                <div class="article-author-row">
                  <span
                    class="article-author-avatar"
                    :style="{ background: heroArticle.author.accent }"
                    aria-hidden="true"
                  >
                    {{ heroArticle.author.initials }}
                  </span>
                  <div class="article-author-copy">
                    <strong>{{ heroArticle.author.name }}</strong>
                    <p>{{ heroArticle.author.role }} · {{ formatReadTime(heroArticle.readTime) }}</p>
                  </div>
                </div>

                <div class="article-feature-stats">
                  <span>阅读 {{ formatCount(heroArticle.readCount) }}</span>
                  <span>点赞 {{ formatCount(heroArticle.likeCount) }}</span>
                  <span>评论 {{ formatCount(heroArticle.commentCount) }}</span>
                </div>
              </div>
            </div>

            <div class="article-feature-visual" :class="`category-${heroArticle.category}`">
              <img
                v-if="heroArticle.coverUrl"
                :src="heroArticle.coverUrl"
                :alt="heroArticle.title"
                loading="lazy"
              />
              <div v-else class="article-cover-placeholder">
                <small>{{ heroArticle.categoryLabel }}</small>
                <strong>{{ heroArticle.author.name }}</strong>
              </div>
            </div>
          </article>

          <section v-if="secondaryArticles.length" class="article-sub-grid">
            <article
              v-for="article in secondaryArticles"
              :key="article.id"
              class="article-sub-card"
              tabindex="0"
              role="link"
              @click="openArticleDetail(article.id)"
              @keydown.enter="openArticleDetail(article.id)"
            >
              <span class="article-sub-kicker">
                {{ article.categoryLabel }} · {{ formatReadTime(article.readTime) }}
              </span>
              <h3>{{ article.title }}</h3>
              <p>{{ article.summary }}</p>
              <div class="article-sub-footer">
                <span>{{ article.author.name }}</span>
                <span>{{ formatCount(article.readCount) }} 阅读</span>
              </div>
            </article>
          </section>

          <section v-if="streamArticles.length" class="article-stream-section">
            <div class="article-section-head">
              <div>
                <span class="article-section-kicker">Reading Stream</span>
                <h2>继续阅读</h2>
              </div>
              <span class="article-section-note">{{ filteredArticles.length }} 篇内容</span>
            </div>

            <div class="article-stream-list">
              <article
                v-for="(article, index) in streamArticles"
                :key="article.id"
                class="article-stream-item"
                tabindex="0"
                role="link"
                @click="openArticleDetail(article.id)"
                @keydown.enter="openArticleDetail(article.id)"
              >
                <span class="article-stream-index">{{ String(index + 1).padStart(2, '0') }}</span>

                <div class="article-stream-main">
                  <div class="article-stream-eyebrow">
                    <span class="article-stream-tag">{{ article.categoryLabel }}</span>
                    <span>{{ formatLongDate(article.publishedAt) }}</span>
                    <span>{{ formatReadTime(article.readTime) }}</span>
                  </div>

                  <h3>{{ article.title }}</h3>
                  <p>{{ article.summary }}</p>

                  <div class="article-stream-footer">
                    <div class="article-author-row">
                      <span
                        class="article-author-avatar article-author-avatar--small"
                        :style="{ background: article.author.accent }"
                        aria-hidden="true"
                      >
                        {{ article.author.initials }}
                      </span>
                      <div class="article-author-copy">
                        <strong>{{ article.author.name }}</strong>
                        <p>{{ article.author.role }}</p>
                      </div>
                    </div>

                    <div class="article-stream-stats">
                      <span>{{ formatCount(article.readCount) }} 阅读</span>
                      <span>{{ formatCount(article.collectCount) }} 收藏</span>
                    </div>
                  </div>
                </div>

                <div class="article-stream-visual" :class="`category-${article.category}`">
                  <img
                    v-if="article.coverUrl"
                    :src="article.coverUrl"
                    :alt="article.title"
                    loading="lazy"
                  />
                  <div v-else class="article-cover-placeholder">
                    <small>{{ article.categoryLabel }}</small>
                    <strong>{{ article.author.initials }}</strong>
                  </div>
                </div>
              </article>
            </div>
          </section>
        </div>

        <aside class="article-hub-sidebar">
          <section class="article-sidebar-card">
            <div class="article-sidebar-head">
              <h2>文章分类</h2>
              <p>保留简单清晰的分类入口，后端新增文章后会自动归到对应分组。</p>
            </div>

            <div class="article-sidebar-list">
              <button
                v-for="category in sidebarCategories"
                :key="category.key"
                class="article-sidebar-item"
                :class="{ active: activeCategoryKey === category.key }"
                type="button"
                @click="selectCategory(category.key)"
              >
                <div>
                  <strong>{{ category.label }}</strong>
                  <small>{{ category.description }}</small>
                </div>
                <span>{{ countByCategory(category.key) }}</span>
              </button>
            </div>
          </section>

          <section v-if="hotArticles.length" class="article-sidebar-card">
            <div class="article-sidebar-head">
              <h2>本页热读</h2>
              <p>直接按当前筛选结果里的阅读热度排序。</p>
            </div>

            <button
              v-for="(article, index) in hotArticles"
              :key="article.id"
              class="article-sidebar-rank"
              type="button"
              @click="openArticleDetail(article.id)"
            >
              <span class="article-sidebar-rank-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="article-sidebar-rank-copy">
                <strong>{{ article.title }}</strong>
                <small>{{ formatCount(article.readCount) }} 阅读 · {{ article.author.name }}</small>
              </span>
            </button>
          </section>

          <section v-if="topAuthors.length" class="article-sidebar-card">
            <div class="article-sidebar-head">
              <h2>活跃作者</h2>
              <p>根据当前列表里的文章数量和阅读量自动汇总。</p>
            </div>

            <div class="article-author-list">
              <article v-for="author in topAuthors" :key="author.name" class="article-author-item">
                <div class="article-author-item-main">
                  <span
                    class="article-author-avatar article-author-avatar--recommend"
                    :style="{ background: author.accent }"
                    aria-hidden="true"
                  >
                    {{ author.initials }}
                  </span>
                  <div class="article-author-item-copy">
                    <strong>{{ author.name }}</strong>
                    <p>{{ author.role }}</p>
                  </div>
                </div>
                <span class="article-author-item-count">{{ author.articleCount }} 篇</span>
              </article>
            </div>
          </section>

          <section v-if="topicTagCloud.length" class="article-sidebar-card">
            <div class="article-sidebar-head">
              <h2>高频主题</h2>
              <p>从文章亮点里提炼，不依赖额外后台配置。</p>
            </div>

            <div class="article-tag-cloud">
              <span v-for="tag in topicTagCloud" :key="tag.name" class="article-tag-cloud-item">
                {{ tag.name }}
                <small>{{ tag.count }}</small>
              </span>
            </div>
          </section>
        </aside>
      </div>

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
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTechArticles } from '@/api/content'
import { recommendedAuthors, techArticleCategories } from '@/data/techCategories'
import { useThemeStore } from '@/stores/theme'

const route = useRoute()
const router = useRouter()
const { isDark, toggleTheme } = useThemeStore()
const techArticles = ref([])
const scrollProgress = ref(0)

/**
 * 目的：统一技术文章页的浏览模式入口。
 * 逻辑：通过推荐、收藏夹、VIP 三种模式切换当前页面展示的文章集合。
 */
const articleModes = [
  { key: 'recommend', label: '推荐', description: '优先展示精选与最近发布的文章。' },
  { key: 'collect', label: '收藏夹', description: '聚焦用户已经收藏过的内容。' },
  { key: 'vip', label: 'VIP', description: '专门查看深度长文和专题内容。' }
]

/**
 * 目的：沿用已有文章分类定义，让列表页和后台分类口径保持一致。
 * 逻辑：当前列表页单独保留浏览侧需要的分类项，VIP 入口由浏览模式承接。
 */
const sidebarCategories = techArticleCategories.filter((item) => item.key !== 'vip')
const topbarCategoryItems = sidebarCategories.filter((item) =>
  ['all', 'frontend', 'backend'].includes(item.key)
)

const activeModeKey = ref(resolveModeKey(route.query.mode))
const activeCategoryKey = ref(resolveCategoryKey(route.query.category))

/**
 * 目的：把本地演示文章和后台文章压成同一份前端展示结构。
 * 逻辑：优先读取后端字段，没有时回退到本地字段和默认值，保证页面每个区块都能稳定渲染。
 */
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

const activeCategoryDescription = computed(() => {
  if (activeModeKey.value === 'collect') {
    return '保持与 PeakDepth 详情页一致的阅读语气，把收藏内容按同一版式重新组织到一页。'
  }

  if (activeModeKey.value === 'vip') {
    return '这里集中展示更适合沉浸阅读的深度专题，结构和文章详情页保持同一套视觉秩序。'
  }

  return activeCategoryInfo.value.description
})

const heroArticle = computed(() => filteredArticles.value[0] || null)
const secondaryArticles = computed(() => filteredArticles.value.slice(1, 4))
const streamArticles = computed(() => filteredArticles.value.slice(filteredArticles.value.length > 4 ? 4 : 1))

/**
 * 目的：把列表页顶部指标保持在文章数据内部闭环，避免再引入额外后台统计接口。
 * 逻辑：当前筛选结果直接汇总文章数、精选数和阅读量，后台新增文章后指标会自动更新。
 */
const overviewStats = computed(() => {
  const readTotal = filteredArticles.value.reduce((sum, item) => sum + Number(item.readCount || 0), 0)
  const featuredTotal = filteredArticles.value.filter((item) => item.featured).length
  const authorTotal = new Set(filteredArticles.value.map((item) => item.author.name)).size

  return [
    { label: '当前文章', value: `${filteredArticles.value.length}` },
    { label: '精选专题', value: `${featuredTotal}` },
    { label: '作者数量', value: `${authorTotal}` },
    { label: '累计阅读', value: formatCount(readTotal) }
  ]
})

/**
 * 目的：让右侧热读区和页面主列表保持同源数据。
 * 逻辑：直接用当前筛选结果按阅读量排序，避免手工维护另一套推荐列表。
 */
const hotArticles = computed(() =>
  [...filteredArticles.value]
    .sort((left, right) => right.readCount - left.readCount)
    .slice(0, 4)
)

/**
 * 目的：根据当前页面文章自动汇总作者榜，让后台只维护文章本身即可。
 * 逻辑：按作者名聚合文章数量和阅读量，再补齐头像色与角色文案。
 */
const topAuthors = computed(() => {
  const authorMap = new Map()

  filteredArticles.value.forEach((article) => {
    const current = authorMap.get(article.author.name) || {
      name: article.author.name,
      role: article.author.role,
      initials: article.author.initials,
      accent: article.author.accent,
      articleCount: 0,
      readCount: 0
    }

    current.articleCount += 1
    current.readCount += Number(article.readCount || 0)
    authorMap.set(article.author.name, current)
  })

  return [...authorMap.values()]
    .sort((left, right) => {
      if (right.articleCount !== left.articleCount) {
        return right.articleCount - left.articleCount
      }

      return right.readCount - left.readCount
    })
    .slice(0, 4)
})

/**
 * 目的：从文章亮点和标签里生成侧边主题云，减少额外配置成本。
 * 逻辑：优先吃文章自带 tags 和 highlights，没有时至少保留分类标签做主题兜底。
 */
const topicTagCloud = computed(() => {
  const tagMap = new Map()

  filteredArticles.value.forEach((article) => {
    const tags = [...article.tags, ...article.highlights, article.categoryLabel].filter(Boolean)
    tags.forEach((tag) => {
      tagMap.set(tag, (tagMap.get(tag) || 0) + 1)
    })
  })

  return [...tagMap.entries()]
    .map(([name, count]) => ({ name, count }))
    .sort((left, right) => right.count - left.count)
    .slice(0, 10)
})

/**
 * 目的：技术文章页优先接入后端文章列表，同时保留本地文章兜底。
 * 逻辑：接口成功就直接使用后台内容，失败时退回演示数据，保证版式和交互仍可验证。
 */
async function loadArticles() {
  try {
    const list = await getTechArticles()
    techArticles.value = Array.isArray(list) ? list : []
  } catch {
    techArticles.value = []
  }
}

onMounted(async () => {
  document.body.classList.add('article-hub-page')
  await loadArticles()
  window.addEventListener('scroll', handleWindowScroll, { passive: true })
  updateScrollState()
})

onBeforeUnmount(() => {
  document.body.classList.remove('article-hub-page')
  window.removeEventListener('scroll', handleWindowScroll)
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
  if (modeKey === 'vip') {
    return article.isVip
  }

  if (modeKey === 'collect') {
    return article.isCollected
  }

  return true
}

function matchesCategory(article, categoryKey) {
  if (categoryKey === 'all') {
    return true
  }

  if (categoryKey === 'history') {
    return article.inHistory
  }

  if (categoryKey === 'collect') {
    return article.isCollected
  }

  if (categoryKey === 'like') {
    return article.isLiked
  }

  return article.category === categoryKey
}

function selectMode(modeKey) {
  activeModeKey.value = modeKey
}

function selectCategory(categoryKey) {
  activeCategoryKey.value = categoryKey
}

/**
 * 目的：让分类计数始终基于当前浏览模式，避免侧栏信息与主列表脱节。
 * 逻辑：先按模式过滤，再按分类计算数量，保证用户切换时看到的数字是真实结果。
 */
function countByCategory(categoryKey) {
  return normalizedArticles.value.filter((article) => {
    return matchesMode(article, activeModeKey.value) && matchesCategory(article, categoryKey)
  }).length
}

/**
 * 目的：让模式切换按钮也能直接展示内容规模。
 * 逻辑：每个模式都用同一份文章源即时统计数量，不再维护独立的模式配置数据。
 */
function countByMode(modeKey) {
  return normalizedArticles.value.filter((article) => matchesMode(article, modeKey)).length
}

/**
 * 目的：统一文章跳转入口，保持列表各区块交互一致。
 * 逻辑：无论从主推、次级推荐还是流式列表点击，最终都进入对应文章详情页。
 */
function openArticleDetail(articleId) {
  router.push(`/articles/${articleId}`)
}

function goHome() {
  router.push('/home')
}

/**
 * 目的：延续详情页的阅读进度提示，让长列表浏览也有明确反馈。
 * 逻辑：根据窗口滚动位置计算百分比，并把结果映射到页面顶部细进度条。
 */
function updateScrollState() {
  const total = document.documentElement.scrollHeight - window.innerHeight
  scrollProgress.value = total > 0 ? Math.min((window.scrollY / total) * 100, 100) : 0
}

function handleWindowScroll() {
  updateScrollState()
}

/**
 * 目的：兜住后端和本地文章字段差异，保证列表布局不会因为字段缺失而断裂。
 * 逻辑：作者信息、阅读指标、精选状态和亮点字段都在这里一次性归一化。
 */
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
  if (category === 'backend') {
    return 'linear-gradient(135deg, #c84b2f, #0891b2)'
  }

  return 'linear-gradient(135deg, #c84b2f, #f59e0b)'
}

function buildInitials(name) {
  const text = String(name || '').trim()
  if (!text) {
    return 'PD'
  }

  return text.length <= 2 ? text : text.slice(0, 2)
}

function formatCount(value) {
  const count = Number(value || 0)

  if (count >= 10000) {
    return `${(count / 10000).toFixed(1).replace('.0', '')}w`
  }

  if (count >= 1000) {
    return `${(count / 1000).toFixed(1).replace('.0', '')}k`
  }

  return `${count}`
}

function formatReadTime(value) {
  const minuteMatch = String(value || '').match(/(\d+)/)
  return minuteMatch ? `${minuteMatch[1]} 分钟阅读` : '6 分钟阅读'
}

function formatLongDate(value) {
  const match = String(value || '').match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!match) {
    return value || '待发布'
  }

  return `${match[1]}.${match[2]}.${match[3]}`
}

function formatHeroDate(value) {
  const match = String(value || '').match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!match) {
    return value || 'PeakDepth'
  }

  return `${match[2]} 月 ${match[3]} 日`
}
</script>

<style scoped src="../styles/views/TechArticleList.css"></style>
