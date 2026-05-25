import { createRouter, createWebHashHistory } from 'vue-router'
import { isAdminUser, isAuthenticated, verifyAdminAccess } from '@/stores/auth'
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
    path: '/articles/:id',
    component: () => import('@/views/TechArticleDetail.vue'),
    meta: { title: '技术文章详情', requiresAuth: true }
  },
  {
    path: '/world',
    component: () => import('@/modules/world/WorldNewsView.vue'),
    meta: { title: '看天下', requiresAuth: true, animation: 'page-blur-in' }
  },
  {
    path: '/book/:id',
    component: () => import('@/modules/world/BookReaderView.vue'),
    meta: { title: '书籍阅读', requiresAuth: true }
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
    meta: { title: '内容列表', requiresAuth: true, animation: 'page-blur-in' }
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
    redirect: '/admin/books-import',
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/books',
    redirect: '/admin/books-import',
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/:section(tech|interview|books-import|books-list|stats|comment)',
    component: () => import('@/modules/admin/views/AdminDashboardView.vue'),
    meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true }
  }
]

function redirectUnavailable() {
  return {
    path: '/home',
    query: { notice: 'unavailable' }
  }
}

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }

    if (to.path.match(/^\/articles\/\d+$/)) {
      return { top: 0 }
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

router.beforeEach(async (to) => {
  if (to.meta.requiresAuth && !isAuthenticated()) {
    stopEntryTransitionLoader()
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  if (to.meta.requiresAdmin && !isAdminUser()) {
    stopEntryTransitionLoader()
    return redirectUnavailable()
  }

  if (to.meta.requiresAdmin) {
    const verified = await verifyAdminAccess()
    if (!verified) {
      stopEntryTransitionLoader()
      return redirectUnavailable()
    }
  }

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
