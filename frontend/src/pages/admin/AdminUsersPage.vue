<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { request } from '../../lib/api'

const users = ref([])
const loading = ref(false)
const error = ref('')
const actingUserId = ref(null)
const filters = reactive({
  keyword: '',
  status: ''
})

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '正常', value: 'NORMAL' },
  { label: '已禁用', value: 'DISABLED' }
]

const summary = computed(() => ({
  total: users.value.length,
  normal: users.value.filter((item) => item.status === 'NORMAL').length,
  disabled: users.value.filter((item) => item.status === 'DISABLED').length,
  completed: users.value.filter((item) => item.profileCompleted).length
}))

const filteredUsers = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return users.value.filter((user) => {
    const matchKeyword = !keyword || [user.name, user.account, user.school, user.college]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
      .includes(keyword)
    const matchStatus = !filters.status || user.status === filters.status
    return matchKeyword && matchStatus
  })
})

function statusLabel(status) {
  return status === 'DISABLED' ? '已禁用' : '正常'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const query = filters.status ? `?status=${filters.status}` : ''
    users.value = await request(`/admin/users${query}`)
  } catch (err) {
    error.value = err.message || '用户数据加载失败'
  } finally {
    loading.value = false
  }
}

async function updateStatus(id, status) {
  actingUserId.value = id
  error.value = ''
  try {
    await request(`/admin/users/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    })
    await load()
  } catch (err) {
    error.value = err.message || '用户状态更新失败'
  } finally {
    actingUserId.value = null
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.status = ''
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-grid">
    <section class="card span-12">
      <div class="section-heading">
        <div>
          <h2>用户管理</h2>
          <p class="muted">支持按状态筛选、搜索账号，并直接恢复或禁用用户。</p>
        </div>
        <div class="actions">
          <button class="ghost-button" @click="resetFilters">重置筛选</button>
          <button class="secondary-button" @click="load">刷新列表</button>
        </div>
      </div>

      <div class="admin-stats-row">
        <div class="item-card mini-stat-card">
          <strong>{{ summary.total }}</strong>
          <span class="muted">用户总数</span>
        </div>
        <div class="item-card mini-stat-card">
          <strong>{{ summary.normal }}</strong>
          <span class="muted">正常账号</span>
        </div>
        <div class="item-card mini-stat-card">
          <strong>{{ summary.disabled }}</strong>
          <span class="muted">已禁用</span>
        </div>
        <div class="item-card mini-stat-card">
          <strong>{{ summary.completed }}</strong>
          <span class="muted">已完善资料</span>
        </div>
      </div>

      <div class="admin-filter-grid">
        <label class="field">
          <span>搜索用户</span>
          <input v-model="filters.keyword" placeholder="输入昵称、账号、学校或学院" />
        </label>
        <label class="field">
          <span>状态筛选</span>
          <select v-model="filters.status" @change="load">
            <option v-for="option in statusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>
      </div>

      <div v-if="error" class="message-box">{{ error }}</div>
      <div v-else-if="loading" class="message-box">正在加载用户列表...</div>
      <div v-else-if="!filteredUsers.length" class="message-box">当前筛选条件下没有匹配用户。</div>
      <div v-else class="table-list">
        <div v-for="user in filteredUsers" :key="user.id" class="table-item admin-table-item">
          <div class="admin-table-main">
            <strong>{{ user.name }}</strong>
            <p class="muted">账号：{{ user.account }}</p>
            <p class="muted">{{ user.school }} / {{ user.college }}</p>
          </div>
          <div>
            <span class="badge" :class="{ success: user.status === 'NORMAL' }">{{ statusLabel(user.status) }}</span>
          </div>
          <div class="admin-table-meta">
            <p class="muted">母语：{{ user.nativeLanguage || '未填写' }}</p>
            <p class="muted">目标语言：{{ user.targetLanguage || '未填写' }}</p>
            <p class="muted">资料状态：{{ user.profileCompleted ? '已完善' : '待完善' }}</p>
          </div>
          <div class="actions">
            <button
              class="secondary-button"
              :disabled="actingUserId === user.id || user.status === 'NORMAL'"
              @click="updateStatus(user.id, 'NORMAL')"
            >
              恢复
            </button>
            <button
              class="ghost-button"
              :disabled="actingUserId === user.id || user.status === 'DISABLED'"
              @click="updateStatus(user.id, 'DISABLED')"
            >
              禁用
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
