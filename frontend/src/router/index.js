import { createRouter, createWebHistory } from 'vue-router'
import { sessionStore } from '../stores/session'

const routes = [
  { path: '/', redirect: () => (sessionStore.currentUser?.role === 'ADMIN' ? '/admin' : '/home') },
  { path: '/login', component: () => import('../pages/LoginPage.vue'), meta: { guestOnly: true, layout: 'plain', audience: 'user' } },
  { path: '/register', component: () => import('../pages/RegisterPage.vue'), meta: { guestOnly: true, layout: 'plain', audience: 'user' } },
  { path: '/forgot-password', component: () => import('../pages/ForgotPasswordPage.vue'), meta: { guestOnly: true, layout: 'plain', audience: 'user' } },
  { path: '/home', component: () => import('../pages/HomePage.vue'), meta: { requiresAuth: true, layout: 'user' } },
  { path: '/profile', component: () => import('../pages/ProfilePage.vue'), meta: { requiresAuth: true, layout: 'user' } },
  { path: '/matches', component: () => import('../pages/MatchPage.vue'), meta: { requiresAuth: true, layout: 'user' } },
  { path: '/users/:id', component: () => import('../pages/UserDetailPage.vue'), meta: { requiresAuth: true, layout: 'user' } },
  { path: '/chat', component: () => import('../pages/ChatPage.vue'), meta: { requiresAuth: true, layout: 'user' } },
  { path: '/community', component: () => import('../pages/CommunityPage.vue'), meta: { requiresAuth: true, layout: 'user' } },
  { path: '/activities', component: () => import('../pages/ActivitiesPage.vue'), meta: { requiresAuth: true, layout: 'user' } },
  { path: '/activities/:id', component: () => import('../pages/ActivityDetailPage.vue'), meta: { requiresAuth: true, layout: 'user' } },
  { path: '/me', component: () => import('../pages/MyCenterPage.vue'), meta: { requiresAuth: true, layout: 'user' } },
  { path: '/admin/login', component: () => import('../pages/admin/AdminLoginPage.vue'), meta: { guestOnly: true, layout: 'plain', audience: 'admin' } },
  { path: '/admin', component: () => import('../pages/admin/AdminDashboardPage.vue'), meta: { requiresAuth: true, adminOnly: true, layout: 'admin' } },
  { path: '/admin/users', component: () => import('../pages/admin/AdminUsersPage.vue'), meta: { requiresAuth: true, adminOnly: true, layout: 'admin' } },
  { path: '/admin/activities', component: () => import('../pages/admin/AdminActivitiesPage.vue'), meta: { requiresAuth: true, adminOnly: true, layout: 'admin' } },
  { path: '/admin/reports', component: () => import('../pages/admin/AdminReportsPage.vue'), meta: { requiresAuth: true, adminOnly: true, layout: 'admin' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const user = sessionStore.currentUser

  if (to.meta.requiresAuth && !user) {
    return to.meta.adminOnly ? '/admin/login' : '/login'
  }

  if (to.meta.guestOnly && user) {
    if (to.meta.audience === 'admin') {
      return user.role === 'ADMIN' ? '/admin' : '/home'
    }

    return user.role === 'ADMIN' ? '/admin' : '/home'
  }

  if (to.meta.adminOnly && user?.role !== 'ADMIN') {
    return '/home'
  }

  return true
})

export default router
