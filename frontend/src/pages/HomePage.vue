<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { currentUserId, request } from '../lib/api'

const router = useRouter()
const data = ref(null)

async function load() {
  data.value = await request(`/home?userId=${currentUserId()}`)
}

function openTopic(topic) {
  router.push({
    path: '/community',
    query: { keyword: topic }
  })
}

function openAnnouncement(item) {
  router.push({
    path: '/activities',
    query: { keyword: item.title }
  })
}

function openMatch(match) {
  router.push(`/users/${match.id}`)
}

function openPost(post) {
  router.push({
    path: '/community',
    query: { focusPost: String(post.id) }
  })
}

function openActivity(activity) {
  router.push(`/activities/${activity.id}`)
}

onMounted(load)
</script>

<template>
  <div v-if="data" class="page-grid">
    <section class="card hero-card span-12">
      <span class="badge">智能匹配平台</span>
      <h2>{{ data.hero.title }}</h2>
      <p>{{ data.hero.subtitle }}</p>
      <div class="actions">
        <button class="primary-button" @click="router.push('/matches')">开始匹配</button>
        <button class="ghost-button" @click="router.push('/activities')">查看活动</button>
      </div>
    </section>

    <section class="card span-4">
      <h3>个人状态</h3>
      <p class="stat-value">{{ data.currentUser.averageScore }}</p>
      <p class="muted">当前资料完整度：{{ data.currentUser.profileCompleted ? '已完善' : '待完善' }}</p>
    </section>

    <section class="card span-4">
      <h3>热门话题</h3>
      <div class="actions">
        <button
          v-for="topic in data.hotTopics"
          :key="topic"
          type="button"
          class="badge secondary home-topic-button"
          @click="openTopic(topic)"
        >
          {{ topic }}
        </button>
      </div>
    </section>

    <section class="card span-4">
      <h3>平台公告</h3>
      <div class="list">
        <button
          v-for="item in data.announcements"
          :key="item.title"
          type="button"
          class="item-card home-click-card"
          @click="openAnnouncement(item)"
        >
          <strong>{{ item.title }}</strong>
          <p class="muted">{{ item.content }}</p>
        </button>
      </div>
    </section>

    <section class="card span-4">
      <h3>推荐伙伴</h3>
      <div class="list">
        <button
          v-for="match in data.recommendedMatches"
          :key="match.id"
          type="button"
          class="item-card home-click-card"
          @click="openMatch(match)"
        >
          <strong>{{ match.name }}</strong>
          <p class="muted">{{ match.nativeLanguage }} -> {{ match.targetLanguage }}</p>
          <span class="badge">匹配分 {{ match.matchScore }}</span>
        </button>
      </div>
    </section>

    <section class="card span-4">
      <h3>热门动态</h3>
      <div class="list">
        <button
          v-for="post in data.hotPosts"
          :key="post.id"
          type="button"
          class="item-card home-click-card"
          @click="openPost(post)"
        >
          <strong>{{ post.author.name }}</strong>
          <p>{{ post.content }}</p>
        </button>
      </div>
    </section>

    <section class="card span-4">
      <h3>近期活动</h3>
      <div class="list">
        <button
          v-for="activity in data.upcomingActivities"
          :key="activity.id"
          type="button"
          class="item-card home-click-card"
          @click="openActivity(activity)"
        >
          <strong>{{ activity.title }}</strong>
          <p class="muted">{{ activity.displayTime }}</p>
          <span class="secondary-button">查看详情</span>
        </button>
      </div>
    </section>
  </div>
</template>
