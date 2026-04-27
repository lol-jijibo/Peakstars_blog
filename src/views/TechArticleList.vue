<template>
  <div class="article-hub-page">
    <blog-mega-header current-page="article" />

    <main class="article-hub-main">
      <div class="article-hub-layout">
        <aside class="article-filter-side">
          <section class="article-filter-panel">
            <div class="article-filter-panel-head">
              <span class="article-filter-kicker">Reading Mode</span>
              <h2>文章导航</h2>
            </div>

            <div class="article-mode-tabs">
              <button
                v-for="mode in articleModes"
                :key="mode.key"
                class="article-mode-tab"
                :class="{ active: activeModeKey === mode.key }"
                type="button"
                @click="selectMode(mode.key)"
              >
                <span class="article-mode-dot"></span>
                <span>{{ mode.label }}</span>
              </button>
            </div>
          </section>

          <section class="article-filter-panel article-filter-panel--topics">
            <div class="article-filter-panel-head">
              <span class="article-filter-kicker">Topics</span>
              <h2>分类</h2>
            </div>

            <div class="article-topic-strip">
              <button
                v-for="category in topicCategories"
                :key="category.key"
                class="article-topic-item"
                :class="{ active: activeCategoryKey === category.key }"
                type="button"
                @click="selectCategory(category.key)"
              >
                <span>{{ category.label }}</span>
              </button>
            </div>
          </section>
        </aside>

        <section class="article-feed">
          <div class="article-feed-head">
            <div>
              <h1>{{ currentHeading }}</h1>
              <p>{{ currentDescription }}</p>
            </div>

            <div class="article-feed-meta">
              <span>{{ filteredArticles.length }} 篇文章</span>
              <span>{{ featuredCount }} 篇精选</span>
            </div>
          </div>

          <div class="article-feed-list">
            <article
              v-for="article in filteredArticles"
              :key="article.id"
              class="article-feed-item"
            >
              <div class="article-feed-main">
                <div class="article-author-row">
                  <div
                    class="article-author-avatar"
                    :style="{ background: article.author.accent }"
                    aria-hidden="true"
                  >
                    {{ article.author.initials }}
                  </div>

                  <div class="article-author-copy">
                    <strong>{{ article.author.name }}</strong>
                    <span>{{ article.author.role }}</span>
                  </div>
                </div>

                <h2>{{ article.title }}</h2>
                <p class="article-feed-summary">{{ article.summary }}</p>

                <div class="article-feed-meta-line">
                  <span>阅读 {{ formatCount(article.readCount) }}</span>
                  <span>点赞 {{ formatCount(article.likeCount) }}</span>
                  <span>收藏 {{ formatCount(article.collectCount) }}</span>
                  <span>{{ article.readTime }}</span>
                </div>
              </div>

              <div v-if="showCover(article)" class="article-feed-cover">
                <img :src="article.coverUrl" :alt="article.title" loading="lazy" />
              </div>
            </article>

            <div v-if="filteredArticles.length === 0" class="article-empty-state">
              <strong>当前筛选下还没有文章。</strong>
              <p>换一个分类继续看，精选内容已经收拢到其他入口里了。</p>
            </div>
          </div>
        </section>

        <aside class="article-side">
          <div class="article-side-head">
            <h2>作者推荐</h2>
          </div>

          <div class="article-author-list">
            <article
              v-for="author in recommendedAuthorCards"
              :key="author.id"
              class="article-author-item"
            >
              <div class="article-author-item-main">
                <div class="article-author-avatar-wrap">
                  <img
                    v-if="author.avatarUrl"
                    class="article-author-avatar article-author-avatar--recommend article-author-avatar-image"
                    :src="author.avatarUrl"
                    :alt="author.name"
                    loading="lazy"
                  />
                  <div
                    v-else
                    class="article-author-avatar article-author-avatar--recommend"
                    :style="{ background: author.accent }"
                    aria-hidden="true"
                  >
                    {{ author.initials }}
                  </div>
                  <span class="article-author-badge" aria-hidden="true">
                    <span class="article-author-badge-star">✦</span>
                  </span>
                </div>

                <div class="article-author-item-copy">
                  <strong>{{ author.name }}</strong>
                  <p>{{ author.subtitle }}</p>
                </div>
              </div>
              <button class="article-follow-btn" type="button">关注</button>
            </article>
          </div>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTechArticles } from '@/api/content'
import BlogMegaHeader from '@/components/BlogMegaHeader.vue'
import { recommendedAuthors } from '@/data/techArticles'

const route = useRoute()
const router = useRouter()
const techArticles = ref([])

const articleModes = [
  { key: 'recommend', label: '推荐' },
  { key: 'collect', label: '收藏夹' },
  { key: 'vip', label: 'VIP文章' }
]

const topicCategories = [
  { key: 'all', label: '全部' },
  { key: 'frontend', label: '前端' },
  { key: 'backend', label: '后端' },
  { key: 'history', label: '历史' },
  { key: 'collect', label: '收藏' },
  { key: 'like', label: '喜欢' }
]

const activeModeKey = ref(resolveModeKey(route.query.mode))
const activeCategoryKey = ref(resolveCategoryKey(route.query.category))

const currentHeading = computed(() => {
  if (activeModeKey.value === 'vip') {
    return 'VIP 技术文章'
  }

  if (activeModeKey.value === 'collect') {
    return '收藏文章'
  }

  return `${resolveCategoryLabel(activeCategoryKey.value)}文章`
})

const currentDescription = computed(() => {
  if (activeModeKey.value === 'vip') {
    return '深度复盘、架构拆解和更完整的工程经验，统一收在这里。'
  }

  if (activeModeKey.value === 'collect') {
    return '你已经留下来的高价值内容，适合反复回看。'
  }

  return resolveCategoryDescription(activeCategoryKey.value)
})

const filteredArticles = computed(() => {
  const baseList = techArticles.value.filter((article) => matchesMode(article, activeModeKey.value))
  const categoryList = baseList.filter((article) => matchesCategory(article, activeCategoryKey.value))

  return [...categoryList].sort((left, right) => {
    if (Number(right.featured) !== Number(left.featured)) {
      return Number(right.featured) - Number(left.featured)
    }

    return right.publishedAt.localeCompare(left.publishedAt)
  })
})

const featuredCount = computed(() => filteredArticles.value.filter((article) => article.featured).length)
//  作者头像集
const authorAvatarMap = {
  fly: '/qq.jpg',
  francek: encodeURI('/【哲风壁纸】夏日-晴天-氛围感.png'),
  'd-life': encodeURI('/ChatGPT Image 2026年4月23日 18_37_46.png'),
  'clean-code': encodeURI('/【哲风壁纸】xiaomiyu7-小米suv.png'),
  'algo-art': encodeURI('/【哲风壁纸】云彩-夜晚-夜景.png'),
  'free-dev': encodeURI('/【哲风壁纸】公路-后视镜-城镇.png')
}

const recommendedAuthorCards = computed(() =>
  recommendedAuthors.map((author) => ({
    ...author,
    avatarUrl: authorAvatarMap[author.id] || ''
  }))
)

// 业务目的：在页面挂载时切换到后端技术文章数据源，替换原本的本地静态数组。
// 业务逻辑：接口结果会写入本地响应式状态，页面筛选、排序和头部统计继续复用原有计算属性。
onMounted(async () => {
  try {
    techArticles.value = await getTechArticles()
  } catch {
    techArticles.value = []
  }
})

watch(
  () => route.query.mode,
  (nextMode) => {
    activeModeKey.value = resolveModeKey(nextMode)
  }
)

watch(
  () => route.query.category,
  (nextCategory) => {
    activeCategoryKey.value = resolveCategoryKey(nextCategory)
  }
)

watch([activeModeKey, activeCategoryKey], ([modeKey, categoryKey]) => {
  const query = {}

  if (modeKey !== 'recommend') {
    query.mode = modeKey
  }

  if (categoryKey !== 'all') {
    query.category = categoryKey
  }

  if (
    String(route.query.mode || 'recommend') !== modeKey ||
    String(route.query.category || 'all') !== categoryKey
  ) {
    router.replace({ path: '/articles', query })
  }
})

function resolveModeKey(mode) {
  return articleModes.some((item) => item.key === mode) ? mode : 'recommend'
}

function resolveCategoryKey(category) {
  return topicCategories.some((item) => item.key === category) ? category : 'all'
}

function matchesMode(article, modeKey) {
  if (modeKey === 'vip') {
    return article.isVip
  }

  if (modeKey === 'collect') {
    return article.isCollected
  }

  return true
}

function matchesCategory(article, categoryKey) {
  if (categoryKey === 'all') {
    return true
  }

  if (categoryKey === 'history') {
    return article.inHistory
  }

  if (categoryKey === 'collect') {
    return article.isCollected
  }

  if (categoryKey === 'like') {
    return article.isLiked
  }

  return article.category === categoryKey
}

function selectMode(modeKey) {
  activeModeKey.value = modeKey
}

function selectCategory(categoryKey) {
  activeCategoryKey.value = categoryKey
}

function resolveCategoryLabel(categoryKey) {
  return topicCategories.find((item) => item.key === categoryKey)?.label || '技术'
}

function resolveCategoryDescription(categoryKey) {
  if (categoryKey === 'frontend') {
    return '聚焦 Vue、交互体验、组件拆分和前端工程细节。'
  }

  if (categoryKey === 'backend') {
    return '围绕接口设计、系统拆分、稳定性和后端演进展开。'
  }

  if (categoryKey === 'history') {
    return '继续阅读你最近浏览过的文章。'
  }

  if (categoryKey === 'collect') {
    return '把留下来的重点内容重新聚到一起。'
  }

  if (categoryKey === 'like') {
    return '这里是你更偏爱的技术主题和表达方式。'
  }

  return '按阅读流展示最近值得点开的技术文章。'
}

function showCover(article) {
  return article.category === 'backend' || article.featured
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
</script>

<style scoped src="../styles/views/TechArticleList.css"></style>
