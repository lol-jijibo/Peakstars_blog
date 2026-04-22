<template>
  <div class="card" @click="$emit('click')">
    <div class="card-top">
      <!-- 左侧图标 -->
      <div class="book-icon">
        <span class="book-emoji">📖</span>
      </div>
      <!-- 右侧信息 -->
      <div class="card-info">
        <div class="card-title">{{ data.title }}</div>
        <div class="card-meta">
          <span class="author">{{ data.author }}</span>
          <span class="sep">|</span>
          <span class="date">{{ data.date }}</span>
        </div>
      </div>
      <!-- 分类标签 -->
      <div class="cat-badge" :class="data.category">
        {{ data.category === 'java' ? 'Java' : '前端' }}
      </div>
    </div>

    <!-- 摘要 -->
    <div class="card-summary">{{ data.summary }}</div>

    <!-- 底部统计 -->
    <div class="card-footer">
      <span class="stat likes">👍 点赞 {{ data.likes }}</span>
      <span class="stat views">👁 浏览 {{ data.views }}</span>
      <div class="tags">
        <span v-for="tag in data.tags.slice(0, 2)" :key="tag" class="tag">{{ tag }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  data: {
    type: Object,
    required: true
  }
})
defineEmits(['click'])
</script>

<style scoped src="../styles/components/InterviewCard.css"></style>
