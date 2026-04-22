<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { resetPassword } from '../lib/api'

const router = useRouter()
const form = reactive({
  account: '',
  newPassword: '',
  confirmPassword: ''
})
const message = ref('')
const error = ref('')

async function submit() {
  error.value = ''
  message.value = ''
  try {
    await resetPassword(form)
    message.value = '密码已重置，请返回登录页继续。'
  } catch (err) {
    error.value = err.message
  }
}
</script>

<template>
  <div class="auth-shell">
    <div class="auth-card">
      <section class="auth-visual">
        <span class="badge">密码重置</span>
        <h1>快速找回账号访问权限</h1>
        <p>当前演示版本采用账号 + 新密码的简单重置流程，便于项目答辩演示。</p>
      </section>
      <section class="auth-form">
        <h2>重置密码</h2>
        <div v-if="message" class="message-box">{{ message }}</div>
        <div v-if="error" class="message-box">{{ error }}</div>
        <div class="field">
          <label>账号</label>
          <input v-model="form.account" />
        </div>
        <div class="field">
          <label>新密码</label>
          <input v-model="form.newPassword" type="password" />
        </div>
        <div class="field">
          <label>确认新密码</label>
          <input v-model="form.confirmPassword" type="password" />
        </div>
        <div class="actions">
          <button class="primary-button" @click="submit">确认重置</button>
          <button class="ghost-button" @click="router.push('/login')">返回登录</button>
        </div>
      </section>
    </div>
  </div>
</template>
