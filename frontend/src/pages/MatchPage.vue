<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { currentUserId, request } from '../lib/api'

const router = useRouter()
const filters = reactive({ language: '', interest: '', college: '' })
const matches = ref([])
const message = ref('')
const quickFilters = ['口语练习', '文化交流', '电影', '设计', '外国语学院']

const matchStats = computed(() => {
  const scores = matches.value.map((item) => Number(item.matchScore) || 0)
  const total = scores.length
  const highest = total ? Math.max(...scores) : 0
  const average = total ? Math.round(scores.reduce((sum, score) => sum + score, 0) / total) : 0
  return { total, highest, average }
})

const enabledFilterCount = computed(() => {
  return [filters.language, filters.interest, filters.college].filter((item) => item.trim()).length
})

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

function applyQuickFilter(tag) {
  if (tag.includes('学院')) {
    filters.college = tag
  } else {
    filters.interest = tag
  }
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-grid">
    <section class="card span-12 match-page-hero">
      <div class="match-page-hero-main">
        <span class="badge">智能匹配中心</span>
        <h2>找到更适合继续聊下去的人</h2>
        <p class="muted">根据语言目标、共同兴趣和交流偏好，优先推荐更容易形成长期互动的语伴。</p>
        <div class="actions">
          <button
            v-for="tag in quickFilters"
            :key="tag"
            type="button"
            class="badge secondary match-quick-button"
            @click="applyQuickFilter(tag)"
          >
            {{ tag }}
          </button>
        </div>
      </div>

      <div class="match-page-hero-stats">
        <article class="mini-stat-card match-mini-stat">
          <strong>{{ matchStats.total }}</strong>
          <span>推荐人数</span>
        </article>
        <article class="mini-stat-card match-mini-stat">
          <strong>{{ matchStats.highest }}</strong>
          <span>最高匹配分</span>
        </article>
        <article class="mini-stat-card match-mini-stat">
          <strong>{{ matchStats.average }}</strong>
          <span>平均匹配分</span>
        </article>
      </div>
    </section>

    <section class="card span-12 match-filter-panel">
      <div class="section-heading">
        <div>
          <h3>智能筛选</h3>
          <p class="muted">输入语言方向、兴趣偏好或院系范围，推荐结果会立即更新。</p>
        </div>
        <span class="badge secondary">已启用 {{ enabledFilterCount }} 个条件</span>
      </div>

      <div v-if="message" class="message-box success-box">{{ message }}</div>

      <div class="form-grid">
        <div class="field">
          <label>语言</label>
          <input v-model="filters.language" placeholder="English / 中文" />
        </div>
        <div class="field">
          <label>兴趣</label>
          <input v-model="filters.interest" placeholder="电影 / 摄影 / 口语练习" />
        </div>
        <div class="field">
          <label>院系</label>
          <input v-model="filters.college" placeholder="外国语学院 / 国际学院" />
        </div>
      </div>

      <div class="actions">
        <button class="primary-button" @click="load">应用筛选</button>
        <button class="ghost-button" @click="resetFilters">清空条件</button>
      </div>
    </section>

    <section v-for="item in matches" :key="item.id" class="card span-4 match-result-card">
      <div class="match-result-head">
        <div class="match-result-user">
          <img :src="item.avatar" :alt="item.name" class="match-result-avatar" />
          <div>
            <h3>{{ item.name }}</h3>
            <p class="muted">{{ item.school }} · {{ item.college }}</p>
          </div>
        </div>
        <div class="match-score-badge">
          <span>匹配分</span>
          <strong>{{ item.matchScore }}</strong>
        </div>
      </div>

      <div class="actions">
        <span class="badge">{{ item.communicationGoal }}</span>
        <span class="badge secondary">{{ item.nativeLanguage }} -> {{ item.targetLanguage }}</span>
      </div>

      <p class="match-result-intro">{{ item.introduction }}</p>

      <div class="match-reason-list">
        <strong>推荐原因</strong>
        <ul>
          <li v-for="reason in item.matchReasons" :key="reason">{{ reason }}</li>
        </ul>
      </div>

      <div class="actions">
        <button class="primary-button" @click="connect(item.id)">发起联系</button>
        <button class="secondary-button" @click="router.push(`/users/${item.id}`)">查看详情</button>
        <button class="ghost-button" @click="feedback(item.id, 'IGNORED')">忽略</button>
      </div>
    </section>
  </div>
</template>
