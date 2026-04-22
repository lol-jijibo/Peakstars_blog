<template>
  <div class="simple-page">
    <header class="header">
      <div class="header-inner">
        <span class="logo">PeakStars_blog</span>
      </div>
    </header>
    <div class="empty-state">
      <div class="empty-icon">❤️</div>
      <div class="empty-title">还没有喜欢的内容</div>
      <div class="empty-sub">点赞你喜欢的面经，它们将出现在这里</div>
      <router-link to="/interview" class="go-btn">去看看</router-link>
    </div>
    <nav class="tab-bar">
      <router-link v-for="tab in navTabs" :key="tab.path" :to="tab.path" class="tab-item" :class="{ active: $route.path === tab.path }">
        <span class="tab-icon">{{ tab.icon }}</span>
        <span class="tab-label">{{ tab.label }}</span>
      </router-link>
    </nav>
  </div>
</template>
<script setup>
const navTabs = [
  { path: '/interview', icon: '📋', label: '面经' },
  { path: '/collect', icon: '⭐', label: '收藏' },
  { path: '/like', icon: '❤️', label: '喜欢' },
  { path: '/mine', icon: '👤', label: '我的' }
]
</script>
<style scoped src="../styles/views/Like.css"></style>
