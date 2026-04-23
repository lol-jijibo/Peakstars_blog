import { createRouter, createWebHashHistory } from 'vue-router'
import { isAuthenticated } from '@/stores/auth'

const routes = [
  {
    path: '/',
    redirect: () => (isAuthenticated() ? '/home' : '/login')
  },
  {
    path: '/home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '博客主页', requiresAuth: true }
  },
  {
    path: '/auth',
    redirect: '/login'
  },
  {
    path: '/login',
    component: () => import('@/views/LoginView.vue'), //统一路由懒加载(按需加载)
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
    meta: { title: '个人中心', requiresAuth: true } //自己定义的额外属性
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  // 情况1：需要登录但未登录 → 重定向到登录页
  // to = 要去的那个页面
  if (to.meta.requiresAuth && !isAuthenticated()) {
    return {
      path: '/login',
      query: { redirect: to.fullPath } // 记录原始目标，登录后跳转回去
    }
  }

  // 情况2：已登录却访问登录页 → 跳转到首页或redirect参数指定的页面
  if (to.path === '/login' && isAuthenticated()) {
    return typeof to.query.redirect === 'string' ? to.query.redirect : '/home'
  }

  return true // 其他情况，直接放行
})

//路由后置守卫：在每次路由切换后执行
router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} - PeakStars_blog` : 'PeakStars_blog'
})

export default router
