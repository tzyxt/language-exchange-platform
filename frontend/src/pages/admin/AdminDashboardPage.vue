<script setup>
import { onMounted, ref } from 'vue'
import { request } from '../../lib/api'

const data = ref(null)
onMounted(async () => {
  data.value = await request('/admin/dashboard')
})
</script>

<template>
  <div v-if="data" class="page-grid">
    <section class="card span-12">
      <h2>后台首页</h2>
      <p class="muted">管理员可在这里查看平台总体运行情况、近期举报和活动动态。</p>
    </section>
    <section class="card span-3" v-for="(value, key) in data.stats" :key="key">
      <h3>{{ key }}</h3>
      <p class="stat-value">{{ value }}</p>
    </section>
    <section class="card span-6">
      <h3>近期举报</h3>
      <div class="list">
        <div v-for="item in data.recentReports" :key="item.id" class="item-card">
          <strong>{{ item.targetType }} #{{ item.targetId }}</strong>
          <p>{{ item.reason }}</p>
        </div>
      </div>
    </section>
    <section class="card span-6">
      <h3>近期活动</h3>
      <div class="list">
        <div v-for="item in data.recentActivities" :key="item.id" class="item-card">
          <strong>{{ item.title }}</strong>
          <p class="muted">{{ item.displayTime }}</p>
        </div>
      </div>
    </section>
  </div>
</template>
