<template>
  <div class="bk-imp">
    <aside class="bk-imp-sidebar">
      <div class="bk-imp-brand">
        <div class="bk-imp-brand-icon">B</div>
        <div class="bk-imp-brand-text">书籍导入中心</div>
      </div>

      <nav class="bk-imp-nav">
        <button class="bk-imp-nav-item" :class="{ active: activePanel === 'import' }" type="button" @click="activePanel = 'import'">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
          <span>导入书籍</span>
        </button>
        <button class="bk-imp-nav-item" :class="{ active: activePanel === 'history' }" type="button" @click="activePanel = 'history'">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          <span>导入记录</span>
        </button>
        <button class="bk-imp-nav-item" :class="{ active: activePanel === 'review' }" type="button" :disabled="!activeJob" @click="activePanel = 'review'">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          <span>审核发布</span>
          <span v-if="activeJob" class="bk-imp-nav-badge">{{ chapters.length }}</span>
        </button>
      </nav>

      <div class="bk-imp-sidebar-footer">
        <div class="bk-imp-stat-mini">
          <span class="bk-imp-stat-mini-val">{{ importJobs.length }}</span>
          <span class="bk-imp-stat-mini-label">总导入</span>
        </div>
        <div class="bk-imp-stat-mini">
          <span class="bk-imp-stat-mini-val">{{ books.length }}</span>
          <span class="bk-imp-stat-mini-label">已发布</span>
        </div>
      </div>
    </aside>

    <main class="bk-imp-main">
      <div v-if="errorMessage" class="bk-imp-toast bk-imp-toast-error">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        <span>{{ errorMessage }}</span>
        <button type="button" @click="errorMessage = ''">&times;</button>
      </div>

      <div v-if="successMessage" class="bk-imp-toast bk-imp-toast-success">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        <span>{{ successMessage }}</span>
        <button type="button" @click="successMessage = ''">&times;</button>
      </div>

      <!-- ====== IMPORT PANEL ====== -->
      <template v-if="activePanel === 'import'">
        <header class="bk-imp-header">
          <h1>导入书籍</h1>
          <p>支持 EPUB、PDF、TXT、Markdown、DOCX、HTML 等主流格式，单文件限制 200MB</p>
        </header>

        <div class="bk-imp-cards">
          <div class="bk-imp-card bk-imp-card-upload" @dragover.prevent @drop.prevent="handleDrop">
            <div class="bk-imp-card-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
            </div>
            <div class="bk-imp-card-title">本地上传</div>
            <div class="bk-imp-card-desc">拖拽或点击选择文件</div>
            <div class="bk-imp-format-tags">
              <span class="bk-imp-format-tag tag-epub">EPUB</span>
              <span class="bk-imp-format-tag tag-pdf">PDF</span>
              <span class="bk-imp-format-tag">TXT</span>
              <span class="bk-imp-format-tag">MD</span>
              <span class="bk-imp-format-tag">DOCX</span>
              <span class="bk-imp-format-tag">HTML</span>
              <span class="bk-imp-format-tag">ZIP</span>
            </div>
            <input ref="fileInputRef" type="file" accept=".epub,.pdf,.txt,.md,.docx,.html,.htm,.zip" class="bk-imp-hidden-input" @change="handleFileSelect" />
            <button class="bk-imp-btn bk-imp-btn-primary" type="button" :disabled="uploading" @click="openFilePicker">
              <svg v-if="uploading" class="bk-imp-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg>
              {{ uploading ? '上传解析中...' : '选择文件上传' }}
            </button>
          </div>

          <div class="bk-imp-card bk-imp-card-external">
            <div class="bk-imp-card-icon bk-imp-card-icon-alt">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
            </div>
            <div class="bk-imp-card-title">外部资源</div>
            <div class="bk-imp-card-desc">从 URL 或 API 导入</div>
            <div class="bk-imp-external-form">
              <input v-model.trim="externalUrl" type="url" placeholder="输入资源 URL 地址" class="bk-imp-input" @keydown.enter="importFromExternal" />
              <input v-model.trim="externalTitle" type="text" placeholder="书籍标题（选填）" class="bk-imp-input" />
              <button class="bk-imp-btn bk-imp-btn-accent" type="button" :disabled="importingExternal || !externalUrl" @click="importFromExternal">
                {{ importingExternal ? '拉取中...' : '导入资源' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Active Job Progress -->
        <div v-if="activeJob" class="bk-imp-progress-section">
          <div class="bk-imp-progress-header">
            <div class="bk-imp-progress-info">
              <span class="bk-imp-progress-title">{{ activeJob.title }}</span>
              <span class="bk-imp-progress-status" :class="'status-' + activeJob.status">{{ resolveJobStatus(activeJob.status) }}</span>
            </div>
            <span class="bk-imp-progress-meta">{{ activeJob.originalFormat?.toUpperCase() || 'ZIP' }} · {{ formatFileSize(activeJob.fileSize) }} · {{ activeJob.totalChapters || 0 }} 章</span>
          </div>
          <div class="bk-imp-progress-bar">
            <div class="bk-imp-progress-fill" :style="{ width: (activeJob.progress || 0) + '%' }"></div>
          </div>
          <div class="bk-imp-progress-footer">
            <span>{{ activeJob.message || '处理中' }}</span>
            <div class="bk-imp-progress-actions">
              <button class="bk-imp-btn bk-imp-btn-ghost" type="button" @click="reloadActiveJob">刷新状态</button>
              <button v-if="activeJob.status === 'await_review'" class="bk-imp-btn bk-imp-btn-primary" type="button" @click="activePanel = 'review'">前往审核</button>
            </div>
          </div>
        </div>
      </template>

      <!-- ====== HISTORY PANEL ====== -->
      <template v-if="activePanel === 'history'">
        <header class="bk-imp-header">
          <h1>导入记录</h1>
          <button class="bk-imp-btn bk-imp-btn-ghost" type="button" @click="reloadHistory">{{ historyLoading ? '加载中...' : '刷新' }}</button>
        </header>

        <div v-if="importJobs.length" class="bk-imp-history-list">
          <div v-for="job in importJobs" :key="job.jobKey" class="bk-imp-history-item" @click="selectJob(job)">
            <div class="bk-imp-history-left">
              <div class="bk-imp-history-format">{{ resolveFormatLabel(job.originalFormat || job.importType) }}</div>
            </div>
            <div class="bk-imp-history-center">
              <div class="bk-imp-history-title">{{ job.title || '未命名' }}</div>
              <div class="bk-imp-history-meta">
                <span>{{ job.author || '未知作者' }}</span>
                <span>{{ job.totalChapters || 0 }} 章</span>
                <span>{{ formatFileSize(job.fileSize) }}</span>
                <span>{{ job.createdAt }}</span>
              </div>
            </div>
            <div class="bk-imp-history-right">
              <span class="bk-imp-progress-status" :class="'status-' + job.status">{{ resolveJobStatus(job.status) }}</span>
              <div v-if="job.status === 'await_review'" class="bk-imp-history-actions">
                <button class="bk-imp-btn bk-imp-btn-sm" type="button" @click.stop="selectJobAndReview(job)">审核</button>
              </div>
              <div v-if="job.status === 'published'" class="bk-imp-history-actions">
                <button class="bk-imp-btn bk-imp-btn-sm bk-imp-btn-ghost" type="button" @click.stop="viewBook(job.bookKey)">查看</button>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="bk-imp-empty">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          <p>暂无导入记录</p>
        </div>
      </template>

      <!-- ====== REVIEW PANEL ====== -->
      <template v-if="activePanel === 'review'">
        <header class="bk-imp-header">
          <h1>审核发布</h1>
          <div class="bk-imp-header-actions">
            <button class="bk-imp-btn bk-imp-btn-ghost" type="button" @click="reloadActiveJob">刷新</button>
            <button class="bk-imp-btn bk-imp-btn-primary" type="button" :disabled="publishing || !canPublish" @click="publishCurrentJob">
              {{ publishing ? '发布中...' : '发布整本书' }}
            </button>
          </div>
        </header>

        <div v-if="activeJob" class="bk-imp-review">
          <div class="bk-imp-review-meta">
            <div class="bk-imp-review-meta-row">
              <span class="bk-imp-review-label">书名</span>
              <span>{{ activeJob.title }}</span>
            </div>
            <div class="bk-imp-review-meta-row">
              <span class="bk-imp-review-label">作者</span>
              <span>{{ activeJob.author || '未填写' }}</span>
            </div>
            <div class="bk-imp-review-meta-row">
              <span class="bk-imp-review-label">章节</span>
              <span>{{ chapters.length }} 章 · {{ totalReviewWords }} 字</span>
            </div>
            <div class="bk-imp-review-meta-row">
              <span class="bk-imp-review-label">格式</span>
              <span>{{ resolveFormatLabel(activeJob.originalFormat || activeJob.importType) }}</span>
            </div>
          </div>

          <div class="bk-imp-review-body">
            <div class="bk-imp-chapter-nav">
              <div class="bk-imp-chapter-nav-head">章节目录</div>
              <button
                v-for="(chapter, idx) in chapters"
                :key="chapter.tempChapterKey"
                class="bk-imp-chapter-item"
                :class="{ active: selectedChapterKey === chapter.tempChapterKey, edited: chapter.reviewStatus === 'edited' }"
                type="button"
                @click="selectChapter(chapter.tempChapterKey)"
              >
                <span class="bk-imp-chapter-idx">{{ String(idx + 1).padStart(2, '0') }}</span>
                <div class="bk-imp-chapter-info">
                  <span class="bk-imp-chapter-title">{{ chapter.title }}</span>
                  <span class="bk-imp-chapter-sub">{{ chapter.wordCount || 0 }} 字</span>
                </div>
                <span v-if="chapter.reviewStatus === 'edited'" class="bk-imp-chapter-edited-dot"></span>
              </button>
            </div>

            <div class="bk-imp-editor-panel">
              <div v-if="selectedChapter" class="bk-imp-editor">
                <div class="bk-imp-editor-top">
                  <span class="bk-imp-editor-chapter-label">CH {{ String(selectedChapter.chapterNo).padStart(2, '0') }}</span>
                  <div class="bk-imp-editor-top-actions">
                    <button class="bk-imp-btn bk-imp-btn-ghost bk-imp-btn-sm" type="button" @click="resetChapterForm">恢复</button>
                    <button class="bk-imp-btn bk-imp-btn-accent bk-imp-btn-sm" type="button" :disabled="savingChapter" @click="saveChapter">
                      {{ savingChapter ? '保存中...' : '保存修改' }}
                    </button>
                  </div>
                </div>
                <div class="bk-imp-editor-fields">
                  <input v-model.trim="chapterForm.title" type="text" placeholder="章节标题" class="bk-imp-input" />
                  <input v-model.trim="chapterForm.subtitle" type="text" placeholder="副标题（选填）" class="bk-imp-input" />
                </div>
                <textarea v-model="chapterForm.contentHtml" rows="20" class="bk-imp-textarea" placeholder="正文 HTML"></textarea>

                <div v-if="selectedChapter.warnings?.length" class="bk-imp-warnings">
                  <div class="bk-imp-warnings-head">预处理告警</div>
                  <div v-for="w in selectedChapter.warnings" :key="w" class="bk-imp-warning-item">{{ w }}</div>
                </div>
              </div>
              <div v-else class="bk-imp-empty bk-imp-empty-sm">
                <p>选择左侧章节进行审核编辑</p>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="bk-imp-empty">
          <p>请先从导入记录中选择一个待审核任务</p>
        </div>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  createBookImportJob,
  createBookImportJobFromFile,
  createBookImportJobFromExternal,
  getBookImportJob,
  getBookImportJobChapters,
  listRecentImportJobs,
  publishBookImportJob,
  updateBookImportJobChapter,
  getAdminBooks
} from '@/modules/admin/api/bookAdmin'

const props = defineProps({
  standalone: { type: Boolean, default: true },
  subModule: { type: String, default: 'import' }
})

const router = useRouter()

const fileInputRef = ref(null)
const activePanel = ref('import')
const uploading = ref(false)
const importingExternal = ref(false)
const publishing = ref(false)
const savingChapter = ref(false)
const historyLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const activeJob = ref(null)
const chapters = ref([])
const selectedChapterKey = ref('')
const importJobs = ref([])
const books = ref([])
const externalUrl = ref('')
const externalTitle = ref('')
const chapterForm = reactive({ title: '', subtitle: '', contentHtml: '' })

const selectedChapter = computed(() =>
  chapters.value.find((c) => c.tempChapterKey === selectedChapterKey.value) || null
)

const canPublish = computed(() =>
  Boolean(activeJob.value?.jobKey && activeJob.value?.status === 'await_review')
)

const totalReviewWords = computed(() =>
  chapters.value.reduce((sum, c) => sum + (c.wordCount || 0), 0)
)

onMounted(() => {
  reloadHistory()
  reloadBooks()
})

watch(() => props.subModule, (val) => {
  activePanel.value = val === 'review' ? 'review' : val === 'history' ? 'history' : 'import'
}, { immediate: true })

function openFilePicker() {
  fileInputRef.value?.click()
}

async function handleFileSelect(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  await uploadFile(file)
}

async function handleDrop(event) {
  const file = event.dataTransfer?.files?.[0]
  if (!file) return
  await uploadFile(file)
}

async function uploadFile(file) {
  if (file.size > 200 * 1024 * 1024) {
    errorMessage.value = '文件大小超过 200MB 限制'
    return
  }

  uploading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const ext = file.name.split('.').pop()?.toLowerCase() || ''
    const isZip = ext === 'zip'
    const job = isZip
      ? await createBookImportJob(file)
      : await createBookImportJobFromFile(file)

    activeJob.value = job
    await loadJobChapters(job.jobKey)
    await reloadHistory()
    await reloadBooks()
    successMessage.value = `${file.name} 解析完成，共 ${job.totalChapters || 0} 章`
    activePanel.value = 'review'
  } catch (error) {
    errorMessage.value = error.message || '书籍导入失败'
  } finally {
    uploading.value = false
  }
}

async function importFromExternal() {
  if (!externalUrl.value) return
  importingExternal.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const job = await createBookImportJobFromExternal({
      sourceUrl: externalUrl.value,
      title: externalTitle.value || '',
      importType: 'api'
    })
    activeJob.value = job
    await loadJobChapters(job.jobKey)
    await reloadHistory()
    successMessage.value = '外部资源导入完成'
    externalUrl.value = ''
    externalTitle.value = ''
    activePanel.value = 'review'
  } catch (error) {
    errorMessage.value = error.message || '外部资源导入失败'
  } finally {
    importingExternal.value = false
  }
}

async function reloadActiveJob() {
  if (!activeJob.value?.jobKey) return
  try {
    activeJob.value = await getBookImportJob(activeJob.value.jobKey)
    await loadJobChapters(activeJob.value.jobKey)
  } catch (error) {
    errorMessage.value = error.message || '刷新任务失败'
  }
}

async function loadJobChapters(jobKey) {
  const list = await getBookImportJobChapters(jobKey)
  chapters.value = Array.isArray(list) ? list : []
  if (chapters.value.length) {
    selectedChapterKey.value = chapters.value[0].tempChapterKey
    fillChapterForm(chapters.value[0])
  } else {
    selectedChapterKey.value = ''
    resetChapterFormValues()
  }
}

function selectChapter(tempChapterKey) {
  selectedChapterKey.value = tempChapterKey
  const chapter = chapters.value.find((c) => c.tempChapterKey === tempChapterKey)
  if (chapter) fillChapterForm(chapter)
}

function fillChapterForm(chapter) {
  chapterForm.title = chapter.title || ''
  chapterForm.subtitle = chapter.subtitle || ''
  chapterForm.contentHtml = chapter.contentHtml || ''
}

function resetChapterForm() {
  if (selectedChapter.value) {
    fillChapterForm(selectedChapter.value)
  } else {
    resetChapterFormValues()
  }
}

function resetChapterFormValues() {
  chapterForm.title = ''
  chapterForm.subtitle = ''
  chapterForm.contentHtml = ''
}

async function saveChapter() {
  if (!activeJob.value?.jobKey || !selectedChapter.value) return
  savingChapter.value = true
  errorMessage.value = ''
  try {
    const updated = await updateBookImportJobChapter(
      activeJob.value.jobKey,
      selectedChapter.value.tempChapterKey,
      {
        title: chapterForm.title,
        subtitle: chapterForm.subtitle,
        contentHtml: chapterForm.contentHtml,
        sortOrder: selectedChapter.value.sortOrder
      }
    )
    chapters.value = chapters.value.map((c) =>
      c.tempChapterKey === updated.tempChapterKey ? updated : c
    )
    fillChapterForm(updated)
    successMessage.value = '章节已保存'
  } catch (error) {
    errorMessage.value = error.message || '保存章节失败'
  } finally {
    savingChapter.value = false
  }
}

async function publishCurrentJob() {
  if (!activeJob.value?.jobKey) return
  publishing.value = true
  errorMessage.value = ''
  try {
    await publishBookImportJob(activeJob.value.jobKey)
    activeJob.value = await getBookImportJob(activeJob.value.jobKey)
    await reloadBooks()
    await reloadHistory()
    successMessage.value = '书籍发布成功'
  } catch (error) {
    errorMessage.value = error.message || '发布书籍失败'
  } finally {
    publishing.value = false
  }
}

async function reloadHistory() {
  historyLoading.value = true
  try {
    const list = await listRecentImportJobs(50)
    importJobs.value = Array.isArray(list) ? list : []
  } catch (error) {
    errorMessage.value = error.message || '加载导入记录失败'
  } finally {
    historyLoading.value = false
  }
}

async function reloadBooks() {
  try {
    const list = await getAdminBooks()
    books.value = Array.isArray(list) ? list : []
  } catch {
    // silent
  }
}

function selectJob(job) {
  activeJob.value = job
  loadJobChapters(job.jobKey)
}

function selectJobAndReview(job) {
  selectJob(job)
  activePanel.value = 'review'
}

function viewBook(bookKey) {
  if (bookKey) router.push(`/book/${bookKey}`)
}

function resolveJobStatus(status) {
  const map = {
    uploaded: '已上传',
    parsing: '解析中',
    await_review: '待审核',
    published: '已发布',
    failed: '失败'
  }
  return map[status] || status || '未知'
}

function resolveFormatLabel(format) {
  const map = {
    epub: 'EPUB',
    pdf: 'PDF',
    txt: 'TXT',
    md: 'MD',
    markdown: 'MD',
    docx: 'DOCX',
    html: 'HTML',
    htm: 'HTML',
    zip: 'ZIP',
    external: 'URL'
  }
  return map[format] || (format || '').toUpperCase()
}

function formatFileSize(bytes) {
  if (!bytes || bytes <= 0) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>

<style scoped>
.bk-imp {
  display: flex;
  min-height: 100vh;
  color: #e8eff8;
  background: linear-gradient(180deg, #070e1a 0%, #0d1829 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', sans-serif;
}

/* ===== Sidebar ===== */
.bk-imp-sidebar {
  width: 220px;
  min-height: 100vh;
  padding: 28px 16px;
  border-right: 1px solid rgba(134, 163, 196, 0.1);
  background: rgba(6, 12, 22, 0.6);
  display: flex;
  flex-direction: column;
  gap: 28px;
  flex-shrink: 0;
}

.bk-imp-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 8px;
}

.bk-imp-brand-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #7de8ff, #6af7b3);
  color: #082032;
  font-weight: 800;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bk-imp-brand-text {
  font-weight: 700;
  font-size: 15px;
  white-space: nowrap;
}

.bk-imp-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bk-imp-nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  border: none;
  background: transparent;
  color: #9eb4d1;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  text-align: left;
  transition: all 0.15s;
  position: relative;
}

.bk-imp-nav-item:hover {
  background: rgba(125, 232, 255, 0.06);
  color: #e8eff8;
}

.bk-imp-nav-item.active {
  background: rgba(125, 232, 255, 0.1);
  color: #7de8ff;
}

.bk-imp-nav-item:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.bk-imp-nav-item svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.bk-imp-nav-badge {
  margin-left: auto;
  background: rgba(125, 232, 255, 0.15);
  color: #7de8ff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 99px;
}

.bk-imp-sidebar-footer {
  margin-top: auto;
  display: flex;
  gap: 12px;
  padding: 16px 8px 0;
  border-top: 1px solid rgba(134, 163, 196, 0.08);
}

.bk-imp-stat-mini {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bk-imp-stat-mini-val {
  font-size: 20px;
  font-weight: 700;
}

.bk-imp-stat-mini-label {
  font-size: 11px;
  color: #6b7f9a;
}

/* ===== Main ===== */
.bk-imp-main {
  flex: 1;
  padding: 32px 40px;
  overflow-y: auto;
  min-width: 0;
}

.bk-imp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
  gap: 16px;
  flex-wrap: wrap;
}

.bk-imp-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
}

.bk-imp-header p {
  margin: 0;
  color: #7b8fa8;
  font-size: 14px;
}

.bk-imp-header-actions {
  display: flex;
  gap: 10px;
}

/* ===== Toast ===== */
.bk-imp-toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 12px;
  margin-bottom: 20px;
  font-size: 14px;
  animation: slideDown 0.25s ease;
}

.bk-imp-toast svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.bk-imp-toast button {
  margin-left: auto;
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  font-size: 18px;
  opacity: 0.6;
}

.bk-imp-toast-error {
  background: rgba(255, 119, 134, 0.1);
  border: 1px solid rgba(255, 119, 134, 0.2);
  color: #ffd1d7;
}

.bk-imp-toast-success {
  background: rgba(106, 247, 179, 0.1);
  border: 1px solid rgba(106, 247, 179, 0.2);
  color: #c3ffe0;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== Cards ===== */
.bk-imp-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 32px;
}

.bk-imp-card {
  border: 1px solid rgba(134, 163, 196, 0.12);
  border-radius: 20px;
  background: rgba(8, 18, 31, 0.72);
  backdrop-filter: blur(18px);
  padding: 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  text-align: center;
  transition: border-color 0.2s;
}

.bk-imp-card:hover {
  border-color: rgba(134, 163, 196, 0.24);
}

.bk-imp-card-upload {
  cursor: pointer;
}

.bk-imp-card-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: rgba(125, 232, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
}

.bk-imp-card-icon svg {
  width: 28px;
  height: 28px;
  color: #7de8ff;
}

.bk-imp-card-icon-alt {
  background: rgba(106, 247, 179, 0.08);
}

.bk-imp-card-icon-alt svg {
  color: #6af7b3;
}

.bk-imp-card-title {
  font-size: 18px;
  font-weight: 700;
}

.bk-imp-card-desc {
  color: #7b8fa8;
  font-size: 13px;
}

.bk-imp-format-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
}

.bk-imp-format-tag {
  padding: 4px 10px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(134, 163, 196, 0.12);
  font-size: 11px;
  font-weight: 600;
  color: #9eb4d1;
  letter-spacing: 0.04em;
}

.bk-imp-format-tag.tag-epub {
  color: #7de8ff;
  border-color: rgba(125, 232, 255, 0.25);
  background: rgba(125, 232, 255, 0.06);
}

.bk-imp-format-tag.tag-pdf {
  color: #ff9f7f;
  border-color: rgba(255, 159, 127, 0.25);
  background: rgba(255, 159, 127, 0.06);
}

.bk-imp-hidden-input {
  display: none;
}

.bk-imp-external-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  margin-top: 8px;
}

/* ===== Inputs ===== */
.bk-imp-input {
  width: 100%;
  border: 1px solid rgba(134, 163, 196, 0.16);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.03);
  color: #e8eff8;
  padding: 10px 14px;
  font: inherit;
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.15s;
}

.bk-imp-input:focus {
  outline: none;
  border-color: rgba(125, 232, 255, 0.4);
}

.bk-imp-input::placeholder {
  color: #5a6d82;
}

.bk-imp-textarea {
  width: 100%;
  border: 1px solid rgba(134, 163, 196, 0.16);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  color: #e8eff8;
  padding: 14px;
  font: inherit;
  font-size: 13px;
  font-family: 'JetBrains Mono', 'Consolas', 'Fira Code', monospace;
  box-sizing: border-box;
  resize: vertical;
  line-height: 1.65;
  transition: border-color 0.15s;
}

.bk-imp-textarea:focus {
  outline: none;
  border-color: rgba(125, 232, 255, 0.4);
}

/* ===== Buttons ===== */
.bk-imp-btn {
  border: none;
  border-radius: 10px;
  padding: 10px 18px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.15s;
  white-space: nowrap;
}

.bk-imp-btn-primary {
  background: linear-gradient(135deg, #7de8ff, #6af7b3);
  color: #082032;
}

.bk-imp-btn-accent {
  background: rgba(106, 247, 179, 0.12);
  border: 1px solid rgba(106, 247, 179, 0.25);
  color: #6af7b3;
}

.bk-imp-btn-accent:hover {
  background: rgba(106, 247, 179, 0.18);
}

.bk-imp-btn-ghost {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(134, 163, 196, 0.15);
  color: #9eb4d1;
}

.bk-imp-btn-ghost:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #e8eff8;
}

.bk-imp-btn-sm {
  padding: 6px 12px;
  font-size: 12px;
  border-radius: 8px;
}

.bk-imp-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.bk-imp-spin {
  width: 16px;
  height: 16px;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== Progress ===== */
.bk-imp-progress-section {
  border: 1px solid rgba(134, 163, 196, 0.12);
  border-radius: 16px;
  background: rgba(8, 18, 31, 0.72);
  backdrop-filter: blur(18px);
  padding: 20px 24px;
}

.bk-imp-progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  gap: 12px;
  flex-wrap: wrap;
}

.bk-imp-progress-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bk-imp-progress-title {
  font-weight: 700;
  font-size: 16px;
}

.bk-imp-progress-meta {
  color: #7b8fa8;
  font-size: 13px;
}

.bk-imp-progress-bar {
  height: 8px;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
  margin-bottom: 12px;
}

.bk-imp-progress-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #7de8ff, #6af7b3);
  transition: width 0.4s ease;
}

.bk-imp-progress-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #7b8fa8;
  font-size: 13px;
}

.bk-imp-progress-actions {
  display: flex;
  gap: 8px;
}

.bk-imp-progress-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 99px;
  font-size: 11px;
  font-weight: 700;
}

.bk-imp-progress-status::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 99px;
  background: currentColor;
}

.status-uploaded { color: #9eb4d1; background: rgba(158, 180, 209, 0.08); }
.status-parsing { color: #7de8ff; background: rgba(125, 232, 255, 0.08); }
.status-await_review { color: #ffd58a; background: rgba(255, 213, 138, 0.08); }
.status-published { color: #6af7b3; background: rgba(106, 247, 179, 0.08); }
.status-failed { color: #ff8f9f; background: rgba(255, 143, 159, 0.08); }

/* ===== History ===== */
.bk-imp-history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bk-imp-history-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-radius: 14px;
  border: 1px solid rgba(134, 163, 196, 0.08);
  background: rgba(8, 18, 31, 0.5);
  cursor: pointer;
  transition: all 0.15s;
}

.bk-imp-history-item:hover {
  border-color: rgba(125, 232, 255, 0.2);
  background: rgba(125, 232, 255, 0.03);
}

.bk-imp-history-left {
  flex-shrink: 0;
}

.bk-imp-history-format {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: rgba(125, 232, 255, 0.06);
  border: 1px solid rgba(125, 232, 255, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  color: #7de8ff;
  letter-spacing: 0.04em;
}

.bk-imp-history-center {
  flex: 1;
  min-width: 0;
}

.bk-imp-history-title {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bk-imp-history-meta {
  display: flex;
  gap: 12px;
  color: #7b8fa8;
  font-size: 12px;
}

.bk-imp-history-right {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.bk-imp-history-actions {
  display: flex;
  gap: 6px;
}

/* ===== Review ===== */
.bk-imp-review {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.bk-imp-review-meta {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.bk-imp-review-meta-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border-radius: 12px;
  background: rgba(8, 18, 31, 0.5);
  border: 1px solid rgba(134, 163, 196, 0.08);
}

.bk-imp-review-label {
  font-size: 11px;
  color: #6b7f9a;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.bk-imp-review-body {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
  min-height: 480px;
}

.bk-imp-chapter-nav {
  border: 1px solid rgba(134, 163, 196, 0.1);
  border-radius: 16px;
  background: rgba(8, 18, 31, 0.72);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.bk-imp-chapter-nav-head {
  padding: 14px 18px;
  font-weight: 700;
  font-size: 13px;
  border-bottom: 1px solid rgba(134, 163, 196, 0.08);
  color: #9eb4d1;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.bk-imp-chapter-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border: none;
  border-bottom: 1px solid rgba(134, 163, 196, 0.04);
  background: transparent;
  color: #c7d5e5;
  cursor: pointer;
  text-align: left;
  transition: all 0.1s;
}

.bk-imp-chapter-item:hover {
  background: rgba(125, 232, 255, 0.04);
}

.bk-imp-chapter-item.active {
  background: rgba(125, 232, 255, 0.08);
  color: #7de8ff;
}

.bk-imp-chapter-idx {
  font-size: 12px;
  font-weight: 700;
  color: #6b7f9a;
  font-family: 'JetBrains Mono', monospace;
  flex-shrink: 0;
}

.bk-imp-chapter-item.active .bk-imp-chapter-idx {
  color: #7de8ff;
}

.bk-imp-chapter-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.bk-imp-chapter-title {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bk-imp-chapter-sub {
  font-size: 11px;
  color: #6b7f9a;
}

.bk-imp-chapter-edited-dot {
  width: 6px;
  height: 6px;
  border-radius: 99px;
  background: #6af7b3;
  flex-shrink: 0;
}

.bk-imp-editor-panel {
  border: 1px solid rgba(134, 163, 196, 0.1);
  border-radius: 16px;
  background: rgba(8, 18, 31, 0.72);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.bk-imp-editor {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px;
  flex: 1;
}

.bk-imp-editor-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bk-imp-editor-chapter-label {
  font-size: 12px;
  font-weight: 700;
  color: #7de8ff;
  letter-spacing: 0.1em;
}

.bk-imp-editor-top-actions {
  display: flex;
  gap: 8px;
}

.bk-imp-editor-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

/* ===== Warnings ===== */
.bk-imp-warnings {
  border-radius: 12px;
  background: rgba(255, 196, 87, 0.06);
  border: 1px solid rgba(255, 196, 87, 0.14);
  padding: 14px 16px;
}

.bk-imp-warnings-head {
  font-weight: 700;
  font-size: 12px;
  color: #ffd58a;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.bk-imp-warning-item {
  color: #ffeab4;
  font-size: 13px;
  padding: 4px 0;
}

/* ===== Empty ===== */
.bk-imp-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 60px 20px;
  color: #6b7f9a;
}

.bk-imp-empty svg {
  width: 48px;
  height: 48px;
  opacity: 0.3;
}

.bk-imp-empty-sm {
  padding: 40px;
}

/* ===== Responsive ===== */
@media (max-width: 1200px) {
  .bk-imp-cards {
    grid-template-columns: 1fr;
  }
  .bk-imp-review-meta {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 900px) {
  .bk-imp-sidebar {
    display: none;
  }
  .bk-imp-main {
    padding: 24px 20px;
  }
  .bk-imp-review-body {
    grid-template-columns: 1fr;
  }
  .bk-imp-review-meta {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 600px) {
  .bk-imp-review-meta,
  .bk-imp-editor-fields {
    grid-template-columns: 1fr;
  }
  .bk-imp-history-meta {
    flex-wrap: wrap;
  }
}
</style>
