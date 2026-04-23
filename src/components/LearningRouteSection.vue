<template>
  <section class="learning-route-section" aria-labelledby="learning-route-title">
    <div class="learning-route-head">
      <h2 id="learning-route-title">学习路线</h2>
      <p>点击一个小方块，进入完整路线与文章详情。</p>
    </div>

    <div v-if="loading" class="learning-route-state">正在加载学习路线...</div>

    <div v-else class="learning-route-grid">
      <article
        v-for="route in routes"
        :key="route.id"
        class="learning-route-card"
        tabindex="0"
        @click="goDetail(route.slug)"
        @keyup.enter="goDetail(route.slug)"
        @keyup.space="goDetail(route.slug)"
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
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getLearningRoutes } from '@/api/learningRoute'

const router = useRouter()
const routes = ref([])
const loading = ref(true)

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

function goDetail(slug) {
  router.push(`/learning-route/${slug}`)
}

onMounted(async () => {
  try {
    const remoteRoutes = await getLearningRoutes()
    routes.value = remoteRoutes.map((route) => ({
      ...route,
      coverUrl: routeCoverMap[route.slug] || route.coverUrl
    }))
  } catch (err) {
    routes.value = fallbackRoutes
  } finally {
    loading.value = false
  }
})
</script>

<style scoped src="../styles/components/LearningRouteSection.css"></style>
