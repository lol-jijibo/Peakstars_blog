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
        <button type="button" @click="openCategory('backend')">后端</button>
      </nav>

      <button class="article-topbar-theme" type="button" :title="isDark ? '切换到浅色' : '切换到深色'" @click="toggleTheme">
        <span class="theme-icon">{{ isDark ? '☀' : '☾' }}</span>
      </button>
      <button class="article-topbar-action" type="button" @click="goArticleList">返回列表 →</button>
    </header>

    <div v-if="isArticleReady" class="article-shell">
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
              <span class="article-date-read">
                <span>{{ displayPublishedAt }}</span>
                <span class="article-date-read-sep">·</span>
                <span class="article-read-history">{{ displayReadTime }}</span>
              </span>
            </div>

            <button class="article-author-follow-inline" type="button" @click="toggleFollow">
              {{ isFollowed ? '已关注' : '+ 关注' }}
            </button>

            <button
              class="article-like-inline"
              :class="{ 'is-liked': currentArticle.isLiked }"
              type="button"
              :disabled="likeSubmitting"
              @click="handleToggleArticleLike"
            >
              <span class="article-like-icon">{{ currentArticle.isLiked ? '♥' : '♡' }}</span>
              <span>{{ currentArticle.isLiked ? '已点赞' : '点赞' }}</span>
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
          <span v-for="tag in articleTags.filter(t => t !== '前端')" :key="tag" class="article-tag"># {{ tag }}</span>
        </div>

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
                  <button type="button" class="article-comment-action-btn article-comment-like-btn" @click="handleLikeComment(comment)">
                    <svg class="comment-icon comment-icon-like" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M3.75 10.75h3.5v8.5h-3.5a1 1 0 0 1-1-1v-6.5a1 1 0 0 1 1-1Z"/>
                      <path d="M9 19.25h6.73a1.75 1.75 0 0 0 1.72-1.43l1.11-5.75a1.75 1.75 0 0 0-1.72-2.07H12.5V5.95A2.2 2.2 0 0 0 10.3 3.75L8.56 9.1A2.5 2.5 0 0 1 8 10.03l-.75.83v5.89A2.5 2.5 0 0 0 9 19.25Z"/>
                    </svg>
                    <span>{{ comment.likeCount || 0 }}</span>
                  </button>
                  <button v-if="getChildComments(comment.id).length" type="button" class="article-comment-action-btn article-comment-reply-btn" @click="handleReply(comment)">
                    <svg class="comment-icon comment-icon-reply" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M7 18.5 3.75 20.5V6.75A1.75 1.75 0 0 1 5.5 5h13a1.75 1.75 0 0 1 1.75 1.75v9a1.75 1.75 0 0 1-1.75 1.75H7Z"/>
                      <circle cx="9.25" cy="11.5" r="0.8" fill="currentColor" stroke="none"/>
                      <circle cx="12" cy="11.5" r="0.8" fill="currentColor" stroke="none"/>
                      <circle cx="14.75" cy="11.5" r="0.8" fill="currentColor" stroke="none"/>
                    </svg>
                    <span>{{ getChildComments(comment.id).length }}</span>
                  </button>
                  <button v-else type="button" class="article-comment-action-btn article-comment-reply-btn" @click="handleReply(comment)">
                    <svg class="comment-icon comment-icon-reply" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M7 18.5 3.75 20.5V6.75A1.75 1.75 0 0 1 5.5 5h13a1.75 1.75 0 0 1 1.75 1.75v9a1.75 1.75 0 0 1-1.75 1.75H7Z"/>
                      <circle cx="9.25" cy="11.5" r="0.8" fill="currentColor" stroke="none"/>
                      <circle cx="12" cy="11.5" r="0.8" fill="currentColor" stroke="none"/>
                      <circle cx="14.75" cy="11.5" r="0.8" fill="currentColor" stroke="none"/>
                    </svg>
                    <span>评论</span>
                  </button>
                  <button v-if="myCommentIds.has(String(comment.id))" type="button" class="article-comment-action-btn article-comment-delete-btn" @click="handleDeleteComment(comment)">
                    🗑 删除
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
                        <button type="button" class="article-comment-action-btn article-comment-like-btn" @click="handleLikeComment(reply)">
                          <svg class="comment-icon comment-icon-like" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M3.75 10.75h3.5v8.5h-3.5a1 1 0 0 1-1-1v-6.5a1 1 0 0 1 1-1Z"/>
                            <path d="M9 19.25h6.73a1.75 1.75 0 0 0 1.72-1.43l1.11-5.75a1.75 1.75 0 0 0-1.72-2.07H12.5V5.95A2.2 2.2 0 0 0 10.3 3.75L8.56 9.1A2.5 2.5 0 0 1 8 10.03l-.75.83v5.89A2.5 2.5 0 0 0 9 19.25Z"/>
                          </svg>
                          <span>{{ reply.likeCount || 0 }}</span>
                        </button>
                        <button type="button" class="article-comment-action-btn article-comment-reply-btn" @click="handleReply(reply)">
                          <svg class="comment-icon comment-icon-reply" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M7 18.5 3.75 20.5V6.75A1.75 1.75 0 0 1 5.5 5h13a1.75 1.75 0 0 1 1.75 1.75v9a1.75 1.75 0 0 1-1.75 1.75H7Z"/>
                            <circle cx="9.25" cy="11.5" r="0.8" fill="currentColor" stroke="none"/>
                            <circle cx="12" cy="11.5" r="0.8" fill="currentColor" stroke="none"/>
                            <circle cx="14.75" cy="11.5" r="0.8" fill="currentColor" stroke="none"/>
                          </svg>
                          <span>评论</span>
                        </button>
                        <button v-if="myCommentIds.has(String(reply.id))" type="button" class="article-comment-action-btn article-comment-delete-btn" @click="handleDeleteComment(reply)">
                          🗑 删除
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
        <section class="article-bottom-share">
          <h2 class="article-sidebar-title">分享</h2>
          <div class="article-share-list">
            <button class="article-share-button" type="button" @click="shareToX">
              <span class="article-share-icon article-share-icon--x">X</span>
              分享到 X
            </button>
            <button class="article-share-button" type="button" @click="shareToLinkedIn">
              <span class="article-share-icon article-share-icon--in">in</span>
              分享到 LinkedIn
            </button>
            <button class="article-share-button" type="button" @click="copyLink">
              <span class="article-share-icon article-share-icon--copy">↗</span>
              {{ copiedText }}
            </button>
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

        <section
          class="article-sidebar-section article-sidebar-share"
        >
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

    <!-- Toast 通知 -->
    <div v-else class="article-shell article-shell--loading">
      <main class="article-main">
        <div v-if="isArticleLoading" class="article-loading-panel" aria-label="文章加载中">
          <div class="article-loading-kicker"></div>
          <div class="article-loading-title"></div>
          <div class="article-loading-subtitle"></div>
          <div class="article-loading-meta"></div>
          <div class="article-loading-line"></div>
          <div class="article-loading-line article-loading-line--short"></div>
          <div class="article-loading-block"></div>
        </div>
        <div v-else class="article-empty-panel">
          <h1>{{ articleLoadFailed ? '文章加载失败' : '文章暂未找到' }}</h1>
          <button type="button" @click="goArticleList">返回文章列表</button>
        </div>
      </main>
    </div>

    <Transition name="toast">
      <div v-if="toastVisible" class="toast-notification">{{ toastMessage }}</div>
    </Transition>

    <!-- 自定义确认弹窗 -->
    <div v-if="confirmVisible" class="confirm-overlay" @click.self="cancelConfirm">
      <div class="confirm-dialog">
        <div class="confirm-title">{{ confirmTitle }}</div>
        <div class="confirm-body">{{ confirmMessage }}</div>
        <div class="confirm-actions">
          <button class="confirm-btn confirm-btn-cancel" type="button" @click="cancelConfirm">取消</button>
          <button class="confirm-btn confirm-btn-danger" type="button" @click="resolveConfirm">确认删除</button>
        </div>
      </div>
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
import { getTechArticles, incrementArticleReadCount, toggleArticleLike, getArticleComments, addArticleComment, deleteArticleComment, invalidateTechArticlesCache } from '@/api/content'
import { highlight, RULE_MAP } from '@/utils/codeHighlight'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const { isDark, toggleTheme } = useThemeStore()
const authStore = useAuthStore()

const techArticles = ref([])
const article = ref(null)
const isArticleLoading = ref(false)
const articleLoadFailed = ref(false)
const articleBodyRef = ref(null)
const seriesSectionRef = ref(null)
const outlineItems = ref([])
const activeOutlineId = ref('')
const scrollProgress = ref(0)
const copiedText = ref('复制链接')
const isFollowed = ref(false)
const likeSubmitting = ref(false)
const comments = ref([])
const commentContent = ref('')
const commentFormFocused = ref(false)
const replyTo = ref(null)
const submitting = ref(false)
const myCommentIds = ref(new Set(JSON.parse(localStorage.getItem('myCommentIds') || '[]')))
const confirmVisible = ref(false)
const confirmTitle = ref('')
const confirmMessage = ref('')
let confirmResolver = null
let articleLoadToken = 0
const LOCAL_TECH_ARTICLE_READ_HISTORY_KEY = 'peakstars_tech_article_read_history'

function showConfirm(title, message) {
  return new Promise((resolve) => {
    confirmTitle.value = title
    confirmMessage.value = message
    confirmVisible.value = true
    confirmResolver = resolve
  })
}

function resolveConfirm() {
  confirmVisible.value = false
  if (confirmResolver) confirmResolver(true)
}

function cancelConfirm() {
  confirmVisible.value = false
  if (confirmResolver) confirmResolver(false)
}

const toastVisible = ref(false)
const toastMessage = ref('')
let toastTimer = null

function showToast(message, duration = 2000) {
  toastMessage.value = message
  toastVisible.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastVisible.value = false
  }, duration)
}

/**
 * 目的：统一详情页文章来源，仅从后端 MySQL 加载。
 * 逻辑：优先读取接口合并结果，找不到时尝试加载全部文章后再查找。
 */
async function loadArticle(articleId) {
  const loadToken = ++articleLoadToken
  isArticleLoading.value = true
  articleLoadFailed.value = false
  article.value = null
  comments.value = []
  outlineItems.value = []
  activeOutlineId.value = ''

  try {
    const localLastReadAt = markLocalArticleReadTime(articleId)
    const list = await getTechArticles({ forceRefresh: true })
    if (loadToken !== articleLoadToken) return

    techArticles.value = Array.isArray(list) ? list : []
    const matchedArticle = techArticles.value.find((item) => String(item.id) === String(articleId)) || null
    article.value = matchedArticle ? { ...matchedArticle, lastReadAt: matchedArticle.lastReadAt || localLastReadAt } : null
  } catch {
    if (loadToken !== articleLoadToken) return

    techArticles.value = []
    article.value = null
    articleLoadFailed.value = true
  } finally {
    if (loadToken === articleLoadToken) {
      isArticleLoading.value = false
    }
  }

  // 进入文章详情页时递增阅读量，同时标记浏览历史
  if (article.value?.id) {
    try {
      const result = await incrementArticleReadCount(article.value.id)
      if (loadToken !== articleLoadToken) return

      if (result && result.readCount !== undefined) {
        article.value = { ...article.value, readCount: result.readCount }
      }
      // 清空文章列表缓存，确保返回列表页时 inHistory 状态刷新
      invalidateTechArticlesCache()
    } catch {
      // 阅读量递增失败不影响页面渲染
    }
  }

  // 加载评论列表
  if (article.value?.id) {
    try {
      const nextComments = await getArticleComments(article.value.id)
      if (loadToken !== articleLoadToken) return

      comments.value = nextComments
    } catch {
      if (loadToken !== articleLoadToken) return

      comments.value = []
    }
  }

  if (loadToken !== articleLoadToken) return

  await nextTick()
  syncOutline()
  updateScrollState()
}

function markLocalArticleReadTime(articleId) {
  const articleKey = String(articleId || '').trim()
  if (!articleKey) {
    return
  }

  const historyMap = readLocalArticleReadHistory()
  const readAt = new Date().toISOString()
  historyMap[articleKey] = readAt
  localStorage.setItem(LOCAL_TECH_ARTICLE_READ_HISTORY_KEY, JSON.stringify(historyMap))
  return readAt
}

function readLocalArticleReadHistory() {
  try {
    const parsed = JSON.parse(localStorage.getItem(LOCAL_TECH_ARTICLE_READ_HISTORY_KEY) || '{}')
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

const currentArticle = computed(() => article.value || {})
const isArticleReady = computed(() => !isArticleLoading.value && Boolean(article.value?.id))

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
const displayReadTime = computed(() => {
  return currentArticle.value.lastReadAt
    ? formatHistoryTime(currentArticle.value.lastReadAt)
    : formatReadTime(currentArticle.value.readTime)
})
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
      myCommentIds.value.add(String(newComment.id))
      localStorage.setItem('myCommentIds', JSON.stringify([...myCommentIds.value]))
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
    // 清空文章列表缓存，确保返回列表页时评论数刷新
    invalidateTechArticlesCache()
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

async function handleDeleteComment(comment) {
  const confirmed = await showConfirm('删除评论', '确定要删除这条评论吗？删除后将无法恢复。')
  if (!confirmed) return
  try {
    const result = await deleteArticleComment(comment.id)
    if (result && result.deleted) {
      comments.value = comments.value.filter((c) => String(c.id) !== String(comment.id))
      myCommentIds.value.delete(String(comment.id))
      localStorage.setItem('myCommentIds', JSON.stringify([...myCommentIds.value]))
      if (currentArticle.value) {
        currentArticle.value = { ...currentArticle.value, commentCount: Math.max((currentArticle.value.commentCount || 0) - 1, 0) }
      }
      showToast('✅️ 删除成功')
      // 清空文章列表缓存，确保返回列表页时评论数刷新
      invalidateTechArticlesCache()
    }
  } catch {
    // 删除失败，不额外处理
  }
}

function toggleFollow() {
  isFollowed.value = !isFollowed.value
}

async function handleToggleArticleLike() {
  if (!currentArticle.value?.id || likeSubmitting.value) {
    return
  }

  if (!authStore.isLoggedIn.value) {
    showToast('请先登录后再点赞')
    router.push({ path: '/auth', query: { redirect: route.fullPath } })
    return
  }

  likeSubmitting.value = true
  try {
    const result = await toggleArticleLike(currentArticle.value.id)
    const liked = Boolean(result?.liked)
    const likeCount = Number(result?.likeCount ?? currentArticle.value.likeCount ?? 0)
    article.value = {
      ...currentArticle.value,
      isLiked: liked,
      likeCount
    }
    invalidateTechArticlesCache()
    showToast(liked ? '已点赞' : '已取消点赞')
  } catch (error) {
    showToast(error?.message || '点赞失败，请稍后再试')
  } finally {
    likeSubmitting.value = false
  }
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

  // HTML: has opening + closing tags or typical HTML structure
  if (/<\/?[a-z][\w-]*(\s[^>]*)?>/i.test(text) && /<\/\w+>/.test(text) && !/^\s*[{[]/.test(text)) return 'HTML'

  // XML: XML declaration or namespace-prefixed tags
  if (/^<\?xml/i.test(text) || /xmlns[:=]/i.test(text)) return 'XML'

  // JSON: valid JSON structure
  try { JSON.parse(text); return 'JSON' } catch {}

  // YAML: indented key-value pairs, no braces/semicolons
  if (/(^|\n)\s*\w[\w.-]*\s*:/m.test(text) && !/[{};]/.test(text) && !/\/\*|\/\/|#include/.test(text)) return 'YAML'

  // Markdown: headings, dividers, or link syntax
  if (/^#{1,6}\s/m.test(text) || /^\*{3,}$/m.test(text) || /\[.*\]\(.*\)/m.test(text)) return 'Markdown'

  // SQL: typical SQL keywords at line start or inline
  if (/\b(SELECT|INSERT\s+INTO|UPDATE\s+\w+\s+SET|DELETE\s+FROM|CREATE\s+(TABLE|INDEX|VIEW)|ALTER\s+TABLE|DROP\s+(TABLE|INDEX|VIEW)|WITH\s+RECURSIVE)\b/i.test(text)) return 'SQL'

  // CSS: selectors with braces and colons, or at-rules
  if (/(@media|@import|@keyframes|@supports|@font-face)\b/.test(text)) return 'CSS'
  if (/(^|\n)\s*[.#@][\w-]+(\s+[\w-]+)*\s*\{/m.test(text) && /:\s*[^;]+;/.test(text)) return 'CSS'

  // Python: distinctive patterns
  if (/\bdef\s+\w+\s*\(/.test(text) || /\bimport\s+\w+/.test(text) || /\bfrom\s+\w+\s+import\b/.test(text) || /\bclass\s+\w+\s*[:\(]/.test(text) || /if\s+__name__\s*==\s*['"]__main__['"]/.test(text) || /\bprint\s*\(/.test(text) || /\belif\s+|else:\s*$/.test(text) || /\bself\b/.test(text) && !/[{;}]/.test(text)) return 'Python'

  // Java: distinctive Java-only patterns (check before JavaScript)
  if (/\bpackage\s+\w/.test(text) || /\bimport\s+(static\s+)?[\w.]+\.\*?\s*;/.test(text) || /\bpublic\s+class\s+\w/.test(text) || /\bpublic\s+static\s+void\s+main\s*\(/.test(text) || /\bSystem\.out\./.test(text) || /\b@Override\b/.test(text) || /\bnew\s+\w+\s*\(/.test(text) && /\b(public|private|protected)\s+(static\s+)?(void|int|String|boolean|long|double|float)/.test(text)) return 'Java'

  // Go
  if (/\bfunc\s+\w+\s*\(/.test(text) || /\bpackage\s+main\b/.test(text) || /\bimport\s*\(/.test(text) || /:=/.test(text) && /\bfmt\./.test(text) || /\bgo\s+func\b/.test(text) || /\bdefer\s+\w/.test(text) || /\bgo\s+routine\b/i.test(text)) return 'Go'

  // Rust
  if (/\bfn\s+\w+\s*[<(]/.test(text) || /\blet\s+mut\b/.test(text) || /\bimpl\s+\w/.test(text) || /\bpub\s+fn\b/.test(text) || /\buse\s+std::/.test(text) || /println!\s*\(/.test(text) || /\/\/!|#!\[/.test(text) || /\bstruct\s+\w+\s*\{/.test(text) && /\bpub\b/.test(text)) return 'Rust'

  // C#: namespace/using + typical C# patterns
  if (/\busing\s+System\b/.test(text) || /\bnamespace\s+\w/.test(text) || /\bConsole\.Write(Line)?\s*\(/.test(text) || /\bvar\s+\w+\s*=\s*new\s+\w+/.test(text) || /\b(public|private|protected|internal)\s+(class|interface|enum)\s+\w/.test(text)) return 'C#'

  // Kotlin
  if (/\b(fun|val)\s+\w+\s*[{(:]/.test(text) || /\bdata\s+class\b/.test(text) || /\bcompanion\s+object\b/.test(text) || /\bsuspend\s+fun\b/.test(text) || /\bby\s+lazy\b/.test(text) || /\b::class\./.test(text)) return 'Kotlin'

  // Swift
  if (/\b(func|var|let)\s+\w+\s*[{(:]/.test(text) && /:\s*(String|Int|Bool|Double|Float|\[.*\]|\(.*\))\b/.test(text) || /\bimport\s+Foundation\b/.test(text) || /\b@IBAction\b|\b@IBOutlet\b|\b@State\b|\b@Binding\b|\b@ObservedObject\b/.test(text)) return 'Swift'

  // PHP
  if (/<\?php/i.test(text) || /\$\w+\s*=\s*/.test(text) || /\$\w+->/.test(text) || /\bnamespace\s+\w/.test(text) && /\$\w+/.test(text)) return 'PHP'

  // Ruby
  if (/\bdef\s+\w+\s*$/.test(text) || /\bputs\s+/.test(text) || /\brequire\s+['"]/.test(text) || /\bclass\s+\w+\s*<\s*\w/.test(text) || /\battr_(accessor|reader|writer)\b/.test(text) || /\bdo\s*\|/.test(text) && /\bend\b/.test(text)) return 'Ruby'

  // TypeScript: type annotations (check before JavaScript)
  if (/\binterface\s+\w+\s*\{/.test(text) || /\btype\s+\w+\s*=\s*/.test(text) || /:\s*(string|number|boolean|void|any|never|unknown|Promise)\b/.test(text) || /\benum\s+\w+\s*\{/.test(text) || /\bas\s+\w+/.test(text) && /(const|let|var|function|=>)/.test(text) || /<[A-Z]\w*>/.test(text) && /(const|let|var|function|import|export)/.test(text)) return 'TypeScript'

  // C / C++
  if (/#include\s*[<"]/.test(text)) return /\b(std::|cout|cin|class\s+\w+\s*\{|template\s*<|vector\s*<|unique_ptr|shared_ptr)\b/.test(text) ? 'C++' : 'C'

  // Shell: shebang or common shell commands
  if (/^#!\/(bin|usr\/bin)\/(bash|sh|zsh|env)/m.test(text) || /\b(echo|cd|mkdir|rm|curl|wget|chmod|export|grep|sed|awk|npm|yarn|pnpm|git\s+clone)\s+/m.test(text) || /\$\{[A-Z_]+\}/.test(text)) return 'Shell'

  // JavaScript: common JS patterns (check after language-specific ones)
  if (/\b(const|let|var)\s+\w+\s*=/.test(text) || /\bfunction\s+\w+\s*\(/.test(text) || /\bimport\s+.*\s+from\s+['"]/.test(text) || /\bexport\s+(default|const|function|class)\b/.test(text) || /\bconsole\.log\b/.test(text) || /\bdocument\./.test(text) || /\bwindow\./.test(text) || /\brequire\s*\(/.test(text) || /\baddEventListener\b/.test(text) || /\bnew\s+Promise\b/.test(text) || /\bsetTimeout\b|\bsetInterval\b/.test(text)) return 'JavaScript'

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
    frontend: '前端工程',
    backend: '后端架构',
    project: '项目业务解析',
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
    return '1 分钟阅读'
  }

  return `${minuteMatch[1]} 分钟阅读`
}

function formatHistoryTime(value) {
  const date = new Date(String(value || '').replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return ''

  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  if (diffMs >= 0 && diffMs < 60000) return '刚刚阅读'
  if (diffMs >= 0 && diffMs < 3600000) return `${Math.floor(diffMs / 60000)} 分钟前阅读`

  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const startOfTarget = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const diffDays = Math.round((startOfToday - startOfTarget) / 86400000)
  const timeText = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`

  if (diffDays === 0) return `今天 ${timeText} 阅读`
  if (diffDays === 1) return `昨天 ${timeText} 阅读`
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')} ${timeText}`
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
  const categoryLabel = targetArticle.categoryLabel || resolveCategoryLabel(targetArticle.category)
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
