<template>
  <div class="detail-page">
    <div v-if="loading" class="detail-loading">加载中...</div>
    <div v-else-if="error" class="detail-error">
      <p>{{ error }}</p>
      <button @click="$router.push('/interview')" class="detail-error-btn">返回题库列表</button>
    </div>

    <div v-else-if="interview" class="detail-shell" :class="{ 'detail-shell--left-collapsed': isLeftPanelCollapsed }">
      <aside class="detail-left" :class="{ 'is-collapsed': isLeftPanelCollapsed }">
        <div class="detail-left-head">
          <div class="detail-left-title-row">
            <span class="detail-left-menu">☰</span>
            <span class="detail-left-title">题目列表</span>
          </div>
          <button class="detail-left-collapse" type="button" @click="goBack">‹</button>
        </div>

        <div class="detail-left-search">
          <input
            v-model.trim="questionKeyword"
            class="detail-left-search-input"
            type="text"
            placeholder="搜索题目"
          />
        </div>

        <nav class="detail-left-list">
          <a
            v-for="item in filteredRelatedArticles"
            :key="item.id"
            class="detail-left-item"
            :class="{ active: String(item.id) === interviewId }"
            @click.prevent="goDetail(item.id)"
          >
            <span class="detail-left-text">{{ item.title }}</span>
          </a>
        </nav>
      </aside>

      <main class="detail-main">
        <button class="detail-back-link" type="button" @click="goBack">← 返回题库列表</button>

        <section class="detail-question-card">
          <h1 class="detail-title">{{ interview.title }}</h1>

          <div class="detail-tags">
            <span class="detail-level-badge">{{ resolveLevelLabel(interview) }}</span>
            <span class="detail-tag">{{ resolveCategoryLabel(interview.category) }}</span>
            <span
              v-for="tag in interview.tags"
              :key="tag"
              class="detail-tag"
            >
              {{ tag }}
            </span>
          </div>

          <div v-if="interview.date" class="detail-publish-time">
            <span class="detail-publish-label">发布时间</span>
            <span class="detail-publish-value">{{ interview.date }}</span>
          </div>

          <div class="detail-meta-row">
            <button class="detail-meta-action" type="button" @click="toggleCollect">
              <span>{{ isCollected ? '★' : '☆' }}</span>
              <span>{{ isCollected ? '已收藏' : '标记' }}</span>
            </button>
            <button class="detail-meta-action" type="button" @click="toggleLike">
              <span>{{ isLiked ? '👍' : '👍🏻' }}</span>
              <span>{{ localLikes }}</span>
            </button>
            <span class="detail-meta-item">👁 {{ interview.views }}</span>
            <button class="detail-meta-action" type="button" @click="copyLink">
              <span>🔗</span>
              <span>{{ copyTip }}</span>
            </button>
          </div>
        </section>

        <article ref="detailContentRef" class="detail-content-card" @click="handleCodeBlockAction" @change="handleCodeBlockAction">
          <section class="detail-answer-block">
            <component
              v-if="summaryBlock && summaryBlock.title"
              :is="'h' + summaryBlock.level"
              :id="summaryBlock.id"
              class="detail-section-title"
              :class="{ 'detail-section-title--highlight': summaryBlock.level === 2 }"
            >
              {{ summaryBlock.title }}
            </component>
            <div
              v-if="summaryBlock"
              class="detail-section-body"
              v-html="summaryBlock.html"
            ></div>
          </section>

          <section
            v-for="section in analysisBlocks"
            :key="section.id"
            class="detail-analysis-block"
          >
            <component
              v-if="section.title"
              :is="'h' + section.level"
              :id="section.id"
              class="detail-section-title"
              :class="{ 'detail-section-title--highlight': section.level === 2 }"
            >
              {{ section.title }}
            </component>
            <div class="detail-section-body" v-html="section.html"></div>
          </section>
        </article>
      </main>

      <aside class="detail-right" v-if="outlineItems.length">
        <div class="detail-right-wrap">
          <div class="detail-right-head">
            <span class="detail-right-title">目录</span>
            <button class="detail-right-fold" type="button" @click="scrollToTop">‹</button>
          </div>
          <nav class="detail-right-toc">
            <a
              v-for="item in outlineItems"
              :key="item.id"
              :href="'#' + item.id"
              class="detail-right-link"
              :class="[activeOutlineId === item.id ? 'active' : '', 'h' + item.level]"
              @click.prevent="scrollToHeading(item.id)"
            >
              <span class="detail-right-text">{{ item.text }}</span>
            </a>
          </nav>
        </div>
      </aside>
    </div>

    <div v-else class="detail-not-found">
      <p>面经不存在</p>
      <button @click="goBack" class="detail-error-btn">返回题库列表</button>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { collectInterview, getInterviewDetail, getInterviews, likeInterview } from '@/api/interview.js'

const route = useRoute()
const router = useRouter()

const interview = ref(null)
const loading = ref(true)
const error = ref('')
const isLiked = ref(false)
const isCollected = ref(false)
const localLikes = ref(0)
const copyTip = ref('复制链接')
const activeOutlineId = ref('')
const outlineItems = ref([])
const relatedArticles = ref([])
const questionKeyword = ref('')
const isLeftPanelCollapsed = ref(false)
const detailContentRef = ref(null)
const detailPageClass = 'interview-detail-page'
const codeBlockLanguages = ['auto', 'java', 'javascript', 'typescript', 'python', 'json', 'xml', 'sql', 'bash', 'css', 'yaml', 'text']
const codeLanguageLabelMap = {
  auto: '自动识别',
  java: 'Java',
  javascript: 'JavaScript',
  typescript: 'TypeScript',
  python: 'Python',
  json: 'JSON',
  xml: 'XML / HTML',
  sql: 'SQL',
  bash: 'Bash',
  css: 'CSS',
  yaml: 'YAML',
  text: '纯文本'
}

const interviewId = computed(() => String(route.params.id))

/**
 * 统一承接题库详情内容
 * 把纯文本面经拆成摘要区与分析区，映射为更贴近题库知识页的结构化版式
 */
const normalizedSections = computed(() => {
  const content = interview.value?.content || ''
  if (!content.trim()) {
    return []
  }

  if (content.trim().startsWith('<')) {
    return [
      {
        id: 'outline-summary',
        title: '',
        level: 2,
        html: enhanceCodeBlocks(content)
      }
    ]
  }

  const lines = content
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)

  const sections = []
  let currentSection = null
  let paragraphBuffer = []

  const flushParagraph = () => {
    if (paragraphBuffer.length === 0) {
      return
    }
    if (!currentSection) {
      currentSection = {
        id: `outline-${sections.length + 1}`,
        title: '',
        level: 0,
        parts: []
      }
      sections.push(currentSection)
    }
    currentSection.parts.push(`<p>${styleInline(paragraphBuffer.join(' '))}</p>`)
    paragraphBuffer = []
  }

  const pushSection = (title, level = 2) => {
    flushParagraph()
    currentSection = {
      id: `outline-${sections.length + 1}`,
      title,
      level,
      parts: []
    }
    sections.push(currentSection)
  }

  lines.forEach((line) => {
    const normalizedLine = line.replace(/\*+/g, '*')

    if (normalizedLine.startsWith('### ')) {
      flushParagraph()
      pushSection(normalizedLine.replace(/^### /, ''), 3)
      return
    }

    if (normalizedLine.startsWith('**') && normalizedLine.endsWith('**')) {
      const title = normalizedLine.replace(/^\*\*/, '').replace(/\*\*$/, '').trim()
      if (title) {
        pushSection(title, 2)
      }
      return
    }

    if (/^\d+\./.test(normalizedLine) || /^-\s/.test(normalizedLine)) {
      flushParagraph()
      if (!currentSection) {
        pushSection('', 0)
      }
      currentSection.parts.push(
        `<p class="detail-list-item">${styleInline(
          normalizedLine.replace(/^\d+\.\s*/, '').replace(/^-\s*/, '')
        )}</p>`
      )
      return
    }

    paragraphBuffer.push(normalizedLine)
  })

  flushParagraph()

  return sections
    .map((section) => ({
      ...section,
      html: enhanceCodeBlocks(section.parts.join(''))
    }))
    .filter((section) => section.html.trim())
})

const summaryBlock = computed(() => normalizedSections.value[0] || null)
const analysisBlocks = computed(() => normalizedSections.value.slice(1))

/**
 * 生成左侧题目筛选列表
 * 始终把当前题目置顶，剩余题目按关键字做本地过滤，保持题库浏览体验
 */
const filteredRelatedArticles = computed(() => {
  const current = interview.value
    ? [{
        id: interview.value.id,
        title: interview.value.title
      }]
    : []

  const keyword = questionKeyword.value.toLowerCase()
  const rest = relatedArticles.value.filter((item) => {
    if (!keyword) {
      return true
    }
    return String(item.title || '').toLowerCase().includes(keyword)
  })

  const merged = [...current, ...rest]
  const used = new Set()

  return merged.filter((item) => {
    const key = String(item.id)
    if (used.has(key)) {
      return false
    }
    used.add(key)
    return !keyword || String(item.title || '').toLowerCase().includes(keyword) || key === interviewId.value
  })
})

onMounted(async () => {
  applyDetailPageShell()
  await fetchDetail()
  window.addEventListener('scroll', handleWindowScroll, { passive: true })
  updateScrollState()
})

onBeforeUnmount(() => {
  clearDetailPageShell()
  window.removeEventListener('scroll', handleWindowScroll)
})

/**
 * 标记当前详情页为独立白底模式
 * 通过给 body 挂载专属类名，精准覆盖全局深色主题外壳背景，不影响其他页面
 */
function applyDetailPageShell() {
  document.body.classList.add(detailPageClass)
}

/**
 * 退出详情页时清理白底模式标记
 * 避免离开页面后全局壳层继续沿用详情页的白色背景
 */
function clearDetailPageShell() {
  document.body.classList.remove(detailPageClass)
}

async function fetchDetail() {
  loading.value = true
  error.value = ''
  outlineItems.value = []
  activeOutlineId.value = ''

  try {
    const data = await getInterviewDetail(route.params.id)
    interview.value = data
    localLikes.value = data.likes ?? 0
    await loadRelatedArticles()
  } catch {
    error.value = '面经加载失败，请检查后端服务是否已经启动。'
  } finally {
    loading.value = false
    await nextTick()
    if (!error.value && interview.value) {
      syncOutline()
      bootstrapCodeBlocks()
      updateScrollState()
    }
  }
}

async function loadRelatedArticles() {
  try {
    const result = await getInterviews({ pageSize: 18 })
    relatedArticles.value = (result.list || []).filter((item) => String(item.id) !== interviewId.value)
  } catch {
    relatedArticles.value = []
  }
}

/**
 * 同步右侧目录结构
 * 目录来源于页面拆分后的块级章节，保证滚动定位与布局标题完全一致
 */
function syncOutline() {
  const container = detailContentRef.value
  if (!container) {
    outlineItems.value = []
    return
  }

  const headings = container.querySelectorAll('h1, h2, h3, h4, h5, h6')
  const items = []

  headings.forEach((heading, index) => {
    if (!heading.id) {
      heading.id = `heading-${index + 1}`
    }

    const text = heading.textContent.trim()
    if (!text) return

    items.push({
      id: heading.id,
      text,
      level: parseInt(heading.tagName.charAt(1)),
      index: items.length + 1
    })
  })

  outlineItems.value = items
  activeOutlineId.value = items[0]?.id || ''
}

function updateScrollState() {
  let activeId = outlineItems.value[0]?.id || ''
  outlineItems.value.forEach((item) => {
    const target = document.getElementById(item.id)
    if (target && target.getBoundingClientRect().top <= 160) {
      activeId = item.id
    }
  })
  activeOutlineId.value = activeId
}

function handleWindowScroll() {
  updateScrollState()
}

function scrollToHeading(id) {
  const target = document.getElementById(id)
  if (!target) {
    return
  }
  target.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function toggleLike() {
  isLiked.value = !isLiked.value
  if (isLiked.value) {
    try {
      const result = await likeInterview(route.params.id)
      localLikes.value = result.likes
    } catch {
      localLikes.value += 1
    }
    return
  }
  localLikes.value = Math.max(0, localLikes.value - 1)
}

async function toggleCollect() {
  isCollected.value = !isCollected.value
  if (!isCollected.value) {
    return
  }
  try {
    await collectInterview(route.params.id)
  } catch {
    // 收藏接口失败时保留前端交互状态
    // 避免用户重复点击造成页面感知抖动
  }
}

function copyLink() {
  const url = window.location.href
  navigator.clipboard.writeText(url).then(() => {
    copyTip.value = '已复制'
    window.setTimeout(() => {
      copyTip.value = '复制链接'
    }, 2000)
  }).catch(() => {
    copyTip.value = '已复制'
    window.setTimeout(() => {
      copyTip.value = '复制链接'
    }, 2000)
  })
}

function goBack(event) {
  if (event?.currentTarget?.classList?.contains('detail-left-collapse')) {
    isLeftPanelCollapsed.value = !isLeftPanelCollapsed.value
    return
  }
  router.push('/interview')
}

async function goDetail(id) {
  if (String(id) === interviewId.value) {
    return
  }
  await router.push(`/interview/${id}`)
  await fetchDetail()
}

function resolveCategoryLabel(category) {
  if (category === 'java') {
    return 'Java'
  }
  if (category === 'frontend') {
    return '前端'
  }
  if (category === 'agent') {
    return 'Agent开发'
  }
  if (category === 'llm') {
    return '大模型原理'
  }
  return '面经'
}

function resolveLevelLabel(data) {
  const viewCount = Number(data?.views || 0)
  if (viewCount >= 3500) {
    return '困难'
  }
  if (viewCount >= 2200) {
    return '中等'
  }
  return '简单'
}

/**
 * 目的：为代码块添加语言标签和复制按钮，显示在右上角。
 * 逻辑：匹配所有 <pre> 标签，包裹成带右上角工具栏的深色代码卡片。
 */
function enhanceCodeBlocks(html) {
  if (/<figure\s+class="code-block"/i.test(html)) return html

  return html.replace(
    /<pre([^>]*)>([\s\S]*?)<\/pre>/gi,
    (_, preAttrs = '', preContent = '') => {
      const codeMatch = preContent.match(/<code([^>]*)>([\s\S]*?)<\/code>/i)
      const codeAttrs = codeMatch?.[1] || ''
      const rawCode = decodeHtml(codeMatch?.[2] || preContent.replace(/<[^>]+>/g, ''))
      const sourceLanguage = extractCodeLanguage(preAttrs, codeAttrs, rawCode)
      return [
        `<figure class="code-block" data-source-language="${encodeAttribute(sourceLanguage)}" data-detected-language="${encodeAttribute(sourceLanguage)}" data-current-language="auto">`,
        '<div class="code-toolbar">',
        '<div class="code-toolbar-main">',
        `<label class="code-lang-select-wrap"><span class="code-lang-caret">▼</span><select class="code-lang-select" data-code-action="switch-language">${buildLanguageOptions(sourceLanguage)}</select></label>`,
        `<span class="code-language-hint">已识别：${formatLanguageLabel(sourceLanguage)}</span>`,
        '</div>',
        '<button type="button" class="code-copy-btn" data-code-action="copy-code">复制代码</button>',
        '</div>',
        `<pre${preAttrs}><code class="language-${sourceLanguage}" data-raw-code="${encodeAttribute(rawCode)}">${encodeHtml(rawCode)}</code></pre>`,
        '</figure>'
      ].join('')
    }
  )
}

function styleInline(text) {
  return text
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
}

/**
 * 初始化详情页代码块交互
 * 统一在内容渲染完成后设置默认语言并补充首屏语法高亮
 */
function bootstrapCodeBlocks() {
  const container = detailContentRef.value
  if (!container) {
    return
  }

  container.querySelectorAll('.code-block').forEach((block) => {
    applyLanguageToCodeBlock(block, block.dataset.currentLanguage || 'auto')
  })
}

/**
 * 响应代码块工具栏操作
 * 通过事件委托统一处理语言切换与复制动作，降低富文本节点维护成本
 */
function handleCodeBlockAction(event) {
  if (event.target.matches('.code-lang-select')) {
    const codeBlock = event.target.closest('.code-block')
    if (codeBlock) {
      applyLanguageToCodeBlock(codeBlock, event.target.value || 'auto')
    }
    return
  }

  const actionTarget = event.target.closest('[data-code-action]')
  if (!actionTarget || !detailContentRef.value?.contains(actionTarget)) {
    return
  }

  const codeBlock = actionTarget.closest('.code-block')
  if (!codeBlock) {
    return
  }

  const action = actionTarget.dataset.codeAction
  if (action === 'switch-language') {
    applyLanguageToCodeBlock(codeBlock, actionTarget.dataset.language || 'auto')
    return
  }

  if (action === 'copy-code') {
    copyCodeBlock(codeBlock, actionTarget)
  }
}

/**
 * 切换代码块展示语言
 * 根据选中的语言重新着色源码，并同步工具栏按钮状态
 */
function applyLanguageToCodeBlock(codeBlock, language) {
  const codeElement = codeBlock.querySelector('code')
  if (!codeElement) {
    return
  }

  const rawCode = decodeHtml(codeElement.dataset.rawCode || '')
  const detectedLanguage = detectLanguage(rawCode, codeBlock.dataset.sourceLanguage || codeElement.className)
  const renderLanguage = language === 'auto' ? detectedLanguage : resolveCodeLanguage(language) || detectedLanguage

  codeElement.innerHTML = highlightCode(rawCode, renderLanguage)
  codeElement.className = `language-${renderLanguage}`
  codeElement.dataset.activeLanguage = renderLanguage
  codeBlock.dataset.detectedLanguage = detectedLanguage
  codeBlock.dataset.currentLanguage = language
  syncLanguageButtons(codeBlock, language, renderLanguage, detectedLanguage)
}

/**
 * 同步语言按钮的选中反馈
 * 让 Auto 按钮同时提示当前实际识别出的语言，方便用户判断效果
 */
function syncLanguageButtons(codeBlock, selectedLanguage, activeLanguage, detectedLanguage) {
  const selectElement = codeBlock.querySelector('.code-lang-select')
  if (selectElement) {
    updateLanguageOptions(
      selectElement,
      codeBlock.dataset.sourceLanguage || detectedLanguage || activeLanguage,
      activeLanguage,
      detectedLanguage
    )
    selectElement.value = selectedLanguage
    selectElement.dataset.activeLanguage = activeLanguage
  }

  const languageHint = codeBlock.querySelector('.code-language-hint')
  if (languageHint) {
    languageHint.textContent = `已识别：${formatLanguageLabel(detectedLanguage || activeLanguage)}`
  }
}

/**
 * 复制代码块原始内容
 * 始终写入未高亮源码，避免复制结果混入样式标签
 */
function copyCodeBlock(codeBlock, trigger) {
  const codeElement = codeBlock.querySelector('code')
  const rawCode = decodeHtml(codeElement?.dataset.rawCode || codeElement?.textContent || '')
  const resetText = () => {
    window.setTimeout(() => {
      trigger.textContent = '复制'
    }, 1600)
  }

  navigator.clipboard.writeText(rawCode).then(() => {
    trigger.textContent = '已复制'
    resetText()
  }).catch(() => {
    trigger.textContent = '复制失败'
    resetText()
  })
}

/**
 * 自动识别代码块语言
 * 优先沿用原始 class 标记，再根据典型语法特征做轻量匹配
 */
function detectLanguage(code, sourceLanguage) {
  const normalizedSource = resolveCodeLanguage(sourceLanguage)
  if (normalizedSource && normalizedSource !== 'auto') {
    return normalizedSource
  }

  const content = code.trim()
  if (!content) {
    return 'text'
  }

  if (/^\s*[{[]/.test(content) && /"\s*:/.test(content)) {
    return 'json'
  }

  if (/^---\s*$[\s\S]*?:\s*.+/m.test(content) || /^\s*[\w.-]+\s*:\s*.+/m.test(content) && !/[;{}<>]/.test(content)) {
    return 'yaml'
  }

  if (/<\/?[a-z][\w:-]*[\s>]/i.test(content) || /^<\?xml/i.test(content)) {
    return 'xml'
  }

  if (/@media\b|@keyframes\b|^\s*[.#]?[\w-]+(?:\s+[.#]?[\w-]+)*\s*\{[\s\S]*:[\s\S]*\}/m.test(content)) {
    return 'css'
  }

  if (/\b(select|insert|update|delete|from|where|order\s+by|group\s+by|join|create\s+table|alter\s+table)\b/i.test(content)) {
    return 'sql'
  }

  if (/(^|\s)def\s+|(\belif\b|\bself\b|\bNone\b|\bTrue\b|\bFalse\b|print\(|from\s+\w+\s+import\s+|__name__\s*==\s*['"]__main__['"])/.test(content)) {
    return 'python'
  }

  if (/\b(interface|type)\s+[A-Z][\w$]*\s*[={<]|:\s*(string|number|boolean|unknown|never|Record|Partial|Promise|void)\b|implements\s+[A-Z]/.test(content)) {
    return 'typescript'
  }

  if (/\b(package\s+\w|import\s+(static\s+)?[\w.]+\.\*?\s*;|System\.out|@Override|public\s+static\s+void\s+main)\b/.test(content)) {
    return 'java'
  }

  if (/\b(public|private|protected|class|interface|implements|extends|new\s+[A-Z][\w$]*\()\b/.test(content)) {
    return 'java'
  }

  if (/\b(const|let|var|function|=>|import\s+.+from|export\s+default|console\.log|document\.|window\.|addEventListener|Promise\b|async\b|await\b)\b/.test(content)) {
    return 'javascript'
  }

  if (/^\s*(#!\/bin\/bash|#!\/bin\/sh|echo\s+|npm\s+|pnpm\s+|yarn\s+|if\s+\[|then$|fi$|grep\s+|chmod\s+)/m.test(content)) {
    return 'bash'
  }

  return 'text'
}

/**
 * 生成指定语言的高亮代码内容
 * 采用零依赖轻量规则，满足语言切换后的即时高亮反馈
 */
function highlightCode(code, language) {
  const normalizedLanguage = resolveCodeLanguage(language) || 'text'
  if (normalizedLanguage === 'text') {
    return encodeHtml(code)
  }

  return applySyntaxRules(encodeHtml(code), normalizedLanguage)
}

/**
 * 应用语言级语法着色规则
 * 先保护注释与字符串，再补充关键字和函数等 token 样式
 */
function applySyntaxRules(escapedCode, language) {
  if (language === 'xml') {
    return escapedCode
      .replace(/(&lt;!--[\s\S]*?--&gt;)/g, '<span class="token-comment">$1</span>')
      .replace(/(&lt;\/?)([\w:-]+)/g, '$1<span class="token-keyword">$2</span>')
      .replace(/([\w:-]+)(=)(&quot;.*?&quot;|&#39;.*?&#39;)/g, '<span class="token-property">$1</span>$2<span class="token-string">$3</span>')
  }

  if (language === 'json') {
    return escapedCode
      .replace(/(&quot;[^&]*?&quot;)(\s*:)/g, '<span class="token-property">$1</span>$2')
      .replace(/(:\s*)(&quot;.*?&quot;)/g, '$1<span class="token-string">$2</span>')
      .replace(/\b(true|false|null)\b/g, '<span class="token-keyword">$1</span>')
      .replace(/\b(-?\d+(?:\.\d+)?)\b/g, '<span class="token-number">$1</span>')
  }

  if (language === 'yaml') {
    return escapedCode
      .replace(/(^|\n)(\s*#.*?$)/gm, '$1<span class="token-comment">$2</span>')
      .replace(/(^|\n)(\s*[\w.-]+)(\s*:)/g, '$1<span class="token-property">$2</span>$3')
      .replace(/(:\s*)(&quot;.*?&quot;|&#39;.*?&#39;)/g, '$1<span class="token-string">$2</span>')
      .replace(/\b(true|false|null)\b/g, '<span class="token-keyword">$1</span>')
      .replace(/\b(-?\d+(?:\.\d+)?)\b/g, '<span class="token-number">$1</span>')
  }

  let content = escapedCode
  const placeholders = []
  const reserveToken = (pattern, className) => {
    content = content.replace(pattern, (match) => {
      const key = `§§TOKEN_${placeholders.length}§§`
      placeholders.push({
        key,
        value: `<span class="${className}">${match}</span>`
      })
      return key
    })
  }

  if (language === 'java' || language === 'javascript' || language === 'typescript') {
    reserveToken(/\/\/[^\n\r]*|\/\*[\s\S]*?\*\//g, 'token-comment')
  }

  if (language === 'sql') {
    reserveToken(/--[^\n\r]*|\/\*[\s\S]*?\*\//g, 'token-comment')
  }

  if (language === 'bash' || language === 'python' || language === 'yaml') {
    reserveToken(/#[^\n\r]*/g, 'token-comment')
  }

  if (language === 'css') {
    reserveToken(/\/\*[\s\S]*?\*\//g, 'token-comment')
  }

  reserveToken(/&quot;[\s\S]*?&quot;|&#39;[\s\S]*?&#39;|`[^`]*`/g, 'token-string')

  const keywordPatterns = {
    java: /\b(package|import|public|private|protected|class|static|final|void|new|return|if|else|switch|case|break|continue|for|while|try|catch|finally|throw|throws|extends|implements|interface|enum|this|super|null|true|false)\b/g,
    javascript: /\b(import|from|export|default|const|let|var|function|return|if|else|switch|case|break|continue|for|while|try|catch|finally|throw|new|class|extends|async|await|null|true|false|typeof)\b/g,
    typescript: /\b(import|from|export|default|const|let|var|function|return|if|else|switch|case|break|continue|for|while|try|catch|finally|throw|new|class|extends|async|await|null|true|false|typeof|interface|type|enum|implements|readonly|public|private|protected|as|infer|keyof)\b/g,
    python: /\b(def|class|return|if|elif|else|for|while|try|except|finally|raise|import|from|as|with|lambda|yield|pass|break|continue|in|is|not|and|or|None|True|False)\b/g,
    sql: /\b(select|from|where|and|or|order|by|group|having|limit|offset|insert|into|values|update|set|delete|left|right|inner|join|on|as|distinct|count|sum|max|min|case|when|then|else|end)\b/gi,
    bash: /\b(if|then|else|fi|for|in|do|done|case|esac|function|echo|export|sudo|cd|ls|cat|grep|find|npm|pnpm|yarn)\b/g,
    css: /(^|[{}\s;])(@media|@keyframes|@supports|@import)\b/gm
  }

  const typePatterns = {
    java: /\b(String|Integer|Long|Boolean|Double|Float|List|Map|Set|HashMap|ArrayList|Object|int|long|double|float|boolean|char|byte|short|void)\b/g,
    javascript: /\b(Array|Object|Promise|Map|Set|Date|RegExp|string|number|boolean|undefined)\b/g,
    typescript: /\b(Array|Object|Promise|Map|Set|Date|RegExp|string|number|boolean|undefined|unknown|never|any|void|Record|Partial|Pick|Omit)\b/g,
    python: /\b(str|int|float|bool|list|dict|tuple|set|object)\b/g,
    sql: /\b(varchar|char|text|int|bigint|decimal|datetime|timestamp|json)\b/gi
  }

  content = content
    .replace(keywordPatterns[language] || /$^/g, '<span class="token-keyword">$1</span>')
    .replace(typePatterns[language] || /$^/g, '<span class="token-type">$1</span>')
    .replace(language === 'css' ? /(^|[{}\s;])(@media|@keyframes|@supports|@import)\b/gm : /$^/g, '$1<span class="token-keyword">$2</span>')
    .replace(language === 'css' ? /(^|\n)(\s*[.#]?[\w-]+)(\s*\{)/g : /$^/g, '$1<span class="token-function">$2</span>$3')
    .replace(language === 'css' ? /([{\s;])([\w-]+)(\s*:)/g : /$^/g, '$1<span class="token-property">$2</span>$3')
    .replace(/@[\w$]+/g, '<span class="token-type">$&</span>')
    .replace(/\b(-?\d+(?:\.\d+)?)\b/g, '<span class="token-number">$1</span>')
    .replace(/\b([A-Za-z_$][\w$]*)(?=\s*\()/g, '<span class="token-function">$1</span>')
    .replace(/\.([A-Za-z_$][\w$]*)/g, '.<span class="token-property">$1</span>')

  placeholders.forEach((item) => {
    content = content.replace(item.key, item.value)
  })

  return content
}

/**
 * 标准化代码语言名称
 * 兼容常见别名写法，保证自动识别与手动切换使用同一套语言值
 */
function resolveCodeLanguage(language) {
  const rawLanguage = String(language || '')
    .replace(/^language-/i, '')
    .replace(/^lang-/i, '')
    .trim()
    .toLowerCase()

  if (!rawLanguage) {
    return ''
  }

  const languageMap = {
    auto: 'auto',
    java: 'java',
    js: 'javascript',
    jsx: 'javascript',
    javascript: 'javascript',
    ts: 'typescript',
    tsx: 'typescript',
    typescript: 'typescript',
    py: 'python',
    python: 'python',
    json: 'json',
    xml: 'xml',
    html: 'xml',
    vue: 'xml',
    sql: 'sql',
    sh: 'bash',
    shell: 'bash',
    bash: 'bash',
    css: 'css',
    scss: 'css',
    less: 'css',
    yml: 'yaml',
    yaml: 'yaml',
    text: 'text',
    plaintext: 'text',
    txt: 'text'
  }

  return languageMap[rawLanguage] || ''
}

/**
 * 目的：综合代码标签与代码内容，得到详情页代码块的初始语言。
 * 逻辑：优先消费编辑器透出的 language/class 标记，没有标记时再走内容识别兜底。
 */
function extractCodeLanguage(preAttrs, codeAttrs, rawCode) {
  const attrs = `${preAttrs || ''} ${codeAttrs || ''}`
  const classMatch = attrs.match(/class=(["'])(.*?)\1/i)
  const dataLanguageMatch = attrs.match(/data-language=(["'])(.*?)\1/i)
  const languageMatch = attrs.match(/language=(["'])(.*?)\1/i)
  const langMatch = attrs.match(/lang=(["'])(.*?)\1/i)
  const sourceLanguage = resolveCodeLanguage(
    dataLanguageMatch?.[2] || languageMatch?.[2] || langMatch?.[2] || classMatch?.[2] || attrs
  )

  return sourceLanguage || detectLanguage(rawCode, '')
}

/**
 * 统一语言按钮展示文案
 * 使用紧凑大写标签，让工具栏信息在深色代码块上更清晰
 */
function formatLanguageLabel(language) {
  const normalizedLanguage = resolveCodeLanguage(language) || 'text'
  return codeLanguageLabelMap[normalizedLanguage] || normalizedLanguage
}

/**
 * 生成语言下拉选项
 * 保持工具栏为单入口切换样式，贴近参考图中的语言选择体验
 */
function buildLanguageOptions(sourceLanguage) {
  return codeBlockLanguages.map((language) => {
    const label = language === 'auto'
      ? `自动识别（${formatLanguageLabel(sourceLanguage || 'text')}）`
      : formatLanguageLabel(language)
    return `<option value="${language}">${label}</option>`
  }).join('')
}

/**
 * 刷新语言下拉选项文案
 * 在自动识别和手动切换后同步提示当前语言，避免下拉内容与高亮状态脱节
 */
function updateLanguageOptions(selectElement, sourceLanguage, activeLanguage, detectedLanguage) {
  Array.from(selectElement.options).forEach((option) => {
    const optionLanguage = option.value || 'auto'
    option.textContent = optionLanguage === 'auto'
      ? `自动识别（${formatLanguageLabel(detectedLanguage || activeLanguage || sourceLanguage || 'text')}）`
      : formatLanguageLabel(optionLanguage)
  })
}

/**
 * 转义代码文本中的特殊字符
 * 保证源码插入富文本节点后按文本显示，不破坏详情页结构
 */
function encodeHtml(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/**
 * 还原代码文本中的实体字符
 * 支撑源码复制和语言切换时读取真实内容，避免重复转义
 */
function decodeHtml(value) {
  return String(value || '')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, '\'')
    .replace(/&amp;/g, '&')
}

/**
 * 转义代码属性值
 * 让原始代码安全保存在 data 属性里，便于后续切换语言时复用
 */
function encodeAttribute(value) {
  return encodeHtml(value).replace(/\r?\n/g, '&#10;')
}
</script>

<style scoped src="../styles/views/InterviewDetail.css"></style>
