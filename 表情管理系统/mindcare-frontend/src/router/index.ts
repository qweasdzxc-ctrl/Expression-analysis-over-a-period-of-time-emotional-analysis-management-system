import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import AdminView from '../views/AdminView.vue'
import CommunityView from '../views/CommunityView.vue'
import PostDetailView from '../views/PostDetailView.vue'
import ProfileView from '../views/ProfileView.vue'
import ChatView from '../views/ChatView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue')
    },
    {
      path: '/user/:username',
      name: 'userHome',
      component: () => import('../views/HomeView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/user/:username/face-capture',
      name: 'faceCapture',
      component: () => import('../views/FaceCaptureView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/user/:username/counseling',
      name: 'counseling',
      component: () => import('../views/CounselingView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/user/:username/community',
      name: 'community',
      component: () => import('../views/CommunityView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/user/:username/chat',
      name: 'chat',
      component: () => import('../views/ChatView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/user/:username/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/',
      redirect: to => {
        const authStore = useAuthStore()
        if (authStore.currentUser?.username) {
          return `/user/${authStore.currentUser.username}`
        }
        return '/login'
      }
    },
    {
      path: '/post/:id',
      name: 'post-detail',
      component: PostDetailView
    },
    {
      path: '/admin/:username',
      name: 'adminHome',
      component: () => import('../views/AdminView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        {
          path: '',
          name: 'adminDashboard',
          component: () => import('../views/AdminDashboardView.vue')
        },
        {
          path: 'manage',
          name: 'adminManage',
          component: () => import('../views/AdminManageView.vue')
        },
        {
          path: 'posts',
          name: 'adminPosts',
          component: () => import('../views/AdminPostsView.vue')
        },
        {
          path: 'comments',
          name: 'adminComments',
          component: () => import('../views/AdminCommentsView.vue')
        }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  // 验证登录状态
  if (!authStore.currentUser && authStore.sessionId) {
    authStore.logout()
    next('/login')
    return
  }

  // 处理根路径
  if (to.path === '/') {
    if (authStore.isAuthenticated && authStore.currentUser) {
      next(`/${authStore.isAdmin ? 'admin' : 'user'}/${authStore.currentUser.username}`)
    } else {
      next('/login')
    }
    return
  }

  // 处理登录页
  if (to.path === '/login') {
    if (authStore.isAuthenticated && authStore.currentUser) {
      next(`/${authStore.isAdmin ? 'admin' : 'user'}/${authStore.currentUser.username}`)
      return
    }
  }

  // 需要认证的页面
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
    return
  }

  // 需要管理员权限的页面
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next('/')
    return
  }

  next()
})

export default router 