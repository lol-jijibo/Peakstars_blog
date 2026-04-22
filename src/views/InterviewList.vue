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


// 记录“下次进入面经列表时是否需要再次显示进入加载页”的会话标记
const ENTRY_LOADER_FLAG = 'peakstars_interview_entry_loader_needed'
// 用户在列表页无操作 20 分钟后，下一次再进入列表时重新触发加载过渡
const ENTRY_IDLE_MS = 1200000
// 这些事件都视为用户仍在活跃浏览列表页，用来重置空闲计时
const ACTIVITY_EVENTS = ['pointerdown', 'pointermove', 'keydown', 'scroll', 'touchstart']
// 页面跳转控制：进入详情、返回登录、返回列表等业务都会用到
const router = useRouter()
// 登录态控制：退出登录时需要清空认证状态
const authStore = useAuthStore()
// 列表页搜索关键词，控制面经内容筛选
const keyword = ref('')
// 当前选中的分类标签，用于切换“全部 / 前端 / Java后端”列表
const activeCategory = ref('all')

// 首屏先展示加载页，形成登录页 -> 加载页 -> 主页的自然视觉过渡
// 是否显示进入论坛的全屏加载层
const showEntryLoader = ref(true)
// 加载过渡是否已经启动，避免重复触发同一轮进入动画
const entryLoadingStarted = ref(false)
// 进入加载条当前的进度百分比
const entryProgress = ref(0)
// 列表数据是否已经请求完成，用来决定进度条何时收尾
const dataLoaded = ref(false)

// 面经列表数据源，渲染主页卡片内容
const list = ref([])
// 列表接口请求中的加载状态，控制列表区域的“加载中”提示
const loading = ref(false)
// 列表接口请求失败时的错误提示文案
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
// 判断是否应该显示入口加载器
function shouldShowEntryLoader() {
  return sessionStorage.getItem(ENTRY_LOADER_FLAG) !== 'false'
}

// 标记需要显示入口加载器
function markEntryLoaderNeeded() {
  sessionStorage.setItem(ENTRY_LOADER_FLAG, 'true')
}

// 标记入口加载器已处理完毕
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

// 清理加载进度条的定时器，避免重复推进进度或页面离开后继续执行
function clearEntryProgressTimer() {
  if (entryProgressTimer) {
    window.clearInterval(entryProgressTimer)
    entryProgressTimer = 0
  }
}

// 清理“空闲后重新允许加载”的计时器，避免重复注册多个空闲倒计时
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

// 记录用户仍在浏览列表页：本次会话内不重复加载，同时重新开始空闲计时
function recordPageActivity() {
  if (showEntryLoader.value) return
  markEntryLoaderHandled()
  scheduleLoaderForIdleReturn()
}

// 给列表页绑定用户活动事件，用来判断用户是否长时间停留未操作
function bindActivityListeners() {
  ACTIVITY_EVENTS.forEach((eventName) => {
    window.addEventListener(eventName, recordPageActivity, { passive: true })
  })
}

// 页面离开时解绑活动监听，避免事件残留影响后续页面
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

// 初始化面经列表页：决定本次进入是直接展示列表，还是先走一次加载过渡
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

// 用户切换分类时立即刷新列表；如果还在加载过渡中则不提前触发请求
watch(activeCategory, () => {
  if (showEntryLoader.value) return
  fetchList()
})

// 搜索词变化后延迟请求，避免输入时频繁刷新列表
watch(keyword, () => {
  if (showEntryLoader.value) return

  clearTimeout(debounceTimer)
  debounceTimer = window.setTimeout(fetchList, 500)
})

// 进入某一篇面经详情页
function goDetail(id) {
  router.push(`/interview/${id}`)
}

function backToLogin() {
  // 退出后下次重新登录，仍然展示一次进入论坛的加载过渡
  markEntryLoaderNeeded()
  authStore.logout()
  router.replace('/login')
}

// 页面销毁时统一清理计时器和事件监听，避免列表页副作用残留
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
