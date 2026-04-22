<template>
  <div class="home-page">
    <blog-mega-header current-page="home" />

    <main class="homepage-main">
      <section class="hero-section">
        <div class="hero-copy">
          <span class="hero-kicker">Blog Home</span>
          <h1>PeakStars 的技术博客主页</h1>
          <p>
            面经已经从首页中拆分为独立页面，主页现在只负责承接博客的整体内容结构。
            你后面可以继续在这里扩展项目复盘、技术专栏、学习路线和工具资源，不再让面经占满整个首页。
          </p>

          <div class="hero-actions">
            <button class="hero-primary-btn" type="button" @click="goInterview">
              进入面经页
            </button>
            <button class="hero-secondary-btn" type="button" @click="goCollect">
              查看收藏模块
            </button>
          </div>
        </div>

        <div class="hero-metrics">
          <article class="hero-metric-card">
            <span>主模块</span>
            <strong>独立面经页</strong>
            <p>面经作为博客的核心内容区，被单独拆成一页，方便持续扩充和深度浏览。</p>
          </article>
          <article class="hero-metric-card">
            <span>次模块</span>
            <strong>收藏 / 喜欢</strong>
            <p>通过顶部超级菜单进入，适合作为个人内容管理和回看入口。</p>
          </article>
          <article class="hero-metric-card">
            <span>主页职责</span>
            <strong>承载博客结构</strong>
            <p>首页现在专注于展示博客总体布局，后面新增任何模块都能自然继续往下接。</p>
          </article>
        </div>
      </section>

      <section class="module-overview">
        <div class="module-overview-head">
          <span class="section-kicker">Content Matrix</span>
          <h2>博客内容矩阵</h2>
          <p>主页不再直接显示面经列表，而是承接模块导航与后续内容扩展。</p>
        </div>

        <div class="module-overview-grid">
          <button
            v-for="card in moduleCards"
            :key="card.title"
            class="module-overview-card"
            type="button"
            @click="handleModuleCard(card)"
          >
            <div class="module-overview-top">
              <span class="module-overview-icon">{{ card.icon }}</span>
              <span class="module-overview-tag">{{ card.tag }}</span>
            </div>
            <strong>{{ card.title }}</strong>
            <p>{{ card.description }}</p>
          </button>
        </div>
      </section>

      <section id="future" class="future-section">
        <div class="section-header">
          <div>
            <span class="section-kicker">Future Modules</span>
            <h2>首页后续可扩展模块</h2>
            <p>这里已经为后面的博客展示位留好空间，你可以继续往下添加真实业务模块。</p>
          </div>
        </div>

        <div class="future-grid">
          <article v-for="future in futureModules" :key="future.title" class="future-card">
            <strong>{{ future.title }}</strong>
            <p>{{ future.description }}</p>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import BlogMegaHeader from '@/components/BlogMegaHeader.vue'

const router = useRouter()

const moduleCards = [
  {
    icon: '📚',
    tag: '主模块',
    title: '面经页',
    description: '面经从首页中拆出后，作为独立页面承担完整内容浏览和分类切换。',
    action: 'route',
    path: '/interview'
  },
  {
    icon: '⭐',
    tag: '次模块',
    title: '收藏',
    description: '从超级菜单快速进入，承接你后续想二次回看的内容。',
    action: 'route',
    path: '/collect'
  },
  {
    icon: '❤️',
    tag: '次模块',
    title: '喜欢',
    description: '作为轻量偏好模块，和收藏一起放在次级导航中。',
    action: 'route',
    path: '/like'
  }
]

const futureModules = [
  { title: '项目复盘区', description: '后面新增项目经历、技术选型与上线总结时，可以直接接在这里。' },
  { title: '专题研究区', description: '适合放 AI、前端工程化、后端架构等系列文章。' },
  { title: '工具与模板区', description: '整理常用脚手架、Prompt、工作流模板等沉淀内容。' }
]

function goInterview() {
  router.push('/interview')
}

function goCollect() {
  router.push('/collect')
}

function handleModuleCard(card) {
  router.push(card.path)
}
</script>

<style scoped src="../styles/views/HomeView.css"></style>
