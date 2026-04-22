<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { currentUserId, request } from '../lib/api'

const router = useRouter()
const filters = reactive({ language: '', interest: '', college: '' })
const matches = ref([])
const message = ref('')

async function load() {
  const query = new URLSearchParams({
    ...(filters.language ? { language: filters.language } : {}),
    ...(filters.interest ? { interest: filters.interest } : {}),
    ...(filters.college ? { college: filters.college } : {}),
    sortBy: 'score'
  })
  matches.value = await request(`/matches/${currentUserId()}?${query.toString()}`)
}

async function connect(id) {
  const data = await request(`/matches/${currentUserId()}/${id}/connect`, { method: 'POST' })
  message.value = `已和 ${data.match.name} 建立联系`
  await load()
  router.push({
    path: '/chat',
    query: data.session?.id ? { session: String(data.session.id) } : undefined
  })
}

async function feedback(id, action) {
  await request(`/matches/${currentUserId()}/${id}/feedback`, {
    method: 'POST',
    body: JSON.stringify({ action })
  })
  await load()
}

function resetFilters() {
  Object.assign(filters, { language: '', interest: '', college: '' })
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-grid">
    <section class="card span-12">
      <h2>智能匹配推荐</h2>
      <div v-if="message" class="message-box">{{ message }}</div>
      <div class="form-grid">
        <div class="field"><label>语言</label><input v-model="filters.language" placeholder="English / 中文" /></div>
        <div class="field"><label>兴趣</label><input v-model="filters.interest" placeholder="电影 / 摄影" /></div>
        <div class="field"><label>院系</label><input v-model="filters.college" placeholder="外国语学院" /></div>
      </div>
      <div class="actions">
        <button class="primary-button" @click="load">应用筛选</button>
        <button class="ghost-button" @click="resetFilters">清空条件</button>
      </div>
    </section>

    <section v-for="item in matches" :key="item.id" class="card span-4">
      <div class="actions">
        <span class="badge">匹配分 {{ item.matchScore }}</span>
        <span class="badge secondary">{{ item.communicationGoal }}</span>
      </div>
      <h3>{{ item.name }}</h3>
      <p class="muted">{{ item.school }} · {{ item.college }}</p>
      <p>{{ item.introduction }}</p>
      <p class="muted">{{ item.matchReasons.join('；') }}</p>
      <div class="actions">
        <button class="primary-button" @click="connect(item.id)">发起联系</button>
        <button class="secondary-button" @click="router.push(`/users/${item.id}`)">查看详情</button>
        <button class="ghost-button" @click="feedback(item.id, 'IGNORED')">忽略</button>
      </div>
    </section>
  </div>
</template>
