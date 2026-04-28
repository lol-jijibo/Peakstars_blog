<template>
  <div class="article-detail-page">
    <blog-mega-header current-page="article" />

    <main class="article-detail-main">
      <button class="article-detail-back" type="button" @click="goBack">返回文章列表</button>

      <div v-if="loading" class="article-detail-state">文章加载中...</div>
      <div v-else-if="error" class="article-detail-state article-detail-state--error">
        <strong>{{ error }}</strong>
        <button type="button" @click="router.push('/articles')">回到文章列表</button>
      </div>

      <!-- 目的: 详情页主体承接文章阅读、互动和侧边信息展示。 -->
      <!-- 逻辑: 左侧互动栏、正文和右侧作者目录三列排列，移动端自动收拢为单列。 -->
      <section v-else-if="article" class="article-detail-layout">
        <aside class="article-action-rail" aria-label="文章互动">
          <button
            class="article-action-btn article-action-btn--like"
            :class="{ active: isLiked }"
            type="button"
            @click="toggleLike"
            aria-label="点赞"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 21.2 10.7 20C5.8 15.6 2.5 12.6 2.5 8.9 2.5 5.9 4.8 3.6 7.8 3.6c1.7 0 3.3.8 4.2 2 1-1.2 2.6-2 4.2-2 3 0 5.3 2.3 5.3 5.3 0 3.7-3.3 6.7-8.2 11.1L12 21.2Z" />
            </svg>
            <span>{{ formatCount(localLikeCount) }}</span>
          </button>

          <button class="article-action-btn article-action-btn--comment" type="button" @click="scrollToComments" aria-label="评论">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M4.4 4.7c1.8-1.7 4.3-2.6 7.5-2.6 3.1 0 5.6.9 7.5 2.6 1.7 1.6 2.6 3.7 2.6 6.2 0 2.5-.9 4.6-2.6 6.1-1.8 1.7-4.3 2.5-7.5 2.5-.7 0-1.3 0-1.9-.1l-5.1 2.4c-.8.4-1.6-.4-1.3-1.2l1.6-4.4C3.1 14.7 2 12.9 2 10.9c0-2.5.8-4.6 2.4-6.2Zm3.2 5.1a1.2 1.2 0 1 0 0 2.4 1.2 1.2 0 0 0 0-2.4Zm4.4 0a1.2 1.2 0 1 0 0 2.4 1.2 1.2 0 0 0 0-2.4Zm4.4 0a1.2 1.2 0 1 0 0 2.4 1.2 1.2 0 0 0 0-2.4Z" />
            </svg>
            <span>{{ formatCount(article.commentCount) }}</span>
          </button>

          <button
            class="article-action-btn article-action-btn--collect"
            :class="{ active: isCollected }"
            type="button"
            @click="toggleCollect"
            aria-label="收藏"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M6 3.2h12c1.1 0 2 .9 2 2v15c0 .8-.9 1.3-1.5.8L12 16.5 5.5 21c-.7.5-1.5 0-1.5-.8v-15c0-1.1.9-2 2-2Z" />
            </svg>
            <span>{{ formatCount(localCollectCount) }}</span>
          </button>
        </aside>

        <article class="article-detail-content">
          <header class="article-detail-hero">
            <div class="article-detail-meta">
              <span>{{ article.category === 'frontend' ? '前端文章' : '后端文章' }}</span>
              <span>{{ article.publishedAt }}</span>
              <span>{{ article.readTime }}</span>
            </div>
            <h1>{{ article.title }}</h1>
            <p>{{ article.summary }}</p>
          </header>

          <div ref="articleContentRef" class="article-detail-body" v-html="articleBody.html"></div>

          <section id="article-comments" class="article-comments-panel">
            <h2>评论</h2>
            <p>共 {{ formatCount(article.commentCount) }} 条讨论，欢迎围绕本文的方案取舍继续交流。</p>
          </section>
        </article>

        <aside class="article-detail-side">
          <section class="article-author-panel">
            <div class="article-author-profile">
              <img v-if="authorAvatarUrl" :src="authorAvatarUrl" :alt="article.author.name" loading="lazy" />
              <div v-else class="article-author-profile-fallback" :style="{ background: article.author.accent }">
                {{ article.author.initials }}
              </div>
              <div>
                <strong>{{ article.author.name }}</strong>
                <span>{{ article.author.role }}</span>
              </div>
            </div>

            <div class="article-author-stats">
              <span><strong>{{ authorArticleCount }}</strong>文章</span>
              <span><strong>{{ followersCount }}</strong>粉丝</span>
              <span><strong>{{ followingCount }}</strong>关注</span>
            </div>
          </section>

          <section class="article-toc-panel">
            <button class="article-toc-head" type="button" @click="isTocCollapsed = !isTocCollapsed">
              <span>目录</span>
              <span class="article-toc-toggle">{{ isTocCollapsed ? '展开' : '收起' }}</span>
            </button>

            <nav v-show="!isTocCollapsed" class="article-toc-list" aria-label="文章目录">
              <button
                v-for="item in articleBody.toc"
                :key="item.id"
                class="article-toc-item"
                :class="[`article-toc-item--level-${item.level}`, { active: activeHeadingId === item.id }]"
                type="button"
                @click="scrollToHeading(item.id)"
              >
                {{ item.title }}
              </button>
            </nav>
          </section>
        </aside>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTechArticles } from '@/api/content'
import BlogMegaHeader from '@/components/BlogMegaHeader.vue'
import { techArticles as fallbackTechArticles } from '@/data/techArticles'

const route = useRoute()
const router = useRouter()

const article = ref(null)
const techArticles = ref([])
const loading = ref(true)
const error = ref('')
const isLiked = ref(false)
const isCollected = ref(false)
const isTocCollapsed = ref(false)
const localLikeCount = ref(0)
const localCollectCount = ref(0)
const activeHeadingId = ref('')
const articleContentRef = ref(null)
let headingObserver = null

// 目的: 复用文章列表作者头像资源，让详情页作者信息与推荐区视觉保持一致。
// 逻辑: 按作者名称匹配头像，未命中时回退到作者首字母渐变头像。
const authorAvatarMap = {
  'Francek Chen': encodeURI('/【哲风壁纸】夏日-晴天-氛围感.png'),
  '科技 D 人生': encodeURI('/ChatGPT Image 2026年4月23日 18_37_46.png'),
  '自由程序员': encodeURI('/【哲风壁纸】xiaomiyu7-小米suv.png'),
  '微笑很纯洁': encodeURI('/【哲风壁纸】公路-后视镜-城镇.png'),
  '算法与编程之美': encodeURI('/【哲风壁纸】云彩-夜晚-夜景.png'),
  'fTiN CAPA': '/qq.jpg'
}

const articleBody = computed(() => createArticleBody(article.value))

const authorAvatarUrl = computed(() => authorAvatarMap[article.value?.author?.name] || '')

const authorArticleCount = computed(() => {
  if (!article.value) {
    return 0
  }

  return techArticles.value.filter((item) => item.author?.name === article.value.author?.name).length || 1
})

const followersCount = computed(() => {
  if (!article.value) {
    return '0'
  }

  return formatCount(Math.max(1200, article.value.readCount * 6 + article.value.likeCount * 38))
})

const followingCount = computed(() => {
  if (!article.value) {
    return 0
  }

  return Math.max(12, authorArticleCount.value * 5 + 8)
})

// 目的: 详情页直接复用文章列表接口，保证列表与详情的数据口径一致。
// 逻辑: 先请求后端内容缓存，失败时回退本地静态数据，再用路由 id 定位当前文章。
watch(
  () => route.params.id,
  async (articleId) => {
    await loadArticle(articleId)
  },
  { immediate: true }
)

// 目的: 正文目录生成后重新绑定滚动监听，让目录颜色跟随当前阅读章节变化。
// 逻辑: 监听目录 id 序列，等待 DOM 更新完成后用 IntersectionObserver 观察标题位置。
watch(
  () => articleBody.value.toc.map((item) => item.id).join('|'),
  async () => {
    await nextTick()
    bindHeadingObserver()
  }
)

onBeforeUnmount(() => {
  if (headingObserver) {
    headingObserver.disconnect()
  }
})

async function loadArticle(articleId) {
  loading.value = true
  error.value = ''

  try {
    techArticles.value = await getTechArticles()
  } catch {
    techArticles.value = fallbackTechArticles
  }

  article.value = techArticles.value.find((item) => item.id === articleId) || null

  if (!article.value) {
    error.value = '没有找到这篇技术文章。'
  } else {
    isLiked.value = Boolean(article.value.isLiked)
    isCollected.value = Boolean(article.value.isCollected)
    localLikeCount.value = Number(article.value.likeCount || 0)
    localCollectCount.value = Number(article.value.collectCount || 0)
    activeHeadingId.value = articleBody.value.toc[0]?.id || ''
  }

  loading.value = false
  await nextTick()
  bindHeadingObserver()
}

// 目的: 把文章正文、摘要和亮点整理成可渲染详情内容与目录数据。
// 逻辑: 使用 DOMParser 给 h2/h3 补稳定 id，目录直接读取标题文本，避免手写字符串切割。
function createArticleBody(currentArticle) {
  if (!currentArticle) {
    return { html: '', toc: [] }
  }

  const baseHtml = buildArticleHtml(currentArticle)
  const parser = new DOMParser()
  const doc = parser.parseFromString(`<div>${baseHtml}</div>`, 'text/html')
  const headings = Array.from(doc.body.querySelectorAll('h2, h3'))
  const toc = headings.map((node, index) => {
    const id = `article-section-${index + 1}`
    node.id = id
    node.dataset.tocId = id

    return {
      id,
      title: node.textContent?.trim() || `章节 ${index + 1}`,
      level: node.tagName === 'H3' ? 3 : 2
    }
  })

  return {
    html: doc.body.innerHTML,
    toc
  }
}

// 目的: 为后台正文补足详情页阅读结构，让每篇文章都有多段可追踪目录。
// 逻辑: 核心观点使用文章精华，正文使用后台富文本，落地清单使用 highlights 字段。
function buildArticleHtml(currentArticle) {
  const highlights = (currentArticle.highlights || [])
    .map((item) => `<li>${escapeHtml(item)}</li>`)
    .join('')
  const contentHtml = currentArticle.contentHtml || ''
  const categoryLabel = currentArticle.category === 'frontend' ? '前端' : '后端'

  return `
    <section>
      <h2>核心观点</h2>
      <p>${escapeHtml(currentArticle.essence || currentArticle.summary)}</p>
    </section>
    ${contentHtml}
    <section>
      <h2>落地清单</h2>
      <ul>${highlights}</ul>
    </section>
    <section>
      <h2>阅读建议</h2>
      <p>这篇${categoryLabel}文章适合结合当前项目复盘：先确认问题边界，再把关键动作拆成可以验证的小步骤。</p>
    </section>
  `
}

// 目的: 让右侧目录准确感知正文当前阅读位置。
// 逻辑: 观察带目录标记的标题进入视口中段时更新 activeHeadingId，目录项随之变色。
function bindHeadingObserver() {
  if (headingObserver) {
    headingObserver.disconnect()
  }

  const headings = articleContentRef.value?.querySelectorAll('[data-toc-id]')
  if (!headings?.length) {
    return
  }

  if (!('IntersectionObserver' in window)) {
    activeHeadingId.value = headings[0].id
    return
  }

  headingObserver = new IntersectionObserver(
    (entries) => {
      const visibleEntry = entries
        .filter((entry) => entry.isIntersecting)
        .sort((left, right) => left.boundingClientRect.top - right.boundingClientRect.top)[0]

      if (visibleEntry?.target?.id) {
        activeHeadingId.value = visibleEntry.target.id
      }
    },
    {
      rootMargin: '-18% 0px -64% 0px',
      threshold: [0, 0.2, 1]
    }
  )

  headings.forEach((heading) => headingObserver.observe(heading))
}

// 目的: 点赞与收藏在详情页即时反馈，避免用户操作后等待远端状态。
// 逻辑: 本地切换选中态并同步调整计数，后续可直接接入真实互动接口。
function toggleLike() {
  isLiked.value = !isLiked.value
  localLikeCount.value += isLiked.value ? 1 : -1
  localLikeCount.value = Math.max(0, localLikeCount.value)
}

function toggleCollect() {
  isCollected.value = !isCollected.value
  localCollectCount.value += isCollected.value ? 1 : -1
  localCollectCount.value = Math.max(0, localCollectCount.value)
}

// 目的: 目录点击后快速回到对应章节，减少长文阅读中的定位成本。
// 逻辑: 使用浏览器原生平滑滚动，并配合标题 scroll-margin 避开顶部导航遮挡。
function scrollToHeading(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// 目的: 左侧评论按钮连接到评论承接区，保留完整互动路径。
// 逻辑: 滚动到固定评论锚点，未来接入真实评论列表时按钮行为无需调整。
function scrollToComments() {
  document.getElementById('article-comments')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// 目的: 详情页提供稳定返回入口，方便用户回到文章流继续浏览。
// 逻辑: 有浏览历史时返回上一页，否则兜底跳回技术文章列表。
function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }

  router.push('/articles')
}

// 目的: 统一互动数据展示格式，让大数字在侧栏和动作栏里保持简洁。
// 逻辑: 千级与万级数字缩写，其余数字原样展示。
function formatCount(value) {
  const count = Number(value || 0)

  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}w`
  }

  if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`
  }

  return `${count}`
}

// 目的: 保护由摘要和亮点拼出的正文片段，避免普通文本被当作 HTML 执行。
// 逻辑: 转义基础 HTML 特殊字符，后台富文本正文仍按可信内容渲染。
function escapeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
</script>

<style scoped src="../styles/views/TechArticleDetail.css"></style>
