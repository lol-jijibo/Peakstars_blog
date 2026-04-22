<template>
  <div class="page-layout">
    <!-- 顶部导航 -->
    <header class="header">
      <div class="header-inner">
        <span class="logo">PeakStars_blog</span>
        <!-- 分类 Tab -->
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

    <!-- 内容区 -->
    <main class="main-content">
      <!-- 搜索框 -->
      <div class="search-bar">
        <input
          v-model="keyword"
          type="text"
          placeholder="🔍 搜索面经..."
          class="search-input"
        />
      </div>

      <!-- 面经列表 -->
      <div class="interview-list">
        <!-- 加载中 -->
        <div v-if="loading" class="loading-tip">
          <span class="loading-spin">⏳</span> 加载中...
        </div>
        <!-- 错误提示 -->
        <div v-else-if="error" class="error-tip">
          <p>{{ error }}</p>
          <button @click="fetchList" class="retry-btn">重试</button>
        </div>
        <!-- 列表 -->
        <template v-else>
          <interview-card
            v-for="item in list"
            :key="item.id"
            :data="item"
            @click="goDetail(item.id)"
          />
          <div v-if="list.length === 0" class="empty-tip">
            <span>暂无相关面经 😅</span>
          </div>
        </template>
      </div>
    </main>

    <!-- 底部 TabBar -->
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
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getInterviews } from '@/api/interview.js'
import InterviewCard from '@/components/InterviewCard.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const keyword = ref('')
const activeCategory = ref('all')

// 列表数据
const list = ref([])
const loading = ref(false)
const error = ref('')

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'frontend', label: '前端' },
  { key: 'java', label: 'Java后端' }
]

const navTabs = [
  { path: '/interview', icon: '📋', label: '面经' },
  { path: '/collect', icon: '⭐', label: '收藏' },
  { path: '/like', icon: '❤️', label: '喜欢' },
  { path: '/mine', icon: '👤', label: '我的' }
]

// 防抖：关键词 500ms 后请求
let debounceTimer = null

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
    error.value = '数据加载失败，请检查后端服务是否启动'
    list.value = []
  } finally {
    loading.value = false
  }
}

// 切换分类立即请求
watch(activeCategory, fetchList)

// 关键词防抖
watch(keyword, () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(fetchList, 500)
})

onMounted(fetchList)

function goDetail(id) {
  router.push(`/interview/${id}`)
}

function backToLogin() {
  authStore.logout()
  router.replace('/login')
}
</script>

<style scoped src="../styles/views/InterviewList.css"></style>
