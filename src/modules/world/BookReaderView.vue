<template>
  <div class="book-reader-page">
    <header class="reader-topbar">
      <div class="reader-topbar-left">
        <button class="reader-topbar-brand" type="button" @click="goBack">
          <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M21 4H3a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h18a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2Zm0 14H4V6h17v12ZM6 8h12v2H6V8Zm0 3h12v2H6v-2Zm0 3h7v2H6v-2Z" />
          </svg>
          <span>{{ book.title || '书籍详情' }}</span>
        </button>
        <button class="reader-topbar-action" type="button">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
            <path d="M12 4v16M20 12H4" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" />
          </svg>
          <span>加入书架</span>
        </button>
      </div>

      <div class="reader-topbar-right">
        <nav class="reader-topbar-nav">
          <button class="reader-topbar-nav-link" type="button" @click="goBack">首页</button>
          <button class="reader-topbar-nav-link active" type="button">我的书架</button>
        </nav>
        <div class="reader-topbar-avatar">{{ avatarText }}</div>
      </div>
    </header>

    <main class="reader-stage">
      <div class="reader-side-nav reader-side-nav-left">
        <button class="reader-side-nav-btn" type="button" :disabled="!canGoPrev" @click="prevChapter">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
            <path d="M15 19 8 12l7-7" stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" />
          </svg>
          <span>PREVIOUS</span>
        </button>
      </div>

      <div class="reader-side-nav reader-side-nav-right">
        <button class="reader-side-nav-btn" type="button" :disabled="!canGoNext" @click="nextChapter">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
            <path d="m9 5 7 7-7 7" stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" />
          </svg>
          <span>NEXT PAGE</span>
        </button>
      </div>

      <div class="reader-shell">
        <div class="reader-card">
          <div class="reader-progress-top">
            <div class="reader-progress-top-fill" :style="{ width: `${progressPercent}%` }"></div>
          </div>

          <div v-if="loading && !currentChapter" class="reader-empty-state">
            <p>章节加载中...</p>
          </div>

          <div v-else-if="errorMessage && !currentChapter" class="reader-empty-state">
            <p>{{ errorMessage }}</p>
            <button class="reader-retry-btn" type="button" @click="reloadBookData">重新加载</button>
          </div>

          <template v-else>
            <div class="reader-layout">
              <section class="reader-columns">
                <article class="reader-column">
                  <div class="reader-column-meta">
                    <span class="reader-column-chip">{{ currentChapterLabel }}</span>
                    <span class="reader-column-submeta">{{ currentChapter?.subtitle || book.author || '阅读中' }}</span>
                  </div>

                  <div class="reader-column-body">
                    <div class="reader-rich-html" v-html="leftPageHtml"></div>
                  </div>

                  <div class="reader-column-footer">
                    <span>PAGE {{ leftPageNumber }}</span>
                    <span class="reader-column-footer-note">{{ footerTitle }}</span>
                  </div>
                </article>

                <article class="reader-column reader-column-right">
                  <div class="reader-column-spacer"></div>

                  <div class="reader-column-body">
                    <div class="reader-rich-html" v-html="rightPageHtml"></div>
                  </div>

                  <div class="reader-column-footer">
                    <span>PAGE {{ rightPageNumber }}</span>
                    <span class="reader-column-footer-badge">{{ progressPercent }}% READ</span>
                  </div>
                </article>
              </section>
            </div>
          </template>
        </div>
      </div>

      <aside class="reader-floating-tools">
        <button class="reader-floating-btn" type="button" title="章节目录" @click="toggleChapterDrawer">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
            <path d="M4 6h16M4 12h16m-7 6h7" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" />
          </svg>
        </button>
        <button class="reader-floating-btn" type="button" title="字号">
          <span>Aa</span>
        </button>
        <button class="reader-floating-btn" type="button" title="标注">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
            <path d="m15.232 5.232 3.536 3.536m-2.036-5.036a2.5 2.5 0 1 1 3.536 3.536L6.5 21.036H3v-3.572L16.732 3.732Z" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" />
          </svg>
        </button>
        <div class="reader-floating-divider"></div>
        <button class="reader-floating-btn" type="button" title="目录概览" @click="toggleChapterDrawer">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
            <path d="M4 6h16M4 10h16M4 14h16M4 18h16" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" />
          </svg>
        </button>
        <button class="reader-floating-btn" type="button" title="主题">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
            <path d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364-6.364-.707.707M6.343 17.657l-.707.707m12.728 0-.707-.707M6.343 6.343l-.707-.707M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10Z" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" />
          </svg>
        </button>
      </aside>
    </main>

    <aside v-if="showChapterDrawer" class="reader-drawer-backdrop" @click.self="toggleChapterDrawer">
      <div class="reader-drawer">
        <div class="reader-drawer-book">
          <div class="reader-drawer-cover" :style="coverStyle">
            <div class="reader-drawer-cover-overlay">
              <span class="reader-drawer-cover-kicker">{{ book.category || 'BOOK' }}</span>
              <strong>{{ book.title || '未命名书籍' }}</strong>
            </div>
          </div>
          <div class="reader-drawer-book-meta">
            <h2>{{ book.title || '未命名书籍' }}</h2>
            <p>{{ book.author || '未知作者' }}</p>
            <p>{{ book.publisher || '未填写出版社' }}</p>
          </div>
          <div class="reader-drawer-stats">
            <span>{{ chapters.length }} 章</span>
            <span>{{ formatWordCount(book.wordCount) }} 字</span>
            <span>{{ formatCount(book.readCount) }} 阅读</span>
          </div>
          <p class="reader-drawer-summary">{{ book.summary || '暂无书籍简介。' }}</p>
        </div>

        <div class="reader-drawer-header">
          <h3>章节目录</h3>
          <button class="reader-drawer-close" type="button" @click="toggleChapterDrawer">关闭</button>
        </div>

        <div class="reader-drawer-list">
          <button
            v-for="(chapter, index) in chapters"
            :key="chapter.id"
            class="reader-drawer-item"
            :class="{ active: currentChapterId === chapter.id }"
            type="button"
            @click="selectChapter(chapter.id)"
          >
            <span class="reader-drawer-index">{{ String(index + 1).padStart(2, '0') }}</span>
            <div class="reader-drawer-item-main">
              <span class="reader-drawer-item-title">{{ chapter.title }}</span>
              <span class="reader-drawer-item-sub">{{ chapter.subtitle || `${formatWordCount(chapter.wordCount)} 字` }}</span>
            </div>
            <span v-if="chapter.free" class="reader-drawer-free">免费</span>
          </button>
        </div>
      </div>
    </aside>

    <footer class="reader-bottom-hint">
      <div class="reader-bottom-hint-inner">
        <span>Use ← → to navigate</span>
        <span>•</span>
        <span>Auto-scroll: OFF</span>
        <span>•</span>
        <span>Immersion Mode: ON</span>
      </div>
    </footer>

    <div class="reader-corner-badge">
      <div>{{ avatarText }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBookChapterDetail, getBookChapters, getBookDetail } from '@/api/book'

const route = useRoute()
const router = useRouter()

const book = ref({
  id: '',
  title: '',
  author: '',
  publisher: '',
  category: '',
  summary: '',
  coverUrl: '',
  tags: [],
  wordCount: 0,
  chapterCount: 0,
  readCount: 0,
  rating: '9.0',
  publishedAt: ''
})
const chapters = ref([])
const currentChapter = ref(null)
const currentChapterId = ref('')
const loading = ref(false)
const errorMessage = ref('')
const showChapterDrawer = ref(false)

const bookKey = computed(() => String(route.params.id || '').trim())
const currentIndex = computed(() => chapters.value.findIndex((item) => item.id === currentChapterId.value))
const canGoPrev = computed(() => currentIndex.value > 0)
const canGoNext = computed(() => currentIndex.value >= 0 && currentIndex.value < chapters.value.length - 1)
const progressPercent = computed(() => {
  if (!chapters.value.length || currentIndex.value < 0) {
    return 0
  }
  return Math.round(((currentIndex.value + 1) / chapters.value.length) * 100)
})
const avatarText = computed(() => {
  const source = String(book.value.author || book.value.title || 'PS').trim()
  return source.slice(0, 2).toUpperCase()
})
const currentChapterLabel = computed(() => {
  const chapterNo = Number(currentChapter.value?.chapterNo || currentIndex.value + 1 || 1)
  return `CHAPTER ${String(chapterNo).padStart(2, '0')}`
})
const footerTitle = computed(() => {
  return `${book.value.author || 'PeakStars Reader'} / ${book.value.title || 'Book Reader'}`
})
const pageSegments = computed(() => {
  const html = normalizeContentHtml(currentChapter.value?.contentHtml)
  if (!html) {
    return ['<p class="reader-placeholder">暂无章节内容</p>', '<p class="reader-placeholder">请选择其他章节继续阅读</p>']
  }

  const blocks = splitHtmlBlocks(html)
  if (blocks.length <= 1) {
    return [html, '<p class="reader-placeholder">本页内容已阅读完毕</p>']
  }

  const midpoint = Math.ceil(blocks.length / 2)
  const left = blocks.slice(0, midpoint).join('')
  const right = blocks.slice(midpoint).join('')

  return [left || '<p class="reader-placeholder">暂无章节内容</p>', right || '<p class="reader-placeholder">本页内容已阅读完毕</p>']
})
const leftPageHtml = computed(() => pageSegments.value[0])
const rightPageHtml = computed(() => pageSegments.value[1])
const leftPageNumber = computed(() => {
  const base = currentIndex.value < 0 ? 1 : currentIndex.value * 2 + 1
  return String(base).padStart(2, '0')
})
const rightPageNumber = computed(() => {
  const base = currentIndex.value < 0 ? 2 : currentIndex.value * 2 + 2
  return String(base).padStart(2, '0')
})

const coverStyle = computed(() => {
  if (book.value.coverUrl) {
    return {
      backgroundImage: `linear-gradient(rgba(6, 16, 14, 0.45), rgba(6, 16, 14, 0.65)), url(${book.value.coverUrl})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center'
    }
  }

  return {
    backgroundImage: 'linear-gradient(160deg, rgba(45, 212, 191, 0.28), rgba(9, 16, 14, 0.9))'
  }
})

onMounted(() => {
  reloadBookData()
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
})

watch(bookKey, () => {
  reloadBookData()
})

async function reloadBookData() {
  if (!bookKey.value) {
    errorMessage.value = '缺少书籍标识，无法加载内容。'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const [bookDetail, chapterList] = await Promise.all([
      getBookDetail(bookKey.value),
      getBookChapters(bookKey.value)
    ])

    book.value = {
      ...book.value,
      ...(bookDetail || {})
    }
    chapters.value = Array.isArray(chapterList) ? chapterList : []

    if (chapters.value.length) {
      const targetChapterId = currentChapterId.value && chapters.value.some((item) => item.id === currentChapterId.value)
        ? currentChapterId.value
        : chapters.value[0].id
      await loadChapter(targetChapterId)
    } else {
      currentChapter.value = null
      currentChapterId.value = ''
      errorMessage.value = '当前书籍还没有已发布章节。'
    }
  } catch (error) {
    errorMessage.value = error.message || '书籍内容加载失败'
  } finally {
    loading.value = false
  }
}

async function loadChapter(chapterId) {
  if (!chapterId) {
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    currentChapter.value = await getBookChapterDetail(bookKey.value, chapterId)
    currentChapterId.value = chapterId
    showChapterDrawer.value = false
  } catch (error) {
    errorMessage.value = error.message || '章节内容加载失败'
  } finally {
    loading.value = false
  }
}

function selectChapter(chapterId) {
  loadChapter(chapterId)
}

function prevChapter() {
  if (!canGoPrev.value) {
    return
  }
  const prev = chapters.value[currentIndex.value - 1]
  if (prev) {
    loadChapter(prev.id)
  }
}

function nextChapter() {
  if (!canGoNext.value) {
    return
  }
  const next = chapters.value[currentIndex.value + 1]
  if (next) {
    loadChapter(next.id)
  }
}

function toggleChapterDrawer() {
  showChapterDrawer.value = !showChapterDrawer.value
}

function goBack() {
  router.push('/world')
}

function handleKeydown(event) {
  if (event.key === 'ArrowLeft') {
    prevChapter()
  }
  if (event.key === 'ArrowRight') {
    nextChapter()
  }
  if (event.key === 'Escape' && showChapterDrawer.value) {
    showChapterDrawer.value = false
  }
}

function formatCount(value) {
  const count = Number(value || 0)
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}w`
  }
  return String(count)
}

function formatWordCount(value) {
  const count = Number(value || 0)
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}万`
  }
  return String(count || 0)
}

function normalizeContentHtml(contentHtml) {
  return String(contentHtml || '')
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/<style[\s\S]*?<\/style>/gi, '')
    .trim()
}

function splitHtmlBlocks(html) {
  const normalized = html
    .replace(/<\/(p|h1|h2|h3|h4|h5|h6|blockquote|ul|ol|pre|table|div)>/gi, '$&<!--BLOCK_SPLIT-->')
    .replace(/<br\s*\/?>/gi, '<br /><!--BLOCK_SPLIT-->')

  return normalized
    .split('<!--BLOCK_SPLIT-->')
    .map((item) => item.trim())
    .filter(Boolean)
}
</script>

<style src="./BookReaderView.css"></style>
