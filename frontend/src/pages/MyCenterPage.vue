<script setup>
import { computed, onMounted, ref } from 'vue'
import { currentUserId, request } from '../lib/api'
import { getFavoriteVocabulary, removeFavoriteVocabulary } from '../lib/vocabulary'

const data = ref(null)
const favoriteWords = ref([])

const favoriteWordCount = computed(() => favoriteWords.value.length)

async function load() {
  data.value = await request(`/me/${currentUserId()}/overview`)
  favoriteWords.value = getFavoriteVocabulary()
}

function removeFavoriteWord(word) {
  favoriteWords.value = removeFavoriteVocabulary(word)
}

onMounted(load)
</script>

<template>
  <div v-if="data" class="page-grid">
    <section class="card span-12 profile-summary-card">
      <div class="profile-summary-main">
        <img :src="data.profile.avatar" :alt="data.profile.name" class="profile-summary-avatar" />
        <div>
          <h2>{{ data.profile.name }}</h2>
          <p>{{ data.profile.introduction }}</p>
          <div class="actions">
            <span class="badge secondary">{{ data.profile.school }}</span>
            <span class="badge secondary">{{ data.profile.college }}</span>
            <span class="badge">{{ data.profile.nativeLanguage }} -> {{ data.profile.targetLanguage }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="card span-3 overview-stat-panel">
      <h3>匹配记录</h3>
      <p class="stat-value">{{ data.stats.matchCount }}</p>
      <p class="muted">已经建立联系的语伴数量</p>
    </section>
    <section class="card span-3 overview-stat-panel">
      <h3>活动参与</h3>
      <p class="stat-value">{{ data.stats.activityCount }}</p>
      <p class="muted">已报名并参与的活动数量</p>
    </section>
    <section class="card span-3 overview-stat-panel">
      <h3>收藏内容</h3>
      <p class="stat-value">{{ data.stats.favoriteCount }}</p>
      <p class="muted">已收藏的社区内容数量</p>
    </section>
    <section class="card span-3 overview-stat-panel">
      <h3>收藏单词</h3>
      <p class="stat-value">{{ favoriteWordCount }}</p>
      <p class="muted">来自聊天词汇助手的个人词库</p>
    </section>

    <section class="card span-6">
      <div class="section-heading">
        <div>
          <h3>最近匹配记录</h3>
          <p class="muted">保留最近建立联系的语伴信息，方便继续交流。</p>
        </div>
      </div>
      <div class="list">
        <div v-for="item in data.recentMatches" :key="item.id" class="record-card">
          <div class="record-card-main">
            <img :src="item.avatar" :alt="item.name" class="record-avatar" />
            <div>
              <strong>{{ item.name }}</strong>
              <p class="muted">{{ item.school }} · {{ item.college }}</p>
              <p>{{ item.introduction }}</p>
            </div>
          </div>
          <div class="record-card-side">
            <span class="badge">匹配分 {{ item.matchScore }}</span>
            <span class="badge secondary">{{ item.communicationGoal }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="card span-6">
      <div class="section-heading">
        <div>
          <h3>消息通知记录</h3>
          <p class="muted">报名、审核、互动等消息会按时间顺序集中展示。</p>
        </div>
      </div>
      <div class="list">
        <div v-for="item in data.notifications" :key="item.id" class="record-card notification-record-card">
          <div class="record-card-main">
            <div class="record-dot"></div>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.content }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="card span-12">
      <div class="section-heading">
        <div>
          <h3>我的收藏单词</h3>
          <p class="muted">这里会同步展示你在聊天词汇助手里收藏的单词，方便集中复习。</p>
        </div>
      </div>

      <div v-if="!favoriteWords.length" class="message-box">
        你还没有收藏单词。去聊天页的“词汇助手”里点收藏后，这里就会自动出现。
      </div>

      <div v-else class="favorite-vocabulary-grid">
        <article v-for="item in favoriteWords" :key="item.normalized" class="favorite-word-card">
          <div class="favorite-word-head">
            <div>
              <strong>{{ item.word }}</strong>
              <p class="muted">{{ item.phonetic }}</p>
            </div>
            <span class="badge secondary">{{ item.topic }}</span>
          </div>
          <p>{{ item.meaning }}</p>
          <div class="actions">
            <span class="muted">收藏时间 {{ new Date(item.savedAt).toLocaleString('zh-CN') }}</span>
            <button class="ghost-button" type="button" @click="removeFavoriteWord(item.word)">移除</button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>
