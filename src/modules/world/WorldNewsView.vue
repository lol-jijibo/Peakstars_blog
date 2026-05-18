<template>
  <div class="world-bookshelf-page">
    <blog-mega-header current-page="world" />

    <main class="world-bookshelf-main">
      <section class="world-bookshelf-hero">
        <div class="world-bookshelf-brand">
          <h1>star_read</h1>
        </div>

        <label class="world-bookshelf-search" for="world-bookshelf-search">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M10.5 4a6.5 6.5 0 1 1 0 13a6.5 6.5 0 0 1 0-13Zm0 2a4.5 4.5 0 1 0 0 9a4.5 4.5 0 0 0 0-9Zm8.91 11.5 2.8 2.8-1.41 1.41-2.8-2.8 1.41-1.41Z"
              fill="currentColor"
            />
          </svg>
          <input
            id="world-bookshelf-search"
            v-model.trim="searchKeyword"
            type="search"
            placeholder="搜索书籍标题或作者"
          />
        </label>
      </section>

      <section class="world-bookshelf-section">
        <div class="world-bookshelf-section-head">
          <h2>继续阅读</h2>
          <div class="world-bookshelf-section-actions">
            <button class="world-bookshelf-link" type="button" @click="reloadBooks">刷新书架</button>
            <div class="world-bookshelf-avatar">
              <img
                v-if="showAvatarImage"
                src="/qq.jpg"
                alt="当前用户头像"
                @error="showAvatarImage = false"
              />
              <span v-else>{{ avatarFallback }}</span>
            </div>
          </div>
        </div>

        <div v-if="continueReadingList.length" class="world-bookshelf-reading-grid">
          <article
            v-for="item in continueReadingList"
            :key="item.id"
            class="world-bookshelf-reading-card"
            @click="openBook(item.id)"
          >
            <div class="world-bookshelf-mini-cover" :style="buildCoverStyle(item)"></div>
            <div class="world-bookshelf-reading-copy">
              <h3>{{ item.title }}</h3>
              <p>{{ item.author }}</p>
            </div>
          </article>
        </div>
        <div v-else-if="loading" class="world-bookshelf-empty">
          <p>书籍内容加载中，请稍候。</p>
        </div>
        <div v-else class="world-bookshelf-empty">
          <p>还没有已发布书籍，先去后台导入一本吧。</p>
        </div>
      </section>

      <section class="world-bookshelf-section">
        <div class="world-bookshelf-section-head">
          <h2>推荐阅读</h2>
          <button class="world-bookshelf-link" type="button" @click="refreshRecommendBatch">
            换一批
          </button>
        </div>

        <div v-if="recommendList.length" class="world-bookshelf-recommend-grid">
          <article
            v-for="item in recommendList"
            :key="item.id"
            class="world-bookshelf-recommend-card"
            @click="openBook(item.id)"
          >
            <div class="world-bookshelf-cover-frame">
              <div class="world-bookshelf-large-cover" :style="buildCoverStyle(item)"></div>
            </div>
            <h3>{{ item.title }}</h3>
            <p class="world-bookshelf-recommend-author">{{ item.author }}</p>
          </article>
        </div>
        <div v-else-if="loading" class="world-bookshelf-empty">
          <p>正在生成推荐书单。</p>
        </div>
        <div v-else class="world-bookshelf-empty">
          <p>当前还没有可展示的书籍推荐。</p>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BlogMegaHeader from '@/components/BlogMegaHeader.vue'
import { getBooks } from '@/api/book'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const searchKeyword = ref('')
const loading = ref(false)
const books = ref([])
const recommendOffset = ref(0)
const showAvatarImage = ref(true)

const continueReadingList = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  const source = keyword
    ? books.value.filter((item) => `${item.title || ''}${item.author || ''}`.toLowerCase().includes(keyword))
    : books.value
  return source.slice(0, 8).map((item, index) => normalizeBook(item, index))
})

const recommendList = computed(() => {
  const source = continueReadingList.value
  if (!source.length) {
    return []
  }
  const start = recommendOffset.value % source.length
  return [...source.slice(start), ...source.slice(0, start)].slice(0, 4)
})

const avatarFallback = computed(() => {
  const source = authStore.currentUser.value?.username || authStore.currentUser.value?.email || 'P'
  return source.trim().slice(0, 1).toUpperCase()
})

onMounted(() => {
  reloadBooks()
})

async function reloadBooks() {
  loading.value = true
  try {
    const list = await getBooks()
    books.value = Array.isArray(list) ? list : []
  } finally {
    loading.value = false
  }
}

function refreshRecommendBatch() {
  if (!continueReadingList.value.length) {
    return
  }
  recommendOffset.value = (recommendOffset.value + 4) % continueReadingList.value.length
}

function openBook(bookKey) {
  router.push(`/book/${bookKey}`)
}

function normalizeBook(item, index) {
  return {
    id: item.id,
    title: item.title || '未命名书籍',
    shortTitle: shortenText(item.title || '书籍', 12),
    kicker: shortenText(item.category || '书籍', 8),
    footer: shortenText(item.author || '未知作者', 10),
    author: shortenText(item.author || '未知作者', 16),
    accent: buildAccentByIndex(index),
    coverUrl: item.coverUrl || ''
  }
}

function buildCoverStyle(item) {
  const style = {
    '--cover-accent': item.accent,
    '--cover-glow': hexToRgba(item.accent, 0.18)
  }
  if (item.coverUrl) {
    style.backgroundImage = `url(${item.coverUrl})`
    style.backgroundSize = 'cover'
    style.backgroundPosition = 'center'
  }
  return style
}

function shortenText(value, maxLength) {
  const text = `${value || ''}`.trim()
  if (!text) {
    return ''
  }
  return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text
}

function buildAccentByIndex(index) {
  const palette = ['#2f2f34', '#d54835', '#e7c8d1', '#6e7e8e', '#3b4259', '#8c4f37']
  return palette[index % palette.length]
}

function hexToRgba(hex, alpha) {
  const normalized = (hex || '').replace('#', '')
  if (!/^[\da-fA-F]{6}$/.test(normalized)) {
    return `rgba(60, 79, 107, ${alpha})`
  }

  const r = Number.parseInt(normalized.slice(0, 2), 16)
  const g = Number.parseInt(normalized.slice(2, 4), 16)
  const b = Number.parseInt(normalized.slice(4, 6), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}
</script>

<style src="./WorldNewsView.css"></style>
