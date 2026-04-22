<script setup>
import { onMounted, ref } from 'vue'
import { currentUserId, request } from '../lib/api'

const data = ref(null)

async function load() {
  data.value = await request(`/me/${currentUserId()}/overview`)
}

onMounted(load)
</script>

<template>
  <div v-if="data" class="page-grid">
    <section class="card span-12 hero-card">
      <h2>{{ data.profile.name }}</h2>
      <p>{{ data.profile.introduction }}</p>
    </section>

    <section class="card span-3">
      <h3>匹配记录</h3>
      <p class="stat-value">{{ data.stats.matchCount }}</p>
    </section>
    <section class="card span-3">
      <h3>活动参与</h3>
      <p class="stat-value">{{ data.stats.activityCount }}</p>
    </section>
    <section class="card span-3">
      <h3>收藏内容</h3>
      <p class="stat-value">{{ data.stats.favoriteCount }}</p>
    </section>
    <section class="card span-3">
      <h3>综合评分</h3>
      <p class="stat-value">{{ data.stats.reviewScore }}</p>
    </section>

    <section class="card span-6">
      <h3>最近匹配</h3>
      <div class="list">
        <div v-for="item in data.recentMatches" :key="item.id" class="item-card">
          <strong>{{ item.name }}</strong>
          <p class="muted">匹配分 {{ item.matchScore }}</p>
        </div>
      </div>
    </section>
    <section class="card span-6">
      <h3>消息通知</h3>
      <div class="list">
        <div v-for="item in data.notifications" :key="item.id" class="item-card">
          <strong>{{ item.title }}</strong>
          <p>{{ item.content }}</p>
        </div>
      </div>
    </section>
  </div>
</template>
