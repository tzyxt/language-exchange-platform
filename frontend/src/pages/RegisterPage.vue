<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register, sendRegisterEmailCode } from '../lib/api'

const router = useRouter()
const form = reactive({
  email: '',
  verificationCode: '',
  password: '',
  confirmPassword: '',
  role: 'USER',
  name: ''
})
const error = ref('')
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
const touched = reactive({
  email: false,
  verificationCode: false,
  name: false,
  password: false,
  confirmPassword: false
})

let countdownTimer = null

const passwordStrength = computed(() => {
  const value = form.password
  let score = 0
  if (value.length >= 8) score += 1
  if (/[A-Za-z]/.test(value) && /\d/.test(value)) score += 1
  if (/[~!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(value)) score += 1

  if (score <= 1) return { label: '弱', className: 'weak', width: '33%' }
  if (score === 2) return { label: '中', className: 'medium', width: '66%' }
  return { label: '强', className: 'strong', width: '100%' }
})

const emailError = computed(() => {
  if (!touched.email) return ''
  const value = form.email.trim()
  if (!value) return '请输入邮箱'
  if (value.length > 50) return '邮箱不能超过 50 位'
  if (!/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(value)) return '请输入正确的邮箱格式'
  return ''
})

const verificationCodeError = computed(() => {
  if (!touched.verificationCode) return ''
  if (!form.verificationCode.trim()) return '请输入验证码'
  if (!/^\d{6}$/.test(form.verificationCode.trim())) return '验证码应为 6 位数字'
  return ''
})

const nameError = computed(() => {
  if (!touched.name) return ''
  const value = form.name.trim()
  if (!value) return '请输入昵称'
  if (value.length < 2) return '昵称至少 2 位'
  if (value.length > 20) return '昵称不能超过 20 位'
  return ''
})

const passwordError = computed(() => {
  if (!touched.password) return ''
  if (!form.password) return '请输入密码'
  if (form.password.length < 8) return '密码至少 8 位'
  if (form.password.length > 20) return '密码不能超过 20 位'
  if (!/(?=.*[A-Za-z])(?=.*\d)/.test(form.password)) return '密码需同时包含字母和数字'
  return ''
})

const confirmPasswordError = computed(() => {
  if (!touched.confirmPassword) return ''
  if (!form.confirmPassword) return '请再次输入密码'
  if (form.confirmPassword !== form.password) return '两次输入的密码不一致'
  return ''
})

const canSendCode = computed(() => {
  return !sendingCode.value && countdown.value === 0 && !!form.email.trim() && !emailError.value
})

const canSubmit = computed(() => {
  return (
    form.email.trim() &&
    form.verificationCode.trim() &&
    form.name.trim() &&
    form.password &&
    form.confirmPassword &&
    !emailError.value &&
    !verificationCodeError.value &&
    !nameError.value &&
    !passwordError.value &&
    !confirmPasswordError.value
  )
})

function startCountdown() {
  countdown.value = 60
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
      countdown.value = 0
    }
  }, 1000)
}

async function sendCode() {
  touched.email = true
  error.value = ''
  if (!canSendCode.value) return

  sendingCode.value = true
  try {
    await sendRegisterEmailCode(form.email.trim())
    startCountdown()
  } catch (err) {
    error.value = err.message || '验证码发送失败'
  } finally {
    sendingCode.value = false
  }
}

async function submit() {
  touched.email = true
  touched.verificationCode = true
  touched.name = true
  touched.password = true
  touched.confirmPassword = true
  error.value = ''
  if (!canSubmit.value) return

  loading.value = true
  try {
    await register({
      email: form.email.trim(),
      verificationCode: form.verificationCode.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
      role: 'USER',
      name: form.name.trim()
    })
    router.push('/profile')
  } catch (err) {
    error.value = err.message || '注册失败'
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<template>
  <div class="login-shell register-shell">
    <div class="login-frame">
      <section class="login-visual register-visual">
        <div class="login-brand">
          <div class="login-brand-mark">LEP</div>
          <span>Language Exchange Platform</span>
        </div>

        <div class="login-visual-copy">
          <span class="login-kicker">Join Us</span>
          <h1>邮箱注册</h1>
          <p>使用邮箱接收验证码完成注册，注册后即可直接用邮箱和密码登录。</p>

          <div class="login-quote">
            <p>“从一封验证邮件开始，走进一段新的语言连接。”</p>
            <span>Language Exchange Platform</span>
          </div>

          <div class="login-demo-card">
            <strong>注册说明</strong>
            <p>新用户通过邮箱验证码注册，系统会自动将邮箱作为登录账号。</p>
            <p>以前已经注册的老用户不受影响，仍然可以继续用原账号登录。</p>
          </div>
        </div>
      </section>

      <section class="login-panel">
        <div class="login-panel-mark">注</div>
        <div class="login-panel-inner">
          <header class="login-panel-header">
            <h2>用户注册</h2>
            <p>填写信息后即可进入平台</p>
          </header>

          <div v-if="error" class="message-box">{{ error }}</div>

          <div class="login-form register-form">
            <div class="register-grid">
              <label class="login-field">
                <span>邮箱</span>
                <input v-model="form.email" placeholder="请输入常用邮箱" @blur="touched.email = true" />
                <div class="register-field-meta">
                  <div class="register-field-hint-block">
                    <small class="field-hint">注册验证码会发送到这个邮箱。</small>
                  </div>
                  <div class="register-field-extra"></div>
                  <small class="field-error" :class="{ 'field-error--hidden': !emailError }">
                    {{ emailError || 'placeholder' }}
                  </small>
                </div>
              </label>

              <label class="login-field">
                <span>邮箱验证码</span>
                <div class="login-password-wrap">
                  <input
                    v-model="form.verificationCode"
                    placeholder="请输入 6 位验证码"
                    @blur="touched.verificationCode = true"
                  />
                  <button type="button" class="login-eye" :disabled="!canSendCode" @click="sendCode">
                    {{
                      sendingCode
                        ? '发送中...'
                        : countdown > 0
                          ? `${countdown}s`
                          : '发送验证码'
                    }}
                  </button>
                </div>
                <div class="register-field-meta">
                  <div class="register-field-hint-block">
                    <small class="field-hint">验证码 10 分钟内有效。</small>
                  </div>
                  <div class="register-field-extra"></div>
                  <small class="field-error" :class="{ 'field-error--hidden': !verificationCodeError }">
                    {{ verificationCodeError || 'placeholder' }}
                  </small>
                </div>
              </label>

              <label class="login-field">
                <span>昵称</span>
                <input v-model="form.name" placeholder="请输入昵称" @blur="touched.name = true" />
                <div class="register-field-meta">
                  <div class="register-field-hint-block">
                    <small class="field-hint">昵称建议 2-20 位，方便他人识别你。</small>
                  </div>
                  <div class="register-field-extra"></div>
                  <small class="field-error" :class="{ 'field-error--hidden': !nameError }">
                    {{ nameError || 'placeholder' }}
                  </small>
                </div>
              </label>

              <label class="login-field">
                <span>密码</span>
                <input v-model="form.password" type="password" placeholder="请输入密码" @blur="touched.password = true" />
                <div class="register-field-meta">
                  <div class="register-field-hint-block">
                    <small class="field-hint">8-20 位，且至少包含一个字母和一个数字。</small>
                  </div>
                  <div class="register-field-extra">
                    <div class="password-strength">
                      <div class="password-strength-bar">
                        <span
                          :class="`password-strength-fill ${passwordStrength.className}`"
                          :style="{ width: passwordStrength.width }"
                        ></span>
                      </div>
                      <span class="password-strength-label">强度：{{ passwordStrength.label }}</span>
                    </div>
                  </div>
                  <small class="field-error" :class="{ 'field-error--hidden': !passwordError }">
                    {{ passwordError || 'placeholder' }}
                  </small>
                </div>
              </label>

              <label class="login-field">
                <span>确认密码</span>
                <input
                  v-model="form.confirmPassword"
                  type="password"
                  placeholder="请再次输入密码"
                  @blur="touched.confirmPassword = true"
                />
                <div class="register-field-meta">
                  <div class="register-field-hint-block">
                    <small class="field-hint">请再次输入相同密码，避免拼写错误。</small>
                  </div>
                  <div class="register-field-extra"></div>
                  <small class="field-error" :class="{ 'field-error--hidden': !confirmPasswordError }">
                    {{ confirmPasswordError || 'placeholder' }}
                  </small>
                </div>
              </label>
            </div>

            <label class="login-field">
              <span>账号类型</span>
              <input value="普通用户" disabled />
            </label>

            <button class="login-submit" :disabled="loading || !canSubmit" @click="submit">
              {{ loading ? '注册中...' : '完成注册' }}
              <span aria-hidden="true">→</span>
            </button>

            <div class="login-footer">
              <span>已经有账号？</span>
              <button type="button" class="login-link-button accent" @click="router.push('/login')">
                返回登录
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>
