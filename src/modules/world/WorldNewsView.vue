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
            <button
              class="world-bookshelf-link"
              type="button"
              :disabled="continueSwitching || continueBatchCount <= 1"
              @click="refreshContinueBatch"
            >
              刷新书架
            </button>
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

        <transition name="world-content-fade" mode="out-in">
          <div
            v-if="continueReadingList.length"
            :key="`continue-list-${continueBatchIndex}`"
            class="world-bookshelf-reading-grid"
          >
            <article
              v-for="(item, index) in continueReadingList"
              :key="`${item.id}-${continueBatchIndex}-${index}`"
              class="world-bookshelf-reading-card world-bookshelf-reading-card-stagger"
              :style="{ '--stagger-delay': `${index * 48}ms` }"
              @click="openBook(item.id)"
            >
              <div class="world-bookshelf-mini-cover" :style="buildCoverStyle(item)"></div>
              <div class="world-bookshelf-reading-copy">
                <h3>{{ item.title }}</h3>
                <p>{{ item.author }}</p>
              </div>
            </article>
          </div>
          <div v-else-if="loading" key="continue-loading" class="world-bookshelf-empty">
            <p>书架内容加载中，请稍候。</p>
          </div>
          <div v-else key="continue-empty" class="world-bookshelf-empty">
            <p>还没有阅读记录，先选一本到阅读页看看吧。</p>
          </div>
        </transition>
      </section>

      <section class="world-bookshelf-section">
        <div class="world-bookshelf-section-head">
          <h2>推荐阅读</h2>
          <button
            class="world-bookshelf-link"
            type="button"
            :disabled="recommendSwitching || recommendBatchCount <= 1"
            @click="refreshRecommendBatch"
          >
            换一批
          </button>
        </div>

        <transition name="world-content-fade" mode="out-in">
          <div
            v-if="recommendList.length"
            :key="`recommend-list-${recommendBatchIndex}`"
            class="world-bookshelf-recommend-grid"
          >
            <article
              v-for="(item, index) in recommendList"
              :key="`${item.id}-${recommendBatchIndex}-${index}`"
              class="world-bookshelf-recommend-card world-bookshelf-recommend-card-stagger"
              :style="{ '--stagger-delay': `${index * 48}ms` }"
              @click="openBook(item.id)"
            >
              <div class="world-bookshelf-cover-frame">
                <div class="world-bookshelf-large-cover" :style="buildCoverStyle(item)"></div>
              </div>
              <h3>{{ item.title }}</h3>
              <p class="world-bookshelf-recommend-author">{{ item.author }}</p>
            </article>
          </div>
          <div v-else-if="loading" key="recommend-loading" class="world-bookshelf-empty">
            <p>正在生成推荐书单。</p>
          </div>
          <div v-else key="recommend-empty" class="world-bookshelf-empty">
            <p>当前还没有可展示的书籍推荐。</p>
          </div>
        </transition>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import BlogMegaHeader from '@/components/BlogMegaHeader.vue'
import { getBooks } from '@/api/book'
import { useAuthStore } from '@/stores/auth'

const BOOK_READING_HISTORY_STORAGE_KEY = 'peakstars-book-reading-history'
const BOOK_BATCH_SIZE = 4
const BOOK_SWITCH_DURATION = 360

const router = useRouter()
const authStore = useAuthStore()

const searchKeyword = ref('')
const loading = ref(false)
const books = ref([])
const readingHistory = ref([])
const continueBatchIndex = ref(0)
const continueSwitching = ref(false)
const recommendBatchIndex = ref(0)
const recommendSwitching = ref(false)
const showAvatarImage = ref(true)

let continueSwitchTimer = null
let recommendSwitchTimer = null

const filteredBookList = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  const source = keyword
    ? books.value.filter((item) => `${item.title || ''}${item.author || ''}`.toLowerCase().includes(keyword))
    : books.value
  return source.map((item, index) => normalizeBook(item, index))
})

const readingHistoryMap = computed(() => {
  return new Map(
    readingHistory.value
      .filter((item) => item?.bookId)
      .map((item) => [String(item.bookId), item])
  )
})

const continueBookSource = computed(() => {
  const source = filteredBookList.value
  if (!source.length) {
    return []
  }

  const ranked = source
    .map((item, index) => {
      const history = readingHistoryMap.value.get(String(item.id))
      const timestamp = history?.updatedAt ? Date.parse(history.updatedAt) : 0
      return {
        ...item,
        historyUpdatedAt: Number.isFinite(timestamp) ? timestamp : 0,
        historyChapterTitle: String(history?.chapterTitle || ''),
        originalIndex: index
      }
    })
    .filter((item) => item.historyUpdatedAt > 0)
    .sort((left, right) => {
      if (right.historyUpdatedAt !== left.historyUpdatedAt) {
        return right.historyUpdatedAt - left.historyUpdatedAt
      }
      return left.originalIndex - right.originalIndex
    })

  if (!ranked.length) {
    return source.slice(0, BOOK_BATCH_SIZE)
  }

  if (ranked.length >= BOOK_BATCH_SIZE) {
    return ranked
  }

  const rankedIds = new Set(ranked.map((item) => String(item.id)))
  const fallbackBooks = source.filter((item) => !rankedIds.has(String(item.id)))
  return ranked.concat(fallbackBooks.slice(0, BOOK_BATCH_SIZE - ranked.length))
})

const continueBookIds = computed(() => {
  return new Set(continueBookSource.value.map((item) => String(item.id)))
})

const recommendBookSource = computed(() => {
  const source = filteredBookList.value
  if (!source.length) {
    return []
  }

  const unreadFirst = source.filter((item) => !continueBookIds.value.has(String(item.id)))
  const fallback = source.filter((item) => continueBookIds.value.has(String(item.id)))
  const combinedSource = unreadFirst.length ? unreadFirst.concat(fallback) : [...source]

  return combinedSource
    .map((item, index) => ({
      ...item,
      historyUpdatedAt: readingHistoryMap.value.get(String(item.id))?.updatedAt
        ? Date.parse(readingHistoryMap.value.get(String(item.id)).updatedAt)
        : 0,
      originalIndex: index
    }))
    .sort((left, right) => {
      const leftUnread = continueBookIds.value.has(String(left.id)) ? 1 : 0
      const rightUnread = continueBookIds.value.has(String(right.id)) ? 1 : 0
      if (leftUnread !== rightUnread) {
        return leftUnread - rightUnread
      }
      if (left.category !== right.category) {
        return String(left.category || '').localeCompare(String(right.category || ''))
      }
      if (left.title !== right.title) {
        return String(left.title || '').localeCompare(String(right.title || ''))
      }
      if (left.historyUpdatedAt !== right.historyUpdatedAt) {
        return left.historyUpdatedAt - right.historyUpdatedAt
      }
      return left.originalIndex - right.originalIndex
    })
})

const continueBatches = computed(() => {
  return buildBookBatches(continueBookSource.value)
})

const continueBatchCount = computed(() => {
  return continueBatches.value.length
})

const continueReadingList = computed(() => {
  return continueBatches.value[continueBatchIndex.value] || []
})

const recommendBatches = computed(() => {
  return buildBookBatches(recommendBookSource.value)
})

const recommendBatchCount = computed(() => {
  return recommendBatches.value.length
})

const recommendList = computed(() => {
  return recommendBatches.value[recommendBatchIndex.value] || []
})

const avatarFallback = computed(() => {
  const source = authStore.currentUser.value?.username || authStore.currentUser.value?.email || 'P'
  return source.trim().slice(0, 1).toUpperCase()
})

watch([filteredBookList, readingHistory], () => {
  stopContinueSwitching()
  continueBatchIndex.value = 0
  stopRecommendSwitching()
  recommendBatchIndex.value = 0
}, { deep: true })

watch(continueBatchCount, (count) => {
  if (!count) {
    stopContinueSwitching()
    continueBatchIndex.value = 0
    return
  }

  if (continueBatchIndex.value >= count) {
    continueBatchIndex.value = 0
  }
})

watch(recommendBatchCount, (count) => {
  if (!count) {
    stopRecommendSwitching()
    recommendBatchIndex.value = 0
    return
  }

  if (recommendBatchIndex.value >= count) {
    recommendBatchIndex.value = 0
  }
})

onMounted(() => {
  reloadBooks()
  syncReadingHistory()
  window.addEventListener('storage', handleStorageChange)
})

onBeforeUnmount(() => {
  stopContinueSwitching()
  stopRecommendSwitching()
  window.removeEventListener('storage', handleStorageChange)
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

function syncReadingHistory() {
  if (typeof window === 'undefined') {
    readingHistory.value = []
    return
  }

  try {
    const rawHistory = window.localStorage.getItem(BOOK_READING_HISTORY_STORAGE_KEY)
    const parsedHistory = JSON.parse(rawHistory || '[]')
    readingHistory.value = Array.isArray(parsedHistory) ? parsedHistory : []
  } catch (error) {
    readingHistory.value = []
  }
}

function handleStorageChange(event) {
  if (event.key === BOOK_READING_HISTORY_STORAGE_KEY) {
    syncReadingHistory()
  }
}

function buildBookBatches(source) {
  if (!source.length) {
    return []
  }

  if (source.length <= BOOK_BATCH_SIZE) {
    return [source]
  }

  const batchCount = Math.ceil(source.length / BOOK_BATCH_SIZE)
  return Array.from({ length: batchCount }, (_, batchIndex) => {
    const start = batchIndex * BOOK_BATCH_SIZE
    return Array.from({ length: BOOK_BATCH_SIZE }, (_, offset) => {
      return source[(start + offset) % source.length]
    })
  })
}

function refreshContinueBatch() {
  if (continueBatchCount.value <= 1 || continueSwitching.value) {
    return
  }

  stopContinueSwitching()
  continueSwitching.value = true
  continueBatchIndex.value = (continueBatchIndex.value + 1) % continueBatchCount.value
  continueSwitchTimer = window.setTimeout(() => {
    continueSwitching.value = false
    continueSwitchTimer = null
  }, BOOK_SWITCH_DURATION)
}

function refreshRecommendBatch() {
  if (recommendBatchCount.value <= 1 || recommendSwitching.value) {
    return
  }

  stopRecommendSwitching()
  recommendSwitching.value = true
  recommendBatchIndex.value = (recommendBatchIndex.value + 1) % recommendBatchCount.value
  recommendSwitchTimer = window.setTimeout(() => {
    recommendSwitching.value = false
    recommendSwitchTimer = null
  }, BOOK_SWITCH_DURATION)
}

function openBook(bookKey) {
  router.push(`/book/${bookKey}`)
}

function stopContinueSwitching() {
  continueSwitching.value = false
  if (continueSwitchTimer) {
    window.clearTimeout(continueSwitchTimer)
    continueSwitchTimer = null
  }
}

function stopRecommendSwitching() {
  recommendSwitching.value = false
  if (recommendSwitchTimer) {
    window.clearTimeout(recommendSwitchTimer)
    recommendSwitchTimer = null
  }
}

function normalizeBook(item, index) {
  return {
    id: item.id,
    title: item.title || '未命名书籍',
    shortTitle: shortenText(item.title || '书籍', 12),
    kicker: shortenText(item.category || '书籍', 8),
    footer: shortenText(item.author || '未知作者', 10),
    author: shortenText(item.author || '未知作者', 16),
    category: item.category || '',
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
