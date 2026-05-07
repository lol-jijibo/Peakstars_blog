<template>
  <div class="article-page">
    <div class="article-progress-bar" :style="{ width: `${scrollProgress}%` }"></div>

    <header class="article-topbar">
      <button class="article-topbar-brand" type="button" @click="goHome">
        Peak<span>Depth</span>
      </button>

      <nav class="article-topbar-nav" aria-label="文章导航">
        <button type="button" @click="goArticleList">文章</button>
        <button type="button" @click="scrollToSeries">系列</button>
        <button type="button" @click="openCategory('frontend')">前端</button>
        <button type="button" @click="openCategory('backend')">后端</button>
      </nav>

      <button class="article-topbar-theme" type="button" :title="isDark ? '切换到浅色' : '切换到深色'" @click="toggleTheme">
        <span class="theme-icon">{{ isDark ? '☀' : '☾' }}</span>
      </button>
      <button class="article-topbar-action" type="button" @click="goArticleList">返回列表 →</button>
    </header>

    <div class="article-shell">
      <main class="article-main">
        <header class="article-header">
          <div class="article-meta-top">
            <span class="article-category-badge">{{ articleCategoryLabel }}</span>
            <span class="article-series">{{ articleSeries }}</span>
          </div>

          <h1 class="article-title">{{ currentArticle.title }}</h1>

          <p class="article-subtitle">{{ articleSubtitle }}</p>

          <div class="article-byline">
            <div class="article-author-avatar">{{ authorAvatarText }}</div>
            <div class="article-byline-text">
              <span class="article-author-name">{{ author.name }}</span>
              <span class="article-date-read">{{ displayPublishedAt }} · {{ displayReadTime }}</span>
            </div>

            <button class="article-author-follow-inline" type="button" @click="toggleFollow">
              {{ isFollowed ? '已关注' : '+ 关注' }}
            </button>

            <div class="article-stats">
              <div class="article-stat">
                <span class="article-stat-num">{{ displayReadCount }}</span>
                <span class="article-stat-label">阅读</span>
              </div>
              <div class="article-stat">
                <span class="article-stat-num">{{ displayLikeCount }}</span>
                <span class="article-stat-label">点赞</span>
              </div>
              <div class="article-stat">
                <span class="article-stat-num">{{ displayCommentCount }}</span>
                <span class="article-stat-label">评论</span>
              </div>
            </div>
          </div>
        </header>

        <article ref="articleBodyRef" class="article-body" v-html="articleHtml"></article>

        <div class="article-tags">
          <span v-for="tag in articleTags" :key="tag" class="article-tag"># {{ tag }}</span>
        </div>

        <section class="article-author-card">
          <div class="article-author-card-header">
            <div class="article-author-avatar-large">{{ authorAvatarText }}</div>
            <div>
              <div class="article-author-card-name">{{ author.name }}</div>
              <div class="article-author-card-role">{{ author.role }}</div>
            </div>
          </div>

          <p>{{ authorIntro }}</p>
        </section>

        <!-- 评论区 -->
        <section class="article-comments">
          <div class="article-comments-header">
            <h2>评论</h2>
            <span class="article-comments-count">{{ comments.length }} 条评论</span>
          </div>

          <!-- 评论输入框 -->
          <div class="article-comment-form">
            <div class="article-comment-form-avatar">{{ avatarInitials }}</div>
            <div class="article-comment-form-body">
              <textarea
                v-model="commentContent"
                class="article-comment-input"
                :placeholder="replyTo ? `回复 ${replyTo.nickname}...` : '写下你的评论...'"
                rows="3"
                @focus="commentFormFocused = true"
              ></textarea>
              <div v-if="commentFormFocused || commentContent" class="article-comment-form-actions">
                <button v-if="replyTo" class="article-comment-cancel-reply" type="button" @click="cancelReply">取消回复</button>
                <span class="article-comment-form-hint">支持 Markdown 粗体、代码</span>
                <button
                  class="article-comment-submit"
                  type="button"
                  :disabled="!commentContent.trim()"
                  @click="submitComment"
                >
                  {{ submitting ? '发布中...' : '发布评论' }}
                </button>
              </div>
            </div>
          </div>

          <!-- 评论列表 -->
          <div v-if="comments.length === 0" class="article-comments-empty">
            <p>暂无评论，来做第一个评论的人吧</p>
          </div>
          <div v-else class="article-comments-list">
            <div v-for="comment in topLevelComments" :key="comment.id" class="article-comment-item">
              <div class="article-comment-avatar" :style="{ background: comment.avatarAccent }">{{ comment.avatarText }}</div>
              <div class="article-comment-body">
                <div class="article-comment-meta">
                  <span class="article-comment-nickname">{{ comment.nickname }}</span>
                  <span class="article-comment-time">{{ formatCommentTime(comment.createdAt) }}</span>
                </div>
                <p class="article-comment-content">{{ comment.content }}</p>
                <div class="article-comment-actions">
                  <button type="button" class="article-comment-action-btn" @click="handleLikeComment(comment)">
                    <span>{{ comment.liked ? '❤️' : '🤍' }}</span>
                    <span>{{ comment.likeCount || 0 }}</span>
                  </button>
                  <button type="button" class="article-comment-action-btn" @click="handleReply(comment)">
                    💬 回复
                  </button>
                </div>
                <!-- 子评论 -->
                <div v-if="getChildComments(comment.id).length" class="article-comment-replies">
                  <div v-for="reply in getChildComments(comment.id)" :key="reply.id" class="article-comment-item article-comment-reply">
                    <div class="article-comment-avatar article-comment-avatar--small" :style="{ background: reply.avatarAccent }">{{ reply.avatarText }}</div>
                    <div class="article-comment-body">
                      <div class="article-comment-meta">
                        <span class="article-comment-nickname">{{ reply.nickname }}</span>
                        <span class="article-comment-time">{{ formatCommentTime(reply.createdAt) }}</span>
                      </div>
                      <p class="article-comment-content">{{ reply.content }}</p>
                      <div class="article-comment-actions">
                        <button type="button" class="article-comment-action-btn" @click="handleLikeComment(reply)">
                          <span>{{ reply.liked ? '❤️' : '🤍' }}</span>
                          <span>{{ reply.likeCount || 0 }}</span>
                        </button>
                        <button type="button" class="article-comment-action-btn" @click="handleReply(reply)">
                          💬 回复
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>

      <aside class="article-sidebar">
        <section v-if="outlineItems.length" class="article-sidebar-section">
          <h2 class="article-sidebar-title">目录</h2>
          <ul class="article-toc-list">
            <li
              v-for="item in outlineItems"
              :key="item.id"
              class="article-toc-item"
              :class="[
                { active: activeOutlineId === item.id },
                item.level
              ]"
            >
              <button type="button" @click="scrollToHeading(item.id)">{{ item.text }}</button>
            </li>
          </ul>
        </section>

        <section ref="seriesSectionRef" class="article-sidebar-section">
          <h2 class="article-sidebar-title">系列相关</h2>
          <button
            v-for="(item, index) in relatedArticles"
            :key="item.id"
            class="article-related-item"
            type="button"
            @click="openArticle(item.id)"
          >
            <span class="article-related-num">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="article-related-info">
              <span class="article-related-title">{{ item.title }}</span>
              <span class="article-related-meta">{{ item.meta }}</span>
            </span>
          </button>
        </section>

        <section class="article-sidebar-section article-sidebar-share">
          <h2 class="article-sidebar-title">分享</h2>
          <div class="article-share-list">
            <button class="article-share-button" type="button" @click="shareToX">
              <span class="article-share-icon article-share-icon--x">𝕏</span>
              分享到 X
            </button>
            <button class="article-share-button" type="button" @click="shareToLinkedIn">
              <span class="article-share-icon article-share-icon--in">in</span>
              分享到 LinkedIn
            </button>
            <button class="article-share-button" type="button" @click="copyLink">
              <span class="article-share-icon article-share-icon--copy">⎘</span>
              {{ copiedText }}
            </button>
          </div>
        </section>
      </aside>
    </div>

    <footer class="article-footer">
      <span class="article-footer-brand">Peak<span>Depth</span></span>
      <span class="article-footer-copy">© 2026 PeakDepth · 深度技术内容</span>
    </footer>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTechArticles, incrementArticleReadCount, getArticleComments, addArticleComment } from '@/api/content'
import { highlight, RULE_MAP } from '@/utils/codeHighlight'
import { useThemeStore } from '@/stores/theme'

const route = useRoute()
const router = useRouter()
const { isDark, toggleTheme } = useThemeStore()

const techArticles = ref([])
const article = ref(null)
const articleBodyRef = ref(null)
const seriesSectionRef = ref(null)
const outlineItems = ref([])
const activeOutlineId = ref('')
const scrollProgress = ref(0)
const copiedText = ref('复制链接')
const isFollowed = ref(false)
const comments = ref([])
const commentContent = ref('')
const commentFormFocused = ref(false)
const replyTo = ref(null)
const submitting = ref(false)

/**
 * 目的：统一详情页文章来源，仅从后端 MySQL 加载。
 * 逻辑：优先读取接口合并结果，找不到时尝试加载全部文章后再查找。
 */
async function loadArticle(articleId) {
  try {
    const list = await getTechArticles()
    techArticles.value = Array.isArray(list) ? list : []
    article.value = techArticles.value.find((item) => String(item.id) === String(articleId)) || null
  } catch {
    techArticles.value = []
    article.value = null
  }

  // 进入文章详情页时递增阅读量
  if (article.value?.id) {
    try {
      const result = await incrementArticleReadCount(article.value.id)
      if (result && result.readCount !== undefined) {
        article.value = { ...article.value, readCount: result.readCount }
      }
    } catch {
      // 阅读量递增失败不影响页面渲染
    }
  }

  // 加载评论列表
  if (article.value?.id) {
    try {
      comments.value = await getArticleComments(article.value.id)
    } catch {
      comments.value = []
    }
  }

  await nextTick()
  syncOutline()
  updateScrollState()
}

const currentArticle = computed(() => article.value || {})

const author = computed(() => ({
  name: currentArticle.value.author?.name || currentArticle.value.authorName || 'PeakDepth',
  role: currentArticle.value.author?.role || currentArticle.value.authorRole || '技术作者'
}))

const articleCategoryLabel = computed(
  () => currentArticle.value.categoryLabel || resolveCategoryLabel(currentArticle.value.category)
)

const articleSeries = computed(() => {
  if (currentArticle.value.series) {
    return currentArticle.value.series
  }

  return `${articleCategoryLabel.value}专题 · ${currentArticle.value.isVip ? '深度解读' : '工程实践'}`
})

const articleSubtitle = computed(() => {
  return (
    currentArticle.value.subtitle ||
    currentArticle.value.summary ||
    currentArticle.value.essence ||
    '围绕真实工程问题拆开背景、取舍与落地过程。'
  )
})

const articleHtml = computed(() => {
  if (currentArticle.value.contentHtml) {
    return currentArticle.value.contentHtml
  }

  return buildArticleHtml(currentArticle.value)
})

const articleTags = computed(() => {
  if (Array.isArray(currentArticle.value.tags) && currentArticle.value.tags.length) {
    return currentArticle.value.tags
  }

  const fallbackTags = [
    articleCategoryLabel.value,
    ...(currentArticle.value.highlights || [])
  ].filter(Boolean)

  return [...new Set(fallbackTags)].slice(0, 6)
})

const authorAvatarText = computed(() => {
  return currentArticle.value.author?.initials || buildInitials(author.value.name)
})

const authorIntro = computed(() => {
  return (
    currentArticle.value.authorIntro ||
    `${author.value.name} 长期关注 ${articleCategoryLabel.value} 场景，持续记录真实项目里的判断依据与工程取舍。`
  )
})

const displayPublishedAt = computed(() => formatLongDate(currentArticle.value.publishedAt))
const displayReadTime = computed(() => formatReadTime(currentArticle.value.readTime))
const displayReadCount = computed(() => formatCompactCount(currentArticle.value.readCount || 0))
const displayLikeCount = computed(() => formatCompactCount(currentArticle.value.likeCount || 0))
const displayCommentCount = computed(() => formatCompactCount(currentArticle.value.commentCount || 0))

const avatarInitials = computed(() => buildInitials('匿名'))

const topLevelComments = computed(() => comments.value.filter((c) => !c.parentId))

function getChildComments(parentId) {
  return comments.value.filter((c) => Number(c.parentId) === Number(parentId))
}

function formatCommentTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days} 天前`
  return date.toLocaleDateString('zh-CN')
}

function handleReply(comment) {
  replyTo.value = comment
  commentFormFocused.value = true
}

function cancelReply() {
  replyTo.value = null
}

async function submitComment() {
  const content = commentContent.value.trim()
  if (!content || submitting.value) return

  submitting.value = true
  try {
    const data = {
      nickname: '匿名用户',
      content,
      avatarText: avatarInitials.value,
      avatarAccent: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      parentId: replyTo.value ? Number(replyTo.value.id) : null
    }
    const newComment = await addArticleComment(currentArticle.value.id, data)
    if (newComment && newComment.id) {
      comments.value.push({ ...newComment, likeCount: 0, liked: false })
    } else {
      await loadComments()
    }
    commentContent.value = ''
    replyTo.value = null
    commentFormFocused.value = false
    // 更新文章评论数
    if (currentArticle.value) {
      currentArticle.value = { ...currentArticle.value, commentCount: (currentArticle.value.commentCount || 0) + 1 }
    }
  } catch {
    // 评论发布失败，保留输入内容
  } finally {
    submitting.value = false
  }
}

async function loadComments() {
  if (!currentArticle.value?.id) return
  try {
    comments.value = await getArticleComments(currentArticle.value.id)
  } catch {
    comments.value = []
  }
}

function handleLikeComment(comment) {
  comment.liked = !comment.liked
  comment.likeCount = (comment.likeCount || 0) + (comment.liked ? 1 : -1)
}

function toggleFollow() {
  isFollowed.value = !isFollowed.value
}

const relatedArticles = computed(() => {
  const currentId = String(currentArticle.value.id)
  const category = currentArticle.value.category

  return techArticles.value
    .filter((item) => String(item.id) !== currentId)
    .sort((left, right) => {
      const leftScore = Number(left.category === category) + Number(left.featured)
      const rightScore = Number(right.category === category) + Number(right.featured)
      return rightScore - leftScore
    })
    .slice(0, 3)
    .map((item) => ({
      id: item.id,
      title: item.title,
      meta: `${formatReadTime(item.readTime)} · ${formatCompactCount(item.readCount || 0)} 阅读`
    }))
})

watch(
  () => route.params.id,
  (articleId) => {
    loadArticle(articleId)
  },
  { immediate: true }
)

watch(articleHtml, async () => {
  await nextTick()
  processCodeBlocks()
  syncOutline()
})

onMounted(() => {
  document.body.classList.add('article-page')
  window.addEventListener('scroll', handleWindowScroll, { passive: true })
  updateScrollState()
})

onBeforeUnmount(() => {
  document.body.classList.remove('article-page')
  window.removeEventListener('scroll', handleWindowScroll)
  document.removeEventListener('click', handleCodeBlockClick)
})

/**
 * 目的：根据文章正文标题生成右侧目录。
 * 逻辑：扫描正文中的 h1、h2 与 h3，写入稳定 id 后同步用于目录跳转与滚动高亮。
 */
function syncOutline() {
  const root = articleBodyRef.value
  if (!root) {
    outlineItems.value = []
    activeOutlineId.value = ''
    return
  }

  const headings = [...root.querySelectorAll('h1, h2, h3')]
  outlineItems.value = headings.map((heading, index) => {
    const id = `article-outline-${index}`
    heading.id = id

    return {
      id,
      text: heading.textContent?.trim() || `章节 ${index + 1}`,
      level: heading.tagName.toLowerCase()
    }
  })
}

/* ===== 代码块增强：语言检测 / 复制按钮 / 语言切换下拉 / 语法高亮 ===== */

const LANG_LIST = Object.keys(RULE_MAP)

const LANG_ICONS = {
  JavaScript: 'JS', TypeScript: 'TS', Python: 'PY', Java: 'JV',
  C: 'C', 'C++': 'C+', 'C#': 'C#', Go: 'GO', Rust: 'RS',
  PHP: 'PHP', Ruby: 'RB', Swift: 'SW', Kotlin: 'KT', SQL: 'SQL',
  HTML: 'HT', CSS: 'CS', Shell: 'SH', YAML: 'YM', JSON: 'JS',
  XML: 'XM', Markdown: 'MD', 'Plain Text': 'TX'
}

function detectLanguage(code) {
  const text = String(code || '').trim()
  if (!text) return 'Plain Text'

  // HTML
  if (/<\/?[a-z][\s\S]*>/i.test(text) && /<\/\w+>/.test(text)) return 'HTML'
  // CSS
  if (/\{[\s\S]*?:[^:]+;[\s\S]*?\}/.test(text) && /@media|@import|@keyframes|#\w+|\.\w+\s*\{/.test(text)) return 'CSS'
  // SQL
  if (/^\s*(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|WITH)\b/i.test(text)) return 'SQL'
  // Python
  if (/^\s*(import |from |def |class |if __name__|print\(|elif |async def )/.test(text) && !/[{;]/.test(text.split('\n')[0])) return 'Python'
  // Java (not JavaScript)
  if (/^\s*(package |import java\.|public class |public static void main|System\.out\.)/.test(text)) return 'Java'
  // Go
  if (/^\s*(package |func |import \(|func main\(\)|:=|fmt\.Print)/.test(text)) return 'Go'
  // Rust
  if (/^\s*(fn |let mut |impl |pub fn |use std::|match |println!)/.test(text)) return 'Rust'
  // TypeScript
  if (/(interface\s+\w+|:\s*(string|number|boolean|void|any)\b|<\w+>|as\s+\w+|import type)/.test(text) && /=>|const|let/.test(text)) return 'TypeScript'
  // JavaScript
  if (/^\s*(const |let |var |function |import |export |=>|async |await |require\()/.test(text)) return 'JavaScript'
  // C / C++
  if (/#include\s*[<"]/.test(text)) return /std::|cout|cin|class\s+\w+\s*\{|template\s*</.test(text) ? 'C++' : 'C'
  // C#
  if (/^\s*(using |namespace |public class|Console\.Write|var\s+\w+\s*=)/.test(text)) return 'C#'
  // PHP
  if (/<\?php|^\s*\$\w+/.test(text)) return 'PHP'
  // Ruby
  if (/^\s*(def |puts |require |module |class |end$|attr_)/.test(text)) return 'Ruby'
  // Swift
  if (/^\s*(import |var |let |func |guard |print\(|struct |enum |protocol )/.test(text) && /: /.test(text)) return 'Swift'
  // Kotlin
  if (/^\s*(fun |val |var |data class |object |companion|suspend fun)/.test(text)) return 'Kotlin'
  // Shell
  if (/^#!\/bin\/(bash|sh|zsh)|^\s*(echo |cd |mkdir |rm |curl |wget |chmod |export )/.test(text)) return 'Shell'
  // YAML
  if (/^\s*\w+:\s*$/m.test(text) && /^\s+[\w-]+:/m.test(text) && !/[{;]/.test(text)) return 'YAML'
  // JSON
  try { JSON.parse(text); return 'JSON' } catch {}
  // XML
  if (/^<\?xml/.test(text)) return 'XML'
  // Markdown
  if (/^#{1,6}\s|^\*{3,}$|^\[.*\]\(.*\)/m.test(text)) return 'Markdown'

  return 'Plain Text'
}

function processCodeBlocks() {
  const root = articleBodyRef.value
  if (!root) return

  // Process <pre> elements that are not already inside .code-block
  const pres = [...root.querySelectorAll('pre')].filter(
    (pre) => !pre.closest('.code-block')
  )

  pres.forEach((pre) => {
    const codeEl = pre.querySelector('code') || pre
    const rawCode = codeEl.textContent || ''
    const detectedLang = detectLanguage(rawCode)

    let hintedLang = ''
    if (codeEl.className) {
      const match = codeEl.className.match(/language-(\w+)/)
      if (match) {
        const label = match[1].charAt(0).toUpperCase() + match[1].slice(1)
        if (LANG_LIST.includes(label)) hintedLang = label
      }
    }

    const activeLang = hintedLang || detectedLang

    // Build wrapper
    const wrapper = document.createElement('div')
    wrapper.className = 'code-block'
    wrapper.setAttribute('data-lang', activeLang)
    // 存储原始代码，用于切换语言时重新高亮
    wrapper.setAttribute('data-raw', rawCode)

    // Build header
    const header = document.createElement('div')
    header.className = 'code-header'
    header.innerHTML = `
      <div class="code-header-left">
        <span class="code-dots">
          <span class="dot dot-r"></span>
          <span class="dot dot-y"></span>
          <span class="dot dot-g"></span>
        </span>
      </div>
      <div class="code-header-right">
        <div class="code-lang-switcher">
          <button class="code-lang-btn" type="button" title="切换语言">
            <span class="code-lang-icon">${LANG_ICONS[activeLang] || activeLang.slice(0, 2).toUpperCase()}</span>
          </button>
          <div class="code-lang-dropdown">
            ${LANG_LIST.map((lang) =>
              `<button class="code-lang-option${lang === activeLang ? ' active' : ''}" type="button" data-lang="${lang}">${lang}</button>`
            ).join('')}
          </div>
        </div>
        <button class="code-copy-btn" type="button" title="复制代码">
          <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
          <span class="code-copy-text">复制</span>
        </button>
      </div>
    `

    // Apply syntax highlighting
    const highlightedHtml = highlight(rawCode, activeLang)

    const preClone = pre.cloneNode(false)
    const codeClone = codeEl.cloneNode(false)
    codeClone.innerHTML = highlightedHtml
    preClone.appendChild(codeClone)

    wrapper.appendChild(header)
    wrapper.appendChild(preClone)
    pre.replaceWith(wrapper)
  })

  // Also handle existing .code-block elements (add copy + lang switcher if missing)
  ;[...root.querySelectorAll('.code-block')].forEach((block) => {
    if (block.querySelector('.code-copy-btn')) return

    const header = block.querySelector('.code-header')
    if (!header) return

    const preEl = block.querySelector('pre')
    const codeEl = preEl?.querySelector('code') || preEl
    const rawCode = codeEl?.textContent || ''

    // 存储原始代码
    block.setAttribute('data-raw', rawCode)

    const existingLang = block.querySelector('.code-lang')
    const langText = existingLang?.textContent?.trim() || block.getAttribute('data-lang') || ''
    const activeLang = LANG_LIST.find((l) => l.toLowerCase() === langText.toLowerCase()) || detectLanguage(rawCode)

    block.setAttribute('data-lang', activeLang)

    const rightArea = document.createElement('div')
    rightArea.className = 'code-header-right'

    const switcher = document.createElement('div')
    switcher.className = 'code-lang-switcher'
    switcher.innerHTML = `
      <button class="code-lang-btn" type="button" title="切换语言">
        <span class="code-lang-icon">${LANG_ICONS[activeLang] || activeLang.slice(0, 2).toUpperCase()}</span>
      </button>
      <div class="code-lang-dropdown">
        ${LANG_LIST.map((lang) =>
          `<button class="code-lang-option${lang === activeLang ? ' active' : ''}" type="button" data-lang="${lang}">${lang}</button>`
        ).join('')}
      </div>
    `

    const copyBtn = document.createElement('button')
    copyBtn.className = 'code-copy-btn'
    copyBtn.type = 'button'
    copyBtn.title = '复制代码'
    copyBtn.innerHTML = `
      <svg class="code-copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
      <span class="code-copy-text">复制</span>
    `

    rightArea.appendChild(switcher)
    rightArea.appendChild(copyBtn)

    if (existingLang) existingLang.remove()

    const oldDots = header.querySelector('.code-dots')
    if (oldDots) {
      const leftArea = document.createElement('div')
      leftArea.className = 'code-header-left'
      leftArea.appendChild(oldDots)
      header.insertBefore(leftArea, header.firstChild)
    }

    header.appendChild(rightArea)

    // Apply highlighting
    const highlightedHtml = highlight(rawCode, activeLang)
    if (codeEl) codeEl.innerHTML = highlightedHtml
  })

  document.addEventListener('click', handleCodeBlockClick)
}

/**
 * 对指定代码块按语言重新高亮
 */
function rehighlightBlock(block, lang) {
  const rawCode = block.getAttribute('data-raw') || ''
  const codeEl = block.querySelector('pre code') || block.querySelector('pre')
  if (!codeEl || !rawCode) return

  codeEl.innerHTML = highlight(rawCode, lang)
}

function handleCodeBlockClick(e) {
  const target = e.target

  // Copy button
  const copyBtn = target.closest('.code-copy-btn')
  if (copyBtn) {
    const block = copyBtn.closest('.code-block')
    const rawCode = block?.getAttribute('data-raw') || block?.querySelector('pre code')?.textContent || block?.querySelector('pre')?.textContent || ''
    navigator.clipboard.writeText(rawCode).then(() => {
      const textSpan = copyBtn.querySelector('.code-copy-text')
      if (textSpan) textSpan.textContent = '已复制'
      copyBtn.classList.add('copied')
      setTimeout(() => {
        if (textSpan) textSpan.textContent = '复制'
        copyBtn.classList.remove('copied')
      }, 1800)
    }).catch(() => {
      const textSpan = copyBtn.querySelector('.code-copy-text')
      if (textSpan) textSpan.textContent = '失败'
      setTimeout(() => {
        if (textSpan) textSpan.textContent = '复制'
      }, 1800)
    })
    return
  }

  // Language toggle button
  const langBtn = target.closest('.code-lang-btn')
  if (langBtn) {
    const switcher = langBtn.closest('.code-lang-switcher')
    const dropdown = switcher?.querySelector('.code-lang-dropdown')
    if (dropdown) {
      document.querySelectorAll('.code-lang-dropdown.open').forEach((d) => {
        if (d !== dropdown) d.classList.remove('open')
      })
      dropdown.classList.toggle('open')
    }
    return
  }

  // Language option
  const langOption = target.closest('.code-lang-option')
  if (langOption) {
    const switcher = langOption.closest('.code-lang-switcher')
    const block = langOption.closest('.code-block')
    const selectedLang = langOption.getAttribute('data-lang')

    if (switcher && block && selectedLang) {
      block.setAttribute('data-lang', selectedLang)

      const icon = switcher.querySelector('.code-lang-icon')
      if (icon) icon.textContent = LANG_ICONS[selectedLang] || selectedLang.slice(0, 2).toUpperCase()

      switcher.querySelectorAll('.code-lang-option').forEach((opt) => {
        opt.classList.toggle('active', opt.getAttribute('data-lang') === selectedLang)
      })

      const dropdown = switcher.querySelector('.code-lang-dropdown')
      if (dropdown) dropdown.classList.remove('open')

      // 切换语言后重新高亮
      rehighlightBlock(block, selectedLang)
    }
    return
  }

  // Close dropdown when clicking outside
  document.querySelectorAll('.code-lang-dropdown.open').forEach((d) => {
    d.classList.remove('open')
  })
}

/**
 * 目的：驱动顶部阅读进度条与目录激活状态。
 * 逻辑：根据滚动位置计算正文阅读进度，并选择最靠近视口顶部的目录项作为当前章节。
 */
function updateScrollState() {
  const total = document.documentElement.scrollHeight - window.innerHeight
  scrollProgress.value = total > 0 ? Math.min((window.scrollY / total) * 100, 100) : 0

  let activeId = outlineItems.value[0]?.id || ''
  outlineItems.value.forEach((item) => {
    const target = document.getElementById(item.id)
    if (target && target.getBoundingClientRect().top <= 140) {
      activeId = item.id
    }
  })

  activeOutlineId.value = activeId
}

function handleWindowScroll() {
  updateScrollState()
}

function goHome() {
  router.push('/home')
}

function goArticleList() {
  router.push('/articles')
}

function openCategory(category) {
  router.push({ path: '/articles', query: { category } })
}

function openArticle(articleId) {
  router.push(`/articles/${articleId}`)
}

function scrollToSeries() {
  seriesSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function scrollToHeading(id) {
  const target = document.getElementById(id)
  if (!target) {
    return
  }

  target.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    copiedText.value = '链接已复制'
    window.setTimeout(() => {
      copiedText.value = '复制链接'
    }, 1600)
  } catch {
    copiedText.value = '复制失败'
    window.setTimeout(() => {
      copiedText.value = '复制链接'
    }, 1600)
  }
}

function shareToX() {
  const url = encodeURIComponent(window.location.href)
  const title = encodeURIComponent(currentArticle.value.title)
  window.open(`https://x.com/intent/tweet?text=${title}&url=${url}`, '_blank', 'noopener,noreferrer')
}

function shareToLinkedIn() {
  const url = encodeURIComponent(window.location.href)
  window.open(`https://www.linkedin.com/sharing/share-offsite/?url=${url}`, '_blank', 'noopener,noreferrer')
}

function resolveCategoryLabel(category) {
  const labelMap = {
    frontend: '前端',
    backend: '后端',
    all: '全部文章',
    history: '历史',
    collect: '收藏',
    like: '喜欢'
  }

  return labelMap[category] || '技术文章'
}

function formatLongDate(value) {
  if (!value) {
    return '2026年4月28日'
  }

  const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!match) {
    return value
  }

  return `${Number(match[1])}年${Number(match[2])}月${Number(match[3])}日`
}

function formatReadTime(value) {
  const text = String(value || '').trim()
  const minuteMatch = text.match(/(\d+)/)

  if (!minuteMatch) {
    return '8 分钟阅读'
  }

  return `${minuteMatch[1]} 分钟阅读`
}

function formatCompactCount(value) {
  const count = Number(value) || 0

  if (count >= 10000) {
    return `${(count / 1000).toFixed(1).replace('.0', '')}k`
  }

  if (count >= 1000) {
    return `${(count / 1000).toFixed(1).replace('.0', '')}k`
  }

  return `${count}`
}

function buildInitials(name) {
  const text = String(name || '').trim()
  if (!text) {
    return 'PK'
  }

  return text.length <= 2 ? text : text.slice(0, 2)
}

function escapeHtml(value) {
  return String(value || '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function escapeAttribute(value) {
  return escapeHtml(value)
}

/**
 * 目的：为缺少富文本正文的文章生成统一详情页内容。
 * 逻辑：组合摘要、亮点、配图和结论段落，让其他文章也能套用同一套杂志式排版。
 */
function buildArticleHtml(targetArticle) {
  const summary = escapeHtml(targetArticle.summary || '')
  const essence = escapeHtml(targetArticle.essence || targetArticle.summary || '')
  const title = escapeHtml(targetArticle.title || '')
  const coverUrl = escapeAttribute(targetArticle.coverUrl || '')
  const categoryLabel = resolveCategoryLabel(targetArticle.category)
  const highlightItems = (targetArticle.highlights || []).filter(Boolean)

  const highlightsHtml = highlightItems.length
    ? `
      <div class="callout callout-info">
        <span class="callout-icon">📌</span>
        <p>${highlightItems
          .map((item, index) => `<strong>重点 ${index + 1}：</strong>${escapeHtml(item)}`)
          .join('<br>')}</p>
      </div>
    `
    : ''

  const imageHtml = coverUrl
    ? `
      <figure class="article-image">
        <img src="${coverUrl}" alt="${title}" loading="lazy" />
        <figcaption class="image-caption">${title}</figcaption>
      </figure>
    `
    : ''

  return `
    <p class="lead">${essence || summary || '这是一篇围绕真实工程问题展开的技术文章。'}</p>
    <p>${summary || essence || '文章从背景、方案与取舍出发，帮助你快速建立对问题的整体认识。'}</p>
    <h2>这篇文章讲什么</h2>
    <p>这篇内容围绕 ${escapeHtml(categoryLabel)} 场景展开，不是只给结论，而是把问题出现的原因、方案的边界以及落地时的注意点一起交代清楚。</p>
    ${highlightsHtml}
    <h2>核心关注点</h2>
    <p>${essence || '文章会围绕系统设计、实现细节和后续演进三个方向，帮助你快速抓住真正值得关注的部分。'}</p>
    ${imageHtml}
    <h3>为什么值得细看</h3>
    <p>相比只罗列知识点的总结，这类工程文章更有价值的地方在于，它会把实践中的取舍路径一并讲明白，让你知道为什么这样做，而不是只知道怎么做。</p>
    <blockquote>
      <p>"真正有帮助的技术写作，不是把知识摆出来，而是把判断过程交给读者。"</p>
      <cite>— PeakDepth 技术编辑部</cite>
    </blockquote>
    <h2>适合谁读</h2>
    <p>如果你正在处理 ${escapeHtml(categoryLabel)} 相关的项目，或者希望快速建立这类问题的工程判断框架，这篇文章会比较适合你。</p>
  `
}
</script>

<style scoped src="../styles/views/TechArticleDetail.css"></style>
