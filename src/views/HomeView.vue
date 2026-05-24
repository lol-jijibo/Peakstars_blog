<template>
  <div class="home-page">
    <blog-mega-header current-page="home" />

    <main class="home-main">
      <!-- HERO -->
      <section class="hero" aria-label="首页主视觉">
        <div class="hero-bg">
          <div class="hero-grid-lines"></div>
          <div class="hero-glow hero-glow-1"></div>
          <div class="hero-glow hero-glow-2"></div>
        </div>
        <div class="hero-inner">
          <div class="hero-badge animate-fade-up">
            <span class="badge-dot"></span>
            <span>面向持续精进的开发者</span>
          </div>
          <h1 class="hero-title animate-fade-up delay-100">
            <span class="hero-title-accent">PeakStars</span> Blog
          </h1>
          <p class="hero-desc animate-fade-up delay-200">
            技术博客与面经论坛，写值得花时间阅读的内容。<br>
            深耕前端工程、后端架构与系统设计，提供系统化学习路线与真实面试经验。
          </p>
          <div class="hero-actions animate-fade-up delay-300">
            <button class="btn-primary" type="button" @click="goArticles">
              <i class="fas fa-book-open"></i>
              <span>探索文章</span>
            </button>
            <button class="btn-outline" type="button" @click="goInterview">
              <i class="fas fa-crosshairs"></i>
              <span>面经宝典</span>
            </button>
          </div>
          <div class="hero-stats animate-fade-up delay-300">
            <div class="stat-item">
              <span class="stat-num">{{ techArticles.length }}</span>
              <span class="stat-label">技术文章</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item stat-item-enroll">
              <span class="stat-num">{{ routeEnrollCount }}</span>
              <span class="stat-label stat-label--enroll">注册人数</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-num">{{ interviewCount }}</span>
              <span class="stat-label">面经数量</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-num">{{ popularTags.length }}+</span>
              <span class="stat-label">技术标签</span>
            </div>
          </div>
        </div>
      </section>

      <!-- FEATURE ZONES -->
      <section class="zones" aria-label="核心内容">
        <div class="container">
          <div class="section-label animate-fade-up">
            <span class="section-label-line"></span>
            <span class="section-label-text">核心板块</span>
            <span class="section-label-line"></span>
          </div>
          <div class="zones-grid">
            <article class="zone-card zc-tech animate-fade-up delay-100" @click="goArticles">
              <div class="zone-icon">
                <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M16 14L6 24L16 34" stroke="currentColor" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M32 14L42 24L32 34" stroke="currentColor" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M28 8L20 40" stroke="currentColor" stroke-width="3" stroke-linecap="round" opacity="0.5"/>
                </svg>
              </div>
              <h3 class="zone-title">技术深耕</h3>
              <p class="zone-desc">深度技术文章，覆盖前端工程、后端架构与系统设计，每一篇都值得反复品读。</p>
              <div class="zone-tags">
                <span class="zone-tag">Vue 3</span>
                <span class="zone-tag">Node.js</span>
                <span class="zone-tag">TypeScript</span>
              </div>
              <span class="zone-arrow"><i class="fas fa-arrow-right"></i></span>
            </article>

            <article class="zone-card zc-interview animate-fade-up delay-200" @click="goInterview">
              <div class="zone-icon">
                <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="24" cy="20" r="8" stroke="currentColor" stroke-width="3"/>
                  <path d="M12 38C12 31.373 17.373 26 24 26C30.627 26 36 31.373 36 38" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
                  <path d="M30 12L36 6" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" opacity="0.5"/>
                  <circle cx="37" cy="5" r="2.5" fill="currentColor" opacity="0.4"/>
                  <path d="M24 16V22" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
                  <path d="M21 19H27" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
                </svg>
              </div>
              <h3 class="zone-title">面经宝典</h3>
              <p class="zone-desc">真实面试经验沉淀，从简历优化到高频考点，助你从容应对每一场面试。</p>
              <div class="zone-tags">
                <span class="zone-tag">大厂面经</span>
                <span class="zone-tag">高频考点</span>
                <span class="zone-tag">真题解析</span>
              </div>
              <span class="zone-arrow"><i class="fas fa-arrow-right"></i></span>
            </article>


            <article class="zone-card zc-route animate-fade-up delay-300" @click="goArticles">
              <div class="zone-icon">
                <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M8 36C12 28 16 32 20 24C24 16 28 20 32 12C34 8 36 10 40 8" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="8" cy="36" r="3" fill="currentColor" opacity="0.9"/>
                  <circle cx="20" cy="24" r="3" fill="currentColor" opacity="0.6"/>
                  <circle cx="32" cy="12" r="3" fill="currentColor" opacity="0.4"/>
                  <circle cx="40" cy="8" r="2.5" stroke="currentColor" stroke-width="2" fill="none" opacity="0.5"/>
                  <path d="M12 38L8 36L10 32" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" opacity="0.3"/>
                </svg>
              </div>
              <h3 class="zone-title">学习路线</h3>
              <p class="zone-desc">从入门到进阶的系统化学习路径，让你不再迷失于海量资料之中。</p>
              <div class="zone-tags">
                <span class="zone-tag">Java 后端</span>
                <span class="zone-tag">全栈进阶</span>
                <span class="zone-tag">前端体系</span>
              </div>
              <span class="zone-arrow"><i class="fas fa-arrow-right"></i></span>
            </article>
          </div>
        </div>
      </section>

      <!-- FEATURED HIGHLIGHTS -->
      <section class="highlights" aria-label="精选内容">
        <div class="container">
          <div class="section-label animate-fade-up">
            <span class="section-label-line"></span>
            <span class="section-label-text">精选推荐</span>
            <span class="section-label-line"></span>
          </div>
          <div class="highlights-grid">
            <article
              v-for="(article, idx) in trendingArticles"
              :key="article.id"
              class="highlight-card animate-fade-up"
              :class="{ 'delay-100': idx === 0, 'delay-200': idx === 1, 'delay-300': idx === 2 }"
              @click="openArticle(article.id)"
            >
              <div class="hl-cover">
                <img class="hl-cover-image" :src="resolveCoverUrl(article.coverUrl)" :alt="article.title" loading="lazy" />
                <span class="hl-badge">{{ resolveArticleLabel(article.category) }}</span>
              </div>
              <div class="hl-body">
                <div class="hl-tags">
                  <span v-for="tag in getArticleTags(article)" :key="tag" class="hl-tag" :class="tagColorClass(tag)">{{ tag }}</span>
                </div>
                <h3 class="hl-title">{{ article.title }}</h3>
                <p class="hl-excerpt">{{ article.essence || article.summary }}</p>
                <div class="hl-meta">
                  <span class="hl-date">{{ formatShortDateCN(article.publishedAt) }}</span>
                  <span class="hl-read">{{ formatReadTime(article.readTime) }}阅读</span>
                </div>
              </div>
            </article>
          </div>
          <div class="highlights-more animate-fade-up">
            <button class="btn-outline btn-sm" type="button" @click="goArticles">
              查看全部文章 <i class="fas fa-arrow-right"></i>
            </button>
          </div>
        </div>
      </section>

      <!-- TECH SPHERE -->
      <section class="tech-sphere" aria-label="技术图谱">
        <div class="container">
          <div class="section-label animate-fade-up">
            <span class="section-label-line"></span>
            <span class="section-label-text">技术图谱</span>
            <span class="section-label-line"></span>
          </div>
          <div class="sphere-grid animate-fade-up delay-100">
            <div
              v-for="tag in popularTags"
              :key="tag.name"
              class="sphere-tag"
              :class="'sp-' + (popularTags.indexOf(tag) % 6)"
            >
              <span class="sphere-tag-name">{{ tag.name }}</span>
              <span class="sphere-tag-count">{{ tag.count }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- ABOUT / PERSONAL -->
      <section class="about-section" aria-label="关于作者">
        <div class="container">
          <div class="about-card animate-fade-up">
            <div class="about-avatar-area">
              <div class="about-avatar-ring">
                <img class="about-avatar-image" src="/qq.jpg" alt="码客星云头像" />
              </div>
            </div>
            <div class="about-content">
              <h3 class="about-name">码客星云</h3>
              <p class="about-role">全栈架构师 / 终身学习者 / 小白本白</p>
              <p class="about-bio">
                相信技术的力量，也相信持续学习的价值。在这里记录架构设计的思考、面试准备的沉淀与工程实践的观察。
                每一篇文章都追求深度与实用性并重，不只是告诉你怎么做，更帮你理解为什么。
              </p>
            </div>
            <div class="about-theme-panel">
              <h4 class="theme-panel-title"><i class="fas fa-palette"></i> 明暗模式</h4>
              <p class="theme-panel-desc">{{ isDarkTheme ? '当前：暗色沉浸模式' : '当前：亮色纸感模式' }}</p>
              <button class="btn-theme" type="button" @click="themeStore.toggleTheme()">
                <i :class="isDarkTheme ? 'fas fa-sun' : 'fas fa-moon'"></i>
                <span>切换到{{ isDarkTheme ? '亮色' : '暗色' }}</span>
              </button>
              <p class="theme-panel-note">背景色支持前后端联动配置</p>
            </div>
          </div>
        </div>
      </section>

      <!-- FOOTER -->
      <footer class="home-footer" aria-label="站点底部">
        <div class="container">
          <div class="footer-inner">
            <div class="footer-brand-col">
              <div class="footer-logo">
                <img class="footer-logo-icon" src="/qq.jpg" alt="PeakStars_blog 头像" />
                <span class="footer-logo-text">PeakStars_blog</span>
              </div>
              <p class="footer-tagline">面向持续精进的开发者。技术博客与面经论坛，写值得你花时间阅读的内容。</p>
            </div>
            <div class="footer-links-col">
              <h4 class="footer-col-title">内容</h4>
              <ul class="footer-links">
                <li><a href="javascript:void(0)" @click="goArticles">技术文章</a></li>
                <li><a href="javascript:void(0)" @click="goInterview">面经宝典</a></li>
              </ul>
            </div>
            <div class="footer-links-col">
              <h4 class="footer-col-title">话题</h4>
              <ul class="footer-links">
                <li><a href="javascript:void(0)" @click="goArticles">前端工程</a></li>
                <li><a href="javascript:void(0)" @click="goArticles">后端架构</a></li>
                <li><a href="javascript:void(0)" @click="goArticles">深度专题</a></li>
              </ul>
            </div>
            <div class="footer-links-col">
              <h4 class="footer-col-title">关于</h4>
              <ul class="footer-links">
                <li><a href="javascript:void(0)">关于我们</a></li>
                <li><a href="javascript:void(0)">投稿指南</a></li>
                <li><a href="javascript:void(0)">联系我们</a></li>
              </ul>
            </div>
          </div>
          <div class="footer-foot">
            <span>© 2026 PeakStars_blog. 保留所有权利。</span>
            <div class="footer-foot-links">
              <a href="javascript:void(0)">隐私政策</a>
              <a href="javascript:void(0)">服务条款</a>
            </div>
          </div>
        </div>
      </footer>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BlogMegaHeader from '@/components/BlogMegaHeader.vue'
import { getTechArticles } from '@/api/content'
import { getInterviews } from '@/api/interview'
import { getLearningRoutes } from '@/api/learningRoute'
import { useThemeStore } from '@/stores/theme'

const router = useRouter()
const themeStore = useThemeStore()
const techArticles = ref([])
const learningRoutes = ref([])
const interviewCount = ref(0)
const routeEnrollCount = ref(0)

const sortedArticles = computed(() => {
  return [...techArticles.value].sort((left, right) => {
    const leftScore = Number(Boolean(left.featured)) * 100000 + Number(left.readCount || 0)
    const rightScore = Number(Boolean(right.featured)) * 100000 + Number(right.readCount || 0)
    return rightScore - leftScore
  })
})

const trendingArticles = computed(() => sortedArticles.value.slice(0, 3))
const isDarkTheme = computed(() => themeStore.isDark.value)

const popularTags = computed(() => {
  const tagMap = new Map()
  techArticles.value.forEach((a) => {
    if (a.tags) a.tags.forEach((t) => {
      tagMap.set(t, (tagMap.get(t) || 0) + 1)
    })
  })
  if (tagMap.size === 0) {
    const defaults = ['Vue 3', 'Node.js', 'Express', 'MySQL', '面经', '前端', '后端', '架构', 'TypeScript', 'Redis']
    return defaults.map(name => ({ name, count: Math.floor(Math.random() * 20) + 3 }))
  }
  return [...tagMap.entries()].slice(0, 12).map(([name, count]) => ({ name, count }))
})

async function loadHomeData() {
  const apiBase = import.meta.env.VITE_JAVA_API_BASE_URL || '/auth-api'
  const [articleResult, routeResult, interviewResult, userCountResult] = await Promise.allSettled([
    getTechArticles({ pageSize: 20 }),
    getLearningRoutes(),
    getInterviews({ page: 1, pageSize: 1 }),
    fetch(`${apiBase}/api/auth/user-count`).then(r => r.ok ? r.json() : null)
  ])

  if (articleResult.status === 'fulfilled' && articleResult.value?.list) {
    techArticles.value = articleResult.value.list
  }

  if (routeResult.status === 'fulfilled' && Array.isArray(routeResult.value)) {
    learningRoutes.value = routeResult.value
  }

  if (interviewResult.status === 'fulfilled') {
    interviewCount.value = Number(interviewResult.value?.total || 0)
  }

  if (userCountResult.status === 'fulfilled' && userCountResult.value?.data) {
    routeEnrollCount.value = Number(userCountResult.value.data.userCount || 0)
  }
}

function goArticles() { router.push('/articles') }
function goInterview() { router.push('/interview') }
function openArticle(articleId) { router.push(`/articles/${articleId}`) }

function resolveArticleLabel(category) {
  const labelMap = { frontend: '前端工程', backend: '后端架构', vip: '深度专题' }
  return labelMap[category] || '技术文章'
}

function getArticleTags(article) {
  if (article.tags && article.tags.length) return article.tags.slice(0, 2)
  const tag = resolveArticleLabel(article.category)
  return [tag]
}

function tagColorClass(tag) {
  const map = {
    '前端工程': 'tc-blue',
    '后端架构': 'tc-orange',
    '深度专题': 'tc-green',
    'Vue 3': 'tc-green',
    'React': 'tc-cyan',
    'Node.js': 'tc-green',
    'TypeScript': 'tc-blue',
    'Docker': 'tc-blue',
    'DevOps': 'tc-red',
    '性能优化': 'tc-purple',
    'SSG': 'tc-green',
    'Redis': 'tc-red'
  }
  return map[tag] || 'tc-cyan'
}

function resolveCoverUrl(coverUrl) {
  return coverUrl || '/peakstars-blog-icon.jpg'
}

function formatReadTime(value) {
  const text = String(value || '').trim()
  const minuteMatch = text.match(/(\d+)/)
  return minuteMatch ? `${minuteMatch[1]}分钟` : '1分钟'
}

function formatShortDateCN(value) {
  const text = String(value || '')
  const match = text.match(/(\d{4})-(\d{2})-(\d{2})/)
  if (!match) return text || '--'
  return `${match[1]}-${match[2]}-${match[3]}`
}

onMounted(() => { loadHomeData() })
</script>

<style scoped src="../styles/views/HomeView.css"></style>

