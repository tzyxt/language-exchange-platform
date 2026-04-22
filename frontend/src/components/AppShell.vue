<script setup>
import { useRoute, useRouter } from 'vue-router'
import { clearSession, sessionStore } from '../stores/session'

const router = useRouter()
const route = useRoute()

const userNav = [
  { label: '首页', path: '/home' },
  { label: '智能匹配', path: '/matches' },
  { label: '聊天交流', path: '/chat' },
  { label: '社区分享', path: '/community' },
  { label: '活动广场', path: '/activities' },
  { label: '我的', path: '/me' }
]

function isNavActive(itemPath) {
  return route.path === itemPath || route.path.startsWith(itemPath + '/')
}

function logout() {
  clearSession()
  router.push('/login')
}
</script>

<template>
  <div class="shell">
    <header class="shell-header">
      <div class="shell-brand">
        <div class="brand-mark">LEP</div>
        <div class="brand-copy">
          <h1>语言互助与跨文化交流平台</h1>
          <p>Smart Matching for Campus Connection</p>
        </div>
      </div>
      <nav class="shell-nav">
        <button
          v-for="item in userNav"
          :key="item.path"
          class="nav-link"
          :class="{ active: isNavActive(item.path) }"
          @click="router.push(item.path)"
        >
          {{ item.label }}
        </button>
      </nav>
      <div class="shell-user">
        <div class="shell-user-meta">
          <strong>{{ sessionStore.currentUser?.name }}</strong>
          <span>普通用户</span>
        </div>
        <button class="ghost-button" @click="logout">退出</button>
      </div>
    </header>
    <main class="shell-main">
      <slot />
    </main>
  </div>
</template>
