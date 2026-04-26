<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { currentUserId, request } from '../lib/api'

const route = useRoute()
const detail = ref(null)
const reviewForm = reactive({ score: 5, content: '' })
const loading = ref(false)
const submitting = ref('')
const error = ref('')
const message = ref('')

const signupStatusText = computed(() => {
  const status = detail.value?.signupStatus
  if (status === 'SUCCESS') return '你已报名成功，请按时参加活动。'
  if (status === 'CANCEL_PENDING') return '你已提交取消申请，正在等待管理员审核。'
  if (status === 'CANCELLED') return '你的报名已取消。'
  return '你当前还没有报名这场活动。'
})

const statusBadgeText = computed(() => {
  const status = detail.value?.signupStatus
  if (status === 'SUCCESS') return '已报名'
  if (status === 'CANCEL_PENDING') return '取消待审核'
  if (status === 'CANCELLED') return '已取消'
  return '可报名'
})

const spotsText = computed(() => {
  if (!detail.value) return ''
  if (!detail.value.limitCount) return '当前活动不限制报名人数'
  const remaining = Math.max(detail.value.limitCount - detail.value.signupCount, 0)
  return `当前已报名 ${detail.value.signupCount} 人，剩余 ${remaining} 个名额`
})

const canSignup = computed(() => {
  if (!detail.value) return false
  const hasStarted = new Date(detail.value.startTime).getTime() <= Date.now()
  const isFull = detail.value.limitCount && detail.value.signupCount >= detail.value.limitCount
  return detail.value.status === 'PUBLISHED' && !hasStarted && !isFull && detail.value.signupStatus === 'NONE'
})

const participantSummary = computed(() => {
  if (!detail.value) return { signed: 0, pending: 0, cancelled: 0 }
  const signed = detail.value.participants.filter((item) => item.signupStatus === 'SUCCESS').length
  const pending = detail.value.participants.filter((item) => item.signupStatus === 'CANCEL_PENDING').length
  const cancelled = detail.value.participants.filter((item) => item.signupStatus === 'CANCELLED').length
  return { signed, pending, cancelled }
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    detail.value = await request(`/activities/${route.params.id}?userId=${currentUserId()}`)
  } catch (err) {
    error.value = err.message || '活动详情加载失败'
  } finally {
    loading.value = false
  }
}

async function signup() {
  submitting.value = 'signup'
  error.value = ''
  message.value = ''
  try {
    await request(`/activities/${route.params.id}/signups`, {
      method: 'POST',
      body: JSON.stringify({ userId: currentUserId(), signupType: 'SIGNUP' })
    })
    message.value = '报名成功，活动状态已更新。'
    await load()
  } catch (err) {
    error.value = err.message || '活动报名失败'
  } finally {
    submitting.value = ''
  }
}

async function review() {
  if (!reviewForm.content.trim()) {
    error.value = '请先填写你的活动评价。'
    return
  }

  submitting.value = 'review'
  error.value = ''
  message.value = ''
  try {
    await request(`/activities/${route.params.id}/reviews`, {
      method: 'POST',
      body: JSON.stringify({ userId: currentUserId(), ...reviewForm, content: reviewForm.content.trim() })
    })
    reviewForm.content = ''
    reviewForm.score = 5
    message.value = '评价提交成功。'
    await load()
  } catch (err) {
    error.value = err.message || '活动评价提交失败'
  } finally {
    submitting.value = ''
  }
}

async function requestCancel() {
  submitting.value = 'cancel'
  error.value = ''
  message.value = ''
  try {
    await request(`/activities/${route.params.id}/signups?userId=${currentUserId()}`, { method: 'DELETE' })
    message.value = '取消申请已提交，请等待管理员审核。'
    await load()
  } catch (err) {
    error.value = err.message || '取消报名失败'
  } finally {
    submitting.value = ''
  }
}

function formatSignupStatus(status) {
  if (status === 'SUCCESS') return '已报名'
  if (status === 'CANCEL_PENDING') return '取消待审核'
  if (status === 'CANCELLED') return '已取消'
  return status
}

function formatActivityType(type) {
  return type === 'ONLINE' ? '线上活动' : '线下活动'
}

onMounted(load)
</script>

<template>
  <div v-if="loading" class="page-grid">
    <section class="card span-12">
      <div class="message-box">正在加载活动详情...</div>
    </section>
  </div>

  <div v-else-if="error && !detail" class="page-grid">
    <section class="card span-12">
      <div class="message-box">{{ error }}</div>
    </section>
  </div>

  <div v-else-if="detail" class="page-grid">
    <section class="card span-8 activity-detail-hero">
      <div class="activity-detail-hero-head">
        <div class="activity-detail-hero-copy">
          <div class="actions">
            <span class="badge">{{ formatActivityType(detail.activityType) }}</span>
            <span class="badge secondary">{{ statusBadgeText }}</span>
          </div>
          <h2>{{ detail.title }}</h2>
          <p>{{ detail.content }}</p>
        </div>
        <div class="activity-detail-hero-meta">
          <article class="activity-detail-stat">
            <strong>{{ detail.signupCount }}</strong>
            <span>当前报名</span>
          </article>
          <article class="activity-detail-stat">
            <strong>{{ detail.reviewCount }}</strong>
            <span>活动评价</span>
          </article>
        </div>
      </div>

      <div class="activity-detail-meta-grid">
        <div class="activity-detail-meta-card">
          <span>活动时间</span>
          <strong>{{ detail.displayTime }}</strong>
        </div>
        <div class="activity-detail-meta-card">
          <span>活动地点</span>
          <strong>{{ detail.location || '线上直播' }}</strong>
        </div>
        <div class="activity-detail-meta-card">
          <span>报名状态</span>
          <strong>{{ signupStatusText }}</strong>
        </div>
      </div>

      <div v-if="message" class="message-box success-box">{{ message }}</div>
      <div v-if="error" class="message-box">{{ error }}</div>

      <div class="activity-detail-action-bar">
        <div class="activity-detail-tips">
          <strong>{{ spotsText }}</strong>
          <span>取消报名不会立刻生效，需要管理员审核后才会更新最终状态。</span>
        </div>
        <div class="actions">
          <button v-if="canSignup" class="primary-button" :disabled="submitting === 'signup'" @click="signup">
            {{ submitting === 'signup' ? '报名中...' : '立即报名' }}
          </button>
          <button
            v-else-if="detail.signupStatus === 'SUCCESS'"
            class="ghost-button"
            :disabled="submitting === 'cancel'"
            @click="requestCancel"
          >
            {{ submitting === 'cancel' ? '提交中...' : '申请取消报名' }}
          </button>
          <button v-else class="ghost-button" disabled>当前不可报名</button>
          <a
            v-if="detail.liveUrl && (detail.signupStatus === 'SUCCESS' || detail.signupStatus === 'CANCEL_PENDING')"
            class="secondary-button"
            :href="detail.liveUrl"
            target="_blank"
          >
            进入直播
          </a>
          <a v-if="detail.replayUrl" class="secondary-button" :href="detail.replayUrl" target="_blank">查看回放</a>
        </div>
      </div>
    </section>

    <section class="card span-4 activity-detail-side">
      <h3>报名概览</h3>
      <div class="activity-detail-side-stats">
        <div class="activity-detail-side-item">
          <strong>{{ participantSummary.signed }}</strong>
          <span>有效报名</span>
        </div>
        <div class="activity-detail-side-item">
          <strong>{{ participantSummary.pending }}</strong>
          <span>取消待审核</span>
        </div>
        <div class="activity-detail-side-item">
          <strong>{{ participantSummary.cancelled }}</strong>
          <span>已取消</span>
        </div>
      </div>

      <div class="activity-detail-side-note">
        <strong>规则提示</strong>
        <p>活动报名在规定时间内直接生效；如果用户主动取消，将转为管理员审核流程。</p>
      </div>
    </section>

    <section class="card span-8">
      <div class="activity-detail-section-title">
        <div>
          <h3>活动评价</h3>
          <p class="muted">报名成功并参加活动的用户可以提交 1 次评价。</p>
        </div>
        <span class="badge secondary">共 {{ detail.reviewCount }} 条</span>
      </div>

      <div v-if="detail.canReview" class="activity-review-editor">
        <div class="field">
          <label>填写参加感受</label>
          <textarea v-model="reviewForm.content" placeholder="分享你的收获、体验和建议..." />
        </div>
        <div class="actions">
          <select v-model="reviewForm.score">
            <option :value="5">5 分</option>
            <option :value="4">4 分</option>
            <option :value="3">3 分</option>
            <option :value="2">2 分</option>
            <option :value="1">1 分</option>
          </select>
          <button class="primary-button" :disabled="submitting === 'review'" @click="review">
            {{ submitting === 'review' ? '提交中...' : '提交评价' }}
          </button>
        </div>
      </div>

      <div v-else class="message-box">
        只有报名成功并参加活动的用户才可以提交评价，且每位用户仅可评价一次。
      </div>

      <div class="list">
        <div v-for="item in detail.reviews" :key="item.id" class="item-card activity-review-card">
          <div class="actions">
            <strong>{{ item.user.name }}</strong>
            <span class="badge secondary">{{ item.score }} 分</span>
          </div>
          <p>{{ item.content }}</p>
        </div>
      </div>
    </section>

    <section class="card span-4">
      <div class="activity-detail-section-title">
        <div>
          <h3>报名记录</h3>
          <p class="muted">展示当前活动全部报名与取消状态。</p>
        </div>
      </div>
      <div v-if="!detail.participants.length" class="message-box">暂时还没有用户报名。</div>
      <div v-else class="list">
        <div v-for="person in detail.participants" :key="person.id" class="item-card activity-participant-card">
          <div class="actions">
            <strong>{{ person.user.name }}</strong>
            <span class="badge secondary">{{ formatSignupStatus(person.signupStatus) }}</span>
          </div>
          <p class="muted">{{ person.signupType }} · {{ person.signupTime }}</p>
        </div>
      </div>
    </section>
  </div>
</template>
