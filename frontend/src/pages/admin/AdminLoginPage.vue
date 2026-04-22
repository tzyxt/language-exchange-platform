<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearSession, sessionStore } from '../../stores/session'
import { login } from '../../lib/api'

const router = useRouter()
const form = reactive({
  account: '',
  password: ''
})
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  if (!form.account.trim() || !form.password) {
    error.value = '请输入管理员账号和密码。'
    return
  }

  loading.value = true
  try {
    await login(form.account.trim(), form.password)
    if (sessionStore.currentUser?.role !== 'ADMIN') {
      clearSession()
      throw new Error('当前账号不是管理员，请使用普通用户登录入口。')
    }
    router.push('/admin')
  } catch (err) {
    error.value = err.message || '管理员登录失败。'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="admin-login-shell">
    <div class="admin-login-card">
      <section class="admin-login-copy">
        <span class="login-kicker">Admin Portal</span>
        <h1>管理员后台登录</h1>
        <p>单独进入后台管理系统，处理用户、活动与举报审核。</p>

        <div class="login-demo-card">
          <strong>后台入口说明</strong>
          <p>这里只允许管理员账号登录，不再默认填充测试账号和密码。</p>
        </div>
      </section>

      <section class="admin-login-panel">
        <h2>后台入口</h2>
        <p class="muted">仅管理员账号可登录</p>

        <div v-if="error" class="message-box">{{ error }}</div>

        <div class="login-form">
          <label class="login-field">
            <span>管理员账号</span>
            <input v-model="form.account" placeholder="请输入管理员账号" />
          </label>

          <label class="login-field">
            <span>密码</span>
            <input v-model="form.password" type="password" placeholder="请输入密码" />
          </label>

          <button class="login-submit admin-submit" :disabled="loading" @click="submit">
            {{ loading ? '登录中...' : '进入后台' }}
            <span aria-hidden="true">→</span>
          </button>

          <div class="login-footer">
            <span>普通用户请前往</span>
            <button type="button" class="login-link-button accent" @click="router.push('/login')">
              用户登录
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>
