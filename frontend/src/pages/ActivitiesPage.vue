<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { currentUserId, request } from '../lib/api'

const router = useRouter()
const route = useRoute()
const filters = reactive({ activityType: '', status: '' })
const activities = ref([])
const currentSlide = ref(0)

const activityImageMap = {
  双语电影夜: 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1200&q=80',
  城市文化漫步: 'https://images.unsplash.com/photo-1467269204594-9661b134dd2b?auto=format&fit=crop&w=1200&q=80',
  'HSK 口语冲刺工作坊': 'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1200&q=80'
}

let slideTimer = null

function resolveActivityImage(activity) {
  return activityImageMap[activity.title] || 'https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=1200&q=80'
}

const filteredActivities = computed(() => {
  const keyword = String(route.query.keyword || '').trim()
  if (!keyword) return activities.value
  return activities.value.filter((item) => `${item.title} ${item.content}`.includes(keyword))
})

const carouselSlides = computed(() => {
  return activities.value.slice(0, 3).map((item) => ({
    ...item,
    cover: resolveActivityImage(item)
  }))
})

async function load() {
  const query = new URLSearchParams({
    userId: currentUserId(),
    ...(filters.activityType ? { activityType: filters.activityType } : {}),
    ...(filters.status ? { status: filters.status } : {})
  })
  activities.value = await request(`/activities?${query.toString()}`)
}

function selectType(type) {
  filters.activityType = type
  load()
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
    <section class="card span-12">
      <h2>活动广场</h2>

      <div v-if="carouselSlides.length" class="activity-carousel">
        <div
          v-for="(slide, index) in carouselSlides"
          :key="slide.id"
          class="activity-carousel-slide"
          :class="{ active: index === currentSlide }"
          :style="{ backgroundImage: `linear-gradient(135deg, rgba(20, 61, 114, 0.58), rgba(163, 72, 47, 0.48)), url(${slide.cover})` }"
        >
          <div class="activity-carousel-copy">
            <span class="badge">{{ slide.activityType === 'ONLINE' ? '线上活动' : '线下活动' }}</span>
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
    </section>

    <section v-for="activity in filteredActivities" :key="activity.id" class="card span-4 activity-card">
      <img :src="resolveActivityImage(activity)" :alt="activity.title" class="activity-card-cover" />
      <div class="activity-card-body">
        <span class="badge">{{ activity.activityType }}</span>
        <h3>{{ activity.title }}</h3>
        <p>{{ activity.content }}</p>
        <p class="muted">{{ activity.displayTime }}</p>
        <p class="muted">报名 {{ activity.signupCount }} / {{ activity.limitCount || '不限' }}</p>
        <div class="actions">
          <button class="primary-button" @click="router.push(`/activities/${activity.id}`)">查看详情</button>
          <span v-if="activity.joined" class="badge success">已报名</span>
        </div>
      </div>
    </section>
  </div>
</template>
