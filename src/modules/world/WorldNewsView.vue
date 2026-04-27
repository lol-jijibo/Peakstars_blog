<template>
  <div class="world-news-page">
    <blog-mega-header current-page="world" />

    <main class="world-news-main">
      <section class="world-news-intro" aria-labelledby="world-news-page-title">
        <h1 id="world-news-page-title">看天下</h1>
        <p>
          《看天下》是一本综合类新闻杂志，创刊于2005年，也是经全球华文媒体BPA认证国内发行量最大的新闻期刊。内容丰富多元，包含时政、财经、科技、文化、娱乐、教育、心理等多个领域。立志于为读者展现更广阔的世界和人生的更多可能。
        </p>
      </section>

      <section class="world-news-list" aria-label="看天下期刊列表">
        <article
          v-for="issue in worldNews"
          :key="issue.id"
          class="world-news-item"
        >
          <div class="world-news-cover-frame" :style="{ '--cover-accent': issue.coverAccent }">
            <div class="world-news-cover">
              <div class="world-news-cover-top">
                <span class="world-news-cover-brand">看天下</span>
                <span class="world-news-cover-issue">{{ issue.issueLabel }}</span>
              </div>
              <div class="world-news-cover-body">
                <span class="world-news-cover-kicker">{{ issue.coverKicker }}</span>
                <strong>{{ issue.coverHeadline }}</strong>
                <p>{{ issue.coverSummary }}</p>
              </div>
              <div class="world-news-cover-footer">
                <span>{{ issue.coverFooter }}</span>
              </div>
            </div>
          </div>

          <div class="world-news-copy">
            <h2>{{ issue.title }}</h2>
            <p class="world-news-category">{{ issue.category }}</p>

            <div class="world-news-meta">
              <span class="world-news-meta-strong">{{ issue.todayReads }}</span>
              <span>人今日阅读</span>
              <span class="world-news-divider">|</span>
              <span>推荐值</span>
              <span class="world-news-meta-strong">{{ issue.recommendation }}%</span>
            </div>

            <p class="world-news-description">{{ issue.description }}</p>
          </div>
        </article>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getWorldNews } from '@/api/content'
import BlogMegaHeader from '@/components/BlogMegaHeader.vue'

const worldNews = ref([])

// 业务目的：把看天下页面切换为后端期刊数据源，后续新增期刊只需要更新数据库。
// 业务逻辑：页面仍然保持原有字段渲染结构，接口返回的数据直接覆盖本地列表状态。
onMounted(async () => {
  try {
    worldNews.value = await getWorldNews()
  } catch {
    worldNews.value = []
  }
})
</script>

<style src="./WorldNewsView.css"></style>
