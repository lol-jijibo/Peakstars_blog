<template>
  <section id="world-news-section" class="learning-route-section" aria-labelledby="world-news-title">
    <div class="learning-route-head">
      <h2 id="world-news-title">看天下</h2>
      <p>精选全球科技、产品和行业动态，把值得关注的外部变化压缩成首页里一眼能扫完的内容卡片。</p>
    </div>

    <transition-group
      :name="enableRevealAnimation ? 'learning-route-reveal' : 'learning-route-static'"
      tag="div"
      class="learning-route-grid"
      appear
    >
      <article
        v-for="news in worldNews"
        :key="news.id"
        class="learning-route-card"
      >
        <img class="learning-route-cover" :src="news.coverUrl" :alt="news.title" loading="lazy" />
        <div class="learning-route-overlay">
          <span class="learning-route-type">{{ news.newsType }}</span>
          <h3>{{ news.title }}</h3>
          <div class="learning-route-meta">
            <time>{{ news.publishedAt }}</time>
            <span>{{ news.tag }}</span>
          </div>
          <div class="learning-route-stats">
            <span>{{ formatCount(news.viewCount) }} 阅读</span>
            <span>{{ formatCount(news.commentCount) }} 评论</span>
            <span>{{ formatCount(news.likeCount) }} 点赞</span>
          </div>
        </div>
      </article>
    </transition-group>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'

const enableRevealAnimation = ref(true)

const worldNews = [
  {
    id: 'world-news-global-ai',
    newsType: 'global',
    title: '全球 AI 产品竞速加快，开发工具与内容平台持续联动升级',
    coverUrl: '/【哲风壁纸】xiaomiyu7-小米suv.png',
    publishedAt: '2026-04-24 11:35',
    tag: '全球科技',
    viewCount: 3652,
    commentCount: 38,
    likeCount: 506
  },
  {
    id: 'world-news-startup',
    newsType: 'industry',
    title: '从资本到生态，技术创业公司开始回到更务实的产品验证周期',
    coverUrl: '/【哲风壁纸】夏日-晴天-氛围感.png',
    publishedAt: '2026-04-24 12:10',
    tag: '行业观察',
    viewCount: 2948,
    commentCount: 27,
    likeCount: 421
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
