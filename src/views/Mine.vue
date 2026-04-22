<template>
  <div class="simple-page">
    <header class="header">
      <div class="header-inner">
        <span class="logo">PeakStars_blog</span>
      </div>
    </header>
    <div class="profile-card">
      <div class="avatar-big">ME</div>
      <div class="nickname">Frontend User</div>
      <div class="bio">Preparing for interviews.</div>
      <div class="stats-row">
        <div class="stat-item">
          <div class="stat-num">8</div>
          <div class="stat-label">Posts</div>
        </div>
        <div class="stat-item">
          <div class="stat-num">0</div>
          <div class="stat-label">Collects</div>
        </div>
        <div class="stat-item">
          <div class="stat-num">0</div>
          <div class="stat-label">Likes</div>
        </div>
      </div>
    </div>
    <div class="menu-list">
      <div class="menu-item" v-for="m in menus" :key="m.icon">
        <span class="menu-icon">{{ m.icon }}</span>
        <span class="menu-text">{{ m.text }}</span>
        <span class="menu-arrow">&gt;</span>
      </div>
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
  { path: '/interview', icon: 'I', label: 'Interview' },
  { path: '/collect', icon: 'C', label: 'Collect' },
  { path: '/like', icon: 'L', label: 'Like' },
  { path: '/mine', icon: 'M', label: 'Mine' }
]
const menus = [
  { icon: 'P', text: 'My interviews' },
  { icon: 'C', text: 'My collects' },
  { icon: 'N', text: 'Notifications' },
  { icon: 'S', text: 'Settings' },
  { icon: 'A', text: 'About us' }
]
</script>
<style scoped src="../styles/views/Mine.css"></style>
