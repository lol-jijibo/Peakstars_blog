<template>
  <div class="home-page">
    <blog-mega-header current-page="home" />

    <main class="homepage-main">
      <section class="home-hero" aria-labelledby="home-title">
        <div class="hero-left">
          <h1 id="home-title">Peakstars 技术论坛</h1>
          <div class="hero-lead" aria-live="polite">
            <transition name="hero-lead-fade" mode="out-in">
              <p :key="activeHeroLead" class="hero-lead-line">{{ activeHeroLead }}</p>
            </transition>
          </div>

          <div class="hero-actions">
            <button class="hero-primary-btn" type="button" @click="goInterview">
              进入面经页
            </button>
          </div>
        </div>

        <div class="hero-right" aria-label="当今技术迭代动态展示">
          <div class="tech-orbit" aria-hidden="true"></div>

          <div class="trend-wheel" aria-label="技术演进轮播控制">
            <button
              v-for="(trend, index) in techTrends"
              :key="trend.title"
              class="trend-wheel-node"
              :class="{ active: index === activeTrendIndex }"
              type="button"
              @click="selectTrend(index)"
            >
              <span>{{ trend.phase }}</span>
              <strong>{{ trend.short }}</strong>
            </button>
          </div>

          <div class="tech-stage">
            <transition name="tech-slide" mode="out-in">
              <article :key="activeTrend.title" class="tech-trend">
                <span>{{ activeTrend.phase }}</span>
                <h2>{{ activeTrend.title }}</h2>
                <p>{{ activeTrend.description }}</p>
              </article>
            </transition>
          </div>
        </div>
      </section>

      <learning-route-section />
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import BlogMegaHeader from '@/components/BlogMegaHeader.vue'
import LearningRouteSection from '@/components/LearningRouteSection.vue'

const router = useRouter()
const activeHeroLeadIndex = ref(0)
const activeTrendIndex = ref(0)
let heroLeadTimer = null
let trendTimer = null

const heroLeadLines = [
  '把每一次重构，都写成下一次出发的星图。',
  '在代码的暗处点灯，让复杂慢慢有了边界。',
  '把灵感编译成路径，把路径沉淀成作品。',
  '每天向前一点点，技术就会长成星河。',
  '让问题开口，让答案在实践里发光。'
]

const techTrends = [
  {
    phase: '01 / Model',
    short: '模型升级',
    title: '大模型人性化',
    description: 'Gemini 3.1 Pro -> GPT-4o -> GPT 5.4 -> ChatGPT Images 2.0'
  },
  {
    phase: '02 / Agent',
    short: 'Agent 落地',
    title: 'Agent 正在重塑软件工作流',
    description: '从单次问答到可规划、可调用工具、可验证结果的任务执行。'
  },
  {
    phase: '03 / Engineering',
    short: '工程融合',
    title: 'vibecoding正重塑软开全流程',
    description: 'Prompt、RAG、工具调用、评测体系和前后端协作正在融合。'
  },
  {
    phase: '04 / Frontend',
    short: '前端演进',
    title: '前端从界面开发走向智能编排',
    description: '组件、状态、数据流和 AI 交互将一起构成体验层。'
  }
]

const activeHeroLead = computed(() => heroLeadLines[activeHeroLeadIndex.value])
const activeTrend = computed(() => techTrends[activeTrendIndex.value])

function goInterview() {
  router.push('/interview')
}

function selectTrend(index) {
  activeTrendIndex.value = index
  restartTrendTimer()
}

function showNextTrend() {
  activeTrendIndex.value = (activeTrendIndex.value + 1) % techTrends.length
}

function showNextHeroLead() {
  activeHeroLeadIndex.value = (activeHeroLeadIndex.value + 1) % heroLeadLines.length
}

function restartHeroLeadTimer() {
  if (heroLeadTimer) {
    window.clearInterval(heroLeadTimer)
  }
  heroLeadTimer = window.setInterval(showNextHeroLead, 3200)
}

function restartTrendTimer() {
  if (trendTimer) {
    window.clearInterval(trendTimer)
  }
  trendTimer = window.setInterval(showNextTrend, 3600)
}

onMounted(() => {
  restartHeroLeadTimer()
  restartTrendTimer()
})

onBeforeUnmount(() => {
  if (heroLeadTimer) {
    window.clearInterval(heroLeadTimer)
  }
  if (trendTimer) {
    window.clearInterval(trendTimer)
  }
})
</script>

<style scoped src="../styles/views/HomeView.css"></style>
