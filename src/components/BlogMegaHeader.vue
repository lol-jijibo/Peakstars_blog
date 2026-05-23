<template>
  <header class="site-header">
    <div class="site-header-inner">
      <button class="site-brand" type="button" @click="goHome">
        <img class="site-brand-mark" src="/qq.jpg" alt="PeakStars_blog 头像" />
        <div class="site-brand-copy">
          <span class="site-brand-name">PeakStars_blog</span>
          <span class="site-brand-sub">技术博客与面经论坛</span>
        </div>
      </button>

      <nav ref="siteNavRef" class="site-nav">
        <button
          v-for="nav in topNavs"
          :key="nav.key"
          class="site-nav-link"
          :class="{ active: isActiveNav(nav) }"
          type="button"
          @click="handleNavClick(nav)"
        >
          <span class="site-nav-link-content">
            <span>{{ nav.label }}</span>
          </span>
        </button>
      </nav>

      <div class="site-header-actions">
        <button
          v-if="currentPage === 'interview'"
          class="header-ghost-btn"
          type="button"
          @click="goHome"
        >
          返回首页
        </button>
        <button
          class="theme-toggle-btn"
          :class="{ 'is-dark': isDarkTheme }"
          type="button"
          :aria-label="isDarkTheme ? '切换到明亮主题' : '切换到暗色主题'"
          @click="themeStore.toggleTheme()"
        >
          <span class="theme-toggle-label">明</span>
          <span class="theme-toggle-track">
            <span class="theme-toggle-thumb">
              <span class="theme-toggle-icon">{{ isDarkTheme ? '☾' : '☀' }}</span>
            </span>
          </span>
          <span class="theme-toggle-label">暗</span>
        </button>
        <button class="header-primary-btn" type="button" @click="goMine">
          <img
            v-if="showHeaderAvatar"
            :src="headerAvatarUrl"
            alt="当前用户头像"
            class="header-user-avatar"
            @error="showHeaderAvatar = false"
          />
          <span v-else class="header-user-avatar header-user-avatar-fallback">我</span>
          我的
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useThemeStore } from '@/stores/theme'

const props = defineProps({
  currentPage: {
    type: String,
    default: 'home'
  }
})

const router = useRouter()
const route = useRoute()
const themeStore = useThemeStore()
const siteNavRef = ref(null)
const showHeaderAvatar = ref(true)
const headerAvatarUrl = '/qq.jpg'
const isDarkTheme = computed(() => themeStore.isDark.value)

const topNavs = [
  { key: 'home', label: 'Home', path: '/home' },
  { key: 'article', label: 'Tech Articles', path: '/articles' },
  { key: 'world', label: 'see world', path: '/world' },
  { key: 'interview', label: 'Interview', path: '/interview' }
]

function isArticleRoute() {
  return route.path === '/articles' || route.path.startsWith('/articles/')
}

function isActiveNav(nav) {
  if (nav.key === 'article') {
    return isArticleRoute()
  }

  if (nav.key === 'home') {
    return props.currentPage === 'home' && !isArticleRoute()
  }

  if (nav.key === 'world') {
    return props.currentPage === 'world'
  }

  if (nav.key === 'interview') {
    return props.currentPage === 'interview'
  }

  return false
}

function handleNavClick(nav) {
  router.push(nav.path)
}

function goHome() {
  router.push('/home')
}

function goMine() {
  router.push('/mine')
}
</script>

<style scoped src="../styles/components/BlogMegaHeader.css"></style>
