<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { currentUserId, request } from '../lib/api'

const router = useRouter()
const route = useRoute()
const filters = reactive({ activityType: '', status: '' })
const activities = ref([])
const currentSlide = ref(0)
const loading = ref(false)
const submittingId = ref(null)
const message = ref('')
const error = ref('')

const activityImageMap = {
  '双语电影夜': 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1200&q=80',
  '城市文化漫步': 'https://images.unsplash.com/photo-1467269204594-9661b134dd2b?auto=format&fit=crop&w=1200&q=80',
  'HSK 口语冲刺工作坊': 'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1200&q=80'
}

let slideTimer = null

function resolveActivityImage(activity) {
  return activityImageMap[activity.title] || 'https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=1200&q=80'
}

function formatType(type) {
  return type === 'ONLINE' ? '线上活动' : '线下活动'
}

function signupStatusText(status) {
  if (status === 'SUCCESS') return '已报名'
  if (status === 'CANCEL_PENDING') return '取消审核中'
  if (status === 'CANCELLED') return '已取消'
  return '可报名'
}

function remainingText(activity) {
  if (!activity.limitCount) return '名额不限'
  const remaining = Math.max(activity.limitCount - activity.signupCount, 0)
  return `剩余 ${remaining} 个名额`
}

function canDirectSignup(activity) {
  const hasStarted = new Date(activity.startTime).getTime() <= Date.now()
  const isFull = activity.limitCount && activity.signupCount >= activity.limitCount
  return activity.status === 'PUBLISHED' && !hasStarted && !isFull && activity.signupStatus === 'NONE'
}

const filteredActivities = computed(() => {
  const keyword = String(route.query.keyword || '').trim().toLowerCase()
  return activities.value.filter((item) => {
    const matchKeyword = !keyword || `${item.title} ${item.content}`.toLowerCase().includes(keyword)
    return matchKeyword
  })
})

const carouselSlides = computed(() => {
  return activities.value.slice(0, 3).map((item) => ({
    ...item,
    cover: resolveActivityImage(item)
  }))
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const query = new URLSearchParams({
      userId: currentUserId(),
      ...(filters.activityType ? { activityType: filters.activityType } : {}),
      ...(filters.status ? { status: filters.status } : {})
    })
    activities.value = await request(`/activities?${query.toString()}`)
  } catch (err) {
    error.value = err.message || '活动列表加载失败'
  } finally {
    loading.value = false
  }
}

function selectType(type) {
  filters.activityType = type
  load()
}

async function directSignup(activityId) {
  submittingId.value = activityId
  message.value = ''
  error.value = ''
  try {
    await request(`/activities/${activityId}/signups`, {
      method: 'POST',
      body: JSON.stringify({ userId: currentUserId(), signupType: 'SIGNUP' })
    })
    message.value = '报名成功，已为你刷新活动状态。'
    await load()
  } catch (err) {
    error.value = err.message || '活动报名失败'
  } finally {
    submittingId.value = null
  }
}

function nextSlide() {
  if (!carouselSlides.value.length) return
  currentSlide.value = (currentSlide.value + 1) % carouselSlides.value.length
}

function startSlideTimer() {
  if (slideTimer) clearInterval(slideTimer)
  slideTimer = setInterval(nextSlide, 4000)
}

onMounted(async () => {
  await load()
  startSlideTimer()
})

onBeforeUnmount(() => {
  if (slideTimer) clearInterval(slideTimer)
})
</script>

<template>
  <div class="page-grid">
    <section class="card span-12 activities-overview-card">
      <div class="activities-overview-copy">
        <span class="badge">活动广场</span>
        <h2>找到适合参与的交流活动</h2>
        <p class="muted">浏览活动、查看名额情况，并直接从列表完成报名。</p>
      </div>
      <div class="activities-overview-stats">
        <article class="mini-stat-card activities-mini-stat">
          <strong>{{ activities.length }}</strong>
          <span>活动总数</span>
        </article>
        <article class="mini-stat-card activities-mini-stat">
          <strong>{{ carouselSlides.length }}</strong>
          <span>精选活动</span>
        </article>
      </div>
    </section>

    <section class="card span-12">
      <div class="section-heading">
        <div>
          <h2>活动精选</h2>
          <p class="muted">优先展示近期可报名、可参与的重点活动。</p>
        </div>
      </div>

      <div v-if="carouselSlides.length" class="activity-carousel">
        <div
          v-for="(slide, index) in carouselSlides"
          :key="slide.id"
          class="activity-carousel-slide"
          :class="{ active: index === currentSlide }"
          :style="{ backgroundImage: `linear-gradient(135deg, rgba(20, 61, 114, 0.58), rgba(163, 72, 47, 0.48)), url(${slide.cover})` }"
        >
          <div class="activity-carousel-copy">
            <span class="badge">{{ formatType(slide.activityType) }}</span>
            <h3>{{ slide.title }}</h3>
            <p>{{ slide.content }}</p>
            <button class="primary-button" @click="router.push(`/activities/${slide.id}`)">查看详情</button>
          </div>
        </div>

        <div class="activity-carousel-dots">
          <button
            v-for="(_, index) in carouselSlides"
            :key="index"
            type="button"
            class="activity-carousel-dot"
            :class="{ active: index === currentSlide }"
            @click="currentSlide = index"
          ></button>
        </div>
      </div>

      <div class="actions">
        <button class="ghost-button" @click="selectType('')">全部活动</button>
        <button class="ghost-button" @click="selectType('ONLINE')">线上活动</button>
        <button class="ghost-button" @click="selectType('OFFLINE')">线下活动</button>
      </div>

      <div v-if="message" class="message-box success-box">{{ message }}</div>
      <div v-if="error" class="message-box">{{ error }}</div>
    </section>

    <section v-if="loading" class="card span-12">
      <div class="message-box">正在加载活动列表...</div>
    </section>

    <section v-for="activity in filteredActivities" :key="activity.id" class="card span-4 activity-card activity-card-enhanced">
      <img :src="resolveActivityImage(activity)" :alt="activity.title" class="activity-card-cover" />
      <div class="activity-card-body">
        <div class="activity-card-head">
          <span class="badge">{{ formatType(activity.activityType) }}</span>
          <span class="badge secondary">{{ signupStatusText(activity.signupStatus) }}</span>
        </div>
        <h3>{{ activity.title }}</h3>
        <p>{{ activity.content }}</p>
        <p class="muted">{{ activity.displayTime }}</p>
        <p class="muted">{{ remainingText(activity) }}</p>
        <div class="actions">
          <button class="primary-button" @click="router.push(`/activities/${activity.id}`)">查看详情</button>
          <button
            v-if="canDirectSignup(activity)"
            class="secondary-button"
            :disabled="submittingId === activity.id"
            @click="directSignup(activity.id)"
          >
            {{ submittingId === activity.id ? '报名中...' : '立即报名' }}
          </button>
        </div>
      </div>
    </section>
  </div>
</template>
