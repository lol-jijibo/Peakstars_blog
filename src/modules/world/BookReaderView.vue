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
      <div class="reader-shell">
        <div class="reader-card">
          <div class="reader-side-nav reader-side-nav-left">
            <button class="reader-side-nav-btn" type="button" :disabled="!canGoPrev" @click="prevChapter">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
                <path d="M15 19 8 12l7-7" stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" />
              </svg>
              <span>上一页</span>
            </button>
          </div>

          <div class="reader-side-nav reader-side-nav-right">
            <button class="reader-side-nav-btn" type="button" :disabled="!canGoNext" @click="nextChapter">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
                <path d="m9 5 7 7-7 7" stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" />
              </svg>
              <span>下一页</span>
            </button>
          </div>

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
              <section class="reader-page-frame">
                <div class="reader-column-meta">
                  <span class="reader-column-chip">{{ currentChapterLabel }}</span>
                  <span class="reader-column-submeta">{{ currentChapter?.subtitle || book.author || '阅读中' }}</span>
                </div>

                <div ref="pageViewportRef" class="reader-page-viewport" @wheel.prevent>
                  <figure v-if="showCoverSpread" class="reader-cover-spread">
                    <img :src="coverSpreadImage.src" :alt="coverSpreadImage.alt" @error="e => e.target.style.display = 'none'" />
                  </figure>
                  <div
                    v-show="!showCoverSpread"
                    ref="pageTrackRef"
                    class="reader-rich-html reader-paginated-track"
                    :style="paginatedTrackStyle"
                    @click="handleContentClick"
                    v-html="chapterContentHtml"
                  ></div>
                </div>

                <div class="reader-page-footer">
                  <div class="reader-column-footer">
                    <span>PAGE {{ leftPageNumber }}</span>
                    <span class="reader-column-footer-note">{{ footerTitle }}</span>
                  </div>

                  <div class="reader-column-footer reader-column-footer--right">
                    <span>{{ rightFooterPageLabel }}</span>
                    <span class="reader-column-footer-badge">{{ currentSpreadLabel }}</span>
                  </div>
                </div>
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
            :key="getChapterIdentity(chapter) || index"
            class="reader-drawer-item"
            :class="{ active: isCurrentChapter(chapter) }"
            type="button"
            @click="selectChapter(chapter)"
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

    <div class="reader-corner-badge">
      <div>{{ avatarText }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBookChapterDetail, getBookChapters, getBookDetail } from '@/api/book'

const BOOK_READING_HISTORY_STORAGE_KEY = 'peakstars-book-reading-history'

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
const pageViewportRef = ref(null)
const pageTrackRef = ref(null)
const currentSpreadIndex = ref(0)
const totalSpreadCount = ref(1)
const columnsPerSpread = ref(2)
const pendingSpreadPlacement = ref('start')
const chapterSwitching = ref(false)
const chapterFadePhase = ref('idle')
const pendingAnchorId = ref('')
const readerPageHeight = ref(520)
const pageViewportWidth = ref(0)
const pageTrackWidth = ref(0)
const pageColumnGap = ref(0)

let paginationFrame = 0
let trackMediaCleanup = []

const bookKey = computed(() => String(route.params.id || '').trim())
const currentIndex = computed(() => chapters.value.findIndex((item) => getChapterIdentity(item) === currentChapterId.value))
const canGoPrev = computed(() => currentSpreadIndex.value > 0 || currentIndex.value > 0)
const canGoNext = computed(() =>
  currentSpreadIndex.value < totalSpreadCount.value - 1 ||
  (currentIndex.value >= 0 && currentIndex.value < chapters.value.length - 1)
)
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
const footerTitle = computed(() => `${book.value.author || 'PeakStars Reader'} / ${book.value.title || 'Book Reader'}`)
const chapterContentHtml = computed(() => {
  const html = normalizeContentHtml(currentChapter.value?.contentHtml)
  if (!html) {
    return '<p class="reader-placeholder">暂无章节内容</p><p class="reader-placeholder">请选择其他章节继续阅读</p>'
  }
  return decoratePartHeadings(html, { removeOpeningCover: true })
})
const coverSpreadImage = computed(() => resolveCoverSpreadImage())
const hasCoverSpread = computed(() => Boolean(coverSpreadImage.value?.src))
const showCoverSpread = computed(() => hasCoverSpread.value && currentSpreadIndex.value === 0)
const contentSpreadIndex = computed(() => Math.max(0, currentSpreadIndex.value - (hasCoverSpread.value ? 1 : 0)))
const currentSpreadOffset = computed(() =>
  contentSpreadIndex.value * (pageTrackWidth.value + pageColumnGap.value)
)
const coverPageWidth = computed(() => {
  if (!pageTrackWidth.value) {
    return '100%'
  }
  return `${pageTrackWidth.value}px`
})
const paginatedTrackStyle = computed(() => ({
  transform: `translate3d(-${currentSpreadOffset.value}px, 0, 0)`,
  '--reader-page-columns': String(columnsPerSpread.value),
  '--reader-page-height': `${readerPageHeight.value}px`,
  '--reader-cover-page-width': coverPageWidth.value,
  '--reader-track-transition-duration': chapterSwitching.value ? '0s' : '0.28s',
  '--reader-track-opacity': chapterFadePhase.value === 'fading-out'
    ? '0.16'
    : chapterFadePhase.value === 'fading-in'
      ? '0.74'
      : '1'
}))
const currentSpreadLabel = computed(() => `${currentSpreadIndex.value + 1} / ${totalSpreadCount.value}`)
const leftPageNumber = computed(() => String(currentSpreadIndex.value * columnsPerSpread.value + 1).padStart(2, '0'))
const rightPageNumber = computed(() => String(currentSpreadIndex.value * columnsPerSpread.value + 2).padStart(2, '0'))
const rightFooterPageLabel = computed(() => (
  columnsPerSpread.value > 1 ? `PAGE ${rightPageNumber.value}` : `PAGE ${leftPageNumber.value}`
))

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
  updateColumnsPerSpread()
  reloadBookData()
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('resize', handleResize)
  cleanupTrackMediaListeners()
  cancelAnimationFrame(paginationFrame)
})

watch(bookKey, () => {
  pendingSpreadPlacement.value = 'start'
  reloadBookData()
})

watch([chapterContentHtml, columnsPerSpread], async () => {
  await syncPaginationLayout()
  applyPendingAnchor()
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
    chapters.value = normalizeReadableChapters(Array.isArray(chapterList) ? chapterList : [])

    if (chapters.value.length) {
      const targetChapterId = currentChapterId.value && chapters.value.some((item) => getChapterIdentity(item) === currentChapterId.value)
        ? currentChapterId.value
        : getChapterIdentity(chapters.value[0])
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

async function loadChapter(chapter) {
  const chapterId = getChapterIdentity(chapter)
  if (!chapterId) {
    return
  }

  chapterSwitching.value = true
  chapterFadePhase.value = 'fading-out'
  loading.value = true
  errorMessage.value = ''

  try {
    const loadedChapter = await getBookChapterDetail(bookKey.value, chapterId)
    currentChapter.value = loadedChapter
    currentChapterId.value = getChapterIdentity(loadedChapter) || chapterId
    showChapterDrawer.value = false
    persistReadingHistory(loadedChapter)
    chapterFadePhase.value = 'fading-in'
  } catch (error) {
    chapterFadePhase.value = 'idle'
    errorMessage.value = error.message || '章节内容加载失败'
  } finally {
    loading.value = false
  }
}

function selectChapter(chapter) {
  const chapterId = getChapterIdentity(chapter)
  if (!chapterId) {
    return
  }

  pendingSpreadPlacement.value = 'start'
  pendingAnchorId.value = ''

  if (chapterId === currentChapterId.value) {
    currentSpreadIndex.value = 0
    showChapterDrawer.value = false
    schedulePaginationMetrics()
    return
  }

  loadChapter(chapterId)
}

function getChapterIdentity(chapter) {
  if (chapter && typeof chapter === 'object') {
    return String(chapter.id || chapter.chapterKey || chapter.key || '').trim()
  }
  return String(chapter || '').trim()
}

function isCurrentChapter(chapter) {
  return getChapterIdentity(chapter) === currentChapterId.value
}

function prevChapter() {
  if (currentSpreadIndex.value > 0) {
    currentSpreadIndex.value -= 1
    return
  }

  if (!canGoPrev.value) {
    return
  }

  const prev = chapters.value[currentIndex.value - 1]
  if (prev) {
    pendingSpreadPlacement.value = 'end'
    loadChapter(prev)
  }
}

function nextChapter() {
  if (currentSpreadIndex.value < totalSpreadCount.value - 1) {
    currentSpreadIndex.value += 1
    return
  }

  if (!canGoNext.value) {
    return
  }

  const next = chapters.value[currentIndex.value + 1]
  if (next) {
    pendingSpreadPlacement.value = 'start'
    loadChapter(next)
  }
}

function toggleChapterDrawer() {
  showChapterDrawer.value = !showChapterDrawer.value
}

function goBack() {
  router.push('/world')
}

function persistReadingHistory(chapter) {
  if (typeof window === 'undefined' || !bookKey.value) {
    return
  }

  try {
    const rawHistory = window.localStorage.getItem(BOOK_READING_HISTORY_STORAGE_KEY)
    const parsedHistory = JSON.parse(rawHistory || '[]')
    const historyList = Array.isArray(parsedHistory) ? parsedHistory : []
    const nextEntry = {
      bookId: bookKey.value,
      chapterId: String(chapter?.id || ''),
      chapterNo: Number(chapter?.chapterNo || currentIndex.value + 1 || 1),
      chapterTitle: String(chapter?.title || ''),
      title: String(book.value.title || ''),
      author: String(book.value.author || ''),
      coverUrl: String(book.value.coverUrl || ''),
      updatedAt: new Date().toISOString()
    }

    const nextHistory = [
      nextEntry,
      ...historyList.filter((item) => String(item?.bookId || '') !== bookKey.value)
    ].slice(0, 40)

    window.localStorage.setItem(BOOK_READING_HISTORY_STORAGE_KEY, JSON.stringify(nextHistory))
  } catch (error) {
    console.warn('Failed to persist book reading history.', error)
  }
}

function handleContentClick(event) {
  const target = event.target
  if (!(target instanceof Element)) {
    return
  }

  const link = target.closest('a[href]')
  if (!link) {
    return
  }

  const rawHref = String(link.getAttribute('href') || '').trim()
  if (!rawHref || rawHref === '#') {
    event.preventDefault()
    return
  }

  if (isExternalReaderHref(rawHref)) {
    return
  }

  const resolvedTarget = resolveReaderLinkTarget(rawHref, link.textContent)
  if (!resolvedTarget) {
    event.preventDefault()
    return
  }

  event.preventDefault()

  if (resolvedTarget.chapterId && resolvedTarget.chapterId !== currentChapterId.value) {
    pendingSpreadPlacement.value = 'start'
    pendingAnchorId.value = resolvedTarget.anchorId || ''
    loadChapter(resolvedTarget.chapterId)
    return
  }

  if (resolvedTarget.anchorId) {
    pendingAnchorId.value = resolvedTarget.anchorId
    applyPendingAnchor()
  }
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

function handleResize() {
  updateColumnsPerSpread()
  schedulePaginationMetrics()
}

function updateColumnsPerSpread() {
  columnsPerSpread.value = window.innerWidth <= 1100 ? 1 : 2
}

async function syncPaginationLayout() {
  cleanupTrackMediaListeners()
  await nextTick()
  bindTrackMediaListeners()
  cancelAnimationFrame(paginationFrame)
  paginationFrame = requestAnimationFrame(() => {
    updatePaginationMetrics()
    applyPendingSpreadPlacement()
  })
}

function schedulePaginationMetrics() {
  cancelAnimationFrame(paginationFrame)
  paginationFrame = requestAnimationFrame(() => {
    updatePaginationMetrics()
  })
}

function updatePaginationMetrics() {
  const viewport = pageViewportRef.value
  const track = pageTrackRef.value
  if (!viewport || !track) {
    totalSpreadCount.value = hasCoverSpread.value ? 2 : 1
    currentSpreadIndex.value = 0
    pageViewportWidth.value = 0
    pageTrackWidth.value = 0
    pageColumnGap.value = 0
    return
  }

  const viewportWidth = viewport.clientWidth
  if (!viewportWidth) {
    return
  }

  const computedStyle = window.getComputedStyle(track)
  const parsedColumnGap = Number.parseFloat(computedStyle.columnGap || '0')
  const columnGap = Number.isFinite(parsedColumnGap) ? parsedColumnGap : 0

  readerPageHeight.value = viewport.clientHeight || readerPageHeight.value
  pageViewportWidth.value = viewportWidth
  pageTrackWidth.value = track.clientWidth || viewportWidth
  pageColumnGap.value = columnGap
  const spreadStride = pageTrackWidth.value + columnGap
  const totalTrackWidth = Math.max(track.scrollWidth + columnGap, spreadStride)
  const contentSpreadCount = Math.max(1, Math.ceil(totalTrackWidth / spreadStride))
  const nextSpreadCount = contentSpreadCount + (hasCoverSpread.value ? 1 : 0)
  totalSpreadCount.value = nextSpreadCount
  currentSpreadIndex.value = Math.min(currentSpreadIndex.value, nextSpreadCount - 1)
}

function applyPendingSpreadPlacement() {
  currentSpreadIndex.value = pendingSpreadPlacement.value === 'end'
    ? Math.max(totalSpreadCount.value - 1, 0)
    : 0
  pendingSpreadPlacement.value = 'start'
  requestAnimationFrame(() => {
    chapterSwitching.value = false
    requestAnimationFrame(() => {
      chapterFadePhase.value = 'idle'
    })
  })
}

function applyPendingAnchor() {
  const anchorId = pendingAnchorId.value
  if (!anchorId) {
    return
  }

  const track = pageTrackRef.value
  const viewport = pageViewportRef.value
  if (!track || !viewport) {
    return
  }

  const anchorTarget = findAnchorTarget(track, anchorId)
  if (!anchorTarget) {
    pendingAnchorId.value = ''
    return
  }

  updatePaginationMetrics()
  const spreadStride = pageTrackWidth.value + pageColumnGap.value
  if (!spreadStride) {
    return
  }
  currentSpreadIndex.value = Math.max(
    0,
    Math.min(totalSpreadCount.value - 1, Math.floor(anchorTarget.offsetLeft / spreadStride))
  )
  pendingAnchorId.value = ''
}

function bindTrackMediaListeners() {
  const track = pageTrackRef.value
  if (!track) {
    return
  }

  const images = Array.from(track.querySelectorAll('img'))
  images.forEach((image) => {
    applyImageOrientation(image)
    if (image.complete) {
      return
    }

    const handler = () => {
      applyImageOrientation(image)
      schedulePaginationMetrics()
    }

    image.addEventListener('load', handler)
    image.addEventListener('error', handler)
    trackMediaCleanup.push(() => {
      image.removeEventListener('load', handler)
      image.removeEventListener('error', handler)
    })
  })
}

function applyImageOrientation(image) {
  const holder = image.closest('.reader-image-page')
  if (!holder) {
    return
  }

  const orientation = resolveImageOrientation(image, holder)
  const isWideSource = image.naturalWidth > image.naturalHeight
  const isTallSource = image.naturalHeight > image.naturalWidth
  holder.classList.toggle('is-rotate-landscape', orientation === 'landscape' && isTallSource)
  holder.classList.toggle('is-rotate-portrait', orientation === 'portrait' && isWideSource)
  holder.classList.toggle('is-keep-portrait', orientation === 'portrait' && !isWideSource)
}

function resolveImageOrientation(image, holder) {
  const rawValue = [
    image.dataset.readerOrientation,
    image.dataset.orientation,
    holder.dataset.readerOrientation,
    image.getAttribute('data-reader-orientation'),
    image.getAttribute('data-orientation'),
    image.getAttribute('alt'),
    image.getAttribute('title'),
    image.currentSrc,
    image.src
  ].find(Boolean)

  const value = String(rawValue || '').toLowerCase()
  if (/(rotate|landscape|horizontal|横排|横向|横版)/i.test(value)) {
    return 'landscape'
  }
  if (/(portrait|vertical|竖排|竖向|竖版)/i.test(value)) {
    return 'portrait'
  }

  if (isBookCoverLikeImage(image)) {
    return 'portrait'
  }

  if (image.naturalWidth && image.naturalHeight) {
    const widthRatio = image.naturalWidth / image.naturalHeight
    if (widthRatio >= 1.18 && widthRatio <= 1.9 && isLikelyRotatedTitlePage(image)) {
      return 'portrait'
    }
  }

  return 'auto'
}

function isLikelyRotatedTitlePage(image) {
  const hint = [
    image.getAttribute('alt'),
    image.getAttribute('title'),
    image.getAttribute('src'),
    image.currentSrc
  ].filter(Boolean).join(' ').toLowerCase()

  if (/(title[-_ ]?page|front|cover|fm|page[-_ ]?0?3|chapter[-_ ]?0?3|扉页|封面|标题页)/i.test(hint)) {
    return true
  }

  return isMostlyLightImage(image)
}

function isMostlyLightImage(image) {
  try {
    const canvas = document.createElement('canvas')
    const context = canvas.getContext('2d', { willReadFrequently: true })
    if (!context) {
      return false
    }

    canvas.width = 24
    canvas.height = 24
    context.drawImage(image, 0, 0, canvas.width, canvas.height)
    const { data } = context.getImageData(0, 0, canvas.width, canvas.height)
    let lightPixels = 0
    let sampledPixels = 0

    for (let index = 0; index < data.length; index += 16) {
      const alpha = data[index + 3]
      if (alpha < 32) {
        continue
      }
      const brightness = (data[index] + data[index + 1] + data[index + 2]) / 3
      sampledPixels += 1
      if (brightness > 220) {
        lightPixels += 1
      }
    }

    return sampledPixels > 0 && lightPixels / sampledPixels > 0.68
  } catch (error) {
    return false
  }
}

function isBookCoverLikeImage(image) {
  const coverIdentity = normalizeImageIdentity(book.value.coverUrl)
  const imageIdentities = [
    image.currentSrc,
    image.src,
    image.getAttribute('src')
  ].map(normalizeImageIdentity).filter(Boolean)

  if (coverIdentity && imageIdentities.some((item) => item === coverIdentity || item.endsWith(coverIdentity) || coverIdentity.endsWith(item))) {
    return true
  }

  const hint = [
    image.getAttribute('alt'),
    image.getAttribute('title'),
    image.getAttribute('src')
  ].filter(Boolean).join(' ').toLowerCase()

  return /(cover|front|title[-_ ]?page|fm|封面|书封|扉页)/i.test(hint)
}

function normalizeImageIdentity(value) {
  const raw = String(value || '').trim()
  if (!raw || raw.startsWith('data:')) {
    return ''
  }

  try {
    const parsed = new URL(raw, window.location.origin)
    return parsed.pathname.replace(/^\/+/, '').toLowerCase()
  } catch (error) {
    return raw.split(/[?#]/)[0].replace(/^\/+/, '').toLowerCase()
  }
}

function cleanupTrackMediaListeners() {
  trackMediaCleanup.forEach((cleanup) => cleanup())
  trackMediaCleanup = []
}

function isExternalReaderHref(href) {
  return /^(https?:|mailto:|tel:|data:|\/\/)/i.test(String(href || '').trim())
}

function resolveReaderLinkTarget(href, linkText) {
  const normalizedHref = String(href || '').trim()
  if (!normalizedHref) {
    return null
  }

  if (normalizedHref.startsWith('#')) {
    return {
      chapterId: currentChapterId.value,
      anchorId: decodeFragment(normalizedHref.slice(1))
    }
  }

  const [pathPart, hashPart = ''] = normalizedHref.split('#')
  const matchedChapter = matchChapterByHref(pathPart, linkText)

  if (matchedChapter) {
    return {
      chapterId: getChapterIdentity(matchedChapter),
      anchorId: decodeFragment(hashPart)
    }
  }

  if (!pathPart) {
    return {
      chapterId: currentChapterId.value,
      anchorId: decodeFragment(hashPart)
    }
  }

  return null
}

function matchChapterByHref(pathValue, linkText) {
  const normalizedPath = normalizeChapterLookupValue(pathValue)
  const normalizedPathBase = normalizeChapterLookupValue(extractFileBaseName(pathValue))
  const normalizedText = normalizeChapterLookupValue(linkText)

  return chapters.value.find((chapter) => {
    const candidates = [
      chapter.id,
      chapter.chapterKey,
      chapter.href,
      chapter.path,
      chapter.sourceHref,
      chapter.sourcePath,
      chapter.title,
      chapter.subtitle
    ]
      .filter(Boolean)
      .map(normalizeChapterLookupValue)

    if (normalizedPath && candidates.some((item) => item === normalizedPath || item.endsWith(normalizedPath))) {
      return true
    }

    if (normalizedPathBase && candidates.some((item) => item === normalizedPathBase || item.endsWith(normalizedPathBase))) {
      return true
    }

    if (normalizedText && candidates.some((item) => item === normalizedText || item.includes(normalizedText) || normalizedText.includes(item))) {
      return true
    }

    return false
  }) || null
}

function normalizeChapterLookupValue(value) {
  const decoded = safeDecode(String(value || ''))
    .trim()
    .replace(/\\/g, '/')
    .replace(/^\.\/+/, '')
    .replace(/^(?:\.\.\/)+/, '')
    .replace(/[?#].*$/, '')

  if (!decoded) {
    return ''
  }

  return decoded
    .split('/')
    .filter(Boolean)
    .join('/')
    .toLowerCase()
    .replace(/\.(xhtml|html|htm|xml)$/g, '')
    .replace(/[\s\-_'"`~!@#$%^&*()+=[\]{}|\\:;,.<>/?，。！？；：、“”‘’（）《》【】、]/g, '')
}

function extractFileBaseName(value) {
  const normalized = safeDecode(String(value || '')).replace(/\\/g, '/').replace(/[?#].*$/, '')
  if (!normalized) {
    return ''
  }
  const segments = normalized.split('/').filter(Boolean)
  return segments.length ? segments[segments.length - 1] : normalized
}

function decodeFragment(value) {
  return safeDecode(String(value || '').trim())
}

function safeDecode(value) {
  try {
    return decodeURIComponent(value)
  } catch (error) {
    return value
  }
}

function findAnchorTarget(track, anchorId) {
  const normalizedId = String(anchorId || '').trim()
  if (!normalizedId) {
    return null
  }

  const anchors = Array.from(track.querySelectorAll('[id], a[name]'))
  return anchors.find((node) => {
    const id = node.getAttribute('id')
    const name = node.getAttribute('name')
    return id === normalizedId || name === normalizedId
  }) || null
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

function normalizeReadableChapters(chapterList) {
  let firstReadableIndex = 0
  while (
    firstReadableIndex < chapterList.length &&
    isOpeningImageOnlyChapter(chapterList[firstReadableIndex])
  ) {
    firstReadableIndex += 1
  }

  return chapterList.slice(firstReadableIndex).map((chapter, index) => ({
    ...chapter,
    chapterNo: index + 1
  }))
}

function isOpeningImageOnlyChapter(chapter) {
  if (Number(chapter?.wordCount || 0) > 0) {
    return false
  }

  const html = normalizeContentHtml(chapter?.contentHtml)
  if (!html) {
    return true
  }

  if (typeof document === 'undefined') {
    return /<(img|image|svg|picture)\b/i.test(html) && !html.replace(/<[^>]+>/g, '').trim()
  }

  const template = document.createElement('template')
  template.innerHTML = html
  const text = String(template.content.textContent || '').replace(/\s+/g, '').trim()
  const hasMedia = Boolean(template.content.querySelector('img, image, svg, picture'))

  return hasMedia && !text
}

function decoratePartHeadings(html, options = {}) {
  if (typeof document === 'undefined') {
    return html
  }

  const template = document.createElement('template')
  template.innerHTML = html
  if (options.removeOpeningCover) {
    removeOpeningCoverImage(template.content)
  }
  decorateReaderImages(template.content)
  const candidates = Array.from(template.content.querySelectorAll('p, h1, h2, h3, h4, h5, h6, div'))
  const textBlocks = candidates.filter((node) => {
    const text = String(node.textContent || '').trim()
    return text && !node.querySelector('img, video, table, ul, ol, blockquote')
  })

  candidates.forEach((node) => {
    const headingMeta = resolveStructuredHeadingMeta(node.textContent, {
      allowNumericMarker: isLeadingChapterMarkerNode(node, textBlocks)
    })
    if (!headingMeta || node.querySelector('img, video, table, ul, ol, blockquote')) {
      return
    }

    const partHeading = document.createElement('div')
    partHeading.className = headingMeta.variant === 'chapter-marker'
      ? 'reader-part-inline reader-part-inline--chapter-mark'
      : 'reader-part-inline'
    partHeading.setAttribute('aria-label', headingMeta.fullTitle)
    const textClass = headingMeta.variant === 'chapter-marker'
      ? 'reader-part-inline-text reader-part-inline-text--chapter-mark'
      : 'reader-part-inline-text'
    partHeading.innerHTML = `<span class="${textClass}">${escapeHtml(headingMeta.fullTitle)}</span>`
    node.replaceWith(partHeading)
  })

  return template.innerHTML
}

function resolveCoverSpreadImage() {
  if (!isOpeningChapter() || !book.value.coverUrl) {
    return null
  }

  return {
    src: book.value.coverUrl,
    alt: book.value.title || 'book cover'
  }
}

function removeOpeningCoverImage(root) {
  if (!isOpeningChapter() || !book.value.coverUrl) {
    return
  }

  const images = Array.from(root.querySelectorAll('img, image'))
  const openingCover = images.find((image) => isOpeningCoverCandidate(image)) || images[0]
  const candidates = [
    openingCover,
    ...images.filter((image) => isImageBeforeOpeningText(image, root))
  ].filter((image, index, list) => image && list.indexOf(image) === index)

  candidates.forEach((image, index) => {
    if (!root.contains(image)) {
      return
    }

    if (index > 0 && !isOpeningCoverCandidate(image)) {
      return
    }

    const holder = findOpeningMediaHolder(image)
    if (holder) {
      holder.remove()
      return
    }

    image.remove()
  })
}

function isImageBeforeOpeningText(image, root) {
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_ELEMENT | NodeFilter.SHOW_TEXT)
  let node = walker.nextNode()

  while (node) {
    if (node === image) {
      return true
    }

    if (node.nodeType === Node.TEXT_NODE && String(node.textContent || '').trim()) {
      return false
    }

    if (node.nodeType === Node.ELEMENT_NODE) {
      const element = node
      if (element !== image && element.tagName === 'IMG') {
        node = walker.nextNode()
        continue
      }

      if (!element.contains(image) && String(element.textContent || '').trim() && !element.querySelector('img')) {
        return false
      }
    }

    node = walker.nextNode()
  }

  return false
}

function isOpeningCoverCandidate(image) {
  return isBookCoverLikeImage(image) || isLikelyCoverSizedImage(image)
}

function isLikelyCoverSizedImage(image) {
  const rawWidth = Number.parseFloat(image.getAttribute('width') || image.style.width || '0')
  const rawHeight = Number.parseFloat(image.getAttribute('height') || image.style.height || '0')
  if (rawWidth > 0 && rawHeight > 0) {
    const ratio = rawWidth / rawHeight
    return ratio >= 0.48 && ratio <= 0.9
  }

  const hint = [
    image.getAttribute('alt'),
    image.getAttribute('title'),
    image.getAttribute('src'),
    image.currentSrc
  ].filter(Boolean).join(' ').toLowerCase()

  return /(cover|front|title[-_ ]?page|fm|page[-_ ]?0?[12]|灏侀潰|涔﹀皝|鎵夐〉)/i.test(hint)
}

function decorateReaderImages(root) {
  const images = Array.from(root.querySelectorAll('img'))
  images.forEach((image) => {
    if (image.closest('.reader-image-page')) {
      return
    }

    const holder = findImageOnlyHolder(image)
    const isCoverPageImage = shouldPromoteToCoverPage(image, images)
    const normalizedImage = image.cloneNode(true)
    normalizedImage.removeAttribute('width')
    normalizedImage.removeAttribute('height')
    normalizedImage.removeAttribute('style')
    normalizedImage.setAttribute('loading', 'lazy')
    normalizedImage.setAttribute('onerror', "this.style.display='none';this.closest('.reader-image-page')?.remove()")

    const figure = document.createElement('figure')
    figure.className = isCoverPageImage ? 'reader-image-page reader-cover-page' : 'reader-image-page'
    figure.setAttribute('aria-label', image.getAttribute('alt') || 'book image')
    figure.appendChild(normalizedImage)

    if (holder) {
      holder.replaceWith(figure)
      return
    }

    image.replaceWith(figure)
  })
}

function findOpeningMediaHolder(image) {
  const svg = image.closest?.('svg')
  if (svg) {
    return findImageOnlyHolder(svg) || svg
  }

  return findImageOnlyHolder(image)
}

function shouldPromoteToCoverPage(image, images) {
  if (isOpeningChapter() && book.value.coverUrl) {
    return false
  }

  return isOpeningChapter() && (image === images[0] || isBookCoverLikeImage(image))
}

function isOpeningChapter() {
  const chapterNo = Number(currentChapter.value?.chapterNo || currentIndex.value + 1 || 1)
  return currentIndex.value <= 0 || chapterNo <= 1
}

function findImageOnlyHolder(image) {
  const holder = image.parentElement
  if (!holder || !['P', 'DIV', 'FIGURE'].includes(holder.tagName)) {
    return null
  }

  const hasOnlyImage = Array.from(holder.childNodes).every((node) => {
    if (node === image) {
      return true
    }
    return node.nodeType === Node.TEXT_NODE && !node.textContent.trim()
  })

  return hasOnlyImage ? holder : null
}

function normalizePartHeadingText(text) {
  const normalized = String(text || '').replace(/\s+/g, '')
  return /^第[零〇一二三四五六七八九十百千万两\d]+部$/.test(normalized) ? normalized : ''
}

function isLeadingChapterMarkerNode(node, textBlocks) {
  const blockIndex = textBlocks.indexOf(node)
  return blockIndex >= 0 && blockIndex <= 1
}

function normalizeChapterMarkerText(text) {
  return String(text || '')
    .replace(/\s+/g, '')
    .replace(/[．。.、,，:：;；·]/g, '')
    .replace(/[０-９]/g, (char) => String(char.charCodeAt(0) - 65296))
}

function resolveStructuredHeadingMeta(text, options = {}) {
  const rawText = String(text || '').replace(/\s+/g, ' ').trim()
  if (!rawText) {
    return null
  }

  const normalizedMarker = normalizeChapterMarkerText(rawText)
  if (options.allowNumericMarker && /^(?:\d{1,3}|[一二三四五六七八九十百千两〇零]{1,4})$/.test(normalizedMarker)) {
    return {
      badge: 'CHAPTER',
      eyebrow: `CHAPTER ${normalizedMarker}`,
      fullTitle: normalizedMarker,
      kicker: '章节序号',
      tone: 'chapter',
      variant: 'chapter-marker'
    }
  }

  const normalizedText = rawText.replace(/\s+/g, '')
  const headingMatch = normalizedText.match(/^(第[零〇一二三四五六七八九十百千万两\d]+)([部章节回卷篇幕集课则])([：:、.\-—_ ]*)(.*)$/)

  if (headingMatch) {
    const [, orderText, unitText, , tailText] = headingMatch
    const mainTitle = `${orderText}${unitText}`
    return {
      badge: unitText,
      eyebrow: formatHeadingEyebrow(orderText, unitText),
      fullTitle: mainTitle,
      kicker: tailText ? tailText.trim() : deriveHeadingKicker(unitText),
      tone: resolveHeadingTone(unitText),
      variant: 'default'
    }
  }

  const specialMatch = normalizedText.match(/^(序章|楔子|引子|前言|序言|后记|尾声|终章|附录|番外)(.*)$/)
  if (specialMatch) {
    const [, keyword, tailText] = specialMatch
    return {
      badge: keyword.slice(0, 2),
      eyebrow: 'SPECIAL ENTRY',
      fullTitle: keyword,
      kicker: tailText ? tailText.trim() : '章节节点',
      tone: 'special',
      variant: 'default'
    }
  }

  return null
}

function formatHeadingEyebrow(orderText, unitText) {
  const unitMap = {
    '部': 'VOLUME',
    '章': 'CHAPTER',
    '节': 'SECTION',
    '回': 'EPISODE',
    '卷': 'BOOK',
    '篇': 'PART',
    '幕': 'ACT',
    '集': 'UNIT',
    '课': 'LESSON',
    '则': 'ENTRY'
  }
  return `${unitMap[unitText] || 'CHAPTER'} ${orderText}`
}

function deriveHeadingKicker(unitText) {
  const kickerMap = {
    '部': '篇章分部',
    '章': '章节正文',
    '节': '节次展开',
    '回': '回目推进',
    '卷': '卷册结构',
    '篇': '篇目内容',
    '幕': '叙事幕次',
    '集': '内容单元',
    '课': '课程章节',
    '则': '条目段落'
  }
  return kickerMap[unitText] || '章节节点'
}

function resolveHeadingTone(unitText) {
  if (['部', '卷', '篇', '幕'].includes(unitText)) {
    return 'major'
  }
  if (['节', '回', '集', '课', '则'].includes(unitText)) {
    return 'minor'
  }
  return 'chapter'
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
</script>

<style src="./BookReaderView.css"></style>
