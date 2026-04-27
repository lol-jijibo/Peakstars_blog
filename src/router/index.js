import { createRouter, createWebHashHistory } from 'vue-router'
import { isAuthenticated } from '@/stores/auth'
import {
  finishEntryTransitionLoader,
  startEntryTransitionLoader,
  stopEntryTransitionLoader
} from '@/stores/entryLoader'

function resolveLoginRedirect(query) {
  return typeof query.redirect === 'string' ? query.redirect : '/home'
}

const routes = [
  {
    path: '/',
    redirect: () => (isAuthenticated() ? '/home' : '/login')
  },
  {
    path: '/home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '博客首页', requiresAuth: true }
  },
  {
    path: '/articles',
    component: () => import('@/views/TechArticleList.vue'),
    meta: { title: '技术文章', requiresAuth: true }
  },
  {
    path: '/world',
    component: () => import('@/modules/world/WorldNewsView.vue'),
    meta: { title: '看天下', requiresAuth: true }
  },
  {
    path: '/ai-hotspot',
    component: () => import('@/modules/ai/AiHotspotView.vue'),
    meta: { title: 'AI 热点', requiresAuth: true }
  },
  {
    path: '/auth',
    redirect: '/login'
  },
  {
    path: '/login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/interview',
    component: () => import('@/views/InterviewList.vue'),
    meta: { title: '内容列表', requiresAuth: true }
  },
  {
    path: '/interview/:id',
    component: () => import('@/views/InterviewDetail.vue'),
    meta: { title: '面经详情', requiresAuth: true }
  },
  {
    path: '/learning-route/:slug',
    component: () => import('@/views/LearningRouteDetail.vue'),
    meta: { title: '学习路线详情', requiresAuth: true }
  },
  {
    path: '/collect',
    component: () => import('@/views/Collect.vue'),
    meta: { title: '我的收藏', requiresAuth: true }
  },
  {
    path: '/like',
    component: () => import('@/views/Like.vue'),
    meta: { title: '我的点赞', requiresAuth: true }
  },
  {
    path: '/mine',
    component: () => import('@/views/Mine.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },
  {
    path: '/admin',
    component: () => import('@/modules/admin/views/AdminDashboardView.vue'),
    meta: { title: '管理后台', requiresAuth: false }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }

    if (to.hash) {
      return {
        el: to.hash,
        behavior: 'smooth'
      }
    }

    return { top: 0 }
  }
})

router.beforeEach((to) => {
  // 业务目的：对需要登录的页面做统一鉴权，避免未登录状态直接进入用户内容页。
  // 业务逻辑：管理后台当前开放直达，其余业务页仍通过 token 状态决定是否跳转登录页。
  if (to.meta.requiresAuth && !isAuthenticated()) {
    stopEntryTransitionLoader()
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  // 业务目的：已登录用户再次访问登录页时自动回到业务页，减少重复登录操作。
  // 业务逻辑：优先读取 redirect 参数，没有则回到首页，并在跳转前展示统一过渡反馈。
  if (to.path === '/login' && isAuthenticated()) {
    const redirect = resolveLoginRedirect(to.query)

    startEntryTransitionLoader({
      title: '正在进入 PeakStars_blog',
      subtitle: '已检测到登录状态，正在为你打开目标页面。'
    })

    return redirect
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} - PeakStars_blog` : 'PeakStars_blog'
  finishEntryTransitionLoader()
})

router.onError(() => {
  stopEntryTransitionLoader()
})

export default router
