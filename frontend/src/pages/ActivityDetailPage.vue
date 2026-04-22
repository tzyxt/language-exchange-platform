<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { currentUserId, request } from '../lib/api'

const route = useRoute()
const detail = ref(null)
const reviewForm = reactive({ score: 5, content: '' })

async function load() {
  detail.value = await request(`/activities/${route.params.id}?userId=${currentUserId()}`)
}

async function signup() {
  await request(`/activities/${route.params.id}/signups`, {
    method: 'POST',
    body: JSON.stringify({ userId: currentUserId(), signupType: 'SIGNUP' })
  })
  await load()
}

async function cancel() {
  await request(`/activities/${route.params.id}/signups?userId=${currentUserId()}`, { method: 'DELETE' })
  await load()
}

async function review() {
  await request(`/activities/${route.params.id}/reviews`, {
    method: 'POST',
    body: JSON.stringify({ userId: currentUserId(), ...reviewForm })
  })
  reviewForm.content = ''
  await load()
}

onMounted(load)
</script>

<template>
  <div v-if="detail" class="detail-grid">
    <section class="card">
      <span class="badge">{{ detail.activityType }}</span>
      <h2>{{ detail.title }}</h2>
      <p>{{ detail.content }}</p>
      <p class="muted">{{ detail.displayTime }}</p>
      <p class="muted">地点：{{ detail.location || '线上直播' }}</p>
      <div class="actions">
        <button v-if="!detail.joined" class="primary-button" @click="signup">报名活动</button>
        <button v-else class="ghost-button" @click="cancel">取消报名</button>
        <a v-if="detail.liveUrl" class="secondary-button" :href="detail.liveUrl" target="_blank">进入直播</a>
        <a v-if="detail.replayUrl" class="secondary-button" :href="detail.replayUrl" target="_blank">查看回放</a>
      </div>
      <div class="field">
        <label>活动评价</label>
        <textarea v-model="reviewForm.content" placeholder="填写参加感受" />
      </div>
      <div class="actions">
        <select v-model="reviewForm.score">
          <option :value="5">5 分</option>
          <option :value="4">4 分</option>
          <option :value="3">3 分</option>
        </select>
        <button class="primary-button" @click="review">提交评价</button>
      </div>
    </section>
    <section class="card">
      <h3>参与与反馈</h3>
      <div class="list">
        <div v-for="person in detail.participants" :key="person.id" class="item-card">
          <strong>{{ person.user.name }}</strong>
          <p class="muted">{{ person.signupType }} · {{ person.signupStatus }}</p>
        </div>
      </div>
    </section>
  </div>
</template>
