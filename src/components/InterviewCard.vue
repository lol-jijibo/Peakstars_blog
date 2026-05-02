<template>
  <article class="int-card" @click="$emit('click')">
    <!-- 分类标签 -->
    <span class="int-card-badge" :style="{ background: badgeColor }">{{ badgeLabel }}</span>

    <!-- 标题 -->
    <h3 class="int-card-title">{{ data.title }}</h3>

    <!-- 难度 + 题数 -->
    <div class="int-card-meta">
      <span class="int-card-diff" :class="diffClass">{{ diffLabel }}</span>
      <span class="int-card-count">{{ questionCount }} 题</span>
    </div>

    <!-- 进度条 -->
    <div class="int-card-progress-shell">
      <div class="int-card-progress-bar" :style="{ width: progressPercent + '%', background: progressColor }"></div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  data: { type: Object, required: true }
})
defineEmits(['click'])

const badgeColor = computed(() => {
  const map = {
    java: 'linear-gradient(135deg, #3b82f6, #60a5fa)',
    frontend: 'linear-gradient(135deg, #8b5cf6, #a78bfa)',
    agent: 'linear-gradient(135deg, #f59e0b, #fbbf24)',
    llm: 'linear-gradient(135deg, #10b981, #34d399)',
    all: 'linear-gradient(135deg, #6c63ff, #5ac8fa)'
  }
  return map[props.data.category] || 'linear-gradient(135deg, #6c63ff, #5ac8fa)'
})

const badgeLabel = computed(() => {
  const map = {
    java: 'Java',
    frontend: '前端',
    agent: 'Agent开发',
    llm: '大模型原理',
    all: '综合'
  }
  return map[props.data.category] || props.data.category || '面经'
})

const diffClass = computed(() => {
  const diff = (props.data.difficulty || '').toLowerCase()
  if (diff.includes('高') || diff.includes('难')) return 'int-diff-hard'
  if (diff.includes('中')) return 'int-diff-medium'
  return 'int-diff-easy'
})

const diffLabel = computed(() => {
  const diff = (props.data.difficulty || '').toLowerCase()
  if (diff.includes('高') || diff.includes('难')) return '困难'
  if (diff.includes('中')) return '中等'
  return '基础'
})

const questionCount = computed(() => props.data.questionCount || props.data.relatedCount || Math.floor(Math.random() * 8) + 3)

const progressPercent = computed(() => {
  if (props.data.mastery) return Math.min(props.data.mastery, 100)
  return Math.min(Math.floor((props.data.likeCount || 0) / 3), 100)
})

const progressColor = computed(() => {
  const p = progressPercent.value
  if (p >= 80) return 'linear-gradient(90deg, #22c55e, #4ade80)'
  if (p >= 50) return 'linear-gradient(90deg, #eab308, #facc15)'
  return 'linear-gradient(90deg, #3b82f6, #60a5fa)'
})
</script>

<style scoped src="../styles/components/InterviewCard.css"></style>
