<template>
  <section id="ai-hotspot-section" class="learning-route-section" aria-labelledby="ai-hotspot-title">
    <div class="learning-route-head">
      <h2 id="ai-hotspot-title">AI 热点</h2>
      <p>把近期 AI 方向里真正值得关注的变化，压缩成首页里可以快速扫读的卡片内容。</p>
    </div>

    <transition-group
      :name="enableRevealAnimation ? 'learning-route-reveal' : 'learning-route-static'"
      tag="div"
      class="learning-route-grid"
      appear
    >
      <article
        v-for="hotspot in hotspots"
        :key="hotspot.id"
        class="learning-route-card"
      >
        <img class="learning-route-cover" :src="hotspot.coverUrl" :alt="hotspot.title" loading="lazy" />
        <div class="learning-route-overlay">
          <span class="learning-route-type">{{ hotspot.hotspotType }}</span>
          <h3>{{ hotspot.title }}</h3>
          <div class="learning-route-meta">
            <time>{{ hotspot.publishedAt }}</time>
            <span>{{ hotspot.difficulty }}</span>
          </div>
          <div class="learning-route-stats">
            <span>{{ formatCount(hotspot.viewCount) }} 阅读</span>
            <span>{{ formatCount(hotspot.commentCount) }} 评论</span>
            <span>{{ formatCount(hotspot.likeCount) }} 点赞</span>
          </div>
        </div>
      </article>
    </transition-group>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'

const enableRevealAnimation = ref(true)

const hotspots = [
  {
    id: 'ai-hotspot-agent-workflow',
    hotspotType: 'agent',
    title: 'Agent 工作流从演示走向真实工程',
    coverUrl: '/【哲风壁纸】xiaomiyu7-小米suv.png',
    publishedAt: '2026-04-24 11:10',
    difficulty: '趋势热点',
    viewCount: 4188,
    commentCount: 56,
    likeCount: 701
  },
  {
    id: 'ai-hotspot-multimodal',
    hotspotType: 'multimodal',
    title: '多模态能力重塑内容生产与交互体验',
    coverUrl: '/【哲风壁纸】夏日-晴天-氛围感.png',
    publishedAt: '2026-04-24 12:00',
    difficulty: '行业观察',
    viewCount: 3924,
    commentCount: 49,
    likeCount: 648
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
