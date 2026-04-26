<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { request } from '../../lib/api'

const reports = ref([])
const loading = ref(false)
const error = ref('')
const actingReportId = ref(null)
const filters = reactive({
  keyword: '',
  status: ''
})

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待处理', value: 'PENDING' },
  { label: '已通过', value: 'RESOLVED' },
  { label: '已驳回', value: 'REJECTED' }
]

const summary = computed(() => ({
  total: reports.value.length,
  pending: reports.value.filter((item) => item.status === 'PENDING').length,
  resolved: reports.value.filter((item) => item.status === 'RESOLVED').length,
  rejected: reports.value.filter((item) => item.status === 'REJECTED').length
}))

const filteredReports = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return reports.value.filter((item) => {
    const matchKeyword = !keyword || [item.reason, item.targetType, item.reportUser?.name]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
      .includes(keyword)
    const matchStatus = !filters.status || item.status === filters.status
    return matchKeyword && matchStatus
  })
})

function statusLabel(status) {
  if (status === 'PENDING') return '待处理'
  if (status === 'RESOLVED') return '已通过'
  if (status === 'REJECTED') return '已驳回'
  return status
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const query = filters.status ? `?status=${filters.status}` : ''
    reports.value = await request(`/admin/reports${query}`)
  } catch (err) {
    error.value = err.message || '举报数据加载失败'
  } finally {
    loading.value = false
  }
}

async function process(id, status) {
  actingReportId.value = id
  error.value = ''
  try {
    await request(`/admin/reports/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    })
    await load()
  } catch (err) {
    error.value = err.message || '举报处理失败'
  } finally {
    actingReportId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="page-grid">
    <section class="card span-12">
      <div class="section-heading">
        <div>
          <h2>举报与内容审核</h2>
          <p class="muted">集中处理用户举报记录，快速区分待处理、已通过和已驳回状态。</p>
        </div>
        <button class="secondary-button" @click="load">刷新列表</button>
      </div>

      <div class="admin-stats-row">
        <div class="item-card mini-stat-card">
          <strong>{{ summary.total }}</strong>
          <span class="muted">举报总数</span>
        </div>
        <div class="item-card mini-stat-card">
          <strong>{{ summary.pending }}</strong>
          <span class="muted">待处理</span>
        </div>
        <div class="item-card mini-stat-card">
          <strong>{{ summary.resolved }}</strong>
          <span class="muted">已通过</span>
        </div>
        <div class="item-card mini-stat-card">
          <strong>{{ summary.rejected }}</strong>
          <span class="muted">已驳回</span>
        </div>
      </div>

      <div class="admin-filter-grid">
        <label class="field">
          <span>搜索举报</span>
          <input v-model="filters.keyword" placeholder="搜索举报原因、目标类型或举报人" />
        </label>
        <label class="field">
          <span>状态筛选</span>
          <select v-model="filters.status" @change="load">
            <option v-for="option in statusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>
      </div>

      <div v-if="error" class="message-box">{{ error }}</div>
      <div v-else-if="loading" class="message-box">正在加载举报列表...</div>
      <div v-else-if="!filteredReports.length" class="message-box">当前没有匹配的举报记录。</div>
      <div v-else class="table-list">
        <div v-for="item in filteredReports" :key="item.id" class="table-item admin-table-item">
          <div class="admin-table-main">
            <strong>{{ item.targetType }} #{{ item.targetId }}</strong>
            <p class="muted">举报人：{{ item.reportUser.name }}</p>
            <p class="muted">创建时间：{{ item.createdAt }}</p>
          </div>
          <div>
            <span class="badge" :class="{ success: item.status === 'RESOLVED', secondary: item.status === 'REJECTED' }">
              {{ statusLabel(item.status) }}
            </span>
          </div>
          <div class="admin-table-meta">
            <p>{{ item.reason }}</p>
          </div>
          <div class="actions">
            <button
              class="secondary-button"
              :disabled="actingReportId === item.id || item.status === 'RESOLVED'"
              @click="process(item.id, 'RESOLVED')"
            >
              通过
            </button>
            <button
              class="ghost-button"
              :disabled="actingReportId === item.id || item.status === 'REJECTED'"
              @click="process(item.id, 'REJECTED')"
            >
              驳回
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
