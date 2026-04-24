<template>
  <div class="interview-page">
    <blog-mega-header current-page="interview" />

    <main class="interview-main">
      <section class="interview-content">
        <div class="interview-module-body">
          <aside class="topic-sidebar">
            <div class="topic-basket">
              <div class="topic-basket-head">
                <h3>面试分类</h3>
              </div>

              <div class="topic-search">
                <input
                  v-model="keyword"
                  type="text"
                  placeholder="搜索面经..."
                  class="search-input"
                />
              </div>

              <div class="topic-groups">
                <section
                  v-for="group in topicGroups"
                  :key="group.key"
                  class="topic-group"
                >
                  <button
                    class="topic-group-title"
                    :class="{ active: activeGroup === group.key }"
                    type="button"
                    @click="selectGroup(group)"
                  >
                    <span>{{ group.label }}</span>
                    <span class="topic-group-arrow">></span>
                  </button>

                  <div class="topic-group-tags">
                    <button
                      v-for="topic in group.items"
                      :key="topic.key"
                      class="topic-pill"
                      :class="{ active: selectedTopic === topic.key }"
                      type="button"
                      @click="selectTopic(topic)"
                    >
                      {{ topic.label }}
                    </button>
                  </div>
                </section>
              </div>
            </div>
          </aside>

          <section class="interview-feed">
            <div class="interview-feed-head">
              <h2>面经帖子</h2>
              <p>当前展示各平台收集的面经帖子内容，包含个人心得和求职历程，欢迎查阅。</p>
            </div>

            <div v-if="loading" class="loading-tip">
              <span class="loading-spin"></span>
              <span>加载中...</span>
            </div>

            <div v-else-if="error" class="error-tip">
              <p>{{ error }}</p>
              <button class="retry-btn" type="button" @click="fetchList">重试</button>
            </div>

            <template v-else>
              <div class="interview-list">
                <interview-card
                  v-for="item in list"
                  :key="item.id"
                  :data="item"
                  @click="goDetail(item.id)"
                />
              </div>

              <div v-if="list.length === 0" class="empty-tip">
                <span>当前分类下还没有面经内容</span>
              </div>
            </template>
          </section>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getInterviews } from '@/api/interview.js'
import InterviewCard from '@/components/InterviewCard.vue'
import BlogMegaHeader from '@/components/BlogMegaHeader.vue'

const route = useRoute()
const router = useRouter()
const initialCategory = typeof route.query.category === 'string' ? route.query.category : 'all'

const keyword = ref('')
const activeCategory = ref(initialCategory)
const activeGroup = ref(resolveGroupKey(initialCategory))
const selectedTopic = ref(initialCategory)
const list = ref([])
const loading = ref(false)
const error = ref('')

const topicGroups = [
  {
    key: 'backend',
    label: '后端',
    category: 'java',
    defaultTopicKey: 'all',
    items: [
      { key: 'all', label: '全部方向', category: 'all' },
      { key: 'java', label: 'Java', category: 'java' },
      { key: 'python', label: 'Python', category: 'all' },
      { key: 'mysql', label: 'MySQL', category: 'java' },
      { key: 'redis', label: 'Redis', category: 'java' },
      { key: 'springboot', label: 'Spring Boot', category: 'java' },
      { key: 'jvm', label: 'JVM', category: 'java' },
      { key: 'network', label: '计算机网络', category: 'all' },
      { key: 'os', label: '操作系统', category: 'all' }
    ]
  },
  {
    key: 'frontend',
    label: '前端',
    category: 'frontend',
    defaultTopicKey: 'frontend',
    items: [
      { key: 'frontend', label: '前端总览', category: 'frontend' },
      { key: 'html', label: 'HTML', category: 'frontend' },
      { key: 'css', label: 'CSS', category: 'frontend' },
      { key: 'javascript', label: 'JavaScript', category: 'frontend' },
      { key: 'typescript', label: 'TypeScript', category: 'frontend' },
      { key: 'vue', label: 'Vue', category: 'frontend' },
      { key: 'react', label: 'React', category: 'frontend' }
    ]
  }
]

let debounceTimer = 0

function resolveGroupKey(category) {
  return category === 'frontend' ? 'frontend' : 'backend'
}

function syncCategoryState(category) {
  activeCategory.value = category
  selectedTopic.value = category
  activeGroup.value = resolveGroupKey(category)
}

async function fetchList() {
  loading.value = true
  error.value = ''

  try {
    const result = await getInterviews({
      category: activeCategory.value,
      keyword: keyword.value.trim()
    })
    list.value = result.list
  } catch {
    error.value = '数据加载失败，请检查后端服务是否已经启动'
    list.value = []
  } finally {
    loading.value = false
  }
}

function selectTopic(topic) {
  selectedTopic.value = topic.key
  activeCategory.value = topic.category
}

function selectGroup(group) {
  activeGroup.value = group.key
  selectedTopic.value = group.defaultTopicKey
  activeCategory.value = group.category
}

function goDetail(id) {
  router.push(`/interview/${id}`)
}

watch(activeCategory, () => {
  fetchList()
})

watch(keyword, () => {
  clearTimeout(debounceTimer)
  debounceTimer = window.setTimeout(fetchList, 500)
})

watch(
  () => route.query.category,
  (nextCategory) => {
    if (typeof nextCategory === 'string') {
      syncCategoryState(nextCategory)
      return
    }

    syncCategoryState('all')
  }
)

onMounted(() => {
  fetchList()
})

onBeforeUnmount(() => {
  clearTimeout(debounceTimer)
})
</script>

<style scoped src="../styles/views/InterviewList.css"></style>
