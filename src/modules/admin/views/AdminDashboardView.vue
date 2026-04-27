<template>
  <div class="admin-console-page">
    <header class="admin-console-hero">
      <div>
        <p class="admin-console-kicker">Content Command Center</p>
        <h1>内容管理后台</h1>
        <p class="admin-console-lead">
          统一查看在线人数、浏览变化、评论新增与内容编辑动态，并直接完成 Excel 导入导出、
          富文本编辑和模块化内容运营。
        </p>
      </div>

      <div class="admin-console-hero-actions">
        <button class="admin-console-secondary-btn" type="button" @click="handleExportCurrentModule">
          导出 XLSX
        </button>
        <button class="admin-console-secondary-btn" type="button" @click="triggerImport">
          导入 XLSX
        </button>
        <button class="admin-console-primary-btn" type="button" @click="openCreateDialog">
          新建内容
        </button>
      </div>
    </header>

    <input
      ref="fileInputRef"
      type="file"
      accept=".xlsx,.xls"
      class="admin-console-hidden-input"
      @change="handleImportFile"
    />

    <section v-if="errorMessage" class="admin-console-error">
      <strong>后台请求失败</strong>
      <p>{{ errorMessage }}</p>
    </section>

    <section class="admin-summary-grid">
      <article class="admin-summary-card admin-summary-card--warm">
        <span class="admin-summary-label">在线人数</span>
        <strong>{{ summary.onlineUsers }}</strong>
        <small>由管理台心跳实时更新</small>
      </article>
      <article class="admin-summary-card admin-summary-card--cool">
        <span class="admin-summary-label">总浏览量</span>
        <strong>{{ formatCount(summary.totalViews) }}</strong>
        <small>技术文章、看天下、AI 热点合并统计</small>
      </article>
      <article class="admin-summary-card admin-summary-card--green">
        <span class="admin-summary-label">评论累计</span>
        <strong>{{ formatCount(summary.totalComments) }}</strong>
        <small>当前已接入文章与热点评论数据</small>
      </article>
      <article class="admin-summary-card admin-summary-card--violet">
        <span class="admin-summary-label">今日编辑</span>
        <strong>{{ summary.editsToday }}</strong>
        <small>最近更新时间 {{ summary.lastUpdatedAt || '--' }}</small>
      </article>
    </section>

    <section class="admin-chart-grid">
      <article class="admin-panel">
        <div class="admin-panel-head">
          <div>
            <span class="admin-panel-kicker">ECharts</span>
            <h2>实时趋势</h2>
          </div>
          <button class="admin-inline-btn" type="button" @click="refreshCurrentType">
            手动刷新
          </button>
        </div>
        <AdminTrendChart :trend-points="trendPoints" />
      </article>

      <article class="admin-panel">
        <div class="admin-panel-head">
          <div>
            <span class="admin-panel-kicker">G2</span>
            <h2>模块热度占比</h2>
          </div>
          <span class="admin-panel-pill">{{ summary.totalContents }} 条内容</span>
        </div>
        <AdminModuleChart :module-stats="moduleStats" />
      </article>
    </section>

    <section class="admin-main-grid">
      <article class="admin-panel admin-content-panel">
        <div class="admin-panel-head admin-panel-head--stack">
          <div class="admin-module-tabs">
            <button
              v-for="module in moduleOptions"
              :key="module.key"
              class="admin-module-tab"
              :class="{ 'is-active': currentType === module.key }"
              type="button"
              @click="switchType(module.key)"
            >
              <span>{{ module.label }}</span>
              <small>{{ module.caption }}</small>
            </button>
          </div>

          <div class="admin-content-toolbar">
            <input
              v-model.trim="keyword"
              type="text"
              class="admin-search-input"
              placeholder="搜索标题、作者、赛道或标签"
            />
            <button class="admin-console-secondary-btn" type="button" @click="refreshCurrentType">
              同步数据
            </button>
          </div>
        </div>

        <div class="admin-content-table-wrap">
          <table class="admin-content-table">
            <thead>
              <tr>
                <th>标题 / 摘要</th>
                <th>模块信息</th>
                <th>数据指标</th>
                <th>发布时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody v-if="filteredRecords.length">
              <tr v-for="record in filteredRecords" :key="`${record.type}-${record.id}`">
                <td>
                  <strong class="admin-table-title">{{ record.title }}</strong>
                  <p class="admin-table-summary">{{ record.summary || record.coverSummary || '暂无摘要' }}</p>
                </td>
                <td>
                  <p class="admin-table-meta">{{ resolveMetaLine(record) }}</p>
                  <div class="admin-tag-row">
                    <span v-for="tag in resolveTags(record)" :key="tag" class="admin-tag">
                      {{ tag }}
                    </span>
                  </div>
                </td>
                <td>
                  <p class="admin-table-metric">浏览 {{ formatCount(record.viewCount) }}</p>
                  <p class="admin-table-metric">评论 {{ formatCount(record.commentCount) }}</p>
                  <p class="admin-table-metric">点赞 {{ formatCount(record.likeCount) }}</p>
                </td>
                <td>{{ record.publishedAt || '--' }}</td>
                <td>
                  <div class="admin-row-actions">
                    <button class="admin-inline-btn" type="button" @click="openEditDialog(record)">
                      编辑
                    </button>
                    <button
                      class="admin-inline-btn admin-inline-btn--danger"
                      type="button"
                      @click="removeRecord(record)"
                    >
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
            <tbody v-else>
              <tr>
                <td colspan="5" class="admin-empty-cell">当前模块暂无数据，或搜索条件未命中内容。</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <aside class="admin-side-stack">
        <article class="admin-panel">
          <div class="admin-panel-head">
            <div>
              <span class="admin-panel-kicker">Recent Edits</span>
              <h2>最近编辑</h2>
            </div>
          </div>
          <div class="admin-activity-list">
            <div v-if="recentEdits.length === 0" class="admin-empty-note">暂时还没有编辑日志。</div>
            <article v-for="item in recentEdits" :key="item.id" class="admin-activity-item">
              <span class="admin-activity-badge">{{ resolveActionLabel(item.actionType) }}</span>
              <strong>{{ item.contentTitle }}</strong>
              <p>{{ resolveTypeLabel(item.contentType) }} · {{ item.operatorName }}</p>
              <small>{{ item.createdAt }}</small>
            </article>
          </div>
        </article>

        <article class="admin-panel">
          <div class="admin-panel-head">
            <div>
              <span class="admin-panel-kicker">Hot Content</span>
              <h2>热门内容</h2>
            </div>
          </div>
          <div class="admin-hot-list">
            <article v-for="item in hotContents" :key="`${item.type}-${item.id}`" class="admin-hot-item">
              <span class="admin-hot-rank">{{ resolveTypeLabel(item.type) }}</span>
              <div>
                <strong>{{ item.title }}</strong>
                <p>{{ formatCount(item.viewCount) }} 浏览 · {{ formatCount(item.commentCount) }} 评论</p>
              </div>
            </article>
          </div>
        </article>
      </aside>
    </section>

    <div v-if="dialogVisible" class="admin-dialog-mask" @click.self="closeDialog">
      <section class="admin-dialog">
        <header class="admin-dialog-head">
          <div>
            <span class="admin-panel-kicker">{{ isEditing ? 'Edit Content' : 'Create Content' }}</span>
            <h2>{{ isEditing ? '编辑内容' : '新建内容' }}</h2>
          </div>
          <button class="admin-dialog-close" type="button" @click="closeDialog">×</button>
        </header>

        <div class="admin-dialog-body">
          <div class="admin-form-grid">
            <label class="admin-form-field">
              <span>标题</span>
              <input v-model.trim="draftForm.title" type="text" />
            </label>

            <label class="admin-form-field">
              <span>发布时间</span>
              <input v-model.trim="draftForm.publishedAt" type="datetime-local" />
            </label>

            <label class="admin-form-field" v-if="currentType === 'tech'">
              <span>分类</span>
              <select v-model="draftForm.category">
                <option value="frontend">前端</option>
                <option value="backend">后端</option>
              </select>
            </label>

            <label class="admin-form-field" v-if="currentType === 'world'">
              <span>期号</span>
              <input v-model.trim="draftForm.issueLabel" type="text" placeholder="例如 2026.08" />
            </label>

            <label class="admin-form-field" v-if="currentType === 'ai'">
              <span>赛道</span>
              <select v-model="draftForm.track">
                <option value="agent">agent</option>
                <option value="multimodal">multimodal</option>
                <option value="infra">infra</option>
              </select>
            </label>

            <label class="admin-form-field" v-if="currentType === 'tech' || currentType === 'ai'">
              <span>作者</span>
              <input v-model.trim="draftForm.authorName" type="text" />
            </label>

            <label class="admin-form-field" v-if="currentType === 'tech'">
              <span>作者角色</span>
              <input v-model.trim="draftForm.authorRole" type="text" />
            </label>

            <label class="admin-form-field">
              <span>封面地址</span>
              <input v-model.trim="draftForm.coverUrl" type="text" />
            </label>

            <label class="admin-form-field" v-if="currentType === 'tech'">
              <span>阅读时长</span>
              <input v-model.trim="draftForm.readTime" type="text" placeholder="例如 8 min" />
            </label>

            <label class="admin-form-field" v-if="currentType === 'world'">
              <span>推荐值</span>
              <input v-model.number="draftForm.recommendation" type="number" min="0" max="100" step="0.1" />
            </label>

            <label class="admin-form-field" v-if="currentType === 'ai'">
              <span>热度</span>
              <input v-model.number="draftForm.heat" type="number" min="0" />
            </label>

            <label class="admin-form-field admin-form-field--wide">
              <span>摘要</span>
              <textarea v-model.trim="draftForm.summary" rows="3"></textarea>
            </label>

            <label class="admin-form-field admin-form-field--wide" v-if="currentType === 'tech'">
              <span>精华摘要</span>
              <textarea v-model.trim="draftForm.essence" rows="3"></textarea>
            </label>

            <label class="admin-form-field admin-form-field--wide" v-if="currentType === 'tech'">
              <span>亮点（用英文逗号分隔）</span>
              <input
                v-model.trim="draftForm.highlightsText"
                type="text"
                placeholder="例如 统一鉴权, 请求追踪, 异常兜底"
              />
            </label>

            <label class="admin-form-field admin-form-field--wide" v-if="currentType === 'ai'">
              <span>标签（用英文逗号分隔）</span>
              <input
                v-model.trim="draftForm.tagsText"
                type="text"
                placeholder="例如 任务编排, 工具调用"
              />
            </label>

            <div class="admin-switch-grid" v-if="currentType === 'tech'">
              <label class="admin-switch-item">
                <input v-model="draftForm.featured" type="checkbox" />
                <span>精选</span>
              </label>
              <label class="admin-switch-item">
                <input v-model="draftForm.vip" type="checkbox" />
                <span>VIP</span>
              </label>
              <label class="admin-switch-item">
                <input v-model="draftForm.collected" type="checkbox" />
                <span>收藏态</span>
              </label>
              <label class="admin-switch-item">
                <input v-model="draftForm.liked" type="checkbox" />
                <span>点赞态</span>
              </label>
              <label class="admin-switch-item">
                <input v-model="draftForm.history" type="checkbox" />
                <span>历史态</span>
              </label>
            </div>

            <div class="admin-switch-grid" v-if="currentType === 'ai'">
              <label class="admin-switch-item">
                <input v-model="draftForm.recommended" type="checkbox" />
                <span>推荐</span>
              </label>
              <label class="admin-switch-item">
                <input v-model="draftForm.today" type="checkbox" />
                <span>今日热点</span>
              </label>
            </div>

            <div class="admin-metric-grid">
              <label class="admin-form-field">
                <span>浏览量</span>
                <input v-model.number="draftForm.viewCount" type="number" min="0" />
              </label>
              <label class="admin-form-field">
                <span>评论量</span>
                <input v-model.number="draftForm.commentCount" type="number" min="0" />
              </label>
              <label class="admin-form-field">
                <span>点赞量</span>
                <input v-model.number="draftForm.likeCount" type="number" min="0" />
              </label>
              <label class="admin-form-field" v-if="currentType === 'tech'">
                <span>收藏量</span>
                <input v-model.number="draftForm.collectCount" type="number" min="0" />
              </label>
            </div>
          </div>

          <div class="admin-editor-block">
            <div class="admin-panel-head">
              <div>
                <span class="admin-panel-kicker">AiEditor</span>
                <h2>正文编辑</h2>
              </div>
            </div>
            <AdminRichEditor
              v-model="draftForm.contentHtml"
              placeholder="请输入正文、期刊导语或热点深读内容"
            />
          </div>
        </div>

        <footer class="admin-dialog-foot">
          <button class="admin-console-secondary-btn" type="button" @click="closeDialog">
            取消
          </button>
          <button class="admin-console-primary-btn" type="button" :disabled="saving" @click="submitDraft">
            {{ saving ? '保存中...' : '保存内容' }}
          </button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useAdminConsoleStore } from '@/modules/admin/stores/adminConsole'

const adminStore = useAdminConsoleStore()
const { currentSummary, errorMessage, hotContents, moduleStats, recentEdits, saving, trendPoints } = storeToRefs(adminStore)

let xlsxLibraryPromise = null

// 业务目的：把富文本编辑器和图表组件改为异步加载，避免管理台首屏一次性加载全部重依赖。
// 业务逻辑：管理页先渲染内容骨架，进入图表区或编辑弹窗时再按需拉取对应组件代码。
const AdminModuleChart = defineAsyncComponent(() => import('@/modules/admin/components/AdminModuleChart.vue'))
const AdminRichEditor = defineAsyncComponent(() => import('@/modules/admin/components/AdminRichEditor.vue'))
const AdminTrendChart = defineAsyncComponent(() => import('@/modules/admin/components/AdminTrendChart.vue'))

const moduleOptions = [
  { key: 'tech', label: '技术文章', caption: '富文本 / Excel / 精选分发' },
  { key: 'world', label: '看天下', caption: '期刊封面 / 推荐值 / 导语' },
  { key: 'ai', label: 'AI 热点', caption: '赛道热度 / 标签 / 推荐流' }
]

const currentType = ref('tech')
const keyword = ref('')
const dialogVisible = ref(false)
const isEditing = ref(false)
const fileInputRef = ref(null)
const draftForm = reactive(createEmptyDraft())

const summary = computed(() => currentSummary.value || {
  onlineUsers: 0,
  totalViews: 0,
  totalComments: 0,
  editsToday: 0,
  totalContents: 0,
  lastUpdatedAt: ''
})

const currentRecords = computed(() => adminStore.getContentList(currentType.value))

const filteredRecords = computed(() => {
  const searchValue = keyword.value.trim().toLowerCase()
  if (!searchValue) {
    return currentRecords.value
  }

  return currentRecords.value.filter((record) => {
    return [
      record.title,
      record.summary,
      record.authorName,
      record.track,
      ...(record.tags || [])
    ]
      .filter(Boolean)
      .some((item) => String(item).toLowerCase().includes(searchValue))
  })
})

// 业务目的：页面进入后立即启动管理台心跳与轮询，让在线人数和仪表盘数据保持刷新。
// 业务逻辑：状态缓存统一交给 Pinia 管理，页面只处理当前模块切换、弹窗状态和表单交互。
onMounted(() => {
  adminStore.startRealtime(currentType.value)
})

watch(currentType, (nextType) => {
  keyword.value = ''
  adminStore.stopRealtime()
  adminStore.startRealtime(nextType)
})

onBeforeUnmount(() => {
  adminStore.stopRealtime()
})

function switchType(type) {
  currentType.value = type
}

function refreshCurrentType() {
  adminStore.refreshAll(currentType.value).catch(() => null)
}

function openCreateDialog() {
  isEditing.value = false
  Object.assign(draftForm, createEmptyDraft())
  dialogVisible.value = true
}

function openEditDialog(record) {
  isEditing.value = true
  Object.assign(draftForm, createDraftFromRecord(record))
  dialogVisible.value = true
}

function closeDialog() {
  dialogVisible.value = false
}

async function submitDraft() {
  const payload = buildSavePayload(draftForm, currentType.value)
  await adminStore.saveContent(currentType.value, payload)
  closeDialog()
}

async function removeRecord(record) {
  const confirmed = window.confirm(`确认删除「${record.title}」吗？`)
  if (!confirmed) {
    return
  }
  await adminStore.removeContent(record.type, record.id)
}

function triggerImport() {
  fileInputRef.value?.click()
}

async function handleImportFile(event) {
  const file = event.target.files?.[0]
  if (!file) {
    return
  }

  const XLSX = await loadXlsxLibrary()
  const buffer = await file.arrayBuffer()
  const workbook = XLSX.read(buffer)
  const worksheet = workbook.Sheets[workbook.SheetNames[0]]
  const rows = XLSX.utils.sheet_to_json(worksheet, { defval: '' })
  const records = rows.map((row) => buildImportRecord(row, currentType.value))
  await adminStore.batchImportContent(currentType.value, records)
  event.target.value = ''
}

async function handleExportCurrentModule() {
  const XLSX = await loadXlsxLibrary()
  const rows = currentRecords.value.map((record) => buildExportRow(record, currentType.value))
  const worksheet = XLSX.utils.json_to_sheet(rows)
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, resolveTypeLabel(currentType.value))
  XLSX.writeFile(workbook, `peakstars-${currentType.value}-content.xlsx`)
}

function resolveTags(record) {
  if (record.type === 'tech') {
    return (record.highlights || []).slice(0, 3)
  }
  if (record.type === 'ai') {
    return (record.tags || []).slice(0, 3)
  }
  return [record.issueLabel, record.coverKicker].filter(Boolean)
}

function resolveMetaLine(record) {
  if (record.type === 'tech') {
    return `${record.category || '未分类'} · ${record.authorName || '匿名作者'} · ${record.readTime || '--'}`
  }
  if (record.type === 'world') {
    return `${record.issueLabel || '--'} · 推荐值 ${record.recommendation ?? 0}%`
  }
  return `${record.track || '未分组'} · ${record.authorName || '匿名来源'} · 热度 ${record.heat ?? 0}`
}

function resolveTypeLabel(type) {
  return moduleOptions.find((item) => item.key === type)?.label || type
}

function resolveActionLabel(actionType) {
  return {
    update: '更新',
    delete: '删除',
    'batch-import': '导入'
  }[actionType] || actionType
}

function formatCount(value) {
  const count = Number(value || 0)
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}w`
  }
  if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`
  }
  return `${count}`
}

function createEmptyDraft() {
  return {
    id: '',
    title: '',
    category: 'frontend',
    summary: '',
    essence: '',
    authorName: '',
    authorRole: '',
    coverUrl: '/peakstars-blog-icon.jpg',
    contentHtml: '<p>请输入正文内容</p>',
    publishedAt: formatDateTimeLocal(new Date()),
    viewCount: 0,
    commentCount: 0,
    likeCount: 0,
    collectCount: 0,
    readTime: '6 min',
    featured: false,
    vip: false,
    collected: false,
    liked: false,
    history: false,
    issueLabel: '',
    recommendation: 80,
    coverAccent: '#d33b2d',
    coverKicker: '期刊聚焦',
    coverHeadline: '',
    coverSummary: '',
    coverFooter: '专题 / 深读 / 评论',
    track: 'agent',
    hotspotType: 'agent',
    heat: 80,
    recommended: true,
    today: true,
    highlightsText: '',
    tagsText: ''
  }
}

function createDraftFromRecord(record) {
  return {
    id: record.id || '',
    title: record.title || '',
    category: record.category || 'frontend',
    summary: record.summary || '',
    essence: record.essence || '',
    authorName: record.authorName || '',
    authorRole: record.authorRole || '',
    coverUrl: record.coverUrl || '/peakstars-blog-icon.jpg',
    contentHtml: record.contentHtml || '<p>请输入正文内容</p>',
    publishedAt: normalizeDateTimeLocal(record.publishedAt),
    viewCount: Number(record.viewCount || 0),
    commentCount: Number(record.commentCount || 0),
    likeCount: Number(record.likeCount || 0),
    collectCount: Number(record.collectCount || 0),
    readTime: record.readTime || '6 min',
    featured: Boolean(record.featured),
    vip: Boolean(record.vip),
    collected: Boolean(record.collected),
    liked: Boolean(record.liked),
    history: Boolean(record.history),
    issueLabel: record.issueLabel || '',
    recommendation: Number(record.recommendation || 0),
    coverAccent: record.coverAccent || '#d33b2d',
    coverKicker: record.coverKicker || '期刊聚焦',
    coverHeadline: record.coverHeadline || '',
    coverSummary: record.coverSummary || '',
    coverFooter: record.coverFooter || '专题 / 深读 / 评论',
    track: record.track || 'agent',
    hotspotType: record.hotspotType || record.track || 'agent',
    heat: Number(record.heat || 0),
    recommended: Boolean(record.recommended),
    today: Boolean(record.today),
    highlightsText: (record.highlights || []).join(', '),
    tagsText: (record.tags || []).join(', ')
  }
}

// 业务目的：把统一表单转换成按模块裁剪后的保存请求，避免无关字段污染不同内容表。
// 业务逻辑：先抽取公共字段，再根据技术文章、看天下、AI 热点分别补充各自专属字段。
function buildSavePayload(form, type) {
  const basePayload = {
    id: form.id || '',
    title: form.title,
    summary: form.summary,
    coverUrl: form.coverUrl,
    contentHtml: form.contentHtml,
    publishedAt: normalizeDateTimePayload(form.publishedAt),
    viewCount: Number(form.viewCount || 0),
    commentCount: Number(form.commentCount || 0),
    likeCount: Number(form.likeCount || 0)
  }

  if (type === 'tech') {
    return {
      ...basePayload,
      category: form.category,
      essence: form.essence,
      authorName: form.authorName,
      authorRole: form.authorRole,
      collectCount: Number(form.collectCount || 0),
      readTime: form.readTime,
      featured: Boolean(form.featured),
      vip: Boolean(form.vip),
      collected: Boolean(form.collected),
      liked: Boolean(form.liked),
      history: Boolean(form.history),
      highlights: splitCommaText(form.highlightsText)
    }
  }

  if (type === 'world') {
    return {
      ...basePayload,
      category: '看天下',
      issueLabel: form.issueLabel,
      recommendation: Number(form.recommendation || 0),
      coverAccent: form.coverAccent,
      coverKicker: form.coverKicker,
      coverHeadline: form.coverHeadline || form.title,
      coverSummary: form.coverSummary || form.summary,
      coverFooter: form.coverFooter
    }
  }

  return {
    ...basePayload,
    authorName: form.authorName,
    track: form.track,
    hotspotType: form.hotspotType || form.track,
    heat: Number(form.heat || 0),
    recommended: Boolean(form.recommended),
    today: Boolean(form.today),
    tags: splitCommaText(form.tagsText)
  }
}

function buildExportRow(record, type) {
  if (type === 'tech') {
    return {
      id: record.id,
      title: record.title,
      category: record.category,
      summary: record.summary,
      essence: record.essence,
      authorName: record.authorName,
      authorRole: record.authorRole,
      publishedAt: record.publishedAt,
      viewCount: record.viewCount,
      commentCount: record.commentCount,
      likeCount: record.likeCount,
      collectCount: record.collectCount,
      readTime: record.readTime,
      featured: record.featured,
      vip: record.vip,
      collected: record.collected,
      liked: record.liked,
      history: record.history,
      highlights: (record.highlights || []).join(', '),
      coverUrl: record.coverUrl,
      contentHtml: record.contentHtml
    }
  }

  if (type === 'world') {
    return {
      id: record.id,
      title: record.title,
      issueLabel: record.issueLabel,
      summary: record.summary,
      publishedAt: record.publishedAt,
      viewCount: record.viewCount,
      recommendation: record.recommendation,
      coverAccent: record.coverAccent,
      coverKicker: record.coverKicker,
      coverHeadline: record.coverHeadline,
      coverSummary: record.coverSummary,
      coverFooter: record.coverFooter,
      contentHtml: record.contentHtml
    }
  }

  return {
    id: record.id,
    title: record.title,
    track: record.track,
    hotspotType: record.hotspotType,
    summary: record.summary,
    authorName: record.authorName,
    publishedAt: record.publishedAt,
    viewCount: record.viewCount,
    commentCount: record.commentCount,
    likeCount: record.likeCount,
    heat: record.heat,
    recommended: record.recommended,
    today: record.today,
    tags: (record.tags || []).join(', '),
    coverUrl: record.coverUrl,
    contentHtml: record.contentHtml
  }
}

function buildImportRecord(row, type) {
  if (type === 'tech') {
    return {
      id: row.id,
      title: row.title,
      category: row.category || 'frontend',
      summary: row.summary,
      essence: row.essence,
      authorName: row.authorName,
      authorRole: row.authorRole,
      publishedAt: row.publishedAt,
      viewCount: Number(row.viewCount || 0),
      commentCount: Number(row.commentCount || 0),
      likeCount: Number(row.likeCount || 0),
      collectCount: Number(row.collectCount || 0),
      readTime: row.readTime || '6 min',
      featured: parseBooleanish(row.featured),
      vip: parseBooleanish(row.vip),
      collected: parseBooleanish(row.collected),
      liked: parseBooleanish(row.liked),
      history: parseBooleanish(row.history),
      highlights: splitCommaText(row.highlights),
      coverUrl: row.coverUrl,
      contentHtml: row.contentHtml || `<p>${row.summary || ''}</p>`
    }
  }

  if (type === 'world') {
    return {
      id: row.id,
      title: row.title,
      issueLabel: row.issueLabel,
      summary: row.summary,
      publishedAt: row.publishedAt,
      viewCount: Number(row.viewCount || 0),
      recommendation: Number(row.recommendation || 0),
      coverAccent: row.coverAccent,
      coverKicker: row.coverKicker,
      coverHeadline: row.coverHeadline,
      coverSummary: row.coverSummary,
      coverFooter: row.coverFooter,
      contentHtml: row.contentHtml || `<p>${row.summary || ''}</p>`
    }
  }

  return {
    id: row.id,
    title: row.title,
    track: row.track || 'agent',
    hotspotType: row.hotspotType || row.track || 'agent',
    summary: row.summary,
    authorName: row.authorName,
    publishedAt: row.publishedAt,
    viewCount: Number(row.viewCount || 0),
    commentCount: Number(row.commentCount || 0),
    likeCount: Number(row.likeCount || 0),
    heat: Number(row.heat || 0),
    recommended: parseBooleanish(row.recommended),
    today: parseBooleanish(row.today),
    tags: splitCommaText(row.tags),
    coverUrl: row.coverUrl,
    contentHtml: row.contentHtml || `<p>${row.summary || ''}</p>`
  }
}

function splitCommaText(value) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function parseBooleanish(value) {
  return ['true', '1', 'yes', 'y', '是'].includes(String(value || '').trim().toLowerCase())
}

function normalizeDateTimeLocal(value) {
  if (!value) {
    return formatDateTimeLocal(new Date())
  }
  return value.replace(' ', 'T').slice(0, 16)
}

function normalizeDateTimePayload(value) {
  return String(value || '').replace('T', ' ')
}

function formatDateTimeLocal(date) {
  const currentDate = new Date(date)
  const year = currentDate.getFullYear()
  const month = String(currentDate.getMonth() + 1).padStart(2, '0')
  const day = String(currentDate.getDate()).padStart(2, '0')
  const hour = String(currentDate.getHours()).padStart(2, '0')
  const minute = String(currentDate.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hour}:${minute}`
}

// 业务目的：仅在用户执行 Excel 导入导出时加载 xlsx 运行时，避免把大包压进管理页首屏。
// 业务逻辑：首次调用时缓存模块 Promise，后续导入导出直接复用同一个运行时实例。
async function loadXlsxLibrary() {
  if (!xlsxLibraryPromise) {
    xlsxLibraryPromise = import('xlsx')
  }
  return xlsxLibraryPromise
}
</script>

<style scoped src="../styles/AdminDashboard.css"></style>
