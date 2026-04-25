<template>
  <div class="detail-page">
    <header class="detail-header">
      <button class="back-btn" @click="goBack">
        <span class="back-icon">‹</span>
      </button>
      <span class="header-title">面经详情</span>
      <div class="header-right">
        <button class="return-login-btn" @click="goMine">我的</button>
        <button class="like-btn" @click="toggleLike" :class="{ liked: isLiked }">
          {{ isLiked ? '❤️' : '🤍' }}
        </button>
      </div>
    </header>

    <div v-if="loading" class="loading-state">加载中...</div>
    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button @click="$router.push('/interview')" class="back-home-btn">返回列表</button>
    </div>

    <main v-else-if="interview" class="detail-main">
      <div class="detail-hero">
        <h1 class="detail-title">{{ interview.title }}</h1>
        <div class="detail-stats">
          <span>{{ interview.date }}</span>
          <span class="dot">·</span>
          <span>{{ interview.views }} 浏览</span>
          <span class="dot">·</span>
          <span>{{ interview.likes }} 点赞</span>
        </div>

        <div class="author-row">
          <div class="avatar" :style="{ background: interview.avatarColor }">
            {{ interview.avatar }}
          </div>
          <span class="author-name">{{ interview.author }}</span>
          <span class="cat-badge" :class="interview.category">
            {{ interview.category === 'java' ? 'Java 后端' : '前端' }}
          </span>
        </div>

        <div class="tags-row">
          <span v-for="tag in interview.tags" :key="tag" class="tag">{{ tag }}</span>
        </div>
      </div>

      <div class="divider"></div>

      <div class="detail-content">
        <div
          v-for="(block, idx) in contentBlocks"
          :key="idx"
          :class="block.type"
          v-html="block.html"
        ></div>
      </div>

      <div class="detail-actions">
        <button class="action-btn like-action" @click="toggleLike" :class="{ liked: isLiked }">
          <span>{{ isLiked ? '❤️' : '🤍' }}</span>
          <span>点赞 {{ localLikes }}</span>
        </button>
        <button
          class="action-btn collect-action"
          @click="toggleCollect"
          :class="{ collected: isCollected }"
        >
          <span>{{ isCollected ? '⭐' : '☆' }}</span>
          <span>{{ isCollected ? '已收藏' : '收藏' }}</span>
        </button>
        <button class="action-btn share-action" @click="copyLink">
          <span>🔗</span>
          <span>{{ copyTip }}</span>
        </button>
      </div>
    </main>

    <div v-else class="not-found">
      <p>面经不存在</p>
      <button @click="goBack" class="back-home-btn">返回列表</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { collectInterview, getInterviewDetail, likeInterview } from '@/api/interview.js'

const route = useRoute()
const router = useRouter()

const interview = ref(null)
const loading = ref(true)
const error = ref('')
const isLiked = ref(false)
const isCollected = ref(false)
const localLikes = ref(0)
const copyTip = ref('分享')

onMounted(async () => {
  try {
    const data = await getInterviewDetail(route.params.id)
    interview.value = data
    localLikes.value = data.likes ?? 0
  } catch {
    error.value = '面经加载失败，请检查后端服务是否已经启动。'
  } finally {
    loading.value = false
  }
})

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
    // 收藏接口失败时保留本地状态，避免打断阅读体验。
  }
}

function copyLink() {
  const url = window.location.href

  navigator.clipboard
    .writeText(url)
    .then(() => {
      copyTip.value = '已复制'
      window.setTimeout(() => {
        copyTip.value = '分享'
      }, 2000)
    })
    .catch(() => {
      copyTip.value = '已复制'
      window.setTimeout(() => {
        copyTip.value = '分享'
      }, 2000)
    })
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }

  router.push('/interview')
}

function goMine() {
  router.push('/mine')
}

const contentBlocks = computed(() => {
  if (!interview.value) return []

  const lines = interview.value.content.split('\n')
  const blocks = []

  for (const line of lines) {
    if (!line.trim()) continue

    if (line.startsWith('**') && line.endsWith('**') && line.indexOf('**', 2) === line.length - 2) {
      blocks.push({ type: 'block-heading', html: line.replace(/\*\*/g, '') })
      continue
    }

    if (/^\d+\./.test(line) || /^-\s/.test(line)) {
      const text = line.replace(/^\d+\.\s?/, '').replace(/^-\s/, '')
      blocks.push({ type: 'block-item', html: styleInline(text) })
      continue
    }

    blocks.push({ type: 'block-para', html: styleInline(line) })
  }

  return blocks
})

function styleInline(text) {
  return text
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
}
</script>

<style scoped src="../styles/views/InterviewDetail.css"></style>
