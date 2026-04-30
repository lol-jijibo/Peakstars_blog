<template>
  <div class="home-page">
    <blog-mega-header current-page="home" />

    <main class="homepage-main">
      <section class="home-editorial" aria-labelledby="home-title">
        <div class="editorial-copy">
          <span class="editorial-kicker">Peakstars Editorial Home</span>
          <h1 id="home-title">Peakstars_blog</h1>

          <div class="editorial-manifesto" aria-live="polite">
            <transition name="manifesto-fade" mode="out-in">
              <p :key="activeManifesto">{{ activeManifesto }}</p>
            </transition>
          </div>

          <p class="editorial-intro">
            把文章页的沉浸式阅读气质提前搬到首页，用统一的版心、杂志式排版和轻量数据编排，
            让首页既有品牌感，也能继续承接真实内容流。
          </p>

          <div class="editorial-actions">
            <button class="editorial-primary-btn" type="button" @click="goArticles">
              进入文章页
            </button>
            <button class="editorial-secondary-btn" type="button" @click="goInterview">
              查看面经区
            </button>
          </div>

          <ul class="editorial-stats" aria-label="首页内容概览">
            <li v-for="item in quickStats" :key="item.label">
              <strong>{{ item.value }}</strong>
              <span>{{ item.label }}</span>
            </li>
          </ul>
        </div>

        <aside class="editorial-stage">
          <button
            v-if="heroArticle"
            class="stage-cover-card"
            type="button"
            :style="buildCoverStyle(heroArticle.coverUrl)"
            @click="openArticle(heroArticle.id)"
          >
            <span class="stage-cover-layer"></span>
            <div class="stage-cover-top">
              <span class="stage-chip">{{ resolveArticleLabel(heroArticle.category) }}</span>
              <span class="stage-meta">{{ formatReadTime(heroArticle.readTime) }}</span>
            </div>
            <div class="stage-cover-body">
              <h2>{{ heroArticle.title }}</h2>
              <p>{{ heroArticle.essence || heroArticle.summary }}</p>
            </div>
            <div class="stage-cover-bottom">
              <span>{{ heroArticle.author?.name || 'Peakstars_blog' }}</span>
              <span>{{ formatCompactCount(heroArticle.readCount) }} 阅读</span>
            </div>
          </button>

          <div class="stage-side-grid">
            <button
              v-if="leadRoute"
              class="stage-side-card"
              type="button"
              @click="openLearningRoute(leadRoute.slug)"
            >
              <span class="stage-side-label">学习路线</span>
              <strong>{{ leadRoute.title }}</strong>
              <p>{{ leadRoute.difficulty }} · {{ formatCompactCount(leadRoute.viewCount) }} 阅读</p>
            </button>

            <button
              v-if="leadSignal"
              class="stage-side-card stage-side-card--accent"
              type="button"
              @click="goAiHotspot"
            >
              <span class="stage-side-label">AI 热点</span>
              <strong>{{ leadSignal.title }}</strong>
              <p>热度 {{ leadSignal.heat || '--' }} · {{ formatShortDate(leadSignal.publishedAt) }}</p>
            </button>
          </div>
        </aside>
      </section>

      <section class="home-shell">
        <div class="home-stream">
          <section class="home-section" aria-labelledby="featured-title">
            <div class="section-head">
              <span class="section-kicker">Featured Longform</span>
              <h2 id="featured-title">首页精选长文</h2>
              <p>文章页的阅读气质保留在首页，但入口改成更适合扫读与分流的编排方式。</p>
            </div>

            <div class="featured-grid">
              <button
                v-if="heroArticle"
                class="feature-hero-card"
                type="button"
                :style="buildCoverStyle(heroArticle.coverUrl)"
                @click="openArticle(heroArticle.id)"
              >
                <span class="feature-hero-layer"></span>
                <div class="feature-hero-content">
                  <span class="feature-kicker">{{ resolveArticleLabel(heroArticle.category) }}</span>
                  <h3>{{ heroArticle.title }}</h3>
                  <p>{{ heroArticle.summary || heroArticle.essence }}</p>
                  <div class="feature-meta-row">
                    <span>{{ formatShortDate(heroArticle.publishedAt) }}</span>
                    <span>{{ formatCompactCount(heroArticle.commentCount) }} 评论</span>
                  </div>
                </div>
              </button>

              <div class="feature-stack">
                <button
                  v-for="article in featureStack"
                  :key="article.id"
                  class="feature-stack-card"
                  type="button"
                  @click="openArticle(article.id)"
                >
                  <span class="feature-stack-tag">{{ resolveArticleLabel(article.category) }}</span>
                  <strong>{{ article.title }}</strong>
                  <p>{{ article.essence || article.summary }}</p>
                  <span class="feature-stack-meta">
                    {{ article.author?.name || 'Peakstars_blog' }} · {{ formatCompactCount(article.readCount) }} 阅读
                  </span>
                </button>
              </div>
            </div>
          </section>

          <section class="home-section" aria-labelledby="route-title">
            <div class="section-head section-head--inline">
              <div>
                <span class="section-kicker">Route Ribbon</span>
                <h2 id="route-title">结构化学习路线</h2>
              </div>
              <button class="section-link-btn" type="button" @click="openLearningRoute(leadRoute?.slug)">
                直接进入
              </button>
            </div>

            <div class="route-ribbon">
              <button
                v-for="route in routePicks"
                :key="route.slug"
                class="route-ribbon-card"
                type="button"
                :style="buildCoverStyle(route.coverUrl)"
                @click="openLearningRoute(route.slug)"
              >
                <span class="route-ribbon-layer"></span>
                <div class="route-ribbon-content">
                  <span class="route-badge">{{ route.routeType }}</span>
                  <h3>{{ route.title }}</h3>
                  <p>{{ route.difficulty }}</p>
                  <div class="route-ribbon-meta">
                    <span>{{ formatShortDate(route.publishedAt) }}</span>
                    <span>{{ formatCompactCount(route.likeCount) }} 点赞</span>
                  </div>
                </div>
              </button>
            </div>
          </section>

          <section class="home-section" aria-labelledby="signal-title">
            <div class="section-head">
              <span class="section-kicker">Signal Board</span>
              <h2 id="signal-title">AI 热点快照</h2>
              <p>不把热点堆成瀑布流，而是保留三张高密度卡片，适合首页快速浏览。</p>
            </div>

            <div class="signal-board">
              <button
                v-for="signal in aiSignals"
                :key="signal.id"
                class="signal-card"
                type="button"
                @click="goAiHotspot"
              >
                <div class="signal-card-head">
                  <span class="signal-tag">{{ signal.hotspotType }}</span>
                  <span class="signal-heat">Heat {{ signal.heat || '--' }}</span>
                </div>
                <strong>{{ signal.title }}</strong>
                <p>{{ signal.summary }}</p>
                <div class="signal-meta-row">
                  <span>{{ signal.authorName || 'Peakstars_blog' }}</span>
                  <span>{{ formatShortDate(signal.publishedAt) }}</span>
                </div>
              </button>
            </div>
          </section>
        </div>

        <aside class="home-rail">
          <section class="rail-card">
            <span class="rail-kicker">Daily Flow</span>
            <h3>今日阅读动线</h3>
            <div class="flow-list">
              <button
                v-if="heroArticle"
                class="flow-item"
                type="button"
                @click="openArticle(heroArticle.id)"
              >
                <span>01</span>
                <div>
                  <strong>先读一篇主打长文</strong>
                  <p>{{ heroArticle.title }}</p>
                </div>
              </button>

              <button
                v-if="leadRoute"
                class="flow-item"
                type="button"
                @click="openLearningRoute(leadRoute.slug)"
              >
                <span>02</span>
                <div>
                  <strong>再进入完整路线</strong>
                  <p>{{ leadRoute.title }}</p>
                </div>
              </button>

              <button
                v-if="leadSignal"
                class="flow-item"
                type="button"
                @click="goAiHotspot"
              >
                <span>03</span>
                <div>
                  <strong>最后补充行业热点</strong>
                  <p>{{ leadSignal.title }}</p>
                </div>
              </button>
            </div>
          </section>

          <section class="rail-card">
            <span class="rail-kicker">Theme Mode</span>
            <h3>自由切换明暗背景</h3>
            <p class="rail-copy">
              当前为{{ isDarkTheme ? '暗色沉浸模式' : '亮色纸感模式' }}，首页与文章页会一起切换，保证整站视觉一致。
            </p>
            <button class="rail-theme-switch" type="button" @click="themeStore.toggleTheme()">
              切换到{{ isDarkTheme ? '亮色' : '暗色' }}背景
            </button>
          </section>

          <section class="rail-card">
            <span class="rail-kicker">Editing Protocol</span>
            <h3>首页编排原则</h3>
            <ul class="protocol-list">
              <li v-for="item in editorialProtocols" :key="item.title">
                <strong>{{ item.title }}</strong>
                <p>{{ item.description }}</p>
              </li>
            </ul>
          </section>
        </aside>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BlogMegaHeader from '@/components/BlogMegaHeader.vue'
import { getTechArticles, getAiHotspots } from '@/api/content'
import { getLearningRoutes } from '@/api/learningRoute'
import { techArticles as localTechArticles } from '@/data/techArticles'
import { aiHotspots as localAiHotspots } from '@/modules/ai/aiHotspots'
import { useThemeStore } from '@/stores/theme'

const router = useRouter()
const themeStore = useThemeStore()
const techArticles = ref([...localTechArticles])
const learningRoutes = ref([])
const aiHotspots = ref([...localAiHotspots])
const activeManifestoIndex = ref(0)
let manifestoTimer = null

/**
 * 目的: 预置首页品牌宣言轮播文案
 * 逻辑: 使用静态文案维持首屏呼吸感，不新增任何后端配置字段
 */
const heroManifestos = [
  '把复杂技术拆成可以继续前进的阅读路径。',
  '让首页先完成判断，再把读者送进深度正文。',
  '同一套内容骨架，既能沉浸阅读，也能高效扫读。',
  '首页像编辑部，文章页像正文，这两者现在连成一体。'
]

/**
 * 目的: 为学习路线接口提供首页兜底内容
 * 逻辑: 当远端接口暂不可用时，首页仍然能输出稳定的路线入口
 */
const fallbackLearningRoutes = [
  {
    id: 'fallback-java',
    slug: 'java-backend-roadmap',
    routeType: 'java',
    title: 'Java 后端工程师成长路线',
    coverUrl: '/【哲风壁纸】xiaomiyu7-小米suv.png',
    publishedAt: '2026-04-23 10:00',
    difficulty: '进阶路线',
    viewCount: 2846,
    commentCount: 36,
    likeCount: 528
  },
  {
    id: 'fallback-fullstack',
    slug: 'fullstack-roadmap',
    routeType: 'fullstack',
    title: '全栈开发者进阶路线',
    coverUrl: '/【哲风壁纸】夏日-晴天-氛围感.png',
    publishedAt: '2026-04-23 11:20',
    difficulty: '系统路线',
    viewCount: 1938,
    commentCount: 24,
    likeCount: 416
  }
]

/**
 * 目的: 固定首页右侧的产品表达原则
 * 逻辑: 用少量静态协议说明首页设计思路，方便后续继续维护同一套编排逻辑
 */
const editorialProtocols = [
  {
    title: '入口不做过载',
    description: '首屏只保留长文、路线、热点三个主入口，减少维护复杂度。'
  },
  {
    title: '数据全部复用',
    description: '首页直接读取现有内容接口，不新增首页专属表或额外后台流程。'
  },
  {
    title: '主题统一切换',
    description: '首页与文章页共享同一套明暗模式状态，降低视觉维护成本。'
  }
]

const activeManifesto = computed(() => heroManifestos[activeManifestoIndex.value])

const sortedArticles = computed(() => {
  return [...techArticles.value].sort((left, right) => {
    const leftScore = Number(Boolean(left.featured)) * 100000 + Number(left.readCount || 0)
    const rightScore = Number(Boolean(right.featured)) * 100000 + Number(right.readCount || 0)
    return rightScore - leftScore
  })
})

const heroArticle = computed(() => sortedArticles.value[0] || null)
const featureStack = computed(() => sortedArticles.value.slice(1, 3))
const routePicks = computed(() => learningRoutes.value.slice(0, 2))
const leadRoute = computed(() => routePicks.value[0] || null)

const aiSignals = computed(() => {
  return [...aiHotspots.value]
    .sort((left, right) => Number(right.heat || 0) - Number(left.heat || 0))
    .slice(0, 3)
})

const leadSignal = computed(() => aiSignals.value[0] || null)
const isDarkTheme = computed(() => themeStore.isDark.value)

/**
 * 目的: 首页内容总览数字与当前模块同步
 * 逻辑: 基于现有列表长度和精选内容实时生成，不维护额外统计接口
 */
const quickStats = computed(() => [
  {
    label: '文章沉淀',
    value: `${techArticles.value.length}+`
  },
  {
    label: '路线专题',
    value: `${learningRoutes.value.length || fallbackLearningRoutes.length}`
  },
  {
    label: '热点快照',
    value: `${aiHotspots.value.length}`
  }
])

/**
 * 目的: 聚合首页现有模块数据
 * 逻辑: 并行拉取文章、路线与热点，任一失败时回退本地兜底，保证首页始终可渲染
 */
async function loadHomeData() {
  const [articleResult, routeResult, aiResult] = await Promise.allSettled([
    getTechArticles(),
    getLearningRoutes(),
    getAiHotspots()
  ])

  techArticles.value =
    articleResult.status === 'fulfilled' && Array.isArray(articleResult.value) && articleResult.value.length
      ? articleResult.value
      : [...localTechArticles]

  learningRoutes.value =
    routeResult.status === 'fulfilled' && Array.isArray(routeResult.value) && routeResult.value.length
      ? routeResult.value
      : [...fallbackLearningRoutes]

  aiHotspots.value =
    aiResult.status === 'fulfilled' && Array.isArray(aiResult.value) && aiResult.value.length
      ? aiResult.value
      : [...localAiHotspots]
}

function goArticles() {
  router.push('/articles')
}

function goInterview() {
  router.push('/interview')
}

function goAiHotspot() {
  router.push('/ai-hotspot')
}

function openArticle(articleId) {
  router.push(`/articles/${articleId}`)
}

function openLearningRoute(slug) {
  if (!slug) {
    return
  }

  router.push(`/learning-route/${slug}`)
}

function showNextManifesto() {
  activeManifestoIndex.value = (activeManifestoIndex.value + 1) % heroManifestos.length
}

function startManifestoTimer() {
  if (manifestoTimer) {
    window.clearInterval(manifestoTimer)
  }

  manifestoTimer = window.setInterval(showNextManifesto, 3200)
}

function buildCoverStyle(coverUrl) {
  const safeCover = coverUrl || '/peakstars-blog-icon.jpg'
  return {
    backgroundImage: `linear-gradient(180deg, rgba(7, 13, 24, 0.08), rgba(7, 13, 24, 0.82)), url("${safeCover}")`
  }
}

function resolveArticleLabel(category) {
  const labelMap = {
    frontend: '前端工程',
    backend: '后端架构',
    vip: '深度专题'
  }

  return labelMap[category] || '技术文章'
}

function formatCompactCount(value) {
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
  const text = String(value || '').trim()
  const minuteMatch = text.match(/(\d+)/)
  return minuteMatch ? `${minuteMatch[1]} min` : '8 min'
}

function formatShortDate(value) {
  const text = String(value || '')
  const match = text.match(/(\d{4})-(\d{2})-(\d{2})/)

  if (!match) {
    return text || '--'
  }

  return `${match[2]}/${match[3]}`
}

onMounted(() => {
  loadHomeData()
  startManifestoTimer()
})

onBeforeUnmount(() => {
  if (manifestoTimer) {
    window.clearInterval(manifestoTimer)
  }
})
</script>

<style scoped src="../styles/views/HomeView.css"></style>
