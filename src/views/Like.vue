<template>
  <div class="simple-page">
    <header class="header">
      <div class="header-inner">
        <span class="logo">PeakStars_blog</span>
      </div>
    </header>

    <template v-if="likedArticles.length">
      <div class="article-list">
        <button
          v-for="article in likedArticles"
          :key="article.id"
          class="article-card"
          type="button"
          @click="router.push(`/articles/${article.id}`)"
        >
          <div class="article-cover" v-if="article.coverUrl">
            <img :src="article.coverUrl" :alt="article.title" @error="e => e.target.src = '/peakstars-blog-icon.svg'" />
          </div>
          <div class="article-body">
            <span class="article-tag">{{ article.category === 'frontend' ? '前端' : '后端' }}</span>
            <strong>{{ article.title }}</strong>
            <p>{{ article.summary }}</p>
            <span class="article-meta">{{ article.author?.name || 'PeakStars' }} · {{ article.readTime }}</span>
          </div>
        </button>
      </div>
    </template>

    <div v-else class="empty-state">
      <div class="empty-icon">❤️</div>
      <div class="empty-title">还没有喜欢的内容</div>
      <div class="empty-sub">点赞你喜欢的文章，它们将出现在这里</div>
      <router-link to="/articles" class="go-btn">去看看</router-link>
    </div>

    <nav class="tab-bar">
      <router-link v-for="tab in navTabs" :key="tab.path" :to="tab.path" class="tab-item" :class="{ active: $route.path === tab.path }">
        <span class="tab-icon">{{ tab.icon }}</span>
        <span class="tab-label">{{ tab.label }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getTechArticles } from '@/api/content'

const router = useRouter()
const likedArticles = ref([])

const navTabs = [
  { path: '/interview', icon: '📋', label: '面经' },
  { path: '/collect', icon: '⭐', label: '收藏' },
  { path: '/like', icon: '❤️', label: '喜欢' },
  { path: '/mine', icon: '👤', label: '我的' }
]

onMounted(async () => {
  try {
    const data = await getTechArticles({ category: 'like', pageSize: 999 })
    likedArticles.value = Array.isArray(data?.list) ? data.list : []
  } catch {
    likedArticles.value = []
  }
})
</script>
<style scoped src="../styles/views/SimplePage.css"></style>
