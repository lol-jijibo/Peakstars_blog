<template>
  <div class="article-page">
    <div class="article-progress-bar" :style="{ width: `${scrollProgress}%` }"></div>

    <header class="article-topbar">
      <button class="article-topbar-brand" type="button" @click="goHome">
        Peak<span>Depth</span>
      </button>

      <nav class="article-topbar-nav" aria-label="文章导航">
        <button type="button" @click="goArticleList">文章</button>
        <button type="button" @click="scrollToSeries">系列</button>
        <button type="button" @click="openCategory('frontend')">前端</button>
        <button type="button" @click="openCategory('backend')">后端</button>
      </nav>

      <button class="article-topbar-action" type="button" @click="goArticleList">返回列表 →</button>
    </header>

    <div class="article-shell">
      <main class="article-main">
        <header class="article-header">
          <div class="article-meta-top">
            <span class="article-category-badge">{{ articleCategoryLabel }}</span>
            <span class="article-series">{{ articleSeries }}</span>
          </div>

          <h1 class="article-title">{{ currentArticle.title }}</h1>

          <p class="article-subtitle">{{ articleSubtitle }}</p>

          <div class="article-byline">
            <div class="article-author-avatar">{{ authorAvatarText }}</div>
            <div class="article-byline-text">
              <span class="article-author-name">{{ author.name }}</span>
              <span class="article-date-read">{{ displayPublishedAt }} · {{ displayReadTime }}</span>
            </div>

            <div class="article-stats">
              <div class="article-stat">
                <span class="article-stat-num">{{ displayReadCount }}</span>
                <span class="article-stat-label">阅读</span>
              </div>
              <div class="article-stat">
                <span class="article-stat-num">{{ displayLikeCount }}</span>
                <span class="article-stat-label">点赞</span>
              </div>
              <div class="article-stat">
                <span class="article-stat-num">{{ displayCommentCount }}</span>
                <span class="article-stat-label">评论</span>
              </div>
            </div>
          </div>
        </header>

        <article ref="articleBodyRef" class="article-body" v-html="articleHtml"></article>

        <div class="article-tags">
          <span v-for="tag in articleTags" :key="tag" class="article-tag"># {{ tag }}</span>
        </div>

        <section class="article-author-card">
          <div class="article-author-card-header">
            <div class="article-author-avatar-large">{{ authorAvatarText }}</div>
            <div>
              <div class="article-author-card-name">{{ author.name }}</div>
              <div class="article-author-card-role">{{ author.role }}</div>
            </div>
          </div>

          <p>{{ authorIntro }}</p>

          <button class="article-author-follow" type="button">关注作者</button>
        </section>
      </main>

      <aside class="article-sidebar">
        <section v-if="outlineItems.length" class="article-sidebar-section">
          <h2 class="article-sidebar-title">目录</h2>
          <ul class="article-toc-list">
            <li
              v-for="item in outlineItems"
              :key="item.id"
              class="article-toc-item"
              :class="{
                active: activeOutlineId === item.id,
                sub: item.level === 'h3'
              }"
            >
              <button type="button" @click="scrollToHeading(item.id)">{{ item.text }}</button>
            </li>
          </ul>
        </section>

        <section ref="seriesSectionRef" class="article-sidebar-section">
          <h2 class="article-sidebar-title">系列相关</h2>
          <button
            v-for="(item, index) in relatedArticles"
            :key="item.id"
            class="article-related-item"
            type="button"
            @click="openArticle(item.id)"
          >
            <span class="article-related-num">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="article-related-info">
              <span class="article-related-title">{{ item.title }}</span>
              <span class="article-related-meta">{{ item.meta }}</span>
            </span>
          </button>
        </section>

        <section class="article-sidebar-section article-sidebar-share">
          <h2 class="article-sidebar-title">分享</h2>
          <div class="article-share-list">
            <button class="article-share-button" type="button" @click="shareToX">
              <span class="article-share-icon article-share-icon--x">𝕏</span>
              分享到 X
            </button>
            <button class="article-share-button" type="button" @click="shareToLinkedIn">
              <span class="article-share-icon article-share-icon--in">in</span>
              分享到 LinkedIn
            </button>
            <button class="article-share-button" type="button" @click="copyLink">
              <span class="article-share-icon article-share-icon--copy">⎘</span>
              {{ copiedText }}
            </button>
          </div>
        </section>
      </aside>
    </div>

    <footer class="article-footer">
      <span class="article-footer-brand">Peak<span>Depth</span></span>
      <span class="article-footer-copy">© 2026 PeakDepth · 深度技术内容</span>
    </footer>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTechArticles } from '@/api/content'
import { techArticles as localTechArticles } from '@/data/techArticles'

const route = useRoute()
const router = useRouter()

const techArticles = ref([])
const article = ref(null)
const articleBodyRef = ref(null)
const seriesSectionRef = ref(null)
const outlineItems = ref([])
const activeOutlineId = ref('')
const scrollProgress = ref(0)
const copiedText = ref('复制链接')

const defaultArticle = localTechArticles.find((item) => item.id === 'rust-ownership-system') || localTechArticles[0]

/**
 * 目的：统一详情页文章来源，兼容后台接口和前端内置参考文章。
 * 逻辑：优先读取接口合并结果，找不到时回退到内置文章，保证参考版式始终可见。
 */
async function loadArticle(articleId) {
  try {
    const list = await getTechArticles()
    techArticles.value = Array.isArray(list) && list.length ? list : localTechArticles
  } catch {
    techArticles.value = localTechArticles
  }

  article.value =
    techArticles.value.find((item) => String(item.id) === String(articleId)) || defaultArticle

  await nextTick()
  syncOutline()
  updateScrollState()
}

const currentArticle = computed(() => article.value || defaultArticle)

const author = computed(() => ({
  name: currentArticle.value.author?.name || currentArticle.value.authorName || 'PeakDepth',
  role: currentArticle.value.author?.role || currentArticle.value.authorRole || '技术作者'
}))

const articleCategoryLabel = computed(
  () => currentArticle.value.categoryLabel || resolveCategoryLabel(currentArticle.value.category)
)

const articleSeries = computed(() => {
  if (currentArticle.value.series) {
    return currentArticle.value.series
  }

  return `${articleCategoryLabel.value}专题 · ${currentArticle.value.isVip ? '深度解读' : '工程实践'}`
})

const articleSubtitle = computed(() => {
  return (
    currentArticle.value.subtitle ||
    currentArticle.value.summary ||
    currentArticle.value.essence ||
    '围绕真实工程问题拆开背景、取舍与落地过程。'
  )
})

const articleHtml = computed(() => {
  if (currentArticle.value.contentHtml) {
    return currentArticle.value.contentHtml
  }

  return buildArticleHtml(currentArticle.value)
})

const articleTags = computed(() => {
  if (Array.isArray(currentArticle.value.tags) && currentArticle.value.tags.length) {
    return currentArticle.value.tags
  }

  const fallbackTags = [
    articleCategoryLabel.value,
    ...(currentArticle.value.highlights || [])
  ].filter(Boolean)

  return [...new Set(fallbackTags)].slice(0, 6)
})

const authorAvatarText = computed(() => {
  return currentArticle.value.author?.initials || buildInitials(author.value.name)
})

const authorIntro = computed(() => {
  return (
    currentArticle.value.authorIntro ||
    `${author.value.name} 长期关注 ${articleCategoryLabel.value} 场景，持续记录真实项目里的判断依据与工程取舍。`
  )
})

const displayPublishedAt = computed(() => formatLongDate(currentArticle.value.publishedAt))
const displayReadTime = computed(() => formatReadTime(currentArticle.value.readTime))
const displayReadCount = computed(() => formatCompactCount(currentArticle.value.readCount || 0))
const displayLikeCount = computed(() => formatCompactCount(currentArticle.value.likeCount || 0))
const displayCommentCount = computed(() => formatCompactCount(currentArticle.value.commentCount || 0))

const relatedArticles = computed(() => {
  const currentId = String(currentArticle.value.id)
  const category = currentArticle.value.category

  return techArticles.value
    .filter((item) => String(item.id) !== currentId)
    .sort((left, right) => {
      const leftScore = Number(left.category === category) + Number(left.featured)
      const rightScore = Number(right.category === category) + Number(right.featured)
      return rightScore - leftScore
    })
    .slice(0, 3)
    .map((item) => ({
      id: item.id,
      title: item.title,
      meta: `${formatReadTime(item.readTime)} · ${formatCompactCount(item.readCount || 0)} 阅读`
    }))
})

watch(
  () => route.params.id,
  (articleId) => {
    loadArticle(articleId)
  },
  { immediate: true }
)

watch(articleHtml, async () => {
  await nextTick()
  syncOutline()
})

onMounted(() => {
  window.addEventListener('scroll', handleWindowScroll, { passive: true })
  updateScrollState()
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleWindowScroll)
})

/**
 * 目的：根据文章正文标题生成右侧目录。
 * 逻辑：扫描正文中的 h2 与 h3，写入稳定 id 后同步用于目录跳转与滚动高亮。
 */
function syncOutline() {
  const root = articleBodyRef.value
  if (!root) {
    outlineItems.value = []
    activeOutlineId.value = ''
    return
  }

  const headings = [...root.querySelectorAll('h2, h3')]
  outlineItems.value = headings.map((heading, index) => {
    const id = `article-outline-${index}`
    heading.id = id

    return {
      id,
      text: heading.textContent?.trim() || `章节 ${index + 1}`,
      level: heading.tagName.toLowerCase()
    }
  })
}

/**
 * 目的：驱动顶部阅读进度条与目录激活状态。
 * 逻辑：根据滚动位置计算正文阅读进度，并选择最靠近视口顶部的目录项作为当前章节。
 */
function updateScrollState() {
  const total = document.documentElement.scrollHeight - window.innerHeight
  scrollProgress.value = total > 0 ? Math.min((window.scrollY / total) * 100, 100) : 0

  let activeId = outlineItems.value[0]?.id || ''
  outlineItems.value.forEach((item) => {
    const target = document.getElementById(item.id)
    if (target && target.getBoundingClientRect().top <= 140) {
      activeId = item.id
    }
  })

  activeOutlineId.value = activeId
}

function handleWindowScroll() {
  updateScrollState()
}

function goHome() {
  router.push('/home')
}

function goArticleList() {
  router.push('/articles')
}

function openCategory(category) {
  router.push({ path: '/articles', query: { category } })
}

function openArticle(articleId) {
  router.push(`/articles/${articleId}`)
}

function scrollToSeries() {
  seriesSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function scrollToHeading(id) {
  const target = document.getElementById(id)
  if (!target) {
    return
  }

  target.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    copiedText.value = '链接已复制'
    window.setTimeout(() => {
      copiedText.value = '复制链接'
    }, 1600)
  } catch {
    copiedText.value = '复制失败'
    window.setTimeout(() => {
      copiedText.value = '复制链接'
    }, 1600)
  }
}

function shareToX() {
  const url = encodeURIComponent(window.location.href)
  const title = encodeURIComponent(currentArticle.value.title)
  window.open(`https://x.com/intent/tweet?text=${title}&url=${url}`, '_blank', 'noopener,noreferrer')
}

function shareToLinkedIn() {
  const url = encodeURIComponent(window.location.href)
  window.open(`https://www.linkedin.com/sharing/share-offsite/?url=${url}`, '_blank', 'noopener,noreferrer')
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

function formatLongDate(value) {
  if (!value) {
    return '2026年4月28日'
  }

  const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!match) {
    return value
  }

  return `${Number(match[1])}年${Number(match[2])}月${Number(match[3])}日`
}

function formatReadTime(value) {
  const text = String(value || '').trim()
  const minuteMatch = text.match(/(\d+)/)

  if (!minuteMatch) {
    return '8 分钟阅读'
  }

  return `${minuteMatch[1]} 分钟阅读`
}

function formatCompactCount(value) {
  const count = Number(value) || 0

  if (count >= 10000) {
    return `${(count / 1000).toFixed(1).replace('.0', '')}k`
  }

  if (count >= 1000) {
    return `${(count / 1000).toFixed(1).replace('.0', '')}k`
  }

  return `${count}`
}

function buildInitials(name) {
  const text = String(name || '').trim()
  if (!text) {
    return 'PK'
  }

  return text.length <= 2 ? text : text.slice(0, 2)
}

function escapeHtml(value) {
  return String(value || '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function escapeAttribute(value) {
  return escapeHtml(value)
}

/**
 * 目的：为缺少富文本正文的文章生成统一详情页内容。
 * 逻辑：组合摘要、亮点、配图和结论段落，让其他文章也能套用同一套杂志式排版。
 */
function buildArticleHtml(targetArticle) {
  const summary = escapeHtml(targetArticle.summary || '')
  const essence = escapeHtml(targetArticle.essence || targetArticle.summary || '')
  const title = escapeHtml(targetArticle.title || '')
  const coverUrl = escapeAttribute(targetArticle.coverUrl || '')
  const categoryLabel = resolveCategoryLabel(targetArticle.category)
  const highlightItems = (targetArticle.highlights || []).filter(Boolean)

  const highlightsHtml = highlightItems.length
    ? `
      <div class="callout callout-info">
        <span class="callout-icon">📌</span>
        <p>${highlightItems
          .map((item, index) => `<strong>重点 ${index + 1}：</strong>${escapeHtml(item)}`)
          .join('<br>')}</p>
      </div>
    `
    : ''

  const imageHtml = coverUrl
    ? `
      <figure class="article-image">
        <img src="${coverUrl}" alt="${title}" loading="lazy" />
        <figcaption class="image-caption">${title}</figcaption>
      </figure>
    `
    : ''

  return `
    <p class="lead">${essence || summary || '这是一篇围绕真实工程问题展开的技术文章。'}</p>
    <p>${summary || essence || '文章从背景、方案与取舍出发，帮助你快速建立对问题的整体认识。'}</p>
    <h2>这篇文章讲什么</h2>
    <p>这篇内容围绕 ${escapeHtml(categoryLabel)} 场景展开，不是只给结论，而是把问题出现的原因、方案的边界以及落地时的注意点一起交代清楚。</p>
    ${highlightsHtml}
    <h2>核心关注点</h2>
    <p>${essence || '文章会围绕系统设计、实现细节和后续演进三个方向，帮助你快速抓住真正值得关注的部分。'}</p>
    ${imageHtml}
    <h3>为什么值得细看</h3>
    <p>相比只罗列知识点的总结，这类工程文章更有价值的地方在于，它会把实践中的取舍路径一并讲明白，让你知道为什么这样做，而不是只知道怎么做。</p>
    <blockquote>
      <p>"真正有帮助的技术写作，不是把知识摆出来，而是把判断过程交给读者。"</p>
      <cite>— PeakDepth 技术编辑部</cite>
    </blockquote>
    <h2>适合谁读</h2>
    <p>如果你正在处理 ${escapeHtml(categoryLabel)} 相关的项目，或者希望快速建立这类问题的工程判断框架，这篇文章会比较适合你。</p>
  `
}
</script>

<style scoped src="../styles/views/TechArticleDetail.css"></style>
