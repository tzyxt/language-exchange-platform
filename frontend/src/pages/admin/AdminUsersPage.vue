<script setup>
import { onMounted, ref } from 'vue'
import { request } from '../../lib/api'

const users = ref([])

async function load() {
  users.value = await request('/admin/users')
}

async function updateStatus(id, status) {
  await request(`/admin/users/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status })
  })
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page-grid">
    <section class="card span-12">
      <h2>用户管理</h2>
      <div class="table-list">
        <div v-for="user in users" :key="user.id" class="table-item">
          <div>
            <strong>{{ user.name }}</strong>
            <p class="muted">{{ user.account }} · {{ user.school }}</p>
          </div>
          <span class="badge">{{ user.status }}</span>
          <span class="muted">{{ user.nativeLanguage }} -> {{ user.targetLanguage }}</span>
          <div class="actions">
            <button class="secondary-button" @click="updateStatus(user.id, 'NORMAL')">恢复</button>
            <button class="ghost-button" @click="updateStatus(user.id, 'DISABLED')">禁用</button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
