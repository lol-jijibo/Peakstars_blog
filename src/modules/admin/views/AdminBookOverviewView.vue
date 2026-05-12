<template>
  <div class="book-overview-shell">
    <section v-if="errorMessage" class="book-overview-alert">
      {{ errorMessage }}
    </section>

    <section class="book-overview-stats">
      <article class="book-overview-stat-card book-overview-stat-glow">
        <div class="book-overview-stat-head">
          <div class="book-overview-stat-icon book-overview-stat-icon-primary">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M6 4.75A1.75 1.75 0 0 1 7.75 3h8.5A1.75 1.75 0 0 1 18 4.75v12.5A1.75 1.75 0 0 1 16.25 19h-8.5A1.75 1.75 0 0 1 6 17.25V4.75Z"></path>
              <path d="M9 7.5h6M9 11h6M9 14.5h4"></path>
              <path d="M4.75 6H6v11.25C6 18.216 6.784 19 7.75 19H15v1.25A1.75 1.75 0 0 1 13.25 22h-6.5A1.75 1.75 0 0 1 5 20.25V6.25A1.25 1.25 0 0 1 6.25 5H7"></path>
            </svg>
          </div>
          <div class="book-overview-stat-side">
            <span class="book-overview-stat-topvalue">+12%</span>
            <span class="book-overview-stat-toplabel">月增长</span>
          </div>
        </div>
        <div class="book-overview-stat-value">{{ formatCompactNumber(totalBooks) }}</div>
        <div class="book-overview-stat-label">系统存量书籍</div>
      </article>

      <article class="book-overview-stat-card book-overview-stat-card-accent">
        <div class="book-overview-stat-head">
          <div class="book-overview-stat-icon book-overview-stat-icon-blue">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M5 16.5 10 11l3 3 6-7"></path>
              <path d="M14 7h5v5"></path>
            </svg>
          </div>
          <div class="book-overview-stat-side">
            <span class="book-overview-stat-topvalue book-overview-stat-topvalue-blue">实时</span>
            <span class="book-overview-stat-toplabel">活跃中</span>
          </div>
        </div>
        <div class="book-overview-stat-value">{{ formatCompactNumber(todayReaders) }}</div>
        <div class="book-overview-stat-label">今日活跃读者</div>
      </article>

      <article class="book-overview-stat-card">
        <div class="book-overview-stat-head">
          <div class="book-overview-stat-icon book-overview-stat-icon-orange">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M5 16.5a4.5 4.5 0 0 1 1.35-8.793A5.5 5.5 0 0 1 17.5 8.75a3.75 3.75 0 1 1 .75 7.75H7.5"></path>
            </svg>
          </div>
          <div class="book-overview-stat-side">
            <span class="book-overview-stat-toplabel">已用空间</span>
            <span class="book-overview-stat-topvalue-light">{{ usedStorageLabel }}</span>
          </div>
        </div>
        <div class="book-overview-stat-value">{{ storageUsagePercent }}%</div>
        <div class="book-overview-storage-bar">
          <span :style="{ width: `${storageUsagePercent}%` }"></span>
        </div>
      </article>

      <article class="book-overview-stat-card book-overview-stat-card-cta">
        <div class="book-overview-stat-head">
          <div class="book-overview-stat-icon book-overview-stat-icon-solid">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 3.75 14.474 6.1l3.402-.228.552 3.365 2.85 1.882-1.467 3.078 1.467 3.078-2.85 1.882-.552 3.365-3.402-.228L12 20.25l-2.474 2.353-3.402.228-.552-3.365-2.85-1.882 1.467-3.078-1.467-3.078 2.85-1.882.552-3.365 3.402.228L12 3.75Z"></path>
              <path d="m9.25 12.35 1.75 1.75 3.75-4"></path>
            </svg>
          </div>
        </div>
        <div class="book-overview-stat-value">{{ pendingReviewCount }}</div>
        <div class="book-overview-stat-label book-overview-stat-label-spaced">待审核内容</div>
        <button class="book-overview-cta-btn" type="button" @click="goImportPage">
          前往处理
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M5 12h14"></path>
            <path d="m13 6 6 6-6 6"></path>
          </svg>
        </button>
      </article>
    </section>

    <section class="book-overview-table-card">
      <div class="book-overview-toolbar">
        <div class="book-overview-tabs">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            class="book-overview-tab"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="book-overview-actions">
          <button class="book-overview-action-btn" type="button" @click="cycleFilter">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M4 5h16l-6.25 7.313v5.125l-3.5 1.75v-6.875L4 5Z"></path>
            </svg>
            筛选
          </button>
          <button class="book-overview-action-btn" type="button" @click="toggleSortDirection">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M8 5v14"></path>
              <path d="m5 8 3-3 3 3"></path>
              <path d="M16 19V5"></path>
              <path d="m13 16 3 3 3-3"></path>
            </svg>
            排序
          </button>
        </div>
      </div>

      <div class="book-overview-table-wrap">
        <table class="book-overview-table">
          <thead>
            <tr>
              <th>书籍详情</th>
              <th>作者</th>
              <th>分类</th>
              <th>阅读指数</th>
              <th>当前状态</th>
              <th>更新日期</th>
              <th class="book-overview-align-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="book in pagedBooks" :key="book.id" class="book-overview-row">
              <td>
                <div class="book-overview-book-meta">
                  <div class="book-overview-cover-frame">
                    <img :src="resolveCoverImage(book)" :alt="book.title" />
                  </div>
                  <div class="book-overview-book-copy">
                    <span class="book-overview-book-title">{{ book.title }}</span>
                    <span class="book-overview-book-ref">{{ book.refCode }}</span>
                  </div>
                </div>
              </td>
              <td class="book-overview-author">{{ book.author || '未知作者' }}</td>
              <td>
                <span class="book-overview-category-pill" :class="`is-${book.categoryTone}`">{{ book.categoryLabel }}</span>
              </td>
              <td>
                <div class="book-overview-read-cell">
                  <span class="book-overview-read-value">{{ formatCompactNumber(book.readCount) }}</span>
                  <div class="book-overview-read-track">
                    <span :style="{ width: `${book.readPercent}%` }"></span>
                  </div>
                </div>
              </td>
              <td>
                <span class="book-overview-status-pill" :class="`is-${book.statusTone}`">
                  <span class="book-overview-status-dot"></span>
                  {{ book.statusLabel }}
                </span>
              </td>
              <td class="book-overview-date">{{ book.displayDate }}</td>
              <td class="book-overview-align-right">
                <button class="book-overview-menu-btn" type="button" @click="openBookDetail(book)">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <circle cx="6.5" cy="12" r="1.4"></circle>
                    <circle cx="12" cy="12" r="1.4"></circle>
                    <circle cx="17.5" cy="12" r="1.4"></circle>
                  </svg>
                </button>
              </td>
            </tr>
            <tr v-if="!pagedBooks.length">
              <td colspan="7" class="book-overview-empty">暂无符合条件的书籍记录</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="book-overview-footer">
        <span class="book-overview-footer-copy">显示 {{ pageRange.start }} 到 {{ pageRange.end }}，共 {{ formatCompactNumber(filteredBooks.length) }} 条书籍记录</span>
        <div class="book-overview-pagination">
          <button class="book-overview-page-icon" type="button" :disabled="page === 1" @click="page = Math.max(1, page - 1)">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="m15 6-6 6 6 6"></path>
            </svg>
          </button>
          <button
            v-for="item in paginationItems"
            :key="item.key"
            type="button"
            class="book-overview-page-btn"
            :class="{ active: item.type === 'page' && item.value === page, ellipsis: item.type === 'ellipsis' }"
            :disabled="item.type === 'ellipsis'"
            @click="item.type === 'page' ? page = item.value : null"
          >
            {{ item.label }}
          </button>
          <button class="book-overview-page-icon" type="button" :disabled="page === totalPages" @click="page = Math.min(totalPages, page + 1)">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="m9 6 6 6-6 6"></path>
            </svg>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getAdminBooks, listRecentImportJobs } from '@/modules/admin/api/bookAdmin'

const router = useRouter()

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'published', label: '已发布' },
  { key: 'draft', label: '草稿箱' },
  { key: 'review', label: '审核中' }
]

const books = ref([])
const importJobs = ref([])
const loading = ref(false)
const errorMessage = ref('')
const activeTab = ref('all')
const page = ref(1)
const pageSize = 10
const filterCycleIndex = ref(0)
const sortDescending = ref(true)

const sampleCoverMap = [
  'https://lh3.googleusercontent.com/aida-public/AB6AXuDMt93gdU22B4C94v4JY_6NBSOVIu-OicMYuhPIT4ar-LpSWGO-ORkbrdPeXkiIxgDXnOKrYu0gLLYe6r_YmvAdhZ87oYkXRpDlAAFTntjcjCvE7DK2jdfoW105SFzmMZLPn-yZwE00WeFfpBC_1sLFKcsFnSUIo9LpDRhXB3-X8FHkbPPFEDcAsbrhWM6coFp1a85ihCtPCGGf6879dSE92ITBsSSMtJ61afMXIa6kN7-N8PMK2pONHu0SBlWR8azcF3LTmyI3REU',
  'https://lh3.googleusercontent.com/aida-public/AB6AXuDNtHa1K-TiCkEx-htH7nymWQll8FvRogEMOhdtZSnhXl6hawefYXu1JVu4Lw3LbsOvEmR6r1ACgL0tCVb1vGWNcKzqLjhCf_IG_YRZBpTEM5Cr62i7ZvC2Eg6ve-p6SDWdcdSQmBnUmSbGNSNUIerIVDwt369Y69qrzXivI0HNRlp4TJIj8-lX87doL1uuAua6fAKq-ps53SpB1MJEXD4iYwZ5d9YwFc1ov9Z7sK508jK4aMu6-U9bUZezeMv95_dlyNe4pGimwe4',
  'https://lh3.googleusercontent.com/aida-public/AB6AXuDjXskEbc4Q-6mXEeHzoWOcCN_dg0mUi16Ic7794n0B3othaZktwHah3Gwrlmp9uytee4ljbQeVQDOgTjxXcKS94nLyAgc_16F3r_K74ozh9zfrPzWPP-KrnY4XQ-Og9CJsi5KIcQgdwZiAPg32HQJpDHLrEiJ1-Ys9xrr9JNcKf6EW9mEpiHthmHozRblSzh5puTTddRqOdqzGn69lFq_Tjq3h40_TcYkAAT6GCdHE9gmBeTp0Us4Soc9vGOoxrisdxkB5nog2X_o'
]

const totalBooks = computed(() => books.value.length)
const totalReads = computed(() => books.value.reduce((sum, item) => sum + Number(item.readCount || 0), 0))
const totalWords = computed(() => books.value.reduce((sum, item) => sum + Number(item.wordCount || 0), 0))
const storageUsagePercent = computed(() => {
  const basis = totalWords.value / 180000
  const percent = Math.round(Math.max(18, Math.min(64, basis || 18)))
  return percent
})
const usedStorageLabel = computed(() => `${(storageUsagePercent.value * 0.194).toFixed(1)} GB`)
const todayReaders = computed(() => Math.max(0, Math.round(totalReads.value * 0.685)))
const pendingReviewCount = computed(() => importJobs.value.filter((item) => item.status && item.status !== 'published').length)

const normalizedBooks = computed(() => {
  const maxReadCount = Math.max(...books.value.map((item) => Number(item.readCount || 0)), 1)
  return books.value.map((book, index) => {
    const normalizedCategory = normalizeCategory(book.category)
    const categoryPreset = resolveCategoryPreset(normalizedCategory, index)
    const statusKey = resolveStatusKey(book, index)
    const statusPreset = resolveStatusPreset(statusKey)
    const readCount = Number(book.readCount || 0)
    return {
      ...book,
      coverImage: resolveCoverImageUrl(book, index),
      refCode: `REF-${String(Number(book.id || index + 7000)).padStart(4, '0')}`,
      categoryKey: categoryPreset.key,
      categoryLabel: categoryPreset.label,
      categoryTone: categoryPreset.tone,
      statusKey,
      statusLabel: statusPreset.label,
      statusTone: statusPreset.tone,
      readCount,
      readPercent: Math.max(16, Math.round((readCount / maxReadCount) * 100)),
      displayDate: formatDisplayDate(book.publishedAt)
    }
  })
})

const filteredBooks = computed(() => {
  const currentFilter = resolveActiveFilter()
  let list = normalizedBooks.value

  if (activeTab.value !== 'all') {
    list = list.filter((item) => item.statusKey === activeTab.value)
  }

  if (currentFilter !== 'all') {
    list = list.filter((item) => item.categoryTone === currentFilter)
  }

  list = [...list].sort((left, right) => {
    const leftValue = String(left.publishedAt || '')
    const rightValue = String(right.publishedAt || '')
    return sortDescending.value ? rightValue.localeCompare(leftValue) : leftValue.localeCompare(rightValue)
  })

  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredBooks.value.length / pageSize)))
const pagedBooks = computed(() => filteredBooks.value.slice((page.value - 1) * pageSize, page.value * pageSize))
const pageRange = computed(() => {
  if (!filteredBooks.value.length) {
    return { start: 0, end: 0 }
  }
  return {
    start: (page.value - 1) * pageSize + 1,
    end: Math.min(page.value * pageSize, filteredBooks.value.length)
  }
})

const paginationItems = computed(() => buildPaginationItems(page.value, totalPages.value))

onMounted(async () => {
  await Promise.all([loadBooks(), loadImportJobs()])
})

watch([page, filteredBooks], () => {
  if (page.value > totalPages.value) {
    page.value = totalPages.value
  }
}, { immediate: true })

watch(activeTab, () => {
  page.value = 1
})

async function loadBooks() {
  loading.value = true
  errorMessage.value = ''
  try {
    const list = await getAdminBooks()
    books.value = Array.isArray(list) ? list : []
  } catch (error) {
    errorMessage.value = error.message || '加载书籍总览失败'
  } finally {
    loading.value = false
  }
}

async function loadImportJobs() {
  try {
    const list = await listRecentImportJobs(50)
    importJobs.value = Array.isArray(list) ? list : []
  } catch (error) {
    if (!errorMessage.value) {
      errorMessage.value = error.message || '加载导入任务失败'
    }
  }
}

function cycleFilter() {
  filterCycleIndex.value = (filterCycleIndex.value + 1) % 4
  page.value = 1
}

function toggleSortDirection() {
  sortDescending.value = !sortDescending.value
  page.value = 1
}

function resolveActiveFilter() {
  return ['all', 'blue', 'gray', 'orange'][filterCycleIndex.value] || 'all'
}

function normalizeCategory(category) {
  return String(category || '').trim().toLowerCase()
}

function resolveCategoryPreset(category, index) {
  if (/(tech|技术|program|开发|engineer|java|前端|后端)/.test(category)) {
    return { key: 'tech', label: 'TECH', tone: 'blue' }
  }
  if (/(design|设计|art|ui|ux)/.test(category)) {
    return { key: 'design', label: 'DESIGN', tone: 'gray' }
  }
  if (/(fiction|小说|文学|科幻|奇幻|story)/.test(category)) {
    return { key: 'fiction', label: 'FICTION', tone: 'orange' }
  }
  return [
    { key: 'tech', label: 'TECH', tone: 'blue' },
    { key: 'design', label: 'DESIGN', tone: 'gray' },
    { key: 'fiction', label: 'FICTION', tone: 'orange' }
  ][index % 3]
}

function resolveStatusKey(book, index) {
  const matchedJob = importJobs.value.find((item) => item.bookKey && String(item.bookKey) === String(book.id))
  if (matchedJob) {
    if (matchedJob.status === 'published') {
      return 'published'
    }
    if (matchedJob.status === 'await_review') {
      return 'review'
    }
    return 'draft'
  }

  if (index === 1) {
    return 'draft'
  }
  return 'published'
}

function resolveStatusPreset(statusKey) {
  if (statusKey === 'review') {
    return { label: '审核中', tone: 'review' }
  }
  if (statusKey === 'draft') {
    return { label: '草稿', tone: 'draft' }
  }
  return { label: '已发布', tone: 'published' }
}

function resolveCoverImageUrl(book, index) {
  if (book.coverUrl) {
    return book.coverUrl
  }
  return sampleCoverMap[index % sampleCoverMap.length]
}

function resolveCoverImage(book) {
  return book.coverImage || sampleCoverMap[0]
}

function formatCompactNumber(value) {
  const count = Number(value || 0)
  return count.toLocaleString('en-US')
}

function formatDisplayDate(value) {
  if (!value) {
    return '--'
  }
  const datePart = String(value).slice(0, 10)
  return datePart.replace(/-/g, '.')
}

function buildPaginationItems(currentPage, pageCount) {
  if (pageCount <= 5) {
    return Array.from({ length: pageCount }, (_, index) => ({
      key: `page-${index + 1}`,
      type: 'page',
      value: index + 1,
      label: index + 1
    }))
  }

  const items = [
    { key: 'page-1', type: 'page', value: 1, label: 1 }
  ]

  if (currentPage > 3) {
    items.push({ key: 'ellipsis-left', type: 'ellipsis', label: '...' })
  }

  const middlePages = [currentPage, currentPage + 1].filter((value) => value > 1 && value < pageCount)
  middlePages.forEach((value) => {
    if (!items.some((item) => item.value === value)) {
      items.push({ key: `page-${value}`, type: 'page', value, label: value })
    }
  })

  if (currentPage + 1 < pageCount - 1) {
    items.push({ key: 'ellipsis-right', type: 'ellipsis', label: '...' })
  }

  if (!items.some((item) => item.value === pageCount)) {
    items.push({ key: `page-${pageCount}`, type: 'page', value: pageCount, label: pageCount })
  }

  return items
}

function goImportPage() {
  router.push('/admin/books-import')
}

function openBookDetail(book) {
  router.push(`/book/${book.id}`)
}
</script>

<style scoped>
.book-overview-shell {
  color: #dde4e1;
  min-height: 100vh;
  padding: 8px 0 0;
  background: #0e1513;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
}

.book-overview-alert {
  margin-bottom: 20px;
  padding: 14px 18px;
  border: 1px solid rgba(255, 180, 171, 0.18);
  border-radius: 16px;
  background: rgba(147, 0, 10, 0.2);
  color: #ffdad6;
  font-size: 13px;
}

.book-overview-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 28px;
  margin-bottom: 30px;
}

.book-overview-stat-card {
  position: relative;
  overflow: hidden;
  min-height: 248px;
  padding: 28px;
  border: 1px solid rgba(60, 74, 70, 0.46);
  border-radius: 22px;
  background: linear-gradient(145deg, rgba(30, 41, 59, 0.4) 0%, rgba(15, 23, 42, 0.4) 100%);
  backdrop-filter: blur(20px);
}

.book-overview-stat-glow::after {
  content: '';
  position: absolute;
  top: -20%;
  right: -10%;
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, rgba(87, 241, 219, 0.05) 0%, transparent 70%);
  pointer-events: none;
}

.book-overview-stat-card-accent {
  border-top: 1px solid rgba(87, 241, 219, 0.2);
}

.book-overview-stat-card-cta {
  border-color: rgba(87, 241, 219, 0.2);
  background: linear-gradient(145deg, rgba(30, 41, 59, 0.45) 0%, rgba(15, 23, 42, 0.45) 100%);
}

.book-overview-stat-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
}

.book-overview-stat-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
}

.book-overview-stat-icon svg {
  width: 22px;
  height: 22px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.book-overview-stat-icon-primary {
  background: rgba(87, 241, 219, 0.12);
  color: #57f1db;
}

.book-overview-stat-icon-blue {
  background: rgba(5, 102, 217, 0.22);
  color: #adc6ff;
}

.book-overview-stat-icon-orange {
  background: rgba(255, 172, 90, 0.16);
  color: #ffb875;
}

.book-overview-stat-icon-solid {
  background: #57f1db;
  color: #003731;
}

.book-overview-stat-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.book-overview-stat-topvalue {
  color: #57f1db;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.2;
}

.book-overview-stat-topvalue-blue {
  color: #adc6ff;
}

.book-overview-stat-topvalue-light {
  color: #dde4e1;
}

.book-overview-stat-toplabel {
  color: #bacac5;
  font-size: 10px;
  font-weight: 600;
  line-height: 1.2;
}

.book-overview-stat-value {
  color: #dde4e1;
  font-size: 38px;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.02em;
}

.book-overview-stat-label {
  margin-top: 6px;
  color: #bacac5;
  font-size: 13px;
  line-height: 1.4;
}

.book-overview-stat-label-spaced {
  margin-bottom: 20px;
}

.book-overview-storage-bar {
  width: 100%;
  height: 8px;
  margin-top: 18px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
}

.book-overview-storage-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #57f1db;
  box-shadow: 0 0 10px rgba(87, 241, 219, 0.5);
}

.book-overview-cta-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  margin-top: 6px;
  padding: 11px 14px;
  border: none;
  border-radius: 12px;
  background: rgba(87, 241, 219, 0.1);
  color: #57f1db;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s ease, box-shadow 0.2s ease;
}

.book-overview-cta-btn:hover {
  background: rgba(87, 241, 219, 0.18);
  box-shadow: 0 0 20px rgba(87, 241, 219, 0.22);
}

.book-overview-cta-btn svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.book-overview-table-card {
  overflow: hidden;
  border: 1px solid rgba(60, 74, 70, 0.46);
  border-radius: 22px;
  background: linear-gradient(145deg, rgba(30, 41, 59, 0.4) 0%, rgba(15, 23, 42, 0.4) 100%);
  box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(20px);
}

.book-overview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 28px 30px;
  border-bottom: 1px solid rgba(60, 74, 70, 0.24);
}

.book-overview-tabs {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px;
  border-radius: 16px;
  background: #09100e;
}

.book-overview-tab {
  min-width: 84px;
  padding: 11px 20px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: #dde4e1;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.book-overview-tab:not(.active) {
  color: rgba(221, 228, 225, 0.84);
}

.book-overview-tab.active {
  background: #57f1db;
  color: #003731;
  box-shadow: 0 0 20px rgba(87, 241, 219, 0.3);
}

.book-overview-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.book-overview-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 11px 18px;
  border: 1px solid rgba(60, 74, 70, 0.34);
  border-radius: 16px;
  background: #242b2a;
  color: #dde4e1;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.book-overview-action-btn:hover {
  border-color: rgba(87, 241, 219, 0.18);
  background: #2f3634;
}

.book-overview-action-btn svg,
.book-overview-menu-btn svg,
.book-overview-page-icon svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.book-overview-menu-btn svg {
  fill: currentColor;
  stroke: none;
}

.book-overview-table-wrap {
  overflow-x: auto;
}

.book-overview-table {
  width: 100%;
  border-collapse: collapse;
}

.book-overview-table th {
  padding: 22px 30px;
  color: rgba(186, 202, 197, 0.58);
  font-size: 10px;
  font-weight: 700;
  text-align: left;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  border-bottom: 1px solid rgba(60, 74, 70, 0.16);
}

.book-overview-table td {
  padding: 18px 30px;
  border-bottom: 1px solid rgba(60, 74, 70, 0.08);
  vertical-align: middle;
}

.book-overview-row {
  transition: background 0.2s ease;
}

.book-overview-row:hover {
  background: rgba(87, 241, 219, 0.03);
}

.book-overview-book-meta {
  display: flex;
  align-items: center;
  gap: 20px;
}

.book-overview-cover-frame {
  width: 52px;
  height: 66px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.35);
}

.book-overview-cover-frame img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s ease;
}

.book-overview-row:hover .book-overview-cover-frame img {
  transform: scale(1.04);
}

.book-overview-book-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.book-overview-book-title {
  color: #dde4e1;
  font-size: 15px;
  font-weight: 700;
  transition: color 0.2s ease;
}

.book-overview-row:hover .book-overview-book-title {
  color: #57f1db;
}

.book-overview-book-ref {
  color: #bacac5;
  font-family: "JetBrains Mono", "Consolas", monospace;
  font-size: 12px;
}

.book-overview-author,
.book-overview-date {
  color: #bacac5;
  font-size: 14px;
  font-weight: 600;
}

.book-overview-date {
  font-size: 13px;
  font-weight: 500;
}

.book-overview-category-pill {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border-radius: 9px;
  border: 1px solid transparent;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.book-overview-category-pill.is-blue {
  background: rgba(5, 102, 217, 0.12);
  border-color: rgba(173, 198, 255, 0.18);
  color: #adc6ff;
}

.book-overview-category-pill.is-gray {
  background: rgba(221, 228, 225, 0.08);
  border-color: rgba(133, 148, 144, 0.22);
  color: #bacac5;
}

.book-overview-category-pill.is-orange {
  background: rgba(255, 172, 90, 0.1);
  border-color: rgba(255, 184, 117, 0.2);
  color: #ffb875;
}

.book-overview-read-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.book-overview-read-value {
  color: #dde4e1;
  font-size: 16px;
  font-weight: 700;
}

.book-overview-read-track {
  width: 76px;
  height: 5px;
  overflow: hidden;
  border-radius: 999px;
  background: #2f3634;
}

.book-overview-read-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #57f1db;
}

.book-overview-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid rgba(60, 74, 70, 0.32);
  font-size: 12px;
  font-weight: 700;
}

.book-overview-status-pill.is-published {
  background: rgba(87, 241, 219, 0.05);
  border-color: rgba(87, 241, 219, 0.2);
  color: #57f1db;
}

.book-overview-status-pill.is-draft {
  background: rgba(221, 228, 225, 0.04);
  border-color: rgba(133, 148, 144, 0.18);
  color: #bacac5;
}

.book-overview-status-pill.is-review {
  background: rgba(255, 172, 90, 0.08);
  border-color: rgba(255, 184, 117, 0.2);
  color: #ffb875;
}

.book-overview-status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 8px currentColor;
}

.book-overview-align-right {
  text-align: right;
}

.book-overview-menu-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #bacac5;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease;
}

.book-overview-menu-btn:hover {
  color: #57f1db;
  background: rgba(255, 255, 255, 0.03);
}

.book-overview-empty {
  padding: 32px 20px;
  color: #bacac5;
  font-size: 13px;
  text-align: center;
}

.book-overview-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 28px 30px;
  border-top: 1px solid rgba(60, 74, 70, 0.16);
  background: rgba(9, 16, 14, 0.3);
}

.book-overview-footer-copy {
  color: #bacac5;
  font-size: 12px;
  font-weight: 600;
}

.book-overview-pagination {
  display: flex;
  align-items: center;
  gap: 8px;
}

.book-overview-page-icon,
.book-overview-page-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  height: 34px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: #bacac5;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.book-overview-page-btn.active {
  background: #57f1db;
  color: #003731;
  box-shadow: 0 0 20px rgba(87, 241, 219, 0.3);
}

.book-overview-page-btn:not(.active):not(.ellipsis):hover,
.book-overview-page-icon:hover:not(:disabled) {
  color: #dde4e1;
  background: rgba(255, 255, 255, 0.04);
}

.book-overview-page-btn.ellipsis {
  cursor: default;
  color: rgba(186, 202, 197, 0.4);
}

.book-overview-page-icon:disabled {
  opacity: 0.3;
  cursor: default;
}

@media (max-width: 1400px) {
  .book-overview-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .book-overview-stats {
    grid-template-columns: 1fr;
  }

  .book-overview-toolbar,
  .book-overview-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .book-overview-actions,
  .book-overview-pagination {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .book-overview-shell {
    padding-top: 0;
  }

  .book-overview-stat-card,
  .book-overview-toolbar,
  .book-overview-footer {
    padding: 20px;
  }

  .book-overview-table th,
  .book-overview-table td {
    padding: 16px 18px;
  }

  .book-overview-tabs {
    flex-wrap: wrap;
  }

  .book-overview-tab {
    min-width: 72px;
    flex: 1 1 40%;
  }
}
</style>
