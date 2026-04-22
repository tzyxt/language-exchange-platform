<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { checkAccountAvailability, register } from '../lib/api'

const router = useRouter()
const form = reactive({
  account: '',
  password: '',
  confirmPassword: '',
  role: 'USER',
  name: ''
})
const error = ref('')
const loading = ref(false)
const checkingAccount = ref(false)
const accountAvailable = ref(null)
const touched = reactive({
  account: false,
  name: false,
  password: false,
  confirmPassword: false
})

let accountCheckTimer = null

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

const accountError = computed(() => {
  if (!touched.account) return ''
  const value = form.account.trim()
  if (!value) return '请输入账号'
  if (value.length < 4) return '账号至少 4 位'
  if (value.length > 20) return '账号不能超过 20 位'
  if (!/^[A-Za-z0-9._]+$/.test(value)) return '账号只能包含字母、数字、下划线和点'
  if (accountAvailable.value === false) return '该账号已被占用'
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

const canSubmit = computed(() => {
  return (
    form.account.trim() &&
    form.name.trim() &&
    form.password &&
    form.confirmPassword &&
    accountAvailable.value !== false &&
    !accountError.value &&
    !nameError.value &&
    !passwordError.value &&
    !confirmPasswordError.value
  )
})

watch(
  () => form.account,
  (value) => {
    accountAvailable.value = null
    if (accountCheckTimer) {
      clearTimeout(accountCheckTimer)
    }

    const normalized = value.trim()
    if (normalized.length < 4 || normalized.length > 20 || !/^[A-Za-z0-9._]+$/.test(normalized)) {
      checkingAccount.value = false
      return
    }

    accountCheckTimer = setTimeout(async () => {
      checkingAccount.value = true
      try {
        const result = await checkAccountAvailability(normalized)
        accountAvailable.value = result.available
      } catch {
        accountAvailable.value = null
      } finally {
        checkingAccount.value = false
      }
    }, 400)
  }
)

async function submit() {
  touched.account = true
  touched.name = true
  touched.password = true
  touched.confirmPassword = true
  error.value = ''
  if (!canSubmit.value || checkingAccount.value) return

  loading.value = true
  try {
    await register({
      account: form.account.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
      role: 'USER',
      name: form.name.trim()
    })
    router.push('/profile')
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}
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
          <h1>创建账号</h1>
          <p>完善你的基础身份信息，开启语言互助、文化分享与校园连接体验。</p>

          <div class="login-quote">
            <p>“注册之后，每一次相遇都可能变成一段新的文化故事。”</p>
            <span>Language Exchange Platform</span>
          </div>

          <div class="login-demo-card">
            <strong>注册规则</strong>
            <p>账号需为 4-20 位，仅支持字母、数字、下划线和点。</p>
            <p>昵称需为 2-20 位；密码需为 8-20 位，且至少包含一个字母和一个数字。</p>
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
                <span>账号</span>
                <input
                  v-model="form.account"
                  placeholder="请输入登录账号"
                  @blur="touched.account = true"
                />
                <div class="register-field-meta">
                  <div class="register-field-hint-block">
                    <small class="field-hint">4-20 位，支持字母、数字、下划线和点。</small>
                    <small v-if="checkingAccount" class="field-hint">正在检查账号是否可用...</small>
                    <small v-else-if="accountAvailable === true" class="field-success">该账号可以使用</small>
                  </div>
                  <div class="register-field-extra"></div>
                  <small class="field-error" :class="{ 'field-error--hidden': !accountError }">
                    {{ accountError || 'placeholder' }}
                  </small>
                </div>
              </label>

              <label class="login-field">
                <span>昵称</span>
                <input
                  v-model="form.name"
                  placeholder="请输入昵称"
                  @blur="touched.name = true"
                />
                <div class="register-field-meta">
                  <div class="register-field-hint-block">
                    <small class="field-hint">昵称建议 2-20 位，便于他人识别你。</small>
                  </div>
                  <div class="register-field-extra"></div>
                  <small class="field-error" :class="{ 'field-error--hidden': !nameError }">
                    {{ nameError || 'placeholder' }}
                  </small>
                </div>
              </label>

              <label class="login-field">
                <span>密码</span>
                <input
                  v-model="form.password"
                  type="password"
                  placeholder="请输入密码"
                  @blur="touched.password = true"
                />
                <div class="register-field-meta">
                  <div class="register-field-hint-block">
                    <small class="field-hint">8-20 位，至少包含一个字母和一个数字。</small>
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
                    <small class="field-hint">请再次输入同一密码，避免拼写错误。</small>
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

            <button class="login-submit" :disabled="loading || checkingAccount || !canSubmit" @click="submit">
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
