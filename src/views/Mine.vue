<template>
  <div class="mine-page">
    <header class="mine-header">
      <div class="header-inner">
        <span class="logo">PeakStars_blog</span>
        <span class="header-title">我的</span>
      </div>
    </header>

    <main class="mine-main">
      <section class="profile-card">
        <div class="profile-avatar">
          <img
            v-if="showAvatarImage"
            :src="avatarUrl"
            alt="当前用户头像"
            class="profile-avatar-image"
            @error="showAvatarImage = false"
          />
          <span v-else class="profile-avatar-fallback">{{ avatarText }}</span>
        </div>
        <div class="profile-copy">
          <h1>{{ displayName }}</h1>
          <p>{{ profileSubtitle }}</p>
          <span class="joined-text">{{ joinedLabel }}</span>
        </div>
      </section>

      <section class="quick-panel">
        <button
          v-for="item in quickEntries"
          :key="item.key"
          class="quick-entry"
          type="button"
          @click="handleQuickEntry(item)"
        >
          <span class="quick-icon">{{ item.icon }}</span>
          <strong>{{ item.label }}</strong>
          <small>{{ item.caption }}</small>
          <span class="quick-count">{{ item.count }}</span>
        </button>
      </section>

      <section id="following-section" class="content-section">
        <div class="section-head">
          <div>
            <h2>我的关注</h2>
            <p>经常看的作者和方向都放在这里。</p>
          </div>
        </div>

        <div class="follow-list">
          <article
            v-for="author in followingAuthors"
            :key="author.id"
            class="follow-card"
          >
            <div class="follow-avatar" :style="{ background: author.accent }">
              {{ author.initials }}
            </div>
            <div class="follow-copy">
              <strong>{{ author.name }}</strong>
              <p>{{ author.subtitle }}</p>
            </div>
          </article>
        </div>
      </section>

      <section id="recent-section" class="content-section">
        <div class="section-head">
          <div>
            <h2>最近浏览</h2>
            <p>继续阅读你最近看过的内容。</p>
          </div>
          <button class="section-link" type="button" @click="router.push('/articles')">
            查看更多
          </button>
        </div>

        <div class="recent-list">
          <button
            v-for="article in recentArticles"
            :key="article.id"
            class="recent-card"
            type="button"
            @click="router.push('/articles')"
          >
            <span class="recent-tag">{{ article.category === 'frontend' ? '前端' : '后端' }}</span>
            <strong>{{ article.title }}</strong>
            <p>{{ article.summary }}</p>
            <span class="recent-meta">{{ article.author.name }} · {{ article.readTime }}</span>
          </button>
        </div>
      </section>

      <button class="logout-btn" type="button" @click="handleLogout">
        退出登录
      </button>
    </main>

    <nav class="tab-bar">
      <router-link
        v-for="tab in navTabs"
        :key="tab.path"
        :to="tab.path"
        class="tab-item"
        :class="{ active: $route.path === tab.path }"
      >
        <span class="tab-icon">{{ tab.icon }}</span>
        <span class="tab-label">{{ tab.label }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { recommendedAuthors, techArticles } from '@/data/techArticles'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const avatarUrl = '/qq.jpg'
const showAvatarImage = ref(true)

const navTabs = [
  { path: '/interview', icon: '📘', label: '面经' },
  { path: '/collect', icon: '⭐', label: '收藏' },
  { path: '/like', icon: '❤️', label: '点赞' },
  { path: '/mine', icon: '👤', label: '我的' }
]

const followingAuthors = recommendedAuthors.slice(0, 3)
const recentArticles = techArticles.filter((article) => article.inHistory).slice(0, 3)

const currentUser = computed(() => authStore.currentUser.value)
const collectedCount = techArticles.filter((article) => article.isCollected).length
const likedCount = techArticles.filter((article) => article.isLiked).length
const recentCount = techArticles.filter((article) => article.inHistory).length

const displayName = computed(() => {
  const user = currentUser.value
  return user?.username || user?.email?.split('@')[0] || 'PeakStars 用户'
})

const profileSubtitle = computed(() => {
  return currentUser.value?.email || '在这里沉淀收藏、点赞和最近看过的内容。'
})

const avatarText = computed(() => {
  const source = displayName.value.trim()
  return source ? source.slice(0, 1).toUpperCase() : '我'
})

const joinedLabel = computed(() => {
  if (!currentUser.value?.joinedAt) {
    return '欢迎回来，继续今天的阅读。'
  }

  return `加入时间 ${formatDate(currentUser.value.joinedAt)}`
})

const quickEntries = [
  {
    key: 'collect',
    icon: '⭐',
    label: '我的收藏',
    caption: '查看收藏的内容',
    count: collectedCount,
    action: () => router.push('/collect')
  },
  {
    key: 'like',
    icon: '❤️',
    label: '点赞的文章',
    caption: '回看点过赞的内容',
    count: likedCount,
    action: () => router.push('/like')
  },
  {
    key: 'follow',
    icon: '👥',
    label: '我的关注',
    caption: '查看关注的作者',
    count: followingAuthors.length,
    action: () => scrollToSection('following-section')
  },
  {
    key: 'recent',
    icon: '🕘',
    label: '最近浏览',
    caption: '继续上次浏览的位置',
    count: recentCount,
    action: () => scrollToSection('recent-section')
  }
]

function handleQuickEntry(item) {
  item.action()
}

function handleLogout() {
  authStore.logout()
  router.replace('/login')
}

function scrollToSection(id) {
  document.getElementById(id)?.scrollIntoView({
    behavior: 'smooth',
    block: 'start'
  })
}

function formatDate(value) {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
    date.getDate()
  ).padStart(2, '0')}`
}
</script>

<style scoped src="../styles/views/Mine.css"></style>
