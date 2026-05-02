<template>
  <div class="interview-page">
    <blog-mega-header current-page="interview" />

    <main class="interview-main">
      <!-- ═══ STATS BAR ═══ -->
      <section class="int-stats" aria-label="面经统计概览">
        <div class="int-stat-card">
          <span class="int-stat-icon int-stat-icon--blue">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <polyline points="10 9 9 9 8 9"/>
            </svg>
          </span>
          <div class="int-stat-copy">
            <strong class="int-stat-num">{{ totalCount }}</strong>
            <span class="int-stat-label">题目总数</span>
          </div>
        </div>
        <div class="int-stat-card">
          <span class="int-stat-icon int-stat-icon--purple">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="2" y="3" width="20" height="4" rx="1"/>
              <rect x="4" y="10" width="16" height="4" rx="1"/>
              <rect x="6" y="17" width="12" height="4" rx="1"/>
            </svg>
          </span>
          <div class="int-stat-copy">
            <strong class="int-stat-num">{{ categoryCount }}</strong>
            <span class="int-stat-label">专题模块</span>
          </div>
        </div>
        <div class="int-stat-card">
          <span class="int-stat-icon int-stat-icon--orange">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
            </svg>
          </span>
          <div class="int-stat-copy">
            <strong class="int-stat-num">{{ hotTagCount }}</strong>
            <span class="int-stat-label">高频考点</span>
          </div>
        </div>
        <div class="int-stat-card">
          <span class="int-stat-icon int-stat-icon--green">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
              <polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
          </span>
          <div class="int-stat-copy">
            <strong class="int-stat-num">{{ masteredCount }}</strong>
            <span class="int-stat-label">已掌握</span>
          </div>
        </div>
      </section>

      <!-- ═══ SEARCH BAR ═══ -->
      <section class="int-search-section" aria-label="面经搜索">
        <div class="int-search-bar">
          <i class="fas fa-search int-search-icon"></i>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索面经、关键词、公司..."
            class="int-search-input"
          />
        </div>
      </section>

      <!-- ═══ CATEGORY TABS ═══ -->
      <section class="int-category-section" aria-label="分类筛选">
        <nav class="int-category-tabs" role="tablist">
          <button
            v-for="firstLevel in visibleFirstLevelCategories"
            :key="firstLevel.key"
            type="button"
            class="int-cat-tab"
            :class="{ active: activeFirstLevel.key === firstLevel.key }"
            @click="selectFirstLevel(firstLevel)"
          >
            {{ firstLevel.label }}
          </button>
        </nav>

        <!-- Sub Category Pills -->
        <div v-if="activeFirstLevel.subCategories.length" class="int-sub-pills">
          <button
            v-for="sub in activeFirstLevel.subCategories"
            :key="sub.key"
            type="button"
            class="int-sub-pill"
            :class="{ active: selectedSubCategory === sub.key }"
            @click="selectSubCategory(sub)"
          >
            {{ sub.label }}
          </button>
        </div>
      </section>

      <!-- ═══ CARDS GRID ═══ -->
      <section class="int-card-section" aria-label="面经列表">
        <div v-if="loading" class="int-loading">
          <span class="int-loading-spin"></span>
          <span>加载中...</span>
        </div>

        <div v-else-if="error" class="int-error">
          <p>{{ error }}</p>
          <button type="button" class="int-retry-btn" @click="fetchList">重试</button>
        </div>

        <template v-else>
          <div class="int-card-grid">
            <interview-card
              v-for="item in list"
              :key="item.id"
              :data="item"
              @click="goDetail(item.id)"
            />
          </div>

          <div v-if="list.length === 0" class="int-empty">
            <i class="fas fa-inbox"></i>
            <p>当前分类下还没有面经内容</p>
          </div>
        </template>
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

const route = useRoute()
const router = useRouter()

/* ── 数据状态 ── */
const keyword = ref('')
const list = ref([])
const loading = ref(false)
const error = ref('')
let debounceTimer = 0

/* ── 统计数据 ── */
const totalCount = ref(0)
const categoryCount = computed(() => Math.max(visibleFirstLevelCategories.value.length - 1, 0))
const hotTagCount = ref(0)
const masteredCount = ref(0)

/* ── 一级分类 ── */
const firstLevelCategories = [
  { key: 'all', label: '全部', subCategories: [] },
  {
    key: 'backend', label: 'Java 后端',
    subCategories: [
      { key: 'all', label: '全部', category: 'all' },
      { key: 'java', label: 'Java 基础', category: 'java' },
      { key: 'collection', label: '集合框架', category: 'java' },
      { key: 'jvm', label: 'JVM', category: 'java' },
      { key: 'concurrent', label: '并发编程', category: 'java' },
      { key: 'springboot', label: 'Spring Boot', category: 'java' },
      { key: 'mysql', label: 'MySQL', category: 'java' },
      { key: 'redis', label: 'Redis', category: 'java' },
      { key: 'system-design', label: 'Elasticsearch', category: 'java' }
    ]
  },
  {
    key: 'frontend', label: '前端',
    subCategories: [
      { key: 'all', label: '全部', category: 'frontend' },
      { key: 'html', label: 'HTML/CSS', category: 'frontend' },
      { key: 'javascript', label: 'JavaScript', category: 'frontend' },
      { key: 'typescript', label: 'TypeScript', category: 'frontend' },
      { key: 'vue', label: 'Vue', category: 'frontend' },
      { key: 'react', label: 'React', category: 'frontend' },
    ]
  },
  { key: 'agent', label: 'Agent 开发', subCategories: [] },
  { key: 'llm', label: '大模型原理', subCategories: [] },
  { key: 'algorithm', label: '算法', subCategories: [] }
]

/* ── 当前选中状态 ── */
const initialCategory = typeof route.query.category === 'string' ? route.query.category : 'all'
const selectedSubCategory = ref(initialCategory)
const activeFirstLevel = ref(resolveFirstLevel(initialCategory))
const supportedFirstLevelKeys = ['all', 'agent', 'llm', 'backend', 'frontend']
const visibleFirstLevelCategories = computed(() =>
  firstLevelCategories.filter((item) => supportedFirstLevelKeys.includes(item.key))
)
/**
 * 目的：统一一级分类与后端接口分类参数的映射关系。
 * 逻辑：把前端展示态的 backend 转成后端真实支持的 java，保证“Java后端-全部”能命中完整数据。
 */
const firstLevelCategoryApiMap = {
  backend: 'java',
  frontend: 'frontend'
}

function resolveFirstLevel(category) {
  if (category === 'frontend') return firstLevelCategories.find((f) => f.key === 'frontend')
  if (category === 'agent') return firstLevelCategories.find((f) => f.key === 'agent')
  if (category === 'llm') return firstLevelCategories.find((f) => f.key === 'llm')
  if (category === 'all' || !category) return firstLevelCategories[0]
  return firstLevelCategories.find((f) => f.key === 'backend')
}

/* ── 当前实际传给 API 的 category ── */
const activeCategory = computed(() => {
  if (activeFirstLevel.value.key === 'all') return 'all'
  if (activeFirstLevel.value.key === 'agent') return 'agent'
  if (activeFirstLevel.value.key === 'llm') return 'llm'
  if (activeFirstLevel.value.key === 'algorithm') return 'all'
  if (selectedSubCategory.value === 'all') {
    return firstLevelCategoryApiMap[activeFirstLevel.value.key] || activeFirstLevel.value.key
  }
  return selectedSubCategory.value
})

/* ── 分类切换 ── */
function selectFirstLevel(firstLevel) {
  activeFirstLevel.value = firstLevel
  if (firstLevel.subCategories.length) {
    selectedSubCategory.value = firstLevel.subCategories[0].key
  } else {
    selectedSubCategory.value = 'all'
  }
}

function selectSubCategory(sub) {
  selectedSubCategory.value = sub.key
}

/* ── 数据请求 ── */
async function fetchList() {
  loading.value = true
  error.value = ''

  try {
    const result = await getInterviews({
      category: activeCategory.value,
      keyword: keyword.value.trim()
    })
    list.value = result.list || []
    totalCount.value = result.total || list.value.length
    hotTagCount.value = result.hotTagCount || 0
    masteredCount.value = result.masteredCount || Math.floor(list.value.length * 0.62)
  } catch {
    error.value = '数据加载失败，请检查后端服务是否已经启动'
    list.value = []
  } finally {
    loading.value = false
  }
}

/* ── 路由导航 ── */
function goDetail(id) {
  router.push(`/interview/${id}`)
}

/* ── Watchers ── */
watch(activeCategory, () => { fetchList() })

watch(keyword, () => {
  clearTimeout(debounceTimer)
  debounceTimer = window.setTimeout(fetchList, 500)
})

watch(() => route.query.category, (nextCategory) => {
  if (typeof nextCategory === 'string') {
    const fl = resolveFirstLevel(nextCategory)
    activeFirstLevel.value = fl
    selectedSubCategory.value = fl.subCategories.length ? nextCategory : 'all'
    return
  }
  activeFirstLevel.value = firstLevelCategories[0]
  selectedSubCategory.value = 'all'
})

/* ── Lifecycle ── */
onMounted(() => { fetchList() })
onBeforeUnmount(() => { clearTimeout(debounceTimer) })
</script>

<style scoped src="../styles/views/InterviewList.css"></style>
