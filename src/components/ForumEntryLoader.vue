<template>
  <transition name="forum-loader-fade">
    <div v-if="visible" class="forum-entry-loader">
      <div class="forum-entry-card">
        <div class="forum-entry-kicker">{{ kicker }}</div>
        <h2 class="forum-entry-title">{{ title }}</h2>
        <p class="forum-entry-subtitle">{{ subtitle }}</p>

        <div class="forum-entry-progress-shell">
          <div class="forum-entry-progress-bar" :style="{ width: `${safeProgress}%` }"></div>
        </div>

        <div class="forum-entry-progress-meta">
          <span>{{ statusText }}</span>
          <strong>{{ safeProgress }}%</strong>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  kicker: {
    type: String,
    default: 'Peakstars Forum'
  },
  title: {
    type: String,
    default: '正在进入 PeakStars_blog'
  },
  subtitle: {
    type: String,
    default: '请稍候，系统正在同步页面内容，并展示当前进入进度。'
  },
  progress: {
    type: Number,
    default: 0
  },
  statusText: {
    type: String,
    default: '等待开始'
  }
})

const safeProgress = computed(() => Math.max(0, Math.min(100, Math.round(props.progress))))
</script>

<style scoped src="../styles/components/ForumEntryLoader.css"></style>
