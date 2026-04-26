<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { request } from '../../lib/api'
import { sessionStore } from '../../stores/session'

const activities = ref([])
const loading = ref(false)
const saving = ref(false)
const actingKey = ref('')
const error = ref('')
const success = ref('')
const editingId = ref(null)

const filters = reactive({
  keyword: '',
  activityType: '',
  status: ''
})

const form = reactive({
  title: '',
  content: '',
  activityType: 'ONLINE',
  location: '',
  liveUrl: '',
  replayUrl: '',
  startTime: '',
  endTime: '',
  limitCount: 30,
  status: 'PUBLISHED'
})

const activityTypeOptions = [
  { label: '全部类型', value: '' },
  { label: '线上活动', value: 'ONLINE' },
  { label: '线下活动', value: 'OFFLINE' }
]

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已关闭', value: 'CLOSED' }
]

const pendingCount = computed(() => activities.value.reduce((sum, item) => sum + (item.cancelRequestCount || 0), 0))

const summary = computed(() => ({
  total: activities.value.length,
  published: activities.value.filter((item) => item.status === 'PUBLISHED').length,
  draft: activities.value.filter((item) => item.status === 'DRAFT').length,
  closed: activities.value.filter((item) => item.status === 'CLOSED').length
}))

const filteredActivities = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return activities.value.filter((item) => {
    const matchKeyword = !keyword || [item.title, item.content, item.location]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
      .includes(keyword)
    const matchType = !filters.activityType || item.activityType === filters.activityType
    const matchStatus = !filters.status || item.status === filters.status
    return matchKeyword && matchType && matchStatus
  })
})

function blankForm() {
  form.title = ''
  form.content = ''
  form.activityType = 'ONLINE'
  form.location = ''
  form.liveUrl = ''
  form.replayUrl = ''
  form.startTime = ''
  form.endTime = ''
  form.limitCount = 30
  form.status = 'PUBLISHED'
}

function toLocalInputValue(value) {
  if (!value) return ''
  return String(value).slice(0, 16)
}

function fillForm(activity) {
  editingId.value = activity.id
  form.title = activity.title
  form.content = activity.content
  form.activityType = activity.activityType
  form.location = activity.location || ''
  form.liveUrl = activity.liveUrl || ''
  form.replayUrl = activity.replayUrl || ''
  form.startTime = toLocalInputValue(activity.startTime)
  form.endTime = toLocalInputValue(activity.endTime)
  form.limitCount = activity.limitCount ?? ''
  form.status = activity.status
}

function resetForm() {
  editingId.value = null
  blankForm()
}

function statusLabel(status) {
  if (status === 'PUBLISHED') return '已发布'
  if (status === 'DRAFT') return '草稿'
  if (status === 'CLOSED') return '已关闭'
  return status
}

function signupStatusLabel(status) {
  if (status === 'SUCCESS') return '已报名'
  if (status === 'CANCEL_PENDING') return '待审核取消'
  if (status === 'CANCELLED') return '已取消'
  return status
}

function payloadFromForm() {
  return {
    title: form.title.trim(),
    content: form.content.trim(),
    activityType: form.activityType,
    location: form.location.trim(),
    liveUrl: form.liveUrl.trim(),
    replayUrl: form.replayUrl.trim(),
    startTime: form.startTime,
    endTime: form.endTime,
    limitCount: form.limitCount === '' ? null : Number(form.limitCount),
    status: form.status,
    createdBy: sessionStore.currentUser.id
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    activities.value = await request('/admin/activities')
  } catch (err) {
    error.value = err.message || '活动数据加载失败'
  } finally {
    loading.value = false
  }
}

async function submitForm() {
  error.value = ''
  success.value = ''

  if (!form.title.trim() || !form.content.trim() || !form.startTime || !form.endTime) {
    error.value = '请把活动标题、内容和起止时间填写完整。'
    return
  }

  if (form.endTime <= form.startTime) {
    error.value = '结束时间必须晚于开始时间。'
    return
  }

  saving.value = true
  try {
    const method = editingId.value ? 'PUT' : 'POST'
    const path = editingId.value ? `/admin/activities/${editingId.value}` : '/admin/activities'
    await request(path, {
      method,
      body: JSON.stringify(payloadFromForm())
    })
    success.value = editingId.value ? '活动信息已更新。' : '活动已创建。'
    resetForm()
    await load()
  } catch (err) {
    error.value = err.message || '活动保存失败'
  } finally {
    saving.value = false
  }
}

async function updateActivityStatus(activity, status) {
  actingKey.value = `${activity.id}-${status}`
  error.value = ''
  success.value = ''
  try {
    await request(`/admin/activities/${activity.id}`, {
      method: 'PUT',
      body: JSON.stringify({
        title: activity.title,
        content: activity.content,
        activityType: activity.activityType,
        location: activity.location || '',
        liveUrl: activity.liveUrl || '',
        replayUrl: activity.replayUrl || '',
        startTime: activity.startTime,
        endTime: activity.endTime,
        limitCount: activity.limitCount,
        status,
        createdBy: activity.createdBy
      })
    })
    success.value = `活动状态已更新为${statusLabel(status)}。`
    await load()
  } catch (err) {
    error.value = err.message || '活动状态更新失败'
  } finally {
    actingKey.value = ''
  }
}

async function updateSignup(activityId, signupId, status) {
  actingKey.value = `${activityId}-${signupId}-${status}`
  error.value = ''
  success.value = ''
  try {
    await request(`/admin/activities/${activityId}/signups/${signupId}`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    })
    success.value = status === 'CANCELLED' ? '已同意取消报名。' : '已驳回取消申请。'
    await load()
  } catch (err) {
    error.value = err.message || '报名审核失败'
  } finally {
    actingKey.value = ''
  }
}

onMounted(() => {
  blankForm()
  load()
})
</script>

<template>
  <div class="page-grid">
    <section class="card span-4 admin-activity-editor">
      <div class="section-heading">
        <div>
          <h2>{{ editingId ? '编辑活动' : '发布活动' }}</h2>
          <p class="muted">支持创建、编辑、切换状态，也能处理取消报名申请。</p>
        </div>
        <button v-if="editingId" class="ghost-button" @click="resetForm">取消编辑</button>
      </div>

      <div class="admin-stats-row vertical">
        <div class="item-card mini-stat-card">
          <strong>{{ pendingCount }}</strong>
          <span class="muted">待处理取消申请</span>
        </div>
      </div>

      <div v-if="error" class="message-box">{{ error }}</div>
      <div v-else-if="success" class="message-box success-box">{{ success }}</div>

      <div class="field">
        <label>活动标题</label>
        <input v-model="form.title" placeholder="例如：双语电影夜" />
      </div>
      <div class="field">
        <label>活动内容</label>
        <textarea v-model="form.content" placeholder="介绍活动亮点、流程和面向人群" />
      </div>
      <div class="field">
        <label>活动类型</label>
        <select v-model="form.activityType">
          <option value="ONLINE">线上活动</option>
          <option value="OFFLINE">线下活动</option>
        </select>
      </div>
      <div class="field">
        <label>活动地点</label>
        <input v-model="form.location" placeholder="线下填写地点，线上可留空" />
      </div>
      <div class="field">
        <label>直播地址</label>
        <input v-model="form.liveUrl" placeholder="线上活动可填写直播链接" />
      </div>
      <div class="field">
        <label>回放地址</label>
        <input v-model="form.replayUrl" placeholder="活动结束后可补充回放链接" />
      </div>
      <div class="field">
        <label>开始时间</label>
        <input v-model="form.startTime" type="datetime-local" />
      </div>
      <div class="field">
        <label>结束时间</label>
        <input v-model="form.endTime" type="datetime-local" />
      </div>
      <div class="field">
        <label>人数上限</label>
        <input v-model="form.limitCount" type="number" min="1" placeholder="留空代表不限人数" />
      </div>
      <div class="field">
        <label>活动状态</label>
        <select v-model="form.status">
          <option value="PUBLISHED">已发布</option>
          <option value="DRAFT">草稿</option>
          <option value="CLOSED">已关闭</option>
        </select>
      </div>
      <div class="actions">
        <button class="primary-button" :disabled="saving" @click="submitForm">
          {{ saving ? '保存中...' : editingId ? '保存修改' : '创建活动' }}
        </button>
        <button class="ghost-button" :disabled="saving" @click="resetForm">清空表单</button>
      </div>
    </section>

    <section class="card span-8 admin-activity-board">
      <div class="section-heading">
        <div>
          <h2>活动管理</h2>
          <p class="muted">查看活动状态、报名情况、取消申请和用户评价。</p>
        </div>
        <button class="secondary-button" @click="load">刷新列表</button>
      </div>

      <div class="admin-stats-row">
        <div class="item-card mini-stat-card">
          <strong>{{ summary.total }}</strong>
          <span class="muted">活动总数</span>
        </div>
        <div class="item-card mini-stat-card">
          <strong>{{ summary.published }}</strong>
          <span class="muted">已发布</span>
        </div>
        <div class="item-card mini-stat-card">
          <strong>{{ summary.draft }}</strong>
          <span class="muted">草稿</span>
        </div>
        <div class="item-card mini-stat-card">
          <strong>{{ summary.closed }}</strong>
          <span class="muted">已关闭</span>
        </div>
      </div>

      <div class="admin-filter-grid three-columns">
        <label class="field">
          <span>关键词</span>
          <input v-model="filters.keyword" placeholder="搜索标题、内容或地点" />
        </label>
        <label class="field">
          <span>活动类型</span>
          <select v-model="filters.activityType">
            <option v-for="option in activityTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>
        <label class="field">
          <span>活动状态</span>
          <select v-model="filters.status">
            <option v-for="option in statusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>
      </div>

      <div v-if="loading" class="message-box">正在加载活动列表...</div>
      <div v-else-if="!filteredActivities.length" class="message-box">当前没有符合条件的活动。</div>
      <div v-else class="list">
        <article v-for="activity in filteredActivities" :key="activity.id" class="item-card admin-activity-card polished-admin-card">
          <div class="section-heading">
            <div>
              <div class="actions">
                <strong>{{ activity.title }}</strong>
                <span class="badge secondary">{{ activity.activityType === 'ONLINE' ? '线上活动' : '线下活动' }}</span>
                <span class="badge" :class="{ success: activity.status === 'PUBLISHED' }">{{ statusLabel(activity.status) }}</span>
              </div>
              <p class="muted">{{ activity.displayTime }}</p>
            </div>
            <div class="actions">
              <button class="ghost-button" @click="fillForm(activity)">编辑</button>
              <button
                class="secondary-button"
                :disabled="actingKey === `${activity.id}-PUBLISHED` || activity.status === 'PUBLISHED'"
                @click="updateActivityStatus(activity, 'PUBLISHED')"
              >
                发布
              </button>
              <button
                class="ghost-button"
                :disabled="actingKey === `${activity.id}-DRAFT` || activity.status === 'DRAFT'"
                @click="updateActivityStatus(activity, 'DRAFT')"
              >
                设为草稿
              </button>
              <button
                class="ghost-button"
                :disabled="actingKey === `${activity.id}-CLOSED` || activity.status === 'CLOSED'"
                @click="updateActivityStatus(activity, 'CLOSED')"
              >
                关闭
              </button>
            </div>
          </div>

          <p>{{ activity.content }}</p>

          <div class="admin-chip-row">
            <span class="badge secondary">报名 {{ activity.signupCount }} / {{ activity.limitCount || '不限' }}</span>
            <span class="badge secondary">取消申请 {{ activity.cancelRequestCount || 0 }}</span>
            <span class="badge secondary">评价 {{ activity.reviews.length }}</span>
            <span v-if="activity.location" class="badge secondary">{{ activity.location }}</span>
          </div>

          <div class="admin-activity-sections">
            <section class="admin-activity-section">
              <div class="activity-detail-section-title">
                <div>
                  <h3>报名与取消管理</h3>
                  <p class="muted">用户报名会直接成功，取消申请需要你在这里处理。</p>
                </div>
              </div>
              <div v-if="!activity.participants.length" class="message-box">当前还没有报名记录。</div>
              <div v-else class="list">
                <div v-for="signup in activity.participants" :key="signup.id" class="item-card admin-signup-card">
                  <div class="actions admin-item-header">
                    <strong>{{ signup.user.name }}</strong>
                    <span class="badge secondary">{{ signupStatusLabel(signup.signupStatus) }}</span>
                  </div>
                  <p class="muted">{{ signup.signupType }} · {{ signup.signupTime }}</p>
                  <div v-if="signup.signupStatus === 'CANCEL_PENDING'" class="actions">
                    <button
                      class="primary-button"
                      :disabled="actingKey === `${activity.id}-${signup.id}-CANCELLED`"
                      @click="updateSignup(activity.id, signup.id, 'CANCELLED')"
                    >
                      同意取消
                    </button>
                    <button
                      class="ghost-button"
                      :disabled="actingKey === `${activity.id}-${signup.id}-SUCCESS`"
                      @click="updateSignup(activity.id, signup.id, 'SUCCESS')"
                    >
                      驳回申请
                    </button>
                  </div>
                </div>
              </div>
            </section>

            <section class="admin-activity-section">
              <div class="activity-detail-section-title">
                <div>
                  <h3>活动评价</h3>
                  <p class="muted">查看参与者提交的活动反馈。</p>
                </div>
              </div>
              <div v-if="!activity.reviews.length" class="message-box">当前还没有活动评价。</div>
              <div v-else class="list">
                <div v-for="review in activity.reviews" :key="review.id" class="item-card">
                  <div class="actions">
                    <strong>{{ review.user.name }}</strong>
                    <span class="badge secondary">{{ review.score }} 分</span>
                  </div>
                  <p>{{ review.content }}</p>
                </div>
              </div>
            </section>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>
