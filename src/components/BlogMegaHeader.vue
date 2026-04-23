<template>
  <header class="site-header">
    <div class="site-header-inner">
      <button class="site-brand" type="button" @click="goHome">
        <div class="site-brand-mark">PS</div>
        <div class="site-brand-copy">
          <span class="site-brand-name">PeakStars_blog</span>
          <span class="site-brand-sub">技术博客与面经论坛</span>
        </div>
      </button>

      <nav class="site-nav" @mouseleave="closeMegaMenu">
        <button
          v-for="nav in topNavs"
          :key="nav.key"
          class="site-nav-link"
          :class="{ active: isActiveNav(nav) || activeMegaMenu === nav.key }"
          type="button"
          @mouseenter="handleNavEnter(nav)"
          @focus="handleNavEnter(nav)"
          @click="handleNavClick(nav)"
        >
          {{ nav.label }}
        </button>

        <transition name="mega-menu-fade">
          <div
            v-if="activeMegaMenu"
            class="mega-menu"
            @mouseenter="activeMegaMenu = activeMegaMenu"
          >
            <aside class="mega-menu-side">
              <button
                v-for="group in megaMenuGroups"
                :key="group.key"
                class="mega-menu-side-link"
                :class="{ active: activeMegaGroup === group.key }"
                type="button"
                @mouseenter="activeMegaGroup = group.key"
                @focus="activeMegaGroup = group.key"
              >
                <span>{{ group.label }}</span>
                <small>{{ group.caption }}</small>
              </button>
            </aside>

            <div class="mega-menu-panel">
              <div class="mega-menu-panel-head">
                <div>
                  <div class="mega-menu-eyebrow">{{ currentMegaGroup.label }}</div>
                  <h3>{{ currentMegaGroup.title }}</h3>
                </div>
                <p>{{ currentMegaGroup.description }}</p>
              </div>

              <div class="mega-menu-grid">
                <button
                  v-for="item in currentMegaGroup.items"
                  :key="item.title"
                  class="mega-menu-card"
                  type="button"
                  @click="handleMegaMenuAction(item)"
                >
                  <div class="mega-menu-card-top">
                    <span class="mega-menu-card-icon">{{ item.icon }}</span>
                    <span v-if="item.badge" class="mega-menu-card-badge">{{ item.badge }}</span>
                  </div>
                  <strong>{{ item.title }}</strong>
                  <p>{{ item.description }}</p>
                </button>
              </div>
            </div>
          </div>
        </transition>
      </nav>

      <div class="site-header-actions">
        <button
          v-if="currentPage === 'interview'"
          class="header-ghost-btn"
          type="button"
          @click="goHome"
        >
          返回主页
        </button>
        <button class="header-primary-btn" type="button" @click="logout">
          退出登录
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  currentPage: {
    type: String,
    default: 'home'
  }
})

const router = useRouter()
const authStore = useAuthStore()
const activeMegaMenu = ref('')
const activeMegaGroup = ref('featured')

const topNavs = [
  { key: 'home', label: '主页', type: 'route', path: '/home' },
  { key: 'interview', label: '面经', type: 'route', path: '/interview' },
  { key: 'favorites', label: '收藏与喜欢', type: 'mega' }
]

const megaMenuGroups = [
  {
    key: 'featured',
    label: '主模块',
    caption: '面经',
    title: '面经模块是当前博客的主内容入口',
    description: '面经从博客主页中拆分为独立页面，作为核心内容区长期沉淀前端与后端求职经验。',
    items: [
      { icon: '📚', title: '全部面经', description: '跳转到完整面经页，浏览全部整理内容。', action: 'route', path: '/interview', badge: 'Primary' },
      { icon: '🧭', title: '前端面经', description: '直接进入前端分类，查看前端方向专题内容。', action: 'route', path: '/interview', query: { category: 'frontend' } },
      { icon: '⚙️', title: 'Java 后端面经', description: '直接进入 Java 分类，查看后端专题内容。', action: 'route', path: '/interview', query: { category: 'java' } }
    ]
  },
  {
    key: 'secondary',
    label: '次模块',
    caption: '收藏 / 喜欢',
    title: '收藏与喜欢作为次模块统一收纳',
    description: '参考超级下拉菜单模式，把次级功能放进顶部导航，减少主页信息拥挤感。',
    items: [
      { icon: '⭐', title: '收藏夹', description: '查看你标记收藏的内容，作为二次回看入口。', action: 'route', path: '/collect', badge: 'Secondary' },
      { icon: '❤️', title: '喜欢内容', description: '进入喜欢模块，查看点赞过的内容。', action: 'route', path: '/like' },
      { icon: '👤', title: '个人中心', description: '预留个人页入口，后续可以承接账号与偏好设置。', action: 'route', path: '/mine' }
    ]
  },
  {
    key: 'future',
    label: '预留模块',
    caption: '后续扩展',
    title: '主页已经为后续功能预留展示位',
    description: '后面可以继续补项目复盘、技术专栏、学习路线等内容，不再挤压面经区域。',
    items: [
      { icon: '🧪', title: '项目复盘', description: '承载项目总结、技术选型和上线复盘。', action: 'route', path: '/home#future' },
      { icon: '📝', title: '技术专栏', description: '沉淀体系化技术文章和专题内容。', action: 'route', path: '/home#future' },
      { icon: '🛠️', title: '工具资源', description: '整理工具链、模板与效率资源。', action: 'route', path: '/home#future' }
    ]
  }
]

const currentMegaGroup = computed(() => {
  return megaMenuGroups.find((group) => group.key === activeMegaGroup.value) || megaMenuGroups[0]
})

function isActiveNav(nav) {
  return (nav.key === 'home' && props.currentPage === 'home') || (nav.key === 'interview' && props.currentPage === 'interview')
}

function handleNavEnter(nav) {
  if (nav.type === 'mega') {
    activeMegaMenu.value = nav.key
    if (!activeMegaGroup.value) activeMegaGroup.value = 'featured'
  }
}

function handleNavClick(nav) {
  if (nav.type === 'route') {
    closeMegaMenu()
    router.push(nav.path)
    return
  }

  activeMegaMenu.value = activeMegaMenu.value === nav.key ? '' : nav.key
}

function handleMegaMenuAction(item) {
  closeMegaMenu()
  router.push({
    path: item.path,
    query: item.query || {}
  })
}

function closeMegaMenu() {
  activeMegaMenu.value = ''
}

function goHome() {
  closeMegaMenu()
  router.push('/home')
}

function goInterview() {
  closeMegaMenu()
  router.push('/interview')
}

function logout() {
  authStore.logout()
  router.replace('/login')
}
</script>

<style scoped src="../styles/components/BlogMegaHeader.css"></style>
