<template>
  <header class="site-header" @keydown.esc="closeMegaMenu">
    <div class="site-header-inner">
      <button class="site-brand" type="button" @click="goHome">
        <div class="site-brand-mark">PS</div>
        <div class="site-brand-copy">
          <span class="site-brand-name">PeakStars_blog</span>
          <span class="site-brand-sub">技术博客与面经论坛</span>
        </div>
      </button>

      <nav
        ref="siteNavRef"
        class="site-nav"
        @mouseenter="cancelCloseMegaMenu"
        @mouseleave="scheduleCloseMegaMenu"
        @focusout="handleNavFocusOut"
      >
        <button
          v-for="nav in topNavs"
          :key="nav.key"
          class="site-nav-link"
          :class="{
            active: isActiveNav(nav),
            'is-open': nav.type === 'mega' && activeMegaMenu === nav.key
          }"
          :aria-expanded="nav.type === 'mega' ? String(activeMegaMenu === nav.key) : undefined"
          :aria-haspopup="nav.type === 'mega' ? 'true' : undefined"
          type="button"
          @mouseenter="handleNavEnter(nav)"
          @mouseleave="handleNavLeave(nav)"
          @focus="handleNavEnter(nav)"
          @click="handleNavClick(nav)"
        >
          <span class="site-nav-link-content">
            <span>{{ nav.label }}</span>
            <span v-if="nav.type === 'mega'" class="site-nav-link-caret">▾</span>
          </span>
        </button>

        <transition name="mega-menu-fade">
          <div
            v-if="activeMegaMenu === 'article'"
            class="mega-menu"
            @mouseenter="cancelCloseMegaMenu"
            @mouseleave="scheduleCloseMegaMenu"
          >
            <aside class="mega-menu-side">
              <div
                v-for="group in articleGroups"
                :key="group.key"
                class="mega-menu-side-item"
              >
                <button
                  class="mega-menu-side-link"
                  :class="{ active: activeMegaGroup === group.key }"
                  type="button"
                  @mouseenter="selectMegaGroup(group.key)"
                  @focus="selectMegaGroup(group.key)"
                  @click="handleMegaGroupClick(group)"
                >
                  <span class="mega-menu-side-copy">
                    <span class="mega-menu-side-title">{{ group.label }}</span>
                    <small>{{ group.caption }}</small>
                  </span>
                  <span class="mega-menu-side-arrow" aria-hidden="true">›</span>
                </button>
              </div>
            </aside>

            <div class="mega-menu-panel">
              <div class="mega-menu-panel-head">
                <div class="mega-menu-panel-copy">
                  <div class="mega-menu-eyebrow">{{ currentMegaGroup.eyebrow }}</div>
                  <h3>{{ currentMegaGroup.title }}</h3>
                  <p>{{ currentMegaGroup.description }}</p>
                </div>

                <button
                  class="mega-menu-panel-cta"
                  type="button"
                  @click="openCurrentMegaGroup"
                >
                  {{ currentMegaGroup.actionLabel }}
                </button>
              </div>

              <div class="mega-menu-showcase">
                <button
                  v-for="preview in orderedMegaPreviews"
                  :key="preview.id"
                  class="mega-menu-preview-card"
                  :class="{ active: activePreviewId === preview.id }"
                  type="button"
                  @mouseenter="activePreviewId = preview.id"
                  @focus="activePreviewId = preview.id"
                  @click="activePreviewId = preview.id"
                >
                  <div class="mega-menu-preview-card-top">
                    <span class="mega-menu-preview-tag">{{ preview.tag }}</span>
                    <span class="mega-menu-preview-meta">{{ preview.meta }}</span>
                  </div>
                  <span class="mega-menu-preview-theme">{{ preview.theme }}</span>
                  <strong>{{ preview.title }}</strong>
                  <p>{{ preview.summary }}</p>
                </button>
              </div>
            </div>
          </div>
        </transition>
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
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTechArticles } from '@/api/content'
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
const activeMegaMenu = ref('')
const activeMegaGroup = ref('tech')
const hoveredNavKey = ref('')
const activePreviewId = ref('')
const techArticles = ref([])
let closeMenuTimer = null
const headerAvatarUrl = '/qq.jpg'
const isDarkTheme = computed(() => themeStore.isDark.value)

const topNavs = [
  { key: 'home', label: '首页', type: 'route', path: '/home' },
  { key: 'article', label: '文章', type: 'mega' },
  { key: 'world', label: '看天下', type: 'route', path: '/world' },
  { key: 'ai', label: 'AI 热点', type: 'route', path: '/ai-hotspot' },
  { key: 'interview', label: '面经', type: 'route', path: '/interview' }
]

const articleGroups = computed(() => {
  const techMegaHighlights = techArticles.value
    .filter((article) => article.featured)
    .slice(0, 4)

  return [
    {
      key: 'tech',
      label: '技术文章',
      caption: '前后端 / 架构',
      eyebrow: 'Featured Reading',
      title: '技术文章精华预览',
      description: techMegaHighlights.length
        ? '先看重点，再决定进入完整文章页。'
        : '技术文章精选内容正在从后端加载。',
      actionLabel: '查看更多',
      action: { type: 'route', path: '/articles' },
      previews: techMegaHighlights.map((article) => ({
        id: article.id,
        theme: article.category === 'frontend' ? '前端精华' : '后端精华',
        tag: article.isVip ? 'VIP 专题' : '精选文章',
        meta: `${article.author.name} · ${article.readTime}`,
        title: article.title,
        summary: article.essence,
        description: article.summary,
        points: article.highlights.slice(0, 3)
      }))
    }
  ]
})

const currentMegaGroup = computed(() => {
  return articleGroups.value.find((group) => group.key === activeMegaGroup.value) || articleGroups.value[0]
})

const currentMegaPreview = computed(() => {
  return (
    currentMegaGroup.value.previews.find((preview) => preview.id === activePreviewId.value) ||
    currentMegaGroup.value.previews[0]
  )
})

const orderedMegaPreviews = computed(() => {
  const activePreview = currentMegaPreview.value
  if (!activePreview) {
    return []
  }
  const restPreviews = currentMegaGroup.value.previews.filter((preview) => preview.id !== activePreview.id)

  return [activePreview, ...restPreviews]
})

watch(
  () => route.fullPath,
  () => {
    closeMegaMenu()
  }
)

watch(
  activeMegaGroup,
  (groupKey) => {
    const group = articleGroups.value.find((item) => item.key === groupKey)
    activePreviewId.value = group?.previews[0]?.id || ''
  },
  { immediate: true }
)

// 业务目的：头部文章下拉预览也统一切到后端数据源，避免导航和文章页出现两套内容口径。
// 业务逻辑：组件挂载后拉取一次技术文章列表，再从精选文章里裁剪出头部需要的预览卡片。
onMounted(async () => {
  try {
    techArticles.value = await getTechArticles()
  } catch {
    techArticles.value = []
  }
})

onBeforeUnmount(() => {
  cancelCloseMegaMenu()
})

function isArticleRoute() {
  return route.path === '/articles'
}

function isActiveNav(nav) {
  if (nav.key === 'home' && hoveredNavKey.value && hoveredNavKey.value !== 'home') {
    return false
  }

  if (nav.key === 'article') {
    return isArticleRoute()
  }

  if (nav.key === 'home') {
    return props.currentPage === 'home' && !isArticleRoute()
  }

  if (nav.key === 'world') {
    return props.currentPage === 'world'
  }

  if (nav.key === 'ai') {
    return props.currentPage === 'ai'
  }

  if (nav.key === 'interview') {
    return props.currentPage === 'interview'
  }

  return false
}

function selectMegaGroup(groupKey) {
  activeMegaGroup.value = groupKey
}

function handleMegaGroupClick(group) {
  selectMegaGroup(group.key)
  openMegaGroup(group)
}

function handleNavEnter(nav) {
  hoveredNavKey.value = nav.key
  cancelCloseMegaMenu()

  if (nav.type !== 'mega') {
    closeMegaMenu()
    return
  }

  activeMegaMenu.value = nav.key
}

function handleNavLeave(nav) {
  if (hoveredNavKey.value === nav.key) {
    hoveredNavKey.value = activeMegaMenu.value || ''
  }
}

function handleNavClick(nav) {
  if (nav.type === 'route') {
    closeMegaMenu()
    router.push(nav.path)
    return
  }

  activeMegaMenu.value = activeMegaMenu.value === nav.key ? '' : nav.key
}

function handleNavFocusOut(event) {
  const nextTarget = event.relatedTarget

  if (!nextTarget || !siteNavRef.value?.contains(nextTarget)) {
    scheduleCloseMegaMenu()
  }
}

function closeMegaMenu() {
  cancelCloseMegaMenu()
  activeMegaMenu.value = ''
  if (hoveredNavKey.value === 'article') {
    hoveredNavKey.value = ''
  }
}

function scheduleCloseMegaMenu() {
  cancelCloseMegaMenu()
  closeMenuTimer = window.setTimeout(() => {
    activeMegaMenu.value = ''
    closeMenuTimer = null
  }, 160)
}

function cancelCloseMegaMenu() {
  if (closeMenuTimer) {
    window.clearTimeout(closeMenuTimer)
    closeMenuTimer = null
  }
}

function openCurrentMegaGroup() {
  const action = currentMegaGroup.value.action
  closeMegaMenu()

  if (action.type === 'route') {
    router.push(action.path)
  }
}

function openMegaGroup(group) {
  const action = group.action
  closeMegaMenu()

  if (action.type === 'route') {
    router.push(action.path)
  }
}

function goHome() {
  hoveredNavKey.value = ''
  closeMegaMenu()
  router.push('/home')
}

function goMine() {
  hoveredNavKey.value = ''
  closeMegaMenu()
  router.push('/mine')
}
</script>

<style scoped src="../styles/components/BlogMegaHeader.css"></style>
