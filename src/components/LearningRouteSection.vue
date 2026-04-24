<template>
  <section id="learning-route-section" class="learning-route-section" aria-labelledby="learning-route-title">
    <div class="learning-route-head">
      <h2 id="learning-route-title">学习路线</h2>
      <p>点击一个小方块，进入完整路线与文章详情。</p>
    </div>

    <div v-if="showSkeleton" class="learning-route-grid learning-route-grid--loading" aria-hidden="true">
      <div
        v-for="item in skeletonCards"
        :key="item"
        class="learning-route-card learning-route-card--skeleton"
      >
        <div class="learning-route-skeleton-chip"></div>
        <div class="learning-route-skeleton-title"></div>
        <div class="learning-route-skeleton-title short"></div>
        <div class="learning-route-skeleton-meta"></div>
      </div>
    </div>

    <transition-group
      v-else
      :name="enableRevealAnimation ? 'learning-route-reveal' : 'learning-route-static'"
      tag="div"
      class="learning-route-grid"
      appear
    >
      <article
        v-for="route in routes"
        :key="route.id"
        class="learning-route-card is-interactive"
        tabindex="0"
        @mouseenter="warmRouteDetail(route.slug)"
        @focus="warmRouteDetail(route.slug)"
        @click="goDetail(route.slug)"
        @keyup.enter="goDetail(route.slug)"
        @keyup.space.prevent="goDetail(route.slug)"
      >
        <img class="learning-route-cover" :src="route.coverUrl" :alt="route.title" loading="lazy" />
        <div class="learning-route-overlay">
          <span class="learning-route-type">{{ route.routeType }}</span>
          <h3>{{ route.title }}</h3>
          <div class="learning-route-meta">
            <time>{{ route.publishedAt }}</time>
            <span>{{ route.difficulty }}</span>
          </div>
          <div class="learning-route-stats">
            <span>{{ formatCount(route.viewCount) }} 阅读</span>
            <span>{{ formatCount(route.commentCount) }} 评论</span>
            <span>{{ formatCount(route.likeCount) }} 点赞</span>
          </div>
        </div>
      </article>
    </transition-group>

    <div id="learning-route-bottom-anchor" class="learning-route-anchor" aria-hidden="true"></div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getLearningRoutes, prefetchLearningRouteDetail } from '@/api/learningRoute'

let cachedLearningRoutes = null
let learningRouteDetailViewPromise = null

const router = useRouter()
const routes = ref(cachedLearningRoutes ? [...cachedLearningRoutes] : [])
const loading = ref(!cachedLearningRoutes)
const enableRevealAnimation = ref(!cachedLearningRoutes)
const skeletonCards = [1, 2]

const fallbackRoutes = [
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

const routeCoverMap = {
  'java-backend-roadmap': '/【哲风壁纸】xiaomiyu7-小米suv.png',
  'fullstack-roadmap': '/【哲风壁纸】夏日-晴天-氛围感.png'
}

const showSkeleton = computed(() => loading.value && routes.value.length === 0)

function normalizeRoutes(routeList) {
  return routeList.map((route) => ({
    ...route,
    coverUrl: routeCoverMap[route.slug] || route.coverUrl
  }))
}

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

function preloadLearningRouteDetailView() {
  if (!learningRouteDetailViewPromise) {
    learningRouteDetailViewPromise = import('@/views/LearningRouteDetail.vue')
  }
  return learningRouteDetailViewPromise
}

function warmRouteDetail(slug) {
  preloadLearningRouteDetailView()
  prefetchLearningRouteDetail(slug)
}

async function goDetail(slug) {
  warmRouteDetail(slug)
  await router.push(`/learning-route/${slug}`)
}

onMounted(async () => {
  try {
    const remoteRoutes = normalizeRoutes(await getLearningRoutes())
    routes.value = remoteRoutes
    cachedLearningRoutes = remoteRoutes
    preloadLearningRouteDetailView()
    remoteRoutes.forEach((route) => {
      prefetchLearningRouteDetail(route.slug)
    })
  } catch (err) {
    if (!routes.value.length) {
      const safeFallback = normalizeRoutes(fallbackRoutes)
      routes.value = safeFallback
      cachedLearningRoutes = safeFallback
    }
  } finally {
    loading.value = false
    window.setTimeout(() => {
      enableRevealAnimation.value = false
    }, 180)
  }
})
</script>

<style scoped src="../styles/components/HomeCardSection.css"></style>
