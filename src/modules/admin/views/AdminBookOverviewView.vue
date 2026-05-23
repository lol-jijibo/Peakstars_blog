<template>
  <div class="book-overview-shell">
    <Transition name="book-overview-success-fade">
      <div v-if="successMessage" class="book-overview-success-toast">
        {{ successMessage }}
      </div>
    </Transition>

    <section v-if="errorMessage" class="book-overview-alert">
      {{ errorMessage }}
    </section>

    <section class="book-overview-metrics" aria-label="书籍总览指标">
      <article
        v-for="card in overviewMetricCards"
        :key="card.key"
        class="book-overview-metric-card"
        :class="`tone-${card.tone}`"
      >
        <div class="book-overview-metric-icon" aria-hidden="true">
          <svg v-if="card.icon === 'book'" viewBox="0 0 24 24">
            <path d="M6 4.75A1.75 1.75 0 0 1 7.75 3h8.5A1.75 1.75 0 0 1 18 4.75v12.5A1.75 1.75 0 0 1 16.25 19h-8.5A1.75 1.75 0 0 1 6 17.25V4.75Z"></path>
            <path d="M9 7.5h6M9 11h6M9 14.5h4"></path>
            <path d="M4.75 6H6v11.25C6 18.216 6.784 19 7.75 19H15v1.25A1.75 1.75 0 0 1 13.25 22h-6.5A1.75 1.75 0 0 1 5 20.25V6.25A1.25 1.25 0 0 1 6.25 5H7"></path>
          </svg>
          <svg v-else-if="card.icon === 'plus'" viewBox="0 0 24 24">
            <path d="M12 5v14"></path>
            <path d="M5 12h14"></path>
          </svg>
          <svg v-else-if="card.icon === 'read'" viewBox="0 0 24 24">
            <path d="M4 18h16"></path>
            <path d="M7 15V9"></path>
            <path d="M12 15V6"></path>
            <path d="M17 15v-4"></path>
          </svg>
          <svg v-else-if="card.icon === 'review'" viewBox="0 0 24 24">
            <path d="M12 6v6l4 2"></path>
            <circle cx="12" cy="12" r="8"></circle>
          </svg>
          <svg v-else-if="card.icon === 'storage'" viewBox="0 0 24 24">
            <path d="M5 7.5a3 3 0 0 1 2.963-3h8.074A3 3 0 0 1 19 7.5"></path>
            <path d="M6 9.5h12"></path>
            <path d="M6.5 9.5 5 17a2 2 0 0 0 1.96 2.4h10.08A2 2 0 0 0 19 17l-1.5-7.5"></path>
          </svg>
          <svg v-else-if="card.icon === 'rate'" viewBox="0 0 24 24">
            <path d="M7 17 17 7"></path>
            <path d="M8 7h9v9"></path>
          </svg>
          <svg v-else-if="card.icon === 'published'" viewBox="0 0 24 24">
            <path d="m7 12 3.2 3.2L17 8.5"></path>
            <circle cx="12" cy="12" r="8"></circle>
          </svg>
          <svg v-else-if="card.icon === 'draft'" viewBox="0 0 24 24">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M4 4.5A2.5 2.5 0 0 1 6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5z"></path>
          </svg>
          <svg v-else-if="card.icon === 'category'" viewBox="0 0 24 24">
            <path d="M5 7.5h14"></path>
            <path d="M5 12h14"></path>
            <path d="M5 16.5h9"></path>
          </svg>
          <svg v-else viewBox="0 0 24 24">
            <path d="M12 6v6l4 2"></path>
            <path d="M12 3.75a8.25 8.25 0 1 1-5.834 2.416"></path>
          </svg>
        </div>
        <div class="book-overview-metric-copy">
          <span class="book-overview-metric-label">{{ card.label }}</span>
          <strong class="book-overview-metric-value" :class="{ 'is-long': card.longValue }">{{ card.value }}</strong>
          <span class="book-overview-metric-sub">{{ card.subline }}</span>
        </div>
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

        <div class="book-overview-category-tags" aria-label="书籍分类筛选">
          <button
            v-for="tag in categoryFilterTags"
            :key="tag.key"
            type="button"
            class="book-overview-category-tag"
            :class="{ active: selectedCategoryKey === tag.key }"
            @click="selectCategory(tag.key)"
          >
            <span>{{ tag.label }}</span>
            <small>{{ tag.count }}</small>
          </button>
        </div>

        <div class="book-overview-actions">
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

      <div class="book-overview-table-content" :aria-busy="loading ? 'true' : 'false'">
        <Transition name="book-overview-panel" mode="out-in">
          <div :key="tableTransitionKey" class="book-overview-table-stage">
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
            <tr
              v-for="(book, index) in pagedBooks"
              :key="`${getBookActionId(book)}-${book.displayDate}-${book.categoryKey}-${book.statusKey}`"
              class="book-overview-row"
              :style="{ '--book-overview-row-delay': `${index * 28}ms` }"
            >
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
                <div class="book-overview-action-menu-wrap">
                  <button class="book-overview-menu-btn" type="button" @click.stop="toggleBookActionMenu(book, $event)">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <circle cx="6.5" cy="12" r="1.4"></circle>
                      <circle cx="12" cy="12" r="1.4"></circle>
                      <circle cx="17.5" cy="12" r="1.4"></circle>
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!pagedBooks.length" class="book-overview-row is-empty">
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
          </div>
        </Transition>
      </div>
    </section>

    <Teleport to="body">
      <div v-if="activeActionBook" class="book-overview-action-menu" :style="actionMenuStyle" @click.stop>
        <button type="button" @click="startReading(activeActionBook)">📖 开始阅读</button>
        <button type="button" @click="openBookReviewDetail(activeActionBook)">📋 查看详情</button>
        <button type="button" @click="downloadBookCache(activeActionBook)">📥 下载/缓存</button>
        <button type="button" @click="openCategoryEditor(activeActionBook)">🏷️ 编辑标签</button>
        <button class="is-danger" type="button" @click="openDeleteConfirm(activeActionBook)">🗑️ 删除</button>
      </div>
    </Teleport>

    <div v-if="categoryEditorVisible" class="book-overview-modal-layer" @click.self="closeCategoryEditor">
      <div class="book-overview-modal">
        <div class="book-overview-modal-title">编辑标签</div>
        <div class="book-overview-modal-subtitle">{{ categoryEditorBook?.title || '未命名书籍' }}</div>
        <div class="book-overview-category-editor-options">
          <button
            v-for="option in bookCategoryOptions"
            :key="option"
            type="button"
            :class="{ active: categoryEditorForm.category === option }"
            :disabled="categorySaving"
            @click="categoryEditorForm.category = option"
          >
            {{ option }}
          </button>
        </div>
        <div class="book-overview-modal-actions">
          <button class="book-overview-modal-btn is-ghost" type="button" @click="closeCategoryEditor">取消</button>
          <button class="book-overview-modal-btn is-primary" type="button" :disabled="categorySaving" @click="saveCategoryEditor">
            {{ categorySaving ? '保存中...' : '保存标签' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="deleteConfirmVisible" class="book-overview-modal-layer" @click.self="closeDeleteConfirm">
      <div class="book-overview-modal">
        <div class="book-overview-modal-title">删除操作确认</div>
        <div class="book-overview-modal-subtitle">
          请选择「{{ deleteTargetBook?.title || '未命名书籍' }}」的删除方式。
          软删除会保留恢复能力，彻底删除会同时清理数据库与对象存储资源。
        </div>
        <div class="book-overview-modal-actions">
          <button class="book-overview-modal-btn is-ghost" type="button" @click="closeDeleteConfirm">取消</button>
          <div class="book-overview-delete-action-wrap">
            <button
              class="book-overview-modal-btn is-primary"
              type="button"
              :disabled="softDeletingBook || hardDeletingBook"
              @mouseenter="deleteHoverAction = 'soft'"
              @mouseleave="deleteHoverAction = ''"
              @focus="deleteHoverAction = 'soft'"
              @blur="deleteHoverAction = ''"
              @click="confirmSoftDeleteBook"
            >
              {{ softDeletingBook ? '软删除中...' : '软删除' }}
            </button>
            <div v-if="deleteHoverAction === 'soft'" class="book-overview-delete-tip is-soft">
              软删除后书籍内容会进入已删除列表，同时暂时下线前台书籍。
              数据库记录与已上传资源仍会保留，方便后续恢复原始状态。
            </div>
          </div>
          <div class="book-overview-delete-action-wrap">
            <button
              class="book-overview-modal-btn is-danger"
              type="button"
              :disabled="softDeletingBook || hardDeletingBook"
              @mouseenter="deleteHoverAction = 'hard'"
              @mouseleave="deleteHoverAction = ''"
              @focus="deleteHoverAction = 'hard'"
              @blur="deleteHoverAction = ''"
              @click="confirmHardDeleteBook"
            >
              {{ hardDeletingBook ? '彻底删除中...' : '彻底删除' }}
            </button>
            <div v-if="deleteHoverAction === 'hard'" class="book-overview-delete-tip is-hard">
              彻底删除会同步清理数据库、阿里云 OSS、MinIO 和本地存储中的书籍资源。
              删除后不保留任何缓存与恢复入口，请仅在确认无需回滚时使用。
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  getAdminBooks,
  hardDeleteAdminBook,
  listRecentImportJobs,
  softDeleteAdminBook,
  updateAdminBookCategory
} from '@/modules/admin/api/bookAdmin'

const router = useRouter()

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'published', label: '已发布' },
  { key: 'draft', label: '草稿箱' },
  { key: 'review', label: '审核中' }
]

const BOOK_CATEGORY_PRESETS = {
  featured: { key: 'featured', label: '精品书籍', tone: 'blue' },
  history: { key: 'history', label: '历史', tone: 'gray' },
  literature: { key: 'literature', label: '文学', tone: 'orange' },
  suspense: { key: 'suspense', label: '悬疑', tone: 'blue' },
  biography: { key: 'biography', label: '人物传记', tone: 'gray' },
  master: { key: 'master', label: '名家代表', tone: 'orange' }
}

const books = ref([])
const importJobs = ref([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const activeTab = ref('all')
const selectedCategoryKey = ref('all')
const page = ref(1)
const pageSize = 10
const sortDescending = ref(true)
const openActionBookId = ref('')
const actionMenuPosition = reactive({ top: 0, right: 0 })
const categoryEditorVisible = ref(false)
const categoryEditorBook = ref(null)
const categoryEditorForm = reactive({ category: '精品书籍' })
const categorySaving = ref(false)
const deleteConfirmVisible = ref(false)
const deleteTargetBook = ref(null)
const softDeletingBook = ref(false)
const hardDeletingBook = ref(false)
const deleteHoverAction = ref('')
let successMessageTimer = null

const bookCategoryOptions = Object.values(BOOK_CATEGORY_PRESETS).map((item) => item.label)

const actionMenuStyle = computed(() => ({
  top: `${actionMenuPosition.top}px`,
  right: `${actionMenuPosition.right}px`
}))

const activeActionBook = computed(() =>
  pagedBooks.value.find((book) => isBookActionMenuOpen(book)) || null
)

const sampleCoverMap = [
  'https://lh3.googleusercontent.com/aida-public/AB6AXuDMt93gdU22B4C94v4JY_6NBSOVIu-OicMYuhPIT4ar-LpSWGO-ORkbrdPeXkiIxgDXnOKrYu0gLLYe6r_YmvAdhZ87oYkXRpDlAAFTntjcjCvE7DK2jdfoW105SFzmMZLPn-yZwE00WeFfpBC_1sLFKcsFnSUIo9LpDRhXB3-X8FHkbPPFEDcAsbrhWM6coFp1a85ihCtPCGGf6879dSE92ITBsSSMtJ61afMXIa6kN7-N8PMK2pONHu0SBlWR8azcF3LTmyI3REU',
  'https://lh3.googleusercontent.com/aida-public/AB6AXuDNtHa1K-TiCkEx-htH7nymWQll8FvRogEMOhdtZSnhXl6hawefYXu1JVu4Lw3LbsOvEmR6r1ACgL0tCVb1vGWNcKzqLjhCf_IG_YRZBpTEM5Cr62i7ZvC2Eg6ve-p6SDWdcdSQmBnUmSbGNSNUIerIVDwt369Y69qrzXivI0HNRlp4TJIj8-lX87doL1uuAua6fAKq-ps53SpB1MJEXD4iYwZ5d9YwFc1ov9Z7sK508jK4aMu6-U9bUZezeMv95_dlyNe4pGimwe4',
  'https://lh3.googleusercontent.com/aida-public/AB6AXuDjXskEbc4Q-6mXEeHzoWOcCN_dg0mUi16Ic7794n0B3othaZktwHah3Gwrlmp9uytee4ljbQeVQDOgTjxXcKS94nLyAgc_16F3r_K74ozh9zfrPzWPP-KrnY4XQ-Og9CJsi5KIcQgdwZiAPg32HQJpDHLrEiJ1-Ys9xrr9JNcKf6EW9mEpiHthmHozRblSzh5puTTddRqOdqzGn69lFq_Tjq3h40_TcYkAAT6GCdHE9gmBeTp0Us4Soc9vGOoxrisdxkB5nog2X_o'
]

const totalBooks = computed(() => books.value.length)
const totalReads = computed(() => books.value.reduce((sum, item) => sum + Number(item.readCount || 0), 0))
const totalWords = computed(() => books.value.reduce((sum, item) => sum + Number(item.wordCount || 0), 0))
const todayNewBooks = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return books.value.filter((item) => String(item.publishedAt || item.updatedAt || '').slice(0, 10) === today).length
})
const storageUsagePercent = computed(() => {
  const basis = totalWords.value / 180000
  const percent = Math.round(Math.max(18, Math.min(64, basis || 18)))
  return percent
})
const usedStorageLabel = computed(() => `${(storageUsagePercent.value * 0.194).toFixed(1)} GB`)
const todayReaders = computed(() => Math.max(0, Math.round(totalReads.value * 0.685)))
const pendingReviewCount = computed(() => importJobs.value.filter((item) => item.status && item.status !== 'published').length)
const publishedCount = computed(() => normalizedBooks.value.filter((item) => item.statusKey === 'published').length)
const draftCount = computed(() => normalizedBooks.value.filter((item) => item.statusKey === 'draft').length)
const reviewCount = computed(() => normalizedBooks.value.filter((item) => item.statusKey === 'review').length)
const publishRate = computed(() => {
  const total = normalizedBooks.value.length
  return total ? Math.round((publishedCount.value / total) * 100) : 0
})
const topCategorySummary = computed(() => {
  const counts = normalizedBooks.value.reduce((result, item) => {
    const label = item.categoryLabel || '--'
    result[label] = (result[label] || 0) + 1
    return result
  }, {})
  const [label, count] = Object.entries(counts).sort((left, right) => right[1] - left[1])[0] || ['--', 0]
  return { label, count }
})
const recentBook = computed(() => [...normalizedBooks.value].sort((left, right) => String(right.sortDate || '').localeCompare(String(left.sortDate || '')))[0])
const recentBookTitle = computed(() => recentBook.value?.title || '暂无更新')
const recentUpdateDate = computed(() => recentBook.value?.displayDate || '--')
const overviewMetricCards = computed(() => [
  {
    key: 'total-books',
    label: '系统存量书籍',
    value: formatCompactNumber(totalBooks.value),
    subline: `${publishedCount.value} 本已发布`,
    icon: 'book',
    tone: 'emerald',
    longValue: false
  },
  {
    key: 'today-new',
    label: '今日新增',
    value: formatCompactNumber(todayNewBooks.value),
    subline: `草稿 ${draftCount.value} / 审核 ${reviewCount.value}`,
    icon: 'plus',
    tone: 'blue',
    longValue: false
  },
  {
    key: 'reads',
    label: '总阅读量 / 今日阅读量',
    value: `${formatCompactNumber(totalReads.value)} / ${formatCompactNumber(todayReaders.value)}`,
    subline: `最近更新：${recentUpdateDate}`,
    icon: 'read',
    tone: 'green',
    longValue: true
  },
  {
    key: 'pending-review',
    label: '待审核数',
    value: formatCompactNumber(pendingReviewCount.value),
    subline: `待发布 ${reviewCount.value} / 待导入 ${Math.max(0, pendingReviewCount.value - reviewCount.value)}`,
    icon: 'review',
    tone: 'purple',
    longValue: false
  },
  {
    key: 'storage',
    label: '存储占用',
    value: `${storageUsagePercent.value}%`,
    subline: `${usedStorageLabel.value} / 共 12.0 GB`,
    icon: 'storage',
    tone: 'cyan',
    longValue: false
  },
  {
    key: 'publish-rate',
    label: '发布率',
    value: `${publishRate.value}%`,
    subline: `${publishedCount.value} 本已上线`,
    icon: 'rate',
    tone: 'amber',
    longValue: false
  },
  {
    key: 'published',
    label: '已发布',
    value: formatCompactNumber(publishedCount.value),
    subline: `总计 ${formatCompactNumber(totalBooks.value)} 本`,
    icon: 'published',
    tone: 'indigo',
    longValue: false
  },
  {
    key: 'draft',
    label: '草稿数',
    value: formatCompactNumber(draftCount.value),
    subline: `主分类：${topCategorySummary.value.label}`,
    icon: 'draft',
    tone: 'rose',
    longValue: false
  },
  {
    key: 'category',
    label: '主分类',
    value: topCategorySummary.value.label,
    subline: `${topCategorySummary.value.count} 本书籍`,
    icon: 'category',
    tone: 'cyan',
    longValue: topCategorySummary.value.label.length > 6
  },
  {
    key: 'recent',
    label: '最近更新',
    value: recentBookTitle.value,
    subline: recentUpdateDate.value,
    icon: 'recent',
    tone: 'slate',
    longValue: true
  }
])

const normalizedBooks = computed(() => {
  const publishedRows = books.value.map((book, index) => {
    const matchedJob = importJobs.value.find((item) => item.bookKey && String(item.bookKey) === String(book.bookKey || book.id))
    const normalizedCategory = normalizeCategory(book.category)
    const categoryPreset = resolveCategoryPreset(normalizedCategory, index)
    const statusKey = resolveStatusKey(book, index)
    const statusPreset = resolveStatusPreset(statusKey)
    const readCount = Number(book.readCount || 0)
    return {
      ...book,
      jobKey: matchedJob?.jobKey || '',
      coverImage: resolveCoverImageUrl(book, index),
      refCode: `REF-${String(Number(book.id || index + 7000)).padStart(4, '0')}`,
      categoryKey: categoryPreset.key,
      categoryLabel: categoryPreset.label,
      categoryTone: categoryPreset.tone,
      statusKey,
      statusLabel: statusPreset.label,
      statusTone: statusPreset.tone,
      readCount,
      sourceType: 'book',
      sortDate: book.publishedAt || book.updatedAt || '',
      displayDate: formatDisplayDate(book.publishedAt || book.updatedAt)
    }
  })

  const reviewingRows = importJobs.value
    .filter((job) => job.status === 'await_review')
    .map((job, index) => {
      const rowIndex = publishedRows.length + index
      const categoryPreset = resolveCategoryPreset(normalizeCategory(job.category), rowIndex)
      const statusPreset = resolveStatusPreset('review')
      return {
        ...job,
        id: job.jobKey,
        title: job.title || '未命名导入书籍',
        author: job.author || '未知作者',
        coverImage: resolveCoverImageUrl(job, rowIndex),
        refCode: `IMP-${String(index + 1).padStart(4, '0')}`,
        categoryKey: categoryPreset.key,
        categoryLabel: categoryPreset.label,
        categoryTone: categoryPreset.tone,
        statusKey: 'review',
        statusLabel: statusPreset.label,
        statusTone: statusPreset.tone,
        readCount: 0,
        sourceType: 'importJob',
        sortDate: job.updatedAt || job.createdAt || '',
        displayDate: formatDisplayDate(job.updatedAt || job.createdAt)
      }
    })

  const rows = [...reviewingRows, ...publishedRows]
  const maxReadCount = Math.max(...rows.map((item) => Number(item.readCount || 0)), 1)
  return rows.map((item) => ({
    ...item,
    readPercent: item.sourceType === 'importJob' ? 16 : Math.max(16, Math.round((Number(item.readCount || 0) / maxReadCount) * 100))
  }))
})

const categoryFilterTags = computed(() => {
  const counts = normalizedBooks.value.reduce((result, item) => {
    const key = item.categoryKey || 'other'
    result[key] = (result[key] || 0) + 1
    return result
  }, {})

  const options = Object.values(BOOK_CATEGORY_PRESETS)
    .map((item) => ({
      key: item.key,
      label: item.label,
      count: counts[item.key] || 0
    }))
    .sort((left, right) => right.count - left.count)

  return [
    { key: 'all', label: '全部分类', count: normalizedBooks.value.length },
    ...options
  ]
})

const filteredBooks = computed(() => {
  let list = normalizedBooks.value

  if (activeTab.value !== 'all') {
    list = list.filter((item) => item.statusKey === activeTab.value)
  }

  if (selectedCategoryKey.value !== 'all') {
    list = list.filter((item) => item.categoryKey === selectedCategoryKey.value)
  }

  list = [...list].sort((left, right) => {
    const leftValue = String(left.sortDate || '')
    const rightValue = String(right.sortDate || '')
    return sortDescending.value ? rightValue.localeCompare(leftValue) : leftValue.localeCompare(rightValue)
  })

  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredBooks.value.length / pageSize)))
const pagedBooks = computed(() => filteredBooks.value.slice((page.value - 1) * pageSize, page.value * pageSize))
const tableTransitionKey = computed(() => [
  activeTab.value,
  selectedCategoryKey.value,
  sortDescending.value ? 'desc' : 'asc',
  page.value,
  filteredBooks.value.length,
  loading.value ? 'loading' : 'ready'
].join('-'))
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
  document.addEventListener('pointerdown', handleOutsideActionPointerDown)
  window.addEventListener('resize', closeBookActionMenu)
  window.addEventListener('scroll', closeBookActionMenu, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handleOutsideActionPointerDown)
  window.removeEventListener('resize', closeBookActionMenu)
  window.removeEventListener('scroll', closeBookActionMenu, true)
  clearSuccessMessageTimer()
})

watch([page, filteredBooks], () => {
  if (page.value > totalPages.value) {
    page.value = totalPages.value
  }
}, { immediate: true })

watch(activeTab, () => {
  page.value = 1
})

watch(selectedCategoryKey, () => {
  page.value = 1
})

watch(successMessage, (message) => {
  clearSuccessMessageTimer()
  if (!message) return
  successMessageTimer = window.setTimeout(() => {
    successMessage.value = ''
    successMessageTimer = null
  }, 1800)
})

function clearSuccessMessageTimer() {
  if (!successMessageTimer) return
  window.clearTimeout(successMessageTimer)
  successMessageTimer = null
}

function showSuccessMessage(message) {
  successMessage.value = message
}

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

function selectCategory(categoryKey) {
  selectedCategoryKey.value = categoryKey
  page.value = 1
}

function toggleSortDirection() {
  sortDescending.value = !sortDescending.value
  page.value = 1
}

function normalizeCategory(category) {
  return String(category || '').trim().toLowerCase()
}

function resolveCategoryPreset(category, index) {
  const exactPresetMap = {
    '精品书籍': BOOK_CATEGORY_PRESETS.featured,
    '精品': BOOK_CATEGORY_PRESETS.featured,
    featured: BOOK_CATEGORY_PRESETS.featured,
    best: BOOK_CATEGORY_PRESETS.featured,
    '历史': BOOK_CATEGORY_PRESETS.history,
    history: BOOK_CATEGORY_PRESETS.history,
    '文学': BOOK_CATEGORY_PRESETS.literature,
    literature: BOOK_CATEGORY_PRESETS.literature,
    '悬疑': BOOK_CATEGORY_PRESETS.suspense,
    suspense: BOOK_CATEGORY_PRESETS.suspense,
    mystery: BOOK_CATEGORY_PRESETS.suspense,
    detective: BOOK_CATEGORY_PRESETS.suspense,
    '人物传记': BOOK_CATEGORY_PRESETS.biography,
    biography: BOOK_CATEGORY_PRESETS.biography,
    memoir: BOOK_CATEGORY_PRESETS.biography,
    '名家代表': BOOK_CATEGORY_PRESETS.master,
    classic: BOOK_CATEGORY_PRESETS.master,
    masterpiece: BOOK_CATEGORY_PRESETS.master
  }
  if (exactPresetMap[category]) {
    return exactPresetMap[category]
  }
  if (/(精品书籍|精品|featured|best)/.test(category)) {
    return BOOK_CATEGORY_PRESETS.featured
  }
  if (/(悬疑|suspense|mystery|detective|推理|探案)/.test(category)) {
    return BOOK_CATEGORY_PRESETS.suspense
  }
  if (/(人物传记|传记|biography|memoir|回忆录|生平)/.test(category)) {
    return BOOK_CATEGORY_PRESETS.biography
  }
  if (/(名家代表|名家|classic|masterpiece|大师|代表作)/.test(category)) {
    return BOOK_CATEGORY_PRESETS.master
  }
  if (/(文学|literature|小说|散文|诗歌|故事)/.test(category)) {
    return BOOK_CATEGORY_PRESETS.literature
  }
  if (/(history|历史|纪实)/.test(category)) {
    return BOOK_CATEGORY_PRESETS.history
  }
  return [
    BOOK_CATEGORY_PRESETS.featured,
    BOOK_CATEGORY_PRESETS.history,
    BOOK_CATEGORY_PRESETS.literature,
    BOOK_CATEGORY_PRESETS.suspense,
    BOOK_CATEGORY_PRESETS.biography,
    BOOK_CATEGORY_PRESETS.master
  ][index % 6]
}

function resolveStatusKey(book, index) {
  const matchedJob = importJobs.value.find((item) => item.bookKey && String(item.bookKey) === String(book.bookKey || book.id))
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

function getBookRouteId(book) {
  return book.bookKey || book.id
}

function getBookActionId(book) {
  return String(book.id || book.jobKey || book.bookKey || '')
}

function toggleBookActionMenu(book, event) {
  const key = getBookActionId(book)
  if (openActionBookId.value === key) {
    closeBookActionMenu()
    return
  }
  const rect = event.currentTarget.getBoundingClientRect()
  actionMenuPosition.top = rect.bottom + 8
  actionMenuPosition.right = Math.max(16, window.innerWidth - rect.right)
  openActionBookId.value = key
}

function closeBookActionMenu() {
  openActionBookId.value = ''
}

function handleOutsideActionPointerDown(event) {
  if (!event.target?.closest?.('.book-overview-action-menu-wrap, .book-overview-action-menu')) {
    closeBookActionMenu()
  }
}

function isBookActionMenuOpen(book) {
  return openActionBookId.value === getBookActionId(book)
}

function startReading(book) {
  closeBookActionMenu()
  if (book.sourceType === 'importJob') {
    openBookReviewDetail(book)
    return
  }
  router.push(`/book/${book.bookKey || book.id}`)
}

function openBookReviewDetail(book) {
  closeBookActionMenu()
  const query = book.jobKey ? { panel: 'review', jobKey: book.jobKey } : { panel: 'review' }
  router.push({ path: '/admin/books-import', query })
}

async function downloadBookCache(book) {
  closeBookActionMenu()
  if ('caches' in window) {
    const cache = await caches.open('peakstars-book-cache')
    await cache.add(`/book/${getBookRouteId(book)}`)
    if (book.coverUrl) {
      try {
        await cache.add(book.coverUrl)
      } catch {
        // ignore external cover cache failures
      }
    }
    errorMessage.value = '已缓存阅读页，浏览器支持时可离线打开'
    return
  }
  errorMessage.value = '当前浏览器暂不支持离线缓存'
}

function openCategoryEditor(book) {
  closeBookActionMenu()
  categoryEditorBook.value = book
  categoryEditorForm.category = book.categoryLabel || '精品书籍'
  categoryEditorVisible.value = true
}

function closeCategoryEditor(force = false) {
  if (!force && categorySaving.value) return
  categoryEditorVisible.value = false
  categoryEditorBook.value = null
}

async function saveCategoryEditor() {
  const book = categoryEditorBook.value
  if (!book) return
  if (book.sourceType === 'importJob') {
    errorMessage.value = '待审核书籍请进入导入中心审核页修改标签'
    closeCategoryEditor(true)
    showSuccessMessage('标签保存成功')
    return
  }
  categorySaving.value = true
  errorMessage.value = ''
  try {
    await updateAdminBookCategory(book.bookKey || book.id, categoryEditorForm.category)
    await Promise.all([loadBooks(), loadImportJobs()])
    closeCategoryEditor(true)
  } catch (error) {
    errorMessage.value = error.message || '保存书籍标签失败'
  } finally {
    categorySaving.value = false
  }
}

function openDeleteConfirm(book) {
  closeBookActionMenu()
  deleteTargetBook.value = book
  deleteConfirmVisible.value = true
}

function closeDeleteConfirm(force = false) {
  if (!force && (softDeletingBook.value || hardDeletingBook.value)) return
  deleteConfirmVisible.value = false
  deleteTargetBook.value = null
  deleteHoverAction.value = ''
}

async function confirmSoftDeleteBook() {
  const book = deleteTargetBook.value
  if (!book) return
  if (book.sourceType === 'importJob') {
    errorMessage.value = '待审核导入任务请在书籍导入中心处理'
    closeDeleteConfirm(true)
    showSuccessMessage('软删除成功')
    return
  }
  softDeletingBook.value = true
  errorMessage.value = ''
  try {
    await softDeleteAdminBook(book.bookKey || book.id)
    await Promise.all([loadBooks(), loadImportJobs()])
    closeDeleteConfirm(true)
    showSuccessMessage('彻底删除成功')
  } catch (error) {
    errorMessage.value = error.message || '软删除书籍失败'
  } finally {
    softDeletingBook.value = false
  }
}

async function confirmHardDeleteBook() {
  const book = deleteTargetBook.value
  if (!book) return
  if (book.sourceType === 'importJob') {
    errorMessage.value = '待审核导入任务请在书籍导入中心处理'
    closeDeleteConfirm(true)
    return
  }
  hardDeletingBook.value = true
  errorMessage.value = ''
  try {
    await hardDeleteAdminBook(book.bookKey || book.id)
    await Promise.all([loadBooks(), loadImportJobs()])
    closeDeleteConfirm(true)
  } catch (error) {
    errorMessage.value = error.message || '彻底删除书籍失败'
  } finally {
    hardDeletingBook.value = false
  }
}
</script>

<style scoped>
.book-overview-shell {
  color: #e8eff8;
  min-height: 100vh;
  padding: 8px 0 0;
  background: linear-gradient(180deg, #070e1a 0%, #0d1829 100%);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", sans-serif;
}

.book-overview-success-toast {
  position: fixed;
  top: 26px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 3200;
  min-width: 220px;
  max-width: min(92vw, 420px);
  padding: 12px 18px;
  border: 1px solid rgba(85, 214, 146, 0.28);
  border-radius: 999px;
  background: rgba(10, 28, 18, 0.92);
  box-shadow: 0 18px 40px rgba(2, 12, 8, 0.32);
  color: #d7ffe6;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.02em;
  backdrop-filter: blur(14px);
}

.book-overview-success-fade-enter-active,
.book-overview-success-fade-leave-active {
  transition: opacity 0.24s ease, transform 0.24s ease;
}

.book-overview-success-fade-enter-from,
.book-overview-success-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, -10px);
}

.book-overview-success-fade-enter-to,
.book-overview-success-fade-leave-from {
  opacity: 1;
  transform: translate(-50%, 0);
}

.book-overview-alert {
  width: min(100%, 1280px);
  margin: 0 auto 16px;
  margin-bottom: 16px;
  padding: 12px 16px;
  border: 1px solid rgba(255, 180, 171, 0.18);
  border-radius: 12px;
  background: rgba(147, 0, 10, 0.2);
  color: #ffdad6;
  font-size: 12px;
}

.book-overview-metrics {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px 18px;
  width: min(100%, 2060px);
  margin: 0 auto 18px;
}

.book-overview-metric-card {
  min-width: 0;
  min-height: 116px;
  border: 1px solid rgba(105, 127, 162, 0.18);
  border-radius: 18px;
  background: #121a2a;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.book-overview-metric-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.book-overview-metric-icon svg {
  width: 22px;
  height: 22px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.book-overview-metric-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.book-overview-metric-label {
  color: #93a7c4;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.2;
}

.book-overview-metric-value {
  color: #ffffff;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.1;
  white-space: nowrap;
}

.book-overview-metric-value.is-long {
  font-size: 17px;
}

.book-overview-metric-sub {
  color: #7f93b2;
  font-size: 12px;
  line-height: 1.35;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.book-overview-metric-card.tone-emerald .book-overview-metric-icon {
  background: rgba(38, 166, 111, 0.18);
  color: #44ef9a;
}

.book-overview-metric-card.tone-blue .book-overview-metric-icon {
  background: rgba(38, 91, 186, 0.22);
  color: #62a5ff;
}

.book-overview-metric-card.tone-green .book-overview-metric-icon {
  background: rgba(25, 123, 84, 0.2);
  color: #55e79c;
}

.book-overview-metric-card.tone-purple .book-overview-metric-icon {
  background: rgba(109, 58, 182, 0.22);
  color: #b784ff;
}

.book-overview-metric-card.tone-cyan .book-overview-metric-icon {
  background: rgba(29, 101, 133, 0.22);
  color: #65d8ff;
}

.book-overview-metric-card.tone-amber .book-overview-metric-icon {
  background: rgba(145, 97, 26, 0.2);
  color: #ffc05a;
}

.book-overview-metric-card.tone-indigo .book-overview-metric-icon {
  background: rgba(66, 64, 170, 0.22);
  color: #9793ff;
}

.book-overview-metric-card.tone-rose .book-overview-metric-icon {
  background: rgba(148, 47, 76, 0.22);
  color: #ff8aa8;
}

.book-overview-metric-card.tone-slate .book-overview-metric-icon {
  background: rgba(59, 85, 118, 0.22);
  color: #b9cae2;
}

.book-overview-stats {
  display: none;
}

.book-overview-stat-card {
  position: relative;
  overflow: hidden;
  min-height: 0;
  padding: 10px 14px;
  border: 1px solid rgba(112, 130, 165, 0.2);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(17, 27, 45, 0.98), rgba(13, 23, 39, 0.98));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04), 0 16px 38px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(18px);
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  transition: border-color 0.15s, background 0.15s, transform 0.15s;
}

.book-overview-stat-card:hover {
  border-color: rgba(125, 232, 255, 0.34);
  background: linear-gradient(180deg, rgba(20, 33, 55, 1), rgba(13, 26, 45, 1));
  transform: translateY(-1px);
}

.book-overview-stat-glow::after {
  content: '';
  position: absolute;
  top: -20%;
  right: -10%;
  width: 72px;
  height: 72px;
  background: radial-gradient(circle, rgba(125, 232, 255, 0.08) 0%, transparent 70%);
  pointer-events: none;
}

.book-overview-stat-card-accent {
  border-top-color: rgba(125, 232, 255, 0.34);
}

.book-overview-stat-card-cta {
  border-color: rgba(106, 247, 179, 0.25);
  background: linear-gradient(180deg, rgba(17, 27, 45, 0.98), rgba(13, 23, 39, 0.98));
}

.book-overview-stat-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 6px;
}

.book-overview-stat-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
}

.book-overview-stat-icon svg {
  width: 14px;
  height: 14px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.book-overview-stat-icon-primary {
  background: rgba(50, 113, 230, 0.24);
  color: #73a8ff;
}

.book-overview-stat-icon-blue {
  background: rgba(37, 176, 104, 0.2);
  color: #55e79c;
}

.book-overview-stat-icon-orange {
  background: rgba(180, 125, 33, 0.22);
  color: #ffc05a;
}

.book-overview-stat-icon-solid {
  background: linear-gradient(135deg, #7de8ff, #6af7b3);
  color: #082032;
}

.book-overview-stat-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.book-overview-stat-topvalue {
  color: #7de8ff;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.2;
}

.book-overview-stat-topvalue-blue {
  color: #6af7b3;
}

.book-overview-stat-topvalue-light {
  color: #d8e8fb;
}

.book-overview-stat-toplabel {
  color: #7f91ab;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.2;
}

.book-overview-stat-value {
  color: #ffffff;
  font-size: 25px;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.02em;
}

.book-overview-stat-label {
  margin-top: 0;
  color: #8ea1bf;
  font-size: 13.5px;
  line-height: 1.4;
}

.book-overview-stat-label-spaced {
  margin-bottom: 6px;
}

.book-overview-storage-bar {
  width: 100%;
  height: 5px;
  margin-top: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
}

.book-overview-storage-bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #7de8ff, #6af7b3);
  box-shadow: 0 0 10px rgba(125, 232, 255, 0.28);
}

.book-overview-cta-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  margin-top: 6px;
  padding: 6px 9px;
  border: none;
  border-radius: 10px;
  background: rgba(106, 247, 179, 0.12);
  border: 1px solid rgba(106, 247, 179, 0.25);
  color: #6af7b3;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s ease, box-shadow 0.2s ease;
}

.book-overview-cta-btn:hover {
  background: rgba(106, 247, 179, 0.18);
  box-shadow: none;
}

.book-overview-cta-btn svg {
  width: 13px;
  height: 13px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.book-overview-insights {
  display: none;
}

.book-overview-insight-card {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid rgba(134, 163, 196, 0.08);
  border-radius: 12px;
  background: rgba(8, 18, 31, 0.5);
}

.book-overview-insight-wide {
  grid-column: span 2;
}

.book-overview-insight-label {
  display: block;
  margin-bottom: 4px;
  color: #6b7f9a;
  font-size: 11px;
  font-weight: 700;
}

.book-overview-insight-card strong {
  display: block;
  overflow: hidden;
  color: #e8eff8;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-overview-insight-card span:last-child {
  display: block;
  overflow: hidden;
  margin-top: 3px;
  color: #7b8fa8;
  font-size: 12px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-overview-table-card {
  overflow: hidden;
  border: 1px solid rgba(134, 163, 196, 0.12);
  border-radius: 16px;
  background: rgba(8, 18, 31, 0.72);
  box-shadow: none;
  backdrop-filter: blur(18px);
}

.book-overview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 22px;
  border-bottom: 1px solid rgba(134, 163, 196, 0.08);
}

.book-overview-tabs {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px;
  border-radius: 10px;
  background: transparent;
}

.book-overview-category-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  flex-wrap: wrap;
  justify-content: center;
}

.book-overview-category-tag {
  min-height: 34px;
  border: 1px solid rgba(134, 163, 196, 0.14);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  color: #9eb4d1;
  cursor: pointer;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 700;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.book-overview-category-tag small {
  min-width: 18px;
  height: 18px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: #d8e8fb;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
  font-size: 10px;
  font-weight: 800;
  box-sizing: border-box;
}

.book-overview-category-tag.active {
  border-color: rgba(125, 232, 255, 0.34);
  background: rgba(125, 232, 255, 0.1);
  color: #7de8ff;
}

.book-overview-tab {
  min-width: 72px;
  padding: 9px 16px;
  border: 1px solid rgba(134, 163, 196, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  color: #9eb4d1;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.book-overview-tab:not(.active) {
  color: #9eb4d1;
}

.book-overview-tab.active {
  border-color: rgba(125, 232, 255, 0.34);
  background: rgba(125, 232, 255, 0.1);
  color: #7de8ff;
  box-shadow: none;
}

.book-overview-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.book-overview-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  border: 1px solid rgba(134, 163, 196, 0.15);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.05);
  color: #9eb4d1;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.book-overview-action-btn:hover {
  border-color: rgba(134, 163, 196, 0.24);
  background: rgba(255, 255, 255, 0.08);
  color: #e8eff8;
}

.book-overview-action-btn svg,
.book-overview-menu-btn svg,
.book-overview-page-icon svg {
  width: 16px;
  height: 16px;
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

.book-overview-table-content {
  position: relative;
  min-height: 420px;
}

.book-overview-table-stage {
  will-change: opacity, transform;
}

.book-overview-panel-enter-active,
.book-overview-panel-leave-active {
  transition: opacity 0.24s ease, transform 0.24s ease, filter 0.24s ease;
}

.book-overview-panel-enter-from,
.book-overview-panel-leave-to {
  opacity: 0;
  transform: translateY(10px);
  filter: saturate(0.92);
}

.book-overview-panel-enter-to,
.book-overview-panel-leave-from {
  opacity: 1;
  transform: translateY(0);
  filter: saturate(1);
}

.book-overview-table {
  width: 100%;
  border-collapse: collapse;
}

.book-overview-table th {
  padding: 16px 22px;
  color: #6b7f9a;
  font-size: 9px;
  font-weight: 700;
  text-align: left;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  border-bottom: 1px solid rgba(134, 163, 196, 0.08);
}

.book-overview-table td {
  padding: 14px 22px;
  border-bottom: 1px solid rgba(134, 163, 196, 0.04);
  vertical-align: middle;
}

.book-overview-row {
  animation: bookOverviewRowFadeIn 0.32s ease both;
  animation-delay: var(--book-overview-row-delay, 0ms);
  transition: background 0.2s ease;
}

.book-overview-row:hover {
  background: rgba(125, 232, 255, 0.03);
}

.book-overview-row.is-empty {
  animation-duration: 0.22s;
}

.book-overview-book-meta {
  display: flex;
  align-items: center;
  gap: 14px;
}

.book-overview-cover-frame {
  width: 44px;
  height: 56px;
  overflow: hidden;
  border: 1px solid rgba(125, 232, 255, 0.12);
  border-radius: 10px;
  background: rgba(125, 232, 255, 0.06);
  box-shadow: none;
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
  gap: 3px;
}

.book-overview-book-title {
  color: #e8eff8;
  font-size: 14px;
  font-weight: 700;
  transition: color 0.2s ease;
}

.book-overview-row:hover .book-overview-book-title {
  color: #7de8ff;
}

.book-overview-book-ref {
  color: #7b8fa8;
  font-family: "JetBrains Mono", "Consolas", monospace;
  font-size: 11px;
}

.book-overview-author,
.book-overview-date {
  color: #9eb4d1;
  font-size: 13px;
  font-weight: 600;
}

.book-overview-date {
  font-size: 12px;
  font-weight: 500;
}

.book-overview-category-pill {
  display: inline-flex;
  align-items: center;
  padding: 5px 11px;
  border-radius: 8px;
  border: 1px solid rgba(134, 163, 196, 0.12);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.book-overview-category-pill.is-blue {
  background: rgba(125, 232, 255, 0.06);
  border-color: rgba(125, 232, 255, 0.25);
  color: #7de8ff;
}

.book-overview-category-pill.is-gray {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(134, 163, 196, 0.12);
  color: #9eb4d1;
}

.book-overview-category-pill.is-orange {
  background: rgba(255, 159, 127, 0.06);
  border-color: rgba(255, 159, 127, 0.25);
  color: #ff9f7f;
}

.book-overview-read-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.book-overview-read-value {
  color: #e8eff8;
  font-size: 14px;
  font-weight: 700;
}

.book-overview-read-track {
  width: 64px;
  height: 4px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
}

.book-overview-read-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #7de8ff, #6af7b3);
  transition: width 0.34s ease;
}

.book-overview-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 11px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 11px;
  font-weight: 700;
}

.book-overview-status-pill.is-published {
  background: rgba(106, 247, 179, 0.08);
  color: #6af7b3;
}

.book-overview-status-pill.is-draft {
  background: rgba(158, 180, 209, 0.08);
  color: #9eb4d1;
}

.book-overview-status-pill.is-review {
  background: rgba(255, 213, 138, 0.08);
  color: #ffd58a;
}

.book-overview-status-dot {
  width: 6px;
  height: 6px;
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
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #9eb4d1;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease;
}

.book-overview-menu-btn:hover {
  color: #7de8ff;
  background: rgba(125, 232, 255, 0.1);
}

.book-overview-action-menu-wrap {
  position: relative;
  display: inline-flex;
  justify-content: flex-end;
}

.book-overview-action-menu {
  position: fixed;
  z-index: 90;
  width: 210px;
  padding: 8px;
  border: 1px solid rgba(134, 163, 196, 0.16);
  border-radius: 12px;
  background: rgba(12, 23, 39, 0.98);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.42), inset 0 1px 0 rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(16px);
}

.book-overview-action-menu button {
  width: 100%;
  min-height: 36px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #cfe4ff;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  padding: 8px 10px;
  text-align: left;
}

.book-overview-action-menu button:hover {
  background: rgba(125, 232, 255, 0.08);
  color: #7de8ff;
}

.book-overview-action-menu button.is-danger {
  color: #ffb3be;
}

.book-overview-action-menu button.is-danger:hover {
  background: rgba(255, 143, 159, 0.1);
  color: #ff8f9f;
}

.book-overview-modal-layer {
  position: fixed;
  z-index: 80;
  inset: 0;
  background: rgba(3, 9, 18, 0.62);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.book-overview-modal {
  width: min(440px, 100%);
  border: 1px solid rgba(134, 163, 196, 0.16);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(18, 30, 50, 0.98), rgba(10, 21, 36, 0.98));
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  padding: 22px;
}

.book-overview-modal-title {
  color: #f4fbff;
  font-size: 18px;
  font-weight: 800;
  margin-bottom: 8px;
}

.book-overview-modal-subtitle {
  color: #8ea1bf;
  font-size: 13px;
  line-height: 1.7;
  margin-bottom: 18px;
}

.book-overview-category-editor-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.book-overview-category-editor-options button {
  border: 1px solid rgba(134, 163, 196, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  color: #9eb4d1;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  padding: 8px 12px;
}

.book-overview-category-editor-options button.active,
.book-overview-category-editor-options button:hover {
  border-color: rgba(125, 232, 255, 0.34);
  background: rgba(125, 232, 255, 0.1);
  color: #7de8ff;
}

.book-overview-modal-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.book-overview-delete-action-wrap {
  position: relative;
  display: inline-flex;
}

.book-overview-modal-btn {
  min-height: 36px;
  border: none;
  border-radius: 9px;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  padding: 0 14px;
}

.book-overview-modal-btn:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.book-overview-modal-btn.is-ghost {
  background: rgba(255, 255, 255, 0.06);
  color: #cfe4ff;
}

.book-overview-modal-btn.is-primary {
  background: linear-gradient(135deg, #6af7d2, #7de8ff);
  color: #082033;
}

.book-overview-modal-btn.is-danger {
  background: linear-gradient(135deg, #ff8f9f, #ffb38a);
  color: #2a0710;
}

.book-overview-delete-tip {
  position: absolute;
  right: calc(100% + 12px);
  top: 50%;
  z-index: 5;
  width: 280px;
  padding: 12px 14px;
  border: 1px solid rgba(134, 163, 196, 0.16);
  border-radius: 14px;
  background: rgba(12, 23, 39, 0.98);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.38), inset 0 1px 0 rgba(255, 255, 255, 0.04);
  color: #d7e7f8;
  font-size: 12px;
  line-height: 1.7;
  text-align: left;
  transform: translateY(-50%);
  pointer-events: none;
}

.book-overview-delete-tip::after {
  content: '';
  position: absolute;
  right: -6px;
  top: 50%;
  width: 12px;
  height: 12px;
  border-top: 1px solid rgba(134, 163, 196, 0.16);
  border-right: 1px solid rgba(134, 163, 196, 0.16);
  background: rgba(12, 23, 39, 0.98);
  transform: translateY(-50%) rotate(45deg);
}

.book-overview-delete-tip.is-soft {
  border-color: rgba(125, 232, 255, 0.22);
}

.book-overview-delete-tip.is-hard {
  border-color: rgba(255, 143, 159, 0.22);
}

.book-overview-empty {
  padding: 26px 18px;
  color: #6b7f9a;
  font-size: 12px;
  text-align: center;
}

.book-overview-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 22px;
  border-top: 1px solid rgba(134, 163, 196, 0.08);
  background: transparent;
}

.book-overview-footer-copy {
  color: #7b8fa8;
  font-size: 11px;
  font-weight: 600;
}

.book-overview-pagination {
  display: flex;
  align-items: center;
  gap: 6px;
}

.book-overview-page-icon,
.book-overview-page-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 30px;
  height: 30px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: #9eb4d1;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.book-overview-page-btn.active {
  background: linear-gradient(135deg, #7de8ff, #6af7b3);
  color: #082032;
  box-shadow: none;
}

.book-overview-page-btn:not(.active):not(.ellipsis):hover,
.book-overview-page-icon:hover:not(:disabled) {
  color: #e8eff8;
  background: rgba(255, 255, 255, 0.08);
}

.book-overview-page-btn.ellipsis {
  cursor: default;
  color: #6b7f9a;
}

.book-overview-page-icon:disabled {
  opacity: 0.3;
  cursor: default;
}

@keyframes bookOverviewRowFadeIn {
  from {
    opacity: 0;
    transform: translateY(6px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1400px) {
  .book-overview-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .book-overview-insights {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .book-overview-insight-wide {
    grid-column: span 1;
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

  .book-overview-category-tags,
  .book-overview-actions,
  .book-overview-pagination {
    justify-content: flex-start;
  }

  .book-overview-insights {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .book-overview-shell {
    padding-top: 0;
  }

  .book-overview-stat-card,
  .book-overview-insight-card,
  .book-overview-toolbar,
  .book-overview-footer {
    padding: 14px;
  }

  .book-overview-table th,
  .book-overview-table td {
    padding: 16px 18px;
  }

  .book-overview-tabs {
    flex-wrap: wrap;
  }

  .book-overview-category-tag {
    flex: 0 1 auto;
  }

  .book-overview-tab {
    min-width: 72px;
    flex: 1 1 40%;
  }
}
</style>
