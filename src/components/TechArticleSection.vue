<template>
  <section id="tech-article-section" class="learning-route-section" aria-labelledby="tech-article-title">
    <div class="learning-route-head">
      <h2 id="tech-article-title">技术文章</h2>
      <p>延续学习路线的节奏，继续看近期值得沉淀的工程实践与技术拆解。</p>
    </div>

    <transition-group
      :name="enableRevealAnimation ? 'learning-route-reveal' : 'learning-route-static'"
      tag="div"
      class="learning-route-grid"
      appear
    >
      <article
        v-for="article in articles"
        :key="article.id"
        class="learning-route-card"
      >
        <img class="learning-route-cover" :src="article.coverUrl" :alt="article.title" loading="lazy" />
        <div class="learning-route-overlay">
          <span class="learning-route-type">{{ article.articleType }}</span>
          <h3>{{ article.title }}</h3>
          <div class="learning-route-meta">
            <time>{{ article.publishedAt }}</time>
            <span>{{ article.difficulty }}</span>
          </div>
          <div class="learning-route-stats">
            <span>{{ formatCount(article.viewCount) }} 阅读</span>
            <span>{{ formatCount(article.commentCount) }} 评论</span>
            <span>{{ formatCount(article.likeCount) }} 点赞</span>
          </div>
        </div>
      </article>
    </transition-group>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'

const enableRevealAnimation = ref(true)

const articles = [
  {
    id: 'tech-article-vue-performance',
    articleType: 'frontend',
    title: 'Vue 组件拆分与页面性能优化实战',
    coverUrl: '/【哲风壁纸】夏日-晴天-氛围感.png',
    publishedAt: '2026-04-24 09:30',
    difficulty: '实践文章',
    viewCount: 3216,
    commentCount: 42,
    likeCount: 518
  },
  {
    id: 'tech-article-java-architecture',
    articleType: 'backend',
    title: 'Spring Boot 接口设计与工程分层复盘',
    coverUrl: '/【哲风壁纸】xiaomiyu7-小米suv.png',
    publishedAt: '2026-04-24 10:20',
    difficulty: '架构拆解',
    viewCount: 2874,
    commentCount: 35,
    likeCount: 463
  }
]

function formatCount(value) {
  const count = Number(value || 0)
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}w`
  }
  if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`
  }
  return `${count}`
}

onMounted(() => {
  window.setTimeout(() => {
    enableRevealAnimation.value = false
  }, 180)
})
</script>

<style scoped src="../styles/components/HomeCardSection.css"></style>
