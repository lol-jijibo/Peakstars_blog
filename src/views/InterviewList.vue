<template>
  <div class="interview-page">
    <blog-mega-header current-page="interview" />

    <!-- 首次进入面经页时，展示论坛式加载过渡与进入进度。 -->
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
        <!-- 页面主体：只保留分类和真实面经内容，不再单独放顶部“小区域”。 -->
        <div class="interview-module-body">
          <!-- 分类侧栏：承接搜索、分组和技术栈切换。 -->
          <aside class="topic-sidebar">
            <div class="topic-basket">
              <div class="topic-basket-head">
                <h3>面试分类</h3>
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
                    <span class="topic-group-arrow">></span>
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

          <!-- 内容区域：恢复数据库里的面经帖子卡片，并保持自适应排布。 -->
          <section class="interview-feed">
            <div class="interview-feed-head">
              <h2>面经帖子</h2>
              <p>当前展示各平台收集的面经帖子内容，包含个人心得和求职历程,欢迎采纳。</p>
            </div>

            <div v-if="loading" class="loading-tip">
              <span class="loading-spin"></span>
              <span>加载中...</span>
            </div>

            <div v-else-if="error" class="error-tip">
              <p>{{ error }}</p>
              <button class="retry-btn" type="button" @click="fetchList">重试</button>
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
          </section>
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

// 进入动效常量：控制首次进入和空闲回访时的加载覆盖层。
const ENTRY_LOADER_FLAG = 'peakstars_interview_entry_loader_needed'
const ENTRY_IDLE_MS = 1200000
const ACTIVITY_EVENTS = ['pointerdown', 'pointermove', 'keydown', 'scroll', 'touchstart']

// 路由与筛选状态：维护当前分类、当前分组和搜索关键词。
const route = useRoute()
const router = useRouter()
const initialCategory = typeof route.query.category === 'string' ? route.query.category : 'all'
const keyword = ref('')
const activeCategory = ref(initialCategory)
const activeGroup = ref(resolveGroupKey(initialCategory))
const selectedTopic = ref(initialCategory)

// 加载状态：用于控制进入动画和数据请求状态。
const showEntryLoader = ref(true)
const entryLoadingStarted = ref(false)
const entryProgress = ref(0)
const dataLoaded = ref(false)
const list = ref([])
const loading = ref(false)
const error = ref('')

// 分类配置：左侧用于控制数据库面经帖子的筛选入口。
const topicGroups = [
  {
    key: 'backend',
    label: '后端',
    category: 'java',
    defaultTopicKey: 'all',
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
    defaultTopicKey: 'frontend',
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

// 定时器引用：避免页面切换时遗留旧的计时器。
let debounceTimer = 0
let entryProgressTimer = 0
let entryIdleTimer = 0

// 进度提示：根据当前加载阶段更新覆盖层提示文案。
const entryStatusText = computed(() => {
  if (!entryLoadingStarted.value) return '等待开始'
  if (entryProgress.value < 35) return '正在初始化论坛界面...'
  if (entryProgress.value < 70) return '正在同步面经列表与分类数据...'
  if (entryProgress.value < 100) return '正在准备进入面经页...'
  return '进入完成'
})

// 分类映射：把 query 里的类别映射到左侧前后端分组。
function resolveGroupKey(category) {
  return category === 'frontend' ? 'frontend' : 'backend'
}

function syncCategoryState(category) {
  activeCategory.value = category
  selectedTopic.value = category
  activeGroup.value = resolveGroupKey(category)
}

// 进入动画记录：通过 sessionStorage 控制何时再次显示覆盖层。
function shouldShowEntryLoader() {
  return sessionStorage.getItem(ENTRY_LOADER_FLAG) !== 'false'
}

function markEntryLoaderNeeded() {
  sessionStorage.setItem(ENTRY_LOADER_FLAG, 'true')
}

function markEntryLoaderHandled() {
  sessionStorage.setItem(ENTRY_LOADER_FLAG, 'false')
}

// 数据请求：从数据库接口读取当前分类下的面经帖子。
async function fetchList() {
  loading.value = true
  error.value = ''

  try {
    const result = await getInterviews({
      category: activeCategory.value,
      keyword: keyword.value.trim()
    })
    list.value = result.list
  } catch (requestError) {
    error.value = '数据加载失败，请检查后端服务是否已经启动'
    list.value = []
  } finally {
    loading.value = false
  }
}

// 定时器清理：离开页面或加载完成时释放旧任务。
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

// 空闲唤醒：页面长时间无操作后，重新允许进入动画出现。
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

// 进入流程：在数据准备完成后关闭覆盖层。
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

// 分类交互：点击分组或主题时刷新数据库帖子结果。
function selectTopic(topic) {
  selectedTopic.value = topic.key
  activeCategory.value = topic.category
}

function selectGroup(group) {
  activeGroup.value = group.key
  selectedTopic.value = group.defaultTopicKey
  activeCategory.value = group.category
}

// 列表联动：分类变化立即请求，关键词变化防抖请求。
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
      syncCategoryState(nextCategory)
      return
    }

    syncCategoryState('all')
  }
)

// 页面跳转：点击帖子卡片后进入对应详情页。
function goDetail(id) {
  router.push(`/interview/${id}`)
}

// 生命周期：进入时初始化，离开时清理副作用。
onMounted(() => {
  bindActivityListeners()
  initInterviewPage()
})

onBeforeUnmount(() => {
  clearTimeout(debounceTimer)
  clearEntryProgressTimer()
  clearEntryIdleTimer()
  unbindActivityListeners()
})
</script>

<style scoped src="../styles/views/InterviewList.css"></style>
