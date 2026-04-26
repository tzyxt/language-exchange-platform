<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { request } from '../../lib/api'

const router = useRouter()
const data = ref(null)
const loading = ref(false)
const error = ref('')

const statCards = computed(() => {
  const stats = data.value?.stats || {}
  return [
    { key: 'userCount', label: '用户总数', value: stats.userCount || 0, hint: '平台注册普通用户' },
    { key: 'activityCount', label: '活动数量', value: stats.activityCount || 0, hint: '已创建的活动条目' },
    { key: 'matchCount', label: '匹配记录', value: stats.matchCount || 0, hint: '用户匹配与连接记录' },
    { key: 'postCount', label: '社区帖子', value: stats.postCount || 0, hint: '社区动态总量' },
    { key: 'reportCount', label: '举报记录', value: stats.reportCount || 0, hint: '待审与已处理举报' }
  ]
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await request('/admin/dashboard')
  } catch (err) {
    error.value = err.message || '后台数据加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-grid">
    <section class="card span-12 admin-hero-card">
      <div>
        <h2>后台首页</h2>
        <p class="muted">管理员可在这里查看平台总体运行情况、近期举报和活动动态。</p>
      </div>
      <div class="actions">
        <button class="secondary-button" @click="router.push('/admin/users')">查看用户管理</button>
        <button class="secondary-button" @click="router.push('/admin/activities')">查看活动管理</button>
        <button class="primary-button" @click="router.push('/admin/reports')">前往举报审核</button>
      </div>
    </section>

    <section v-if="error" class="card span-12">
      <div class="message-box">{{ error }}</div>
    </section>

    <template v-else>
      <section v-for="card in statCards" :key="card.key" class="card span-4 admin-stat-card">
        <p class="admin-stat-label">{{ card.label }}</p>
        <p class="stat-value">{{ card.value }}</p>
        <p class="muted">{{ card.hint }}</p>
      </section>

      <section class="card span-6">
        <div class="section-heading">
          <div>
            <h3>近期举报</h3>
            <p class="muted">优先关注待处理记录，避免风险内容继续扩散。</p>
          </div>
          <button class="ghost-button" @click="router.push('/admin/reports')">全部举报</button>
        </div>
        <div v-if="loading" class="message-box">正在加载举报数据...</div>
        <div v-else-if="!data?.recentReports?.length" class="message-box">暂无举报记录。</div>
        <div v-else class="list">
          <div v-for="item in data.recentReports" :key="item.id" class="item-card">
            <div class="actions admin-item-header">
              <strong>{{ item.targetType }} #{{ item.targetId }}</strong>
              <span class="badge">{{ item.status }}</span>
            </div>
            <p>{{ item.reason }}</p>
            <p class="muted">举报人：{{ item.reportUser.name }}</p>
          </div>
        </div>
      </section>

      <section class="card span-6">
        <div class="section-heading">
          <div>
            <h3>近期活动</h3>
            <p class="muted">查看即将开始的活动和当前运营情况。</p>
          </div>
          <button class="ghost-button" @click="router.push('/admin/activities')">全部活动</button>
        </div>
        <div v-if="loading" class="message-box">正在加载活动数据...</div>
        <div v-else-if="!data?.recentActivities?.length" class="message-box">暂无活动数据。</div>
        <div v-else class="list">
          <div v-for="item in data.recentActivities" :key="item.id" class="item-card">
            <div class="actions admin-item-header">
              <strong>{{ item.title }}</strong>
              <span class="badge secondary">{{ item.activityType }}</span>
            </div>
            <p class="muted">{{ item.displayTime }}</p>
            <p>{{ item.content }}</p>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>
