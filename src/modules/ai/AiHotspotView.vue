<template>
  <div class="ai-hotspot-page">
    <blog-mega-header current-page="ai" />

    <main class="ai-hotspot-main">
      <section class="ai-hotspot-shell" aria-labelledby="ai-hotspot-page-title">
        <el-row :gutter="18" class="ai-hotspot-layout">
          <el-col :xs="24" :lg="16" class="ai-hotspot-feed-col">
            <div class="ai-hotspot-feed">
              <div class="ai-feed-tabs" role="tablist" aria-label="AI 热点列表切换">
                <button
                  v-for="tab in feedTabs"
                  :key="tab.key"
                  type="button"
                  class="ai-feed-tab"
                  :class="{ 'is-active': activeFeedTab === tab.key }"
                  :aria-selected="activeFeedTab === tab.key"
                  @click="activeFeedTab = tab.key"
                >
                  {{ tab.label }}
                </button>
              </div>

              <div class="ai-feed-list">
                <article
                  v-for="hotspot in displayedHotspots"
                  :key="hotspot.id"
                  class="ai-feed-item"
                >
                  <div class="ai-feed-content">
                    <div class="ai-feed-copy">
                      <h3>{{ hotspot.title }}</h3>
                      <p class="ai-feed-summary">{{ hotspot.summary }}</p>
                    </div>

                    <div class="ai-feed-stats">
                      <span>{{ hotspot.authorName }}</span>
                      <span>浏览 {{ formatViewCountInK(hotspot.viewCount) }}</span>
                      <span>点赞 {{ hotspot.likeCount }}</span>
                      <span>{{ hotspot.publishedAt }}</span>
                    </div>
                    <div class="ai-feed-tags">
                      <span
                        v-for="tag in hotspot.tags"
                        :key="tag"
                        class="ai-feed-tag"
                      >
                        {{ tag }}
                      </span>
                    </div>
                  </div>

                  <div class="ai-feed-cover-shell">
                    <img
                      class="ai-feed-cover"
                      :src="hotspot.coverUrl"
                      :alt="hotspot.title"
                      loading="lazy"
                    />
                  </div>
                </article>
              </div>
            </div>
          </el-col>

          <el-col :xs="24" :lg="8" class="ai-hotspot-sidebar-col">
            <div class="ai-hotspot-sidebar">
              <el-card shadow="never" class="ai-panel ai-signin-card">
                <!-- 业务目的：将问候语和签到入口收拢到同一行，减少侧边卡片纵向占用。业务逻辑：左侧展示当前时间段问候语与用户名，右侧保留签到按钮，配合样式在窄屏时自动换行。 -->
                <div class="ai-signin-row">
                  <div class="ai-signin-copy">
                    <p>{{ dailyMessage }}</p>
                  </div>
                  <button type="button" class="ai-signin-btn">去签到</button>
                </div>
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
import { ElCard, ElCol, ElRow } from 'element-plus'
import 'element-plus/es/components/card/style/css'
import 'element-plus/es/components/col/style/css'
import 'element-plus/es/components/row/style/css'
import { getAiHotspots, getTechArticles } from '@/api/content'
import BlogMegaHeader from '@/components/BlogMegaHeader.vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
// 业务目的：维护热点列表当前选中的资讯标签，先用前端状态模拟后端栏目切换。
// 输入输出：输入用户点击的标签 key，输出当前列表应该渲染的热点分组。
// 关键逻辑：标签配置与当前选中值拆开维护，后续接 MySQL 时只需替换数据来源，不用改交互层。
const activeFeedTab = ref('recommended')
const rankingPage = ref(0)
const now = ref(new Date())
const aiHotspots = ref([])
const techArticles = ref([])
let greetingTimer = null

const rankingPageSize = 5
const feedTabs = [
  { key: 'recommended', label: '推荐' },
  { key: 'latest', label: '最新' }
]

// 业务目的：让 AI 频道右侧文章榜直接复用后端技术文章数据，避免榜单继续依赖前端静态数组。
// 业务逻辑：先按阅读数排序，再按固定分页大小切组，保持“换一换”的交互不变。
const rankingArticles = computed(() =>
  [...techArticles.value].sort((left, right) => right.readCount - left.readCount)
)

const rankingGroups = computed(() =>
  Array.from(
    { length: Math.ceil(rankingArticles.value.length / rankingPageSize) || 0 },
    (_, index) => rankingArticles.value.slice(index * rankingPageSize, (index + 1) * rankingPageSize)
  )
)

// 业务目的：解析前端模拟发布时间，先支撑“最新”标签排序，后续可直接替换成后端时间戳。
// 输入输出：输入热点发布时间字符串，输出可比较的毫秒时间。
// 关键逻辑：把日期中的空格替换成 ISO 兼容格式，降低不同浏览器的解析偏差。
function parsePublishedAt(value) {
  return new Date(String(value || '').replace(' ', 'T')).getTime()
}

// 业务目的：给“推荐 / 最新”标签提供统一的数据出口，先模拟后端列表接口返回。
// 输入输出：输入当前选中的标签 key，输出对应顺序的热点数组。
// 关键逻辑：推荐按热度降序展示，最新按发布时间倒序展示。
const displayedHotspots = computed(() => {
  const hotspotList = [...aiHotspots.value]

  if (activeFeedTab.value === 'latest') {
    return hotspotList.sort((left, right) => parsePublishedAt(right.publishedAt) - parsePublishedAt(left.publishedAt))
  }

  return hotspotList
    .filter((item) => item.isRecommended)
    .sort((left, right) => right.heat - left.heat)
})

const displayedRankArticles = computed(() => {
  return rankingGroups.value[rankingPage.value] || []
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
  if (!rankingGroups.value.length) {
    return
  }
  rankingPage.value = (rankingPage.value + 1) % rankingGroups.value.length
}

// 业务目的：把浏览人数统一格式化为 k 单位，贴近资讯流列表的阅读数据显示方式。
// 输入输出：输入原始浏览人数数字，输出带 k 后缀的展示字符串。
// 关键逻辑：即使不足 1000 也保留一位小数，保证整列数据的视觉格式一致。
function formatViewCountInK(value) {
  const count = Number(value || 0)

  return `${(count / 1000).toFixed(1)}k`
}

function syncCurrentTime() {
  now.value = new Date()
}

onMounted(async () => {
  // 业务目的：页面初始化时并行加载 AI 热点流和右侧文章榜数据，统一切换到后端服务。
  // 业务逻辑：两个接口互不依赖，先并发请求，再一次性回填响应式状态，减少首屏等待。
  try {
    const [remoteHotspots, remoteArticles] = await Promise.all([getAiHotspots(), getTechArticles()])
    aiHotspots.value = remoteHotspots
    techArticles.value = remoteArticles
  } catch {
    aiHotspots.value = []
    techArticles.value = []
  }
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
