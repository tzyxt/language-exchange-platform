<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../lib/api'
import { clearSession, sessionStore } from '../stores/session'

const router = useRouter()
const form = reactive({ account: '', password: '', remember: false })
const loading = ref(false)
const error = ref('')
const showPassword = ref(false)
const touched = reactive({ account: false, password: false })

const accountError = computed(() => {
  if (!touched.account) return ''
  if (!form.account.trim()) return '请输入账号'
  if (form.account.trim().length < 4) return '账号至少 4 位'
  if (form.account.trim().length > 20) return '账号不能超过 20 位'
  return ''
})

const passwordError = computed(() => {
  if (!touched.password) return ''
  if (!form.password) return '请输入密码'
  return ''
})

const canSubmit = computed(() => {
  return Boolean(form.account.trim() && form.password && !accountError.value && !passwordError.value)
})

async function submit() {
  touched.account = true
  touched.password = true
  error.value = ''
  if (!canSubmit.value) return

  loading.value = true
  try {
    await login(form.account.trim(), form.password)
    if (sessionStore.currentUser?.role === 'ADMIN') {
      clearSession()
      throw new Error('管理员请使用后台登录入口')
    }
    router.push('/home')
  } catch (err) {
    error.value = err.message || '登录失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-shell">
    <div class="login-frame">
      <section class="login-visual">
        <div class="login-brand">
          <div class="login-brand-mark">LEP</div>
          <span>Language Exchange Platform</span>
        </div>

        <div class="login-visual-copy">
          <span class="login-kicker">Campus Edition</span>
          <h1>欢迎回来</h1>
          <p>连接全球学伴，在语言互助与跨文化交流中发现新的可能。</p>

          <div class="login-quote">
            <p>“每一场对话，都是一次新的理解与靠近。”</p>
            <span>Language Exchange Platform</span>
          </div>

          <div class="login-demo-card">
            <strong>登录提示</strong>
            <p>请输入你自己的账号和密码登录，系统不再默认预填测试账号。</p>
            <p>管理员账号请从独立后台入口登录，普通用户只使用当前入口。</p>
          </div>
        </div>
      </section>

      <section class="login-panel">
        <div class="login-panel-mark">文A</div>
        <div class="login-panel-inner">
          <header class="login-panel-header">
            <h2>用户登录</h2>
            <p>开始您的跨文化交流之旅</p>
          </header>

          <div v-if="error" class="message-box">{{ error }}</div>

          <div class="login-form">
            <label class="login-field">
              <span>账号 / 邮箱 / 手机号</span>
              <input
                v-model="form.account"
                placeholder="请输入您的账号"
                @blur="touched.account = true"
              />
              <small v-if="accountError" class="field-error">{{ accountError }}</small>
            </label>

            <label class="login-field">
              <span>密码</span>
              <div class="login-password-wrap">
                <input
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  placeholder="请输入您的密码"
                  @blur="touched.password = true"
                />
                <button type="button" class="login-eye" @click="showPassword = !showPassword">
                  {{ showPassword ? '隐藏' : '显示' }}
                </button>
              </div>
              <small v-if="passwordError" class="field-error">{{ passwordError }}</small>
            </label>

            <div class="login-row">
              <label class="login-check">
                <input v-model="form.remember" type="checkbox" />
                <span>记住我</span>
              </label>
              <button type="button" class="login-link-button" @click="router.push('/forgot-password')">
                忘记密码？
              </button>
            </div>

            <button class="login-submit" :disabled="loading || !canSubmit" @click="submit">
              {{ loading ? '登录中...' : '登录' }}
              <span aria-hidden="true">→</span>
            </button>

            <div class="login-footer">
              <span>还没有账号？</span>
              <button type="button" class="login-link-button accent" @click="router.push('/register')">
                立即注册
              </button>
            </div>

            <div class="login-footer">
              <span>管理员入口：</span>
              <button type="button" class="login-link-button" @click="router.push('/admin/login')">
                前往后台登录
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>
