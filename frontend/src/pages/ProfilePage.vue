<script setup>
import { onMounted, reactive, ref } from 'vue'
import { currentUserId, request } from '../lib/api'

const form = reactive({
  name: '',
  gender: '',
  school: '',
  college: '',
  grade: '',
  avatar: '',
  nativeLanguage: '',
  targetLanguage: '',
  languageLevel: '',
  interestTags: '',
  targetTags: '',
  availableTime: '',
  preferredMode: '',
  communicationGoal: '',
  introduction: ''
})
const message = ref('')

async function load() {
  const data = await request(`/profile/${currentUserId()}`)
  Object.assign(form, {
    ...data,
    interestTags: (data.interestTags || []).join('、'),
    targetTags: (data.targetTags || []).join('、')
  })
}

async function save() {
  const payload = {
    ...form,
    interestTags: form.interestTags.split(/[、,]/).map((item) => item.trim()).filter(Boolean),
    targetTags: form.targetTags.split(/[、,]/).map((item) => item.trim()).filter(Boolean)
  }
  await request(`/profile/${currentUserId()}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
  message.value = '资料已保存'
}

onMounted(load)
</script>

<template>
  <div class="page-grid">
    <section class="card span-12">
      <h2>个人资料与用户画像</h2>
      <p class="muted">完善你的语言能力、兴趣标签和交流目标，系统将根据这些信息生成高质量推荐。</p>
      <div v-if="message" class="message-box">{{ message }}</div>
      <div class="form-grid">
        <div class="field"><label>昵称</label><input v-model="form.name" /></div>
        <div class="field"><label>性别</label><input v-model="form.gender" /></div>
        <div class="field"><label>学校</label><input v-model="form.school" /></div>
        <div class="field"><label>院系</label><input v-model="form.college" /></div>
        <div class="field"><label>年级</label><input v-model="form.grade" /></div>
        <div class="field"><label>头像地址</label><input v-model="form.avatar" /></div>
        <div class="field"><label>母语</label><input v-model="form.nativeLanguage" /></div>
        <div class="field"><label>目标语言</label><input v-model="form.targetLanguage" /></div>
        <div class="field"><label>语言等级</label><input v-model="form.languageLevel" /></div>
        <div class="field"><label>空闲时间</label><input v-model="form.availableTime" /></div>
        <div class="field"><label>交流方式</label><input v-model="form.preferredMode" /></div>
        <div class="field"><label>交流目标</label><input v-model="form.communicationGoal" /></div>
      </div>
      <div class="field">
        <label>兴趣标签</label>
        <input v-model="form.interestTags" placeholder="电影、旅行、摄影" />
      </div>
      <div class="field">
        <label>目标标签</label>
        <input v-model="form.targetTags" placeholder="口语练习、文化交流" />
      </div>
      <div class="field">
        <label>个人简介</label>
        <textarea v-model="form.introduction" />
      </div>
      <div class="actions">
        <button class="primary-button" @click="save">保存资料</button>
      </div>
    </section>
  </div>
</template>
