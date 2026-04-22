<template>
  <div class="page-layout">
    <!-- 登录成功后的过渡加载层：展示论坛进入提示与动态进度 -->
    <transition name="forum-loader-fade">
      <div v-if="showEntryLoader" class="forum-entry-loader">
        <div class="forum-entry-card">
          <div class="forum-entry-kicker">Peakstars Forum</div>
          <h2 class="forum-entry-title">正在进入Peakstars的技术论坛</h2>
          <p class="forum-entry-subtitle">
            请稍候，系统正在自动加载论坛内容，并实时展示当前进度。
          </p>

          <div class="forum-entry-progress-shell">
            <div class="forum-entry-progress-bar" :style="{ width: `${entryProgress}%` }"></div>
          </div>

          <div class="forum-entry-progress-meta">
            <span>{{ entryStatusText }}</span>
            <strong>{{ entryProgress }}%</strong>
          </div>
        </div>
      </div>
    </transition>

    <header class="header">
      <div class="header-inner">
        <span class="logo">PeakStars_blog</span>
        <div class="category-tabs">
          <span
            v-for="tab in tabs"
            :key="tab.key"
            class="cat-tab"
            :class="{ active: activeCategory === tab.key }"
            @click="activeCategory = tab.key"
          >{{ tab.label }}</span>
        </div>
        <button class="login-back-btn" @click="backToLogin">退出登录</button>
      </div>
    </header>

    <main class="main-content">
      <div class="search-bar">
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索面经..."
          class="search-input"
        />
      </div>

      <div class="interview-list">
        <div v-if="loading" class="loading-tip">
          <span class="loading-spin">○</span> 加载中...
        </div>

        <div v-else-if="error" class="error-tip">
          <p>{{ error }}</p>
          <button @click="fetchList" class="retry-btn">重试</button>
        </div>

        <template v-else>
          <interview-card
            v-for="item in list"
            :key="item.id"
            :data="item"
            @click="goDetail(item.id)"
          />

          <div v-if="list.length === 0" class="empty-tip">
            <span>暂无相关面经</span>
          </div>
        </template>
      </div>
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
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getInterviews } from '@/api/interview.js'
import InterviewCard from '@/components/InterviewCard.vue'
import { useAuthStore } from '@/stores/auth'

const ENTRY_LOADER_FLAG = 'peakstars_interview_entry_loader_needed'
const ENTRY_IDLE_MS = 180000
const ACTIVITY_EVENTS = ['pointerdown', 'pointermove', 'keydown', 'scroll', 'touchstart']

const router = useRouter()
const authStore = useAuthStore()
const keyword = ref('')
const activeCategory = ref('all')

// 首屏先展示加载页，形成登录页 -> 加载页 -> 主页的自然视觉过渡
const showEntryLoader = ref(true)
const entryLoadingStarted = ref(false)
const entryProgress = ref(0)
const dataLoaded = ref(false)

const list = ref([])
const loading = ref(false)
const error = ref('')

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'frontend', label: '前端' },
  { key: 'java', label: 'Java后端' }
]

const navTabs = [
  { path: '/interview', icon: '📚', label: '面经' },
  { path: '/collect', icon: '⭐', label: '收藏' },
  { path: '/like', icon: '❤️', label: '喜欢' },
  { path: '/mine', icon: '👤', label: '我的' }
]

let debounceTimer = 0
let entryProgressTimer = 0
let entryIdleTimer = 0

// 根据进度阶段切换提示文案，让加载过程更有“正在进入论坛”的展示感
const entryStatusText = computed(() => {
  if (!entryLoadingStarted.value) return '等待开始'
  if (entryProgress.value < 35) return '正在初始化论坛界面...'
  if (entryProgress.value < 70) return '正在同步技术帖与分类数据...'
  if (entryProgress.value < 100) return '正在准备进入论坛...'
  return '进入完成'
})

function shouldShowEntryLoader() {
  return sessionStorage.getItem(ENTRY_LOADER_FLAG) !== 'false'
}

function markEntryLoaderNeeded() {
  sessionStorage.setItem(ENTRY_LOADER_FLAG, 'true')
}

function markEntryLoaderHandled() {
  sessionStorage.setItem(ENTRY_LOADER_FLAG, 'false')
}

async function fetchList() {
  loading.value = true
  error.value = ''

  try {
    const result = await getInterviews({
      category: activeCategory.value,
      keyword: keyword.value.trim()
    })
    list.value = result.list
  } catch (e) {
    error.value = '数据加载失败，请检查后端服务是否已启动'
    list.value = []
  } finally {
    loading.value = false
  }
}

function clearEntryProgressTimer() {
  if (entryProgressTimer) {
    window.clearInterval(entryProgressTimer)
    entryProgressTimer = 0
  }
}

function clearEntryIdleTimer() {
  if (entryIdleTimer) {
    window.clearTimeout(entryIdleTimer)
    entryIdleTimer = 0
  }
}

// 用户长时间停留但没有操作时，仅为下次进入列表页重新打开加载动画
function scheduleLoaderForIdleReturn() {
  clearEntryIdleTimer()
  entryIdleTimer = window.setTimeout(() => {
    markEntryLoaderNeeded()
  }, ENTRY_IDLE_MS)
}

function recordPageActivity() {
  if (showEntryLoader.value) return
  markEntryLoaderHandled()
  scheduleLoaderForIdleReturn()
}

function bindActivityListeners() {
  ACTIVITY_EVENTS.forEach((eventName) => {
    window.addEventListener(eventName, recordPageActivity, { passive: true })
  })
}

function unbindActivityListeners() {
  ACTIVITY_EVENTS.forEach((eventName) => {
    window.removeEventListener(eventName, recordPageActivity)
  })
}

// 加载完成后补齐到 100%，再淡出进入主页内容
function finishEntryLoading() {
  clearEntryProgressTimer()
  entryProgress.value = 100

  window.setTimeout(() => {
    showEntryLoader.value = false
    markEntryLoaderHandled()
    scheduleLoaderForIdleReturn()
  }, 180)
}

// 自动驱动进入动画和数据请求，让用户无需点击即可进入主页
function startEntryLoading() {
  if (entryLoadingStarted.value) return

  entryLoadingStarted.value = true
  entryProgress.value = 0
  dataLoaded.value = false

  fetchList().finally(() => {
    dataLoaded.value = true

    if (entryProgress.value >= 92) {
      finishEntryLoading()
    }
  })

  clearEntryProgressTimer()
  entryProgressTimer = window.setInterval(() => {
    const maxProgress = dataLoaded.value ? 100 : 92
    const step = entryProgress.value < 60 ? 14 : entryProgress.value < 85 ? 8 : 3

    entryProgress.value = Math.min(entryProgress.value + step, maxProgress)

    if (dataLoaded.value && entryProgress.value >= 100) {
      finishEntryLoading()
    }
  }, 120)
}

async function initInterviewPage() {
  if (shouldShowEntryLoader()) {
    startEntryLoading()
    return
  }

  showEntryLoader.value = false
  markEntryLoaderHandled()
  await fetchList()
  scheduleLoaderForIdleReturn()
}

watch(activeCategory, () => {
  if (showEntryLoader.value) return
  fetchList()
})

watch(keyword, () => {
  if (showEntryLoader.value) return

  clearTimeout(debounceTimer)
  debounceTimer = window.setTimeout(fetchList, 500)
})

function goDetail(id) {
  router.push(`/interview/${id}`)
}

function backToLogin() {
  // 退出后下次重新登录，仍然展示一次进入论坛的加载过渡
  markEntryLoaderNeeded()
  authStore.logout()
  router.replace('/login')
}

onBeforeUnmount(() => {
  clearTimeout(debounceTimer)
  clearEntryProgressTimer()
  clearEntryIdleTimer()
  unbindActivityListeners()
})

onMounted(() => {
  // 页面挂载后初始化列表页，并根据会话状态决定是否展示加载过渡
  bindActivityListeners()
  initInterviewPage()
})
</script>

<style scoped src="../styles/views/InterviewList.css"></style>
