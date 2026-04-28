<template>
  <div class="admin-console-page">
    <input
      ref="fileInputRef"
      type="file"
      accept=".xlsx,.xls"
      class="admin-console-hidden-input"
      @change="handleImportFile"
    />

    <aside class="sidebar">
      <div class="logo">
        <div class="logo-text">PeakStars Blog</div>
      </div>

      <nav class="nav">
        <div class="nav-section">内容</div>
        <button
          v-for="module in moduleOptions"
          :key="module.key"
          class="nav-item"
          :class="{ active: currentType === module.key }"
          type="button"
          @click="switchType(module.key)"
        >
          <span class="icon icon-emoji" aria-hidden="true">{{ resolveSidebarEmoji(module.key) }}</span>
          {{ module.navLabel }}
          <span class="nav-badge">{{ module.badge }}</span>
        </button>

        <div class="nav-section">数据</div>
        <button class="nav-item" type="button" @click="scrollToTable">
          <span class="icon">数</span>
          数据分析
        </button>
        <button class="nav-item" type="button" @click="scrollToActivity">
          <span class="icon">评</span>
          评论管理
          <span class="nav-badge">{{ formatCount(moduleSummary.commentCount) }}</span>
        </button>

        <div class="nav-section">系统</div>
        <button class="nav-item" type="button" @click="openCreateDialog">
          <span class="icon">建</span>
          新建内容
        </button>
        <button class="nav-item" type="button" @click="handleExportCurrentModule">
          <span class="icon">出</span>
          导出内容
        </button>
      </nav>

      <div class="sidebar-footer">
        <div class="user-info">
          <div class="avatar">PS</div>
          <div>
            <div class="user-name">PeakStars 管理员</div>
            <div class="user-role">在线 {{ summary.onlineUsers }} / 最近更新 {{ summary.lastUpdatedAt || '--' }}</div>
          </div>
        </div>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="topbar-title">{{ activeModule.topbarTitle }}</div>
        <div class="topbar-actions">
          <label class="search-box">
            <!-- 目的: 将后台检索入口替换为更直观的彩色搜索图标; 逻辑: 使用内联 SVG 承载图标样式并保持与输入框同一交互热区。 -->
            <span class="search-box-icon" aria-hidden="true">
              <svg viewBox="0 0 20 20" focusable="false">
                <defs>
                  <linearGradient id="admin-search-icon-ring" x1="3" y1="3" x2="15" y2="15" gradientUnits="userSpaceOnUse">
                    <stop offset="0%" stop-color="#45caff" />
                    <stop offset="55%" stop-color="#3ddc97" />
                    <stop offset="100%" stop-color="#ffd166" />
                  </linearGradient>
                </defs>
                <circle cx="8.5" cy="8.5" r="5.5" fill="#10243d" stroke="url(#admin-search-icon-ring)" stroke-width="2" />
                <path d="M12.6 12.6L16.4 16.4" stroke="#ffb347" stroke-width="2.2" stroke-linecap="round" />
                <circle cx="8.5" cy="8.5" r="1.4" fill="#8be9fd" opacity="0.9" />
              </svg>
            </span>
            <input v-model.trim="keyword" type="text" placeholder="搜索内容…" />
          </label>
          <button class="btn btn-ghost" type="button" @click="handleExportCurrentModule">导出</button>
          <button class="btn btn-primary" type="button" @click="openCreateDialog">+ 新建{{ activeModule.shortLabel }}</button>
        </div>
      </header>

      <div class="content">
        <section v-if="errorMessage" class="admin-console-error">
          <strong>后台请求失败</strong>
          <p>{{ errorMessage }}</p>
        </section>

        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon">文</div>
            <div class="stat-label">{{ activeModule.statLabels.total }}</div>
            <div class="stat-value">{{ moduleSummary.contentCount }}</div>
            <div class="stat-trend">↗ 本月新增 {{ moduleSummary.currentMonthCount }} 条</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">阅</div>
            <div class="stat-label">{{ activeModule.statLabels.views }}</div>
            <div class="stat-value stat-value-green">{{ formatCount(moduleSummary.viewCount) }}</div>
            <div class="stat-trend stat-trend-green">↗ 热度内容持续增长</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">评</div>
            <div class="stat-label">{{ activeModule.statLabels.comments }}</div>
            <div class="stat-value stat-value-blue">{{ formatCount(moduleSummary.commentCount) }}</div>
            <div class="stat-trend stat-trend-blue">↗ 今日活跃保持在线</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">待</div>
            <div class="stat-label">{{ activeModule.statLabels.focus }}</div>
            <div class="stat-value stat-value-red">{{ activeModule.focusCount }}</div>
            <div class="stat-trend stat-trend-red">{{ activeModule.focusHint }}</div>
          </div>
        </div>

        <div class="tab-row">
          <button
            v-for="tab in filterTabs"
            :key="tab.key"
            class="tab"
            :class="{ active: currentFilter === tab.key }"
            type="button"
            @click="currentFilter = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="two-col">
          <div ref="tableSectionRef" class="panel">
            <div class="panel-header">
              <span class="panel-title">{{ activeModule.panelTitle }}</span>
              <span class="panel-action">查看全部 →</span>
            </div>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>标题</th>
                    <th>分类</th>
                    <th>状态</th>
                    <th>阅读</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody v-if="filteredRecords.length">
                  <tr v-for="record in filteredRecords" :key="`${record.type}-${record.id}`">
                    <td>
                      <div class="post-title-cell">
                        <span class="post-title">{{ record.title }}</span>
                        <span class="post-meta">{{ resolveMeta(record) }}</span>
                      </div>
                    </td>
                    <td>
                      <span class="tag">{{ resolveCategory(record) }}</span>
                    </td>
                    <td>
                      <span class="badge" :class="resolveStatus(record).className">{{ resolveStatus(record).label }}</span>
                    </td>
                    <td class="mono-cell">{{ resolveReadValue(record) }}</td>
                    <td>
                      <div class="row-actions">
                        <button class="icon-btn" type="button" title="编辑" @click="openEditDialog(record)">编</button>
                        <button class="icon-btn" type="button" title="刷新" @click="refreshCurrentType">预</button>
                        <button class="icon-btn del" type="button" title="删除" @click="removeRecord(record)">删</button>
                      </div>
                    </td>
                  </tr>
                </tbody>
                <tbody v-else>
                  <tr>
                    <td colspan="5" class="empty-cell">当前条件下暂无内容。</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div ref="activitySectionRef" class="side-panels">
            <div class="panel">
              <div class="panel-header">
                <span class="panel-title">{{ activeModule.sideTitle }}</span>
                <span class="panel-action">管理</span>
              </div>
              <div v-for="item in sideCategoryItems" :key="item.name" class="cat-item">
                <span class="cat-name">
                  <span class="cat-dot" :style="{ background: item.color }"></span>
                  {{ item.name }}
                </span>
                <span class="cat-count">{{ item.count }}</span>
              </div>
            </div>

            <div class="panel">
              <div class="panel-header">
                <span class="panel-title">最近动态</span>
              </div>
              <div v-if="currentRecentEdits.length">
                <div v-for="item in currentRecentEdits" :key="item.id" class="activity-item">
                  <div class="activity-dot" :style="{ background: resolveActivityColor(item.actionType) }"></div>
                  <div>
                    <div class="activity-text">{{ resolveActivityText(item) }}</div>
                    <div class="activity-time">{{ item.createdAt }}</div>
                  </div>
                </div>
              </div>
              <div v-else class="activity-empty">当前暂无最近动态。</div>
            </div>

            <div class="panel">
              <div class="panel-header">
                <span class="panel-title">本月内容目标</span>
              </div>
              <div class="goal-box">
                <div class="goal-head">
                  <span>已完成</span>
                  <span class="goal-strong">{{ progressInfo.current }} / {{ progressInfo.target }} 条</span>
                </div>
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: `${progressInfo.percent}%` }"></div>
                </div>
                <div class="goal-note">{{ progressInfo.note }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="dialogVisible" class="modal-overlay" @click.self="closeDialog">
      <div class="modal">
        <div class="modal-title">+ {{ isEditing ? `编辑${activeModule.shortLabel}` : `新建${activeModule.shortLabel}` }}</div>

        <div class="form-group">
          <label class="form-label">标题</label>
          <input v-model.trim="draftForm.title" class="form-input" type="text" placeholder="请输入标题…" />
        </div>

        <div class="form-group">
          <label class="form-label">摘要</label>
          <textarea v-model.trim="draftForm.summary" class="form-input" placeholder="简短描述内容…" rows="3"></textarea>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">{{ activeModule.formCategoryLabel }}</label>
            <select v-if="currentType === 'tech'" v-model="draftForm.category" class="form-input">
              <option value="frontend">前端</option>
              <option value="backend">后端</option>
            </select>
            <input
              v-else-if="currentType === 'world'"
              v-model.trim="draftForm.issueLabel"
              class="form-input"
              type="text"
              placeholder="例如 2026.08"
            />
            <select v-else v-model="draftForm.track" class="form-input">
              <option value="agent">Agent</option>
              <option value="multimodal">多模态</option>
              <option value="infra">基础设施</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">状态</label>
            <select v-model="draftForm.visualStatus" class="form-input">
              <option value="published">已发布</option>
              <option value="draft">草稿</option>
              <option value="review">重点关注</option>
            </select>
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">{{ activeModule.formTagLabel }}</label>
            <input
              v-model.trim="draftForm.tagsText"
              class="form-input"
              type="text"
              :placeholder="activeModule.formTagPlaceholder"
            />
          </div>

          <div class="form-group">
            <label class="form-label">发布时间</label>
            <input v-model.trim="draftForm.publishedAt" class="form-input" type="datetime-local" />
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label class="form-label">作者</label>
            <input v-model.trim="draftForm.authorName" class="form-input" type="text" placeholder="请输入作者…" />
          </div>

          <div class="form-group">
            <label class="form-label">封面地址</label>
            <input v-model.trim="draftForm.coverUrl" class="form-input" type="text" placeholder="请输入封面地址…" />
          </div>
        </div>

        <div class="editor-group">
          <label class="form-label">正文内容</label>
          <AdminRichEditor
            v-model="draftForm.contentHtml"
            placeholder="请输入正文内容"
          />
        </div>

        <div class="modal-footer">
          <button class="btn btn-ghost" type="button" @click="closeDialog">取消</button>
          <button class="btn btn-primary" type="button" :disabled="saving" @click="submitDraft">
            {{ isEditing ? '保存修改' : '发布内容' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useAdminConsoleStore } from '@/modules/admin/stores/adminConsole'

const AdminRichEditor = defineAsyncComponent(() => import('@/modules/admin/components/AdminRichEditor.vue'))

const adminStore = useAdminConsoleStore()
const { currentSummary, errorMessage, recentEdits, saving } = storeToRefs(adminStore)

let xlsxLibraryPromise = null

// 业务目的：给左侧导航提供彩色模块图标，让每个模块入口都和参考模板一样有明显的视觉识别。
// 业务逻辑：模块图标统一映射为彩色 emoji，模板层只按模块 key 读取，避免继续使用乱码文字占位。
const sidebarEmojiMap = {
  tech: '📝',
  world: '📰',
  ai: '✨',
  analytics: '📊',
  comment: '💬',
  create: '🆕',
  export: '📤'
}

// 业务目的：严格贴合模板布局时，所有可变标题、文案和字段映射都从模块配置统一生成。
// 业务逻辑：这样可以保持页面长得和模板一致，但表头、侧栏和表单内容仍然是我们项目自己的业务表达。
const moduleOptions = computed(() => [
  {
    key: 'tech',
    icon: '文',
    badge: `${adminStore.getContentList('tech').length}`,
    navLabel: '文章管理',
    topbarTitle: '文章管理',
    shortLabel: '文章',
    panelTitle: '最新文章',
    sideTitle: '文章分类',
    formCategoryLabel: '分类',
    formTagLabel: '亮点',
    formTagPlaceholder: '例如 前端, Vue, 工程化',
    statLabels: {
      total: '文章总数',
      views: '总阅读量',
      comments: '评论数',
      focus: '重点内容'
    }
  },
  {
    key: 'world',
    icon: '刊',
    badge: `${adminStore.getContentList('world').length}`,
    navLabel: '期刊管理',
    topbarTitle: '期刊管理',
    shortLabel: '期刊',
    panelTitle: '最新期刊',
    sideTitle: '期刊结构',
    formCategoryLabel: '期号',
    formTagLabel: '封面信息',
    formTagPlaceholder: '例如 热点观察, 专题聚焦',
    statLabels: {
      total: '期刊总数',
      views: '总阅读量',
      comments: '推荐热度',
      focus: '高推荐'
    }
  },
  {
    key: 'ai',
    icon: 'AI',
    badge: `${adminStore.getContentList('ai').length}`,
    navLabel: '热点管理',
    topbarTitle: 'AI 热点管理',
    shortLabel: '热点',
    panelTitle: '最新热点',
    sideTitle: '热点赛道',
    formCategoryLabel: '赛道',
    formTagLabel: '标签',
    formTagPlaceholder: '例如 Agent, 工作流, 工具调用',
    statLabels: {
      total: '热点总数',
      views: '总浏览量',
      comments: '讨论量',
      focus: '今日热点'
    }
  }
])

const tableSectionRef = ref(null)
const activitySectionRef = ref(null)
const currentType = ref('tech')
const currentFilter = ref('all')
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

const activeModule = computed(() => moduleOptions.value.find((item) => item.key === currentType.value) || moduleOptions.value[0])
const currentRecords = computed(() => adminStore.getContentList(currentType.value))

// 业务目的：表格和统计卡都依赖同一份模块聚合结果，保证模板每块数字表达一致。
// 业务逻辑：统一聚合浏览、评论、当月新增和推荐类计数，避免顶部和右侧面板口径不一致。
const moduleSummary = computed(() => {
  return currentRecords.value.reduce((result, record) => {
    result.contentCount += 1
    result.viewCount += Number(record.viewCount || record.todayReads || 0)
    result.commentCount += Number(record.commentCount || 0)
    result.currentMonthCount += isCurrentMonth(record.publishedAt) ? 1 : 0
    result.featuredCount += Number(isFocusRecord(record))
    return result
  }, {
    contentCount: 0,
    viewCount: 0,
    commentCount: 0,
    currentMonthCount: 0,
    featuredCount: 0
  })
})

const filterTabs = computed(() => {
  if (currentType.value === 'tech') {
    return [
      { key: 'all', label: '全部' },
      { key: 'featured', label: '精选' },
      { key: 'vip', label: 'VIP' },
      { key: 'history', label: '最近浏览' }
    ]
  }

  if (currentType.value === 'world') {
    return [
      { key: 'all', label: '全部' },
      { key: 'recommended', label: '高推荐' },
      { key: 'latest', label: '最新期号' },
      { key: 'cover', label: '封面重点' }
    ]
  }

  return [
    { key: 'all', label: '全部' },
    { key: 'recommended', label: '推荐' },
    { key: 'today', label: '今日热点' },
    { key: 'high-heat', label: '高热度' }
  ]
})

const filteredRecords = computed(() => {
  const baseList = currentRecords.value.filter((record) => matchFilter(record, currentFilter.value))
  const searchValue = keyword.value.trim().toLowerCase()
  if (!searchValue) {
    return baseList
  }

  return baseList.filter((record) => {
    return [
      record.title,
      record.summary,
      record.coverSummary,
      record.authorName,
      record.issueLabel,
      record.track,
      ...(record.highlights || []),
      ...(record.tags || [])
    ]
      .filter(Boolean)
      .some((item) => String(item).toLowerCase().includes(searchValue))
  })
})

const currentRecentEdits = computed(() => {
  const scopedLogs = recentEdits.value.filter((item) => item.contentType === currentType.value)
  return (scopedLogs.length ? scopedLogs : recentEdits.value).slice(0, 4)
})

const sideCategoryItems = computed(() => {
  if (currentType.value === 'tech') {
    return [
      { name: '前端开发', count: countBy(currentRecords.value, (item) => item.category === 'frontend'), color: '#e8c97e' },
      { name: '后端架构', count: countBy(currentRecords.value, (item) => item.category === 'backend'), color: '#7ab0e0' },
      { name: 'VIP 深读', count: countBy(currentRecords.value, (item) => Boolean(item.vip)), color: '#70c99a' },
      { name: '精选分发', count: countBy(currentRecords.value, (item) => Boolean(item.featured)), color: '#e07070' }
    ]
  }

  if (currentType.value === 'world') {
    return currentRecords.value.slice(0, 4).map((item, index) => ({
      name: item.issueLabel || item.title,
      count: `${Number(item.recommendation || 0).toFixed(1)}%`,
      color: ['#e8c97e', '#7ab0e0', '#70c99a', '#e07070'][index] || '#e8c97e'
    }))
  }

  return [
    { name: 'Agent 落地', count: countBy(currentRecords.value, (item) => item.track === 'agent'), color: '#e8c97e' },
    { name: '多模态交互', count: countBy(currentRecords.value, (item) => item.track === 'multimodal'), color: '#7ab0e0' },
    { name: '模型基础设施', count: countBy(currentRecords.value, (item) => item.track === 'infra'), color: '#70c99a' },
    { name: '今日热点', count: countBy(currentRecords.value, (item) => Boolean(item.today)), color: '#e07070' }
  ]
})

const progressInfo = computed(() => {
  const targetMap = { tech: 20, world: 6, ai: 10 }
  const target = targetMap[currentType.value] || 10
  const current = moduleSummary.value.currentMonthCount
  const percent = Math.min(100, Math.round((current / target) * 100))
  const remain = Math.max(0, target - current)
  return {
    current,
    target,
    percent,
    note: remain > 0 ? `距离目标还差 ${remain} 条，继续补齐内容节奏。` : '本月目标已完成，当前可以继续扩充重点内容。'
  }
})

watch(currentType, (nextType) => {
  keyword.value = ''
  currentFilter.value = 'all'
  adminStore.stopRealtime()
  adminStore.startRealtime(nextType)
})

onMounted(() => {
  adminStore.startRealtime(currentType.value)
})

onBeforeUnmount(() => {
  adminStore.stopRealtime()
})

function switchType(type) {
  currentType.value = type
}

// 业务目的：按模块 key 返回左侧导航的彩色图标字符，保证每个模块都有稳定图标。
// 业务逻辑：已配置模块直接返回对应 emoji，未命中时回退到文章图标，避免导航出现空白占位。
function resolveSidebarEmoji(iconKey) {
  return sidebarEmojiMap[iconKey] || sidebarEmojiMap.tech
}

function scrollToTable() {
  tableSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function scrollToActivity() {
  activitySectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function matchFilter(record, filterKey) {
  if (filterKey === 'all') {
    return true
  }

  if (currentType.value === 'tech') {
    return {
      featured: Boolean(record.featured),
      vip: Boolean(record.vip),
      history: Boolean(record.history)
    }[filterKey]
  }

  if (currentType.value === 'world') {
    return {
      recommended: Number(record.recommendation || 0) >= 80,
      latest: isCurrentMonth(record.publishedAt),
      cover: Boolean(record.coverSummary)
    }[filterKey]
  }

  return {
    recommended: Boolean(record.recommended),
    today: Boolean(record.today),
    'high-heat': Number(record.heat || 0) >= 80
  }[filterKey]
}

function resolveMeta(record) {
  if (record.type === 'tech') {
    return `${record.publishedAt || '--'} / ${record.authorName || '匿名作者'}`
  }
  if (record.type === 'world') {
    return `${record.publishedAt || '--'} / ${record.issueLabel || '未设置期号'}`
  }
  return `${record.publishedAt || '--'} / ${record.authorName || '匿名来源'}`
}

function resolveCategory(record) {
  if (record.type === 'tech') {
    return record.category === 'frontend' ? '前端开发' : '后端架构'
  }
  if (record.type === 'world') {
    return record.issueLabel || '期刊'
  }
  return record.track || 'AI'
}

function resolveStatus(record) {
  if (record.type === 'tech') {
    if (record.featured) {
      return { label: '精选', className: 'badge-published' }
    }
    if (record.vip) {
      return { label: 'VIP', className: 'badge-review' }
    }
    return { label: '已发布', className: 'badge-draft' }
  }

  if (record.type === 'world') {
    if (Number(record.recommendation || 0) >= 80) {
      return { label: '重点推荐', className: 'badge-published' }
    }
    if (Number(record.recommendation || 0) >= 75) {
      return { label: '持续关注', className: 'badge-review' }
    }
    return { label: '常规期号', className: 'badge-draft' }
  }

  if (record.today) {
    return { label: '今日热点', className: 'badge-review' }
  }
  if (record.recommended) {
    return { label: '推荐', className: 'badge-published' }
  }
  return { label: '已收录', className: 'badge-draft' }
}

function resolveReadValue(record) {
  return formatCount(record.viewCount || record.todayReads || 0)
}

function resolveActivityColor(actionType) {
  return {
    update: '#70c99a',
    delete: '#e07070',
    'batch-import': '#e8c97e'
  }[actionType] || '#7ab0e0'
}

function resolveActivityText(item) {
  return `${resolveActionLabel(item.actionType)}《${item.contentTitle}》`
}

function resolveActionLabel(actionType) {
  return {
    update: '内容已更新',
    delete: '内容已删除',
    'batch-import': '批量导入完成'
  }[actionType] || '内容已变更'
}

function isFocusRecord(record) {
  if (record.type === 'tech') {
    return record.featured || record.vip
  }
  if (record.type === 'world') {
    return Number(record.recommendation || 0) >= 80
  }
  return record.today || record.recommended
}

function countBy(records, predicate) {
  return records.filter(predicate).length
}

function isCurrentMonth(value) {
  if (!value) {
    return false
  }
  const normalized = String(value).replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) {
    return false
  }
  const now = new Date()
  return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth()
}

function formatCount(value) {
  const count = Number(value || 0)
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}k`
  }
  if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`
  }
  return `${count}`
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
  const confirmed = window.confirm(`确认删除《${record.title}》吗？`)
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
  XLSX.utils.book_append_sheet(workbook, worksheet, activeModule.value.shortLabel)
  XLSX.writeFile(workbook, `peakstars-${currentType.value}-content.xlsx`)
}

function createEmptyDraft() {
  return {
    id: '',
    title: '',
    category: 'frontend',
    summary: '',
    authorName: '',
    coverUrl: '/peakstars-blog-icon.jpg',
    contentHtml: '<p>请输入正文内容</p>',
    publishedAt: formatDateTimeLocal(new Date()),
    issueLabel: '',
    recommendation: 80,
    track: 'agent',
    heat: 80,
    featured: false,
    vip: false,
    history: false,
    recommended: true,
    today: false,
    highlightsText: '',
    tagsText: '',
    visualStatus: 'published'
  }
}

function createDraftFromRecord(record) {
  return {
    id: record.id || '',
    title: record.title || '',
    category: record.category || 'frontend',
    summary: record.summary || '',
    authorName: record.authorName || '',
    coverUrl: record.coverUrl || '/peakstars-blog-icon.jpg',
    contentHtml: record.contentHtml || '<p>请输入正文内容</p>',
    publishedAt: normalizeDateTimeLocal(record.publishedAt),
    issueLabel: record.issueLabel || '',
    recommendation: Number(record.recommendation || 80),
    track: record.track || 'agent',
    heat: Number(record.heat || 80),
    featured: Boolean(record.featured),
    vip: Boolean(record.vip),
    history: Boolean(record.history),
    recommended: Boolean(record.recommended),
    today: Boolean(record.today),
    highlightsText: [record.coverKicker, ...(record.highlights || [])].filter(Boolean).join(', '),
    tagsText: (record.tags || []).join(', '),
    visualStatus: inferVisualStatus(record)
  }
}

// 业务目的：在页面完全照模板收口后，保存请求仍要兼容现有后台三类内容接口。
// 业务逻辑：表单只暴露模板级基础字段，其余后台所需字段通过模块默认值和可视状态推导补齐。
function buildSavePayload(form, type) {
  if (type === 'tech') {
    const isFeatured = form.visualStatus === 'published' || form.visualStatus === 'review'
    const isVip = form.visualStatus === 'review'
    return {
      id: form.id || '',
      category: form.category,
      title: form.title,
      summary: form.summary,
      essence: form.summary,
      authorName: form.authorName || 'PeakStars',
      authorRole: '后台内容维护',
      coverUrl: form.coverUrl,
      contentHtml: form.contentHtml,
      publishedAt: normalizeDateTimePayload(form.publishedAt),
      viewCount: 0,
      commentCount: 0,
      likeCount: 0,
      collectCount: 0,
      readTime: '6 min',
      featured: isFeatured,
      vip: isVip,
      collected: false,
      liked: false,
      history: form.visualStatus === 'draft' || form.history,
      highlights: splitCommaText(form.tagsText || form.highlightsText)
    }
  }

  if (type === 'world') {
    const tagList = splitCommaText(form.tagsText || form.highlightsText)
    return {
      id: form.id || '',
      title: form.title,
      issueLabel: form.issueLabel,
      summary: form.summary,
      publishedAt: normalizeDateTimePayload(form.publishedAt),
      viewCount: 0,
      likeCount: 0,
      commentCount: 0,
      recommendation: form.visualStatus === 'review' ? 85 : Number(form.recommendation || 78),
      coverAccent: '#d33b2d',
      coverKicker: tagList[0] || '专题聚焦',
      coverHeadline: form.title,
      coverSummary: form.summary,
      coverFooter: '专题 / 深读 / 评论',
      coverUrl: form.coverUrl,
      contentHtml: form.contentHtml
    }
  }

  return {
    id: form.id || '',
    title: form.title,
    summary: form.summary,
    authorName: form.authorName || 'PeakStars',
    track: form.track,
    hotspotType: form.track,
    publishedAt: normalizeDateTimePayload(form.publishedAt),
    viewCount: 0,
    commentCount: 0,
    likeCount: 0,
    heat: form.visualStatus === 'review' ? 90 : Number(form.heat || 80),
    recommended: form.visualStatus !== 'draft',
    today: form.visualStatus === 'review' || form.today,
    tags: splitCommaText(form.tagsText),
    coverUrl: form.coverUrl,
    contentHtml: form.contentHtml
  }
}

function buildExportRow(record, type) {
  if (type === 'tech') {
    return {
      id: record.id,
      title: record.title,
      category: record.category,
      summary: record.summary,
      authorName: record.authorName,
      publishedAt: record.publishedAt,
      highlights: (record.highlights || []).join(', '),
      featured: record.featured,
      vip: record.vip,
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
      recommendation: record.recommendation,
      publishedAt: record.publishedAt,
      coverKicker: record.coverKicker,
      coverSummary: record.coverSummary,
      coverUrl: record.coverUrl,
      contentHtml: record.contentHtml
    }
  }

  return {
    id: record.id,
    title: record.title,
    track: record.track,
    summary: record.summary,
    authorName: record.authorName,
    heat: record.heat,
    recommended: record.recommended,
    today: record.today,
    tags: (record.tags || []).join(', '),
    publishedAt: record.publishedAt,
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
      essence: row.summary,
      authorName: row.authorName || 'PeakStars',
      authorRole: '后台内容维护',
      publishedAt: row.publishedAt,
      viewCount: Number(row.viewCount || 0),
      commentCount: Number(row.commentCount || 0),
      likeCount: Number(row.likeCount || 0),
      collectCount: Number(row.collectCount || 0),
      readTime: row.readTime || '6 min',
      featured: parseBooleanish(row.featured),
      vip: parseBooleanish(row.vip),
      collected: false,
      liked: false,
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
      coverAccent: row.coverAccent || '#d33b2d',
      coverKicker: row.coverKicker || '专题聚焦',
      coverHeadline: row.coverHeadline || row.title,
      coverSummary: row.coverSummary || row.summary,
      coverFooter: row.coverFooter || '专题 / 深读 / 评论',
      coverUrl: row.coverUrl,
      contentHtml: row.contentHtml || `<p>${row.summary || ''}</p>`
    }
  }

  return {
    id: row.id,
    title: row.title,
    track: row.track || 'agent',
    hotspotType: row.hotspotType || row.track || 'agent',
    summary: row.summary,
    authorName: row.authorName || 'PeakStars',
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

function inferVisualStatus(record) {
  if (record.type === 'tech') {
    if (record.vip) {
      return 'review'
    }
    if (record.history) {
      return 'draft'
    }
    return 'published'
  }
  if (record.type === 'world') {
    return Number(record.recommendation || 0) >= 80 ? 'review' : 'published'
  }
  if (record.today) {
    return 'review'
  }
  if (!record.recommended) {
    return 'draft'
  }
  return 'published'
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

// 业务目的：模板视觉保持一致的前提下，仍保留批量导入导出能力，避免后台运营能力倒退。
// 业务逻辑：xlsx 运行时继续按需加载，页面结构再像模板，也不把额外依赖提前打进首屏。
async function loadXlsxLibrary() {
  if (!xlsxLibraryPromise) {
    xlsxLibraryPromise = import('xlsx')
  }
  return xlsxLibraryPromise
}
</script>

<style scoped src="../styles/AdminDashboard.css"></style>
