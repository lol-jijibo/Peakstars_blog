<template>
  <div class="app-container">
    <router-view v-slot="{ Component, route }">
      <transition
        :name="transitionName"
        mode="out-in"
        @after-enter="onAfterEnter"
      >
        <keep-alive :include="cachedViewNames">
          <component :is="Component" :key="route.path" />
        </keep-alive>
      </transition>
    </router-view>
    <forum-entry-loader
      :visible="entryTransitionLoaderState.visible"
      :kicker="entryTransitionLoaderState.kicker"
      :title="entryTransitionLoaderState.title"
      :subtitle="entryTransitionLoaderState.subtitle"
      :progress="entryTransitionLoaderState.progress"
      :status-text="entryTransitionLoaderState.statusText"
    />
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ForumEntryLoader from '@/components/ForumEntryLoader.vue'
import { entryTransitionLoaderState } from '@/stores/entryLoader'

// 需要被 keep-alive 缓存的组件名（须与组件内 name 选项一致）
const cachedViewNames = ['TechArticleList']

const route = useRoute()
const transitionName = ref('page-fade')

/**
 * 业务目的：统一识别技术文章详情页，确保每次进入详情都从顶部开始阅读。
 * 业务逻辑：通过文章详情路由正则判断，进入后直接覆盖任何残留滚动位置。
 */
function isArticleDetailPath(path) {
  return /^\/articles\/\d+$/.test(path)
}

/**
 * 监听路由变化，根据 from/to 的路径关系决定过渡方向：
 * - 列表 → 详情：使用柔和淡入，避免横向滑动打断阅读节奏
 * - 详情 → 列表：柔和淡入淡出返回
 * - 其他：使用 meta.animation 或默认 fade
 */
let previousPath = ''

watch(
  () => route.path,
  (toPath) => {
    const fromIsList = previousPath === '/articles'
    const toIsDetail = /^\/articles\/\d+$/.test(toPath)
    const fromIsDetail = /^\/articles\/\d+$/.test(previousPath)
    const toIsList = toPath === '/articles'

    if (toIsDetail || fromIsDetail) {
      transitionName.value = 'page-fade'
    } else {
      transitionName.value = route.meta.animation || 'page-fade'
    }

    previousPath = toPath
  },
  { immediate: true }
)

/**
 * 业务目的：在文章详情页进入完成后统一回到顶部，避免沿用上一页滚动位置。
 * 业务逻辑：列表页的滚动恢复改由列表组件自行维护，这里只兜底详情页首屏位置。
 */
function onAfterEnter(el) {
  nextTick(() => {
    const currentPath = window.location.hash.replace('#', '')

    if (isArticleDetailPath(currentPath)) {
      window.scrollTo(0, 0)
    }
  })
}
</script>

<style scoped src="./styles/App.css"></style>
