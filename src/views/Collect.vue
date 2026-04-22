<template>
  <div class="simple-page">
    <header class="header">
      <div class="header-inner">
        <span class="logo">PeakStars_blog</span>
      </div>
    </header>
    <div class="empty-state">
      <div class="empty-icon">⭐</div>
      <div class="empty-title">收藏夹空空如也</div>
      <div class="empty-sub">快去面经列表收藏感兴趣的内容吧</div>
      <router-link to="/interview" class="go-btn">去逛逛</router-link>
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
<style scoped src="../styles/views/Collect.css"></style>
