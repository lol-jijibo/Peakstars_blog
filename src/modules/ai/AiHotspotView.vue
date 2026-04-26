<template>
  <div class="ai-hotspot-page">
    <blog-mega-header current-page="ai" />

    <main class="ai-hotspot-main">
      <section class="ai-hotspot-shell" aria-labelledby="ai-hotspot-page-title">
        <el-row :gutter="24" class="ai-hotspot-layout">
          <el-col :xs="24" :lg="16" class="ai-hotspot-feed-col">
            <div class="ai-hotspot-feed">
              <div class="ai-feed-head">
                <div>
                  <span class="ai-feed-kicker">Flow List</span>
                  <h2>{{ activeTrackLabel }}</h2> 
                </div>
                <p>{{ activeTrackDescription }}</p>
              </div>

              <div class="ai-feed-list">
                <el-card
                  v-for="hotspot in filteredHotspots"
                  :key="hotspot.id"
                  shadow="hover"
                  class="ai-feed-card"
                >
                  <div class="ai-feed-card-main">
                    <div class="ai-feed-card-head">
                      <div class="ai-feed-meta-top">
                        <el-tag size="small" :type="resolveTagType(hotspot.hotspotType)" effect="dark">
                          {{ hotspot.hotspotType }}
                        </el-tag>
                        <span class="ai-feed-time">{{ hotspot.publishedAt }}</span>
                      </div>
                      <div class="ai-feed-score">
                        <strong>{{ hotspot.heat }}</strong>
                        <span>热度</span>
                      </div>
                    </div>

                    <h3>{{ hotspot.title }}</h3>
                    <p class="ai-feed-summary">{{ hotspot.summary }}</p>

                    <div class="ai-feed-tags">
                      <el-tag
                        v-for="tag in hotspot.tags"
                        :key="tag"
                        size="small"
                        effect="plain"
                      >
                        {{ tag }}
                      </el-tag>
                    </div>

                    <el-divider />

                    <div class="ai-feed-bottom">
                      <div class="ai-feed-stats">
                        <span>{{ formatCount(hotspot.viewCount) }} 阅读</span>
                        <span>{{ formatCount(hotspot.commentCount) }} 评论</span>
                        <span>{{ formatCount(hotspot.likeCount) }} 点赞</span>
                      </div>
                      <el-button text type="primary">查看解读</el-button>
                    </div>
                  </div>
                </el-card>
              </div>
            </div>
          </el-col>

          <el-col :xs="24" :lg="8" class="ai-hotspot-sidebar-col">
            <div class="ai-hotspot-sidebar">
              <el-card shadow="never" class="ai-panel ai-signin-card">
                <div class="ai-signin-copy">
                  <span class="ai-signin-kicker">时段问候</span>
                  <p>{{ dailyMessage }}</p>
                </div>
                <button type="button" class="ai-signin-btn">去签到</button>
              </el-card>

              <el-card shadow="never" class="ai-panel ai-ranking-card">
                <template #header>
                  <div class="ai-ranking-head">
                    <div class="ai-ranking-title">
                      <span class="ai-ranking-badge">☰</span>
                      <strong>文章榜</strong>
                    </div>
                    <button type="button" class="ai-ranking-switch" @click="switchRankingPage">
                      ↻ 换一换
                    </button>
                  </div>
                </template>

                <div class="ai-ranking-list">
                  <article
                    v-for="(article, index) in displayedRankArticles"
                    :key="article.id"
                    class="ai-ranking-item"
                  >
                    <span class="ai-ranking-index">{{ rankingStart + index + 1 }}</span>
                    <div class="ai-ranking-copy">
                      <h3>{{ article.title }}</h3>
                    </div>
                  </article>
                </div>

                <button type="button" class="ai-ranking-more">查看更多 ></button>
              </el-card>
            </div>
          </el-col>
        </el-row>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElButton, ElCard, ElCol, ElDivider, ElRow, ElTag } from 'element-plus'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/card/style/css'
import 'element-plus/es/components/col/style/css'
import 'element-plus/es/components/divider/style/css'
import 'element-plus/es/components/row/style/css'
import 'element-plus/es/components/tag/style/css'
import BlogMegaHeader from '@/components/BlogMegaHeader.vue'
import { aiHotspots, aiTracks } from './aiHotspots'
import { techArticles } from '@/data/techArticles'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const activeTrack = ref('all')
const rankingPage = ref(0)
const now = ref(new Date())
let greetingTimer = null

const tracks = aiTracks
const rankingPageSize = 5

const rankingArticles = [...techArticles].sort((left, right) => right.readCount - left.readCount)
const rankingGroups = Array.from(
  { length: Math.ceil(rankingArticles.length / rankingPageSize) },
  (_, index) => rankingArticles.slice(index * rankingPageSize, (index + 1) * rankingPageSize)
)

const filteredHotspots = computed(() => {
  if (activeTrack.value === 'all') {
    return aiHotspots
  }

  return aiHotspots.filter((item) => item.track === activeTrack.value)
})

const activeTrackMeta = computed(() => {
  return tracks.find((item) => item.key === activeTrack.value) || tracks[0]
})

const activeTrackLabel = computed(() => activeTrackMeta.value.label)
const activeTrackDescription = computed(() => activeTrackMeta.value.description)

const displayedRankArticles = computed(() => {
  return rankingGroups[rankingPage.value] || []
})

const rankingStart = computed(() => rankingPage.value * rankingPageSize)

const currentUser = computed(() => authStore.currentUser.value)

const displayName = computed(() => {
  const user = currentUser.value
  return user?.username || user?.email?.split('@')[0] || 'PeakStars 用户'
})

const dailyMessage = computed(() => {
  const hour = now.value.getHours()
  let greeting = '你好'

  if (hour >= 5 && hour < 11) {
    greeting = '早上好'
  } else if (hour >= 11 && hour < 18) {
    greeting = '中午好'
  } else if (hour >= 18 || hour < 1) {
    greeting = '晚上好'
  }

  return `${greeting}，${displayName.value}`
})

function switchRankingPage() {
  rankingPage.value = (rankingPage.value + 1) % rankingGroups.length
}

function resolveTagType(type) {
  if (type === 'agent') {
    return 'primary'
  }

  if (type === 'multimodal') {
    return 'success'
  }

  if (type === 'infra') {
    return 'warning'
  }

  return 'info'
}

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

function syncCurrentTime() {
  now.value = new Date()
}

onMounted(() => {
  syncCurrentTime()
  greetingTimer = window.setInterval(syncCurrentTime, 60000)
})

onBeforeUnmount(() => {
  if (greetingTimer) {
    window.clearInterval(greetingTimer)
    greetingTimer = null
  }
})
</script>

<style src="./AiHotspotView.css"></style>
