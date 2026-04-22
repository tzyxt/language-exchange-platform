<script setup>
import { useRoute, useRouter } from 'vue-router'
import { clearSession, sessionStore } from '../stores/session'

const router = useRouter()
const route = useRoute()

const adminNav = [
  { label: '控制台', path: '/admin' },
  { label: '用户管理', path: '/admin/users' },
  { label: '活动管理', path: '/admin/activities' },
  { label: '举报审核', path: '/admin/reports' }
]

function isNavActive(itemPath) {
  if (itemPath === '/admin') {
    return route.path === '/admin'
  }

  return route.path === itemPath || route.path.startsWith(itemPath + '/')
}

function logout() {
  clearSession()
  router.push('/admin/login')
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand" @click="router.push('/admin')">
        <div class="admin-brand-mark">ADM</div>
        <div>
          <strong>后台管理</strong>
          <span>Language Exchange Admin</span>
        </div>
      </div>

      <nav class="admin-nav">
        <button
          v-for="item in adminNav"
          :key="item.path"
          class="admin-nav-link"
          :class="{ active: isNavActive(item.path) }"
          @click="router.push(item.path)"
        >
          {{ item.label }}
        </button>
      </nav>
    </aside>

    <div class="admin-main-wrap">
      <header class="admin-topbar">
        <div>
          <h1>管理员后台</h1>
          <p>内容审核、用户管理与活动运营都在这里集中处理</p>
        </div>
        <div class="admin-user">
          <div class="admin-user-meta">
            <strong>{{ sessionStore.currentUser?.name }}</strong>
            <span>管理员</span>
          </div>
          <button class="ghost-button" @click="logout">退出后台</button>
        </div>
      </header>

      <main class="admin-main">
        <slot />
      </main>
    </div>
  </div>
</template>
