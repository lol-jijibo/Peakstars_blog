<template>
  <div class="interview-page">
    <blog-mega-header current-page="interview" />

    <!-- 登录后首次进入面经页时，展示论坛加载过渡与实时进度。 -->
    <transition name="forum-loader-fade">
      <div v-if="showEntryLoader" class="forum-entry-loader">
        <div class="forum-entry-card">
          <div class="forum-entry-kicker">Peakstars Forum</div>
          <h2 class="forum-entry-title">正在进入 Peakstars 的技术论坛</h2>
          <p class="forum-entry-subtitle">
            请稍候，系统正在自动加载面经内容，并同步展示当前进入进度。
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

    <main class="interview-main">
      <section class="interview-content">
        <div class="section-header">
          <div>
            <span class="section-kicker">Primary Module</span>
            <h2>面经列表</h2>
            <p>聚合前后端高频面试方向，方便后续持续补充与分类扩展。</p>
          </div>

          <div class="section-controls">
            <button class="back-home-btn" type="button" @click="goHome">
              返回主页
            </button>
          </div>
        </div>

        <div class="interview-module-body">
          <!-- 左侧改成目录式导航，统一承载搜索入口和前后端技术栈专题。 -->
          <aside class="topic-sidebar">
            <div class="topic-basket">
              <div class="topic-basket-head">
                <span class="topic-basket-kicker">Interview Basket</span>
                <h3>面试分类</h3>
                <p>按前后端方向归档技术栈与专题，后续可继续扩展具体面经内容。</p>
              </div>

              <div class="topic-search">
                <input
                  v-model="keyword"
                  type="text"
                  placeholder="搜索面经..."
                  class="search-input"
                />
              </div>

              <div class="topic-groups">
                <section
                  v-for="group in topicGroups"
                  :key="group.key"
                  class="topic-group"
                >
                  <button
                    class="topic-group-title"
                    :class="{ active: activeGroup === group.key }"
                    type="button"
                    @click="selectGroup(group)"
                  >
                    <span>{{ group.label }}</span>
                    <span class="topic-group-arrow">›</span>
                  </button>

                  <div class="topic-group-tags">
                    <button
                      v-for="topic in group.items"
                      :key="topic.key"
                      class="topic-pill"
                      :class="{ active: selectedTopic === topic.key }"
                      type="button"
                      @click="selectTopic(topic)"
                    >
                      {{ topic.label }}
                    </button>
                  </div>
                </section>
              </div>
            </div>
          </aside>

          <div class="interview-feed">
            <div v-if="loading" class="loading-tip">
              <span class="loading-spin">●</span> 加载中...
            </div>

            <div v-else-if="error" class="error-tip">
              <p>{{ error }}</p>
              <button @click="fetchList" class="retry-btn">重试</button>
            </div>

            <template v-else>
              <div class="interview-list">
                <interview-card
                  v-for="item in list"
                  :key="item.id"
                  :data="item"
                  @click="goDetail(item.id)"
                />
              </div>

              <div v-if="list.length === 0" class="empty-tip">
                <span>当前分类下还没有面经内容</span>
              </div>
            </template>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getInterviews } from '@/api/interview.js'
import InterviewCard from '@/components/InterviewCard.vue'
import BlogMegaHeader from '@/components/BlogMegaHeader.vue'

const ENTRY_LOADER_FLAG = 'peakstars_interview_entry_loader_needed'
const ENTRY_IDLE_MS = 1200000
const ACTIVITY_EVENTS = ['pointerdown', 'pointermove', 'keydown', 'scroll', 'touchstart']

const route = useRoute()
const router = useRouter()
const keyword = ref('')
const activeCategory = ref(typeof route.query.category === 'string' ? route.query.category : 'all')
const activeGroup = ref('backend')
const selectedTopic = ref(typeof route.query.category === 'string' ? route.query.category : 'all')

const showEntryLoader = ref(true)
const entryLoadingStarted = ref(false)
const entryProgress = ref(0)
const dataLoaded = ref(false)

const list = ref([])
const loading = ref(false)
const error = ref('')

// 左侧分类篮子用于承载面经方向，当前先把结构搭好，后面可以继续扩充子类与题库。
const topicGroups = [
  {
    key: 'backend',
    label: '后端',
    category: 'java',
    items: [
      { key: 'all', label: '全部方向', category: 'all' },
      { key: 'java', label: 'Java', category: 'java' },
      { key: 'python', label: 'Python', category: 'all' },
      { key: 'golang', label: 'Go', category: 'all' },
      { key: 'mysql', label: 'MySQL', category: 'java' },
      { key: 'redis', label: 'Redis', category: 'java' },
      { key: 'spring', label: 'Spring', category: 'java' },
      { key: 'springboot', label: 'Spring Boot', category: 'java' },
      { key: 'jvm', label: 'JVM', category: 'java' },
      { key: 'network', label: '计算机网络', category: 'all' },
      { key: 'os', label: '操作系统', category: 'all' }
    ]
  },
  {
    key: 'frontend',
    label: '前端',
    category: 'frontend',
    items: [
      { key: 'frontend', label: '前端总览', category: 'frontend' },
      { key: 'html', label: 'HTML', category: 'frontend' },
      { key: 'css', label: 'CSS', category: 'frontend' },
      { key: 'javascript', label: 'JavaScript', category: 'frontend' },
      { key: 'typescript', label: 'TypeScript', category: 'frontend' },
      { key: 'vue', label: 'Vue', category: 'frontend' },
      { key: 'react', label: 'React', category: 'frontend' },
      { key: 'engineering', label: '工程化', category: 'frontend' },
      { key: 'browser', label: '浏览器原理', category: 'frontend' },
      { key: 'performance', label: '性能优化', category: 'frontend' }
    ]
  }
]

let debounceTimer = 0
let entryProgressTimer = 0
let entryIdleTimer = 0

// 根据当前进度阶段反馈用户正在执行的进入动作，让加载过程更有感知。
const entryStatusText = computed(() => {
  if (!entryLoadingStarted.value) return '等待开始'
  if (entryProgress.value < 35) return '正在初始化论坛界面...'
  if (entryProgress.value < 70) return '正在同步面经列表与分类数据...'
  if (entryProgress.value < 100) return '正在准备进入面经页...'
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

// 进入动画结束后及时清理定时器，避免重复叠加更新进度。
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

function finishEntryLoading() {
  clearEntryProgressTimer()
  entryProgress.value = 100

  window.setTimeout(() => {
    showEntryLoader.value = false
    markEntryLoaderHandled()
    scheduleLoaderForIdleReturn()
  }, 180)
}

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

// 点击左侧分类时同步高亮状态，并复用现有主分类筛选能力。
function selectTopic(topic) {
  selectedTopic.value = topic.key
  activeCategory.value = topic.category
}

function selectGroup(group) {
  activeGroup.value = group.key
  activeCategory.value = group.category
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

watch(
  () => route.query.category,
  (nextCategory) => {
    if (typeof nextCategory === 'string') {
      activeCategory.value = nextCategory
      selectedTopic.value = nextCategory
      activeGroup.value = nextCategory === 'frontend' ? 'frontend' : 'backend'
      return
    }

    activeCategory.value = 'all'
    selectedTopic.value = 'all'
    activeGroup.value = 'backend'
  }
)

function goDetail(id) {
  router.push(`/interview/${id}`)
}

function goHome() {
  router.push('/home')
}

onBeforeUnmount(() => {
  clearTimeout(debounceTimer)
  clearEntryProgressTimer()
  clearEntryIdleTimer()
  unbindActivityListeners()
})

onMounted(() => {
  bindActivityListeners()
  initInterviewPage()
})
</script>

<style scoped src="../styles/views/InterviewList.css"></style>
