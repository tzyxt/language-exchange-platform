<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { currentUserId, request } from '../lib/api'

const route = useRoute()
const router = useRouter()
const data = ref(null)

async function load() {
  data.value = await request(`/users/${route.params.id}?viewerId=${currentUserId()}`)
}

async function connect() {
  const result = await request(`/matches/${currentUserId()}/${route.params.id}/connect`, { method: 'POST' })
  router.push({
    path: '/chat',
    query: result.session?.id ? { session: String(result.session.id) } : undefined
  })
}

onMounted(load)
</script>

<template>
  <div v-if="data" class="detail-grid">
    <section class="card">
      <span class="badge">匹配分 {{ data.matchScore }}</span>
      <h2>{{ data.name }}</h2>
      <p class="muted">{{ data.school }} · {{ data.college }} · {{ data.grade }}</p>
      <p>{{ data.introduction }}</p>
      <div class="actions">
        <span v-for="tag in data.interestTags" :key="tag" class="badge secondary">{{ tag }}</span>
      </div>
      <div class="list">
        <div v-for="reason in data.matchReasons" :key="reason" class="item-card">{{ reason }}</div>
      </div>
      <div class="actions">
        <button class="primary-button" @click="connect">立即联系</button>
      </div>
    </section>

    <section class="card">
      <h3>公开评价</h3>
      <div class="list">
        <div v-for="review in data.publicReviews" :key="review.id" class="item-card">
          <strong>{{ review.fromUser.name }}</strong>
          <p class="muted">评分 {{ review.score }}</p>
          <p>{{ review.content }}</p>
        </div>
      </div>
    </section>
  </div>
</template>
