<template>
  <div class="learning-detail-page">
    <blog-mega-header current-page="home" />

    <main class="learning-detail-main">
      <button class="learning-detail-back" type="button" @click="goBack">
        ← 返回上一页
      </button>

      <div v-if="loading" class="learning-detail-shell learning-detail-shell--skeleton" aria-hidden="true">
        <section class="learning-detail-hero">
          <div class="learning-detail-copy learning-detail-skeleton-block">
            <div class="learning-detail-skeleton-chip"></div>
            <div class="learning-detail-skeleton-title"></div>
            <div class="learning-detail-skeleton-title short"></div>
            <div class="learning-detail-skeleton-paragraph"></div>
            <div class="learning-detail-skeleton-paragraph wide"></div>
            <div class="learning-detail-skeleton-tags">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>

          <div class="learning-detail-cover learning-detail-skeleton-block"></div>
        </section>

        <section class="learning-detail-stats learning-detail-skeleton-stats">
          <div></div>
          <div></div>
          <div></div>
        </section>

        <section class="learning-detail-content-grid">
          <aside class="learning-detail-outline learning-detail-skeleton-block"></aside>
          <section class="learning-detail-article learning-detail-skeleton-block"></section>
        </section>
      </div>

      <div v-else-if="error" class="learning-detail-state error">{{ error }}</div>

      <article v-else class="learning-detail-shell learning-detail-shell--ready">
        <section class="learning-detail-hero">
          <div class="learning-detail-copy">
            <span class="learning-detail-type">{{ routeDetail.routeType }}</span>
            <h1>{{ routeDetail.title }}</h1>
            <p>{{ routeDetail.summary }}</p>

            <div class="learning-detail-meta">
              <span>{{ routeDetail.publishedAt }}</span>
              <span>{{ routeDetail.estimatedMinutes }} min</span>
              <span>{{ routeDetail.difficulty }}</span>
              <span>{{ routeDetail.stageCount }} 阶段</span>
            </div>
          </div>

          <img :src="routeDetail.coverUrl" :alt="routeDetail.title" class="learning-detail-cover" />
        </section>

        <section class="learning-detail-stats" aria-label="路线数据">
          <div>
            <strong>{{ formatCount(routeDetail.viewCount) }}</strong>
            <span>阅读人数</span>
          </div>
          <div>
            <strong>{{ formatCount(routeDetail.commentCount) }}</strong>
            <span>评论人数</span>
          </div>
          <div>
            <strong>{{ formatCount(routeDetail.likeCount) }}</strong>
            <span>点赞人数</span>
          </div>
        </section>

        <section class="learning-detail-content-grid">
          <aside class="learning-detail-outline">
            <h2>路线阶段</h2>
            <ol>
              <li v-for="stage in routeDetail.stages" :key="stage">{{ stage }}</li>
            </ol>
            <div class="learning-detail-tags">
              <span v-for="tag in routeDetail.tags" :key="tag">{{ tag }}</span>
            </div>
          </aside>

          <section class="learning-detail-article">
            <h2>文章详情</h2>
            <p v-for="paragraph in contentParagraphs" :key="paragraph">{{ paragraph }}</p>
          </section>
        </section>
      </article>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BlogMegaHeader from '@/components/BlogMegaHeader.vue'
import {
  getCachedLearningRouteDetail,
  getLearningRouteDetail,
  primeLearningRouteDetail
} from '@/api/learningRoute'

const route = useRoute()
const router = useRouter()
const routeDetail = ref(getCachedLearningRouteDetail(route.params.slug))
const loading = ref(!routeDetail.value)
const error = ref('')

const fallbackDetails = {
  'java-backend-roadmap': {
    slug: 'java-backend-roadmap',
    routeType: 'java',
    title: 'Java 后端工程师成长路线',
    summary: '从 Java 基础、集合并发、JVM 到 Spring Boot、MyBatis、Redis、MySQL 与微服务实战，按阶段建立可落地的后端能力树。',
    coverUrl: '/【哲风壁纸】xiaomiyu7-小米suv.png',
    content: '这条路线适合希望系统进入 Java 后端方向的学习者。前期重点是 Java 语法、面向对象、集合、异常、IO 与并发基础；中期进入 JVM、MySQL、Redis、Spring Boot、MyBatis 和接口设计；后期通过权限系统、内容系统、缓存优化、异步任务、日志监控和部署发布，把知识串成真实工程能力。学习时不要只追求“看过”，更要把每个阶段沉淀成一个可运行的小项目。',
    stages: ['Java 基础与面向对象', '集合、泛型、异常与 IO', '多线程、线程池与 JVM 基础', 'MySQL 建模与 SQL 优化', 'Spring Boot + MyBatis 接口开发', 'Redis 缓存与登录鉴权', '微服务、部署、日志与监控'],
    tags: ['Java', 'Spring Boot', 'MyBatis', 'MySQL', 'Redis'],
    publishedAt: '2026-04-23 10:00',
    estimatedMinutes: 12,
    difficulty: '进阶路线',
    stageCount: 7,
    viewCount: 2846,
    commentCount: 36,
    likeCount: 528
  },
  'fullstack-roadmap': {
    slug: 'fullstack-roadmap',
    routeType: 'fullstack',
    title: '全栈开发者进阶路线',
    summary: '从 Vue3 前端体验、Java API 服务、MySQL 数据建模、部署监控和 AI 工具链为主线，串起从页面到系统的完整交付能力。',
    coverUrl: '/【哲风壁纸】夏日-晴天-氛围感.png',
    content: '全栈路线的核心不是“什么都会一点”，而是理解一次完整交付如何发生：从页面设计、组件拆分、状态管理，到 Java 后端 API、MySQL 数据结构、权限认证、部署上线和监控复盘。建议每学一个阶段都围绕同一个产品原型迭代，让前端、后端、数据库和工程化自然连接起来。',
    stages: ['HTML/CSS/JavaScript 体验基础', 'Vue3 组件、路由与状态管理', 'Java REST API 与分层架构', 'MySQL 表设计与 MyBatis 持久化', '登录鉴权、文件上传与内容管理', '前后端联调、环境配置与部署', '性能优化、监控日志与故障复盘', 'AI Coding 辅助开发工作流'],
    tags: ['Vue3', 'Java API', '全栈', '部署', 'AI Coding'],
    publishedAt: '2026-04-23 11:20',
    estimatedMinutes: 15,
    difficulty: '系统路线',
    stageCount: 8,
    viewCount: 1938,
    commentCount: 24,
    likeCount: 416
  }
}

const routeCoverMap = {
  'java-backend-roadmap': '/【哲风壁纸】xiaomiyu7-小米suv.png',
  'fullstack-roadmap': '/【哲风壁纸】夏日-晴天-氛围感.png'
}

const contentParagraphs = computed(() => {
  if (!routeDetail.value?.content) {
    return []
  }

  return routeDetail.value.content
    .split(/\n+/)
    .map((item) => item.trim())
    .filter(Boolean)
})

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

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }

  router.push({
    path: '/home',
    hash: '#learning-route-bottom-anchor'
  })
}

onMounted(async () => {
  if (routeDetail.value) {
    return
  }

  try {
    const detail = await getLearningRouteDetail(route.params.slug)
    routeDetail.value = {
      ...detail,
      coverUrl: routeCoverMap[detail.slug] || detail.coverUrl
    }
    primeLearningRouteDetail(route.params.slug, routeDetail.value)
  } catch (err) {
    routeDetail.value = fallbackDetails[route.params.slug] || null
    if (routeDetail.value) {
      primeLearningRouteDetail(route.params.slug, routeDetail.value)
    } else {
      error.value = err.message || '路线详情加载失败，请稍后重试。'
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped src="../styles/views/LearningRouteDetail.css"></style>
