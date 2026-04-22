<script setup>
import { onMounted, reactive, ref } from 'vue'
import { request } from '../../lib/api'
import { sessionStore } from '../../stores/session'

const activities = ref([])
const form = reactive({
  title: '',
  content: '',
  activityType: 'ONLINE',
  location: '',
  liveUrl: '',
  replayUrl: '',
  startTime: '2026-04-20T19:00:00',
  endTime: '2026-04-20T21:00:00',
  limitCount: 30,
  status: 'PUBLISHED'
})

async function load() {
  activities.value = await request('/admin/activities')
}

async function create() {
  await request('/admin/activities', {
    method: 'POST',
    body: JSON.stringify({
      ...form,
      createdBy: sessionStore.currentUser.id,
      limitCount: Number(form.limitCount)
    })
  })
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page-grid">
    <section class="card span-4">
      <h2>发布活动</h2>
      <div class="field"><label>标题</label><input v-model="form.title" /></div>
      <div class="field"><label>内容</label><textarea v-model="form.content" /></div>
      <div class="field"><label>类型</label><select v-model="form.activityType"><option value="ONLINE">线上</option><option value="OFFLINE">线下</option></select></div>
      <div class="field"><label>地点</label><input v-model="form.location" /></div>
      <div class="field"><label>直播地址</label><input v-model="form.liveUrl" /></div>
      <div class="field"><label>开始时间</label><input v-model="form.startTime" type="datetime-local" /></div>
      <div class="field"><label>结束时间</label><input v-model="form.endTime" type="datetime-local" /></div>
      <div class="field"><label>人数上限</label><input v-model="form.limitCount" type="number" /></div>
      <div class="actions"><button class="primary-button" @click="create">创建活动</button></div>
    </section>

    <section class="card span-8">
      <h2>活动管理</h2>
      <div class="list">
        <div v-for="activity in activities" :key="activity.id" class="item-card">
          <strong>{{ activity.title }}</strong>
          <p class="muted">{{ activity.displayTime }} · {{ activity.activityType }}</p>
          <p>{{ activity.content }}</p>
        </div>
      </div>
    </section>
  </div>
</template>
